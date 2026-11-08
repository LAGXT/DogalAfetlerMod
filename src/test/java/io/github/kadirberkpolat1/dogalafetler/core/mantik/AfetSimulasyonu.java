package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Afet yöneticisinin saf mantığını Minecraft'sız koşturan test simülasyonu. Seçim sunucudakiyle aynı yoldan geçer
 * ({@link SecimTuru}: zamanlayıcı + hedef sıralaması + erteleme); uygun türler her adımda {@link TekSeferlikKurallar} ile
 * dünyanın gününden, ay ve güneş durumundan ve gelişim puanından kurulur. Eşzamanlı doğal afetler, gün çarpanı ve yoğunluk
 * zayıflatması yöneticidekiyle aynı kurallardandır ({@link EszamanliAfetler}, {@link ZamanlayiciAyarlari#gunCarpani},
 * {@link AfetYogunlugu}; K-E67). Afetler sabit süre ya da {@link #sureler} ile tür ve seviyeye göre sürer; yer her zaman
 * uygundur; ilk afet garantisi (AS-12) yöneticideki gibi işler. {@link #cakisma}: süren afet varken her yeni afet onunla çakışır (tek oyuncu: hepsi aynı oyuncunun çevresinde),
 * kapalıyken hiç çakışmaz (dağınık çok oyunculu sunucu). {@code donemGecisleri}: ay çarpması ayı kaldırır (ve tsunami
 * çarpanını ekler), güneş patlaması güneşi NORMAL dışına alır.
 */
final class AfetSimulasyonu {
    record Olay(AfetTuru tur, int seviye, long baslangic, long bitis, int hedef) {
    }

    /** Afetin tür ve seviyesine göre başlangıcından bitişine süresi (tick). */
    @FunctionalInterface
    interface SureTablosu {
        long sure(AfetTuru tur, int seviye);
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
    private SureTablosu sureler;
    private boolean cakisma;
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
        this.sureler = null;
        return this;
    }

    AfetSimulasyonu sureler(SureTablosu tablo) {
        this.sureler = tablo;
        return this;
    }

    /** Açıkken süren afet varken başlayan her doğal afet onunla çakışır (tek oyuncu). */
    AfetSimulasyonu cakisma(boolean acik) {
        this.cakisma = acik;
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
        List<Olay> aktifler = new ArrayList<>();
        ArrayDeque<Long> dogalBaslangiclar = new ArrayDeque<>();
        int ihlal = 0;
        boolean ay = ayVar;
        GunesEvresi gunes = evre;
        EnumSet<AfetTuru> uygun = EnumSet.noneOf(AfetTuru.class);
        long uygunGunu = Long.MIN_VALUE;
        boolean uygunAy = !ay;
        GunesEvresi uygunGunes = null;
        long son = toplamTick;

        for (long saat = 0; saat < toplamTick; saat += adim) {
            double gun = gunSabit ? baslangicGunu : baslangicGunu + saat / (double) AfetZamanlayicisi.GUN_TICK;
            double gunCarpani = zamanlayici.gunCarpani(gun);
            for (Iterator<Olay> it = aktifler.iterator(); it.hasNext();) {
                Olay biten = it.next();
                if (saat >= biten.bitis()) {
                    bekleme.afetBitti(biten.tur(), biten.seviye(), saat, zamanlayici, siklik, gunCarpani);
                    if (biten.tur().seviyeli()) {
                        oncekiYuksek = SiddetSecici.yuksek(biten.seviye());
                    }
                    it.remove();
                }
            }
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
            List<AfetTuru> aktifTurler = aktifler.stream().map(Olay::tur).toList();
            boolean yuvaDolu = EszamanliAfetler.yuvaDolu(aktifTurler, zamanlayici.eszamanliSinir(gun), false);
            boolean garanti = olaylar.isEmpty() && zamanlayici.ilkAfetGarantiTick() > 0
                    && saat >= AfetZamanlayicisi.beklemeOlcekle(zamanlayici.ilkAfetGarantiTick(), siklik);
            AfetZamanlayicisi.Girdi girdi = new AfetZamanlayicisi.Girdi(yuvaDolu, saat, adim, bekleme,
                    EszamanliAfetler.adaylar(uygun, aktifTurler), tur -> carpanlar.carpan(tur, simdi), siklik, false, garanti,
                    gunCarpani);
            boolean cakisiyor = cakisma && !aktifler.isEmpty();
            SecimTuru.Sonuc<Integer> secim = SecimTuru.isle(zamanlayici, girdi, adaylar, sonHedef,
                    (tur, hedef) -> cakisiyor ? SecimTuru.Deneme.CAKISTI : SecimTuru.Deneme.BASLADI, rastgele);
            if (!secim.basladi()) {
                continue;
            }
            if (yuvaDolu || aktifTurler.contains(secim.tur())) {
                ihlal++;
                continue;
            }
            while (!dogalBaslangiclar.isEmpty() && dogalBaslangiclar.peekFirst() <= saat - AfetZamanlayicisi.GUN_TICK) {
                dogalBaslangiclar.removeFirst();
            }
            AfetTuru tur = secim.tur();
            double yuk = AfetYogunlugu.yuk(dogalBaslangiclar.size(), gun, siklik);
            int seviye = tur.seviyeli() ? SiddetSecici.sec(siddet, gun, maksSiddet, oncekiYuksek, yuk, rastgele) : 0;
            Integer hedef = secim.hedef();
            if (hedef != null) {
                sonHedef = hedef;
            }
            long sure = sureler == null ? afetSuresi : sureler.sure(tur, seviye);
            Olay olay = new Olay(tur, seviye, saat, saat + sure, hedef == null ? -1 : hedef);
            aktifler.add(olay);
            olaylar.add(olay);
            dogalBaslangiclar.addLast(saat);
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
