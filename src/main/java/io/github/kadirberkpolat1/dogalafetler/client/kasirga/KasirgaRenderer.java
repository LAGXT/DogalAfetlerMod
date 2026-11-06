package io.github.kadirberkpolat1.dogalafetler.client.kasirga;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaVarligi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.EtekProfili;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.HuniProfili;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaEvresi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaKaliteButcesi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.RankineProfili;
import io.github.kadirberkpolat1.dogalafetler.client.kasirga.IstemciKasirga.GorselEnkaz;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Kasırganın istemci katmanları (tasarım notu §2, spike SK-01/02/10): iki katmanlı prosedürel huni ağı, alt girdaplar, kubbe
 * biçimli taban enkaz eteği ve dönen toz, duvar bulutu (arkasında RFD yarığı) ve yalnız görsel enkaz kopyaları. Bulut
 * katmanlarının hepsi tek atlas ve tek render tipiyle aynı tampona yazılır: kare başına tek çizim çağrısı, yarı saydam
 * dörtgenler kameraya göre birlikte sıralanır; parçacık motoru kullanılmaz (16384 sınırı, parçacık tick'i yok). Enkaz
 * kopyaları vanilla blok modelleriyle opak blok tamponlarına gider. Dönüş siklonik: yukarıdan bakınca saat yönünün tersi.
 * Sprite boyutu kameraya uzaklığın {@link #EKRAN_ORANI} katıyla sınırlıdır: kamera huninin ya da eteğin içindeyken tek
 * sprite ekranı kaplamaz (doldurma oranı, spike B9); uzaktan görüntü değişmez. Yakın LOD eşiğinde sınır
 * {@link #EKRAN_ORANI_YAKIN}'a iner.
 */
final class KasirgaRenderer extends EntityRenderer<KasirgaVarligi> {
    /**
     * Toz ve etek renkleri doygun ve açık tutulur: fırtına ışığı (yağmurda gök ışığı ~%65, mavimsi) ve öndeki yağmur çizgileri
     * eklenince ekranda kahverengi kalmalı (R &gt; G &gt; B; K-17). Eski 0,52/0,42/0,31 vitrinde nötr gri görünüyordu.
     */
    private static final float[] TOZ = {0.80f, 0.56f, 0.30f};
    private static final float[] ETEK_ALT = {0.62f, 0.42f, 0.22f};
    private static final float[] ETEK_UST = {0.78f, 0.60f, 0.40f};
    private static final float[][] YAPRAK = {{0.30f, 0.48f, 0.22f}, {0.42f, 0.58f, 0.24f}, {0.50f, 0.38f, 0.18f}};
    private static final float PIKSEL = 1f / KasirgaDokusu.BOYUT;
    private static final double TAM_TUR = Math.PI * 2;
    /** Sprite yarı boyutu / kameraya uzaklık üst sınırı (~ 20° yarı açı). */
    private static final float EKRAN_ORANI = 0.36f;
    /** Yakın LOD'da (kamera eteğin içinde) sınır: yakındaki her sprite sınıra takılır, üst üste binme kare süresini belirler. */
    private static final float EKRAN_ORANI_YAKIN = 0.18f;
    /** Duvar bulutu sprite alfası (spike 0,88: üst üste binen 200 sprite GPU doldurma oranının en pahalı kalemiydi). */
    private static final double DUVAR_ALFA = 0.74;

    /** Son çizilen bulut geçişinin LOD kademesi (görsel otomasyonun kanıtı); yalnız render thread'i yazar ve okur. */
    private static double sonMesafe = -1;
    private static int sonTaban;
    private static int sonDuvar;
    private static int sonGirdap;

    private float ekranOrani = EKRAN_ORANI;
    private final BlockRenderDispatcher bloklar;

    private float[] noktaX = new float[0];
    private float[] noktaY = new float[0];
    private float[] noktaZ = new float[0];
    private float[] noktaA = new float[0];
    private float[] noktaU = new float[0];
    private float[] noktaNx = new float[0];
    private float[] noktaNz = new float[0];
    private float[] halkaR = new float[0];
    private float[] halkaG = new float[0];
    private float[] halkaB = new float[0];
    private float[] halkaV = new float[0];

    private float sagX;
    private float sagY;
    private float sagZ;
    private float ustX;
    private float ustY;
    private float ustZ;
    private float kamX;
    private float kamY;
    private float kamZ;
    private Matrix4f mat;
    private Matrix3f nor;
    private int isik;
    private VertexConsumer vc;

    KasirgaRenderer(EntityRendererProvider.Context baglam) {
        super(baglam);
        bloklar = baglam.getBlockRenderDispatcher();
        shadowRadius = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(KasirgaVarligi kasirga) {
        return KasirgaDokusu.KONUM;
    }

    @Override
    public boolean shouldRender(KasirgaVarligi kasirga, Frustum gorus, double x, double y, double z) {
        return kasirga.shouldRender(x, y, z) && gorus.isVisible(kasirga.getBoundingBoxForCulling());
    }

    /**
     * Varlık geçişinde yalnız opak enkaz kopyaları (derinlik yazar). Derinlik yazmayan bulut katmanlarını
     * {@link KasirgaBulutCizimi} {@code AFTER_PARTICLES}'ta çizer.
     */
    @Override
    public void render(KasirgaVarligi k, float yaw, float kismi, PoseStack poz, MultiBufferSource tampon, int paketliIsik) {
        if (IstemciKasirga.gizli) {
            return;
        }
        enkaz(k, k.seviyeBilgisi().rankine(), poz, tampon, IstemciKasirga.zaman(k, kismi), paketliIsik);
    }

    /** Yarı saydam bulut katmanları; {@code poz} kasırganın çizim konumuna taşınmış, tampon çağıran tarafından boşaltılır. */
    void bulut(KasirgaVarligi k, double ex, double ey, double ez, float kismi, PoseStack poz, MultiBufferSource tampon,
            int paketliIsik) {
        KasirgaDokusu.hazirla();
        Vec3 kamera = entityRenderDispatcher.camera.getPosition();
        kamX = (float) (kamera.x - ex);
        kamY = (float) (kamera.y - ey);
        kamZ = (float) (kamera.z - ez);
        double mesafe = Math.hypot(kamX, kamZ);
        sonMesafe = mesafe;
        Quaternionf yonelim = entityRenderDispatcher.cameraOrientation();
        Vector3f sag = yonelim.transform(new Vector3f(1, 0, 0));
        Vector3f ust = yonelim.transform(new Vector3f(0, 1, 0));
        sagX = sag.x();
        sagY = sag.y();
        sagZ = sag.z();
        ustX = ust.x();
        ustY = ust.y();
        ustZ = ust.z();

        KasirgaSeviyesi s = k.seviyeBilgisi();
        KasirgaKaliteButcesi butce = KasirgaKaliteButcesi.icin(IstemciKasirga.kalite());
        KasirgaKaliteButcesi.Lod lod = KasirgaAyarlari.lod();
        ekranOrani = lod.yakinda(mesafe) ? EKRAN_ORANI_YAKIN : EKRAN_ORANI;
        double t = IstemciKasirga.zaman(k, kismi);
        RankineProfili rankine = s.rankine();

        // Evreye göre biçim: siren evresinde (oluşum) yalnız duvar bulutu alçalır ve tabanda kısa bir huni bulutu sarkar;
        // doğuşta huni bulut tabanından iner, ip evresinde incelip kıvrılır, dağılırken solar.
        double inis = 1;
        double tabanOrani = 1;
        double huniAlfa = 1;
        double alfa = 1;
        double duvarAlfa = 1;
        double yaricapCarpani = 1;
        double kivrimCarpani = 1;
        double tepeCarpani = 1;
        float p = k.evreIlerleme();
        KasirgaEvresi evre = k.evre();
        switch (evre) {
            case OLUSUM -> {
                inis = 0;
                tabanOrani = 0;
                huniAlfa = 0.7 * p;
                yaricapCarpani = 0.5;
                duvarAlfa = 0.35 + 0.65 * p;
            }
            case DOGUS -> {
                inis = p;
                tabanOrani = Math.min(1, 0.3 + p);
                yaricapCarpani = 0.55 + 0.45 * p;
            }
            case IP -> {
                yaricapCarpani = 1 - 0.65 * p;
                kivrimCarpani = 1 + 3 * p;
                tabanOrani = 1 - 0.6 * p;
                tepeCarpani = 1 - 0.3 * p;
            }
            case DAGILMA -> {
                yaricapCarpani = 0.35;
                kivrimCarpani = 4;
                tabanOrani = 0.4 * (1 - p);
                tepeCarpani = 0.7;
                alfa = 1 - p;
                duvarAlfa = 1 - p;
            }
            default -> {
            }
        }

        PoseStack.Pose son = poz.last();
        mat = son.pose();
        nor = son.normal();
        isik = paketliIsik;
        vc = tampon.getBuffer(KasirgaRenderTipi.BULUT);

        double h = s.yukseklik();
        double alt = h * Math.max(s.altBoslukOrani(), (1 - inis) * 0.85);
        double rAlt = s.tabanYaricapi() * yaricapCarpani;
        double rUst = s.tepeYaricapi() * tepeCarpani;
        double kivrim = (0.15 * s.tabanYaricapi() + 1.0) * kivrimCarpani;
        float a = (s.seviye() - 1) / 4f;
        float hr = Mth.lerp(a, 0.84f, 0.40f);
        float hg = Mth.lerp(a, 0.84f, 0.40f);
        float hb = Mth.lerp(a, 0.82f, 0.39f);
        // S1 ince ve açık bir halat, S5 koyu ve yoğun bir kama: opaklık seviyeyle artar.
        double disAlfa = 0.55 + 0.35 * a;
        double icAlfa = 0.68 + 0.29 * a;
        int uTekrar = s.seviye() >= 3 ? 3 : 2;
        double kahve = 0.25 + 0.12 * s.seviye();
        double disOmega = rankine.acisalHiz(rAlt * 1.3);
        double icOmega = rankine.acisalHiz(rAlt * 0.8);

        if (huniAlfa * alfa > 0.01) {
            huniAgi(t, 0, 0, butce.huniDilim(), butce.huniHalka(), alt, h, rAlt, rUst, kivrim, 0,
                    uKaymasi(t, disOmega, uTekrar), uTekrar, KasirgaDokusu.HUNI_A_V0, KasirgaDokusu.HUNI_A_V1, hr, hg, hb,
                    (float) (disAlfa * alfa * huniAlfa), kahve);
            // İç katman: kamera huninin içindeyken atlanır (iki tam ekran katmanı üst üste binmesin; dış katman görünür kalır).
            if (mesafe > rAlt * 0.8) {
                huniAgi(t, 0, 0, butce.huniDilim(), butce.huniHalka(), alt, h, rAlt * 0.8, rUst * 0.8, kivrim, 0,
                        uKaymasi(t, icOmega, uTekrar) + 0.5, uTekrar, KasirgaDokusu.HUNI_B_V0, KasirgaDokusu.HUNI_B_V1,
                        hr * 0.78f, hg * 0.78f, hb * 0.80f, (float) (icAlfa * alfa * huniAlfa), kahve);
            }
        }
        int girdap = evre == KasirgaEvresi.OLGUN ? butce.altGirdapSayisi(s.seviye(), mesafe, lod) : 0;
        sonGirdap = girdap;
        sonTaban = 0;
        for (int g = 0; g < girdap; g++) {
            double faz = g * TAM_TUR / girdap - t * Math.PI;
            double yorunge = s.tabanYaricapi() * 1.2;
            huniAgi(t, Math.cos(faz) * yorunge, Math.sin(faz) * yorunge, 8, 8, 0, h * 0.32, 0.8, 2.4 + 0.02 * h, 1.2, g * 3.3,
                    t * 0.9, 1, KasirgaDokusu.HUNI_A_V0, KasirgaDokusu.HUNI_A_V1, Math.min(1, hr * 1.15f),
                    Math.min(1, hg * 1.15f), Math.min(1, hb * 1.15f), (float) (0.45 * alfa), 0.6);
        }
        if (tabanOrani > 0.01) {
            double hE = s.enkazYuksekligi();
            double rEtek = s.etekYaricapi() * (0.6 + 0.4 * tabanOrani);
            double rEtekUst = HuniProfili.yaricap(rAlt, rUst, Math.max(0, (hE - alt) / (h - alt))) * 1.05;
            // S1 seyrek bir toz girdabı, S5 huniyi saran kalın enkaz kılıfı.
            float etekAlfa = (float) ((0.45 + 0.55 * a) * alfa * Math.min(1, tabanOrani * 1.25));
            double etekOmega = rankine.acisalHiz(rEtek * 0.75);
            int dilim = butce.huniDilim();
            int halka = Math.max(4, butce.huniHalka() / 4);
            etekAgi(t, dilim, halka, rEtek, rEtekUst, hE, etekOmega, 0, etekAlfa);
            if (mesafe > rEtek * 0.82) {
                etekAgi(t, dilim, halka, rEtek * 0.82, rEtekUst * 0.9, hE * 0.85, etekOmega * 1.4, 0.5, etekAlfa * 0.7f);
            }
            int adet = (int) (butce.tabanSpriteSayisi(s.seviye(), mesafe, lod) * tabanOrani);
            sonTaban = adet;
            taban(k, s, rankine, adet, t, alfa, rEtek, rEtekUst);
        }
        sonDuvar = butce.duvarSpriteSayisi(mesafe, lod);
        duvar(k, s, sonDuvar, t, duvarAlfa * Math.min(1, 0.4 + Math.max(inis, 0.6 * duvarAlfa)),
                evre == KasirgaEvresi.OLUSUM ? 8 * (1 - p) : 0);
    }

    static void lodSifirla() {
        sonMesafe = -1;
    }

    static String lodOzeti() {
        return sonMesafe < 0 ? "çizilmedi" : String.format(Locale.ROOT,
                "kamera %.0f blok: taban sprite %d, duvar sprite %d, alt girdap %d", sonMesafe, sonTaban, sonDuvar, sonGirdap);
    }

    /** Yüzey dönüşü: U zamanla artar, doku -θ yönünde (siklonik) kayar. */
    private static double uKaymasi(double t, double omega, int uTekrar) {
        return t * omega / TAM_TUR * uTekrar;
    }

    /**
     * Dilim × halka ağı: {@code r(h)} profiliyle huni, ekseni zamanla kıvrılır (tabanı en çok); U kayar, üstte ve altta alfa
     * söner, kenarda Fresnel taklidiyle saydamlaşır; tabana doğru enkaz rengine kayar.
     */
    private void huniAgi(double t, double cx, double cz, int dilim, int halka, double alt, double ust, double rAlt,
            double rUst, double kivrim, double tohum, double uKay, int uTekrar, float v0, float v1, float r, float g,
            float b, float alfa, double kahve) {
        diziHazirla((dilim + 1) * (halka + 1), halka + 1);
        for (int j = 0; j <= halka; j++) {
            double h = (double) j / halka;
            double y = alt + h * (ust - alt);
            double yaricap = HuniProfili.yaricap(rAlt, rUst, h);
            double genlik = kivrim * Math.pow(1 - h, 1.3);
            double ox = cx + genlik * Math.sin(t * 0.9 + h * 3.1 + tohum);
            double oz = cz + genlik * Math.cos(t * 0.7 + h * 2.3 + tohum * 1.7);
            double dikey = puruzsuz(0, 0.08, h) * (1 - 0.75 * puruzsuz(0.85, 1, h));
            double kk = kahve * Math.pow(1 - h, 3);
            halkaR[j] = (float) (r + (TOZ[0] - r) * kk);
            halkaG[j] = (float) (g + (TOZ[1] - g) * kk);
            halkaB[j] = (float) (b + (TOZ[2] - b) * kk);
            halkaV[j] = (float) (v0 + (1 - h) * (v1 - v0));
            double uHalka = uKay + h * 0.35;
            for (int i = 0; i <= dilim; i++) {
                double teta = TAM_TUR * i / dilim;
                float c = (float) Math.cos(teta);
                float sn = (float) Math.sin(teta);
                int n = j * (dilim + 1) + i;
                float x = (float) (ox + c * yaricap);
                float z = (float) (oz + sn * yaricap);
                noktaX[n] = x;
                noktaY[n] = (float) y;
                noktaZ[n] = z;
                float dx = x - kamX;
                float dy = (float) y - kamY;
                float dz = z - kamZ;
                float uzunluk = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) + 1e-4f;
                float fresnel = Math.abs(c * dx + sn * dz) / uzunluk;
                noktaA[n] = (float) (alfa * dikey * (0.35 + 0.65 * Math.pow(fresnel, 0.5)));
                noktaU[n] = (float) ((double) i / dilim * uTekrar + uHalka);
                noktaNx[n] = c;
                noktaNz[n] = sn;
            }
        }
        agiYaz(dilim, halka);
    }

    private void agiYaz(int dilim, int halka) {
        for (int j = 0; j < halka; j++) {
            for (int i = 0; i < dilim; i++) {
                int a0 = j * (dilim + 1) + i;
                int a1 = a0 + 1;
                agKosesi(a0, j);
                agKosesi(a1, j);
                agKosesi(a1 + dilim + 1, j + 1);
                agKosesi(a0 + dilim + 1, j + 1);
            }
        }
    }

    private void agKosesi(int n, int halka) {
        vc.vertex(mat, noktaX[n], noktaY[n], noktaZ[n]).color(halkaR[halka], halkaG[halka], halkaB[halka], noktaA[n])
                .uv(noktaU[n], halkaV[halka]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(isik)
                .normal(nor, noktaNx[n], 0, noktaNz[n]).endVertex();
    }

    /**
     * Taban enkaz eteği: zeminde {@code rZemin}'den başlayıp {@code yukseklik}'te huniyle birleşen dışa şişkin kubbe
     * ({@link EtekProfili}). Üst kenar açıya ve zamana göre dalgalanır, U dokusu Rankine hızıyla kayar; renk tabanda koyu
     * kahverengi, üstte kahverengi-gri.
     */
    private void etekAgi(double t, int dilim, int halka, double rZemin, double rUst, double yukseklik, double omega,
            double faz, float alfa) {
        diziHazirla((dilim + 1) * (halka + 1), halka + 1);
        double uKay = t * omega / TAM_TUR * 2 + faz;
        for (int j = 0; j <= halka; j++) {
            double h = (double) j / halka;
            double yaricapTemel = EtekProfili.yaricap(rZemin, rUst, h);
            double dikeyAlfa = EtekProfili.alfa(h);
            float ka = (float) h;
            halkaR[j] = Mth.lerp(ka, ETEK_ALT[0], ETEK_UST[0]);
            halkaG[j] = Mth.lerp(ka, ETEK_ALT[1], ETEK_UST[1]);
            halkaB[j] = Mth.lerp(ka, ETEK_ALT[2], ETEK_UST[2]);
            halkaV[j] = (float) (KasirgaDokusu.HUNI_B_V1 - h * (KasirgaDokusu.HUNI_B_V1 - KasirgaDokusu.HUNI_B_V0));
            for (int i = 0; i <= dilim; i++) {
                double teta = TAM_TUR * i / dilim;
                float c = (float) Math.cos(teta);
                float sn = (float) Math.sin(teta);
                double dalga = 0.5 * Math.sin(3 * teta + omega * t * 0.6 + faz * 4) + 0.3 * Math.sin(7 * teta - t * 1.3 + faz);
                double y = -1 + h * (yukseklik + 1) * (1 + 0.18 * dalga * h);
                double yaricap = yaricapTemel * (1 + 0.05 * dalga);
                int n = j * (dilim + 1) + i;
                float x = (float) (c * yaricap);
                float z = (float) (sn * yaricap);
                noktaX[n] = x;
                noktaY[n] = (float) y;
                noktaZ[n] = z;
                float dx = x - kamX;
                float dy = (float) y - kamY;
                float dz = z - kamZ;
                float uzunluk = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) + 1e-4f;
                float fresnel = Math.abs(c * dx + sn * dz) / uzunluk;
                noktaA[n] = (float) (alfa * dikeyAlfa * (0.7 + 0.3 * Math.sqrt(fresnel)));
                noktaU[n] = (float) ((double) i / dilim * 2 + uKay);
                noktaNx[n] = c;
                noktaNz[n] = sn;
            }
        }
        agiYaz(dilim, halka);
    }

    /**
     * Etek üstündeki dönen toz: kubbenin omzunda içe kıvrılan toz rulosu (simit üzerinde yuvarlanan sprite'lar) ve Rankine
     * profiliyle dönen toz halkası; S1-2'de havada takla atan yaprak ve çimen parçaları.
     */
    private void taban(KasirgaVarligi k, KasirgaSeviyesi s, RankineProfili rankine, int adet, double t, double alfa,
            double rEtek, double rEtekUst) {
        double rT = s.tabanYaricapi();
        double hE = s.enkazYuksekligi();
        double rIc = rT * 0.45;
        double rRulo = rEtekUst + 0.6 * (rEtek - rEtekUst);
        double tup = 0.25 * hE;
        boolean yaprakli = s.seviye() <= 2;
        int tohum = k.getId() * 7919;
        for (int i = 0; i < adet; i++) {
            double u1 = rastgele(i, 1, tohum);
            double u2 = rastgele(i, 2, tohum);
            double u3 = rastgele(i, 3, tohum);
            double u4 = rastgele(i, 4, tohum);
            double u5 = rastgele(i, 5, tohum);
            double u6 = rastgele(i, 6, tohum);
            int tur = i % 5;
            float ton = (float) (0.75 + 0.35 * u5);
            float cr = TOZ[0] * ton;
            float cg = TOZ[1] * ton;
            float cb = TOZ[2] * ton;
            float u0 = tozKaresi(t, u2);
            double r;
            double y;
            double boyut;
            double sa;
            double aci;
            double donus;
            if (tur == 0) {
                double psi = u2 * TAM_TUR + t * (1.2 + 0.6 * u3);
                r = rRulo + tup * Math.cos(psi);
                y = 0.7 * hE + tup * Math.sin(psi);
                boyut = tup * (0.6 + 0.5 * u6);
                sa = 0.6;
                aci = u4 * TAM_TUR - rankine.acisalHiz(rRulo) * t;
                donus = u1 * TAM_TUR + t * (u6 - 0.5);
            } else if (yaprakli && tur >= 3) {
                r = rT * (0.7 + 1.5 * u1);
                double hMax = hE * (1.6 + 1.6 * u3);
                double fy = kesir(u2 + t * (0.18 + 0.25 * u3));
                y = fy * hMax;
                boyut = 0.25 + 0.3 * u6;
                sa = Math.min(1, 4 * Math.sin(Math.PI * fy));
                aci = u4 * TAM_TUR - rankine.acisalHiz(r) * t * (0.7 + 0.3 * u5);
                donus = u1 * TAM_TUR + t * (3 + 5 * u6);
                float[] renk = YAPRAK[(int) (u5 * YAPRAK.length)];
                cr = renk[0];
                cg = renk[1];
                cb = renk[2];
                u0 = KasirgaDokusu.YAPRAK_U0;
            } else {
                r = rIc + (rEtek - rIc) * Math.pow(u1, 0.8);
                double hMax = hE * (1.15 - 0.45 * (r - rIc) / (rEtek - rIc));
                double fy = kesir(u2 + t * (1.5 + 2.5 * u3) / hMax);
                y = fy * hMax;
                boyut = Mth.clamp(rT * (0.16 + 0.2 * u6), 0.7, 6) * (0.7 + 0.6 * fy);
                sa = 0.9 * Math.pow(Math.sin(Math.PI * fy), 0.6);
                aci = u4 * TAM_TUR - rankine.acisalHiz(r) * t * (0.85 + 0.3 * u5);
                donus = u1 * TAM_TUR + t * (u6 - 0.5) * 2;
            }
            sprite((float) (Math.cos(aci) * r), (float) y, (float) (Math.sin(aci) * r), (float) boyut, (float) donus, u0,
                    KasirgaDokusu.TOZ_V0, cr, cg, cb, (float) (sa * alfa));
        }
    }

    /** Toz sprite'ı üç kare arasında döner (dördüncü kare yaprak). */
    private static float tozKaresi(double t, double u) {
        return ((int) (t * 5 + u * 3) % 3) * KasirgaDokusu.KARE;
    }

    /**
     * Duvar bulutu: huni tepesinde yavaş dönen koyu disk (0,05 tur/sn); arkasında (hareketin tersi) açık renkli RFD yarığı
     * kasırganın yönünü uzaktan okutur. {@code ekYukseklik}: siren evresinde bulut bu kadar yukarıdan alçalır.
     */
    private void duvar(KasirgaVarligi k, KasirgaSeviyesi s, int adet, double t, double alfa, double ekYukseklik) {
        double rw = s.duvarBulutuYaricapi();
        double h = s.yukseklik() + ekYukseklik;
        double arka = k.yon() + Math.PI;
        float koyuluk = (s.seviye() - 1) / 4f;
        float dr = Mth.lerp(koyuluk, 0.55f, 0.26f);
        float dg = Mth.lerp(koyuluk, 0.57f, 0.28f);
        float db = Mth.lerp(koyuluk, 0.60f, 0.33f);
        int tohum = k.getId() * 104729;
        for (int i = 0; i < adet; i++) {
            double u1 = rastgele(i, 11, tohum);
            double u2 = rastgele(i, 12, tohum);
            double u3 = rastgele(i, 13, tohum);
            double u4 = rastgele(i, 14, tohum);
            double u5 = rastgele(i, 15, tohum);
            double r = rw * (0.1 + 0.9 * Math.sqrt(u1));
            double aci = u2 * TAM_TUR - 0.314 * t * (0.5 + 0.5 * (1 - r / rw));
            double y = h - 3 + 7 * (r / rw) + (u3 - 0.5) * 6;
            float boyut = (float) ((7 + 12 * u4) * (0.55 + 0.09 * s.seviye()) + r * 0.10);
            double fark = Mth.wrapDegrees(Math.toDegrees(aci - arka));
            boolean yarik = Math.abs(fark) < 35 && r > rw * 0.35;
            float ton = (float) (0.85 + 0.3 * u5);
            int kare = (int) (u5 * 4) & 3;
            float x = (float) (Math.cos(aci) * r);
            float z = (float) (Math.sin(aci) * r);
            if (yarik) {
                sprite(x, (float) y, z, boyut, (float) (u3 * 6), kare * KasirgaDokusu.KARE, KasirgaDokusu.BULUT_V0, 0.72f,
                        0.74f, 0.76f, (float) (0.30 * alfa));
            } else {
                sprite(x, (float) y, z, boyut, (float) (u3 * 6), kare * KasirgaDokusu.KARE, KasirgaDokusu.BULUT_V0,
                        Math.min(1, dr * ton), Math.min(1, dg * ton), Math.min(1, db * ton), (float) (DUVAR_ALFA * alfa));
            }
        }
    }

    /** Yalnız görsel enkaz kopyaları: sarmal yükseliş, tepede dışa savrulma ve düşüş; ömrü bitenler silinir. */
    private void enkaz(KasirgaVarligi k, RankineProfili rankine, PoseStack poz, MultiBufferSource tampon, double t,
            int paketliIsik) {
        List<GorselEnkaz> liste = IstemciKasirga.ENKAZ.get(k.getId());
        if (liste == null) {
            return;
        }
        Iterator<GorselEnkaz> it = liste.iterator();
        while (it.hasNext()) {
            GorselEnkaz e = it.next();
            double tau = t - e.dogum();
            double tepeZamani = e.tepe() / e.dikeyHiz();
            double r;
            double y;
            double aci;
            if (tau < tepeZamani) {
                r = e.yaricap();
                y = e.dikeyHiz() * Math.max(0, tau);
                aci = e.aci() - rankine.acisalHiz(r) * tau;
            } else {
                double t2 = tau - tepeZamani;
                r = e.yaricap() + e.disaHiz() * t2;
                y = e.tepe() + e.yukariHiz() * t2 - 8 * t2 * t2;
                aci = e.aci() - rankine.acisalHiz(e.yaricap()) * (tepeZamani + 0.5 * t2);
            }
            if (y < -3 || tau > 25) {
                it.remove();
                continue;
            }
            poz.pushPose();
            poz.translate(Math.cos(aci) * r, y, Math.sin(aci) * r);
            poz.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(e.donusHizi() * tau), e.eksenX(), e.eksenY(),
                    e.eksenZ()));
            poz.scale(e.olcek(), e.olcek(), e.olcek());
            poz.translate(-0.5, -0.5, -0.5);
            bloklar.renderSingleBlock(e.durum(), poz, tampon, paketliIsik, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            poz.popPose();
        }
    }

    /** Kameraya dönük kare; {@code donus} kendi düzlemindeki dönüşü, (u0, v0) atlas karesinin sol üstü. */
    private void sprite(float x, float y, float z, float boyut, float donus, float u0, float v0, float r, float g, float b,
            float a) {
        float dx = x - kamX;
        float dy = y - kamY;
        float dz = z - kamZ;
        float sinir = ekranOrani * (float) Math.sqrt(dx * dx + dy * dy + dz * dz) + 0.3f;
        float bo = Math.min(boyut, sinir);
        float c = Mth.cos(donus);
        float s = Mth.sin(donus);
        float rx = (sagX * c + ustX * s) * bo;
        float ry = (sagY * c + ustY * s) * bo;
        float rz = (sagZ * c + ustZ * s) * bo;
        float ux = (ustX * c - sagX * s) * bo;
        float uy = (ustY * c - sagY * s) * bo;
        float uz = (ustZ * c - sagZ * s) * bo;
        float ua = u0 + PIKSEL;
        float ub = u0 + KasirgaDokusu.KARE - PIKSEL;
        float va = v0 + PIKSEL;
        float vb = v0 + KasirgaDokusu.KARE - PIKSEL;
        spriteKosesi(x - rx - ux, y - ry - uy, z - rz - uz, ua, vb, r, g, b, a);
        spriteKosesi(x - rx + ux, y - ry + uy, z - rz + uz, ua, va, r, g, b, a);
        spriteKosesi(x + rx + ux, y + ry + uy, z + rz + uz, ub, va, r, g, b, a);
        spriteKosesi(x + rx - ux, y + ry - uy, z + rz - uz, ub, vb, r, g, b, a);
    }

    private void spriteKosesi(float x, float y, float z, float u, float v, float r, float g, float b, float a) {
        vc.vertex(mat, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(isik)
                .normal(nor, 0, 1, 0).endVertex();
    }

    private void diziHazirla(int nokta, int halka) {
        if (noktaX.length < nokta) {
            noktaX = new float[nokta];
            noktaY = new float[nokta];
            noktaZ = new float[nokta];
            noktaA = new float[nokta];
            noktaU = new float[nokta];
            noktaNx = new float[nokta];
            noktaNz = new float[nokta];
        }
        if (halkaR.length < halka) {
            halkaR = new float[halka];
            halkaG = new float[halka];
            halkaB = new float[halka];
            halkaV = new float[halka];
        }
    }

    private static double puruzsuz(double a, double b, double x) {
        double t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    private static double kesir(double x) {
        return x - Math.floor(x);
    }

    /** Sprite başına kararlı rastgele sayı [0, 1): her karede aynı sprite aynı parametreleri alır (nesne yok). */
    static double rastgele(int i, int kanal, int tohum) {
        int h = i * 374761393 + kanal * 668265263 + tohum;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }
}
