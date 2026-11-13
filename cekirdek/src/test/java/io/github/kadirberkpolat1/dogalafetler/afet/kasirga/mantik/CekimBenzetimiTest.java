package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Kasırga çekiminin kalibrasyonu: vanilla canlı hareketinin tick sırasıyla saf benzetim. Her tick çekim ivmesi (oyuncunun
 * istemcisinde {@code PlayerTickEvent} START, aiStep'ten önce), yürüme/koşma girdisi (yerde 0,1/0,13, havada 0,02/0,026), hız
 * tavanı, hareket, yerçekimi 0,08 ve 0,98 dikey sürünme, yatay sürtünme (yerde 0,546, havada 0,91). Koşu hızı
 * 0,13 / (1 - 0,546) = 0,286 blok/tick = 5,7 blok/sn.
 */
class CekimBenzetimiTest {
    /** Benzetim sonucu: son uzaklık, çekirdeğe giriş tick'i (-1 yok), girişten sonra en büyük uzaklık, son ve en büyük yükseklik. */
    private record Sonuc(double d, int giris, double girisSonrasiEnUzak, double y, double enYuksek) {
    }

    /**
     * @param kac      varlık merkezden dışa koşar (true) ya da girdisiz durur
     * @param kosar    koşu (true) ya da yürüyüş girdisi
     * @param tegetHiz başlangıçta siklonik teğet hız (blok/tick; havada başlar)
     * @param kayma    kasırganın doğuya yürüyüş hızı (blok/tick)
     * @param firlatma fırlatma rampası (0-1)
     */
    private static Sonuc benzet(int seviye, double x0, double y0, int tick, boolean kac, boolean kosar, double tegetHiz,
            double kayma, double firlatma) {
        KasirgaSeviyesi s = KasirgaSeviyesi.icin(seviye);
        double tavan = s.kaldirmaTavani(KasirgaSeviyesi.S5_KALDIRMA_TAVANI);
        double x = x0;
        double y = y0;
        double z = 0;
        double vx = 0;
        double vy = 0;
        double vz = -tegetHiz;
        boolean yerde = y <= 0;
        double merkez = 0;
        int giris = -1;
        double enUzak = 0;
        double enYuksek = y;
        for (int t = 0; t < tick; t++) {
            merkez += kayma;
            double[] onceki = {vx, vy, vz};
            double[] a = Cekim.ivme(x - merkez, z, y, vx, vy, vz, s, 1, tavan, firlatma);
            vx += a[0];
            vy += a[1];
            vz += a[2];
            if (kac) {
                double d = Math.hypot(x - merkez, z);
                double girdi = yerde ? (kosar ? 0.13 : 0.1) : (kosar ? 0.026 : 0.02);
                vx += (x - merkez) / d * girdi;
                vz += z / d * girdi;
            }
            double[] k = Cekim.kirp(new double[] {vx, vy, vz}, onceki, firlatma);
            vx = k[0];
            vy = k[1];
            vz = k[2];
            x += vx;
            y += vy;
            z += vz;
            if (y <= 0) {
                y = 0;
                vy = 0;
                yerde = true;
            } else {
                yerde = false;
            }
            enYuksek = Math.max(enYuksek, y);
            vy = (vy - 0.08) * 0.98;
            if (yerde && vy < 0) {
                vy = 0;
            }
            double f = yerde ? Cekim.YER_SURTUNMESI : Cekim.HAVA_SURTUNMESI;
            vx *= f;
            vz *= f;
            double d = Math.hypot(x - merkez, z);
            if (giris < 0 && d < s.tabanYaricapi()) {
                giris = t;
            }
            if (giris >= 0) {
                enUzak = Math.max(enUzak, d);
            }
        }
        return new Sonuc(Math.hypot(x - merkez, z), giris, enUzak, y, enYuksek);
    }

    private static Sonuc benzet(int seviye, double x0, int tick, boolean kac, boolean kosar, double tegetHiz, double kayma) {
        return benzet(seviye, x0, tegetHiz != 0 ? 3 : 0, tick, kac, kosar, tegetHiz, kayma, 0);
    }

    /** Çekim yarıçapı ve hedef içe hız seviyeyle büyür; kenarda bile S5, koşu hızının belirgin üstünde çeker. */
    @Test
    void iceHizVeMenzilSeviyeyleArtar() {
        double kosu = 0.13 / (1 - Cekim.YER_SURTUNMESI);
        double onceki = 0;
        for (int l = 1; l <= 5; l++) {
            KasirgaSeviyesi s = KasirgaSeviyesi.icin(l);
            double v0 = Cekim.TABAN_HIZ + Cekim.HIZ_ARTISI * s.guc();
            double kenar = v0 * (Cekim.KENAR_ORANI + (1 - Cekim.KENAR_ORANI) * 0.1);
            assertTrue(kenar > onceki, "S" + l + " kenar hızı artar");
            onceki = kenar;
        }
        KasirgaSeviyesi s5 = KasirgaSeviyesi.icin(5);
        assertTrue((Cekim.TABAN_HIZ + Cekim.HIZ_ARTISI * s5.guc()) * 0.55 > 1.2 * kosu, "S5 kenarda koşunun belirgin üstü");
        assertTrue(s5.cekimYaricapi() > 100, "S5 çok uzaktan çeker: " + s5.cekimYaricapi());
        assertTrue(s5.cekimYaricapi() >= 6 * KasirgaSeviyesi.icin(1).cekimYaricapi() - 1e-9, "S5 menzili S1'in 6 katı");
    }

    @Test
    void s5VarligiKenardanCekirdegeCekerYukseltir() {
        KasirgaSeviyesi s5 = KasirgaSeviyesi.icin(5);
        double tavan = s5.kaldirmaTavani(KasirgaSeviyesi.S5_KALDIRMA_TAVANI);
        Sonuc girdisiz = benzet(5, 0.85 * s5.cekimYaricapi(), 400, false, true, 0, 0);
        assertTrue(girdisiz.giris() >= 0 && girdisiz.giris() <= 200, "çekirdeğe giriş tick'i " + girdisiz.giris());
        assertTrue(girdisiz.y() > 0.8 * tavan, "çekirdekte tavana yakın kaldırılır: " + girdisiz.y());
        assertTrue(girdisiz.d() < s5.tabanYaricapi(), "yörünge çekirdek içinde: " + girdisiz.d());
        Sonuc kosan = benzet(5, 0.85 * s5.cekimYaricapi(), 600, true, true, 0, 0);
        assertTrue(kosan.giris() >= 0 && kosan.d() < s5.tabanYaricapi(), "S5 kenardan koşarak kaçılamaz: " + kosan.d());
    }

    @Test
    void s1S2KenardanKacilirS3OrtadanKacilamazS4Kacilamaz() {
        KasirgaSeviyesi s1 = KasirgaSeviyesi.icin(1);
        assertTrue(benzet(1, 0.85 * s1.cekimYaricapi(), 400, true, true, 0, 0).d() > s1.cekimYaricapi(), "S1 koşu");
        assertTrue(benzet(1, 0.85 * s1.cekimYaricapi(), 600, true, false, 0, 0).d() > s1.cekimYaricapi(), "S1 yürüyüş");
        KasirgaSeviyesi s2 = KasirgaSeviyesi.icin(2);
        assertTrue(benzet(2, 0.85 * s2.cekimYaricapi(), 600, true, true, 0, 0).d() > s2.cekimYaricapi(), "S2 kenardan koşu");
        KasirgaSeviyesi s3 = KasirgaSeviyesi.icin(3);
        assertTrue(benzet(3, 0.5 * s3.cekimYaricapi(), 400, true, true, 0, 0).giris() >= 0, "S3 ortadan koşan çekilir");
        assertTrue(benzet(3, 0.5 * s3.cekimYaricapi(), 400, true, false, 0, 0).giris() >= 0, "S3 ortadan yürüyen çekilir");
        KasirgaSeviyesi s4 = KasirgaSeviyesi.icin(4);
        Sonuc s4Kosu = benzet(4, 0.85 * s4.cekimYaricapi(), 800, true, true, 0, 0);
        assertTrue(s4Kosu.giris() >= 0 && s4Kosu.d() < s4.tabanYaricapi(), "S4 kenardan koşu: " + s4Kosu.d());
    }

    /** Hızlı dönerek çekirdeğe giren ya da çekirdekte başlayan varlık hiçbir seviyede çekirdekten dışarı savırılmaz. */
    @Test
    void hicbirSeviyedeCekirdektenDisariSavurmaYok() {
        for (int l = 1; l <= 5; l++) {
            KasirgaSeviyesi s = KasirgaSeviyesi.icin(l);
            for (double tegetHiz : new double[] {0.3, 0.8, 1.4}) {
                for (double pay : new double[] {0.2, 0.5, 0.9}) {
                    Sonuc r = benzet(l, pay * s.tabanYaricapi(), 400, false, true, tegetHiz, 0);
                    assertTrue(r.girisSonrasiEnUzak() < 1.05 * s.tabanYaricapi(),
                            "S" + l + " teğet " + tegetHiz + " başlangıç " + pay + ": en uzak " + r.girisSonrasiEnUzak());
                }
            }
            Sonuc kenardan = benzet(l, 0.85 * s.cekimYaricapi(), 600, false, true, 0, 0);
            assertTrue(kenardan.giris() >= 0 && kenardan.girisSonrasiEnUzak() < 1.05 * s.tabanYaricapi(), "S" + l + " kenardan");
        }
    }

    /** Yürüyen kasırga kaldırdığı varlığı çekirdeğinde taşır (dışarıda yüksekten bırakmaz). */
    @Test
    void yuruyenKasirgaVarligiCekirdegindeTasir() {
        for (int l = 1; l <= 5; l++) {
            KasirgaSeviyesi s = KasirgaSeviyesi.icin(l);
            Sonuc r = benzet(l, 0.85 * s.cekimYaricapi(), 800, false, true, 0, s.hiz() / 20);
            assertTrue(r.giris() >= 0 && r.girisSonrasiEnUzak() < 1.05 * s.tabanYaricapi() && r.d() < s.tabanYaricapi(),
                    "S" + l + ": son " + r.d() + ", en uzak " + r.girisSonrasiEnUzak());
        }
    }
}
