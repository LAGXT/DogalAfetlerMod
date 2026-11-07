package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

/**
 * Hedefe düz bir çizgide, sabit hızla inen meteorun yörüngesi. Meteor {@code azimut} yönünden (0 = +X, π/2 = +Z) gelir,
 * yere {@code aciDerece} ile girer ve hedefin {@code yukseklik} blok üstünden başlar.
 *
 * @param tamSure hedefe varışın kesirli tick'i
 */
public record Yorunge(double baslangicX, double baslangicY, double baslangicZ, double hizX, double hizY, double hizZ,
        double tamSure) {
    public static final double EN_KUCUK_ACI = 15;
    /** Eksen başına paket sınırı 3,9 blok/tick'in altında kalan en büyük hız. */
    public static final double EN_BUYUK_HIZ = 3.8;

    /**
     * Hedefe {@code sureTick}'te varan yörünge: hız yol uzunluğundan çıkar ({@link #EN_BUYUK_HIZ}'la sınırlı; sınıra
     * takılırsa uçuş uzar). Açı değişince yol, dolayısıyla hız değişir; süre değişmez.
     */
    public static Yorunge sureyle(double hedefX, double hedefY, double hedefZ, double aciDerece, double azimut,
            double yukseklik, double sureTick) {
        Yorunge birim = hesapla(hedefX, hedefY, hedefZ, aciDerece, azimut, yukseklik, 1);
        double hiz = Math.min(EN_BUYUK_HIZ, birim.tamSure() / Math.max(1, sureTick));
        return hesapla(hedefX, hedefY, hedefZ, aciDerece, azimut, yukseklik, hiz);
    }

    public static Yorunge hesapla(double hedefX, double hedefY, double hedefZ, double aciDerece, double azimut,
            double yukseklik, double hiz) {
        double aci = Math.toRadians(Math.max(EN_KUCUK_ACI, Math.min(90, aciDerece)));
        double yatay = aciDerece >= 90 ? 0 : yukseklik / Math.tan(aci);
        double bx = hedefX + Math.cos(azimut) * yatay;
        double by = hedefY + yukseklik;
        double bz = hedefZ + Math.sin(azimut) * yatay;
        double dx = hedefX - bx;
        double dy = hedefY - by;
        double dz = hedefZ - bz;
        double uzunluk = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double olcek = hiz / uzunluk;
        return new Yorunge(bx, by, bz, dx * olcek, dy * olcek, dz * olcek, uzunluk / hiz);
    }

    /** Hedefe varış tick'i (yukarı yuvarlanmış). */
    public int sureTick() {
        return (int) Math.ceil(tamSure);
    }

    public double[] konum(double tick) {
        return new double[] {baslangicX + hizX * tick, baslangicY + hizY * tick, baslangicZ + hizZ * tick};
    }
}
