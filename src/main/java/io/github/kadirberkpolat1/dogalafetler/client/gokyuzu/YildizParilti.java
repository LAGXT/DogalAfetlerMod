package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuAyarlari;
import net.minecraft.util.Mth;

/**
 * Yıldız parıltısı ve titremesi: her yıldız grubunun (VBO) frekans ve fazları grup numarasından hash ile türetilir, yıldızlar
 * gruplara eşit dağıtıldığı için gökyüzü aynı anda değil, küme küme parlar. Frekanslar 0,22-1,5 Hz (3 Hz altı; küçükçe
 * alışılmış strob yok) ve {@code k/3600} Hz olduğundan {@code sn} sarıldığında faz sıçramaz. Fotosensitivite ayarı
 * ({@code pulsarTitresim}) genliği ve hızı düşürür; 0'da yalnız çok hafif, yavaş bir parıltı kalır. Parlak (ışınlı) yıldızlar
 * ayrıca birkaç saniyelik yumuşak nefes alır.
 */
final class YildizParilti {
    /** En fazla yıldız grubu ({@code GokyuzuCizici.hazirla} kalite tablosundaki en büyük grup sayısı). */
    static final int GRUP_MAKS = 14;
    /** Parlak yıldız grubu sayısı. */
    static final int PARLAK_GRUP = 3;
    private static final float[] PARLAK_PERIYOT = {5f, 7.5f, 9f};

    private static final float[] F1 = new float[GRUP_MAKS];
    private static final float[] F2 = new float[GRUP_MAKS];
    private static final float[] FAZ1 = new float[GRUP_MAKS];
    private static final float[] FAZ2 = new float[GRUP_MAKS];

    static {
        for (int g = 0; g < GRUP_MAKS; g++) {
            int k = 800 + (int) (GokyuzuCizim.rastgele(g, 1, 7) * 4600f);
            F1[g] = k / 3600f;
            F2[g] = Math.round(k * 0.37f) / 3600f;
            FAZ1[g] = GokyuzuCizim.rastgele(g, 2, 7) * Mth.TWO_PI;
            FAZ2[g] = GokyuzuCizim.rastgele(g, 3, 7) * Mth.TWO_PI;
        }
    }

    private YildizParilti() {
    }

    /** Fotosensitivite ayarı [0,1] (0 = en sakin). */
    static float hassasiyet() {
        return (float) GokyuzuAyarlari.pulsarTitresim();
    }

    /**
     * {@code g}. grubun bu andaki alfa çarpanı ve renk kayması: {@code cikis[0]} alfa (1 çevresinde), {@code cikis[1]} kırmızı
     * (+) / mavi (−) kayma. İki frekanslı çarpım (±%32'ye kadar) + hafif tayf kayması (atmosfer saçılımı hissi).
     */
    static void grup(int g, float sn, float hassasiyet, float[] cikis) {
        float genlik = (0.12f + 0.20f * hassasiyet) * (0.6f + 0.4f * hassasiyet);
        float s1 = Mth.sin(sn * Mth.TWO_PI * F1[g] + FAZ1[g]);
        float s2 = Mth.sin(sn * Mth.TWO_PI * F2[g] + FAZ2[g]);
        cikis[0] = 1f + genlik * s1 * (0.55f + 0.45f * s2);
        cikis[1] = 0.07f * s2 * (0.4f + 0.6f * hassasiyet);
    }

    /** Parlak yıldız grubunun alfa çarpanı (0,55..1): 5-9 sn'lik yumuşak nefes. */
    static float parlak(int g, float sn, float hassasiyet) {
        float nefes = 0.5f + 0.5f * Mth.sin(Mth.TWO_PI * sn / PARLAK_PERIYOT[g] + g * 2.1f);
        return 1f - (0.15f + 0.30f * hassasiyet) * (1f - nefes);
    }
}
