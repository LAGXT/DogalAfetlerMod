package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKapisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.VucutSicakligi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.uyum.ColdSweatKoprusu;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Esikler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Buyukluk;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Etki;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.EtkiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Yon;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YemeGecmisi;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTanimi;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;

/**
 * Sıcaklık yiyeceklerinin sunucu API'si (madde 14, 15; yalnız sunucu ana thread'i). {@link #uygula} bir yeme anını işler:
 * azalan verim (oyuncu başına, tür başına son yeme zamanı, {@code PersistentData} {@value #ETIKET}), etki hesabı (kademe
 * miktarı config'ten; Ateşli Güveç sıcakta güçlenir), {@code VucutSicakligi.etkiEkle} ve gösterge durum efekti, sonra
 * {@link YiyecekYendiEvent}. Sıcaklık kapısı kapalıyken yalnız yemek değeri kalır: sıcaklık, efekt ve azalan verim
 * kaydı yazılmaz; Cold Sweat yüklüyse etki ve verim yine hesaplanır ve toplam etki Cold Sweat çekirdeğine gider (F-18).
 */
public final class YiyecekAPI {
    public static final String ETIKET = "dogalafetler_yiyecek";

    /** Bir {@link #uygula} çağrısının sonucu. */
    public record Sonuc(boolean uygulandi, double verimCarpani, Etki etki, Kademe onceki, Kademe sonraki) {
    }

    private YiyecekAPI() {
    }

    /** Oyuncu {@code tanim}'daki yiyeceği yedi: sıcaklık etkisini ve durum efektini uygular (yemek değerini çağıran verir). */
    public static Sonuc uygula(ServerPlayer oyuncu, YiyecekTanimi tanim) {
        Objects.requireNonNull(oyuncu, "oyuncu");
        Objects.requireNonNull(tanim, "tanim");
        Esikler esikler = SicaklikAyarlari.al().esikler();
        double once = VucutSicakligi.al(oyuncu);
        Kademe onceki = esikler.kademe(once);
        boolean kapiAcik = SicaklikKapisi.acikMi(oyuncu.server);
        boolean coldSweat = !kapiAcik && SicaklikKapisi.coldSweatYuklu();
        if (!kapiAcik && !coldSweat) {
            Etki yok = new Etki(0, 0, tanim.sureTick());
            MinecraftForge.EVENT_BUS.post(new YiyecekYendiEvent(oyuncu, tanim, onceki, onceki, 1.0, yok, false));
            return new Sonuc(false, 1.0, yok, onceki, onceki);
        }
        EtkiAyarlari ayarlar = YiyecekAyarlari.etki();
        long simdi = oyuncu.serverLevel().getGameTime();
        YemeGecmisi gecmis = gecmis(oyuncu);
        double carpan = gecmis.ye(tanim.tur(), simdi, ayarlar);
        gecmisYaz(oyuncu, gecmis, simdi, ayarlar);
        boolean sicakta = tanim.sicaktaGuclenir()
                && Etki.sicakMi(once, SicaklikYoneticisi.analiz(oyuncu).ortamBirim(), ayarlar);
        Etki etki = Etki.hesapla(tanim, ayarlar, carpan, sicakta);
        boolean uygulandi;
        if (coldSweat) {
            ColdSweatKoprusu.yiyecekEtkisi(oyuncu, etki.anlik() + etki.sureli());
            uygulandi = etki.anlik() + etki.sureli() != 0;
        } else {
            uygulandi = VucutSicakligi.etkiEkle(oyuncu, etki.anlik(), etki.sureTick(), etki.sureli());
        }
        if (uygulandi) {
            oyuncu.addEffect(new MobEffectInstance(YiyecekKayitlari.efekt(tanim.yon()), etki.sureTick(),
                    tanim.buyukluk().seviye(), false, false, true));
        }
        Kademe sonraki = esikler.kademe(VucutSicakligi.al(oyuncu));
        MinecraftForge.EVENT_BUS.post(new YiyecekYendiEvent(oyuncu, tanim, onceki, sonraki, carpan, etki, uygulandi));
        return new Sonuc(uygulandi, carpan, etki, onceki, sonraki);
    }

