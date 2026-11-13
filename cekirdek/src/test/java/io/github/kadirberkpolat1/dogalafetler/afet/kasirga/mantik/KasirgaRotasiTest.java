package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Kasırganın rastgele yürüyüşü (AF2-20): tür dağılımı, 512 blok sınırı, oyuncu üstüne ara hedef yok, kayıttan devam, yüklülük. */
class KasirgaRotasiTest {
    private static final KasirgaRotasi.KonumKosulu HEPSI = (x, z) -> true;
    /** S5 hızı 4,4 blok/sn, tick başına. */
    private static final double ADIM = 4.4 / 20;

    @Test
    void turDagilimiYuzdeYetmisRastgeleOtuzOyuncu() {
        KasirgaRotasi rota = KasirgaRotasi.yeni(0, 0, 0, 20261108L);
        List<double[]> oyuncu = List.<double[]>of(new double[] {0, 0});
        int rastgele = 0;
        int oyuncuTuru = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            KasirgaRotasi.Secim s = rota.yeniAraHedef(0, 0, oyuncu, HEPSI);
            if (s.tur() == KasirgaRotasi.Tur.RASTGELE) {
                rastgele++;
            } else {
                oyuncuTuru++;
            }
            assertTrue(rota.kalanTick() >= KasirgaRotasi.ARALIK_EN_AZ && rota.kalanTick() <= KasirgaRotasi.ARALIK_EN_COK,
                    "seçim aralığı 200-400: " + rota.kalanTick());
        }
        assertEquals(0.70, (double) rastgele / n, 0.03, "RASTGELE payı");
        assertEquals(0.30, (double) oyuncuTuru / n, 0.03, "OYUNCU payı");
    }

    @Test
    void oyuncuYokkenHepRastgele() {
        KasirgaRotasi rota = KasirgaRotasi.yeni(0, 0, 0, 7L);
        for (int i = 0; i < 2000; i++) {
            assertEquals(KasirgaRotasi.Tur.RASTGELE, rota.yeniAraHedef(0, 0, List.of(), HEPSI).tur());
        }
    }

    /**
     * Tick tick yürüyüş: Kasirga.hareketEt'in eşi (dönüş hızı 1,5°/tick, 40 tick'te bir ±35° sapma, yeni ara hedefte yön hemen
     * ona, 512 kırpma, sınırda zorunlu seçim {@link KasirgaRotasi#zorunluSecimSerbest} ile).
     */
    private static final class Yuruyus {
        final KasirgaRotasi rota;
        final Random r;
        final List<KasirgaRotasi.Secim> secimler = new ArrayList<>();
        final List<Integer> secimTick = new ArrayList<>();
        final List<Integer> zorunluTick = new ArrayList<>();
        double x;
        double z;
        double yon;
        double hedefYon;
        double enUzak;
        int oyuncuUstu;
        int sinirTick;

        Yuruyus(long tohum) {
            r = new Random(tohum);
            yon = r.nextDouble() * 2 * Math.PI;
            hedefYon = yon;
            rota = KasirgaRotasi.yeni(0, 0, yon, tohum);
        }

        void sec(int t, List<double[]> oyuncular) {
            KasirgaRotasi.Secim s = rota.yeniAraHedef(x, z, oyuncular, HEPSI);
            secimler.add(s);
            secimTick.add(t);
            hedefYon = Yonelim.sapmaliYon(Yonelim.yon(x, z, s.x(), s.z()), r.nextDouble());
            for (double[] o : oyuncular) {
                if (Math.hypot(s.x() - o[0], s.z() - o[1]) < KasirgaRotasi.OYUNCUYA_EN_AZ) {
                    oyuncuUstu++;
                }
            }
        }

        void tick(int t, List<double[]> oyuncular) {
            if (rota.secimZamani(1, x, z)) {
                sec(t, oyuncular);
            } else if (t % Yonelim.SAPMA_ARALIGI_TICK == 0) {
                hedefYon = Yonelim.sapmaliYon(Yonelim.yon(x, z, rota.hedefX(), rota.hedefZ()), r.nextDouble());
            }
            yon = Yonelim.don(yon, hedefYon);
            double nx = x + Math.cos(yon) * ADIM;
            double nz = z + Math.sin(yon) * ADIM;
            double u = rota.baslangicaUzaklik(nx, nz);
            boolean sinirda = u > KasirgaRotasi.EN_COK_UZAKLIK;
            if (sinirda) {
                nx *= KasirgaRotasi.EN_COK_UZAKLIK / u;
                nz *= KasirgaRotasi.EN_COK_UZAKLIK / u;
            }
            x = nx;
            z = nz;
            enUzak = Math.max(enUzak, rota.baslangicaUzaklik(x, z));
            if (sinirda) {
                sinirTick++;
                if (rota.zorunluSecimSerbest()) {
                    zorunluTick.add(t);
                    sec(t, oyuncular);
                } else {
                    hedefYon = Yonelim.yon(x, z, rota.hedefX(), rota.hedefZ());
                }
            }
        }
    }

    /** Rastgele yürüyen oyuncular: her biri 40 tick'te bir yeni yön, 4,3 blok/sn. */
    private static void oyunculariYurut(List<double[]> oyuncular, Random r, int t) {
        for (double[] o : oyuncular) {
            if (t % 40 == 0) {
                o[2] = r.nextDouble() * 2 * Math.PI;
            }
            o[0] += Math.cos(o[2]) * 4.3 / 20;
            o[1] += Math.sin(o[2]) * 4.3 / 20;
        }
    }

    @Test
    void yirmiDakikaBoyunca512BloguAsmazOyuncuUstuneAraHedefYok() {
        int toplamSecim = 0;
        for (long tohum = 1; tohum <= 6; tohum++) {
            Yuruyus y = new Yuruyus(tohum);
            Random r = new Random(tohum * 31);
            List<double[]> oyuncular = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                oyuncular.add(new double[] {r.nextDouble() * 200 - 100, r.nextDouble() * 200 - 100, 0});
            }
            for (int t = 0; t < 20 * 60 * 20; t++) {
                oyunculariYurut(oyuncular, r, t);
                y.tick(t, oyuncular);
                assertTrue(y.rota.baslangicaUzaklik(y.x, y.z) <= KasirgaRotasi.EN_COK_UZAKLIK + 1e-9,
                        "kasırga başlangıca 512'den uzak: tohum " + tohum + " tick " + t);
            }
            for (KasirgaRotasi.Secim s : y.secimler) {
                assertTrue(s.baslangicaUzaklik() <= KasirgaRotasi.EN_COK_UZAKLIK + 1e-9, "ara hedef 512'den uzak: " + s);
            }
            assertEquals(0, y.oyuncuUstu, "oyuncuya 32 bloktan yakın ara hedef, tohum " + tohum);
            assertTrue(y.secimler.size() >= 20 * 60 * 20 / KasirgaRotasi.ARALIK_EN_COK, "seçim sayısı " + y.secimler.size());
            toplamSecim += y.secimler.size();
        }
        assertTrue(toplamSecim > 300, "toplam seçim " + toplamSecim);
    }

    /**
     * AF2-25 (inceleme Af3): başlangıçtan 512 blokta dışarı bakan kasırga tick başına ara hedef seçmez. İlk 200 tick'te zorunlu
     * seçim yok (rotanın ilki ve en çok bir varış seçimi), kasırga sınırdan ayrılır (yön eldeki ara hedefe döner); uzun
     * yürüyüşte iki seçim arası en az 40 tick ve her zorunlu seçim öncekinden en az {@link KasirgaRotasi#ZORUNLU_ARALIK} sonra.
     */
    @Test
    void sinirdaDisariBakanKasirgaTickBasinaSecmezVeIceriDoner() {
        for (long tohum = 1; tohum <= 6; tohum++) {
            Yuruyus y = new Yuruyus(tohum);
            y.x = KasirgaRotasi.EN_COK_UZAKLIK - 0.1;
            y.yon = 0;
            y.hedefYon = 0;
            List<double[]> yok = List.of();
            for (int t = 0; t < 200; t++) {
                y.tick(t, yok);
            }
            assertTrue(y.sinirTick > 0, "kasırga sınıra hiç değmedi, tohum " + tohum);
            assertTrue(y.zorunluTick.isEmpty() && y.secimler.size() <= 2,
                    "ilk 200 tick'te zorunlu seçim olmamalı, tohum " + tohum + ": seçim " + y.secimTick + " zorunlu " + y.zorunluTick);
            assertTrue(y.sinirTick < 200, "kasırga sınırda takılı kaldı: sınır tick'i " + y.sinirTick + ", tohum " + tohum);
            for (int t = 200; t < 20 * 60 * 10; t++) {
                y.tick(t, yok);
            }
            for (int i = 1; i < y.secimTick.size(); i++) {
                int fark = y.secimTick.get(i) - y.secimTick.get(i - 1);
                assertTrue(fark >= Yonelim.SAPMA_ARALIGI_TICK, "iki seçim arası " + fark + " tick, tohum " + tohum);
                if (y.zorunluTick.contains(y.secimTick.get(i))) {
                    assertTrue(fark >= KasirgaRotasi.ZORUNLU_ARALIK, "zorunlu seçim öncekinden " + fark + " tick sonra");
                }
            }
            assertTrue(y.enUzak <= KasirgaRotasi.EN_COK_UZAKLIK + 1e-9, "512 aşıldı: " + y.enUzak);
        }
    }

    @Test
    void tekOyuncuEtrafindaYuzlerceSecimdeOyuncuUstuSifir() {
        KasirgaRotasi rota = KasirgaRotasi.yeni(0, 0, 0, 99L);
        Random r = new Random(5);
        int yakin = 0;
        for (int i = 0; i < 5000; i++) {
            double[] o = {r.nextDouble() * 160 - 80, r.nextDouble() * 160 - 80};
            double x = r.nextDouble() * 200 - 100;
            double z = r.nextDouble() * 200 - 100;
            KasirgaRotasi.Secim s = rota.yeniAraHedef(x, z, List.of(o), HEPSI);
            if (Math.hypot(s.x() - o[0], s.z() - o[1]) < KasirgaRotasi.OYUNCUYA_EN_AZ) {
                yakin++;
            }
        }
        assertEquals(0, yakin);
    }

    @Test
    void durumKaydindanAyniAraHedeflerleSurer() {
        KasirgaRotasi rota = KasirgaRotasi.yeni(10, -20, 1.2, 424242L);
        List<double[]> oyuncular = List.of(new double[] {40, 40}, new double[] {-90, 10});
        for (int i = 0; i < 7; i++) {
            rota.yeniAraHedef(rota.hedefX(), rota.hedefZ(), oyuncular, HEPSI);
        }
        rota.secimZamani(37, rota.hedefX(), rota.hedefZ());
        KasirgaRotasi.Durum d = rota.yaz();
        KasirgaRotasi okunan = KasirgaRotasi.oku(d);
        assertEquals(d, okunan.yaz(), "durum gidiş-dönüş");
        for (int i = 0; i < 20; i++) {
            double x = rota.hedefX() + 3;
            double z = rota.hedefZ() - 5;
            KasirgaRotasi.Secim a = rota.yeniAraHedef(x, z, oyuncular, HEPSI);
            KasirgaRotasi.Secim b = okunan.yeniAraHedef(x, z, oyuncular, HEPSI);
            assertEquals(a, b, "seçim " + i);
            assertEquals(rota.kalanTick(), okunan.kalanTick());
        }
        assertEquals(rota.yaz(), okunan.yaz());
    }

    @Test
    void rastgeleAciVeUzaklikOyuncuUzakligiAralikta() {
        KasirgaRotasi rota = KasirgaRotasi.yeni(0, 0, 0.3, 1234L);
        Random r = new Random(77);
        int rastgele = 0;
        int oyuncu = 0;
        for (int i = 0; i < 10000; i++) {
            double x = r.nextDouble() * 300 - 150;
            double z = r.nextDouble() * 300 - 150;
            double[] o = {x + 200 * Math.cos(i), z + 200 * Math.sin(i)};
            double oncekiYon = rota.sonYon();
            KasirgaRotasi.Secim s = rota.yeniAraHedef(x, z, List.of(o), HEPSI);
            if (s.tur() == KasirgaRotasi.Tur.RASTGELE) {
                assertTrue(s.uzaklik() >= KasirgaRotasi.MESAFE_EN_AZ - 1e-9 && s.uzaklik() <= KasirgaRotasi.MESAFE_EN_COK + 1e-9,
                        "RASTGELE uzaklık " + s.uzaklik());
                if (!s.donus()) {
                    double aci = Math.atan2(s.z() - z, s.x() - x);
                    assertTrue(Math.abs(Yonelim.aciFarki(aci, oncekiYon)) <= KasirgaRotasi.SAPMA + 1e-9,
                            "RASTGELE açı önceki bacağın ±60° dışında: " + s);
                    rastgele++;
                }
            } else {
                double d = Math.hypot(s.x() - o[0], s.z() - o[1]);
                assertTrue(d >= KasirgaRotasi.OYUNCU_EN_AZ - 1e-9 && d <= KasirgaRotasi.OYUNCU_EN_COK + 1e-9,
                        "OYUNCU uzaklığı " + d);
                oyuncu++;
            }
        }
        assertTrue(rastgele > 5000 && oyuncu > 2000, "rastgele " + rastgele + ", oyuncu " + oyuncu);
    }

    @Test
    void donusBaslangicaYonelir() {
        KasirgaRotasi rota = KasirgaRotasi.yeni(0, 0, 0, 55L);
        int donus = 0;
        for (int i = 0; i < 2000; i++) {
            KasirgaRotasi.Secim s = rota.yeniAraHedef(500, 0, List.of(), HEPSI);
            assertTrue(s.baslangicaUzaklik() <= KasirgaRotasi.EN_COK_UZAKLIK + 1e-9);
            if (s.donus()) {
                donus++;
                double aci = Math.atan2(s.z(), s.x() - 500);
                assertTrue(Math.abs(Yonelim.aciFarki(aci, Math.PI)) <= KasirgaRotasi.SAPMA + 1e-9, "dönüş başlangıca doğru: " + s);
            }
        }
        assertTrue(donus > 0, "sınırda dönüş seçimi olmalı");
    }

    @Test
    void yukluOlmayanAdaySecilmez() {
        KasirgaRotasi.KonumKosulu doguYarisi = (x, z) -> x >= 0;
        KasirgaRotasi rota = KasirgaRotasi.yeni(0, 0, Math.PI, 31L);
        Random r = new Random(3);
        for (int i = 0; i < 5000; i++) {
            double x = r.nextDouble() * 40;
            double z = r.nextDouble() * 200 - 100;
            KasirgaRotasi.Secim s = rota.yeniAraHedef(x, z, List.of(new double[] {x + 150, z}), doguYarisi);
            assertTrue(s.x() >= 0, "yüklü olmayan aday seçildi: " + s);
        }
    }
}
