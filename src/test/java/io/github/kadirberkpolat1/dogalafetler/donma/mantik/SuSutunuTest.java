package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import static io.github.kadirberkpolat1.dogalafetler.donma.mantik.SuSutunu.Tur.BUZ;
import static io.github.kadirberkpolat1.dogalafetler.donma.mantik.SuSutunu.Tur.DIGER;
import static io.github.kadirberkpolat1.dogalafetler.donma.mantik.SuSutunu.Tur.INCE;
import static io.github.kadirberkpolat1.dogalafetler.donma.mantik.SuSutunu.Tur.SU;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.github.kadirberkpolat1.dogalafetler.donma.mantik.SuSutunu.Tur;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SuSutunuTest {
    @Test
    void enAlttakiSuDonmaz() {
        assertArrayEquals(new Tur[] {BUZ, BUZ, SU, DIGER}, SuSutunu.planla(new Tur[] {SU, SU, SU, DIGER}, 4));
    }

    @Test
    void derinSudaKalinlikSinirinaKadarDonar() {
        assertArrayEquals(new Tur[] {BUZ, BUZ, BUZ, BUZ, SU}, SuSutunu.planla(new Tur[] {SU, SU, SU, SU, SU}, 4));
        assertArrayEquals(new Tur[] {BUZ, SU, SU, SU, SU}, SuSutunu.planla(new Tur[] {SU, SU, SU, SU, SU}, 1));
    }

    @Test
    void dondurSonraCozBaslangicSutununuVerir() {
        Tur[] baslangic = {SU, SU, SU, DIGER};
        assertArrayEquals(baslangic, SuSutunu.planla(SuSutunu.planla(baslangic, 4), 0));
    }

    @Test
    void altiSuOlmayanBuzaDokunulmaz() {
        Tur[] oyuncuBuzu = {BUZ, BUZ, DIGER};
        assertArrayEquals(oyuncuBuzu, SuSutunu.planla(oyuncuBuzu, 0));
        Tur[] buzSutunu = {BUZ, BUZ, BUZ, BUZ, BUZ};
        assertArrayEquals(buzSutunu, SuSutunu.planla(buzSutunu, 0));
    }

    /** D1: 1 derin su tamamen donar (yürünebilir kıyı); çözülme yalnız buz bizimse geri verir. */
    @Test
    void birDerinSuTamamenDonarBizimseCozulur() {
        Tur[] sig = {SU, DIGER};
        assertArrayEquals(new Tur[] {INCE, DIGER}, SuSutunu.planlaSeviye(sig, 1, 1));
        assertArrayEquals(new Tur[] {BUZ, DIGER}, SuSutunu.planlaSeviye(sig, 5, 1));
        assertArrayEquals(new Tur[] {INCE, DIGER}, SuSutunu.planlaSeviye(new Tur[] {BUZ, DIGER}, 1, -1, true));
        assertArrayEquals(sig, SuSutunu.planlaSeviye(new Tur[] {BUZ, DIGER}, 0, -1, true));
        assertArrayEquals(new Tur[] {BUZ, DIGER}, SuSutunu.planlaSeviye(new Tur[] {BUZ, DIGER}, 0, -1, false),
                "bizim olmayan tek buz (oyuncunun) çözülmez");
        assertArrayEquals(new Tur[] {SU}, SuSutunu.planlaSeviye(new Tur[] {SU}, 5, 1), "altı bilinmeyen tek blok donmaz");
    }

    @Test
    void ikiVeDortDerinSudaEnAltSuKalir() {
        assertArrayEquals(new Tur[] {BUZ, SU, DIGER}, SuSutunu.planlaSeviye(new Tur[] {SU, SU, DIGER}, 5, 1));
        assertArrayEquals(new Tur[] {BUZ, BUZ, BUZ, SU, DIGER},
                SuSutunu.planlaSeviye(new Tur[] {SU, SU, SU, SU, DIGER}, 9, 1));
        assertArrayEquals(new Tur[] {SU, SU, SU, SU, DIGER},
                SuSutunu.planlaSeviye(new Tur[] {BUZ, BUZ, BUZ, SU, DIGER}, 0, -1, true));
    }

    @Test
    void suOlmayanSutunaDokunulmaz() {
        Tur[] kara = {DIGER, DIGER};
        assertArrayEquals(kara, SuSutunu.planla(kara, 4));
    }

    @Test
    void sonDurumYalnizSonKalinligaBaglidir() {
        Random r = new Random(3);
        for (int deneme = 0; deneme < 500; deneme++) {
            int uzunluk = 1 + r.nextInt(5);
            Tur[] baslangic = new Tur[uzunluk];
            for (int i = 0; i < uzunluk; i++) {
                baslangic[i] = r.nextInt(4) == 0 ? DIGER : SU;
            }
            Tur[] mevcut = baslangic;
            int son = 0;
            for (int i = 0; i < 1 + r.nextInt(6); i++) {
                son = r.nextInt(5);
                mevcut = SuSutunu.planla(mevcut, son);
            }
            assertArrayEquals(SuSutunu.planla(baslangic, son), mevcut);
            assertArrayEquals(baslangic, SuSutunu.planla(mevcut, 0));
        }
    }

    @Test
    void donarkenDogalBuzErimez() {
        Tur[] dogal = {BUZ, SU, SU, DIGER};
        assertArrayEquals(dogal, SuSutunu.planla(dogal, 0, 1));
        assertArrayEquals(new Tur[] {BUZ, BUZ, SU, DIGER}, SuSutunu.planla(dogal, 2, 1));
    }

    @Test
    void cozulurkenYeniBuzOlusmaz() {
        Tur[] su = {SU, SU, SU, DIGER};
        assertArrayEquals(su, SuSutunu.planla(su, 2, -1));
        assertArrayEquals(new Tur[] {BUZ, SU, SU, DIGER}, SuSutunu.planla(new Tur[] {BUZ, BUZ, SU, DIGER}, 1, -1));
        assertArrayEquals(su, SuSutunu.planla(new Tur[] {BUZ, BUZ, SU, DIGER}, 0, -1));
    }

    @Test
    void yonSifirDokunmaz() {
        Tur[] s = {BUZ, SU, SU, DIGER};
        assertArrayEquals(s, SuSutunu.planla(s, 3, 0));
    }
}
