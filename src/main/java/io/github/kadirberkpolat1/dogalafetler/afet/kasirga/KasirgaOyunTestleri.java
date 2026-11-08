package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import com.mojang.authlib.GameProfile;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaEvresi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaRotasi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimKimlikleri;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.OyunTestiTemizligi;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Kasırganın oyun içi testleri (bölüm A, KOM.2/KOM.3, başarım #1). Gerçek {@link Kasirga} afeti tek afet yuvasını
 * kullandığı için her test kendi batch'indedir (çekirdek §13). Sahne {@code kasirga_sahne} (32x16x32 hava) kutusuna her test
 * kendisi kurulur ve sonunda temizlenir; kasırga sahnenin ortasına taşınır ({@link Kasirga#konumAyarla}: başka testlerin
 * sahte oyuncusu çevrim içiyse kasırga onun 30-100 blok uzağında doğardı). Kurulumda düşen test de batch sonunda temizlenir
 * ({@link OyunTestiTemizligi#batchSonu}: afet durur, {@code afetBlokHasari} geri; K-E15, GP18).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KasirgaOyunTestleri {
    private static final String SAHNE = "kasirga_sahne";
    private static final BlockPos MERKEZ = new BlockPos(16, 3, 16);
    /** {@code rotaOyuncuyaKilitlenmez}: oyuncunun çevresinde bu kadar blok (chunk'a yuvarlanır) test boyunca forceload. */
    private static final int ROTA_YUKLU = 64;
    /** Rota testinin forceload ettikleri; test sonunda ya da batch sonunda bırakılır. */
    private static final List<ChunkPos> ZORLANAN = new ArrayList<>();
    /** {@code rotaOyuncuyaKilitlenmez}: kasırganın yürüdüğü tick (~10 sn). */
    private static final int ROTA_TICK = 200;

    private KasirgaOyunTestleri() {
    }

    @AfterBatch(batch = "kasirga_blok")
    public static void blokSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "kasirga_stop")
    public static void stopSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "kasirga_kural")
    public static void kuralSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "kasirga_cekim")
    public static void cekimSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "kasirga_cekim_bolgesi")
    public static void cekimBolgesiSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "kasirga_dogal")
    public static void dogalSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "kasirga_hareket")
    public static void hareketSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "kasirga_kayit")
    public static void kayitSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "kasirga_isleme")
    public static void islemeSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "kasirga_devrik")
    public static void devrikSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "kasirga_hafif")
    public static void hafifSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "kasirga_rota")
    public static void rotaSonrasi(ServerLevel level) {
        chunklariBirak(level);
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "kasirga_rota_kayit")
    public static void rotaKayitSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    private static void chunklariBirak(ServerLevel level) {
        for (ChunkPos c : ZORLANAN) {
            level.setChunkForced(c.x, c.z, false);
        }
        ZORLANAN.clear();
    }

    private static void dogrula(GameTestHelper y, boolean kosul, String mesaj) {
        if (!kosul) {
            y.fail(mesaj);
        }
    }

    /** y 1 taş, y 2 kalıcı meşe yaprağı (S1-S3 kaldırır); {@code yaprak=false} ise yalnız taş. */
    private static void zemin(GameTestHelper y, boolean yaprak) {
        BlockState ust = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                y.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                if (yaprak) {
                    y.setBlock(new BlockPos(x, 2, z), ust);
                }
            }
        }
    }

    /**
     * Küçük meşe: gövde {@code (x, 2..6, z)} beş dikey kütük; y 5-6'da 5x5, y 7'de 3x3 kalıcı yaprak (gövdenin tepesi
     * yaprakla örtülü). Döner: gövde konumları (göreceli, aşağıdan yukarı).
     */
    private static BlockPos[] agac(GameTestHelper y, int x, int z) {
        BlockState yaprak = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int k = 5; k <= 7; k++) {
            int r = k == 7 ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    y.setBlock(new BlockPos(x + dx, k, z + dz), yaprak);
                }
            }
        }
        BlockPos[] govde = new BlockPos[5];
        for (int i = 0; i < govde.length; i++) {
            govde[i] = new BlockPos(x, 2 + i, z);
            y.setBlock(govde[i], Blocks.OAK_LOG);
        }
        return govde;
    }

    private static boolean dikeyMeseKutugu(ServerLevel level, BlockPos mutlak) {
        BlockState s = level.getBlockState(mutlak);
        return s.is(Blocks.OAK_LOG) && s.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y;
    }

    private static AABB kutu(GameTestHelper y) {
        return new AABB(y.absolutePos(BlockPos.ZERO), y.absolutePos(new BlockPos(32, 16, 32)));
    }

    private static void sahneyiTemizle(GameTestHelper y) {
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                for (int k = 0; k < 16; k++) {
                    y.setBlock(new BlockPos(x, k, z), Blocks.AIR);
                }
            }
        }
        for (ItemEntity e : y.getLevel().getEntitiesOfClass(ItemEntity.class, kutu(y).inflate(48))) {
            e.discard();
        }
    }

    /** Blok hasarı kuralını ayarlar; eski değeri döner (K-E15: test sonunda, başarısızlıkta batch sonunda geri konur). */
    private static boolean blokHasari(GameTestHelper y, boolean deger) {
        return OyunTestiTemizligi.blokHasari(y.getLevel(), deger);
    }

    private static AfetYoneticisi yonetici(GameTestHelper y) {
        return AfetYoneticisi.al(y.getLevel().getServer());
    }

    /** Test sonu: afet durur, kural eski değerine döner, sahne boşalır. */
    private static void bitir(GameTestHelper y, boolean oncekiKural) {
        yonetici(y).durdur();
        blokHasari(y, oncekiKural);
        sahneyiTemizle(y);
    }

    /** Seviye {@code seviye} kasırgayı başlatır, sahnenin ortasına taşır ve AKTIF'in {@code evre} evresine getirir. */
    private static Kasirga baslat(GameTestHelper y, int seviye, boolean hareket, KasirgaEvresi evre) {
        return baslat(y, seviye, hareket, evre, null);
    }

    /** {@code hedef} null değilse kasırga onu hedef alarak başlar (doğuş noktası), sonra yine sahnenin ortasına taşınır. */
    private static Kasirga baslat(GameTestHelper y, int seviye, boolean hareket, KasirgaEvresi evre, ServerPlayer hedef) {
        AfetYoneticisi yonetici = yonetici(y);
        yonetici.durdur();
        AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(AfetTuru.KASIRGA, seviye, hedef, y.getLevel(),
                y.absolutePos(MERKEZ));
        dogrula(y, sonuc == AfetYoneticisi.BaslatmaSonucu.BASLADI, "kasırga başlamadı: " + sonuc);
        if (!(yonetici.aktifAfet().orElse(null) instanceof Kasirga kasirga)) {
            throw new IllegalStateException("aktif afet Kasirga değil");
        }
        kasirga.konumAyarla(Vec3.atBottomCenterOf(y.absolutePos(MERKEZ)));
        if (!hareket) {
            kasirga.hareketiKapat();
        }
        yonetici.asamaIleri();
        dogrula(y, kasirga.asama() == AfetAsamasi.SIREN, "siren aşamasına geçmedi");
        yonetici.asamaIleri();
        dogrula(y, kasirga.asama() == AfetAsamasi.AKTIF, "aktif aşamaya geçmedi");
        while (kasirga.durum().evre().ordinal() < evre.ordinal() && kasirga.asama() == AfetAsamasi.AKTIF) {
            yonetici.asamaIleri();
        }
        dogrula(y, kasirga.durum().evre() == evre, "evre " + evre + " yerine " + kasirga.durum().evre());
        return kasirga;
    }

    private static int varlikSayisi(GameTestHelper y, EntityType<?> tur) {
        return y.getLevel().getEntities(tur, kutu(y).inflate(128), e -> true).size();
    }

    @GameTest(template = SAHNE, batch = "kasirga_blok", timeoutTicks = 40)
    public static void kaldirilabilirBloklarSeviyeyeVeEtiketeGore(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        KasirgaSeviyesi[] seviyeler = {KasirgaSeviyesi.icin(1), KasirgaSeviyesi.icin(2), KasirgaSeviyesi.icin(5)};
        Object[][] durumlar = {
            {Blocks.OAK_LEAVES, true, true, true},
            {Blocks.GRASS_BLOCK, false, false, true},
            {Blocks.STONE, false, false, true},
            {Blocks.OAK_PLANKS, false, true, true},
            {Blocks.OBSIDIAN, false, false, false},
            {Blocks.DEEPSLATE, false, false, false},
            {Blocks.DEEPSLATE_DIAMOND_ORE, false, false, false},
            {Blocks.DEEPSLATE_BRICK_STAIRS, false, false, false},
            {Blocks.BEDROCK, false, false, false},
            {Blocks.CHEST, false, false, false},
        };
        try {
            for (int i = 0; i < durumlar.length; i++) {
                BlockPos yer = new BlockPos(2 + 2 * i, 2, 2);
                y.setBlock(yer, (Block) durumlar[i][0]);
                BlockPos mutlak = y.absolutePos(yer);
                BlockState blok = level.getBlockState(mutlak);
                for (int k = 0; k < seviyeler.length; k++) {
                    boolean beklenen = (Boolean) durumlar[i][k + 1];
                    dogrula(y, Kasirga.kaldirilabilir(level, mutlak, blok, seviyeler[k]) == beklenen,
                            blok.getBlock() + " S" + seviyeler[k].seviye() + " kaldırılabilir " + !beklenen + " çıktı");
                }
            }
            BlockPos uzun = new BlockPos(2, 2, 6);
            y.setBlock(uzun, Blocks.TALL_GRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                    DoubleBlockHalf.LOWER));
            BlockPos mutlak = y.absolutePos(uzun);
            dogrula(y, !Kasirga.kaldirilabilir(level, mutlak, level.getBlockState(mutlak), seviyeler[2]),
                    "çift blok (uzun ot) kaldırılmamalı");
        } finally {
            sahneyiTemizle(y);
        }
        y.succeed();
    }

    /** KOM.3: stop → kasırga ve uçan blok varlığı kalmaz, ortam sesleri durur, havadaki bloklar yere konur. */
    @GameTest(template = SAHNE, batch = "kasirga_stop", timeoutTicks = 200)
    public static void stopTemizBiterVarlikVeSesKalmaz(GameTestHelper y) {
        zemin(y, true);
        boolean onceki = blokHasari(y, true);
        Kasirga kasirga = baslat(y, 2, false, KasirgaEvresi.OLGUN);
        y.runAfterDelay(50, () -> {
            try {
                dogrula(y, OrtamSesi.aktifMi(Kasirga.UGULTU_KIMLIK) && OrtamSesi.aktifMi(Kasirga.RUZGAR_KIMLIK),
                        "ortam sesi katmanları başlamadı");
                dogrula(y, varlikSayisi(y, KasirgaKayitlari.KASIRGA.get()) == 1, "kasırga varlığı yok");
                dogrula(y, kasirga.ucanBlokSayisi() > 0 && kasirga.toplamKaldirilan() > 0,
                        "S2 yaprak kaldırmadı: uçan " + kasirga.ucanBlokSayisi() + ", kaldırılan " + kasirga.toplamKaldirilan());
                dogrula(y, kasirga.ucanBlokSayisi() <= KasirgaAyarlari.ucanBlokTavani(), "uçan blok tavanı aşıldı");
                yonetici(y).durdur();
                dogrula(y, yonetici(y).aktifAfet().isEmpty(), "afet durmadı");
                dogrula(y, varlikSayisi(y, KasirgaKayitlari.KASIRGA.get()) == 0, "stop sonrası kasırga varlığı kaldı");
                dogrula(y, varlikSayisi(y, KasirgaKayitlari.UCAN_BLOK.get()) == 0, "stop sonrası uçan blok kaldı");
                dogrula(y, !OrtamSesi.aktifMi(Kasirga.UGULTU_KIMLIK) && !OrtamSesi.aktifMi(Kasirga.RUZGAR_KIMLIK),
                        "stop sonrası ortam sesi sürüyor");
            } finally {
                bitir(y, onceki);
            }
            y.succeed();
        });
    }

    /**
     * Kn4: varlık işlemeyen (yüklü ama biletsiz, K-E21 gibi eşzamanlı üretilmiş) uzak chunk'taki uçan blok kasırganın tick'inde
     * yere konur ve varlık kalkar; blok kaybolmaz.
     */
    @GameTest(template = SAHNE, batch = "kasirga_isleme", timeoutTicks = 40)
    public static void isleyemeyenKonumdakiUcanBlokYereKonur(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        boolean onceki = blokHasari(y, true);
        Kasirga kasirga = baslat(y, 2, false, KasirgaEvresi.DOGUS);
        BlockPos uzak = y.absolutePos(MERKEZ).offset(4096, 0, 4096);
        BlockPos[] konan = new BlockPos[1];
        try {
            level.getChunk(uzak);
            dogrula(y, !level.isPositionEntityTicking(uzak), "uzak chunk varlık işliyor (sahne geçersiz)");
            UcanBlokVarligi ucan = UcanBlokVarligi.olustur(level, Blocks.OAK_PLANKS.defaultBlockState(), kasirga, -1, 0, 0, 0);
            ucan.setPos(uzak.getX() + 0.5, uzak.getY() + 20, uzak.getZ() + 0.5);
            level.addFreshEntity(ucan);
            kasirga.ucanEkle(ucan);
            kasirga.isleyemeyenUcanlariBirak(level);
            int zemin = level.getHeight(Heightmap.Types.MOTION_BLOCKING, uzak.getX(), uzak.getZ()) - 1;
            konan[0] = new BlockPos(uzak.getX(), zemin, uzak.getZ());
            dogrula(y, ucan.isRemoved(), "işlemeyen konumdaki uçan blok varlığı kalkmadı");
            dogrula(y, level.getBlockState(konan[0]).is(Blocks.OAK_PLANKS),
                    "blok yere konmadı: " + konan[0].toShortString() + " = " + level.getBlockState(konan[0]));
            dogrula(y, kasirga.ucanBlokSayisi() == 0, "kasırga listesinde uçan blok kaldı");
        } finally {
            if (konan[0] != null && level.getBlockState(konan[0]).is(Blocks.OAK_PLANKS)) {
                level.setBlock(konan[0], Blocks.AIR.defaultBlockState(), 2);
            }
            bitir(y, onceki);
        }
        y.succeed();
    }

    /** GOREV §6 #3: {@code afetBlokHasari=false} iken kasırga hiçbir bloğu değiştirmez. */
    @GameTest(template = SAHNE, batch = "kasirga_kural", timeoutTicks = 200)
    public static void blokHasariKapaliykenBlokDegismez(GameTestHelper y) {
        zemin(y, true);
        boolean onceki = blokHasari(y, false);
        BlockPos a = y.absolutePos(BlockPos.ZERO);
        BlockPos b = y.absolutePos(new BlockPos(31, 15, 31));
        long once = SanalSu.bolgeHash(y.getLevel(), a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
        Kasirga kasirga = baslat(y, 3, false, KasirgaEvresi.OLGUN);
        y.runAfterDelay(60, () -> {
            try {
                long sonra = SanalSu.bolgeHash(y.getLevel(), a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
                dogrula(y, kasirga.toplamKaldirilan() == 0 && kasirga.ucanBlokSayisi() == 0, "kural kapalıyken blok kalktı");
                dogrula(y, once == sonra, "kural kapalıyken sahne değişti");
            } finally {
                bitir(y, onceki);
            }
            y.succeed();
        });
    }

    /**
     * S4 sütun taraması ağacın tepe yaprağını alıp gövdeye iner ve gövdeyi devirir; uçan blok tavanı doluyken kütükler
     * rüzgârın teğet yönüne yatay konur (K-05 devrik kütük). Yatırılan kütük parça bütçesinden düştüğü için (S4 3/tick)
     * beş kat gövde iki sütun adımında devrilir. {@code afetBlokHasari=false} iken aynı adım hiçbir bloğu değiştirmez.
     * Doğuşun ilk yarısında evre payı 0 olduğu için kasırganın kendi tick'i blok kaldırmaz; adım testten sürülür.
     */
    @GameTest(template = SAHNE, batch = "kasirga_devrik", timeoutTicks = 40)
    public static void s4AgacGovdesiniDevirirKuralKapaliykenDokunmaz(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        zemin(y, false);
        BlockPos[] govde = agac(y, MERKEZ.getX() + 4, MERKEZ.getZ());
        BlockPos a = y.absolutePos(BlockPos.ZERO);
        BlockPos b = y.absolutePos(new BlockPos(31, 15, 31));
        boolean onceki = blokHasari(y, false);
        long once = SanalSu.bolgeHash(level, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
        Kasirga kasirga = baslat(y, 4, false, KasirgaEvresi.DOGUS);
        BlockPos sutun = y.absolutePos(govde[0]);
        try {
            dogrula(y, kasirga.sutunuIsleTest(level, sutun.getX(), sutun.getZ(), false) == 0, "kural kapalıyken sütun işlendi");
            dogrula(y, once == SanalSu.bolgeHash(level, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ()),
                    "kural kapalıyken sahne değişti");
            blokHasari(y, true);
            int kalkan = kasirga.sutunuIsleTest(level, sutun.getX(), sutun.getZ(), false)
                    + kasirga.sutunuIsleTest(level, sutun.getX(), sutun.getZ(), false);
            for (BlockPos p : govde) {
                dogrula(y, !dikeyMeseKutugu(level, y.absolutePos(p)), "gövde kütüğü yerinde: " + p.toShortString());
            }
            int devrik = 0;
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    BlockState s = level.getBlockState(y.absolutePos(new BlockPos(x, 2, z)));
                    if (s.is(Blocks.OAK_LOG) && s.getValue(BlockStateProperties.AXIS) != Direction.Axis.Y) {
                        devrik++;
                    }
                }
            }
            dogrula(y, devrik == govde.length, "zeminde devrik kütük " + devrik + ", beklenen " + govde.length + " (kalkan "
                    + kalkan + ")");
        } finally {
            bitir(y, onceki);
        }
        y.succeed();
    }

    /** S1 hafif kalır: ağacın tepe yaprağını alır ama tekrarlanan sütun adımlarında gövdeye dokunmaz. */
    @GameTest(template = SAHNE, batch = "kasirga_hafif", timeoutTicks = 40)
    public static void s1AgacGovdesineDokunmaz(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        zemin(y, false);
        BlockPos[] govde = agac(y, MERKEZ.getX() + 2, MERKEZ.getZ());
        boolean onceki = blokHasari(y, true);
        Kasirga kasirga = baslat(y, 1, false, KasirgaEvresi.DOGUS);
        BlockPos sutun = y.absolutePos(govde[0]);
        try {
            int kalkan = 0;
            for (int i = 0; i < 8; i++) {
                kalkan += kasirga.sutunuIsleTest(level, sutun.getX(), sutun.getZ(), false);
            }
            dogrula(y, kalkan == 1, "S1 gövde sütunundan " + kalkan + " blok aldı (yalnız tepe yaprağı beklenir)");
            for (BlockPos p : govde) {
                dogrula(y, dikeyMeseKutugu(level, y.absolutePos(p)), "S1 gövde kütüğünü aldı: " + p.toShortString());
            }
        } finally {
            bitir(y, onceki);
        }
        y.succeed();
    }

    /** Bölüm A.3: mob huni çekirdeğinde havaya kalkar ve hasar alır. */
    @GameTest(template = SAHNE, batch = "kasirga_cekim", timeoutTicks = 200)
    public static void cekirdektekiMobHavayaKalkarVeHasarAlir(GameTestHelper y) {
        zemin(y, false);
        boolean onceki = blokHasari(y, false);
        Pig domuz = y.spawn(EntityType.PIG, new BlockPos(18, 2, 16));
        double baslangicY = domuz.getY();
        double[] enYuksek = {baslangicY};
        y.onEachTick(() -> enYuksek[0] = Math.max(enYuksek[0], domuz.getY()));
        baslat(y, 3, false, KasirgaEvresi.OLGUN);
        y.runAfterDelay(60, () -> {
            try {
                dogrula(y, enYuksek[0] >= baslangicY + 2, "domuz kalkmadı: en yüksek +" + (enYuksek[0] - baslangicY));
                dogrula(y, !domuz.isAlive() || domuz.getHealth() < domuz.getMaxHealth(), "domuz hasar almadı");
            } finally {
                domuz.discard();
                bitir(y, onceki);
            }
            y.succeed();
        });
    }

    /**
     * Tur 3 (AF3-01, AF3-02): çekim bölgesindeki mob (S2, merkezden 14 blok) çekirdeğe çekilir ve çekirdekten dışarı
     * savrulmaz. Domuz hasarsızdır (çekirdek hasarıyla panikleyip koşmasın); yapay zekâsı açık kalır, çünkü NoAI mob
     * ({@code isEffectiveAi} false) hiç {@code travel} etmez ve hızı onu taşımaz. Aynı bölgedeki hayatta kalma oyuncusunun hızına
     * sunucu dokunmaz: oyuncunun çekimi kendi istemcisinde uygulanır (sahte oyuncunun istemcisi yok, yatay hızı sıfır kalır;
     * eski kod iki tick'te bir yazardı).
     */
    @GameTest(template = SAHNE, batch = "kasirga_cekim_bolgesi", timeoutTicks = 200)
    public static void cekimBolgesindekiMobCekirdegeCekilirSavrulmaz(GameTestHelper y) {
        zemin(y, false);
        boolean onceki = blokHasari(y, false);
        Pig domuz = y.spawn(EntityType.PIG, new BlockPos(30, 2, 16));
        domuz.setInvulnerable(true);
        BlockPos m = y.absolutePos(MERKEZ);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), "kasirga-cekim"),
                m.getX() - 10.5, m.getY() - 1, m.getZ() + 0.5);
        oyuncu.setGameMode(GameType.SURVIVAL);
        Kasirga kasirga = baslat(y, 2, false, KasirgaEvresi.OLGUN);
        double cekirdek = kasirga.seviyeBilgisi().tabanYaricapi();
        double baslangic = Math.hypot(domuz.getX() - kasirga.konum().x, domuz.getZ() - kasirga.konum().z);
        int[] giris = {-1};
        int[] tick = {0};
        double[] enUzak = {0};
        double[] oyuncuHizi = {0};
        y.onEachTick(() -> {
            tick[0]++;
            double d = Math.hypot(domuz.getX() - kasirga.konum().x, domuz.getZ() - kasirga.konum().z);
            if (giris[0] < 0 && d < cekirdek) {
                giris[0] = tick[0];
            }
            if (giris[0] >= 0) {
                enUzak[0] = Math.max(enUzak[0], d);
            }
            Vec3 v = oyuncu.getDeltaMovement();
            oyuncuHizi[0] = Math.max(oyuncuHizi[0], Math.hypot(v.x, v.z));
        });
        y.runAfterDelay(100, () -> {
            try {
                double son = Math.hypot(domuz.getX() - kasirga.konum().x, domuz.getZ() - kasirga.konum().z);
                dogrula(y, baslangic > cekirdek + 4, "domuz çekim bölgesinde başlamalı: " + baslangic);
                dogrula(y, giris[0] >= 0 && giris[0] <= 80, "domuz 80 tick'te çekirdeğe girmedi (giriş " + giris[0] + ", son "
                        + son + ")");
                dogrula(y, enUzak[0] < cekirdek + 0.5, "domuz çekirdekten savruldu: en uzak " + enUzak[0] + " / " + cekirdek);
                dogrula(y, son < cekirdek, "domuz çekirdekte kalmadı: " + son);
                dogrula(y, oyuncuHizi[0] < 1e-9, "sunucu oyuncunun yatay hızını yazdı: " + oyuncuHizi[0]);
            } finally {
                domuz.discard();
                y.getLevel().getServer().getPlayerList().remove(oyuncu);
                bitir(y, onceki);
            }
            y.succeed();
        });
    }

    /** Başarım #1 (GOREV §6 #16): gerçek kasırga doğal bitince etki alanındaki oyuncu İlk Rüzgâr'ı alır. */
    @GameTest(template = SAHNE, batch = "kasirga_dogal", timeoutTicks = 200)
    public static void dogalBitenKasirgaIlkRuzgariVerir(GameTestHelper y) {
        zemin(y, false);
        boolean onceki = blokHasari(y, false);
        Kasirga kasirga = baslat(y, 1, false, KasirgaEvresi.DOGUS);
        BlockPos m = y.absolutePos(MERKEZ);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), "kasirga-test"),
                m.getX() + 6.5, m.getY(), m.getZ() + 0.5);
        oyuncu.setGameMode(GameType.CREATIVE);
        y.runAfterDelay(45, () -> {
            try {
                dogrula(y, kasirga.etkilenenler().contains(oyuncu.getUUID()), "oyuncu etkilenenlere girmedi");
                for (int i = 0; i < 8 && yonetici(y).aktifAfet().isPresent(); i++) {
                    yonetici(y).asamaIleri();
                }
                dogrula(y, yonetici(y).aktifAfet().isEmpty(), "kasırga doğal bitmedi");
                dogrula(y, varlikSayisi(y, KasirgaKayitlari.KASIRGA.get()) == 0, "doğal bitişte kasırga varlığı kaldı");
                Advancement ilk = y.getLevel().getServer().getAdvancements()
                        .getAdvancement(BasarimKimlikleri.basarim(Basarimlar.ILK_RUZGAR));
                dogrula(y, ilk != null, "ilk_ruzgar advancement'ı yüklenmedi (runData?)");
                dogrula(y, oyuncu.getAdvancements().getOrStartProgress(ilk).isDone(), "İlk Rüzgâr verilmedi");
            } finally {
                y.getLevel().getServer().getPlayerList().remove(oyuncu);
                bitir(y, onceki);
            }
            y.succeed();
        });
    }

    /** Radar §3: hareketli kasırganın etki alanı güncel merkezi döner; görüntü varlığı onu izler. */
    @GameTest(template = SAHNE, batch = "kasirga_hareket", timeoutTicks = 200)
    public static void etkiAlaniHareketliMerkeziIzler(GameTestHelper y) {
        zemin(y, false);
        boolean onceki = blokHasari(y, false);
        Kasirga kasirga = baslat(y, 1, true, KasirgaEvresi.OLGUN);
        EtkiAlani ilk = yonetici(y).etkiAlani(kasirga).orElseThrow();
        y.runAfterDelay(60, () -> {
            try {
                EtkiAlani son = yonetici(y).etkiAlani(kasirga).orElseThrow();
                double yol = Math.hypot(son.x() - ilk.x(), son.z() - ilk.z());
                dogrula(y, yol >= 3, "etki alanı merkezi ilerlemedi: " + yol);
                KasirgaVarligi varlik = kasirga.varlik();
                dogrula(y, varlik != null && varlik.position().distanceTo(kasirga.konum()) < 1,
                        "görüntü varlığı konumu izlemiyor");
                dogrula(y, son.yaricap() == kasirga.seviyeBilgisi().cekimYaricapi(), "etki yarıçapı çekim yarıçapı değil");
            } finally {
                bitir(y, onceki);
            }
            y.succeed();
        });
    }

    /** Sunucu yeniden açılınca devam: kasırganın konumu, yönü ve AKTIF'te geçen süresi afetin NBT'sinden geri gelir. */
    @GameTest(template = SAHNE, batch = "kasirga_kayit", timeoutTicks = 100)
    public static void kayittanOkununcaKonumVeSureKorunur(GameTestHelper y) {
        zemin(y, false);
        boolean onceki = blokHasari(y, false);
        Kasirga kasirga = baslat(y, 2, true, KasirgaEvresi.OLGUN);
        y.runAfterDelay(20, () -> {
            try {
                CompoundTag kayit = kasirga.durumYaz(new CompoundTag());
                Kasirga okunan = new Kasirga();
                okunan.durumOku(kayit);
                dogrula(y, okunan.konum().distanceTo(kasirga.konum()) < 1e-6, "konum korunmadı");
                dogrula(y, okunan.aktifGecen() == kasirga.aktifGecen(), "AKTIF süresi korunmadı");
                dogrula(y, okunan.yon() == kasirga.yon(), "yön korunmadı");
                dogrula(y, okunan.asama() == AfetAsamasi.AKTIF && okunan.aktifKalanTick() == kasirga.aktifKalanTick(),
                        "aşama/kalan süre korunmadı");
                EtkiAlani a = okunan.etkiAlani();
                dogrula(y, Math.abs(a.x() - kasirga.konum().x) < 1e-6 && Math.abs(a.z() - kasirga.konum().z) < 1e-6,
                        "okunan etki alanı güncel merkezde değil");
            } finally {
                bitir(y, onceki);
            }
            y.succeed();
        });
    }

    /**
     * AF2-20: kasırga oyuncuyu izlemez. Oyuncu çevresi ±{@value #ROTA_YUKLU} blok forceload edilir; chunk'lar entity-ticking
     * olunca oyuncuyu hedef alan S1 kasırga başlar, oyuncunun 40 blok doğusuna konur ve {@value #ROTA_TICK} tick hareketli
     * yürür. Her tick: ara hedef oyuncuya en az {@link KasirgaRotasi#OYUNCUYA_EN_AZ} blok uzakta, kasırga yalnız entity-ticking
     * konuma ilerler; sonda en az bir ara hedef seçilmiş ve kasırga yürümüş olmalı. Oyuncu yaratıcı kipte (çekilmez).
     */
    @GameTest(template = SAHNE, batch = "kasirga_rota", timeoutTicks = 800)
    public static void rotaOyuncuyaKilitlenmez(GameTestHelper y) {
        zemin(y, false);
        boolean onceki = blokHasari(y, false);
        ServerLevel level = y.getLevel();
        BlockPos m = y.absolutePos(MERKEZ);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), "kasirga-rota"),
                m.getX() + 0.5, m.getY(), m.getZ() + 0.5);
        oyuncu.setGameMode(GameType.CREATIVE);
        List<ChunkPos> alan = new ArrayList<>();
        for (int cx = (m.getX() - ROTA_YUKLU) >> 4; cx <= (m.getX() + ROTA_YUKLU) >> 4; cx++) {
            for (int cz = (m.getZ() - ROTA_YUKLU) >> 4; cz <= (m.getZ() + ROTA_YUKLU) >> 4; cz++) {
                alan.add(new ChunkPos(cx, cz));
                if (level.setChunkForced(cx, cz, true)) {
                    ZORLANAN.add(new ChunkPos(cx, cz));
                }
            }
        }
        Kasirga[] kasirga = {null};
        Vec3[] son = {null};
        double[] yol = {0};
        int[] gecen = {0};
        boolean[] bitti = {false};
        y.onEachTick(() -> {
            if (bitti[0]) {
                return;
            }
            if (kasirga[0] == null) {
                for (ChunkPos c : alan) {
                    if (!level.isPositionEntityTicking(new BlockPos(c.getMinBlockX(), m.getY(), c.getMinBlockZ()))) {
                        return;
                    }
                }
                kasirga[0] = baslat(y, 1, true, KasirgaEvresi.OLGUN, oyuncu);
                kasirga[0].konumAyarla(Vec3.atBottomCenterOf(m.east(40)));
                son[0] = kasirga[0].konum();
                return;
            }
            Kasirga k = kasirga[0];
            try {
                double[] ara = k.araHedef();
                if (k.araHedefNo() > 0 && ara != null) {
                    double d = Math.hypot(ara[0] - oyuncu.getX(), ara[1] - oyuncu.getZ());
                    dogrula(y, d >= KasirgaRotasi.OYUNCUYA_EN_AZ, "ara hedef #" + k.araHedefNo() + " oyuncuya " + Math.round(d)
                            + " blok: (" + Math.round(ara[0]) + ", " + Math.round(ara[1]) + ")");
                }
                Vec3 konum = k.konum();
                if (!konum.equals(son[0])) {
                    yol[0] += Math.hypot(konum.x - son[0].x, konum.z - son[0].z);
                    dogrula(y, level.isPositionEntityTicking(BlockPos.containing(konum)),
                            "kasırga entity-ticking olmayan konuma ilerledi: " + BlockPos.containing(konum).toShortString());
                    son[0] = konum;
                }
                if (++gecen[0] < ROTA_TICK) {
                    return;
                }
                bitti[0] = true;
                dogrula(y, k.araHedefNo() >= 1, "ara hedef seçilmedi");
                dogrula(y, yol[0] >= 3, "kasırga yürümedi: " + yol[0]);
            } catch (RuntimeException hata) {
                bitti[0] = true;
                throw hata;
            } finally {
                if (bitti[0]) {
                    level.getServer().getPlayerList().remove(oyuncu);
                    bitir(y, onceki);
                    chunklariBirak(level);
                }
            }
            y.succeed();
        });
    }

    /**
     * AF2-20 kayıttan devam: hareketli kasırganın NBT'si ({@code ozel.rota}) yeni bir {@link Kasirga}'ya okununca ara hedef, tohum,
     * seçim sayacı ve zamanlayıcı aynı; aynı girdilerle bir sonraki seçim de aynı ara hedefi verir.
     */
    @GameTest(template = SAHNE, batch = "kasirga_rota_kayit", timeoutTicks = 100)
    public static void rotaKayittanDevamEder(GameTestHelper y) {
        zemin(y, false);
        boolean onceki = blokHasari(y, false);
        Kasirga kasirga = baslat(y, 2, true, KasirgaEvresi.OLGUN);
        y.runAfterDelay(20, () -> {
            try {
                KasirgaRotasi rota = kasirga.rota();
                dogrula(y, rota != null && rota.sayac() >= 1, "rota ya da ara hedef yok");
                CompoundTag kayit = kasirga.durumYaz(new CompoundTag());
                dogrula(y, kayit.getCompound("ozel").contains("rota"), "NBT'de rota yok");
                Kasirga okunan = new Kasirga();
                okunan.durumOku(kayit);
                KasirgaRotasi geri = okunan.rota();
                dogrula(y, geri != null && geri.yaz().equals(rota.yaz()), "rota durumu korunmadı: " + rota.yaz() + " / "
                        + (geri == null ? null : geri.yaz()));
                dogrula(y, okunan.araHedefNo() == kasirga.araHedefNo() && geri.tohum() == rota.tohum(),
                        "ara hedef no ya da tohum korunmadı");
                double[] a = kasirga.araHedef();
                double[] b = okunan.araHedef();
                dogrula(y, a[0] == b[0] && a[1] == b[1], "ara hedef korunmadı");
                List<double[]> oyuncular = List.of(new double[] {a[0] + 50, a[1]});
                Vec3 k = kasirga.konum();
                KasirgaRotasi.Secim s1 = KasirgaRotasi.oku(rota.yaz()).yeniAraHedef(k.x, k.z, oyuncular, (x, z) -> true);
                KasirgaRotasi.Secim s2 = KasirgaRotasi.oku(geri.yaz()).yeniAraHedef(k.x, k.z, oyuncular, (x, z) -> true);
                dogrula(y, s1.equals(s2), "sonraki seçim farklı: " + s1 + " / " + s2);
                dogrula(y, s1.no() == rota.sayac() + 1, "sonraki seçim numarası " + s1.no());
            } finally {
                bitir(y, onceki);
            }
            y.succeed();
        });
    }
}
