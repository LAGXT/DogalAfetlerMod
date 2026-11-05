package io.github.kadirberkpolat1.dogalafetler.su;

/**
 * İlerleyen tsunami dalgası: cephe çizgisi {@code (x0, z0)}'dan {@code yon} doğrultusunda {@code hiz} blok/tick ile
 * ilerler. Cephenin {@code s} blok arkasındaki yüzey {@code taban + H * f(s) * g(yan) * sonme}: önde dik yükselen bir
 * duvar, arkada uzun bir kuyruk. {@code menzil}'e varınca durur ve {@code 5 * uzunluk} ilerleme süresinde söner.
 * Parametreler ağdan bir kez gider; istemci aynı formülle hesaplar.
 */
public final class TsunamiDalgasi {
    private static final double ON_YUZ = 2.5;

    private final double x0;
    private final double z0;
    private final double yonX;
    private final double yonZ;
    private final double baslangic;
    private final double hiz;
    private final double yukseklik;
    private final double genislik;
    private final double uzunluk;
    private final double taban;
    private final double menzil;

    public TsunamiDalgasi(double x0, double z0, double yonX, double yonZ, double baslangic, double hiz, double yukseklik,
            double genislik, double uzunluk, double taban, double menzil) {
        double boy = Math.sqrt(yonX * yonX + yonZ * yonZ);
        this.x0 = x0;
        this.z0 = z0;
        this.yonX = yonX / boy;
        this.yonZ = yonZ / boy;
        this.baslangic = baslangic;
        this.hiz = hiz;
        this.yukseklik = yukseklik;
        this.genislik = genislik;
        this.uzunluk = uzunluk;
        this.taban = taban;
        this.menzil = menzil;
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

    /** Tam batmış varlığa uygulanan yatay ivme (blok/tick²). */
    public double ivme() {
        return 0.02 + 0.0075 * yukseklik;
    }

    public double x0() {
        return x0;
    }

    public double z0() {
        return z0;
    }

    public double yonX() {
        return yonX;
    }

    public double yonZ() {
        return yonZ;
    }

    public double baslangic() {
        return baslangic;
    }

    public double hiz() {
        return hiz;
    }

    public double yukseklik() {
        return yukseklik;
    }

    public double genislik() {
        return genislik;
    }

    public double uzunluk() {
        return uzunluk;
    }

    public double taban() {
        return taban;
    }

    public double menzil() {
        return menzil;
    }
}
