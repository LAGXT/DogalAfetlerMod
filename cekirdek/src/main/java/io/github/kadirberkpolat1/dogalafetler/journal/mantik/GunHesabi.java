package io.github.kadirberkpolat1.dogalafetler.journal.mantik;

/**
 * "Hayatta kalınan gün" hesabı (saf Java). Gün, modun oyun günüdür ({@code OyunGunu}: dünya yaşı, uyku birikimi dahil);
 * her yaşamın başlangıç günü kaydedilir, yaşam günü = şimdiki gün − başlangıç günü. Defter ilk kez bir oyuncuya bakarken
 * başlangıç gün, vanilla "son ölümden beri" sayacından (tick) geriye doğru kestirilir: var olan dünyada da doğru başlar.
 */
public final class GunHesabi {
    public static final long GUN_TICK = 24_000L;

    private GunHesabi() {
    }

    /** Bu yaşamın gün sayısı (başlangıç gelecekte kalırsa, örneğin gün komutla geri alındıysa 0). */
    public static long yasamGunu(long dunyaGunu, long baslangicGunu) {
        return Math.max(0, dunyaGunu - baslangicGunu);
    }

    /** Son ölümden beri geçen vanilla tick sayısından yaşamın başlangıç günü kestirimi. */
    public static long baslangicKestirimi(long dunyaGunu, long sonOlumdenBeriTick) {
        return dunyaGunu - Math.floorDiv(Math.max(0, sonOlumdenBeriTick), GUN_TICK);
    }

    /** Kayıtlı en uzun yaşam ile süren yaşamın büyüğü. */
    public static long enUzun(long kayitliEnUzun, long suren) {
        return Math.max(kayitliEnUzun, suren);
    }
}
