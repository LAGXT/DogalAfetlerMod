package io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimKimlikleri;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.AyDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.komut.DogalAfetKomutu;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAPI;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.network.ICustomPacket;
import net.minecraftforge.network.NetworkDirection;

/**
 * Ay çarpmasının oyun içi testleri (bölüm H, GOREV §6 #1–#4, #8, #12, #16). Gerçek {@link AyCarpmasi} afeti tek afet
 * yuvasını ve global ay alanını kullandığı için her test kendi {@code batch}'indedir; başta ve sonda dönemi sıfırlar
 * (K-E15). Yağmur başladığı tick'te afet bitirilir: sahneye meteor düşmez.
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AyCarpmasiOyunTestleri {
    private AyCarpmasiOyunTestleri() {
    }

    private static int komut(GameTestHelper y, String komut) {
        MinecraftServer server = y.getLevel().getServer();
        try {
            return server.getCommands().getDispatcher().execute(komut, server.createCommandSourceStack()
                    .withSuppressedOutput().withPermission(DogalAfetKomutu.YETKI_SEVIYESI));
        } catch (CommandSyntaxException hata) {
            y.fail("/" + komut + " çalışmadı: " + hata.getMessage());
            return -1;
        }
    }

    private static void dogrula(GameTestHelper y, boolean kosul, String mesaj) {
        if (!kosul) {
            y.fail(mesaj);
        }
    }

    private static AfetYoneticisi temizBasla(GameTestHelper y) {
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        yonetici.sifirla();
        return yonetici;
    }

    private static Afet baslat(GameTestHelper y, AfetYoneticisi yonetici) {
        dogrula(y, komut(y, "dogalafet start ay_carpmasi") == 1, "/dogalafet start ay_carpmasi başlamadı");
        Afet afet = yonetici.aktifAfet().orElseThrow();
        dogrula(y, afet instanceof AyCarpmasi && afet.tur() == AfetTuru.AY_CARPMASI && afet.seviye() == 0,
                "gerçek ay çarpması bekleniyordu: " + afet);
        return afet;
    }

    private static void sirenden(GameTestHelper y, AfetYoneticisi yonetici, Afet afet) {
        yonetici.asamaIleri();
        dogrula(y, afet.asama() == AfetAsamasi.SIREN, "SIREN'e geçmedi");
    }

    @GameTest(template = "bos", batch = "ay_carpmasi_zincir")
    public static void zincirSirenCarpmaYagmurDenizYukselmesi(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        ServerLevel level = y.getLevel();
        try {
            Afet afet = baslat(y, yonetici);
            dogrula(y, donem.ayDurumu() == AyDurumu.VAR, "ön-uyarıda ay yerinde (gizli pencere)");
            sirenden(y, yonetici, afet);
            dogrula(y, donem.ayDurumu() == AyDurumu.YAKLASIYOR && donem.ayOlaySaati() == donem.saat() && donem.ayVar(),
                    "sirenle ay yaklaşmalı: " + donem.ozet());
            dogrula(y, OrtamSesi.aktifMi(AyCarpmasi.YAKLASMA_KATMANI) && OrtamSesi.aktifMi(AyCarpmasi.CITIRTI_KATMANI),
                    "yaklaşma ortam katmanları");
            yonetici.asamaIleri();
            dogrula(y, afet.asama() == AfetAsamasi.AKTIF && afet.altAsama() == 0, "AKTIF'e (çarpma) geçmedi");
            dogrula(y, donem.ayDurumu() == AyDurumu.YOK && donem.ayOlaySaati() == donem.saat(),
                    "çarpmada ay yok olmalı (çarpma saatiyle): " + donem.ozet());
            dogrula(y, donem.carpan(AfetTuru.TSUNAMI) >= 3.0, "tsunami çarpanı çarpma anında: " + donem.carpan(AfetTuru.TSUNAMI));
            dogrula(y, !OrtamSesi.aktifMi(AyCarpmasi.YAKLASMA_KATMANI), "çarpmada yaklaşma sesi kesilir");
            dogrula(y, donem.ekVeri("su").contains("bekleme"), "deniz yükselmesinin 10 dk beklemesi kurulmalı");
            yonetici.asamaIleri();
            dogrula(y, afet.asama() == AfetAsamasi.AKTIF && afet.altAsama() == 1
                    && MeteorAPI.aktifYagmurSayisi(level) == 1, "asama ileri ay parçası yağmurunu başlatmalı");
            yonetici.asamaIleri();
            dogrula(y, afet.asama() == AfetAsamasi.BITIS, "yağmurdan sonra BITIS");
            yonetici.asamaIleri();
            dogrula(y, yonetici.aktifAfet().isEmpty() && MeteorAPI.aktifYagmurSayisi(level) == 0,
                    "doğal bitişte yağmur durur, yuva boşalır");
            dogrula(y, donem.ayDurumu() == AyDurumu.YOK, "ayın yokluğu kalıcı");
            dogrula(y, komut(y, "dogalafet start ay_carpmasi") == 0 && yonetici.aktifAfet().isEmpty(),
                    "ay yokken ay çarpması yeniden başlamaz (madde 4)");
            dogrula(y, komut(y, "dogalafet aysonrasi atla") == 1 && donem.deniz().hedefArtis() > 0,
                    "aysonrasi atla deniz yükselmesini hemen başlatmalı: " + donem.deniz());
            dogrula(y, komut(y, "dogalafet asama ileri") == 1
                    && donem.deniz().mevcutArtis() == donem.deniz().hedefArtis(), "asama ileri yükselmeyi tamamlar");
        } finally {
            yonetici.sifirla();
        }
        dogrula(y, donem.ayDurumu() == AyDurumu.VAR && donem.ayOlaySaati() == AyDurumu.OLAY_YOK,
                "sifirla ayı geri getirir");
        y.succeed();
    }

    @GameTest(template = "bos", batch = "ay_carpmasi_stop")
    public static void sirendeStopAyiGeriGetirirCarpmadanSonraStopAyiKorur(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        ServerLevel level = y.getLevel();
        try {
            Afet afet = baslat(y, yonetici);
            sirenden(y, yonetici, afet);
            yonetici.durdur();
            dogrula(y, donem.ayDurumu() == AyDurumu.VAR && donem.ayOlaySaati() == AyDurumu.OLAY_YOK,
                    "siren sırasında stop: ay yerine döner (ulaşılmamış dönem)");
            dogrula(y, donem.carpan(AfetTuru.TSUNAMI) == 1.0 && !donem.ekVeri("su").contains("bekleme"),
                    "çarpma olmadı: çarpan ve deniz süreci yok");
            dogrula(y, !OrtamSesi.aktifMi(AyCarpmasi.YAKLASMA_KATMANI) && !OrtamSesi.aktifMi(AyCarpmasi.CITIRTI_KATMANI),
                    "stop ortam katmanlarını söndürür");

            afet = baslat(y, yonetici);
            sirenden(y, yonetici, afet);
            yonetici.asamaIleri();
            yonetici.asamaIleri();
            dogrula(y, MeteorAPI.aktifYagmurSayisi(level) == 1, "yağmur başlamalı");
            dogrula(y, komut(y, "dogalafet stop") >= 1, "/dogalafet stop");
            dogrula(y, yonetici.aktifAfet().isEmpty() && MeteorAPI.aktifYagmurSayisi(level) == 0,
                    "stop yağmuru ve afeti bitirir");
            dogrula(y, donem.ayDurumu() == AyDurumu.YOK && donem.carpan(AfetTuru.TSUNAMI) > 1.0,
                    "çarpmadan sonra stop: ayın yokluğu ve tsunami çarpanı kalır");
        } finally {
            yonetici.sifirla();
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "ay_carpmasi_sarsinti")
    public static void sarsintiDesteksizBloklariDusururStopGeriKoyar(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        BlockPos m = y.absolutePos(BlockPos.ZERO);
        int ustY = m.getY() + 40;
        BlockPos tavanBas = new BlockPos(m.getX() - 8, ustY + 10, m.getZ() - 8);
        BlockPos tavanSon = new BlockPos(m.getX() + 8, ustY + 10, m.getZ() + 8);
        BlockPos yaprakBas = new BlockPos(m.getX() - 3, ustY, m.getZ() - 3);
        BlockPos yaprakSon = new BlockPos(m.getX() + 3, ustY, m.getZ() + 3);
        // Bariyer tavanı yüzeyi sahneye sabitler: tarama tavandan 12 blok iner, alttaki başka sahnelere ulaşmaz.
        BlockPos.betweenClosed(tavanBas, tavanSon).forEach(p -> level.setBlockAndUpdate(p, Blocks.BARRIER.defaultBlockState()));
        BlockPos.betweenClosed(yaprakBas, yaprakSon).forEach(p -> level.setBlockAndUpdate(p,
                Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true)));
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(y, m.getX() + 0.5, ustY - 6, m.getZ() + 0.5);
        GameRules.BooleanValue kural = level.getGameRules().getRule(Kurallar.AFET_BLOK_HASARI);
        boolean oncekiKural = kural.get();
        AABB alan = new AABB(yaprakBas.offset(-2, -20, -2), yaprakSon.offset(3, 2, 3));
        try {
            kural.set(false, level.getServer());
            SarsintiDusmeleri kapali = new SarsintiDusmeleri();
            kapali.tick(level, List.of(oyuncu), 240, 240, 20, 8, 1.0);
            dogrula(y, kapali.toplam() == 0, "afetBlokHasari kapalıyken blok düşmez");
            kural.set(true, level.getServer());
            SarsintiDusmeleri dusmeler = new SarsintiDusmeleri();
            for (int i = 0; i < 6 && dusmeler.toplam() < 20; i++) {
                dusmeler.tick(level, List.of(oyuncu), 240, 240, 20, 8, 1.0);
            }
            int dusen = level.getEntitiesOfClass(FallingBlockEntity.class, alan).size();
            dogrula(y, dusmeler.toplam() > 0 && dusmeler.toplam() <= 20 && dusen == dusmeler.toplam(),
                    "altı boş yapraklar düşen bloka dönmeli (tavan 20): " + dusmeler.toplam() + ", varlık " + dusen);
            dusmeler.geriAl(level);
            long yaprak = BlockPos.betweenClosedStream(yaprakBas, yaprakSon)
                    .filter(p -> level.getBlockState(p).is(Blocks.OAK_LEAVES)).count();
            dogrula(y, yaprak == 49 && level.getEntitiesOfClass(FallingBlockEntity.class, alan).isEmpty(),
                    "temiz durdurma havadaki blokları geri koyar, varlık kalmaz: yaprak " + yaprak);
        } finally {
            kural.set(oncekiKural, level.getServer());
            level.getServer().getPlayerList().remove(oyuncu);
            level.getEntitiesOfClass(FallingBlockEntity.class, alan).forEach(FallingBlockEntity::discard);
            BlockPos.betweenClosed(tavanBas, tavanSon).forEach(p -> level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));
            BlockPos.betweenClosed(yaprakBas, yaprakSon).forEach(p -> level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "ay_carpmasi_kayit")
    public static void ayOlaySaatiKayittaVePakette(GameTestHelper y) {
        CompoundTag eski = new CompoundTag();
        eski.putBoolean("ayVar", false);
        dogrula(y, DunyaDonemi.yukle(eski).ayDurumu() == AyDurumu.YOK
                && DunyaDonemi.yukle(eski).ayOlaySaati() == AyDurumu.OLAY_YOK, "eski kayıt (ayOlay yok) okunur");
        CompoundTag etiket = new CompoundTag();
        etiket.putBoolean("ayVar", true);
        etiket.putLong("ayOlay", 4321L);
        DunyaDonemi d = DunyaDonemi.yukle(etiket);
        dogrula(y, d.ayDurumu() == AyDurumu.YAKLASIYOR && d.ayOlaySaati() == 4321L, "yaklaşan ay okunur");
        DunyaDonemi geri = DunyaDonemi.yukle(d.save(new CompoundTag()));
        dogrula(y, geri.ayDurumu() == AyDurumu.YAKLASIYOR && geri.ayOlaySaati() == 4321L, "kayıt gidiş-dönüşü");
        DonemPaketi paket = DonemPaketi.den(geri, 2.0);
        FriendlyByteBuf tampon = new FriendlyByteBuf(Unpooled.buffer());
        paket.yaz(tampon);
        DonemPaketi okunan = DonemPaketi.oku(tampon);
        dogrula(y, okunan.equals(paket) && okunan.ayDurumu() == AyDurumu.YAKLASIYOR && tampon.readableBytes() == 0,
                "dönem paketi ay olay saatini taşır: " + okunan);
        y.succeed();
    }

    @GameTest(template = "bos", batch = "ay_carpmasi_giris")
    public static void sonradanGirenAysizGokVeCarpmaSaatiniAlir(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        MinecraftServer server = y.getLevel().getServer();
        ServerPlayer oyuncu = null;
        try {
            long carpma = donem.saat();
            donem.ayDurumuAyarla(AyDurumu.YOK, carpma);
            ICustomPacket<?> ornek = (ICustomPacket<?>) AgKanali.KANAL.toVanillaPacket(DonemPaketi.den(donem, 1.0),
                    NetworkDirection.PLAY_TO_CLIENT);
            byte kimlik = ByteBufUtil.getBytes(ornek.getInternalData())[0];
            oyuncu = CekirdekOyunTestleri.sahteOyuncu(y, 0.5, 2, 0.5);
            EmbeddedChannel kanal = (EmbeddedChannel) oyuncu.connection.connection.channel();
            DonemPaketi son = null;
            for (Object mesaj = kanal.readOutbound(); mesaj != null; mesaj = kanal.readOutbound()) {
                if (mesaj instanceof ICustomPacket<?> ozel && ozel.getName().equals(ornek.getName())) {
                    byte[] bayt = ByteBufUtil.getBytes(ozel.getInternalData());
                    if (bayt.length > 1 && bayt[0] == kimlik) {
                        son = DonemPaketi.oku(new FriendlyByteBuf(Unpooled.wrappedBuffer(bayt, 1, bayt.length - 1)));
                    }
                }
            }
            dogrula(y, son != null && son.ayDurumu() == AyDurumu.YOK && son.ayOlaySaati() == carpma,
                    "sonradan giren oyuncu ayın yokluğunu ve çarpma saatini almalı: " + son);
        } finally {
            if (oyuncu != null) {
                server.getPlayerList().remove(oyuncu);
            }
            yonetici.sifirla();
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "ay_carpmasi_basarim")
    public static void dogalBitenAyCarpmasiAyDustuVerir(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        MinecraftServer server = y.getLevel().getServer();
        BlockPos m = y.absolutePos(BlockPos.ZERO);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), "ay-tanik"),
                m.getX() + 0.5, m.getY() + 2, m.getZ() + 0.5);
        oyuncu.setGameMode(GameType.SURVIVAL);
        Advancement ayDustu = server.getAdvancements().getAdvancement(BasarimKimlikleri.basarim(Basarimlar.AY_DUSTU));
        try {
            dogrula(y, ayDustu != null, "Ay Düştü başarımı yüklenmedi (runData?)");
            Afet afet = baslat(y, yonetici);
            for (int i = 0; i < 5 && yonetici.aktifAfet().isPresent(); i++) {
                yonetici.asamaIleri();
            }
            dogrula(y, yonetici.aktifAfet().isEmpty() && afet.sonlandi(), "ay çarpması doğal bitmeli");
            dogrula(y, oyuncu.getAdvancements().getOrStartProgress(ayDustu).isDone(),
                    "gerçek ay çarpmasından sağ çıkan Ay Düştü alır (başarım §A.3 #12)");
        } finally {
            server.getPlayerList().remove(oyuncu);
            yonetici.sifirla();
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "ay_carpmasi_hiz", timeoutTicks = 400)
    public static void hizlaSirenSarsintiYagmurKendiligindenBiter(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        double oncekiSiklik = AyCarpmasiAyarlari.meteorDakikaBasinaAyarla(0);
        Afet afet = baslat(y, yonetici);
        sirenden(y, yonetici, afet);
        yonetici.hizAyarla(60);
        y.succeedWhen(() -> {
            y.assertTrue(yonetici.aktifAfet().isEmpty() && afet.sonlandi(),
                    "hızlı akışta siren → çarpma → sarsıntı → yağmur kendiliğinden bitmeli: " + afet.asama());
            y.assertTrue(donem.ayDurumu() == AyDurumu.YOK && donem.carpan(AfetTuru.TSUNAMI) > 1.0, "çarpma oldu");
            AyCarpmasiAyarlari.meteorDakikaBasinaAyarla(oncekiSiklik);
            yonetici.sifirla();
        });
    }
}
