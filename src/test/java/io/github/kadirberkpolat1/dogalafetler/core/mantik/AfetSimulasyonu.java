package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
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
import java.util.random.RandomGenerator;

/**
 * Afet yöneticisinin saf mantığını Minecraft'sız koşturan test simülasyonu. Seçim sunucudakiyle aynı yoldan geçer
 * ({@link SecimTuru}: zamanlayıcı + hedef sıralaması + erteleme); uygun türler her adımda {@link TekSeferlikKurallar} ile
 * dünyanın gününden, ay ve güneş durumundan ve gelişim puanından kurulur. Eşzamanlı doğal afetler, gün çarpanı, yoğunluk
 * zayıflatması ve güneş/ay gün saati penceresi yöneticidekiyle aynı kurallardandır ({@link EszamanliAfetler},
 * {@link ZamanlayiciAyarlari#gunCarpani}, {@link AfetYogunlugu}, {@link GunSaatiPenceresi#dogalSecimCarpani}; K-E67); gün
 * saati simülasyon saatinin 24000'e göre kalanıdır (0 = gün doğumu). Seviye yöneticideki gibi başlatmadan önce tür başına
 * seçilir. Afetler sabit süre ya da {@link #sureler} ile tür ve seviyeye göre sürer; yer her zaman uygundur; ilk afet
 * garantisi (AS-12) yöneticideki gibi işler. Etki alanı: {@link #alanlar} verilirse her afet hedef oyuncunun çevresinde
 * modelin çizdiği alana yerleşir (oyuncular birbirinden {@value #OYUNCU_ARALIGI} blok uzakta) ve süren bir bölgesel afetin
 * alanıyla kesişirse çakışır ({@code EtkiAlani.kesisir}); {@link #hepsiCakisir} süren afet varken her yeni afeti
 * çakıştırır (tek yuva: bekleme zinciri sınamaları); ikisi de yoksa hiç çakışma yok. {@code donemGecisleri}: ay çarpması
 * ayı kaldırır (ve tsunami çarpanını ekler), güneş patlaması güneşi NORMAL dışına alır.
 */
final class AfetSimulasyonu {
    record Olay(AfetTuru tur, int seviye, long baslangic, long bitis, int hedef) {
    }

    /** Oyuncular arası uzaklık (blok): farklı oyuncuya giden afetler çakışmaz. */
    static final double OYUNCU_ARALIGI = 10_000;

    /** Hedef oyuncuya göre etki alanı (oyuncu 0, 0'da); global alan {@link EtkiAlani#herYer()}. */
    @FunctionalInterface
    interface AlanModeli {
        EtkiAlani alan(AfetTuru tur, int seviye, RandomGenerator rastgele);
    }

    private record Aktif(Olay olay, EtkiAlani alan) {
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
    private boolean hepsiCakisir;
    private AlanModeli alanlar;
    private boolean pencere = true;
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

    /** Süren afet varken başlayan her doğal afet onunla çakışır: tek yuva gibi (bekleme zinciri sınamaları). */
    AfetSimulasyonu hepsiCakisir() {
        this.hepsiCakisir = true;
        this.alanlar = null;
        return this;
    }

    /** Afetler hedef oyuncunun çevresinde modelin alanına yerleşir; kesişen alan çakışır. */
    AfetSimulasyonu alanlar(AlanModeli model) {
        this.alanlar = model;
        this.hepsiCakisir = false;
        return this;
    }

