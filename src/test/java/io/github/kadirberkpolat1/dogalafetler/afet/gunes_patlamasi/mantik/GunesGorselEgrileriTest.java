package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GunesGorselEgrileriTest {
    private static final float EPS = 1e-6f;
    private static final float SIREN_SONU = GunesPatlamasiEgrileri.SIREN_SONU;

    @Test
    void auroraSirendeAcilirYalnizGeceVeYagmursuz() {
        assertEquals(0.35f, GunesGorselEgrileri.aurora(0f, 1f, 0f), EPS, "siren başında sönük");
        assertEquals(1f, GunesGorselEgrileri.aurora(SIREN_SONU, 1f, 0f), EPS, "siren sonunda tam");
        assertEquals(0f, GunesGorselEgrileri.aurora(1f, 0f, 0f), EPS, "gündüz yok");
        assertEquals(0f, GunesGorselEgrileri.aurora(1f, 1f, 1f), EPS, "yağmurda yok");
        float onceki = -1f;
        for (int i = 0; i <= 100; i++) {
            float a = GunesGorselEgrileri.aurora(SIREN_SONU * i / 100f, 1f, 0f);
            assertTrue(a >= onceki, "monoton");
            onceki = a;
        }
    }

    @Test
    void isiDalgasiGolgedeYokGunleArtar() {
        assertEquals(0f, GunesGorselEgrileri.isiDalgasi(1f, 1f, false), EPS, "göğü görmeyen yerde yok");
        assertEquals(0f, GunesGorselEgrileri.isiDalgasi(1f, 0f, true), EPS, "gece yok");
        assertEquals(0f, GunesGorselEgrileri.isiDalgasi(0f, 1f, true), EPS, "siren başında yok");
        assertEquals(1f, GunesGorselEgrileri.isiDalgasi(1f, 1f, true), EPS, "5. gün sonunda tam");
        assertTrue(GunesGorselEgrileri.isiDalgasi(0.5f, 1f, true) > GunesGorselEgrileri.isiDalgasi(SIREN_SONU, 1f, true));
    }

    @Test
    void kurulukIkinciGundenSonraBaslarBasamaklanir() {
        assertEquals(0f, GunesGorselEgrileri.kuruluk(SIREN_SONU), EPS, "1. gün yeşil");
        assertEquals(0f, GunesGorselEgrileri.kuruluk(GunesGorselEgrileri.KURULUK_BASI), EPS, "2. gün sonu başlangıç");
        assertEquals(GunesGorselEgrileri.KURULUK_TAVANI, GunesGorselEgrileri.kuruluk(1f), EPS, "son: tavan");
        assertEquals(0, GunesGorselEgrileri.kurulukBasamagi(0f));
        assertEquals(GunesGorselEgrileri.KURULUK_BASAMAGI,
                GunesGorselEgrileri.kurulukBasamagi(GunesGorselEgrileri.KURULUK_TAVANI));
        assertEquals(GunesGorselEgrileri.KURULUK_TAVANI,
                GunesGorselEgrileri.basamakPayi(GunesGorselEgrileri.KURULUK_BASAMAGI), EPS);
        float onceki = -1f;
        for (int i = 0; i <= 100; i++) {
            float k = GunesGorselEgrileri.kuruluk(i / 100f);
            assertTrue(k >= onceki, "monoton");
            onceki = k;
        }
    }

    @Test
    void karisimRenkYokuKorurUclariVerir() {
        assertEquals(-1, GunesGorselEgrileri.karistir(-1, 0x123456, 0.5f), "renk yok korunur");
        assertEquals(0x79C05A, GunesGorselEgrileri.karistir(0x79C05A, GunesGorselEgrileri.KURU_CIMEN, 0f));
        assertEquals(GunesGorselEgrileri.KURU_CIMEN,
                GunesGorselEgrileri.karistir(0x79C05A, GunesGorselEgrileri.KURU_CIMEN, 1f) & 0xFFFFFF);
        assertEquals(0x808080, GunesGorselEgrileri.karistir(0x000000, 0xFFFFFF, 0.502f) & 0xFFFFFF);
    }

    @Test
    void safakTonuDogusOrtasindaTepe() {
        assertEquals(0f, GunesGorselEgrileri.safakTonu(0f), EPS);
        assertEquals(0.14f, GunesGorselEgrileri.safakTonu(0.5f), 1e-5f);
        assertEquals(0f, GunesGorselEgrileri.safakTonu(1f), EPS, "doğuş tamam: ton yok");
    }

    @Test
    void isinDeseniSinirli() {
        for (int dilim = 0; dilim < 200; dilim++) {
            float v = GunesGorselEgrileri.isin(dilim, dilim * 37.5f, dilim % 3);
            assertTrue(v >= 0.1f - EPS && v <= 1f + EPS, "0,1–1: " + v);
        }
    }
}
