package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShaderKoduTest {
    /** {@code shaders/lib/kopru.glsl} çözücüsünün float32 kopyası; imza tutmazsa null. */
    private static int[] coz(float[] c) {
        int[] veri = new int[3];
        for (int i = 0; i < 3; i++) {
            float x = c[i] * 255f;
            float k = (float) Math.floor((x - (float) Math.floor(x)) * ShaderKodu.KESIR);
            if (k < ShaderKodu.IMZA || k >= ShaderKodu.IMZA + 1024) {
                return null;
            }
            veri[i] = (int) k - ShaderKodu.IMZA;
        }
        return veri;
    }

    @Test
    void herRenkteVeriGeriOkunur() {
        float[] isik = {0.35f, 0.75f, 0.85f, 1.15f};
        int afet = ShaderKodu.ASIT | ShaderKodu.SEL | ShaderKodu.ORMAN_YANGINI;
        for (float v = 0f; v <= 1f; v += 1f / 97f) {
            float[] c = ShaderKodu.kodla(v, 1f - v, v * 0.5f, 3, false, true, isik, afet);
            int[] d = coz(c);
            assertEquals(3 | 16 | (ShaderKodu.nicele(0.35f) << 5), d[0], "kırmızı @" + v);
            assertEquals(ShaderKodu.nicele(0.75f) | (ShaderKodu.nicele(0.85f) << 5), d[1], "yeşil @" + v);
            assertEquals(ShaderKodu.nicele(1.15f) | (afet << 5), d[2], "mavi @" + v);
        }
    }

    @Test
    void donusEvresiUcBiteSigarVeImzaBozulmaz() {
        float[] isik = {1f, 1f, 1f, 1f};
        for (float v = 0f; v <= 1f; v += 1f / 53f) {
            int[] d = coz(ShaderKodu.kodla(v, v * 0.8f, 1f - v, ShaderKodu.EVRE_DONUS, true, false, isik, 0));
            assertEquals(ShaderKodu.EVRE_DONUS, d[0] & 7, "evre @" + v);
            assertEquals(8, d[0] & 24, "ay bitleri @" + v);
        }
    }

    @Test
    void gorunenRenkYarimAdimdanAzKayar() {
        float[] isik = {1.55f, 1.55f, 1.55f, 1.55f};
        for (float v = 0f; v <= 1f; v += 1f / 211f) {
            float[] c = ShaderKodu.kodla(v, v, v, 6, true, true, isik, 31);
            for (float k : c) {
                assertTrue(Math.abs(k - v) <= 1f / 255f, "kayma @" + v + ": " + k);
                assertTrue(k <= 1f, "1'i aştı @" + v);
            }
        }
    }

    @Test
    void kodlanmamisRenkCoguncaReddedilir() {
        int kabul = 0;
        int n = 0;
        for (float r = 0f; r <= 1f; r += 0.0137f) {
            for (float b = 0f; b <= 1f; b += 0.0173f) {
                n++;
                if (coz(new float[] {r, 0.7f * r + 0.1f, b}) != null) {
                    kabul++;
                }
            }
        }
        assertTrue(kabul < n / 20, "imza fazla geçirgen: " + kabul + "/" + n);
        assertNull(coz(new float[] {0.13f, 0.13f, 0.13f}));
    }
}
