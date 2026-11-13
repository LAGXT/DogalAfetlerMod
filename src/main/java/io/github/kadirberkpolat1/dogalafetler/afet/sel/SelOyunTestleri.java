package io.github.kadirberkpolat1.dogalafetler.afet.sel;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import io.github.kadirberkpolat1.dogalafetler.su.SuAraliklari;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Selin oyun içi testleri (K-E80). Sahne sanal su testlerinin {@code su_alani} şablonudur: kapalı havza, batıda deniz (x 1..6,
 * y 1..2; yüzey y3), doğuda kara (x 7..14, y 1..3; zemin y4) ve yüksek bir sütün (x 12..14, z 12..14, y 4..8). Her test kendi
 * batch'indedir (hiz ve sabit bölge globaldir).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SelOyunTestleri {
    private static final String SAHNE = "su_alani";
    private static final int KAMP_X = 9;
    private static final int KAMP_Z = 8;

    private SelOyunTestleri() {
    }

    @AfterBatch(batch = "sel_ana")
    public static void anaSonrasi(ServerLevel level) {
        temizle(level);
    }

    @AfterBatch(batch = "sel_tetik")
    public static void tetikSonrasi(ServerLevel level) {
        level.setWeatherParameters(6000, 0, false, false);
    }

    private static void temizle(ServerLevel level) {
        MinecraftServer server = level.getServer();
        AfetYoneticisi.al(server).durdur();
        AfetYoneticisi.al(server).hizAyarla(1.0);
        SanalSu.bolgeBirak(level);
    }

    private static BlockPos m(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new BlockPos(x, y, z));
    }

    private static void kutu(GameTestHelper h, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            h.setBlock(p, s);
        }
    }

    /** {@code (x, z)} sütununun sanal su aralıkları; sel yokken dünya olmayabilir (boş aralık). */
    private static SuAraliklari araliklar(GameTestHelper h, int x, int z) {
        BlockPos p = m(h, x, 0, z);
        SuAraliklari a = new SuAraliklari();
        SanalSuDunyasi d = SanalSu.dunya(h.getLevel());
        if (d != null) {
            d.araliklar(p.getX(), p.getZ(), SanalSu.saat(h.getLevel()), a);
        }
        return a;
    }

    private static long havzaHash(GameTestHelper h) {
        BlockPos a = m(h, 0, 0, 0);
        BlockPos b = m(h, 15, 11, 15);
        return SanalSu.bolgeHash(h.getLevel(), Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), b.getY(), Math.max(a.getZ(), b.getZ()));
    }

    private static void komut(GameTestHelper h, String komut) {
        MinecraftServer server = h.getLevel().getServer();
        try {
            server.getCommands().getDispatcher().execute(komut,
                    server.createCommandSourceStack().withSuppressedOutput().withPermission(2));
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException hata) {
            h.fail("/" + komut + " çalışmadı: " + hata.getMessage());
        }
    }

    private static void havza(GameTestHelper h) {
        BlockState tas = Blocks.STONE.defaultBlockState();
        kutu(h, 0, 0, 0, 15, 0, 15, tas);
        kutu(h, 0, 1, 0, 15, 9, 0, tas);
        kutu(h, 0, 1, 15, 15, 9, 15, tas);
        kutu(h, 0, 1, 1, 0, 9, 14, tas);
        kutu(h, 15, 1, 1, 15, 9, 14, tas);
        kutu(h, 1, 1, 1, 6, 2, 14, Blocks.WATER.defaultBlockState());
        kutu(h, 7, 1, 1, 14, 3, 14, tas);
        kutu(h, 12, 4, 12, 14, 8, 14, tas);
        BlockPos a = m(h, 0, 0, 0);
        BlockPos b = m(h, 15, 0, 15);
        SanalSu.bolgeSabitle(h.getLevel(), new ChunkPos(a), new ChunkPos(b));
    }

    /**
     * Seviye 1 sel (K-E81: 5 blok): AKTIF'te deniz yüzeyi 5 blok yükselir, altındaki kara sütunu [y4, y8) sel suyuna gömülür, yüksek sütün kuru
     * kalır. Karadaki kamp ateşi çevresindeki sel suyunu buharlaştırır (dibinde kuru, uzakta sel sürer, denizin kendi
     * yüzeyi yükseldiği gibi kalır); gerçek blok hiç değişmez. {@code stop}: sel ve ısı bölgeleri anında kalkar, havza aynı.
     */
    @GameTest(template = SAHNE, batch = "sel_ana", timeoutTicks = 900)
    public static void selKiyiyiKaplarIsiKaynagiSondururStopTemizler(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        havza(h);
        h.setBlock(KAMP_X, 4, KAMP_Z, Blocks.CAMPFIRE);
        long hashOnce = havzaHash(h);
        int y4 = m(h, 0, 4, 0).getY();
        double artis = SelAyarlari.seviye(1).artisBlok();
        h.assertTrue(artis == Math.rint(artis) && artis <= 5, "S1 artışı tam sayı ve havza duvarının altında olmalı: " + artis);
        int yUst = m(h, 0, 3 + (int) artis, 0).getY();
        BlockPos oyuncuKonumu = m(h, 13, 9, 13);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(h, oyuncuKonumu.getX() + 0.5, oyuncuKonumu.getY(),
                oyuncuKonumu.getZ() + 0.5);
        AfetYoneticisi yonetici = AfetYoneticisi.al(l.getServer());
        yonetici.durdur();
        AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(AfetTuru.SEL, 1, oyuncu, l, oyuncuKonumu);
        h.assertTrue(sonuc == AfetYoneticisi.BaslatmaSonucu.BASLADI, "sel başlamadı: " + sonuc);
        h.assertTrue(yonetici.asamaIleri(AfetTuru.SEL) && yonetici.asamaIleri(AfetTuru.SEL), "AKTIF'e ilerlemedi");
        Sel sel = (Sel) yonetici.aktifAfet(AfetTuru.SEL).orElseThrow();
        h.assertTrue(sel.selKimlik() > 0, "sel suyu kaydedilmedi");
        komut(h, "dogalafet hiz 10");
        h.startSequence()
                .thenWaitUntil(() -> {
                    SanalSuDunyasi d = SanalSu.dunya(l);
                    h.assertTrue(d != null && d.selDizisi().length == 1, "sel suyu sürmüyor");
                    SuAraliklari kara = araliklar(h, 14, KAMP_Z);
                    h.assertTrue(kara.adet() == 1 && kara.alt(0) == y4 && kara.ust(0) == yUst, "uzak kara sütunu [y4, yUst) değil: "
                            + kara.adet() + " aralık, sel artışı " + d.selArtisi(SanalSu.saat(l)));
                    SuAraliklari kamp = araliklar(h, KAMP_X, KAMP_Z);
                    h.assertTrue(kamp.adet() == 0, "kamp ateşinin dibinde sel suyu buharlaşmadı (ısı bölgesi "
                            + sel.isiBolgesiSayisi() + ")");
                    SuAraliklari deniz = araliklar(h, 3, 8);
                    h.assertTrue(deniz.adet() == 1 && deniz.ust(0) == yUst, "deniz yüzeyi " + (int) artis + " blok yükselmedi");
                    h.assertTrue(araliklar(h, 13, 13).adet() == 0, "yüksek sütün ıslanmış");
                    h.assertTrue(havzaHash(h) == hashOnce, "sel gerçek blokları değiştirdi");
                })
                .thenExecute(() -> {
                    h.assertTrue(yonetici.durdur(AfetTuru.SEL), "stop afeti bulamadı");
                    h.assertTrue(yonetici.aktifAfet(AfetTuru.SEL).isEmpty(), "stop sonrası sel sürüyor");
                    SanalSuDunyasi d = SanalSu.dunya(l);
                    h.assertTrue(d == null || d.seller().isEmpty() && d.isiDizisi().length == 0,
                            "stop sonrası sel suyu ya da ısı bölgesi kaldı");
                    h.assertTrue(araliklar(h, 14, KAMP_Z).adet() == 0 && araliklar(h, 3, 8).adet() == 0, "su kaldı");
                    h.assertTrue(havzaHash(h) == hashOnce, "stop iz bıraktı");
                    l.getServer().getPlayerList().remove(oyuncu);
                    temizle(l);
                })
                .thenSucceed();
    }

    /**
     * K-E81: sel suyunda gözü suyun altında olan oyuncu vanilla'nın (tick başına 1) 2–3 katı hızla hava kaybeder (varsayılan 2,5 kat:
     * 40 tick'te 100 civarı). Oyuncu kuru karada başlar; gözü sel suyuna girince hava tam doldurulup ölçülür.
     */
    @GameTest(template = SAHNE, batch = "sel_bogulma", timeoutTicks = 600)
    public static void selSuyundaOyuncuIkiUcKatHizliBogulur(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        havza(h);
        BlockPos konum = m(h, 10, 4, 4);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(h, konum.getX() + 0.5, konum.getY(), konum.getZ() + 0.5);
        oyuncu.setGameMode(GameType.SURVIVAL);
        AfetYoneticisi yonetici = AfetYoneticisi.al(l.getServer());
        yonetici.durdur();
        AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(AfetTuru.SEL, 1, oyuncu, l, konum);
        h.assertTrue(sonuc == AfetYoneticisi.BaslatmaSonucu.BASLADI, "sel başlamadı: " + sonuc);
        h.assertTrue(yonetici.asamaIleri(AfetTuru.SEL) && yonetici.asamaIleri(AfetTuru.SEL), "AKTIF'e ilerlemedi");
        komut(h, "dogalafet hiz 10");
        h.onEachTick(oyuncu::doTick);
        int olcumTick = 40;
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(SanalSu.gozSelSuyundaMi(oyuncu), "oyuncunun gözü sel suyunda değil"))
                .thenExecute(() -> oyuncu.setAirSupply(oyuncu.getMaxAirSupply()))
                .thenExecuteAfter(olcumTick, () -> {
                    int kayip = oyuncu.getMaxAirSupply() - oyuncu.getAirSupply();
                    DogalAfetler.LOGGER.info("Sel GameTest: {} tick'te hava kaybı {} (vanilla {})", olcumTick, kayip, olcumTick);
                    h.assertTrue(SanalSu.gozSelSuyundaMi(oyuncu), "ölçüm süresince sel suyunda kalmalıydı");
                    h.assertTrue(kayip >= 2 * olcumTick && kayip <= 3 * olcumTick + 3,
                            "hava kaybı " + kayip + ", beklenen " + 2 * olcumTick + "…" + 3 * olcumTick);
                    yonetici.durdur(AfetTuru.SEL);
                    l.getServer().getPlayerList().remove(oyuncu);
                    temizle(l);
                })
                .thenSucceed();
    }

    /**
     * Doğal seçim çarpanı: yağmur seli artır, fırtına yağmurdan fazla artır (üstüne eklenmez), açık havada 1; tsunami
     * ayrıca çarpar.
     */
    @GameTest(template = SAHNE, batch = "sel_tetik", timeoutTicks = 100)
    public static void yagmurVeFirtinaSelOlasiliginiArtirir(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        MinecraftServer server = l.getServer();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        var a = SelAyarlari.anlikTetikleyiciler();
        ServerLevel ust = server.overworld();
        ust.setWeatherParameters(6000, 0, false, false);
        double acik = SelSistemi.secimCarpani(yonetici, server);
        ust.setWeatherParameters(0, 6000, true, false);
        double yagmur = SelSistemi.secimCarpani(yonetici, server);
        ust.setWeatherParameters(0, 6000, true, true);
        double firtina = SelSistemi.secimCarpani(yonetici, server);
        ust.setWeatherParameters(6000, 0, false, false);
        h.assertTrue(acik == 1.0, "açık havada çarpan " + acik);
        h.assertTrue(yagmur == a.yagmur(), "yağmurda çarpan " + yagmur + " beklenen " + a.yagmur());
        h.assertTrue(firtina == a.firtina() && firtina >= yagmur, "fırtınada çarpan " + firtina);
        h.assertTrue(DunyaDonemi.al(server).carpan(AfetTuru.SEL) >= 1.0, "geçici çarpan listesi tutarsız");
        h.succeed();
    }
}
