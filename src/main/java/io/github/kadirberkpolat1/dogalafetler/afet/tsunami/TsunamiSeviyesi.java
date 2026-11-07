package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.afet.ortak.mantik.BlokRolu;

/**
 * Seviye tablosu (madde 10.4, {@code docs/tasarim/tsunami.md} §6): dalga yüksekliği (blok), hız (blok/sn), karaya giriş (blok),
 * cephe genişliği (blok), çekilme mesafesi (blok), sürükleme ivmesi (blok/tick², tam batmış varlık), su hasarı (can/sn,
 * {@code Dalga.hasar}) ve yıkılan en büyük sertlik. Saf Java.
 */
public record TsunamiSeviyesi(int seviye, double yukseklik, double hizBlokSn, double karaGiris, double genislik,
        double cekilme, double ivme, double hasar, float sertlik) {
    private static final TsunamiSeviyesi[] TABLO = {
        new TsunamiSeviyesi(1, 4, 4, 30, 60, 10, 0.04, 0.5, 1.0f),
        new TsunamiSeviyesi(2, 7, 5, 60, 100, 15, 0.06, 1.0, 2.0f),
        new TsunamiSeviyesi(3, 10, 6, 100, 140, 20, 0.08, 1.5, 3.0f),
        new TsunamiSeviyesi(4, 14, 7, 150, 200, 30, 0.10, 2.0, 3.5f),
        new TsunamiSeviyesi(5, 20, 8, 220, 260, 40, 0.12, 3.0, 4.0f),
    };

    /** Sığlaşmanın açıkta başladığı yükseklik payı ({@code H·(1 - d/d0)^0,25} sıfıra inmesin: ufukta alçak, geniş çizgi). */
    static final double ACIK_PAY = 0.35;

    public static TsunamiSeviyesi al(int seviye) {
        return TABLO[Math.max(1, Math.min(TABLO.length, seviye)) - 1];
    }

    /** Dalga hızı (blok / ölçekli tick). */
    public double hizTick() {
        return hizBlokSn / 20.0;
    }

    /** Dalganın açıkta doğduğu uzaklık: kıyıya ~18 sn'de varır (tasarım notu zaman çizelgesi), 60–150 blok. */
    public double aciklik() {
        return Math.max(60, Math.min(150, hizBlokSn * 18));
    }

    /** Yüzeyin cephe arkasındaki uzunluğu (önde duvar, arkada kuyruk). */
    public double uzunluk() {
        return 24 + 3 * yukseklik;
    }

    /** S3+ ikinci, %60 yükseklikte dalga. */
    public boolean ikinciDalga() {
        return seviye >= 3;
    }

    /** Kırıktaş ve taş yapı blokları yalnız S4–5'te yıkılır. */
    public boolean tasYikar() {
        return seviye >= 4;
    }

    /** Envanterli kaplar (fıçı, sandık, fırın) S3+'ta içerikleri dökülerek yıkılır. */
    public boolean kapYikar() {
        return seviye >= 3;
    }

    /**
     * Ortak blok rolünün sertliğe bakmayan yıkım kararı: dekor ({@code #dogalafetler:afet_dekor}) her seviyede, kap
     * {@link #kapYikar} ile; {@link BlokRolu#DIGER} burada yıkılmaz, sınıf tablosu (sertlik, malzeme) karar verir.
     */
    public boolean rolYikar(BlokRolu rol) {
        return switch (rol) {
            case DEKOR -> true;
            case KAP -> kapYikar();
            case DIGER -> false;
        };
    }

    /** Tick başına yıkılan en çok blok (TPS kısmasından önce): S1 16 … S5 48. */
    public int yikimButcesi() {
        return 8 + 8 * seviye;
    }

    /** Tsunaminin doldurabileceği en çok mağara cebi. */
    public int magaraSayisi() {
        return 1 + seviye / 2;
    }

    /** Çekilmenin merkezdeki görsel derinliği (blok); ay sonrası çarpanla, 1–8. */
    public double cekilmeDerinligi(double ayCarpani) {
        return Math.max(1, Math.min(8, (1 + 0.8 * seviye) * ayCarpani));
    }

    /**
     * Sığlaşma (tasarım notu: {@code H·(1 - d/d0)^0,25}): {@code kalanOran} cephenin kıyıya kalan yolunun açıklığa oranı
     * (1 açıkta doğduğu an, 0 kıyıda). Yükseklik payı açıkta {@link #ACIK_PAY}, kıyıda 1.
     */
    public static double siglasma(double kalanOran) {
        double k = Math.max(0, Math.min(1, kalanOran));
        return Math.max(ACIK_PAY, Math.pow(1 - k, 0.25));
    }
}
