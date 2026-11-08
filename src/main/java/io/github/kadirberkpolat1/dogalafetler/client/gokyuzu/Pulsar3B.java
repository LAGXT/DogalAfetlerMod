package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarStrob;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarTitresimi;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 3B pulsar (pulsar-2 tur 4, PL-28–PL-30). Dünya pozunda (yalnız kamera dönüşü, dünya eksenleri) gerçek 3B geometri:
 * kenarı kararan (limb darkening) dokulu çekirdek küre ({@value #CEKIRDEK_DERECE}°; merkez buz mavisi-beyaz, kenar doygun
 * mavi/mor ve koyu, yüzeyde yavaş kaynayan granülasyon), hacimsel hale (3 eksenli kesişen bilboard + kameraya dönük
 * derinlik katmanları, ~{@value #HALE_DERECE}°; korona ≥ 52°), dipol manyetik alan çizgileri ({@code r = L·sin²θ},
 * toplamalı şeritler), manyetik eksen boyunca iki ışın konisi (yalnız iki hüzme: gökte yatay eksen, ekranda pulsarın
 * sağında ve solunda; iç parlak dış saydam, gök kubbesini geçer) ve konilerin kutup uçlarından dışa saçılan plazma
 * parçacıkları. {@code merkez} gök çapasından gelir ve {@value #UZAKLIK} birime ölçeklenir: kamera merkezli ölçek
 * açıları ve paralaksı değiştirmez, derinlikli katmanlar uzak düzlemi aşmaz.
 *
 * <p>Sıra: hale, korona, çatırtı parlaması ve çekirdeğin arkasındaki alan/koni parçaları önce (toplamalı) çizilir; opak
 * küre (normal karışım) onları örter, hale kürenin arkasından taşan bir parlama olarak kalır. Öndeki katmanların küre
 * diskine düşen köşeleri kısılır ({@value #MASKE_TABAN}): çekirdek doymuş beyaz bir lekeye dönmez.
 *
 * <p>Manyetik eksen gök çerçevesine sabittir (günlük gök dönüşüyle döner, kameradan bağımsızdır: oyuncu yana gidince
 * koniler başka açıdan görünür); pulsar kendi ekseninde dönmez (yüzey dokusunun iki katmanı zıt yönlere kayar: dönme
 * değil kaynama). Titreşim ({@link PulsarTitresimi}, gerçek zaman) çekirdek, koniler ve alan çizgilerini birlikte yana
 * kaydırır; koni parlaklığı {@link PulsarStrob} ile sesle aynı vuruş fazındadır. Girişte karışım toplamalı olmalıdır
 * (gök aşaması); çıkışta yine toplamalıdır. Yalnız render thread'i çağırır.
 */
public final class Pulsar3B {
    private static final ResourceLocation KONI = doku("pulsar3b_koni");
    private static final ResourceLocation PARCACIK = doku("pulsar3b_parcacik");
    private static final ResourceLocation HALE = doku("pulsar3b_hale_katman");
    private static final ResourceLocation ALAN = doku("pulsar3b_alan");
    private static final ResourceLocation YUZEY = doku("pulsar3b_yuzey");
    private static final ResourceLocation KORONA = doku("pulsar_korona");
    private static final ResourceLocation PARLAMA = doku("hale");

    /** Merkezin ölçeklendiği uzaklık: alan çizgileri ve hale derinliğiyle birlikte uzak düzlemin (128) içinde kalır. */
    static final float UZAKLIK = 50f;
    /** Uzak köşeler kamera yönünde bu yarıçapa çekilir: ekrandaki yerleri aynı kalır, uzak düzlemi aşmaz. */
    private static final float KUBBE = 100f;
    public static final float CEKIRDEK_DERECE = 10f;
    public static final float HALE_DERECE = PulsarTitresimi.KADRAJ_DERECE;
    public static final float KORONA_DERECE = 56f;
    /** Hale ve korona dokularında görünür alfanın kuad yarı boyuna oranı. */
    private static final float HALE_DOKU = 0.84f;
    private static final float KORONA_DOKU = 0.9f;
    /** Işın konisi boyu ({@link #UZAKLIK} katı), taban yarıçapları (çekirdek yarıçapı katı) ve yarı açıları (derece). */
    private static final float ISIN_BOY = 5.8f;
    private static final float IC_KONI_TABAN = 0.22f;
    private static final float DIS_KONI_TABAN = 0.45f;
    private static final float IC_KONI_ACI = 1.8f;
    private static final float DIS_KONI_ACI = 5f;
    /** Dipol kabuklarının ekvator yarıçapı (çekirdek yarıçapı katı); çizgiler sırayla bu kabuklara dağılır. */
    private static final float[] ALAN_L = {1.9f, 2.6f, 3.4f};
    private static final float ALAN_GENISLIK = 0.12f;
    /** Parçacık ömrü (sn): her parçacık kutuptan dışa akıp söner, yeni nesli karmayla yeniden doğar. */
    private static final float OMUR = 2.4f;
    /**
     * Manyetik eksen gök çerçevesinde: güneş (pulsar) yerel +Y'de, teğet düzlemde {@code aci} derece ve gözlemciye
     * doğru {@code egim} derece yatık (bir koni yakın, öteki uzak: perspektif). 90° yerel X'tir: gök dönüşünün ekseni,
     * dünyada kuzey–güney; pulsar doğu–batı yolunda olduğundan bakışa dik ve yatay kalır, iki koni ekranda sağa ve sola
     * uzanır (üstte ve altta hüzme yok).
     */
    private static final Vector3f EKSEN = gokEkseni(90f, 4f);
    private static final float[] SOGUK = {219 / 255f, 239 / 255f, 247 / 255f};
    private static final float[] SICAK = {0.90f, 0.80f, 1f};
    private static final float[] BEYAZ = {1f, 1f, 1f};
    private static final float[] ALAN_SOGUK = {0.62f, 0.74f, 1f};
    private static final float[] ALAN_SICAK = {0.80f, 0.66f, 1f};
    /**
     * Çekirdek küre: merkez (bakışa dik yüzey) ve kenar renkleri, soğuk → sıcak. Kenar renginin parlaklığı merkezinkinin
     * ~%60'ı (görünen dış halka merkezden %20–25 koyu); kanallar ≤ 0,95: yüzey dokusunun tepesi (1,0) dışında doymaz.
     */
    private static final float[] CEKIRDEK_SOGUK = {0.86f, 0.93f, 0.95f};
    private static final float[] CEKIRDEK_SICAK = {0.92f, 0.86f, 0.95f};
    private static final float[] KENAR_SOGUK = {0.42f, 0.58f, 0.88f};
    private static final float[] KENAR_SICAK = {0.62f, 0.50f, 0.88f};
    /** Yüzey dokusunun ikinci katmanının ölçeği ve alfası (normal karışım). */
    private static final float YUZEY2_OLCEK = 1.7f;
    private static final float YUZEY2_ALFA = 0.45f;
    /** Öndeki toplamalı katmanların küre diski içindeki alfa çarpanı (diskin açısal yarıçapının 0,8–1,1 katında 1'e çıkar). */
    private static final float MASKE_TABAN = 0.2f;
    /** Kenar parlaması halkası: iç kenar (alfa 0), tepe ve dış kenar (alfa 0), çekirdek yarıçapı katı. */
    private static final float[] PARILTI_R = {0.97f, 1.08f, 2.1f};

    /** Kademe başına geometri; sıra {@code EfektKalitesi} ile aynı (Düşük, Orta, Yüksek, Ultra). */
    private record Kademe(int enlem, int boylam, int haleKatman, boolean ikiKopya, int alanCizgi, int alanParca,
            int koniDilim, int koniHalka, int parcacik) {
    }

    private static final Kademe[] KADEMELER = {
            new Kademe(8, 12, 2, false, 4, 16, 8, 4, 40),
            new Kademe(12, 18, 4, true, 8, 24, 12, 6, 120),
            new Kademe(16, 24, 6, true, 10, 32, 16, 8, 200),
            new Kademe(20, 32, 8, true, 12, 48, 24, 12, 300),
    };

    private static final float[] HIZLI = new float[2];
    private static final float[] YAVAS = new float[2];
    private static final float[] SON_OFSET = new float[2];
    private static int sonDortgen;
    private static int dortgen;

    private static final Vector3f YON = new Vector3f();
    private static final Vector3f U = new Vector3f();
    private static final Vector3f V = new Vector3f();
    private static final Vector3f SAKIN = new Vector3f();
    private static final Vector3f TITREK = new Vector3f();
    private static final Vector3f ZIT = new Vector3f();
    private static final Vector3f A1 = new Vector3f();
    private static final Vector3f E1 = new Vector3f();
    private static final Vector3f E2 = new Vector3f();
    private static final Vector3f TABAN = new Vector3f();
    private static final Vector3f YONLU = new Vector3f();
    private static final Vector3f GECICI = new Vector3f();
    private static final Vector3f SU = new Vector3f();
    private static final Vector3f SV = new Vector3f();
    /** Kürenin çerçevesi: kutup, kutba dik kameraya bakan eksen, üçüncü eksen (boylam dikişi kenarda kalır). */
    private static final Vector3f KP = new Vector3f();
    private static final Vector3f KW = new Vector3f();
    private static final Vector3f KQ = new Vector3f();

    /** Spagettileşme biçimi: çekim yönünde uzama, enine incelme, kara deliğe doğru kayma. */
    private static final Vector3f BICIM_H = new Vector3f();
    private static boolean bicimVar;
    private static float bicimUzun;
    private static float bicimInce;
    private static float bicimKayma;

    /** Ön katman maskesi: küre merkezinin yönü ve açısal yarıçapının sinüsü (0: maske kapalı). */
    private static final Vector3f MASKE = new Vector3f();
    private static float maskeSin;

    private static int kureEnlem = -1;
    private static int kureBoylam = -1;
    private static float[] kureX = new float[0];
    private static float[] kureY = new float[0];
    private static float[] kureZ = new float[0];
    private static float[] kureU = new float[0];
    private static float[] kureV = new float[0];
    private static float[] knx = new float[0];
    private static float[] kny = new float[0];
    private static float[] knz = new float[0];
    private static float[] kpx = new float[0];
    private static float[] kpy = new float[0];
    private static float[] kpz = new float[0];
    private static float[] kmu = new float[0];
    private static int cemberDilim = -1;
    private static float[] cemberCos = new float[0];
    private static float[] cemberSin = new float[0];
    private static final int ALAN_EN_COK = 48;
    private static final float[] apx = new float[ALAN_EN_COK + 1];
    private static final float[] apy = new float[ALAN_EN_COK + 1];
    private static final float[] apz = new float[ALAN_EN_COK + 1];
    private static final float[] asx = new float[ALAN_EN_COK + 1];
    private static final float[] asy = new float[ALAN_EN_COK + 1];
    private static final float[] asz = new float[ALAN_EN_COK + 1];
    private static final float[] aal = new float[ALAN_EN_COK + 1];

    private Pulsar3B() {
    }

    private static ResourceLocation doku(String ad) {
        return DogalAfetler.konum("textures/environment/gokyuzu/" + ad + ".png");
    }

    private static Vector3f gokEkseni(float aci, float egim) {
        float a = aci * Mth.DEG_TO_RAD;
        float e = egim * Mth.DEG_TO_RAD;
        return new Vector3f(Mth.sin(a) * Mth.cos(e), -Mth.sin(e), Mth.cos(a) * Mth.cos(e)).normalize();
    }

    /**
     * Son karede çekirdeğin titreşim ofseti {x, y} derece; {@code d} pulsarın bakış yönü olmak üzere x ekseni
     * {@code normalize(d × (0,1,0))} (ekran sağı), y ekseni {@code x × d} (yukarı). |d.y| > 0,9'da başvuru (1,0,0) olur.
     */
    public static float[] sonOfsetDerece() {
        return new float[] {SON_OFSET[0], SON_OFSET[1]};
    }

    /** Son çizimde (pulsar ya da tek başına küre) gönderilen dörtgen sayısı. */
    public static int sonDortgen() {
        return sonDortgen;
    }

    /** Pulsarsız kare: dörtgen sayısı 0 (otomasyon satırı önceki karenin sayısını göstermesin). */
    static void bosKare() {
        sonDortgen = 0;
    }

    /**
     * Pulsarı çizer. {@code dunya}: kamera dönüşü pozu; {@code merkez}: gök çapasının kameraya göre konumu; {@code guc}
     * 0–1 (ısınma: buz mavisinden mor-beyaza); {@code titresimCarpani} yutmadaki zirve çarpanı (normalde 1);
     * {@code cekimHedefi} kara deliğin merkezi (aynı çerçeve; null: yok), {@code spagetti} 0–1 çekirdeğin ona doğru uzaması.
     */
    public static void ciz(Matrix4f dunya, Vector3f merkez, float guc, float alfa, float titresimCarpani,
            @Nullable Vector3f cekimHedefi, float spagetti) {
        dortgen = 0;
        maskeSin = 0f;
        if (!(alfa > 0.002f) || merkez.lengthSquared() < 1e-8f) {
            SON_OFSET[0] = 0f;
            SON_OFSET[1] = 0f;
            sonDortgen = 0;
            return;
        }
        Kademe k = KADEMELER[IstemciAyarlari.efektKalitesi().ordinal()];
        boolean dusuk = k == KADEMELER[0];
        guc = kenetle(guc);
        spagetti = kenetle(spagetti);
        float a = Math.min(1f, alfa);
        double t = Util.getNanos() * 1e-9;
        float ayar = kenetle((float) GokyuzuAyarlari.pulsarTitresim());
        PulsarTitresimi.yavasOfset(t, guc, titresimCarpani, YAVAS);
        PulsarTitresimi.hizliOfset(t, guc, ayar, dusuk, titresimCarpani, HIZLI);
        float catirti = PulsarTitresimi.catirti(t) * ayar;
        float vurus = PulsarSesi.vurusSaniyesi();
        double vurusSn = Float.isNaN(vurus) ? t % PulsarStrob.DONGU_SN : vurus;
        float koni = PulsarStrob.koniParlakligi(vurusSn, ayar, dusuk);
        SON_OFSET[0] = YAVAS[0] + HIZLI[0];
        SON_OFSET[1] = YAVAS[1] + HIZLI[1];

        float olcek = UZAKLIK / merkez.length();
        YON.set(merkez).normalize();
        teget(YON, U, V);
        float derece = UZAKLIK * Mth.DEG_TO_RAD;
        SAKIN.set(YON).mul(UZAKLIK).fma(YAVAS[0] * derece, U).fma(YAVAS[1] * derece, V);
        TITREK.set(YON).mul(UZAKLIK).fma(SON_OFSET[0] * derece, U).fma(SON_OFSET[1] * derece, V);
        ZIT.set(YON).mul(UZAKLIK).fma((YAVAS[0] - HIZLI[0]) * derece, U).fma((YAVAS[1] - HIZLI[1]) * derece, V);
        manyetikCerceve();
        float r = UZAKLIK * Mth.sin(CEKIRDEK_DERECE * 0.5f * Mth.DEG_TO_RAD);
        bicimKur(cekimHedefi, olcek, spagetti, r);

        float[] c = GokyuzuEgrileri.karistir(SOGUK, SICAK, guc);
        float[] parlak = GokyuzuEgrileri.karistir(c, BEYAZ, 0.5f);
        float[] alanRenk = GokyuzuEgrileri.karistir(ALAN_SOGUK, ALAN_SICAK, guc);
        float isik = 0.8f + 0.2f * guc;
        float sonum = 1f - 0.5f * spagetti;
        float isinA = a * (0.7f + 0.3f * guc) * (1f + 0.6f * catirti) * sonum * koni;
        float alanA = a * 0.45f * isik * sonum;
        float kayma = (float) (t * 0.35 % 1.0);
        float boy = ISIN_BOY * UZAKLIK;

        BufferBuilder bb = basla(KORONA);
        float korona = UZAKLIK * (float) Math.tan(KORONA_DERECE * 0.5f * Mth.DEG_TO_RAD) / KORONA_DOKU;
        SU.set(U).mul(korona);
        SV.set(V).mul(korona);
        bilboard(bb, dunya, SAKIN, SU, SV, true, c, a * (0.36f + 0.28f * guc) * (1f - 0.6f * spagetti));
        bitir(bb);

        bb = basla(HALE);
        float haleA = a * isik * (k.ikiKopya ? 0.4f : 0.65f);
        hale(bb, dunya, TITREK, k, c, haleA);
        if (k.ikiKopya) {
            hale(bb, dunya, ZIT, k, c, haleA);
        }
        bitir(bb);
        if (catirti > 0.004f) {
            bb = basla(PARLAMA);
            float patlama = 0.4224f * UZAKLIK;
            SU.set(U).mul(patlama);
            SV.set(V).mul(patlama);
            bilboard(bb, dunya, TITREK, SU, SV, false, BEYAZ, a * catirti * 0.9f);
            bitir(bb);
        }

        bb = basla(ALAN);
        alanCizgileri(bb, dunya, k, r, true, kayma, alanRenk, alanA);
        bitir(bb);
        bb = basla(KONI);
        koniCifti(bb, dunya, A1, E1, E2, boy, r, k, true, c, parlak, isinA);
        bitir(bb);

        kureHazirla(k, TITREK, r, A1, true);
        float[] ic = GokyuzuEgrileri.karistir(CEKIRDEK_SOGUK, CEKIRDEK_SICAK, guc);
        float[] kenar = GokyuzuEgrileri.karistir(KENAR_SOGUK, KENAR_SICAK, guc);
        RenderSystem.defaultBlendFunc();
        dortgen += kureYuzeyGecisi(dunya, k, ic, kenar, a, 1f, (float) (t * 0.011 % 1.0), (float) (t * 0.017 % 1.0));
        dortgen += kureYuzeyGecisi(dunya, k, ic, kenar, a * YUZEY2_ALFA, YUZEY2_OLCEK, (float) (-t * 0.014 % 1.0),
                (float) (-t * 0.009 % 1.0));
        toplamaliKarisim();
        dortgen += kenarParilti(dunya, TITREK, r, k.boylam, parlak, a * 0.25f * (0.75f + 0.25f * koni));

        MASKE.set(TITREK).normalize();
        maskeSin = r / TITREK.length();
        bb = basla(ALAN);
        alanCizgileri(bb, dunya, k, r, false, kayma, alanRenk, alanA);
        bitir(bb);
        bb = basla(KONI);
        koniCifti(bb, dunya, A1, E1, E2, boy, r, k, false, c, parlak, isinA);
        bitir(bb);
        bb = basla(PARCACIK);
        parcaciklar(bb, dunya, t, k, r, boy, GokyuzuEgrileri.karistir(c, BEYAZ, 0.4f),
                a * 0.8f * (0.55f + 0.45f * koni) * sonum);
        bitir(bb);
        maskeSin = 0f;
        bicimVar = false;
        sonDortgen = dortgen;
    }

    /**
     * Tek geçişli küre (normal karışım, örter; sonra toplamalı karışıma döner): bakışa dik kısımlar {@code icRenk}, kenara
     * doğru {@code kenarRenk}. Yalnız kameraya bakan yüzler gönderilir; dilim sayısı kademeden. {@code yariBirim}
     * {@code merkez} ile aynı ölçekte yarıçap.
     */
    public static void kure(Matrix4f dunya, Vector3f merkez, float yariBirim, float[] icRenk, float[] kenarRenk,
            float alfa) {
        if (!(alfa > 0.002f) || !(yariBirim > 0f) || merkez.lengthSquared() < 1e-8f) {
            sonDortgen = 0;
            return;
        }
        Kademe k = KADEMELER[IstemciAyarlari.efektKalitesi().ordinal()];
        teget(YON.set(merkez).normalize(), U, V);
        kureHazirla(k, merkez, yariBirim, V, false);
        RenderSystem.defaultBlendFunc();
        sonDortgen = kureRenkGecisi(dunya, k, icRenk, kenarRenk, Math.min(1f, alfa));
        toplamaliKarisim();
    }

    // ------------------------------------------------------------------ çerçeveler

    /** {@code yon}'a teğet birim eksenler: {@code u = yon × başvuru} (ekran sağı, titreşimin x'i), {@code v = u × yon}. */
    private static void teget(Vector3f yon, Vector3f u, Vector3f v) {
        u.set(Math.abs(yon.y) > 0.9f ? 1f : 0f, Math.abs(yon.y) > 0.9f ? 0f : 1f, 0f).cross(yon).negate().normalize();
        v.set(u).cross(yon).normalize();
    }

    /** {@code a}'ya dik birim eksenler. */
    private static void dik(Vector3f a, Vector3f e1, Vector3f e2) {
        e1.set(Math.abs(a.y) < 0.9f ? 0f : 1f, Math.abs(a.y) < 0.9f ? 1f : 0f, 0f).cross(a).normalize();
        e2.set(a).cross(e1).normalize();
    }

    /** Gök çerçevesindeki manyetik eksenin bu karedeki dünya yönü (güneş yolu dönüşüyle aynı). */
    private static void manyetikCerceve() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        float zaman = level == null ? 0f : level.getTimeOfDay(mc.getPartialTick());
        Quaternionf q = new Quaternionf(Axis.YP.rotationDegrees(-90f)).mul(Axis.XP.rotationDegrees(zaman * 360f));
        q.transform(A1.set(EKSEN));
        dik(A1, E1, E2);
    }

    private static void bicimKur(@Nullable Vector3f hedef, float olcek, float spagetti, float r) {
        bicimVar = false;
        if (hedef == null || spagetti <= 0.001f) {
            return;
        }
        BICIM_H.set(hedef).mul(olcek).sub(YON.x * UZAKLIK, YON.y * UZAKLIK, YON.z * UZAKLIK);
        if (BICIM_H.lengthSquared() < 1e-8f) {
            return;
        }
        BICIM_H.normalize();
        bicimUzun = 1f + 1.3f * spagetti;
        bicimInce = 1f - 0.35f * spagetti;
        bicimKayma = r * (bicimUzun - 1f) * 0.6f;
        bicimVar = true;
    }

    /** Merkeze göre yerel noktayı spagetti biçimine sokar: kara delik tarafı daha çok uzar (damla). */
    private static void bicim(float x, float y, float z, Vector3f cikti) {
        if (!bicimVar) {
            cikti.set(x, y, z);
            return;
        }
        float hx = BICIM_H.x;
        float hy = BICIM_H.y;
        float hz = BICIM_H.z;
        float d = x * hx + y * hy + z * hz;
        float uzun = d > 0f ? bicimUzun : 1f + (bicimUzun - 1f) * 0.35f;
        float boyuna = d * uzun + bicimKayma;
        cikti.set((x - hx * d) * bicimInce + hx * boyuna, (y - hy * d) * bicimInce + hy * boyuna,
                (z - hz * d) * bicimInce + hz * boyuna);
    }

    // ------------------------------------------------------------------ parçalar

    /**
     * Hacimsel hale: manyetik çerçevede kesişen üç düzlem (kameraya kenardan bakan düzlem çizgi gibi parlamasın diye
     * bakışla söner) ve bakış ekseni boyunca derinlik katmanları (küre kesiti gibi daralan).
     */
    private static void hale(BufferBuilder bb, Matrix4f m, Vector3f merkez, Kademe k, float[] c, float a) {
        float yari = UZAKLIK * (float) Math.tan(HALE_DERECE * 0.5f * Mth.DEG_TO_RAD) / HALE_DOKU;
        float eksenA = a * 0.3f;
        eksenDuzlemi(bb, m, merkez, E1, E2, A1, yari, c, eksenA);
        eksenDuzlemi(bb, m, merkez, A1, E1, E2, yari, c, eksenA);
        eksenDuzlemi(bb, m, merkez, A1, E2, E1, yari, c, eksenA);
        int n = k.haleKatman;
        float katmanA = a * 0.7f / n;
        float derin = yari * HALE_DOKU;
        for (int j = 0; j < n; j++) {
            float z = ((j + 0.5f) / n * 2f - 1f) * 0.65f * derin;
            float s = yari * (float) Math.sqrt(Math.max(0.05f, 1f - (z / derin) * (z / derin)));
            YONLU.set(merkez).fma(z, YON);
            SU.set(U).mul(s);
            SV.set(V).mul(s);
            bilboard(bb, m, YONLU, SU, SV, true, c, katmanA);
        }
    }

    private static void eksenDuzlemi(BufferBuilder bb, Matrix4f m, Vector3f merkez, Vector3f p, Vector3f q,
            Vector3f normal, float yari, float[] c, float a) {
        float bakis = (float) Math.pow(Math.abs(normal.dot(YON)), 0.8);
        SU.set(p).mul(yari);
        SV.set(q).mul(yari);
        bilboard(bb, m, merkez, SU, SV, true, c, a * bakis);
    }

    /**
     * Dipol alan çizgileri (manyetik eksen çevresinde, toplamalı şeritler): {@code r = L·sin²θ}, θ çekirdek yüzeyinden
     * yüzeyine. Şerit kameraya dönüktür; doku çizgi boyunca kayar (akan plazma). {@code uzak}: yalnız çekirdeğin
     * arkasındaki (önce çizilip çekirdekle örtülen) ya da önündeki parçalar.
     */
    private static void alanCizgileri(BufferBuilder bb, Matrix4f m, Kademe k, float r, boolean uzak, float kayma,
            float[] c, float a) {
        if (a <= 0.002f) {
            return;
        }
        int n = k.alanCizgi;
        int parca = Math.min(ALAN_EN_COK, k.alanParca);
        float genislik = r * ALAN_GENISLIK;
        for (int i = 0; i < n; i++) {
            float fi = Mth.TWO_PI * i / n + 0.37f;
            float cf = Mth.cos(fi);
            float sf = Mth.sin(fi);
            float l = r * ALAN_L[i % ALAN_L.length];
            float t0 = (float) Math.asin(Math.sqrt(Math.min(0.98f, 1.04f * r / l)));
            float aralik = Mth.PI - 2f * t0;
            for (int s = 0; s <= parca; s++) {
                float teta = t0 + aralik * s / parca;
                float st = Mth.sin(teta);
                float yar = l * st * st;
                float ra = yar * st;
                float ax = yar * Mth.cos(teta);
                bicim((E1.x * cf + E2.x * sf) * ra + A1.x * ax, (E1.y * cf + E2.y * sf) * ra + A1.y * ax,
                        (E1.z * cf + E2.z * sf) * ra + A1.z * ax, GECICI);
                apx[s] = TITREK.x + GECICI.x;
                apy[s] = TITREK.y + GECICI.y;
                apz[s] = TITREK.z + GECICI.z;
                aal[s] = (float) Math.pow(Mth.sin(Mth.PI * s / parca), 0.6) * (0.4f + 0.6f * st);
            }
            for (int s = 0; s <= parca; s++) {
                int on = Math.min(parca, s + 1);
                int arka = Math.max(0, s - 1);
                GECICI.set(apx[on] - apx[arka], apy[on] - apy[arka], apz[on] - apz[arka])
                        .cross(apx[s], apy[s], apz[s]);
                float uz = GECICI.length();
                float w = uz < 1e-6f ? 0f : genislik * (0.5f + 0.5f * aal[s]) / uz;
                asx[s] = GECICI.x * w;
                asy[s] = GECICI.y * w;
                asz[s] = GECICI.z * w;
            }
            for (int s = 0; s < parca; s++) {
                float derinlik = ((apx[s] + apx[s + 1]) * 0.5f - TITREK.x) * YON.x
                        + ((apy[s] + apy[s + 1]) * 0.5f - TITREK.y) * YON.y
                        + ((apz[s] + apz[s + 1]) * 0.5f - TITREK.z) * YON.z;
                if (derinlik > 0f != uzak) {
                    continue;
                }
                float u0 = 2f * s / parca + kayma;
                float u1 = 2f * (s + 1) / parca + kayma;
                int e = s + 1;
                kose(bb, m, apx[s] - asx[s], apy[s] - asy[s], apz[s] - asz[s], u0, 0f, c, a * aal[s]);
                kose(bb, m, apx[e] - asx[e], apy[e] - asy[e], apz[e] - asz[e], u1, 0f, c, a * aal[e]);
                kose(bb, m, apx[e] + asx[e], apy[e] + asy[e], apz[e] + asz[e], u1, 1f, c, a * aal[e]);
                kose(bb, m, apx[s] + asx[s], apy[s] + asy[s], apz[s] + asz[s], u0, 1f, c, a * aal[s]);
                dortgen++;
            }
        }
    }

    /**
     * Manyetik eksen {@code a} boyunca iki koni (kutuplardan zıt yönlere); her biri dar parlak iç ve geniş soluk dış
     * katman. {@code uzak}: yalnız kameradan uzağa bakan (çekirdekten önce çizilen) ya da yaklaşan koni.
     */
    private static void koniCifti(BufferBuilder bb, Matrix4f m, Vector3f a, Vector3f e1, Vector3f e2, float boy,
            float r, Kademe k, boolean uzak, float[] c, float[] parlak, float alfa) {
        if (alfa <= 0.002f) {
            return;
        }
        cemberTablosu(k.koniDilim);
        for (int isaret = -1; isaret <= 1; isaret += 2) {
            YONLU.set(a).mul(isaret);
            if (YONLU.dot(YON) > 0f != uzak) {
                continue;
            }
            bicim(YONLU.x * r * 0.85f, YONLU.y * r * 0.85f, YONLU.z * r * 0.85f, GECICI);
            TABAN.set(TITREK).add(GECICI);
            float l = koniBoyu(YONLU, boy);
            koni(bb, m, l, r * DIS_KONI_TABAN, DIS_KONI_ACI, e1, e2, k, c, alfa * 0.4f);
            koni(bb, m, l, r * IC_KONI_TABAN, IC_KONI_ACI, e1, e2, k, parlak, alfa * 0.95f);
        }
    }

    /**
     * Kameraya yaklaşan koni kameranın yanından arkaya geçmesin: ucu bakış ekseninde merkezin en az %15'i önde kalır.
     * Pulsar yutmada güneş yolunda 40° kayar; gök çerçevesine sabit eksen bakışa göre o kadar yatışabilir.
     */
    private static float koniBoyu(Vector3f yon, float boy) {
        float yaklasma = -yon.dot(YON);
        return yaklasma > 0f ? Math.min(boy, 0.85f * UZAKLIK / yaklasma) : boy;
    }

    /** {@link #TABAN}'dan {@link #YONLU} yönünde koni yüzeyi; halkalar tabana doğru sık (parlak kısım düzgün kalsın). */
    private static void koni(BufferBuilder bb, Matrix4f m, float boy, float taban, float yariAci, Vector3f e1,
            Vector3f e2, Kademe k, float[] c, float a) {
        float egim = (float) Math.tan(yariAci * Mth.DEG_TO_RAD);
        int dilim = k.koniDilim;
        int halka = k.koniHalka;
        for (int j = 0; j < halka; j++) {
            float u0 = (float) Math.pow((float) j / halka, 1.7);
            float u1 = (float) Math.pow((float) (j + 1) / halka, 1.7);
            float s0 = u0 * boy;
            float s1 = u1 * boy;
            float r0 = taban + s0 * egim;
            float r1 = taban + s1 * egim;
            for (int i = 0; i < dilim; i++) {
                koniKosesi(bb, m, s0, r0, i, u0, dilim, e1, e2, c, a);
                koniKosesi(bb, m, s1, r1, i, u1, dilim, e1, e2, c, a);
                koniKosesi(bb, m, s1, r1, i + 1, u1, dilim, e1, e2, c, a);
                koniKosesi(bb, m, s0, r0, i + 1, u0, dilim, e1, e2, c, a);
                dortgen++;
            }
        }
    }

    /**
     * Koni köşesi; alfa yüzey normalinin bakışla açısından: koninin ortası (bakışa dik yüzler) parlak, kenarı saydam.
     * Koni kameraya uçtan baktığında eksen terimi görünür tutar.
     */
    private static void koniKosesi(BufferBuilder bb, Matrix4f m, float s, float yaricap, int i, float u, int dilim,
            Vector3f e1, Vector3f e2, float[] c, float a) {
        float cs = cemberCos[i];
        float sn = cemberSin[i];
        float nx = e1.x * cs + e2.x * sn;
        float ny = e1.y * cs + e2.y * sn;
        float nz = e1.z * cs + e2.z * sn;
        float px = TABAN.x + YONLU.x * s + nx * yaricap;
        float py = TABAN.y + YONLU.y * s + ny * yaricap;
        float pz = TABAN.z + YONLU.z * s + nz * yaricap;
        float l = (float) Math.sqrt(px * px + py * py + pz * pz);
        float yuz = Math.abs(nx * px + ny * py + nz * pz) / l;
        float eksen = (YONLU.x * px + YONLU.y * py + YONLU.z * pz) / l;
        float bakis = Math.max((float) Math.pow(yuz, 1.3), eksen * eksen);
        kose(bb, m, px, py, pz, u, (float) i / dilim, c, a * bakis);
    }

    /**
     * Plazma parçacıkları: kutuplardan koniler boyunca dışa akar, koni dışına da saçılır. Konum yalnız zamandan ve
     * parçacık numarasından karmayla gelir (durum yok, deterministik); her ömür sonunda yeni nesil başka yöne doğar.
     */
    private static void parcaciklar(BufferBuilder bb, Matrix4f m, double t, Kademe k, float r, float boy, float[] c,
            float a) {
        if (a <= 0.002f) {
            return;
        }
        float egim = (float) Math.tan(DIS_KONI_ACI * Mth.DEG_TO_RAD);
        float taban = r * DIS_KONI_TABAN;
        for (int i = 0; i < k.parcacik; i++) {
            long h0 = karma(i * 0x632BE59BD9B4E019L + 7L);
            double tau = t / OMUR + birim(h0);
            long nesil = (long) Math.floor(tau);
            float x = (float) (tau - nesil);
            long h = karma(h0 ^ (nesil * 0x9E3779B97F4A7C15L));
            float isaret = (h & 1L) == 0L ? 1f : -1f;
            float hiz = 0.5f + 0.5f * birim(karma(h + 1L));
            float aci = Mth.TWO_PI * birim(karma(h + 2L));
            float sacilma = (float) Math.sqrt(birim(karma(h + 3L))) * 1.3f;
            float boyut = r * (0.16f + 0.2f * birim(karma(h + 4L))) * (1f + 0.5f * x);
            float boyu = koniBoyu(YONLU.set(A1).mul(isaret), boy);
            float s = r * 0.9f + boyu * 0.45f * hiz * (0.04f + 0.96f * (float) Math.pow(x, 0.8));
            float yaricap = (taban + s * egim) * sacilma * (1f + 0.8f * x);
            float cs = Mth.cos(aci) * yaricap;
            float sn = Mth.sin(aci) * yaricap;
            GECICI.set(TITREK).fma(isaret * s, A1).fma(cs, E1).fma(sn, E2);
            teget(YONLU.set(GECICI).normalize(), SU, SV);
            SU.mul(boyut);
            SV.mul(boyut);
            bilboard(bb, m, GECICI, SU, SV, false, c, a * (float) Math.pow(Mth.sin(Mth.PI * x), 0.7));
        }
    }

    /**
     * Kürenin siluetini yumuşatan toplamalı halka (çekirdek düzleminde): diskin içinde {@code PARILTI_R[0]}'da 0,
     * kenarın hemen dışında tepe, dışa doğru söner. Merkeze katkısı yoktur.
     */
    private static int kenarParilti(Matrix4f m, Vector3f merkez, float r, int dilim, float[] c, float a) {
        if (a <= 0.002f) {
            return 0;
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int sayi = 0;
        for (int bant = 0; bant < 2; bant++) {
            float ra = r * PARILTI_R[bant];
            float rb = r * PARILTI_R[bant + 1];
            float aa = bant == 0 ? 0f : a;
            float ab = bant == 0 ? a : 0f;
            for (int i = 0; i < dilim; i++) {
                float f0 = Mth.TWO_PI * i / dilim;
                float f1 = Mth.TWO_PI * (i + 1) / dilim;
                halkaKosesi(bb, m, merkez, f0, ra, c, aa);
                halkaKosesi(bb, m, merkez, f0, rb, c, ab);
                halkaKosesi(bb, m, merkez, f1, rb, c, ab);
                halkaKosesi(bb, m, merkez, f1, ra, c, aa);
                sayi++;
            }
        }
        BufferUploader.drawWithShader(bb.end());
        return sayi;
    }

    private static void halkaKosesi(BufferBuilder bb, Matrix4f m, Vector3f merkez, float fi, float yaricap, float[] c,
            float a) {
        float cs = Mth.cos(fi) * yaricap;
        float sn = Mth.sin(fi) * yaricap;
        bb.vertex(m, merkez.x + U.x * cs + V.x * sn, merkez.y + U.y * cs + V.y * sn, merkez.z + U.z * cs + V.z * sn)
                .color(c[0], c[1], c[2], Math.min(1f, a)).endVertex();
    }

    // ------------------------------------------------------------------ küre

    /**
     * Küre köşelerini hazırlar: kutup {@code kutup}, ön yüzün ortası kameraya bakar (boylam dikişi ve kutup büzülmesi
     * kenarda, kenar kararmasının altında kalır). {@code kmu}: yüzey normalinin bakışla kosinüsü (merkez 1, kenar 0).
     */
    private static void kureHazirla(Kademe k, Vector3f merkez, float yari, Vector3f kutup, boolean bicimli) {
        kureTablosu(k.enlem, k.boylam);
        KP.set(kutup);
        KW.set(merkez).normalize().negate();
        KW.fma(-KW.dot(KP), KP);
        if (KW.lengthSquared() < 1e-6f) {
            dik(KP, KW, KQ);
        } else {
            KQ.set(KW.normalize()).cross(KP).normalize();
        }
        int n = kureX.length;
        for (int i = 0; i < n; i++) {
            float x = kureX[i];
            float y = kureY[i];
            float z = kureZ[i];
            float nx = KQ.x * x + KP.x * y + KW.x * z;
            float ny = KQ.y * x + KP.y * y + KW.y * z;
            float nz = KQ.z * x + KP.z * y + KW.z * z;
            knx[i] = nx;
            kny[i] = ny;
            knz[i] = nz;
            if (bicimli) {
                bicim(nx * yari, ny * yari, nz * yari, GECICI);
            } else {
                GECICI.set(nx * yari, ny * yari, nz * yari);
            }
            float px = merkez.x + GECICI.x;
            float py = merkez.y + GECICI.y;
            float pz = merkez.z + GECICI.z;
            kpx[i] = px;
            kpy[i] = py;
            kpz[i] = pz;
            float l = (float) Math.sqrt(px * px + py * py + pz * pz);
            kmu[i] = Math.max(0f, Math.min(1f, -(nx * px + ny * py + nz * pz) / l));
        }
    }

    /** Çekirdek yüzeyi: kenar kararması (merkez {@code ic}, kenar {@code kenar}) × kayan döşenebilir yüzey dokusu. */
    private static int kureYuzeyGecisi(Matrix4f m, Kademe k, float[] ic, float[] kenar, float alfa, float olcek, float du,
            float dv) {
        if (alfa <= 0.002f) {
            return 0;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, YUZEY);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int sutun = k.boylam + 1;
        float a = Math.min(1f, alfa);
        int sayi = 0;
        for (int i = 0; i < k.enlem; i++) {
            for (int j = 0; j < k.boylam; j++) {
                int a0 = i * sutun + j;
                int b0 = a0 + sutun;
                if (!onYuz(a0, a0 + 1, b0, b0 + 1)) {
                    continue;
                }
                yuzeyKosesi(bb, m, a0, ic, kenar, a, olcek, du, dv);
                yuzeyKosesi(bb, m, b0, ic, kenar, a, olcek, du, dv);
                yuzeyKosesi(bb, m, b0 + 1, ic, kenar, a, olcek, du, dv);
                yuzeyKosesi(bb, m, a0 + 1, ic, kenar, a, olcek, du, dv);
                sayi++;
            }
        }
        BufferUploader.drawWithShader(bb.end());
        return sayi;
    }

    /** Boylam boyu enlemin iki katı: döşenebilir doku ekvatorda kare kalır. */
    private static void yuzeyKosesi(BufferBuilder bb, Matrix4f m, int i, float[] ic, float[] kenar, float a, float olcek,
            float du, float dv) {
        float t = (float) Math.pow(kmu[i], 0.8);
        bb.vertex(m, kpx[i], kpy[i], kpz[i]).uv(kureU[i] * 2f * olcek + du, kureV[i] * olcek + dv)
                .color(Mth.lerp(t, kenar[0], ic[0]), Mth.lerp(t, kenar[1], ic[1]), Mth.lerp(t, kenar[2], ic[2]), a)
                .endVertex();
    }

    /** {@link #kure} geçişi: renk merkezden kenara {@code (1 − μ)²} ile {@code ic} → {@code kenar}. */
    private static int kureRenkGecisi(Matrix4f m, Kademe k, float[] ic, float[] kenar, float alfa) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int sutun = k.boylam + 1;
        int sayi = 0;
        for (int i = 0; i < k.enlem; i++) {
            for (int j = 0; j < k.boylam; j++) {
                int a0 = i * sutun + j;
                int b0 = a0 + sutun;
                if (!onYuz(a0, a0 + 1, b0, b0 + 1)) {
                    continue;
                }
                renkKosesi(bb, m, a0, ic, kenar, alfa);
                renkKosesi(bb, m, b0, ic, kenar, alfa);
                renkKosesi(bb, m, b0 + 1, ic, kenar, alfa);
                renkKosesi(bb, m, a0 + 1, ic, kenar, alfa);
                sayi++;
            }
        }
        BufferUploader.drawWithShader(bb.end());
        return sayi;
    }

    private static void renkKosesi(BufferBuilder bb, Matrix4f m, int i, float[] ic, float[] kenar, float alfa) {
        float w = (1f - kmu[i]) * (1f - kmu[i]);
        bb.vertex(m, kpx[i], kpy[i], kpz[i]).color(Mth.lerp(w, ic[0], kenar[0]), Mth.lerp(w, ic[1], kenar[1]),
                Mth.lerp(w, ic[2], kenar[2]), alfa).endVertex();
    }

    /** Kameraya bakan yüz (siluetteki ince şerit de alınır: kenarda boşluk kalmasın). */
    private static boolean onYuz(int a, int b, int c, int d) {
        float nx = knx[a] + knx[b] + knx[c] + knx[d];
        float ny = kny[a] + kny[b] + kny[c] + kny[d];
        float nz = knz[a] + knz[b] + knz[c] + knz[d];
        float px = kpx[a] + kpx[b] + kpx[c] + kpx[d];
        float py = kpy[a] + kpy[b] + kpy[c] + kpy[d];
        float pz = kpz[a] + kpz[b] + kpz[c] + kpz[d];
        float nokta = nx * px + ny * py + nz * pz;
        return nokta < 0.08f * (float) Math.sqrt((nx * nx + ny * ny + nz * nz) * (px * px + py * py + pz * pz));
    }

    /** Birim küre tablosu: {@code kureY} kutup ekseni; {@code kureU} boylam (0–1), {@code kureV} enlem (0–1). */
    private static void kureTablosu(int enlem, int boylam) {
        if (enlem == kureEnlem && boylam == kureBoylam) {
            return;
        }
        int sutun = boylam + 1;
        int n = (enlem + 1) * sutun;
        kureX = new float[n];
        kureY = new float[n];
        kureZ = new float[n];
        kureU = new float[n];
        kureV = new float[n];
        knx = new float[n];
        kny = new float[n];
        knz = new float[n];
        kpx = new float[n];
        kpy = new float[n];
        kpz = new float[n];
        kmu = new float[n];
        for (int i = 0; i <= enlem; i++) {
            float teta = Mth.PI * i / enlem;
            float st = Mth.sin(teta);
            float ct = Mth.cos(teta);
            for (int j = 0; j <= boylam; j++) {
                float fi = Mth.TWO_PI * j / boylam;
                int x = i * sutun + j;
                kureX[x] = st * Mth.cos(fi);
                kureY[x] = ct;
                kureZ[x] = st * Mth.sin(fi);
                kureU[x] = (float) j / boylam;
                kureV[x] = (float) i / enlem;
            }
        }
        kureEnlem = enlem;
        kureBoylam = boylam;
    }

    private static void cemberTablosu(int dilim) {
        if (dilim == cemberDilim) {
            return;
        }
        cemberCos = new float[dilim + 1];
        cemberSin = new float[dilim + 1];
        for (int i = 0; i <= dilim; i++) {
            float fi = Mth.TWO_PI * i / dilim;
            cemberCos[i] = Mth.cos(fi);
            cemberSin[i] = Mth.sin(fi);
        }
        cemberDilim = dilim;
    }

    // ------------------------------------------------------------------ köşe yardımcıları

    private static BufferBuilder basla(ResourceLocation doku) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, doku);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        return bb;
    }

    private static void bitir(BufferBuilder bb) {
        BufferUploader.drawWithShader(bb.end());
    }

    private static void toplamaliKarisim() {
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    /** {@code merkez}'de yarı eksenleri {@code p}, {@code q} olan dokulu dörtgen; {@code bicimli} ise köşeler spagetti biçiminde. */
    private static void bilboard(BufferBuilder bb, Matrix4f m, Vector3f merkez, Vector3f p, Vector3f q, boolean bicimli,
            float[] c, float a) {
        if (a <= 0.002f) {
            return;
        }
        for (int j = 0; j < 4; j++) {
            float su = j == 1 || j == 2 ? 1f : -1f;
            float sv = j >= 2 ? 1f : -1f;
            float x = p.x * su + q.x * sv;
            float y = p.y * su + q.y * sv;
            float z = p.z * su + q.z * sv;
            if (bicimli) {
                bicim(x, y, z, GECICI);
                x = GECICI.x;
                y = GECICI.y;
                z = GECICI.z;
            }
            kose(bb, m, merkez.x + x, merkez.y + y, merkez.z + z, su > 0f ? 1f : 0f, sv > 0f ? 1f : 0f, c, a);
        }
        dortgen++;
    }

    /**
     * Dokulu köşe; {@value #KUBBE} birimden uzaksa kamera yönünde kubbeye çekilir. Alfa 1'e kenetlenir (bayt taşmasın);
     * maske açıksa küre diskine düşen köşenin alfası kısılır.
     */
    private static void kose(BufferBuilder bb, Matrix4f m, float x, float y, float z, float u, float v, float[] c,
            float a) {
        float l2 = x * x + y * y + z * z;
        if (maskeSin > 0f) {
            a *= diskMaskesi(x, y, z, l2);
        }
        if (l2 > KUBBE * KUBBE) {
            float s = KUBBE / (float) Math.sqrt(l2);
            x *= s;
            y *= s;
            z *= s;
        }
        bb.vertex(m, x, y, z).uv(u, v).color(c[0], c[1], c[2], Math.min(1f, Math.max(0f, a))).endVertex();
    }

    /** Köşenin küre merkezinden açısal uzaklığına göre {@value #MASKE_TABAN}–1 (diskin 0,8–1,1 katı arasında yumuşak). */
    private static float diskMaskesi(float x, float y, float z, float l2) {
        float d = x * MASKE.x + y * MASKE.y + z * MASKE.z;
        if (d <= 0f || l2 <= 0f) {
            return 1f;
        }
        float sin = (float) Math.sqrt(Math.max(0f, 1f - d * d / l2));
        float t = Mth.clamp((sin / maskeSin - 0.8f) / 0.3f, 0f, 1f);
        return MASKE_TABAN + (1f - MASKE_TABAN) * t * t * (3f - 2f * t);
    }

    private static float kenetle(float x) {
        return Float.isNaN(x) ? 0f : Math.max(0f, Math.min(1f, x));
    }

    /** SplitMix64 son karıştırması. */
    private static long karma(long x) {
        long z = x + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Karmanın üst 24 bitinden [0, 1). */
    private static float birim(long k) {
        return (k >>> 40) / (float) (1 << 24);
    }
}
