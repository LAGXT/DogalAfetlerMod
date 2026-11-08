package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

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

class MeteorAyarGocuTest {
    /** {@code main}'in (meteor-2 öncesi) varsayılanları; yağmur süre ve yoğunluğu o dosyada yok. */
    private static Map<String, Object> mainDosyasi() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("meteor.varlikTavani", 48);
        d.put("meteor.girisYuksekligi", 140);
        d.put("meteor.yagmurBuyukOrani", 0.05);
        d.put("meteor.kraterBlokButcesi", 512);
        d.put("gunes_patlamasi.buyukMeteorOrani", 0.05);
        d.put("ay_carpmasi.meteorDakikaBasina", 7.5);
        d.put("ay_carpmasi.meteorSuresiSn", 90);
        d.put("ay_carpmasi.meteorBuyukOrani", 0.04);
        return d;
    }

    @Test
    void eskiVarsayilanYeniyeCekilirEksikAnahtaraDokunulmaz() {
        Map<String, Number> plan = MeteorAyarGocu.plan(0, mainDosyasi()::get);
        Map<String, Number> beklenen = new LinkedHashMap<>();
        beklenen.put("meteor.varlikTavani", 448);
        beklenen.put("meteor.girisYuksekligi", 200);
        beklenen.put("meteor.yagmurBuyukOrani", 0.10);
        beklenen.put("gunes_patlamasi.buyukMeteorOrani", 0.10);
        beklenen.put("ay_carpmasi.meteorDakikaBasina", 180.0);
        beklenen.put("ay_carpmasi.meteorSuresiSn", 600);
        beklenen.put("ay_carpmasi.meteorBuyukOrani", 0.10);
        assertEquals(beklenen, plan, "türü korunarak; dosyada olmayan yağmur anahtarları planda yok");
        assertEquals(List.copyOf(beklenen.keySet()), List.copyOf(plan.keySet()), "tablo sırasıyla");
    }

    @Test
    void araSurumDegerleriZincirleYeniyeVarir() {
        for (Object tavan : new Object[] {96, 240, 48}) {
            assertEquals(448, MeteorAyarGocu.plan(0, Map.of("meteor.varlikTavani", tavan)::get).get("meteor.varlikTavani"));
        }
        for (double dk : new double[] {24, 90}) {
            assertEquals(180.0, MeteorAyarGocu.plan(0, Map.of("gunes_patlamasi.meteorYagmuruDakikaBasina", dk)::get)
                    .get("gunes_patlamasi.meteorYagmuruDakikaBasina"));
        }
        for (double dk : new double[] {7.5, 30, 90}) {
            assertEquals(180.0, MeteorAyarGocu.plan(0, Map.of("ay_carpmasi.meteorDakikaBasina", dk)::get)
                    .get("ay_carpmasi.meteorDakikaBasina"));
        }
        assertEquals(600, MeteorAyarGocu.plan(0, Map.of("gunes_patlamasi.meteorYagmuruSaniye", 180)::get)
                .get("gunes_patlamasi.meteorYagmuruSaniye"));
    }

    @Test
    void elleDegistirilenDegerKorunur() {
        Map<String, Object> d = mainDosyasi();
        d.put("meteor.varlikTavani", 300);
        d.put("ay_carpmasi.meteorDakikaBasina", 120.0);
        d.put("meteor.yagmurBuyukOrani", 0.07);
        Map<String, Number> plan = MeteorAyarGocu.plan(0, d::get);
        assertFalse(plan.containsKey("meteor.varlikTavani"));
        assertFalse(plan.containsKey("ay_carpmasi.meteorDakikaBasina"));
        assertFalse(plan.containsKey("meteor.yagmurBuyukOrani"));
        assertEquals(200, plan.get("meteor.girisYuksekligi"), "elle değer yalnız kendi anahtarını korur");
        assertTrue(MeteorAyarGocu.plan(0, Map.of("meteor.varlikTavani", 448)::get).isEmpty(), "yeni varsayılan kalır");
    }

    @Test
    void guncelSurumeDokunulmaz() {
        assertTrue(MeteorAyarGocu.plan(MeteorAyarGocu.GUNCEL, mainDosyasi()::get).isEmpty());
        assertTrue(MeteorAyarGocu.plan(MeteorAyarGocu.GUNCEL + 3, mainDosyasi()::get).isEmpty(), "ileri sürüm");
    }

    @Test
    void sayiTuruOnemsizSayiOlmayanAtlanir() {
        assertEquals(448, MeteorAyarGocu.plan(0, Map.of("meteor.varlikTavani", 96L)::get).get("meteor.varlikTavani"));
        assertEquals(448, MeteorAyarGocu.plan(0, Map.of("meteor.varlikTavani", 96.0)::get).get("meteor.varlikTavani"));
        assertEquals(180.0, MeteorAyarGocu.plan(0, Map.of("ay_carpmasi.meteorDakikaBasina", 90)::get)
                .get("ay_carpmasi.meteorDakikaBasina"));
        assertTrue(MeteorAyarGocu.plan(0, Map.of("meteor.varlikTavani", "96")::get).isEmpty());
    }

    @Test
    void tabloSiraliYeniEskilerdeYokTekAdim() {
        Set<String> gorulen = new HashSet<>();
        int onceki = 0;
        for (SurumluGocPlani.Adim a : MeteorAyarGocu.ADIMLAR) {
            assertTrue(a.surum() >= onceki && a.surum() >= 1 && a.surum() <= MeteorAyarGocu.GUNCEL, a.yol());
            onceki = a.surum();
            assertTrue(gorulen.add(a.surum() + "/" + a.yol()), "aynı sürümde iki adım: " + a.yol());
            assertFalse(a.eskiler().isEmpty(), a.yol());
            for (Number eski : a.eskiler()) {
                assertTrue(Math.abs(eski.doubleValue() - a.yeni().doubleValue()) > 1e-9, "yeni eskilerde: " + a.yol());
            }
            assertFalse(a.yol().equals(MeteorAyarGocu.SURUM_YOLU));
        }
    }
}
