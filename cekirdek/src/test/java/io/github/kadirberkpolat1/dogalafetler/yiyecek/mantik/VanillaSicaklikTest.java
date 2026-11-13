package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Vanilla yiyecek eşlemesi (madde 14 son kural, S3-06): kademeli ısınma ve serinlik listeleri, eski satır biçimi. */
class VanillaSicaklikTest {
    @Test
    void kademeliSatirCozulur() {
        VanillaSicaklik.Girdi g = VanillaSicaklik.coz("minecraft:rabbit_stew=kucuk:90");
        assertEquals("minecraft:rabbit_stew", g.esya());
        assertEquals(Buyukluk.KUCUK, g.buyukluk());
        assertEquals(90, g.sureSaniye());
        YiyecekTanimi t = VanillaSicaklik.tanim(g, Yon.ISINMA);
        assertEquals("minecraft:rabbit_stew", t.tur(), "azalan verim türü eşya kimliği");
        assertEquals(Yon.ISINMA, t.yon());
        assertEquals(Buyukluk.KUCUK, t.buyukluk());
        assertEquals(1800, t.sureTick());
        assertFalse(t.sicaktaGuclenir());
        assertEquals(Yon.SERINLIK, VanillaSicaklik.tanim(VanillaSicaklik.coz("minecraft:melon_slice=cok_kucuk:30"),
                Yon.SERINLIK).yon(), "yön listeden gelir");
        VanillaSicaklik.Girdi bosluklu = VanillaSicaklik.coz(" mod:yemek = cok_buyuk : 12 ");
        assertEquals("mod:yemek", bosluklu.esya(), "boşluklar kırpılır");
        assertEquals(Buyukluk.COK_BUYUK, bosluklu.buyukluk());
        assertEquals(12, bosluklu.sureSaniye());
    }

    @Test
    void eskiBicimCokKucukSayilir() {
        VanillaSicaklik.Girdi g = VanillaSicaklik.coz("minecraft:baked_potato=30");
        assertEquals(Buyukluk.COK_KUCUK, g.buyukluk());
        assertEquals(30, g.sureSaniye());
        for (String satir : VanillaSicaklik.ESKI_ISINMA) {
            assertTrue(VanillaSicaklik.gecerliMi(satir), "elle korunmuş eski liste geçerli kalır: " + satir);
            assertEquals(Buyukluk.COK_KUCUK, VanillaSicaklik.coz(satir).buyukluk(), satir);
        }
    }

    @Test
    void gecersizSatirlarReddedilir() {
        for (String kotu : new String[] {"", "baked_potato=30", "minecraft:baked_potato", "minecraft:baked_potato=0",
                "minecraft:baked_potato=601", "minecraft:baked_potato=abc", "Minecraft:Baked=30", "a:b=30=4",
                "minecraft:baked_potato=orta", "minecraft:baked_potato=dev:30", "minecraft:baked_potato=ORTA:30",
                "minecraft:baked_potato=orta:0", "minecraft:baked_potato=orta:30:5", "minecraft:baked_potato=:30"}) {
            assertFalse(VanillaSicaklik.gecerliMi(kotu), kotu);
            assertThrows(IllegalArgumentException.class, () -> VanillaSicaklik.coz(kotu), kotu);
        }
        assertFalse(VanillaSicaklik.gecerliMi(42));
    }

    private static Map<String, VanillaSicaklik.Girdi> liste(List<String> satirlar) {
        Map<String, VanillaSicaklik.Girdi> m = new HashMap<>();
        for (String satir : satirlar) {
            assertTrue(VanillaSicaklik.gecerliMi(satir), satir);
            VanillaSicaklik.Girdi g = VanillaSicaklik.coz(satir);
            assertTrue(g.esya().startsWith("minecraft:"), satir);
            assertTrue(m.put(g.esya(), g) == null, "tekrar: " + satir);
            assertTrue(g.sureSaniye() <= 90, "vanilla etkisi kısa: " + satir);
            assertTrue(g.buyukluk().compareTo(Buyukluk.KUCUK) <= 0, "vanilla etkisi mod yiyeceklerinden zayıf: " + satir);
        }
        return m;
    }

    @Test
    void varsayilanIsinmaKademeliCorbalarEttenGuclu() {
        Map<String, VanillaSicaklik.Girdi> m = liste(VanillaSicaklik.VARSAYILAN_ISINMA);
        for (String esya : VanillaSicaklik.ESKI_ISINMA) {
            assertTrue(m.containsKey(esya.split("=")[0]), "eski listedeki yiyecek kalmalı: " + esya);
        }
        VanillaSicaklik.Girdi et = m.get("minecraft:cooked_beef");
        for (String corba : List.of("minecraft:mushroom_stew", "minecraft:beetroot_soup", "minecraft:rabbit_stew",
                "minecraft:suspicious_stew")) {
            VanillaSicaklik.Girdi g = m.get(corba);
            assertTrue(g.buyukluk().compareTo(et.buyukluk()) > 0 && g.sureSaniye() > et.sureSaniye(),
                    "çorba/yahni pişmiş etten güçlü ve uzun: " + corba);
        }
        assertTrue(m.get("minecraft:rabbit_stew").sureSaniye() >= m.get("minecraft:mushroom_stew").sureSaniye(),
                "etli yahni en uzun");
        assertEquals(Buyukluk.COK_KUCUK, m.get("minecraft:baked_potato").buyukluk(), "şartnamenin örneği: kızarmış patates");
        assertFalse(m.containsKey("minecraft:beef"), "çiğ yiyecek ısıtmaz");
        assertFalse(m.containsKey("minecraft:melon_slice"));
    }

    @Test
    void varsayilanSerinlikSogukYiyeceklerVeSuSisesi() {
        Map<String, VanillaSicaklik.Girdi> serin = liste(VanillaSicaklik.VARSAYILAN_SERINLIK);
        Map<String, VanillaSicaklik.Girdi> sicak = liste(VanillaSicaklik.VARSAYILAN_ISINMA);
        for (String esya : List.of("minecraft:melon_slice", "minecraft:sweet_berries", "minecraft:glow_berries",
                "minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish", "minecraft:milk_bucket",
                VanillaSicaklik.SU_SISESI)) {
            assertTrue(serin.containsKey(esya), "serinletmeli: " + esya);
        }
        assertTrue(serin.get("minecraft:milk_bucket").buyukluk().compareTo(serin.get("minecraft:melon_slice").buyukluk()) > 0,
                "içecekler meyveden güçlü");
        for (String esya : serin.keySet()) {
            assertFalse(sicak.containsKey(esya), "iki listede birden: " + esya);
        }
        assertFalse(serin.containsKey("minecraft:cooked_cod"), "pişmiş balık serinletmez");
    }
}
