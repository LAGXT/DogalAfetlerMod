package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikcilikKayitlari.Balik;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.DelikOmru;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.IsirmaSuresi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaBloklari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKapisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.VucutSicakligi;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Buz altı balıkçılığın oyun testleri (madde 15, GOREV \u00a76 #3 "buz deliği açılır ve yeniden donar"). Düz GameTest
 * dünyasında sahne testin içinde kurulur: 3x3 havuz (y 2–3 su, y 4 buz) taş duvarla çevrili, 5x5 (x, z 0..4); deliğin
 * yeri ortadaki (2, 4, 2). Global donma ilerlemesini değiştiren testler kendi batch'lerindedir; eşik altı ve chunk
 * sınırı testleri ayrı batch'te tek başınadır. Her test kendi sahnesini sonda havaya çevirir.
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BalikcilikOyunTestleri {
    private static final String BATCH = "balikcilik";
    private static final BlockPos DELIK = new BlockPos(2, 4, 2);
    private static final Field ISIRMA = ObfuscationReflectionHelper.findField(FishingHook.class, "f_37089_");

    private BalikcilikOyunTestleri() {
    }

    /** Global donma ilerlemesi; kızıl cüce dönemi kapalı (çözülme dönemi yok, evre kızıl cüce değil). */
    private static void donma(GameTestHelper y, float g) {
        DunyaDonemi d = DunyaDonemi.al(y.getLevel().getServer());
        if (d.gunesEvresi() == GunesEvresi.KIZIL_CUCE) {
            d.gunesEvresiAyarla(GunesEvresi.GUNESSIZ);
        }
        d.cozulmeDonemiAyarla(false);
        d.donmaIlerlemesiAyarla(g);
    }

    private static void havuzKur(GameTestHelper y) {
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                boolean ic = x >= 1 && x <= 3 && z >= 1 && z <= 3;
                y.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                y.setBlock(new BlockPos(x, 2, z), ic ? Blocks.WATER : Blocks.STONE);
                y.setBlock(new BlockPos(x, 3, z), ic ? Blocks.WATER : Blocks.STONE);
                y.setBlock(new BlockPos(x, 4, z), ic ? Blocks.ICE : Blocks.STONE);
                for (int yy = 5; yy <= 7; yy++) {
                    y.setBlock(new BlockPos(x, yy, z), Blocks.AIR);
                }
            }
        }
    }

    private static void temizle(GameTestHelper y, ServerPlayer... oyuncular) {
        for (ServerPlayer o : oyuncular) {
            if (o.fishing != null) {
                o.fishing.discard();
            }
            y.getLevel().getServer().getPlayerList().remove(o);
        }
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                for (int yy = 7; yy >= 1; yy--) {
                    y.setBlock(new BlockPos(x, yy, z), Blocks.AIR);
                }
            }
        }
    }

    /** Buz üstünde (havuzun içinde, deliğin yanında) hayatta kalma modunda sahte oyuncu. */
    private static ServerPlayer oyuncu(GameTestHelper y, int x, int z) {
        BlockPos ayak = y.absolutePos(new BlockPos(x, 5, z));
        ServerPlayer o = CekirdekOyunTestleri.sahteOyuncu(y, ayak.getX() + 0.5, ayak.getY(), ayak.getZ() + 0.5);
        o.setGameMode(GameType.SURVIVAL);
        return o;
    }

    /** Oyuncunun ana elindeki eşyayla bloğun üstüne sağ tık (Forge olayı, sunucu yolu). */
    private static PlayerInteractEvent.RightClickBlock sagTik(ServerPlayer o, BlockPos mutlak) {
        return ForgeHooks.onRightClickBlock(o, InteractionHand.MAIN_HAND, mutlak,
                new BlockHitResult(Vec3.upFromBottomCenterOf(mutlak, 1.0), Direction.UP, mutlak, false));
    }

    private static boolean delikMi(BlockState s) {
        return s.is(BalikcilikKayitlari.BUZ_DELIGI.get());
    }

    @GameTest(template = "bos", batch = BATCH)
    public static void kazmaylaEgilerekDelikAcilirAletBesHasarAlir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        donma(y, 50f);
        havuzKur(y);
        ServerPlayer o = oyuncu(y, 1, 1);
        ItemStack kazma = new ItemStack(Items.IRON_PICKAXE);
        o.setItemInHand(InteractionHand.MAIN_HAND, kazma);
        BlockPos delik = y.absolutePos(DELIK);

        o.setShiftKeyDown(false);
        sagTik(o, delik);
        y.assertTrue(level.getBlockState(delik).is(Blocks.ICE), "eğilmeden delik açılmamalı");

        o.setShiftKeyDown(true);
        o.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
        sagTik(o, delik);
        y.assertTrue(level.getBlockState(delik).is(Blocks.ICE), "kürekle delik açılmamalı");

        BlockPos kuru = y.absolutePos(new BlockPos(0, 5, 0));
        level.setBlockAndUpdate(kuru, Blocks.ICE.defaultBlockState());
        o.setItemInHand(InteractionHand.MAIN_HAND, kazma);
        sagTik(o, kuru);
        y.assertTrue(level.getBlockState(kuru).is(Blocks.ICE), "altı su olmayan buzda delik açılmamalı");
        y.assertTrue(kazma.getDamageValue() == 0, "açılmayan delik aleti aşındırmamalı");

        PlayerInteractEvent.RightClickBlock olay = sagTik(o, delik);
        BlockState s = level.getBlockState(delik);
        y.assertTrue(delikMi(s), "eğilip kazmayla sağ tıkta buz deliği olmalı: " + s);
        y.assertTrue(!s.getValue(BuzDeligiBlogu.INCE), "vanilla buzda açılan delik ince değil");
        y.assertTrue(s.getFluidState().isSource(), "delik su göstermeli (kaynak su sıvı durumu)");
        y.assertTrue(olay.isCanceled() && olay.getCancellationResult() == InteractionResult.SUCCESS,
                "olay başarıyla kapanmalı");
        y.assertTrue(kazma.getDamageValue() == BalikcilikAyarlari.aletHasari(),
                "kazma " + BalikcilikAyarlari.aletHasari() + " dayanıklılık kaybetmeli: " + kazma.getDamageValue());
        y.assertTrue(level.getBlockTicks().hasScheduledTick(delik, BalikcilikKayitlari.BUZ_DELIGI.get()),
                "yeniden donma zamanlanmalı");

        ItemStack balta = new ItemStack(Items.IRON_AXE);
        o.setItemInHand(InteractionHand.MAIN_HAND, balta);
        BlockPos ikinci = y.absolutePos(new BlockPos(3, 4, 3));
        sagTik(o, ikinci);
        y.assertTrue(delikMi(level.getBlockState(ikinci)) && balta.getDamageValue() == BalikcilikAyarlari.aletHasari(),
                "baltayla da açılmalı");
        temizle(y, o);
        y.succeed();
    }

    @GameTest(template = "bos", batch = "balikcilik_esik_alti")
    public static void esikAltindaAcilmazKizilCucedeAcilir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        donma(y, (float) BalikcilikAyarlari.aktiflikEsigi() - 20f);
        havuzKur(y);
        ServerPlayer o = oyuncu(y, 1, 1);
        ItemStack kazma = new ItemStack(Items.IRON_PICKAXE);
        o.setItemInHand(InteractionHand.MAIN_HAND, kazma);
        o.setShiftKeyDown(true);
        BlockPos delik = y.absolutePos(DELIK);
        sagTik(o, delik);
        y.assertTrue(level.getBlockState(delik).is(Blocks.ICE), "donma eşiğin altındayken delik açılmamalı");
        y.assertTrue(kazma.getDamageValue() == 0, "eşik altında alet aşınmamalı");
        DunyaDonemi d = DunyaDonemi.al(level.getServer());
        d.cozulmeDonemiAyarla(true);
        try {
            sagTik(o, delik);
            y.assertTrue(delikMi(level.getBlockState(delik)),
                    "kızıl cüce (çözülme) döneminde donmuş yüzeyde eşik aranmamalı (madde 15)");
        } finally {
            d.cozulmeDonemiAyarla(false);
        }
        temizle(y, o);
        y.succeed();
    }

    /** Chunk sınırı: aynı chunk'ta sınır kadar delik açılır, fazlası açılmaz. Sahne mutlak konumda tek chunk'a kurulur. */
    @GameTest(template = "bos", batch = "balikcilik_sinir")
    public static void chunkBasinaDelikSiniriAsilmaz(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        donma(y, 60f);
        int sinir = BalikcilikAyarlari.chunkBasinaDelik();
        y.assertTrue(sinir >= 1 && sinir <= 12, "test sınırı 1..12 bekler: " + sinir);
        BlockPos koken = y.absolutePos(BlockPos.ZERO);
        ChunkPos chunk = new ChunkPos(koken);
        int x0 = chunk.getMinBlockX() + 2;
        int z = chunk.getMinBlockZ() + 8;
        int y0 = koken.getY();
        int n = sinir + 1;
        List<BlockPos> kurulan = new ArrayList<>();
        for (int x = x0 - 1; x <= x0 + n; x++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean ic = x >= x0 && x < x0 + n && dz == 0;
                BlockPos p = new BlockPos(x, y0 + 1, z + dz);
                level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(p.above(), (ic ? Blocks.WATER : Blocks.STONE).defaultBlockState());
                level.setBlockAndUpdate(p.above(2), (ic ? Blocks.ICE : Blocks.STONE).defaultBlockState());
                level.setBlockAndUpdate(p.above(3), Blocks.AIR.defaultBlockState());
                kurulan.add(p);
            }
        }
        // Önceki koşulardan bu chunk'ta kalmış delikler (yalnız bizim bloğumuz) suya çevrilir.
        int eski = BuzDeligiAcma.acikDelikSayisi(level, chunk);
        if (eski > 0) {
            BlockPos.betweenClosedStream(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                    chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ())
                    .filter(p -> delikMi(level.getBlockState(p))).map(BlockPos::immutable).toList()
                    .forEach(p -> level.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState()));
        }
        y.assertTrue(BuzDeligiAcma.acikDelikSayisi(level, chunk) == 0, "chunk başta deliksiz olmalı");
        BlockPos ayak = new BlockPos(x0, y0 + 4, z + 1);
        ServerPlayer o = CekirdekOyunTestleri.sahteOyuncu(y, ayak.getX() + 0.5, ayak.getY(), ayak.getZ() + 0.5);
        o.setGameMode(GameType.SURVIVAL);
        o.setShiftKeyDown(true);
        o.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        for (int i = 0; i < n; i++) {
            BlockPos buz = new BlockPos(x0 + i, y0 + 3, z);
            y.assertTrue(new ChunkPos(buz).equals(chunk), "sahne tek chunk'ta olmalı");
            sagTik(o, buz);
            boolean acildi = delikMi(level.getBlockState(buz));
            y.assertTrue(acildi == (i < sinir), (i + 1) + ". delik " + (i < sinir ? "açılmalı" : "sınır aşıldığı için açılmamalı"));
        }
        y.assertTrue(BuzDeligiAcma.acikDelikSayisi(level, chunk) == sinir, "chunk'ta tam sınır kadar delik olmalı");
        level.getServer().getPlayerList().remove(o);
        for (BlockPos p : kurulan) {
            for (int k = 3; k >= 0; k--) {
                level.setBlockAndUpdate(p.above(k), Blocks.AIR.defaultBlockState());
            }
        }
        y.succeed();
    }

    /** \u00a76 #3: delik zamanlanır, üç donma aşamasından geçer ve eski buz geri gelir (ince buzda ince buz). */
    @GameTest(template = "bos", batch = BATCH)
    public static void delikZamanDoluncaYenidenDonar(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        donma(y, 50f);
        havuzKur(y);
        BlockPos ince = y.absolutePos(new BlockPos(1, 4, 1));
        level.setBlockAndUpdate(ince, DonmaBloklari.INCE_BUZ.get().defaultBlockState());
        ServerPlayer o = oyuncu(y, 3, 3);
        o.setShiftKeyDown(true);
        o.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_PICKAXE));
        BlockPos delik = y.absolutePos(DELIK);
        sagTik(o, delik);
        sagTik(o, ince);
        Block blok = BalikcilikKayitlari.BUZ_DELIGI.get();
        y.assertTrue(delikMi(level.getBlockState(delik)) && delikMi(level.getBlockState(ince)), "iki delik açılmalı");
        y.assertTrue(level.getBlockState(ince).getValue(BuzDeligiBlogu.INCE), "ince buzdaki delik ince işaretli olmalı");
        for (BlockPos p : List.of(delik, ince)) {
            for (int asama = 0; asama <= DelikOmru.SON_ASAMA; asama++) {
                BlockState s = level.getBlockState(p);
                y.assertTrue(delikMi(s) && s.getValue(BuzDeligiBlogu.ASAMA) == asama, "aşama " + asama + " bekleniyordu: " + s);
                y.assertTrue(level.getBlockTicks().hasScheduledTick(p, blok), "aşama " + asama + " sonraki tick'i zamanlamalı");
                s.tick(level, p, level.random);
            }
        }
        y.assertTrue(level.getBlockState(delik).is(Blocks.ICE), "delik yeniden buz olmalı: " + level.getBlockState(delik));
        y.assertTrue(level.getBlockState(ince).is(DonmaBloklari.INCE_BUZ.get()),
                "ince buzdaki delik ince buz olmalı: " + level.getBlockState(ince));
        y.assertTrue(level.getBlockState(delik.below()).is(Blocks.WATER), "alttaki su kalmalı");
        temizle(y, o);
        y.succeed();
    }

    /**
     * §6 #3 gerçek zamanlanmış tick'le: aşama 1'de konan delik (onPlace aşama süresini zamanlar) kendi tick'leriyle
     * 1 → 2 → 3 → buz ilerler; her geçiş bir sonrakini yeniden zamanlar ve aralık aşama süresidir (15 sn). Açılıştan
     * aşama 1'e ilk gecikme (≥ 3900 tick) çok uzun olduğu için aynı {@code tick} yolu buradan sınanır.
     */
    @GameTest(template = "bos", batch = BATCH, timeoutTicks = 1300)
    public static void delikGercekZamanlanmisTickleYenidenDonar(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        donma(y, 50f);
        havuzKur(y);
        BlockPos delik = y.absolutePos(DELIK);
        Block blok = BalikcilikKayitlari.BUZ_DELIGI.get();
        int asamaSuresi = DelikOmru.asamaGecikmesi(BalikcilikAyarlari.delikOmru());
        level.setBlock(delik, blok.defaultBlockState().setValue(BuzDeligiBlogu.ASAMA, 1), Block.UPDATE_ALL);
        y.assertTrue(level.getBlockTicks().hasScheduledTick(delik, blok), "aşama 1 tick'i zamanlanmalı");
        long[] son = {level.getGameTime()};
        int[] gorulen = {1};
        List<String> gecisler = new ArrayList<>();
        y.onEachTick(() -> {
            BlockState s = level.getBlockState(delik);
            int simdi = delikMi(s) ? s.getValue(BuzDeligiBlogu.ASAMA) : s.is(Blocks.ICE) ? DelikOmru.SON_ASAMA + 1 : -1;
            if (simdi == gorulen[0]) {
                return;
            }
            long t = level.getGameTime();
            long aralik = t - son[0];
            gecisler.add(gorulen[0] + "→" + simdi + " (" + aralik + " tick)");
            DogalAfetler.LOGGER.info("[balikcilik] test: delik aşaması {} → {} ({} tick sonra; zamanlanmış: {})", gorulen[0],
                    simdi, aralik, delikMi(s) && level.getBlockTicks().hasScheduledTick(delik, blok));
            y.assertTrue(simdi == gorulen[0] + 1, "aşama sırası bozuk: " + gecisler + " şimdi " + s);
            y.assertTrue(Math.abs(aralik - asamaSuresi) <= 2, "aşama aralığı " + asamaSuresi + " tick olmalı: " + gecisler);
            if (delikMi(s)) {
                y.assertTrue(level.getBlockTicks().hasScheduledTick(delik, blok), "aşama " + simdi + " yeniden zamanlanmalı");
            }
            gorulen[0] = simdi;
            son[0] = t;
        });
        y.succeedWhen(() -> {
            y.assertTrue(gorulen[0] == DelikOmru.SON_ASAMA + 1, "delik gerçek tick'le buza dönmedi: " + gecisler);
            y.assertTrue(level.getBlockState(delik.below()).is(Blocks.WATER), "alttaki su kalmalı");
            temizle(y);
        });
    }

    /** B-20: global donma 0 (sıfırlanmış dünya) iken son aşamadaki delik buz değil su olur. */
    @GameTest(template = "bos", batch = "balikcilik_sifir")
    public static void sifirDonmadaDelikSuyaDoner(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        donma(y, 0f);
        havuzKur(y);
        BlockPos delik = y.absolutePos(DELIK);
        level.setBlock(delik, BalikcilikKayitlari.BUZ_DELIGI.get().defaultBlockState()
                .setValue(BuzDeligiBlogu.ASAMA, DelikOmru.SON_ASAMA).setValue(BuzDeligiBlogu.INCE, true), Block.UPDATE_ALL);
        BlockState s = level.getBlockState(delik);
        s.tick(level, delik, level.random);
        BlockState sonra = level.getBlockState(delik);
        y.assertTrue(sonra.is(Blocks.WATER) && sonra.getFluidState().isSource(),
                "donma 0'da delik kaynak su olmalı (ince buz değil): " + sonra);
        temizle(y);
        y.succeed();
    }

    /** Ömür: ilk gecikme + üç aşama config aralığında (4–5 dk) kalır. */
    @GameTest(template = "bos", batch = BATCH)
    public static void delikOmruConfigAraligindadir(GameTestHelper y) {
        DelikOmru.Ayarlar a = BalikcilikAyarlari.delikOmru();
        y.assertTrue(a.enAzTick() == 240 * 20 && a.enCokTick() == 300 * 20, "varsayılan ömür 4–5 dk olmalı");
        for (double u : new double[] {0.0, 0.5, 0.999}) {
            int omur = DelikOmru.omurTick(u, a);
            int toplam = DelikOmru.ilkGecikme(omur, a) + DelikOmru.SON_ASAMA * DelikOmru.asamaGecikmesi(a);
            y.assertTrue(toplam == omur && omur >= a.enAzTick() && omur <= a.enCokTick(), "ömür aralık dışı: " + toplam);
        }
        y.succeed();
    }

    private static Set<Item> izinliLoot() {
        Set<Item> s = new HashSet<>(BalikcilikKayitlari.ciglar());
        s.add(BalikcilikKayitlari.BUZ_PARCASI.get());
        s.add(Items.SNOWBALL);
        s.add(Items.ENCHANTED_BOOK);
        return s;
    }

    private static void lootuDenetle(GameTestHelper y, List<ItemStack> l, String ne) {
        Set<Item> izinli = izinliLoot();
        Set<Enchantment> kitap = Set.of(Enchantments.FROST_WALKER, Enchantments.FISHING_SPEED, Enchantments.FISHING_LUCK);
        for (ItemStack s : l) {
            String kimlik = String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem()));
            y.assertTrue(!kimlik.contains("meteor") && !kimlik.contains("kozmik"), ne + ": meteor ödülü çıktı: " + kimlik);
            y.assertTrue(izinli.contains(s.getItem()), ne + ": beklenmeyen eşya: " + kimlik);
            if (s.is(Items.ENCHANTED_BOOK)) {
                Map<Enchantment, Integer> b = EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(s));
                y.assertTrue(!b.isEmpty() && kitap.containsAll(b.keySet()), ne + ": hazine kitabı büyüsü: " + b.keySet());
            }
        }
    }

    /** SplitMix64 karıştırıcısı. */
    private static long karistir(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Meteor ödülü hiç çıkmaz; oranlar config'e yakın (1000 çekiliş, sabit tohumlar). */
    @GameTest(template = "bos", batch = BATCH)
    public static void bincekiliseMeteorCikmazOranlarTutar(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        LootTable tablo = level.getServer().getLootData().getLootTable(BuzDeligiLootDegistirici.TABLO);
        y.assertTrue(tablo != LootTable.EMPTY, "balıkçılık tablosu yüklenmemiş");
        Vec3 merkez = Vec3.atCenterOf(y.absolutePos(DELIK));
        List<ItemStack> hepsi = new ArrayList<>();
        int balik = 0;
        int cekilis = 1000;
        Set<Item> turler = new HashSet<>();
        for (int i = 0; i < cekilis; i++) {
            LootParams p = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, merkez)
                    .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD)).withLuck(0)
                    .create(LootContextParamSets.FISHING);
            // Ardışık tohumların ilk zarı (LCG) ilişkili; tohumlar karıştırılır.
            List<ItemStack> l = tablo.getRandomItems(p, karistir(0x15_0000L + i));
            y.assertTrue(l.size() == 1, "her çekiliş tek yığın vermeli: " + l);
            hepsi.addAll(l);
            for (ItemStack s : l) {
                if (s.is(BalikcilikEtiketleri.CIG_KUTUP_BALIGI)) {
                    balik++;
                    turler.add(s.getItem());
                }
            }
        }
        lootuDenetle(y, hepsi, "1000 çekiliş");
        double oran = balik / (double) cekilis;
        y.assertTrue(oran >= 0.87 && oran <= 0.97, "balık oranı config'e (%92) yakın olmalı: " + oran);
        y.assertTrue(turler.size() == Balik.values().length, "beş türün hepsi çıkmalı: " + turler);
        y.succeed();
    }

    private static FishingHook oltaAt(ServerLevel level, ServerPlayer o, BlockPos hedef) {
        FishingHook olta = new FishingHook(o, level, 0, 0);
        olta.moveTo(hedef.getX() + 0.5, hedef.getY() + 1.3, hedef.getZ() + 0.5, 0f, 0f);
        olta.setDeltaMovement(0, -0.15, 0);
        level.addFreshEntity(olta);
        return olta;
    }

    private static void isirmaZorla(GameTestHelper y, FishingHook olta) {
        try {
            ISIRMA.setInt(olta, 20);
        } catch (IllegalAccessException e) {
            y.fail("FishingHook.nibble yazılamadı: " + e);
        }
    }

    /**
     * Olta deliğe düşünce bizim bekleme süremiz işler (30–60 sn); zorlanmış ısırmada çekilen loot bizim tablodan gelir ve
     * {@link BalikTutulduEvent} yayınlanır.
     */
    @GameTest(template = "bos", batch = BATCH, timeoutTicks = 300)
    public static void deligeAtilanOltaBuzDeligiLootunuVerir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        donma(y, 50f);
        havuzKur(y);
        BlockPos delik = y.absolutePos(DELIK);
        level.setBlockAndUpdate(delik, BalikcilikKayitlari.BUZ_DELIGI.get().defaultBlockState());
        ServerPlayer o = oyuncu(y, 1, 1);
        ItemStack olta_ = new ItemStack(Items.FISHING_ROD);
        o.setItemInHand(InteractionHand.MAIN_HAND, olta_);
        FishingHook olta = oltaAt(level, o, delik);
        long atis = level.getGameTime();
        List<BalikTutulduEvent> olaylar = new ArrayList<>();
        Consumer<BalikTutulduEvent> dinleyici = olaylar::add;
        int enCok = IsirmaSuresi.beklemeTick(IsirmaSuresi.toplamTick(1.0, 0, BalikcilikAyarlari.isirma()));
        int enAz = IsirmaSuresi.beklemeTick(IsirmaSuresi.toplamTick(0.0, 0, BalikcilikAyarlari.isirma()));
        boolean[] bitti = {false};
        y.onEachTick(() -> {
            int kalan = BuzBalikciligi.kalanBekleme(olta);
            // isInWater oltanın bir sonraki tick başında güncellenir: delikteki ilk tick'te hâlâ false olabilir.
            if (bitti[0] || kalan <= 0 || !olta.isInWater()) {
                return;
            }
            bitti[0] = true;
            dinle(dinleyici);
            try {
                y.assertTrue(BuzBalikciligi.delikteMi(olta), "olta deliğin suyunda yüzmeli");
                // Bekleme olta deliğe girdiği tick'te başlar ve her tick bir azalır: atıştan beri geçen tick kadar eksilebilir.
                long gecen = level.getGameTime() - atis;
                DogalAfetler.LOGGER.info("[balikcilik] test: delikte kalan bekleme {} tick, atıştan {} tick ({}..{})", kalan,
                        gecen, enAz, enCok);
                y.assertTrue(kalan <= enCok && kalan >= enAz - gecen - 1, "bekleme config aralığında (30–60 sn) olmalı: "
                        + kalan + " (" + enAz + ".." + enCok + ", geçen " + gecen + ")");
                // Isırmada vanilla şamandırayı aşağı çeker: olta deliğin altındaki suya inse de delikte sayılmalı.
                olta.setPos(olta.getX(), delik.getY() - 0.35, olta.getZ());
                y.assertTrue(BuzBalikciligi.delikteMi(olta), "deliğin altındaki suya inen olta delikte sayılmalı");
                isirmaZorla(y, olta);
                int hasar = olta.retrieve(olta_);
                y.assertTrue(hasar == 1, "çekme oltaya 1 hasar vermeli: " + hasar);
                List<ItemEntity> esyalar = level.getEntitiesOfClass(ItemEntity.class, new AABB(delik).inflate(2, 3, 2));
                y.assertTrue(!esyalar.isEmpty(), "çekilen eşya doğmalı");
                for (ItemEntity e : esyalar) {
                    y.assertTrue(e.getY() >= delik.getY() + 1.0 && e.getDeltaMovement().y > 0,
                            "çekilen eşya deliğin üstünden oyuncuya fırlamalı (delikte kalmamalı): y=" + e.getY());
                    e.discard();
                }
                y.assertTrue(olaylar.size() == 1, "BalikTutulduEvent bir kez yayınlanmalı: " + olaylar.size());
                BalikTutulduEvent e = olaylar.get(0);
                y.assertTrue(e.oyuncu() == o && e.delik().equals(delik) && !e.yakalananlar().isEmpty(),
                        "olayın oyuncusu, deliği ve yakalananları doğru olmalı");
                lootuDenetle(y, e.yakalananlar(), "delikten çekiliş");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(dinleyici);
                temizle(y, o);
            }
        });
        y.succeedWhen(() -> y.assertTrue(bitti[0], "olta delikte beklemeye başlamadı (yüzme: " + olta.isInWater()
                + ", konum " + olta.blockPosition() + ", kaldırıldı: " + olta.isRemoved() + ")"));
    }

    private static void dinle(Consumer<BalikTutulduEvent> dinleyici) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BalikTutulduEvent.class, dinleyici);
    }

    /**
     * Ba4: vanilla açık suda (buzsuz havuz) zorlanmış ısırmada {@code ItemFishedEvent} sonucu vanilla tablodandır: hiçbir
     * yığın bizim ad alanımızdan değil (GLM tetiklenmedi) ve {@link BalikTutulduEvent} yayınlanmaz. Kendi batch'inde donma
     * 0: donma %50 iken süreç gökyüzüne açık havuz yüzeyini buza çeviriyordu (şamandıra buzun içinde kalıyordu).
     */
    @GameTest(template = "bos", batch = "balikcilik_acik_su", timeoutTicks = 200)
    public static void acikSudaVanillaLootDegismez(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        donma(y, 0f);
        havuzKur(y);
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                y.setBlock(new BlockPos(x, 4, z), Blocks.WATER);
            }
        }
        BlockPos su = y.absolutePos(DELIK);
        ServerPlayer o = oyuncu(y, 0, 0);
        ItemStack olta_ = new ItemStack(Items.FISHING_ROD);
        o.setItemInHand(InteractionHand.MAIN_HAND, olta_);
        FishingHook olta = oltaAt(level, o, su);
        List<BalikTutulduEvent> olaylar = new ArrayList<>();
        Consumer<BalikTutulduEvent> dinleyici = olaylar::add;
        List<ItemStack> vanilla = new ArrayList<>();
        Consumer<ItemFishedEvent> tutulan = e -> vanilla.addAll(e.getDrops());
        boolean[] bitti = {false};
        y.onEachTick(() -> {
            // Koşul şamandıranın bloğundaki sıvı (ilk koşuda isInWater bu pencerede hiç true olmadı); retrieve zorlanmış
            // ısırmada sıvıya bakmadan loot yuvarlar, GLM'nin kararı yalnız delikteMi'ye bağlıdır.
            if (bitti[0] || olta.isRemoved() || olta.tickCount < 10
                    || !level.getFluidState(olta.blockPosition()).is(FluidTags.WATER)) {
                return;
            }
            bitti[0] = true;
            dinle(dinleyici);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ItemFishedEvent.class, tutulan);
            try {
                y.assertTrue(!BuzBalikciligi.delikteMi(olta) && BuzBalikciligi.kalanBekleme(olta) == -1,
                        "açık suda bizim bekleme işlememeli");
                isirmaZorla(y, olta);
                olta.retrieve(olta_);
                y.assertTrue(!vanilla.isEmpty(), "vanilla balıkçılık bir şey vermeli");
                for (ItemStack s : vanilla) {
                    String kimlik = String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem()));
                    y.assertTrue(!kimlik.startsWith(DogalAfetler.MOD_ID + ":"),
                            "açık suda buz deliği loot'u çıktı (GLM yalnız delikte çalışmalı): " + kimlik);
                }
                y.assertTrue(olaylar.isEmpty(), "açık suda BalikTutulduEvent yayınlanmamalı");
                DogalAfetler.LOGGER.info("[balikcilik] test: açık su vanilla loot {}", vanilla);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(dinleyici);
                MinecraftForge.EVENT_BUS.unregister(tutulan);
                temizle(y, o);
            }
        });
        y.succeedWhen(() -> y.assertTrue(bitti[0], "olta açık suya düşmedi: " + olta.blockPosition() + " kaldırıldı="
                + olta.isRemoved() + " sıvı=" + level.getFluidState(olta.blockPosition()) + " yüzme=" + olta.isInWater()));
    }

    /** Normal buza atılan olta suya değmez: bekleme başlamaz, zorlanmış çekme bizim olayımızı yayınlamaz. */
    @GameTest(template = "bos", batch = BATCH, timeoutTicks = 200)
    public static void normalBuzaAtilanOltaTutmaz(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        donma(y, 50f);
        havuzKur(y);
        BlockPos buz = y.absolutePos(DELIK);
        ServerPlayer o = oyuncu(y, 1, 1);
        ItemStack olta_ = new ItemStack(Items.FISHING_ROD);
        o.setItemInHand(InteractionHand.MAIN_HAND, olta_);
        FishingHook olta = oltaAt(level, o, buz);
        List<BalikTutulduEvent> olaylar = new ArrayList<>();
        Consumer<BalikTutulduEvent> dinleyici = olaylar::add;
        y.runAfterDelay(60, () -> {
            dinle(dinleyici);
            try {
                y.assertTrue(!olta.isRemoved(), "olta buzun üstünde durmalı");
                y.assertTrue(!olta.isInWater() && !BuzBalikciligi.delikteMi(olta), "olta normal buzda suya değmemeli");
                y.assertTrue(BuzBalikciligi.kalanBekleme(olta) == -1, "normal buzda bekleme başlamamalı");
                isirmaZorla(y, olta);
                olta.retrieve(olta_);
                y.assertTrue(olaylar.isEmpty(), "normal buzda BalikTutulduEvent yayınlanmamalı");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(dinleyici);
            }
            temizle(y, o);
            y.succeed();
        });
    }

    /** Madde 14/15: çiğ buz balığı küçük serinlik, pişmiş orta ısınma, pişmiş Donmuş Ringa büyük ısınma (B-19). */
    @GameTest(template = "bos", batch = BATCH)
    public static void cigBalikSogutururPismisIsitir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        y.assertTrue(SicaklikKapisi.acikMi(level.getServer()), "sıcaklık sistemi açık olmalı (test önkoşulu)");
        ServerPlayer o = oyuncu(y, 2, 2);
        double[] sonuc = new double[3];
        Item[] yiyecekler = {Balik.BUZ_ALABALIGI.cig(), Balik.KUTUP_MORINASI.pismis(), Balik.DONMUS_RINGA.pismis()};
        for (int i = 0; i < yiyecekler.length; i++) {
            VucutSicakligi.sifirla(o);
            VucutSicakligi.ayarla(o, 0);
            ItemStack yemek = new ItemStack(yiyecekler[i], 2);
            yemek.finishUsingItem(level, o);
            sonuc[i] = VucutSicakligi.al(o);
            y.assertTrue(yemek.getCount() == 1, "bir tane yenmeli");
            y.assertTrue(!VucutSicakligi.etkiler(o).isEmpty(), "süreli sıcaklık etkisi eklenmeli");
        }
        // Yiyecek kademeleri (B-19): küçük −8, orta +20, büyük +35; yenince %25'i.
        y.assertTrue(Math.abs(sonuc[0] + 2.0) < 1e-6, "çiğ balık anında −2 soğutmalı: " + sonuc[0]);
        y.assertTrue(Math.abs(sonuc[1] - 5.0) < 1e-6, "pişmiş balık anında +5 ısıtmalı: " + sonuc[1]);
        y.assertTrue(Math.abs(sonuc[2] - 8.75) < 1e-6, "pişmiş Donmuş Ringa anında +8,75 ısıtmalı: " + sonuc[2]);
        VucutSicakligi.sifirla(o);
        level.getServer().getPlayerList().remove(o);
        y.succeed();
    }

    /** Madde 15 risk/denge: delikte oltası olan, buz üstünde hareketsiz oyuncunun ortamı giderek soğur. */
    @GameTest(template = "bos", batch = BATCH, timeoutTicks = 300)
    public static void buzdaHareketsizBalikciDahaHizliSogur(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        donma(y, 50f);
        havuzKur(y);
        BlockPos delik = y.absolutePos(DELIK);
        level.setBlockAndUpdate(delik, BalikcilikKayitlari.BUZ_DELIGI.get().defaultBlockState());
        ServerPlayer o = oyuncu(y, 1, 1);
        y.assertTrue(BuzBalikciligi.buzUstundeMi(o), "oyuncu buz üstünde durmalı: " + level.getBlockState(o.getOnPos()));
        double once = SicaklikYoneticisi.analiz(o).ortam().toplam();
        o.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FISHING_ROD));
        oltaAt(level, o, delik);
        y.succeedWhen(() -> {
            double sogutma = BuzBalikciligi.sonSogutma(o);
            y.assertTrue(sogutma <= -1.0, "hareketsizlik soğuması henüz güçlenmedi: " + sogutma);
            double simdi = SicaklikYoneticisi.analiz(o).ortam().toplam();
            y.assertTrue(simdi <= once - 0.9, "ortam sıcaklığı düşmeli: " + once + " -> " + simdi);
            temizle(y, o);
        });
    }
}
