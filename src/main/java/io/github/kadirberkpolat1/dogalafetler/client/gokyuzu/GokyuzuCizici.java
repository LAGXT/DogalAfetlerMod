package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi.AyCarpmasiGokyuzu;
import io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi.IstemciAy;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KaraDelikYutmaEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarTitresimi;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Ortak gökyüzü çizicisi (madde 16): bütün evreler ve ay var/yok kombinasyonları tek yerden çizilir. Sıra: kubbe →
 * gün doğumu yelpazesi → derin uzay (Samanyolu, galaksi, nebula, kalıntı, yıldız) → güneş evresi → ay → ufuk altı
 * karanlık disk. Yıldızlar ve şerit kalite ya da yıldız yoğunluğu değişince bir kez derlenen VBO'lardandır. Süpernova
 * koreografisi (çökme → parlama → kabuk) evre başlangıcından beri geçen saniyeyle, uzun sönüm evre ilerlemesiyle çizilir.
 * Pulsarın titreşimi gerçek zamandan (kare süresinden bağımsız), kara delik yutması evre saniyesinden çizilir.
 */
final class GokyuzuCizici {
    private static final ResourceLocation GUNES_VANILLA =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/sun.png");
    private static final ResourceLocation AY_VANILLA =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/moon_phases.png");
    private static final ResourceLocation SAMANYOLU = doku("samanyolu");
    private static final ResourceLocation GUNES = doku("gunes");
    private static final ResourceLocation HALE = doku("hale");
    private static final ResourceLocation SN_ISIN = doku("supernova_isin");
    private static final ResourceLocation KARA_DELIK = doku("kara_delik");
    private static final ResourceLocation KARA_DELIK_AKIS = doku("kara_delik_akis");
    private static final ResourceLocation KARA_DELIK_JET = doku("kara_delik_jet");
    private static final ResourceLocation PULSAR_CEKIRDEK = doku("pulsar_cekirdek");
    private static final ResourceLocation PULSAR_HALE = doku("pulsar_hale");
    private static final ResourceLocation PULSAR_KORONA = doku("pulsar_korona");
    private static final ResourceLocation PULSAR_ISIN = doku("pulsar_isin_keskin");
    private static final ResourceLocation KARADELIK_DISK_AKIS = doku("karadelik_disk_akis");
    private static final ResourceLocation KARADELIK_MERCEK = doku("karadelik_mercek");
    private static final ResourceLocation KARADELIK_FOTON = doku("karadelik_foton");
    private static final ResourceLocation SPAGETTI = doku("spagetti");
    private static final ResourceLocation SN_HALKA = doku("supernova_halka");
    private static final ResourceLocation YILDIZ_ISIN = doku("yildiz_isin");

    /**
     * Vanilla güneş diskinin yarı boyu (100 birim uzaklıkta): {@code sun.png} 32 pikselin ortadaki 8'i parlak disk, kare 30
     * birim yarı boylu → 7,5. Kendi dokumuzda disk yarıçapı dokunun 0,42'si.
     */
    private static final float GUNES_DISK = 7.5f;
    private static final float DOKU_DISK_ORANI = 0.42f;

    private static final Vector3f SAMANYOLU_N;
    private static final Vector3f SAMANYOLU_E1;
    private static final Vector3f SAMANYOLU_E2;

    static {
        float egim = (float) Math.toRadians(20);
        float az = (float) Math.toRadians(30);
        SAMANYOLU_N = new Vector3f(Mth.cos(egim) * Mth.cos(az), Mth.sin(egim), Mth.cos(egim) * Mth.sin(az)).normalize();
        Vector3f y = new Vector3f(0, 1, 0);
        SAMANYOLU_E1 = new Vector3f(y).sub(new Vector3f(SAMANYOLU_N).mul(y.dot(SAMANYOLU_N))).normalize();
        SAMANYOLU_E2 = new Vector3f(SAMANYOLU_N).cross(SAMANYOLU_E1).normalize();
    }

    /** Gök küresinde sabit bir cisim (gece yüzü −Y, gündüz yüzü +Y kutbu çevresinde). */
    private record Cisim(int doku, float az, float el, float derece, float donus, float alfa) {
        Vector3f yon() {
            float a = (float) Math.toRadians(az);
            float e = (float) Math.toRadians(el);
            return new Vector3f(Mth.cos(e) * Mth.cos(a), Mth.sin(e), Mth.cos(e) * Mth.sin(a));
        }
    }

    // doku: 0 büyük sarmal galaksi, 1 küçük galaksi, 2 kırmızı nebula, 3 mavi nebula, 4 karışık nebula
    private static final Cisim[] CISIMLER = {
            new Cisim(0, 40, -62, 9f, 35f, 0.95f),
            new Cisim(1, 205, -52, 4f, 0f, 0.9f),
            new Cisim(2, 120, -72, 6.5f, 10f, 0.9f),
            new Cisim(3, 300, -60, 4.5f, 70f, 0.85f),
            new Cisim(4, 250, -78, 5.5f, 140f, 0.85f),
            new Cisim(0, 160, 64, 7f, -20f, 0.8f),
            new Cisim(4, 330, 66, 5f, 40f, 0.75f),
            new Cisim(1, 70, 58, 3.5f, 0f, 0.8f),
    };

    /**
     * Çok uzak, çok küçük kara delikler (gökyüzü-2; M87/EHT görünümü): açısal çapı {@code derece}, jet ve Doppler yönü
     * {@code donus}, akış dokusunun dönüşü {@code akis} °/sn. Kademeye göre ilk 1/2/3/4'ü çizilir.
     */
    private record KaraDelik(float az, float el, float derece, float donus, float akis, float alfa) {
        Vector3f yon() {
            float a = (float) Math.toRadians(az);
            float e = (float) Math.toRadians(el);
            return new Vector3f(Mth.cos(e) * Mth.cos(a), Mth.sin(e), Mth.cos(e) * Mth.sin(a));
        }
    }

    private static final KaraDelik[] KARA_DELIKLER = {
            new KaraDelik(75, -40, 1.6f, 25f, 14f, 1f),
            new KaraDelik(250, -32, 1.2f, -40f, -18f, 0.9f),
            new KaraDelik(165, -68, 1.0f, 70f, 22f, 0.85f),
            new KaraDelik(330, 55, 1.3f, 10f, -12f, 0.85f),
    };
    /** Derin uzayın gün döngüsünden bağımsız yavaş dönüşü: {@value #PRESESYON_SN} sn'de bir tur, eğik kutup ekseni. */
    private static final float PRESESYON_SN = 600f;
    private static final Vector3f KUTUP = new Vector3f(0.25f, 0.94f, 0.22f).normalize();
    /** Galaksilerin kendi dönüşü: saatte bir tur ({@code sn} 3600 sn'de sarılır, açı sürekli kalır). */
    private static final float GALAKSI_DONUS_SN = 3600f;
    /**
     * Pulsar kuadlarının yarı boyları (100 birim uzaklıkta; vanilla güneş diski ~8,6°): çekirdek 20° kuad → 10° disk, hale
     * 40° → ~34°, korona 64° → ≥ 52° görünür (doku oranları {@code pulsar_gok_dokulari.py}). Boy evre boyunca sabit.
     */
    private static final float CEKIRDEK_YARI = 17.6f;
    private static final float HALE_YARI = 36.4f;
    private static final float KORONA_YARI = 62.5f;
    /** Işın boyu teğet düzlemde: 900 birim ≈ 84°, gök kubbesini aşar; uzak köşeler küreye çekilir (uzak düzlem). */
    private static final float ISIN_BOY = 900f;
    private static final float[] ISIN_ACISI = {33f, -58f};
    private static final float[] PULSAR_SOGUK = {0.70f, 0.83f, 1f};
    private static final float[] PULSAR_SICAK = {0.86f, 0.74f, 1f};
    /** Teğet düzlemde 1°'nin birim karşılığı (100 birim uzaklık, küçük açı). */
    private static final float DERECE_BIRIM = 100f * Mth.DEG_TO_RAD;
    /** {@code karadelik_buyuk}: disk dış yarıçapı, foton halkası, disk basıklığı; {@code karadelik_foton} halka yarıçapı. */
    private static final float KD_DISK_DIS = 0.92f;
    private static final float KD_FOTON = 0.335f;
    private static final float KD_BASIKLIK = 0.26f;
    private static final float FOTON_DOKU_R = 0.84f;
    /** Akresyon diskinin gök düzlemindeki eğimi (derece). */
    private static final float KD_EGIM = -16f;
    /** Parlamada kara delik silüetinin rengi; Einstein halkasının disk dokusu yarıçapına oranı. */
    private static final float[] SILUET = {0.07f, 0.03f, 0.12f};
    private static final float EINSTEIN_HALKASI = 1.45f;
    /** Pulsar yolundaki kızıl cüce doğuşunun ışık yayı: doğu ufkunda ±72° (ufkun %40'ı). */
    private static final float DOGUS_YAYI_YARI = 72f;
    /** Titreşim ofsetleri; yalnız render thread'i yazar. */
    private static final float[] HIZLI = new float[2];
    private static final float[] YAVAS = new float[2];

