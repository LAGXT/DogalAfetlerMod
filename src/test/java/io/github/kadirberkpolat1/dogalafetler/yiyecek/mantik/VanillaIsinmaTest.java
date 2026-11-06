package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Vanilla pişmiş yiyecek eşlemesi (madde 14 son kural): çok küçük ve kısa ısınma, config listesi. */
class VanillaIsinmaTest {
    @Test
    void satirCozulur() {
        VanillaIsinma.Girdi g = VanillaIsinma.coz("minecraft:baked_potato=30");
        assertEquals("minecraft:baked_potato", g.esya());
        assertEquals(30, g.sureSaniye());
        YiyecekTanimi t = VanillaIsinma.tanim(g);
        assertEquals("minecraft:baked_potato", t.tur(), "azalan verim türü eşya kimliği");
        assertEquals(Yon.ISINMA, t.yon());
        assertEquals(Buyukluk.COK_KUCUK, t.buyukluk());
        assertEquals(600, t.sureTick());
        assertFalse(t.sicaktaGuclenir());
        assertEquals(" mod:yemek ", " " + VanillaIsinma.coz(" mod:yemek = 12 ").esya() + " ", "boşluklar kırpılır");
    }

    @Test
    void gecersizSatirlarReddedilir() {
        for (String kotu : new String[] {"", "baked_potato=30", "minecraft:baked_potato", "minecraft:baked_potato=0",
                "minecraft:baked_potato=601", "minecraft:baked_potato=abc", "Minecraft:Baked=30", "a:b=30=4"}) {
            assertFalse(VanillaIsinma.gecerliMi(kotu), kotu);
            assertThrows(IllegalArgumentException.class, () -> VanillaIsinma.coz(kotu), kotu);
        }
        assertFalse(VanillaIsinma.gecerliMi(42));
    }

    @Test
    void varsayilanListeGecerliKisaVePismisYiyecekler() {
        Set<String> esyalar = new HashSet<>();
        for (String satir : VanillaIsinma.VARSAYILAN) {
            assertTrue(VanillaIsinma.gecerliMi(satir), satir);
            VanillaIsinma.Girdi g = VanillaIsinma.coz(satir);
            assertTrue(esyalar.add(g.esya()), "tekrar: " + satir);
            assertTrue(g.esya().startsWith("minecraft:"), satir);
            assertTrue(g.sureSaniye() <= 60, "kısa: " + satir);
        }
        assertTrue(esyalar.contains("minecraft:baked_potato"), "şartnamenin örneği: kızarmış patates");
        assertTrue(esyalar.contains("minecraft:cooked_beef"));
        assertFalse(esyalar.contains("minecraft:beef"), "çiğ yiyecek ısıtmaz");
    }
}
