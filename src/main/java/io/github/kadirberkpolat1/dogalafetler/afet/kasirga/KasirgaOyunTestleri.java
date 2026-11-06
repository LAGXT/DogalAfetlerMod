package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import com.mojang.authlib.GameProfile;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaEvresi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimKimlikleri;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import java.util.UUID;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Kasırganın oyun içi testleri (bölüm A, KOM.2/KOM.3, başarım #1). Gerçek {@link Kasirga} afeti tek afet yuvasını
 * kullandığı için her test kendi batch'indedir (çekirdek §13). Sahne {@code kasirga_sahne} (32x16x32 hava) kutusuna her test
 * kendisi kurulur ve sonunda temizlenir; kasırga sahnenin ortasına taşınır ({@link Kasirga#konumAyarla}: başka testlerin
 * sahte oyuncusu çevrim içiyse kasırga onun 30-100 blok uzağında doğardı).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KasirgaOyunTestleri {
    private static final String SAHNE = "kasirga_sahne";
    private static final BlockPos MERKEZ = new BlockPos(16, 3, 16);

    private KasirgaOyunTestleri() {
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

    /** Blok hasarı kuralını ayarlar; eski değeri döner (K-E15: test sonunda geri konur). */
    private static boolean blokHasari(GameTestHelper y, boolean deger) {
        GameRules.BooleanValue kural = y.getLevel().getGameRules().getRule(Kurallar.AFET_BLOK_HASARI);
        boolean onceki = kural.get();
        kural.set(deger, y.getLevel().getServer());
        return onceki;
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
        AfetYoneticisi yonetici = yonetici(y);
        yonetici.durdur();
        AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(AfetTuru.KASIRGA, seviye, null, y.getLevel(),
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
}
