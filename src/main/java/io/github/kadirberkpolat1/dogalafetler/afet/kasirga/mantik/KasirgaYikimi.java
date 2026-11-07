package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/**
 * Kasırganın blok yıkım kuralları (K-04, K-05). Tick başına sütun denemesi, sütunun çekilme
 * olasılığı (seviye × merkeze uzaklık), sütun tarama derinliği, seviyenin en üst bloğun altında alabildiği blok sınıfları ve
 * parçalanma/eşya/devrilme bütçesi. Blok sınıfı etiketlerden ({@code Kasirga}) gelir; sertlik ve dışlamalar
 * ({@code Kasirga#kaldirilabilir}) ayrıca uygulanır.
 */
public final class KasirgaYikimi {
    /** Uçan blok tavanı doluyken parçalanan bloğun eşya düşürme olasılığı (K-04). */
    public static final double ESKI_ESYA_PAYI = 1.0 / 16;
    /** Yıkım (sütun seçimi) yarıçapı / çekim yarıçapı: K-04'ün %60'ı, taban enkaz eteğinin yarıçapına eşit. */
    public static final double YIKIM_ORANI = 0.6;

    private static final int[] DERINLIK = {1, 1, 3, 4, 6};
    private static final double[] CEKIRDEK_OLASILIGI = {0.6, 0.75, 0.9, 1, 1};
    private static final double[] KENAR_OLASILIGI = {0.15, 0.2, 0.25, 0.3, 0.35};
    private static final double[] DENEME_KATI = {1, 1, 1.5, 2, 2.5};
    private static final int[] KUTUK_TICK = {0, 0, 0, 8, 12};

    /** Sütundaki blok sınıfı (etiketlerden; N12). */
    public enum Sinif {
        /** Anında kırılan bitki ve küçük nesne (çiçek, ot, fidan, meşale). */
        BITKI,
        YAPRAK,
        KUTUK,
        /** Tahta, ahşap merdiven/basamak/çit/çit kapısı/kapak. */
        AHSAP_YAPI,
        /** Toprak, kum, çakıl, taş, kar, kil (yalnız S4-5, yalnız en üstte). */
        ZEMIN,
        DIGER
    }

    /**
     * Bir tick'in yıkım bütçesi.
     *
     * @param deneme   rastgele sütun denemesi
     * @param parca    uçan blok tavanı doluyken en çok parçalanan ya da yatırılan blok (K-04 tavanı)
     * @param kutuk    en çok devrilen (yatırılan ya da uçan) gövde kütüğü
     * @param esyaPayi parçalanan bloğun eşya düşürme olasılığı
     */
    public record Butce(int deneme, int parca, int kutuk, double esyaPayi) {
        public static final Butce YOK = new Butce(0, 0, 0, 0);
    }

    private KasirgaYikimi() {
    }

    private static int sira(int seviye) {
        if (seviye < 1 || seviye > DERINLIK.length) {
            throw new IllegalArgumentException("Kasırga seviyesi 1-5 olmalı: " + seviye);
        }
        return seviye - 1;
    }

    /** Sütun seçim yarıçapı (blok). */
    public static double yikimYaricapi(double cekimYaricapi) {
        return cekimYaricapi * YIKIM_ORANI;
    }

    /** Sütunun yukarıdan aşağı taranan blok sayısı (en üst blok dahil; aradaki hava da sayılır). */
    public static int derinlik(int seviye) {
        return DERINLIK[sira(seviye)];
    }

    /**
     * Seçilen sütunun bu denemede çekilme olasılığı: huni çekirdeğinde ({@code mesafe ≤ cekirdekYaricapi}) seviyenin tam
     * değeri, yıkım yarıçapının kenarına doğru doğrusal olarak kenar değerine iner, dışında 0.
     */
    public static double cekimOlasiligi(int seviye, double mesafe, double cekirdekYaricapi, double yikimYaricapi) {
        int i = sira(seviye);
        if (mesafe > yikimYaricapi) {
            return 0;
        }
        if (mesafe <= cekirdekYaricapi || yikimYaricapi <= cekirdekYaricapi) {
            return CEKIRDEK_OLASILIGI[i];
        }
        double t = (mesafe - cekirdekYaricapi) / (yikimYaricapi - cekirdekYaricapi);
        return CEKIRDEK_OLASILIGI[i] + (KENAR_OLASILIGI[i] - CEKIRDEK_OLASILIGI[i]) * t;
    }