    /** Pişmiş buz balığı (madde 15): orta ısınma, süre config {@code yiyecek.balik.pismisSaniye}. */
    public static YiyecekTanimi pismisBalik(String tur) {
        return YiyecekTanimi.pismisBalik(tur, YiyecekAyarlari.balikPismisSaniye());
    }

    /** Daha çok ısıtan pişmiş balık (ör. Donmuş Ringa: {@link Buyukluk#BUYUK}). */
    public static YiyecekTanimi pismisBalik(String tur, Buyukluk buyukluk) {
        return new YiyecekTanimi(tur, Yon.ISINMA, buyukluk, YiyecekAyarlari.balikPismisSaniye(), false);
    }

    /** Çiğ buz balığı (madde 15): hafif serinlik, süre config {@code yiyecek.balik.cigSaniye}. */
    public static YiyecekTanimi cigBalik(String tur) {
        return YiyecekTanimi.cigBalik(tur, YiyecekAyarlari.balikCigSaniye());
    }

    /** Vanilla (ya da config'e eklenmiş) pişmiş yiyeceğin tanımı; ısınma kapalıysa ya da listede yoksa boş. */
    public static Optional<YiyecekTanimi> vanillaTanimi(Item esya) {
        if (!YiyecekAyarlari.vanillaIsinma() || esya instanceof SicaklikYiyecegi) {
            return Optional.empty();
        }
        return Optional.ofNullable(YiyecekAyarlari.vanillaTanimlari().get(esya));
    }

    /** Türün azalan verim penceresinden kalan (tick); pencere dışındaysa 0. */
    public static long kalanVerimTick(ServerPlayer oyuncu, String tur) {
        return gecmis(oyuncu).kalanTick(tur, oyuncu.serverLevel().getGameTime(), YiyecekAyarlari.etki());
    }

    /** Penceresi süren türler → kalan tick ({@code bilgi} komutu). */
    public static Map<String, Long> surenPencereler(ServerPlayer oyuncu) {
        long simdi = oyuncu.serverLevel().getGameTime();
        EtkiAyarlari ayarlar = YiyecekAyarlari.etki();
        YemeGecmisi g = gecmis(oyuncu);
        Map<String, Long> sonuc = new HashMap<>();
        for (String tur : g.kayitlar().keySet()) {
            long kalan = g.kalanTick(tur, simdi, ayarlar);
            if (kalan > 0) {
                sonuc.put(tur, kalan);
            }
        }
        return sonuc;
    }

    private static YemeGecmisi gecmis(ServerPlayer oyuncu) {
        CompoundTag kalici = oyuncu.getPersistentData();
        Map<String, Long> kayit = new HashMap<>();
        if (kalici.contains(ETIKET, Tag.TAG_COMPOUND)) {
            CompoundTag etiket = kalici.getCompound(ETIKET);
            for (String tur : etiket.getAllKeys()) {
                kayit.put(tur, etiket.getLong(tur));
            }
        }
        return YemeGecmisi.kayitlardan(kayit);
    }

    private static void gecmisYaz(ServerPlayer oyuncu, YemeGecmisi gecmis, long simdi, EtkiAyarlari ayarlar) {
        gecmis.buda(simdi, ayarlar);
        CompoundTag etiket = new CompoundTag();
        gecmis.kayitlar().forEach(etiket::putLong);
        oyuncu.getPersistentData().put(ETIKET, etiket);
    }

    /** GameTest: türün son yeme zamanını geriye alarak 60 sn beklemeyi taklit eder. */
    static void sonYemeAyarla(ServerPlayer oyuncu, String tur, long tick) {
        CompoundTag kalici = oyuncu.getPersistentData();
        CompoundTag etiket = kalici.getCompound(ETIKET);
        etiket.putLong(tur, tick);
        kalici.put(ETIKET, etiket);
    }

    /** Boyut dönüşü gibi ölümsüz kopyada geçmiş yeni oyuncu nesnesine taşınır (ölümde yeni hayat temiz başlar). */
    static void kopyala(ServerPlayer eski, ServerPlayer yeni) {
        CompoundTag kalici = eski.getPersistentData();
        if (kalici.contains(ETIKET, Tag.TAG_COMPOUND)) {
            yeni.getPersistentData().put(ETIKET, kalici.getCompound(ETIKET).copy());
        }
    }
}
