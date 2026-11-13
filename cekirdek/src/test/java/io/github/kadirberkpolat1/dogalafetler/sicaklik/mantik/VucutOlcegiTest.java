package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** S3-01: vücut birimi ↔ °C dönüşümü (oyuncuya görünen her değer buradan geçer). */
class VucutOlcegiTest {
    @Test
    void olcekUclariVeNotr() {
        assertEquals(37.0, VucutOlcegi.santigrat(0), 1e-12);
        assertEquals(32.0, VucutOlcegi.santigrat(-100), 1e-12);
        assertEquals(42.0, VucutOlcegi.santigrat(100), 1e-12);
        assertEquals(32.0, VucutOlcegi.EN_DUSUK_C, 1e-12);
        assertEquals(42.0, VucutOlcegi.EN_YUKSEK_C, 1e-12);
    }

    @Test
    void eskiKayitlarAnlamliSantigratVerir() {
        // Sıcaklık-2 dünyasındaki −30 birimlik kayıt aynen okunur ve 35,5 °C (üşüme) görünür.
        assertEquals(35.5, VucutOlcegi.santigrat(-30), 1e-12);
        assertEquals(SicaklikDurumu.USUME, Esikler.VARSAYILAN.kademe(-30).durum());
        assertEquals("35.5", VucutOlcegi.metin(VucutOlcegi.santigrat(-30)));
    }

    @Test
    void gidisDonusTutarli() {
        for (double c = 32; c <= 42; c += 0.25) {
            assertEquals(c, VucutOlcegi.santigrat(VucutOlcegi.birim(c)), 1e-9);
        }
        assertEquals(-20, VucutOlcegi.birim(36.0), 1e-9);
        assertEquals(50, VucutOlcegi.birim(39.5), 1e-9);
    }

    @Test
    void metinBirOndalikVeNoktali() {
        assertEquals("36.4", VucutOlcegi.metin(36.42));
        assertEquals("37.0", VucutOlcegi.metin(37));
        assertEquals("40.5", VucutOlcegi.metin(VucutOlcegi.santigrat(70)));
    }
}
