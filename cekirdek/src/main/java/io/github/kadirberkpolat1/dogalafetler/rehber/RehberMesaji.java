package io.github.kadirberkpolat1.dogalafetler.rehber;

import io.github.kadirberkpolat1.dogalafetler.rehber.mantik.KonusmaRitmi;

/**
 * Karakterin söylediği üç rehber cümlesi. {@link #anahtar()} hem ağ paketinin hem oyuncu NBT'sindeki "söylendi" bayrağının
 * adıdır (kalıcı: yeniden adlandırma bayrakları sıfırlar); metinler tek yerdedir, dil dosyaları buradan üretilir.
 */
public enum RehberMesaji {
    /** Buzul kıyameti başladığında. */
    BALIK("balik", "I think I should catch some fish", "Sanırım Biraz Balık Tutmalıyım"),
    /** Kıyamette buza boş elle sağ tık. */
    ALET("alet", "I need a tool for this.", "Bunun İçin Bir Alete İhtiyacım Var."),
    /** Kıyamette buza kazma ya da baltayla, ayakta sağ tık. */
    EGIL("egil", "I should crouch.", "Eğilmeliyim.");

    private final String anahtar;
    private final String ingilizce;
    private final String turkce;
    private final int[] puflar;

    RehberMesaji(String anahtar, String ingilizce, String turkce) {
        this.anahtar = anahtar;
        this.ingilizce = ingilizce;
        this.turkce = turkce;
        this.puflar = KonusmaRitmi.puflar(turkce);
    }

    public String anahtar() {
        return anahtar;
    }

    /** Dil anahtarı: {@code message.dogalafetler.rehber.<anahtar>}. */
    public String ceviriAnahtari() {
        return "message.dogalafetler.rehber." + anahtar;
    }

    public String ingilizce() {
        return ingilizce;
    }

    public String turkce() {
        return turkce;
    }

    /** Buhar tick'leri ({@link KonusmaRitmi#puflar}); dizi paylaşılır, değiştirilmez. */
    public int[] puflar() {
        return puflar;
    }

    /** Anahtardan mesaj; tanınmayan anahtar {@code null} (sürüm farkı paketi sessizce yok sayılır). */
    public static RehberMesaji anahtarlaBul(String anahtar) {
        for (RehberMesaji m : values()) {
            if (m.anahtar.equals(anahtar)) {
                return m;
            }
        }
        return null;
    }
}
