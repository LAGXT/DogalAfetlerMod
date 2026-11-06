package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Madde 7b/7e, N3, N4: ısı alışverişi, yün, tüy, aşırı soğuk istisnası ve aşırı ısınma (GOREV §6 #2). */
class IsiAlisverisiTest {
    private static final IsiAlisverisi.Ayarlar A = IsiAlisverisi.Ayarlar.VARSAYILAN;
    private static final Yalitim.Ayarlar Y = Yalitim.Ayarlar.VARSAYILAN;
    private static final Yalitim.Girdi CIPLAK = new Yalitim.Girdi(0, 0, 0, 0);
    /** "Normal soğuk": karlı ova gecesi, aşırı soğuk eşiğinin üstünde. */
    private static final double NORMAL_SOGUK = -10.0;
    /** Süpernova sonrası dondurucu soğuk. */
    private static final double DONDURUCU = -45.0;

    private static double egilim(double vucut, double ortam, Yalitim.Girdi yalitim) {
        return IsiAlisverisi.egilim(new IsiAlisverisi.Girdi(vucut, ortam, Etkinlik.YURUME, Islaklik.KURU, yalitim), A, Y);
    }

    /** Sabit ortamda {@code saniye} boyunca 1 Hz adım. */
    private static double benzet(double vucut, double ortam, Etkinlik etkinlik, Islaklik islaklik,
            Yalitim.Girdi yalitim, int saniye) {
        double b = vucut;
        for (int i = 0; i < saniye; i++) {
            b = IsiAlisverisi.adim(new IsiAlisverisi.Girdi(b, ortam, etkinlik, islaklik, yalitim), 1.0, A, Y);
        }
        return b;
    }

    @Test
    void farkBuyukseDegisimHizliKucukseYavas() {
        double kucukFark = Math.abs(egilim(0, 8, CIPLAK));
        double buyukFark = Math.abs(egilim(0, -30, CIPLAK));
        assertTrue(buyukFark > 2 * kucukFark, kucukFark + " / " + buyukFark);
        // Aynı ortamda vücut ortama yaklaştıkça değişim yavaşlar (anında eşitlenmez).
        double basta = Math.abs(egilim(0, -30, CIPLAK));
        double yaklasinca = Math.abs(egilim(-50, -30, CIPLAK));
        assertTrue(yaklasinca < basta / 2, basta + " / " + yaklasinca);
        double birSaniye = benzet(0, -30, Etkinlik.YURUME, Islaklik.KURU, CIPLAK, 1);
        assertTrue(birSaniye < 0 && birSaniye > -2, "anında eşitlenmemeli: " + birSaniye);
    }

    @Test
    void hareketIsitirDurmakVeIslaklikSogutur() {
        IsiAlisverisi.Girdi yurume = new IsiAlisverisi.Girdi(0, 5, Etkinlik.YURUME, Islaklik.KURU, CIPLAK);
        double y = IsiAlisverisi.egilim(yurume, A, Y);
        double k = IsiAlisverisi.egilim(new IsiAlisverisi.Girdi(0, 5, Etkinlik.KOSMA, Islaklik.KURU, CIPLAK), A, Y);
        double d = IsiAlisverisi.egilim(new IsiAlisverisi.Girdi(0, 5, Etkinlik.DURMA, Islaklik.KURU, CIPLAK), A, Y);
        double islak = IsiAlisverisi.egilim(new IsiAlisverisi.Girdi(0, 5, Etkinlik.YURUME, Islaklik.ISLAK, CIPLAK), A, Y);
        double suda = IsiAlisverisi.egilim(new IsiAlisverisi.Girdi(0, 5, Etkinlik.YURUME, Islaklik.SUDA, CIPLAK), A, Y);
        assertTrue(k > y, "koşmak ısıtır");
        assertTrue(d < y, "durmak soğutur");
        assertTrue(islak < y, "ıslaklık soğutur");
        assertTrue(suda < islak, "suda daha hızlı");
    }

    @Test
    void konforluOrtamdaCiplakOyuncuNotrKalir() {
        double b = benzet(0, A.konforSicakligi(), Etkinlik.YURUME, Islaklik.KURU, CIPLAK, 3600);
        assertEquals(Kademe.NOTR, Esikler.VARSAYILAN.kademe(b), "vücut: " + b);
    }

    @Test
    void yunHerParcadaSogumaHiziniCeyrekAzaltir() {
        double referans = egilim(0, NORMAL_SOGUK, new Yalitim.Girdi(4, 0, 0, 0));
        assertTrue(referans < 0);
        for (int yun = 1; yun <= 4; yun++) {
            double h = egilim(0, NORMAL_SOGUK, new Yalitim.Girdi(4, yun, 0, 0));
            assertEquals(1.0 - 0.25 * yun, h / referans, 1e-9, yun + " yünlü parça");
        }
    }

    @Test
    void dortYunluParcaylaNormalSogukteDonmaYok() {
        Yalitim.Girdi dortYun = new Yalitim.Girdi(4, 4, 0, 0);
        for (Etkinlik e : Etkinlik.values()) {
            double b = benzet(0, NORMAL_SOGUK, e, Islaklik.KURU, dortYun, 6 * 3600);
            assertTrue(b >= 0, e + ": " + b);
        }
        // Yünsüz aynı soğukta donmaya doğru gider.
        double yunsuz = benzet(0, NORMAL_SOGUK, Etkinlik.DURMA, Islaklik.KURU, new Yalitim.Girdi(4, 0, 0, 0), 3600);
        assertTrue(Esikler.VARSAYILAN.kademe(yunsuz).ordinal() < Kademe.NOTR.ordinal(), "yünsüz: " + yunsuz);
    }

    @Test
    void tuyYalitimiSogumayiYavaslatirVeDonmaSuresiniUzatir() {
        double onceki = Double.NEGATIVE_INFINITY;
        for (int tuy : new int[] {0, 8, 20, 40}) {
            double h = egilim(0, NORMAL_SOGUK, new Yalitim.Girdi(4, 0, tuy, 0));
            assertTrue(h > onceki, tuy + " tüy: " + h);
            onceki = h;
        }
        int ciplakSure = donmaSuresi(-35, new Yalitim.Girdi(4, 0, 0, 0));
        int tuyluSure = donmaSuresi(-35, new Yalitim.Girdi(4, 0, 40, 0));
        assertTrue(tuyluSure > 2 * ciplakSure, ciplakSure + " / " + tuyluSure);
    }

    @Test
    void asiriSogukteYunVeTuyYalnizYavaslatir() {
        Yalitim.Girdi tam = new Yalitim.Girdi(4, 4, 4 * 30, 0);
        assertTrue(DONDURUCU <= A.asiriSogukEsigi());
        int ciplak = donmaSuresi(DONDURUCU, CIPLAK);
        int korunakli = donmaSuresi(DONDURUCU, tam);
        assertTrue(ciplak > 0 && ciplak < 15 * 60, "çıplak " + ciplak);
        assertTrue(korunakli > 0, "yalıtım sınırsız koruma vermemeli");
        assertTrue(korunakli > 10 * ciplak, "çok yavaş ilerlemeli: " + ciplak + " / " + korunakli);
    }

    @Test
    void cokYalitimlaSicaktaAsiriIsinirSoguktaIyiGelir() {
        Yalitim.Girdi kalin = new Yalitim.Girdi(4, 4, 4 * 30, 0);
        double ilik = benzet(0, 22, Etkinlik.YURUME, Islaklik.KURU, kalin, 3600);
        assertEquals(Kademe.YANIYOR, Esikler.VARSAYILAN.kademe(ilik), "ılık ortamda: " + ilik);
        double ciplakIlik = benzet(0, 22, Etkinlik.YURUME, Islaklik.KURU, CIPLAK, 3600);
        assertEquals(Kademe.NOTR, Esikler.VARSAYILAN.kademe(ciplakIlik), "çıplak ılık: " + ciplakIlik);
        Yalitim.Girdi orta = new Yalitim.Girdi(4, 0, 4 * 8, 0);
        double soguk = benzet(0, NORMAL_SOGUK, Etkinlik.YURUME, Islaklik.KURU, orta, 3600);
        assertEquals(Kademe.NOTR, Esikler.VARSAYILAN.kademe(soguk), "soğukta tüylü: " + soguk);
    }

    @Test
    void vucutOlcekSinirlariniAsmaz() {
        assertEquals(-100, benzet(0, -200, Etkinlik.DURMA, Islaklik.SUDA, CIPLAK, 36_000), 1e-9);
        assertEquals(100, benzet(0, 200, Etkinlik.KOSMA, Islaklik.KURU, CIPLAK, 36_000), 1e-9);
    }

    /** Yürüyen kuru oyuncunun bu ortamdaki ısınma dengesi: {@code E + U·I} (S-02, etkinlik 1). */
    private static double denge(double ortam, Yalitim.Girdi yalitim) {
        return A.esdeger(ortam) + A.metabolikIsi() * Yalitim.toplam(yalitim, Y);
    }

    @Test
    void dortYunluAsiriIsinmisVucutSerinOrtamdaDengeyeIner() {
        Yalitim.Girdi dortYun = new Yalitim.Girdi(4, 4, 0, 0);
        for (double ortam : new double[] {18, 8}) {
            double b = benzet(90, ortam, Etkinlik.YURUME, Islaklik.KURU, dortYun, 600);
            assertTrue(Esikler.VARSAYILAN.kademe(b) != Kademe.YANIYOR, ortam + " °C: " + b);
            assertEquals(denge(ortam, dortYun), b, 1.5, ortam + " °C dengesi");
        }
        assertTrue(egilim(90, 18, dortYun) < -0.2, "sıcaktan nötre dönüş yünle kesilmemeli");
    }

    @Test
    void ucYunluAsiriIsinmisVucutDaYunsuzHizlaDoner() {
        Yalitim.Girdi ucYun = new Yalitim.Girdi(4, 3, 0, 0);
        Yalitim.Girdi yunsuz = new Yalitim.Girdi(4, 0, 0, 0);
        assertEquals(egilim(90, 18, yunsuz), egilim(90, 18, ucYun), 1e-12, "nötrün üstünde yün hızı kesmez");
        double b = benzet(90, 18, Etkinlik.YURUME, Islaklik.KURU, ucYun, 600);
        assertEquals(denge(18, ucYun), b, 1.5);
    }

    @Test
    void yunKesintisiYalnizNotrunAltinda() {
        Yalitim.Girdi dortYun = new Yalitim.Girdi(4, 4, 0, 0);
        for (Etkinlik e : new Etkinlik[] {Etkinlik.DURMA, Etkinlik.YURUME}) {
            assertEquals(0, benzet(0, NORMAL_SOGUK, e, Islaklik.KURU, dortYun, 3600), 1e-9, "N3: " + e);
        }
        // Sıcak vücut −10 °C'de nötre iner, sıfırı geçen adım bölünür ve nötrün altında 4 yün soğumayı keser.
        IsiAlisverisi.Girdi g = new IsiAlisverisi.Girdi(5, NORMAL_SOGUK, Etkinlik.YURUME, Islaklik.KURU, dortYun);
        assertEquals(0, IsiAlisverisi.adim(g, 120, A, Y), 1e-9, "büyük adım sıfırda durmalı");
        double ucYun = benzet(20, NORMAL_SOGUK, Etkinlik.YURUME, Islaklik.KURU, new Yalitim.Girdi(4, 3, 0, 0), 600);
        double yunsuz = benzet(20, NORMAL_SOGUK, Etkinlik.YURUME, Islaklik.KURU, new Yalitim.Girdi(4, 0, 0, 0), 600);
        assertTrue(ucYun < 0 && ucYun > yunsuz, "nötrün altında 3 yün yavaşlatır: " + ucYun + " / " + yunsuz);
    }

    /** Nötrden donma hasarı eşiğine kaç saniyede inildiği (en çok 48 saat; inilmezse -1). */
    private static int donmaSuresi(double ortam, Yalitim.Girdi yalitim) {
        double b = 0;
        for (int s = 1; s <= 48 * 3600; s++) {
            b = IsiAlisverisi.adim(new IsiAlisverisi.Girdi(b, ortam, Etkinlik.YURUME, Islaklik.KURU, yalitim), 1.0, A, Y);
            if (b <= Esikler.VARSAYILAN.donmaHasari()) {
                return s;
            }
        }
        return -1;
    }
}
