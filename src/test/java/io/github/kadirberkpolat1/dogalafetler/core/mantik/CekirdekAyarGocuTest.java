package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CekirdekAyarGocuTest {
    @Test
    void eskiVarsayilanlarYeniyeCekilirElleDegisenKalir() {
        Map<String, Object> dosya = new HashMap<>();
        dosya.put("cekirdek.zamanlayici.baslangicBeklemeTick", 24_000L);
        dosya.put("cekirdek.zamanlayici.genelBeklemeTick", 9_000L);
        dosya.put("cekirdek.afetler.kasirga.beklemeTick", 48_000);
        dosya.put("cekirdek.afetler.deprem.beklemeTick", 48_000L);
        dosya.put("cekirdek.afetler.ay_carpmasi.beklemeTick", 240_000L);
        Map<String, Number> plan = CekirdekAyarGocu.plan(0, dosya::get);
        assertEquals(6_000L, plan.get("cekirdek.zamanlayici.baslangicBeklemeTick"));
        assertTrue(!plan.containsKey("cekirdek.zamanlayici.genelBeklemeTick"), "elle değişen 9000 kalmalı");
        assertEquals(24_000L, plan.get("cekirdek.afetler.kasirga.beklemeTick"), "sayı türü önemsiz");
        assertEquals(24_000L, plan.get("cekirdek.afetler.deprem.beklemeTick"));
        assertTrue(!plan.containsKey("cekirdek.afetler.ay_carpmasi.beklemeTick"), "ay çarpması beklemesi değişmedi");
        assertTrue(!plan.containsKey("cekirdek.afetler.tsunami.beklemeTick"), "dosyada olmayan anahtar Forge'a kalır");
    }

    @Test
    void guncelSurumdeGocYok() {
        Map<String, Object> dosya = Map.of("cekirdek.zamanlayici.baslangicBeklemeTick", 24_000L);
        assertTrue(CekirdekAyarGocu.plan(CekirdekAyarGocu.GUNCEL, dosya::get).isEmpty());
    }

    @Test
    void yeniVarsayilanlarTabloylaAyni() {
        ZamanlayiciAyarlari v = ZamanlayiciAyarlari.VARSAYILAN;
        for (CekirdekAyarGocu.Adim a : CekirdekAyarGocu.ADIMLAR) {
            long beklenen = a.yol().endsWith("baslangicBeklemeTick") ? v.baslangicBeklemeTick()
                    : a.yol().endsWith("genelBeklemeTick") ? v.genelBeklemeTick()
                    : v.turBekleme(io.github.kadirberkpolat1.dogalafetler.core.AfetTuru.KASIRGA);
            assertEquals(beklenen, a.yeni(), a.yol());
        }
    }
}
