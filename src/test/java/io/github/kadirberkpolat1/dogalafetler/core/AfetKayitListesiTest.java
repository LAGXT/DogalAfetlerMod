package io.github.kadirberkpolat1.dogalafetler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class AfetKayitListesiTest {
    private static CompoundTag afet(String tur) {
        CompoundTag t = new CompoundTag();
        t.putString("tur", tur);
        return t;
    }

    @Test
    void listeYazilipOkununcaSiraKorunur() {
        CompoundTag donem = new CompoundTag();
        AfetKayitListesi.yaz(donem, List.of(afet("deprem"), afet("kasirga")));
        List<CompoundTag> okunan = AfetKayitListesi.oku(donem);
        assertEquals(2, okunan.size());
        assertEquals("deprem", okunan.get(0).getString("tur"));
        assertEquals("kasirga", okunan.get(1).getString("tur"));
        assertFalse(donem.contains(AfetKayitListesi.ESKI_ANAHTAR), "eski anahtar yazılmaz");
    }

    @Test
    void eskiTekAfetKaydiTekElemanliListeOlur() {
        CompoundTag donem = new CompoundTag();
        donem.put(AfetKayitListesi.ESKI_ANAHTAR, afet("tsunami"));
        List<CompoundTag> okunan = AfetKayitListesi.oku(donem);
        assertEquals(1, okunan.size());
        assertEquals("tsunami", okunan.get(0).getString("tur"));
    }

    @Test
    void bosListeYazilmazBosKayitBosOkunur() {
        CompoundTag donem = new CompoundTag();
        AfetKayitListesi.yaz(donem, List.of());
        assertFalse(donem.contains(AfetKayitListesi.ANAHTAR));
        assertTrue(AfetKayitListesi.oku(donem).isEmpty());
    }
}
