package io.github.kadirberkpolat1.dogalafetler.client.su;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.su.SuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.su.DenizAlani;
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
    private static final int N = 2 * YAKIN + 1;
    private static final float[] UST = new float[N * N];
    private static final float[] ALT = new float[N * N];
    /** 0 yok, 1 sanal (dalgalı), 2 sanal (sakin, mağara), 3 gerçek su (yalnız yan yüz komşusu). */
    private static final byte[] TUR = new byte[N * N];
    private static final SuAraliklari A = new SuAraliklari();
    private static final BufferBuilder BB = new BufferBuilder(1 << 21);
    /** Yakın halkanın kendi kurucusu: yapım birkaç tick'e yayılırken uzak LOD kurulumu araya girebilir. */
    private static final BufferBuilder BB_YAKIN = new BufferBuilder(1 << 21);
    /** Köşelerin yazıldığı kurucu. */
    private static BufferBuilder bb = BB;
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
    private static long butceTick = Long.MIN_VALUE;
    private static long butceHarcanan;
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
        ofset(sh, 0, 0, 0);
        VertexBuffer.unbind();
        mc.gameRenderer.lightTexture().turnOffLightLayer();
        RenderSystem.enableCull();
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
     * Yapımı {@code su.yakinKurulumButcesiMs} (tick başına, CLIENT) dolana kadar satır satır sürdürür; kalanı sonraki tick'e.
     * Yapım bitene kadar önceki tampon çizilir.
     */
    private static void yapIlerlet(ClientLevel l, SanalSuDunyasi d, long tick) {
        if (tick != butceTick) {
            butceTick = tick;
            butceHarcanan = 0;
        }
        long butce = (long) (SuAyarlari.istemci().yakinKurulumButcesiMs.get() * 1e6);
        if (yapAsama == 0 || butceHarcanan >= butce) {
            return;
        }
        long t0 = System.nanoTime();
        if (yapSonTick != tick) {
            yapSonTick = tick;
            yapTickSayisi++;
        }
        bb = BB_YAKIN;
        oyAktif = yapOy;
        dortgen = yapDortgen;
        while (yapAsama != 0 && butceHarcanan + System.nanoTime() - t0 < butce) {
            if (yapAsama == 1) {
                sorguSatiri(d, yapSatir);
                if (++yapSatir == N) {
                    yapSatir = 0;
                    if (yapVar) {
                        BB_YAKIN.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                        dortgen = 0;
                        yapAsama = 2;
                    } else {
                        yakinDortgen = 0;
                        yakinBos = true;
                        yapAsama = 0;
                    }
                }
            } else {
                for (int j = 0; j < N; j++) {
                    if (TUR[yapSatir * N + j] != 0) {
                        sutun(l, d.alan(), yapOx - YAKIN + yapSatir, yapOz - YAKIN + j, yapSatir, j, yapT, yapTick);
                    }
                }
                if (++yapSatir == N) {
                    yakinDortgen = dortgen;
                    yakinBos = !yukle(BB_YAKIN, true);
                    yakinOx = yapOx;
                    yakinOy = yapOy;
                    yakinOz = yapOz;
                    yapAsama = 0;
                }
            }
        }
        yapDortgen = dortgen;
        bb = BB;
        long gecen = System.nanoTime() - t0;
        butceHarcanan += gecen;
        yapNs += gecen;
        olcYakinNs += gecen;
        olcEnCokTickNs = Math.max(olcEnCokTickNs, butceHarcanan);
        logEnCokTickNs = Math.max(logEnCokTickNs, butceHarcanan);
        if (yapAsama == 0) {
            olcYakin++;
            olcTickToplam += yapTickSayisi;
            logYapim++;
            logYapimNs += yapNs;
            logTickToplam += yapTickSayisi;
        }
    }

    /** Halkanın {@code i}. sütun sırasının su aralıkları ({@link #UST}, {@link #ALT}, {@link #TUR}). */
    private static void sorguSatiri(SanalSuDunyasi d, int i) {
        DenizAlani alan = d.alan();
        for (int j = 0; j < N; j++) {
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
                int c = renk(l, gx, gz);
                float alfa = 0.72f;
                int isik = tamIsik;
                float p = A.sakin(A.adet() - 1) ? 0 : parlama(gx + LOD / 2.0, y, gz + LOD / 2.0, saat, 2.5);
                if (p > 0.02f) {
                    c = karistir(c, gunesRengi, Math.min(1f, p));
                    alfa = Math.max(alfa, Math.min(0.95f, p));
                    isik = parlakIsik(isik, p);
                }
                dortgenUst(gx - uzakOx, gz - uzakOz, LOD, y, y, y, y, c, alfa, isik);
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

    /**
     * Sahte güneş yansıması (tasarım notu "parlama"): dalgacık normali ile göz-güneş yarım vektörünün açısı, ^48; 0..1.
     * {@code egim} normal sapmasını büyütür (düz LOD karolarında yansıma şeridi dağılsın).
     */
    private static float parlama(double x, double y, double z, double t, double egim) {
        if (parlamaSoluk <= 0) {
            return 0;
        }
        double nx = -(dalgacik(x + 0.5, z, t) - dalgacik(x - 0.5, z, t)) * egim;
        double nz = -(dalgacik(x, z + 0.5, t) - dalgacik(x, z - 0.5, t)) * egim;
        double vx = kamX - x;
        double vy = kamY - y;
        double vz = kamZ - z;
        double vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (vl < 1e-6) {
            return 0;
        }
        double hx = vx / vl + gunesX;
        double hy = vy / vl + gunesY;
        double hz = vz / vl;
        double d = (nx * hx + hy + nz * hz) / (Math.sqrt(nx * nx + 1 + nz * nz) * Math.sqrt(hx * hx + hy * hy + hz * hz));
        return d <= 0 ? 0 : (float) (Math.pow(d, 48) * parlamaSoluk);
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
        float yu = UST[k] - 0.11f;
        float alt = ALT[k];
        int isik = isik(l, x, (int) Math.floor(UST[k]), z, tick);
        int renk = renk(l, x, z);
        float egim = 0;
        for (int q = 0; q < 4; q++) {
            int n = komsu(i + DI[q], j + DJ[q]);
            if (n >= 0 && TUR[n] != 0) {
                egim = Math.max(egim, Math.abs(UST[k] - UST[n]));
            }
        }
        float kopuk = tur == 1 && bilesen > 1 ? Math.max(0, Math.min(0.85f, (egim - 0.6f) / 2.5f)) : 0;
        float derin = UST[k] - alt;
        if (tur == 1 && (derin < 1.5f || kuruKomsu(i, j))) {
            // Kıyı şeridi: kuru karaya değen ya da çok sığ sütunda 2 sn periyotla ileri geri "yalayan" köpük bandı.
            float yalama = 0.55f + 0.45f * (float) Math.sin(t * 0.157 + (x + z) * 0.35);
            kopuk = Math.max(kopuk, (kuruKomsu(i, j) ? 0.75f : 0.4f) * yalama);
        }
        float a = Math.max(kopuk, derin < 1 ? 0.25f + 0.15f * derin : Math.min(0.85f, 0.4f + 0.06f * derin));
        int c = karistir(renk, 0xFFFFFF, kopuk);
        int ustIsik = isik;
        if (tur == 1) {
            float p = parlama(x + 0.5, UST[k], z + 0.5, t, 1);
            if (p > 0.02f) {
                c = karistir(c, gunesRengi, Math.min(1f, p));
                a = Math.max(a, Math.min(0.95f, p));
                ustIsik = parlakIsik(isik, p);
            }
        }
        float rx = x - yapOx;
        float rz = z - yapOz;
        if (tur == 3) {
            // Gerçek su: üstünü vanilla çizer; yalnız alçalmış sanal komşuya bakan "duvar" aşağıda çizilir.
        } else if (tur == 1) {
            dortgenUst(rx, rz, 1, yu + (float) dalgacik(x, z, t), yu + (float) dalgacik(x, z + 1, t),
                    yu + (float) dalgacik(x + 1, z + 1, t), yu + (float) dalgacik(x + 1, z, t), c, a, ustIsik);
        } else {
            dortgenUst(rx, rz, 1, yu, yu, yu, yu, c, a, isik);
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
