package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.esya.ModSekmesi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok.KomurlesmisKutukBlogu;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Tur 3 blok dilimi oyun testleri. Kül Bloğu: modun "Doğal Afetler" sekmesinde (sekme bütün {@code dogalafetler} eşyalarını
 * taşır) ve Doğal Bloklar sekmesinde; 8 katlı Kül Katmanı kırılınca (ipek dokunuşta da) ve orta tıkla Kül Bloğu
 * verir; yaprak külü 8 kata varınca gerçek Kül Bloğu yerleşir; çalışma masasında 8 katman ↔ 1 blok. Kömürleşmiş kütük
 * ve tahta: çalışma masasında 1 kütük → 4 tahta (tarif kitabında kütükle açılır), tahta çubuk ve çalışma masası yapar
 * ama tekne yapmaz, ikisi de yakıt değil ve bitişik ateş ya da lavla tutuşmaz, kütük suya konunca ya da su ona gelince
 * planlanmış tick'te kömür bloğuna döner (su kaldırıldıysa dönmez), pulsar kavurmasının su kıyısında yerinde
 * kömürleştirdiği kütük kendiliğinden dönmez. Düz dünyada sahne testin içinde kurulur ({@code donma_alan}, 9×6×9
 * hava); config dosyasına yazılmaz (K-E28).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PulsarBlokOyunTestleri {
    private static final String ALAN = "donma_alan";
    private static final int EN = 9;
    private static final String KUL = "pulsar_blok_kul";
    private static final String ODUN = "pulsar_blok_odun";

    private PulsarBlokOyunTestleri() {
    }

    private static BlockPos p(int x, int yy, int z) {
        return new BlockPos(x, yy + 1, z);
    }

    /** Taş taban, üstü hava. */
    private static void zemin(GameTestHelper y) {
        for (int x = 0; x < EN; x++) {
            for (int z = 0; z < EN; z++) {
                y.setBlock(p(x, 0, z), Blocks.STONE);
                for (int yy = 1; yy < 5; yy++) {
                    y.setBlock(p(x, yy, z), Blocks.AIR);
                }
            }
        }
    }

    private static boolean icinde(CreativeModeTab sekme, Item esya) {
        return sekme.getDisplayItems().stream().anyMatch(s -> s.is(esya));
    }

    private static CreativeModeTab kur(ServerLevel level, CreativeModeTab sekme) {
        sekme.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), false,
                level.registryAccess()));
        return sekme;
    }

    // ---------------------------------------------------------------- kül

    /** Kül Bloğu envanterde: mod sekmesinde (bütün mod eşyalarıyla), Doğal Bloklar'da; tahta İnşa Blokları'nda. */
    @GameTest(template = "bos", batch = KUL)
    public static void kulBloguYaraticiSekmelerdeGorunur(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        CreativeModeTab mod = kur(level, ModSekmesi.SEKME.get());
        CreativeModeTab dogal = kur(level, BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.NATURAL_BLOCKS));
        CreativeModeTab insa = kur(level, BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.BUILDING_BLOCKS));
        Item kulBlogu = PulsarKayitlari.KUL_BLOGU.get().asItem();
        y.assertTrue(icinde(mod, kulBlogu), "Kül Bloğu mod sekmesinde yok");
        y.assertTrue(icinde(dogal, kulBlogu), "Kül Bloğu Doğal Bloklar sekmesinde yok");
        y.assertTrue(icinde(insa, PulsarKayitlari.KOMURLESMIS_TAHTA_ESYASI.get()), "tahta İnşa Blokları'nda yok");
        y.assertTrue(mod.getIconItem().is(kulBlogu), "mod sekmesinin ikonu Kül Bloğu değil: " + mod.getIconItem());
        List<String> eksik = new ArrayList<>();
        int sayi = 0;
        for (Item esya : ForgeRegistries.ITEMS) {
            ResourceLocation kimlik = ForgeRegistries.ITEMS.getKey(esya);
            if (kimlik != null && DogalAfetler.MOD_ID.equals(kimlik.getNamespace())) {
                sayi++;
                if (!icinde(mod, esya)) {
                    eksik.add(kimlik.getPath());
                }
            }
        }
        y.assertTrue(eksik.isEmpty(), "mod sekmesinde olmayan mod eşyaları: " + eksik);
        y.assertTrue(mod.getDisplayItems().size() == sayi, "mod sekmesi " + mod.getDisplayItems().size()
                + " eşya, kayıtlı " + sayi);
        DogalAfetler.LOGGER.info("Pulsar blok GameTest: mod sekmesinde {} eşya", sayi);
        y.succeed();
    }

    /** 8 katlı kül (Kül Bloğu modeliyle çizilir) kırılınca ve orta tıkla Kül Bloğu; 1–7 kat kat sayısı kadar katman. */
    @GameTest(template = ALAN, batch = KUL)
    public static void sekizKatKulKirilincaVeOrtaTiklaKulBloguVerir(GameTestHelper y) {
        zemin(y);
        ServerLevel level = y.getLevel();
        BlockPos konum = y.absolutePos(p(4, 1, 4));
        Item katman = PulsarKayitlari.KUL_KATMANI.get().asItem();
        Item blok = PulsarKayitlari.KUL_BLOGU.get().asItem();
        ItemStack kurek = new ItemStack(Items.DIAMOND_SHOVEL);
        ItemStack ipek = new ItemStack(Items.DIAMOND_SHOVEL);
        ipek.enchant(Enchantments.SILK_TOUCH, 1);
        for (int kat = 1; kat <= SnowLayerBlock.MAX_HEIGHT; kat++) {
            BlockState durum = PulsarKayitlari.KUL_KATMANI.get().defaultBlockState().setValue(SnowLayerBlock.LAYERS, kat);
            level.setBlock(konum, durum, Block.UPDATE_ALL);
            for (ItemStack alet : List.of(kurek, ipek, ItemStack.EMPTY)) {
                List<ItemStack> dusen = Block.getDrops(durum, level, konum, null, null, alet);
                boolean dogru = kat < SnowLayerBlock.MAX_HEIGHT
                        ? dusen.size() == 1 && dusen.get(0).is(katman) && dusen.get(0).getCount() == kat
                        : dusen.size() == 1 && dusen.get(0).is(blok) && dusen.get(0).getCount() == 1;
                y.assertTrue(dogru, kat + " kat (" + alet + ") loot'u yanlış: " + dusen);
            }
            ItemStack orta = durum.getBlock().getCloneItemStack(level, konum, durum);
            y.assertTrue(orta.is(kat < SnowLayerBlock.MAX_HEIGHT ? katman : blok), kat + " kat orta tık: " + orta);
        }
        y.succeed();
    }

    /** Yaprak külü 7 katlı katmanın üstüne düşünce gerçek Kül Bloğu olur; üstüne düşen sonraki kül yeni katman açar. */
    @GameTest(template = ALAN, batch = KUL)
    public static void kulSekizKataVarincaKulBloguOlur(GameTestHelper y) {
        zemin(y);
        BlockState yaprak = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        y.setBlock(p(4, 1, 4), PulsarKayitlari.KUL_KATMANI.get().defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7));
        y.setBlock(p(4, 4, 4), yaprak);
        int yazilan = KavurmaKuyrugu.donustur(y.getLevel(), y.absolutePos(p(4, 4, 4)), 1f, 0.5);
        y.assertTrue(yazilan == 2 && y.getBlockState(p(4, 4, 4)).isAir(), "yaprak → hava + kül: " + yazilan);
        y.assertTrue(y.getBlockState(p(4, 1, 4)).is(PulsarKayitlari.KUL_BLOGU.get()),
                "8. kat kül bloğu olmalı: " + y.getBlockState(p(4, 1, 4)));
        y.setBlock(p(4, 4, 4), yaprak);
        KavurmaKuyrugu.donustur(y.getLevel(), y.absolutePos(p(4, 4, 4)), 1f, 0.5);
        BlockState ust = y.getBlockState(p(4, 2, 4));
        y.assertTrue(ust.is(PulsarKayitlari.KUL_KATMANI.get()) && ust.getValue(SnowLayerBlock.LAYERS) == 1,
                "kül bloğunun üstüne bir kat kül: " + ust);
        y.succeed();
    }

    /** Çalışma masası: 8 Kül Katmanı → 1 Kül Bloğu, 1 Kül Bloğu → 8 Kül Katmanı (8 katlı külü kıran kat kaybetmez). */
    @GameTest(template = "bos", batch = KUL)
    public static void kulKatmaniVeKulBloguBirbirineDoner(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        Item katman = PulsarKayitlari.KUL_KATMANI.get().asItem();
        Item blok = PulsarKayitlari.KUL_BLOGU.get().asItem();
        Optional<ItemStack> bloga = uret(level, katman, katman, katman, katman, katman, katman, katman, katman);
        y.assertTrue(bloga.isPresent() && bloga.get().is(blok) && bloga.get().getCount() == 1,
                "8 katman → 1 Kül Bloğu bekleniyordu: " + bloga);
        Optional<ItemStack> katmana = uret(level, blok);
        y.assertTrue(katmana.isPresent() && katmana.get().is(katman)
                && katmana.get().getCount() == SnowLayerBlock.MAX_HEIGHT, "1 Kül Bloğu → 8 katman bekleniyordu: " + katmana);
        Optional<ItemStack> yedi = uret(level, katman, katman, katman, katman, katman, katman, katman);
        y.assertTrue(yedi.isEmpty(), "7 katman bir şey üretmemeli: " + yedi);
        y.succeed();
    }

    // ---------------------------------------------------------------- kömürleşmiş odun

    private static TransientCraftingContainer tezgah(Item... dokuz) {
        AbstractContainerMenu bos = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player oyuncu, int yuva) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player oyuncu) {
                return true;
            }
        };
        TransientCraftingContainer t = new TransientCraftingContainer(bos, 3, 3);
        for (int i = 0; i < dokuz.length; i++) {
            if (dokuz[i] != null) {
                t.setItem(i, new ItemStack(dokuz[i]));
            }
        }
        return t;
    }

    private static Optional<ItemStack> uret(ServerLevel level, Item... dokuz) {
        TransientCraftingContainer t = tezgah(dokuz);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, t, level)
                .map(tarif -> tarif.assemble(t, level.registryAccess()));
    }

    /**
     * Çalışma masası: 1 kütük → 4 tahta (kitapta kütükle açılır); tahta çubuk ve çalışma masası yapar; hiçbir tekne/sal
     * tarifi tahtayla eşleşmez (vanilla tekne tarifleri belirli tahta eşyası ister, Nether tahtası gibi).
     */
    @GameTest(template = "bos", batch = ODUN)
    public static void kutukTahtayaDonerTahtaTekneYapmaz(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        Item kutuk = PulsarKayitlari.KOMURLESMIS_KUTUK_ESYASI.get();
        Item tahta = PulsarKayitlari.KOMURLESMIS_TAHTA_ESYASI.get();
        Optional<ItemStack> tahtalar = uret(level, kutuk);
        y.assertTrue(tahtalar.isPresent() && tahtalar.get().is(tahta) && tahtalar.get().getCount() == 4,
                "1 kütük → 4 tahta bekleniyordu: " + tahtalar);
        Optional<ItemStack> cubuk = uret(level, tahta, null, null, tahta);
        y.assertTrue(cubuk.isPresent() && cubuk.get().is(Items.STICK), "tahta → çubuk: " + cubuk);
        Optional<ItemStack> masa = uret(level, tahta, tahta, null, tahta, tahta);
        y.assertTrue(masa.isPresent() && masa.get().is(Items.CRAFTING_TABLE), "tahta → çalışma masası: " + masa);
        TransientCraftingContainer tekne = tezgah(tahta, null, tahta, tahta, tahta, tahta);
        List<String> tekneler = new ArrayList<>();
        int tekneTarifi = 0;
        for (CraftingRecipe tarif : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (tarif.getResultItem(level.registryAccess()).getItem() instanceof BoatItem) {
                tekneTarifi++;
                if (tarif.matches(tekne, level)) {
                    tekneler.add(tarif.getId().toString());
                }
            }
        }
        y.assertTrue(tekneTarifi > 0, "hiç tekne tarifi yüklenmedi");
        y.assertTrue(tekneler.isEmpty(), "kömürleşmiş tahtayla tekne yapılıyor: " + tekneler);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(y, 0.5, 2, 0.5);
        try {
            ResourceLocation tarif = DogalAfetler.konum("komurlesmis_tahta");
            y.assertTrue(!oyuncu.getRecipeBook().contains(tarif), "tahta tarifi kütüksüz kitapta");
            oyuncu.getInventory().add(new ItemStack(kutuk));
            oyuncu.inventoryMenu.broadcastChanges();
            y.assertTrue(oyuncu.getRecipeBook().contains(tarif), "kütük envantere girince tahta tarifi kitapta değil");
        } finally {
            level.getServer().getPlayerList().remove(oyuncu);
        }
        y.succeed();
    }

    /** Kütük ve tahta yakıt değil (meşe tahtası 300); etiketler Nether odunu gibi, kütük {@code minecraft:logs}'ta değil. */
    @GameTest(template = "bos", batch = ODUN)
    public static void kutukVeTahtaYakitDegil(GameTestHelper y) {
        ItemStack kutuk = new ItemStack(PulsarKayitlari.KOMURLESMIS_KUTUK_ESYASI.get());
        ItemStack tahta = new ItemStack(PulsarKayitlari.KOMURLESMIS_TAHTA_ESYASI.get());
        for (RecipeType<?> tur : List.of(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING)) {
            y.assertTrue(ForgeHooks.getBurnTime(kutuk, tur) == 0, "kütük yakıt: " + ForgeHooks.getBurnTime(kutuk, tur));
            y.assertTrue(ForgeHooks.getBurnTime(tahta, tur) == 0, "tahta yakıt: " + ForgeHooks.getBurnTime(tahta, tur));
        }
        y.assertTrue(ForgeHooks.getBurnTime(new ItemStack(Items.OAK_PLANKS), RecipeType.SMELTING) == 300,
                "karşılaştırma: meşe tahtası 300 tick yanmalı");
        y.assertTrue(tahta.is(ItemTags.PLANKS) && tahta.is(ItemTags.NON_FLAMMABLE_WOOD), "tahta etiketleri");
        y.assertTrue(kutuk.is(ItemTags.NON_FLAMMABLE_WOOD) && !kutuk.is(ItemTags.LOGS) && !kutuk.is(ItemTags.LOGS_THAT_BURN),
                "kütük eşya etiketleri");
        y.assertTrue(PulsarKayitlari.KOMURLESMIS_TAHTA.get().defaultBlockState().is(BlockTags.PLANKS)
                && !PulsarKayitlari.KOMURLESMIS_KUTUK.get().defaultBlockState().is(BlockTags.LOGS), "blok etiketleri");
        y.succeed();
    }

    /** Bitişik ateş ve lavla tutuşmazlar: hiçbir yüzde yanıcı değil, 100 tick sonra yerinde (meşe tahtası yanıcı). */
    @GameTest(template = ALAN, batch = ODUN, timeoutTicks = 160)
    public static void kutukVeTahtaAtesVeLavlaTutusmaz(GameTestHelper y) {
        zemin(y);
        ServerLevel level = y.getLevel();
        y.setBlock(p(2, 1, 2), PulsarKayitlari.KOMURLESMIS_KUTUK.get());
        y.setBlock(p(6, 1, 6), PulsarKayitlari.KOMURLESMIS_TAHTA.get());
        for (BlockPos k : List.of(p(2, 1, 2), p(6, 1, 6))) {
            y.setBlock(k.east(), Blocks.FIRE);
            y.setBlock(k.west(), Blocks.FIRE);
            y.setBlock(k.north(), Blocks.LAVA);
            y.setBlock(k.south(), Blocks.LAVA);
            BlockPos m = y.absolutePos(k);
            BlockState s = level.getBlockState(m);
            y.assertTrue(!YanginYoneticisi.yanabilir(s, level, m), "ortak yangın yakabilir: " + s);
            for (Direction yon : Direction.values()) {
                y.assertTrue(!s.isFlammable(level, m, yon) && s.getFlammability(level, m, yon) == 0
                        && s.getFireSpreadSpeed(level, m, yon) == 0, s + " " + yon + " yüzü yanıcı");
            }
        }
        BlockPos mese = y.absolutePos(p(4, 3, 4));
        y.assertTrue(Blocks.OAK_PLANKS.defaultBlockState().isFlammable(level, mese, Direction.UP),
                "karşılaştırma: meşe tahtası yanıcı olmalı");
        y.runAfterDelay(100, () -> {
            y.assertTrue(y.getBlockState(p(2, 1, 2)).is(PulsarKayitlari.KOMURLESMIS_KUTUK.get()),
                    "kütük yandı: " + y.getBlockState(p(2, 1, 2)));
            y.assertTrue(y.getBlockState(p(6, 1, 6)).is(PulsarKayitlari.KOMURLESMIS_TAHTA.get()),
                    "tahta yandı: " + y.getBlockState(p(6, 1, 6)));
            y.succeed();
        });
    }

    /**
     * Suya değen kütük 2–4 dakikalık planlanmış tick kurar (su sonradan gelse de, kütük suyun yanına konsa da); tick'te
     * su hâlâ bitişikse kömür bloğu, su kaldırıldıysa kütük kalır. Su taş cebinde: akmaz, kaldırılınca iz bırakmaz.
     */
    @GameTest(template = ALAN, batch = ODUN)
    public static void kutukSuyaDeginceTicktenSonraKomurBloguOlur(GameTestHelper y) {
        zemin(y);
        ServerLevel level = y.getLevel();
        Block kutuk = PulsarKayitlari.KOMURLESMIS_KUTUK.get();
        BlockState yatay = kutuk.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        for (int z : new int[] {2, 6}) {
            y.setBlock(p(4, 1, z), Blocks.STONE);
            y.setBlock(p(3, 1, z - 1), Blocks.STONE);
            y.setBlock(p(3, 1, z + 1), Blocks.STONE);
        }
        BlockPos a = p(2, 1, 2);
        y.setBlock(a, yatay);
        y.assertTrue(!level.getBlockTicks().hasScheduledTick(y.absolutePos(a), kutuk), "susuz kütükte tick planlandı");
        y.setBlock(p(3, 1, 2), Blocks.WATER);
        y.assertTrue(level.getBlockTicks().hasScheduledTick(y.absolutePos(a), kutuk), "su gelince tick planlanmadı");
        BlockPos b = p(2, 1, 6);
        y.setBlock(p(3, 1, 6), Blocks.WATER);
        y.setBlock(b, yatay);
        y.assertTrue(level.getBlockTicks().hasScheduledTick(y.absolutePos(b), kutuk), "suyun yanına konunca planlanmadı");
        y.assertTrue(KomurlesmisKutukBlogu.suyaDegiyor(level, y.absolutePos(a)), "su bitişik görünmeli");

        y.getBlockState(a).tick(level, y.absolutePos(a), level.random);
        y.assertTrue(y.getBlockState(a).is(Blocks.COAL_BLOCK), "suda tick sonrası kömür bloğu: " + y.getBlockState(a));

        y.setBlock(p(3, 1, 6), Blocks.STONE);
        y.assertTrue(!KomurlesmisKutukBlogu.suyaDegiyor(level, y.absolutePos(b)), "su kaldırıldı");
        y.getBlockState(b).tick(level, y.absolutePos(b), level.random);
        y.assertTrue(y.getBlockState(b).equals(yatay), "su kalktıysa kütük kalmalı (eksen korunur): " + y.getBlockState(b));
        y.succeed();
    }

    /**
     * Pulsar kavurması su kıyısındaki meşe kütüğü yerinde kömürleştirir: tick planlanmaz, yanındaki su dışı komşu
     * değişimi de planlamaz; su kaldırılıp yeniden konunca (su kütüğe gelince) planlanır.
     */
    @GameTest(template = ALAN, batch = ODUN)
    public static void kavurmaKutuguSuKiyisindaKendiligindenDonmez(GameTestHelper y) {
        zemin(y);
        ServerLevel level = y.getLevel();
        Block kutuk = PulsarKayitlari.KOMURLESMIS_KUTUK.get();
        y.setBlock(p(4, 1, 4), Blocks.STONE);
        y.setBlock(p(3, 1, 3), Blocks.STONE);
        y.setBlock(p(3, 1, 5), Blocks.STONE);
        y.setBlock(p(3, 1, 4), Blocks.WATER);
        BlockPos k = p(2, 1, 4);
        y.setBlock(k, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        BlockPos m = y.absolutePos(k);
        y.assertTrue(KavurmaKuyrugu.donustur(level, m, 1f, 0.5) == 1, "kavurma kütüğü dönüştürmedi");
        y.assertTrue(y.getBlockState(k).is(kutuk) && KomurlesmisKutukBlogu.suyaDegiyor(level, m),
                "su kıyısında kömürleşmiş kütük bekleniyordu: " + y.getBlockState(k));
        y.assertTrue(!level.getBlockTicks().hasScheduledTick(m, kutuk), "kavurma dönüşümü tick planladı");
        y.setBlock(p(2, 1, 5), Blocks.COARSE_DIRT);
        y.assertTrue(!level.getBlockTicks().hasScheduledTick(m, kutuk), "su dışı komşu değişimi tick planladı");
        y.setBlock(p(3, 1, 4), Blocks.STONE);
        y.setBlock(p(3, 1, 4), Blocks.WATER);
        y.assertTrue(level.getBlockTicks().hasScheduledTick(m, kutuk), "su yeniden gelince tick planlanmadı");
        y.succeed();
    }
}
