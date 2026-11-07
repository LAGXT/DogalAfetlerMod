package io.github.kadirberkpolat1.dogalafetler.afet.deprem;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.ArtciPlani;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.FayHatti;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimKimlikleri;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetAsamaDegistiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Depremin oyun içi testleri (GOREV §6 #1–#4, #3 "afetBlokHasari=false iken blok değişmez; deprem dayanıklı blok kırılmaz").
 * Sahne {@code deprem_sahne} (32x20x32 hava) kutusuna her test kendisi kurulur ve geçince boşaltılır (kutu y ≈ −60'ta
 * olduğundan kazı tabanı test süresince dünya tabanına iner, {@link DepremAyarlari#geciciEnAltY}); deprem merkezi
 * kutunun ortasına sabitlenir (geçici kayıt, başlatılır başlatılmaz geri alınır). Her test tek afet yuvasını kullandığı
 * için kendi batch'indedir. Sahne yardımcılarını sinkhole testleri de kullanır. Başarısız biten test de batch sonunda
 * temizlenir ({@link #batchSonu}: afet durur, kazı tabanı, {@code afetBlokHasari}, dinleyiciler ve sahte oyuncular geri; K-E15).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DepremOyunTestleri {
    public static final String SAHNE = "deprem_sahne";
    public static final int BOY = 32;
    public static final int YUKSEKLIK = 20;
    private static final int YUZEY = 9;
    private static final BlockPos MERKEZ = new BlockPos(16, YUZEY + 1, 16);
    private static final BlockPos[] KOSELER = {new BlockPos(1, YUZEY + 1, 1), new BlockPos(30, YUZEY + 1, 1),
            new BlockPos(1, YUZEY + 1, 30), new BlockPos(30, YUZEY + 1, 30)};

    /** İlk {@link #blokHasari} çağrısından önceki kural değeri; sahne temizlenince null. */
    private static Boolean eskiBlokHasari;
    private static final List<ServerPlayer> OYUNCULAR = new ArrayList<>();
    private static final List<Object> DINLEYICILER = new ArrayList<>();

    private DepremOyunTestleri() {
    }

    @AfterBatch(batch = "deprem_yarik")
    public static void yarikSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    @AfterBatch(batch = "deprem_kapali")
    public static void kapaliSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    @AfterBatch(batch = "deprem_stop")
    public static void stopSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    @AfterBatch(batch = "deprem_basarim")
    public static void basarimSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    @AfterBatch(batch = "deprem_artci")
    public static void artciSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    /** Batch sonu (geçen ya da başarısız test): afet durur, kazı tabanı, kural, dinleyici ve sahte oyuncular geri döner. */
    public static void batchSonu(ServerLevel level) {
        AfetYoneticisi.al(level.getServer()).durdur();
        DepremAyarlari.geciciEnAltY(null);
        if (eskiBlokHasari != null) {
            level.getGameRules().getRule(Kurallar.AFET_BLOK_HASARI).set(eskiBlokHasari, level.getServer());
            eskiBlokHasari = null;
        }
        DINLEYICILER.forEach(MinecraftForge.EVENT_BUS::unregister);
        DINLEYICILER.clear();
        oyunculariCikar(level);
    }

    /** Batch sonunda çıkarılan sahte oyuncu. */
    public static ServerPlayer oyuncu(GameTestHelper y, double x, double yy, double z) {
        ServerPlayer o = CekirdekOyunTestleri.sahteOyuncu(y, x, yy, z);
        OYUNCULAR.add(o);
        return o;
    }

    /** Batch sonunda kaldırılan olay dinleyicisi. */
    public static <T extends Event> void dinle(Class<T> tur, Consumer<T> dinleyici) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, tur, dinleyici);
        DINLEYICILER.add(dinleyici);
    }

    private static void oyunculariCikar(ServerLevel level) {
        for (ServerPlayer o : OYUNCULAR) {
            if (level.getServer().getPlayerList().getPlayer(o.getUUID()) == o) {
                level.getServer().getPlayerList().remove(o);
            }
        }
        OYUNCULAR.clear();
    }

    /** Taban: y 1–({@code yuzey}−2) taş, bir kat toprak, yüzeyde çimen; üstü hava. */
    public static void zemin(GameTestHelper y, int yuzey) {
        DepremAyarlari.geciciEnAltY(y.getLevel().getMinBuildHeight() + 1);
        for (int x = 0; x < BOY; x++) {
            for (int z = 0; z < BOY; z++) {
                for (int k = 1; k <= yuzey; k++) {
                    Block b = k == yuzey ? Blocks.GRASS_BLOCK : k == yuzey - 1 ? Blocks.DIRT : Blocks.STONE;
                    y.setBlock(new BlockPos(x, k, z), b);
                }
            }
        }
    }

    public static AABB kutu(GameTestHelper y) {
        return new AABB(y.absolutePos(BlockPos.ZERO), y.absolutePos(new BlockPos(BOY, YUKSEKLIK, BOY)));
    }

    public static long kutuHash(GameTestHelper y) {
        BlockPos a = y.absolutePos(BlockPos.ZERO);
        BlockPos b = y.absolutePos(new BlockPos(BOY - 1, YUKSEKLIK - 1, BOY - 1));
        return SanalSu.bolgeHash(y.getLevel(), Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), b.getY(), Math.max(a.getZ(), b.getZ()));
    }

    public static boolean blokHasari(GameTestHelper y, boolean deger) {
        var kural = y.getLevel().getGameRules().getRule(Kurallar.AFET_BLOK_HASARI);
        boolean eski = kural.get();
        if (eskiBlokHasari == null) {
            eskiBlokHasari = eski;
        }
        kural.set(deger, y.getLevel().getServer());
        return eski;
    }

    public static int dusenSayisi(GameTestHelper y) {
        return y.getLevel().getEntitiesOfClass(FallingBlockEntity.class, kutu(y).inflate(4)).size();
    }

    /**
     * Test geçince (kural testçe geri konduktan sonra): afet durur, sahte oyuncular çıkar, düşen blok ve eşyalar kalkar, kutu
     * havayla boşalır.
     */
    public static void sahneyiTemizle(GameTestHelper y) {
        AfetYoneticisi.al(y.getLevel().getServer()).durdur();
        DepremAyarlari.geciciEnAltY(null);
        eskiBlokHasari = null;
        oyunculariCikar(y.getLevel());
        for (Entity e : y.getLevel().getEntitiesOfClass(Entity.class, kutu(y).inflate(4),
                e -> e instanceof FallingBlockEntity || e instanceof ItemEntity)) {
            e.discard();
        }
        for (int x = 0; x < BOY; x++) {
            for (int z = 0; z < BOY; z++) {
                for (int k = 0; k < YUKSEKLIK; k++) {
                    y.setBlock(new BlockPos(x, k, z), Blocks.AIR);
                }
            }
        }
    }

    /** Sabit merkezli afeti geçici kayıtla başlatır, kaydı hemen geri alır ve AKTIF'e ilerletir. */
    public static <T extends io.github.kadirberkpolat1.dogalafetler.core.Afet> T aktifBaslat(GameTestHelper y,
            AfetTuru tur, int seviye, BlockPos merkez, Supplier<T> fabrika, ServerPlayer oyuncu) {
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        Object[] olusan = new Object[1];
        try (AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(tur, () -> {
            olusan[0] = fabrika.get();
            return (io.github.kadirberkpolat1.dogalafetler.core.Afet) olusan[0];
        })) {
            AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(tur, seviye, oyuncu, y.getLevel(), merkez);
            if (sonuc != AfetYoneticisi.BaslatmaSonucu.BASLADI) {
                y.fail(tur.kimlik() + " başlamadı: " + sonuc);
            }
        }
        @SuppressWarnings("unchecked")
        T afet = (T) olusan[0];
        while (afet.asama() != AfetAsamasi.AKTIF) {
            if (!yonetici.asamaIleri()) {
                y.fail("aşama ilerlemedi");
            }
        }
        return afet;
    }

    private static Deprem baslat(GameTestHelper y, int seviye) {
        AfetYoneticisi.al(y.getLevel().getServer()).durdur();
        BlockPos merkez = y.absolutePos(MERKEZ);
        ServerPlayer oyuncu = oyuncu(y, merkez.getX() + 0.5, merkez.getY(), merkez.getZ() - 9.5);
        return aktifBaslat(y, AfetTuru.DEPREM, seviye, merkez, () -> Deprem.sabitMerkezli(merkez), oyuncu);
    }

    private static int kirilganSayisi(GameTestHelper y) {
        int n = 0;
        for (int x = 0; x < BOY; x++) {
            for (int z = 0; z < BOY; z++) {
                for (int k = YUZEY + 1; k < YUKSEKLIK; k++) {
                    if (y.getBlockState(new BlockPos(x, k, z)).is(DepremEtiketleri.KIRILGAN)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /** Kalas platform (altı boş) ve 7x7 yaprak kümesi; dört köşede yüzeyde dayanıklı taş. */
    private static void yapilar(GameTestHelper y) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                y.setBlock(new BlockPos(x, YUZEY + 4, z), Blocks.OAK_PLANKS);
            }
        }
        for (int x = 21; x <= 27; x++) {
            for (int z = 21; z <= 27; z++) {
                y.setBlock(new BlockPos(x, YUZEY + 2, z), Blocks.OAK_LEAVES.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
            }
        }
        for (BlockPos k : KOSELER) {
            y.setBlock(k, Blocks.STONE);
        }
    }

    /**
     * Seviye 1: yarık hattının kutudaki her sütununda yüzey boş, hat üzerindeki taş (dayanıklı) da kırılır; kırılgan bloklar
     * azalır; hat dışındaki dayanıklı taş yerinde; deprem doğal biter.
     */
    @GameTest(template = SAHNE, batch = "deprem_yarik", timeoutTicks = 700)
    public static void depremYarikAcarKirilganYikarDayanikliKorur(GameTestHelper y) {
        zemin(y, YUZEY);
        yapilar(y);
        boolean eski = blokHasari(y, true);
        int kirilganOnce = kirilganSayisi(y);
        Deprem deprem = baslat(y, 1);
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        BlockPos koken = y.absolutePos(BlockPos.ZERO);
        y.succeedWhen(() -> {
            y.assertTrue(yonetici.aktifAfet().isEmpty(), "deprem sürüyor");
            FayHatti.Plan plan = deprem.plan();
            y.assertTrue(plan != null && !plan.yarik().isEmpty(), "yarık planı yok");
            int bos = 0;
            int tasKirildi = 0;
            for (FayHatti.Hucre h : plan.yarik()) {
                BlockPos g = new BlockPos(h.x() - koken.getX(), YUZEY, h.z() - koken.getZ());
                if (g.getX() < 0 || g.getX() >= BOY || g.getZ() < 0 || g.getZ() >= BOY) {
                    continue;
                }
                y.assertTrue(y.getBlockState(g).isAir(), "yarık yüzeyi boş değil: " + g);
                bos++;
                for (int k = YUZEY - 2; k > YUZEY - h.derinlik(); k--) {
                    if (y.getBlockState(new BlockPos(g.getX(), k, g.getZ())).isAir()) {
                        tasKirildi++;
                    }
                }
            }
            y.assertTrue(bos >= 16, "kutuda yarık sütunu az: " + bos);
            y.assertTrue(tasKirildi > 0, "yarık hattındaki taş kırılmadı");
            for (BlockPos k : KOSELER) {
                y.assertTrue(y.getBlockState(k).is(Blocks.STONE), "hat dışı dayanıklı taş kırıldı: " + k);
            }
            y.assertTrue(kirilganSayisi(y) < kirilganOnce, "kırılgan blok yıkılmadı (" + kirilganOnce + ")");
            y.assertTrue(deprem.isler().degisenBlok() > 0, "blok değişimi sayılmadı");
            blokHasari(y, eski);
            sahneyiTemizle(y);
        });
    }

    /**
     * Başarım §A.3 #3 gerçek depremle uçtan uca (izleme-inceleme-5 P2-4): doğal biten S1 depremin etki alanındaki oyuncu
     * "Sallanan Zemin"i alır; 600 blok uzaktaki (etki alanı dışı) oyuncu almaz. Blok hasarı kapalı; oyuncular yaratıcı.
     */
    @GameTest(template = SAHNE, batch = "deprem_basarim", timeoutTicks = 600)
    public static void dogalBitenDepremSallananZeminVerir(GameTestHelper y) {
        zemin(y, YUZEY);
        blokHasari(y, false);
        BlockPos merkez = y.absolutePos(MERKEZ);
        ServerPlayer uzak = oyuncu(y, merkez.getX() + 600.5, merkez.getY(), merkez.getZ() + 0.5);
        uzak.setGameMode(GameType.CREATIVE);
        Deprem deprem = baslat(y, 1);
        ServerPlayer yakin = OYUNCULAR.get(OYUNCULAR.size() - 1);
        yakin.setGameMode(GameType.CREATIVE);
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        Advancement sallanan = y.getLevel().getServer().getAdvancements()
                .getAdvancement(BasarimKimlikleri.basarim(Basarimlar.SALLANAN_ZEMIN));
        y.assertTrue(sallanan != null, "advancement yüklü değil (runData?)");
        y.succeedWhen(() -> {
            y.assertTrue(yonetici.aktifAfet().isEmpty() && deprem.sonlandi(), "deprem sürüyor: " + deprem.asama());
            y.assertTrue(deprem.etkilenenler().contains(yakin.getUUID()) && !deprem.etkilenenler().contains(uzak.getUUID()),
                    "etkilenen yalnız alandaki oyuncu olmalı: " + deprem.etkilenenler());
            y.assertTrue(yakin.getAdvancements().getOrStartProgress(sallanan).isDone(),
                    "doğal biten depremden sağ çıkan Sallanan Zemin almalı");
            y.assertTrue(!uzak.getAdvancements().getOrStartProgress(sallanan).isDone(), "alan dışındaki oyuncu almamalı");
            DogalAfetler.LOGGER.info("Deprem GameTest: Sallanan Zemin gerçek depremle verildi (etkilenen {})",
                    deprem.etkilenenler().size());
            sahneyiTemizle(y);
        });
    }

    /** {@code afetBlokHasari=false}: AKTIF boyunca ve sonunda kutunun blok hash'i aynı, değişen blok 0 (GOREV §6 #3). */
    @GameTest(template = SAHNE, batch = "deprem_kapali", timeoutTicks = 500)
    public static void depremBlokHasariKapaliykenDunyaDegismez(GameTestHelper y) {
        zemin(y, YUZEY);
        yapilar(y);
        boolean eski = blokHasari(y, false);
        long once = kutuHash(y);
        Deprem deprem = baslat(y, 1);
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        y.succeedWhen(() -> {
            y.assertTrue(kutuHash(y) == once, "blok hasarı kapalıyken kutu değişti");
            y.assertTrue(yonetici.aktifAfet().isEmpty(), "deprem sürüyor");
            y.assertTrue(deprem.isler().degisenBlok() == 0, "değişen blok " + deprem.isler().degisenBlok());
            blokHasari(y, eski);
            sahneyiTemizle(y);
        });
    }

    /** {@code /dogalafet stop} AKTIF'te: afet, ortam sesi, çatlak örtüleri ve düşen bloklar kalmaz. */
    @GameTest(template = SAHNE, batch = "deprem_stop", timeoutTicks = 300)
    public static void depremStopTemizBirakir(GameTestHelper y) {
        zemin(y, YUZEY);
        yapilar(y);
        boolean eski = blokHasari(y, true);
        Deprem deprem = baslat(y, 1);
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        y.assertTrue(OrtamSesi.aktifMi(Deprem.SES_KATMANI), "AKTIF'te gürleme katmanı yok");
        y.runAfterDelay(12, () -> {
            y.assertTrue(deprem.isler() != null && deprem.isler().degisenBlok() > 0, "yarık açılmaya başlamadı");
            y.assertTrue(yonetici.durdur(), "stop afet bulamadı");
        });
        y.runAfterDelay(14, () -> {
            y.assertTrue(yonetici.aktifAfet().isEmpty(), "stop sonrası afet sürüyor");
            y.assertTrue(!OrtamSesi.aktifMi(Deprem.SES_KATMANI), "gürleme katmanı kaldı");
            y.assertTrue(deprem.isler().ortuSayisi() == 0, "çatlak örtüsü kaldı");
            y.assertTrue(deprem.isler().canliDusenSayisi() == 0, "düşen blok kaldı");
            blokHasari(y, eski);
            sahneyiTemizle(y);
            y.succeed();
        });
    }

    /**
     * DS-14: seviye 3'te ana sarsıntıdan 20–60 sn sonra bir artçı (config 0, 0, 1, 2, 3): sürerken {@code altAsama} 1 ve
     * {@code deprem/artci} katmanı açık; deprem son artçı bitmeden doğal bitmez. Blok hasarı kapalı (S3 yarığı 96 blok,
     * komşu test alanlarına taşmasın): kutunun hash'i ve değişen blok sayısı artçı boyunca da değişmez.
     */
    @GameTest(template = SAHNE, batch = "deprem_artci", timeoutTicks = 2400)
    public static void depremArtciSarsintilariPlanlaBlokYikmaz(GameTestHelper y) {
        zemin(y, YUZEY);
        yapilar(y);
        boolean eski = blokHasari(y, false);
        long once = kutuHash(y);
        List<Integer> artcilar = new ArrayList<>();
        List<Boolean> katmanAcik = new ArrayList<>();
        List<AfetBittiEvent> bitisler = new ArrayList<>();
        Consumer<AfetAsamaDegistiEvent> asama = olay -> {
            if (olay.afet().tur() == AfetTuru.DEPREM && olay.yeniAltAsama() > 0) {
                artcilar.add(olay.yeniAltAsama());
                katmanAcik.add(OrtamSesi.aktifMi(Deprem.ARTCI_KATMANI));
            }
        };
        Consumer<AfetBittiEvent> bitis = olay -> {
            if (olay.tur() == AfetTuru.DEPREM) {
                bitisler.add(olay);
            }
        };
        dinle(AfetAsamaDegistiEvent.class, asama);
        dinle(AfetBittiEvent.class, bitis);
        Deprem deprem = baslat(y, 3);
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        int sure = deprem.seviyeBilgisi().sureTick();
        List<ArtciPlani.Artci> plan = deprem.artcilar();
        y.assertTrue(plan.size() == 1, "S3 artçı sayısı " + plan.size());
        long ilk = plan.get(0).baslangic() - sure;
        y.assertTrue(ilk >= 400 && ilk <= 1200, "ilk artçı ana sarsıntıdan " + ilk + " tick sonra");
        y.assertTrue(deprem.aktifKalanTick() >= plan.get(0).bitis() - 1, "kalan süre artçıyı kapsamalı");
        y.succeedWhen(() -> {
            y.assertTrue(yonetici.aktifAfet().isEmpty(), "deprem sürüyor");
            MinecraftForge.EVENT_BUS.unregister(asama);
            MinecraftForge.EVENT_BUS.unregister(bitis);
            y.assertTrue(artcilar.equals(List.of(1)), "artçı alt aşamaları " + artcilar);
            y.assertTrue(katmanAcik.equals(List.of(true)), "artçı gürleme katmanı açılmadı");
            y.assertTrue(bitisler.size() == 1 && bitisler.get(0).dogalBitti(), "doğal bitiş yok");
            y.assertTrue(!OrtamSesi.aktifMi(Deprem.ARTCI_KATMANI) && !OrtamSesi.aktifMi(Deprem.SES_KATMANI),
                    "ortam katmanı kaldı");
            y.assertTrue(kutuHash(y) == once && deprem.isler().degisenBlok() == 0, "artçılı depremde blok değişti");
            blokHasari(y, eski);
            sahneyiTemizle(y);
        });
    }
}
