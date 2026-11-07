package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Güneş patlaması AKTIF boyunca gün saatinin sabit tutulması (gökyüzü-2): başta bir sonraki öğleye (ileri; saat geri
 * sarılmaz) geçilir, sonra vanilla'nın tick başına +1'i geri alınır. Başka bir değişiklik ({@code /time set}, uyku, başka
 * mod) dışarıdan sayılır ve yeni sabit olur. {@code doDaylightCycle}'a dokunulmaz; oyun günü
 * ({@code getGameTime() + uykuBirikimi}, K-C29) gün saatinden bağımsız sürer. Minecraft sınıfı kullanmaz.
 */
public final class GunSaatiSabitleme {
    public static final long GUN_TICK = 24_000L;
    /** Vanilla öğlesi (güneş tepede). */
    public static final long OGLE = 6_000L;

    private GunSaatiSabitleme() {
    }

    /** Sabitlemenin ilk hedefi: {@code gunSaati}'nden küçük olmayan ilk öğle. */
    public static long ilkHedef(long gunSaati) {
        long ogle = Math.floorDiv(gunSaati, GUN_TICK) * GUN_TICK + OGLE;
        return gunSaati <= ogle ? ogle : ogle + GUN_TICK;
    }

    /**
     * Bu tick sonunda tutulacak saat: gün saati hedefte ya da gün döngüsü açıkken vanilla'nın bir tick ilerlettiği yerdeyse
     * hedef korunur; başka her değer dış değişikliktir ve yeni hedef olur.
     */
    public static long tut(long hedef, long gunSaati, boolean gunDongusu) {
        if (gunSaati == hedef || gunDongusu && gunSaati == hedef + 1) {
            return hedef;
        }
        return gunSaati;
    }
}
