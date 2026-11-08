package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Afet yöneticisinin saf mantığını Minecraft'sız koşturan test simülasyonu. Seçim sunucudakiyle aynı yoldan geçer
 * ({@link SecimTuru}: zamanlayıcı + hedef sıralaması + erteleme); uygun türler her adımda {@link TekSeferlikKurallar} ile
 * dünyanın gününden, ay ve güneş durumundan ve gelişim puanından kurulur. Afetler sabit süre sürer; yer her zaman uygundur.
 * {@code donemGecisleri}: ay çarpması ayı kaldırır (ve tsunami çarpanını ekler), güneş patlaması güneşi NORMAL dışına alır.
 */
final class AfetSimulasyonu {
    record Olay(AfetTuru tur, int seviye, long baslangic, long bitis, int hedef) {
    }

    record Sonuc(EnumMap<AfetTuru, Integer> sayilar, List<Olay> olaylar, int ayniAnIhlali, long toplamTick) {
        int sayi(AfetTuru tur) {
            return sayilar.getOrDefault(tur, 0);
        }

        int toplam() {
            return olaylar.size();
        }

        double gunlukOlay() {
            return toplam() / (toplamTick / (double) AfetZamanlayicisi.GUN_TICK);
        }

        /** Türün ilk başladığı oyun günü (simülasyon başından); hiç çıkmadıysa boş. */
        OptionalDouble ilkGun(AfetTuru tur) {
            for (Olay olay : olaylar) {
                if (olay.tur() == tur) {
                    return OptionalDouble.of(olay.baslangic() / (double) AfetZamanlayicisi.GUN_TICK);
                }
            }
            return OptionalDouble.empty();
        }
    }

    private ZamanlayiciAyarlari zamanlayici = ZamanlayiciAyarlari.VARSAYILAN;
    private final SiddetAyarlari siddet = SiddetAyarlari.VARSAYILAN;
    private TekSeferlikAyarlari tekSeferlik = TekSeferlikAyarlari.VARSAYILAN;
    private CarpanListesi carpanlar = new CarpanListesi();
    private int oyuncuSayisi = 1;
    private long afetSuresi = 6000;
    private double siklik = 1.0;
    private long tohum = 20261105L;
    private int maksSiddet = 5;
    private long adim = 20;
    private double baslangicGunu;
    private boolean gunSabit;
    private boolean ayVar = true;
    private GunesEvresi evre = GunesEvresi.NORMAL;
    private double puan;
    private boolean donemGecisleri;
    private Set<AfetTuru> durmaTurleri = EnumSet.noneOf(AfetTuru.class);

    AfetSimulasyonu zamanlayici(ZamanlayiciAyarlari ayarlar) {
        this.zamanlayici = ayarlar;
        return this;
    }

    AfetSimulasyonu tekSeferlik(TekSeferlikAyarlari ayarlar) {
        this.tekSeferlik = ayarlar;
        return this;
    }

    AfetSimulasyonu carpanlar(CarpanListesi liste) {
        this.carpanlar = liste;
        return this;
    }

    AfetSimulasyonu oyuncuSayisi(int sayi) {
        this.oyuncuSayisi = sayi;
        return this;
    }

    AfetSimulasyonu afetSuresi(long tick) {
        this.afetSuresi = tick;
        return this;
    }

    AfetSimulasyonu siklik(double carpan) {
        this.siklik = carpan;
        return this;
    }

    AfetSimulasyonu tohum(long deger) {
        this.tohum = deger;
        return this;
    }

    AfetSimulasyonu adim(long tick) {
        this.adim = tick;
        return this;
    }

    /** Simülasyon bu oyun gününde başlar; gün saatle ilerler. */
    AfetSimulasyonu gun(double gun) {
        this.baslangicGunu = gun;
        this.gunSabit = false;
        return this;
    }

    /** Oyun günü simülasyon boyunca sabit kalır. */
    AfetSimulasyonu gunSabit(double gun) {
        this.baslangicGunu = gun;
        this.gunSabit = true;
        return this;
    }

    AfetSimulasyonu ayVar(boolean var) {
        this.ayVar = var;
        return this;
    }

    AfetSimulasyonu evre(GunesEvresi gunesEvresi) {
        this.evre = gunesEvresi;
        return this;
    }

    AfetSimulasyonu puan(double gelisimPuani) {
        this.puan = gelisimPuani;
        return this;
    }

