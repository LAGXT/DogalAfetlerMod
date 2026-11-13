package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Vücut ile ortam arasındaki ısı alışverişi (madde 7b, N3, N4). Vücut sıcaklığı B, −100…+100 vücut birimidir (0 nötr).
 * Ortam (hissedilen) °C'si {@code E = (T − T_konfor) · birimPerDerece} ile vücut birimine çevrilir. Model (Newton soğuması + metabolik ısı):
 *
 * <pre>
 * B' = k · g(E − B) · w / (1 + I) · [(E − B) + U · (a · (1 + I) − 1)],  g(d) = 1 + |d| / hizlanmaOlcegi
 * </pre>
 *
 * {@code g}, fark büyüdükçe hızı doğrusal-üstü artıran kazançtır (denge noktasını değiştirmez): uç ortamda vücut
 * ~30–60 sn'de belirgin değişir, fark kapanırken yavaşlar.
 *
 * {@code k} eşitlenme hızı, {@code w} ıslaklık iletkenliği (S3-02: {@code suda ? suIletkenlik : 1 + (islakIletkenlik − 1)·W}), {@code I} toplam yalıtım, {@code U} konfor ortamında metabolik
 * ısının karşılığı, {@code a} etkinlik. Fark büyükse değişim hızlı, küçükse yavaştır; yalıtım hızı böler ve dengeyi sıcağa
 * iter (ılık ortamda çok yalıtımlı oyuncu aşırı ısınır). Yünün soğuma kesintisi (N3) yalnız vücut nötrün altındayken
 * (B ≤ 0) soğuma yönünde uygulanır: hız yünsüz yalıtımla hesaplanıp {@code 1 − 0,25·n} ile çarpılır. Nötrün üstündeki
 * vücut yünsüz yalıtımla tam hızda soğur (aşırı ısınmış oyuncu yünlü zırhla da dengeye iner); nötrü geçen adım
 * {@link #adim} içinde ikiye bölünür. Aşırı soğuk eşiğinin altında yalıtımın ısı tutma terimi kalkar, yün kesintisi
 * küçülür: yalıtım donmayı yalnız yavaşlatır.
 */
public final class IsiAlisverisi {
    public static final double OLCEK = 100.0;

    private IsiAlisverisi() {
    }

    /**
     * @param vucut     vücut sıcaklığı (vücut birimi)
     * @param ortam     hissedilen sıcaklık (°C, {@link Hissedilen})
     * @param islaklik  W (0–1, {@link Islaklik})
     * @param suda      suyun içinde mi (iletkenlik su değeri)
     */
    public record Girdi(double vucut, double ortam, Etkinlik etkinlik, double islaklik, boolean suda,
            Yalitim.Girdi yalitim) {
    }

    /**
     * @param esitlenmeHizi     k (1/sn): çıplak, kuru oyuncunun farkı kapatma oranı
     * @param konforSicakligi   çıplak, yürüyen oyuncunun nötr kaldığı ortam (°C)
     * @param birimPerDerece    1 °C ortam farkının vücut birimi karşılığı
     * @param metabolikIsi      U: konfor ortamında metabolik ısının vücut birimi karşılığı
     * @param asiriSogukEsigi   bu °C ve altında yalıtım yalnız yavaşlatır
     * @param hizlanmaOlcegi    {@code g(d) = 1 + |d|/ölçek}: vücut-ortam farkı (vücut birimi) bu kadar olduğunda hız 2 katına
     *                          çıkar (fark büyüdükçe hızlanır; küçük ölçek sert, büyük ölçek doğrusala yakın)
     */
    public record Ayarlar(double esitlenmeHizi, double konforSicakligi, double birimPerDerece, double metabolikIsi,
            double durmaEtkinligi, double yurumeEtkinligi, double kosmaEtkinligi, double islakIletkenlik,
            double suIletkenlik, double asiriSogukEsigi, double hizlanmaOlcegi) {
        public static final Ayarlar VARSAYILAN = new Ayarlar(0.02, 18, 2.0, 38, 0.8, 1.0, 1.4, 1.5, 3.0, -25, 100);

        /** Fark büyüdükçe artan hız kazancı: {@code 1 + |fark| / hizlanmaOlcegi}. */
        public double hizlanma(double fark) {
            return 1 + Math.abs(fark) / hizlanmaOlcegi;
        }

        /** Ortam °C'sinin vücut birimi karşılığı. */
        public double esdeger(double ortamC) {
            return (ortamC - konforSicakligi) * birimPerDerece;
        }

        public double etkinlik(Etkinlik e) {
            return switch (e) {
                case DURMA -> durmaEtkinligi;
                case YURUME -> yurumeEtkinligi;
                case KOSMA -> kosmaEtkinligi;
            };
        }

        /** Suda {@code suIletkenlik}; değilse kurudan (1) tam ıslaklığa ({@code islakIletkenlik}) doğrusal. */
        public double iletkenlik(double islaklik, boolean suda) {
            return suda ? suIletkenlik : 1 + (islakIletkenlik - 1) * Islaklik.sinirla(islaklik);
        }
    }

    /** Vücut sıcaklığının değişim hızı (vücut birimi/sn), termostat dahil. */
    public static double egilim(Girdi g, Ayarlar a, Yalitim.Ayarlar y) {
        double e = a.esdeger(g.ortam());
        double b = g.vucut();
        double etkinlik = a.etkinlik(g.etkinlik());
        double k = a.esitlenmeHizi() * a.iletkenlik(g.islaklik(), g.suda()) * a.hizlanma(e - b);
        double u = a.metabolikIsi();
        double kuru = Yalitim.kuruToplam(g.yalitim(), y);
        int yun = g.yalitim().yunluParca();
        double hiz;
        if (g.ortam() <= a.asiriSogukEsigi()) {
            hiz = k / (1 + kuru) * ((e - b) + u * (etkinlik - 1));
            if (hiz < 0 && b <= 0) {
                hiz *= Yalitim.yunSogumaCarpani(yun, true, y);
            }
        } else {
            double tam = Yalitim.toplam(g.yalitim(), y);
            double kuvvet = (e - b) + u * (etkinlik * (1 + tam) - 1);
            if (kuvvet >= 0) {
                hiz = k / (1 + tam) * kuvvet;
            } else {
                hiz = k / (1 + kuru) * ((e - b) + u * (etkinlik * (1 + kuru) - 1));
                if (b <= 0) {
                    hiz *= Yalitim.yunSogumaCarpani(yun, false, y);
                }
            }
        }
        return hiz + Yalitim.termostatCekimi(b, e, g.yalitim().termostatParcasi(), y);
    }

    /**
     * {@code saniye} sonraki vücut sıcaklığı (ölçeğe sıkıştırılır). Nötrün üstünden altına geçen adım iki parçaya bölünür:
     * 0'a kadar üst tarafın hızıyla, kalan süre 0'daki hızla (yün kesintisi orada başlar).
     */
    public static double adim(Girdi g, double saniye, Ayarlar a, Yalitim.Ayarlar y) {
        double b = g.vucut();
        double hiz = egilim(g, a, y);
        double yeni = b + hiz * saniye;
        if (b > 0 && yeni < 0) {
            double kalan = saniye - b / -hiz;
            Girdi notr = new Girdi(0, g.ortam(), g.etkinlik(), g.islaklik(), g.suda(), g.yalitim());
            yeni = egilim(notr, a, y) * kalan;
        }
        return sinirla(yeni);
    }

    public static double sinirla(double vucut) {
        return Math.max(-OLCEK, Math.min(OLCEK, vucut));
    }
}
