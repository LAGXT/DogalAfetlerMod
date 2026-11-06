package io.github.kadirberkpolat1.dogalafetler.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Sanal suyun oyun içi testleri (§6 #15): deniz yükselmesi ve mağara suyu öncesi/sonrası bölge hash'i aynı; sahte oyuncu
 * sanal suda hava kaybeder ve boğulur; dalga mob'u ve eşyayı sürükler, oyuncuya istemcinin uygulayacağı ivmeyi verir.
 * Sahneler düz dünyada {@code su_alani} (16x12x16 hava) şablonunda kurulur (K0-31). Her test kendi batch'indedir (deniz
 * artışı ve hız globaldir).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SuOyunTestleri {
    private SuOyunTestleri() {
    }

    private static BlockPos m(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new BlockPos(x, y, z));
    }

    private static void kutu(GameTestHelper h, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            h.setBlock(p, s);
        }
    }

    private static SuAraliklari araliklar(GameTestHelper h, int x, int z) {
        BlockPos p = m(h, x, 0, z);
        SuAraliklari a = new SuAraliklari();
        SanalSu.boyut(h.getLevel()).dunya.araliklar(p.getX(), p.getZ(), SanalSu.sunucuSaati, a);
        return a;
    }

    private static long sablonHash(GameTestHelper h) {
        BlockPos a = m(h, 0, 0, 0);
        BlockPos b = m(h, 15, 11, 15);
        return SanalSu.bolgeHash(h.getLevel(), Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), b.getY(), Math.max(a.getZ(), b.getZ()));
    }

    private static int suSayisi(GameTestHelper h) {
        return SanalSu.bolgeSayim(h.getLevel(), m(h, 0, 0, 0), m(h, 15, 11, 15), s -> s.is(Blocks.WATER));
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

    private static void sabitle(GameTestHelper h) {
        BlockPos a = m(h, 0, 0, 0);
        BlockPos b = m(h, 15, 0, 15);
        SanalSu.bolgeSabitle(h.getLevel(), new ChunkPos(a), new ChunkPos(b));
    }

    /**
     * Kapalı havza: taş taban, dört kenarda y9'a kadar taş duvar; içeride batıda su (x 1..6, y 1..2), doğuda kara (x 7..14,
     * y 1..3) ve yüksek bir sütün (x 12..14, z 12..14, y 1..8). {@code denizseviyesi 4}: kara sütunu [y4, y7) sanal suda,
     * yüksek sütün kuru, gerçek su bloğu sayısı ve şablon hash'i aynı; {@code denizseviyesi 0}: su kalmaz, hash aynı.
     */
    @GameTest(template = "su_alani", batch = "su_deniz", timeoutTicks = 400)
    public static void denizYukselmesiBolgeyiDegistirmezSifirIzBirakmaz(GameTestHelper h) {
        BlockState tas = Blocks.STONE.defaultBlockState();
        kutu(h, 0, 0, 0, 15, 0, 15, tas);
        kutu(h, 0, 1, 0, 15, 9, 0, tas);
        kutu(h, 0, 1, 15, 15, 9, 15, tas);
        kutu(h, 0, 1, 1, 0, 9, 14, tas);
        kutu(h, 15, 1, 1, 15, 9, 14, tas);
        kutu(h, 1, 1, 1, 6, 2, 14, Blocks.WATER.defaultBlockState());
        kutu(h, 7, 1, 1, 14, 3, 14, tas);
        kutu(h, 12, 4, 12, 14, 8, 14, tas);
        sabitle(h);
        long hashOnce = sablonHash(h);
        int suOnce = suSayisi(h);
        int y4 = m(h, 0, 4, 0).getY();
        int y7 = m(h, 0, 7, 0).getY();
        komut(h, "dogalafet denizseviyesi 4");
        h.startSequence()
                .thenWaitUntil(() -> {
                    SuAraliklari k = araliklar(h, 10, 8);
                    h.assertTrue(k.adet() == 1 && k.alt(0) == y4 && k.ust(0) == y7, "kara sütunu henüz [y4, y7) değil");
                })
                .thenExecute(() -> {
                    h.assertTrue(araliklar(h, 13, 13).adet() == 0, "yüksek sütün ıslak");
                    h.assertTrue(araliklar(h, 3, 8).ust(0) == y7, "su sütunu yükselmedi");
                    h.assertTrue(suSayisi(h) == suOnce, "gerçek su bloğu sayısı değişti");
                    h.assertTrue(sablonHash(h) == hashOnce, "yükselmede şablon hash'i değişti");
                    DogalAfetler.LOGGER.info("Sanal su GameTest: deniz +4 hash={} su={} kütle={}", Long.toHexString(hashOnce),
                            suOnce, SanalSu.boyut(h.getLevel()).dunya.alan().sonGovdeSayisi());
                    komut(h, "dogalafet denizseviyesi 0");
                })
                .thenWaitUntil(() -> h.assertTrue(araliklar(h, 10, 8).adet() == 0 && araliklar(h, 3, 8).adet() == 0,
                        "deniz 0 sonrası su kaldı"))
                .thenExecute(() -> {
                    h.assertTrue(DunyaDonemi.al(h.getLevel().getServer()).deniz().mevcutArtis() == 0f, "artış 0 değil");
                    h.assertTrue(sablonHash(h) == hashOnce && suSayisi(h) == suOnce, "deniz 0 iz bıraktı");
                    SanalSu.bolgeBirak(h.getLevel());
                })
                .thenSucceed();
    }

    /** Taş tepe içinde 8x4x8 oda; {@code acik} ise y 4..5 hizasından dışarı açılan tünel (mağara ağzı). */
    private static void oda(GameTestHelper h, boolean acik) {
        kutu(h, 0, 0, 0, 13, 9, 13, Blocks.STONE.defaultBlockState());
        kutu(h, 3, 2, 3, 10, 5, 10, Blocks.AIR.defaultBlockState());
        if (acik) {
            kutu(h, 11, 4, 6, 13, 5, 7, Blocks.AIR.defaultBlockState());
        }
    }

    /**
     * Yan ağızlı mağara: su ağzın eşiğine (y4) kadar dolar (oda y 2..3, 128 hücre), tünel kuru; kalıştan sonra seviye
     * kademeli düşer (hız 50), sonunda su kalmaz ve şablon ile cep kutusu hash'i baştakiyle aynıdır.
     */
    @GameTest(template = "su_alani", batch = "su_magara", timeoutTicks = 600)
    public static void magaraDolarKururDunyaDegismez(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        oda(h, true);
        h.setBlock(5, 2, 5, Blocks.TORCH);
        h.setBlock(8, 2, 8, Blocks.POINTED_DRIPSTONE);
        long hashOnce = sablonHash(h);
        int esik = m(h, 0, 4, 0).getY();
        int id = SanalSu.magaraDoldur(l, m(h, 4, 2, 4), 4000, 40);
        h.assertTrue(id > 0, "mağara dolmadı");
        SanalSuDunyasi.Magara mg = SanalSu.boyut(l).dunya.magaralar().get(id);
        h.assertTrue(mg.cep().hacim() == 128 && mg.cep().ustY() == esik, "cep " + mg.cep().hacim() + " yüzey " + mg.cep().ustY());
        h.assertTrue(mg.cep().icerir(m(h, 5, 2, 5).getX(), m(h, 5, 2, 5).getY(), m(h, 5, 2, 5).getZ()), "meşale suda değil");
        List<Double> seviyeler = new ArrayList<>();
        h.startSequence()
                .thenExecuteAfter(25, () -> {
                    SuAraliklari oda = araliklar(h, 4, 4);
                    h.assertTrue(oda.adet() == 1 && oda.ust(0) == esik, "oda sütunu eşiğe kadar dolu değil");
                    for (int x = 11; x <= 15; x++) {
                        h.assertTrue(araliklar(h, x, 6).adet() == 0, "ağız/dış sütunu ıslak x=" + x);
                    }
                    h.assertTrue(sablonHash(h) == hashOnce, "dolu mağarada hash değişti");
                    komut(h, "dogalafet hiz 50");
                })
                .thenWaitUntil(() -> {
                    SanalSuDunyasi.Magara g = SanalSu.boyut(l).dunya.magaralar().get(id);
                    if (g != null && g.asama(SanalSu.sunucuSaati) == MagaraZamani.Asama.BUHARLASIYOR) {
                        double s = g.seviye(SanalSu.sunucuSaati);
                        if (seviyeler.isEmpty() || s < seviyeler.get(seviyeler.size() - 1)) {
                            seviyeler.add(s);
                        }
                    }
                    h.assertTrue(SanalSu.boyut(l).dunya.magaralar().isEmpty(), "mağara kurumadı");
                })
                .thenExecute(() -> {
                    komut(h, "dogalafet hiz 1");
                    DogalAfetler.LOGGER.info("Sanal su GameTest: mağara kuruma seviyeleri {}", seviyeler);
                    h.assertTrue(seviyeler.size() >= 3, "seviye kademeli düşmedi: " + seviyeler);
                    h.assertTrue(araliklar(h, 4, 4).adet() == 0, "mağara bitti ama su kaldı");
                    h.assertTrue(sablonHash(h) == hashOnce, "kurumadan sonra şablon hash'i değişti");
                })
                .thenSucceed();
    }

    /**
     * Kapalı oda tavana kadar dolar; içindeki sahte oyuncu (yaşam tick'i elle, bağlantısı tick'lenmediği için) suyun
     * altında sayılır, ıslaktır ({@link SanalSu#icindeMi}), havası azalır ve hava bitince boğulma hasarı alır.
     */
    @GameTest(template = "su_alani", batch = "su_oyuncu", timeoutTicks = 400)
    public static void sahteOyuncuSanalSudaHavaKaybederBogulur(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        oda(h, false);
        BlockPos ayak = m(h, 6, 2, 6);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(h, ayak.getX() + 0.5, ayak.getY(), ayak.getZ() + 0.5);
        oyuncu.setGameMode(GameType.SURVIVAL);
        int id = SanalSu.magaraDoldur(l, m(h, 4, 2, 4), 4000, 6000);
        h.assertTrue(id > 0, "oda dolmadı");
        float can = oyuncu.getHealth();
        h.onEachTick(oyuncu::doTick);
        h.startSequence()
                .thenExecuteAfter(60, () -> {
                    h.assertTrue(SanalSu.icindeMi(oyuncu), "oyuncu ıslak değil");
                    h.assertTrue(oyuncu.isInWater() && oyuncu.isUnderWater(), "oyuncu suyun altında sayılmadı");
                    h.assertTrue(oyuncu.getAirSupply() < oyuncu.getMaxAirSupply(), "hava azalmadı: " + oyuncu.getAirSupply());
                    oyuncu.setAirSupply(0);
                })
                .thenWaitUntil(() -> h.assertTrue(oyuncu.getHealth() < can, "boğulma hasarı yok"))
                .thenExecute(() -> {
                    DogalAfetler.LOGGER.info("Sanal su GameTest: oyuncu hava={} can={} -> {}", oyuncu.getAirSupply(), can,
                            oyuncu.getHealth());
                    SanalSu.magaraBitir(l, id);
                    l.getServer().getPlayerList().remove(oyuncu);
                })
                .thenSucceed();
    }

    /**
     * Düz taş zeminde +x yönünde ilerleyen dalga: domuz ve eşya dalga yönünde 3 bloktan fazla sürüklenir (sunucu); sahte
     * oyuncu ıslaktır ve istemcisinin uygulayacağı ivme dalga yönündedir (oyuncuyu kendi istemcisi iter, SU-03).
     */
    @GameTest(template = "su_alani", batch = "su_dalga", timeoutTicks = 400)
    public static void dalgaMobuVeEsyayiSurukler(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        kutu(h, 0, 0, 0, 15, 0, 15, Blocks.STONE.defaultBlockState());
        Pig domuz = h.spawn(EntityType.PIG, 3, 1, 6);
        BlockPos ep = m(h, 3, 1, 9);
        ItemEntity esya = new ItemEntity(l, ep.getX() + 0.5, ep.getY() + 0.1, ep.getZ() + 0.5, new ItemStack(Items.OAK_LOG));
        l.addFreshEntity(esya);
        BlockPos op = m(h, 3, 1, 12);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(h, op.getX() + 0.5, op.getY(), op.getZ() + 0.5);
        BlockPos koken = m(h, -20, 1, 8);
        double domuzX = domuz.getX();
        double esyaX = esya.getX();
        int id = SanalSu.dalgaBaslat(l, new Dalga(koken.getX(), koken.getZ() + 0.5, 1, 0, SanalSu.sunucuSaati, 0.5, 6, 60, 60,
                koken.getY(), 200));
        h.startSequence()
                .thenExecuteAfter(110, () -> {
                    double[] it = new double[2];
                    h.assertTrue(SanalSu.icindeMi(oyuncu), "oyuncu dalgada ıslak değil");
                    h.assertTrue(SanalSu.itmeHesapla(oyuncu, it) != null && it[0] > 0 && Math.abs(it[1]) < 1e-9,
                            "oyuncunun sürükleme ivmesi dalga yönünde değil");
                    DogalAfetler.LOGGER.info("Sanal su GameTest: dalga domuz dx={} eşya dx={} oyuncu ivme={} domuz suda={}",
                            domuz.getX() - domuzX, esya.getX() - esyaX, it[0], domuz.isInWater());
                    h.assertTrue(domuz.isInWater(), "domuz suyu tanımadı");
                    h.assertTrue(domuz.getX() - domuzX > 3, "domuz sürüklenmedi: dx=" + (domuz.getX() - domuzX));
                    h.assertTrue(esya.getX() - esyaX > 3, "eşya sürüklenmedi: dx=" + (esya.getX() - esyaX));
                    SanalSu.dalgaBitir(l, id);
                    l.getServer().getPlayerList().remove(oyuncu);
                })
                .thenSucceed();
    }

    /**
     * Deniz yükselmesi süreci (bölüm H, N10, N18, §3a): ay yok olunca bekleme kurulur; {@code aysonrasi atla} yükselmeyi
     * başlatır (hedef 4, kalıcı); artış monoton yükselir; {@code stop} süreci dondurur (KOMUT); {@code asama ileri} durmuş
     * süreci hedefe ulaştırır; buharlaşma artışı derinlik kadar kademeli 0'a indirir ve geri getirmez.
     */
    @GameTest(template = "bos", batch = "su_surec", timeoutTicks = 400)
    public static void denizSureciAyYokOluncaBaslarDururBuharlasmaSifirlar(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        AfetYoneticisi y = AfetYoneticisi.al(server);
        DunyaDonemi d = DunyaDonemi.al(server);
        komut(h, "dogalafet sifirla");
        d.ayVarAyarla(false);
        long bekleme = DenizAdimi.dakikaTick(SuAyarlari.sunucu().aysonrasiBeklemeDk.get());
        h.assertTrue(d.ekVeri("su").getLong("bekleme") == bekleme, "ay yok olunca bekleme kurulmadı");
        h.assertTrue(y.aktifDonemSurecleri().contains(DenizYukselmesiSureci.ORNEK), "beklemede süreç aktif değil");
        h.assertTrue(DenizYukselmesiSureci.ORNEK.atla(server) == DenizYukselmesiSureci.AtlaSonucu.BASLADI, "atla başlatmadı");
        h.assertTrue(d.deniz().hedefArtis() == 4f && d.deniz().mevcutArtis() == 0f && d.deniz().kalici(),
                "hedef 4 / mevcut 0 / kalıcı değil: " + d.deniz());
        h.assertTrue(DenizYukselmesiSureci.ORNEK.atla(server) == DenizYukselmesiSureci.AtlaSonucu.ZATEN, "ikinci atla");
        komut(h, "dogalafet hiz 100");
        List<Float> artislar = new ArrayList<>();
        float[] donuk = new float[1];
        h.startSequence()
                .thenExecuteAfter(10, () -> {
                    float m = d.deniz().mevcutArtis();
                    h.assertTrue(m > 0f && m < 4f, "10 tick sonra artış " + m);
                    komut(h, "dogalafet stop");
                    h.assertTrue(y.durdurmaNedeni(DenizYukselmesiSureci.ORNEK).orElse(null) == DonemSureci.DurdurmaNedeni.KOMUT,
                            "stop süreci durdurmadı");
                    donuk[0] = d.deniz().mevcutArtis();
                })
                .thenExecuteAfter(10, () -> {
                    h.assertTrue(d.deniz().mevcutArtis() == donuk[0], "durmuş süreç ilerledi");
                    komut(h, "dogalafet asama ileri");
                    h.assertTrue(d.deniz().mevcutArtis() == 4f, "asama ileri hedefe ulaştırmadı");
                    d.buharlasmaDerinligiAyarla(1.5f);
                })
                .thenExecuteAfter(2, () -> {
                    artislar.add(d.deniz().mevcutArtis());
                    h.assertTrue(Math.abs(d.deniz().mevcutArtis() - 2.5f) < 1e-4, "buharlaşma 1,5: artış " + d.deniz().mevcutArtis());
                    d.buharlasmaDerinligiAyarla(5f);
                })
                .thenExecuteAfter(2, () -> {
                    artislar.add(d.deniz().mevcutArtis());
                    h.assertTrue(d.deniz().mevcutArtis() == 0f, "buharlaşma 5: artış 0 değil");
                    d.buharlasmaDerinligiAyarla(0f);
                })
                .thenExecuteAfter(2, () -> {
                    h.assertTrue(d.deniz().mevcutArtis() == 0f && d.deniz().hedefArtis() == 0f, "yükselme geri geldi");
                    h.assertTrue(DenizYukselmesiSureci.ORNEK.atla(server) == DenizYukselmesiSureci.AtlaSonucu.BITTI,
                            "buharlaşmadan sonra atla 'bitti' demeli");
                    h.assertTrue(DenizYukselmesiSureci.buharlasmaPayi(server) == 4f, "buharlaşma payı");
                    DogalAfetler.LOGGER.info("Sanal su GameTest: deniz süreci donuk={} buharlaşma artışları={}", donuk[0], artislar);
                    komut(h, "dogalafet sifirla");
                })
                .thenSucceed();
    }
}
