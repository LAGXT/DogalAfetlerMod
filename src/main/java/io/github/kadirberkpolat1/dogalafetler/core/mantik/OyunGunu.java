package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/**
 * Oyun günü = dünya yaşı, oyuncunun yaşadığı gün (madde 10, bölüm D): {@code (oyunZamani + uykuBirikimi) / 24000 + ofset}.
 * {@code oyunZamani} dünyanın toplam tick sayısıdır ({@code Level.getGameTime()}): {@code /time set} onu geri ya da ileri
 * almaz, {@code doDaylightCycle=false} iken de ilerler. Uyku geceyi atlar ama oyun zamanını ilerletmez; atlanan gün saati
 * {@code uykuBirikimi}'nde toplanır ({@link #uykuAtlamasi}), böylece uyuyan sunucuda gün görünen günle aynı hızda ilerler.
 * Ofset {@code /dogalafet gun} ile ayarlanır.
 */
public final class OyunGunu {
    public static final long GUN_TICK = 24_000L;

    private OyunGunu() {
    }

    public static long gun(long oyunZamani, long uykuBirikimi, long ofset) {
        return Math.max(0, Math.floorDiv(oyunZamani + uykuBirikimi, GUN_TICK) + ofset);
    }

    /** {@code istenenGun}'ü veren ofset. */
    public static long ofset(long istenenGun, long oyunZamani, long uykuBirikimi) {
        return istenenGun - Math.floorDiv(oyunZamani + uykuBirikimi, GUN_TICK);
    }

    /** Uykunun atladığı gün saati: uyanış saati ile yatış saati arası (geri gidiş birikmez). */
    public static long uykuAtlamasi(long yeniGunSaati, long eskiGunSaati) {
        return Math.max(0, yeniGunSaati - eskiGunSaati);
    }
}
