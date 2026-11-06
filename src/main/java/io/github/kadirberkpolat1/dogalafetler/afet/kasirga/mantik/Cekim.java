package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/**
 * Kasırganın varlıklara uyguladığı hız değişimi (tick başına, blok/tick): içe doğru çekim, siklonik teğet sürükleme ve
 * huni çekirdeğinde yukarı kaldırma (madde 10 "çekim gücü", bölüm A "nesneleri havaya çeksin"). Siklonik: yukarıdan
 * bakınca saat yönünün tersi; x doğu, z güney iken açı azalır, teğet birim vektör {@code (sin θ, -cos θ)}.
 */
public final class Cekim {
    /** Çekim yarıçapının kenarında içe ivme (blok/tick²), seviye 1'de; seviyeyle doğrusal artar. */
    public static final double TABAN_ICE = 0.012;
    /** Çekirdek yarıçapında kaldırma ivmesi (blok/tick²); yerçekimini (0,08) seviyeyle aşar. */
    public static final double TABAN_KALDIRMA = 0.07;
    /** Yatay hız tavanı (blok/tick). */
    public static final double EN_COK_YATAY = 1.4;
    /** Dikey hız tavanı (blok/tick). */
    public static final double EN_COK_DIKEY = 0.9;

    private Cekim() {
    }

    /**
     * @param dx          varlığın kasırga eksenine göre doğu ofseti
     * @param dz          varlığın kasırga eksenine göre güney ofseti
     * @param yukseklik    varlığın kasırga zemininden yüksekliği
     * @param cekirdek     kaldırmanın işlediği yarıçap (huni tabanı)
     * @param cekimYaricapi çekimin söndüğü yarıçap
     * @param seviye       1-5
     * @param guc          evre ve config çarpanı (0 = etkisiz)
     * @param tavan        kaldırmanın durduğu yükseklik (oyuncu en çok 25 blok yükselir)
     * @return {vx, vy, vz} eklenecek hız; çekim yarıçapı dışında sıfır
     */
    public static double[] ivme(double dx, double dz, double yukseklik, double cekirdek, double cekimYaricapi, int seviye,
            double guc, double tavan) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d >= cekimYaricapi || guc <= 0) {
            return new double[3];
        }
        double kenar = 1 - d / cekimYaricapi;
        double olcek = guc * (0.6 + 0.4 * seviye);
        double ice = TABAN_ICE * olcek * (0.35 + 0.65 * kenar);
        double teget = ice * 1.3;
        double ux = d > 1e-6 ? dx / d : 0;
        double uz = d > 1e-6 ? dz / d : 0;
        // Çekirdeğin içinde içe çekim söner (varlık eksene yığılmasın, çevresinde döner).
        double iceCarpan = d < cekirdek * 0.5 ? d / (cekirdek * 0.5) : 1;
        double vx = -ux * ice * iceCarpan + uz * teget;
        double vz = -uz * ice * iceCarpan - ux * teget;
        double vy = 0;
        if (d < cekirdek * 1.15 && yukseklik < tavan) {
            double cekirdekPayi = 1 - Math.min(1, d / (cekirdek * 1.15));
            double tavanPayi = 1 - Math.max(0, yukseklik) / tavan;
            vy = TABAN_KALDIRMA * olcek * (0.4 + 0.6 * cekirdekPayi) * Math.sqrt(Math.max(0, tavanPayi));
        }
        return new double[] {vx, vy, vz};
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
