package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import java.util.List;

/**
 * Sıcaklık yiyecekleri: eşya adı (kimlikler sabit: başarım ve README), sıcaklık tanımı, yemek değeri ve kap. İlk dokuzu madde
 * 14'ündür (yön, kademe ve süre şartnameden; besin ve doygunluk N17, Tavuk Çorbası "yüksek doyuruculuk": en yüksek); ardından
 * sıcaklık-3'ün (S3-06) altı serinletici ve altı ısıtıcı yiyeceği gelir. Sıra kayıt ve sekme sırasıdır: yeniler sona eklenir.
 * Yeni kademeler malzemenin sıcaklığına göre: buz/sıkıştırılmış buz en güçlü, kar ve soğuk meyve hafif; koyu çorbalar
 * pişmiş tabaktan, tabak sıcak içecekten güçlü. Besin vanilla ölçeğinde, Tavuk Çorbası'nı geçmez.
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
            kayit("kutup_baligi_corbasi", Yon.ISINMA, Buyukluk.BUYUK, 150, 9, 0.9f, Kap.KASE),
            kayit("maras_dondurmasi", Yon.SERINLIK, Buyukluk.BUYUK, 120, 5, 0.4f, Kap.KASE),
            kayit("soguk_elma_suyu", Yon.SERINLIK, Buyukluk.KUCUK, 90, 3, 0.4f, Kap.SISE),
            kayit("kar_helvasi", Yon.SERINLIK, Buyukluk.ORTA, 60, 4, 0.4f, Kap.KASE),
            kayit("soguk_komposto", Yon.SERINLIK, Buyukluk.ORTA, 120, 3, 0.4f, Kap.SISE),
            kayit("parlak_sorbe", Yon.SERINLIK, Buyukluk.COK_BUYUK, 150, 3, 0.3f, Kap.KASE),
            kayit("buzlu_kakao", Yon.SERINLIK, Buyukluk.ORTA, 90, 4, 0.5f, Kap.SISE),
            kayit("tarhana_corbasi", Yon.ISINMA, Buyukluk.BUYUK, 150, 7, 0.7f, Kap.KASE),
            kayit("kuzu_haslama", Yon.ISINMA, Buyukluk.ORTA, 150, 8, 0.8f, Kap.KASE),
            kayit("kabak_corbasi", Yon.ISINMA, Buyukluk.ORTA, 120, 6, 0.6f, Kap.KASE),
            kayit("sucuklu_yumurta", Yon.ISINMA, Buyukluk.ORTA, 90, 8, 0.7f, Kap.KASE),
            kayit("firinda_elma", Yon.ISINMA, Buyukluk.KUCUK, 90, 5, 0.5f, Kap.YOK),
            kayit("elma_cayi", Yon.ISINMA, Buyukluk.KUCUK, 120, 2, 0.3f, Kap.SISE));

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
