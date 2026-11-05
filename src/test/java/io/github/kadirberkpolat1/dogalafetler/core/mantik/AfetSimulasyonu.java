package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Afet yöneticisinin saf mantığını Minecraft'sız koşturan test simülasyonu: zamanlayıcı, bekleme, şiddet seçimi ve hedef
 * seçimi, gerçek yöneticideki sırayla ({@code kontrol aralığı} = 20 tick) çağrılır. Afetler sabit süre sürer.
 */
final class AfetSimulasyonu {
    static final long ADIM = 20;

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
    }

    private ZamanlayiciAyarlari zamanlayici = ZamanlayiciAyarlari.VARSAYILAN;
    private SiddetAyarlari siddet = SiddetAyarlari.VARSAYILAN;
    private Set<AfetTuru> uygunTurler = EnumSet.allOf(AfetTuru.class);
    private CarpanListesi carpanlar = new CarpanListesi();
    private int oyuncuSayisi = 1;
    private long afetSuresi = 6000;
    private double siklik = 1.0;
    private long tohum = 20261105L;
    private int maksSiddet = 5;

    AfetSimulasyonu zamanlayici(ZamanlayiciAyarlari ayarlar) {
        this.zamanlayici = ayarlar;
        return this;
    }

    AfetSimulasyonu uygunTurler(Set<AfetTuru> turler) {
        this.uygunTurler = EnumSet.copyOf(turler);
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

    AfetSimulasyonu maksSiddet(int seviye) {
        this.maksSiddet = seviye;
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
        Integer sonHedef = null;
        boolean oncekiYuksek = false;
        Olay aktif = null;
        int ihlal = 0;

        for (long saat = 0; saat < toplamTick; saat += ADIM) {
            if (aktif != null && saat >= aktif.bitis()) {
                bekleme.afetBitti(aktif.tur(), aktif.seviye(), saat, zamanlayici);
                if (aktif.tur().seviyeli()) {
                    oncekiYuksek = SiddetSecici.yuksek(aktif.seviye());
                }
                aktif = null;
            }
            final long simdi = saat;
            AfetZamanlayicisi.Girdi girdi = new AfetZamanlayicisi.Girdi(aktif != null, saat, ADIM, bekleme,
                    uygunTurler, tur -> carpanlar.carpan(tur, simdi), siklik, false);
            var secim = AfetZamanlayicisi.sec(zamanlayici, girdi, rastgele);
            if (secim.isEmpty()) {
                continue;
            }
            if (aktif != null) {
                ihlal++;
                continue;
            }
            AfetTuru tur = secim.get();
            int seviye = tur.seviyeli()
                    ? SiddetSecici.sec(siddet, saat / (double) AfetZamanlayicisi.GUN_TICK, maksSiddet, oncekiYuksek,
                            rastgele)
                    : 0;
            Integer hedef = tur.sunucuGeneli() ? null : HedefSecici.sirala(adaylar, sonHedef, rastgele).get(0);
            if (hedef != null) {
                sonHedef = hedef;
            }
            aktif = new Olay(tur, seviye, saat, saat + afetSuresi, hedef == null ? -1 : hedef);
            olaylar.add(aktif);
            sayilar.merge(tur, 1, Integer::sum);
        }
        return new Sonuc(sayilar, olaylar, ihlal, toplamTick);
    }
}
