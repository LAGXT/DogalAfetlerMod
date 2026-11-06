package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

/**
 * Madde 12 ödülleri: her başarım XP verir (advancement JSON'u, {@link BasarimTuru#varsayilanXp()}); "hedef" başarımlar
 * ayrıca {@value #HEDEF_YIYECEK_EN_AZ}–{@value #HEDEF_YIYECEK_EN_COK} sıcak yiyecek ({@code dogalafetler:rewards/hedef}),
 * "meydan okuma" başarımları daha büyük XP ve kozmetik ünvan ({@link Basarimlar#unvan()}) verir.
 */
public final class OdulTablosu {
    public static final int HEDEF_YIYECEK_EN_AZ = 1;
    public static final int HEDEF_YIYECEK_EN_COK = 2;

    private OdulTablosu() {
    }

    /** Config XP'si ile JSON'daki varsayılan XP arasındaki fark; kazanılırken eklenir (negatifse geri alınır). */
    public static int xpFarki(BasarimTuru tur, int ayarXp) {
        return ayarXp - tur.varsayilanXp();
    }
}
