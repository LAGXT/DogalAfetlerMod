package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** S3-09: sıcaklık-2 dünyasının eşik ve kaynak varsayılanları yenisine çekilir, elle değerler korunur. */
class SicaklikAyarGocuTest {
    private static final String E = "sicaklik.esikler.";

    /** Sıcaklık-2 varsayılanlarıyla yazılmış, sürüm anahtarsız dosya. */
    private static Map<String, Object> eskiDosya() {
        Map<String, Object> d = new HashMap<>();
        d.put(E + "notrBant", 15.0);
        d.put(E + "soguk", -30.0);
        d.put(E + "cokSoguk", -55.0);
        d.put(E + "donmaHasari", -75.0);
        d.put(E + "sicak", 30.0);
        d.put(E + "cokSicak", 55.0);
        d.put(E + "yanmaHasari", 75.0);
        d.put("sicaklik.kaynaklar", new ArrayList<>(SicaklikAyarGocu.KAYNAKLAR_0));
        return d;
    }

    @Test
    void eskiVarsayilanlarYeniEsiklereGecer() {
        Map<String, Number> plan = SicaklikAyarGocu.plan(0, eskiDosya()::get);
        Esikler v = Esikler.VARSAYILAN;
        assertEquals(v.notrBant(), plan.get(E + "notrBant").doubleValue());
        assertEquals(v.soguk(), plan.get(E + "soguk").doubleValue());
        assertEquals(v.cokSoguk(), plan.get(E + "cokSoguk").doubleValue());
        assertEquals(v.donmaHasari(), plan.get(E + "donmaHasari").doubleValue());
        assertEquals(v.sicak(), plan.get(E + "sicak").doubleValue());
        assertEquals(v.cokSicak(), plan.get(E + "cokSicak").doubleValue());
        assertEquals(v.yanmaHasari(), plan.get(E + "yanmaHasari").doubleValue());
        assertEquals(Map.of("sicaklik.kaynaklar", SicaklikAyarGocu.KAYNAKLAR),
                SicaklikAyarGocu.listePlani(0, eskiDosya()::get));
    }

    @Test
    void elleDegistirilenlerKorunur() {
        Map<String, Object> d = eskiDosya();
        d.put(E + "soguk", -25.0);
        List<String> liste = new ArrayList<>(SicaklikAyarGocu.KAYNAKLAR_0);
        liste.add("minecraft:glowstone=3:2");
        d.put("sicaklik.kaynaklar", liste);
        assertTrue(!SicaklikAyarGocu.plan(0, d::get).containsKey(E + "soguk"));
        assertTrue(SicaklikAyarGocu.listePlani(0, d::get).isEmpty(), "elle satır eklenmiş liste korunur");
    }

    @Test
    void guncelDosyaDegismez() {
        assertTrue(SicaklikAyarGocu.plan(SicaklikAyarGocu.GUNCEL, eskiDosya()::get).isEmpty());
        assertTrue(SicaklikAyarGocu.listePlani(SicaklikAyarGocu.GUNCEL, eskiDosya()::get).isEmpty());
    }

    @Test
    void yeniVarsayilanlarTabloylaVeKomurSatirlariylaUyumlu() {
        assertTrue(SicaklikAyarGocu.KAYNAKLAR.containsAll(SicaklikAyarGocu.KAYNAKLAR_0));
        assertTrue(SicaklikAyarGocu.KAYNAKLAR.contains("dogalafetler:komurlesmis_kutuk=2:2"));
        assertTrue(SicaklikAyarGocu.KAYNAKLAR.contains("dogalafetler:komurlesmis_tahta=1.5:2"));
        for (String satir : SicaklikAyarGocu.KAYNAKLAR) {
            assertTrue(KaynakTanimi.gecerliMi(satir), satir);
        }
        assertEquals("sicaklik.ayarSurumu", SicaklikAyarGocu.PLAN.surumYolu());
        assertEquals(SicaklikAyarGocu.ADIMLAR, SicaklikAyarGocu.PLAN.adimlar());
        assertEquals(SicaklikAyarGocu.LISTE_ADIMLARI, SicaklikAyarGocu.PLAN.listeAdimlari());
    }
}
