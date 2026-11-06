package io.github.kadirberkpolat1.dogalafetler.su;

/**
 * Tick başına zaman bütçesi (SU-26, SU-33): bir tick'te bütçeyi aşan süre sonraki tick'lerin bütçesinden düşülür, böylece
 * uzun sürede tick başına ortalama bütçeyi geçmez. İş parçaları kısa tutulursa tek tick'in aşımı bir parçayla sınırlıdır.
 * Saf mantık, tek thread.
 */
public final class KurulumButcesi {
    private long tick = Long.MIN_VALUE;
    private long butce;
    private long harcanan;

    /** Yeni tick'te sayacı açar: önceki tick'in aşımı borç olarak taşınır. Aynı tick'te tekrar çağrı etkisizdir. */
    public void tickBasla(long yeniTick, long butceNs) {
        if (yeniTick == tick) {
            return;
        }
        long borc = tick == Long.MIN_VALUE ? 0 : Math.max(0, harcanan - butce);
        tick = yeniTick;
        butce = butceNs;
        harcanan = borc;
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
