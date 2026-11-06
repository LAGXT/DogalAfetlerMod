package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TpsKismasiTest {
    private static final long MS = 1_000_000L;
    private static final TpsKismasi.Esikler E = TpsKismasi.Esikler.VARSAYILAN;

    @Test
    void yalnizDoluGirdilerOrtalamayaGirer() {
        long[] tickler = new long[100];
        for (int i = 0; i < 10; i++) {
            tickler[i] = 40 * MS;
        }
        assertEquals(40.0, TpsKismasi.ortalamaMs(tickler), 1e-9, "boş (0) girdiler ortalamayı düşürmemeli");
        assertEquals(20.0, TpsKismasi.tps(40.0), 1e-9, "40 ms/tick 20 TPS'e tavanlanır");
        assertEquals(0.0, TpsKismasi.ortalamaMs(new long[100]), 1e-12);
        assertEquals(20.0, TpsKismasi.tps(0.0), 1e-12, "ölçüm yokken TPS 20 sayılır");
    }

    @Test
    void tpsTekOrtalamadanHesaplanir() {
        long[] tickler = {60 * MS, 60 * MS, 0, 0};
        double ms = TpsKismasi.ortalamaMs(tickler);
        assertEquals(60.0, ms, 1e-9);
        assertEquals(1000.0 / 60.0, TpsKismasi.tps(ms), 1e-9);
    }

    @Test
    void varsayilanEsikler() {
        assertEquals(18.0, E.esik());
        assertEquals(15.0, E.kritikEsik());
        assertEquals(19.0, E.toparlanma());
        assertEquals(16.5, E.kritikToparlanma());
    }

    @Test
    void dususEsiklerdeDonusToparlanmaEsiklerinde() {
        assertEquals(1.0, TpsKismasi.carpan(18.0, 1.0, E), "eşiğin kendisi kısmaz");
        assertEquals(0.5, TpsKismasi.carpan(17.99, 1.0, E));
        assertEquals(0.25, TpsKismasi.carpan(14.99, 1.0, E));
        assertEquals(0.5, TpsKismasi.carpan(18.5, 0.5, E), "yarı bütçeden 19'un altında dönüş yok");
        assertEquals(1.0, TpsKismasi.carpan(19.0, 0.5, E));
        assertEquals(0.25, TpsKismasi.carpan(14.9, 0.5, E));
        assertEquals(0.25, TpsKismasi.carpan(16.4, 0.25, E), "çeyrekten 16,5'in altında dönüş yok");
        assertEquals(0.5, TpsKismasi.carpan(16.5, 0.25, E));
        assertEquals(0.5, TpsKismasi.carpan(18.9, 0.25, E));
        assertEquals(1.0, TpsKismasi.carpan(19.0, 0.25, E));
        assertEquals(0.5, TpsKismasi.carpan(TpsKismasi.tps(1000.0 / 16.7), 1.0, E), "16,7 TPS yarı bütçe");
        assertEquals(0.25, TpsKismasi.carpan(TpsKismasi.tps(80.0), 1.0, E), "80 ms/tick çeyrek bütçe");
    }

    @Test
    void esikCevresindekiSalinimdaKademeGidipGelmez() {
        double carpan = 1.0;
        int degisim = 0;
        for (int i = 0; i < 200; i++) {
            double tps = i % 2 == 0 ? 17.8 : 18.6;
            double yeni = TpsKismasi.carpan(tps, carpan, E);
            if (yeni != carpan) {
                degisim++;
            }
            carpan = yeni;
        }
        assertEquals(1, degisim, "18 çevresinde salınan TPS yalnız bir kez kısmalı");
        assertEquals(0.5, carpan);
    }

    @Test
    void esiklerTutarliHaleGetirilir() {
        TpsKismasi.Esikler ters = new TpsKismasi.Esikler(15, 18, 10, 30);
        assertEquals(15.0, ters.esik());
        assertEquals(15.0, ters.kritikEsik(), "kritik eşik eşiği aşamaz");
        assertEquals(15.0, ters.toparlanma(), "toparlanma eşikten düşük olamaz");
        assertEquals(15.0, ters.kritikToparlanma(), "kritik toparlanma [kritik, toparlanma] aralığında");
    }
}
