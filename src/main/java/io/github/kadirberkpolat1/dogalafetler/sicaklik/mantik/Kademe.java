package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Vücut sıcaklığı kademesi, soğuktan sıcağa sıralı (ordinal soğuktan sıcağa artar; pakette bayt olarak gider).
 * Etkiler: SOGUK yavaşlama, COK_SOGUK + buz kenarı, DONUYOR + donma hasarı; SICAK titreme, COK_SICAK güçlü titreme,
 * YANIYOR + aşırı ısınma hasarı (madde 7c). Oyuncuya görünen durum {@link #durum()} (S3-04).
 */
public enum Kademe {
    DONUYOR,
    COK_SOGUK,
    SOGUK,
    NOTR,
    SICAK,
    COK_SICAK,
    YANIYOR;

    private static final Kademe[] DEGERLER = values();

    public static Kademe sirayla(int sira) {
        return DEGERLER[Math.max(0, Math.min(DEGERLER.length - 1, sira))];
    }

    /** SOGUK üşüme, COK_SOGUK/DONUYOR donma, SICAK bunalma, COK_SICAK/YANIYOR yanma. */
    public SicaklikDurumu durum() {
        return switch (this) {
            case DONUYOR, COK_SOGUK -> SicaklikDurumu.DONMA;
            case SOGUK -> SicaklikDurumu.USUME;
            case NOTR -> SicaklikDurumu.NORMAL;
            case SICAK -> SicaklikDurumu.BUNALMA;
            case COK_SICAK, YANIYOR -> SicaklikDurumu.YANMA;
        };
    }

    public boolean hasarli() {
        return this == DONUYOR || this == YANIYOR;
    }
}
