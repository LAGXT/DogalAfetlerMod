package io.github.kadirberkpolat1.dogalafetler.core;

import com.mojang.authlib.GameProfile;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonumTipi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.ZamanlayiciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.TekSeferlikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.TekSeferlikKurallar;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.SirenIstegiEvent;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Çekirdeğin oyun içi testleri: dünya dönemi kaydı, kapalı gamerule'daki afetin seçilmemesi ve afet bitiş olayının sağ
 * çıkanları. Tek aktif afet yuvasını kullanan testler kendi {@code batch}'indedir (batch'ler sırayla koşar).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CekirdekOyunTestleri {
    private CekirdekOyunTestleri() {
    }

    /**
     * Oyuncu listesine giren sahte oyuncu. {@code GameTestHelper.makeMockServerPlayerInLevel()} Forge 1.20.1'de kanalsız
     * {@link Connection} kurar ve {@code PlayerList.placeNewPlayer} → {@code NetworkFilters.injectIfNecessary}
     * {@code channel().pipeline()} çağrısında NPE verir; burada bağlantı bir {@link EmbeddedChannel}'a bağlanır.
     */
    public static ServerPlayer sahteOyuncu(GameTestHelper yardimci, double x, double y, double z) {
        return sahteOyuncu(yardimci, new GameProfile(UUID.randomUUID(), "cekirdek-test"), x, y, z);
    }

    /** Verilen profille sahte oyuncu (aynı profil = aynı oyuncunun yeniden girişi). */
    public static ServerPlayer sahteOyuncu(GameTestHelper yardimci, GameProfile profil, double x, double y, double z) {
        ServerLevel level = yardimci.getLevel();
        ServerPlayer oyuncu = new ServerPlayer(level.getServer(), level, profil);
        Connection baglanti = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(baglanti);
        level.getServer().getPlayerList().placeNewPlayer(baglanti, oyuncu);
        oyuncu.moveTo(x, y, z);
        return oyuncu;
    }

    @GameTest(template = "bos")
    public static void donemKaydiYazilipOkununcaAyniKalir(GameTestHelper yardimci) {
        ServerLevel level = yardimci.getLevel();
        DunyaDonemi donem = new DunyaDonemi();
        donem.saatIlerlet(123_456);
        donem.ayVarAyarla(false);
        donem.gunesEvresiAyarla(GunesEvresi.GUNESSIZ);
        donem.evreIlerlemesiAyarla(0.625f);
        donem.donmaIlerlemesiAyarla(73.5f);
        donem.cozulmeDonemiAyarla(true);
        donem.buharlasmaDerinligiAyarla(4.25f);
        donem.denizAyarla(new DenizSeviyesi(4.5f, 2.75f, 99_000L, false));
        donem.carpanEkle(AfetTuru.TSUNAMI, 3.5, 72_000, 48_000, SonumTipi.USTEL);
        donem.bekleme().afetBitti(AfetTuru.DEPREM, 4, donem.saat(), ZamanlayiciAyarlari.VARSAYILAN);
        donem.bekleme().ertele(AfetTuru.TSUNAMI, donem.saat(), 6_000);
        for (int i = 0; i < 7; i++) {
            AfetTuru tur = AfetTuru.values()[i];
            donem.gecmiseEkle(new GecmisKaydi(tur, tur.seviyeli() ? 1 + i % 5 : 0, new BlockPos(i * 10, 64, -i),
                    Level.OVERWORLD, 1_000L + i));
        }
        UUID hedef = UUID.randomUUID();
        donem.sonHedefAyarla(hedef);
        donem.sonSeviyeAyarla(4);
        donem.gunOfsetiAyarla(90);
        donem.ekVeri("donma").putFloat("maskeOrani", 0.25f);
        donem.ekVeriDegisti("donma");

        OyunTestiAfeti afet = new OyunTestiAfeti(AfetTuru.SINKHOLE, 16, 500);
        afet.baslat(level, new AfetBaglami(AfetTuru.SINKHOLE, 3, new BlockPos(10, 70, 20), Level.OVERWORLD, hedef,
                1_234L, 1_500L, 600L, true));
        afet.asamaIleri(level);
        afet.asamaIleri(level);
        afet.etkilenenEkle(hedef);
        afet.tick(level, 40);
        donem.aktifAfetAyarla(afet);
        CompoundTag beklenenAfet = afet.durumYaz(new CompoundTag());
        CompoundTag ilkKayit = donem.save(new CompoundTag());

        DunyaDonemi geri;
        File dosya = null;
        try {
            dosya = Files.createTempFile("dogalafetler_donem", ".dat").toFile();
            donem.setDirty();
            donem.save(dosya);
            geri = DunyaDonemi.yukle(NbtIo.readCompressed(dosya).getCompound("data"));
        } catch (IOException hata) {
            yardimci.fail("dönem dosyası yazılıp okunamadı: " + hata);
            return;
        } finally {
            if (dosya != null && !dosya.delete()) {
                dosya.deleteOnExit();
            }
        }

        yardimci.assertTrue(geri.saat() == 123_456, "saat");
        yardimci.assertFalse(geri.ayVar(), "ay");
        yardimci.assertTrue(geri.gunesEvresi() == GunesEvresi.GUNESSIZ, "güneş evresi");
        yardimci.assertTrue(geri.evreIlerlemesi() == 0.625f, "evre ilerlemesi");
        yardimci.assertTrue(geri.evreBaslangicSaati() == 123_456, "evre başlangıcı");
        yardimci.assertTrue(geri.donmaIlerlemesi() == 73.5f, "donma ilerlemesi");
        yardimci.assertTrue(geri.cozulmeDonemi(), "çözülme dönemi");
        yardimci.assertTrue(geri.buharlasmaDerinligi() == 4.25f, "buharlaşma derinliği");
        yardimci.assertTrue(geri.deniz().equals(new DenizSeviyesi(4.5f, 2.75f, 99_000L, false)), "deniz seviyesi");
        yardimci.assertTrue(geri.carpanlar().equals(donem.carpanlar()) && geri.carpanlar().size() == 1, "çarpanlar");
        for (AfetTuru tur : AfetTuru.values()) {
            yardimci.assertTrue(geri.turBeklemeKalan(tur) == donem.turBeklemeKalan(tur), "bekleme " + tur.kimlik());
        }
        yardimci.assertTrue(geri.genelBeklemeKalan() == donem.genelBeklemeKalan() && geri.genelBeklemeKalan() > 0,
                "genel bekleme");
        yardimci.assertTrue(geri.sonAfetler().equals(donem.sonAfetler())
                && geri.sonAfetler().size() == DunyaDonemi.GECMIS_SINIRI, "son afetler");
        yardimci.assertTrue(geri.sonHedef().equals(Optional.of(hedef)), "son hedef");
        yardimci.assertTrue(geri.sonSeviye() == 4, "son seviye");
        yardimci.assertTrue(geri.gunOfseti() == 90, "gün ofseti");
        yardimci.assertTrue(geri.ekVeri("donma").getFloat("maskeOrani") == 0.25f, "ek veri");
        yardimci.assertTrue(geri.save(new CompoundTag()).equals(ilkKayit), "ikinci kayıt ilkinin aynısı değil");

        CompoundTag okunanAfet = geri.bekleyenAfetiAl();
        yardimci.assertTrue(beklenenAfet.equals(okunanAfet), "aktif afetin NBT'si korunmadı");
        OyunTestiAfeti geriAfet = new OyunTestiAfeti(AfetTuru.SINKHOLE, 16, 500);
        geriAfet.durumOku(okunanAfet);
        yardimci.assertTrue(geriAfet.asama() == AfetAsamasi.AKTIF && geriAfet.seviye() == 3
                && geriAfet.aktifKalanTick() == 460 && geriAfet.etkilenenler().equals(Set.of(hedef))
                && geriAfet.baglam().equals(afet.baglam()), "aktif afet geri kurulamadı");
        yardimci.succeed();
    }

    @GameTest(template = "bos", batch = "cekirdek_secim")
    public static void kapaliKuralliAfetSecilmez(GameTestHelper yardimci) {
        MinecraftServer server = yardimci.getLevel().getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        GameRules kurallar = server.getGameRules();
        boolean oncekiKasirga = kurallar.getBoolean(Kurallar.KASIRGA_AFETI);
        boolean oncekiGenel = kurallar.getBoolean(Kurallar.DOGAL_AFETLER_ACIK);
        BlockPos konum = yardimci.absolutePos(BlockPos.ZERO);
        ServerPlayer oyuncu = sahteOyuncu(yardimci, konum.getX() + 0.5, konum.getY(), konum.getZ() + 0.5);
        try (AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(AfetTuru.KASIRGA,
                () -> new OyunTestiAfeti(AfetTuru.KASIRGA, 8, 100))) {
            kurallar.getRule(Kurallar.DOGAL_AFETLER_ACIK).set(true, server);
            kurallar.getRule(Kurallar.KASIRGA_AFETI).set(false, server);
            for (int i = 0; i < 300; i++) {
                Optional<AfetTuru> secim = yonetici.secimYap(true);
                yardimci.assertTrue(secim.isEmpty() || secim.get() != AfetTuru.KASIRGA,
                        "kasirgaAfeti=false iken kasırga seçildi");
            }
            kurallar.getRule(Kurallar.KASIRGA_AFETI).set(true, server);
            boolean secildi = false;
            for (int i = 0; i < 300 && !secildi; i++) {
                secildi = yonetici.secimYap(true).equals(Optional.of(AfetTuru.KASIRGA));
            }
            yardimci.assertTrue(secildi, "kural açıkken zorlanmış seçim kasırgayı hiç seçmedi");
            kurallar.getRule(Kurallar.DOGAL_AFETLER_ACIK).set(false, server);
            for (int i = 0; i < 50; i++) {
                yardimci.assertTrue(yonetici.secimYap(true).isEmpty(), "dogalAfetlerAcik=false iken seçim yapıldı");
            }
            yardimci.assertTrue(yonetici.aktifAfet().isEmpty(), "kuru seçim afet başlatmamalı");
        } finally {
            kurallar.getRule(Kurallar.KASIRGA_AFETI).set(oncekiKasirga, server);
            kurallar.getRule(Kurallar.DOGAL_AFETLER_ACIK).set(oncekiGenel, server);
            server.getPlayerList().remove(oyuncu);
        }
        yardimci.succeed();
    }

    @GameTest(template = "bos", batch = "cekirdek_bitis")
    public static void afetBittiOlayiSagCikanlariVerir(GameTestHelper yardimci) {
        ServerLevel level = yardimci.getLevel();
        MinecraftServer server = level.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        BlockPos merkez = yardimci.absolutePos(BlockPos.ZERO);
        ServerPlayer kalan = sahteOyuncu(yardimci, merkez.getX() + 0.5, merkez.getY(), merkez.getZ() + 0.5);
        ServerPlayer olen = sahteOyuncu(yardimci, merkez.getX() + 2.5, merkez.getY(), merkez.getZ() + 0.5);
        List<AfetBittiEvent> olaylar = new ArrayList<>();
        Consumer<AfetBittiEvent> dinleyici = olaylar::add;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AfetBittiEvent.class, dinleyici);
        OyunTestiAfeti[] olusan = new OyunTestiAfeti[1];
        try (AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(AfetTuru.SINKHOLE, () -> {
            olusan[0] = new OyunTestiAfeti(AfetTuru.SINKHOLE, 16, 100);
            return olusan[0];
        })) {
            AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(AfetTuru.SINKHOLE, 2, kalan, level, merkez);
            yardimci.assertTrue(sonuc == AfetYoneticisi.BaslatmaSonucu.BASLADI, "afet başlamadı: " + sonuc);
            Afet afet = yonetici.aktifAfet().orElseThrow();
            yonetici.asamaIleri();
            yardimci.assertTrue(afet.asama() == AfetAsamasi.SIREN, "siren aşamasına geçmedi");
            yonetici.asamaIleri();
            yardimci.assertTrue(afet.asama() == AfetAsamasi.AKTIF, "aktif aşamaya geçmedi");
            yardimci.assertTrue(afet.etkilenenler().equals(Set.of(kalan.getUUID(), olen.getUUID())),
                    "alandaki oyuncular etkilenen sayılmadı: " + afet.etkilenenler());
            olen.kill();
            yardimci.assertTrue(afet.olenler().equals(Set.of(olen.getUUID())), "ölüm kaydedilmedi");
            yonetici.asamaIleri();
            yardimci.assertTrue(afet.asama() == AfetAsamasi.BITIS, "bitiş aşamasına geçmedi");
            yonetici.asamaIleri();
            yardimci.assertTrue(yonetici.aktifAfet().isEmpty(), "afet sona ermedi");
            yardimci.assertTrue(olusan[0].temizlemeSayisi() == 1, "afet bir kez temizlenmeli");
            yardimci.assertTrue(olaylar.size() == 1, "AfetBittiEvent sayısı: " + olaylar.size());
            AfetBittiEvent olay = olaylar.get(0);
            yardimci.assertTrue(olay.tur() == AfetTuru.SINKHOLE && olay.seviye() == 2 && olay.dogalBitti(),
                    "olayın türü, seviyesi ya da nedeni yanlış");
            yardimci.assertTrue(olay.etkilenenler().equals(Set.of(kalan.getUUID(), olen.getUUID())),
                    "etkilenenler yanlış: " + olay.etkilenenler());
            yardimci.assertTrue(olay.sagCikanlar().equals(Set.of(kalan.getUUID())),
                    "sağ çıkanlar yanlış: " + olay.sagCikanlar());
            yardimci.assertTrue(yonetici.donem().sonAfetler().get(0).tur() == AfetTuru.SINKHOLE, "geçmişe yazılmadı");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(dinleyici);
            yonetici.durdur();
            server.getPlayerList().remove(kalan);
            server.getPlayerList().remove(olen);
        }
        yardimci.succeed();
    }

    private static int komut(GameTestHelper yardimci, String komut) {
        MinecraftServer server = yardimci.getLevel().getServer();
        try {
            return server.getCommands().getDispatcher().execute(komut,
                    server.createCommandSourceStack().withSuppressedOutput().withPermission(2));
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException hata) {
            yardimci.fail("/" + komut + " çalışmadı: " + hata.getMessage());
            return -1;
        }
    }

    @GameTest(template = "bos", batch = "cekirdek_gunes")
    public static void gunesSureciStopIleNormaleDonerUlasilmisDonemKalir(GameTestHelper yardimci) {
        ServerLevel level = yardimci.getLevel();
        MinecraftServer server = level.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        yonetici.durdur();
        donem.gunesEvresiAyarla(GunesEvresi.NORMAL);
        BlockPos konum = yardimci.absolutePos(BlockPos.ZERO);
        try (AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(AfetTuru.GUNES_PATLAMASI,
                () -> new OyunTestiAfeti(AfetTuru.GUNES_PATLAMASI, 0, 10_000))) {
            yardimci.assertTrue(yonetici.elleBaslat(AfetTuru.GUNES_PATLAMASI, null, null, level, konum)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "güneş patlaması başlamadı");
            yonetici.asamaIleri();
            yonetici.asamaIleri();
            yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.BUYUYEN, "AKTIF'te güneş büyümeli");
            yardimci.assertTrue(komut(yardimci, "dogalafet stop") == 1, "stop başarısız");
            yardimci.assertTrue(yonetici.aktifAfet().isEmpty(), "afet durmadı");
            yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.NORMAL,
                    "tamamlanmamış büyüme durdurulunca güneş NORMAL olmalı: " + donem.gunesEvresi());
            yardimci.assertTrue(TekSeferlikKurallar.uygunMu(AfetTuru.GUNES_PATLAMASI, donem.ayVar(), donem.gunesEvresi(),
                    1000, 1000, TekSeferlikAyarlari.VARSAYILAN), "güneş patlaması yeniden seçilebilir kalmalı");

            yardimci.assertTrue(yonetici.elleBaslat(AfetTuru.GUNES_PATLAMASI, null, null, level, konum)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "ikinci güneş patlaması başlamadı");
            yonetici.asamaIleri();
            yonetici.asamaIleri();
            donem.gunesEvresiAyarla(GunesEvresi.SUPERNOVA);
            yardimci.assertTrue(komut(yardimci, "dogalafet stop") == 1, "ikinci stop başarısız");
            yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.SUPERNOVA, "ulaşılmış süpernova geri alınmamalı");
        } finally {
            yonetici.durdur();
            donem.gunesEvresiAyarla(GunesEvresi.NORMAL);
        }
        yardimci.succeed();
    }

    /** Durdurulması sayılan sahte dönem süreci. */
    private static final class SahteSurec implements DonemSureci {
        private final AfetTuru tur;
        private boolean aktif = true;
        private int durdurma;

        private SahteSurec(AfetTuru tur) {
            this.tur = tur;
        }

        @Override
        public String kimlik() {
            return "oyuntesti_" + tur.kimlik();
        }

        @Override
        public Optional<AfetTuru> afetTuru() {
            return Optional.of(tur);
        }

        @Override
        public boolean aktifMi(ServerLevel ustDunya) {
            return aktif;
        }

        @Override
        public void durdur(ServerLevel ustDunya) {
            aktif = false;
            durdurma++;
        }
    }

    @GameTest(template = "bos", batch = "cekirdek_surec")
    public static void donemSureciStopVeGameruleIleDurur(GameTestHelper yardimci) {
        MinecraftServer server = yardimci.getLevel().getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        GameRules kurallar = server.getGameRules();
        boolean oncekiAy = kurallar.getBoolean(Kurallar.AY_CARPMASI_AFETI);
        yonetici.durdur();
        SahteSurec ay = new SahteSurec(AfetTuru.AY_CARPMASI);
        SahteSurec gunes = new SahteSurec(AfetTuru.GUNES_PATLAMASI);
        try (AfetKaydi.GeciciKayit k1 = AfetYoneticisi.geciciDonemSureciKaydet(ay);
                AfetKaydi.GeciciKayit k2 = AfetYoneticisi.geciciDonemSureciKaydet(gunes)) {
            yardimci.assertTrue(yonetici.aktifDonemSurecleri().size() == 2, "iki süreç aktif olmalı");
            yardimci.assertTrue(komut(yardimci, "dogalafet durum") == 1, "durum başarısız");
            donem.gunesEvresiAyarla(GunesEvresi.BUYUYEN);
            yardimci.assertTrue(komut(yardimci, "dogalafet stop") == 1, "afet yokken stop süreçleri durdurmalı");
            yardimci.assertTrue(ay.durdurma == 1 && gunes.durdurma == 1, "durdur() çağrılmadı");
            yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.NORMAL, "güneş süreci durunca BUYUYEN geri alınmalı");
            yardimci.assertTrue(yonetici.aktifDonemSurecleri().isEmpty(), "süreçler aktif kaldı");
            yardimci.assertTrue(komut(yardimci, "dogalafet stop") == 0, "durdurulacak bir şey yokken stop 0 dönmeli");

            ay.aktif = true;
            gunes.aktif = true;
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(true, server);
            yardimci.assertTrue(yonetici.kurallariDenetle().isEmpty(), "kurallar açıkken süreç durmamalı");
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(false, server);
            yardimci.assertTrue(yonetici.kurallariDenetle().equals(List.of(ay)), "ay çarpması kuralı kapanınca ay süreci durmalı");
            yardimci.assertTrue(ay.durdurma == 2 && gunes.durdurma == 1, "yalnız kuralı kapanan süreç durmalı");
        } finally {
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(oncekiAy, server);
            donem.gunesEvresiAyarla(GunesEvresi.NORMAL);
        }
        yardimci.succeed();
    }

    @GameTest(template = "bos", batch = "cekirdek_siren", timeoutTicks = 200)
    public static void sirenSirasindaCikipGireneKalanSureyleYenidenGider(GameTestHelper yardimci) {
        ServerLevel level = yardimci.getLevel();
        MinecraftServer server = level.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        BlockPos merkez = yardimci.absolutePos(BlockPos.ZERO);
        GameProfile profil = new GameProfile(UUID.randomUUID(), "siren-test");
        ServerPlayer ilk = sahteOyuncu(yardimci, profil, merkez.getX() + 0.5, merkez.getY(), merkez.getZ() + 0.5);
        List<SirenIstegiEvent> istekler = new ArrayList<>();
        Consumer<SirenIstegiEvent> dinleyici = istekler::add;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, SirenIstegiEvent.class, dinleyici);
        AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(AfetTuru.DEPREM,
                () -> new OyunTestiAfeti(AfetTuru.DEPREM, 16, 100));
        ServerPlayer[] ikinci = new ServerPlayer[1];
        Runnable temizle = () -> {
            MinecraftForge.EVENT_BUS.unregister(dinleyici);
            yonetici.durdur();
            kayit.close();
            if (ikinci[0] != null) {
                server.getPlayerList().remove(ikinci[0]);
            }
        };
        try {
            yardimci.assertTrue(yonetici.elleBaslat(AfetTuru.DEPREM, 1, ilk, level, merkez)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "afet başlamadı");
            yonetici.asamaIleri();
            yardimci.assertTrue(istekler.size() == 1 && istekler.get(0).oyuncular().contains(ilk),
                    "SIREN başlarken oyuncuya siren gitmeli");
            server.getPlayerList().remove(ilk);
            ikinci[0] = sahteOyuncu(yardimci, profil, merkez.getX() + 0.5, merkez.getY(), merkez.getZ() + 0.5);
        } catch (RuntimeException hata) {
            temizle.run();
            throw hata;
        }
        int ilkSure = istekler.get(0).sureTick();
        yardimci.succeedWhen(() -> {
            yardimci.assertTrue(istekler.size() >= 2, "yeniden giren oyuncuya siren isteği henüz gitmedi");
            SirenIstegiEvent son = istekler.get(istekler.size() - 1);
            yardimci.assertTrue(son.oyuncular().stream().anyMatch(o -> o.getUUID().equals(profil.getId())),
                    "ikinci istek yeniden giren oyuncuya değil");
            yardimci.assertTrue(son.sureTick() < ilkSure, "ikinci istek kalan süreyle gitmeli: " + son.sureTick());
            temizle.run();
        });
    }
}
