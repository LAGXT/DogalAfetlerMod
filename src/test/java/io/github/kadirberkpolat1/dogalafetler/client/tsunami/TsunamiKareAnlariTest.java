package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.afet.tsunami.TsunamiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.tsunami.TsunamiSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.su.Cekilme;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoublePredicate;
import org.junit.jupiter.api.Test;

/**
 * AF3-15: otomasyonun kare anları senkron evreden; eski (çekilme 18 sn, dalga AKTIF + 10 sn, kıyı + 28 sn) ve kısa (çekilme
 * 8 sn, dalga + 2 sn, kıyı + 10 sn) zamanlamada aynı evreye düşer. Zaman AKTIF'in başından tick. Entegrasyon 8: adım listesinin
 * kendisi (kamera, bekleme, ölçüm pencereleri dahil) K-E67 varsayılanları ve gerçek seviye tablosuyla yürütülür.
 */
class TsunamiKareAnlariTest {
    private static final double KX = 120;
    private static final double KZ = 0;
    private static final double HIZ = 6.0 / 20;

    /** Doğuş tick'i ve kıyıya yol tick'iyle kuzeyden doğuya ilerleyen dalga (açıklık = hız × yol). */
    private static Dalga dalga(double dogus, double yol) {
        return new Dalga(KX - HIZ * yol, 0, 1, 0, dogus, HIZ, 10, 40, 30, 60, 400);
    }

    /** {@code kosul}'un ilk doğru olduğu tick (otomasyonun {@code e:} adımı gibi tick tick). */
    private static int ilk(DoublePredicate kosul) {
        for (int t = 0; t < 4000; t++) {
            if (kosul.test(t)) {
                return t;
            }
        }
        return -1;
    }

    private record Anlar(int cekilme, int ufuk, int vitrin, int carpma, int karada) {
    }

    private static Anlar anlar(long cekilmeSure, double dogus, double yol) {
        Cekilme c = new Cekilme(KX, KZ, 40, 4, 0, cekilmeSure);
        Dalga w = dalga(dogus, yol);
        return new Anlar(ilk(t -> TsunamiKareAnlari.cekilmeIlerlemesi(c, t) >= 0.42),
                ilk(t -> TsunamiKareAnlari.kalanOran(w, KX, KZ, t) <= 0.86),
                ilk(t -> TsunamiKareAnlari.kalanOran(w, KX, KZ, t) <= 0.22),
                ilk(t -> TsunamiKareAnlari.kiyidanSonra(w, KX, KZ, t) >= 60),
                ilk(t -> TsunamiKareAnlari.kiyidanSonra(w, KX, KZ, t) >= 160));
    }

    @Test
    void eskiZamanlamadaEskiSabitAnlaraDenkGelir() {
        Anlar a = anlar(360, 200, 360);
        assertEquals(161, a.cekilme(), 12, "çekilme karesi (eski b:150 + kamera)");
        assertEquals(260, a.ufuk(), 12, "ufuk ~13 sn");
        assertEquals(480, a.vitrin(), 12, "vitrin ~24 sn");
        assertEquals(620, a.carpma(), 1, "çarpma ~31 sn");
        assertEquals(720, a.karada(), 1, "karada ~36 sn");
    }

    @Test
    void kisaZamanlamadaAyniEvreler() {
        Anlar a = anlar(160, 40, 160);
        int kiyi = 200;
        assertTrue(a.ufuk() > 40 && a.ufuk() < a.vitrin(), "ufuk doğuştan sonra: " + a);
        assertTrue(a.vitrin() < kiyi, "vitrin kıyıdan önce: " + a);
        assertEquals(kiyi + 60, a.carpma(), 1);
        assertEquals(kiyi + 160, a.karada(), 1);
        assertTrue(a.cekilme() < kiyi, "çekilme karesi dalga kıyıya varmadan: " + a);
        assertEquals(0.86, 1 - (a.ufuk() - 40) / 160.0, 0.01, "aynı kalan oran");
    }

    /** AKTIF komutunun yollandığı tick'ten sunucunun AKTIF'e geçip senkron verinin geldiği tick'e gecikme. */
    private static final int GECIKME = 2;

    /**
     * Adım listesini AKTIF komutundan (son {@code asama ileri}) sonra {@code TsunamiOtomasyonu.tick} kurallarıyla yürütür ve
     * her {@code s:} karesinin alındığı tick'i verir: {@code k/v/g/q} bir tick, {@code b:N} N tick, {@code f:} pencere + 1,
     * {@code e:} koşulun ilk doğru olduğu tick + 1; kare {@code kapi} tick sonra alınır (A-34 kapısı, yavaş makinede uzar).
     */
    private static Map<String, Integer> kareTickleri(List<String> adimlar, Cekilme c, Dalga w, int kapi) {
        Map<String, Integer> kareler = new LinkedHashMap<>();
        int t = 1;
        for (String adim : adimlar.subList(adimlar.lastIndexOf("k:dogalafet asama ileri") + 1, adimlar.size())) {
            String d = adim.substring(2);
            switch (adim.charAt(0)) {
                case 'b' -> t += Integer.parseInt(d);
                case 'f' -> t += TsunamiKareAnlari.OLCUM_TICK + 1;
                case 's' -> {
                    kareler.put(d.substring(d.lastIndexOf('-') + 1), t + kapi);
                    t += kapi + 1;
                }
                case 'e' -> {
                    String evre = d.substring(0, d.indexOf('='));
                    double esik = Double.parseDouble(d.substring(d.indexOf('=') + 1));
                    DoublePredicate geldi = switch (evre) {
                        case "cekilme" -> x -> TsunamiKareAnlari.cekilmeIlerlemesi(c, x) >= esik;
                        case "kalan" -> x -> TsunamiKareAnlari.kalanOran(w, KX, KZ, x) <= esik;
                        default -> x -> TsunamiKareAnlari.kiyidanSonra(w, KX, KZ, x) >= esik;
                    };
                    int u = t + 1;
                    while (!geldi.test(u)) {
                        u++;
                    }
                    t = u + 1;
                }
                default -> t += 1;
            }
        }
        return kareler;
    }

