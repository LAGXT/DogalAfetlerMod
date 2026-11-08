package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/**
 * Kasırganın varlıklara uyguladığı hız değişimi (tick başına, blok/tick; madde 10 "çekim gücü", bölüm A "nesneleri havaya
 * çeksin"; tur 3 AF3-02). Siklonik: yukarıdan bakınca saat yönünün tersi; x doğu, z güney iken teğet birim vektör
 * {@code (sin θ, -cos θ)}.
 *
 * <p>Çekim bölgesinde ({@code çekirdek ≤ d < R}) hedef bir içe sürüklenme hızı vardır: {@code V = KENAR_HIZI · güç ·
 * √(R/d)} (kütle korunumundaki 1/r içe akışın yumuşatılmışı; R'nin son %10'unda sıfırdan açılır). İvme {@code V · (1 -
 * sürtünme)}'dir: sabit ivmede vanilla sürtünmesi hızı {@code ivme / (1 - sürtünme)}'de dengeler, yani varlık yerde de havada
 * da aynı {@code V} ile sürüklenir ve oyuncunun koşu girdisiyle (yerde 0,13 / 0,454 = 0,286 blok/tick, havada 0,026 / 0,09)
 * doğrudan karşılaştırılabilir. Teğet bileşen içe bileşenin yarısıdır. Çekirdekte ({@code d < çekirdek}) dışa net ivme
 * yoktur: içe yay {@code çekirdek/2}'ye kadar sürer, varlığın kendi teğet hızı için {@code vₜ²/d} merkezcil ivme ve dışa
 * radyal hızın yarısı kadar sönüm eklenir; varlık çekirdekte döner, kaldırılır ve merkezkaçla fırlatılmaz.
 */
public final class Cekim {
    /** Güç 1'de çekim yarıçapının kenarındaki içe sürüklenme hızı (blok/tick): S5 (güç 4,8) kenarda 8 blok/sn. */
    public static final double KENAR_HIZI = 8.0 / 4.8 / 20;
    /** Güç 1'de kaldırma ivmesi (blok/tick²): S1 0,06 yerçekiminin (0,08) altında kalır, S2 eksende 0,096 aşar. */
    public static final double TABAN_KALDIRMA = 0.06;
    /** Teğet / içe oranı (çekim bölgesinde). */
    public static final double TEGET_ORANI = 0.5;
    /** Çekim yarıçapının bu payında çekim sıfırdan tam değerine açılır (kenarda ani sıçrama olmasın). */
    public static final double KENAR_SOLMA = 0.1;
    /** Vanilla yatay sürtünmeleri: yerde (blok sürtünmesi 0,6 × 0,91), havada canlı. */
    public static final double YER_SURTUNMESI = 0.6 * 0.91;
    public static final double HAVA_SURTUNMESI = 0.91;
    /** Yatay hız tavanı (blok/tick). */
    public static final double EN_COK_YATAY = 1.4;
    /** Dikey hız tavanı (blok/tick). */
    public static final double EN_COK_DIKEY = 0.9;

    private Cekim() {
    }

    /**
     * @param dx            varlığın kasırga eksenine göre doğu ofseti
     * @param dz            varlığın kasırga eksenine göre güney ofseti
     * @param yukseklik     varlığın kasırga zemininden yüksekliği
     * @param vx            varlığın şu anki doğu hızı (merkezcil terim ve dışa hız sönümü için)
     * @param vz            varlığın şu anki güney hızı
     * @param cekirdek      huni taban yarıçapı (kaldırma ve dönme bölgesi)
     * @param cekimYaricapi çekimin söndüğü yarıçap
     * @param seviyeGucu    {@link KasirgaSeviyesi#guc()}
     * @param carpan        evre gücü × {@code cekimCarpani} (0 = etkisiz)
     * @param tavan         kaldırmanın durduğu yükseklik ({@link KasirgaSeviyesi#kaldirmaTavani})
     * @param surtunme      varlığın bu tick'teki yatay sürtünme çarpanı (yerde {@link #YER_SURTUNMESI}, havada
     *                      {@link #HAVA_SURTUNMESI})
     * @return {vx, vy, vz} eklenecek hız; çekim yarıçapı dışında sıfır
     */
    public static double[] ivme(double dx, double dz, double yukseklik, double vx, double vz, double cekirdek,
            double cekimYaricapi, double seviyeGucu, double carpan, double tavan, double surtunme) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d >= cekimYaricapi || carpan <= 0) {
            return new double[3];
        }
        double ux = d > 1e-6 ? dx / d : 0;
        double uz = d > 1e-6 ? dz / d : 0;
        double k = 1 - Math.max(0, Math.min(1, surtunme));
        double cekirdekHizi = iceHiz(Math.min(d, cekirdek), cekirdek, cekimYaricapi, seviyeGucu, carpan);
        double ice;
        double teget;
        if (d >= cekirdek) {
            ice = iceHiz(d, cekirdek, cekimYaricapi, seviyeGucu, carpan) * k;
            teget = TEGET_ORANI * ice;
        } else {
            double yay = cekirdek * 0.5;
            ice = cekirdekHizi * k * Math.max(0, (d - yay) / (cekirdek - yay));
            teget = TEGET_ORANI * cekirdekHizi * k * Math.min(1, d / yay);
        }
        if (d < cekirdek * 1.15 && d > 1e-6) {
            double vr = vx * ux + vz * uz;
            double vt = vx * uz - vz * ux;
            ice += vt * vt / Math.max(d, 0.5);
            if (vr > 0) {
                ice += 0.5 * vr;
            }
        }
        double ax = -ux * ice + uz * teget;
        double az = -uz * ice - ux * teget;
        double ay = 0;
        if (d < cekirdek * 1.15 && yukseklik < tavan) {
            double cekirdekPayi = 1 - Math.min(1, d / (cekirdek * 1.15));
            double tavanPayi = 1 - Math.max(0, yukseklik) / tavan;
            ay = TABAN_KALDIRMA * seviyeGucu * carpan * (0.4 + 0.6 * cekirdekPayi) * Math.sqrt(Math.max(0, tavanPayi));
        }
        return new double[] {ax, ay, az};
    }

    /**
     * Çekim bölgesinde {@code d} uzaklığındaki hedef içe sürüklenme hızı (blok/tick): sabit ivmede sürtünmeyle dengelenen
     * hız. Çekirdek sınırında en büyük, kenarda {@code KENAR_HIZI · güç · çarpan}, dışında 0.
     */
    public static double iceHiz(double d, double cekirdek, double cekimYaricapi, double seviyeGucu, double carpan) {
        if (d >= cekimYaricapi || carpan <= 0) {
            return 0;
        }
        double solma = Math.min(1, (cekimYaricapi - d) / (KENAR_SOLMA * cekimYaricapi));
        return KENAR_HIZI * seviyeGucu * carpan * Math.sqrt(cekimYaricapi / Math.max(d, cekirdek)) * solma;
    }

    /** Hızı tavanlara kırpar (yerinde). */
    public static double[] kirp(double[] hiz) {
        double yatay = Math.sqrt(hiz[0] * hiz[0] + hiz[2] * hiz[2]);
        if (yatay > EN_COK_YATAY) {
            hiz[0] *= EN_COK_YATAY / yatay;
            hiz[2] *= EN_COK_YATAY / yatay;
        }
        hiz[1] = Math.max(-3.9, Math.min(EN_COK_DIKEY, hiz[1]));
        return hiz;
    }
}
