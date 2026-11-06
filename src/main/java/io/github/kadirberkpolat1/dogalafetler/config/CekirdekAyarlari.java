package io.github.kadirberkpolat1.dogalafetler.config;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.GelisimKriteri;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.GelisimOlcutu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SiddetAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonumTipi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.TekSeferlikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.TpsKismasi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.ZamanlayiciAyarlari;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * SERVER config'in {@code cekirdek} bölümü: afet yöneticisi, şiddet, tek seferlik olaylar, hedef seçimi, TPS kısması, ağ
 * sıklığı ve geçici çarpan varsayılanları (madde 3, 4, 10, N17). Süreler ölçekli afet saati tick'idir (20 tick = 1 sn,
 * 24 000 tick = 1 Minecraft günü). Saf mantık sınıflarına kayıt (record) olarak verilir.
 */
public final class CekirdekAyarlari {
    private static AyarKaydi.Bolum<CekirdekAyarlari> bolum;

    private final ForgeConfigSpec.DoubleValue gunlukAfetOrani;
    private final ForgeConfigSpec.IntValue kontrolAraligiTick;
    private final ForgeConfigSpec.LongValue genelBeklemeTick;
    private final ForgeConfigSpec.DoubleValue siddetBeklemeCarpani;
    private final ForgeConfigSpec.LongValue ertelemeTick;
    private final ForgeConfigSpec.LongValue baslangicBeklemeTick;
    private final Map<AfetTuru, ForgeConfigSpec.IntValue> agirliklar = new EnumMap<>(AfetTuru.class);
    private final Map<AfetTuru, ForgeConfigSpec.LongValue> turBeklemeleri = new EnumMap<>(AfetTuru.class);

    private final ForgeConfigSpec.IntValue onUyariEnAzTick;
    private final ForgeConfigSpec.IntValue onUyariEnCokTick;
    private final ForgeConfigSpec.IntValue erkenBildirimTick;

    private final ForgeConfigSpec.IntValue seviye2Gunu;
    private final ForgeConfigSpec.IntValue seviye3Gunu;
    private final ForgeConfigSpec.IntValue seviye4Gunu;
    private final ForgeConfigSpec.IntValue seviye5Gunu;
    private final ForgeConfigSpec.DoubleValue nadirlikBaslangic;
    private final ForgeConfigSpec.DoubleValue nadirlikSon;
    private final ForgeConfigSpec.DoubleValue nadirlikGecisGunu;
    private final ForgeConfigSpec.DoubleValue acilisRampaGunu;
    private final ForgeConfigSpec.DoubleValue acilisBaslangicOrani;
    private final ForgeConfigSpec.BooleanValue gelisimDahil;
    private final ForgeConfigSpec.DoubleValue puanBasinaGun;

    private final ForgeConfigSpec.EnumValue<GelisimOlcutu> gelisimOlcutu;
    private final ForgeConfigSpec.BooleanValue gunesPatlamasiTekrarlanabilir;
    private final ForgeConfigSpec.EnumValue<GelisimKriteri> gunesPatlamasiKriteri;
    private final ForgeConfigSpec.IntValue gunesPatlamasiMinGun;
    private final ForgeConfigSpec.DoubleValue gunesPatlamasiPuanEsigi;
    private final ForgeConfigSpec.EnumValue<GelisimKriteri> ayCarpmasiKriteri;
    private final ForgeConfigSpec.IntValue ayCarpmasiMinGun;
    private final ForgeConfigSpec.DoubleValue ayCarpmasiPuanEsigi;

    private final ForgeConfigSpec.ConfigValue<List<? extends String>> boyutlar;
    private final ForgeConfigSpec.IntValue sirenEkYaricap;
    private final ForgeConfigSpec.IntValue ardisikAfetMesafesi;

    private final ForgeConfigSpec.DoubleValue tpsEsigi;
    private final ForgeConfigSpec.DoubleValue tpsKritikEsigi;
    private final ForgeConfigSpec.DoubleValue tpsToparlanmaEsigi;
    private final ForgeConfigSpec.DoubleValue tpsKritikToparlanmaEsigi;

    private final ForgeConfigSpec.IntValue afetPaketAraligiTick;
    private final ForgeConfigSpec.IntValue donemPaketAraligiTick;
    private final ForgeConfigSpec.IntValue donemTazelemeTick;

