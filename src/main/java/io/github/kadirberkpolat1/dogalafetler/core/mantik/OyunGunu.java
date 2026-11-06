package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/**
 * Oyun günü = dünya yaşı (madde 10): {@code oyunZamani / 24000 + ofset}. {@code oyunZamani} dünyanın toplam tick sayısıdır
 * ({@code Level.getGameTime()}); gün saatinden ({@code getDayTime()}) farklı olarak {@code /time set} ve uyku onu geri ya
 * da ileri almaz, {@code doDaylightCycle=false} iken de ilerler. Ofset {@code /dogalafet gun} ile ayarlanır.
 */
public final class OyunGunu {
    public static final long GUN_TICK = 24_000L;

    private OyunGunu() {
    }

    public static long gun(long oyunZamani, long ofset) {
        return Math.max(0, Math.floorDiv(oyunZamani, GUN_TICK) + ofset);
    }

    /** {@code istenenGun}'ü veren ofset. */
    public static long ofset(long istenenGun, long oyunZamani) {
        return istenenGun - Math.floorDiv(oyunZamani, GUN_TICK);
    }
}
