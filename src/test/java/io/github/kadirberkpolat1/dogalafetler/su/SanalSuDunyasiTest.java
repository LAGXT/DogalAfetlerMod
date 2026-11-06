package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SanalSuDunyasiTest {
    private static DenizAlani duzKiyi() {
        DenizAlani alan = new DenizAlani(4, 1, 63);
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
    void dalgaVeDenizAraliklariBirlesir() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        dunya.denizArtisiAyarla(4);
        dunya.dalgaEkle(1, new Dalga(-40, 4, 1, 0, 0, 1, 8, 80, 30, 63, 200));
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
        dunya.magaraEkle(1, cep, new MagaraZamani(0, cep.minY(), cep.ustY(), 1, 1000, 1));
        assertEquals(1.0, dunya.batiklik(2.5, 2.5, 10, 11.8, 500), 1e-9);
        assertEquals(0.0, dunya.batiklik(2.5, 2.5, 14, 15.8, 500), 1e-9);
        assertEquals(14, dunya.ustu(2.5, 12, 2.5, 500), 1e-9);
    }

    @Test
    void temizleDalgaMagaraVeCekilmeyiSilerDenizAyriAyarlanir() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        dunya.denizArtisiAyarla(4);
        dunya.dalgaEkle(1, new Dalga(-40, 4, 1, 0, 0, 1, 8, 80, 30, 63, 200));
        dunya.cekilmeAyarla(2, new Cekilme(4, 4, 6, 2, 0, 10));
        dunya.temizle();
        assertTrue(dunya.dalgalar().isEmpty() && dunya.cekilmeler().isEmpty() && dunya.magaralar().isEmpty());
        assertTrue(dunya.aktif(), "deniz artışı dönemin alanıdır, temizle silmez");
        dunya.denizArtisiAyarla(0);
        SuAraliklari a = new SuAraliklari();
        dunya.araliklar(2, 4, 50, a);
        assertEquals(0, a.adet());
        assertTrue(!dunya.aktif());
    }

    @Test
    void dalgaItmesiYonundeVeTepeninUstundeSifir() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        Dalga d = new Dalga(-40, 4, 1, 0, 0, 1, 8, 80, 30, 63, 200);
        dunya.dalgaEkle(7, d);
        double[] it = new double[2];
        double yuzey = d.yuzey(2.5, 4.5, 50);
        assertTrue(dunya.itme(2.5, 4.5, 63, 64.8, 50, it) != null, "dalganın içindeki varlık itilir");
        assertTrue(it[0] > 0 && Math.abs(it[1]) < 1e-12, "itme dalga yönünde (+x)");
        assertTrue(dunya.itme(2.5, 4.5, yuzey + 0.1, yuzey + 1.9, 50, it) == null, "tepenin üstündeki varlık itilmez");
        dunya.dalgaKaldir(7);
        assertTrue(dunya.itme(2.5, 4.5, 63, 64.8, 50, it) == null, "biten dalga itmez");
        assertEquals(0, dunya.dalgalar().size());
    }

    @Test
    void donmusMagaraSeviyesiDegismez() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        MagaraCebi cep = MagaraDolumu.doldur((x, y, z) -> x >= 0 && x < 5 && z >= 0 && z < 5 && y >= 10 && y < 14
                ? MagaraDolumu.Tur.BOS : MagaraDolumu.Tur.KATI, 2, 10, 2, 1000, 10_000);
        dunya.magaraEkle(3, cep, new MagaraZamani(0, cep.minY(), cep.ustY(), 1, 10, 0.01));
        dunya.magaraDondur(3, 100);
        double donuk = dunya.ustu(2.5, 10.5, 2.5, 100);
        assertEquals(donuk, dunya.ustu(2.5, 10.5, 2.5, 300), 1e-9, "durmuş mağara kurumaz");
        dunya.magaraCoz(3, 300);
        assertEquals(donuk, dunya.ustu(2.5, 10.5, 2.5, 300), 1e-9, "çözülünce kaldığı yerden");
        assertTrue(dunya.ustu(2.5, 10.5, 2.5, 400) < donuk, "sonra kurumaya devam eder");
    }

    @Test
    void kalicilikEtiketiDalgaCekilmeVeMagaraParametreleriniTasir() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        Dalga d = new Dalga(-40, 4, 1, 0, 120, 0.5, 8, 80, 30, 63, 200, 0.07, 1.5);
        Cekilme c = new Cekilme(4, 4, 6, 2, 100, 10);
        dunya.dalgaEkle(3, d);
        dunya.cekilmeAyarla(5, c);
        net.minecraft.nbt.CompoundTag etiket = new net.minecraft.nbt.CompoundTag();
        SuKalicilik.dalgaYaz(etiket, 3, d);
        assertEquals(d, SuKalicilik.dalgaOku(etiket));
        net.minecraft.nbt.CompoundTag ce = new net.minecraft.nbt.CompoundTag();
        SuKalicilik.cekilmeYaz(ce, c);
        assertEquals(c, SuKalicilik.cekilmeOku(ce));
        MagaraZamani z = new MagaraZamani(50, 10, 14, 0.1, 600, 0.002);
        net.minecraft.nbt.CompoundTag me = new net.minecraft.nbt.CompoundTag();
        SuKalicilik.zamanYaz(me, z, 77.5);
        MagaraZamani geri = SuKalicilik.zamanOku(me, 10, 14);
        assertEquals(z.seviye(80), geri.seviye(80), 1e-9);
        assertEquals(z.bitis(), geri.bitis(), 1e-9);
        assertEquals(77.5, SuKalicilik.donmaOku(me), 1e-9);
    }

    @Test
    void alanBilmeyinceDalgaTabaniCanliZemindenGelir() {
        // Doğrulama bulgusu (P1): alan sütunu henüz okumamışken dalga aralığı yalnız taban'dan başlıyordu; tabanın altındaki
        // varlık (ör. şablon kenarından düşen domuz) kuru kalıyordu. Canlı okuyucu dünyanın zeminini verir.
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        dunya.dalgaEkle(1, new Dalga(-80, 4, 1, 0, 0, 1, 8, 80, 30, 63, 200));
        SuAraliklari a = new SuAraliklari();
        dunya.araliklar(-40, 4, 50, a);
        assertEquals(1, a.adet());
        assertEquals(63, a.alt(0), "canlı zemin yokken taban");
        dunya.canliZemin((x, z) -> 60);
        dunya.araliklar(-40, 4, 50, a);
        assertEquals(60, a.alt(0), "alan bilmiyorsa zemin dünyadan okunur");
        dunya.canliZemin((x, z) -> Integer.MIN_VALUE);
        dunya.araliklar(-40, 4, 50, a);
        assertEquals(63, a.alt(0), "dünya da bilmiyorsa taban");
    }

    @Test
    void dalgaTabaniAlaninEskiZemininiDegilCanliZeminiKullanir() {
        // Doğrulama-2 bulgusu: alan sütunu okuduktan sonra kod blok değiştirdi (blok olayı yok, tazeleme 20 tick sonra);
        // alan hâlâ eski zemini (64) biliyor, hendeğin dibi 61. Dalga aralığı gerçek zeminden başlamalı.
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        dunya.dalgaEkle(1, new Dalga(-80, 4, 1, 0, 0, 1, 8, 80, 30, 63, 200));
        assertEquals(64, dunya.alan().zemin(12, 4), "alan eski zemini biliyor");
        dunya.canliZemin((x, z) -> 61);
        SuAraliklari a = new SuAraliklari();
        dunya.araliklar(12, 4, 95, a);
        assertEquals(1, a.adet());
        assertEquals(61, a.alt(0), "dalga tabanı canlı zemin");
    }
}