    private final ForgeConfigSpec.DoubleValue tsunamiCarpani;
    private final ForgeConfigSpec.LongValue tsunamiCarpanSuresiTick;
    private final ForgeConfigSpec.LongValue tsunamiSonumTick;
    private final ForgeConfigSpec.EnumValue<SonumTipi> tsunamiSonumTipi;

    private CekirdekAyarlari(ForgeConfigSpec.Builder k) {
        ZamanlayiciAyarlari z = ZamanlayiciAyarlari.VARSAYILAN;
        k.comment("Afet zamanlayıcısı (madde 4). Sabit takvim yoktur; her afet olasılıkla ve bekleme kurallarıyla seçilir.")
                .push("zamanlayici");
        gunlukAfetOrani = k.comment("Bekleme dışındayken bir Minecraft gününde beklenen afet sayısı (sekiz türün hepsi",
                "seçilebilirken). Oyuncu sayısından bağımsızdır; afetSikligi gamerule'ü bunu çarpar.")
                .defineInRange("gunlukAfetOrani", z.gunlukAfetOrani(), 0.0, 100.0);
        kontrolAraligiTick = k.comment("Zamanlayıcının kaç tick'te bir zar attığı (olasılık aralığa göre ölçeklenir).")
                .defineInRange("kontrolAraligiTick", 20, 1, 1200);
        genelBeklemeTick = k.comment("Bir afet bittikten sonra yenisi seçilmeden geçmesi gereken en az süre.")
                .defineInRange("genelBeklemeTick", z.genelBeklemeTick(), 0L, 100_000_000L);
        siddetBeklemeCarpani = k.comment("Şiddetli afetten sonra bekleme uzaması: çarpan = 1 + bu × max(0, seviye - 2);",
                "seviyesiz güneş patlaması ve ay çarpması seviye 5 sayılır.")
                .defineInRange("siddetBeklemeCarpani", z.siddetBeklemeCarpani(), 0.0, 10.0);
        ertelemeTick = k.comment("Hedefte uygun yer yoksa (N14) türün yeniden denenmesi için beklenen süre.")
                .defineInRange("ertelemeTick", z.ertelemeTick(), 0L, 100_000_000L);
        baslangicBeklemeTick = k.comment("Yeni dünyada ilk afetten önce geçmesi gereken süre.")
                .defineInRange("baslangicBeklemeTick", z.baslangicBeklemeTick(), 0L, 100_000_000L);
        k.pop();

        k.comment("Afet başına olasılık ağırlığı ve kendi beklemesi. Nadirlik sırası (madde 4): altı sık afet birbirine yakın,",
                "ay çarpması nadir, güneş patlaması en nadir. Ağırlık 0 olan afet rastgele seçilmez.").push("afetler");
        for (AfetTuru tur : AfetTuru.values()) {
            k.push(tur.kimlik());
            agirliklar.put(tur, k.comment("Olasılık ağırlığı.")
                    .defineInRange("agirlik", ZamanlayiciAyarlari.varsayilanAgirlik(tur), 0, 10_000));
            turBeklemeleri.put(tur, k.comment("Aynı afetin yeniden seçilebilmesi için beklenen süre.")
                    .defineInRange("beklemeTick", ZamanlayiciAyarlari.varsayilanTurBeklemesi(tur), 0L, 1_000_000_000L));
            k.pop();
        }
        k.pop();

        k.comment("Gizli ön-uyarı penceresi (madde 4): afet seçildikten sonra siren çalmadan önceki süre.").push("onUyari");
        onUyariEnAzTick = k.comment("Pencerenin en kısa süresi (900 tick = 45 sn).")
                .defineInRange("enAzTick", 900, 0, 72_000);
        onUyariEnCokTick = k.comment("Pencerenin en uzun süresi (1800 tick = 90 sn).")
                .defineInRange("enCokTick", 1800, 0, 72_000);
        erkenBildirimTick = k.comment("Pencerenin önüne eklenen erken bölüm: yalnız Hassas Sismograf görür (madde 13b).")
                .defineInRange("erkenBildirimTick", 600, 0, 72_000);
        k.pop();

        SiddetAyarlari s = SiddetAyarlari.VARSAYILAN;
        k.comment("Afet şiddet seviyeleri (madde 10).").push("siddet");
        seviye2Gunu = k.comment("Seviye 2'nin açıldığı oyun günü.").defineInRange("seviye2Gunu", s.seviye2Gunu(), 0, 1_000_000);
        seviye3Gunu = k.comment("Seviye 3'ün açıldığı oyun günü.").defineInRange("seviye3Gunu", s.seviye3Gunu(), 0, 1_000_000);
        seviye4Gunu = k.comment("Seviye 4'ün açıldığı oyun günü.").defineInRange("seviye4Gunu", s.seviye4Gunu(), 0, 1_000_000);
        seviye5Gunu = k.comment("Seviye 5'in açıldığı oyun günü.").defineInRange("seviye5Gunu", s.seviye5Gunu(), 0, 1_000_000);
        nadirlikBaslangic = k.comment("Yüksek seviye nadirlik katsayısı k (oyun başında): seviye L'nin taban ağırlığı k^(L-1).",
                "k < 1 olduğu sürece yüksek seviye her zaman daha nadirdir.")
                .defineInRange("nadirlikBaslangic", s.nadirlikBaslangic(), 0.01, 1.0);
        nadirlikSon = k.comment("Nadirlik katsayısının ulaştığı son değer (ağırlık yüksek seviyelere kayar).")
                .defineInRange("nadirlikSon", s.nadirlikSon(), 0.01, 1.0);
        nadirlikGecisGunu = k.comment("Katsayının başlangıçtan son değere vardığı oyun günü.")
                .defineInRange("nadirlikGecisGunu", s.nadirlikGecisGunu(), 0.0, 1_000_000.0);
        acilisRampaGunu = k.comment("Yeni açılan seviyenin tam ağırlığa ulaştığı gün sayısı (hemen baskın olmaz).")
                .defineInRange("acilisRampaGunu", s.acilisRampaGunu(), 0.0, 1_000_000.0);
        acilisBaslangicOrani = k.comment("Yeni açılan seviyenin açıldığı gündeki ağırlık oranı.")
                .defineInRange("acilisBaslangicOrani", s.acilisBaslangicOrani(), 0.0, 1.0);
        gelisimDahil = k.comment("Açıkken şiddet için kullanılan gün oyuncu gelişim puanıyla sınırlanır (madde 10, isteğe bağlı).")
                .define("gelisimDahil", s.gelisimDahil());
        puanBasinaGun = k.comment("Gelişim kriteri açıkken bir gelişim puanının kaç oyun günü sayıldığı.")
                .defineInRange("puanBasinaGun", s.puanBasinaGun(), 0.0, 1000.0);
        k.pop();

        TekSeferlikAyarlari t = TekSeferlikAyarlari.VARSAYILAN;
        k.comment("Tek seferlik ve geç oyun afetleri (madde 3, 4, bölüm D ve H).").push("tekSeferlik");
        gelisimOlcutu = k.comment("Gelişim puanının oyunculardan nasıl toplandığı: ORTALAMA (çevrim içi oyuncuların ortalaması)",
                "ya da EN_GELISMIS. Oyuncu puanı = XP seviyesi + zırh puanı (tam demir zırh 15, elmas 20).")
                .defineEnum("gelisimOlcutu", GelisimOlcutu.ORTALAMA);
        gunesPatlamasiTekrarlanabilir = k.comment("Kızıl cüce dönemine ulaşıldıktan sonra güneş patlaması yeniden seçilebilir mi.")
                .define("gunesPatlamasiTekrarlanabilir", t.gunesPatlamasiTekrarlanabilir());
        gunesPatlamasiKriteri = k.comment("Güneş patlamasının kapısı: GUN (oyun günü ≥ gunesPatlamasiMinGun), PUAN (gelişim puanı ≥",
                "gunesPatlamasiPuanEsigi), IKISI_DE ya da HERHANGI.")
                .defineEnum("gunesPatlamasiKriteri", t.gunesPatlamasiKriteri());
        gunesPatlamasiMinGun = k.comment("Güneş patlaması için gereken oyun günü (GUN, IKISI_DE, HERHANGI).")
                .defineInRange("gunesPatlamasiMinGun", t.gunesPatlamasiMinGun(), 0, 1_000_000);
        gunesPatlamasiPuanEsigi = k.comment("Güneş patlaması için gereken gelişim puanı (PUAN, IKISI_DE, HERHANGI).")
                .defineInRange("gunesPatlamasiPuanEsigi", t.gunesPatlamasiPuanEsigi(), 0.0, 10_000.0);
        ayCarpmasiKriteri = k.comment("Ay çarpmasının kapısı: GUN, PUAN, IKISI_DE ya da HERHANGI (güneş patlamasındaki gibi).")
                .defineEnum("ayCarpmasiKriteri", t.ayCarpmasiKriteri());
        ayCarpmasiMinGun = k.comment("Ay çarpması için gereken oyun günü.")
                .defineInRange("ayCarpmasiMinGun", t.ayCarpmasiMinGun(), 0, 1_000_000);
        ayCarpmasiPuanEsigi = k.comment("Ay çarpması için gereken gelişim puanı.")
                .defineInRange("ayCarpmasiPuanEsigi", t.ayCarpmasiPuanEsigi(), 0.0, 10_000.0);
        k.pop();

        k.comment("Hedef oyuncu ve etki alanı (madde 3, N11).").push("hedef");
        boyutlar = k.comment("Afetlerin oluşabildiği boyutlar. Başka boyuttaki oyuncular siren ve gökyüzü durumunu yine alır.")
                .defineListAllowEmpty(List.of("boyutlar"), () -> List.of("minecraft:overworld"),
                        o -> o instanceof String metin && ResourceLocation.tryParse(metin) != null);
        sirenEkYaricap = k.comment("Bölgesel afette etki alanının bu kadar blok dışındaki oyuncular da (afetin boyutunda) sireni",
                "duyar ve afet durumunu alır. Sunucu geneli afette siren her boyuttaki oyuncuya gider (N11).")
                .defineInRange("sirenEkYaricap", 64, 0, 4096);
        ardisikAfetMesafesi = k.comment("Son afetin merkezine bu kadar yakın oyuncular hedef seçiminde sona bırakılır.")
                .defineInRange("ardisikAfetMesafesi", 128, 0, 100_000);
        k.pop();

        k.comment("TPS kısması (madde 3): tüm sistemler tek ölçerden okur.").push("tps");
        TpsKismasi.Esikler e = TpsKismasi.Esikler.VARSAYILAN;
        tpsEsigi = k.comment("Bu TPS'in altında ağır işlerin bütçesi yarıya iner.").defineInRange("esik", e.esik(), 1.0, 20.0);
        tpsKritikEsigi = k.comment("Bu TPS'in altında bütçe çeyreğe iner.")
                .defineInRange("kritikEsik", e.kritikEsik(), 1.0, 20.0);
        tpsToparlanmaEsigi = k.comment("Kısılmış bütçe TPS bu değere çıkınca tama döner (histerezis).")
                .defineInRange("toparlanmaEsigi", e.toparlanma(), 1.0, 20.0);
        tpsKritikToparlanmaEsigi = k.comment("Çeyrek bütçe TPS bu değere çıkınca yarıya döner (histerezis).")
                .defineInRange("kritikToparlanmaEsigi", e.kritikToparlanma(), 1.0, 20.0);
        k.pop();

        k.comment("Ağ sıklığı (madde 3: durum değişiminde ve düşük sıklıkta paket).").push("ag");
        afetPaketAraligiTick = k.comment("Afet durum paketi ve oyuncu takibi aralığı (20 = 1 Hz).")
                .defineInRange("afetPaketAraligiTick", 20, 1, 200);
        donemPaketAraligiTick = k.comment("Dönem değişiklikleri bu aralıkta toplanarak gönderilir (evre ve ay değişimi hemen).")
                .defineInRange("donemPaketAraligiTick", 20, 1, 200);
        donemTazelemeTick = k.comment("Değişiklik olmasa da dönem paketi bu aralıkta yenilenir; istemcinin afet saati sunucu",
                "saatine hizalanır (20 = 1 Hz). En çok 60: istemci saati son paketten en çok 60 tick ileri tahmin eder; daha",
                "seyrek tazelemede gökyüzü geçişleri donar ve sıçrardı (K-C35).")
                .defineInRange("donemTazelemeTick", 20, 20, 60);
        k.pop();

        k.comment("Geçici olasılık çarpanı varsayılanları (madde 4, bölüm H): ay çarpmasından sonra tsunami.").push("carpan");
        tsunamiCarpani = k.comment("Ay çarpmasından sonra tsunami olasılığının çarpanı.")
                .defineInRange("tsunamiCarpani", 3.5, 0.01, 100.0);
        tsunamiCarpanSuresiTick = k.comment("Çarpanın tam değerde kaldığı süre (72 000 tick = 3 gün).")
                .defineInRange("tsunamiCarpanSuresiTick", 72_000L, 0L, 100_000_000L);
        tsunamiSonumTick = k.comment("Çarpanın normale dönme süresi (48 000 tick = 2 gün).")
                .defineInRange("tsunamiSonumTick", 48_000L, 0L, 100_000_000L);
        tsunamiSonumTipi = k.comment("Sönüm biçimi: DOGRUSAL ya da USTEL.").defineEnum("tsunamiSonumTipi", SonumTipi.DOGRUSAL);
        k.pop();
    }

