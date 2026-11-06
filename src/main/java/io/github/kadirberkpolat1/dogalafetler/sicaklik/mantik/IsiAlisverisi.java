package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Vücut ile ortam arasındaki ısı alışverişi (madde 7b, N3, N4). Vücut sıcaklığı B, −100…+100 vücut birimidir (0 nötr).
 * Ortam °C'si {@code E = (T − T_konfor) · birimPerDerece} ile vücut birimine çevrilir. Model (Newton soğuması + metabolik ısı):
 *
 * <pre>
 * B' = k · w / (1 + I) · [(E − B) + U · (a · (1 + I) − 1)]
 * </pre>
 *
 * {@code k} eşitlenme hızı, {@code w} ıslaklık iletkenliği, {@code I} toplam yalıtım, {@code U} konfor ortamında metabolik
 * ısının karşılığı, {@code a} etkinlik. Fark büyükse değişim hızlı, küçükse yavaştır; yalıtım hızı böler ve dengeyi sıcağa
 * iter (ılık ortamda çok yalıtımlı oyuncu aşırı ısınır). Soğuma yönünde yün kendi oranıyla ayrıca uygulanır (N3): hız
 * yünsüz yalıtımla hesaplanıp {@code 1 − 0,25·n} ile çarpılır. Aşırı soğuk eşiğinin altında yalıtımın ısı tutma terimi kalkar,
 * yün kesintisi küçülür: yalıtım donmayı yalnız yavaşlatır.
 */
public final class IsiAlisverisi {
    public static final double OLCEK = 100.0;

    private IsiAlisverisi() {
    }

    /**
     * @param vucut     vücut sıcaklığı (vücut birimi)
     * @param ortam     ortam sıcaklığı (°C)
     */
    public record Girdi(double vucut, double ortam, Etkinlik etkinlik, Islaklik islaklik, Yalitim.Girdi yalitim) {
    }

    /**
     * @param esitlenmeHizi     k (1/sn): çıplak, kuru oyuncunun farkı kapatma oranı
     * @param konforSicakligi   çıplak, yürüyen oyuncunun nötr kaldığı ortam (°C)
     * @param birimPerDerece    1 °C ortam farkının vücut birimi karşılığı
     * @param metabolikIsi      U: konfor ortamında metabolik ısının vücut birimi karşılığı
     * @param asiriSogukEsigi   bu °C ve altında yalıtım yalnız yavaşlatır
     */
    public record Ayarlar(double esitlenmeHizi, double konforSicakligi, double birimPerDerece, double metabolikIsi,
            double durmaEtkinligi, double yurumeEtkinligi, double kosmaEtkinligi, double islakIletkenlik,
            double suIletkenlik, double asiriSogukEsigi) {
        public static final Ayarlar VARSAYILAN = new Ayarlar(0.006, 18, 2.0, 38, 0.8, 1.0, 1.4, 1.5, 3.0, -25);

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

        public double iletkenlik(Islaklik i) {
            return switch (i) {
                case KURU -> 1.0;
                case ISLAK -> islakIletkenlik;
                case SUDA -> suIletkenlik;
            };
        }
    }

    /** Vücut sıcaklığının değişim hızı (vücut birimi/sn), termostat dahil. */
    public static double egilim(Girdi g, Ayarlar a, Yalitim.Ayarlar y) {
        double e = a.esdeger(g.ortam());
        double b = g.vucut();
        double etkinlik = a.etkinlik(g.etkinlik());
        double k = a.esitlenmeHizi() * a.iletkenlik(g.islaklik());
        double u = a.metabolikIsi();
        double kuru = Yalitim.kuruToplam(g.yalitim(), y);
        int yun = g.yalitim().yunluParca();
        double hiz;
        if (g.ortam() <= a.asiriSogukEsigi()) {
            hiz = k / (1 + kuru) * ((e - b) + u * (etkinlik - 1));
            if (hiz < 0) {
                hiz *= Yalitim.yunSogumaCarpani(yun, true, y);
            }
        } else {
            double tam = Yalitim.toplam(g.yalitim(), y);
            double kuvvet = (e - b) + u * (etkinlik * (1 + tam) - 1);
            if (kuvvet >= 0) {
                hiz = k / (1 + tam) * kuvvet;
            } else {
                hiz = k / (1 + kuru) * ((e - b) + u * (etkinlik * (1 + kuru) - 1))
                        * Yalitim.yunSogumaCarpani(yun, false, y);
            }
        }
        return hiz + Yalitim.termostatCekimi(b, e, g.yalitim().termostatParcasi(), y);
    }

    /** {@code saniye} sonraki vücut sıcaklığı (ölçeğe sıkıştırılır). */
    public static double adim(Girdi g, double saniye, Ayarlar a, Yalitim.Ayarlar y) {
        return sinirla(g.vucut() + egilim(g, a, y) * saniye);
    }

    public static double sinirla(double vucut) {
        return Math.max(-OLCEK, Math.min(OLCEK, vucut));
    }
}
