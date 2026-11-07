package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik;

/**
 * Güneş patlamasının meteor yoğunluğu (oyuncu başına dakikada; kullanıcı isteği 2026-11-07, {@code .vibe/kararlar/meteor-2.md}).
 * Öncü yağmur ön-uyarının son {@code oncuTick}'inde başlar ve sirenin (güneş büyürken) sonuna kadar {@code oncuBas}'tan
 * {@code yagmur}'a doğrusal artar; AKTIF'in başında {@code ilkGunTick} (istek 27: 10 dk), sonraki her günün başında
 * {@code gunlukTick} boyunca yağmur yoğunluğu, arasında bölüm D'nin doğrusal arka plan eğrisi
 * ({@link GunesPatlamasiEgrileri#dakikaBasinaMeteor}) sürer. Süreler ölçekli tick'tir ve afetin AKTIF sayacından ölçülür
 * (uyku sayılmaz); yoğunluk gerçek dakikadır. Afet pencereden önce biterse yağmur afetle biter.
 */
public final class MeteorYagmuruEgrisi {
    private MeteorYagmuruEgrisi() {
    }

    /** Öncü pencerenin ön-uyarıdaki payı (kısa ön-uyarıda hepsi). */
    static long onUyariPayi(long onUyariSure, long oncuTick) {
        return Math.max(0, Math.min(oncuTick, onUyariSure));
    }

    /** Ön-uyarıda, {@code kalan} tick kala: pencereden önce 0, pencerede doğrusal artış. */
    public static double onUyari(long kalan, long onUyariSure, long sirenSure, long oncuTick, double oncuBas,
            double yagmur) {
        long pay = onUyariPayi(onUyariSure, oncuTick);
        if (pay <= 0 || kalan > pay) {
            return 0;
        }
        return dogrusal(pay - Math.max(0, kalan), pay + sirenSure, oncuBas, yagmur);
    }

    /** Sirende, başından {@code gecen} tick sonra: ön-uyarıdaki artışın devamı, siren sonunda {@code yagmur}. */
    public static double siren(long gecen, long onUyariSure, long sirenSure, long oncuTick, double oncuBas,
            double yagmur) {
        long pay = onUyariPayi(onUyariSure, oncuTick);
        return dogrusal(pay + Math.max(0, Math.min(gecen, sirenSure)), pay + sirenSure, oncuBas, yagmur);
    }

    /** Günün başındaki yağmur penceresinde mi ({@code aktifGecen}: AKTIF'te geçen; ilk gün {@code ilkGunTick}). */
    public static boolean yagmurda(long aktifGecen, long ilkGunTick, long gunlukTick) {
        long pencere = aktifGecen < GunesPatlamasiEgrileri.GUN_TICK ? ilkGunTick : gunlukTick;
        return pencere > 0 && Math.floorMod(aktifGecen, GunesPatlamasiEgrileri.GUN_TICK) < pencere;
    }

    /** AKTIF: yağmur penceresinde arka plan ile yağmurun büyüğü, dışında arka plan. */
    public static double aktif(long aktifGecen, long aktifSure, double arkaBas, double arkaSon, long ilkGunTick,
            long gunlukTick, double yagmur) {
        double arka = GunesPatlamasiEgrileri.dakikaBasinaMeteor(aktifGecen, aktifSure, arkaBas, arkaSon);
        return yagmurda(aktifGecen, ilkGunTick, gunlukTick) ? Math.max(arka, yagmur) : arka;
    }

    private static double dogrusal(long gecen, long toplam, double bas, double son) {
        if (toplam <= 0) {
            return son;
        }
        return bas + (son - bas) * Math.max(0, Math.min(1, gecen / (double) toplam));
    }
}
