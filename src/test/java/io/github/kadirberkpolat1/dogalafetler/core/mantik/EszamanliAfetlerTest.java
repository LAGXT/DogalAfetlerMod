package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** K-E67: eşzamanlı doğal afet sınırı günle artar, sunucu geneli afet tek başınadır, alanlar çakışmaz, gün çarpanı. */
class EszamanliAfetlerTest {
    private static final ZamanlayiciAyarlari A = ZamanlayiciAyarlari.VARSAYILAN;

    @Test
    void sinirGunleArtar() {
        assertEquals(1, A.eszamanliSinir(0));
        assertEquals(1, A.eszamanliSinir(14.9));
        assertEquals(2, A.eszamanliSinir(15));
        assertEquals(2, A.eszamanliSinir(39.9));
        assertEquals(3, A.eszamanliSinir(40));
        assertEquals(3, A.eszamanliSinir(10_000));
    }

    @Test
    void yuvaSinirdaDolarSunucuGeneliTekBasinaTpsIkinciyiErteler() {
        assertFalse(EszamanliAfetler.yuvaDolu(List.of(), 1, true), "boşken TPS kısması ilk afeti engellemez");
        assertTrue(EszamanliAfetler.yuvaDolu(List.of(AfetTuru.DEPREM), 1, false));
        assertFalse(EszamanliAfetler.yuvaDolu(List.of(AfetTuru.DEPREM), 2, false));
        assertTrue(EszamanliAfetler.yuvaDolu(List.of(AfetTuru.DEPREM), 2, true), "TPS kısılıyken ikinci afet ertelenir");
        assertTrue(EszamanliAfetler.yuvaDolu(List.of(AfetTuru.DEPREM, AfetTuru.KASIRGA), 2, false));
        assertFalse(EszamanliAfetler.yuvaDolu(List.of(AfetTuru.DEPREM, AfetTuru.KASIRGA), 3, false));
        assertTrue(EszamanliAfetler.yuvaDolu(List.of(AfetTuru.AY_CARPMASI), 3, false), "ay çarpması tek başına");
        assertTrue(EszamanliAfetler.yuvaDolu(List.of(AfetTuru.GUNES_PATLAMASI), 3, false), "güneş patlaması tek başına");
    }

    @Test
    void adaylarFarkliTurVeSunucuGeneliYalniz() {
        Set<AfetTuru> hepsi = EnumSet.allOf(AfetTuru.class);
        assertEquals(hepsi, EszamanliAfetler.adaylar(hepsi, List.of()));
        Set<AfetTuru> depremVarken = EszamanliAfetler.adaylar(hepsi, List.of(AfetTuru.DEPREM));
        assertFalse(depremVarken.contains(AfetTuru.DEPREM), "aynı tür ikinci kez seçilmez");
        assertFalse(depremVarken.contains(AfetTuru.AY_CARPMASI) || depremVarken.contains(AfetTuru.GUNES_PATLAMASI),
                "afet sürerken sunucu geneli seçilmez");
        assertTrue(depremVarken.contains(AfetTuru.KASIRGA));
        assertEquals(5, depremVarken.size(), "kalan beş bölgesel tür");
    }

    @Test
    void etkiAlanlariCakismasi() {
        EtkiAlani a = EtkiAlani.daire(0, 64, 0, 30);
        assertTrue(a.kesisir(EtkiAlani.daire(50, 64, 0, 30)), "merkezler arası 50 < 60");
        assertFalse(a.kesisir(EtkiAlani.daire(70, 64, 0, 30)), "merkezler arası 70 > 60");
        assertFalse(a.kesisir(EtkiAlani.daire(60, 0, 0, 30)), "değen kenar çakışma sayılmaz; yükseklik önemsiz");
        assertTrue(a.kesisir(EtkiAlani.herYer()) && EtkiAlani.herYer().kesisir(a), "global alan her alanla çakışır");
    }

    @Test
    void gunCarpaniDogrusalArtarSonraSabit() {
        assertEquals(1.0, A.gunCarpani(0), 1e-12);
        assertEquals(1.0, A.gunCarpani(-5), 1e-12);
        double yarim = 1 + (A.siklikArtisCarpani() - 1) / 2;
        assertEquals(yarim, A.gunCarpani(A.siklikArtisGunu() / 2), 1e-9);
        assertEquals(A.siklikArtisCarpani(), A.gunCarpani(A.siklikArtisGunu()), 1e-12);
        assertEquals(A.siklikArtisCarpani(), A.gunCarpani(1_000), 1e-12);
        for (double gun = 0; gun < 100; gun += 0.5) {
            assertTrue(A.gunCarpani(gun + 0.5) >= A.gunCarpani(gun), "gün çarpanı azalmaz");
        }
    }

    @Test
    void gunCarpaniBolgeselOraniVeBeklemeyiEtkilerDonemOlayinaAzEtkiler() {
        double c = A.gunCarpani(30);
        assertEquals(AfetZamanlayicisi.turOrani(A, AfetTuru.DEPREM, 1, 1) * c,
                AfetZamanlayicisi.turOrani(A, AfetTuru.DEPREM, 1, 1, c), 1e-12);
        assertEquals(AfetZamanlayicisi.turOrani(A, AfetTuru.AY_CARPMASI, 1, 1) * Math.pow(c, AfetZamanlayicisi.DONEM_GUN_USSU),
                AfetZamanlayicisi.turOrani(A, AfetTuru.AY_CARPMASI, 1, 1, c), 1e-12, "dönem olayı çarpanın dördüncü kökü");
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.afetBitti(AfetTuru.SINKHOLE, 1, 0, A, 1.0, 4.0);
        assertEquals(Math.round(A.genelBeklemeTick() / 4.0), bekleme.genelKalan(0));
        assertEquals(Math.round(A.turBekleme(AfetTuru.SINKHOLE) / 4.0), bekleme.turKalan(AfetTuru.SINKHOLE, 0));
        BeklemeDurumu ay = new BeklemeDurumu();
        ay.afetBitti(AfetTuru.AY_CARPMASI, 0, 0, A, 1.0, 4.0);
        assertEquals(Math.round(A.turBekleme(AfetTuru.AY_CARPMASI) * A.siddetUzamasi(AfetTuru.AY_CARPMASI, 0)),
                ay.turKalan(AfetTuru.AY_CARPMASI, 0), "dönem olayının kendi beklemesi gün çarpanına bölünmez");
    }
}
