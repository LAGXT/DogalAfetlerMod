package io.github.kadirberkpolat1.dogalafetler.config;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.adim;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuAyarGocu;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorAyarGocu;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SurumluGocPlaniTest {
    /** İki sürümlü örnek: {@code a.x} 1'de 10 → 20, 2'de 20 → 30; {@code a.y} yalnız 2'de 5 → 6. */
    private static final SurumluGocPlani PLAN = new SurumluGocPlani("a.ayarSurumu", 2, List.of(
            adim(1, "a.x", 20, 10),
            adim(2, "a.x", 30, 20),
            adim(2, "a.y", 6.0, 5.0)));

    @Test
    void eskiSurumAdimlariSiraylaGecer() {
        Map<String, Object> dosya = new LinkedHashMap<>();
        dosya.put("a.x", 10);
        dosya.put("a.y", 5.0);
        Map<String, Number> beklenen = new LinkedHashMap<>();
        beklenen.put("a.x", 30);
        beklenen.put("a.y", 6.0);
        assertEquals(beklenen, PLAN.plan(0, dosya::get), "10 → 20 → 30 zinciri");
        assertEquals(Map.of("a.x", 30, "a.y", 6.0), PLAN.plan(1, Map.<String, Object>of("a.x", 20, "a.y", 5.0)::get),
                "sürüm 1'den yalnız 2. adımlar");
        assertTrue(PLAN.plan(1, Map.<String, Object>of("a.x", 10)::get).isEmpty(),
                "sürüm 1 dosyasında 10 elle değerdir");
    }

    @Test
    void elleDegerEksikAnahtarVeSayiOlmayanDokunulmaz() {
        assertTrue(PLAN.plan(0, Map.<String, Object>of("a.x", 15, "a.y", "5")::get).isEmpty());
        assertTrue(PLAN.plan(0, Map.<String, Object>of()::get).isEmpty());
        assertEquals(30, PLAN.plan(0, Map.<String, Object>of("a.x", 10.0)::get).get("a.x"), "sayı türü önemsiz");
        assertEquals(30, PLAN.plan(0, Map.<String, Object>of("a.x", 20L)::get).get("a.x"));
    }

    @Test
    void guncelVeIleriSurumeDokunulmaz() {
        Map<String, Object> dosya = Map.of("a.x", 10, "a.y", 5.0);
        assertTrue(PLAN.plan(2, dosya::get).isEmpty());
        assertTrue(PLAN.plan(5, dosya::get).isEmpty());
        assertEquals("a.ayarSurumu", PLAN.surumYolu());
        assertEquals(2, PLAN.guncel());
        assertEquals(3, PLAN.adimlar().size());
    }

    @Test
    void bozukTabloKurulmaz() {
        assertThrows(IllegalArgumentException.class, () -> new SurumluGocPlani("s", 2,
                List.of(adim(2, "a", 1, 0), adim(1, "b", 1, 0))), "sırasız");
        assertThrows(IllegalArgumentException.class, () -> new SurumluGocPlani("s", 1,
                List.of(adim(2, "a", 1, 0))), "güncelden ileri");
        assertThrows(IllegalArgumentException.class, () -> new SurumluGocPlani("s", 1,
                List.of(adim(0, "a", 1, 0))), "sürüm 0");
        assertThrows(IllegalArgumentException.class, () -> new SurumluGocPlani("s", 1,
                List.of(adim(1, "a", 1, 0), adim(1, "a", 2, 1))), "aynı sürümde iki adım");
        assertThrows(IllegalArgumentException.class, () -> new SurumluGocPlani("s", 1,
                List.of(adim(1, "a", 1))), "eski değer yok");
        assertThrows(IllegalArgumentException.class, () -> new SurumluGocPlani("s", 1,
                List.of(adim(1, "a", 1, 1.0))), "yeni eskilerde");
        assertThrows(IllegalArgumentException.class, () -> new SurumluGocPlani("s", 1,
                List.of(adim(1, "s", 1, 0))), "sürüm anahtarı göç ettirilmez");
    }

    @Test
    void bolumlerOrtakPlaniKullanir() {
        assertEquals(MeteorAyarGocu.SURUM_YOLU, MeteorAyarGocu.PLAN.surumYolu());
        assertEquals(MeteorAyarGocu.GUNCEL, MeteorAyarGocu.PLAN.guncel());
        assertEquals(MeteorAyarGocu.ADIMLAR, MeteorAyarGocu.PLAN.adimlar());
        assertEquals(GokyuzuAyarGocu.SURUM_YOLU, GokyuzuAyarGocu.PLAN.surumYolu());
        assertEquals(GokyuzuAyarGocu.GUNCEL, GokyuzuAyarGocu.PLAN.guncel());
        assertEquals(GokyuzuAyarGocu.ADIMLAR, GokyuzuAyarGocu.PLAN.adimlar());
    }
}
