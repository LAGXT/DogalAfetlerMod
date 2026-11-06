package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Afet hedefi olacak oyuncu sırası (madde 3, 4, N14). İki ya da daha çok aday varsa son hedef listeden çıkar (aynı oyuncu art
 * arda hedef alınmaz); kalanlar karıştırılır ve son afet merkezine yakın olmayanlar öne alınır (bir yerin çevresinde art arda
 * afet çıkmasın). Yönetici sıradaki ilk uygun yeri olan oyuncuyu seçer; hiçbiri uygun değilse afet ertelenir.
 */
public final class HedefSecici {
    /**
     * Hedef adayı.
     *
     * @param kimlik            oyuncu kimliği
     * @param sonMerkezeYakin   son afetin merkezine yakın mı (tercihte sona atılır)
     */
    public record Aday<T>(T kimlik, boolean sonMerkezeYakin) {
    }

    private HedefSecici() {
    }

    /** Denenecek hedeflerin sırası. {@code sonHedef} {@code null} olabilir. */
    public static <T> List<T> sirala(List<Aday<T>> adaylar, T sonHedef, RandomGenerator rastgele) {
        List<Aday<T>> liste = new ArrayList<>(adaylar);
        if (liste.size() >= 2 && sonHedef != null) {
            liste.removeIf(aday -> sonHedef.equals(aday.kimlik()));
        }
        for (int i = liste.size() - 1; i > 0; i--) {
            int j = rastgele.nextInt(i + 1);
            Aday<T> gecici = liste.get(i);
            liste.set(i, liste.get(j));
            liste.set(j, gecici);
        }
        liste.sort((a, b) -> Boolean.compare(a.sonMerkezeYakin(), b.sonMerkezeYakin()));
        List<T> sonuc = new ArrayList<>(liste.size());
        for (Aday<T> aday : liste) {
            sonuc.add(aday.kimlik());
        }
        return sonuc;
    }
}
