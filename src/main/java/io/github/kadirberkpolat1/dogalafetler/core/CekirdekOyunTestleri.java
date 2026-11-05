package io.github.kadirberkpolat1.dogalafetler.core;

import com.mojang.authlib.GameProfile;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonumTipi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.ZamanlayiciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
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
        ServerLevel level = yardimci.getLevel();
        ServerPlayer oyuncu = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "cekirdek-test"));
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
}