    /** Güneş/ay gün saati penceresi (varsayılan açık; kapalı: eski, gün saatinden bağımsız oran). */
    AfetSimulasyonu pencere(boolean acik) {
        this.pencere = acik;
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
        EnumMap<AfetTuru, Long> paylar = new EnumMap<>(AfetTuru.class);
        for (AfetTuru tur : AfetTuru.values()) {
            paylar.put(tur, GunSaatiPenceresi.gorunmePayi(tur, OnUyariPenceresi.VARSAYILAN_ERKEN_BILDIRIM,
                    OnUyariPenceresi.VARSAYILAN_EN_AZ, OnUyariPenceresi.VARSAYILAN_EN_COK));
        }
        Set<AfetTuru> kalanDurma = EnumSet.noneOf(AfetTuru.class);
        kalanDurma.addAll(durmaTurleri);
        Integer sonHedef = null;
        boolean oncekiYuksek = false;
        List<Aktif> aktifler = new ArrayList<>();
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
            for (Iterator<Aktif> it = aktifler.iterator(); it.hasNext();) {
                Olay biten = it.next().olay();
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
            final long gunSaati = Math.floorMod(saat, AfetZamanlayicisi.GUN_TICK);
            List<AfetTuru> aktifTurler = aktifler.stream().map(a -> a.olay().tur()).toList();
            boolean yuvaDolu = EszamanliAfetler.yuvaDolu(aktifTurler, aktifTurler.size(), zamanlayici.eszamanliSinir(gun),
                    false);
            boolean garanti = olaylar.isEmpty() && zamanlayici.ilkAfetGarantiTick() > 0
                    && saat >= AfetZamanlayicisi.beklemeOlcekle(zamanlayici.ilkAfetGarantiTick(), siklik);
            AfetZamanlayicisi.Girdi girdi = new AfetZamanlayicisi.Girdi(yuvaDolu, saat, adim, bekleme,
                    EszamanliAfetler.adaylar(uygun, aktifTurler),
                    tur -> carpanlar.carpan(tur, simdi) * (pencere
                            ? GunSaatiPenceresi.dogalSecimCarpani(tur, gunSaati, paylar.get(tur), true) : 1.0),
                    siklik, false, garanti, gunCarpani);
            while (!dogalBaslangiclar.isEmpty() && dogalBaslangiclar.peekFirst() <= saat - AfetZamanlayicisi.GUN_TICK) {
                dogalBaslangiclar.removeFirst();
            }
            final boolean oncekiYuksekSimdi = oncekiYuksek;
            double yuk = AfetYogunlugu.yuk(dogalBaslangiclar.size(), gun, siklik);
            EnumMap<AfetTuru, Integer> seviyeler = new EnumMap<>(AfetTuru.class);
            EtkiAlani[] yeniAlan = new EtkiAlani[1];
            SecimTuru.Sonuc<Integer> secim = SecimTuru.isle(zamanlayici, girdi, adaylar, sonHedef, (tur, hedef) -> {
                int seviye = seviyeler.computeIfAbsent(tur, t -> t.seviyeli()
                        ? SiddetSecici.sec(siddet, gun, maksSiddet, oncekiYuksekSimdi, yuk, rastgele) : 0);
                yeniAlan[0] = yerlestir(tur, seviye, hedef, rastgele);
                return cakisir(aktifler, yeniAlan[0]) ? SecimTuru.Deneme.CAKISTI : SecimTuru.Deneme.BASLADI;
            }, rastgele);
            if (!secim.basladi()) {
                continue;
            }
            if (yuvaDolu || aktifTurler.contains(secim.tur())) {
                ihlal++;
                continue;
            }
            AfetTuru tur = secim.tur();
            int seviye = seviyeler.getOrDefault(tur, 0);
            Integer hedef = secim.hedef();
            if (hedef != null) {
                sonHedef = hedef;
            }
            long sure = sureler == null ? afetSuresi : sureler.sure(tur, seviye);
            Olay olay = new Olay(tur, seviye, saat, saat + sure, hedef == null ? -1 : hedef);
            aktifler.add(new Aktif(olay, yeniAlan[0]));
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

    /** Hedef oyuncunun çevresinde mutlak alan; model yoksa ya da sunucu geneli türde global. */
    private EtkiAlani yerlestir(AfetTuru tur, int seviye, Integer hedef, SplittableRandom rastgele) {
        if (alanlar == null || tur.sunucuGeneli() || hedef == null) {
            return EtkiAlani.herYer();
        }
        EtkiAlani a = alanlar.alan(tur, seviye, rastgele);
        return a.global() ? a : EtkiAlani.daire(a.x() + hedef * OYUNCU_ARALIGI, 0, a.z(), a.yaricap());
    }

    /** Yöneticideki gibi: yalnız süren bölgesel afetlerle (sunucu geneli yuvayı zaten doldurur). */
    private boolean cakisir(List<Aktif> aktifler, EtkiAlani yeni) {
        for (Aktif aktif : aktifler) {
            if (aktif.olay().tur().sunucuGeneli()) {
                continue;
            }
            if (hepsiCakisir || alanlar != null && yeni.kesisir(aktif.alan())) {
                return true;
            }
        }
        return false;
    }
}
