package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

/**
 * Sel suyu (K-E80) sanal su dünyasında: yükselme, ay çarpması sonrası kalıcı artışla toplanması, ısı bölgelerinde yalnız selin
 * sönmesi (denizin kendi seviyesi sürer), temizleme, bitiş ve paket gidiş gelisi.
 */
class SanalSuDunyasiSelTest {
    /** Sol 8 blok deniz (zemin 63, dip 58), sağı düz kara (zemin 64): sel karayı kaplar. */
    private static DenizAlani duzKiyi() {
        DenizAlani alan = new DenizAlani(8, 1, 63);
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

    /** 100'de başlar; 100 tick yükselir, 1000 tick tepede (2 blok), 100 tick çekilir. */
    private static SelSuyu sel() {
        return new SelSuyu(100, 100, 1000, 100, 2.0);
    }

    @Test
    void selKarayiTepedeKaplarOncesindeVeSonrasindaKuruBirakir() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        SuAraliklari a = new SuAraliklari();
        dunya.araliklar(10, 4, 600, a);
        assertEquals(0, a.adet(), "sel yokken kara kuru");
        assertTrue(!dunya.aktif() && !dunya.alanGerekli());

        dunya.selEkle(1, sel());
        assertTrue(dunya.aktif() && dunya.alanGerekli(), "sel sürerken alan okunur");
        dunya.araliklar(10, 4, 50, a);
        assertEquals(0, a.adet(), "başlamadan önce kuru");
        dunya.araliklar(10, 4, 600, a);
        assertEquals(1, a.adet(), "tepede kara su altında");
        assertEquals(64, a.alt(0));
        assertEquals(65.0, a.ust(0), 1e-9, "deniz yüzeyi 63 + 2 blok");
        dunya.araliklar(10, 4, 1500, a);
        assertEquals(0, a.adet(), "çekildikten sonra kuru");
    }

