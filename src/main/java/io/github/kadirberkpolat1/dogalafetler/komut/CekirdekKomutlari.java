package io.github.kadirberkpolat1.dogalafetler.komut;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import io.github.kadirberkpolat1.dogalafetler.config.CekirdekAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetBaglami;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekCevirileri;
import io.github.kadirberkpolat1.dogalafetler.core.DenizSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.GecmisKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.SiklikBilgisi;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.GeciciCarpan;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.GunSaatiPenceresi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.OyunGunu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SeviyeDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SiddetSecici;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Çekirdeğin {@code /dogalafet} alt komutlarının işleri: {@code durum}, {@code start}, {@code stop}, {@code gun}, {@code hiz},
 * {@code siklik}, {@code sifirla}, {@code asama ileri} (KOMUTLAR, N18) ve alt komutsuz kökün kullanım listesi. {@code durum}
 * aktif ve durdurulmuş dönem süreçlerinin kendi satırını ({@link DonemSureci#durumSatiri}: aşama, kalan gün) ayrı satır yazar.
 * {@code stop} ve {@code asama ileri} aktif afet yokken dönem süreçlerine ({@link DonemSureci}) gider; {@code stop} afeti
 * durdururken süren süreçleri yalnız hatırlatır (ikinci {@code stop} onları durdurur). Çıktılar yalnız komutu yazan OP'ye
 * gider (N6) ve dil anahtarlarıyla yazılır. Başarılı komut 1, reddedilen 0 döner.
 */
public final class CekirdekKomutlari {
    private static final String ON = "commands.dogalafetler.";
    public static final String DURUM_BASLIK = ON + "durum.baslik";
    public static final String DURUM_AFET_YOK = ON + "durum.afet_yok";
    public static final String DURUM_AFET = ON + "durum.afet";
    public static final String DURUM_AFET_KONUM = ON + "durum.afet_konum";
    public static final String DURUM_GUN = ON + "durum.gun";
    public static final String DURUM_BEKLEME = ON + "durum.bekleme";
    public static final String DURUM_CARPANLAR = ON + "durum.carpanlar";
    public static final String DURUM_SEVIYELER = ON + "durum.seviyeler";
    /** K-E67: olasılığı 0 olan seviyenin nedeni ve yoğunluk notu. */
    public static final String SEVIYE_NEDEN_MAKS = ON + "seviye_neden.maks";
    public static final String SEVIYE_NEDEN_GUN = ON + "seviye_neden.gun";
    public static final String SEVIYE_NEDEN_GELISIM = ON + "seviye_neden.gelisim";
    public static final String SEVIYE_NEDEN_ART_ARDA = ON + "seviye_neden.art_arda";
    public static final String SEVIYE_YOGUNLUK = ON + "seviye_yogunluk";
    public static final String DURUM_KAYITLI = ON + "durum.kayitli";
    public static final String DURUM_GOKYUZU = ON + "durum.gokyuzu";
    public static final String DURUM_DENIZ = ON + "durum.deniz";
    public static final String DURUM_DONMA = ON + "durum.donma";
    public static final String DURUM_SON_AFETLER = ON + "durum.son_afetler";
    public static final String DURUM_SURECLER = ON + "durum.surecler";
    public static final String DURUM_SURECLER_DURAN = ON + "durum.surecler_duran";
    public static final String SUREC_DURDU_KOMUT = ON + "durum.surec_durdu.komut";
    public static final String SUREC_DURDU_KURAL = ON + "durum.surec_durdu.kural";
    public static final String START_BASLADI = ON + "start.basladi";
    public static final String START_BILINMEYEN = ON + "start.bilinmeyen";
    public static final String START_KAYITLI_DEGIL = ON + "start.kayitli_degil";
    public static final String START_ZATEN_AKTIF = ON + "start.zaten_aktif";
    public static final String START_YER_UYGUN_DEGIL = ON + "start.yer_uygun_degil";
    public static final String START_HATA = ON + "start.hata";
    /** K-E67: komutla verilen seviye {@code afetMaksSiddet}'i aştı. */
    public static final String START_MAKS_ASILDI = ON + "start.maks_asildi";
    /** Gökyüzü-2 istek 26: komutla başlatma gün saati penceresine bakmaz. */
    public static final String START_PENCERE = ON + "start.pencere";
    /** Gökyüzü-2 Gk1: doğal patlama penceresi beklenirken kalan sürenin yerine. */
    public static final String DURUM_PENCERE = ON + "durum.pencere";
    public static final String STOP_DURDU = ON + "stop.durdu";
    public static final String STOP_AFET_YOK = ON + "stop.afet_yok";
    public static final String STOP_SURECLER = ON + "stop.surecler";
    public static final String STOP_TUR_YOK = ON + "stop.tur_yok";
    public static final String STOP_SURECLER_SURUYOR = ON + "stop.surecler_suruyor";
    public static final String GUN_AYARLANDI = ON + "gun.ayarlandi";
    public static final String HIZ_AYARLANDI = ON + "hiz.ayarlandi";
    public static final String YARDIM_BASLIK = ON + "yardim.baslik";
    public static final String SIFIRLA_TAMAM = ON + "sifirla.tamam";
    public static final String ASAMA_AFET = ON + "asama.afet";
    public static final String ASAMA_AFET_BITTI = ON + "asama.afet_bitti";
    public static final String ASAMA_YOK = ON + "asama.yok";
    public static final String ASAMA_SUREC_YOK = ON + "asama.surec_yok";
    public static final String SEVIYE = ON + "seviye";
    public static final String YOK = ON + "yok";
    public static final String VAR = ON + "var";
    public static final String EVET = ON + "evet";
    public static final String HAYIR = ON + "hayir";
    public static final String KALICI = ON + "kalici";
    public static final String GECICI = ON + "gecici";
    public static final String BILINMIYOR = ON + "bilinmiyor";
    public static final String HER_YER = ON + "her_yer";

    private CekirdekKomutlari() {
    }

    static int durum(CommandContext<CommandSourceStack> baglam) {
        CommandSourceStack kaynak = baglam.getSource();
        MinecraftServer server = kaynak.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        long gun = yonetici.oyunGunu();
        List<Component> satirlar = new ArrayList<>();
        satirlar.add(Component.translatable(DURUM_BASLIK));
        List<Afet> aktifler = yonetici.aktifAfetler();
        for (Afet afet : aktifler) {
            AfetBaglami afetBaglami = afet.baglam();
            long pencere = afet.pencereBeklemesi();
            Component kalan = pencere >= 0 ? Component.translatable(DURUM_PENCERE, sure(pencere))
                    : sure(yonetici.zamanOlcegi().gercekTick(yonetici.kalanOku(afet)));
            satirlar.add(Component.translatable(DURUM_AFET, afetAdi(afet.tur()), seviyeMetni(afet.seviye()),
                    Component.translatable(afet.asama().ceviriAnahtari()), afet.altAsama(), kalan));
            Component yaricap = yonetici.etkiAlani(afet).map(CekirdekKomutlari::yaricap)
                    .orElseGet(() -> Component.translatable(BILINMIYOR));
            satirlar.add(Component.translatable(DURUM_AFET_KONUM, afetBaglami.merkez().toShortString(),
                    afetBaglami.boyut().location().toString(), yaricap, oyuncuAdi(server, afetBaglami.hedefOyuncu())));
        }
        if (aktifler.isEmpty()) {
            satirlar.add(Component.translatable(DURUM_AFET_YOK));
        }
        satirlar.add(Component.translatable(DURUM_GUN, gun, donem.gunOfseti(), sayi(yonetici.zamanOlcegi().carpan()),
                sayi(TpsOlcer.tps()), sayi(TpsOlcer.kismaCarpani())));
        satirlar.add(Component.translatable(DURUM_BEKLEME, sure(donem.genelBeklemeKalan()), turBeklemeleri(donem)));
        satirlar.add(SiklikBilgisi.sonrakiSatiri(yonetici));
        satirlar.add(Component.translatable(DURUM_CARPANLAR, carpanlar(donem)));
        satirlar.add(Component.translatable(DURUM_SEVIYELER, gun, Kurallar.maksSiddet(server.getGameRules()),
                seviyeOlasiliklari(yonetici.seviyeDurumu(gun))));
        satirlar.add(Component.translatable(DURUM_KAYITLI, kayitliAfetler()));
        satirlar.add(Component.translatable(DURUM_GOKYUZU, Component.translatable(donem.ayVar() ? VAR : YOK),
                Component.translatable(donem.gunesEvresi().ceviriAnahtari()), sayi(donem.evreIlerlemesi() * 100.0)));
        DenizSeviyesi deniz = donem.deniz();
        satirlar.add(Component.translatable(DURUM_DENIZ, sayi(deniz.mevcutArtis()), sayi(deniz.hedefArtis()),
                Component.translatable(deniz.kalici() ? KALICI : GECICI)));
        satirlar.add(Component.translatable(DURUM_DONMA, sayi(donem.donmaIlerlemesi()),
                Component.translatable(donem.cozulmeDonemi() ? EVET : HAYIR), sayi(donem.buharlasmaDerinligi())));
        List<DonemSureci> suren = yonetici.aktifDonemSurecleri();
        Map<DonemSureci, DonemSureci.DurdurmaNedeni> duran = yonetici.durdurulmusDonemSurecleri();
        satirlar.add(Component.translatable(DURUM_SURECLER, surecAdlari(suren)));
        satirlar.add(Component.translatable(DURUM_SURECLER_DURAN, duranSurecler(duran)));
        ServerLevel ust = server.overworld();
        List<DonemSureci> surecler = new ArrayList<>(suren);
        surecler.addAll(duran.keySet());
        for (DonemSureci surec : surecler) {
            surec.durumSatiri(ust).ifPresent(satirlar::add);
        }
        satirlar.add(Component.translatable(DURUM_SON_AFETLER, sonAfetler(donem)));
        for (Component satir : satirlar) {
            kaynak.sendSuccess(() -> satir, false);
        }
        return 1;
    }

    static int start(CommandContext<CommandSourceStack> baglam, Integer seviye, ServerPlayer oyuncu) {
        CommandSourceStack kaynak = baglam.getSource();
        String ad = StringArgumentType.getString(baglam, "afet");
        Optional<AfetTuru> secilen = AfetTuru.kimlikIle(ad);
        if (secilen.isEmpty()) {
            kaynak.sendFailure(Component.translatable(START_BILINMEYEN, ad));
            return 0;
        }
        AfetTuru tur = secilen.get();
        AfetYoneticisi yonetici = AfetYoneticisi.al(kaynak.getServer());
        AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(tur, seviye, oyuncu, kaynak.getLevel(),
                BlockPos.containing(kaynak.getPosition()));
        switch (sonuc) {
            case BASLADI -> {
                Afet afet = yonetici.aktifAfet(tur).orElseThrow();
                AfetBaglami afetBaglami = afet.baglam();
                Component mesaj = Component.translatable(START_BASLADI, afetAdi(tur), seviyeMetni(afet.seviye()),
                        afetBaglami.merkez().toShortString(), oyuncuAdi(kaynak.getServer(), afetBaglami.hedefOyuncu()),
                        sure(yonetici.zamanOlcegi().gercekTick(yonetici.kalanOku(afet))));
                kaynak.sendSuccess(() -> mesaj, false);
                int maks = Kurallar.maksSiddet(kaynak.getServer().getGameRules());
                if (tur.seviyeli() && seviye != null && afet.seviye() > maks) {
                    kaynak.sendSuccess(() -> Component.translatable(START_MAKS_ASILDI, afet.seviye(), maks), false);
                }
                GunSaatiPenceresi.dogalPatlama(tur).ifPresent(p -> kaynak.sendSuccess(() -> Component.translatable(
                        START_PENCERE, afetAdi(tur), p.bas(), p.son()), false));
                return 1;
            }
            case ZATEN_AKTIF -> kaynak.sendFailure(Component.translatable(START_ZATEN_AKTIF, afetAdi(tur)));
            case KAYITLI_DEGIL -> kaynak.sendFailure(Component.translatable(START_KAYITLI_DEGIL, afetAdi(tur)));
            case YER_UYGUN_DEGIL, CAKISIYOR, HEDEF_YOK ->
                    kaynak.sendFailure(Component.translatable(START_YER_UYGUN_DEGIL, afetAdi(tur)));
            case HATA -> kaynak.sendFailure(Component.translatable(START_HATA, afetAdi(tur)));
        }
        return 0;
    }

    /**
     * {@code stop}: süren bütün afetleri durdurur; afet yoksa dönem süreçlerini. {@code stop <afet>}: yalnız o türden süren afeti
     * (AS-8).
     */
    static int stop(CommandContext<CommandSourceStack> baglam, Optional<String> ad) {
        CommandSourceStack kaynak = baglam.getSource();
        AfetYoneticisi yonetici = AfetYoneticisi.al(kaynak.getServer());
        if (ad.isPresent()) {
            Optional<AfetTuru> secilen = AfetTuru.kimlikIle(ad.get());
            if (secilen.isEmpty()) {
                kaynak.sendFailure(Component.translatable(START_BILINMEYEN, ad.get()));
                return 0;
            }
            if (!yonetici.durdur(secilen.get())) {
                kaynak.sendFailure(Component.translatable(STOP_TUR_YOK, afetAdi(secilen.get())));
                return 0;
            }
            kaynak.sendSuccess(() -> Component.translatable(STOP_DURDU, afetAdi(secilen.get())), false);
            return 1;
        }
        List<Afet> afetler = yonetici.aktifAfetler();
        if (yonetici.durdur()) {
            for (Afet afet : afetler) {
                kaynak.sendSuccess(() -> Component.translatable(STOP_DURDU, afetAdi(afet.tur())), false);
            }
            List<DonemSureci> suren = yonetici.aktifDonemSurecleri();
            if (!suren.isEmpty()) {
                Component adlar = surecAdlari(suren);
                kaynak.sendSuccess(() -> Component.translatable(STOP_SURECLER_SURUYOR, adlar), false);
            }
            return 1;
        }
        List<DonemSureci> durdurulan = yonetici.donemSurecleriniDurdur();
        if (durdurulan.isEmpty()) {
            kaynak.sendFailure(Component.translatable(STOP_AFET_YOK));
            return 0;
        }
        Component adlar = surecAdlari(durdurulan);
        kaynak.sendSuccess(() -> Component.translatable(STOP_SURECLER, adlar), false);
        return 1;
    }

    static int gun(CommandContext<CommandSourceStack> baglam) {
        CommandSourceStack kaynak = baglam.getSource();
        AfetYoneticisi yonetici = AfetYoneticisi.al(kaynak.getServer());
        int istenen = IntegerArgumentType.getInteger(baglam, "sayi");
        DunyaDonemi donem = yonetici.donem();
        donem.gunOfsetiAyarla(OyunGunu.ofset(istenen, kaynak.getServer().overworld().getGameTime(), donem.uykuBirikimi()));
        long gun = yonetici.oyunGunu();
        Component olasiliklar = seviyeOlasiliklari(yonetici.seviyeDurumu(gun));
        kaynak.sendSuccess(() -> Component.translatable(GUN_AYARLANDI, gun, olasiliklar), false);
        return 1;
    }

    static int hiz(CommandContext<CommandSourceStack> baglam) {
        CommandSourceStack kaynak = baglam.getSource();
        AfetYoneticisi yonetici = AfetYoneticisi.al(kaynak.getServer());
        yonetici.hizAyarla(DoubleArgumentType.getDouble(baglam, "carpan"));
        double carpan = yonetici.zamanOlcegi().carpan();
        kaynak.sendSuccess(() -> Component.translatable(HIZ_AYARLANDI, sayi(carpan)), false);
        return 1;
    }

    /**
     * {@code siklik [yüzde]}: {@code afetSikligi} gamerule'ünü (kalıcı, dünya verisinde) okur ya da yazar. Yazınca süren
     * beklemeler hemen yeni sıklığa ölçeklenir ve sonraki afet tahmini gösterilir; {@code /gamerule afetSikligi} ile aynı
     * değerdir.
     */
    static int siklik(CommandContext<CommandSourceStack> baglam, Integer yuzde) {
        CommandSourceStack kaynak = baglam.getSource();
        MinecraftServer server = kaynak.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        List<Component> satirlar = new ArrayList<>();
        if (yuzde == null) {
            SiklikBilgisi.sinirUyarisi(yonetici).ifPresent(satirlar::add);
            satirlar.add(SiklikBilgisi.sonrakiSatiri(yonetici));
        } else {
            server.getGameRules().getRule(Kurallar.AFET_SIKLIGI).set(yuzde, server);
            yonetici.siklikDenetle(false);
            satirlar.addAll(SiklikBilgisi.degisimSatirlari(yonetici));
        }
        for (Component satir : satirlar) {
            kaynak.sendSuccess(() -> satir, false);
        }
        return 1;
    }

    /** Alt komutsuz {@code /dogalafet}: kaynakın kullanabildiği alt komutların kullanımı, alfabetik. */
    static int yardim(CommandContext<CommandSourceStack> baglam) {
        CommandSourceStack kaynak = baglam.getSource();
        CommandDispatcher<CommandSourceStack> dagitici = kaynak.getServer().getCommands().getDispatcher();
        CommandNode<CommandSourceStack> kok = dagitici.getRoot().getChild(DogalAfetKomutu.KOK);
        List<String> kullanimlar = new ArrayList<>(dagitici.getSmartUsage(kok, kaynak).values());
        kullanimlar.sort(null);
        kaynak.sendSuccess(() -> Component.translatable(YARDIM_BASLIK), false);
        for (String kullanim : kullanimlar) {
            kaynak.sendSuccess(() -> Component.literal("/" + DogalAfetKomutu.KOK + " " + kullanim), false);
        }
        return 1;
    }

    static int sifirla(CommandContext<CommandSourceStack> baglam) {
        CommandSourceStack kaynak = baglam.getSource();
        AfetYoneticisi.al(kaynak.getServer()).sifirla();
        kaynak.sendSuccess(() -> Component.translatable(SIFIRLA_TAMAM), false);
        return 1;
    }

    /**
     * {@code asama ileri [afet|süreç]}: ad yoksa birincil afet, süren bir afetin adıysa o afet (AS-8), değilse yalnız o dönem
     * süreci ilerler (K-C48).
     */
    static int asamaIleri(CommandContext<CommandSourceStack> baglam, Optional<String> surec) {
        CommandSourceStack kaynak = baglam.getSource();
        MinecraftServer server = kaynak.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        Optional<AfetTuru> tur = surec.isEmpty() ? yonetici.aktifAfet().map(Afet::tur)
                : AfetTuru.kimlikIle(surec.get()).filter(t -> yonetici.aktifAfet(t).isPresent());
        if (tur.isPresent() && yonetici.asamaIleri(tur.get())) {
            Optional<Afet> sonra = yonetici.aktifAfet(tur.get());
            Component mesaj = sonra.isPresent()
                    ? Component.translatable(ASAMA_AFET, afetAdi(tur.get()),
                            Component.translatable(sonra.get().asama().ceviriAnahtari()))
                    : Component.translatable(ASAMA_AFET_BITTI, afetAdi(tur.get()));
            kaynak.sendSuccess(() -> mesaj, false);
            return 1;
        }
        Optional<Component> donemMesaji = tur.isPresent() ? Optional.empty() : yonetici.donemAsamaIleri(surec);
        if (donemMesaji.isPresent()) {
            Component mesaj = donemMesaji.get();
            kaynak.sendSuccess(() -> mesaj, false);
            return 1;
        }
        kaynak.sendFailure(surec.isPresent() ? Component.translatable(ASAMA_SUREC_YOK, surec.get())
                : Component.translatable(ASAMA_YOK));
        return 0;
    }

    // ---------------------------------------------------------------- biçimlendirme

    static Component afetAdi(AfetTuru tur) {
        return Component.translatable(tur.ceviriAnahtari());
    }

    static Component seviyeMetni(int seviye) {
        Component ad = Component.translatable(CekirdekCevirileri.seviyeAnahtari(seviye));
        return seviye <= 0 ? ad : Component.translatable(SEVIYE, seviye, ad);
    }

    /** Tick süresini {@code d:ss} ya da {@code s:dd:ss} biçiminde yazar; bilinmeyen (&lt; 0) için "bilinmiyor". */
    static Component sure(long tick) {
        if (tick < 0) {
            return Component.translatable(BILINMIYOR);
        }
        long saniye = (tick + 19) / 20;
        long saat = saniye / 3600;
        long dakika = saniye / 60 % 60;
        long sn = saniye % 60;
        return Component.literal(saat > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", saat, dakika, sn)
                : String.format(Locale.ROOT, "%d:%02d", dakika, sn));
    }

    static String sayi(double deger) {
        return String.format(Locale.ROOT, "%.2f", deger);
    }

    private static Component yaricap(EtkiAlani alan) {
        return alan.global() ? Component.translatable(HER_YER) : Component.literal(sayi(alan.yaricap()));
    }

    private static Component oyuncuAdi(MinecraftServer server, UUID kimlik) {
        if (kimlik == null) {
            return Component.translatable(YOK);
        }
        ServerPlayer oyuncu = server.getPlayerList().getPlayer(kimlik);
        return oyuncu != null ? oyuncu.getDisplayName() : Component.literal(kimlik.toString());
    }

    private static Component turBeklemeleri(DunyaDonemi donem) {
        MutableComponent sonuc = Component.empty();
        boolean ilk = true;
        for (AfetTuru tur : AfetTuru.values()) {
            long kalan = donem.turBeklemeKalan(tur);
            if (kalan <= 0) {
                continue;
            }
            if (!ilk) {
                sonuc.append(", ");
            }
            sonuc.append(afetAdi(tur)).append(" ").append(sure(kalan));
            ilk = false;
        }
        return ilk ? Component.translatable(YOK) : sonuc;
    }

    private static Component carpanlar(DunyaDonemi donem) {
        MutableComponent sonuc = Component.empty();
        boolean ilk = true;
        for (GeciciCarpan carpan : donem.carpanlar()) {
            if (!ilk) {
                sonuc.append(", ");
            }
            long kalan = carpan.baslangic() + carpan.sabitSure() + carpan.sonumSuresi() - donem.saat();
            sonuc.append(afetAdi(carpan.tur())).append(" x" + sayi(carpan.deger(donem.saat())) + " (")
                    .append(sure(Math.max(0, kalan))).append(")");
            ilk = false;
        }
        return ilk ? Component.translatable(YOK) : sonuc;
    }

    /**
     * {@code S1 54.3%, …, S5 0.0% (16. günde açılır)}: doğal seçimin olasılıkları; 0 olan seviyenin yanında nedeni, yoğunluk
     * zayıflatması etkinse sonda notu (K-E67).
     */
    static Component seviyeOlasiliklari(SeviyeDurumu durum) {
        MutableComponent sonuc = Component.empty();
        for (int seviye = AfetTuru.EN_DUSUK_SEVIYE; seviye <= AfetTuru.EN_YUKSEK_SEVIYE; seviye++) {
            if (seviye > AfetTuru.EN_DUSUK_SEVIYE) {
                sonuc.append(", ");
            }
            sonuc.append(String.format(Locale.ROOT, "S%d %.1f%%", seviye, durum.olasilik(seviye) * 100.0));
            int s = seviye;
            durum.neden(seviye).ifPresent(neden -> sonuc.append(" (").append(nedenMetni(neden, s, durum.maks()))
                    .append(")"));
        }
        if (durum.zayiflatildi()) {
            sonuc.append("; ").append(Component.translatable(SEVIYE_YOGUNLUK, sayi(durum.yuk())));
        }
        return sonuc;
    }

    private static Component nedenMetni(SiddetSecici.KapaliNeden neden, int seviye, int maks) {
        return switch (neden) {
            case MAKS -> Component.translatable(SEVIYE_NEDEN_MAKS, maks);
            case GUN -> Component.translatable(SEVIYE_NEDEN_GUN, CekirdekAyarlari.al().siddet().esik(seviye));
            case GELISIM -> Component.translatable(SEVIYE_NEDEN_GELISIM);
            case ART_ARDA -> Component.translatable(SEVIYE_NEDEN_ART_ARDA);
        };
    }

    private static Component surecAdlari(List<DonemSureci> surecler) {
        MutableComponent sonuc = Component.empty();
        boolean ilk = true;
        for (DonemSureci surec : surecler) {
            if (!ilk) {
                sonuc.append(", ");
            }
            sonuc.append(surec.ad());
            ilk = false;
        }
        return ilk ? Component.translatable(YOK) : sonuc;
    }

    private static Component duranSurecler(Map<DonemSureci, DonemSureci.DurdurmaNedeni> surecler) {
        MutableComponent sonuc = Component.empty();
        boolean ilk = true;
        for (Map.Entry<DonemSureci, DonemSureci.DurdurmaNedeni> girdi : surecler.entrySet()) {
            if (!ilk) {
                sonuc.append(", ");
            }
            String anahtar = girdi.getValue() == DonemSureci.DurdurmaNedeni.KOMUT ? SUREC_DURDU_KOMUT : SUREC_DURDU_KURAL;
            sonuc.append(Component.translatable(anahtar, girdi.getKey().ad()));
            ilk = false;
        }
        return ilk ? Component.translatable(YOK) : sonuc;
    }

    private static Component kayitliAfetler() {
        MutableComponent sonuc = Component.empty();
        boolean ilk = true;
        for (AfetTuru tur : AfetKaydi.kayitliTurler()) {
            if (!ilk) {
                sonuc.append(", ");
            }
            sonuc.append(afetAdi(tur));
            ilk = false;
        }
        return ilk ? Component.translatable(YOK) : sonuc;
    }

    private static Component sonAfetler(DunyaDonemi donem) {
        MutableComponent sonuc = Component.empty();
        boolean ilk = true;
        for (GecmisKaydi kayit : donem.sonAfetler()) {
            if (!ilk) {
                sonuc.append("; ");
            }
            sonuc.append(afetAdi(kayit.tur())).append(" ").append(seviyeMetni(kayit.seviye()))
                    .append(" " + kayit.merkez().toShortString());
            ilk = false;
        }
        return ilk ? Component.translatable(YOK) : sonuc;
    }

    public static void cevirileriYaz(CeviriYazici y) {
        y.ekle(DURUM_BASLIK, "Natural Disasters status", "Doğal Afetler durumu");
        y.ekle(DURUM_AFET_YOK, "No active disaster.", "Aktif afet yok.");
        y.ekle(DURUM_AFET, "Active disaster: %s, %s; phase: %s (sub-phase %s), remaining: %s",
                "Aktif afet: %s, %s; aşama: %s (alt aşama %s), kalan: %s");
        y.ekle(DURUM_PENCERE, "time-of-day window awaited: ~%s", "patlama penceresi bekleniyor: ~%s");
        y.ekle(DURUM_AFET_KONUM, "Centre: %s (%s), impact radius: %s, target: %s",
                "Merkez: %s (%s), etki yarıçapı: %s, hedef: %s");
        y.ekle(DURUM_GUN, "Game day: %s (offset %s), speed: x%s, TPS: %s (budget x%s)",
                "Oyun günü: %s (ofset %s), hız: x%s, TPS: %s (bütçe x%s)");
        y.ekle(DURUM_BEKLEME, "Cooldown: general %s; per disaster: %s", "Bekleme: genel %s; afet başına: %s");
        y.ekle(DURUM_CARPANLAR, "Temporary multipliers: %s", "Geçici çarpanlar: %s");
        y.ekle(DURUM_SEVIYELER, "Level probabilities (day %s, max level %s): %s",
                "Seviye olasılıkları (gün %s, en yüksek seviye %s): %s");
        y.ekle(SEVIYE_NEDEN_MAKS, "above afetMaksSiddet %s", "afetMaksSiddet %s üstünde");
        y.ekle(SEVIYE_NEDEN_GUN, "unlocks on day %s", "%s. günde açılır");
        y.ekle(SEVIYE_NEDEN_GELISIM, "player progress too low (gelisimDahil)", "oyuncu gelişimi yetersiz (gelisimDahil)");
        y.ekle(SEVIYE_NEDEN_ART_ARDA, "previous natural disaster was level 4/5", "önceki doğal afet seviye 4/5 idi");
        y.ekle(SEVIYE_YOGUNLUK, "many recent disasters (load x%s): higher levels are rarer",
                "son günde çok afet (yük x%s): yüksek seviyeler daha seyrek");
        y.ekle(DURUM_KAYITLI, "Registered disasters: %s", "Kayıtlı afetler: %s");
        y.ekle(DURUM_GOKYUZU, "Moon: %s; sun: %s (progress %s%%)", "Ay: %s; güneş: %s (ilerleme %%%s)");
        y.ekle(DURUM_DENIZ, "Sea level: +%s blocks (target +%s, %s)", "Deniz seviyesi: +%s blok (hedef +%s, %s)");
        y.ekle(DURUM_DONMA, "Freeze progress: %s%% (thaw era: %s); evaporation depth: %s blocks",
                "Donma ilerlemesi: %%%s (çözülme dönemi: %s); buharlaşma derinliği: %s blok");
        y.ekle(DURUM_SURECLER, "Running era processes: %s", "Süren dönem süreçleri: %s");
        y.ekle(DURUM_SURECLER_DURAN, "Stopped era processes: %s", "Durdurulmuş dönem süreçleri: %s");
        y.ekle(SUREC_DURDU_KOMUT, "%s stopped (stop)", "%s durduruldu (stop)");
        y.ekle(SUREC_DURDU_KURAL, "%s stopped (rule)", "%s durduruldu (kural)");
        y.ekle(DURUM_SON_AFETLER, "Recent disasters: %s", "Son afetler: %s");
        y.ekle(START_BASLADI, "%s started (%s), centre %s, target %s; siren in %s",
                "%s başlatıldı (%s), merkez %s, hedef %s; sirene %s");
        y.ekle(START_PENCERE, "Started by command: the time-of-day window is ignored (a natural %s goes off only at day "
                + "time %s–%s).", "Komutla başlatıldı: gün saati penceresine bakılmaz (doğal %s yalnız gün saati %s–%s "
                + "arasında patlar).");
        y.ekle(START_BILINMEYEN, "Unknown disaster: %s", "Bilinmeyen afet: %s");
        y.ekle(START_KAYITLI_DEGIL, "%s is not registered yet.", "%s henüz kayıtlı değil.");
        y.ekle(START_ZATEN_AKTIF, "%s is already running; other disasters can be started alongside it.",
                "%s zaten sürüyor; başka türden afetler yanında başlatılabilir.");
        y.ekle(START_YER_UYGUN_DEGIL, "No suitable place found for %s.", "%s için uygun yer bulunamadı.");
        y.ekle(START_HATA, "%s could not be started; see the server log.", "%s başlatılamadı; sunucu log'una bakın.");
        y.ekle(START_MAKS_ASILDI, "Level %s is above afetMaksSiddet (%s); applied because it was started by command.",
                "Seviye %s, afetMaksSiddet (%s) üstünde; komutla başlatıldığı için uygulandı.");
        y.ekle(STOP_DURDU, "%s stopped.", "%s durduruldu.");
        y.ekle(STOP_TUR_YOK, "No %s is running.", "Süren %s yok.");
        y.ekle(STOP_AFET_YOK, "There is no disaster or era process to stop.", "Durdurulacak afet ya da dönem süreci yok.");
        y.ekle(STOP_SURECLER, "Era processes stopped: %s", "Dönem süreçleri durduruldu: %s");
        y.ekle(STOP_SURECLER_SURUYOR, "Era processes still running: %s; run /dogalafet stop again to stop them.",
                "Süren dönem süreçleri: %s; durdurmak için yeniden /dogalafet stop.");
        y.ekle(GUN_AYARLANDI, "Game day set to %s. Level probabilities: %s",
                "Oyun günü %s olarak ayarlandı. Seviye olasılıkları: %s");
        y.ekle(HIZ_AYARLANDI, "Disaster and era timers run at x%s.", "Afet ve dönem zamanlayıcıları x%s hızında.");
        y.ekle(YARDIM_BASLIK, "Natural Disasters commands (OP):", "Doğal Afetler komutları (OP):");
        SiklikBilgisi.cevirileriYaz(y);
        y.ekle(SIFIRLA_TAMAM, "The world's disaster and era state was reset.",
                "Dünyanın afet ve dönem durumu ilk hâline döndürüldü.");
        y.ekle(ASAMA_AFET, "%s phase: %s", "%s aşaması: %s");
        y.ekle(ASAMA_AFET_BITTI, "%s has ended.", "%s sona erdi.");
        y.ekle(ASAMA_YOK, "There is no disaster or era phase to advance.", "İlerletilecek afet ya da dönem aşaması yok.");
        y.ekle(ASAMA_SUREC_YOK, "No running or stopped era process named %s can advance.",
                "%s adlı, süren ya da durdurulmuş ve ilerleyebilen bir dönem süreci yok.");
        y.ekle(SEVIYE, "Level %s (%s)", "Seviye %s (%s)");
        y.ekle(YOK, "none", "yok");
        y.ekle(VAR, "present", "var");
        y.ekle(EVET, "yes", "evet");
        y.ekle(HAYIR, "no", "hayır");
        y.ekle(KALICI, "permanent", "kalıcı");
        y.ekle(GECICI, "temporary", "geçici");
        y.ekle(BILINMIYOR, "unknown", "bilinmiyor");
        y.ekle(HER_YER, "everywhere", "her yer");
    }
}
