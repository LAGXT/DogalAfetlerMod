package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetIzi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetKumesi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.DenizTakibi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Oyuncu başına kalıcı başarım verisi: {@code getPersistentData()} içinde {@value #ETIKET} (sıcaklığın
 * {@code VucutSicakligi} deseni). Capability yerine bu seçildi: veri yalnız sunucuda okunur, istemciye gitmez, küçüktür
 * (&lt; 200 bayt) ve oyuncu dosyasıyla kaydedilir; ölüm/boyut dönüşünde {@link #kopyala} ile taşınır (A-03). Alanlar:
 * {@code atlatilan} (sağ çıkılan afet türleri), {@code balik} (buz deliğinden toplam balık), {@code unvan},
 * {@code kutup}/{@code kutupSon}/{@code kutupDongu} (Kutup Sığınağı sayacı ve ait olduğu sıfırlama döngüsü),
 * {@code denizOlum} (yükselişte ölüm döngüsü), {@code afet} (süren afetteki iz: olay ve yedek tahmin
 * ipuçları ayrı, A-31). Dünya verisinde ({@code DunyaDonemi.ekVeri("basarim")}) yalnız sıfırlama işareti ve süren
 * afetin gelen ipuçları. Yalnız sunucu ana thread'inde.
 */
public final class BasarimVerisi {
    public static final String ETIKET = "dogalafetler_basarim";
    static final String EK_VERI = "basarim";
    private static final String SIFIRLAMA = "sifirlama";
    private static final String IPUCU_AFET = "ipucuAfet";
    private static final String IPUCU_BITLERI = "ipucuBitleri";
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
     * {@code DonemDegistiEvent(SIFIRLAMA)}: yeni döngü işareti (sıfırlama ek veriyi boşaltır). Oyun saatinden ve
     * oturumda kesin artan: aynı tick'teki iki {@code sifirla} de farklı işaret alır; hep sıfırdan farklıdır.
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
        iz.olayBitleri = etiket.getInt("olay");
        iz.tahminBitleri = etiket.getInt("tahmin");
        iz.yanginOlay = etiket.getInt("yanginOlay");
        iz.yanginTahmin = etiket.getInt("yanginTahmin");
        iz.yanginOlaySon = etiket.contains("yanginOlaySon", Tag.TAG_LONG) ? etiket.getLong("yanginOlaySon")
                : AfetIzi.SANIYE_YOK;
        iz.yanginTahminSon = etiket.contains("yanginTahminSon", Tag.TAG_LONG) ? etiket.getLong("yanginTahminSon")
                : AfetIzi.SANIYE_YOK;
        return iz;
    }

    public static void afetIziYaz(ServerPlayer oyuncu, AfetIzi iz) {
        CompoundTag etiket = new CompoundTag();
        etiket.putLong("no", iz.no());
        etiket.putBoolean("alanda", iz.alanda);
        etiket.putBoolean("oldu", iz.oldu);
        etiket.putInt("olay", iz.olayBitleri);
        etiket.putInt("tahmin", iz.tahminBitleri);
        etiket.putInt("yanginOlay", iz.yanginOlay);
        etiket.putInt("yanginTahmin", iz.yanginTahmin);
        if (iz.yanginOlaySon != AfetIzi.SANIYE_YOK) {
            etiket.putLong("yanginOlaySon", iz.yanginOlaySon);
        }
        if (iz.yanginTahminSon != AfetIzi.SANIYE_YOK) {
            etiket.putLong("yanginTahminSon", iz.yanginTahminSon);
        }
        kok(oyuncu).put("afet", etiket);
    }

    /**
     * {@code afetNo}'lu afet sürerken olayı gelmiş ipuçları ({@link Ipucu#bit()} maskesi; dünya verisinde, bütün
     * oyuncular için ortak, A-31). Başka bir afete aitse 0.
     */
    static int gelenIpuclari(MinecraftServer server, long afetNo) {
        CompoundTag ek = DunyaDonemi.al(server).ekVeri(EK_VERI);
        return ek.contains(IPUCU_AFET, Tag.TAG_LONG) && ek.getLong(IPUCU_AFET) == afetNo ? ek.getInt(IPUCU_BITLERI) : 0;
    }

    /** Afetten ipucu olayı geldi: bu afette o ipucunun tek kaynağı artık olaydır. */
    static void ipucuGeldi(MinecraftServer server, long afetNo, Ipucu ipucu) {
        int bitler = gelenIpuclari(server, afetNo);
        if ((bitler & ipucu.bit()) != 0) {
            return;
        }
        DunyaDonemi donem = DunyaDonemi.al(server);
        CompoundTag ek = donem.ekVeri(EK_VERI);
        ek.putLong(IPUCU_AFET, afetNo);
        ek.putInt(IPUCU_BITLERI, bitler | ipucu.bit());
        donem.ekVeriDegisti(EK_VERI);
    }

    /** Ölüm ya da End dönüşü ({@code PlayerEvent.Clone}): veri yeni oyuncu nesnesine taşınır. */
    static void kopyala(ServerPlayer eski, ServerPlayer yeni) {
        CompoundTag kalici = eski.getPersistentData();
        if (kalici.contains(ETIKET, Tag.TAG_COMPOUND)) {
            yeni.getPersistentData().put(ETIKET, kalici.getCompound(ETIKET).copy());
        }
    }
}
