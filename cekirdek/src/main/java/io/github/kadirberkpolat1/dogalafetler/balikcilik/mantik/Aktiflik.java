package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

/**
 * Buz altı balıkçılığın açık olduğu yer (madde 15): suyun üstü buzken ve ya global donma ilerlemesi eşiği geçmişken
 * ya da kızıl cüce döneminde (global ilerleme düşse bile). Kızıl cücede "kalıcı kış bölgeleri ve hâlâ donmuş su
 * yüzeyleri" aynı koşula iner: kış bölgesinde de deliğin açılması için su üstü buz gerekir, buz varsa yeter.
 */
public final class Aktiflik {
    private Aktiflik() {
    }

    /**
     * @param donma     global donma ilerlemesi 0–100
     * @param esik      config eşiği 0–100
     * @param kizilCuce kızıl cüce dönemi (çözülme dönemi açık ya da güneş evresi KIZIL_CUCE)
     * @param buzYuzeyi blok su üstündeki buz ({@code DonmaApi.buzYuzeyiMi})
     */
    public static boolean aktifMi(double donma, double esik, boolean kizilCuce, boolean buzYuzeyi) {
        return buzYuzeyi && (donma >= esik || kizilCuce);
    }
}
