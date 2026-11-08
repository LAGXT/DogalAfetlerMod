package io.github.kadirberkpolat1.dogalafetler.afet.tsunami.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.afet.tsunami.TsunamiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TsunamiAyarGocuTest {
    private static final String GECIKME = "tsunami.dalgaGecikmeSn";

    /** Güncelleme turu 2'nin varsayılanları; ayarSurumu anahtarı o dosyada yok. */
    private static Map<String, Object> eskiDosya() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("tsunami.aramaYaricapi", 96);
        d.put(GECIKME, 10.0);
        d.put("tsunami.cekilmeSn", 18.0);
        return d;
    }

    @Test
    void eskiDalgaGecikmesiYeniVarsayilanaCekilir() {
        assertEquals(Map.of(GECIKME, 2.0), TsunamiAyarGocu.plan(0, eskiDosya()::get));
        assertEquals(TsunamiAyarlari.VARSAYILAN_DALGA_GECIKME_SN, TsunamiAyarGocu.ADIMLAR.get(0).yeni().doubleValue(), 0.0,
                "tablonun yeni değeri spec varsayılanı");
    }

    @Test
    void elleDegistirilenDegerKorunur() {
        Map<String, Object> d = eskiDosya();
        d.put(GECIKME, 6.5);
        assertTrue(TsunamiAyarGocu.plan(0, d::get).isEmpty());
        assertTrue(TsunamiAyarGocu.plan(0, Map.<String, Object>of(GECIKME, 2.0)::get).isEmpty(), "yeni varsayılan kalır");
    }

    @Test
    void eksikAnahtaraVeGuncelSurumeDokunulmaz() {
        assertTrue(TsunamiAyarGocu.plan(0, Map.<String, Object>of()::get).isEmpty(), "Forge eksik anahtarı kendisi doldurur");
        assertTrue(TsunamiAyarGocu.plan(TsunamiAyarGocu.GUNCEL, eskiDosya()::get).isEmpty());
        assertTrue(TsunamiAyarGocu.plan(TsunamiAyarGocu.GUNCEL + 2, eskiDosya()::get).isEmpty(), "ileri sürüm");
    }

    @Test
    void sayiTuruOnemsizSayiOlmayanAtlanir() {
        assertEquals(2.0, TsunamiAyarGocu.plan(0, Map.<String, Object>of(GECIKME, 10)::get).get(GECIKME));
        assertEquals(2.0, TsunamiAyarGocu.plan(0, Map.<String, Object>of(GECIKME, 10L)::get).get(GECIKME));
        assertTrue(TsunamiAyarGocu.plan(0, Map.<String, Object>of(GECIKME, "10.0")::get).isEmpty());
    }

    @Test
    void tabloSiraliYeniEskilerdeYok() {
        Set<String> gorulen = new HashSet<>();
        int onceki = 0;
        for (SurumluGocPlani.Adim a : TsunamiAyarGocu.ADIMLAR) {
            assertTrue(a.surum() >= onceki && a.surum() >= 1 && a.surum() <= TsunamiAyarGocu.GUNCEL, a.yol());
            onceki = a.surum();
            assertTrue(gorulen.add(a.surum() + "/" + a.yol()), "aynı sürümde iki adım: " + a.yol());
            assertTrue(a.yol().startsWith("tsunami."), a.yol());
            assertFalse(a.eskiler().isEmpty(), a.yol());
            for (Number eski : a.eskiler()) {
                assertTrue(Math.abs(eski.doubleValue() - a.yeni().doubleValue()) > 1e-9, "yeni eskilerde: " + a.yol());
            }
            assertFalse(a.yol().equals(TsunamiAyarGocu.SURUM_YOLU));
        }
    }
}
