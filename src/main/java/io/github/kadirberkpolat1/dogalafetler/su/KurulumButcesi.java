package io.github.kadirberkpolat1.dogalafetler.su;

/**
 * Tick başına zaman bütçesi (SU-26, SU-33): bir tick'te bütçeyi aşan süre sonraki tick'lerin bütçesinden düşülür, böylece
 * uzun sürede tick başına ortalama bütçeyi geçmez. İş parçaları kısa tutulursa tek tick'in aşımı bir parçayla sınırlıdır.
 * Yakın halka ve uzak LOD aynı bütçeyi paylaşır (SU-38): ikisi de bekliyorsa bu tick'te daha az harcayanın parçası çalışır.
 * Saf mantık, tek thread.
 */
public final class KurulumButcesi {
    public static final int YOK = 0;
    public static final int YAKIN = 1;
    public static final int UZAK = 2;
    private long tick = Long.MIN_VALUE;
    private long butce;
    private long harcanan;
    private final long[] isNs = new long[3];

    /** Yeni tick'te sayacı açar: önceki tick'in aşımı borç olarak taşınır. Aynı tick'te tekrar çağrı etkisizdir. */
    public void tickBasla(long yeniTick, long butceNs) {
        if (yeniTick == tick) {
            return;
        }
        long borc = tick == Long.MIN_VALUE ? 0 : Math.max(0, harcanan - butce);
        tick = yeniTick;
        butce = butceNs;
        harcanan = borc;
        isNs[YAKIN] = 0;
        isNs[UZAK] = 0;
    }

    /** Sıradaki parçanın işi: ikisi de bekliyorsa bu tick'te daha az harcayan (eşitlikte yakın halka). */
    public int siradaki(boolean yakin, boolean uzak) {
        if (yakin && uzak) {
            return isNs[UZAK] < isNs[YAKIN] ? UZAK : YAKIN;
        }
        return yakin ? YAKIN : uzak ? UZAK : YOK;
    }

    public void harca(int is, long ns) {
        harcanan += ns;
        isNs[is] += ns;
    }

    public boolean calisabilir() {
        return harcanan < butce;
    }

    public void harca(long ns) {
        harcanan += ns;
    }

    /** Bu tick'te borç dahil harcanan süre (ns). */
    public long harcanan() {
        return harcanan;
    }

    public long kalan() {
        return Math.max(0, butce - harcanan);
    }
}
