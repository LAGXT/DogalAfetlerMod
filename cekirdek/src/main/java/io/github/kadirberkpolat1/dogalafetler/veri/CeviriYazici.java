package io.github.kadirberkpolat1.dogalafetler.veri;

/** Bir dil anahtarının İngilizce ({@code en_us}) ve Türkçe ({@code tr_tr}) metnini birlikte yazar. */
@FunctionalInterface
public interface CeviriYazici {
    void ekle(String anahtar, String ingilizce, String turkce);
}
