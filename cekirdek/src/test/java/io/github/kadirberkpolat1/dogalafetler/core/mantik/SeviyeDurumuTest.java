package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Kullanıcı şikayeti (K-E67): "{@code /dogalafet gun} ile günü artırsak bile seviye 4-5 %0 gösteriyor". Çıktı doğal seçimin
 * olasılıklarını gösterir ve %0 olan seviyenin nedenini söyler.
 */
class SeviyeDurumuTest {
    private static final SiddetAyarlari A = SiddetAyarlari.VARSAYILAN;

    @Test
    void gunYuzdeDortVeBesSifirdanBuyuk() {
        SeviyeDurumu durum = SeviyeDurumu.hesapla(A, 100, 100, 5, false, 1.0);
        assertTrue(durum.olasilik(4) > 0 && durum.olasilik(5) > 0, "gün 100: S4/S5 > 0");
        for (int s = 1; s <= 5; s++) {
            assertEquals(Optional.empty(), durum.neden(s));
        }
        assertTrue(!durum.zayiflatildi());
    }

    @Test
    void sifirOlanSeviyeninNedeniVerilir() {
        SeviyeDurumu erken = SeviyeDurumu.hesapla(A, 3, 3, 5, false, 1.0);
        assertEquals(0.0, erken.olasilik(4));
        assertEquals(Optional.of(SiddetSecici.KapaliNeden.GUN), erken.neden(4));
        assertEquals(Optional.of(SiddetSecici.KapaliNeden.GUN), erken.neden(5));
        assertEquals(Optional.empty(), erken.neden(3));

        SeviyeDurumu serbest = SeviyeDurumu.hesapla(A, 100, 100, 5, false, 1.0);
        SeviyeDurumu artArda = SeviyeDurumu.hesapla(A, 100, 100, 5, true, 1.0);
        assertEquals(Optional.empty(), artArda.neden(4), "art arda 4-5 yasak değil, seyreltilir");
        assertEquals(Optional.empty(), artArda.neden(5));
        assertTrue(artArda.olasilik(4) > 0 && artArda.olasilik(4) < serbest.olasilik(4));
        assertTrue(artArda.olasilik(5) > 0 && artArda.olasilik(5) < serbest.olasilik(5));

        SeviyeDurumu maks = SeviyeDurumu.hesapla(A, 100, 100, 3, false, 1.0);
        assertEquals(Optional.of(SiddetSecici.KapaliNeden.MAKS), maks.neden(4));
        assertEquals(3, maks.maks());

        SeviyeDurumu gelisim = SeviyeDurumu.hesapla(A.gelisimIle(true, 2.0), 100, 3, 5, false, 1.0);
        assertEquals(Optional.of(SiddetSecici.KapaliNeden.GELISIM), gelisim.neden(4));
    }

    @Test
    void yogunlukNotuVeToplamBir() {
        SeviyeDurumu yogun = SeviyeDurumu.hesapla(A, 60, 60, 5, false, 5.0);
        assertTrue(yogun.zayiflatildi());
        double toplam = 0;
        for (int s = 1; s <= 5; s++) {
            assertTrue(yogun.olasilik(s) > 0);
            toplam += yogun.olasilik(s);
        }
        assertEquals(1.0, toplam, 1e-9);
    }
}
