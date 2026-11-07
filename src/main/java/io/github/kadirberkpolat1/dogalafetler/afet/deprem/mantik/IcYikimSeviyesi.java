package io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik;

/**
 * Depremin iç yıkım eşikleri (bölüm C "tahta evler gibi sağlam olmayan bloklar yıkılıp yere düşsün", madde 10; AF-D2): S1
 * içeriye dokunmaz; S2+ dekor (fener, meşale, saksı, tabela, bayrak, mum, halı, çerçeve, tablo) eşya olarak düşer; S3+
 * yapraklar dökülür; S4+ altı hava olan zayıf tavan blokları düşen blok olur; S5 aynı etkiler daha büyük bütçe ve olasılıkla.
 * Saf Java'dır.
 *
 * @param olcek tick bütçesi çarpanı ({@code deprem.icYikimButcesi} S4 ölçeğindedir)
 * @param dekorOlasiligi taranan dekor bloğunun / asılı varlığın düşme olasılığı (0: kapalı)
 * @param yaprakOlasiligi taranan yaprağın dökülme olasılığı (0: kapalı)
 * @param tavanOlasiligi altı hava olan zayıf bloğun düşme olasılığı (0: kapalı)
 * @param tavanSertligi etiket dışı bir bloğun "zayıf" sayıldığı en yüksek sertlik (vanilla {@code destroyTime})
 */
public record IcYikimSeviyesi(int seviye, double olcek, double dekorOlasiligi, double yaprakOlasiligi,
        double tavanOlasiligi, float tavanSertligi) {
    /** Dökülen yaprağın yağma tablosundan eşya bırakma olasılığı (fidan, çubuk, elma; eşya yağmuru olmasın). */
    public static final double YAPRAK_ESYA_ORANI = 0.1;
    /** Alanın rastgele sütununda yüzeyden aşağı taranan en çok blok. */
    public static final int SUTUN_DERINLIGI = 24;

    private static final double[] OLCEK = {0, 0.5, 0.75, 1.0, 1.75};
    private static final double[] DEKOR = {0, 0.3, 0.5, 0.7, 0.9};
    private static final double[] YAPRAK = {0, 0, 0.25, 0.4, 0.6};
    private static final double[] TAVAN = {0, 0, 0, 0.5, 0.8};
    private static final float[] SERTLIK = {0, 0, 0, 0.8f, 1.0f};

    public static IcYikimSeviyesi seviye(int seviye) {
        int i = Math.max(1, Math.min(5, seviye)) - 1;
        return new IcYikimSeviyesi(i + 1, OLCEK[i], DEKOR[i], YAPRAK[i], TAVAN[i], SERTLIK[i]);
    }

    /** Bu seviyede iç yıkım yok (S1). */
    public boolean bos() {
        return olcek <= 0;
    }

    public boolean dekor() {
        return dekorOlasiligi > 0;
    }

    public boolean yaprak() {
        return yaprakOlasiligi > 0;
    }

    public boolean tavan() {
        return tavanOlasiligi > 0;
    }

    /** Bu tick taranacak beklenen sütun sayısı; {@code kisma}: TPS kademesi × adım (ana yıkımla aynı çarpan). */
    public double beklenenSutun(int ayar, double kisma) {
        return Math.max(0, ayar) * olcek * Math.max(0, kisma);
    }

    /** Tick başına en çok düşen tavan bloğu (S4+; genel düşen blok tavanı ayrıca geçerli). */
    public int tavanSiniri(int ayar) {
        return tavan() ? Math.max(1, (int) Math.ceil(Math.max(0, ayar) * olcek / 2)) : 0;
    }

    /** Etiket dışı blok sertliğiyle zayıf mı (kırılmaz blokların negatif sertliği hiçbir zaman zayıf değil). */
    public boolean zayifSertlik(float sertlik) {
        return tavan() && sertlik >= 0 && sertlik <= tavanSertligi;
    }
}
