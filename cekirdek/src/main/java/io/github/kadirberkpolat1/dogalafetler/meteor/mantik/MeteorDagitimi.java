package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import java.util.random.RandomGenerator;

/**
 * Meteorları çevrim içi oyunculara sırayla (round-robin) dağıtır: her oyuncu aynı sayıda meteor alır, fark en çok
 * birdir (bölüm D "tek oyuncuya yığılma olmasın"). Düşüş noktası oyuncunun çevresindeki halkada alana göre düzgün
 * rastgele bir ofsettir.
 */
public final class MeteorDagitimi {
    private int sira;

    public MeteorDagitimi(int baslangic) {
        this.sira = Math.max(0, baslangic);
    }

    /** Sıradaki oyuncunun indeksi (0 ≤ i &lt; {@code oyuncuSayisi}); oyuncu yoksa 0. */
    public int siradaki(int oyuncuSayisi) {
        if (oyuncuSayisi <= 0) {
            return 0;
        }
        int i = Math.floorMod(sira, oyuncuSayisi);
        sira = i + 1;
        return i;
    }

    /** {@code ic}–{@code dis} blok halkasında alana göre düzgün bir yatay ofset: {@code {dx, dz}}. */
    public static double[] ofset(RandomGenerator r, double ic, double dis) {
        double yaricap = yaricap(r, ic, dis);
        double aci = r.nextDouble() * 2 * Math.PI;
        return new double[] {Math.cos(aci) * yaricap, Math.sin(aci) * yaricap};
    }

    /**
     * Bakış yönüne ağırlıklı halka ofseti (MT2-28): {@code onOrani} olasılıkla yön bakışın ±90°'lik ön yarısına, kalanı
     * arka yarısına düşer; yarı içinde açı, halkada yarıçap alana göre düzgün. {@code bakisAcisi} radyan, {@link #ofset}
     * ekseninde (0 = +X, π/2 = +Z); {@code onOrani} 0,5'te dağılım yönsüz {@link #ofset} ile aynıdır.
     */
    public static double[] ofset(RandomGenerator r, double ic, double dis, double bakisAcisi, double onOrani) {
        double yaricap = yaricap(r, ic, dis);
        boolean on = r.nextDouble() < onOrani;
        double aci = bakisAcisi + (r.nextDouble() - 0.5) * Math.PI + (on ? 0 : Math.PI);
        return new double[] {Math.cos(aci) * yaricap, Math.sin(aci) * yaricap};
    }

    /** Minecraft yaw'ının (derece; 0 = +Z'ye, 90 = −X'e bakar) {@link #ofset} eksenindeki bakış açısı (radyan). */
    public static double bakisAcisi(float yRot) {
        return Math.toRadians(yRot + 90.0);
    }

    private static double yaricap(RandomGenerator r, double ic, double dis) {
        double a = Math.max(0, Math.min(ic, dis));
        double b = Math.max(a, dis);
        return Math.sqrt(a * a + r.nextDouble() * (b * b - a * a));
    }
}
