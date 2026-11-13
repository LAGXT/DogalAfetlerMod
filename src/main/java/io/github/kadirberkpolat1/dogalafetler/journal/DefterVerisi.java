package io.github.kadirberkpolat1.dogalafetler.journal;

import io.github.kadirberkpolat1.dogalafetler.afet.tsunami.TsunamiSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikcilikEtiketleri;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.journal.DefterPaketi.EsyaKaydi;
import io.github.kadirberkpolat1.dogalafetler.journal.DefterPaketi.SonAfet;
import io.github.kadirberkpolat1.dogalafetler.journal.DefterPaketi.TurKaydi;
import io.github.kadirberkpolat1.dogalafetler.journal.mantik.GunHesabi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Esikler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.VucutOlcegi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Oyuncu başına kalıcı defter verisi: {@code getPersistentData()} içinde {@value #ETIKET} ({@code BasarimVerisi} ve
 * {@code VucutSicakligi} deseni). Capability yerine bu seçildi: veri yalnızca sunucuda okunur, istemciye yalnız defter
 * açılırken {@link DefterPaketi} olarak gider, oyuncu dosyasıyla kaydedilir ve ölümün/boyut dönüşünün ötesinde
 * {@link #kopyala} ile taşınır. Alanlar:
 * <ul>
 * <li>{@code afet}: tür kimliği → {@code karsi} (karşılaşılan), {@code atl} (atlatılan), {@code sev} (S1…S5 atlatılan,
 * 5 tam sayı), {@code olum} (o afetin etki alanında ölüm), {@code ilkGun}/{@code ilkTarih} (ilk karşılaşma: oyun günü ve
 * gerçek zaman, ms);</li>
 * <li>{@code enSev}/{@code enTur}: atlatılan en yüksek seviye ve türü; {@code dalga}: karşılaşılan tsunami dalgaları;</li>
 * <li>{@code son}: son {@value #SON_AFET_SAYISI} atlatılan afet (en yenisi başta);</li>
 * <li>{@code bas}/{@code en}: yaşamın başlangıç oyun günü ve kayıtlı en uzun yaşam (gün);</li>
 * <li>{@code sogukC}/{@code sicakC}: gözlenen en düşük/en yüksek vücut sıcaklığı (°C; yoksa anahtar yok);</li>
 * <li>{@code parca}/{@code cekirdek}: toplanan Meteorit Parçası / Kozmik Çekirdek; {@code delik}: açılan buz deliği;
 * {@code tutus}/{@code buzBalik}: buz deliğinden çekme ve buz balığı sayısı;</li>
 * <li>{@code esya}: eşya kimliği → tutulan adet (en çok {@value #EN_COK_ESYA} tür; dolunca yeni tür eklenmez).</li>
 * </ul>
 * Ölüm sayısı, mesafe ve olta tutuşları vanilla istatistiklerinden okunur (ayrıca sayılmaz). Yalnız sunucu ana thread'inde.
 */
public final class DefterVerisi {
    public static final String ETIKET = "dogalafetler_defter";
    public static final int SON_AFET_SAYISI = 5;
    static final int EN_COK_ESYA = 256;
    private static final int SEVIYE_SAYISI = 5;

    private DefterVerisi() {
    }

    private static CompoundTag kok(ServerPlayer oyuncu) {
        CompoundTag kalici = oyuncu.getPersistentData();
        if (!kalici.contains(ETIKET, Tag.TAG_COMPOUND)) {
            kalici.put(ETIKET, new CompoundTag());
        }
        return kalici.getCompound(ETIKET);
    }

    private static CompoundTag alt(CompoundTag ust, String ad) {
        if (!ust.contains(ad, Tag.TAG_COMPOUND)) {
            ust.put(ad, new CompoundTag());
        }
        return ust.getCompound(ad);
    }

    private static int topla(int a, int b) {
        return (int) Math.min(Integer.MAX_VALUE, (long) a + Math.max(0, b));
    }

    private static int[] seviyeDizisi(CompoundTag tur) {
        int[] dizi = tur.getIntArray("sev");
        return dizi.length == SEVIYE_SAYISI ? dizi : new int[SEVIYE_SAYISI];
    }

    // ---------------------------------------------------------------- yazma

    /**
     * Afet bitti ve oyuncu etkilenmişti ({@code AfetBittiEvent.basarimSayilir()} olaylarında). Karşılaşma her etkilenen
     * oyuncuya yazılır; ölümünü görmeyen, bitişte çevrim içi olan {@code atlatti} oyuncuya atlatma (tür, seviye, en yüksek,
     * son afetler) eklenir.
     */
    static void afetBitti(ServerPlayer oyuncu, AfetTuru tur, int seviye, boolean atlatti, long dunyaGunu, long tarih) {
        CompoundTag k = kok(oyuncu);
        CompoundTag t = alt(alt(k, "afet"), tur.kimlik());
        t.putInt("karsi", topla(t.getInt("karsi"), 1));
        if (!t.contains("ilkGun", Tag.TAG_LONG)) {
            t.putLong("ilkGun", dunyaGunu);
            t.putLong("ilkTarih", tarih);
        }
        if (tur == AfetTuru.TSUNAMI) {
            k.putInt("dalga", topla(k.getInt("dalga"), TsunamiSeviyesi.al(seviye).ikinciDalga() ? 2 : 1));
        }
        if (!atlatti) {
            return;
        }
        t.putInt("atl", topla(t.getInt("atl"), 1));
        if (tur.seviyeli() && seviye >= AfetTuru.EN_DUSUK_SEVIYE) {
            int sira = Math.min(seviye, AfetTuru.EN_YUKSEK_SEVIYE) - 1;
            int[] dizi = seviyeDizisi(t);
            dizi[sira] = topla(dizi[sira], 1);
            t.putIntArray("sev", dizi);
            if (seviye >= k.getInt("enSev")) {
                k.putInt("enSev", seviye);
                k.putString("enTur", tur.kimlik());
            }
        }
        ListTag son = k.getList("son", Tag.TAG_COMPOUND);
        CompoundTag satir = new CompoundTag();
        satir.putString("t", tur.kimlik());
        satir.putInt("s", seviye);
        satir.putLong("g", dunyaGunu);
        son.add(0, satir);
        while (son.size() > SON_AFET_SAYISI) {
            son.remove(son.size() - 1);
        }
        k.put("son", son);
    }

    /** Oyuncu {@code tur} afetinin etki alanında, afet sürerken öldü. */
    static void afetteOldu(ServerPlayer oyuncu, AfetTuru tur) {
        CompoundTag t = alt(alt(kok(oyuncu), "afet"), tur.kimlik());
        t.putInt("olum", topla(t.getInt("olum"), 1));
    }

    /** Ölüm: süren yaşam kaydedilir (en uzun yaşam güncellenir), yeni yaşam bugün başlar. */
    static void oldu(ServerPlayer oyuncu, long dunyaGunu) {
        CompoundTag k = kok(oyuncu);
        yasamiBaslat(oyuncu, k, dunyaGunu);
        long yasam = GunHesabi.yasamGunu(dunyaGunu, k.getLong("bas"));
        k.putLong("en", GunHesabi.enUzun(k.getLong("en"), yasam));
        k.putLong("bas", dunyaGunu);
    }

    /** Yaşamın başlangıç günü henüz yoksa vanilla "son ölümden beri" sayacından kestirir. */
    private static void yasamiBaslat(ServerPlayer oyuncu, CompoundTag k, long dunyaGunu) {
        if (!k.contains("bas", Tag.TAG_LONG)) {
            int tick = oyuncu.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
            k.putLong("bas", GunHesabi.baslangicKestirimi(dunyaGunu, tick));
        }
    }

    /** Buz deliğinden bir çekme: {@code buzBalik} tutulan buz balığı adedi (0 olabilir: çöp ya da hazine). */
    static void buzTutusu(ServerPlayer oyuncu, int buzBalik) {
        CompoundTag k = kok(oyuncu);
        k.putInt("tutus", topla(k.getInt("tutus"), 1));
        k.putInt("buzBalik", topla(k.getInt("buzBalik"), buzBalik));
    }

    static void delikAcildi(ServerPlayer oyuncu) {
        CompoundTag k = kok(oyuncu);
        k.putInt("delik", topla(k.getInt("delik"), 1));
    }

    /** Olta ile tutulan yiğınlar eşya sayacına işlenir ({@value #EN_COK_ESYA} tür sınırı). */
    static void esyalarTutuldu(ServerPlayer oyuncu, List<ItemStack> yiginlar) {
        CompoundTag esyalar = alt(kok(oyuncu), "esya");
        for (ItemStack yigin : yiginlar) {
            if (yigin.isEmpty()) {
                continue;
            }
            ResourceLocation kimlik = ForgeRegistries.ITEMS.getKey(yigin.getItem());
            if (kimlik == null) {
                continue;
            }
            String anahtar = kimlik.toString();
            if (!esyalar.contains(anahtar, Tag.TAG_INT) && esyalar.size() >= EN_COK_ESYA) {
                continue;
            }
            esyalar.putInt(anahtar, topla(esyalar.getInt(anahtar), yigin.getCount()));
        }
    }

    /** Meteorit Parçası ({@code cekirdek} false) ya da Kozmik Çekirdek topladı. */
    static void meteorToplandi(ServerPlayer oyuncu, boolean cekirdek, int adet) {
        CompoundTag k = kok(oyuncu);
        String anahtar = cekirdek ? "cekirdek" : "parca";
        k.putInt(anahtar, topla(k.getInt(anahtar), adet));
    }

    /** Gözlenen vücut sıcaklığı (°C) en düşük/en yüksek kaydı genişletiyorsa yazılır (0,01 °C'ye yuvarlanır). */
    static void sicaklikOrnegi(ServerPlayer oyuncu, double santigrat) {
        double c = Math.round(santigrat * 100.0) / 100.0;
        CompoundTag k = kok(oyuncu);
        if (!k.contains("sogukC", Tag.TAG_DOUBLE) || c < k.getDouble("sogukC")) {
            k.putDouble("sogukC", c);
        }
        if (!k.contains("sicakC", Tag.TAG_DOUBLE) || c > k.getDouble("sicakC")) {
            k.putDouble("sicakC", c);
        }
    }

    /** Ölümde ve boyut dönüşünde veri yeni oyuncu nesnesine taşınır. */
    static void kopyala(ServerPlayer eski, ServerPlayer yeni) {
        CompoundTag kalici = eski.getPersistentData();
        if (kalici.contains(ETIKET, Tag.TAG_COMPOUND)) {
            yeni.getPersistentData().put(ETIKET, kalici.getCompound(ETIKET).copy());
        }
    }

    // ---------------------------------------------------------------- okuma

    /** Defterin gösterdiği tüm değerler (yalnız okuma; yaşamın başlangıcı ilk bakışta kaydedilir). */
    static DefterPaketi ozet(ServerPlayer oyuncu) {
        long dunya = AfetYoneticisi.al(oyuncu.server).oyunGunu();
        CompoundTag k = kok(oyuncu);
        yasamiBaslat(oyuncu, k, dunya);
        long yasam = GunHesabi.yasamGunu(dunya, k.getLong("bas"));
        long enUzun = GunHesabi.enUzun(k.getLong("en"), yasam);
        ServerStatsCounter istatistik = oyuncu.getStats();

        MesafeGrubu[] gruplar = MesafeGrubu.values();
        long[] mesafe = new long[gruplar.length];
        for (int i = 0; i < gruplar.length; i++) {
            mesafe[i] = gruplar[i].santimetre(istatistik);
        }

        CompoundTag afetler = k.getCompound("afet");
        List<TurKaydi> turler = new ArrayList<>(AfetTuru.values().length);
        for (AfetTuru tur : AfetTuru.values()) {
            CompoundTag t = afetler.getCompound(tur.kimlik());
            turler.add(new TurKaydi(t.getInt("atl"), t.getInt("karsi"), t.getInt("olum"), seviyeDizisi(t),
                    t.contains("ilkGun", Tag.TAG_LONG) ? t.getLong("ilkGun") : -1, t.getLong("ilkTarih")));
        }

        List<SonAfet> son = new ArrayList<>(SON_AFET_SAYISI);
        ListTag sonListe = k.getList("son", Tag.TAG_COMPOUND);
        for (int i = 0; i < sonListe.size() && son.size() < SON_AFET_SAYISI; i++) {
            CompoundTag satir = sonListe.getCompound(i);
            AfetTuru tur = AfetTuru.kimlikIle(satir.getString("t")).orElse(null);
            if (tur != null) {
                son.add(new SonAfet(tur.ordinal(), satir.getInt("s"), satir.getLong("g")));
            }
        }

        CompoundTag esyaEtiketi = k.getCompound("esya");
        List<EsyaKaydi> esyalar = new ArrayList<>(esyaEtiketi.size());
        int balikToplam = 0;
        int esyaToplam = 0;
        for (String anahtar : esyaEtiketi.getAllKeys()) {
            ResourceLocation kimlik = ResourceLocation.tryParse(anahtar);
            Item esya = kimlik == null ? null : ForgeRegistries.ITEMS.getValue(kimlik);
            if (esya == null || esya == Items.AIR) {
                continue;
            }
            int adet = esyaEtiketi.getInt(anahtar);
            boolean balik = balikMi(esya);
            if (balik) {
                balikToplam = topla(balikToplam, adet);
            } else {
                esyaToplam = topla(esyaToplam, adet);
            }
            esyalar.add(new EsyaKaydi(anahtar, adet, balik));
        }
        esyalar.sort(Comparator.comparingInt(EsyaKaydi::adet).reversed().thenComparing(EsyaKaydi::kimlik));
        if (esyalar.size() > DefterPaketi.EN_COK_ESYA) {
            esyalar = new ArrayList<>(esyalar.subList(0, DefterPaketi.EN_COK_ESYA));
        }

        Esikler e = SicaklikAyarlari.al().esikler();
        float[] esik = {(float) VucutOlcegi.santigrat(e.donmaHasari()), (float) VucutOlcegi.santigrat(e.cokSoguk()),
                (float) VucutOlcegi.santigrat(e.soguk()), (float) VucutOlcegi.santigrat(e.sicak()),
                (float) VucutOlcegi.santigrat(e.cokSicak()), (float) VucutOlcegi.santigrat(e.yanmaHasari())};

        int enYuksekTur = AfetTuru.kimlikIle(k.getString("enTur")).map(Enum::ordinal).orElse(-1);
        return new DefterPaketi(dunya, yasam, enUzun, istatistik.getValue(Stats.CUSTOM.get(Stats.DEATHS)), mesafe, turler,
                k.getInt("enSev"), enYuksekTur, k.getInt("dalga"),
                k.contains("sogukC", Tag.TAG_DOUBLE) ? (float) k.getDouble("sogukC") : Float.NaN,
                k.contains("sicakC", Tag.TAG_DOUBLE) ? (float) k.getDouble("sicakC") : Float.NaN, k.getInt("parca"),
                k.getInt("cekirdek"), k.getInt("delik"), k.getInt("tutus"), k.getInt("buzBalik"),
                istatistik.getValue(Stats.CUSTOM.get(Stats.FISH_CAUGHT)), balikToplam, esyaToplam, son, esyalar, esik);
    }

    /** Balık: vanilla balık etiketi ya da modun buz balığı etiketleri (çiğ/pişmiş). */
    private static boolean balikMi(Item esya) {
        ItemStack yigin = new ItemStack(esya);
        return yigin.is(ItemTags.FISHES) || yigin.is(BalikcilikEtiketleri.CIG_KUTUP_BALIGI)
                || yigin.is(BalikcilikEtiketleri.PISMIS_KUTUP_BALIGI);
    }
}
