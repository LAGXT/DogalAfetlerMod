package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Vücut sıcaklığı kademesi, soğuktan sıcağa sıralı (ordinal soğuktan sıcağa artar; pakette bayt olarak gider).
 * Etkiler: SOGUK yavaşlama, COK_SOGUK + buz kenarı, DONUYOR + donma hasarı; SICAK titreme, COK_SICAK güçlü titreme,
 * YANIYOR + aşırı ısınma hasarı (madde 7c).
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

    public boolean hasarli() {
        return this == DONUYOR || this == YANIYOR;
    }
}
