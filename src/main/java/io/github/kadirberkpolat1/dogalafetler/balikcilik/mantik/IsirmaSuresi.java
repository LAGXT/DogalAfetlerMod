package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

/**
 * Buz deliğinde balığın ısırma süresi (madde 15: "normalden biraz uzun, 30–60 sn, Lure büyüsü hızlandırsın").
 * Toplam süre olta deliğe düştüğünden ısırmaya kadardır; son {@link #YAKLASMA_TICK} tick vanilla'nın "balık yaklaşıyor"
 * evresidir (kabık parçacığı, sonra şapırtı ve ısırma).
 */
public final class IsirmaSuresi {
    /** Vanilla yaklaşma evresi (vanilla 20–80 tick rastgele; burada sabit, ısırma anı toplam süreye denk gelir). */
    public static final int YAKLASMA_TICK = 40;

    /**
     * @param enAzTick          Lure'suz alt sınır
     * @param enCokTick         Lure'suz üst sınır
     * @param lureKisaltmaTick  Lure seviyesi başına kısaltma (vanilla seviye başına 100 tick)
     * @param tabanTick         en kısa toplam
     */
    public record Ayarlar(int enAzTick, int enCokTick, int lureKisaltmaTick, int tabanTick) {
        public Ayarlar {
            if (enAzTick <= 0 || enCokTick < enAzTick || lureKisaltmaTick < 0 || tabanTick <= 0) {
                throw new IllegalArgumentException("ısırma süresi ayarı geçersiz");
            }
        }
    }

    private IsirmaSuresi() {
    }

    /** {@code u} ∈ [0, 1) zarıyla toplam süre; Lure seviyesi başına kısalır, tabandan kısa olmaz. */
    public static int toplamTick(double u, int lure, Ayarlar a) {
        int aralik = a.enCokTick() - a.enAzTick() + 1;
        int ek = Math.min(aralik - 1, (int) Math.floor(Math.max(0.0, Math.min(u, Math.nextDown(1.0))) * aralik));
        int t = a.enAzTick() + ek - Math.max(0, lure) * a.lureKisaltmaTick();
        return Math.max(a.tabanTick(), t);
    }

    /** Yaklaşma evresine kadar beklenen tick (en az 1). */
    public static int beklemeTick(int toplamTick) {
        return Math.max(1, toplamTick - YAKLASMA_TICK);
    }
}
