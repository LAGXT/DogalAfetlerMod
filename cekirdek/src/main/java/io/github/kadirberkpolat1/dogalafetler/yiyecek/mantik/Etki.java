package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

/**
 * Bir yeme anının sıcaklık etkisi: {@code anlik} hemen, {@code sureli} {@code sureTick} boyunca giderek azalan hızla eklenir
 * (sıcaklık {@code VucutSicakligi.etkiEkle}). Değerler işaretli vücut birimidir (serinlik negatif).
 */
public record Etki(double anlik, double sureli, int sureTick) {
    public double toplam() {
        return anlik + sureli;
    }

    /**
     * @param verimCarpani azalan verim çarpanı ({@link YemeGecmisi#ye}); 1 tam etki
     * @param sicakta      vücut ya da ortam sıcak mı ({@link #sicakMi}); yalnız yan etkili tanımda sayılır
     */
    public static Etki hesapla(YiyecekTanimi tanim, EtkiAyarlari ayarlar, double verimCarpani, boolean sicakta) {
        double miktar = ayarlar.miktar(tanim.buyukluk()) * verimCarpani;
        if (tanim.sicaktaGuclenir() && sicakta) {
            miktar *= ayarlar.yanEtkiCarpani();
        }
        double isaretli = miktar * tanim.yon().isaret();
        double anlik = isaretli * ayarlar.anlikOran();
        return new Etki(anlik, isaretli - anlik, tanim.sureTick());
    }

    /** Yan etki koşulu: vücut ya da ortam (vücut birimi) eşikte ya da üstünde. */
    public static boolean sicakMi(double vucut, double ortamBirim, EtkiAyarlari ayarlar) {
        return Math.max(vucut, ortamBirim) >= ayarlar.yanEtkiEsigi();
    }
}
