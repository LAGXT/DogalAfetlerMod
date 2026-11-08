package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class KasirgaAyarGocuTest {
    /** Güncelleme turu 2'nin kasırga varsayılanları (ör. "Yeni Dünya (5)"); ayarSurumu anahtarı o dosyada yok. */
    private static Map<String, Object> eskiDosya() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("kasirga.ucanBlokTavani", 40);
        d.put("kasirga.kaldirmaTavani", 25.0);
        d.put("kasirga.boyutCarpani", 1.0);
        d.put("kasirga.cekimCarpani", 1.0);
        return d;
    }

    @Test
    void eskiKaldirmaTavaniYeniOrantiliVarsayilanaCekilir() {
        assertEquals(Map.of("kasirga.kaldirmaTavani", KasirgaSeviyesi.S5_KALDIRMA_TAVANI), KasirgaAyarGocu.plan(0, eskiDosya()::get));
        assertEquals(0.25 * KasirgaSeviyesi.icin(5).yukseklik(), KasirgaSeviyesi.S5_KALDIRMA_TAVANI, 1e-9,
                "yeni varsayılan S5 huni yüksekliğinin dörtte biri");
    }

    @Test
    void elleDegistirilenDegerKorunur() {
        Map<String, Object> d = eskiDosya();
        d.put("kasirga.kaldirmaTavani", 40.0);
        assertTrue(KasirgaAyarGocu.plan(0, d::get).isEmpty());
        assertTrue(KasirgaAyarGocu.plan(0, Map.of("kasirga.kaldirmaTavani", 36.0)::get).isEmpty(), "yeni varsayılan kalır");
    }

    @Test
    void eksikAnahtaraVeGuncelSurumeDokunulmaz() {
        assertTrue(KasirgaAyarGocu.plan(0, Map.<String, Object>of()::get).isEmpty(), "Forge eksik anahtarı kendisi doldurur");
        assertTrue(KasirgaAyarGocu.plan(KasirgaAyarGocu.GUNCEL, eskiDosya()::get).isEmpty());
        assertEquals(36.0, KasirgaAyarGocu.plan(0, Map.of("kasirga.kaldirmaTavani", 25)::get).get("kasirga.kaldirmaTavani"),
                "sayı türü önemsiz");
    }

    @Test
    void tabloSiraliYeniEskilerdeYok() {
        Set<String> gorulen = new HashSet<>();
        int onceki = 0;
        for (SurumluGocPlani.Adim a : KasirgaAyarGocu.ADIMLAR) {
            assertTrue(a.surum() >= onceki && a.surum() >= 1 && a.surum() <= KasirgaAyarGocu.GUNCEL, a.yol());
            onceki = a.surum();
            assertTrue(gorulen.add(a.surum() + "/" + a.yol()), "aynı sürümde iki adım: " + a.yol());
            assertTrue(a.yol().startsWith("kasirga."), a.yol());
            assertFalse(a.eskiler().isEmpty(), a.yol());
            for (Number eski : a.eskiler()) {
                assertTrue(Math.abs(eski.doubleValue() - a.yeni().doubleValue()) > 1e-9, "yeni eskilerde: " + a.yol());
            }
            assertFalse(a.yol().equals(KasirgaAyarGocu.SURUM_YOLU));
        }
        assertEquals(KasirgaAyarGocu.SURUM_YOLU, KasirgaAyarGocu.PLAN.surumYolu());
        assertEquals(KasirgaAyarGocu.ADIMLAR, KasirgaAyarGocu.PLAN.adimlar());
    }
}
