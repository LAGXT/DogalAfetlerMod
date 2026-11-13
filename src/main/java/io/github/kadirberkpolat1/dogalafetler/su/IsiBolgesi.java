package io.github.kadirberkpolat1.dogalafetler.su;

/**
 * Sel suyunu buharlaştıran bir ısı kaynağının bölgesi: {@code (x, y, z)} bloğundan {@code yaricap} içinde sel suyu yoktur
 * (lav, ateş, kamp ateşi, yanan fırın, meşale…). Çekirdekte ({@link #CEKIRDEK} × yarıçap) tam kuru, kenara doğru yumuşakça
 * sele karışır. Yalnız selin yükselmesi söner; denizin kendi (ve ay çarpması sonrası kalıcı) seviyesi buharlaşmaz.
 * Yatay mesafeye bakılır: ısı kaynağı sel yüzeyinin yakınındaki yükseklikte aranır ({@code SelIsiTarayici}).
 */
public record IsiBolgesi(int x, int y, int z, float yaricap) {
    /** Yarıçapın bu oranı içinde bölge tam kurudur. */
    public static final double CEKIRDEK = 0.5;

    /** Sütun {@code (px, pz)} için kuruluk 0 (sel sürer) .. 1 (tam kuru). */
    public double oran(double px, double pz) {
        double dx = px - (x + 0.5);
        double dz = pz - (z + 0.5);
        double d2 = dx * dx + dz * dz;
        double r = yaricap;
        if (!(r > 0) || d2 >= r * r) {
            return 0;
        }
        double d = Math.sqrt(d2);
        double cekirdek = r * CEKIRDEK;
        if (d <= cekirdek) {
            return 1;
        }
        double u = 1 - (d - cekirdek) / (r - cekirdek);
        return u * u * (3 - 2 * u);
    }
}
