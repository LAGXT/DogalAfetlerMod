package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.config.CekirdekAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetBaglami;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.AfetZamanlayicisi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonumTipi;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
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
     * başına değişen bölüm {@code tsunami.yikimBolumTick} (1) aşılmaz, şeridin her bölümü yine de kırılır.
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
        int[] enCokBolum = {0};
        h.onEachTick(() -> {
            long t = DunyaDonemi.al(l).saat();
            if (kirildi[0] < 0 && l.getBlockState(m(h, 9, 3, 8)).isAir()) {
                kirildi[0] = t;
            }
            if (kirildi[1] < 0 && l.getBlockState(m(h, 9, 3, 6)).isAir()) {
                kirildi[1] = t;
            }
            enCokBolum[0] = Math.max(enCokBolum[0], a.sonTickBirim());
        });
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(kirildi[0] >= 0 && kirildi[1] >= 0
                        && l.getBlockState(m(h, 10, 3, 10)).isAir() && l.getBlockState(m(h, 9, 3, 3)).isAir()
                        && l.getBlockState(m(h, 9, 3, 14)).isAir(), "cam/ahşap/yaprak henüz kırılmadı"))
                .thenExecute(() -> {
                    h.assertTrue(kirildi[1] - kirildi[0] >= 5, "ahşap camdan sonra kırılmadı: " + kirildi[0] + ", "
                            + kirildi[1]);
                    h.assertTrue(enCokBolum[0] >= 1 && enCokBolum[0] <= butce, "tick başına değişen bölüm " + enCokBolum[0]
                            + " (bütçe " + butce + ")");
                    h.assertTrue(a.toplamBolum() >= camBolumleri.size(), "cam şeridinin bölümleri " + camBolumleri.size()
                            + ", değişen " + a.toplamBolum());
                    h.assertBlockPresent(Blocks.COBBLESTONE, 11, 3, 8);
                    h.assertBlockPresent(Blocks.DIRT, 9, 2, 8);
                    h.assertBlockPresent(Blocks.STONE, 9, 0, 8);
                    DogalAfetler.LOGGER.info("Tsunami GameTest: yıkım cam t={} ahşap t={} kırılan={} cam bölümü={} değişen "
                            + "bölüm={} tick başına en çok={}", kirildi[0], kirildi[1], a.kirilan(), camBolumleri.size(),
                            a.toplamBolum(), enCokBolum[0]);
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
        int tam = ikinciYiginGecikmesi(h, sv, 1.0);
        int kisma = ikinciYiginGecikmesi(h, sv, 0.25);
        h.assertTrue(kisma > tam, "TPS kısmasında yıkım yavaşlamadı: ikinci yığın tam hızda " + tam + ", kısmada " + kisma
                + " tick sonra");
        DogalAfetler.LOGGER.info("Tsunami GameTest: jeton ikinci yığın gecikmesi tam {} tick, kısma (x0,25) {} tick", tam, kisma);
        h.succeed();
    }

    /** İki cam yığınını sırayla tek yıkım kuyruğuyla yıkar, her tick'i denetler; ikinci yığının ilk kırıldığı tick (başından). */
    private static int ikinciYiginGecikmesi(GameTestHelper h, TsunamiSeviyesi sv, double kisma) {
        ServerLevel l = h.getLevel();
        sahne(h, 2);
        int ust = m(h, 0, 10, 0).getY();
        double taban = m(h, 0, 3, 0).getY();
        TsunamiGeometrisi g = geometri(h);
        TsunamiYikimi y = new TsunamiYikimi();
        TsunamiYikimi.Ayar ayar = new TsunamiYikimi.Ayar(TsunamiYikimi.blokButcesi(sv, 1.0, kisma), 1, 1, 0, 0, false);
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
                kalan.addTo(SectionPos.asLong(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4), 1);
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
                    kirilan.addTo(SectionPos.asLong(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4), 1);
                    return true;
                });
                h.assertTrue(kirilan.size() <= 1, "bir tick'te " + kirilan.size() + " bölüm başladı");
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

    /**
     * B.3/B.4 ve #2 Dalga Avcısı: karadaki sahte oyuncuyu dalga sürükleyince afet {@link Ipucu#SURUKLENDI} yayınlar (bir kez);
     * dalgadaki oyuncu ve domuz hasar alır.
     */
    @GameTest(template = "su_alani", batch = "tsunami_ipucu", timeoutTicks = 300)
    public static void dalgaOyuncuyuSurukleyinceIpucuVerirHasarVerir(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        sahne(h, 2);
        BlockPos ayak = m(h, 10, 3, 8);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(h, ayak.getX() + 0.5, ayak.getY(), ayak.getZ() + 0.5);
        oyuncu.setGameMode(GameType.SURVIVAL);
        // Hasar dalga ilerlerken verilir (menzilde duran su itmez): domuz kıyıya yakın, cephenin uzun süre geçtiği yerde.
        Pig domuz = h.spawn(EntityType.PIG, 7, 3, 11);
        h.onEachTick(oyuncu::doTick);
        float oyuncuCan = oyuncu.getHealth();
        float domuzCan = domuz.getHealth();
        List<Ipucu> gelen = new ArrayList<>();
        Consumer<BasarimIpucuEvent> dinleyici = e -> {
            if (e.oyuncu() == oyuncu) {
                gelen.add(e.ipucu());
            }
        };
        MinecraftForge.EVENT_BUS.addListener(dinleyici);
        TsunamiAfeti a = baslat(h, 2);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(!gelen.isEmpty(), "SURUKLENDI ipucu gelmedi"))
                .thenWaitUntil(() -> h.assertTrue(oyuncu.getHealth() < oyuncuCan && domuz.getHealth() < domuzCan,
                        "dalga hasarı yok: oyuncu " + oyuncu.getHealth() + " domuz " + domuz.getHealth()))
                .thenExecute(() -> {
                    MinecraftForge.EVENT_BUS.unregister(dinleyici);
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
     * H.5: ay çarpmasından sonraki tsunami çarpanı {@link DunyaDonemi#carpan}'dan okunur: zamanlayıcının tsunami oranı
     * çarpanla artar, çekilme {@code tsunami.ayCekilmeCarpani} ile derinleşir. Çarpan ay yolundaki gibi
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
        double tepe = ayar.tsunamiCarpani();
        donem.carpanEkle(AfetTuru.TSUNAMI, tepe, 20, 20, SonumTipi.DOGRUSAL);
        double oranOnce = AfetZamanlayicisi.turOrani(ayar.zamanlayici(), AfetTuru.TSUNAMI, once, 1);
        double oranAy = AfetZamanlayicisi.turOrani(ayar.zamanlayici(), AfetTuru.TSUNAMI, donem.carpan(AfetTuru.TSUNAMI), 1);
        h.assertTrue(tepe > 1 && oranAy > oranOnce && Math.abs(oranAy - oranOnce * tepe) < 1e-9 * oranAy,
                "tsunami oranı çarpanla artmadı: " + oranOnce + " -> " + oranAy + " (çarpan " + tepe + ")");
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
                    DogalAfetler.LOGGER.info("Tsunami GameTest: H.5 oran {} -> {} (x{}), çekilme derinliği {} -> {} -> {}",
                            oranOnce, oranAy, tepe, derinOnce, derinAy, derinSonra);
                })
                .thenSucceed();
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
