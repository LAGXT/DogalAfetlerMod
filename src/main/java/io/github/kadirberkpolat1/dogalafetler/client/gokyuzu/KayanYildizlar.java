package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.vertex.BufferBuilder;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuAyarlari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Kayan yıldızlar: günün bu saniyesinden belirleyici hash ile üretilir (herkes aynı yıldızı görür), sabit sayıda yuva ve
 * tek çizim. Her yuvanın döngüsünde (8-18 sn) olasılıkla bir kayan yıldız çıkar; {@value #SURE} sn'de büyük çember yayında
 * ilerler, yumuşak belirir ve söner (ani parlama yok; fotosensitivite ayarı sıklığı düşürür). Dünya çerçevesinde, göğeün
 * görünür yarısında (yükseklik 14-76°) çizilir; derin uzay dönüşünden bağımsızdır. Olay şiddeti yüksekken
 * ({@link GokyuzuCizim#kritik}) olasılık artar ve iki ek yuva açılır.
 */
final class KayanYildizlar {
    private static final ResourceLocation DOKU = GokyuzuCizim.doku("kayan_yildiz");
    /** Yuva döngü süreleri (sn); hepsi 3600'ü böler. İlk 4 yuva her zaman, son 2 yuva olay sırasında açık. */
    private static final int[] PERIYOT = {8, 10, 12, 15, 9, 18};
    private static final int TEMEL_YUVA = 4;
    private static final float SURE = 1.1f;
    private static final float YARI_GENISLIK = 0.9f;

    private static final float[] BX = new float[PERIYOT.length];
    private static final float[] BY = new float[PERIYOT.length];
    private static final float[] BZ = new float[PERIYOT.length];
    private static final float[] KX = new float[PERIYOT.length];
    private static final float[] KY = new float[PERIYOT.length];
    private static final float[] KZ = new float[PERIYOT.length];
    private static final float[] YX = new float[PERIYOT.length];
    private static final float[] YY = new float[PERIYOT.length];
    private static final float[] YZ = new float[PERIYOT.length];
    private static final float[] ALFA = new float[PERIYOT.length];
    private static final float[] TON = new float[PERIYOT.length];

    private KayanYildizlar() {
    }

    /** {@code dunya}: dünya (göğün gün dönüşü öncesi) poz matrisi; toplamalı karma açık olmalı. */
    static void ciz(Matrix4f dunya, float sn, float uzay, float kritik) {
        if (uzay <= 0.02f) {
            return;
        }
        float hassasiyet = (float) GokyuzuAyarlari.pulsarTitresim();
        float olasilik = (0.4f + 0.45f * kritik) * (0.6f + 0.4f * hassasiyet);
        int yuva = kritik >= 0.6f ? PERIYOT.length : TEMEL_YUVA;
        int n = 0;
        for (int k = 0; k < yuva; k++) {
            int periyot = PERIYOT[k];
            int dongu = (int) Math.floor(sn / periyot);
            if (GokyuzuCizim.rastgele(dongu, k, 1) > olasilik) {
                continue;
            }
            float t0 = GokyuzuCizim.rastgele(dongu, k, 2) * (periyot - SURE - 0.1f);
            float q = (sn - dongu * periyot - t0) / SURE;
            if (q <= 0f || q >= 1f) {
                continue;
            }
            float az = GokyuzuCizim.rastgele(dongu, k, 3) * 360f * Mth.DEG_TO_RAD;
            float el = (14f + 62f * GokyuzuCizim.rastgele(dongu, k, 4)) * Mth.DEG_TO_RAD;
            float beta = GokyuzuCizim.rastgele(dongu, k, 5) * Mth.TWO_PI;
            float toplam = (20f + 20f * GokyuzuCizim.rastgele(dongu, k, 6)) * Mth.DEG_TO_RAD;
            float kuyruk = (9f + 5f * GokyuzuCizim.rastgele(dongu, k, 8)) * Mth.DEG_TO_RAD;
            // Başlangıç yönü p0 ve teğet t (p0'a dik, rastgele yönlü); nokta p(s) = p0·cos s + t·sin s büyük çember yayıdır.
            float ce = Mth.cos(el);
            float px = ce * Mth.cos(az);
            float py = Mth.sin(el);
            float pz = ce * Mth.sin(az);
            // u = p0'a dik yatay vektör (normalleştirilmiş), v = p0 × u
            float ul = Mth.sqrt(px * px + pz * pz);
            float ux = -pz / ul;
            float uy = 0f;
            float uz = px / ul;
            float vx = py * uz - pz * uy;
            float vy = pz * ux - px * uz;
            float vz = px * uy - py * ux;
            float cb = Mth.cos(beta);
            float sb = Mth.sin(beta);
            float tx = ux * cb + vx * sb;
            float ty = uy * cb + vy * sb;
            float tz = uz * cb + vz * sb;
            float sBas = q * toplam;
            float sKuy = sBas - kuyruk;
            float cs = Mth.cos(sBas);
            float ss = Mth.sin(sBas);
            float bx = px * cs + tx * ss;
            float by = py * cs + ty * ss;
            float bz = pz * cs + tz * ss;
            // Başta teğet: -p0·sin s + t·cos s; yan vektör w = p × teğet (birim).
            float gx = -px * ss + tx * cs;
            float gy = -py * ss + ty * cs;
            float gz = -pz * ss + tz * cs;
            float wx = by * gz - bz * gy;
            float wy = bz * gx - bx * gz;
            float wz = bx * gy - by * gx;
            float wl = Mth.sqrt(wx * wx + wy * wy + wz * wz);
            float w = YARI_GENISLIK / Math.max(wl, 1e-4f);
            float kc = Mth.cos(sKuy);
            float ks = Mth.sin(sKuy);
            BX[n] = bx * 100f;
            BY[n] = by * 100f;
            BZ[n] = bz * 100f;
            KX[n] = (px * kc + tx * ks) * 100f;
            KY[n] = (py * kc + ty * ks) * 100f;
            KZ[n] = (pz * kc + tz * ks) * 100f;
            YX[n] = wx * w;
            YY[n] = wy * w;
            YZ[n] = wz * w;
            ALFA[n] = uzay * (float) Math.pow(Mth.sin(Mth.PI * q), 0.8) * (0.55f + 0.45f * GokyuzuCizim.rastgele(dongu, k, 7));
            TON[n] = GokyuzuCizim.rastgele(dongu, k, 9);
            n++;
        }
        if (n == 0) {
            return;
        }
        BufferBuilder bb = GokyuzuCizim.baslat(DOKU);
        for (int i = 0; i < n; i++) {
            float g = 0.92f + 0.08f * TON[i];
            float b = 1f - 0.12f * TON[i];
            float a = Math.min(1f, ALFA[i]);
            bb.vertex(dunya, KX[i] - YX[i], KY[i] - YY[i], KZ[i] - YZ[i]).uv(0f, 0f).color(0.88f, g, b, a).endVertex();
            bb.vertex(dunya, BX[i] - YX[i], BY[i] - YY[i], BZ[i] - YZ[i]).uv(1f, 0f).color(0.88f, g, b, a).endVertex();
            bb.vertex(dunya, BX[i] + YX[i], BY[i] + YY[i], BZ[i] + YZ[i]).uv(1f, 1f).color(0.88f, g, b, a).endVertex();
            bb.vertex(dunya, KX[i] + YX[i], KY[i] + YY[i], KZ[i] + YZ[i]).uv(0f, 1f).color(0.88f, g, b, a).endVertex();
        }
        GokyuzuCizim.bitir(bb);
    }
}
