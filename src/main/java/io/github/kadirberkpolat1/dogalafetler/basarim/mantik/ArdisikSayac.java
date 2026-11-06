package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

/**
 * Koşulın kesintisiz kaç adımdır (saniye) tuttuğu. N16 "Yıldızlı Gece": göğü gören konumda en az 10 saniye; koşul
 * bozulunca sıfırlanır. Bellekte tutulur (oyuncu çıkınca kaybolur).
 */
public final class ArdisikSayac {
    private int deger;

    /** Bir adım: koşul tutuyorsa artırır, tutmuyorsa sıfırlar; yeni değeri döner. */
    public int adim(boolean kosul) {
        deger = kosul ? deger + 1 : 0;
        return deger;
    }

    public int deger() {
        return deger;
    }

    public void sifirla() {
        deger = 0;
    }
}
