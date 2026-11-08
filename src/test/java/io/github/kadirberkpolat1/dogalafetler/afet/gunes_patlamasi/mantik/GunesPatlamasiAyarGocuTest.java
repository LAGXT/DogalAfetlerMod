package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.GunesPatlamasiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GunesPatlamasiAyarGocuTest {
    private static final String ONCU = "gunes_patlamasi.meteorYagmuruOncuSaniye";

    /** Güncelleme turu 2'nin varsayılanları; ayarSurumu anahtarı o dosyada yok. */
    private static Map<String, Object> eskiDosya() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("gunes_patlamasi.aktifSureGun", 5.0);
        d.put(ONCU, 90);
        d.put("gunes_patlamasi.meteorYagmuruOncuDakikaBasina", 6.0);
        return d;
    }

    @Test
    void eskiOncuPenceresiYeniVarsayilanaCekilir() {
        assertEquals(Map.of(ONCU, 5), GunesPatlamasiAyarGocu.plan(0, eskiDosya()::get));
        assertEquals(GunesPatlamasiAyarlari.VARSAYILAN_ONCU_SANIYE, GunesPatlamasiAyarGocu.ADIMLAR.get(0).yeni().intValue(),
                "tablonun yeni değeri spec varsayılanı");
    }

    @Test
    void elleDegistirilenDegerKorunur() {
        Map<String, Object> d = eskiDosya();
        d.put(ONCU, 45);
        assertTrue(GunesPatlamasiAyarGocu.plan(0, d::get).isEmpty());
        assertTrue(GunesPatlamasiAyarGocu.plan(0, Map.<String, Object>of(ONCU, 5)::get).isEmpty(), "yeni varsayılan kalır");
    }

    @Test
    void eksikAnahtaraVeGuncelSurumeDokunulmaz() {
        assertTrue(GunesPatlamasiAyarGocu.plan(0, Map.<String, Object>of()::get).isEmpty(), "Forge eksik anahtarı kendisi doldurur");
        assertTrue(GunesPatlamasiAyarGocu.plan(GunesPatlamasiAyarGocu.GUNCEL, eskiDosya()::get).isEmpty());
        assertTrue(GunesPatlamasiAyarGocu.plan(GunesPatlamasiAyarGocu.GUNCEL + 2, eskiDosya()::get).isEmpty(), "ileri sürüm");
    }

    @Test
    void sayiTuruOnemsizSayiOlmayanAtlanir() {
        assertEquals(5, GunesPatlamasiAyarGocu.plan(0, Map.<String, Object>of(ONCU, 90.0)::get).get(ONCU));
        assertEquals(5, GunesPatlamasiAyarGocu.plan(0, Map.<String, Object>of(ONCU, 90L)::get).get(ONCU));
        assertTrue(GunesPatlamasiAyarGocu.plan(0, Map.<String, Object>of(ONCU, "90")::get).isEmpty());
    }

    @Test
    void tabloSiraliYeniEskilerdeYok() {
        Set<String> gorulen = new HashSet<>();
        int onceki = 0;
        for (SurumluGocPlani.Adim a : GunesPatlamasiAyarGocu.ADIMLAR) {
            assertTrue(a.surum() >= onceki && a.surum() >= 1 && a.surum() <= GunesPatlamasiAyarGocu.GUNCEL, a.yol());
            onceki = a.surum();
            assertTrue(gorulen.add(a.surum() + "/" + a.yol()), "aynı sürümde iki adım: " + a.yol());
            assertTrue(a.yol().startsWith("gunes_patlamasi."), a.yol());
            assertFalse(a.eskiler().isEmpty(), a.yol());
            for (Number eski : a.eskiler()) {
                assertTrue(Math.abs(eski.doubleValue() - a.yeni().doubleValue()) > 1e-9, "yeni eskilerde: " + a.yol());
            }
            assertFalse(a.yol().equals(GunesPatlamasiAyarGocu.SURUM_YOLU));
        }
    }
}
