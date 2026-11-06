package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetIzi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetKumesi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.DenizTakibi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.KutupSayaci;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Unvan;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Oyuncu başına kalıcı başarım verisi: {@code getPersistentData()} içinde {@value #ETIKET} (sıcaklığın
 * {@code VucutSicakligi} deseni). Capability yerine bu seçildi: veri yalnız sunucuda okunur, istemciye gitmez, küçüktür
 * (&lt; 200 bayt) ve oyuncu dosyasıyla kaydedilir; ölüm/boyut dönüşünde {@link #kopyala} ile taşınır (A-03). Alanlar:
 * {@code atlatilan} (sağ çıkılan afet türleri), {@code balik} (buz deliğinden toplam balık), {@code unvan},
 * {@code kutup}/{@code kutupSon}/{@code kutupDongu} (Kutup Sığınağı sayacı ve ait olduğu sıfırlama döngüsü),
 * {@code denizOlum} (yükselişte ölüm döngüsü), {@code afet} (süren afetteki iz). Yalnız sunucu ana thread'inde.
 */
public final class BasarimVerisi {
    public static final String ETIKET = "dogalafetler_basarim";
    static final String EK_VERI = "basarim";
    private static final String SIFIRLAMA = "sifirlama";
    private static long sonIsaret;

    private BasarimVerisi() {
    }

    private static CompoundTag kok(ServerPlayer oyuncu) {
        CompoundTag kalici = oyuncu.getPersistentData();
        if (!kalici.contains(ETIKET, Tag.TAG_COMPOUND)) {
            kalici.put(ETIKET, new CompoundTag());
        }
        return kalici.getCompound(ETIKET);
    }

    public static AfetKumesi atlatilanlar(ServerPlayer oyuncu) {
        ListTag liste = kok(oyuncu).getList("atlatilan", Tag.TAG_STRING);
        List<String> kimlikler = new ArrayList<>();
        for (int i = 0; i < liste.size(); i++) {
            kimlikler.add(liste.getString(i));
        }
        return AfetKumesi.kimliklerden(kimlikler);
    }

    public static void atlatilanlariYaz(ServerPlayer oyuncu, AfetKumesi kume) {
        ListTag liste = new ListTag();
        for (String kimlik : kume.kimlikler()) {
            liste.add(StringTag.valueOf(kimlik));
        }
        kok(oyuncu).put("atlatilan", liste);
    }

    public static int balik(ServerPlayer oyuncu) {
        return kok(oyuncu).getInt("balik");
    }

    /** Toplamı artırır, yeni toplamı döner. */
    public static int balikEkle(ServerPlayer oyuncu, int adet) {
        CompoundTag etiket = kok(oyuncu);
        int toplam = (int) Math.min(Integer.MAX_VALUE, (long) etiket.getInt("balik") + Math.max(0, adet));
        etiket.putInt("balik", toplam);
        return toplam;
    }

    public static Optional<Unvan> unvan(ServerPlayer oyuncu) {
        return Unvan.kimlikIle(kok(oyuncu).getString("unvan"));
    }

    public static void unvanYaz(ServerPlayer oyuncu, Optional<Unvan> unvan) {
        kok(oyuncu).putString("unvan", unvan.map(Unvan::kimlik).orElse(""));
    }

    /**
     * Kutup Sığınağı sayacı. Döneme bağlı ara sayaçtır (K-E11): {@code /dogalafet sifirla} sonrası (döngü işareti
     * {@link #sifirlamaDongusu} değişti) sıfırdan başlar; çevrim dışı oyuncunun eski birikimi de ilk okumada düşer.
     */
    public static KutupSayaci kutup(ServerPlayer oyuncu) {
        CompoundTag etiket = kok(oyuncu);
        if (etiket.getLong("kutupDongu") != sifirlamaDongusu(oyuncu)) {
            return new KutupSayaci(0, KutupSayaci.YOK);
        }
        return new KutupSayaci(etiket.getLong("kutup"),
                etiket.contains("kutupSon", Tag.TAG_LONG) ? etiket.getLong("kutupSon") : KutupSayaci.YOK);
    }

    public static void kutupYaz(ServerPlayer oyuncu, KutupSayaci sayac) {
        CompoundTag etiket = kok(oyuncu);
        etiket.putLong("kutupDongu", sifirlamaDongusu(oyuncu));
        etiket.putLong("kutup", sayac.birikim());
        if (sayac.son() == KutupSayaci.YOK) {
            etiket.remove("kutupSon");
        } else {
            etiket.putLong("kutupSon", sayac.son());
        }
    }

    /** Dünyanın son {@code sifirla} işareti ({@code DunyaDonemi.ekVeri("basarim")}); hiç sıfırlanmamışsa 0. */
    static long sifirlamaDongusu(ServerPlayer oyuncu) {
        return DunyaDonemi.al(oyuncu.server).ekVeri(EK_VERI).getLong(SIFIRLAMA);
    }

    /**
     * {@code DonemDegistiEvent(SIFIRLAMA)}: yeni döngü işareti (sıfırlama ek veriyi boşaltır). Oyun saatinden ve oturumda
     * kesin artan: aynı tick'teki iki {@code sifirla} de farklı işaret alır; hep sıfırdan farklıdır.
     */
    static void sifirlandi(DunyaDonemi donem, long oyunZamani) {
        sonIsaret = Math.max(oyunZamani + 1, sonIsaret + 1);
        donem.ekVeri(EK_VERI).putLong(SIFIRLAMA, sonIsaret);
        donem.ekVeriDegisti(EK_VERI);
    }

    /** Sunucu kapanınca: bir sonraki dünya kendi oyun saatinden başlar. */
    static void sunucuDurdu() {
        sonIsaret = 0;
    }

    public static long denizOlum(ServerPlayer oyuncu) {
        CompoundTag etiket = kok(oyuncu);
        return etiket.contains("denizOlum", Tag.TAG_LONG) ? etiket.getLong("denizOlum") : DenizTakibi.OLUM_YOK;
    }

    public static void denizOlumYaz(ServerPlayer oyuncu, long dongu) {
        kok(oyuncu).putLong("denizOlum", dongu);
    }

    public static AfetIzi afetIzi(ServerPlayer oyuncu) {
        AfetIzi iz = new AfetIzi();
        CompoundTag etiket = kok(oyuncu).getCompound("afet");
        if (!etiket.contains("no", Tag.TAG_LONG)) {
            return iz;
        }
        iz.numaraAyarla(etiket.getLong("no"));
        iz.alanda = etiket.getBoolean("alanda");
        iz.oldu = etiket.getBoolean("oldu");
        iz.suruklendi = etiket.getBoolean("suruklendi");
        iz.cukuraDustu = etiket.getBoolean("cukuraDustu");
        iz.yanginSaniye = etiket.getInt("yanginSaniye");
        iz.hasarAldi = etiket.getBoolean("hasarAldi");
        return iz;
    }

    public static void afetIziYaz(ServerPlayer oyuncu, AfetIzi iz) {
        CompoundTag etiket = new CompoundTag();
        etiket.putLong("no", iz.no());
        etiket.putBoolean("alanda", iz.alanda);
        etiket.putBoolean("oldu", iz.oldu);
        etiket.putBoolean("suruklendi", iz.suruklendi);
        etiket.putBoolean("cukuraDustu", iz.cukuraDustu);
        etiket.putInt("yanginSaniye", iz.yanginSaniye);
        etiket.putBoolean("hasarAldi", iz.hasarAldi);
        kok(oyuncu).put("afet", etiket);
    }

    /** Ölüm ya da End dönüşü ({@code PlayerEvent.Clone}): veri yeni oyuncu nesnesine taşınır. */
    static void kopyala(ServerPlayer eski, ServerPlayer yeni) {
        CompoundTag kalici = eski.getPersistentData();
        if (kalici.contains(ETIKET, Tag.TAG_COMPOUND)) {
            yeni.getPersistentData().put(ETIKET, kalici.getCompound(ETIKET).copy());
        }
    }
}
