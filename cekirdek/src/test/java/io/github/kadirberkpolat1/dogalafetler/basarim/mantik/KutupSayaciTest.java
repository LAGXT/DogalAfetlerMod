package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KutupSayaciTest {
    private static final long GUN = KutupSayaci.GUN_TICK;

    @Test
    void aktifkenSaniyeSaniyeBirikir() {
        KutupSayaci s = new KutupSayaci(0, KutupSayaci.YOK);
        s.guncelle(1000, true);
        assertEquals(0, s.birikim(), "ilk gözlem yalnız başlangıçtır");
        s.guncelle(1020, true);
        s.guncelle(1040, true);
        assertEquals(40, s.birikim());
    }

    @Test
    void pasifkenDuraklarSifirlanmaz() {
        KutupSayaci s = new KutupSayaci(500, 1000);
        s.guncelle(1020, false);
        s.guncelle(5000, false);
        assertEquals(500, s.birikim(), "donma %100 değilken saymaz");
        s.guncelle(5020, true);
        assertEquals(520, s.birikim(), "pasifken geçen süre eklenmez");
    }

    @Test
    void olumSifirlarGirisCevrimDisiSureyiSaymaz() {
        KutupSayaci s = new KutupSayaci(GUN, 2000);
        s.olum();
        assertEquals(0, s.birikim());
        s.guncelle(2020, true);
        assertEquals(20, s.birikim(), "ölümden sonra yeniden birikir");
        s.girisYapti();
        s.guncelle(2020 + 5 * GUN, true);
        assertEquals(20, s.birikim(), "çevrim dışı geçen süre hayatta kalma sayılmaz");
    }

    @Test
    void uykuAtlamasiSayilirAmaSinirli() {
        KutupSayaci s = new KutupSayaci(0, 0);
        s.guncelle(12_000, true);
        assertEquals(12_000, s.birikim(), "uyku birikimi (K-C42) oyun gününü ilerletir");
        s.guncelle(12_000 + 10 * GUN, true);
        assertEquals(12_000 + KutupSayaci.EN_COK_ADIM, s.birikim(), "tek adımda en çok bir gün + pay");
        s.guncelle(0, true);
        assertEquals(12_000 + KutupSayaci.EN_COK_ADIM, s.birikim(), "saat geri giderse eklenmez");
    }

    @Test
    void ucGunEsigi() {
        KutupSayaci s = new KutupSayaci(3 * GUN - 20, 100);
        assertTrue(s.gun() < 3);
        s.guncelle(120, true);
        assertEquals(3.0, s.gun(), 1e-9);
    }
}