    /**
     * K-E67 varsayılanları (çekilme 8 sn, dalga + 2 sn) ve gerçek S1–S5 hız/açıklığıyla S5 ve S1 adım listeleri her kareyi
     * evresinde alır: çekilme %42'den sonra, ufuk dalga açıktayken, vitrin kıyıya vurmadan (kalan 0–0,22), çarpma kıyı + 60,
     * karada kıyı + 160 tick'ten sonra. Eski listede ufuk ile vitrin arasındaki 100 tick'lik ölçüm vitrini kıyı sonrasına atıyordu.
     */
    @Test
    void kisaZamanlamadaAdimListesiKareleriEvresindeAlir() {
        long cekilmeSure = Math.round(TsunamiAyarlari.VARSAYILAN_CEKILME_SN * 20);
        double dogus = GECIKME + Math.round(TsunamiAyarlari.VARSAYILAN_DALGA_GECIKME_SN * 20);
        for (int s = 1; s <= 5; s++) {
            TsunamiSeviyesi sv = TsunamiSeviyesi.al(s);
            Cekilme c = new Cekilme(KX, KZ, 40, 4, GECIKME, cekilmeSure);
            Dalga w = new Dalga(KX - sv.aciklik(), 0, 1, 0, dogus, sv.hizTick(), 10, 40, 30, 60, 400);
            double kiyi = dogus + sv.aciklik() / sv.hizTick();
            for (int kapi : new int[] {1, 20}) {
                Map<String, Integer> k = kareTickleri(TsunamiKareAnlari.tsunamiAdimlari(s, "t"), c, w, kapi);
                String ne = "S" + s + " kapı " + kapi + " kıyı " + kiyi + ": " + k;
                assertTrue(TsunamiKareAnlari.cekilmeIlerlemesi(c, k.get("cekilme")) >= 0.42 && k.get("cekilme") < kiyi,
                        "çekilme karesi " + ne);
                double ufuk = TsunamiKareAnlari.kalanOran(w, KX, KZ, k.get("ufuk"));
                assertTrue(ufuk <= 0.86 && ufuk > 0.22, "ufuk karesi dalga açıktayken: " + ufuk + " " + ne);
                double vitrin = TsunamiKareAnlari.kalanOran(w, KX, KZ, k.get("vitrin"));
                assertTrue(vitrin > 0 && vitrin <= 0.22, "vitrin karesi kıyıya vurmadan: " + vitrin + " " + ne);
                double carpma = TsunamiKareAnlari.kiyidanSonra(w, KX, KZ, k.get("carpma"));
                assertTrue(carpma >= 60 && carpma <= 60 + kapi + 2, "çarpma karesi: " + carpma + " " + ne);
                double karada = TsunamiKareAnlari.kiyidanSonra(w, KX, KZ, k.get("karada"));
                assertTrue(karada >= 160 && karada <= 160 + 2 * kapi + 10, "karada karesi: " + karada + " " + ne);
            }
        }
    }

    @Test
    void sinirlar() {
        Dalga w = dalga(100, 200);
        assertEquals(1, TsunamiKareAnlari.kalanOran(w, KX, KZ, 0), 1e-12, "doğuştan önce 1");
        assertEquals(1, TsunamiKareAnlari.kalanOran(w, KX, KZ, 100), 1e-12);
        assertEquals(0, TsunamiKareAnlari.kalanOran(w, KX, KZ, 300), 1e-12, "kıyıda 0");
        assertEquals(0, TsunamiKareAnlari.kalanOran(w, KX, KZ, 900), 1e-12, "sonrası 0");
        assertEquals(0, TsunamiKareAnlari.kiyidanSonra(w, KX, KZ, 300), 1e-9);
        assertEquals(-200, TsunamiKareAnlari.kiyidanSonra(w, KX, KZ, 100), 1e-9);
        assertEquals(0, TsunamiKareAnlari.kalanOran(w, w.x0() - 5, KZ, 150), 1e-12, "kıyı çizginin gerisinde: 0");
        Cekilme c = new Cekilme(0, 0, 10, 3, 50, 0);
        assertEquals(0, TsunamiKareAnlari.cekilmeIlerlemesi(c, 50), 1e-12);
        assertEquals(1, TsunamiKareAnlari.cekilmeIlerlemesi(c, 51), 1e-12, "süresiz çekilme hemen tamam");
    }
}