    /**
     * Sütunun en üstünden {@code derinlikIndeksi} blok aşağıdaki (0 = en üst) bu sınıftaki blok bu seviyede alınabilir mi.
     * En üst blokta yalnız sertlik ve zemin kuralı vardır (K-04); altında yapraklar ve bitkiler S3+, gövde kütüğü S4+, ahşap
     * yapı S5, zemin ve diğerleri hiç.
     */
    public static boolean alinir(int seviye, Sinif sinif, int derinlikIndeksi) {
        if (derinlikIndeksi < 0 || derinlikIndeksi >= derinlik(seviye)) {
            return false;
        }
        if (derinlikIndeksi == 0) {
            return true;
        }
        return switch (sinif) {
            case BITKI, YAPRAK -> seviye >= 3;
            case KUTUK -> seviye >= 4;
            case AHSAP_YAPI -> seviye >= 5;
            case ZEMIN, DIGER -> false;
        };
    }

    /** Dikey kütük gövdenin tamamı birden devrilir (S4-5); S2-3 açıkta kalan en üst kütüğü tek blok olarak alır. */
    public static boolean devirir(int seviye) {
        return KUTUK_TICK[sira(seviye)] > 0;
    }

    /**
     * Tick bütçesi. {@code blokKaldirmaTick} seviye tablosu × {@code blokCarpani}, {@code pay} evre payı, {@code kisma}
     * TPS kısması. K-04'ün denemesi {@code taban = blok × pay × kisma}; sütun denemesi bunun seviyeyle artan katı (okuma
     * ucuz). Parça tavanı ve eşya payı K-04 ile aynı ({@code max(1, taban/4)}, 1/16): tavan doluyken tick başına değişen
     * blok ve eşya K-04'ü aşmaz. Seviyeyle artan parça payı (S5 8) dedicated S5'te ortalama tick'i 4,3 ms'den 7,4 ms'ye
     * çıkardı (AF-23 rev.).
     */
    public static Butce butce(int seviye, int blokKaldirmaTick, double pay, double kisma) {
        int i = sira(seviye);
        if (pay <= 0 || blokKaldirmaTick <= 0 || kisma <= 0) {
            return Butce.YOK;
        }
        int taban = Math.max(1, (int) Math.round(blokKaldirmaTick * pay * kisma));
        int deneme = Math.max(1, (int) Math.round(taban * DENEME_KATI[i]));
        int kutuk = KUTUK_TICK[i] == 0 ? 0 : Math.max(1, (int) Math.round(KUTUK_TICK[i] * pay * kisma));
        return new Butce(deneme, eskiParcaTavani(taban), kutuk, ESKI_ESYA_PAYI);
    }

    /** K-04'ün parça tavanı: denemenin dörtte biri, en az 1. */
    public static int eskiParcaTavani(int taban) {
        return Math.max(1, taban / 4);
    }

    /**
     * Devrilen gövdenin yattığı yön: kasırga merkezine göre {@code (dx, dz)}'deki sütunda siklonik teğet rüzgâr (yukarıdan
     * saat yönünün tersi; x doğu, z güney), baskın eksene yuvarlanmış birim adım {@code {sx, sz}}. Merkezde doğu.
     */
    public static int[] devrilmeYonu(double dx, double dz) {
        double tx = dz;
        double tz = -dx;
        if (tx == 0 && tz == 0) {
            return new int[] {1, 0};
        }
        return Math.abs(tx) >= Math.abs(tz) ? new int[] {tx > 0 ? 1 : -1, 0} : new int[] {0, tz > 0 ? 1 : -1};
    }
}
