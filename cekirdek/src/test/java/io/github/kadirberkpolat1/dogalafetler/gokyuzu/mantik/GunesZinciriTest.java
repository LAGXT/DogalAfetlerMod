package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GunesZinciriTest {
    /** Zincir mekaniği sabit ayarla (1,5 gün süpernova, 7 gün kapı) sınanır; varsayılanlar ayrı testte. */
    private static final GunesZinciri.Ayarlar AYAR = new GunesZinciri.Ayarlar(1.5, 7.0, 6_000L, 30.0);

    @Test
    void varsayilanlarSartnameyeUyar() {
        GunesZinciri.Ayarlar v = GunesZinciri.Ayarlar.VARSAYILAN;
        assertEquals(24_000, v.supernovaTick(), "süpernova 1 gün (K-E72)");
        assertEquals(72_000, v.gecikmeTick(), "kızıl cüce 3 gün sonra (K-E72, Kutup Sığınağı alt sınırı)");
        assertThrows(IllegalArgumentException.class, () -> new GunesZinciri.Ayarlar(0.5, 7, 6000, 30));
        assertThrows(IllegalArgumentException.class, () -> new GunesZinciri.Ayarlar(2.5, 7, 6000, 30));
        assertThrows(IllegalArgumentException.class, () -> new GunesZinciri.Ayarlar(1.5, 2.9, 6000, 30),
                "Kutup Sığınağı'nın 3 gününden kısa gecikme reddedilmeli");
    }

    @Test
    void evreSirasiVeSureler() {
        assertEquals(Optional.of(GunesEvresi.GUNESSIZ), GunesZinciri.sonraki(GunesEvresi.SUPERNOVA));
        assertEquals(Optional.of(GunesEvresi.KIZIL_CUCE), GunesZinciri.sonraki(GunesEvresi.GUNESSIZ));
        assertEquals(Optional.empty(), GunesZinciri.sonraki(GunesEvresi.KIZIL_CUCE));
        assertEquals(Optional.empty(), GunesZinciri.sonraki(GunesEvresi.BUYUYEN), "büyüme afetindir");
        assertEquals(Optional.empty(), GunesZinciri.sonraki(GunesEvresi.NORMAL));
        assertEquals(36_000, GunesZinciri.evreSuresi(GunesEvresi.SUPERNOVA, AYAR));
        assertEquals(6_000, GunesZinciri.evreSuresi(GunesEvresi.KIZIL_CUCE, AYAR));
        assertEquals(-1, GunesZinciri.evreSuresi(GunesEvresi.BUYUYEN, AYAR));
        assertEquals(-1, GunesZinciri.evreSuresi(GunesEvresi.NORMAL, AYAR));
    }

    @Test
    void aktiflikYalnizZincirEvrelerinde() {
        assertTrue(GunesZinciri.aktif(GunesEvresi.SUPERNOVA, 0.99f));
        assertTrue(GunesZinciri.aktif(GunesEvresi.GUNESSIZ, 1f), "güneşsiz dönem kızıl cüceye kadar sürer");
        assertTrue(GunesZinciri.aktif(GunesEvresi.KIZIL_CUCE, 0.5f), "doğuş sürüyor");
        assertFalse(GunesZinciri.aktif(GunesEvresi.KIZIL_CUCE, 1f), "doğuş bitti: süreç tamam");
        assertFalse(GunesZinciri.aktif(GunesEvresi.BUYUYEN, 0.5f));
        assertFalse(GunesZinciri.aktif(GunesEvresi.NORMAL, 0f));
    }

    @Test
    void ilerlemeZamanAdimiylaBirikirVeBireKenetlenir() {
        long sure = AYAR.supernovaTick();
        float p = 0f;
        int adim = 0;
        while (p < 1f && adim < 100_000) {
            p = GunesZinciri.ilerlet(p, 20, sure);
            adim++;
        }
        assertEquals(1f, p);
        assertEquals(sure / 20, adim, 1, "1,5 gün = 1800 saniyelik adım");
        assertEquals(0.5f, GunesZinciri.ilerlet(0.5f, 0, sure), "hız 0 adımı ilerletmez");
        assertEquals(1f, GunesZinciri.ilerlet(0.2f, 1_000_000, sure));
        assertEquals(0.3f, GunesZinciri.ilerlet(0.3f, 100, -1), "süresiz evre ilerlemez");
    }

    @Test
    void kizilCuceKapisiDonmaYuzdeYuzdeBirikirAltindaSifirlanir() {
        long gecen = 0;
        gecen = GunesZinciri.kapiIlerlet(gecen, 99.5f, 1000);
        assertEquals(0, gecen, "donma %100 olmadan saat işlemez");
        for (int i = 0; i < 167; i++) {
            gecen = GunesZinciri.kapiIlerlet(gecen, 100f, 1000);
        }
        assertFalse(GunesZinciri.kapiAcik(gecen, AYAR), "7 günden önce kapı kapalı");
        gecen = GunesZinciri.kapiIlerlet(gecen, 100f, 1000);
        assertTrue(GunesZinciri.kapiAcik(gecen, AYAR), "7 gün dolunca kapı açık");
        assertEquals(0, GunesZinciri.kapiIlerlet(gecen, 80f, 20), "donma geri düşerse sayaç sıfırlanır");
        assertEquals(20, GunesZinciri.kapiIlerlet(0, 99.9995f, 20), "kayan nokta payı");
    }

    @Test
    void uykuVeZamanAtlamasiSafagiGecerseDogusKacmaz() {
        assertTrue(GunesZinciri.safakGecti(13_000, 24_000), "uyku geceyi atlayınca (13000 → 24000) şafak geçilmiş sayılır");
        assertTrue(GunesZinciri.safakGecti(22_999, 23_000), "pencereye girildi");
        assertFalse(GunesZinciri.safakGecti(22_000, 22_999));
        assertFalse(GunesZinciri.safakGecti(23_100, 23_200), "pencere içinde kalmak safakMi'nin işi");
        assertFalse(GunesZinciri.safakGecti(18_000, 1_000), "/time set geri almak şafak değildir");
        assertFalse(GunesZinciri.safakGecti(Long.MIN_VALUE, 23_500), "ilk tick: önceki saat yok");
        assertTrue(GunesZinciri.safakGecti(24_000L * 3 + 20_000, 24_000L * 4 + 100), "çok günlü saat");
        assertTrue(GunesZinciri.safakGecti(5_000, 5_000 + 3 * 24_000L), "time add birkaç gün");
    }

    @Test
    void dogusNedeniKapiSafakVeGunDongusu() {
        GunesZinciri.DogusNedeni bekle = GunesZinciri.DogusNedeni.BEKLE;
        assertEquals(bekle, GunesZinciri.dogusNedeni(false, 22_000, 23_500, true), "kapı kapalıyken şafak da doğurmaz");
        assertEquals(bekle, GunesZinciri.dogusNedeni(false, 6_000, 6_001, false), "kapı kapalıyken döngü kapalı da doğurmaz");
        assertEquals(bekle, GunesZinciri.dogusNedeni(true, 5_999, 6_000, true), "öğlen şafak beklenir");
        assertEquals(GunesZinciri.DogusNedeni.SAFAK, GunesZinciri.dogusNedeni(true, 22_999, 23_000, true));
        assertEquals(GunesZinciri.DogusNedeni.SAFAK_GECILDI, GunesZinciri.dogusNedeni(true, 13_000, 24_000, true),
                "uyku şafağı atlayınca");
        assertEquals(GunesZinciri.DogusNedeni.GUN_DONGUSU_KAPALI, GunesZinciri.dogusNedeni(true, 6_000, 6_000, false),
                "şef kararı: gün döngüsü kapalıyken o anki gün saatinde doğar");
        assertEquals(GunesZinciri.DogusNedeni.GUN_DONGUSU_KAPALI, GunesZinciri.dogusNedeni(true, Long.MIN_VALUE, 18_000, false),
                "gece yarısı da olsa beklenmez");
        assertEquals(GunesZinciri.DogusNedeni.GUN_DONGUSU_KAPALI, GunesZinciri.dogusNedeni(true, 23_100, 23_100, false),
                "döngü kapalıyken şafak penceresinde de neden döngüdür (log aynı)");
        assertFalse(GunesZinciri.DogusNedeni.BEKLE.dogar());
        assertTrue(GunesZinciri.DogusNedeni.GUN_DONGUSU_KAPALI.dogar());
    }

    @Test
    void dogusIlkSafagaHizalanir() {
        assertFalse(GunesZinciri.safakMi(6_000), "öğlen doğmaz");
        assertFalse(GunesZinciri.safakMi(22_999));
        assertTrue(GunesZinciri.safakMi(23_000), "şafak başlangıcı");
        assertTrue(GunesZinciri.safakMi(23_999));
        assertFalse(GunesZinciri.safakMi(24_000), "yeni gün 0 = şafak penceresi kapandı");
        assertTrue(GunesZinciri.safakMi(24_000L * 5 + 23_500), "çok günlü saat");
        assertEquals(17_000, GunesZinciri.safagaKalan(6_000));
        assertEquals(0, GunesZinciri.safagaKalan(23_400));
        assertEquals(23_000, GunesZinciri.safagaKalan(24_000));
        assertEquals(1_000, GunesZinciri.safagaKalan(-2_000), "negatif saat de kalanı doğru verir");
    }
}