    /** Bölümü config kayıt kancasına ekler; mod kurucusunda {@code AyarKaydi.kaydet}'ten önce çağrılır. */
    public static void kaydet() {
        bolum = AyarKaydi.sunucuBolumu("cekirdek", "Çekirdek: afet yöneticisi ve dünya dönemi", CekirdekAyarlari::new);
    }

    public static CekirdekAyarlari al() {
        if (bolum == null) {
            throw new IllegalStateException("CekirdekAyarlari.kaydet() çağrılmadı");
        }
        return bolum.get();
    }

    public ZamanlayiciAyarlari zamanlayici() {
        Map<AfetTuru, Integer> a = new EnumMap<>(AfetTuru.class);
        Map<AfetTuru, Long> b = new EnumMap<>(AfetTuru.class);
        for (AfetTuru tur : AfetTuru.values()) {
            a.put(tur, agirliklar.get(tur).get());
            b.put(tur, turBeklemeleri.get(tur).get());
        }
        return new ZamanlayiciAyarlari(gunlukAfetOrani.get(), genelBeklemeTick.get(), siddetBeklemeCarpani.get(),
                ertelemeTick.get(), baslangicBeklemeTick.get(), a, b);
    }

    public SiddetAyarlari siddet() {
        return new SiddetAyarlari(seviye2Gunu.get(), seviye3Gunu.get(), seviye4Gunu.get(), seviye5Gunu.get(),
                nadirlikBaslangic.get(), nadirlikSon.get(), nadirlikGecisGunu.get(), acilisRampaGunu.get(),
                acilisBaslangicOrani.get(), gelisimDahil.get(), puanBasinaGun.get());
    }

