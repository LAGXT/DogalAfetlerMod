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
        double a = Math.max(0, Math.min(ic, dis));
        double b = Math.max(a, dis);
        double yaricap = Math.sqrt(a * a + r.nextDouble() * (b * b - a * a));
        double aci = r.nextDouble() * 2 * Math.PI;
        return new double[] {Math.cos(aci) * yaricap, Math.sin(aci) * yaricap};
    }
}
