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
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.network.ICustomPacket;
import net.minecraftforge.network.NetworkDirection;

/**
 * Çekirdeğin oyun içi testleri: dünya dönemi kaydı, kapalı gamerule'daki afetin seçilmemesi, afet bitiş olayının sağ
 * çıkanları, dönem süreçlerinin durdurulup sürdürülmesi ve afet kodu hata verince sunucunun sürmesi. Tek aktif afet
 * yuvasını kullanan testler kendi {@code batch}'indedir (batch'ler sırayla koşar).
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
        donem.uykuEkle(11_458);
        donem.surecDurdurmasiAyarla("oyuntesti_kayit", DonemSureci.DurdurmaNedeni.KURAL);

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
        yardimci.assertTrue(geri.uykuBirikimi() == 11_458, "uyku birikimi");
        yardimci.assertTrue(geri.surecDurdurmasi("oyuntesti_kayit").equals(Optional.of(DonemSureci.DurdurmaNedeni.KURAL)),
                "süreç durdurma kaydı");
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

    /** Durdurma, sürdürme ve aşama çağrılarını sayan sahte dönem süreci. */
    private static final class SahteSurec implements DonemSureci {
        private final AfetTuru tur;
        private boolean aktif = true;
        private int durdurma;
        private DurdurmaNedeni sonNeden;
        private int devam;
        private int asama;

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
        public void durdur(ServerLevel ustDunya, DurdurmaNedeni neden) {
            durdurma++;
            sonNeden = neden;
        }

        @Override
        public void devamEt(ServerLevel ustDunya) {
            devam++;
        }

        @Override
        public Optional<Component> asamaIleri(ServerLevel ustDunya) {
            asama++;
            return Optional.of(Component.literal(kimlik() + " aşama " + asama));
        }

        private void kaydiSil(DunyaDonemi donem) {
            donem.surecDurdurmasiAyarla(kimlik(), null);
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
            yardimci.assertTrue(ay.durdurma == 1 && gunes.durdurma == 1 && ay.sonNeden == DonemSureci.DurdurmaNedeni.KOMUT,
                    "durdur(KOMUT) çağrılmadı");
            yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.NORMAL, "güneş süreci durunca BUYUYEN geri alınmalı");
            yardimci.assertTrue(yonetici.aktifDonemSurecleri().isEmpty(), "süreçler aktif kaldı");
            yardimci.assertTrue(yonetici.durdurulmusDonemSurecleri().size() == 2, "iki süreç durdurulmuş görünmeli");
            yardimci.assertTrue(komut(yardimci, "dogalafet durum") == 1, "durum (duran süreçler) başarısız");
            yardimci.assertTrue(komut(yardimci, "dogalafet stop") == 0, "durdurulacak bir şey yokken stop 0 dönmeli");

            yonetici.donemSureciBasladi(ay);
            yonetici.donemSureciBasladi(gunes);
            yardimci.assertTrue(yonetici.aktifDonemSurecleri().size() == 2, "yeni döngü eski durdurma kaydını silmeli (Y4)");
            yardimci.assertTrue(komut(yardimci, "dogalafet asama ileri " + gunes.kimlik()) == 1 && gunes.asama == 1
                    && ay.asama == 0, "asama ileri <süreç> yalnız o süreci ilerletmeli (K-C48)");
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(true, server);
            yardimci.assertTrue(yonetici.kurallariDenetle().isEmpty(), "kurallar açıkken süreç durmamalı");
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(false, server);
            yardimci.assertTrue(yonetici.kurallariDenetle().equals(List.of(ay)), "ay çarpması kuralı kapanınca ay süreci durmalı");
            yardimci.assertTrue(ay.durdurma == 2 && gunes.durdurma == 1 && ay.sonNeden == DonemSureci.DurdurmaNedeni.KURAL,
                    "yalnız kuralı kapanan süreç durmalı");
            ay.aktif = false;
            yonetici.kurallariDenetle();
            yardimci.assertTrue(yonetici.durdurmaNedeni(ay).isEmpty(), "tamamlanan sürecin durdurma kaydı silinmeli");
        } finally {
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(oncekiAy, server);
            donem.gunesEvresiAyarla(GunesEvresi.NORMAL);
            ay.kaydiSil(donem);
            gunes.kaydiSil(donem);
        }
        yardimci.succeed();
    }

    /** K-C41: kural ile duran süreç kural açılınca sürer; stop ile duran sürmez, ama {@code asama ileri} onu ilerletir. */
    @GameTest(template = "bos", batch = "cekirdek_surec_devam")
    public static void kuraldanDuranSurecAcilincaSurerKomuttanDuranSurmez(GameTestHelper yardimci) {
        MinecraftServer server = yardimci.getLevel().getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        GameRules kurallar = server.getGameRules();
        boolean oncekiAy = kurallar.getBoolean(Kurallar.AY_CARPMASI_AFETI);
        Optional<DonemSureci.DurdurmaNedeni> kural = Optional.of(DonemSureci.DurdurmaNedeni.KURAL);
        Optional<DonemSureci.DurdurmaNedeni> komutla = Optional.of(DonemSureci.DurdurmaNedeni.KOMUT);
        yonetici.durdur();
        SahteSurec ay = new SahteSurec(AfetTuru.AY_CARPMASI);
        try (AfetKaydi.GeciciKayit kayit = AfetYoneticisi.geciciDonemSureciKaydet(ay)) {
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(false, server);
            yardimci.assertTrue(yonetici.kurallariDenetle().equals(List.of(ay)), "kural kapanınca süreç durmalı");
            yardimci.assertTrue(yonetici.durdurmaNedeni(ay).equals(kural) && ay.sonNeden == DonemSureci.DurdurmaNedeni.KURAL,
                    "durdurma nedeni KURAL olmalı");
            yardimci.assertTrue(komut(yardimci, "dogalafet durum") == 1, "durum başarısız");
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(true, server);
            yonetici.kurallariDenetle();
            yardimci.assertTrue(ay.devam == 1 && yonetici.durdurmaNedeni(ay).isEmpty()
                    && yonetici.aktifDonemSurecleri().contains(ay), "kural açılınca süreç kaldığı yerden sürmeli");

            yardimci.assertTrue(komut(yardimci, "dogalafet stop") == 1, "stop süreci durdurmalı");
            yardimci.assertTrue(yonetici.durdurmaNedeni(ay).equals(komutla), "durdurma nedeni KOMUT olmalı");
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(false, server);
            yonetici.kurallariDenetle();
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(true, server);
            yonetici.kurallariDenetle();
            yardimci.assertTrue(ay.devam == 1 && yonetici.durdurmaNedeni(ay).equals(komutla),
                    "stop ile duran süreç kural açılınca sürmemeli");
            yardimci.assertTrue(komut(yardimci, "dogalafet asama ileri") == 1 && ay.asama == 1,
                    "asama ileri duran süreci ilerletmeli");
            yardimci.assertTrue(yonetici.durdurmaNedeni(ay).equals(komutla), "asama ileri süreci sürdürmemeli");
            yardimci.assertTrue(DunyaDonemi.yukle(donem.save(new CompoundTag())).surecDurdurmasi(ay.kimlik()).equals(komutla),
                    "durdurma kaydı kalıcı olmalı");

            yonetici.donemSureciBasladi(ay);
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(false, server);
            yonetici.kurallariDenetle();
            yardimci.assertTrue(komut(yardimci, "dogalafet stop") == 1 && yonetici.durdurmaNedeni(ay).equals(komutla),
                    "kural ile durmuş sürece stop KOMUT'a çevirmeli");
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(true, server);
            yonetici.kurallariDenetle();
            yardimci.assertTrue(ay.devam == 1 && yonetici.durdurmaNedeni(ay).equals(komutla),
                    "KOMUT'a çevrilen süreç kural açılınca sürmemeli");
        } finally {
            kurallar.getRule(Kurallar.AY_CARPMASI_AFETI).set(oncekiAy, server);
            ay.kaydiSil(donem);
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

    /** K-C32 sınamasında istisna atacak kanca. */
    private enum Kanca {
        ETKI_ALANI, AKTIF_KALAN, ISTEMCI_VERISI, ASAMA_TICK, TEMIZLE
    }

    /** Seçilen kancası {@code patlasin} sonrasında istisna atan test afeti. */
    private static final class HataVerenAfet extends OyunTestiAfeti {
        private final Kanca kanca;
        private boolean patlasin;

        private HataVerenAfet(AfetTuru tur, Kanca kanca) {
            super(tur, 16, 100_000);
            this.kanca = kanca;
        }

        private void patla(Kanca cagrilan) {
            if (patlasin && kanca == cagrilan) {
                throw new IllegalStateException("oyun testi: kasıtlı hata (" + cagrilan + ")");
            }
        }

        @Override
        public EtkiAlani etkiAlani() {
            patla(Kanca.ETKI_ALANI);
            return super.etkiAlani();
        }

        @Override
        public long aktifKalanTick() {
            patla(Kanca.AKTIF_KALAN);
            return super.aktifKalanTick();
        }

        @Override
        protected void istemciVerisiYaz(CompoundTag etiket) {
            patla(Kanca.ISTEMCI_VERISI);
        }

        @Override
        protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
            patla(Kanca.ASAMA_TICK);
            super.asamaTick(level, asama, adim);
        }

        @Override
        protected void temizle(ServerLevel level, boolean temiz) {
            patla(Kanca.TEMIZLE);
            super.temizle(level, temiz);
        }
    }

    /** {@code AfetDurumPaketi.YOK}'un ağdaki vanilla paketi (kanal adı ve ham baytı; ilk bayt paket kimliği). */
    private static ICustomPacket<?> yokPaketi() {
        return (ICustomPacket<?>) AgKanali.KANAL.toVanillaPacket(AfetDurumPaketi.YOK, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static byte[] yokBaytlari() {
        return ByteBufUtil.getBytes(yokPaketi().getInternalData());
    }

    /** Sahte oyuncunun gömülü kanalına giden afet durum paketleri (ham, eskiden yeniye); kanalın giden kuyruğu boşalır. */
    private static List<byte[]> afetDurumPaketleri(ServerPlayer oyuncu) {
        ICustomPacket<?> yok = yokPaketi();
        byte kimlik = ByteBufUtil.getBytes(yok.getInternalData())[0];
        EmbeddedChannel kanal = (EmbeddedChannel) oyuncu.connection.connection.channel();
        List<byte[]> sonuc = new ArrayList<>();
        for (Object mesaj = kanal.readOutbound(); mesaj != null; mesaj = kanal.readOutbound()) {
            if (mesaj instanceof ICustomPacket<?> ozel && ozel.getName().equals(yok.getName())) {
                byte[] bayt = ByteBufUtil.getBytes(ozel.getInternalData());
                if (bayt.length > 0 && bayt[0] == kimlik) {
                    sonuc.add(bayt);
                }
            }
        }
        return sonuc;
    }

    /**
     * K-C32: afet {@code kanca}'sı {@code asama} aşamasında hata verince sunucu tick'i sürer, afet {@link BitisNedeni#HATA}
     * ile biter ve alandaki oyuncunun istemcisine son giden afet paketi {@code YOK} olur.
     */
    private static void afetKancasiHataVerince(GameTestHelper yardimci, Kanca kanca, AfetAsamasi asama) {
        ServerLevel level = yardimci.getLevel();
        MinecraftServer server = level.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        BlockPos merkez = yardimci.absolutePos(BlockPos.ZERO);
        ServerPlayer oyuncu = sahteOyuncu(yardimci, merkez.getX() + 0.5, merkez.getY(), merkez.getZ() + 0.5);
        List<AfetBittiEvent> olaylar = new ArrayList<>();
        Consumer<AfetBittiEvent> dinleyici = olaylar::add;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AfetBittiEvent.class, dinleyici);
        HataVerenAfet[] olusan = new HataVerenAfet[1];
        AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(AfetTuru.DEPREM, () -> {
            olusan[0] = new HataVerenAfet(AfetTuru.DEPREM, kanca);
            return olusan[0];
        });
        Runnable temizle = () -> {
            MinecraftForge.EVENT_BUS.unregister(dinleyici);
            yonetici.durdur();
            kayit.close();
            server.getPlayerList().remove(oyuncu);
        };
        byte[] yok = yokBaytlari();
        try {
            yardimci.assertTrue(yonetici.elleBaslat(AfetTuru.DEPREM, 1, oyuncu, level, merkez)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "afet başlamadı");
            while (olusan[0].asama() != asama) {
                yardimci.assertTrue(yonetici.asamaIleri(), "aşama ilerlemedi");
            }
            List<byte[]> once = afetDurumPaketleri(oyuncu);
            yardimci.assertTrue(!once.isEmpty() && !Arrays.equals(once.get(once.size() - 1), yok),
                    "oyuncu önce aktif afet paketi almalı");
            olusan[0].patlasin = true;
        } catch (RuntimeException hata) {
            temizle.run();
            throw hata;
        }
        yardimci.succeedWhen(() -> {
            yardimci.assertTrue(yonetici.aktifAfet().isEmpty(), "afet hata verince durmadı");
            yardimci.assertTrue(olaylar.size() == 1 && olaylar.get(0).neden() == BitisNedeni.HATA,
                    "HATA nedenli tek AfetBittiEvent bekleniyordu: " + olaylar.size());
            List<byte[]> sonra = afetDurumPaketleri(oyuncu);
            yardimci.assertTrue(!sonra.isEmpty() && Arrays.equals(sonra.get(sonra.size() - 1), yok),
                    "istemciye son giden afet paketi YOK olmalı");
            temizle.run();
        });
    }

    @GameTest(template = "bos", batch = "cekirdek_hata_kalan", timeoutTicks = 200)
    public static void aktifKalanTickHatasindaAfetHataylaBiter(GameTestHelper yardimci) {
        afetKancasiHataVerince(yardimci, Kanca.AKTIF_KALAN, AfetAsamasi.AKTIF);
    }

    @GameTest(template = "bos", batch = "cekirdek_hata_alan", timeoutTicks = 200)
    public static void etkiAlaniHatasindaAfetHataylaBiter(GameTestHelper yardimci) {
        afetKancasiHataVerince(yardimci, Kanca.ETKI_ALANI, AfetAsamasi.SIREN);
    }
}
