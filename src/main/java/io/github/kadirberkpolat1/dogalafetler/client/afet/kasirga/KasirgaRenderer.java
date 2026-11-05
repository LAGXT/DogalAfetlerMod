package io.github.kadirberkpolat1.dogalafetler.client.afet.kasirga;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.HuniProfili;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaEvresi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaKaliteButcesi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaVarligi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.RankineProfili;
import io.github.kadirberkpolat1.dogalafetler.client.afet.kasirga.KasirgaIstemciDurumu.GorselEnkaz;
import io.github.kadirberkpolat1.dogalafetler.client.afet.kasirga.KasirgaIstemciDurumu.Katman;
import java.util.Iterator;
import java.util.List;
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
 * Kasırganın bütün istemci katmanları (tasarım notu §2): huni gövdesi, alt girdaplar, taban enkaz halkası, duvar bulutu
 * ve yalnız görsel enkaz kopyaları. Bulut katmanlarının hepsi tek atlas ve tek render tipiyle aynı tampona yazılır: kare
 * başına tek çizim çağrısı, yarı saydam dörtgenler kameraya göre birlikte sıralanır. Sprite'lar parçacık motoruna
 * değil bu tampona yazılır (16384 parçacık sınırına ve parçacık tick'ine takılmaz). Enkaz kopyaları vanilla blok
 * modelleriyle sabit blok tamponlarına gider. Dönüş siklonik: yukarıdan bakınca saat yönünün tersi.
 */
final class KasirgaRenderer extends EntityRenderer<KasirgaVarligi> {
    private static final float[] TOZ = {0.50f, 0.43f, 0.35f};
    private static final float YAPRAK_R = 0.30f;
    private static final float YAPRAK_G = 0.48f;
    private static final float YAPRAK_B = 0.22f;
    private static final float PIKSEL = 1f / KasirgaDokusu.BOYUT;
    private static final double TAM_TUR = Math.PI * 2;

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

    @Override
    public void render(KasirgaVarligi k, float yaw, float kismi, PoseStack poz, MultiBufferSource tampon,
            int paketliIsik) {
        KasirgaDokusu.hazirla();
        KasirgaIstatistik.kareBasi();
        double ex = Mth.lerp(kismi, k.xOld, k.getX());
        double ey = Mth.lerp(kismi, k.yOld, k.getY());
        double ez = Mth.lerp(kismi, k.zOld, k.getZ());
        KasirgaIstatistik.konum(ex, ez);
        Vec3 kamera = entityRenderDispatcher.camera.getPosition();
        kamX = (float) (kamera.x - ex);
        kamY = (float) (kamera.y - ey);
        kamZ = (float) (kamera.z - ez);
        double mesafe = Math.hypot(kamX, kamZ);
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
        KasirgaKaliteButcesi butce = KasirgaKaliteButcesi.icin(KasirgaIstemciDurumu.kalite(k));
        double t = KasirgaIstemciDurumu.zaman(k, kismi);
        RankineProfili rankine = k.rankine();

        // Evreye göre biçim: doğuşta huni bulut tabanından iner, ip evresinde incelip kıvrılır, dağılırken solar.
        double inis = 1;
        double tabanOrani = 1;
        double alfa = 1;
        double yaricapCarpani = 1;
        double kivrimCarpani = 1;
        double tepeCarpani = 1;
        float p = k.evreIlerleme();
        KasirgaEvresi evre = k.evre();
        switch (evre) {
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
        int uTekrar = s.seviye() >= 3 ? 3 : 2;
        double kahve = 0.25 + 0.12 * s.seviye();
        double disOmega = rankine.acisalHiz(rAlt * 1.3);
        double icOmega = rankine.acisalHiz(rAlt * 0.8);

        if (KasirgaIstemciDurumu.acik(Katman.HUNI)) {
            long bas = System.nanoTime();
            KasirgaIstemciDurumu.HuniKipi kip = KasirgaIstemciDurumu.huniKipi;
            if (kip != KasirgaIstemciDurumu.HuniKipi.SPRITE) {
                double karmaAlfa = kip == KasirgaIstemciDurumu.HuniKipi.KARMA ? 0.85 : 1;
                KasirgaIstatistik.huniDortgen += huniAgi(t, 0, 0, butce.huniDilim(), butce.huniHalka(), alt, h, rAlt,
                        rUst, kivrim, 0, uKaymasi(t, disOmega, uTekrar), uTekrar, KasirgaDokusu.HUNI_A_V0,
                        KasirgaDokusu.HUNI_A_V1, hr, hg, hb, (float) (0.62 * alfa * karmaAlfa), kahve);
                if (kip == KasirgaIstemciDurumu.HuniKipi.AG) {
                    KasirgaIstatistik.huniDortgen += huniAgi(t, 0, 0, butce.huniDilim(), butce.huniHalka(), alt, h,
                            rAlt * 0.8, rUst * 0.8, kivrim, 0, uKaymasi(t, icOmega, uTekrar) + 0.5, uTekrar,
                            KasirgaDokusu.HUNI_B_V0, KasirgaDokusu.HUNI_B_V1, hr * 0.78f, hg * 0.78f, hb * 0.80f,
                            (float) (0.80 * alfa), kahve);
                }
            }
            if (kip != KasirgaIstemciDurumu.HuniKipi.AG) {
                boolean karma = kip == KasirgaIstemciDurumu.HuniKipi.KARMA;
                KasirgaIstatistik.huniSprite += huniSprite(t, butce.huniHalka() / (karma ? 4 : 2), alt, h,
                        rAlt * (karma ? 1.08 : 1), rUst * (karma ? 1.08 : 1), kivrim, disOmega, hr, hg, hb,
                        alfa * (karma ? 0.55 : 0.85));
            }
            KasirgaIstatistik.sure(Katman.HUNI, System.nanoTime() - bas);
        }
        if (KasirgaIstemciDurumu.acik(Katman.ALT_GIRDAP)) {
            long bas = System.nanoTime();
            int adet = evre == KasirgaEvresi.OLGUN ? butce.altGirdapSayisi(s.seviye()) : 0;
            for (int g = 0; g < adet; g++) {
                double faz = g * TAM_TUR / adet - t * Math.PI;
                double yorunge = s.tabanYaricapi() * 1.2;
                KasirgaIstatistik.altGirdapDortgen += huniAgi(t, Math.cos(faz) * yorunge, Math.sin(faz) * yorunge, 8,
                        8, 0, h * 0.32, 0.8, 2.4 + 0.02 * h, 1.2, g * 3.3, t * 0.9, 1, KasirgaDokusu.HUNI_A_V0,
                        KasirgaDokusu.HUNI_A_V1, Math.min(1, hr * 1.15f), Math.min(1, hg * 1.15f),
                        Math.min(1, hb * 1.15f), (float) (0.45 * alfa), 0.6);
            }
            KasirgaIstatistik.sure(Katman.ALT_GIRDAP, System.nanoTime() - bas);
        }
        if (KasirgaIstemciDurumu.acik(Katman.TABAN)) {
            long bas = System.nanoTime();
            int adet = (int) (butce.tabanSpriteSayisi(s.seviye(), mesafe) * tabanOrani);
            KasirgaIstatistik.tabanSprite += taban(k, s, rankine, adet, t, alfa);
            KasirgaIstatistik.sure(Katman.TABAN, System.nanoTime() - bas);
        }
        if (KasirgaIstemciDurumu.acik(Katman.DUVAR)) {
            long bas = System.nanoTime();
            KasirgaIstatistik.duvarSprite += duvar(k, s, butce.duvarSprite(), t, alfa * Math.min(1, 0.4 + inis));
            KasirgaIstatistik.sure(Katman.DUVAR, System.nanoTime() - bas);
        }
        if (KasirgaIstemciDurumu.acik(Katman.ENKAZ)) {
            long bas = System.nanoTime();
            KasirgaIstatistik.gorselEnkaz += enkaz(k, rankine, poz, tampon, t);
            KasirgaIstatistik.sure(Katman.ENKAZ, System.nanoTime() - bas);
        }
    }

    /** Yüzey dönüşü: U zamanla artar, doku -θ yönünde (siklonik) kayar. */
    private static double uKaymasi(double t, double omega, int uTekrar) {
        return t * omega / TAM_TUR * uTekrar;
    }

    /**
     * Dilim × halka ağı: {@code r(h)} profiliyle huni, ekseni zamanla kıvrılır; U kayar, üstte ve altta alfa söner, kenarda
     * Fresnel taklidiyle saydamlaşır; tabana doğru enkaz rengine kayar. Dörtgen sayısını döner.
     */
    private int huniAgi(double t, double cx, double cz, int dilim, int halka, double alt, double ust, double rAlt,
            double rUst, double kivrim, double tohum, double uKay, int uTekrar, float v0, float v1, float r, float g,
            float b, float alfa, double kahve) {
        int nokta = (dilim + 1) * (halka + 1);
        diziHazirla(nokta, halka + 1);
        for (int j = 0; j <= halka; j++) {
            double h = (double) j / halka;
            double y = alt + h * (ust - alt);
            double yaricap = HuniProfili.yaricap(rAlt, rUst, h);
            double genlik = kivrim * Math.pow(1 - h, 1.3);
            double ox = cx + genlik * Math.sin(t * 0.9 + h * 3.1 + tohum);
            double oz = cz + genlik * Math.cos(t * 0.7 + h * 2.3 + tohum * 1.7);
            double dikey = puruzsuz(0, 0.08, h) * (1 - 0.75 * puruzsuz(0.85, 1, h));
            double k = kahve * Math.pow(1 - h, 3);
            halkaR[j] = (float) (r + (TOZ[0] - r) * k);
            halkaG[j] = (float) (g + (TOZ[1] - g) * k);
            halkaB[j] = (float) (b + (TOZ[2] - b) * k);
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
                noktaA[n] = (float) (alfa * dikey * (0.15 + 0.85 * Math.pow(fresnel, 0.6)));
                noktaU[n] = (float) ((double) i / dilim * uTekrar + uHalka);
                noktaNx[n] = c;
                noktaNz[n] = sn;
            }
        }
        for (int j = 0; j < halka; j++) {
            for (int i = 0; i < dilim; i++) {
                int a0 = j * (dilim + 1) + i;
                int a1 = a0 + 1;
                int b1 = a1 + dilim + 1;
                int b0 = a0 + dilim + 1;
                agKosesi(a0, j);
                agKosesi(a1, j);
                agKosesi(b1, j + 1);
                agKosesi(b0, j + 1);
            }
        }
        return dilim * halka;
    }

    private void agKosesi(int n, int halka) {
        vc.vertex(mat, noktaX[n], noktaY[n], noktaZ[n]).color(halkaR[halka], halkaG[halka], halkaB[halka], noktaA[n])
                .uv(noktaU[n], halkaV[halka]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(isik)
                .normal(nor, noktaNx[n], 0, noktaNz[n]).endVertex();
    }

    /** (b) Weather2 tarzı: huni yüzeyinde halkalar hâlinde dönen bulut sprite'ları. Sprite sayısını döner. */
    private int huniSprite(double t, int halka, double alt, double ust, double rAlt, double rUst, double kivrim,
            double omega, float r, float g, float b, double alfa) {
        int toplam = 0;
        for (int j = 0; j < halka; j++) {
            double h = (j + 0.5) / halka;
            double y = alt + h * (ust - alt);
            double yaricap = HuniProfili.yaricap(rAlt, rUst, h);
            double boyut = yaricap * 0.55 + 1.5;
            double genlik = kivrim * Math.pow(1 - h, 1.3);
            double ox = genlik * Math.sin(t * 0.9 + h * 3.1);
            double oz = genlik * Math.cos(t * 0.7 + h * 2.3);
            double dikey = puruzsuz(0, 0.08, h) * (1 - 0.75 * puruzsuz(0.85, 1, h));
            int adet = Mth.clamp((int) (TAM_TUR * yaricap / (boyut * 0.75)), 6, 48);
            for (int m = 0; m < adet; m++) {
                double teta = TAM_TUR * m / adet - t * omega * (1.1 - 0.4 * h) + j * 0.37;
                int kare = (j + m) & 3;
                sprite((float) (ox + Math.cos(teta) * yaricap), (float) y, (float) (oz + Math.sin(teta) * yaricap),
                        (float) boyut, (float) (j * 1.3 + m * 0.7), kare * KasirgaDokusu.KARE, KasirgaDokusu.BULUT_V0,
                        r, g, b, (float) (alfa * dikey));
                toplam++;
            }
        }
        return toplam;
    }

    /** Taban enkaz halkası ve yere yayılan etek: Rankine profiliyle dönen, yükselip sönen toz sprite'ları. */
    private int taban(KasirgaVarligi k, KasirgaSeviyesi s, RankineProfili rankine, int adet, double t, double alfa) {
        double rT = s.tabanYaricapi();
        double hE = s.enkazYuksekligi();
        int tohum = k.getId() * 7919;
        for (int i = 0; i < adet; i++) {
            double u1 = rastgele(i, 1, tohum);
            double u2 = rastgele(i, 2, tohum);
            double u3 = rastgele(i, 3, tohum);
            double u4 = rastgele(i, 4, tohum);
            double u5 = rastgele(i, 5, tohum);
            double u6 = rastgele(i, 6, tohum);
            boolean etek = (i & 3) == 0;
            double r;
            double y;
            double boyut;
            double a;
            if (etek) {
                r = rT * (0.9 + 1.3 * u1);
                double fy = kesir(u2 + t * 0.05 * (0.5 + u3));
                y = 0.2 + 1.8 * fy;
                boyut = rT * (0.30 + 0.25 * u6) + 1;
                a = 0.55 * Math.sqrt(Math.sin(Math.PI * fy));
            } else {
                r = rT * (0.45 + 1.35 * Math.pow(u1, 0.8));
                double hMax = hE * (1.15 - 0.75 * (r / rT - 0.45) / 1.35);
                double fy = kesir(u2 + t * (1.5 + 2.5 * u3) / hMax);
                y = fy * hMax;
                boyut = Mth.clamp(rT * (0.16 + 0.2 * u6), 0.7, 6) * (0.7 + 0.6 * fy);
                a = 0.85 * Math.pow(Math.sin(Math.PI * fy), 0.7);
            }
            double aci = u4 * TAM_TUR - rankine.acisalHiz(r) * t * (0.85 + 0.3 * u5);
            float ton = (float) (0.75 + 0.35 * u5);
            float cr = TOZ[0] * ton;
            float cg = TOZ[1] * ton;
            float cb = TOZ[2] * ton;
            if (s.seviye() <= 2 && !etek && u3 < 0.3) {
                cr = YAPRAK_R;
                cg = YAPRAK_G;
                cb = YAPRAK_B;
                boyut *= 0.5;
            }
            int kare = ((int) (t * 5 + u2 * 4)) & 3;
            sprite((float) (Math.cos(aci) * r), (float) y, (float) (Math.sin(aci) * r), (float) boyut,
                    (float) (u1 * TAM_TUR + t * (u6 - 0.5) * 2), kare * KasirgaDokusu.KARE, KasirgaDokusu.TOZ_V0, cr,
                    cg, cb, (float) (a * alfa));
        }
        return adet;
    }

    /** Duvar bulutu: huni tepesinde yavaş dönen koyu disk; arkasında (hareketin tersi) açık renkli RFD yarığı. */
    private int duvar(KasirgaVarligi k, KasirgaSeviyesi s, int adet, double t, double alfa) {
        double rw = s.duvarBulutuYaricapi();
        double h = s.yukseklik();
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
            if (yarik) {
                sprite((float) (Math.cos(aci) * r), (float) y, (float) (Math.sin(aci) * r), boyut, (float) (u3 * 6),
                        kare * KasirgaDokusu.KARE, KasirgaDokusu.BULUT_V0, 0.72f, 0.74f, 0.76f, (float) (0.30 * alfa));
            } else {
                sprite((float) (Math.cos(aci) * r), (float) y, (float) (Math.sin(aci) * r), boyut, (float) (u3 * 6),
                        kare * KasirgaDokusu.KARE, KasirgaDokusu.BULUT_V0, Math.min(1, dr * ton), Math.min(1, dg * ton),
                        Math.min(1, db * ton), (float) (0.88 * alfa));
            }
        }
        return adet;
    }

    /** Yalnız görsel enkaz kopyaları: sarmal yükseliş, tepede dışa savrulma ve düşüş; ömrü bitenler silinir. */
    private int enkaz(KasirgaVarligi k, RankineProfili rankine, PoseStack poz, MultiBufferSource tampon, double t) {
        List<GorselEnkaz> liste = KasirgaIstemciDurumu.ENKAZ.get(k.getId());
        if (liste == null) {
            return 0;
        }
        int cizilen = 0;
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
            poz.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(e.donusHizi() * tau), e.eksenX(),
                    e.eksenY(), e.eksenZ()));
            poz.scale(e.olcek(), e.olcek(), e.olcek());
            poz.translate(-0.5, -0.5, -0.5);
            bloklar.renderSingleBlock(e.durum(), poz, tampon, isik, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            poz.popPose();
            cizilen++;
        }
        return cizilen;
    }

    /** Kameraya dönük kare; {@code donus} kendi düzlemindeki dönüşü, (u0, v0) atlas karesinin sol üstü. */
    private void sprite(float x, float y, float z, float boyut, float donus, float u0, float v0, float r, float g,
            float b, float a) {
        float c = Mth.cos(donus);
        float s = Mth.sin(donus);
        float rx = (sagX * c + ustX * s) * boyut;
        float ry = (sagY * c + ustY * s) * boyut;
        float rz = (sagZ * c + ustZ * s) * boyut;
        float ux = (ustX * c - sagX * s) * boyut;
        float uy = (ustY * c - sagY * s) * boyut;
        float uz = (ustZ * c - sagZ * s) * boyut;
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

    /** Sprite başına kararlı rastgele sayı [0, 1): her karede aynı sprite aynı parametreleri alır. */
    static double rastgele(int i, int kanal, int tohum) {
        int h = i * 374761393 + kanal * 668265263 + tohum;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }
}
