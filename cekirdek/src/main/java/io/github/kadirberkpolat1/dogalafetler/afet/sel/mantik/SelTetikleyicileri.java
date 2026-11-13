package io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik;

/**
 * Selin doğal seçimdeki olasılığını artıran etkenler: yağmur, fırtına ve süren tsunami anında; tsunaminin bitişi ve ay
 * çarpması ise geçici çarpanla (süreleri config) çekirdeğin çarpan listesine girer. Çarpanlar birbiriyle çarpılır; hava
 * durumundan yalnız en güçlüsü sayılır (fırtına yağmurun üstüne eklenmez). Saf Java'dır.
 */
public final class SelTetikleyicileri {
    /** Çekirdek çarpanının tek seferlik üst sınırı ({@code GeciciCarpan.EN_COK}). */
    public static final double CARPAN_EN_COK = 100.0;

    private SelTetikleyicileri() {
    }

    /** Anında etkenlerin çarpanları (config). */
    public record Anlik(double yagmur, double firtina, double tsunamiSururken) {
        public static final Anlik VARSAYILAN = new Anlik(2.5, 3.5, 4.0);
    }

    /** Geçici çarpan: {@code tepe} {@code sabitTick} sürer, {@code sonumTick} içinde 1'e iner. */
    public record Gecici(double tepe, long sabitTick, long sonumTick) {
        public Gecici {
            tepe = Math.max(1.0, Math.min(CARPAN_EN_COK, tepe));
            sabitTick = Math.max(0, sabitTick);
            sonumTick = Math.max(0, sonumTick);
        }

        /** Etkisiz (tepe 1) ya da süresiz çarpan çekirdeğe eklenmez. */
        public boolean etkili() {
            return tepe > 1.0 && sabitTick + sonumTick > 0;
        }
    }

    /**
     * Doğal seçimde selin anlık çarpanı.
     *
     * @param yagmur         üst dünyada yağmur yağıyor
     * @param firtina        üst dünyada fırtına var (yağmuru da kapsar)
     * @param tsunamiSururken bir tsunami sürüyor (AKTIF ya da sönüm)
     */
    public static double anlikCarpan(boolean yagmur, boolean firtina, boolean tsunamiSururken, Anlik a) {
        double hava = firtina ? a.firtina() : yagmur ? a.yagmur() : 1.0;
        double tsunami = tsunamiSururken ? a.tsunamiSururken() : 1.0;
        return Math.max(1.0, hava) * Math.max(1.0, tsunami);
    }
}
