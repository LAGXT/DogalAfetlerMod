package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SanalSuDunyasiTest {
    private static DenizSeviyesiAlani duzKiyi() {
        DenizSeviyesiAlani alan = new DenizSeviyesiAlani(4, 96, 1);
        alan.iste(0, 0);
        alan.isle((cx, cz, zemin, su, taban) -> {
            for (int i = 0; i < 256; i++) {
                boolean deniz = (i & 15) < 8;
                zemin[i] = deniz ? 63 : 64;
                su[i] = deniz;
                taban[i] = deniz ? 58 : 64;
            }
            return true;
        }, 10);
        return alan;
    }

    @Test
    void denizArtisiZamanlaDogrusalYukselir() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        dunya.denizAyarla(4, 100, 200);
        assertEquals(0, dunya.artis(50), 1e-9);
        assertEquals(2, dunya.artis(200), 1e-9);
        assertEquals(4, dunya.artis(1000), 1e-9);
    }

    @Test
    void dalgaVeDenizAraliklariBirlesir() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        dunya.denizAyarla(4, 0, 1);
        dunya.dalgaEkle(new TsunamiDalgasi(-40, 4, 1, 0, 0, 1, 8, 80, 30, 63, 200));
        SuAraliklari a = new SuAraliklari();
        dunya.araliklar(2, 4, 50, a);
        assertEquals(1, a.adet());
        assertEquals(63, a.alt(0));
        assertTrue(a.ust(0) > 67, "dalga yuzeyi yukselmis denizin ustunde");
    }

    @Test
    void magaraCebindekiVarlikBatiklikOraniVerir() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        MagaraCebi cep = MagaraDolumu.doldur((x, y, z) -> x >= 0 && x < 5 && z >= 0 && z < 5 && y >= 10 && y < 14
                ? MagaraDolumu.Tur.BOS : MagaraDolumu.Tur.KATI, 2, 10, 2, 1000, 10_000);
        dunya.magaraEkle(cep, new MagaraZamani(0, cep.minY(), cep.ustY(), 1, 1000, 1));
        assertEquals(1.0, dunya.batiklik(2.5, 2.5, 10, 11.8, 500), 1e-9);
        assertEquals(0.0, dunya.batiklik(2.5, 2.5, 14, 15.8, 500), 1e-9);
        assertEquals(14, dunya.ustu(2.5, 12, 2.5, 500), 1e-9);
    }

    @Test
    void temizleninceHicSuKalmaz() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        dunya.denizAyarla(4, 0, 1);
        dunya.temizle();
        SuAraliklari a = new SuAraliklari();
        dunya.araliklar(2, 4, 50, a);
        assertEquals(0, a.adet());
        assertTrue(!dunya.aktif());
    }
}