    private static int karaDelikSayisi;
    private static int spagettiDilim;
    private static ResourceLocation karadelikBuyuk;

    private static final Vector3f TEPE = new Vector3f(0, 1, 0);
    private static EfektKalitesi kurulanKalite;
    private static double kurulanYogunluk;
    private static VertexBuffer kubbe;
    private static VertexBuffer karanlik;
    private static VertexBuffer samanyoluVbo;
    private static VertexBuffer parlakVbo;
    private static VertexBuffer[] yildizGruplari = new VertexBuffer[0];
    private static final ResourceLocation[] cisimDokulari = new ResourceLocation[5];
    private static ResourceLocation kalintiDokusu;
    private static boolean ilkCizimYazildi;
    private static long cizimSuresiNs;
    private static long cizimSayisi;

    private GokyuzuCizici() {
    }

    private static ResourceLocation doku(String ad) {
        return DogalAfetler.konum("textures/environment/gokyuzu/" + ad + ".png");
    }

    // ------------------------------------------------------------------ kurulum (kaliteye göre)

    private static void hazirla() {
        EfektKalitesi k = IstemciAyarlari.efektKalitesi();
        double yogunluk = GokyuzuAyarlari.yildizYogunlugu();
        if (k == kurulanKalite && yogunluk == kurulanYogunluk && kubbe != null) {
            return;
        }
        long bas = System.nanoTime();
        kapat();
        int yildiz;
        int dilim;
        int grup;
        int cisimBoy;
        int kucukBoy;
        int kalintiBoy;
        switch (k) {
            case DUSUK -> {
                yildiz = 2000; dilim = 24; grup = 1; cisimBoy = 128; kucukBoy = 64; kalintiBoy = 512; karaDelikSayisi = 1;
                spagettiDilim = 1;
            }
            case ORTA -> {
                yildiz = 4000; dilim = 48; grup = 3; cisimBoy = 256; kucukBoy = 128; kalintiBoy = 512; karaDelikSayisi = 2;
                spagettiDilim = 2;
            }
            case ULTRA -> {
                yildiz = 10000; dilim = 96; grup = 8; cisimBoy = 512; kucukBoy = 256; kalintiBoy = 2048; karaDelikSayisi = 4;
                spagettiDilim = 4;
            }
            default -> {
                yildiz = 6000; dilim = 64; grup = 6; cisimBoy = 256; kucukBoy = 128; kalintiBoy = 1024; karaDelikSayisi = 3;
                spagettiDilim = 3;
            }
        }
        karadelikBuyuk = doku("karadelik_buyuk_" + (k == EfektKalitesi.DUSUK || k == EfektKalitesi.ORTA ? 256 : 512));
        cisimDokulari[0] = doku("galaksi_sarmal_" + cisimBoy);
        cisimDokulari[1] = doku("galaksi_kucuk_" + kucukBoy);
        cisimDokulari[2] = doku("nebula_kirmizi_" + cisimBoy);
        cisimDokulari[3] = doku("nebula_mavi_" + cisimBoy);
        cisimDokulari[4] = doku("nebula_karisik_" + cisimBoy);
        kalintiDokusu = doku("kalinti_" + kalintiBoy);

        kubbe = disk(16f);
        karanlik = disk(-16f);
        samanyoluVbo = samanyoluSeridi(dilim);
        yildiz = Math.max(200, (int) Math.round(yildiz * yogunluk));
        int parlak = yildizlariKur(yildiz, grup);
        kurulanKalite = k;
        kurulanYogunluk = yogunluk;
        DogalAfetler.LOGGER.info("[gokyuzu] VBO'lar kuruldu: kalite={}, yildiz={}, parlak={}, samanyolu dilim={}, grup={}, "
                + "kalinti={}px, süre={} ms", k, yildiz, parlak, dilim, grup, kalintiBoy,
                (System.nanoTime() - bas) / 1_000_000);
    }

    private static void kapat() {
        for (VertexBuffer v : new VertexBuffer[] {kubbe, karanlik, samanyoluVbo, parlakVbo}) {
            if (v != null) {
                v.close();
            }
        }
        for (VertexBuffer v : yildizGruplari) {
            v.close();
        }
        kubbe = karanlik = samanyoluVbo = parlakVbo = null;
        yildizGruplari = new VertexBuffer[0];
    }

    private static VertexBuffer yukle(BufferBuilder.RenderedBuffer veri) {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vb.bind();
        vb.upload(veri);
        VertexBuffer.unbind();
        return vb;
    }

    /** Vanilla {@code buildSkyDisc} ile aynı disk (kubbe y=16, ufuk altı y=-16). */
    private static VertexBuffer disk(float y) {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        float r = Math.signum(y) * 512f;
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        bb.vertex(0, y, 0).endVertex();
        for (int i = -180; i <= 180; i += 45) {
            bb.vertex(r * Mth.cos(i * Mth.DEG_TO_RAD), y, 512f * Mth.sin(i * Mth.DEG_TO_RAD)).endVertex();
        }
        return yukle(bb.end());
    }

    private static Vector3f seritNoktasi(float teta, float enlem) {
        Vector3f c = new Vector3f(SAMANYOLU_E1).mul(Mth.cos(teta)).add(new Vector3f(SAMANYOLU_E2).mul(Mth.sin(teta)));
        return c.mul(Mth.cos(enlem)).add(new Vector3f(SAMANYOLU_N).mul(Mth.sin(enlem)));
    }

    /**
     * Gök küresinde eğik büyük çember boyunca 28° geniş şerit; u=0,5 galaksi merkezi (gece tepe noktasına yakın). Enine üç
     * bant: dış bantlarda köşe alfası kenarda 0'a iner (doku kenarındaki düşüşe ek; sert kenarlı şerit görünmesin).
     */
    private static VertexBuffer samanyoluSeridi(int dilim) {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float w = (float) Math.toRadians(14);
        float[] v = {0f, 0.3f, 0.7f, 1f};
        float[] alfa = {0f, 1f, 1f, 0f};
        for (int i = 0; i < dilim; i++) {
            float u0 = (float) i / dilim;
            float u1 = (float) (i + 1) / dilim;
            float t0 = u0 * Mth.TWO_PI;
            float t1 = u1 * Mth.TWO_PI;
            for (int bant = 0; bant < 3; bant++) {
                float e0 = Mth.lerp(v[bant], -w, w);
                float e1 = Mth.lerp(v[bant + 1], -w, w);
                Vector3f a = seritNoktasi(t0, e0).mul(100f);
                Vector3f b = seritNoktasi(t0, e1).mul(100f);
                Vector3f c = seritNoktasi(t1, e1).mul(100f);
                Vector3f d = seritNoktasi(t1, e0).mul(100f);
                bb.vertex(a.x, a.y, a.z).uv(u0, v[bant]).color(1f, 1f, 1f, alfa[bant]).endVertex();
                bb.vertex(b.x, b.y, b.z).uv(u0, v[bant + 1]).color(1f, 1f, 1f, alfa[bant + 1]).endVertex();
                bb.vertex(c.x, c.y, c.z).uv(u1, v[bant + 1]).color(1f, 1f, 1f, alfa[bant + 1]).endVertex();
                bb.vertex(d.x, d.y, d.z).uv(u1, v[bant]).color(1f, 1f, 1f, alfa[bant]).endVertex();
            }
        }
        return yukle(bb.end());
    }

