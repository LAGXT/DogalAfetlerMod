package io.github.kadirberkpolat1.dogalafetler.su;

/**
 * Tsunami öncesi suyun çekilmesi: {@code (x, z)} merkezli {@code yaricap} içinde gerçek su yüzeyi görsel olarak
 * {@code derinlik} kadar iner (kenara doğru yumuşak sıfırlanır), {@code sure} tick'te tamamlanır. İstemci gerçek suyun
 * üst {@link #gizlenenKatman} bloğunu çizmez, yerine alçalmış sanal yüzeyi çizer; gerçek bloklar değişmez.
 */
public record Cekilme(double x, double z, double yaricap, double derinlik, long baslangic, long sure) {
    /** Merkezde 1, kenarda 0 (smoothstep). */
    public double oran(double px, double pz) {
        double d = Math.sqrt((px - x) * (px - x) + (pz - z) * (pz - z));
        if (d >= yaricap) {
            return 0;
        }
        double u = 1 - d / yaricap;
        return u * u * (3 - 2 * u);
    }

    public boolean icinde(double px, double pz) {
        return oran(px, pz) > 0;
    }

    public double dusus(double px, double pz, double t) {
        if (t <= baslangic) {
            return 0;
        }
        double zaman = sure <= 0 ? 1 : Math.min(1, (t - baslangic) / sure);
        return derinlik * oran(px, pz) * zaman;
    }

    /** Sütunda gizlenecek gerçek su bloğu sayısı (zamandan bağımsız; chunk bir kez yeniden derlenir). */
    public int gizlenenKatman(double px, double pz) {
        double o = oran(px, pz);
        return o <= 0 ? 0 : (int) Math.ceil(derinlik * o - 1e-9);
    }
}
