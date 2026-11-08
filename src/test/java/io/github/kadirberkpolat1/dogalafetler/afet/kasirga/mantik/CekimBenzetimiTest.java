package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Kasırga çekiminin kalibrasyonu (tur 3, AF3-02): vanilla canlı hareketinin tick sırasıyla saf benzetim. Her tick çekim
 * ivmesi (oyuncunun istemcisinde {@code PlayerTickEvent} START, aiStep'ten önce), yürüme/koşma girdisi (yerde 0,1/0,13, havada
 * 0,02/0,026), hız tavanı, hareket, yerçekimi 0,08 ve 0,98 dikey sürünme, yatay sürtünme (yerde 0,546, havada 0,91).
 * Koşu hızı 0,13 / (1 - 0,546) = 0,286 blok/tick = 5,7 blok/sn.
 */
class CekimBenzetimiTest {
    private static final double KOSU = 0.13 / (1 - Cekim.YER_SURTUNMESI) * 20;

    /** Benzetim sonucu: son uzaklık, çekirdeğe giriş tick'i (-1 yok), girişten sonra en büyük uzaklık, son yükseklik. */
    private record Sonuc(double d, int giris, double girisSonrasiEnUzak, double y) {
    }

    /**
     * @param kac      varlık merkezden dışa koşar (true) ya da girdisiz durur
     * @param kosar    koşu (true) ya da yürüyüş girdisi
     * @param tegetHiz başlangıçta siklonik teğet hız (blok/tick; havada başlar)
     * @param kayma    kasırganın doğuya yürüyüş hızı (blok/tick)
     */
    private static Sonuc benzet(int seviye, double x0, int tick, boolean kac, boolean kosar, double tegetHiz, double kayma) {
        KasirgaSeviyesi s = KasirgaSeviyesi.icin(seviye);
        double tavan = s.kaldirmaTavani(KasirgaSeviyesi.S5_KALDIRMA_TAVANI);
        double x = x0;
        double y = tegetHiz != 0 ? 3 : 0;
        double z = 0;
        double vx = 0;
        double vy = 0;
        double vz = -tegetHiz;
        boolean yerde = y <= 0;
        double merkez = 0;
        int giris = -1;
        double enUzak = 0;
        for (int t = 0; t < tick; t++) {
            merkez += kayma;
            double f = yerde ? Cekim.YER_SURTUNMESI : Cekim.HAVA_SURTUNMESI;
            double[] a = Cekim.ivme(x - merkez, z, y, vx, vz, s.tabanYaricapi(), s.cekimYaricapi(), s.guc(), 1, tavan, f);
            vx += a[0];
            vy += a[1];
            vz += a[2];
            if (kac) {
                double d = Math.hypot(x - merkez, z);
                double girdi = yerde ? (kosar ? 0.13 : 0.1) : (kosar ? 0.026 : 0.02);
                vx += (x - merkez) / d * girdi;
                vz += z / d * girdi;
            }
            double[] k = Cekim.kirp(new double[] {vx, vy, vz});
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
            vy = (vy - 0.08) * 0.98;
            if (yerde && vy < 0) {
                vy = 0;
            }
            f = yerde ? Cekim.YER_SURTUNMESI : Cekim.HAVA_SURTUNMESI;
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
        return new Sonuc(Math.hypot(x - merkez, z), giris, enUzak, y);
    }

    private static double iceHizBlokSn(int seviye, double d) {
        KasirgaSeviyesi s = KasirgaSeviyesi.icin(seviye);
        return Cekim.iceHiz(d, s.tabanYaricapi(), s.cekimYaricapi(), s.guc(), 1) * 20;
    }

    /** Şef ölçütü: S1 kenarda koşarak kaçılır, S3 ≈ koşu, S5 kenarda bile koşunun belirgin üstü ve çekirdek yakınında kaçış yok. */
    @Test
    void iceHizTablosuSeviyeyleHizlaArtar() {
        for (int l = 1; l <= 5; l++) {
            KasirgaSeviyesi s = KasirgaSeviyesi.icin(l);
            double kenar = iceHizBlokSn(l, 0.9 * s.cekimYaricapi());
            double orta = iceHizBlokSn(l, 0.5 * s.cekimYaricapi());
            double cekirdek = iceHizBlokSn(l, s.tabanYaricapi());
            assertTrue(kenar < orta && orta < cekirdek, "S" + l + " merkeze doğru güçlenir");
            if (l > 1) {
                assertTrue(kenar > iceHizBlokSn(l - 1, 0.9 * KasirgaSeviyesi.icin(l - 1).cekimYaricapi()), "S" + l);
            }
        }
        assertTrue(iceHizBlokSn(1, 0.9 * 15) < 0.5 * KOSU, "S1 kenarda yürüyerek bile kaçılır");
        assertTrue(iceHizBlokSn(1, 5) < 4.3, "S1 çekirdek sınırında yürüyüşten yavaş");
        double s3Orta = iceHizBlokSn(3, 18);
        assertTrue(Math.abs(s3Orta - KOSU) < 0.1 * KOSU, "S3 orta yarıçapta ≈ koşu: " + s3Orta);
        assertTrue(iceHizBlokSn(5, 0.9 * 72) > 1.4 * KOSU, "S5 kenarda koşunun belirgin üstü");
        assertTrue(iceHizBlokSn(5, 24) > 2.3 * KOSU, "S5 çekirdek yakınında kaçış yok");
        assertEquals(8.0 * Math.sqrt(1 / 0.9), iceHizBlokSn(5, 0.9 * 72), 1e-9);
    }

    @Test
    void s5VarligiKenardanUcBucukSaniyedeCekirdegeCeker() {
        KasirgaSeviyesi s5 = KasirgaSeviyesi.icin(5);
        Sonuc girdisiz = benzet(5, 0.85 * s5.cekimYaricapi(), 400, false, true, 0, 0);
        assertTrue(girdisiz.giris() >= 0 && girdisiz.giris() <= 80, "çekirdeğe giriş tick'i " + girdisiz.giris());
        assertTrue(girdisiz.y() > 0.5 * s5.kaldirmaTavani(KasirgaSeviyesi.S5_KALDIRMA_TAVANI), "çekirdekte kaldırılır: " + girdisiz.y());
        Sonuc kosan = benzet(5, 0.85 * s5.cekimYaricapi(), 400, true, true, 0, 0);
        assertTrue(kosan.giris() >= 0 && kosan.d() < s5.tabanYaricapi(), "S5 kenardan koşarak kaçılamaz: " + kosan.d());
    }

    @Test
    void s1KenardanKosarakKacilirS3OrtadanZorKacilirS4Kacilamaz() {
        KasirgaSeviyesi s1 = KasirgaSeviyesi.icin(1);
        assertTrue(benzet(1, 0.85 * s1.cekimYaricapi(), 200, true, true, 0, 0).d() > s1.cekimYaricapi(), "S1 koşu");
        assertTrue(benzet(1, 0.85 * s1.cekimYaricapi(), 300, true, false, 0, 0).d() > s1.cekimYaricapi(), "S1 yürüyüş");
        KasirgaSeviyesi s3 = KasirgaSeviyesi.icin(3);
        Sonuc s3Kosu = benzet(3, 0.5 * s3.cekimYaricapi(), 200, true, true, 0, 0);
        assertTrue(s3Kosu.d() > 0.5 * s3.cekimYaricapi() && s3Kosu.d() < s3.cekimYaricapi() + 20,
                "S3 orta yarıçaptan koşu ancak yavaşça kaçar: " + s3Kosu.d());
        assertTrue(benzet(3, 0.5 * s3.cekimYaricapi(), 200, true, false, 0, 0).giris() >= 0, "S3 ortadan yürüyen çekilir");
        KasirgaSeviyesi s4 = KasirgaSeviyesi.icin(4);
        assertTrue(benzet(4, 0.85 * s4.cekimYaricapi(), 600, true, true, 0, 0).d() < s4.tabanYaricapi(), "S4 kenardan koşu");
    }

    /** Hızlı dönerek çekirdeğe giren ya da çekirdekte başlayan varlık hiçbir seviyede çekirdekten dışarı savrulmaz. */
    @Test
    void hicbirSeviyedeCekirdektenDisariSavurmaYok() {
        for (int l = 1; l <= 5; l++) {
            KasirgaSeviyesi s = KasirgaSeviyesi.icin(l);
            for (double tegetHiz : new double[] {0.3, 0.8, 1.4}) {
                for (double pay : new double[] {0.2, 0.5, 0.9}) {
                    Sonuc r = benzet(l, pay * s.tabanYaricapi(), 400, false, true, tegetHiz, 0);
                    assertTrue(r.girisSonrasiEnUzak() < s.tabanYaricapi(),
                            "S" + l + " teğet " + tegetHiz + " başlangıç " + pay + ": en uzak " + r.girisSonrasiEnUzak());
                }
            }
            Sonuc kenardan = benzet(l, 0.85 * s.cekimYaricapi(), 600, false, true, 0, 0);
            assertTrue(kenardan.giris() >= 0 && kenardan.girisSonrasiEnUzak() < s.tabanYaricapi(), "S" + l + " kenardan");
        }
    }

    /** Yürüyen kasırga kaldırdığı varlığı çekirdeğinde taşır (dışarıda yüksekten bırakmaz). */
    @Test
    void yuruyenKasirgaVarligiCekirdegindeTasir() {
        for (int l = 1; l <= 5; l++) {
            KasirgaSeviyesi s = KasirgaSeviyesi.icin(l);
            Sonuc r = benzet(l, 0.85 * s.cekimYaricapi(), 800, false, true, 0, s.hiz() / 20);
            assertTrue(r.giris() >= 0 && r.girisSonrasiEnUzak() < s.tabanYaricapi() && r.d() < s.tabanYaricapi(),
                    "S" + l + ": son " + r.d() + ", en uzak " + r.girisSonrasiEnUzak());
        }
    }
}
