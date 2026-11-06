package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

/**
 * Buz altı balıkçılığın açık olduğu yer (madde 15): suyun üstü buzken ve ya global donma ilerlemesi eşiği geçmişken
 * ya da kızıl cüce döneminde (global ilerleme düşse bile) kalıcı kış bölgesinde veya hâlâ donmuş bir su yüzeyinde.
 */
public final class Aktiflik {
    /** Kalıcı kış bölgesi sayılan en düşük sütun ağırlığı (donma §A {@code kisAgirligi}, maske sınırı 0,5). */
    public static final double KIS_ESIGI = 0.5;

    private Aktiflik() {
    }

    /**
     * @param donma           global donma ilerlemesi 0–100
     * @param esik            config eşiği 0–100
     * @param kizilCuce       kızıl cüce dönemi (çözülme dönemi açık ya da güneş evresi KIZIL_CUCE)
     * @param kisAgirligi     sütunun kalıcı kış ağırlığı 0–1
     * @param buzYuzeyi       blok su üstündeki buz ({@code DonmaApi.buzYuzeyiMi})
     */
    public static boolean aktifMi(double donma, double esik, boolean kizilCuce, double kisAgirligi, boolean buzYuzeyi) {
        if (!buzYuzeyi) {
            return false;
        }
        // Kızıl cücede donmuş yüzey (buzYuzeyi) tek başına yeter; kış bölgesi madde 15'in açık sayımı için ayrı yazılır.
        return donma >= esik || kizilCuce && (kisBolgesiMi(kisAgirligi) || buzYuzeyi);
    }

    public static boolean kisBolgesiMi(double kisAgirligi) {
        return kisAgirligi >= KIS_ESIGI;
    }
}