    AfetSimulasyonu donemGecisleri(boolean acik) {
        this.donemGecisleri = acik;
        return this;
    }

    /** Verilen türlerin hepsi en az bir kez çıkınca simülasyon durur. */
    AfetSimulasyonu ilkOlaylardaDur(Set<AfetTuru> turler) {
        this.durmaTurleri = EnumSet.copyOf(turler);
        return this;
    }

    Sonuc kos(long toplamTick) {
        SplittableRandom rastgele = new SplittableRandom(tohum);
        BeklemeDurumu bekleme = new BeklemeDurumu();
        EnumMap<AfetTuru, Integer> sayilar = new EnumMap<>(AfetTuru.class);
        List<Olay> olaylar = new ArrayList<>();
        List<HedefSecici.Aday<Integer>> adaylar = new ArrayList<>();
        for (int i = 0; i < oyuncuSayisi; i++) {
            adaylar.add(new HedefSecici.Aday<>(i, false));
        }
        Set<AfetTuru> kalanDurma = EnumSet.noneOf(AfetTuru.class);
        kalanDurma.addAll(durmaTurleri);
        Integer sonHedef = null;
        boolean oncekiYuksek = false;
        Olay aktif = null;
        int ihlal = 0;
        boolean ay = ayVar;
        GunesEvresi gunes = evre;
        EnumSet<AfetTuru> uygun = EnumSet.noneOf(AfetTuru.class);
        long uygunGunu = Long.MIN_VALUE;
        boolean uygunAy = !ay;
        GunesEvresi uygunGunes = null;
        long son = toplamTick;

        for (long saat = 0; saat < toplamTick; saat += adim) {
            if (aktif != null && saat >= aktif.bitis()) {
                bekleme.afetBitti(aktif.tur(), aktif.seviye(), saat, zamanlayici);
                if (aktif.tur().seviyeli()) {
                    oncekiYuksek = SiddetSecici.yuksek(aktif.seviye());
                }
                aktif = null;
            }
            double gun = gunSabit ? baslangicGunu : baslangicGunu + saat / (double) AfetZamanlayicisi.GUN_TICK;
            long gunTam = (long) Math.floor(gun);
            if (gunTam != uygunGunu || ay != uygunAy || gunes != uygunGunes) {
                uygun = EnumSet.noneOf(AfetTuru.class);
                for (AfetTuru tur : AfetTuru.values()) {
                    if (TekSeferlikKurallar.uygunMu(tur, ay, gunes, false, false, gunTam, puan, tekSeferlik)) {
                        uygun.add(tur);
                    }
                }
                uygunGunu = gunTam;
                uygunAy = ay;
                uygunGunes = gunes;
            }
            final long simdi = saat;
            AfetZamanlayicisi.Girdi girdi = new AfetZamanlayicisi.Girdi(aktif != null, saat, adim, bekleme, uygun,
                    tur -> carpanlar.carpan(tur, simdi), siklik, false);
            SecimTuru.Sonuc<Integer> secim = SecimTuru.isle(zamanlayici, girdi, adaylar, sonHedef, (tur, hedef) -> true,
                    rastgele);
            if (!secim.basladi()) {
                continue;
            }
            if (aktif != null) {
                ihlal++;
                continue;
            }
            AfetTuru tur = secim.tur();
            int seviye = tur.seviyeli() ? SiddetSecici.sec(siddet, gun, maksSiddet, oncekiYuksek, rastgele) : 0;
            Integer hedef = secim.hedef();
            if (hedef != null) {
                sonHedef = hedef;
            }
            aktif = new Olay(tur, seviye, saat, saat + afetSuresi, hedef == null ? -1 : hedef);
            olaylar.add(aktif);
            sayilar.merge(tur, 1, Integer::sum);
            if (donemGecisleri && tur == AfetTuru.AY_CARPMASI) {
                ay = false;
                carpanlar.ekle(new GeciciCarpan(AfetTuru.TSUNAMI, 3.5, saat, 72_000, 48_000, SonumTipi.DOGRUSAL));
            }
            if (donemGecisleri && tur == AfetTuru.GUNES_PATLAMASI) {
                gunes = GunesEvresi.GUNESSIZ;
            }
            kalanDurma.remove(tur);
            if (!durmaTurleri.isEmpty() && kalanDurma.isEmpty()) {
                son = saat + adim;
                break;
            }
        }
        return new Sonuc(sayilar, olaylar, ihlal, son);
    }
}
