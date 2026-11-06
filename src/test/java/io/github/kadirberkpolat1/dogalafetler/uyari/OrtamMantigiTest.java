package io.github.kadirberkpolat1.dogalafetler.uyari;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrtamMantigiTest {
    @Test
    void hacimEgrisiNoktalarArasindaDogrusalUclardaSabittir() {
        HacimEgrisi egri = HacimEgrisi.of(new HacimEgrisi.Nokta(0, 0f), new HacimEgrisi.Nokta(40, 1f),
                new HacimEgrisi.Nokta(100, 0.5f));
        assertEquals(0f, egri.deger(0), 1e-6f);
        assertEquals(0.5f, egri.deger(20), 1e-6f);
        assertEquals(1f, egri.deger(40), 1e-6f);
        assertEquals(0.75f, egri.deger(70), 1e-6f);
        assertEquals(0.5f, egri.deger(10_000), 1e-6f, "son noktadan sonra son değer");
        assertEquals(0f, egri.deger(-5), 1e-6f, "ilk noktadan önce ilk değer");
        assertEquals(0.8f, HacimEgrisi.sabit(0.8f).deger(123), 1e-6f);
        HacimEgrisi giris = HacimEgrisi.giris(20, 0.6f);
        assertEquals(0.3f, giris.deger(10), 1e-6f);
        assertEquals(0.6f, giris.deger(500), 1e-6f);
    }

    @Test
    void hacimEgrisiGecersizNoktalariReddeder() {
        assertThrows(IllegalArgumentException.class, HacimEgrisi::of);
        assertThrows(IllegalArgumentException.class,
                () -> HacimEgrisi.of(new HacimEgrisi.Nokta(10, 0f), new HacimEgrisi.Nokta(10, 1f)), "artmayan tick");
        assertThrows(IllegalArgumentException.class, () -> HacimEgrisi.of(new HacimEgrisi.Nokta(0, 1.5f)), "hacim > 1");
        assertThrows(IllegalArgumentException.class, () -> HacimEgrisi.of(new HacimEgrisi.Nokta(-1, 0.5f)));
        List<HacimEgrisi.Nokta> cok = new ArrayList<>();
        for (int i = 0; i <= HacimEgrisi.EN_COK_NOKTA; i++) {
            cok.add(new HacimEgrisi.Nokta(i, 0.5f));
        }
        assertThrows(IllegalArgumentException.class, () -> new HacimEgrisi(cok));
    }

    @Test
    void mesafeyleDogrusalSonumGlobaldeSonumYok() {
        assertEquals(1f, OrtamSonumu.carpan(0, 100), 1e-6f);
        assertEquals(0.75f, OrtamSonumu.carpan(25, 100), 1e-6f);
        assertEquals(0f, OrtamSonumu.carpan(100, 100), 1e-6f);
        assertEquals(0f, OrtamSonumu.carpan(250, 100), 1e-6f);
        assertEquals(1f, OrtamSonumu.carpan(5000, OrtamSonumu.GLOBAL), 1e-6f, "global katman her yerde");
    }

    @Test
    void ortamDuzeyiEgriSonumTavanVeBastirmaCarpimidir() {
        assertEquals(0.5f * 0.8f * 0.5f, SesDuzeyi.ortam(0.5f, 0.8f, 50, 1f), 1e-6f);
        assertEquals(0f, SesDuzeyi.ortam(1f, 1f, 0, 1f), 0f, "tavan 0 = ortam sesleri kapalı");
        float bastirma = (float) Math.pow(10, -3 / 20.0);
        assertEquals(bastirma, SesDuzeyi.ortam(1f, 1f, 100, bastirma), 1e-6f);
    }

    /** Ses.md §4 kural 3 ve §5: eşzamanlı katman tavanı kaliteye göre, aynı sesten en çok 6; fazlası en sessizden düşer. */
    @Test
    void karisimEnYuksekDuzeylileriSecerAyniSestenAltiyiAsmaz() {
        List<OrtamKarisimi.Aday> adaylar = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            adaylar.add(new OrtamKarisimi.Aday("ugultu" + i, "a:ugultu", 0.1f * (i + 1)));
        }
        adaylar.add(new OrtamKarisimi.Aday("citirti", "a:citirti", 0.05f));
        adaylar.add(new OrtamKarisimi.Aday("sessiz", "a:sessiz", 0f));
        Set<String> secilen = OrtamKarisimi.secilenler(adaylar, 16, OrtamKarisimi.TUR_TAVANI);
        assertEquals(Set.of("ugultu7", "ugultu6", "ugultu5", "ugultu4", "ugultu3", "ugultu2", "citirti"), secilen,
                "aynı sesten en güçlü 6, duyulmayan (0) seçilmez");
        Set<String> dar = OrtamKarisimi.secilenler(adaylar, 2, OrtamKarisimi.TUR_TAVANI);
        assertEquals(Set.of("ugultu7", "ugultu6"), dar, "tavan doluysa en sessizler düşer");
    }

    @Test
    void karisimTavaniKaliteTablosundandir() {
        assertEquals(8, OrtamKarisimi.tavan(EfektKalitesi.DUSUK));
        assertEquals(12, OrtamKarisimi.tavan(EfektKalitesi.ORTA));
        assertEquals(16, OrtamKarisimi.tavan(EfektKalitesi.YUKSEK));
        assertEquals(20, OrtamKarisimi.tavan(EfektKalitesi.ULTRA));
    }

    /** Ses.md §4 kural 4: siren çalarken ortam sesleri −3 dB alçalır; geçiş sıçramasız. */
    @Test
    void sirenCalarkenOrtamUcDesibelYumusakcaAlcalirVeGeriDoner() {
        Bastirma bastirma = new Bastirma();
        float hedef = (float) Math.pow(10, Bastirma.SIREN_BASTIRMA_DB / 20.0);
        float onceki = bastirma.carpan();
        assertEquals(1f, onceki, 0f);
        for (int i = 0; i < Bastirma.GECIS_TICK; i++) {
            float simdiki = bastirma.ilerle(true);
            assertTrue(simdiki < onceki, "her tick biraz alçalır");
            assertTrue(onceki - simdiki < 0.02f, "sıçrama yok");
            onceki = simdiki;
        }
        assertEquals(hedef, bastirma.carpan(), 1e-5f);
        assertEquals(hedef, bastirma.ilerle(true), 1e-5f, "hedefte kalır");
        for (int i = 0; i < Bastirma.GECIS_TICK; i++) {
            bastirma.ilerle(false);
        }
        assertEquals(1f, bastirma.carpan(), 1e-5f);
    }
}
