package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CekirdekAyarGocuTest {
    private static final ZamanlayiciAyarlari Z = ZamanlayiciAyarlari.VARSAYILAN;
    private static final SiddetAyarlari S = SiddetAyarlari.VARSAYILAN;

    @Test
    void surumSifirdanEskiVarsayilanlarZincirleYeniyeCekilirElleDegisenKalir() {
        Map<String, Object> dosya = new HashMap<>();
        dosya.put("cekirdek.zamanlayici.baslangicBeklemeTick", 24_000L);
        dosya.put("cekirdek.zamanlayici.genelBeklemeTick", 9_000L);
        dosya.put("cekirdek.afetler.kasirga.beklemeTick", 48_000);
        dosya.put("cekirdek.afetler.deprem.beklemeTick", 48_000L);
        dosya.put("cekirdek.afetler.ay_carpmasi.beklemeTick", 240_000L);
        Map<String, Number> plan = CekirdekAyarGocu.plan(0, dosya::get);
        assertEquals(Z.baslangicBeklemeTick(), plan.get("cekirdek.zamanlayici.baslangicBeklemeTick"), "24000 → 6000 → 3600");
        assertFalse(plan.containsKey("cekirdek.zamanlayici.genelBeklemeTick"), "elle değişen 9000 kalmalı");
        assertEquals(Z.turBekleme(AfetTuru.KASIRGA), plan.get("cekirdek.afetler.kasirga.beklemeTick"), "sayı türü önemsiz");
        assertEquals(Z.turBekleme(AfetTuru.DEPREM), plan.get("cekirdek.afetler.deprem.beklemeTick"));
        assertFalse(plan.containsKey("cekirdek.afetler.ay_carpmasi.beklemeTick"), "ay çarpması beklemesi değişmedi");
        assertFalse(plan.containsKey("cekirdek.afetler.tsunami.beklemeTick"), "dosyada olmayan anahtar Forge'a kalır");
    }

    /**
     * Kullanıcının "Yeni Dünya (5)" toml'u sürüm 1'de, eski varsayılanlar yazılı: K-E67 değişiklikleri göçle etkili olur.
     */
    @Test
    void surumBirDunyasindaKE67VarsayilanlariUygulanir() {
        Map<String, Object> dosya = new HashMap<>();
        dosya.put("cekirdek.zamanlayici.gunlukAfetOrani", 0.8);
        dosya.put("cekirdek.zamanlayici.genelBeklemeTick", 6_000L);
        dosya.put("cekirdek.zamanlayici.siddetBeklemeCarpani", 0.25);
        dosya.put("cekirdek.zamanlayici.baslangicBeklemeTick", 6_000L);
        dosya.put("cekirdek.zamanlayici.ilkAfetGarantiTick", 24_000L);
        dosya.put("cekirdek.afetler.sinkhole.beklemeTick", 24_000L);
        dosya.put("cekirdek.afetler.gunes_patlamasi.beklemeTick", 480_000L);
        dosya.put("cekirdek.onUyari.enAzTick", 900);
        dosya.put("cekirdek.onUyari.enCokTick", 1800);
        dosya.put("cekirdek.onUyari.erkenBildirimTick", 600);
        dosya.put("cekirdek.siddet.seviye2Gunu", 10);
        dosya.put("cekirdek.siddet.seviye3Gunu", 25);
        dosya.put("cekirdek.siddet.seviye4Gunu", 50);
        dosya.put("cekirdek.siddet.seviye5Gunu", 80);
        dosya.put("cekirdek.siddet.nadirlikBaslangic", 0.35);
        dosya.put("cekirdek.siddet.nadirlikSon", 0.6);
        dosya.put("cekirdek.siddet.nadirlikGecisGunu", 120.0);
        dosya.put("cekirdek.siddet.acilisRampaGunu", 15.0);
        dosya.put("cekirdek.siddet.acilisBaslangicOrani", 0.2);
        Map<String, Number> plan = CekirdekAyarGocu.plan(1, dosya::get);
        assertEquals(Z.gunlukAfetOrani(), plan.get("cekirdek.zamanlayici.gunlukAfetOrani"));
        assertEquals(Z.genelBeklemeTick(), plan.get("cekirdek.zamanlayici.genelBeklemeTick"));
        assertEquals(Z.siddetBeklemeCarpani(), plan.get("cekirdek.zamanlayici.siddetBeklemeCarpani"));
        assertEquals(Z.baslangicBeklemeTick(), plan.get("cekirdek.zamanlayici.baslangicBeklemeTick"));
        assertEquals(Z.ilkAfetGarantiTick(), plan.get("cekirdek.zamanlayici.ilkAfetGarantiTick"));
        assertEquals(Z.turBekleme(AfetTuru.SINKHOLE), plan.get("cekirdek.afetler.sinkhole.beklemeTick"));
        assertFalse(plan.containsKey("cekirdek.afetler.gunes_patlamasi.beklemeTick"));
        assertEquals(OnUyariPenceresi.VARSAYILAN_EN_AZ, plan.get("cekirdek.onUyari.enAzTick"));
        assertEquals(OnUyariPenceresi.VARSAYILAN_EN_COK, plan.get("cekirdek.onUyari.enCokTick"));
        assertEquals(OnUyariPenceresi.VARSAYILAN_ERKEN_BILDIRIM, plan.get("cekirdek.onUyari.erkenBildirimTick"));
        assertEquals(S.seviye2Gunu(), plan.get("cekirdek.siddet.seviye2Gunu"));
        assertEquals(S.seviye3Gunu(), plan.get("cekirdek.siddet.seviye3Gunu"));
        assertEquals(S.seviye4Gunu(), plan.get("cekirdek.siddet.seviye4Gunu"));
        assertEquals(S.seviye5Gunu(), plan.get("cekirdek.siddet.seviye5Gunu"));
        assertEquals(S.nadirlikBaslangic(), plan.get("cekirdek.siddet.nadirlikBaslangic"));
        assertEquals(S.nadirlikSon(), plan.get("cekirdek.siddet.nadirlikSon"));
        assertEquals(S.nadirlikGecisGunu(), plan.get("cekirdek.siddet.nadirlikGecisGunu"));
        assertEquals(S.acilisRampaGunu(), plan.get("cekirdek.siddet.acilisRampaGunu"));
        assertEquals(S.acilisBaslangicOrani(), plan.get("cekirdek.siddet.acilisBaslangicOrani"));
        assertEquals(18, plan.size(), "her eski varsayılan göçmeli: " + plan.keySet());

        Map<String, Object> elle = Map.of("cekirdek.siddet.seviye4Gunu", 30, "cekirdek.onUyari.enAzTick", 400);
        assertTrue(CekirdekAyarGocu.plan(1, elle::get).isEmpty(), "elle değiştirilmiş değerlere dokunulmaz");
    }

    @Test
    void guncelSurumdeGocYok() {
        Map<String, Object> dosya = Map.of("cekirdek.zamanlayici.baslangicBeklemeTick", 24_000L,
                "cekirdek.siddet.seviye4Gunu", 50);
        assertTrue(CekirdekAyarGocu.plan(CekirdekAyarGocu.GUNCEL, dosya::get).isEmpty());
    }

    /**
     * K-E72: sürüm 2 dosyası (K-E67 varsayılanları) seviye 4-5, nadirlik, gün çarpanı ve nadir ağırlıklarıyla (zincirle sürüm 4'ün) yeni
     * varsayılana çekilir.
     */
    @Test
    void surumIkiDunyasindaKE72VarsayilanlariUygulanir() {
        Map<String, Object> dosya = new HashMap<>();
        dosya.put("cekirdek.siddet.seviye3Gunu", 4);
        dosya.put("cekirdek.siddet.seviye4Gunu", 9);
        dosya.put("cekirdek.siddet.seviye5Gunu", 16);
        dosya.put("cekirdek.siddet.nadirlikBaslangic", 0.5);
        dosya.put("cekirdek.siddet.nadirlikSon", 0.8);
        dosya.put("cekirdek.siddet.nadirlikGecisGunu", 60.0);
        dosya.put("cekirdek.zamanlayici.siklikArtisCarpani", 8.0);
        dosya.put("cekirdek.zamanlayici.siklikArtisGunu", 45.0);
        dosya.put("cekirdek.afetler.ay_carpmasi.agirlik", 32);
        dosya.put("cekirdek.afetler.gunes_patlamasi.agirlik", 40);
        Map<String, Number> plan = CekirdekAyarGocu.plan(2, dosya::get);
        assertEquals(S.seviye4Gunu(), plan.get("cekirdek.siddet.seviye4Gunu"));
        assertEquals(S.seviye5Gunu(), plan.get("cekirdek.siddet.seviye5Gunu"));
        assertEquals(S.nadirlikSon(), plan.get("cekirdek.siddet.nadirlikSon"));
        assertEquals(Z.siklikArtisCarpani(), plan.get("cekirdek.zamanlayici.siklikArtisCarpani"));
        assertEquals(Z.agirlik(AfetTuru.AY_CARPMASI), plan.get("cekirdek.afetler.ay_carpmasi.agirlik"));
        assertFalse(plan.containsKey("cekirdek.afetler.gunes_patlamasi.agirlik"), "elle değişen 40 kalmalı");
        assertEquals(9, plan.size(), plan.keySet().toString());
    }

    @Test
    void tabloSurumSirasindaVeGuncelSurumDort() {
        assertEquals(4, CekirdekAyarGocu.GUNCEL);
        int onceki = 1;
        for (SurumluGocPlani.Adim a : CekirdekAyarGocu.ADIMLAR) {
            assertTrue(a.surum() >= onceki && a.surum() <= CekirdekAyarGocu.GUNCEL, a.toString());
            onceki = a.surum();
        }
        // Sürüm 3 dünyası (K-E72 varsayılanları): sürüm 4'ün nadirlik, günlük oran ve nadir ağırlık değerlerine çekilir.
        Map<String, Object> s3 = new HashMap<>();
        s3.put("cekirdek.siddet.nadirlikBaslangic", 0.7);
        s3.put("cekirdek.siddet.nadirlikSon", 1.5);
        s3.put("cekirdek.siddet.nadirlikGecisGunu", 150.0);
        s3.put("cekirdek.zamanlayici.gunlukAfetOrani", 5.0);
        s3.put("cekirdek.afetler.ay_carpmasi.agirlik", 50);
        s3.put("cekirdek.afetler.gunes_patlamasi.agirlik", 35);
        Map<String, Number> plan = CekirdekAyarGocu.plan(3, s3::get);
        assertEquals(S.nadirlikBaslangic(), plan.get("cekirdek.siddet.nadirlikBaslangic"));
        assertEquals(S.nadirlikSon(), plan.get("cekirdek.siddet.nadirlikSon"));
        assertEquals(S.nadirlikGecisGunu(), plan.get("cekirdek.siddet.nadirlikGecisGunu"));
        assertEquals(Z.gunlukAfetOrani(), plan.get("cekirdek.zamanlayici.gunlukAfetOrani"));
        assertEquals(Z.agirlik(AfetTuru.AY_CARPMASI), plan.get("cekirdek.afetler.ay_carpmasi.agirlik"));
        assertEquals(Z.agirlik(AfetTuru.GUNES_PATLAMASI), plan.get("cekirdek.afetler.gunes_patlamasi.agirlik"));
        assertEquals(6, plan.size(), plan.keySet().toString());
        Map<String, Object> elle = Map.of("cekirdek.afetler.ay_carpmasi.agirlik", 70, "cekirdek.siddet.nadirlikSon", 1.2);
        assertTrue(CekirdekAyarGocu.plan(3, elle::get).isEmpty(), "elle değiştirilen değerlere dokunulmaz");
    }
}
