package io.github.kadirberkpolat1.dogalafetler.core;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.CekirdekAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.AfetZamanlayicisi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.OyunGunu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SeviyeDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SiddetAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SiddetSecici;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonrakiAfetTahmini;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonumTipi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.ZamanlayiciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.TekSeferlikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.SirenIstegiEvent;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenKarari;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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

    /**
     * K-E16: vanilla GameTest yerleşimi bir batch'in 8. testinde satır kırar ve yeni satırın derinliğini yalnız o batch'in
     * şablonlarından hesaplar; aynı satırdaki daha derin sahneler (ör. 32x32 donma gölü) yeni satırın alan temizliğiyle kesilir ve
     * suyu komşu sahnelere akar. Batch başına en çok 7 test: bütün sahneler tek satırda kalır.
     */
    @GameTest(template = "bos", batch = "cekirdek_yerlesim")
    public static void hicBirBatchSatirKirmaz(GameTestHelper yardimci) {
        Map<String, Long> sayim = GameTestRegistry.getAllTestFunctions().stream()
                .collect(Collectors.groupingBy(TestFunction::getBatchName, TreeMap::new, Collectors.counting()));
        List<String> buyuk = sayim.entrySet().stream().filter(e -> e.getValue() >= 8)
                .map(e -> e.getKey() + "=" + e.getValue()).toList();
        yardimci.assertTrue(buyuk.isEmpty(), "8 ve üstü testli batch satır kırar (K-E16): " + buyuk);
        yardimci.succeed();
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
        donem.bekleme().afetBitti(AfetTuru.DEPREM, 4, donem.saat(), ZamanlayiciAyarlari.VARSAYILAN, 1.0);
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
        donem.afetEkle(afet);
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

        List<CompoundTag> okunanlar = geri.bekleyenAfetleriAl();
        yardimci.assertTrue(okunanlar.size() == 1, "kayıtta bir afet olmalı: " + okunanlar.size());
        CompoundTag okunanAfet = okunanlar.get(0);
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

    private static int komut(MinecraftServer server, String komut) {
        try {
            return server.getCommands().getDispatcher().execute(komut,
                    server.createCommandSourceStack().withSuppressedOutput().withPermission(2));
        } catch (CommandSyntaxException hata) {
            return -1;
        }
    }

    /**
     * Kullanıcı şikayeti (2026-11-08, "afet sıklığı komutu çalışmıyor"): {@code /dogalafet siklik 1000} gamerule'ü yazar,
     * süren beklemeleri hemen onda bire ölçekler, ölçek dönem kaydında ve gamerule dünya kaydında kalır, sonraki afet
     * tahmini kısalır. {@code /gamerule afetSikligi} yolu aynı ölçeklemeyi yapar; komutta aralık dışı değer reddedilir.
     */
    @GameTest(template = "bos", batch = "cekirdek_siklik")
    public static void siklikKomutuBeklemeyiOlcekleriKaliciTahminKisalir(GameTestHelper yardimci) {
        MinecraftServer server = yardimci.getLevel().getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        DunyaDonemi donem = yonetici.donem();
        GameRules kurallar = server.getGameRules();
        GameRules.IntegerValue kural = kurallar.getRule(Kurallar.AFET_SIKLIGI);
        int onceki = kural.get();
        boolean oncekiGenel = kurallar.getBoolean(Kurallar.DOGAL_AFETLER_ACIK);
        try {
            kurallar.getRule(Kurallar.DOGAL_AFETLER_ACIK).set(true, server);
            kural.set(100, server);
            yonetici.siklikDenetle(false);
            donem.beklemeleriSifirla();
            ZamanlayiciAyarlari ayar = CekirdekAyarlari.al().zamanlayici();
            donem.bekleme().afetBitti(AfetTuru.DEPREM, 1, donem.saat(), ayar, donem.uygulananSiklik());
            long genel = donem.genelBeklemeKalan();
            long deprem = donem.turBeklemeKalan(AfetTuru.DEPREM);
            yardimci.assertTrue(genel == ayar.genelBeklemeTick(), "%100'de genel bekleme config değeri olmalı: " + genel);
            SonrakiAfetTahmini normal = yonetici.sonrakiAfetTahmini().orElse(null);
            yardimci.assertTrue(normal != null && normal.enErken() >= genel, "%100 tahmini: " + normal);

            yardimci.assertTrue(komut(server, "dogalafet siklik 1000") == 1, "/dogalafet siklik 1000 başarısız");
            yardimci.assertTrue(kural.get() == 1000, "gamerule yazılmadı: " + kural.get());
            yardimci.assertTrue(donem.uygulananSiklik() == 10.0, "uygulanan sıklık: " + donem.uygulananSiklik());
            yardimci.assertTrue(Math.abs(donem.genelBeklemeKalan() - genel / 10.0) <= 1,
                    "genel bekleme onda bire inmedi: " + genel + " -> " + donem.genelBeklemeKalan());
            yardimci.assertTrue(Math.abs(donem.turBeklemeKalan(AfetTuru.DEPREM) - deprem / 10.0) <= 1,
                    "deprem beklemesi onda bire inmedi: " + deprem + " -> " + donem.turBeklemeKalan(AfetTuru.DEPREM));
            SonrakiAfetTahmini on = yonetici.sonrakiAfetTahmini().orElse(null);
            long baslangic = Math.max(0, AfetZamanlayicisi.beklemeOlcekle(ayar.baslangicBeklemeTick(), 10) - donem.saat());
            yardimci.assertTrue(on != null && on.enErken() == Math.max(baslangic, donem.genelBeklemeKalan()),
                    "%1000 en erken = bekleme bitişi olmalı: " + on);
            yardimci.assertTrue(on.ortalama() * 5 < normal.ortalama(), "%1000 tahmini kısalmadı: " + normal + " -> " + on);

            DunyaDonemi geri = DunyaDonemi.yukle(donem.save(new CompoundTag()));
            yardimci.assertTrue(geri.uygulananSiklik() == 10.0 && geri.genelBeklemeKalan() == donem.genelBeklemeKalan(),
                    "ölçek dönem kaydında kalmadı: " + geri.uygulananSiklik() + " " + geri.genelBeklemeKalan());
            yardimci.assertTrue("1000".equals(kurallar.createTag().getString(Kurallar.AFET_SIKLIGI.getId())),
                    "afetSikligi dünya kaydına (level.dat) yazılmadı");

            long onluk = donem.genelBeklemeKalan();
            komut(server, "gamerule afetSikligi 50");
            yardimci.assertTrue(yonetici.siklikDenetle(true), "/gamerule değişimi görülmedi");
            yardimci.assertTrue(donem.uygulananSiklik() == 0.5 && Math.abs(donem.genelBeklemeKalan() - onluk * 20) <= 20,
                    "/gamerule 50 beklemeyi yirmi katına çıkarmadı: " + onluk + " -> " + donem.genelBeklemeKalan());
            komut(server, "gamerule afetSikligi 999999999");
            yonetici.siklikDenetle(false);
            yardimci.assertTrue(donem.uygulananSiklik() == 10.0 && SiklikBilgisi.sinirUyarisi(yonetici).isPresent(),
                    "1000 üstü değer %1000'e kısılmalı ve söylenmeli");

            yardimci.assertTrue(komut(server, "dogalafet siklik 1001") == -1, "komutta 1001 reddedilmeli");
            yardimci.assertTrue(komut(server, "dogalafet siklik -1") == -1, "komutta -1 reddedilmeli");
            yardimci.assertTrue(komut(server, "dogalafet siklik") == 1, "/dogalafet siklik (okuma) başarısız");
            yardimci.assertTrue(komut(server, "dogalafet durum") == 1, "/dogalafet durum başarısız");
            yardimci.assertTrue(komut(server, "dogalafet") == 1, "alt komutsuz /dogalafet kullanım listesi vermeli");
        } finally {
            kural.set(onceki, server);
            kurallar.getRule(Kurallar.DOGAL_AFETLER_ACIK).set(oncekiGenel, server);
            yonetici.siklikDenetle(false);
            donem.beklemeleriSifirla();
        }
        yardimci.succeed();
    }

    /**
     * AS-8 (kullanıcı isteği 2026-11-08): komutla iki farklı afet aynı anda AKTIF olur, aynı tür reddedilir, {@code stop <afet>}
     * yalnız onu durdurur, doğal seçim yalnız boş yuvada açılır (K-E67) ve dönem kaydı iki afeti birden taşır.
     */
    @GameTest(template = "bos", batch = "cekirdek_coklu")
    public static void komutlaIkiFarkliAfetAyniAndaSurer(GameTestHelper yardimci) {
        ServerLevel level = yardimci.getLevel();
        MinecraftServer server = level.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        GameRules kurallar = server.getGameRules();
        boolean oncekiGenel = kurallar.getBoolean(Kurallar.DOGAL_AFETLER_ACIK);
        BlockPos konum = yardimci.absolutePos(BlockPos.ZERO);
        ServerPlayer oyuncu = sahteOyuncu(yardimci, konum.getX() + 0.5, konum.getY(), konum.getZ() + 0.5);
        try (AfetKaydi.GeciciKayit deprem = AfetKaydi.geciciKaydet(AfetTuru.DEPREM,
                () -> new OyunTestiAfeti(AfetTuru.DEPREM, 8, 100));
                AfetKaydi.GeciciKayit kasirga = AfetKaydi.geciciKaydet(AfetTuru.KASIRGA,
                        () -> new OyunTestiAfeti(AfetTuru.KASIRGA, 8, 100))) {
            kurallar.getRule(Kurallar.DOGAL_AFETLER_ACIK).set(true, server);
            yardimci.assertTrue(yonetici.elleBaslat(AfetTuru.DEPREM, 2, oyuncu, level, konum)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "deprem başlamadı");
            yardimci.assertTrue(yonetici.elleBaslat(AfetTuru.KASIRGA, 1, oyuncu, level, konum)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "deprem sürerken kasırga başlamadı");
            yardimci.assertTrue(yonetici.elleBaslat(AfetTuru.DEPREM, 1, oyuncu, level, konum)
                    == AfetYoneticisi.BaslatmaSonucu.ZATEN_AKTIF, "aynı türden ikinci afet reddedilmedi");
            yardimci.assertTrue(komut(server, "dogalafet start deprem 3") == 0, "komutla aynı tür reddedilmedi");
            yardimci.assertTrue(yonetici.aktifAfetler().size() == 2, "iki afet sürmeli: " + yonetici.aktifAfetler().size());
            for (int i = 0; i < 2; i++) {
                yardimci.assertTrue(yonetici.asamaIleri(AfetTuru.KASIRGA) && yonetici.asamaIleri(AfetTuru.DEPREM),
                        "hedefli aşama ilerlemedi");
            }
            Afet d = yonetici.aktifAfet(AfetTuru.DEPREM).orElseThrow();
            Afet k = yonetici.aktifAfet(AfetTuru.KASIRGA).orElseThrow();
            yardimci.assertTrue(d.baglam().baslangicSaati() != k.baglam().baslangicSaati(),
                    "aynı tick'te başlayan iki afet farklı afetNo almalı (AS-16)");
            yardimci.assertTrue(d.asama() == AfetAsamasi.AKTIF && k.asama() == AfetAsamasi.AKTIF && d.seviye() == 2,
                    "iki farklı afet aynı anda AKTIF olmalı: " + d.asama() + " / " + k.asama());
            Optional<AfetTuru> yanina = yonetici.secimYap(true);
            yardimci.assertTrue(yonetici.dogalYuvaDolu() ? yanina.isEmpty()
                    : yanina.map(t -> t != AfetTuru.DEPREM && t != AfetTuru.KASIRGA && !t.sunucuGeneli()).orElse(true),
                    "afet sürerken doğal seçim yalnız boş yuvaya, sürmeyen bölgesel türden olmalı (K-E67): " + yanina);

            List<CompoundTag> kayit = DunyaDonemi.yukle(yonetici.donem().save(new CompoundTag())).bekleyenAfetleriAl();
            yardimci.assertTrue(kayit.size() == 2 && Afet.turOku(kayit.get(0)).equals(Optional.of(AfetTuru.DEPREM))
                    && Afet.turOku(kayit.get(1)).equals(Optional.of(AfetTuru.KASIRGA)), "dönem kaydı iki afeti taşımalı");
            OyunTestiAfeti geri = new OyunTestiAfeti(AfetTuru.KASIRGA, 8, 100);
            geri.durumOku(kayit.get(1));
            yardimci.assertTrue(geri.asama() == AfetAsamasi.AKTIF && geri.baglam().equals(k.baglam()),
                    "kayıttan dönen kasırga aynı olmalı");

            yardimci.assertTrue(komut(server, "dogalafet durum") == 1, "durum başarısız");
            yardimci.assertTrue(komut(server, "dogalafet stop deprem") == 1, "stop deprem başarısız");
            yardimci.assertTrue(yonetici.aktifAfet(AfetTuru.DEPREM).isEmpty() && yonetici.aktifAfet(AfetTuru.KASIRGA)
                    .orElse(null) == k, "stop deprem yalnız depremi durdurmalı");
            yardimci.assertTrue(komut(server, "dogalafet stop deprem") == 0, "sürmeyen tür için stop 0 dönmeli");
            yardimci.assertTrue(komut(server, "dogalafet asama ileri kasirga") == 1, "asama ileri kasirga başarısız");
            yardimci.assertTrue(komut(server, "dogalafet stop") == 1 && yonetici.aktifAfetler().isEmpty(),
                    "stop hepsini durdurmalı");
            yardimci.assertTrue(yonetici.secimYap(true).isPresent(), "liste boşalinca doğal seçim açılmalı");
        } finally {
            yonetici.durdur();
            kurallar.getRule(Kurallar.DOGAL_AFETLER_ACIK).set(oncekiGenel, server);
            server.getPlayerList().remove(oyuncu);
        }
        yardimci.succeed();
    }

    /**
     * K-E67 (kullanıcı isteği "gün geçtikçe sayıları artsın"): gün 15'ten önce tek, 15–39 arası iki, 40'tan sonra üç doğal
     * afet aynı anda sürer; etki alanı süren afetinkiyle çakışan doğal afet başlamaz; TPS kısılıyken ikinci afet ertelenir;
     * komutla başlatma bunlara bağlı değildir; yoğunluk yalnız doğal başlangıçları sayar.
     */
    @GameTest(template = "bos", batch = "cekirdek_eszamanli")
    public static void dogalAfetlerGunleEszamanliCakismadanBaslar(GameTestHelper yardimci) {
        ServerLevel level = yardimci.getLevel();
        MinecraftServer server = level.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        DunyaDonemi donem = yonetici.donem();
        long oncekiOfset = donem.gunOfseti();
        BlockPos konum = yardimci.absolutePos(BlockPos.ZERO);
        ServerPlayer a = sahteOyuncu(yardimci, konum.getX() + 0.5, konum.getY(), konum.getZ() + 0.5);
        ServerPlayer b = sahteOyuncu(yardimci, konum.getX() + 300.5, konum.getY(), konum.getZ() + 0.5);
        ServerPlayer c = sahteOyuncu(yardimci, konum.getX() + 600.5, konum.getY(), konum.getZ() + 0.5);
        try (AfetKaydi.GeciciKayit deprem = AfetKaydi.geciciKaydet(AfetTuru.DEPREM,
                () -> new OyunTestiAfeti(AfetTuru.DEPREM, 8, 6000));
                AfetKaydi.GeciciKayit kasirga = AfetKaydi.geciciKaydet(AfetTuru.KASIRGA,
                        () -> new OyunTestiAfeti(AfetTuru.KASIRGA, 8, 6000));
                AfetKaydi.GeciciKayit sinkhole = AfetKaydi.geciciKaydet(AfetTuru.SINKHOLE,
                        () -> new OyunTestiAfeti(AfetTuru.SINKHOLE, 8, 6000))) {
            gunAyarla(server, donem, 5);
            int dogalOnce = donem.sonGunDogalAfetSayisi();
            yardimci.assertTrue(yonetici.dogalBaslat(AfetTuru.DEPREM, 1, a) == AfetYoneticisi.BaslatmaSonucu.BASLADI,
                    "ilk doğal afet başlamadı");
            yardimci.assertTrue(donem.sonGunDogalAfetSayisi() == dogalOnce + 1, "doğal başlangıç sayılmadı");
            yardimci.assertTrue(yonetici.dogalBaslat(AfetTuru.KASIRGA, 1, b) == AfetYoneticisi.BaslatmaSonucu.ZATEN_AKTIF,
                    "gün 5'te ikinci doğal afet başlamamalı");

            gunAyarla(server, donem, 20);
            yardimci.assertTrue(yonetici.dogalBaslat(AfetTuru.KASIRGA, 1, a) == AfetYoneticisi.BaslatmaSonucu.CAKISIYOR,
                    "süren depremle aynı yerde doğal kasırga çakışmalı");
            yardimci.assertTrue(yonetici.aktifAfet(AfetTuru.KASIRGA).isEmpty(), "çakışan afet kalmamalı");
            yardimci.assertTrue(yonetici.dogalBaslat(AfetTuru.DEPREM, 1, b) == AfetYoneticisi.BaslatmaSonucu.ZATEN_AKTIF,
                    "aynı tür ikinci kez başlamamalı");
            yardimci.assertTrue(yonetici.dogalBaslat(AfetTuru.KASIRGA, 1, b) == AfetYoneticisi.BaslatmaSonucu.BASLADI,
                    "gün 20'de uzaktaki ikinci doğal afet başlamalı");
            yardimci.assertTrue(yonetici.dogalBaslat(AfetTuru.SINKHOLE, 1, c) == AfetYoneticisi.BaslatmaSonucu.ZATEN_AKTIF,
                    "gün 20'de üçüncü doğal afet başlamamalı");
            yardimci.assertTrue(yonetici.secimYap(true).isEmpty(), "yuva doluyken seçim yapılmamalı");

            gunAyarla(server, donem, 45);
            TpsOlcer.testKismasi(0.5);
            yardimci.assertTrue(yonetici.dogalBaslat(AfetTuru.SINKHOLE, 1, c) == AfetYoneticisi.BaslatmaSonucu.ZATEN_AKTIF,
                    "TPS kısılıyken üçüncü afet ertelenmeli");
            TpsOlcer.testKismasi(null);
            yardimci.assertTrue(yonetici.dogalBaslat(AfetTuru.SINKHOLE, 1, c) == AfetYoneticisi.BaslatmaSonucu.BASLADI,
                    "gün 45'te üçüncü doğal afet başlamalı");
            yardimci.assertTrue(yonetici.aktifAfetler().size() == 3 && yonetici.dogalYuvaDolu(), "üç afet, yuva dolu");

            yonetici.durdur(AfetTuru.KASIRGA);
            int dogalSonra = donem.sonGunDogalAfetSayisi();
            yardimci.assertTrue(yonetici.elleBaslat(AfetTuru.KASIRGA, 1, a, level, konum)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "komutla başlatma çakışmaya bağlı olmamalı");
            yardimci.assertTrue(donem.sonGunDogalAfetSayisi() == dogalSonra, "komutla başlayan yoğunluğa sayılmamalı");
        } finally {
            TpsOlcer.testKismasi(null);
            yonetici.durdur();
            donem.gunOfsetiAyarla(oncekiOfset);
            server.getPlayerList().remove(a);
            server.getPlayerList().remove(b);
            server.getPlayerList().remove(c);
        }
        yardimci.succeed();
    }

    /**
     * Z4: tek oyuncunun çevresinde alanları çakışmayan iki farklı tür (gün 20, sınır 2) aynı anda doğal olarak başlar;
     * üçüncü tür sınırda kalır.
     */
    @GameTest(template = "bos", batch = "cekirdek_eszamanli_tek")
    public static void tekOyuncuCevresindeCakismayanIkiDogalAfetBaslar(GameTestHelper yardimci) {
        MinecraftServer server = yardimci.getLevel().getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        DunyaDonemi donem = yonetici.donem();
        long oncekiOfset = donem.gunOfseti();
        BlockPos konum = yardimci.absolutePos(BlockPos.ZERO);
        ServerPlayer oyuncu = sahteOyuncu(yardimci, konum.getX() + 0.5, konum.getY(), konum.getZ() + 0.5);
        try (AfetKaydi.GeciciKayit deprem = AfetKaydi.geciciKaydet(AfetTuru.DEPREM, () -> ofsetliAfet(AfetTuru.DEPREM, 40));
                AfetKaydi.GeciciKayit kasirga = AfetKaydi.geciciKaydet(AfetTuru.KASIRGA,
                        () -> ofsetliAfet(AfetTuru.KASIRGA, -40));
                AfetKaydi.GeciciKayit sinkhole = AfetKaydi.geciciKaydet(AfetTuru.SINKHOLE,
                        () -> ofsetliAfet(AfetTuru.SINKHOLE, 0))) {
            gunAyarla(server, donem, 20);
            yardimci.assertTrue(yonetici.dogalBaslat(AfetTuru.DEPREM, 1, oyuncu) == AfetYoneticisi.BaslatmaSonucu.BASLADI,
                    "ilk doğal afet başlamadı");
            yardimci.assertTrue(yonetici.dogalBaslat(AfetTuru.KASIRGA, 1, oyuncu) == AfetYoneticisi.BaslatmaSonucu.BASLADI,
                    "aynı oyuncunun öteki yanında, alanı çakışmayan ikinci doğal afet başlamalı");
            Afet d = yonetici.aktifAfet(AfetTuru.DEPREM).orElseThrow();
            Afet k = yonetici.aktifAfet(AfetTuru.KASIRGA).orElseThrow();
            yardimci.assertTrue(d.baglam().hedefOyuncu().equals(oyuncu.getUUID())
                    && k.baglam().hedefOyuncu().equals(oyuncu.getUUID()), "ikisi de aynı oyuncuyu hedeflemeli");
            yardimci.assertTrue(!yonetici.etkiAlani(d).orElseThrow().kesisir(yonetici.etkiAlani(k).orElseThrow()),
                    "alanlar çakışmamalı");
            yardimci.assertTrue(yonetici.dogalYuvaDolu(), "gün 20'de iki doğal afetle yuva dolu");
            yardimci.assertTrue(yonetici.dogalBaslat(AfetTuru.SINKHOLE, 1, oyuncu) == AfetYoneticisi.BaslatmaSonucu.ZATEN_AKTIF,
                    "üçüncü doğal afet başlamamalı");
        } finally {
            yonetici.durdur();
            donem.gunOfsetiAyarla(oncekiOfset);
            server.getPlayerList().remove(oyuncu);
        }
        yardimci.succeed();
    }

    /**
     * Z5: komutla başlatılan afet doğal yuvayı doldurmaz (gün 5, sınır 1): komutla deprem sürerken alanı çakışmayan doğal
     * afet başlar; ondan sonra yuva (doğal afetle) dolar.
     */
    @GameTest(template = "bos", batch = "cekirdek_eszamanli_komut")
    public static void komutAfetiDogalYuvayiDoldurmaz(GameTestHelper yardimci) {
        ServerLevel level = yardimci.getLevel();
        MinecraftServer server = level.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        DunyaDonemi donem = yonetici.donem();
        long oncekiOfset = donem.gunOfseti();
        BlockPos konum = yardimci.absolutePos(BlockPos.ZERO);
        ServerPlayer a = sahteOyuncu(yardimci, konum.getX() + 0.5, konum.getY(), konum.getZ() + 0.5);
        ServerPlayer b = sahteOyuncu(yardimci, konum.getX() + 300.5, konum.getY(), konum.getZ() + 0.5);
        try (AfetKaydi.GeciciKayit deprem = AfetKaydi.geciciKaydet(AfetTuru.DEPREM,
                () -> new OyunTestiAfeti(AfetTuru.DEPREM, 8, 6000));
                AfetKaydi.GeciciKayit kasirga = AfetKaydi.geciciKaydet(AfetTuru.KASIRGA,
                        () -> new OyunTestiAfeti(AfetTuru.KASIRGA, 8, 6000))) {
            gunAyarla(server, donem, 5);
            yardimci.assertTrue(yonetici.elleBaslat(AfetTuru.DEPREM, 1, a, level, konum)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "komutla deprem başlamadı");
            yardimci.assertTrue(!yonetici.dogalYuvaDolu(), "komut afeti doğal yuvayı doldurmamalı");
            yardimci.assertTrue(yonetici.dogalBaslat(AfetTuru.KASIRGA, 1, b) == AfetYoneticisi.BaslatmaSonucu.BASLADI,
                    "komut afeti sürerken doğal afet başlamalı");
            yardimci.assertTrue(yonetici.dogalYuvaDolu(), "gün 5'te bir doğal afetle yuva dolu");
        } finally {
            yonetici.durdur();
            donem.gunOfsetiAyarla(oncekiOfset);
            server.getPlayerList().remove(a);
            server.getPlayerList().remove(b);
        }
        yardimci.succeed();
    }

    /**
     * Z1: dünya kaydı sürüm 1'deki {@code sonSeviye} (eski kodda komutla başlatılan S5 de yazıyordu) yüklenirken bir kez
     * sıfırlanır: kullanıcının dünyasında {@code gun} olasılıkları S4/S5 > 0. Sürüm 2 kaydındaki (doğal) değer korunur, art
     * arda 4-5 kuralı aynen işler.
     */
    @GameTest(template = "bos", batch = "cekirdek_kayit_surum")
    public static void eskiKayittakiSonSeviyeBirKezSifirlanir(GameTestHelper yardimci) {
        DunyaDonemi donem = new DunyaDonemi();
        donem.sonSeviyeAyarla(5);
        CompoundTag etiket = donem.save(new CompoundTag());
        yardimci.assertTrue(etiket.getInt("surum") == DunyaDonemi.BICIM_SURUMU && DunyaDonemi.BICIM_SURUMU == 2,
                "kayıt biçimi 2");
        SiddetAyarlari ayar = CekirdekAyarlari.al().siddet();

        DunyaDonemi yeni = DunyaDonemi.yukle(etiket.copy());
        yardimci.assertTrue(yeni.sonSeviye() == 5, "sürüm 2 kaydı korunur: " + yeni.sonSeviye());
        SeviyeDurumu kural = SeviyeDurumu.hesapla(ayar, 100, 100, 5, SiddetSecici.yuksek(yeni.sonSeviye()), 1.0);
        SeviyeDurumu serbest = SeviyeDurumu.hesapla(ayar, 100, 100, 5, false, 1.0);
        yardimci.assertTrue(kural.olasilik(5) > 0 && kural.olasilik(5) < serbest.olasilik(5)
                && kural.olasilik(4) < serbest.olasilik(4), "doğal 5 sonrası art arda seyreltmesi");

        CompoundTag eski = etiket.copy();
        eski.putInt("surum", 1);
        DunyaDonemi geri = DunyaDonemi.yukle(eski);
        yardimci.assertTrue(geri.sonSeviye() == 0, "sürüm 1 kaydında sonSeviye sıfırlanmalı: " + geri.sonSeviye());
        SeviyeDurumu durum = SeviyeDurumu.hesapla(ayar, 100, 100, 5, SiddetSecici.yuksek(geri.sonSeviye()), 1.0);
        yardimci.assertTrue(durum.olasilik(4) > 0 && durum.olasilik(5) > 0,
                "eski kayıttan sonra gün 100'de S4/S5 > 0: " + durum.olasilik(4) + " " + durum.olasilik(5));
        CompoundTag yeniden = geri.save(new CompoundTag());
        yardimci.assertTrue(yeniden.getInt("surum") == 2 && yeniden.getInt("sonSeviye") == 0, "bir kez: yeni biçimle yazılır");
        yardimci.succeed();
    }

    /** Merkezi başlangıç noktasından (bu testlerde hedef oyuncunun bloğu) x ekseninde {@code dx} blok uzakta seçen test afeti (yarıçap 8). */
    private static OyunTestiAfeti ofsetliAfet(AfetTuru tur, int dx) {
        return new OyunTestiAfeti(tur, 8, 6000) {
            @Override
            public BlockPos merkezSec(ServerLevel level, ServerPlayer hedef, int seviye, BlockPos merkezNoktasi,
                    RandomSource rastgele) {
                return merkezNoktasi.offset(dx, 0, 0);
            }
        };
    }

    private static void gunAyarla(MinecraftServer server, DunyaDonemi donem, long gun) {
        donem.gunOfsetiAyarla(OyunGunu.ofset(gun, server.overworld().getGameTime(), donem.uykuBirikimi()));
    }

    /**
     * AS-9: oyuncu bir afetin sirenini duyarken aynı ya da düşük öncelikli ikinci afetin sireni ona gitmez; ilk siren bitince
     * de gitmez (susar, kuyruğa alınmaz).
     */
    @GameTest(template = "bos", batch = "cekirdek_coklu_siren")
    public static void ayniAndaIkiSirendeOncelikliCalarOtekiSusar(GameTestHelper yardimci) {
        ServerLevel level = yardimci.getLevel();
        MinecraftServer server = level.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        BlockPos konum = yardimci.absolutePos(BlockPos.ZERO);
        ServerPlayer oyuncu = sahteOyuncu(yardimci, konum.getX() + 0.5, konum.getY(), konum.getZ() + 0.5);
        List<SirenIstegiEvent> istekler = new ArrayList<>();
        Consumer<SirenIstegiEvent> dinleyici = istekler::add;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, SirenIstegiEvent.class, dinleyici);
        try (AfetKaydi.GeciciKayit deprem = AfetKaydi.geciciKaydet(AfetTuru.DEPREM,
                () -> new OyunTestiAfeti(AfetTuru.DEPREM, 8, 100));
                AfetKaydi.GeciciKayit kasirga = AfetKaydi.geciciKaydet(AfetTuru.KASIRGA,
                        () -> new OyunTestiAfeti(AfetTuru.KASIRGA, 8, 100))) {
            yonetici.elleBaslat(AfetTuru.DEPREM, 1, oyuncu, level, konum);
            yonetici.elleBaslat(AfetTuru.KASIRGA, 1, oyuncu, level, konum);
            yonetici.asamaIleri(AfetTuru.DEPREM);
            yardimci.assertTrue(istekler.stream().anyMatch(e -> e.afetTuru() == AfetTuru.DEPREM
                    && e.oyuncular().contains(oyuncu)), "deprem sireni oyuncuya gitmedi");
            yonetici.asamaIleri(AfetTuru.KASIRGA);
            yardimci.assertTrue(yonetici.aktifAfet(AfetTuru.KASIRGA).orElseThrow().asama() == AfetAsamasi.SIREN,
                    "kasırga SIREN'de olmalı");
            yonetici.asamaIleri(AfetTuru.DEPREM);
            yardimci.assertTrue(istekler.stream().noneMatch(e -> e.afetTuru() == AfetTuru.KASIRGA),
                    "eşit öncelikte sonra başlayan kasırganın sireni gitmemeli: " + istekler.size());
        } finally {
            MinecraftForge.EVENT_BUS.unregister(dinleyici);
            yonetici.durdur();
            server.getPlayerList().remove(oyuncu);
        }
        yardimci.succeed();
    }

    /**
     * AS-16 (inceleme As7): aynı tick'te başlayan kasırga ve ay çarpması farklı afetNo alır; ikisinin de siren isteği gider ve
     * istemci kararı yüksek öncelikli ay sirenini "zaten çalıyor" diye atlamaz; sayaç dönem kaydında kalır.
     */
    @GameTest(template = "bos", batch = "cekirdek_coklu_siren")
    public static void ayniTicktekiIkiAfetFarkliNoVeIkiSiren(GameTestHelper yardimci) {
        ServerLevel level = yardimci.getLevel();
        MinecraftServer server = level.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        BlockPos konum = yardimci.absolutePos(BlockPos.ZERO);
        ServerPlayer oyuncu = sahteOyuncu(yardimci, konum.getX() + 0.5, konum.getY(), konum.getZ() + 0.5);
        List<SirenIstegiEvent> istekler = new ArrayList<>();
        Consumer<SirenIstegiEvent> dinleyici = istekler::add;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, SirenIstegiEvent.class, dinleyici);
        try (AfetKaydi.GeciciKayit kasirga = AfetKaydi.geciciKaydet(AfetTuru.KASIRGA,
                () -> new OyunTestiAfeti(AfetTuru.KASIRGA, 8, 100));
                AfetKaydi.GeciciKayit ay = AfetKaydi.geciciKaydet(AfetTuru.AY_CARPMASI,
                        () -> new OyunTestiAfeti(AfetTuru.AY_CARPMASI, 8, 100))) {
            long saat = yonetici.donem().saat();
            yardimci.assertTrue(yonetici.elleBaslat(AfetTuru.KASIRGA, 1, oyuncu, level, konum)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "kasırga başlamadı");
            yardimci.assertTrue(yonetici.elleBaslat(AfetTuru.AY_CARPMASI, null, null, level, konum)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "ay çarpması başlamadı");
            yardimci.assertTrue(yonetici.donem().saat() == saat, "iki başlatma aynı tick'te olmalı");
            long kasirgaNo = yonetici.aktifAfet(AfetTuru.KASIRGA).orElseThrow().baglam().baslangicSaati();
            long ayNo = yonetici.aktifAfet(AfetTuru.AY_CARPMASI).orElseThrow().baglam().baslangicSaati();
            yardimci.assertTrue(kasirgaNo != ayNo && kasirgaNo >= saat && ayNo >= saat,
                    "farklı afetNo bekleniyordu: " + kasirgaNo + " / " + ayNo + " (saat " + saat + ")");
            yonetici.asamaIleri(AfetTuru.KASIRGA);
            yonetici.asamaIleri(AfetTuru.AY_CARPMASI);
            yardimci.assertTrue(istekler.stream().anyMatch(e -> e.afetTuru() == AfetTuru.KASIRGA && e.oyuncular().contains(oyuncu))
                    && istekler.stream().anyMatch(e -> e.afetTuru() == AfetTuru.AY_CARPMASI
                            && e.oyuncular().contains(oyuncu)), "iki siren isteği de oyuncuya gitmeli: " + istekler.size());
            yardimci.assertTrue(SirenKarari.istek(true, false, kasirgaNo, ayNo) == SirenKarari.KES_VE_BASLAT,
                    "istemci ay sirenini kasırga sireni çalarken kabul etmeli");
            DunyaDonemi geri = DunyaDonemi.yukle(yonetici.donem().save(new CompoundTag()));
            long sonraki = geri.yeniAfetNo();
            yardimci.assertTrue(sonraki > Math.max(kasirgaNo, ayNo) + 1, "sayaç kayıtta kalmalı: " + sonraki);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(dinleyici);
            yonetici.durdur();
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
            yardimci.assertTrue(yonetici.tekSeferlikUygun(AfetTuru.GUNES_PATLAMASI, 1000, 1000,
                    TekSeferlikAyarlari.VARSAYILAN), "güneş patlaması yeniden seçilebilir kalmalı");

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

    @AfterBatch(batch = "cekirdek_gercek_kural")
    public static void gercekKuralSonrasi(ServerLevel level) {
        MinecraftServer server = level.getServer();
        OyunTestiTemizligi.batchSonu(level);
        server.getGameRules().getRule(Kurallar.GUNES_PATLAMASI_AFETI).set(true, server);
        AfetYoneticisi.al(server).sifirla();
    }

    /**
     * K-C7 ve K-C41 gerçek güneş patlamasıyla (madde 11; izleme-inceleme-5 P2-2). Seçimin başlattığı (elle olmayan) afet
     * ON_UYARI'da kural kapanınca {@code IPTAL} ile kalkar; AKTIF'te {@code KURAL} ile yumuşak durur ve tamamlanmamış büyüme
     * {@code NORMAL}'e döner (K-C26). Doğal bitişten sonra güneş zinciri kural kapanınca {@code KURAL} ile durur ve ilerlemez,
     * açılınca sürer.
     */
    @GameTest(template = "bos", batch = "cekirdek_gercek_kural", timeoutTicks = 400)
    public static void gercekGunesKuralKapanincaIptalKuralVeDevam(GameTestHelper yardimci) {
        MinecraftServer server = yardimci.getLevel().getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        yonetici.durdur();
        yonetici.sifirla();
        GameRules.BooleanValue kural = server.getGameRules().getRule(Kurallar.GUNES_PATLAMASI_AFETI);
        List<BitisNedeni> nedenler = new ArrayList<>();
        OyunTestiTemizligi.dinle(AfetBittiEvent.class, olay -> {
            if (olay.tur() == AfetTuru.GUNES_PATLAMASI) {
                nedenler.add(olay.neden());
            }
        });
        DonemSureci zincir = AfetYoneticisi.donemSurecleri().stream().filter(s -> s.kimlik().equals("gunes_zinciri"))
                .findFirst().orElseThrow();
        GunesEvresi[] donukEvre = new GunesEvresi[1];
        float[] donuk = new float[1];
        kural.set(true, server);
        gercekGunesBaslat(yardimci, yonetici);
        kural.set(false, server);
        yardimci.startSequence()
                .thenWaitUntil(() -> yardimci.assertTrue(yonetici.aktifAfet().isEmpty(),
                        "ON_UYARI'daki afet kural kapanınca kalkmadı"))
                .thenExecute(() -> {
                    yardimci.assertTrue(nedenler.equals(List.of(BitisNedeni.IPTAL)) && donem.gunesEvresi() == GunesEvresi.NORMAL,
                            "ON_UYARI'da kural kapanınca IPTAL ve evre NORMAL: " + nedenler + ", " + donem.gunesEvresi());
                    kural.set(true, server);
                    gercekGunesBaslat(yardimci, yonetici);
                    yonetici.asamaIleri();
                    yonetici.asamaIleri();
                    yardimci.assertTrue(yonetici.aktifAfet().map(Afet::asama).orElse(null) == AfetAsamasi.AKTIF
                            && donem.gunesEvresi() == GunesEvresi.BUYUYEN, "AKTIF'te güneş büyümeli: " + donem.ozet());
                    kural.set(false, server);
                })
                .thenWaitUntil(() -> yardimci.assertTrue(yonetici.aktifAfet().isEmpty(),
                        "AKTIF sunucu geneli afet kural kapanınca durmadı"))
                .thenExecute(() -> {
                    yardimci.assertTrue(nedenler.size() == 2 && nedenler.get(1) == BitisNedeni.KURAL,
                            "AKTIF'te KURAL bitişi bekleniyordu: " + nedenler);
                    yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.NORMAL,
                            "tamamlanmamış büyüme NORMAL'e dönmeli (K-C26): " + donem.gunesEvresi());
                    kural.set(true, server);
                    gercekGunesBaslat(yardimci, yonetici);
                    for (int i = 0; i < 8 && yonetici.aktifAfet().isPresent(); i++) {
                        yonetici.asamaIleri();
                    }
                    yardimci.assertTrue(yonetici.aktifAfet().isEmpty() && donem.gunesEvresi() == GunesEvresi.SUPERNOVA
                            && zincir.aktifMi(server.overworld()), "doğal bitiş süpernova ve güneş zinciri: " + donem.ozet());
                })
                // Zincir yeni döngüsünü kendi ilk tick'inde kaydeder (eski durdurma kaydını siler); kural ondan sonra kapanır.
                .thenExecuteAfter(2, () -> {
                    kural.set(false, server);
                    yardimci.assertTrue(yonetici.kurallariDenetle().contains(zincir)
                            && yonetici.durdurmaNedeni(zincir).equals(Optional.of(DonemSureci.DurdurmaNedeni.KURAL)),
                            "kural kapanınca güneş zinciri KURAL ile durmalı");
                    donukEvre[0] = donem.gunesEvresi();
                    donuk[0] = donem.evreIlerlemesi();
                    yonetici.hizAyarla(1000);
                })
                .thenExecuteAfter(20, () -> {
                    yardimci.assertTrue(donem.gunesEvresi() == donukEvre[0] && donem.evreIlerlemesi() == donuk[0],
                            "kuralla durmuş zincir ilerledi: " + donem.ozet());
                    kural.set(true, server);
                    yonetici.kurallariDenetle();
                    yardimci.assertTrue(yonetici.durdurmaNedeni(zincir).isEmpty(), "kural açılınca durdurma kaydı silinmeli");
                })
                .thenWaitUntil(() -> yardimci.assertTrue(donem.gunesEvresi() != donukEvre[0] || donem.evreIlerlemesi() > donuk[0],
                        "kural açılınca zincir sürmedi: " + donem.ozet()))
                .thenExecute(() -> {
                    DogalAfetler.LOGGER.info("Çekirdek GameTest: gerçek güneş kural yolu {}; zincir sürdü: {}", nedenler, donem.ozet());
                    yonetici.sifirla();
                })
                .thenSucceed();
    }

    private static void gercekGunesBaslat(GameTestHelper yardimci, AfetYoneticisi yonetici) {
        yardimci.assertTrue(yonetici.secimleBaslat(AfetTuru.GUNES_PATLAMASI) == AfetYoneticisi.BaslatmaSonucu.BASLADI,
                "güneş patlaması başlamadı");
        Afet afet = yonetici.aktifAfet().orElseThrow();
        yardimci.assertTrue(!(afet instanceof OyunTestiAfeti) && !afet.baglam().elle(),
                "gerçek ve seçimle başlamış afet bekleniyordu: " + afet.getClass().getSimpleName());
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
            yardimci.assertTrue(komut(yardimci, "dogalafet stop") == 1 && yonetici.durdurmaNedeni(ay).equals(komutla)
                    && ay.sonNeden == DonemSureci.DurdurmaNedeni.KOMUT,
                    "kural ile durmuş sürece stop KOMUT'a çevirmeli ve sahibine durdur(KOMUT) bildirmeli (Af15)");
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

    /** Deprem için {@code AfetDurumPaketi.yok}'un ağdaki vanilla paketi (kanal adı ve ham baytı; ilk bayt paket kimliği). */
    private static ICustomPacket<?> yokPaketi() {
        return (ICustomPacket<?>) AgKanali.KANAL.toVanillaPacket(AfetDurumPaketi.yok(AfetTuru.DEPREM),
                NetworkDirection.PLAY_TO_CLIENT);
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
