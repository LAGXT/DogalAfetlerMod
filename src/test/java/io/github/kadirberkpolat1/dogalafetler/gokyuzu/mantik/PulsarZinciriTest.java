package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarZinciri.AsamaAdimi;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PulsarZinciriTest {
    private static final long GUN = PulsarZinciri.GUN_TICK;

    @Test
    void oyunTickiUykuVeGunOfsetiniKatar() {
        assertEquals(1_000L, PulsarZinciri.oyunTick(1_000L, 0L, 0L));
        assertEquals(1_000L + 9_000L, PulsarZinciri.oyunTick(1_000L, 9_000L, 0L), "uyku atlanan gece kadar ileri");
        assertEquals(1_000L + 15 * GUN, PulsarZinciri.oyunTick(1_000L, 0L, 15L), "/dogalafet gun ofseti gün başına 24000");
        assertEquals(1_000L - 2 * GUN, PulsarZinciri.oyunTick(1_000L, 0L, -2L), "ofset geri de alabilir");
    }

    @Test
    void onBesGunDoluncaKaraDelikBaslar() {
        long bas = 5_000L;
        assertFalse(PulsarZinciri.doldu(bas, bas, 15));
        assertFalse(PulsarZinciri.doldu(bas, bas + 15 * GUN - 1, 15), "bir tick eksik");
        assertTrue(PulsarZinciri.doldu(bas, bas + 15 * GUN, 15), "tam 15 gün");
        assertEquals(15.0, PulsarZinciri.kalanGun(bas, bas, 15), 1e-9);
        assertEquals(10.5, PulsarZinciri.kalanGun(bas, bas + 4 * GUN + GUN / 2, 15), 1e-9);
        assertEquals(0.0, PulsarZinciri.kalanGun(bas, bas + 40 * GUN, 15), 1e-9, "alt sınır 0");
        assertEquals(1f / 3f, PulsarZinciri.ilerleme(bas, bas + 5 * GUN, 15), 1e-6);
        assertEquals(1f, PulsarZinciri.ilerleme(bas, bas + 99 * GUN, 15));
    }

    @Test
    void uykuVeOfsetSureyiKisaltir() {
        long bas = PulsarZinciri.oyunTick(24_000L, 0L, 0L);
        long uykuyla = PulsarZinciri.oyunTick(24_000L + 3 * GUN, 7 * GUN, 0L);
        assertEquals(5.0, PulsarZinciri.kalanGun(bas, uykuyla, 15), 1e-9, "3 gün yaşandı + 7 gece uyundu");
        long ofsetle = PulsarZinciri.oyunTick(24_000L, 0L, 15L);
        assertTrue(PulsarZinciri.doldu(bas, ofsetle, 15), "gunOfseti +15 zinciri ilerletir");
        long geri = PulsarZinciri.oyunTick(24_000L, 0L, -3L);
        assertEquals(0L, PulsarZinciri.gecen(bas, geri), "geri alınan ofset negatif süre vermez");
        assertEquals(15.0, PulsarZinciri.kalanGun(bas, geri, 15), 1e-9);
    }

    @Test
    void yutmaSaniyesiVeIlerlemesi() {
        assertEquals(0f, PulsarZinciri.yutmaSaniyesi(1_000L, 1_000L));
        assertEquals(0f, PulsarZinciri.yutmaSaniyesi(1_000L, 900L), "saat geri gitmez");
        assertEquals(45f, PulsarZinciri.yutmaSaniyesi(1_000L, 1_000L + 900L));
        assertEquals(0.5f, PulsarZinciri.yutmaIlerlemesi(45f), 1e-6);
        assertEquals(1f, PulsarZinciri.yutmaIlerlemesi(200f));
        assertFalse(PulsarZinciri.yutmaParlamasiGeldi(54.99f));
        assertTrue(PulsarZinciri.yutmaParlamasiGeldi(55f));
        assertFalse(PulsarZinciri.yutmaBitti(89.99f));
        assertTrue(PulsarZinciri.yutmaBitti(PulsarZinciri.YUTMA_SN));
        assertEquals(35f, PulsarZinciri.yutmaKalanSaniye(55f), 1e-6);
        assertEquals(0f, PulsarZinciri.yutmaKalanSaniye(120f));
        assertTrue(PulsarZinciri.YUTMA_PARLAMA_SN < PulsarZinciri.YUTMA_SN);
    }

    @Test
    void aktiflikYalnizPulsarYolunda() {
        assertTrue(PulsarZinciri.aktif(GunesEvresi.PULSAR, 0f, false));
        assertTrue(PulsarZinciri.aktif(GunesEvresi.KARADELIK_YUTMA, 0.3f, false));
        assertTrue(PulsarZinciri.aktif(GunesEvresi.GUNESSIZ, 0f, true));
        assertFalse(PulsarZinciri.aktif(GunesEvresi.GUNESSIZ, 0f, false), "süpernova yolundaki güneşsiz dönem");
        assertTrue(PulsarZinciri.aktif(GunesEvresi.KIZIL_CUCE, 0.9f, true));
        assertFalse(PulsarZinciri.aktif(GunesEvresi.KIZIL_CUCE, 1f, true), "doğuş bitti");
        assertFalse(PulsarZinciri.aktif(GunesEvresi.KIZIL_CUCE, 0.5f, false), "normal kızıl cüce");
        for (GunesEvresi evre : new GunesEvresi[] {GunesEvresi.NORMAL, GunesEvresi.BUYUYEN, GunesEvresi.SUPERNOVA}) {
            assertFalse(PulsarZinciri.aktif(evre, 0f, true), evre.kimlik());
        }
    }

    @Test
    void asamaSirasiPulsardanEskiGuneseUlasir() {
        assertEquals(AsamaAdimi.ISINMA, PulsarZinciri.asamaAdimi(GunesEvresi.PULSAR, 0.4f, false));
        assertEquals(AsamaAdimi.KARADELIK, PulsarZinciri.asamaAdimi(GunesEvresi.PULSAR, 1f, false));
        assertEquals(AsamaAdimi.GUNESSIZ, PulsarZinciri.asamaAdimi(GunesEvresi.KARADELIK_YUTMA, 0.2f, false));
        assertEquals(AsamaAdimi.KIZIL_CUCE, PulsarZinciri.asamaAdimi(GunesEvresi.GUNESSIZ, 0f, true));
        assertEquals(AsamaAdimi.ESKI_GUNES, PulsarZinciri.asamaAdimi(GunesEvresi.KIZIL_CUCE, 0.1f, true));
        assertEquals(AsamaAdimi.YOK, PulsarZinciri.asamaAdimi(GunesEvresi.GUNESSIZ, 0f, false), "süpernova yolu");
        assertEquals(AsamaAdimi.YOK, PulsarZinciri.asamaAdimi(GunesEvresi.KIZIL_CUCE, 1f, true));
        assertEquals(AsamaAdimi.YOK, PulsarZinciri.asamaAdimi(GunesEvresi.NORMAL, 0f, false));
        GunesEvresi evre = GunesEvresi.PULSAR;
        float ilerleme = 1f;
        boolean yol = false;
        GunesEvresi[] beklenen = {GunesEvresi.KARADELIK_YUTMA, GunesEvresi.GUNESSIZ, GunesEvresi.KIZIL_CUCE,
            GunesEvresi.NORMAL};
        for (GunesEvresi hedef : beklenen) {
            GunesEvresi yeni = PulsarZinciri.asamaAdimi(evre, ilerleme, yol).hedef();
            assertEquals(hedef, yeni);
            yol = yeni == GunesEvresi.GUNESSIZ || yeni == GunesEvresi.KIZIL_CUCE;
            evre = yeni;
            ilerleme = 0f;
        }
        assertEquals(Optional.empty(), PulsarZinciri.sonraki(GunesEvresi.NORMAL, false));
    }

    @Test
    void takmaAdYalnizKendiAsamasinda() {
        assertEquals(Optional.empty(), PulsarZinciri.takmaAd(GunesEvresi.PULSAR, 0f, false));
        assertEquals(Optional.of("karadelik"), PulsarZinciri.takmaAd(GunesEvresi.KARADELIK_YUTMA, 0f, false));
        assertEquals(Optional.of("gunessiz"), PulsarZinciri.takmaAd(GunesEvresi.GUNESSIZ, 0f, true));
        assertEquals(Optional.of("kizilcuce"), PulsarZinciri.takmaAd(GunesEvresi.KIZIL_CUCE, 0f, true));
        assertEquals(Optional.empty(), PulsarZinciri.takmaAd(GunesEvresi.GUNESSIZ, 0f, false), "yol dışı");
        assertEquals(Optional.empty(), PulsarZinciri.takmaAd(GunesEvresi.KIZIL_CUCE, 0f, false));
    }

    @Test
    void yutmaGunduzPenceresiniBekler() {
        long sure = 15 * PulsarZinciri.GUN_TICK;
        assertFalse(PulsarZinciri.yutmaBaslar(0, sure - 1, 15, 5_000, true), "günler dolmadı");
        assertFalse(PulsarZinciri.yutmaBaslar(0, sure, 15, 18_000, true), "gece beklenir");
        assertFalse(PulsarZinciri.yutmaBaslar(0, sure, 15, 2_999, true));
        assertTrue(PulsarZinciri.yutmaBaslar(0, sure, 15, 3_000, true));
        assertTrue(PulsarZinciri.yutmaBaslar(0, sure, 15, 7_199 + 5 * PulsarZinciri.GUN_TICK, true), "gün saati mod 24000");
        assertFalse(PulsarZinciri.yutmaBaslar(0, sure, 15, 7_200, true));
        assertTrue(PulsarZinciri.yutmaBaslar(0, sure, 15, 18_000, false), "gün döngüsü kapalı: hemen");
        assertFalse(PulsarZinciri.yutmaBaslar(0, sure + PulsarZinciri.YUTMA_EN_COK_BEKLEME - 1, 15, 18_000, true));
        assertTrue(PulsarZinciri.yutmaBaslar(0, sure + PulsarZinciri.YUTMA_EN_COK_BEKLEME, 15, 18_000, true),
                "bir gün beklendi: pencere gelmese de başlar");
    }
}
