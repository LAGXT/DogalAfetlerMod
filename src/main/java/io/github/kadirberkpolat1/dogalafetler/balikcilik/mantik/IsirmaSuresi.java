package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.LootAgirliklari.Kategori;

/**
 * Balıkçılıkta balığın ısırma süresi. Toplam süre olta suya düştüğünden ısırmaya kadardır. Normal olta 10–60 sn (200–1200
 * tick), Kutup Oltası 2 kat hızlı olduğundan {@link #olcekle} ile yarısı (5–30 sn); Lure, taban ve nadirlik dilimleri
 * aynı oranla küçülür. Buz deliğinde çıkacak şeyin nadirliği önce seçilir ve süre o katmanın dilimine düşer: yaygın (ve çöp)
 * aralığın en hızlı beşte biri, orta (ve eşya) ortası, nadir (hazine, özel balık) en uzun dilimi. Açık denizde katman
 * seçilmez, süre tüm aralıktan çekilir. Lure kısaltır, taban altına inilmez. Son {@link #YAKLASMA_TICK} tick vanilla'nın
 * "balık yaklaşıyor" evresidir (kabarcık parçacığı, sonra şapırtı ve ısırma).
 */
public final class IsirmaSuresi {
    /** Vanilla yaklaşma evresi (vanilla 20–80 tick rastgele; burada sabit, ısırma anı toplam süreye denk gelir). */
    public static final int YAKLASMA_TICK = 40;

    /** Aralık diliminin başı ve sonu (0–1): yaygın en hızlı, nadir en yavaş. */
    private static final double[] YAYGIN_DILIMI = {0.0, 0.2};
    private static final double[] ORTA_DILIMI = {0.2, 0.6};
    private static final double[] NADIR_DILIMI = {0.6, 1.0};

    /**
     * @param enAzTick          Lure'suz alt sınır (en yaygın şeyin en hızlı ısırması)
     * @param enCokTick         Lure'suz üst sınır (en nadir şeyin en yavaş ısırması)
     * @param lureKisaltmaTick  Lure seviyesi başına kısaltma
     * @param tabanTick         en kısa toplam
     */
    public record Ayarlar(int enAzTick, int enCokTick, int lureKisaltmaTick, int tabanTick) {
        public Ayarlar {
            if (enAzTick <= 0 || enCokTick < enAzTick || lureKisaltmaTick < 0 || tabanTick <= 0) {
                throw new IllegalArgumentException("ısırma süresi ayarı geçersiz");
            }
        }
    }

    /** Ayarların hepsini {@code carpan} ile küçültür/büyütür (en az 1 tick); Kutup Oltası için 0,5. */
    public static Ayarlar olcekle(Ayarlar a, double carpan) {
        int enAz = Math.max(1, (int) Math.round(a.enAzTick() * carpan));
        int enCok = Math.max(enAz, (int) Math.round(a.enCokTick() * carpan));
        return new Ayarlar(enAz, enCok, Math.max(0, (int) Math.round(a.lureKisaltmaTick() * carpan)),
                Math.max(1, (int) Math.round(a.tabanTick() * carpan)));
    }

    private IsirmaSuresi() {
    }

    /** {@code u} ∈ [0, 1) zarıyla toplam süre (tüm aralık); Lure seviyesi başına kısalır, tabandan kısa olmaz. */
    public static int toplamTick(double u, int lure, Ayarlar a) {
        int aralik = a.enCokTick() - a.enAzTick() + 1;
        int ek = Math.min(aralik - 1, (int) Math.floor(Math.max(0.0, Math.min(u, Math.nextDown(1.0))) * aralik));
        int t = a.enAzTick() + ek - Math.max(0, lure) * a.lureKisaltmaTick();
        return Math.max(a.tabanTick(), t);
    }

    /** Kategorinin dilimindeki toplam süre: yaygın hızlı, nadir uzun. */
    public static int toplamTick(double u, int lure, Ayarlar a, Kategori kategori) {
        double[] d = dilim(kategori);
        int aralik = a.enCokTick() - a.enAzTick();
        int alt = a.enAzTick() + (int) Math.round(aralik * d[0]);
        int ust = a.enAzTick() + (int) Math.round(aralik * d[1]);
        return toplamTick(u, lure, new Ayarlar(alt, Math.max(alt, ust), a.lureKisaltmaTick(), a.tabanTick()));
    }

    /** Kategorinin aralık dilimi (0–1): [başlangıç, bitiş]. */
    static double[] dilim(Kategori kategori) {
        return switch (kategori) {
            case YAYGIN, COP -> YAYGIN_DILIMI;
            case ORTA, ESYA -> ORTA_DILIMI;
            case NADIR, HAZINE, OZEL -> NADIR_DILIMI;
        };
    }

    /** Yaklaşma evresine kadar beklenen tick (en az 1). */
    public static int beklemeTick(int toplamTick) {
        return Math.max(1, toplamTick - YAKLASMA_TICK);
    }
}
