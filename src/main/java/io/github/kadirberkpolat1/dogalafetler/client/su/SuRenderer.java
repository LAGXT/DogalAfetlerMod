package io.github.kadirberkpolat1.dogalafetler.client.su;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.su.Cekilme;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import io.github.kadirberkpolat1.dogalafetler.su.SuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.su.DenizAlani;
import io.github.kadirberkpolat1.dogalafetler.su.KurulumButcesi;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import io.github.kadirberkpolat1.dogalafetler.su.SuAraliklari;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Sanal su yüzeyi (şader'siz). İki {@link VertexBuffer}: yakın halka (kamera çevresi {@link #YAKIN} blok, 1 blok hücre,
 * dalgacık, yan yüzler, köpük) her oyun tick'inde; uzak LOD ({@link #LOD}x{@link #LOD} blok düz hücre, görüş mesafesine
 * kadar; kalite ve config sınırları) {@link #UZAK_YENILEME} tick'te bir kurulur (SU-02). Çizim {@code AFTER_TRANSLUCENT_BLOCKS} aşamasında vanilla
 * {@code rendertype_translucent} şaderi, su dokusu, biyom su rengi ve ışık haritasıyla yapılır. Biyom rengi ve ışık
 * sütun başına önbellektedir (vanilla renk önbelleği 256 chunk ile sınırlı; LOD onu taşırıyordu).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SuRenderer {
    static final int YAKIN = 40;
    static final int LOD = 4;
    static final int UZAK_YENILEME = 10;
    private static final ResourceLocation SU = ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");
    /** Kendi köpük dokumuz (animasyonlu, {@code tools/assets/su_kopuk.py}); blok atlası {@code block/} klasöründen toplar. */
    private static final ResourceLocation KOPUK = DogalAfetler.konum("block/su_kopuk");
    /** Işık demetleri: tepe rengi ve en çok sayısı (kalite). */
    private static final int ISIK_RENGI = 0xBFE6FF;
    /** Gökyüzü görmeyen suda (mağara) demetler fener/ışıltaşı ışığının sıcak rengini alır. */
    private static final int SICAK_ISIK_RENGI = 0xFFC27A;
    private static final int N = 2 * YAKIN + 1;
    private static final float[] UST = new float[N * N];
    private static final float[] ALT = new float[N * N];
    /** 0 yok, 1 sanal (dalgalı), 2 sanal (sakin, mağara), 3 gerçek su (yalnız yan yüz komşusu), 4 çekilmede açığa çıkan dip. */
    private static final byte[] TUR = new byte[N * N];
    /** Köşe ızgarası ({@code (N+1)²}): dalgacık yüksekliği ve çok oktavlı yüzey eğimi (SU-28). */
    private static final float[] VH = new float[(N + 1) * (N + 1)];
    /** Mağara tavan yansıması gücü (köşe başına) ve hesaplandığı yapım (SU-35). */
    private static final float[] YANSIMA = new float[(N + 1) * (N + 1)];
    private static final int[] YANSIMA_NO = new int[(N + 1) * (N + 1)];
    private static final float[] VSX = new float[(N + 1) * (N + 1)];
    private static final float[] VSZ = new float[(N + 1) * (N + 1)];
    private static final SuAraliklari A = new SuAraliklari();
    private static final BufferBuilder BB = new BufferBuilder(1 << 21);
    /** Yakın halkanın kendi kurucusu: yapım birkaç tick'e yayılırken uzak LOD kurulumu araya girebilir. */
    private static final BufferBuilder BB_YAKIN = new BufferBuilder(1 << 21);
    /** Su altı ışık demetleri (her kare, birkaç dörtgen). */
    private static final BufferBuilder BB_ISIK = new BufferBuilder(1 << 16);
    /** Köşelerin yazıldığı kurucu. */
    private static BufferBuilder bb = BB;
    private static VertexBuffer vbIsik;
    private static final BlockPos.MutableBlockPos P = new BlockPos.MutableBlockPos();
    private static final BlockPos.MutableBlockPos P2 = new BlockPos.MutableBlockPos();
    private static final Map<Long, int[]> RENK = new HashMap<>();
    /** [0..255] ışık, [256..511] ışığın okunduğu y, [512] okuma tick'i. */
    private static final Map<Long, int[]> ISIK = new HashMap<>();
    private static final int[] DI = {1, -1, 0, 0};
    private static final int[] DJ = {0, 0, 1, -1};
    private static ClientLevel sonSeviye;
    private static VertexBuffer vbYakin;
    private static VertexBuffer vbUzak;
    private static boolean yakinBos = true;
    private static boolean uzakBos = true;
    private static long kurulanTick = Long.MIN_VALUE;
    private static long uzakTick = Long.MIN_VALUE;
    private static int yakinOx;
    private static int yakinOy;
    private static int yakinOz;
    private static int uzakOx;
    private static int uzakOy;
    private static int uzakOz;
    /** Kurulmakta olan tamponun Y kökeni; köşe Y'si buna göre göreli yazılır. */
    private static int oyAktif;
    private static float u0;
    private static float u1;
    private static float v0;
    private static float v1;
    private static float ku0;
    private static float ku1;
    private static float kv0;
    private static float kv1;
    /** Ufka yakın gök rengi (yansıma), gün doğumu/batımı rengiyle; normal eğimi oktav sayısı (kalite). */
    private static int ufukRengi = 0xA0C0E0;
    private static int oktav = 2;
    /** Kurulan su dünyası (dalga tepesi köpüğü ve dudak için). */
    private static SanalSuDunyasi aktifDunya;
    /** {@link #golgele} çıktısı (tahsissiz). */
    private static int gRenk;
    private static float gAlfa;
    private static int gIsik;
    /** Gölgeleme ışık kipi: açık havada güneş ve ufuk, mağarada tavandan blok ışığı ve sıcak, koyu yansıma. */
    private static double glX;
    private static double glY;
    private static float glGuc;
    private static int glRenk;
    private static int yansimaRengi;
    private static float esX;
    private static float esZ;
    private static final int[] QC = new int[4];
    private static final float[] QA = new float[4];
    private static final int[] QI = new int[4];
    private static int dortgen;
    private static int yakinDortgen;
    private static int uzakDortgen;
    // Ölçüm (kare başına CPU maliyeti).
    private static long olcYakinNs;
    private static long olcUzakNs;
    private static long olcCizimNs;
    private static int olcKare;
    private static int olcYakin;
    private static int olcUzak;
    /** Kurulan karenin ölçekli afet saati (dalga ve mağara seviyesi ondan). */
    private static double saat;
    /** Dalgacık bileşeni (kalite: 1..4); 1 iken eğim köpüğü yok. */
    private static int bilesen = 3;
    /** Güneş yönü (gökyüzü çiziminin dönüşüyle: (-sin θ, cos θ, 0)), yansıma gücü ve rengi; kamera konumu. */
    private static double gunesX;
    private static double gunesY;
    private static float parlamaSoluk;
    private static int gunesRengi = 0xFFFFFF;
    private static double kamX;
    private static double kamY;
    private static double kamZ;

    // Yakın halka yapımı (S-P2-3, SU-26): 0 boşta, 1 sütun sorgusu, 2 köşeler; satır satır, tick başına bütçeyle.
    private static int yapAsama;
    private static int yapSatir;
    private static double yapT;
    private static long yapTick = Long.MIN_VALUE;
    private static int yapOx;
    private static int yapOy;
    private static int yapOz;
    private static boolean yapVar;
    private static int yapDortgen;
    private static long yapNs;
    private static int yapTickSayisi;
    private static long yapSonTick;
    private static int yapSutun;
    /** Yapım sayısı: {@link #YANSIMA} köşe önbelleğinin damgası. */
    private static int yapNo;
    private static final KurulumButcesi BUTCE = new KurulumButcesi();
    /** Tick'in gerçek kurulum işi (borç hariç, ölçüm). */
    private static long tickIsNs;
    private static long tickIsTick = Long.MIN_VALUE;
    /** Yapım parçası (sütun): bütçe her parçadan sonra denetlenir, tek tick'in aşımı bir parçayla sınırlı (SU-33). */
    private static final int PARCA = 16;
    // Ölçüm: tick başına en çok kurulum ve yayılma (otomasyon özeti ve 10 sn'lik log).
    private static long olcEnCokTickNs;
    private static long olcTickToplam;
    private static long logYapim;
    private static long logYapimNs;
    private static long logEnCokTickNs;
    private static long logTickToplam;
    private static long logUzak;
    private static long logUzakNs;

    private SuRenderer() {
    }

    /** Maliyet sayacını sıfırlar. */
    public static void olcumSifirla() {
        olcYakinNs = 0;
        olcUzakNs = 0;
        olcCizimNs = 0;
        olcKare = 0;
        olcYakin = 0;
        olcUzak = 0;
        olcEnCokTickNs = 0;
        olcTickToplam = 0;
    }

    /** Son sıfırlamadan beri kare başına ortalama CPU maliyeti ve kurulum ortalamaları. */
    public static String olcumOzeti() {
        double kare = Math.max(1, olcKare);
        return String.format(Locale.ROOT,
                "kare=%d su_cpu_kare_basina=%.2fms (yakin_kurulum_ort=%.2fms x%d, yakin_tick_en_cok=%.2fms yayilma=%.1f tick, "
                        + "uzak_kurulum_ort=%.2fms x%d, cizim_ort=%.3fms) dortgen yakin=%d uzak=%d",
                olcKare, (olcYakinNs + olcUzakNs + olcCizimNs) / kare / 1e6, olcYakinNs / Math.max(1.0, olcYakin) / 1e6,
                olcYakin, olcEnCokTickNs / 1e6, olcTickToplam / Math.max(1.0, olcYakin), olcUzakNs / Math.max(1.0, olcUzak) / 1e6,
                olcUzak, olcCizimNs / kare / 1e6, yakinDortgen, uzakDortgen);
    }

    @SubscribeEvent
    public static void ciz(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        olcKare++;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        SanalSuDunyasi d = SanalSu.dunya(l);
        if (d == null) {
            yapIptal();
            yakinBos = true;
            uzakBos = true;
            return;
        }
        if (l != sonSeviye) {
            yapIptal();
            yakinBos = true;
            uzakBos = true;
            RENK.clear();
            ISIK.clear();
            sonSeviye = l;
        }
        if (RENK.size() > 4096) {
            RENK.clear();
        }
        if (ISIK.size() > 4096) {
            ISIK.clear();
        }
        long tick = l.getGameTime();
        saat = IstemciDonem.saat(e.getPartialTick());
        bilesen = switch (IstemciAyarlari.efektKalitesi()) {
            case DUSUK -> 1;
            case ORTA -> 2;
            case YUKSEK -> 3;
            case ULTRA -> 4;
        };
        Vec3 cam = e.getCamera().getPosition();
        gunesHazirla(l, e.getPartialTick(), cam);
        ufukHazirla(l, e.getPartialTick(), cam);
        oktav = bilesen - 1;
        aktifDunya = d;
        if (tick != kurulanTick) {
            kurulanTick = tick;
            spriteGuncelle(mc);
            if (tick % 200 == 0) {
                kurulumLogu();
            }
            int dx = (int) Math.floor(cam.x) - uzakOx;
            int dz = (int) Math.floor(cam.z) - uzakOz;
            if (tick - uzakTick >= UZAK_YENILEME || tick < uzakTick || dx * dx + dz * dz > 64) {
                uzakTick = tick;
                long t1 = System.nanoTime();
                uzakKur(mc, l, d, e.getCamera(), tick);
                long gecen = System.nanoTime() - t1;
                olcUzakNs += gecen;
                olcUzak++;
                logUzakNs += gecen;
                logUzak++;
            }
        }
        if (yapAsama == 0 && tick != yapTick) {
            yapBaslat(e.getCamera(), tick);
        }
        yapIlerlet(l, d, tick);
        if (yakinBos && uzakBos) {
            return;
        }
        long t2 = System.nanoTime();
        PoseStack ps = e.getPoseStack();
        Matrix4f proj = e.getProjectionMatrix();
        ShaderInstance sh = GameRenderer.getRendertypeTranslucentShader();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        mc.gameRenderer.lightTexture().turnOnLightLayer();
        // Vanilla chunk çizimi gibi: model-görünüm yalnız kamera dönüşü, öteleme CHUNK_OFFSET'te. Aksi hâlde şaderin
        // silindirik sis mesafesi mutlak Y ile hesaplanır ve su kamera yüksekliği kadar sisli görünür.
        Matrix4f mv = ps.last().pose();
        if (!uzakBos) {
            ofset(sh, uzakOx - cam.x, uzakOy - cam.y, uzakOz - cam.z);
            vbUzak.bind();
            vbUzak.drawWithShader(mv, proj, sh);
        }
        if (!yakinBos) {
            ofset(sh, yakinOx - cam.x, yakinOy - cam.y, yakinOz - cam.z);
            vbYakin.bind();
            vbYakin.drawWithShader(mv, proj, sh);
        }
        if (bilesen > 1 && IstemciSu.kameraSuda(e.getCamera())) {
            isinlariCiz(d, cam, mv, proj, sh);
        }
        ofset(sh, 0, 0, 0);
        VertexBuffer.unbind();
        mc.gameRenderer.lightTexture().turnOffLightLayer();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        olcCizimNs += System.nanoTime() - t2;
    }

    private static void ofset(ShaderInstance sh, double x, double y, double z) {
        if (sh.CHUNK_OFFSET != null) {
            sh.CHUNK_OFFSET.set((float) x, (float) y, (float) z);
        }
    }

    private static void spriteGuncelle(Minecraft mc) {
        TextureAtlasSprite sp = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SU);
        u0 = sp.getU0();
        u1 = sp.getU1();
        v0 = sp.getV0();
        v1 = sp.getV1();
        TextureAtlasSprite kp = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(KOPUK);
        ku0 = kp.getU0();
        ku1 = kp.getU1();
        kv0 = kp.getV0();
        kv1 = kp.getV1();
    }

    /** Uzak LOD mesafesi (chunk): görüş mesafesi, kalite (Düşük 8, Orta 12) ve config üst sınırının en küçüğü. */
    static int lodChunk(Minecraft mc) {
        int kalite = switch (IstemciAyarlari.efektKalitesi()) {
            case DUSUK -> 8;
            case ORTA -> 12;
            default -> 32;
        };
        return Math.min(Math.min(mc.options.getEffectiveRenderDistance(), kalite), SuAyarlari.istemci().lodMesafeChunk.get());
    }

    private static double dalgacik(double x, double z, double t) {
        double h = 0.06 * Math.sin(0.55 * x + 0.08 * t);
        if (bilesen > 1) {
            h += 0.04 * Math.sin(0.37 * z - 0.06 * t + 1.3);
        }
        if (bilesen > 2) {
            h += 0.03 * Math.sin(0.9 * (x + z) + 0.11 * t);
        }
        if (bilesen > 3) {
            h += 0.015 * Math.sin(1.7 * x - 1.3 * z + 0.19 * t);
        }
        return h;
    }

    /**
     * Yüzey eğimi {@link #esX}, {@link #esZ}: dalgacığın analitik türevi + yalnız normale giren kısa dalga oktavları
     * (kalite: Düşük 0, Orta 1, Yüksek 2, Ultra 3). Yüzey düz levha gibi değil, kırışıklı ışık yansıtır.
     */
    private static void egim(double x, double z, double t, double carpan) {
        double sx = 0.033 * Math.cos(0.55 * x + 0.08 * t);
        double sz = 0;
        if (bilesen > 1) {
            sz += 0.0148 * Math.cos(0.37 * z - 0.06 * t + 1.3);
        }
        if (bilesen > 2) {
            double c = 0.027 * Math.cos(0.9 * (x + z) + 0.11 * t);
            sx += c;
            sz += c;
        }
        if (bilesen > 3) {
            double c = Math.cos(1.7 * x - 1.3 * z + 0.19 * t);
            sx += 0.0255 * c;
            sz -= 0.0195 * c;
        }
        if (oktav > 0) {
            double c = 0.11 * Math.cos(1.9 * (0.765 * x + 0.644 * z) + 0.21 * t);
            sx += c * 0.765;
            sz += c * 0.644;
        }
        if (oktav > 1) {
            double c = 0.08 * Math.cos(3.7 * (-0.666 * x + 0.746 * z) + 0.33 * t + 0.9);
            sx -= c * 0.666;
            sz += c * 0.746;
        }
        if (oktav > 2) {
            double c = 0.055 * Math.cos(6.1 * (-0.575 * x - 0.818 * z) + 0.47 * t + 2.1);
            sx -= c * 0.575;
            sz -= c * 0.818;
        }
        esX = (float) (sx * carpan);
        esZ = (float) (sz * carpan);
    }

    /** Gökün ufka yakın rengi: vanilla gök rengi açılmış, gün doğumu/batımı rengine kaydırılmış (yüzey yansıması). */
    private static void ufukHazirla(ClientLevel l, float kismi, Vec3 cam) {
        Vec3 g = l.getSkyColor(cam, kismi);
        int gok = rgb(g.x, g.y, g.z);
        int u = parlat(gok, 1.2f);
        float[] sr = l.effects().getSunriseColor(l.getTimeOfDay(kismi), kismi);
        if (sr != null) {
            u = karistir(u, rgb(sr[0], sr[1], sr[2]), Math.min(0.9f, sr[3] * 0.9f));
        }
        ufukRengi = u;
    }

    private static int rgb(double r, double g, double b) {
        return ((int) (Math.max(0, Math.min(1, r)) * 255) << 16) | ((int) (Math.max(0, Math.min(1, g)) * 255) << 8)
                | (int) (Math.max(0, Math.min(1, b)) * 255);
    }

    private static int parlat(int c, float f) {
        return (Math.min(255, (int) ((c >> 16 & 255) * f)) << 16) | (Math.min(255, (int) ((c >> 8 & 255) * f)) << 8)
                | Math.min(255, (int) ((c & 255) * f));
    }

    /**
     * Köşe başına şader'siz su gölgelemesi (SU-28): dağınık ışık (normal · güneş), Fresnel (Schlick) ile ufuk rengi
     * yansıması ve alfa artışı, güneş parıltısı (^64). Kamera yüzeyin altındaysa yansıma yok, yüzey koyu turkuaz.
     */
    private static void golgele(int taban, float alfa, double px, double py, double pz, double sx, double sz, int isik) {
        double nl = Math.sqrt(sx * sx + 1 + sz * sz);
        double nx = -sx / nl;
        double ny = 1 / nl;
        double nz = -sz / nl;
        double vx = kamX - px;
        double vy = kamY - py;
        double vz = kamZ - pz;
        double vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (vl < 1e-6) {
            gRenk = taban;
            gAlfa = alfa;
            gIsik = isik;
            return;
        }
        vx /= vl;
        vy /= vl;
        vz /= vl;
        boolean alttan = vy < 0;
        double f = alttan ? 0 : 0.02 + 0.98 * Math.pow(1 - Math.max(0, nx * vx + ny * vy + nz * vz), 5);
        double dif = 0.86 + 0.32 * Math.max(0, nx * glX + ny * glY) * Math.max(0.3, glGuc);
        int c = parlat(taban, (float) dif);
        if (alttan) {
            c = karistir(c, 0x0E3A4A, 0.35f);
        }
        c = karistir(c, yansimaRengi, (float) Math.min(0.8, f));
        float spec = 0;
        if (glGuc > 0 && !alttan) {
            double hx = vx + glX;
            double hy = vy + glY;
            double hz = vz;
            double d = (nx * hx + ny * hy + nz * hz) / Math.sqrt(hx * hx + hy * hy + hz * hz);
            if (d > 0) {
                spec = (float) (Math.pow(d, 64) * glGuc * 1.6);
            }
        }
        if (spec > 0.02f) {
            c = karistir(c, glRenk, Math.min(1f, spec));
        }
        gRenk = c;
        gAlfa = (float) Math.min(0.95, Math.max(alfa + (0.92 - alfa) * f, Math.min(0.95f, spec)));
        gIsik = spec > 0.02f ? parlakIsik(isik, spec) : isik;
    }

    private static void gunesKipi() {
        glX = gunesX;
        glY = gunesY;
        glGuc = parlamaSoluk;
        glRenk = gunesRengi;
        yansimaRengi = ufukRengi;
    }

    /** Mağara suyu: gök yansıması yok; tavandaki ışık kaynaklarının sıcak parıltısı, gücü blok ışığıyla. */
    private static void magaraKipi(int isik) {
        float b = LightTexture.block(isik) / 15f;
        glX = 0.25;
        glY = 0.97;
        glGuc = 0.9f * b;
        glRenk = 0xFFE2B0;
        yansimaRengi = karistir(0x23272B, 0xFFCF8E, 0.45f * b);
    }

    private static long chunkAnahtari(int x, int z) {
        return ((long) (x >> 4) << 32) | ((z >> 4) & 0xffffffffL);
    }

    /** Sütunun biyom su rengi (vanilla karışımı, deniz seviyesinde); sütun başına bir kez hesaplanır. */
    private static int renk(ClientLevel l, int x, int z) {
        int[] c = RENK.computeIfAbsent(chunkAnahtari(x, z), a -> {
            int[] n = new int[256];
            Arrays.fill(n, -1);
            return n;
        });
        int i = (x & 15) | ((z & 15) << 4);
        int v = c[i];
        if (v == -1) {
            v = BiomeColors.getAverageWaterColor(l, P2.set(x, l.getSeaLevel(), z)) & 0xFFFFFF;
            c[i] = v;
        }
        return v;
    }

    /** Yüzeyin ışığı; y değişince ya da 40 tick'te bir yeniden okunur. */
    private static int isik(ClientLevel l, int x, int y, int z, long tick) {
        int[] c = ISIK.computeIfAbsent(chunkAnahtari(x, z), a -> {
            int[] n = new int[513];
            Arrays.fill(n, 256, 512, Integer.MIN_VALUE);
            n[512] = (int) tick;
            return n;
        });
        if ((int) tick - c[512] > 40) {
            Arrays.fill(c, 256, 512, Integer.MIN_VALUE);
            c[512] = (int) tick;
        }
        int i = (x & 15) | ((z & 15) << 4);
        if (c[256 + i] != y) {
            c[i] = LevelRenderer.getLightColor(l, P.set(x, y, z));
            c[256 + i] = y;
        }
        return c[i];
    }

    /** Yakın halka yapımını başlatır: köken, zaman ve tick yapım boyunca sabit (dalgacık ve dalga aynı anda). */
    private static void yapBaslat(Camera camera, long tick) {
        Vec3 cam = camera.getPosition();
        yapOx = (int) Math.floor(cam.x);
        yapOy = (int) Math.floor(cam.y);
        yapOz = (int) Math.floor(cam.z);
        yapT = saat;
        yapTick = tick;
        yapSonTick = Long.MIN_VALUE;
        yapSatir = 0;
        yapSutun = 0;
        yapNo++;
        yapVar = false;
        yapNs = 0;
        yapTickSayisi = 0;
        yapAsama = 1;
    }

    private static void yapIptal() {
        if (yapAsama == 2) {
            BB_YAKIN.end().release();
        }
        yapAsama = 0;
    }

    /**
     * Yapımı {@code su.yakinKurulumButcesiMs} (tick başına, CLIENT) dolana kadar {@link #PARCA} sütunluk parçalarla sürdürür;
     * kalanı sonraki tick'e, aşım sonraki tick'in bütçesinden düşülür ({@link KurulumButcesi}). Yapım bitene kadar önceki tampon
     * çizilir.
     */
    private static void yapIlerlet(ClientLevel l, SanalSuDunyasi d, long tick) {
        BUTCE.tickBasla(tick, (long) (SuAyarlari.istemci().yakinKurulumButcesiMs.get() * 1e6));
        if (yapAsama == 0 || !BUTCE.calisabilir()) {
            return;
        }
        if (tickIsTick != tick) {
            tickIsTick = tick;
            tickIsNs = 0;
        }
        long t0 = System.nanoTime();
        if (yapSonTick != tick) {
            yapSonTick = tick;
            yapTickSayisi++;
        }
        bb = BB_YAKIN;
        oyAktif = yapOy;
        dortgen = yapDortgen;
        long onceki = t0;
        while (yapAsama != 0 && BUTCE.calisabilir()) {
            int j0 = yapSutun;
            int j1 = Math.min(N, j0 + PARCA);
            if (yapAsama == 1) {
                sorguParcasi(d, yapSatir, j0, j1);
            } else {
                for (int j = j0; j < j1; j++) {
                    if (TUR[yapSatir * N + j] != 0) {
                        sutun(l, d.alan(), yapOx - YAKIN + yapSatir, yapOz - YAKIN + j, yapSatir, j, yapT, yapTick);
                    }
                }
            }
            yapSutun = j1;
            if (j1 == N) {
                yapSutun = 0;
                satirBitti();
            }
            long simdi = System.nanoTime();
            BUTCE.harca(simdi - onceki);
            onceki = simdi;
        }
        yapDortgen = dortgen;
        bb = BB;
        long gecen = System.nanoTime() - t0;
        tickIsNs += gecen;
        yapNs += gecen;
        olcYakinNs += gecen;
        olcEnCokTickNs = Math.max(olcEnCokTickNs, tickIsNs);
        logEnCokTickNs = Math.max(logEnCokTickNs, tickIsNs);
        if (yapAsama == 0) {
            olcYakin++;
            olcTickToplam += yapTickSayisi;
            logYapim++;
            logYapimNs += yapNs;
            logTickToplam += yapTickSayisi;
        }
    }

    /** Satır sonu: aşama geçişi ya da yapımın bitmesi. */
    private static void satirBitti() {
        if (++yapSatir < N) {
            return;
        }
        yapSatir = 0;
        if (yapAsama == 1) {
            if (yapVar) {
                BB_YAKIN.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                dortgen = 0;
                yapAsama = 2;
            } else {
                yakinDortgen = 0;
                yakinBos = true;
                yapAsama = 0;
            }
        } else {
            yakinDortgen = dortgen;
            yakinBos = !yukle(BB_YAKIN, true);
            yakinOx = yapOx;
            yakinOy = yapOy;
            yakinOz = yapOz;
            yapAsama = 0;
        }
    }

    /** Halkanın {@code i}. sütun sırasının {@code [j0, j1)} parçası: köşeler ve su aralıkları ({@link #UST}, {@link #ALT}, {@link #TUR}). */
    private static void sorguParcasi(SanalSuDunyasi d, int i, int j0, int j1) {
        DenizAlani alan = d.alan();
        int jSon = j1 == N ? N : j1 - 1;
        for (int satir = i; satir <= (i == N - 1 ? N : i); satir++) {
            for (int j = j0; j <= jSon; j++) {
                int x = yapOx - YAKIN + satir;
                int z = yapOz - YAKIN + j;
                int v = satir * (N + 1) + j;
                VH[v] = (float) dalgacik(x, z, yapT);
                egim(x, z, yapT, 1);
                VSX[v] = esX;
                VSZ[v] = esZ;
            }
        }
        Cekilme[] cekilmeler = d.cekilmeDizisi();
        for (int j = j0; j < j1; j++) {
            int x = yapOx - YAKIN + i;
            int z = yapOz - YAKIN + j;
            int k = i * N + j;
            d.araliklar(x, z, yapT, A);
            if (A.adet() > 0) {
                int s = A.adet() - 1;
                UST[k] = (float) A.ust(s);
                ALT[k] = (float) A.alt(s);
                TUR[k] = (byte) (A.sakin(s) ? 2 : 1);
                yapVar = true;
            } else if (alan.su(x, z)) {
                UST[k] = alan.zemin(x, z);
                ALT[k] = alan.taban(x, z);
                TUR[k] = 3;
                for (Cekilme c : cekilmeler) {
                    if (c.icinde(x + 0.5, z + 0.5)) {
                        // Sanal yüzey dibin altına indi: gerçek su gizli, ıslak dip açıkta (koyu, parlak).
                        UST[k] = alan.taban(x, z);
                        TUR[k] = 4;
                        yapVar = true;
                        break;
                    }
                }
            } else {
                TUR[k] = 0;
            }
        }
    }

    /** 10 sn'de bir kurulum maliyeti log'a (S-P2-3 ölçümü): yapım başına ve tick başına en çok ms, yayılma. */
    private static void kurulumLogu() {
        if (logYapim == 0 && logUzak == 0) {
            return;
        }
        DogalAfetler.LOGGER.info("Sanal su: kurulum ms yakın_ort={} tick_en_cok={} (bütçe {}) yayılma={} tick yapım={} uzak_ort={} x{}",
                String.format(Locale.ROOT, "%.2f", logYapimNs / Math.max(1.0, logYapim) / 1e6),
                String.format(Locale.ROOT, "%.2f", logEnCokTickNs / 1e6),
                String.format(Locale.ROOT, "%.2f", SuAyarlari.istemci().yakinKurulumButcesiMs.get()),
                String.format(Locale.ROOT, "%.1f", logTickToplam / Math.max(1.0, logYapim)), logYapim,
                String.format(Locale.ROOT, "%.2f", logUzakNs / Math.max(1.0, logUzak) / 1e6), logUzak);
        logYapim = 0;
        logYapimNs = 0;
        logEnCokTickNs = 0;
        logTickToplam = 0;
        logUzak = 0;
        logUzakNs = 0;
    }

    private static void uzakKur(Minecraft mc, ClientLevel l, SanalSuDunyasi d, Camera camera, long tick) {
        Vec3 cam = camera.getPosition();
        int cx = (int) Math.floor(cam.x);
        int cz = (int) Math.floor(cam.z);
        uzakOx = cx;
        uzakOy = (int) Math.floor(cam.y);
        uzakOz = cz;
        oyAktif = uzakOy;
        int r = lodChunk(mc) * 16;
        int gx0 = Math.floorDiv(cx - r, LOD) * LOD;
        int gz0 = Math.floorDiv(cz - r, LOD) * LOD;
        bb = BB;
        BB.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        dortgen = 0;
        int tamIsik = LightTexture.pack(0, 15);
        for (int gx = gx0; gx <= cx + r; gx += LOD) {
            for (int gz = gz0; gz <= cz + r; gz += LOD) {
                int dx = gx + LOD / 2 - cx;
                int dz = gz + LOD / 2 - cz;
                boolean yakinda = gx >= cx - YAKIN && gx + LOD <= cx + YAKIN + 1 && gz >= cz - YAKIN
                        && gz + LOD <= cz + YAKIN + 1;
                if (yakinda || dx * dx + dz * dz > r * r) {
                    continue;
                }
                d.araliklar(gx + LOD / 2, gz + LOD / 2, saat, A);
                if (A.adet() == 0) {
                    continue;
                }
                float y = (float) A.ust(A.adet() - 1) - 0.16f;
                double mx = gx + LOD / 2.0;
                double mz = gz + LOD / 2.0;
                if (A.sakin(A.adet() - 1)) {
                    esX = 0;
                    esZ = 0;
                } else {
                    egim(mx, mz, saat, 2.5);
                }
                gunesKipi();
                golgele(renk(l, gx, gz), 0.72f, mx, y, mz, esX, esZ, tamIsik);
                int lodRenk = gRenk;
                float lodAlfa = gAlfa;
                for (Dalga w : d.dalgaDizisi()) {
                    // Uzakta da dalga tepesi okunsun: cepheye 0..5 blok karolar beyaz köpük, gövde turkuaz.
                    double h = w.yuzey(mx, mz, saat);
                    if (!Double.isNaN(h) && h - w.taban() > 1) {
                        double s = w.cepheyeUzaklik(mx, mz, saat);
                        float kp = s < 5 ? (float) Math.max(0, 0.9 * (1 - Math.abs(s - 2.5) / 2.5)) : 0;
                        lodRenk = karistir(karistir(lodRenk, 0x3E8F94, (float) Math.min(0.6, (h - w.taban()) / 8)), 0xFFFFFF, kp);
                        lodAlfa = Math.max(lodAlfa, 0.6f + 0.35f * kp);
                    }
                }
                dortgenUst(gx - uzakOx, gz - uzakOz, LOD, y, y, y, y, lodRenk, lodAlfa, gIsik);
            }
        }
        uzakDortgen = dortgen;
        uzakBos = !yukle(BB, false);
    }

    /** Kurucuyu ilgili tampona yükler; boşsa {@code false}. */
    private static boolean yukle(BufferBuilder kurucu, boolean yakin) {
        if (dortgen == 0) {
            kurucu.end().release();
            return false;
        }
        VertexBuffer vb = yakin ? vbYakin : vbUzak;
        if (vb == null) {
            vb = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            if (yakin) {
                vbYakin = vb;
            } else {
                vbUzak = vb;
            }
        }
        vb.bind();
        vb.upload(kurucu.end());
        VertexBuffer.unbind();
        return true;
    }

    /** Halkadaki dört komşudan biri kuru (ne sanal ne gerçek su) mı: sütun yeni kıyı çizgisinde. */
    private static boolean kuruKomsu(int i, int j) {
        for (int q = 0; q < 4; q++) {
            int n = komsu(i + DI[q], j + DJ[q]);
            if (n >= 0 && TUR[n] == 0) {
                return true;
            }
        }
        return false;
    }

    /** Güneş yönü, ufka yakınlıkla sönen yansıma gücü (yağmurda yok) ve gün batımında turuncuya kayan renk. */
    private static void gunesHazirla(ClientLevel l, float kismi, Vec3 cam) {
        double aci = l.getSunAngle(kismi);
        gunesX = -Math.sin(aci);
        gunesY = Math.cos(aci);
        parlamaSoluk = (float) (Math.max(0, Math.min(1, (gunesY + 0.08) / 0.2)) * (1 - l.getRainLevel(kismi)));
        float ufuk = (float) Math.max(0, Math.min(1, gunesY * 2.5));
        gunesRengi = (255 << 16) | ((int) (150 + 105 * ufuk) << 8) | (int) (70 + 185 * ufuk);
        kamX = cam.x;
        kamY = cam.y;
        kamZ = cam.z;
    }

    /** Yansıyan sütunun ışığı: blok ışığı yansıma gücüyle yükselir (gün batımında da parlak kalsın). */
    private static int parlakIsik(int isik, float p) {
        return LightTexture.pack(Math.max(LightTexture.block(isik), Math.round(15 * Math.min(1f, p))), LightTexture.sky(isik));
    }

    private static int komsu(int i, int j) {
        return i < 0 || j < 0 || i >= N || j >= N ? -1 : i * N + j;
    }

    private static void sutun(ClientLevel l, DenizAlani alan, int x, int z, int i, int j, double t, long tick) {
        int k = i * N + j;
        byte tur = TUR[k];
        float rx = x - yapOx;
        float rz = z - yapOz;
        int isik = isik(l, x, (int) Math.floor(UST[k]), z, tick);
        if (tur == 4) {
            float y = UST[k] + 0.02f;
            dortgenUst(rx, rz, 1, y, y, y, y, 0x2E2B22, 0.45f, isik);
            return;
        }
        float yu = UST[k] - 0.11f;
        float alt = ALT[k];
        int renk = renk(l, x, z);
        float egimH = 0;
        for (int q = 0; q < 4; q++) {
            int n = komsu(i + DI[q], j + DJ[q]);
            if (n >= 0 && TUR[n] != 0) {
                egimH = Math.max(egimH, Math.abs(UST[k] - UST[n]));
            }
        }
        float kopuk = tur == 1 && bilesen > 1 ? Math.max(0, Math.min(0.85f, (egimH - 0.6f) / 2.5f)) : 0;
        float derin = UST[k] - alt;
        boolean kiyi = kuruKomsu(i, j);
        if (tur == 2 && kiyi) {
            // Mağara suyunun duvara değen kenarı: ince, parlak ıslak şerit (köpük dokusu, yavaş nabız).
            kopuk = 0.22f + 0.08f * (float) Math.sin(t * 0.08 + (x * 3 + z) * 0.7);
        }
        if (tur == 1 && (derin < 1.5f || kiyi)) {
            // Kıyı şeridi: kuru karaya değen ya da çok sığ sütunda 2 sn periyotla ileri geri "yalayan" köpük bandı.
            float yalama = 0.55f + 0.45f * (float) Math.sin(t * 0.157 + (x + z) * 0.35);
            kopuk = Math.max(kopuk, (kiyi ? 0.85f : 0.45f) * yalama);
        }
        Dalga dalga = null;
        double dalgaH = 0;
        double dalgaS = 0;
        if (tur == 1 && aktifDunya != null) {
            for (Dalga w : aktifDunya.dalgaDizisi()) {
                double y = w.yuzey(x + 0.5, z + 0.5, t);
                if (!Double.isNaN(y) && y - w.taban() > dalgaH) {
                    dalgaH = y - w.taban();
                    dalga = w;
                    dalgaS = w.cepheyeUzaklik(x + 0.5, z + 0.5, t);
                }
            }
        }
        if (dalga != null && dalgaH > 1) {
            // Dalga gövdesi turkuaza kayar; tepede beyaz köpük bandı, arkasında yanal köpük çizgileri (tsunami.md).
            renk = karistir(renk, 0x3E8F94, (float) Math.min(0.7, dalgaH / 8));
            if (dalgaS < 5) {
                kopuk = Math.max(kopuk, (float) Math.max(0, 0.95 * (1 - Math.abs(dalgaS - 2.5) / 2.5)));
            } else if (dalgaS < 14) {
                double u = -(x + 0.5 - dalga.x0()) * dalga.yonZ() + (z + 0.5 - dalga.z0()) * dalga.yonX();
                double serit = 0.5 + 0.5 * Math.sin(u * 0.9 + dalgaS * 0.35);
                kopuk = Math.max(kopuk, (float) (0.45 * serit * (1 - (dalgaS - 5) / 9)));
            }
        }
        float a = Math.max(kopuk * 0.6f, derin < 1 ? 0.25f + 0.15f * derin : Math.min(0.85f, 0.4f + 0.06f * derin));
        int tabanRenk = karistir(renk, 0xFFFFFF, kopuk * 0.5f);
        if (tur == 2) {
            magaraKipi(isik);
        } else {
            gunesKipi();
        }
        if (tur == 1) {
            int v00 = i * (N + 1) + j;
            int v01 = v00 + 1;
            int v10 = v00 + N + 1;
            int v11 = v10 + 1;
            float y00 = yu + VH[v00];
            float y01 = yu + VH[v01];
            float y11 = yu + VH[v11];
            float y10 = yu + VH[v10];
            kosegolge(0, tabanRenk, a, x, y00, z, v00, isik);
            kosegolge(1, tabanRenk, a, x, y01, z + 1, v01, isik);
            kosegolge(2, tabanRenk, a, x + 1, y11, z + 1, v11, isik);
            kosegolge(3, tabanRenk, a, x + 1, y10, z, v10, isik);
            dortgenSerbest(rx, y00, rz, rx, y01, rz + 1, rx + 1, y11, rz + 1, rx + 1, y10, rz, false, x, z);
            if (kopuk > 0.08f) {
                for (int q = 0; q < 4; q++) {
                    QC[q] = 0xFFFFFF;
                    QA[q] = Math.min(1f, kopuk * 1.1f);
                    QI[q] = isik;
                }
                dortgenSerbest(rx, y00 + 0.015f, rz, rx, y01 + 0.015f, rz + 1, rx + 1, y11 + 0.015f, rz + 1, rx + 1,
                        y10 + 0.015f, rz, true, x, z);
            }
            if (dalga != null && bilesen > 1 && dalgaH > 3 && dalgaS > 1.8 && dalgaS < 3.2) {
                dudak(dalga, rx, rz, dalga.taban() + dalgaH, dalgaH, isik);
            }
        } else if (tur == 2) {
            // Mağara suyu sakin ama cımsız değil: dalgacığın %90'ı yükseklikte, eğim ışıkta ve tavan yansımasında (SU-28, SU-35).
            int v00 = i * (N + 1) + j;
            int v01 = v00 + 1;
            int v10 = v00 + N + 1;
            int v11 = v10 + 1;
            float y00 = yu + 0.9f * VH[v00];
            float y01 = yu + 0.9f * VH[v01];
            float y11 = yu + 0.9f * VH[v11];
            float y10 = yu + 0.9f * VH[v10];
            kosegolgeOlcekli(0, tabanRenk, a, x, y00, z, v00, isik);
            kosegolgeOlcekli(1, tabanRenk, a, x, y01, z + 1, v01, isik);
            kosegolgeOlcekli(2, tabanRenk, a, x + 1, y11, z + 1, v11, isik);
            kosegolgeOlcekli(3, tabanRenk, a, x + 1, y10, z, v10, isik);
            tavanYansimasi(l, 0, x, y00, z, v00);
            tavanYansimasi(l, 1, x, y01, z + 1, v01);
            tavanYansimasi(l, 2, x + 1, y11, z + 1, v11);
            tavanYansimasi(l, 3, x + 1, y10, z, v10);
            dortgenSerbest(rx, y00, rz, rx, y01, rz + 1, rx + 1, y11, rz + 1, rx + 1, y10, rz, false, x, z);
            if (kopuk > 0.08f) {
                for (int q = 0; q < 4; q++) {
                    QC[q] = 0xFFF4E0;
                    QA[q] = kopuk;
                    QI[q] = isik;
                }
                dortgenSerbest(rx, y00 + 0.015f, rz, rx, y01 + 0.015f, rz + 1, rx + 1, y11 + 0.015f, rz + 1, rx + 1,
                        y10 + 0.015f, rz, true, x, z);
            }
        }
        for (int q = 0; q < 4; q++) {
            int n = komsu(i + DI[q], j + DJ[q]);
            float taban;
            if (n >= 0 && TUR[n] != 0) {
                taban = Math.max(UST[n] - 0.11f, alt);
            } else if (tur == 3) {
                continue;
            } else {
                int zn = alan.zemin(x + DI[q], z + DJ[q]);
                if (zn != Integer.MIN_VALUE && zn >= UST[k]) {
                    continue;
                }
                taban = alt;
            }
            if (yu - taban < 0.05f) {
                continue;
            }
            float golge = q < 2 ? 0.6f : 0.8f;
            int cy = karistir(carp(renk, golge), 0xFFFFFF, kopuk * 0.8f);
            float ay = Math.max(0.55f, kopuk);
            float ex0;
            float ez0;
            float ex1;
            float ez1;
            switch (q) {
                case 0 -> {
                    ex0 = rx + 1;
                    ez0 = rz;
                    ex1 = rx + 1;
                    ez1 = rz + 1;
                }
                case 1 -> {
                    ex0 = rx;
                    ez0 = rz;
                    ex1 = rx;
                    ez1 = rz + 1;
                }
                case 2 -> {
                    ex0 = rx;
                    ez0 = rz + 1;
                    ex1 = rx + 1;
                    ez1 = rz + 1;
                }
                default -> {
                    ex0 = rx;
                    ez0 = rz;
                    ex1 = rx + 1;
                    ez1 = rz;
                }
            }
            dikey(ex0, ez0, ex1, ez1, taban, yu, cy, ay, isik);
        }
    }

    private static void kosegolgeOlcekli(int q, int taban, float alfa, double x, double y, double z, int v, int isik) {
        golgele(taban, alfa, x, y, z, VSX[v] * 1.8, VSZ[v] * 1.8, isik);
        QC[q] = gRenk;
        QA[q] = gAlfa;
        QI[q] = gIsik;
    }

    /**
     * Mağara suyunda tavan yansıması (SU-35, şader'siz): köşeden kameraya bakış dalgacık normaline göre yansıtılır, ışın
     * yarım blok adımlarla en çok 12 blok izlenir. Işık kaynağına (ışıltaşı, fener) vuran ışın keskin sıcak parlama, başka
     * bloka vuran ise o noktanın blok ışığıyla hafif amber verir; Fresnel ile yatık bakışta güçlenir. Dalgacık ışını
     * oynattığı için yansıma titrer. Köşe başına yapım başına bir kez hesaplanır ({@link #YANSIMA}).
     */
    private static void tavanYansimasi(ClientLevel l, int q, double px, double py, double pz, int v) {
        if (YANSIMA_NO[v] != yapNo) {
            YANSIMA_NO[v] = yapNo;
            YANSIMA[v] = yansimaGucu(l, px, py, pz, VSX[v] * 2.2, VSZ[v] * 2.2);
        }
        float g = YANSIMA[v];
        if (g <= 0.01f) {
            return;
        }
        QC[q] = karistir(QC[q], 0xFFD49A, Math.min(0.9f, g));
        QA[q] = Math.max(QA[q], Math.min(0.95f, 0.5f + 0.45f * g));
        QI[q] = parlakIsik(QI[q], g);
    }

    private static float yansimaGucu(ClientLevel l, double px, double py, double pz, double sx, double sz) {
        double vx = kamX - px;
        double vy = kamY - py;
        double vz = kamZ - pz;
        double vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (vy <= 0 || vl < 1e-6) {
            return 0;
        }
        vx /= vl;
        vy /= vl;
        vz /= vl;
        double nl = Math.sqrt(sx * sx + 1 + sz * sz);
        double nx = -sx / nl;
        double ny = 1 / nl;
        double nz = -sz / nl;
        double d = nx * vx + ny * vy + nz * vz;
        double rx = 2 * d * nx - vx;
        double ry = 2 * d * ny - vy;
        double rz = 2 * d * nz - vz;
        if (ry < 0.05) {
            return 0;
        }
        double fresnel = 0.3 + 0.7 * Math.pow(1 - Math.max(0, d), 3);
        int ox = Integer.MIN_VALUE;
        int oy = 0;
        int oz = 0;
        for (int s = 1; s <= 24; s++) {
            double t = s * 0.5;
            int bx = (int) Math.floor(px + rx * t);
            int by = (int) Math.floor(py + ry * t);
            int bz = (int) Math.floor(pz + rz * t);
            if (bx == ox && by == oy && bz == oz) {
                continue;
            }
            BlockState b = l.getBlockState(P.set(bx, by, bz));
            if (!b.isAir()) {
                int yayim = b.getLightEmission(l, P);
                if (yayim > 0) {
                    return (float) (fresnel * yayim / 15.0);
                }
                if (ox == Integer.MIN_VALUE) {
                    return 0;
                }
                double bl = (l.getBrightness(LightLayer.BLOCK, P.set(ox, oy, oz)) - 9) / 6.0;
                return bl > 0 ? (float) (fresnel * bl * bl * 0.4) : 0;
            }
            ox = bx;
            oy = by;
            oz = bz;
        }
        return 0;
    }

    private static void kosegolge(int q, int taban, float alfa, double x, double y, double z, int v, int isik) {
        golgele(taban, alfa, x, y, z, VSX[v], VSZ[v], isik);
        QC[q] = gRenk;
        QA[q] = gAlfa;
        QI[q] = gIsik;
    }

    /**
     * Kırılan dudak (tsunami.md "köpüklü yıkılan tepe", yalnız görsel ağ): tepeden ilerleme yönüne çıkan ve aşağı kıvrılan
     * iki şerit, turkuaz gövde üstüne köpük dokusu. Fizik sütun aralığıyla yetinir.
     */
    private static void dudak(Dalga w, float rx, float rz, double tepe, double h, int isik) {
        double boy = Math.min(3.0, 0.28 * h);
        float ex = (float) (-w.yonZ() * 0.55);
        float ez = (float) (w.yonX() * 0.55);
        float cx = rx + 0.5f;
        float cz = rz + 0.5f;
        float px = (float) (w.yonX() * boy);
        float pz = (float) (w.yonZ() * boy);
        float qx = px * 1.35f;
        float qz = pz * 1.35f;
        float y0 = (float) tepe;
        float y1 = (float) (tepe - 0.25 * boy);
        float y2 = (float) (tepe - 0.85 * boy);
        for (int kat = 0; kat < 2; kat++) {
            boolean doku = kat == 1;
            float ek = doku ? 0.02f : 0f;
            for (int q = 0; q < 4; q++) {
                QC[q] = doku ? 0xFFFFFF : 0x6FC2BC;
                QA[q] = doku ? 0.9f : 0.85f;
                QI[q] = isik;
            }
            dortgenSerbest(cx - ex, y0 + ek, cz - ez, cx + ex, y0 + ek, cz + ez, cx + ex + px, y1 + ek, cz + ez + pz,
                    cx - ex + px, y1 + ek, cz - ez + pz, doku, 0, 0);
            for (int q = 0; q < 4; q++) {
                QC[q] = doku ? 0xFFFFFF : 0xD8F0F0;
                QA[q] = doku ? 0.95f : 0.8f;
            }
            dortgenSerbest(cx - ex + px, y1 + ek, cz - ez + pz, cx + ex + px, y1 + ek, cz + ez + pz, cx + ex + qx, y2,
                    cz + ez + qz, cx - ex + qx, y2, cz - ez + qz, doku, 0, 0);
        }
    }

    /**
     * Serbest dörtgen; köşe renk/alfa/ışığı {@link #QC}, {@link #QA}, {@link #QI}. {@code kopukDoku} ise köpük dokusunun
     * {@code (bx, bz)} hücresine düşen yarısı (2 blokta bir tekrar), değilse su dokusu.
     */
    private static void dortgenSerbest(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, boolean kopukDoku, int bx, int bz) {
        float ua;
        float ub;
        float va;
        float vb;
        if (kopukDoku) {
            float du = (ku1 - ku0) * 0.5f;
            float dv = (kv1 - kv0) * 0.5f;
            ua = ku0 + du * (bx & 1);
            ub = ua + du;
            va = kv0 + dv * (bz & 1);
            vb = va + dv;
        } else {
            ua = u0;
            ub = u1;
            va = v0;
            vb = v1;
        }
        kose(x0, y0, z0, QC[0], QA[0], ua, va, QI[0], 1);
        kose(x1, y1, z1, QC[1], QA[1], ua, vb, QI[1], 1);
        kose(x2, y2, z2, QC[2], QA[2], ub, vb, QI[2], 1);
        kose(x3, y3, z3, QC[3], QA[3], ub, va, QI[3], 1);
        dortgen++;
    }

    /**
     * Su altı ışık demetleri (SU-28): kamera sanal suyun içindeyken yüzeyden aşağı inen, kameraya dönük, alt ucu güneşten
     * uzağa kayan, yavaşça salınan saydam huzmeler; toplamalı karışım, derinlik yazılmaz. Her huzme enine üç bant: dış
     * köşelerde alfa 0 (sert kenar yok), aşağı doğru %60 genişler, genişliği yavaşça dalgalanır. Gökyüzü ışığı düşükse
     * (mağara) renk sıcak. Kalite: Orta 10, Yüksek 18, Ultra 26 huzme.
     */
    private static void isinlariCiz(SanalSuDunyasi d, Vec3 cam, Matrix4f mv, Matrix4f proj, ShaderInstance sh) {
        double yuzey = d.ustu(cam.x, cam.y, cam.z, saat);
        if (Double.isNaN(yuzey)) {
            return;
        }
        int enCok = bilesen >= 4 ? 26 : bilesen == 3 ? 18 : 10;
        int ox = (int) Math.floor(cam.x);
        int oy = (int) Math.floor(cam.y);
        int oz = (int) Math.floor(cam.z);
        int gok = Minecraft.getInstance().level.getBrightness(LightLayer.SKY, P.set(ox, (int) Math.floor(yuzey) + 1, oz));
        int renk = gok < 8 ? SICAK_ISIK_RENGI : ISIK_RENGI;
        int demet = 0;
        bb = BB_ISIK;
        oyAktif = oy;
        BB_ISIK.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        dortgen = 0;
        int tam = LightTexture.pack(15, 15);
        float du = (u1 - u0) * 0.12f;
        for (int gx = Math.floorDiv(ox - 15, 3); gx * 3 <= ox + 15 && demet < enCok; gx++) {
            for (int gz = Math.floorDiv(oz - 15, 3); gz * 3 <= oz + 15 && demet < enCok; gz++) {
                int h = (gx * 73856093) ^ (gz * 19349663);
                h ^= h >>> 13;
                h *= 0x5bd1e995;
                h ^= h >>> 15;
                if ((h & 3) == 0) {
                    continue;
                }
                double px = gx * 3 + ((h >>> 4) & 255) / 255.0 * 3;
                double pz = gz * 3 + ((h >>> 12) & 255) / 255.0 * 3;
                double dx = px - cam.x;
                double dz = pz - cam.z;
                double dl = Math.sqrt(dx * dx + dz * dz);
                if (dl > 15 || dl < 0.8) {
                    continue;
                }
                double gen = (0.4 + ((h >>> 20) & 15) / 15.0 * 0.6) * (1 + 0.18 * Math.sin(saat * 0.035 + (h & 511)));
                double wx = -dz / dl * gen;
                double wz = dx / dl * gen;
                double ust = yuzey - 0.15;
                double boy = 9 + ((h >>> 24) & 7);
                double kx = -gunesX * boy * 0.25 + 0.4 * Math.sin(saat * 0.02 + (h & 255));
                double kz = 0.3 * Math.cos(saat * 0.017 + (h & 127));
                float a = (float) (0.3 * (0.6 + 0.4 * Math.sin(saat * 0.05 + (h & 63))));
                float sx = (float) (px - ox);
                float sz = (float) (pz - oz);
                float ua = u0 + du * ((h >>> 8) & 7);
                float yu = (float) ust;
                float ya = (float) (ust - boy);
                // Enine dört kenar çizgisi (-1, -1/3, 1/3, 1): dıştakiler alfa 0, alt uç 1,6 kat geniş ve güneşten uzağa kaymış.
                for (int b = 0; b < 3; b++) {
                    float s0 = -1 + b * (2f / 3f);
                    float s1 = s0 + 2f / 3f;
                    float a0 = b == 0 ? 0f : a;
                    float a1 = b == 2 ? 0f : a;
                    float u = ua + du * b / 3f;
                    kose(sx + (float) (wx * s0), yu, sz + (float) (wz * s0), renk, a0, u, v0, tam, 0);
                    kose(sx + (float) (wx * s1), yu, sz + (float) (wz * s1), renk, a1, u + du / 3f, v0, tam, 0);
                    kose(sx + (float) (wx * s1 * 1.6 + kx), ya, sz + (float) (wz * s1 * 1.6 + kz), renk, 0f, u + du / 3f, v1, tam, 0);
                    kose(sx + (float) (wx * s0 * 1.6 + kx), ya, sz + (float) (wz * s0 * 1.6 + kz), renk, 0f, u, v1, tam, 0);
                    dortgen++;
                }
                demet++;
            }
        }
        bb = BB;
        if (dortgen == 0) {
            BB_ISIK.end().release();
            return;
        }
        if (vbIsik == null) {
            vbIsik = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        vbIsik.bind();
        vbIsik.upload(BB_ISIK.end());
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        ofset(sh, ox - cam.x, oy - cam.y, oz - cam.z);
        vbIsik.drawWithShader(mv, proj, sh);
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }

    private static int carp(int c, float f) {
        return ((int) ((c >> 16 & 255) * f) << 16) | ((int) ((c >> 8 & 255) * f) << 8) | (int) ((c & 255) * f);
    }

    private static int karistir(int a, int b, float f) {
        int r = (int) ((a >> 16 & 255) * (1 - f) + (b >> 16 & 255) * f);
        int g = (int) ((a >> 8 & 255) * (1 - f) + (b >> 8 & 255) * f);
        int bl = (int) ((a & 255) * (1 - f) + (b & 255) * f);
        return (r << 16) | (g << 8) | bl;
    }

    private static void kose(float x, float y, float z, int c, float a, float u, float v, int isik, float ny) {
        bb.vertex(x, y - oyAktif, z).color((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, a).uv(u, v).uv2(isik)
                .normal(0, ny, 0).endVertex();
    }

    private static void dortgenUst(float x, float z, float b, float y00, float y01, float y11, float y10, int c, float a,
            int isik) {
        kose(x, y00, z, c, a, u0, v0, isik, 1);
        kose(x, y01, z + b, c, a, u0, v1, isik, 1);
        kose(x + b, y11, z + b, c, a, u1, v1, isik, 1);
        kose(x + b, y10, z, c, a, u1, v0, isik, 1);
        dortgen++;
    }

    private static void dikey(float x0, float z0, float x1, float z1, float alt, float ust, int c, float a, int isik) {
        float vb0 = v0 + (v1 - v0) * Math.min(1, ust - alt);
        kose(x0, ust, z0, c, a, u0, v0, isik, 0);
        kose(x1, ust, z1, c, a, u1, v0, isik, 0);
        kose(x1, alt, z1, c, a, u1, vb0, isik, 0);
        kose(x0, alt, z0, c, a, u0, vb0, isik, 0);
        dortgen++;
    }
}
