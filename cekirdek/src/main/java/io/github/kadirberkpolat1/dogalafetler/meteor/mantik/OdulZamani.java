package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

/**
 * Meteor ödüllerinin süreleri (madde 13c): çarpmadan sonraki ilk {@value #SICAK_SANIYE} sn "sıcak", yerde
 * {@value #OMUR_SANIYE} sn (10 dk) sonra yok olur. Sıcaklık eşyanın NBT'sinde bitiş oyun tick'i olarak tutulur.
 */
public final class OdulZamani {
    public static final int SICAK_SANIYE = 20;
    public static final int OMUR_SANIYE = 600;
    /** Bitiş etiketi olmayan (soğuk) parça. */
    public static final long SOGUK = Long.MIN_VALUE;

    private OdulZamani() {
    }

    public static long sicakBitis(long simdi, int sicakSaniye) {
        return simdi + 20L * Math.max(0, sicakSaniye);
    }

    public static boolean sicakMi(long simdi, long bitis) {
        return bitis != SOGUK && simdi < bitis;
    }

    public static int omurTick(int saniye) {
        return 20 * Math.max(1, saniye);
    }
}
