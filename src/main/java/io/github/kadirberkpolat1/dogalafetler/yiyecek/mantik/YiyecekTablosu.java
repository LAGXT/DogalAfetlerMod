package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import java.util.List;

/**
 * Madde 14'ün dokuz yiyeceği: eşya adı (kimlikler sabit: başarım ve README), sıcaklık tanımı, yemek değeri ve kap.
 * Yön, kademe ve süre şartnameden; besin ve doygunluk N17 (Tavuk Çorbası "yüksek doyuruculuk": en yüksek).
 */
public final class YiyecekTablosu {
    /** Yenince geri verilen boş kap. */
    public enum Kap {
        SISE,
        KASE,
        YOK
    }

    /**
     * @param besin      doyurduğu açlık (yarım but = 1)
     * @param doygunluk  vanilla doygunluk çarpanı ({@code saturationMod}; doygunluk = besin · çarpan · 2)
     */
    public record Kayit(String ad, YiyecekTanimi tanim, int besin, float doygunluk, Kap kap) {
    }

    public static final String AD_ALANI = "dogalafetler";

    public static final List<Kayit> KAYITLAR = List.of(
            kayit("sicak_salep", Yon.ISINMA, Buyukluk.ORTA, 90, 4, 0.6f, Kap.SISE),
            kayit("sicak_cikolata", Yon.ISINMA, Buyukluk.ORTA, 90, 5, 0.6f, Kap.SISE),
            kayit("sicak_tavuk_corbasi", Yon.ISINMA, Buyukluk.BUYUK, 150, 10, 0.9f, Kap.KASE),
            new Kayit("atesli_guvec", new YiyecekTanimi(AD_ALANI + ":atesli_guvec", Yon.ISINMA, Buyukluk.COK_BUYUK, 180,
                    true), 8, 0.8f, Kap.KASE),
            kayit("isitici_cay", Yon.ISINMA, Buyukluk.KUCUK, 180, 2, 0.3f, Kap.SISE),
            kayit("ayran", Yon.SERINLIK, Buyukluk.ORTA, 90, 4, 0.5f, Kap.SISE),
            kayit("buzlu_karpuz", Yon.SERINLIK, Buyukluk.KUCUK, 60, 3, 0.4f, Kap.YOK),
            kayit("meyve_serbeti", Yon.SERINLIK, Buyukluk.BUYUK, 120, 4, 0.4f, Kap.SISE),
            kayit("kutup_baligi_corbasi", Yon.ISINMA, Buyukluk.BUYUK, 150, 9, 0.9f, Kap.KASE));

    private YiyecekTablosu() {
    }

    private static Kayit kayit(String ad, Yon yon, Buyukluk buyukluk, int saniye, int besin, float doygunluk, Kap kap) {
        return new Kayit(ad, new YiyecekTanimi(AD_ALANI + ":" + ad, yon, buyukluk, saniye, false), besin, doygunluk, kap);
    }

    public static Kayit al(String ad) {
        for (Kayit k : KAYITLAR) {
            if (k.ad().equals(ad)) {
                return k;
            }
        }
        throw new IllegalArgumentException("tabloda yok: " + ad);
    }
}