    private record Yildiz(Vector3f yon, float yari, float aci, float r, float g, float b, float a) {
    }

    /** Yıldızları üretir ve gruplara bölünmüş VBO'lara yazar; en parlak %1 ayrıca ışınlı sprite olur. */
    private static int yildizlariKur(int adet, int grup) {
        Random r = new Random(20261105L);
        // Tayf sınıfları O/B, A, F, G, K, M, kızıl dev; eşikler birikimli olasılık.
        float[][] renkler = {GokyuzuEgrileri.rgb(0x9BB0FF), GokyuzuEgrileri.rgb(0xCAD7FF), GokyuzuEgrileri.rgb(0xF8F7FF),
                GokyuzuEgrileri.rgb(0xFFF4EA), GokyuzuEgrileri.rgb(0xFFD2A1), GokyuzuEgrileri.rgb(0xFFB56C),
                GokyuzuEgrileri.rgb(0xFF8A5C)};
        float[] esik = {0.06f, 0.18f, 0.40f, 0.65f, 0.85f, 0.97f, 1f};
        List<List<Yildiz>> gruplar = new ArrayList<>();
        for (int g = 0; g < grup; g++) {
            gruplar.add(new ArrayList<>());
        }
        List<Yildiz> parlaklar = new ArrayList<>();
        for (int i = 0; i < adet; i++) {
            Vector3f yon;
            if (r.nextFloat() < 0.35f) {
                float teta = r.nextFloat() * Mth.TWO_PI;
                float enlem = (float) Mth.clamp(r.nextGaussian() * Math.toRadians(7), -Math.toRadians(14),
                        Math.toRadians(14));
                yon = seritNoktasi(teta, enlem);
            } else {
                yon = new Vector3f((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian());
                if (yon.lengthSquared() < 1e-4f) {
                    continue;
                }
                yon.normalize();
            }
            float m = (float) -Math.log(1.0 - r.nextFloat());
            float yari = 0.10f + 0.28f * Math.min(1f, m / 4.5f);
            float alfa = Math.min(1f, 0.25f + m * 0.22f);
            float sec = r.nextFloat();
            int sinif = 0;
            while (sec >= esik[sinif] && sinif < esik.length - 1) {
                sinif++;
            }
            float[] c = renkler[sinif];
            float aci = r.nextFloat() * Mth.TWO_PI;
            gruplar.get(i % grup).add(new Yildiz(yon, yari, aci, c[0], c[1], c[2], alfa));
            if (m > 4.6f) {
                parlaklar.add(new Yildiz(yon, 1.1f + 0.35f * Math.min(3f, m - 4.6f), aci, c[0], c[1], c[2], 1f));
            }
        }
        yildizGruplari = new VertexBuffer[grup];
        for (int g = 0; g < grup; g++) {
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (Yildiz y : gruplar.get(g)) {
                yildizKuadi(bb, y, false);
            }
            yildizGruplari[g] = yukle(bb.end());
        }
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (Yildiz y : parlaklar) {
            yildizKuadi(bb, y, true);
        }
        parlakVbo = yukle(bb.end());
        return parlaklar.size();
    }

    /** Vanilla {@code drawStars} ile aynı yönelim: kuad küre merkezine bakar, kendi ekseninde rastgele döner. */
    private static void yildizKuadi(BufferBuilder bb, Yildiz y, boolean dokulu) {
        double x = y.yon.x;
        double yy = y.yon.y;
        double z = y.yon.z;
        double cx = x * 100;
        double cy = yy * 100;
        double cz = z * 100;
        double d8 = Math.atan2(x, z);
        double s8 = Math.sin(d8);
        double c8 = Math.cos(d8);
        double d11 = Math.atan2(Math.sqrt(x * x + z * z), yy);
        double s11 = Math.sin(d11);
        double c11 = Math.cos(d11);
        double s14 = Math.sin(y.aci);
        double c14 = Math.cos(y.aci);
        for (int j = 0; j < 4; j++) {
            double a = ((j & 2) - 1) * (double) y.yari;
            double b = ((j + 1 & 2) - 1) * (double) y.yari;
            double d21 = a * c14 - b * s14;
            double d22 = b * c14 + a * s14;
            double d23 = d21 * s11;
            double d24 = -d21 * c11;
            double d25 = d24 * s8 - d22 * c8;
            double d26 = d22 * s8 + d24 * c8;
            bb.vertex(cx + d25, cy + d23, cz + d26);
            if (dokulu) {
                bb.uv((j & 2) == 0 ? 0f : 1f, (j + 1 & 2) == 0 ? 0f : 1f);
            }
            bb.color(y.r, y.g, y.b, y.a).endVertex();
        }
    }

    // ------------------------------------------------------------------ çizim

    private static boolean gokGizli(Camera kamera) {
        FogType sivi = kamera.getFluidInCamera();
        if (sivi == FogType.POWDER_SNOW || sivi == FogType.LAVA) {
            return true;
        }
        return kamera.getEntity() instanceof LivingEntity l
                && (l.hasEffect(MobEffects.BLINDNESS) || l.hasEffect(MobEffects.DARKNESS));
    }

    /** Kanca (b) yolu: gökyüzünün tamamını biz çizeriz. */
    static void ciz(ClientLevel level, float pt, PoseStack poz, Camera kamera, Matrix4f izdusum, boolean sisli,
            Runnable sisKur) {
        long bas = System.nanoTime();
        sisKur.run();
        if (sisli || gokGizli(kamera)) {
            return;
        }
        hazirla();
        IstemciGokyuzu.Gorunum d = IstemciGokyuzu.gorunum(pt);
        float zaman = level.getTimeOfDay(pt);
        float gun = GokyuzuRenkleri.gunIsigi(zaman);
        float yagmursuz = 1f - level.getRainLevel(pt);
        float sn = ((level.getGameTime() % 72000L) + pt) / 20f;
        Vec3 v = level.getSkyColor(kamera.getPosition(), pt);
        float[] gok = GokyuzuRenkleri.gokRengi(d, new float[] {(float) v.x, (float) v.y, (float) v.z}, gun);
        if (!ilkCizimYazildi) {
            ilkCizimYazildi = true;
            DogalAfetler.LOGGER.info("[gokyuzu] ilk özel gökyüzü çizimi (kanca b, sarmalayıcı): {}", d);
        }

        RenderSystem.disableCull();
        FogRenderer.levelFogColor();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(gok[0], gok[1], gok[2], 1f);
        kubbe.bind();
        kubbe.drawWithShader(poz.last().pose(), izdusum, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        // Kubbe rengi shader rengine yazıldı; sıfırlanmazsa gündüz (uzay katmanı atlanınca) güneş gök rengiyle çarpılır.
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gunDogumu(level, poz, zaman, pt);

        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        dogusYayi(poz, d, yagmursuz);
        poz.pushPose();
        poz.mulPose(Axis.YP.rotationDegrees(-90f));
        poz.mulPose(Axis.XP.rotationDegrees(zaman * 360f));
        float uzay = GokyuzuRenkleri.uzayAlfa(d, zaman) * yagmursuz;
        if (uzay > 0.003f) {
            FogRenderer.setupNoFog();
            derinUzay(poz.last().pose(), izdusum, d, uzay, gun, sn);
            sisKur.run();
        }
        gunesEvresi(poz.last().pose(), d, yagmursuz, sn);
        if (d.ayVar() && !IstemciAy.ayiGizle()) {
            ay(poz.last().pose(), level, d, yagmursuz);
        }
        poz.popPose();
        AyCarpmasiGokyuzu.ciz(poz.last().pose(), level, pt, yagmursuz, sisKur);

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0f, 0f, 0f, 1f);
        Minecraft mc = Minecraft.getInstance();
        double ufuk = mc.player.getEyePosition(pt).y - level.getLevelData().getHorizonHeight(level);
        if (ufuk < 0.0) {
            poz.pushPose();
            poz.translate(0f, 12f, 0f);
            karanlik.bind();
            karanlik.drawWithShader(poz.last().pose(), izdusum, GameRenderer.getPositionShader());
            VertexBuffer.unbind();
            poz.popPose();
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        cizimSuresiNs += System.nanoTime() - bas;
        cizimSayisi++;
    }

    /** Kanca (b) çiziminin kare başına ortalama CPU süresini (µs) verir ve sayacı sıfırlar. GPU süresi dahil değil. */
    static double ortalamaCizimMikrosaniye() {
        double sonuc = cizimSayisi == 0 ? 0 : cizimSuresiNs / 1000.0 / cizimSayisi;
        cizimSuresiNs = 0;
        cizimSayisi = 0;
        return sonuc;
    }

    /** Kanca (c) yolu: vanilla gökyüzü çizildikten sonra yalnız derin uzay eklenir; güneş ve ay gizlenemez. */
    static void afterSkyCiz(PoseStack poz, Matrix4f izdusum, float pt, Camera kamera) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        IstemciGokyuzu.Gorunum d = IstemciGokyuzu.gorunum(pt);
        if (level == null || d.vanillaMi() || gokGizli(kamera)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        hazirla();
        float zaman = level.getTimeOfDay(pt);
        float sn = ((level.getGameTime() % 72000L) + pt) / 20f;
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        poz.pushPose();
        poz.mulPose(Axis.YP.rotationDegrees(-90f));
        poz.mulPose(Axis.XP.rotationDegrees(zaman * 360f));
        Matrix4f m = poz.last().pose();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        FogRenderer.setupNoFog();
        derinUzay(m, izdusum, d, GokyuzuRenkleri.uzayAlfa(d, zaman) * (1f - level.getRainLevel(pt)),
                GokyuzuRenkleri.gunIsigi(zaman), sn);
        poz.popPose();
        AyCarpmasiGokyuzu.ciz(poz.last().pose(), level, pt, 1f - level.getRainLevel(pt), () -> { });
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void gunDogumu(ClientLevel level, PoseStack poz, float zaman, float pt) {
        float[] renk = level.effects().getSunriseColor(zaman, pt);
        if (renk == null) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        poz.pushPose();
        poz.mulPose(Axis.XP.rotationDegrees(90f));
        poz.mulPose(Axis.ZP.rotationDegrees(Mth.sin(level.getSunAngle(pt)) < 0f ? 180f : 0f));
        poz.mulPose(Axis.ZP.rotationDegrees(90f));
        Matrix4f m = poz.last().pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(m, 0f, 100f, 0f).color(renk[0], renk[1], renk[2], renk[3]).endVertex();
        for (int j = 0; j <= 16; j++) {
            float a = j * Mth.TWO_PI / 16f;
            float s = Mth.sin(a);
            float c = Mth.cos(a);
            bb.vertex(m, s * 120f, c * 120f, -c * 40f * renk[3]).color(renk[0], renk[1], renk[2], 0f).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
        poz.popPose();
    }

    /** Samanyolu gündüz yarı parlaklıkta: güneşsiz/kızıl cüce gündüzünde göğe yapışık bir şerit gibi öne çıkmasın. */
    private static final float SAMANYOLU_GUNDUZ = 0.5f;

    /** Derin uzayın yavaş dönüş açısı (derece); {@code sn} 3600 sn'de sarılır, 600 sn'lik tur sürekli kalır. */
    private static float presesyonDerecesi(float sn) {
        return sn % PRESESYON_SN / PRESESYON_SN * 360f;
    }

    private static Matrix4f presesyon(Matrix4f m, float sn) {
        return new Matrix4f(m).rotate(new Quaternionf().rotationAxis((float) Math.toRadians(presesyonDerecesi(sn)), KUTUP));
    }

    /**
     * Kara deliğin dünya yönü (görsel otomasyonun yakın kadrajı için): çizimdeki gün dönüşü ve yavaş dönüşle aynı.
     */
    static Vector3f karaDelikYonu(int i, float zaman, float sn) {
        return new Quaternionf(Axis.YP.rotationDegrees(-90f)).mul(Axis.XP.rotationDegrees(zaman * 360f))
                .mul(new Quaternionf().rotationAxis((float) Math.toRadians(presesyonDerecesi(sn)), KUTUP))
                .transform(KARA_DELIKLER[Math.floorMod(i, KARA_DELIKLER.length)].yon());
    }

    private static void derinUzay(Matrix4f gunluk, Matrix4f izdusum, IstemciGokyuzu.Gorunum d, float uzay, float gun,
            float sn) {
        Matrix4f m = presesyon(gunluk, sn);
        boolean zengin = GokyuzuRenkleri.zengin(d);
        if (zengin && GokyuzuAyarlari.nebulaKatmani()) {
            RenderSystem.setShaderTexture(0, SAMANYOLU);
            RenderSystem.setShaderColor(1f, 1f, 1f, uzay * 0.6f * Mth.lerp(gun, 1f, SAMANYOLU_GUNDUZ));
            samanyoluVbo.bind();
            samanyoluVbo.drawWithShader(m, izdusum, GameRenderer.getPositionTexColorShader());
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            for (int i = 0; i < CISIMLER.length; i++) {
                Cisim c = CISIMLER[i];
                float yari = 100f * (float) Math.tan(Math.toRadians(c.derece) / 2);
                float donus = c.doku <= 1 ? (i % 2 == 0 ? 1f : -1f) * sn / GALAKSI_DONUS_SN * 360f : 0f;
                kuad(m, cisimDokulari[c.doku], c.yon(), yari, c.donus + donus, 1f, 1f, 1f, c.alfa * uzay);
            }
            karaDelikler(m, uzay, sn);
        }
        float kalinti = GokyuzuRenkleri.kalintiAlfa(d);
        if (kalinti > 0f) {
            float genisleme = d.evre() == GunesEvresi.GUNESSIZ ? GokyuzuEgrileri.kalintiGenisleme(d.ilerleme())
                    : d.evre() == GunesEvresi.KIZIL_CUCE || d.evre() == GunesEvresi.PULSAR
                    || d.evre() == GunesEvresi.KARADELIK_YUTMA ? GokyuzuEgrileri.kalintiGenisleme(1f) : 1f;
            float yari = 100f * (float) Math.tan(Math.toRadians(20)) * genisleme;
            kuad(gunluk, kalintiDokusu, TEPE, yari, 15f, 1f, 1f, 1f, kalinti * Math.max(uzay, 0.35f));
            if (d.evre() != GunesEvresi.PULSAR && d.evre() != GunesEvresi.KARADELIK_YUTMA) {
                // Ortadaki sönük pulsar noktası (0,5 Hz); pulsar ve yutma döneminde onun yerine asıl pulsar çizilir.
                float nabiz = 0.55f + 0.45f * Mth.sin(sn * Mth.TWO_PI * 0.5f);
                kuad(gunluk, YILDIZ_ISIN, TEPE, 2.2f, 45f, 0.7f, 0.85f, 1f, kalinti * nabiz);
            }
        }
        float carpan = zengin ? 1f : 0.6f;
        float[] frekans = {1.1f, 1.5f, 1.9f, 2.3f, 0.7f, 2.9f, 1.3f, 3.4f};
        float[] faz = {0f, 1.7f, 3.1f, 4.4f, 0.9f, 2.4f, 5.2f, 3.8f};
        for (int g = 0; g < yildizGruplari.length; g++) {
            // Kırpışma: iki frekanslı çarpım (±%32) ve hafif renk kayması (atmosfer tayf saçılması hissi).
            float s1 = Mth.sin(sn * Mth.TWO_PI * frekans[g] + faz[g]);
            float s2 = Mth.sin(sn * Mth.TWO_PI * frekans[g] * 0.37f + faz[g] * 1.9f);
            float titreme = yildizGruplari.length > 1 ? 1f + 0.32f * s1 * (0.55f + 0.45f * s2) : 1f;
            float renkKaymasi = yildizGruplari.length > 1 ? 0.07f * s2 : 0f;
            RenderSystem.setShaderColor(1f + renkKaymasi, 1f, 1f - renkKaymasi, Math.min(1f, uzay * carpan * titreme));
            yildizGruplari[g].bind();
            yildizGruplari[g].drawWithShader(m, izdusum, GameRenderer.getPositionColorShader());
            VertexBuffer.unbind();
        }
        RenderSystem.setShaderTexture(0, YILDIZ_ISIN);
        RenderSystem.setShaderColor(1f, 1f, 1f, uzay * carpan);
        parlakVbo.bind();
        parlakVbo.drawWithShader(m, izdusum, GameRenderer.getPositionTexColorShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void gunesEvresi(Matrix4f m, IstemciGokyuzu.Gorunum d, float yagmursuz, float sn) {
        if (d.onceki() == GunesEvresi.SUPERNOVA && d.gecis() < 1f) {
            supernova(m, 1f, 1e6f, yagmursuz * (1f - d.gecis()), sn);
        }
        float p = d.ilerleme();
        switch (d.evre()) {
            case NORMAL -> {
                // Pulsar yolunun sonunda kızıl cüce 20 sn'de söner, eski güneş belirir.
                float donus = d.onceki() == GunesEvresi.KIZIL_CUCE ? d.gecis() : 1f;
                if (donus < 1f) {
                    kizilCuce(m, d, 1f, yagmursuz * (1f - donus), sn);
                }
                kuad(m, GUNES_VANILLA, TEPE, 30f, 0f, 1f, 1f, 1f, yagmursuz * donus);
            }
            case BUYUYEN -> buyuyen(m, GokyuzuEgrileri.buyuyenOlcek(p), GokyuzuEgrileri.buyuyenRenk(p), p, yagmursuz, sn);
            case SUPERNOVA -> {
                float s = d.saniye();
                float cokme = GokyuzuEgrileri.SN_COKME_SN;
                if (s < cokme + 1f) {
                    // Çöken şişmiş güneş: turuncudan mavi-beyaza döner, küçülür, parlamayla kaybolur.
                    float c = GokyuzuEgrileri.yumusak(0f, cokme, s);
                    float[] r = GokyuzuEgrileri.karistir(GokyuzuEgrileri.buyuyenRenk(1f), new float[] {0.9f, 0.95f, 1f}, c);
                    float sonum = 1f - GokyuzuEgrileri.yumusak(cokme, cokme + 1f, s);
                    buyuyen(m, GokyuzuEgrileri.supernovaCekirdekOlcek(s), r, 1f - c, yagmursuz * sonum, sn);
                }
                supernova(m, p, s, yagmursuz, sn);
            }
            case GUNESSIZ -> {
            }
            case PULSAR -> pulsar(m, TEPE, PulsarEgrileri.yogunluk(p), yagmursuz, 1f, null, 0f, 1f);
            case KARADELIK_YUTMA -> karaDelikYutma(m, d.saniye(), yagmursuz, sn);
            case KIZIL_CUCE -> kizilCuce(m, d, p, yagmursuz, sn);
        }
    }

    private static void kizilCuce(Matrix4f m, IstemciGokyuzu.Gorunum d, float p, float a, float sn) {
        float disk = GUNES_DISK * d.boyutOrani();
        float dogus = 1f - p;
        float da = GokyuzuEgrileri.kizilCuceDiskAlfa(p) * a;
        kuad(m, HALE, TEPE, disk * (5f + 3f * dogus), 0f, 1f, 0.3f, 0.15f, (0.4f + 0.3f * dogus) * a);
        kuadOrtu(m, GUNES, TEPE, disk / DOKU_DISK_ORANI, sn * 2f, 1f, 0.416f, 0.227f, da);
        kuad(m, GUNES, TEPE, disk / DOKU_DISK_ORANI, sn * 2f, 1f, 0.416f, 0.227f, (0.25f + 0.6f * dogus) * da);
        kuad(m, HALE, TEPE, disk * 1.8f, 0f, 1f, 0.5f, 0.28f, 0.5f * dogus * da);
    }

    /**
     * Pulsar yolundaki kızıl cüce doğuşu: doğu ufkunda ufkun %40'ını (±{@value #DOGUS_YAYI_YARI}°) saran kızıl-turuncu
     * ışık yayı; dünya çerçevesinde, toplamalı, tek çizim. Normal süpernova yolunun doğuşu değişmez.
     */
    private static void dogusYayi(PoseStack poz, IstemciGokyuzu.Gorunum d, float yagmursuz) {
        if (d.evre() != GunesEvresi.KIZIL_CUCE || !d.pulsarYolu() || d.ilerleme() >= 1f) {
            return;
        }
        float a = 0.85f * (float) Math.pow(1f - d.ilerleme(), 0.6) * yagmursuz;
        if (a <= 0.003f) {
            return;
        }
        int n = 24;
        Matrix4f m = poz.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < 4; k++) {
                int j = k == 1 || k == 2 ? i + 1 : i;
                boolean ust = k >= 2;
                float f = (float) j / n * 2f - 1f;
                float w = 1f - f * f;
                float az = f * DOGUS_YAYI_YARI * Mth.DEG_TO_RAD;
                float el = (ust ? 3f + 15f * w : -3f) * Mth.DEG_TO_RAD;
                float yatay = 100f * Mth.cos(el);
                bb.vertex(m, Mth.cos(az) * yatay, 100f * Mth.sin(el), Mth.sin(az) * yatay)
                        .color(1f, 0.45f, 0.2f, ust ? 0f : a * (0.25f + 0.75f * w)).endVertex();
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /** Şişen güneş: disk normal karışım + %12 toplamalı (parlak gökte sarı-beyaza doymasın), etrafında turuncu hale. */
    private static void buyuyen(Matrix4f m, float olcek, float[] c, float haleP, float a, float sn) {
        float disk = GUNES_DISK * olcek;
        kuad(m, HALE, TEPE, disk * (3f + 3f * haleP), 0f, c[0], c[1] * 0.8f, c[2] * 0.55f, (0.2f + 0.3f * haleP) * a);
        kuadOrtu(m, GUNES, TEPE, disk / DOKU_DISK_ORANI, sn * 2f, c[0], c[1], c[2], a);
        kuad(m, GUNES, TEPE, disk / DOKU_DISK_ORANI, sn * 2f, c[0], c[1], c[2], 0.12f * a);
    }

    /** Süpernova: çekirdek hale, 12 ışın, genişleyen kabuk; Düşük kalitede büyük toplamalı kareler küçülür (G6). */
    private static void supernova(Matrix4f m, float p, float s, float a, float sn) {
        float cokme = GokyuzuEgrileri.SN_COKME_SN;
        float acilis = GokyuzuEgrileri.yumusak(cokme - 0.2f, cokme + 0.6f, s);
        if (acilis <= 0f) {
            return;
        }
        float ga = a * acilis;
        float f = GokyuzuEgrileri.supernovaFlas(s);
        float bp = GokyuzuEgrileri.supernovaParlaklik(p);
        float k = IstemciAyarlari.efektKalitesi() == EfektKalitesi.DUSUK ? 0.6f : 1f;
        float sicak = GokyuzuEgrileri.yumusak(0.05f, 0.6f, p);
        kuad(m, HALE, TEPE, (40f + 50f * f) * k, 0f, 0.8f, 0.88f, 1f, (0.3f * bp + 0.35f * f) * ga);
        kuad(m, SN_HALKA, TEPE, GokyuzuEgrileri.supernovaHalka(s, p), sn * 0.5f, Mth.lerp(sicak, 0.85f, 1f),
                Mth.lerp(sicak, 0.9f, 0.93f), Mth.lerp(sicak, 1f, 0.78f), (0.35f + 0.65f * bp) * ga);
        kuad(m, SN_ISIN, TEPE, (30f + 70f * f + 25f * bp) * k, sn * 1.5f, 0.85f, 0.92f, 1f,
                Math.min(1f, 0.35f + 0.6f * bp + 0.4f * f) * ga);
        kuad(m, HALE, TEPE, 4f + 6f * bp + 8f * f, 0f, 1f, 1f, 1f, ga);
    }

    private static void ay(Matrix4f m, ClientLevel level, IstemciGokyuzu.Gorunum d, float yagmursuz) {
        float r = 1f;
        float g = 1f;
        float b = 1f;
        int evre = level.getMoonPhase();
        switch (d.evre()) {
            case SUPERNOVA, PULSAR -> {
                r = 0.75f;
                g = 0.85f;
            }
            case KARADELIK_YUTMA -> {
                r = 0.7f;
                g = 0.62f;
                b = 0.9f;
            }
            case GUNESSIZ -> {
                // Güneş yok: hilal yerine yalnız kalıntı ışığıyla soluk gri dolunay diski (#5A6070), evre döngüsü durur.
                r = 0.353f;
                g = 0.376f;
                b = 0.439f;
                evre = 0;
            }
            case KIZIL_CUCE -> {
                g = 0.72f;
                b = 0.55f;
            }
            default -> {
            }
        }
        float u0 = (evre % 4) / 4f;
        float v0 = (evre / 4 % 2) / 2f;
        float u1 = (evre % 4 + 1) / 4f;
        float v1 = (evre / 4 % 2 + 1) / 2f;
        float s = 20f;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, AY_VANILLA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.vertex(m, -s, -100f, s).uv(u1, v1).color(r, g, b, yagmursuz).endVertex();
        bb.vertex(m, s, -100f, s).uv(u0, v1).color(r, g, b, yagmursuz).endVertex();
        bb.vertex(m, s, -100f, -s).uv(u0, v0).color(r, g, b, yagmursuz).endVertex();
        bb.vertex(m, -s, -100f, -s).uv(u1, v0).color(r, g, b, yagmursuz).endVertex();
        BufferUploader.drawWithShader(bb.end());
    }

    /** {@link #kuad} ama normal alfa karışımıyla (örten disk); sonra toplamalı karışıma döner. */
    private static void kuadOrtu(Matrix4f m, ResourceLocation doku, Vector3f yon, float yari, float donus, float r,
            float g, float b, float a) {
        RenderSystem.defaultBlendFunc();
        kuad(m, doku, yon, yari, donus, r, g, b, a);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    private static float titresimAyari() {
        return (float) GokyuzuAyarlari.pulsarTitresim();
    }

    /** Güneş yolunda, güneşin (pulsarın) yerinden {@code aci} derece ötedeki yön (gün dönüşü çerçevesinde). */
    private static Vector3f gunesYolu(float aci) {
        float r = aci * Mth.DEG_TO_RAD;
        return new Vector3f(0f, Mth.cos(r), Mth.sin(r));
    }

    /** {@link #gunesYolu} yönünün bu karedeki dünya yönü (görsel otomasyonun kadrajı için). */
    static Vector3f gunesYoluYonu(float aci, float zaman) {
        return new Quaternionf(Axis.YP.rotationDegrees(-90f)).mul(Axis.XP.rotationDegrees(zaman * 360f))
                .transform(gunesYolu(aci));
    }

    /** {@code yon}'a teğet birim eksenler ({@link #kuadDik} ile aynı seçim). */
    private static void teget(Vector3f yon, Vector3f u, Vector3f v) {
        u.set(Math.abs(yon.y) > 0.9f ? 1f : 0f, Math.abs(yon.y) > 0.9f ? 0f : 1f, 0f).cross(yon).normalize();
        v.set(yon).cross(u).normalize();
    }

    /** {@code yon}'u teğet eksenlerde ({@code du}, {@code dv}) derece kaydırır. */
    private static Vector3f kaydir(Vector3f yon, float du, float dv) {
        Vector3f u = new Vector3f();
        Vector3f v = new Vector3f();
        teget(yon, u, v);
        return new Vector3f(yon).mul(100f).add(u.mul(du * DERECE_BIRIM)).add(v.mul(dv * DERECE_BIRIM)).normalize();
    }

    /** {@code hedef}'in {@code yon}'un teğet düzlemindeki açısı (derece; {@link #kuadDik}'in {@code donus}'u). */
    private static float yonAcisi(Vector3f yon, Vector3f hedef) {
        Vector3f u = new Vector3f();
        Vector3f v = new Vector3f();
        teget(yon, u, v);
        return (float) Math.toDegrees(Math.atan2(hedef.dot(v), hedef.dot(u)));
    }

    /**
     * Titreşen pulsar (pulsar-2, PL-2): dönme yok. Korona yavaş sarsıntıyla, çekirdek ve keskin iki yönlü ışınlar yavaş +
     * hızlı titreşimle savrulur; hale hızlı bileşenin zıt iki kopyasıdır (%50 alfa, bulanık çift görüntü; Düşük'te tek
     * kopya). Çatırtı anlarında beyaz patlama (hızlı bileşen gibi {@code pulsarTitresim} ile ölçeklenir). Renk ısınmayla mavi-beyazdan mor-beyaza. {@code hedef} verilirse pulsar ona
     * doğru uzar ({@code spagetti} 0–1). Işın çifti Düşük'te bir, diğerlerinde iki (ikincisinin alfası {@code ikinciIsin}).
     * Kuad: korona 1, hale 1–2, ışın 2–4, çekirdek 1, çatırtı 1.
     */
    private static void pulsar(Matrix4f m, Vector3f yon, float guc, float a, float carpan, @Nullable Vector3f hedef,
            float spagetti, float ikinciIsin) {
        if (a <= 0.002f) {
            return;
        }
        double t = Util.getNanos() * 1e-9;
        boolean dusuk = kurulanKalite == EfektKalitesi.DUSUK;
        float ayar = titresimAyari();
        PulsarTitresimi.yavasOfset(t, guc, carpan, YAVAS);
        PulsarTitresimi.hizliOfset(t, guc, ayar, dusuk, carpan, HIZLI);
        float catirti = PulsarTitresimi.catirti(t) * ayar;
        float[] c = GokyuzuEgrileri.karistir(PULSAR_SOGUK, PULSAR_SICAK, guc);
        Vector3f merkez = kaydir(yon, YAVAS[0], YAVAS[1]);
        Vector3f titrek = kaydir(yon, YAVAS[0] + HIZLI[0], YAVAS[1] + HIZLI[1]);
        float egim = hedef != null && spagetti > 0f ? yonAcisi(yon, hedef) : 0f;
        float uzun = 1f + 1.3f * spagetti;
        float ince = 1f - 0.35f * spagetti;
        float isik = 0.8f + 0.2f * guc;

        kuadDik(m, PULSAR_KORONA, merkez, KORONA_YARI * (1f + 0.5f * spagetti), KORONA_YARI * ince, egim, c[0], c[1],
                c[2], a * (0.45f + 0.35f * guc) * (1f - 0.6f * spagetti));
        if (dusuk) {
            kuadDik(m, PULSAR_HALE, titrek, HALE_YARI * uzun, HALE_YARI * ince, egim, c[0], c[1], c[2], a * 0.8f * isik);
        } else {
            Vector3f zit = kaydir(yon, YAVAS[0] - HIZLI[0], YAVAS[1] - HIZLI[1]);
            kuadDik(m, PULSAR_HALE, titrek, HALE_YARI * uzun, HALE_YARI * ince, egim, c[0], c[1], c[2], a * 0.5f * isik);
            kuadDik(m, PULSAR_HALE, zit, HALE_YARI * uzun, HALE_YARI * ince, egim, c[0], c[1], c[2], a * 0.5f * isik);
        }
        float isinA = a * (0.7f + 0.3f * guc) * (1f + 0.6f * catirti) * (1f - 0.5f * spagetti);
        seritCift(m, PULSAR_ISIN, titrek, ISIN_ACISI[0], ISIN_BOY, 1.7f, c[0], c[1], c[2], isinA * 0.95f);
        if (!dusuk && ikinciIsin > 0f) {
            seritCift(m, PULSAR_ISIN, titrek, ISIN_ACISI[1], ISIN_BOY * 0.7f, 1.1f, c[0], c[1], c[2],
                    isinA * 0.6f * ikinciIsin);
        }
        kuadDik(m, PULSAR_CEKIRDEK, titrek, CEKIRDEK_YARI * uzun, CEKIRDEK_YARI * ince, egim, Mth.lerp(0.35f, 1f, c[0]),
                Mth.lerp(0.35f, 1f, c[1]), Mth.lerp(0.35f, 1f, c[2]), a);
        if (catirti > 0.004f) {
            kuad(m, HALE, titrek, CEKIRDEK_YARI * 2.4f, 0f, 1f, 1f, 1f, a * catirti * 0.9f);
        }
    }

    /**
     * Kara delik yutması (PL-4): güneş yolunda pulsardan {@link KaraDelikYutmaEgrileri#KARADELIK_ACI} ötede yaklaşan kara
     * delik (mercek ve gölge normal karışım; akış ve foton halkası toplamalı, Düşük'te yok), çekilip spagettileşen pulsar
     * ve parlama. Kuad Ultra'da en çok ~16.
     */
    private static void karaDelikYutma(Matrix4f m, float s, float yagmursuz, float sn) {
        boolean sade = kurulanKalite == EfektKalitesi.DUSUK;
        Vector3f delik = gunesYolu(KaraDelikYutmaEgrileri.KARADELIK_ACI);
        float cap = KaraDelikYutmaEgrileri.karaDelikCapi(s);
        float yari = 100f * (float) Math.tan(Math.toRadians(cap) / 2) / KD_DISK_DIS;
        float kd = KaraDelikYutmaEgrileri.karaDelikAlfa(s) * yagmursuz;
        RenderSystem.defaultBlendFunc();
        kuad(m, KARADELIK_MERCEK, delik, yari * 2.2f, KD_EGIM, 1f, 1f, 1f,
                KaraDelikYutmaEgrileri.merceklenme(s) * yagmursuz);
        kuad(m, karadelikBuyuk, delik, yari, KD_EGIM, 1f, 1f, 1f, kd);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        if (!sade) {
            float mor = KaraDelikYutmaEgrileri.kalintiMorlugu(s);
            kuadUv(m, KARADELIK_DISK_AKIS, delik, yari, yari * KD_BASIKLIK, KD_EGIM,
                    sn * KaraDelikYutmaEgrileri.diskDonusHizi(s), Mth.lerp(mor, 0.85f, 0.72f),
                    Mth.lerp(mor, 0.8f, 0.42f), 1f, 0.8f * KaraDelikYutmaEgrileri.diskParlakligi(s) * yagmursuz);
            kuad(m, KARADELIK_FOTON, delik, yari * KD_FOTON / FOTON_DOKU_R, KD_EGIM, 0.92f, 0.88f, 1f,
                    KaraDelikYutmaEgrileri.fotonHalkasi(s) * yagmursuz);
            mercekYaylari(m, delik, yari, s, sn, yagmursuz);
        }
        float pa = KaraDelikYutmaEgrileri.pulsarAlfa(s) * yagmursuz;
        if (pa > 0.002f) {
            Vector3f yon = gunesYolu(KaraDelikYutmaEgrileri.KARADELIK_ACI * KaraDelikYutmaEgrileri.cekim(s));
            float spagetti = KaraDelikYutmaEgrileri.spagetti(s);
            if (spagetti > 0.01f) {
                spagettiKuyrugu(m, yon, delik, spagetti, pa);
            }
            // İkinci ışın çifti kuyruk başlamadan söner: kare başına kuad ~16'yı aşmasın.
            float ikinci = 1f - GokyuzuEgrileri.yumusak(KaraDelikYutmaEgrileri.YAKLASMA_SN - 3f,
                    KaraDelikYutmaEgrileri.YAKLASMA_SN, s);
            pulsar(m, yon, 1f, pa, KaraDelikYutmaEgrileri.titresimCarpani(s), delik, spagetti, ikinci);
        }
        float parlama = KaraDelikYutmaEgrileri.parlama(s);
        if (parlama > 0.003f) {
            kuad(m, HALE, delik, 85f, 0f, 0.92f, 0.84f, 1f, parlama);
            siluet(m, delik, yari, parlama * kd);
        }
    }

    /**
     * Parlamanın altında kara deliğin koyu silüeti (normal karışım, parlak halenin üstünde): iki kutup jeti,
     * akresyon diski ve gölge, foton halkası ve dıştaki Einstein halkası okunur kalır.
     */
    private static void siluet(Matrix4f m, Vector3f delik, float yari, float a) {
        float r = SILUET[0];
        float g = SILUET[1];
        float b = SILUET[2];
        RenderSystem.defaultBlendFunc();
        kuadDik(m, KARA_DELIK_JET, delik, yari * 0.2f, yari * 3.4f, KD_EGIM, r, g, b, 0.85f * a);
        kuad(m, karadelikBuyuk, delik, yari, KD_EGIM, r, g, b, 0.95f * a);
        kuad(m, KARADELIK_FOTON, delik, yari * KD_FOTON / FOTON_DOKU_R, KD_EGIM, r, g, b, 0.9f * a);
        kuad(m, KARADELIK_FOTON, delik, yari * EINSTEIN_HALKASI / FOTON_DOKU_R, KD_EGIM, r, g, b, 0.8f * a);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    /** Kara deliğin arkasındaki yıldızların merceklenmiş yayları: diskin dışında teğet soluk şeritler (toplamalı). */
    private static void mercekYaylari(Matrix4f m, Vector3f delik, float yari, float s, float sn, float yagmursuz) {
        float a = KaraDelikYutmaEgrileri.mercekYaylari(s) * yagmursuz;
        if (a <= 0.003f) {
            return;
        }
        int n = kurulanKalite == EfektKalitesi.ORTA ? 4 : kurulanKalite == EfektKalitesi.ULTRA ? 8 : 6;
        float disk = yari * KD_DISK_DIS;
        for (int i = 0; i < n; i++) {
            float aci = i * 360f / n + (i % 2) * 11f + sn * 3f;
            float uzak = disk * (1.25f + 0.09f * (i * 7 % 5));
            float ar = aci * Mth.DEG_TO_RAD;
            Vector3f yon = kaydir(delik, Mth.cos(ar) * uzak / DERECE_BIRIM, Mth.sin(ar) * uzak / DERECE_BIRIM);
            kuadDik(m, HALE, yon, uzak * 0.42f, uzak * 0.035f, aci + 90f, 0.82f, 0.88f, 1f,
                    a * (0.55f + 0.15f * (i * 3 % 4)));
        }
    }

    /**
     * Pulsardan kara deliğe uzanan eğik kuyruk: büyük çember yayına yanal şişkinlik eklenmiş çoklu çizgi (kademeye göre
     * 1/2/3/4 dilim, tek çizim), pulsar ucunda mor-beyaz ve geniş, diske doğru mavi ve ince.
     */
    private static void spagettiKuyrugu(Matrix4f m, Vector3f pulsar, Vector3f delik, float spagetti, float a) {
        int n = spagettiDilim;
        Vector3f dik = new Vector3f(pulsar).cross(delik);
        if (dik.lengthSquared() < 1e-8f) {
            return;
        }
        dik.normalize();
        float kiris = pulsar.distance(delik);
        Vector3f[] nokta = new Vector3f[n + 1];
        for (int i = 0; i <= n; i++) {
            float t = (float) i / n * spagetti * 0.92f;
            nokta[i] = new Vector3f(pulsar).lerp(delik, t).add(new Vector3f(dik).mul(0.16f * kiris * Mth.sin(Mth.PI * t)))
                    .normalize().mul(100f);
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SPAGETTI);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < 4; j++) {
                int k = j == 0 || j == 3 ? i : i + 1;
                float t = (float) k / n;
                Vector3f ileri = new Vector3f(nokta[Math.min(n, k + 1)]).sub(nokta[Math.max(0, k - 1)]);
                Vector3f yan = new Vector3f(nokta[k]).cross(ileri).normalize()
                        .mul(Mth.lerp(t, CEKIRDEK_YARI * 0.7f, 1.4f) * (j < 2 ? -1f : 1f));
                float ka = a * spagetti * (0.95f - 0.35f * t);
                bb.vertex(m, nokta[k].x + yan.x, nokta[k].y + yan.y, nokta[k].z + yan.z).uv(t, j < 2 ? 0f : 1f)
                        .color(Mth.lerp(t, 0.9f, 0.55f), Mth.lerp(t, 0.8f, 0.7f), 1f, ka).endVertex();
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /**
     * {@code merkez}'in teğet düzleminde {@code aci} yönünde ve tersinde {@code boy} uzunlukta iki şerit (tek çizim; doku u:
     * merkez 0 → uç 1). Köşeler kamera yönünde 100 birime çekilir: ekrandaki yeri aynı kalır, uzak düzlemi aşmaz.
     */
    private static void seritCift(Matrix4f m, ResourceLocation doku, Vector3f merkez, float aci, float boy, float yari,
            float r, float g, float b, float a) {
        if (a <= 0.002f) {
            return;
        }
        Vector3f u = new Vector3f();
        Vector3f v = new Vector3f();
        teget(merkez, u, v);
        float ar = aci * Mth.DEG_TO_RAD;
        Vector3f yon = u.mul(Mth.cos(ar)).add(v.mul(Mth.sin(ar)));
        Vector3f yan = new Vector3f(merkez).cross(yon).normalize().mul(yari);
        Vector3f c = new Vector3f(merkez).mul(100f);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, doku);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int k = 0; k < 2; k++) {
            Vector3f uc = new Vector3f(yon).mul(k == 0 ? boy : -boy).add(c);
            kuyrukKosesi(bb, m, new Vector3f(c).sub(yan), 0f, 0f, r, g, b, a);
            kuyrukKosesi(bb, m, new Vector3f(uc).sub(yan), 1f, 0f, r, g, b, a);
            kuyrukKosesi(bb, m, new Vector3f(uc).add(yan), 1f, 1f, r, g, b, a);
            kuyrukKosesi(bb, m, new Vector3f(c).add(yan), 0f, 1f, r, g, b, a);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void kuyrukKosesi(BufferBuilder bb, Matrix4f m, Vector3f p, float u, float v, float r, float g,
            float b, float a) {
        float olcek = Math.min(1f, 100f / p.length());
        bb.vertex(m, p.x * olcek, p.y * olcek, p.z * olcek).uv(u, v).color(r, g, b, a).endVertex();
    }

    /**
     * Kara delikler: jet (toplamalı, ince ve soluk, Düşük'te yok), yıldızları örten gölge + akresyon halkası (normal
     * karışım) ve yavaş dönen akış dokusu (Düşük'te yok).
     */
    private static void karaDelikler(Matrix4f m, float uzay, float sn) {
        boolean sade = kurulanKalite == EfektKalitesi.DUSUK;
        for (int i = 0; i < karaDelikSayisi; i++) {
            KaraDelik k = KARA_DELIKLER[i];
            Vector3f yon = k.yon();
            float yari = 100f * (float) Math.tan(Math.toRadians(k.derece) / 2);
            float a = uzay * k.alfa;
            if (!sade) {
                float titresim = 0.8f + 0.2f * Mth.sin(sn * 1.3f + i * 2.1f);
                kuadDik(m, KARA_DELIK_JET, yon, yari * 0.22f, yari * 6.5f, k.donus, 0.72f, 0.84f, 1f, 0.5f * a * titresim);
            }
            kuadOrtu(m, KARA_DELIK, yon, yari, k.donus, 1f, 1f, 1f, a);
            if (!sade) {
                kuad(m, KARA_DELIK_AKIS, yon, yari, k.donus + sn * k.akis, 1f, 0.78f, 0.45f, 0.65f * a);
            }
        }
    }

    private static void kuad(Matrix4f m, ResourceLocation doku, Vector3f yon, float yari, float donus, float r, float g,
            float b, float a) {
        kuadDik(m, doku, yon, yari, yari, donus, r, g, b, a);
    }

    /** Gök küresine teğet dokulu dikdörtgen: merkez {@code yon*100}, yarı boylar {@code yariU}/{@code yariV}, kendi ekseninde {@code donus}. */
    private static void kuadDik(Matrix4f m, ResourceLocation doku, Vector3f yon, float yariU, float yariV, float donus,
            float r, float g, float b, float a) {
        if (a <= 0.002f) {
            return;
        }
        Vector3f ref = Math.abs(yon.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(ref).cross(yon).normalize();
        Vector3f v = new Vector3f(yon).cross(u).normalize();
        float dr = (float) Math.toRadians(donus);
        Vector3f uu = new Vector3f(u).mul(Mth.cos(dr)).add(new Vector3f(v).mul(Mth.sin(dr))).mul(yariU);
        Vector3f vv = new Vector3f(v).mul(Mth.cos(dr)).sub(new Vector3f(u).mul(Mth.sin(dr))).mul(yariV);
        Vector3f c = new Vector3f(yon).mul(100f);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, doku);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.vertex(m, c.x - uu.x - vv.x, c.y - uu.y - vv.y, c.z - uu.z - vv.z).uv(0f, 0f).color(r, g, b, a).endVertex();
        bb.vertex(m, c.x + uu.x - vv.x, c.y + uu.y - vv.y, c.z + uu.z - vv.z).uv(1f, 0f).color(r, g, b, a).endVertex();
        bb.vertex(m, c.x + uu.x + vv.x, c.y + uu.y + vv.y, c.z + uu.z + vv.z).uv(1f, 1f).color(r, g, b, a).endVertex();
        bb.vertex(m, c.x - uu.x + vv.x, c.y - uu.y + vv.y, c.z - uu.z + vv.z).uv(0f, 1f).color(r, g, b, a).endVertex();
        BufferUploader.drawWithShader(bb.end());
    }

    /**
     * {@link #kuadDik} ama doku UV'si merkez çevresinde {@code uvDonus} derece döner (geometri sabit): basık diskte akışın
     * diskle birlikte dönmesi için. Dokunun içeriği yarıçap ≤ 0,46 UV içinde kalmalı (köşeler kenetli şeffaf kenardan okur).
     */
    private static void kuadUv(Matrix4f m, ResourceLocation doku, Vector3f yon, float yariU, float yariV, float donus,
            float uvDonus, float r, float g, float b, float a) {
        if (a <= 0.002f) {
            return;
        }
        Vector3f ref = Math.abs(yon.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(ref).cross(yon).normalize();
        Vector3f v = new Vector3f(yon).cross(u).normalize();
        float dr = (float) Math.toRadians(donus);
        Vector3f uu = new Vector3f(u).mul(Mth.cos(dr)).add(new Vector3f(v).mul(Mth.sin(dr))).mul(yariU);
        Vector3f vv = new Vector3f(v).mul(Mth.cos(dr)).sub(new Vector3f(u).mul(Mth.sin(dr))).mul(yariV);
        Vector3f c = new Vector3f(yon).mul(100f);
        float ur = (float) Math.toRadians(uvDonus);
        float cs = Mth.cos(ur) * 0.5f;
        float sn = Mth.sin(ur) * 0.5f;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, doku);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int j = 0; j < 4; j++) {
            float su = j == 1 || j == 2 ? 1f : -1f;
            float sv = j >= 2 ? 1f : -1f;
            bb.vertex(m, c.x + su * uu.x + sv * vv.x, c.y + su * uu.y + sv * vv.y, c.z + su * uu.z + sv * vv.z)
                    .uv(0.5f + su * cs - sv * sn, 0.5f + su * sn + sv * cs).color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
    }
}
