package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KaraDelikMercek;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KaraDelikYutmaEgrileri;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Yutan kara delik (pulsar-2 tur 4, şef kararı B; referans Gargantua): dünya çapasına ({@link GokCapasi#KARADELIK}) bağlı,
 * kameranın dönüşünü taşıyan dünya pozunda çizilen 3B nesne. Akresyon diski gerçek eğik halkadır (disk düzlemi gökte
 * sabit; oyuncu uçunca bakış vektörüyle görünen eğim değişir). Arka yarısı Schwarzschild ince-merceğiyle
 * ({@link KaraDelikMercek}) gölgenin üstünde ve altında iki yay olarak görünür; ön yarı gölgenin önünden geçer. Disk
 * referanstaki gibi beyaz-sıcak ve soluk (doygunluk düşük). Köşe parlaklığı Doppler çarpanı (≤ 1; Doppler yalnız
 * gölgenin iki yanındaki yanal bantta: yaklaşan yan 1, uzaklaşan ×0,3; gölgenin üstü ve altındaki orta sütunda yay ve
 * ön yarı tepesi ≥ 0,9) × besleme eğrisi × mor kalıntı çarpanıdır; 1'i aşan pay toplamalı ek geçişte çizilir. Üstüne
 * aynı geometrinin {@value #PARILTI_KALINLIK} kat kalın soluk kopyası parlama verir. Sıra: mercek sönümü (normal) →
 * bükülen yıldızlar → pulsar → spagetti tüpü → arka yarının yayları → tam siyah gölge (normal karışım: arkadaki
 * yıldız, nebula, kalıntı ve pulsar silinir) → ön yarı → foton halkası → parlama ve silüet. Konumlar Schwarzschild
 * yarıçapı (rs) biriminde hesaplanır; disk dış yarıçapı yutma eğrisindeki açısal çapın yarısıdır. Yalnız render
 * thread'i.
 */
public final class KaraDelik3B {
    private static final ResourceLocation DISK = doku("karadelik_3b_disk_soluk");
    private static final ResourceLocation PARILTI = doku("karadelik_3b_parilti_soluk");
    private static final ResourceLocation TUP = doku("karadelik_3b_tup");
    private static final ResourceLocation HALKA = doku("karadelik_3b_halka");
    private static final ResourceLocation YILDIZ = doku("karadelik_3b_yildiz");
    private static final ResourceLocation HALE = doku("hale");
    private static final ResourceLocation JET = doku("kara_delik_jet");
    /** Güneş yolu düzleminin normali (dünya +Z): diskin gökteki yönelimi buna göre kurulur. */
    private static final Vector3f GUNES_YOLU_EKSENI = new Vector3f(0f, 0f, 1f);
    /** Disk dokusunun azimutta tekrarı. */
    private static final float DISK_TEKRAR = 2f;
    /** Uzaklaşan ve yaklaşan yanın tonu (Doppler: kırmızıya / maviye kayma, soluk); yutum sonrası mor kalıntı. */
    private static final float[] UZAKLASAN = {1f, 0.92f, 0.84f};
    private static final float[] YAKLASAN = {0.94f, 0.97f, 1f};
    private static final float[] MOR = {0.88f, 0.62f, 1f};
    /** Yutumdan sonraki mor kalıntının çarpanı (1 + bu × morluk): besleme eğrisiyle birlikte Doppler çarpanından sonra. */
    private static final float KALINTI_EK = 0.8f;
    /**
     * Gölgenin üstündeki ve altındaki orta sütunun Doppler çarpanı: üstte merceklenmiş arka yarı, altta ön yarı ve ikinci
     * görüntü (referansta alttaki en parlak, üstteki ~0,9'u). Orada madde bakışa dik gider; doğrusal Doppler ×0,65
     * verirdi.
     */
    private static final float TEPE_UST = 0.9f;
    private static final float TEPE_ALT = 1f;
    /**
     * Doppler'in yanal bandı: köşenin görüntü düzlemindeki yanal uzaklığı (|r·cos φ|) gölge yarıçapının bu katları
     * arasında orta sütunun çarpanından diskin Doppler'ine geçer. Bandın dışı (gölgenin iki yanı) tam Doppler.
     */
    private static final float YANAL_IC = 0.5f;
    private static final float YANAL_DIS = 1f;
    /** Parlama kopyası: dikey kalınlık katı, alfası, radyal genişleme (iç ve dış yarıçap çarpanı). */
    private static final float PARILTI_KALINLIK = 2.5f;
    private static final float PARILTI_ALFA = 0.3f;
    private static final float PARILTI_IC = 0.8f;
    private static final float PARILTI_DIS = 1.12f;
    private static final float[] TUP_PULSAR = {0.76f, 0.86f, 1f};
    private static final float[] TUP_DISK = {1f, 0.8f, 0.56f};
    /** Parlamada kara delik silüetinin rengi (PL-27, PL-41: örtü altında bile koyu okunsun). */
    private static final float[] SILUET = {0.015f, 0.006f, 0.03f};
    /** Parlama halesinin yarı boyu (kameradan {@value GokCapasi#SIGDIRMA} birimde; ~40°). */
    private static final float PARLAMA_HALE = 76.5f;
    /** Pulsar çekirdeğinin açısal yarıçapı (çekirdek 10°): tüpün başındaki kalınlık buna göre. */
    private static final float CEKIRDEK_YARI_DERECE = 5f;
    /** Bükülen yıldızların kaynak düzlemindeki kayması (alan yarıçapı / sn): yıldızlar halkanın çevresinde akar. */
    private static final float YILDIZ_KAYMA = 0.008f;
    /**
     * Spagetti tüpünün diske girdiği azimut (sağ yan kenar; madde arkaya döner). Pulsar güneş yolunda, yani diskin kutup
     * doğrultusunda durduğu için yan seçimi sabit: konuma göre seçmek tüpün ucunu iki yan arasında sıçratırdı.
     */
    private static final float GIRIS = 0f;
    private static final int ARKA = 0;
    private static final int IKINCI = 1;
    private static final int ON = 2;
    private static final int TUMU = 3;
    private static final int GECIS_ANA = 0;
    private static final int GECIS_PARILTI = 1;
    private static final int GECIS_KALINTI = 2;

    /** Kademe: disk dilimi, radyal bant, yıldız, tüp kesiti ve çevresi, disk kuyruğu dilimi. */
    private record Kademe(int dilim, int bant, int yildiz, int kesit, int cevre, int kuyruk) {
    }

    private static EfektKalitesi kurulan;
    private static Kademe kademe;
    /** Yıldız başına: kaynak düzleminde x, y (en büyük alanın yarıçapına oranla), yarı boy (gök birimi), parlaklık, r, g, b. */
    private static float[] yildizlar = new float[0];
    private static int koseSayisi;
    private static int sonDortgen;
    private static boolean ilkCizimYazildi;

    private static final Vector3f C = new Vector3f();
    private static final Vector3f D = new Vector3f();
    private static final Vector3f A = new Vector3f();
    private static final Vector3f V = new Vector3f();
    private static final Vector3f B = new Vector3f();
    private static final Vector3f N = new Vector3f();
    private static final float[] GOR = new float[4];
    private static final Vector3f[] KOSE = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
    private static final float[] KOSE_ALFA = new float[4];
    private static float rs;
    private static float egim;

    private KaraDelik3B() {
    }

    private static ResourceLocation doku(String ad) {
        return DogalAfetler.konum("textures/environment/gokyuzu/" + ad + ".png");
    }

    /** Son karede çizilen dörtgen (kara delik, tüp ve mercek yıldızları dahil; pulsar hariç). */
    public static int sonDortgen() {
        return sonDortgen;
    }

    /** Kara deliksiz kare: dörtgen sayısı 0. */
    static void bosKare() {
        sonDortgen = 0;
    }

    private static void hazirla() {
        EfektKalitesi k = IstemciAyarlari.efektKalitesi();
        if (k == kurulan) {
            return;
        }
        kademe = switch (k) {
            case DUSUK -> new Kademe(24, 2, 120, 6, 5, 8);
            case ORTA -> new Kademe(72, 3, 300, 12, 8, 16);
            case ULTRA -> new Kademe(96, 4, 600, 16, 12, 24);
            default -> new Kademe(72, 4, 450, 16, 10, 20);
        };
        Random r = new Random(20261109L);
        float[][] renkler = {GokyuzuEgrileri.rgb(0xCAD7FF), GokyuzuEgrileri.rgb(0xF8F7FF), GokyuzuEgrileri.rgb(0xFFF4EA),
                GokyuzuEgrileri.rgb(0xFFD2A1)};
        yildizlar = new float[kademe.yildiz() * 7];
        for (int i = 0; i < kademe.yildiz(); i++) {
            float x;
            float y;
            do {
                x = r.nextFloat() * 2f - 1f;
                y = r.nextFloat() * 2f - 1f;
            } while (x * x + y * y >= 1f);
            float m = (float) -Math.log(1.0 - r.nextFloat());
            float[] c = renkler[r.nextInt(renkler.length)];
            int o = i * 7;
            yildizlar[o] = x;
            yildizlar[o + 1] = y;
            yildizlar[o + 2] = 0.12f + 0.2f * Math.min(1f, m / 4f);
            yildizlar[o + 3] = Math.min(1f, 0.35f + 0.2f * m);
            yildizlar[o + 4] = c[0];
            yildizlar[o + 5] = c[1];
            yildizlar[o + 6] = c[2];
        }
        kurulan = k;
        ilkCizimYazildi = false;
    }

    /**
     * {@code dunya}: kamera dönüşü pozu (dünya eksenleri); {@code merkez}: {@link GokCapasi#merkez}; {@code birim}:
     * {@link GokCapasi#birim}; {@code s}: yutma saniyesi. Disk düzlemi bakış yönüne göre kurulur (paralaks eğimi yok).
     */
    public static void ciz(Matrix4f dunya, Vector3f merkez, float birim, float s, float yagmursuz, float sn) {
        ciz(dunya, merkez, birim, s, yagmursuz, sn, merkez, null, 0f, null);
    }

    /**
     * Tam çizim. {@code diskYonu}: kara deliğin paralakssız dünya yönü (disk düzlemi buna göre gökte sabit; gerçek bakış
     * {@code merkez}'den). {@code pulsar}: yutulan pulsarın merkezi (null: tüp yok); {@code pulsarCiz} yıldızlardan sonra,
     * gölgeden önce çağrılır (gölgenin arkasında kalan pulsar silinir, mercek sönümü onu karartmaz).
     */
    static void ciz(Matrix4f dunya, Vector3f merkez, float birim, float s, float yagmursuz, float sn, Vector3f diskYonu,
            @Nullable Vector3f pulsar, float pulsarAlfa, @Nullable Runnable pulsarCiz) {
        hazirla();
        koseSayisi = 0;
        float cap = KaraDelikYutmaEgrileri.karaDelikCapi(s);
        rs = birim * GokCapasi.KARADELIK.uzaklik() * (float) Math.tan(Math.toRadians(cap) / 2.0)
                / KaraDelikMercek.DISK_DIS_RS;
        cerceve(merkez, diskYonu, KaraDelikYutmaEgrileri.gorunenEgim(s));
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        float merc = KaraDelikYutmaEgrileri.merceklenme(s) * yagmursuz;
        normal();
        sonum(dunya, 0.5f * merc);
        toplamali();
        yildizlar(dunya, s, merc * (0.75f + 0.25f * KaraDelikYutmaEgrileri.mercekYaylari(s)));
        if (pulsarCiz != null) {
            pulsarCiz.run();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            toplamali();
        }
        if (pulsar != null) {
            tup(dunya, pulsar, s, sn, pulsarAlfa);
        }
        float diskA = KaraDelikYutmaEgrileri.diskParlakligi(s) * yagmursuz;
        float mor = KaraDelikYutmaEgrileri.kalintiMorlugu(s);
        float akis = sn * KaraDelikYutmaEgrileri.diskDonusHizi(s) / 360f * DISK_TEKRAR % 1f;
        if (pulsar != null) {
            kuyruk(dunya, s, sn, pulsarAlfa, true);
        }
        disk(dunya, ARKA, diskA, akis, mor);
        disk(dunya, IKINCI, diskA, akis, mor);
        normal();
        golge(dunya, 0f, 0f, 0f, KaraDelikYutmaEgrileri.karaDelikAlfa(s) * yagmursuz);
        toplamali();
        disk(dunya, ON, diskA, akis, mor);
        if (pulsar != null) {
            kuyruk(dunya, s, sn, pulsarAlfa, false);
        }
        float foton = KaraDelikYutmaEgrileri.fotonHalkasi(s) * yagmursuz;
        halka(dunya, KaraDelikMercek.FOTON_RS, 0.09f, 0.96f, 0.94f, 1f, foton);
        if (kurulan != EfektKalitesi.DUSUK) {
            halka(dunya, KaraDelikMercek.FOTON_RS, 0.4f, 0.9f, 0.86f, 1f, 0.3f * foton);
        }
        float parlama = KaraDelikYutmaEgrileri.parlama(s);
        if (parlama > 0.003f) {
            levha(dunya, HALE, PARLAMA_HALE, PARLAMA_HALE, 0.92f, 0.84f, 1f, parlama);
            siluet(dunya, akis, parlama * KaraDelikYutmaEgrileri.karaDelikAlfa(s));
        }
        toplamali();
        sonDortgen = koseSayisi / 4;
        if (!ilkCizimYazildi) {
            ilkCizimYazildi = true;
            DogalAfetler.LOGGER.info("[gokyuzu] 3B kara delik: kalite={}, dilim={}, yıldız={}, tüp={}x{}, dörtgen={}",
                    kurulan, kademe.dilim(), kademe.yildiz(), kademe.kesit(), kademe.cevre(), sonDortgen);
        }
    }

    /**
     * Disk çerçevesi: normal göğün güneş yolu teğetinden (batı göğünde yukarı) bakışa doğru {@code egimDerece} yatık;
     * disk ekranda yatay uzanır. A sağ, V yukarı (görüntü düzlemi), D bakış, B disk düzleminde arkaya.
     */
    private static void cerceve(Vector3f merkez, Vector3f diskYonu, float egimDerece) {
        C.set(merkez);
        D.set(merkez).normalize();
        Vector3f y0 = new Vector3f(diskYonu).normalize();
        Vector3f t = new Vector3f(y0).cross(GUNES_YOLU_EKSENI);
        if (t.lengthSquared() < 1e-6f) {
            t.set(0f, 1f, 0f).cross(y0).cross(y0).negate();
        }
        t.normalize();
        float e = egimDerece * Mth.DEG_TO_RAD;
        N.set(t).mul(Mth.cos(e)).sub(new Vector3f(y0).mul(Mth.sin(e))).normalize();
        float nd = N.dot(D);
        V.set(N).sub(new Vector3f(D).mul(nd));
        if (V.lengthSquared() < 1e-6f) {
            V.set(t);
        }
        V.normalize();
        A.set(D).cross(V).normalize();
        egim = (float) Math.toDegrees(Math.asin(Mth.clamp(-nd, -1f, 1f)));
        float i = egim * Mth.DEG_TO_RAD;
        B.set(D).mul(Mth.cos(i)).add(new Vector3f(V).mul(Mth.sin(i)));
    }

    /** Görüntü düzleminde (rs) nokta; {@code z} bakış yönünde gerçek derinlik. */
    private static Vector3f duzlem(float x, float y, float z, Vector3f cikis) {
        return cikis.set(C).add(A.x * x * rs + V.x * y * rs + D.x * z * rs, A.y * x * rs + V.y * y * rs + D.y * z * rs,
                A.z * x * rs + V.z * y * rs + D.z * z * rs);
    }

    /** Noktayı kameradan aynı yönde merkezin uzaklığına çeker: ekrandaki yeri aynı, uzak düzlemi aşmaz. */
    private static Vector3f kureye(Vector3f p) {
        return p.normalize().mul(C.length());
    }

    /**
     * Disk noktasının (r rs, φ derece) görüntüsü: ön yarı gerçek 3B konumda, merceklenmiş arka yarı görüntü düzleminde.
     * {@code GOR[3]} büyütme. İkinci görüntü yoksa false.
     */
    private static boolean diskKosesi(float r, float fi, boolean ikincil, Vector3f cikis) {
        if (!KaraDelikMercek.diskGoruntusu(r, fi, egim, ikincil, GOR)) {
            return false;
        }
        duzlem(GOR[0], GOR[1], Math.min(0f, GOR[2]), cikis);
        return true;
    }

    // ------------------------------------------------------------------ parçalar

    /** Merceğin çevresindeki yıldız sönümü (normal karışım): bükülen yıldız alanı boyunca merkezden kenara azalır. */
    private static void sonum(Matrix4f m, float a) {
        if (a <= 0.003f) {
            return;
        }
        float alan = KaraDelikMercek.YILDIZ_ALANI * KaraDelikMercek.DISK_DIS_RS;
        float ic = alan * 0.4f;
        int n = Math.max(16, kademe.dilim() * 2 / 3);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Vector3f p = new Vector3f();
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n;
            float a1 = (i + 1) * Mth.TWO_PI / n;
            renkKosesi(bb, m, kureye(duzlem(0f, 0f, 0f, p)), 0f, 0f, 0f, a);
            renkKosesi(bb, m, kureye(duzlem(Mth.cos(a0) * ic, Mth.sin(a0) * ic, 0f, p)), 0f, 0f, 0f, a);
            renkKosesi(bb, m, kureye(duzlem(Mth.cos(a1) * ic, Mth.sin(a1) * ic, 0f, p)), 0f, 0f, 0f, a);
            renkKosesi(bb, m, kureye(duzlem(0f, 0f, 0f, p)), 0f, 0f, 0f, a);
            renkKosesi(bb, m, kureye(duzlem(Mth.cos(a0) * ic, Mth.sin(a0) * ic, 0f, p)), 0f, 0f, 0f, a);
            renkKosesi(bb, m, kureye(duzlem(Mth.cos(a0) * alan, Mth.sin(a0) * alan, 0f, p)), 0f, 0f, 0f, 0f);
            renkKosesi(bb, m, kureye(duzlem(Mth.cos(a1) * alan, Mth.sin(a1) * alan, 0f, p)), 0f, 0f, 0f, 0f);
            renkKosesi(bb, m, kureye(duzlem(Mth.cos(a1) * ic, Mth.sin(a1) * ic, 0f, p)), 0f, 0f, 0f, a);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /**
     * Gölge çevresindeki bükülen yıldızlar: kaynak düzleminde gökte sabit (yavaşça akan) yıldızlar sapma tablosundan
     * iki görüntüye taşınır (Einstein halkasına toplanır, halkaya yakın olanlar teğet yaya uzar). Alan disk dış
     * yarıçapının {@link KaraDelikMercek#YILDIZ_ALANI} katı; kara delik yaklaştıkça büyür, yıldız yoğunluğu sabit kalır.
     */
    private static void yildizlar(Matrix4f m, float s, float a) {
        if (a <= 0.003f) {
            return;
        }
        float dis = KaraDelikMercek.DISK_DIS_RS * rs;
        float alan = KaraDelikMercek.YILDIZ_ALANI * dis;
        float enBuyukAlan = KaraDelikMercek.YILDIZ_ALANI * C.length()
                * (float) Math.tan(Math.toRadians(KaraDelikYutmaEgrileri.EN_BUYUK_CAP) / 2.0);
        float einstein = KaraDelikMercek.YILDIZ_EINSTEIN * dis;
        float kayma = s * YILDIZ_KAYMA;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, YILDIZ);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Vector3f merkez = new Vector3f();
        Vector3f radyal = new Vector3f();
        Vector3f teget = new Vector3f();
        Vector3f p = new Vector3f();
        for (int o = 0; o < yildizlar.length; o += 7) {
            float bx = ((yildizlar[o] + 1f + kayma) % 2f) - 1f;
            float by = yildizlar[o + 1];
            float beta = (float) Math.sqrt(bx * bx + by * by) * enBuyukAlan;
            if (beta >= alan) {
                continue;
            }
            float kenar = 1f - GokyuzuEgrileri.yumusak(0.6f, 1f, beta / alan);
            float dx = beta > 1e-5f ? bx * enBuyukAlan / beta : 1f;
            float dy = beta > 1e-5f ? by * enBuyukAlan / beta : 0f;
            radyal.set(A).mul(dx).add(V.x * dy, V.y * dy, V.z * dy);
            teget.set(V).mul(dx).sub(A.x * dy, A.y * dy, A.z * dy);
            float boy = yildizlar[o + 2];
            for (int g = 0; g < 2; g++) {
                float theta = g == 0 ? KaraDelikMercek.goruntuArti(beta, einstein)
                        : -KaraDelikMercek.goruntuEksi(beta, einstein);
                float t = boy * Mth.clamp(KaraDelikMercek.tegetselUzama(theta, beta), 0.2f, 6f);
                float r = boy * Mth.clamp(KaraDelikMercek.radyalUzama(theta, einstein), 0.2f, 1f);
                float ya = a * yildizlar[o + 3] * kenar * (g == 0 ? 1f : 0.7f);
                merkez.set(C).add(radyal.x * theta, radyal.y * theta, radyal.z * theta);
                for (int k = 0; k < 4; k++) {
                    float sr = k == 1 || k == 2 ? 1f : -1f;
                    float st = k >= 2 ? 1f : -1f;
                    p.set(merkez).add(radyal.x * sr * r + teget.x * st * t, radyal.y * sr * r + teget.y * st * t,
                            radyal.z * sr * r + teget.z * st * t);
                    dokuKosesi(bb, m, kureye(p), sr > 0f ? 1f : 0f, st > 0f ? 1f : 0f, yildizlar[o + 4],
                            yildizlar[o + 5], yildizlar[o + 6], ya);
                }
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /**
     * Spagetti: pulsar çekirdeğinden diskin {@link #GIRIS} kenarına uzanan 3B kıvrımlı tüp (kübik Bézier, diskin
     * dönüş yönünde teğet girer; yanal dalga). Kesitler halka; köşe alfası bakışa dönüklükten (ince saydam hacim:
     * ortası parlak, kenarı sönük), akış dokusu kara deliğe doğru kayar. Boyu spagettileşmeyle uzar.
     */
    private static void tup(Matrix4f m, Vector3f pulsar, float s, float sn, float a) {
        float spagetti = KaraDelikYutmaEgrileri.spagetti(s);
        if (spagetti <= 0.01f || a <= 0.002f) {
            return;
        }
        Vector3f p3 = new Vector3f();
        diskKosesi(KaraDelikMercek.DISK_DIS_RS * 0.95f, GIRIS, false, p3);
        float gr = GIRIS * Mth.DEG_TO_RAD;
        Vector3f akisYonu = new Vector3f(A).mul(-Mth.sin(gr)).add(new Vector3f(B).mul(Mth.cos(gr)));
        float boy = pulsar.distance(p3);
        Vector3f p0 = new Vector3f(pulsar);
        Vector3f p1 = new Vector3f(p0).lerp(p3, 0.3f).add(new Vector3f(N).mul(boy * 0.12f));
        Vector3f p2 = new Vector3f(p3).sub(new Vector3f(akisYonu).mul(boy * 0.45f));
        float r0 = pulsar.length() * (float) Math.tan(Math.toRadians(CEKIRDEK_YARI_DERECE)) * 0.55f;
        float r1 = rs * 0.35f;
        int kesit = kademe.kesit();
        int cevre = kademe.cevre();
        float akis = sn * 0.8f % 1f;
        float[] konum = new float[(kesit + 1) * (cevre + 1) * 3];
        float[] alfa = new float[(kesit + 1) * (cevre + 1)];
        float[] boyuna = new float[kesit + 1];
        Vector3f c = new Vector3f();
        Vector3f teget = new Vector3f();
        Vector3f u1 = new Vector3f();
        Vector3f u2 = new Vector3f();
        Vector3f yan = new Vector3f();
        for (int k = 0; k <= kesit; k++) {
            float t = spagetti * k / kesit;
            float q = 1f - t;
            c.set(p0).mul(q * q * q).add(new Vector3f(p1).mul(3f * q * q * t)).add(new Vector3f(p2).mul(3f * q * t * t))
                    .add(new Vector3f(p3).mul(t * t * t));
            teget.set(p1).sub(p0).mul(3f * q * q).add(new Vector3f(p2).sub(p1).mul(6f * q * t))
                    .add(new Vector3f(p3).sub(p2).mul(3f * t * t));
            if (teget.lengthSquared() < 1e-8f) {
                teget.set(p3).sub(p0);
            }
            teget.normalize();
            u1.set(teget).cross(c);
            if (u1.lengthSquared() < 1e-8f) {
                u1.set(teget).cross(V);
            }
            u1.normalize();
            u2.set(u1).cross(teget).normalize();
            float dalga = boy * 0.04f * Mth.sin(t * 3f * Mth.PI - sn * 1.3f) * Mth.sin(Mth.PI * t);
            c.add(u1.x * dalga, u1.y * dalga, u1.z * dalga);
            float yaricap = Mth.lerp((float) Math.pow(t, 0.6), r0, r1);
            boyuna[k] = t;
            for (int j = 0; j <= cevre; j++) {
                float aci = (j % cevre) * Mth.TWO_PI / cevre;
                yan.set(u1).mul(Mth.cos(aci)).add(u2.x * Mth.sin(aci), u2.y * Mth.sin(aci), u2.z * Mth.sin(aci));
                int i = k * (cevre + 1) + j;
                konum[i * 3] = c.x + yan.x * yaricap;
                konum[i * 3 + 1] = c.y + yan.y * yaricap;
                konum[i * 3 + 2] = c.z + yan.z * yaricap;
                float bakis = Math.abs(yan.dot(c.x + yan.x * yaricap, c.y + yan.y * yaricap, c.z + yan.z * yaricap)
                        / Math.max(1e-4f, c.length()));
                alfa[i] = a * spagetti * (0.12f + 0.88f * bakis * bakis) * (1f - 0.3f * t);
            }
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TUP);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int k = 0; k < kesit; k++) {
            for (int j = 0; j < cevre; j++) {
                for (int q = 0; q < 4; q++) {
                    int kk = k + (q == 1 || q == 2 ? 1 : 0);
                    int jj = j + (q >= 2 ? 1 : 0);
                    int i = kk * (cevre + 1) + jj;
                    float t = boyuna[kk];
                    bb.vertex(m, konum[i * 3], konum[i * 3 + 1], konum[i * 3 + 2])
                            .uv(t * 3f - akis, (float) jj / cevre + t * 1.5f)
                            .color(Mth.lerp(t, TUP_PULSAR[0], TUP_DISK[0]), Mth.lerp(t, TUP_PULSAR[1], TUP_DISK[1]),
                                    Mth.lerp(t, TUP_PULSAR[2], TUP_DISK[2]), alfa[i]).endVertex();
                    koseSayisi++;
                }
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /**
     * Diske akan kuyruk: tüpün girdiği yan kenardan diskin dönüş yönünde ~1,2 turda iç kenara sarmal şerit (diskle
     * aynı mercek: arka yarısı yay olur). {@code arka}: yalnız arka (gölgeden önce) ya da yalnız ön yarısı.
     */
    private static void kuyruk(Matrix4f m, float s, float sn, float a, boolean arka) {
        float boy = GokyuzuEgrileri.yumusak(0.5f, 1f, KaraDelikYutmaEgrileri.spagetti(s));
        if (boy <= 0.01f || a <= 0.002f) {
            return;
        }
        int n = kademe.kuyruk();
        float akis = sn * 0.8f % 1f;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TUP);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float dis = KaraDelikMercek.DISK_DIS_RS * 0.95f;
        float ic = KaraDelikMercek.DISK_IC_RS + 0.4f;
        for (int i = 0; i < n; i++) {
            float t0 = boy * i / n;
            float t1 = boy * (i + 1) / n;
            float orta = Math.floorMod((int) (GIRIS + 430f * (t0 + t1) * 0.5f), 360);
            if ((orta < 180) != arka) {
                continue;
            }
            boolean tamam = true;
            for (int q = 0; q < 4 && tamam; q++) {
                float t = q == 1 || q == 2 ? t1 : t0;
                float r = Mth.lerp(t, dis, ic);
                float w = Mth.lerp(t, 0.9f, 0.35f) * (q >= 2 ? 1f : -1f);
                tamam = diskKosesi(r + w, GIRIS + 430f * t, false, KOSE[q]);
            }
            if (!tamam) {
                continue;
            }
            for (int q = 0; q < 4; q++) {
                float t = q == 1 || q == 2 ? t1 : t0;
                float f = KaraDelikMercek.doppler(KaraDelikMercek.yaklasmaCos(GIRIS + 430f * t, egim));
                dokuKosesi(bb, m, KOSE[q], t * 4f - akis, q >= 2 ? 1f : 0f, TUP_DISK[0], TUP_DISK[1], TUP_DISK[2],
                        a * f * (1f - 0.4f * t));
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /**
     * Akresyon diski: {@code parca} ARKA merceklenmiş arka yarı (gölgenin üstünde yay), IKINCI arka yarının ikinci
     * görüntüsü (altında), ON gölgenin önünden geçen yarı. Parlama kopyası, ana geçiş ve (parlaklık 1'i aşabiliyorsa)
     * kalıntı geçişi.
     */
    private static void disk(Matrix4f m, int parca, float a, float akis, float mor) {
        diskGecisi(m, parca, a, akis, mor, null, GECIS_PARILTI);
        diskGecisi(m, parca, a, akis, mor, null, GECIS_ANA);
        if (a * (1f + KALINTI_EK * mor) > 1f) {
            diskGecisi(m, parca, a, akis, mor, null, GECIS_KALINTI);
        }
    }

    /**
     * Diskin bir geçişi. Köşe parlaklığı {@link #dopplerCarpani} × {@code a} (besleme eğrisi) × (1 +
     * {@value #KALINTI_EK} × {@code mor}): ana geçiş 1'e kadarını, kalıntı geçişi aşanı toplamalı çizer (köşe alfası
     * baytta).
     * Parlama geçişi tek radyal bant: yarıçap aralığı genişler, köşeler görüntü düzleminde orta çizgiden dikeyde
     * {@value #PARILTI_KALINLIK} kat uzaklaşır, alfa {@value #PARILTI_ALFA}. {@code siluet} verilirse yalnız ana geçiş, düz
     * renk ve Doppler'siz (TUMU: iki yarının ana görüntüsü).
     */
    private static void diskGecisi(Matrix4f m, int parca, float a, float akis, float mor, @Nullable float[] siluet,
            int gecis) {
        if (a <= 0.002f) {
            return;
        }
        boolean parilti = gecis == GECIS_PARILTI;
        int dilim = kademe.dilim();
        int bant = parilti ? 1 : kademe.bant();
        boolean ikincil = parca == IKINCI;
        float ic = KaraDelikMercek.DISK_IC_RS * (parilti ? PARILTI_IC : 1f);
        float dis = KaraDelikMercek.DISK_DIS_RS * (parilti ? PARILTI_DIS : 1f);
        float tepe = parca == ARKA ? TEPE_UST : TEPE_ALT;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, parilti ? PARILTI : DISK);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < dilim; i++) {
            boolean arka = i < dilim / 2;
            if (parca != TUMU && arka == (parca == ON)) {
                continue;
            }
            float f0 = i * 360f / dilim;
            float f1 = (i + 1) * 360f / dilim;
            for (int j = 0; j < bant; j++) {
                float v0 = (float) j / bant;
                float v1 = (float) (j + 1) / bant;
                boolean tamam = true;
                float enBuyuk = 0f;
                for (int q = 0; q < 4 && tamam; q++) {
                    float fi = q == 1 || q == 2 ? f1 : f0;
                    float r = Mth.lerp(q >= 2 ? v1 : v0, ic, dis);
                    tamam = parilti ? pariltiKosesi(r, (ic + dis) * 0.5f, fi, ikincil, KOSE[q])
                            : diskKosesi(r, fi, ikincil, KOSE[q]);
                    float parlak = siluet != null ? a
                            : dopplerCarpani(fi, r, tepe) * a * (1f + KALINTI_EK * mor);
                    KOSE_ALFA[q] = switch (gecis) {
                        case GECIS_PARILTI -> PARILTI_ALFA * parlak;
                        case GECIS_KALINTI -> Math.max(0f, parlak - 1f);
                        default -> Math.min(1f, parlak);
                    };
                    enBuyuk = Math.max(enBuyuk, KOSE_ALFA[q]);
                }
                if (!tamam || enBuyuk <= 0.002f) {
                    continue;
                }
                for (int q = 0; q < 4; q++) {
                    float fi = q == 1 || q == 2 ? f1 : f0;
                    float v = q >= 2 ? v1 : v0;
                    float u = fi / 360f * DISK_TEKRAR - akis;
                    if (siluet != null) {
                        dokuKosesi(bb, m, KOSE[q], u, v, siluet[0], siluet[1], siluet[2], KOSE_ALFA[q]);
                        continue;
                    }
                    float ton = (KaraDelikMercek.yaklasmaCos(fi, egim) + 1f) * 0.5f;
                    dokuKosesi(bb, m, KOSE[q], u, v,
                            Mth.lerp(mor, Mth.lerp(ton, UZAKLASAN[0], YAKLASAN[0]), MOR[0]),
                            Mth.lerp(mor, Mth.lerp(ton, UZAKLASAN[1], YAKLASAN[1]), MOR[1]),
                            Mth.lerp(mor, Mth.lerp(ton, UZAKLASAN[2], YAKLASAN[2]), MOR[2]), KOSE_ALFA[q]);
                }
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /**
     * Köşenin Doppler çarpanı (≤ 1). Gölgenin iki yanında (yanal uzaklık ≥ {@value #YANAL_DIS} gölge yarıçapı) diskin
     * Doppler'i: yaklaşan yan 1, uzaklaşan ×0,3. Gölgenin üstü ve altındaki orta sütunda (≤ {@value #YANAL_IC}) madde
     * bakışa dik gider ve çarpan {@code tepe}; arada yumuşak geçiş. Sol/sağ oranı bandın dışında kalır, yaylar ve ön
     * yarı ortada sönmez.
     */
    private static float dopplerCarpani(float fi, float r, float tepe) {
        float d = KaraDelikMercek.doppler(KaraDelikMercek.yaklasmaCos(fi, egim));
        float yanal = Math.abs(r * Mth.cos(fi * Mth.DEG_TO_RAD)) / KaraDelikMercek.GOLGE_RS;
        float t = Mth.clamp((yanal - YANAL_IC) / (YANAL_DIS - YANAL_IC), 0f, 1f);
        return tepe + (d - tepe) * t * t * (3f - 2f * t);
    }

    /**
     * Parlama kopyasının köşesi: disk noktasının görüntüsü, aynı azimutta {@code orta} yarıçapın görüntüsünden dikeyde
     * {@value #PARILTI_KALINLIK} kat uzaklaştırılır (kenardan görülen diskte kalınlık dikeydir).
     */
    private static boolean pariltiKosesi(float r, float orta, float fi, boolean ikincil, Vector3f cikis) {
        if (!KaraDelikMercek.diskGoruntusu(orta, fi, egim, ikincil, GOR)) {
            return false;
        }
        float yOrta = GOR[1];
        if (!KaraDelikMercek.diskGoruntusu(r, fi, egim, ikincil, GOR)) {
            return false;
        }
        duzlem(GOR[0], yOrta + PARILTI_KALINLIK * (GOR[1] - yOrta), Math.min(0f, GOR[2]), cikis);
        return true;
    }

    /** Tam siyah gölge diski ({@link KaraDelikMercek#GOLGE_RS}); normal karışımla arkasındakini siler. */
    private static void golge(Matrix4f m, float r, float g, float b, float a) {
        if (a <= 0.002f) {
            return;
        }
        int n = kademe.dilim();
        float y = KaraDelikMercek.GOLGE_RS;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Vector3f p = new Vector3f();
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n;
            float a1 = (i + 1) * Mth.TWO_PI / n;
            renkKosesi(bb, m, duzlem(0f, 0f, 0f, p), r, g, b, a);
            renkKosesi(bb, m, duzlem(Mth.cos(a0) * y, Mth.sin(a0) * y, 0f, p), r, g, b, a);
            renkKosesi(bb, m, duzlem(Mth.cos(a1) * y, Mth.sin(a1) * y, 0f, p), r, g, b, a);
            renkKosesi(bb, m, duzlem(0f, 0f, 0f, p), r, g, b, a);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /** Görüntü düzleminde ince halka şeridi (yarıçap ve yarı genişlik rs). */
    private static void halka(Matrix4f m, float yaricap, float yariGenislik, float r, float g, float b, float a) {
        if (a <= 0.002f) {
            return;
        }
        int n = kademe.dilim();
        float ic = yaricap - yariGenislik;
        float dis = yaricap + yariGenislik;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, HALKA);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Vector3f p = new Vector3f();
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n;
            float a1 = (i + 1) * Mth.TWO_PI / n;
            float u0 = (float) i / n;
            float u1 = (float) (i + 1) / n;
            dokuKosesi(bb, m, duzlem(Mth.cos(a0) * ic, Mth.sin(a0) * ic, 0f, p), u0, 0f, r, g, b, a);
            dokuKosesi(bb, m, duzlem(Mth.cos(a0) * dis, Mth.sin(a0) * dis, 0f, p), u0, 1f, r, g, b, a);
            dokuKosesi(bb, m, duzlem(Mth.cos(a1) * dis, Mth.sin(a1) * dis, 0f, p), u1, 1f, r, g, b, a);
            dokuKosesi(bb, m, duzlem(Mth.cos(a1) * ic, Mth.sin(a1) * ic, 0f, p), u1, 0f, r, g, b, a);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /** Görüntü düzleminde merkezli dokulu levha (yarı boylar gök birimi; u sağa, v yukarı); köşeler küreye çekilir. */
    private static void levha(Matrix4f m, ResourceLocation doku, float yariA, float yariV, float r, float g, float b,
            float a) {
        if (a <= 0.002f) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, doku);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Vector3f p = new Vector3f();
        for (int q = 0; q < 4; q++) {
            float su = q == 1 || q == 2 ? 1f : -1f;
            float sv = q >= 2 ? 1f : -1f;
            p.set(C).add(A.x * su * yariA + V.x * sv * yariV, A.y * su * yariA + V.y * sv * yariV,
                    A.z * su * yariA + V.z * sv * yariV);
            dokuKosesi(bb, m, kureye(p), su > 0f ? 1f : 0f, sv > 0f ? 1f : 0f, r, g, b, a);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /**
     * Parlamanın altında koyu silüet (normal karışım, PL-27, PL-41 tam opak): iki kutup jeti, disk ve gölge, foton halkası ve
     * dıştaki Einstein halkası (disk yarıçapının {@link KaraDelikMercek#YILDIZ_EINSTEIN} katı).
     */
    private static void siluet(Matrix4f m, float akis, float a) {
        if (a <= 0.003f) {
            return;
        }
        float dis = KaraDelikMercek.DISK_DIS_RS * rs;
        normal();
        levha(m, JET, dis * 0.22f, dis * 3.7f, SILUET[0], SILUET[1], SILUET[2], a);
        diskGecisi(m, TUMU, a, akis, 0f, SILUET, GECIS_ANA);
        golge(m, SILUET[0], SILUET[1], SILUET[2], a);
        halka(m, KaraDelikMercek.FOTON_RS, 0.25f, SILUET[0], SILUET[1], SILUET[2], a);
        halka(m, KaraDelikMercek.YILDIZ_EINSTEIN * KaraDelikMercek.DISK_DIS_RS, 0.6f, SILUET[0], SILUET[1], SILUET[2],
                a);
        toplamali();
    }

    // ------------------------------------------------------------------ yardımcılar

    private static void dokuKosesi(BufferBuilder bb, Matrix4f m, Vector3f p, float u, float v, float r, float g, float b,
            float a) {
        bb.vertex(m, p.x, p.y, p.z).uv(u, v).color(r, g, b, a).endVertex();
        koseSayisi++;
    }

    private static void renkKosesi(BufferBuilder bb, Matrix4f m, Vector3f p, float r, float g, float b, float a) {
        bb.vertex(m, p.x, p.y, p.z).color(r, g, b, a).endVertex();
        koseSayisi++;
    }

    private static void normal() {
        RenderSystem.defaultBlendFunc();
    }

    private static void toplamali() {
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }
}
