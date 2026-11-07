package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.config.CekirdekAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetBaglami;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.OyunTestiTemizligi;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.AfetZamanlayicisi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.BeklemeDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonumTipi;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Tsunaminin oyun içi testleri. Sahne {@code su_alani} (16x12x16) içinde kurulur: taş taban (y0), batıda duvarlı deniz
 * (x 1..5, y 1..2; dalga tabanı y2), doğuda kara. Dalga +x yönünde, küçük ölçekle ({@link TsunamiAfeti.Olcek}: açıklık 6,
 * karaya giriş 8, genişlik 12, uzunluk 10, gecikmesiz) şablonun dışına yıkım taşmaz. Afet çekirdeğin final metotlarıyla
 * sürülür ({@code baslat}, {@code asamaIleri}, {@code tick}, {@code durdur}); yönetici yuvasını yalnız N14 testi kullanır.
 * Her test kendi batch'indedir (sanal su ve dünya saati ortaktır; K-E16 komuşu sahneden bağımsızlık).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TsunamiOyunTestleri {
    private static final TsunamiAfeti.Olcek OLCEK = new TsunamiAfeti.Olcek(6, 8, 12, 10, 0);

    private TsunamiOyunTestleri() {
    }

    @AfterBatch(batch = "tsunami_yer")
    public static void yerSonrasi(ServerLevel level) {
        temizlik(level);
    }

    @AfterBatch(batch = "tsunami_stop")
    public static void stopSonrasi(ServerLevel level) {
        temizlik(level);
    }

    @AfterBatch(batch = "tsunami_yikim")
    public static void yikimSonrasi(ServerLevel level) {
        temizlik(level);
    }

    @AfterBatch(batch = "tsunami_jeton")
    public static void jetonSonrasi(ServerLevel level) {
        temizlik(level);
    }

    @AfterBatch(batch = "tsunami_ipucu")
    public static void ipucuSonrasi(ServerLevel level) {
        temizlik(level);
    }

    @AfterBatch(batch = "tsunami_dogal")
    public static void dogalSonrasi(ServerLevel level) {
        temizlik(level);
    }

    @AfterBatch(batch = "tsunami_kayit")
    public static void kayitSonrasi(ServerLevel level) {
        temizlik(level);
    }

    @AfterBatch(batch = "tsunami_ay")
    public static void aySonrasi(ServerLevel level) {
        temizlik(level);
    }

    @AfterBatch(batch = "tsunami_jeton_sutun")
    public static void jetonSutunSonrasi(ServerLevel level) {
        temizlik(level);
    }

    @AfterBatch(batch = "tsunami_tps")
    public static void tpsSonrasi(ServerLevel level) {
        temizlik(level);
    }

    @AfterBatch(batch = "tsunami_dogal_yikim")
    public static void dogalYikimSonrasi(ServerLevel level) {
        temizlik(level);
    }

    @AfterBatch(batch = "tsunami_yamac")
    public static void yamacSonrasi(ServerLevel level) {
        temizlik(level);
    }

    @AfterBatch(batch = "tsunami_blok_hasari")
    public static void blokHasariSonrasi(ServerLevel level) {
        temizlik(level);
    }

    /**
     * Başarısız test de batch sonunda temizlenir (K-E15): testler afeti yöneticisiz sürdüğü için kalan dalga, çekilme ve mağara
     * boyuttan silinir, sabitlenen TPS kısması ve sahte oyuncu/dinleyici geri alınır.
     */
    private static void temizlik(ServerLevel level) {
        TpsOlcer.testKismasi(null);
        SanalSuDunyasi d = SanalSu.dunya(level);
        if (d != null) {
            new ArrayList<>(d.dalgalar().keySet()).forEach(id -> SanalSu.dalgaBitir(level, id));
            new ArrayList<>(d.cekilmeler().keySet()).forEach(id -> SanalSu.cekilmeBitir(level, id));
            new ArrayList<>(d.magaralar().keySet()).forEach(id -> SanalSu.magaraBitir(level, id));
        }
        SanalSu.bolgeBirak(level);
        OyunTestiTemizligi.batchSonu(level);
    }

    private static BlockPos m(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new BlockPos(x, y, z));
    }

    private static void kutu(GameTestHelper h, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            h.setBlock(p, s);
        }
    }

    /** Taş taban, duvarlı deniz (x 1..5, y 1..{@code derin}) ve x 6..14'te toprak kara (y 1..2). */
    private static void sahne(GameTestHelper h, int derin) {
        BlockState tas = Blocks.STONE.defaultBlockState();
        kutu(h, 0, 0, 0, 15, 0, 15, tas);
        kutu(h, 0, 1, 0, 0, derin, 15, tas);
        kutu(h, 1, 1, 0, 5, derin, 0, tas);
        kutu(h, 1, 1, 15, 5, derin, 15, tas);
        kutu(h, 6, 1, 0, 14, Math.max(2, derin), 15, Blocks.DIRT.defaultBlockState());
        kutu(h, 1, 1, 1, 5, derin, 14, Blocks.WATER.defaultBlockState());
    }

    /** Kaşık: x 7..14 taş yar (y 1..6); ağız x 7..8, y 3..4, z 8; iç boşluk x 9..12, y 1..3, z 6..10 (su eşiği y3). */
    private static void magaraSahnesi(GameTestHelper h) {
        sahne(h, 2);
        kutu(h, 7, 1, 0, 14, 6, 15, Blocks.STONE.defaultBlockState());
        kutu(h, 9, 1, 6, 12, 3, 10, Blocks.AIR.defaultBlockState());
        kutu(h, 7, 3, 8, 8, 4, 8, Blocks.AIR.defaultBlockState());
    }

    private static TsunamiGeometrisi geometri(GameTestHelper h) {
        BlockPos k = m(h, 6, 2, 8);
        return new TsunamiGeometrisi(k.getX(), k.getZ() + 0.5, 1, 0, k.getY(), 70);
    }

    /** Afeti test ölçeğiyle başlatır, AKTIF'e geçirir ve her tick ilerletir. */
    private static TsunamiAfeti baslat(GameTestHelper h, int seviye) {
        ServerLevel l = h.getLevel();
        TsunamiAfeti a = new TsunamiAfeti();
        a.testIcin(geometri(h), OLCEK);
        a.baslat(l, new AfetBaglami(AfetTuru.TSUNAMI, seviye, m(h, 10, 3, 8), l.dimension(), null,
                DunyaDonemi.al(l).saat(), 0, 0, true));
        a.asamaIleri(l);
        a.asamaIleri(l);
        h.onEachTick(() -> a.tick(l, 1));
        return a;
    }

    private static SanalSuDunyasi dunya(GameTestHelper h) {
        return SanalSu.dunya(h.getLevel());
    }

    /** Boyutta dalga, çekilme ve ({@code magaraDa}) mağara kalmadı mı; boş sanal su dünyası {@code null} döner. */
    private static boolean bos(GameTestHelper h, boolean magaraDa) {
        SanalSuDunyasi d = dunya(h);
        return d == null || (d.dalgalar().isEmpty() && d.cekilmeler().isEmpty() && (!magaraDa || d.magaralar().isEmpty()));
    }

    private static long magaraHash(GameTestHelper h) {
        BlockPos a = m(h, 6, 0, 4);
        BlockPos b = m(h, 14, 7, 12);
        return SanalSu.bolgeHash(h.getLevel(), a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    /**
     * N14/N10: düz dünyada (deniz yok) yönetici tsunamiyi başlatmaz; 70 sütunluk 3 derin deniz küçük eşikle (30) yeterli,
     * dalga yönü denizden karaya (+x) ve kıyı suyun karaya bakan kenarında; deniz don(dur)ulunca yer uygun değil.
     */
    @GameTest(template = "su_alani", batch = "tsunami_yer", timeoutTicks = 100)
    public static void denizYoksaBaslamazDenizdenKarayaYonlenir(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        AfetYoneticisi y = AfetYoneticisi.al(l.getServer());
        y.durdur();
        BlockPos merkez = m(h, 10, 3, 8);
        h.assertTrue(KiyiAnalizi.analiz(l, merkez, 9, 30).isEmpty(), "denizsiz sahne uygun sayıldı");
        AfetYoneticisi.BaslatmaSonucu sonuc = y.elleBaslat(AfetTuru.TSUNAMI, 1, null, l, merkez);
        h.assertTrue(sonuc == AfetYoneticisi.BaslatmaSonucu.YER_UYGUN_DEGIL && y.aktifAfet().isEmpty(),
                "düz dünyada tsunami başladı: " + sonuc);
        sahne(h, 3);
        TsunamiGeometrisi g = KiyiAnalizi.analiz(l, merkez, 9, 30).orElse(null);
        h.assertTrue(g != null, "deniz bulunamadı");
        h.assertTrue(g.yonX() > 0.9, "dalga yönü karaya değil: " + g.yonX() + ", " + g.yonZ());
        int kiyiX = (int) Math.floor(g.kiyiX()) - m(h, 0, 0, 0).getX();
        h.assertTrue(kiyiX == 5, "kıyı suyun kara kenarında değil: x=" + kiyiX);
        h.assertTrue(g.taban() == m(h, 0, 3, 0).getY(), "taban su yüzeyinde değil: " + g.taban());
        kutu(h, 1, 3, 1, 5, 3, 14, Blocks.ICE.defaultBlockState());
        h.assertTrue(KiyiAnalizi.analiz(l, merkez, 9, 30).isEmpty(), "donmuş deniz uygun sayıldı (N10)");
        DogalAfetler.LOGGER.info("Tsunami GameTest: N14 sonuç={}, deniz sütunu={}, yön=({}, {})", sonuc, g.denizSutunu(),
                g.yonX(), g.yonZ());
        h.succeed();
    }

    /**
     * B.2 ve temiz durdurma: AKTIF'te çekilme ve S1 tablosuyla dalga başlar (ivme 0,04, hasar 0,5), dalga kıyıya varmadan
     * önce yükselir (sığlaşma), kıyıya vurunca çekilme biter; mağaraya dalga girince cep dolar; {@code durdur(temiz)}
     * dalga, çekilme ve mağara bırakmaz, mağara kutusunun hash'i baştakiyle aynı.
     */
    @GameTest(template = "su_alani", batch = "tsunami_stop", timeoutTicks = 300)
    public static void cekilmeDalgaMagaraVeStopTemiz(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        magaraSahnesi(h);
        long hashOnce = magaraHash(h);
        TsunamiAfeti a = baslat(h, 1);
        h.assertTrue(dunya(h).cekilmeler().containsKey(a.cekilmeKimligi()), "çekilme başlamadı");
        Dalga d = dunya(h).dalgalar().get(a.dalga1());
        h.assertTrue(d != null && d.ivme() == 0.04 && d.hasar() == 0.5, "S1 dalgası tabloyla değil: " + d);
        double ilk = d.yukseklik();
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(a.altAsama() >= 2 && a.cekilmeKimligi() < 0, "dalga kıyıya varmadı"))
                .thenExecute(() -> {
                    double simdi = dunya(h).dalgalar().get(a.dalga1()).yukseklik();
                    h.assertTrue(simdi > ilk, "dalga sığlaşırken yükselmedi: " + ilk + " -> " + simdi);
                    h.assertTrue(dunya(h).cekilmeler().isEmpty(), "kıyıya vuran dalgada çekilme sürüyor");
                })
                .thenWaitUntil(() -> h.assertTrue(!a.magaralar().isEmpty(), "mağara dolmadı"))
                .thenExecute(() -> {
                    h.assertTrue(dunya(h).magaralar().containsKey(a.magaralar().get(0)), "mağara cebi yok");
                    a.durdur(l, true);
                    h.assertTrue(bos(h, true), "stop sanal su bıraktı");
                    h.assertTrue(magaraHash(h) == hashOnce, "mağara kutusunun blokları değişti");
                    DogalAfetler.LOGGER.info("Tsunami GameTest: stop temiz, yükseklik {} -> kıyıda, mağara {}, hash={}",
                            ilk, a.magaralar(), Long.toHexString(hashOnce));
                })
                .thenSucceed();
    }

    /**
     * B.5 yıkım sırası (S2): aynı cephe satırındaki cam anında, ahşap 0,3 sn sonra kırılır; yaprak kırılır; kırıktaş (yalnız
     * S4–5) ve doğal arazi (toprak, taş taban) kalır. TS-15b: satır boyu cam şeridi (z 3..14) iki chunk'a taşabilir; tick
     * başına başlatılan birim (varsayılan chunk sütunu) {@code tsunami.yikimBolumTick} (1) aşılmaz, şeridin her bölümü yine
     * de kırılır. Tick başına birim afetin kendi biriktirdiği en büyük değerden okunur (TS-17): {@code onEachTick}
     * gözlemcisinin afet tick'ine göre aynı oyun tick'indeki sırası belirsizdir.
     */
    @GameTest(template = "su_alani", batch = "tsunami_yikim", timeoutTicks = 300)
    public static void dalgaOnceCamSonraAhsapYikarAraziKalir(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        sahne(h, 2);
        LongOpenHashSet camBolumleri = new LongOpenHashSet();
        for (int z = 3; z <= 14; z++) {
            if (z != 6) {
                h.setBlock(9, 3, z, Blocks.GLASS);
                BlockPos p = m(h, 9, 3, z);
                camBolumleri.add(SectionPos.asLong(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4));
            }
        }
        h.setBlock(9, 3, 6, Blocks.OAK_PLANKS);
        h.setBlock(10, 3, 10, Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        h.setBlock(11, 3, 8, Blocks.COBBLESTONE);
        TsunamiAfeti a = baslat(h, 2);
        int butce = TsunamiAyarlari.al().yikimBolumTick.get();
        long[] kirildi = {-1, -1};
        h.onEachTick(() -> {
            long t = DunyaDonemi.al(l).saat();
            if (kirildi[0] < 0 && l.getBlockState(m(h, 9, 3, 8)).isAir()) {
                kirildi[0] = t;
            }
            if (kirildi[1] < 0 && l.getBlockState(m(h, 9, 3, 6)).isAir()) {
                kirildi[1] = t;
            }
        });
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(kirildi[0] >= 0 && kirildi[1] >= 0
                        && l.getBlockState(m(h, 10, 3, 10)).isAir() && l.getBlockState(m(h, 9, 3, 3)).isAir()
                        && l.getBlockState(m(h, 9, 3, 14)).isAir(), "cam/ahşap/yaprak henüz kırılmadı"))
                .thenExecute(() -> {
                    h.assertTrue(kirildi[1] - kirildi[0] >= 5, "ahşap camdan sonra kırılmadı: " + kirildi[0] + ", "
                            + kirildi[1]);
                    int enCokBirim = a.enCokBirimTick();
                    h.assertTrue(enCokBirim >= 1 && enCokBirim <= butce, "tick başına başlatılan birim en çok " + enCokBirim
                            + " (bütçe " + butce + ")");
                    h.assertTrue(a.toplamBolum() >= camBolumleri.size(), "cam şeridinin bölümleri " + camBolumleri.size()
                            + ", değişen " + a.toplamBolum());
                    h.assertBlockPresent(Blocks.COBBLESTONE, 11, 3, 8);
                    h.assertBlockPresent(Blocks.DIRT, 9, 2, 8);
                    h.assertBlockPresent(Blocks.STONE, 9, 0, 8);
                    DogalAfetler.LOGGER.info("Tsunami GameTest: yıkım cam t={} ahşap t={} kırılan={} cam bölümü={} değişen "
                            + "bölüm={} tick başına en çok={}", kirildi[0], kirildi[1], a.kirilan(), camBolumleri.size(),
                            a.toplamBolum(), enCokBirim);
                    a.durdur(l, true);
                })
                .thenSucceed();
    }

    /**
     * Ts3-1 jetonlu hız (S5, blok jetonu {@code 48 × kısma}): iki cam yığını (her biri 3×8×7) sırayla yıkılır. Birinci yığının
     * bölümü tek tick'te kırılır ve jeton borçlanır; ikinci yığın borç kapanınca başlar: 1/4 TPS kısmasında (jeton 12) tam
     * hızdan (48) geç. Her tick'te en çok bir bölüm başlar, başlayan bölümün kalan bütün camı aynı tick'te kırılır, toplam
     * kırılan {@code jeton × tick + en büyük bölüm}ü aşmaz.
     */
    @GameTest(template = "su_alani", batch = "tsunami_jeton", timeoutTicks = 100)
    public static void yikimJetonuKismadaYavaslarBolumBolunmez(GameTestHelper h) {
        TsunamiSeviyesi sv = TsunamiSeviyesi.al(5);
        int tam = ikinciYiginGecikmesi(h, sv, 1.0, false);
        int kisma = ikinciYiginGecikmesi(h, sv, 0.25, false);
        h.assertTrue(kisma > tam, "TPS kısmasında yıkım yavaşlamadı: ikinci yığın tam hızda " + tam + ", kısmada " + kisma
                + " tick sonra");
        DogalAfetler.LOGGER.info("Tsunami GameTest: jeton ikinci yığın gecikmesi tam {} tick, kısma (x0,25) {} tick", tam, kisma);
        h.succeed();
    }

    /**
     * Ts4-3: aynı değişmezler varsayılan birimle (chunk sütunu, {@code tsunami.yikimSutunBirimi=true}): tick başına en çok bir
     * sütun başlar, başlayan sütunun hazır bütün camı aynı tick'te kırılır, kısmada (×0,25) ikinci yığın geç başlar.
     */
    @GameTest(template = "su_alani", batch = "tsunami_jeton_sutun", timeoutTicks = 100)
    public static void sutunKipindeJetonKismadaYavaslarSutunBolunmez(GameTestHelper h) {
        TsunamiSeviyesi sv = TsunamiSeviyesi.al(5);
        int tam = ikinciYiginGecikmesi(h, sv, 1.0, true);
        int kisma = ikinciYiginGecikmesi(h, sv, 0.25, true);
        h.assertTrue(kisma > tam, "sütun kipinde TPS kısmasında yıkım yavaşlamadı: tam " + tam + ", kısma " + kisma);
        DogalAfetler.LOGGER.info("Tsunami GameTest: sütun kipi jeton ikinci yığın gecikmesi tam {} tick, kısma {} tick", tam, kisma);
        h.succeed();
    }

    /**
     * Ts4-3: afetin yıkım ayarı {@link TpsOlcer} kısmasını okur (jeton ve tick başına birim kısmayla çarpılır); birim tavanı
     * kısmasız bütçenin 4 katı kalır (Ts4-2).
     */
    @GameTest(template = "su_alani", batch = "tsunami_tps")
    public static void yikimAyariTpsKismasiniOkur(GameTestHelper h) {
        TsunamiSeviyesi sv = TsunamiSeviyesi.al(5);
        TsunamiAyarlari a = TsunamiAyarlari.al();
        double carpan = a.yikimCarpani.get();
        TsunamiYikimi.Ayar tam = TsunamiAfeti.yikimAyari(sv);
        TpsOlcer.testKismasi(0.25);
        TsunamiYikimi.Ayar kisma;
        try {
            kisma = TsunamiAfeti.yikimAyari(sv);
        } finally {
            TpsOlcer.testKismasi(null);
        }
        h.assertTrue(tam.blok() == TsunamiYikimi.blokButcesi(sv, carpan, 1.0)
                && kisma.blok() == TsunamiYikimi.blokButcesi(sv, carpan, 0.25), "jeton kısmayı okumuyor: " + tam + " / " + kisma);
        h.assertTrue(kisma.birim() == Math.max(1, (int) Math.round(a.yikimBolumTick.get() * 0.25)), "birim kısmayı okumuyor");
        h.assertTrue(tam.birimTavani() == 4 * TsunamiYikimi.blokButcesi(sv, carpan, 1.0) && kisma.birimTavani() == tam.birimTavani(),
                "birim tavanı kısmasız bütçenin 4 katı değil: " + tam.birimTavani() + " / " + kisma.birimTavani());
        h.succeed();
    }

    /**
     * İki cam yığınını sırayla tek yıkım kuyruğuyla yıkar, her tick'i denetler; ikinci yığının ilk kırıldığı tick (başından). Birim
     * {@code sutun} ise chunk sütunu, değilse bölüm (16³).
     */
    private static int ikinciYiginGecikmesi(GameTestHelper h, TsunamiSeviyesi sv, double kisma, boolean sutun) {
        ServerLevel l = h.getLevel();
        sahne(h, 2);
        int ust = m(h, 0, 10, 0).getY();
        double taban = m(h, 0, 3, 0).getY();
        TsunamiGeometrisi g = geometri(h);
        TsunamiYikimi y = new TsunamiYikimi();
        TsunamiYikimi.Ayar ayar = new TsunamiYikimi.Ayar(TsunamiYikimi.blokButcesi(sv, 1.0, kisma), 1, 1, 0, 0, sutun,
                4 * TsunamiYikimi.blokButcesi(sv, 1.0, 1.0));
        long simdi = DunyaDonemi.al(l).saat();
        int tick = 0;
        int toplam = 0;
        int ikinci = -1;
        int enBuyuk = 0;
        for (int yigin = 0; yigin < 2; yigin++) {
            int x0 = 7 + 4 * yigin;
            kutu(h, x0, 3, 5, x0 + 2, 10, 11, Blocks.GLASS.defaultBlockState());
            List<BlockPos> camlar = new ArrayList<>();
            Long2IntOpenHashMap kalan = new Long2IntOpenHashMap();
            for (BlockPos p : BlockPos.betweenClosed(m(h, x0, 3, 5), m(h, x0 + 2, 10, 11))) {
                camlar.add(p.immutable());
                kalan.addTo(birimAnahtari(p, sutun), 1);
            }
            enBuyuk = Math.max(enBuyuk, kalan.values().intStream().max().orElse(0));
            for (double b = x0 - 6 + 0.1; b < x0 - 6 + 3; b += 0.7) {
                y.satirEkle(g, b, 8, ust);
            }
            int bas = tick;
            while (!camlar.isEmpty() && tick - bas < 100) {
                y.tick(l, simdi + tick, sv, taban, kisma, ayar, true, p -> false);
                tick++;
                Long2IntOpenHashMap kirilan = new Long2IntOpenHashMap();
                camlar.removeIf(p -> {
                    if (!l.getBlockState(p).isAir()) {
                        return false;
                    }
                    kirilan.addTo(birimAnahtari(p, sutun), 1);
                    return true;
                });
                h.assertTrue(kirilan.size() <= 1, "bir tick'te " + kirilan.size() + (sutun ? " sütun" : " bölüm") + " başladı");
                for (var e : kirilan.long2IntEntrySet()) {
                    h.assertTrue(e.getIntValue() == kalan.get(e.getLongKey()), "bölüm bölündü: " + e.getIntValue() + " / "
                            + kalan.get(e.getLongKey()));
                    kalan.addTo(e.getLongKey(), -e.getIntValue());
                    toplam += e.getIntValue();
                }
                if (yigin == 1 && ikinci < 0 && !kirilan.isEmpty()) {
                    ikinci = tick - bas;
                }
                h.assertTrue(toplam <= ayar.blok() * tick + enBuyuk, "ortalama hız jetonu aştı: " + toplam + " blok / " + tick
                        + " tick (jeton " + ayar.blok() + ")");
            }
            h.assertTrue(camlar.isEmpty(), "cam yığını 100 tick'te bitmedi: kalan " + camlar.size());
        }
        return ikinci;
    }

    private static long birimAnahtari(BlockPos p, boolean sutun) {
        return sutun ? ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4) : SectionPos.asLong(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4);
    }

    /**
     * B.3/B.4 ve #2 Dalga Avcısı: karadaki sahte oyuncuyu dalga sürükleyince afet {@link Ipucu#SURUKLENDI} yayınlar (bir kez);
     * dalgadaki oyuncu ve domuz hasar alır.
     */
    @GameTest(template = "su_alani", batch = "tsunami_ipucu", timeoutTicks = 300)
    public static void dalgaOyuncuyuSurukleyinceIpucuVerirHasarVerir(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        sahne(h, 2);
        BlockPos ayak = m(h, 10, 3, 8);
        ServerPlayer oyuncu = OyunTestiTemizligi.oyuncu(h, ayak.getX() + 0.5, ayak.getY(), ayak.getZ() + 0.5);
        oyuncu.setGameMode(GameType.SURVIVAL);
        // Hasar dalga ilerlerken verilir (menzilde duran su itmez): domuz kıyıya yakın, cephenin uzun süre geçtiği yerde;
        // yürüyüp dalga şeridinden çıkmasın diye yapay zekâsız. Yapay zekâsız mob travel etmediği için dalga onu sürüklemez:
        // domuzda yalnız hasar sınanır (mob sürüklemesi SuOyunTestleri.dalgaMobuVeEsyayiSurukler).
        Pig domuz = h.spawn(EntityType.PIG, 7, 3, 11);
        domuz.setNoAi(true);
        h.onEachTick(oyuncu::doTick);
        float oyuncuCan = oyuncu.getHealth();
        float domuzCan = domuz.getHealth();
        List<Ipucu> gelen = new ArrayList<>();
        Consumer<BasarimIpucuEvent> dinleyici = e -> {
            if (e.oyuncu() == oyuncu) {
                gelen.add(e.ipucu());
            }
        };
        OyunTestiTemizligi.dinle(BasarimIpucuEvent.class, dinleyici);
        TsunamiAfeti a = baslat(h, 2);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(!gelen.isEmpty(), "SURUKLENDI ipucu gelmedi"))
                .thenWaitUntil(() -> h.assertTrue(oyuncu.getHealth() < oyuncuCan && domuz.getHealth() < domuzCan,
                        "dalga hasarı yok: oyuncu " + oyuncu.getHealth() + " domuz " + domuz.getHealth()))
                .thenExecute(() -> {
                    h.assertTrue(gelen.size() == 1 && gelen.get(0) == Ipucu.SURUKLENDI, "ipucu: " + gelen);
                    DogalAfetler.LOGGER.info("Tsunami GameTest: ipucu {} oyuncu can {} -> {} domuz can {} -> {}", gelen, oyuncuCan,
                            oyuncu.getHealth(), domuzCan, domuz.getHealth());
                    a.durdur(l, true);
                    l.getServer().getPlayerList().remove(oyuncu);
                })
                .thenSucceed();
    }

    /**
     * Doğal bitiş: AKTIF son dalga menzile varınca, BITIS dalga sönünce biter; mağara suyu kalır ({@code magara_suyu} süreci
     * kurutur), dalga ve çekilme kalmaz, mağara kutusunun blokları değişmez.
     */
    @GameTest(template = "su_alani", batch = "tsunami_dogal", timeoutTicks = 600)
    public static void dogalBitisteMagaraSuyuKalirDalgaKalmaz(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        magaraSahnesi(h);
        long hashOnce = magaraHash(h);
        TsunamiAfeti a = baslat(h, 1);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(a.sonlandi(), "afet kendiliğinden bitmedi: " + a.asama()))
                .thenExecute(() -> {
                    a.durdur(l, true);
                    h.assertTrue(bos(h, false), "dalga/çekilme kaldı");
                    h.assertTrue(!a.magaralar().isEmpty() && dunya(h).magaralar().containsKey(a.magaralar().get(0)),
                            "doğal bitişte mağara suyu kaldırıldı");
                    h.assertTrue(magaraHash(h) == hashOnce, "mağara kutusunun blokları değişti");
                    a.magaralar().forEach(id -> SanalSu.magaraBitir(l, id));
                })
                .thenSucceed();
    }

    /**
     * Ts3 + Ts4-3: S2 doğal bitişi yıkımlı sahnede, bekleyen yıkım boşalınca gelir ({@code doğal=true}, bekleyen 0). Dalga
     * tepesini aşan ahşap sütun (y 3..18, şablonun üstüne taşar) boşluk bırakmadan tamamen yıkılır: üst kısmı havada kalmaz.
     */
    @GameTest(template = "su_alani", batch = "tsunami_dogal_yikim", timeoutTicks = 900)
    public static void dogalBitisYikimBitinceGelirDalgaUstuKalmaz(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        sahne(h, 2);
        kutu(h, 10, 3, 8, 10, 18, 8, Blocks.OAK_PLANKS.defaultBlockState());
        for (int z = 4; z <= 12; z += 2) {
            h.setBlock(8, 3, z, Blocks.GLASS);
        }
        TsunamiAfeti a = baslat(h, 2);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(a.sonlandi(), "afet kendiliğinden bitmedi: " + a.asama()))
                .thenExecute(() -> {
                    h.assertTrue(a.dogalBitti(), "bitiş doğal sayılmadı");
                    h.assertTrue(a.bekleyenYikim() == 0, "doğal bitişte bekleyen yıkım var: " + a.bekleyenYikim());
                    h.assertTrue(a.kirilan() > 0, "yıkımlı sahnede blok kırılmadı");
                    for (int y = 3; y <= 18; y++) {
                        h.assertTrue(l.getBlockState(m(h, 10, y, 8)).isAir(), "ahşap sütun y=" + y + " kaldı (dalga üstü havada)");
                    }
                    DogalAfetler.LOGGER.info("Tsunami GameTest: doğal bitiş yıkımla kırılan={} bekleyen={} doğal={}", a.kirilan(),
                            a.bekleyenYikim(), a.dogalBitti());
                    a.durdur(l, true);
                    a.magaralar().forEach(id -> SanalSu.magaraBitir(l, id));
                })
                .thenSucceed();
    }

    /**
     * K-E39 (inceleme-entegrasyon-5 P2-1): dalga tepesini aşan yapı yıkılır (Ts3), ama zemini dalga tepesinden yüksek yamaçtaki
     * yapıya dalga ulaşmaz: sütun taramasının yukarı uzaması ilk doğal arazide kesilir. Karada toprak tepe (y 3..12) ve üstünde
     * kalas (y 13..15) ayakta kalır; dalganın içindeki cam kırılır. S2 düşen dalganın tepesi en çok y 10 (Ts3 kırmızı koşusu).
     */
    @GameTest(template = "su_alani", batch = "tsunami_yamac", timeoutTicks = 900)
    public static void yamacUstuYapiyaDalgaUlasmazKalir(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        sahne(h, 2);
        kutu(h, 11, 3, 3, 12, 12, 5, Blocks.DIRT.defaultBlockState());
        kutu(h, 11, 13, 3, 12, 15, 5, Blocks.OAK_PLANKS.defaultBlockState());
        h.setBlock(8, 3, 8, Blocks.GLASS);
        TsunamiAfeti a = baslat(h, 2);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(a.sonlandi(), "afet kendiliğinden bitmedi: " + a.asama()))
                .thenExecute(() -> {
                    h.assertTrue(l.getBlockState(m(h, 8, 3, 8)).isAir() && a.kirilan() > 0, "dalganın içindeki cam kırılmadı");
                    int kalas = 0;
                    for (BlockPos p : BlockPos.betweenClosed(m(h, 11, 13, 3), m(h, 12, 15, 5))) {
                        kalas += l.getBlockState(p).is(Blocks.OAK_PLANKS) ? 1 : 0;
                    }
                    h.assertTrue(kalas == 18, "yamaç üstü kalas yıkıldı: 18 bloktan " + kalas + " kaldı");
                    DogalAfetler.LOGGER.info("Tsunami GameTest: yamaç üstü yapı ayakta (kalas {}/18), kırılan {}", kalas, a.kirilan());
                    a.durdur(l, true);
                    a.magaralar().forEach(id -> SanalSu.magaraBitir(l, id));
                    kutu(h, 11, 12, 3, 12, 15, 5, Blocks.AIR.defaultBlockState());
                })
                .thenSucceed();
    }

    /**
     * Madde 11 {@code afetBlokHasari=false} (izleme-inceleme-5 P2-1): S2 dalgası kıyıya vurup kendiliğinden biter, hiçbir blok
     * kırılmaz; sahnenin (deniz, kara, cam, kalas, yaprak) blok hash'i baştakiyle aynı.
     */
    @GameTest(template = "su_alani", batch = "tsunami_blok_hasari", timeoutTicks = 900)
    public static void blokHasariKapaliykenDalgaBlokKirmaz(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        sahne(h, 2);
        h.setBlock(9, 3, 6, Blocks.OAK_PLANKS);
        for (int z = 4; z <= 12; z += 2) {
            h.setBlock(8, 3, z, Blocks.GLASS);
        }
        h.setBlock(10, 3, 10, Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        OyunTestiTemizligi.blokHasari(l, false);
        BlockPos a = m(h, 0, 0, 0);
        BlockPos b = m(h, 15, 10, 15);
        long once = SanalSu.bolgeHash(l, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
        TsunamiAfeti t = baslat(h, 2);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(t.sonlandi(), "afet kendiliğinden bitmedi: " + t.asama()))
                .thenExecute(() -> {
                    long sonra = SanalSu.bolgeHash(l, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
                    h.assertTrue(t.kirilan() == 0, "blok hasarı kapalıyken " + t.kirilan() + " blok kırıldı");
                    h.assertTrue(sonra == once, "blok hasarı kapalıyken sahne değişti");
                    h.assertBlockPresent(Blocks.OAK_PLANKS, 9, 3, 6);
                    DogalAfetler.LOGGER.info("Tsunami GameTest: blok hasarı kapalı, doğal bitiş, kırılan {} hash {}", t.kirilan(),
                            Long.toHexString(once));
                    t.durdur(l, true);
                    t.magaralar().forEach(id -> SanalSu.magaraBitir(l, id));
                })
                .thenSucceed();
    }

    /**
     * Kalıcılık (sanal su §A2): kayıttan okunan afet dalgayı yeniden başlatmaz, aynı kimliği sürdürür; aynı afet numarasıyla
     * ikinci {@code dalgaBaslat} yeni dalga eklemez.
     */
    @GameTest(template = "su_alani", batch = "tsunami_kayit", timeoutTicks = 200)
    public static void kayittanOkunanAfetDalgayiIkilemez(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        sahne(h, 2);
        TsunamiAfeti a = new TsunamiAfeti();
        a.testIcin(geometri(h), OLCEK);
        a.baslat(l, new AfetBaglami(AfetTuru.TSUNAMI, 1, m(h, 10, 3, 8), l.dimension(), null, DunyaDonemi.al(l).saat(), 0,
                0, true));
        a.asamaIleri(l);
        a.asamaIleri(l);
        int dalgaSayisi = dunya(h).dalgalar().size();
        CompoundTag kayit = a.durumYaz(new CompoundTag());
        TsunamiAfeti b = new TsunamiAfeti();
        b.durumOku(kayit);
        b.yuklendi(l);
        h.assertTrue(b.dalga1() == a.dalga1() && SanalSu.dalgaVarMi(l, b.dalga1()), "kayıttaki dalga kimliği kayboldu");
        h.assertTrue(SanalSu.dalgaBaslat(l, dunya(h).dalgalar().get(b.dalga1()), b.baglam().baslangicSaati()) == b.dalga1(),
                "aynı afet numarasıyla ikinci dalga eklendi");
        h.onEachTick(() -> b.tick(l, 1));
        h.startSequence()
                .thenExecuteAfter(40, () -> {
                    h.assertTrue(dunya(h).dalgalar().size() == dalgaSayisi, "yüklenen afet dalga ekledi");
                    b.durdur(l, true);
                    h.assertTrue(!SanalSu.dalgaVarMi(l, a.dalga1()), "durdurulan afetin dalgası kaldı");
                })
                .thenSucceed();
    }

    /**
     * H.5: ay çarpmasından sonraki tsunami çarpanı {@link DunyaDonemi#carpan}'dan okunur: zamanlayıcının seçimi (girdi
     * yöneticideki gibi {@code donem::carpan}) tsunamiyi çarpan kadar daha sık seçer, çekilme {@code tsunami.ayCekilmeCarpani}
     * ile derinleşir. Çarpan ay yolundaki gibi
     * {@link DunyaDonemi#carpanEkle} ile ama 40 tick'lik eklenir: süresi dolunca 1'e iner, global dönem eski değerine
     * döner (K-E15) ve çekilme yeniden normal derinlikte.
     */
    @GameTest(template = "su_alani", batch = "tsunami_ay", timeoutTicks = 300)
    public static void ayCarpaniOraniArtirirCekilmeyiDerinlestirir(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        sahne(h, 2);
        DunyaDonemi donem = DunyaDonemi.al(l);
        CekirdekAyarlari ayar = CekirdekAyarlari.al();
        double once = donem.carpan(AfetTuru.TSUNAMI);
        h.assertTrue(once == 1.0, "başta tsunami çarpanı 1 değil: " + once);
        double derinOnce = cekilmeDerinligi(h);
        double payOnce = tsunamiSecimPayi(ayar, donem);
        h.assertTrue(payOnce > 0 && payOnce < 1, "kasırga/tsunami seçim payı sınanamaz: " + payOnce);
        double tepe = ayar.tsunamiCarpani();
        donem.carpanEkle(AfetTuru.TSUNAMI, tepe, 20, 20, SonumTipi.DOGRUSAL);
        double carpan = donem.carpan(AfetTuru.TSUNAMI);
        double payAy = tsunamiSecimPayi(ayar, donem);
        double oranKati = payAy / (1 - payAy) / (payOnce / (1 - payOnce));
        h.assertTrue(carpan > 1 && Math.abs(oranKati - carpan) < 0.01 * carpan,
                "zamanlayıcı tsunamiyi çarpan kadar sık seçmedi: pay " + payOnce + " -> " + payAy + ", kat " + oranKati
                        + " (çarpan " + carpan + ", config " + tepe + ")");
        double derinAy = cekilmeDerinligi(h);
        double kat = TsunamiAyarlari.al().ayCekilmeCarpani.get();
        h.assertTrue(Math.abs(derinAy - Math.min(8, derinOnce * kat)) < 1e-9,
                "ay sonrası çekilme derinleşmedi: " + derinOnce + " -> " + derinAy + " (kat " + kat + ")");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(donem.carpan(AfetTuru.TSUNAMI) == 1.0,
                        "çarpan süresi dolmadı: " + donem.carpan(AfetTuru.TSUNAMI)))
                .thenExecute(() -> {
                    double derinSonra = cekilmeDerinligi(h);
                    h.assertTrue(derinSonra == derinOnce, "çarpan bitince çekilme normale dönmedi: " + derinSonra);
                    DogalAfetler.LOGGER.info("Tsunami GameTest: H.5 seçim payı {} -> {} (kat {}, çarpan {}), çekilme derinliği "
                            + "{} -> {} -> {}", payOnce, payAy, oranKati, carpan, derinOnce, derinAy, derinSonra);
                })
                .thenSucceed();
    }

    /**
     * Zamanlayıcının seçim yolu (H.5, Ts3-2): {@link AfetZamanlayicisi#sec} girdisi yöneticideki gibi {@code donem::carpan}
     * ile kurulur; uygun türler kasırga ve tsunami, zorlamalı (bekleme ve olay olasılığı yok). Seçim rastgelesi [0, 1)'i eşit
     * adımla tarar; dönen tsunami payı {@code a·c / (a·c + b)} (a, b ağırlıklar, c çarpan), payların oran katı {@code c}.
     */
    private static double tsunamiSecimPayi(CekirdekAyarlari ayar, DunyaDonemi donem) {
        int adim = 20_000;
        double[] r = new double[1];
        RandomGenerator izgara = () -> (long) (r[0] * 0x1.0p53) << 11;
        AfetZamanlayicisi.Girdi girdi = new AfetZamanlayicisi.Girdi(false, donem.saat(), 1, new BeklemeDurumu(),
                EnumSet.of(AfetTuru.KASIRGA, AfetTuru.TSUNAMI), donem::carpan, 1, true);
        int tsunami = 0;
        for (int i = 0; i < adim; i++) {
            r[0] = (i + 0.5) / adim;
            if (AfetZamanlayicisi.sec(ayar.zamanlayici(), girdi, izgara).orElse(null) == AfetTuru.TSUNAMI) {
                tsunami++;
            }
        }
        return (double) tsunami / adim;
    }

    /** S1 afeti AKTIF'e geçirip sanal sudaki çekilmesinin derinliğini okur ve afeti temiz durdurur. */
    private static double cekilmeDerinligi(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        TsunamiAfeti a = new TsunamiAfeti();
        a.testIcin(geometri(h), OLCEK);
        a.baslat(l, new AfetBaglami(AfetTuru.TSUNAMI, 1, m(h, 10, 3, 8), l.dimension(), null, DunyaDonemi.al(l).saat(), 0,
                0, true));
        a.asamaIleri(l);
        a.asamaIleri(l);
        double d = dunya(h).cekilmeler().get(a.cekilmeKimligi()).derinlik();
        a.durdur(l, true);
        return d;
    }
}
