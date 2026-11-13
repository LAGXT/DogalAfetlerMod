package io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SinkholeMantigiTest {
    @Test
    void cukurDaireselHalkalarBasamakliCekirdekEnDerin() {
        for (int sev = 1; sev <= 5; sev++) {
            SinkholeSeviyesi s = SinkholeSeviyesi.varsayilan(sev);
            CukurPlani.Plan p = CukurPlani.olustur(s, 99L + sev);
            assertEquals(s.basamak(), p.halkaSayisi());
            int[] enAzDerinlik = new int[p.halkaSayisi()];
            java.util.Arrays.fill(enAzDerinlik, Integer.MAX_VALUE);
            int oncekiHalka = 0;
            for (CukurPlani.Sutun c : p.sutunlar()) {
                assertTrue(Math.hypot(c.dx(), c.dz()) <= s.yaricap() + 1.01, "çap dışı sütun: " + c);
                assertTrue(c.halka() >= oncekiHalka, "halka sırası (çekirdek önce çöker)");
                oncekiHalka = c.halka();
                enAzDerinlik[c.halka()] = Math.min(enAzDerinlik[c.halka()], c.derinlik());
                assertTrue(c.derinlik() >= 1 && c.derinlik() <= s.derinlik() + 3);
            }
            for (int h = 1; h < p.halkaSayisi(); h++) {
                int enCokDis = p.halka(h).stream().mapToInt(CukurPlani.Sutun::derinlik).max().orElse(0);
                assertTrue(enCokDis < enAzDerinlik[h - 1] + 1, "S" + sev + " dış halka bir basamak sığ olmalı");
            }
            double alan = Math.PI * s.yaricap() * s.yaricap();
            assertTrue(p.sutunlar().size() > alan * 0.6 && p.sutunlar().size() < alan * 1.8, "S" + sev + " daire alanı");
            assertTrue(!p.catlaklar().isEmpty() && !p.kenar().isEmpty());
        }
    }

    @Test
    void asamaSureleriToplamiAktifSure() {
        for (int sev = 1; sev <= 5; sev++) {
            SinkholeSeviyesi s = SinkholeSeviyesi.varsayilan(sev);
            assertEquals(s.cokusTick(), s.sure(SinkholeSeviyesi.COKUS) + s.sure(SinkholeSeviyesi.GERI_CEKILME),
                    "S" + sev + " çöküş + geri çekilme = tablo çöküş süresi");
            assertEquals(s.catlamaTick() + SinkholeSeviyesi.SARKMA_TICK + s.cokusTick() + SinkholeSeviyesi.DURULMA_TICK,
                    s.aktifTick());
        }
        assertEquals(48, SinkholeSeviyesi.varsayilan(5).cap());
        assertEquals(6, SinkholeSeviyesi.varsayilan(1).cap());
    }
}