    @Test
    void selKaliciDenizArtisinaEklenirVeEnCokArtisaKirpilir() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        dunya.denizArtisiAyarla(1.5);
        dunya.selEkle(1, sel());
        assertEquals(1.5, dunya.etkinArtis(0), 1e-9);
        assertEquals(3.5, dunya.etkinArtis(600), 1e-9, "kalıcı artış + sel");
        dunya.denizArtisiAyarla(15.0);
        assertEquals(DenizAlani.EN_COK_ARTIS, dunya.etkinArtis(600), 1e-9, "sistemin en büyük artışını aşmaz");
    }

    @Test
    void selSuyundaMiYalnizSelinKapladigiYerdeDogru() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        assertTrue(!dunya.selSuyundaMi(10.5, 64.5, 4.5, 600), "sel yokken kara kuru");
        dunya.selEkle(1, sel());
        assertTrue(!dunya.selSuyundaMi(10.5, 64.5, 4.5, 50), "sel başlamadan önce değil");
        assertTrue(dunya.selSuyundaMi(10.5, 64.5, 4.5, 600), "tepede karanın sel suyu");
        assertTrue(!dunya.selSuyundaMi(10.5, 65.5, 4.5, 600), "su yüzeyinin üstü sel suyu değil");
        assertTrue(dunya.selSuyundaMi(2.5, 64.5, 4.5, 600), "denizin sel yükselttiği katman sel suyudur");
        assertTrue(!dunya.selSuyundaMi(2.5, 62.5, 4.5, 600), "denizin kendi suyu (yüzeyin altı) sel değil");
        assertTrue(!dunya.selSuyundaMi(10.5, 64.5, 4.5, 1500), "çekildikten sonra değil");

        dunya.denizArtisiAyarla(1.0);
        assertTrue(!dunya.selSuyundaMi(2.5, 63.5, 4.5, 600), "ay çarpması sonrası kalıcı yükselme sel sayılmaz");
        assertTrue(dunya.selSuyundaMi(2.5, 65.5, 4.5, 600), "kalıcı yükselmenin üstündeki sel katmanı");
        assertTrue(dunya.selSuyundaMi(10.5, 64.5, 4.5, 600), "kalıcı deniz karaya ulaşmamış, kara sel suyunda");
    }

    @Test
    void isiBolgesiSelSuyunuSonduruDenizinKendiSeviyesiniDegil() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        dunya.denizArtisiAyarla(1.0);
        dunya.selEkle(1, sel());
        dunya.isiBolgeleriAyarla(List.of(new IsiBolgesi(8, 64, 4, 6f)));
        SuAraliklari a = new SuAraliklari();
        assertEquals(1.0, dunya.sutunArtisi(8, 4, 600), 1e-9, "çekirdekte yalnız kalıcı artış kalır");
        assertEquals(3.0, dunya.sutunArtisi(15, 4, 600), 1e-9, "bölgenin dışında tam sel");
        double kenar = dunya.sutunArtisi(12, 4, 600);
        assertTrue(kenar > 1.0 && kenar < 3.0, "kenarda kısmen: " + kenar);
        dunya.araliklar(8, 4, 600, a);
        assertEquals(0, a.adet(), "ısı kaynağının dibinde kara kuru (zemin 64, seviye 64)");
        dunya.araliklar(15, 4, 600, a);
        assertEquals(1, a.adet(), "uzakta sel sürer");
        dunya.araliklar(2, 4, 600, a);
        assertTrue(a.adet() == 1 && a.ust(0) > 63, "deniz suyu buharlaşmaz");
    }

    @Test
    void isiBolgesiSelYokkenDenizArtisiniEtkilemez() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        dunya.denizArtisiAyarla(3.0);
        dunya.isiBolgeleriAyarla(List.of(new IsiBolgesi(10, 64, 4, 6f)));
        assertEquals(3.0, dunya.sutunArtisi(10, 4, 0), 1e-9);
        SuAraliklari a = new SuAraliklari();
        dunya.araliklar(10, 4, 0, a);
        assertEquals(1, a.adet(), "ay çarpması sonrası kalıcı deniz ısıyla sönmez");
    }

    @Test
    void temizleSelleriVeIsiBolgelerineYolAcarBitenSellerSecilir() {
        SanalSuDunyasi dunya = new SanalSuDunyasi(duzKiyi());
        dunya.selEkle(1, sel());
        dunya.selEkle(2, new SelSuyu(0, 10, 10, 10, 1));
        dunya.isiBolgeleriAyarla(List.of(new IsiBolgesi(1, 64, 1, 3f)));
        assertEquals(List.of(2), dunya.bitenSeller(100), "yalnız ikinci sel bitti");
        assertTrue(dunya.selKaldir(2) && !dunya.selKaldir(2));
        assertEquals(1, dunya.seller().size());
        assertEquals(1, dunya.selDizisi().length);
        dunya.temizle();
        assertTrue(dunya.seller().isEmpty() && dunya.isiDizisi().length == 0);
        assertTrue(!dunya.aktif());
    }

    @Test
    void suPaketiSelVeIsiBolgeleriniAynenTasir() {
        Map<Integer, SelSuyu> seller = Map.of(5, sel(), 9, new SelSuyu(12.5, 1, 2, 3, 0.25));
        List<IsiBolgesi> isi = List.of(new IsiBolgesi(-300, 63, 12345, 4.5f), new IsiBolgesi(7, -2, 9, 2f));
        FriendlyByteBuf tampon = new FriendlyByteBuf(Unpooled.buffer());
        SuPaketi.selYaz(tampon, seller);
        SuPaketi.isiYaz(tampon, isi);
        assertEquals(seller, SuPaketi.selOku(tampon));
        assertEquals(isi, SuPaketi.isiOku(tampon));
        assertEquals(0, tampon.readableBytes(), "paket fazla bayt bırakmamalı");

        SanalSuDunyasi istemci = new SanalSuDunyasi(duzKiyi());
        seller.forEach(istemci::selEkle);
        istemci.isiBolgeleriAyarla(isi);
        SuPaketi paket = SuPaketi.durumdan(null, istemci);
        assertEquals(seller, paket.seller());
        assertEquals(isi, paket.isiBolgeleri().stream().sorted(java.util.Comparator.comparingInt(IsiBolgesi::x)).toList());
        SanalSuDunyasi alici = new SanalSuDunyasi(duzKiyi());
        paket.uygula(alici);
        assertEquals(istemci.selArtisi(600), alici.selArtisi(600), 1e-12);
        assertEquals(2, alici.isiDizisi().length);
    }
}
