package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GokyuzuAyarGocuTest {
    /** Güncelleme turu 1'in (gökyüzü-2) varsayılanları; ayarSurumu anahtarı o dosyada yok. */
    private static Map<String, Object> eskiDosya() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("gokyuzu.pulsarOlasiligi", 0.15);
        d.put("gokyuzu.pulsarIsinmaGun", 2.0);
        d.put("gokyuzu.pulsarYanginDakikaBasina", 20.0);
        d.put("gokyuzu.pulsarKavrulmaOlasiligi", 0.6);
        return d;
    }

    @Test
    void eskiVarsayilanYeniyeCekilir() {
        Map<String, Number> plan = GokyuzuAyarGocu.plan(0, eskiDosya()::get);
        Map<String, Number> beklenen = new LinkedHashMap<>();
        beklenen.put("gokyuzu.pulsarIsinmaGun", 1.0);
        beklenen.put("gokyuzu.pulsarYanginDakikaBasina", 24.0);
        assertEquals(beklenen, plan);
        assertEquals(List.copyOf(beklenen.keySet()), List.copyOf(plan.keySet()), "tablo sırasıyla");
    }

    @Test
    void elleDegistirilenDegerKorunur() {
        Map<String, Object> d = eskiDosya();
        d.put("gokyuzu.pulsarIsinmaGun", 3.5);
        Map<String, Number> plan = GokyuzuAyarGocu.plan(0, d::get);
        assertFalse(plan.containsKey("gokyuzu.pulsarIsinmaGun"));
        assertEquals(24.0, plan.get("gokyuzu.pulsarYanginDakikaBasina"), "elle değer yalnız kendi anahtarını korur");
        d.put("gokyuzu.pulsarYanginDakikaBasina", 60.0);
        assertTrue(GokyuzuAyarGocu.plan(0, d::get).isEmpty());
        assertTrue(GokyuzuAyarGocu.plan(0, Map.of("gokyuzu.pulsarIsinmaGun", 1.0)::get).isEmpty(), "yeni varsayılan kalır");
    }

    @Test
    void eksikAnahtaraVeGuncelSurumeDokunulmaz() {
        assertTrue(GokyuzuAyarGocu.plan(0, Map.<String, Object>of()::get).isEmpty(), "Forge eksik anahtarı kendisi doldurur");
        assertTrue(GokyuzuAyarGocu.plan(GokyuzuAyarGocu.GUNCEL, eskiDosya()::get).isEmpty());
        assertTrue(GokyuzuAyarGocu.plan(GokyuzuAyarGocu.GUNCEL + 2, eskiDosya()::get).isEmpty(), "ileri sürüm");
    }

    @Test
    void sayiTuruOnemsizSayiOlmayanAtlanir() {
        assertEquals(24.0, GokyuzuAyarGocu.plan(0, Map.of("gokyuzu.pulsarYanginDakikaBasina", 20)::get)
                .get("gokyuzu.pulsarYanginDakikaBasina"));
        assertEquals(1.0, GokyuzuAyarGocu.plan(0, Map.of("gokyuzu.pulsarIsinmaGun", 2L)::get)
                .get("gokyuzu.pulsarIsinmaGun"));
        assertTrue(GokyuzuAyarGocu.plan(0, Map.of("gokyuzu.pulsarIsinmaGun", "2.0")::get).isEmpty());
    }

    @Test
    void tabloSiraliYeniEskilerdeYok() {
        Set<String> gorulen = new HashSet<>();
        int onceki = 0;
        for (SurumluGocPlani.Adim a : GokyuzuAyarGocu.ADIMLAR) {
            assertTrue(a.surum() >= onceki && a.surum() >= 1 && a.surum() <= GokyuzuAyarGocu.GUNCEL, a.yol());
            onceki = a.surum();
            assertTrue(gorulen.add(a.surum() + "/" + a.yol()), "aynı sürümde iki adım: " + a.yol());
            assertTrue(a.yol().startsWith("gokyuzu."), a.yol());
            assertFalse(a.eskiler().isEmpty(), a.yol());
            for (Number eski : a.eskiler()) {
                assertTrue(Math.abs(eski.doubleValue() - a.yeni().doubleValue()) > 1e-9, "yeni eskilerde: " + a.yol());
            }
            assertFalse(a.yol().equals(GokyuzuAyarGocu.SURUM_YOLU));
        }
    }
}
