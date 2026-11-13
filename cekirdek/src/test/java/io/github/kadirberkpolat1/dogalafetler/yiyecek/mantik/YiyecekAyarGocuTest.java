package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Yiyecek config göçü (S3-09): eski varsayılan ısınma listesi kademeli listeye, elle değiştirilen liste korunur. */
class YiyecekAyarGocuTest {
    private static final String YOL = "yiyecek.vanillaYiyecekler";

    /** Sıcaklık-3 öncesi dosya: ayarSurumu anahtarı yok, ısınma listesi eski varsayılanında. */
    private static Map<String, Object> eskiDosya() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("yiyecek.vanillaIsinma", true);
        d.put(YOL, new ArrayList<>(VanillaSicaklik.ESKI_ISINMA));
        d.put("yiyecek.kademe.orta", 20.0);
        return d;
    }

    @Test
    void eskiVarsayilanListeYeniyeCekilir() {
        Map<String, List<String>> plan = YiyecekAyarGocu.listePlani(0, eskiDosya()::get);
        assertEquals(Map.of(YOL, VanillaSicaklik.VARSAYILAN_ISINMA), plan);
        assertNotEquals(VanillaSicaklik.ESKI_ISINMA, VanillaSicaklik.VARSAYILAN_ISINMA);
    }

    @Test
    void elleDegistirilenListeKorunur() {
        Map<String, Object> d = eskiDosya();
        List<String> elle = new ArrayList<>(VanillaSicaklik.ESKI_ISINMA);
        elle.add("mod:sicak_corba=45");
        d.put(YOL, elle);
        assertTrue(YiyecekAyarGocu.listePlani(0, d::get).isEmpty(), "elle eklenmiş satır listeyi korur");
        List<String> sirasiz = new ArrayList<>(VanillaSicaklik.ESKI_ISINMA);
        sirasiz.add(0, sirasiz.remove(sirasiz.size() - 1));
        d.put(YOL, sirasiz);
        assertTrue(YiyecekAyarGocu.listePlani(0, d::get).isEmpty(), "yeniden sıralanmış liste elle değiştirilmiştir");
        d.put(YOL, List.of());
        assertTrue(YiyecekAyarGocu.listePlani(0, d::get).isEmpty(), "boşaltılmış liste (kapatma) korunur");
        d.put(YOL, new ArrayList<>(VanillaSicaklik.VARSAYILAN_ISINMA));
        assertTrue(YiyecekAyarGocu.listePlani(0, d::get).isEmpty(), "yeni varsayılan kalır");
    }

    @Test
    void eksikAnahtaraVeGuncelSurumeDokunulmaz() {
        assertTrue(YiyecekAyarGocu.listePlani(0, Map.<String, Object>of()::get).isEmpty(),
                "Forge eksik anahtarı kendisi doldurur");
        assertTrue(YiyecekAyarGocu.listePlani(YiyecekAyarGocu.GUNCEL, eskiDosya()::get).isEmpty());
        assertTrue(YiyecekAyarGocu.listePlani(YiyecekAyarGocu.GUNCEL + 1, eskiDosya()::get).isEmpty(), "ileri sürüm");
        assertTrue(YiyecekAyarGocu.listePlani(0, Map.<String, Object>of(YOL, "liste değil")::get).isEmpty());
    }

    @Test
    void planTutarli() {
        SurumluGocPlani plan = YiyecekAyarGocu.PLAN;
        assertEquals(YiyecekAyarGocu.SURUM_YOLU, plan.surumYolu());
        assertEquals("yiyecek.ayarSurumu", plan.surumYolu());
        assertEquals(YiyecekAyarGocu.GUNCEL, plan.guncel());
        assertTrue(plan.adimlar().isEmpty(), "sayı varsayılanı değişmedi");
        for (SurumluGocPlani.ListeAdimi a : plan.listeAdimlari()) {
            assertTrue(a.surum() >= 1 && a.surum() <= YiyecekAyarGocu.GUNCEL, a.yol());
            assertTrue(a.yol().startsWith(YiyecekAyarGocu.BOLUM + "."), a.yol());
            assertFalse(a.eskiler().contains(a.yeni()), a.yol());
            for (String satir : a.yeni()) {
                assertTrue(VanillaSicaklik.gecerliMi(satir), satir);
            }
        }
    }
}
