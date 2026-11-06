package io.github.kadirberkpolat1.dogalafetler.su;

/**
 * İlerleyen tsunami dalgası (SU-01): cephe çizgisi {@code (x0, z0)}'dan {@code (yonX, yonZ)} doğrultusunda {@code hiz}
 * blok/tick ile ilerler. Cephenin {@code s} blok arkasındaki yüzey {@code taban + yukseklik * f(s) * g(yan) * sönme}: önde dik
 * yükselen bir duvar, arkada uzun bir kuyruk. {@code menzil}'e varınca durur ve {@code 5 * uzunluk} ilerleme süresinde söner.
 * {@code ivme} tam batmış varlığa uygulanan yatay sürükleme (blok/tick²), {@code hasar} dalganın içindeki canlıya saniyede
 * verilen hasar (0: yok). Zaman ölçekli afet saatidir; parametreler ağdan bir kez gider, istemci aynı formülle hesaplar.
 */
public record Dalga(double x0, double z0, double yonX, double yonZ, double baslangic, double hiz, double yukseklik,
        double genislik, double uzunluk, double taban, double menzil, double ivme, double hasar) {
    private static final double ON_YUZ = 2.5;

    public Dalga {
        double boy = Math.sqrt(yonX * yonX + yonZ * yonZ);
        if (!(boy > 0) || !(hiz > 0) || !(uzunluk > 0) || !(genislik > 0)) {
            throw new IllegalArgumentException("geçersiz dalga: yön, hız, uzunluk ve genişlik pozitif olmalı");
        }
        yonX /= boy;
        yonZ /= boy;
    }

    /** Varsayılan sürükleme (yükseklikle artar) ve hasarsız dalga. */
    public Dalga(double x0, double z0, double yonX, double yonZ, double baslangic, double hiz, double yukseklik,
            double genislik, double uzunluk, double taban, double menzil) {
        this(x0, z0, yonX, yonZ, baslangic, hiz, yukseklik, genislik, uzunluk, taban, menzil, 0.02 + 0.0075 * yukseklik, 0);
    }

    private double ilerleme(double t) {
        return (t - baslangic) * hiz;
    }

    /** Cephenin başlangıç çizgisine uzaklığı (blok). */
    public double cephe(double t) {
        return Math.min(ilerleme(t), menzil);
    }

    public boolean bitti(double t) {
        return ilerleme(t) > menzil + 5 * uzunluk;
    }

    /** Sütun merkezindeki dalga yüzeyi (mutlak Y); dalga yoksa NaN. */
    public double yuzey(double x, double z, double t) {
        double il = ilerleme(t);
        if (il < 0) {
            return Double.NaN;
        }
        double dx = x - x0;
        double dz = z - z0;
        double boyuna = dx * yonX + dz * yonZ;
        double yan = Math.abs(-dx * yonZ + dz * yonX);
        double s = Math.min(il, menzil) - boyuna;
        double yarim = genislik / 2;
        if (s < 0 || s > uzunluk || yan > yarim) {
            return Double.NaN;
        }
        double fs = s < ON_YUZ ? Math.sqrt(s / ON_YUZ) : 0.25 + 0.75 * Math.exp(-(s - ON_YUZ) / (uzunluk * 0.35));
        double fy = Math.sqrt(Math.min(1.0, (yarim - yan) / 8.0));
        double sonme = il > menzil ? Math.max(0, 1 - (il - menzil) / (5 * uzunluk)) : 1;
        double h = yukseklik * fs * fy * sonme;
        return h < 0.05 ? Double.NaN : taban + h;
    }

    /** Noktanın cepheye uzaklığı ({@code s}); 0 cephe, artı arkası (köpük ve sıçrantı için). */
    public double cepheyeUzaklik(double x, double z, double t) {
        return Math.min(ilerleme(t), menzil) - ((x - x0) * yonX + (z - z0) * yonZ);
    }
}