    public TekSeferlikAyarlari tekSeferlik() {
        return new TekSeferlikAyarlari(gunesPatlamasiTekrarlanabilir.get(), gunesPatlamasiKriteri.get(),
                gunesPatlamasiMinGun.get(), gunesPatlamasiPuanEsigi.get(), ayCarpmasiKriteri.get(), ayCarpmasiMinGun.get(),
                ayCarpmasiPuanEsigi.get());
    }

    public GelisimOlcutu gelisimOlcutu() {
        return gelisimOlcutu.get();
    }

    public int kontrolAraligiTick() {
        return kontrolAraligiTick.get();
    }

    public long onUyariEnAzTick() {
        return onUyariEnAzTick.get();
    }

    public long onUyariEnCokTick() {
        return onUyariEnCokTick.get();
    }

    public long erkenBildirimTick() {
        return erkenBildirimTick.get();
    }

    /** Afetlerin oluşabildiği boyutların kimlikleri (N11). */
    public Set<ResourceLocation> boyutlar() {
        Set<ResourceLocation> sonuc = new HashSet<>();
        for (String metin : boyutlar.get()) {
            ResourceLocation konum = ResourceLocation.tryParse(metin);
            if (konum != null) {
                sonuc.add(konum);
            }
        }
        return sonuc;
    }

    public double sirenEkYaricap() {
        return sirenEkYaricap.get();
    }

    public double ardisikAfetMesafesi() {
        return ardisikAfetMesafesi.get();
    }

    public TpsKismasi.Esikler tpsEsikleri() {
        return new TpsKismasi.Esikler(tpsEsigi.get(), tpsKritikEsigi.get(), tpsToparlanmaEsigi.get(),
                tpsKritikToparlanmaEsigi.get());
    }

    public int afetPaketAraligiTick() {
        return afetPaketAraligiTick.get();
    }

    public int donemPaketAraligiTick() {
        return donemPaketAraligiTick.get();
    }

    public int donemTazelemeTick() {
        return donemTazelemeTick.get();
    }

    public double tsunamiCarpani() {
        return tsunamiCarpani.get();
    }

    public long tsunamiCarpanSuresiTick() {
        return tsunamiCarpanSuresiTick.get();
    }

    public long tsunamiSonumTick() {
        return tsunamiSonumTick.get();
    }

    public SonumTipi tsunamiSonumTipi() {
        return tsunamiSonumTipi.get();
    }
}
