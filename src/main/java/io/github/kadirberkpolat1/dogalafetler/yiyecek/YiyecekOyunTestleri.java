package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import com.mojang.authlib.GameProfile;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKancalari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKapisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.VucutSicakligi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.uyum.ColdSweatKoprusu;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Esikler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SureliEtki;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.EtkiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTablosu;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTanimi;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Yon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Yiyecek oyun içi testleri (GOREV §6 #2, #3; madde 14). Yeme gerçek yoldan sınanır: eşyanın {@code finishUsingItem}'ı (yemek
 * değeri, sıcaklık, efekt, kap) ve vanilla yiyecekte Forge'un {@code completeUsingItem}'daki gibi {@code onItemUseFinish}.
 * Sahte oyuncular tick almaz; sıcaklık güncellemesi gerektiğinde doğrudan çağrılır.
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class YiyecekOyunTestleri {
    private static final double EPS = 1e-6;
    private static final int SALEP_BESIN = YiyecekTablosu.al("sicak_salep").besin();

    private YiyecekOyunTestleri() {
    }

    private static ServerPlayer oyuncu(GameTestHelper y, String ad, double dx) {
        BlockPos m = y.absolutePos(BlockPos.ZERO);
        ServerPlayer o = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), ad), m.getX() + 0.5 + dx,
                m.getY(), m.getZ() + 0.5);
        o.setGameMode(GameType.SURVIVAL);
        return o;
    }

    private static void cikar(GameTestHelper y, List<ServerPlayer> oyuncular) {
        for (ServerPlayer o : oyuncular) {
            y.getLevel().getServer().getPlayerList().remove(o);
        }
    }

    private static void dogrula(GameTestHelper y, boolean kosul, String mesaj) {
        if (!kosul) {
            y.fail(mesaj);
        }
    }

    /** Kapıya bağlı testler kuralı kendisi açar (önceki bir batch'te kapalı kalmış olabilir). */
    private static GameRules.BooleanValue kuraliAc(MinecraftServer server) {
        GameRules.BooleanValue kural = server.getGameRules().getRule(Kurallar.SICAKLIK_SISTEMI);
        kural.set(true, server);
        return kural;
    }

    /**
     * Mutlak ortam ölçen test için nötr dönem: donma 0, güneş normal. Dönem globaldir; önceki batch'lerin bıraktığı durum
     * (ör. balıkçılık testlerinin donma %60'ı) ortamı düşürür. Eski değerleri geri koyan işi döndürür.
     */
    private static Runnable notrDonem(MinecraftServer server) {
        DunyaDonemi d = DunyaDonemi.al(server);
        float donma = d.donmaIlerlemesi();
        GunesEvresi evre = d.gunesEvresi();
        float evreIlerlemesi = d.evreIlerlemesi();
        d.donmaIlerlemesiAyarla(0f);
        if (evre != GunesEvresi.NORMAL) {
            d.gunesEvresiAyarla(GunesEvresi.NORMAL);
        }
        return () -> {
            d.donmaIlerlemesiAyarla(donma);
            if (evre != GunesEvresi.NORMAL) {
                d.gunesEvresiAyarla(evre);
                d.evreIlerlemesiAyarla(evreIlerlemesi);
            }
        };
    }

    /** Elde {@code adet} kadar yiyecek tutup birini yer; elde kalanı (ya da boş kabı) döndürür. */
    private static ItemStack ye(ServerPlayer o, String ad, int adet) {
        ItemStack yigin = new ItemStack(YiyecekKayitlari.esya(ad), adet);
        o.setItemInHand(InteractionHand.MAIN_HAND, yigin);
        ItemStack sonuc = yigin.finishUsingItem(o.level(), o);
        o.setItemInHand(InteractionHand.MAIN_HAND, sonuc);
        return sonuc;
    }

    /** Vanilla yiyeceği {@code LivingEntity.completeUsingItem} gibi yer: bitiş + Forge {@code Finish} olayı. */
    private static void vanillaYe(ServerPlayer o, Item esya) {
        vanillaArtis(o, new ItemStack(esya));
    }

    /** {@link #vanillaYe} yığınla (iksir NBT'si korunur); vücuttaki anında değişimi döndürür. */
    private static double vanillaArtis(ServerPlayer o, ItemStack yigin) {
        double once = VucutSicakligi.al(o);
        ItemStack kopya = yigin.copy();
        ItemStack sonuc = yigin.finishUsingItem(o.level(), o);
        ForgeEventFactory.onItemUseFinish(o, kopya, 0, sonuc);
        return VucutSicakligi.al(o) - once;
    }

    private static boolean yakin(double a, double b) {
        return Math.abs(a - b) < EPS;
    }

    private static SureliEtki sonEtki(ServerPlayer o) {
        List<SureliEtki> etkiler = VucutSicakligi.etkiler(o);
        return etkiler.isEmpty() ? null : etkiler.get(etkiler.size() - 1);
    }

    /** §6 #3: Salep ısıtır (anında + süreli), "İçten Isınma II" 90 sn, açlık dolar, şişe geri gelir, süt efekti silmez. */
    @GameTest(template = "bos", batch = "yiyecek_salep")
    public static void salepIsitirEfektVerirSiseGeriVerir(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        ServerPlayer o = oyuncu(y, "salep-test", 0);
        try {
            EtkiAyarlari a = YiyecekAyarlari.etki();
            VucutSicakligi.ayarla(o, 0);
            o.getFoodData().setFoodLevel(10);
            ItemStack kalan = ye(o, "sicak_salep", 2);
            double vucut = VucutSicakligi.al(o);
            dogrula(y, vucut > 0 && yakin(vucut, a.orta() * a.anlikOran()), "anında ısınma: " + vucut);
            SureliEtki etki = sonEtki(o);
            dogrula(y, etki != null && yakin(etki.toplam(), a.orta() * (1 - a.anlikOran())) && etki.sure() == 90 * 20,
                    "süreli kısım orta, 90 sn olmalı: " + VucutSicakligi.etkiler(o).size());
            MobEffectInstance efekt = o.getEffect(YiyecekKayitlari.ICTEN_ISINMA.get());
            dogrula(y, efekt != null && efekt.getAmplifier() == 1 && efekt.getDuration() == 1800 && efekt.showIcon()
                    && !efekt.isVisible(), "İçten Isınma II, 90 sn, ikonlu, parçacıksız olmalı: " + efekt);
            dogrula(y, o.getEffect(YiyecekKayitlari.SERINLIK.get()) == null, "serinlik olmamalı");
            dogrula(y, kalan.is(YiyecekKayitlari.esya("sicak_salep")) && kalan.getCount() == 1, "bir salep kalmalı: " + kalan);
            dogrula(y, o.getInventory().countItem(Items.GLASS_BOTTLE) == 1, "boş şişe envantere gelmeli");
            dogrula(y, o.getFoodData().getFoodLevel() == 10 + SALEP_BESIN, "yemek değeri: "
                    + o.getFoodData().getFoodLevel());
            ItemStack son = ye(o, "sicak_salep", 1);
            dogrula(y, son.is(Items.GLASS_BOTTLE) && son.getCount() == 1, "son salep şişeye dönmeli: " + son);
            o.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
            dogrula(y, o.hasEffect(YiyecekKayitlari.ICTEN_ISINMA.get()), "süt gösterge efektini silmemeli");
            y.succeed();
        } finally {
            cikar(y, List.of(o));
        }
    }

    /** §6 #2: 60 sn içinde ikinci Salep yarı etki; farklı tür (Çikolata) tam; pencere dolunca yine tam. */
    @GameTest(template = "bos", batch = "yiyecek_azalan")
    public static void ikinciSalepYariEtkiFarkliTurBagimsiz(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        ServerPlayer o = oyuncu(y, "azalan-test", 0);
        try {
            String salep = YiyecekKayitlari.esya("sicak_salep").builtInRegistryHolder().key().location().toString();
            VucutSicakligi.ayarla(o, -60);
            double d1 = artis(o, "sicak_salep");
            double t1 = sonEtki(o).toplam();
            double d2 = artis(o, "sicak_salep");
            double t2 = sonEtki(o).toplam();
            dogrula(y, d1 > 0 && yakin(d2, d1 / 2), "ikinci salep anında yarı: " + d1 + " / " + d2);
            dogrula(y, yakin(t2, t1 / 2), "ikinci salep süreli yarı: " + t1 + " / " + t2);
            double d3 = artis(o, "sicak_cikolata");
            dogrula(y, yakin(d3, d1), "farklı tür bağımsız (tam): " + d3);
            long kalan = YiyecekAPI.kalanVerimTick(o, salep);
            dogrula(y, kalan > 0 && kalan <= 1200, "salep penceresi sürmeli: " + kalan);
            long simdi = o.serverLevel().getGameTime();
            YiyecekAPI.sonYemeAyarla(o, salep, simdi - 1199);
            dogrula(y, yakin(artis(o, "sicak_salep"), d1 / 2), "59,95 sn: hâlâ yarı");
            YiyecekAPI.sonYemeAyarla(o, salep, simdi - 1200);
            dogrula(y, yakin(artis(o, "sicak_salep"), d1), "60 sn sonra tam");
            y.succeed();
        } finally {
            cikar(y, List.of(o));
        }
    }

    private static double artis(ServerPlayer o, String ad) {
        double once = VucutSicakligi.al(o);
        ye(o, ad, 1);
        return VucutSicakligi.al(o) - once;
    }

    /** Ayran serinletir: anında −orta·pay, "Serinlik II", şişe geri. */
    @GameTest(template = "bos", batch = "yiyecek_ayran")
    public static void ayranSogutur(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        ServerPlayer o = oyuncu(y, "ayran-test", 0);
        try {
            EtkiAyarlari a = YiyecekAyarlari.etki();
            VucutSicakligi.ayarla(o, 0);
            ItemStack kalan = ye(o, "ayran", 1);
            double vucut = VucutSicakligi.al(o);
            dogrula(y, vucut < 0 && yakin(vucut, -a.orta() * a.anlikOran()), "ayran anında serinletmeli: " + vucut);
            SureliEtki etki = sonEtki(o);
            dogrula(y, etki != null && etki.toplam() < 0, "süreli serinlik olmalı");
            MobEffectInstance efekt = o.getEffect(YiyecekKayitlari.SERINLIK.get());
            dogrula(y, efekt != null && efekt.getAmplifier() == 1 && efekt.getDuration() == 1800, "Serinlik II 90 sn: " + efekt);
            dogrula(y, kalan.is(Items.GLASS_BOTTLE), "şişe geri gelmeli: " + kalan);
            ItemStack karpuz = ye(o, "buzlu_karpuz", 1);
            dogrula(y, karpuz.isEmpty(), "karpuz diliminin kabı yok: " + karpuz);
            y.succeed();
        } finally {
            cikar(y, List.of(o));
        }
    }

    /** Kapı kapalıyken (sicaklikSistemi=false) yalnız yemek değeri: sıcaklık, efekt ve azalan verim kaydı yok. */
    @GameTest(template = "bos", batch = "yiyecek_kapi")
    public static void kapiKapaliykenYalnizAclik(GameTestHelper y) {
        MinecraftServer server = y.getLevel().getServer();
        GameRules.BooleanValue kural = kuraliAc(server);
        ServerPlayer o = oyuncu(y, "kapi-test", 0);
        try {
            VucutSicakligi.ayarla(o, 0);
            o.getFoodData().setFoodLevel(10);
            ItemStack kalan;
            kural.set(false, server);
            try {
                kalan = ye(o, "sicak_salep", 1);
            } finally {
                kural.set(true, server);
            }
            dogrula(y, o.getFoodData().getFoodLevel() == 10 + SALEP_BESIN, "yemek değeri verilmeli");
            dogrula(y, VucutSicakligi.al(o) == 0, "kapı kapalıyken vücut değişti: " + VucutSicakligi.al(o));
            dogrula(y, VucutSicakligi.etkiler(o).isEmpty(), "kapı kapalıyken süreli etki kaydedildi");
            dogrula(y, o.getActiveEffects().stream().noneMatch(e -> DogalAfetler.MOD_ID.equals(
                    ForgeRegistries.MOB_EFFECTS.getKey(e.getEffect()).getNamespace())), "kapı kapalıyken efekt verildi");
            dogrula(y, YiyecekAPI.surenPencereler(o).isEmpty(), "kapı kapalıyken azalan verim kaydı yazıldı");
            dogrula(y, kalan.is(Items.GLASS_BOTTLE), "kap yine geri gelmeli");
            y.succeed();
        } finally {
            cikar(y, List.of(o));
        }
    }

    private record Ornek(String ad, int adet, List<Item> girdi, List<Item> kalan) {
    }

    /** Madde 14 tarifleri {@code RecipeManager} ile: sonuç, adet ve kalanlar (süt kovasının kovası geri gelir). */
    @GameTest(template = "bos", batch = "yiyecek_tarif")
    public static void tariflerMadde14IleBirebir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        ServerPlayer o = oyuncu(y, "tarif-test", 0);
        Item b = Items.GLASS_BOTTLE;
        try {
            List<Ornek> ornekler = List.of(
                    new Ornek("sicak_salep", 3, List.of(b, b, b, Items.MILK_BUCKET, Items.HONEY_BOTTLE),
                            List.of(Items.BUCKET, Items.GLASS_BOTTLE)),
                    new Ornek("sicak_cikolata", 3, List.of(b, b, b, Items.MILK_BUCKET, Items.COCOA_BEANS, Items.COCOA_BEANS,
                            Items.SUGAR), List.of(Items.BUCKET)),
                    new Ornek("sicak_tavuk_corbasi", 1, List.of(Items.BOWL, Items.COOKED_CHICKEN, Items.CARROT, Items.POTATO),
                            List.of()),
                    new Ornek("atesli_guvec", 1, List.of(Items.BOWL, Items.COOKED_BEEF, Items.POTATO, Items.BLAZE_POWDER),
                            List.of()),
                    new Ornek("atesli_guvec", 1, List.of(Items.BOWL, Items.COOKED_PORKCHOP, Items.POTATO,
                            Items.BLAZE_POWDER), List.of()),
                    new Ornek("isitici_cay", 2, List.of(b, Items.SWEET_BERRIES, Items.SWEET_BERRIES, Items.HONEY_BOTTLE),
                            List.of()),
                    new Ornek("ayran", 3, List.of(b, b, b, Items.MILK_BUCKET, Items.SNOWBALL, Items.SNOWBALL),
                            List.of(Items.BUCKET)),
                    new Ornek("buzlu_karpuz", 1, List.of(Items.MELON_SLICE, Items.SNOWBALL), List.of()),
                    new Ornek("meyve_serbeti", 1, List.of(b, Items.SWEET_BERRIES, Items.SWEET_BERRIES, Items.SWEET_BERRIES,
                            Items.SUGAR, Items.SNOWBALL), List.of()));
            for (Ornek ornek : ornekler) {
                tarifSina(y, level, o, ornek.ad(), YiyecekKayitlari.esya(ornek.ad()), ornek.adet(), ornek.girdi(),
                        ornek.kalan());
            }
            TransientCraftingContainer eksik = tezgah(o, List.of(b, b, b, Items.MILK_BUCKET));
            dogrula(y, level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, eksik, level).isEmpty(),
                    "balsız salep eşleşmemeli");
            kutupBaligiCorbasi(y, level, o);
            y.succeed();
        } finally {
            cikar(y, List.of(o));
        }
    }

    /** Girdiyle {@code RecipeManager} {@code ad} tarifini bulur; ürün, adet ve kalanlar beklenen gibi. */
    private static void tarifSina(GameTestHelper y, ServerLevel level, ServerPlayer o, String ad, Item urun, int adet,
            List<Item> girdi, List<Item> beklenenKalan) {
        TransientCraftingContainer tezgah = tezgah(o, girdi);
        Optional<CraftingRecipe> tarif = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, tezgah, level);
        dogrula(y, tarif.isPresent() && tarif.get().getId().equals(DogalAfetler.konum(ad)),
                ad + " tarifi eşleşmedi: " + tarif.map(Recipe::getId));
        ItemStack sonuc = tarif.get().assemble(tezgah, level.registryAccess());
        dogrula(y, sonuc.is(urun) && sonuc.getCount() == adet, ad + " sonucu: " + sonuc);
        List<Item> kalan = new ArrayList<>();
        for (ItemStack k : tarif.get().getRemainingItems(tezgah)) {
            if (!k.isEmpty()) {
                kalan.add(k.getItem());
            }
        }
        dogrula(y, sayim(kalan).equals(sayim(beklenenKalan)), ad + " kalanları: " + kalan);
    }

    /**
     * Sıcaklık-3 tarifleri (S3-06) ve Buz Kovası (S3-05) {@code RecipeManager} ile: her yeni yiyeceğin örneği var; süt
     * kovasının kovası ve bal şişesinin şişesi geri gelir; kova + buz tek, yığılmayan Buz Kovası verir (kova tükenir).
     */
    @GameTest(template = "bos", batch = "yiyecek_yeni_tarif")
    public static void yeniTariflerVeBuzKovasiTarifi(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        ServerPlayer o = oyuncu(y, "yeni-tarif-test", 0);
        Item b = Items.GLASS_BOTTLE;
        Item k = Items.BOWL;
        try {
            List<Ornek> ornekler = List.of(
                    new Ornek("maras_dondurmasi", 2, List.of(k, k, Items.MILK_BUCKET, Items.SUGAR, Items.ICE),
                            List.of(Items.BUCKET)),
                    new Ornek("soguk_elma_suyu", 2, List.of(b, b, Items.APPLE, Items.APPLE, Items.SNOWBALL), List.of()),
                    new Ornek("kar_helvasi", 1, List.of(k, Items.SNOWBALL, Items.SNOWBALL, Items.HONEY_BOTTLE),
                            List.of(Items.GLASS_BOTTLE)),
                    new Ornek("soguk_komposto", 1, List.of(b, Items.APPLE, Items.SWEET_BERRIES, Items.SUGAR, Items.SNOWBALL),
                            List.of()),
                    new Ornek("parlak_sorbe", 1, List.of(k, Items.GLOW_BERRIES, Items.GLOW_BERRIES, Items.PACKED_ICE,
                            Items.SUGAR), List.of()),
                    new Ornek("buzlu_kakao", 3, List.of(b, b, b, Items.MILK_BUCKET, Items.COCOA_BEANS, Items.COCOA_BEANS,
                            Items.ICE), List.of(Items.BUCKET)),
                    new Ornek("tarhana_corbasi", 3, List.of(k, k, k, Items.MILK_BUCKET, Items.WHEAT, Items.WHEAT,
                            Items.BEETROOT), List.of(Items.BUCKET)),
                    new Ornek("kuzu_haslama", 1, List.of(k, Items.COOKED_MUTTON, Items.POTATO, Items.CARROT), List.of()),
                    new Ornek("kabak_corbasi", 2, List.of(k, k, Items.PUMPKIN, Items.CARROT), List.of()),
                    new Ornek("sucuklu_yumurta", 1, List.of(k, Items.EGG, Items.EGG, Items.COOKED_BEEF), List.of()),
                    new Ornek("firinda_elma", 1, List.of(Items.APPLE, Items.SUGAR, Items.HONEY_BOTTLE),
                            List.of(Items.GLASS_BOTTLE)),
                    new Ornek("elma_cayi", 2, List.of(b, b, Items.APPLE, Items.SUGAR), List.of()));
            List<String> beklenen = new ArrayList<>();
            for (YiyecekTablosu.Kayit kayit : yeniYiyecekler()) {
                beklenen.add(kayit.ad());
            }
            dogrula(y, ornekler.stream().map(Ornek::ad).toList().equals(beklenen), "her yeni yiyeceğin tarif örneği olmalı");
            for (Ornek ornek : ornekler) {
                tarifSina(y, level, o, ornek.ad(), YiyecekKayitlari.esya(ornek.ad()), ornek.adet(), ornek.girdi(),
                        ornek.kalan());
            }
            tarifSina(y, level, o, "buz_kovasi", YiyecekKayitlari.BUZ_KOVASI.get(), 1, List.of(Items.BUCKET, Items.ICE),
                    List.of());
            dogrula(y, new ItemStack(YiyecekKayitlari.BUZ_KOVASI.get()).getMaxStackSize() == 1, "Buz Kovası yığılmamalı");
            y.succeed();
        } finally {
            cikar(y, List.of(o));
        }
    }

    /** Sıcaklık-3'ün yiyecekleri: tabloda madde 14'ün dokuzundan sonrakiler. */
    private static List<YiyecekTablosu.Kayit> yeniYiyecekler() {
        return YiyecekTablosu.KAYITLAR.subList(9, YiyecekTablosu.KAYITLAR.size());
    }

    /**
     * S3-06: her yeni yiyecek gerçek yeme yoluyla doğru yönde anında etki (± kademe · pay), kendi yönünün gösterge efekti
     * (amplifier = kademe seviyesi, süre = etki süresi; karşı yön yok), yemek değeri ve boş kap verir; serinleten ve ısıtan
     * yiyecekler dengeli.
     */
    @GameTest(template = "bos", batch = "yiyecek_yeni")
    public static void yeniYiyeceklerDogruYondeEtkiEfektVeKapVerir(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        ServerPlayer o = oyuncu(y, "yeni-yiyecek-test", 0);
        try {
            EtkiAyarlari a = YiyecekAyarlari.etki();
            int isinma = 0;
            int serinlik = 0;
            for (YiyecekTablosu.Kayit kayit : yeniYiyecekler()) {
                YiyecekTanimi t = kayit.tanim();
                o.removeAllEffects();
                VucutSicakligi.ayarla(o, 0);
                o.getFoodData().setFoodLevel(0);
                ItemStack kalan = ye(o, kayit.ad(), 1);
                double vucut = VucutSicakligi.al(o);
                double beklenen = a.miktar(t.buyukluk()) * a.anlikOran() * t.yon().isaret();
                dogrula(y, Math.signum(vucut) == t.yon().isaret() && yakin(vucut, beklenen),
                        kayit.ad() + " anında etki " + vucut + ", beklenen " + beklenen);
                MobEffectInstance efekt = o.getEffect(YiyecekKayitlari.efekt(t.yon()));
                dogrula(y, efekt != null && efekt.getAmplifier() == t.buyukluk().seviye()
                        && efekt.getDuration() == t.sureTick(), kayit.ad() + " gösterge efekti: " + efekt);
                MobEffect karsi = YiyecekKayitlari.efekt(t.yon() == Yon.ISINMA ? Yon.SERINLIK : Yon.ISINMA);
                dogrula(y, o.getEffect(karsi) == null, kayit.ad() + " karşı yön efekti vermemeli");
                dogrula(y, o.getFoodData().getFoodLevel() == kayit.besin(), kayit.ad() + " yemek değeri: "
                        + o.getFoodData().getFoodLevel());
                boolean kapDogru = switch (kayit.kap()) {
                    case SISE -> kalan.is(Items.GLASS_BOTTLE) && kalan.getCount() == 1;
                    case KASE -> kalan.is(Items.BOWL) && kalan.getCount() == 1;
                    case YOK -> kalan.isEmpty();
                };
                dogrula(y, kapDogru, kayit.ad() + " kabı: " + kalan);
                if (t.yon() == Yon.ISINMA) {
                    isinma++;
                } else {
                    serinlik++;
                }
            }
            dogrula(y, isinma + serinlik >= 10 && isinma >= 5 && serinlik >= 5,
                    "en az 10 yeni yiyecek, sıcak/soğuk dengeli: " + isinma + "/" + serinlik);
            y.succeed();
        } finally {
            cikar(y, List.of(o));
        }
    }

    /**
     * S3-06 vanilla serinlik: karpuz dilimi çok küçük serinletir ("Serinlik I", 30 sn), su şişesi ve süt kovası küçük
     * serinletir (sütün efekt temizlemesi gösterge efektini silmez), aynı eşya kimlikli başka iksir (hız) serinletmez;
     * {@code vanillaSerinlik} kapalıyken serinlik yok (config dosyasına yazılmadan, K-E28).
     */
    @GameTest(template = "bos", batch = "yiyecek_vanilla_kademe")
    public static void vanillaSoguklarSerinletirBaskaIksirSerinletmez(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        ServerPlayer o = oyuncu(y, "vanilla-serin-test", 0);
        try {
            EtkiAyarlari a = YiyecekAyarlari.etki();
            VucutSicakligi.ayarla(o, 0);
            double karpuz = vanillaArtis(o, new ItemStack(Items.MELON_SLICE));
            dogrula(y, karpuz < 0 && yakin(karpuz, -a.cokKucuk() * a.anlikOran()), "karpuz serinletmeli: " + karpuz);
            MobEffectInstance efekt = o.getEffect(YiyecekKayitlari.SERINLIK.get());
            dogrula(y, efekt != null && efekt.getAmplifier() == 0 && efekt.getDuration() == 30 * 20, "Serinlik I 30 sn: "
                    + efekt);
            dogrula(y, o.getEffect(YiyecekKayitlari.ICTEN_ISINMA.get()) == null, "karpuz ısıtmamalı");
            double su = vanillaArtis(o, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
            dogrula(y, yakin(su, -a.kucuk() * a.anlikOran()), "su şişesi küçük serinletmeli: " + su);
            double hiz = vanillaArtis(o, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS));
            dogrula(y, hiz == 0, "hız iksiri serinletmemeli: " + hiz);
            double sut = vanillaArtis(o, new ItemStack(Items.MILK_BUCKET));
            dogrula(y, yakin(sut, -a.kucuk() * a.anlikOran()), "süt kovası küçük serinletmeli: " + sut);
            dogrula(y, o.hasEffect(YiyecekKayitlari.SERINLIK.get()), "sütün temizlemesinden sonra Serinlik görünmeli");
            YiyecekAyarlari.vanillaSerinlikTesti(false);
            double kapali = vanillaArtis(o, new ItemStack(Items.SWEET_BERRIES));
            dogrula(y, kapali == 0, "vanillaSerinlik=false iken serinletmemeli: " + kapali);
            y.succeed();
        } finally {
            YiyecekAyarlari.vanillaSerinlikTesti(null);
            cikar(y, List.of(o));
        }
    }

    /** S3-06 kademeli vanilla ısınma: tavşan yahnisi (küçük, 90 sn) pişmiş sığırdan (çok küçük, 45 sn) fazla ve uzun ısıtır. */
    @GameTest(template = "bos", batch = "yiyecek_vanilla_kademe")
    public static void kademeliVanillaIsinmaYahniEttenGuclu(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        ServerPlayer et = oyuncu(y, "vanilla-et-test", 0);
        ServerPlayer yahni = oyuncu(y, "vanilla-yahni-test", 1);
        try {
            EtkiAyarlari a = YiyecekAyarlari.etki();
            VucutSicakligi.ayarla(et, 0);
            VucutSicakligi.ayarla(yahni, 0);
            double etArtis = vanillaArtis(et, new ItemStack(Items.COOKED_BEEF));
            double yahniArtis = vanillaArtis(yahni, new ItemStack(Items.RABBIT_STEW));
            dogrula(y, yakin(etArtis, a.cokKucuk() * a.anlikOran()), "pişmiş sığır çok küçük: " + etArtis);
            dogrula(y, yakin(yahniArtis, a.kucuk() * a.anlikOran()) && yahniArtis > etArtis,
                    "yahni etten fazla ısıtmalı: " + yahniArtis + " / " + etArtis);
            SureliEtki etSureli = sonEtki(et);
            SureliEtki yahniSureli = sonEtki(yahni);
            dogrula(y, etSureli != null && yahniSureli != null && etSureli.sure() == 45 * 20 && yahniSureli.sure() == 90 * 20
                    && yahniSureli.toplam() > etSureli.toplam(), "yahninin süreli kısmı daha uzun ve büyük olmalı");
            y.succeed();
        } finally {
            cikar(y, List.of(et, yahni));
        }
    }

    /**
     * Kutup Balığı Çorbası malzemesi {@code #dogalafetler:pismis_kutup_baligi} etiketidir (balıkçılık doldurur) ve tarif
     * {@code forge:not(forge:tag_empty)} koşuluyla yüklenir (F-17). Etiket boşken tarif yoktur ve boş etiketin "Empty Tag"
     * bariyeri eşleşmez; doluysa (entegrasyon) tarif etiketlidir ve ilk balıkla gerçekten pişirilir.
     */
    private static void kutupBaligiCorbasi(GameTestHelper y, ServerLevel level, ServerPlayer o) {
        Optional<? extends Recipe<?>> tarif = level.getRecipeManager().byKey(DogalAfetler.konum("kutup_baligi_corbasi"));
        Optional<Item> balik = BuiltInRegistries.ITEM.getTag(YiyecekKayitlari.PISMIS_KUTUP_BALIGI)
                .flatMap(etiket -> etiket.stream().findFirst()).map(h -> h.value());
        if (balik.isEmpty()) {
            dogrula(y, tarif.isEmpty(), "etiket boşken kutup balığı çorbası tarifi yüklenmemeli");
            TransientCraftingContainer bariyer = tezgah(o, List.of(Items.BOWL, Items.BARRIER, Items.POTATO));
            dogrula(y, level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, bariyer, level).isEmpty(),
                    "kase + bariyer + patates eşleşmemeli");
        } else {
            dogrula(y, tarif.isPresent(), "etiket doluyken kutup balığı çorbası tarifi yüklenmeli");
            boolean etiketli = false;
            for (Ingredient malzeme : tarif.get().getIngredients()) {
                etiketli |= malzeme.toJson().toString().contains(YiyecekKayitlari.PISMIS_KUTUP_BALIGI.location().toString());
            }
            dogrula(y, etiketli, "malzeme #pismis_kutup_baligi etiketi olmalı");
            TransientCraftingContainer tezgah = tezgah(o, List.of(Items.BOWL, balik.get(), Items.POTATO));
            Optional<CraftingRecipe> e = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, tezgah, level);
            dogrula(y, e.isPresent() && e.get().assemble(tezgah, level.registryAccess())
                    .is(YiyecekKayitlari.esya("kutup_baligi_corbasi")), "kutup balığı çorbası pişmedi");
        }
        DogalAfetler.LOGGER.info("Yiyecek GameTest: kutup balığı çorbası tarifi {}, etikette balık {}",
                tarif.isPresent() ? "yüklü" : "yok (koşullu, etiket boş)",
                balik.map(i -> BuiltInRegistries.ITEM.getKey(i).toString()).orElse("yok (balıkçılık dalında dolar)"));
    }

    private static TransientCraftingContainer tezgah(ServerPlayer o, List<Item> girdi) {
        TransientCraftingContainer tezgah = new TransientCraftingContainer(new CraftingMenu(0, o.getInventory()), 3, 3);
        for (int i = 0; i < girdi.size(); i++) {
            tezgah.setItem(i, new ItemStack(girdi.get(i)));
        }
        return tezgah;
    }

    private static Map<Item, Integer> sayim(List<Item> esyalar) {
        Map<Item, Integer> sayim = new HashMap<>();
        for (Item e : esyalar) {
            sayim.merge(e, 1, Integer::sum);
        }
        return sayim;
    }

    /** Ateşli Güveç sıcak ortamda (yan etki) daha güçlü ısıtır ve oyuncuyu aşırı ısınmaya iter; yemeyen oyuncu itilmez. */
    @GameTest(template = "bos", batch = "yiyecek_guvec")
    public static void guvecSicaktaAsiriIsinmayaIter(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        kuraliAc(level.getServer());
        Runnable donemiGeriKoy = notrDonem(level.getServer());
        ServerPlayer kontrol = oyuncu(y, "guvec-kontrol", 0);
        ServerPlayer yiyen = oyuncu(y, "guvec-yiyen", 1);
        try {
            EtkiAyarlari a = YiyecekAyarlari.etki();
            Esikler e = SicaklikAyarlari.al().esikler();
            // Geçici kaynak 2 tick yaşar: bu test aynı tick'te biter, sonraki batch'lere ısı bırakmaz.
            SicaklikKancalari.ortamKaynagiEkle(level, Vec3.atCenterOf(y.absolutePos(BlockPos.ZERO)).add(0.5, 0, 0), 45, 10, 2);
            double ortam = SicaklikYoneticisi.analiz(yiyen).ortamBirim();
            dogrula(y, ortam >= a.yanEtkiEsigi(), "sahne sıcak olmalı: ortam " + ortam);
            // Bunalma bandının üst ucu: anlık etki yanmaya taşır, kontrolün hasar eşiğine payı eski testteki gibi 25 birim.
            double bas = e.cokSicak() - 5;
            VucutSicakligi.ayarla(kontrol, bas);
            VucutSicakligi.ayarla(yiyen, bas);
            Kademe once = e.kademe(bas);
            dogrula(y, once == Kademe.SICAK, "başlangıç bunalmada olmalı: " + once);
            ye(yiyen, "atesli_guvec", 1);
            double anlik = VucutSicakligi.al(yiyen) - bas;
            dogrula(y, yakin(anlik, a.cokBuyuk() * a.yanEtkiCarpani() * a.anlikOran()),
                    "sıcakta yan etki çarpanı uygulanmalı: " + anlik);
            Kademe hemen = e.kademe(VucutSicakligi.al(yiyen));
            dogrula(y, hemen.ordinal() > once.ordinal(), "kademe hemen yükselmeli: " + once + " -> " + hemen);
            MobEffectInstance efekt = yiyen.getEffect(YiyecekKayitlari.ICTEN_ISINMA.get());
            dogrula(y, efekt != null && efekt.getAmplifier() == 3 && efekt.getDuration() == 180 * 20, "İçten Isınma IV 180 sn: "
                    + efekt);
            for (int i = 0; i < 30; i++) {
                SicaklikYoneticisi.guncelle(kontrol, 20);
                SicaklikYoneticisi.guncelle(yiyen, 20);
            }
            Kademe yiyenKademe = e.kademe(VucutSicakligi.al(yiyen));
            Kademe kontrolKademe = e.kademe(VucutSicakligi.al(kontrol));
            dogrula(y, yiyenKademe == Kademe.YANIYOR, "güveç 30 sn'de aşırı ısınmaya itmeli: " + VucutSicakligi.al(yiyen));
            dogrula(y, kontrolKademe.ordinal() < Kademe.YANIYOR.ordinal(), "yemeyen yanmamalı: " + VucutSicakligi.al(kontrol));
            double yiyenSon = VucutSicakligi.al(yiyen);
            DogalAfetler.LOGGER.info(String.format(Locale.ROOT,
                    "Yiyecek GameTest: güveç sıcakta ortam=%.1f anında=%.2f 30 sn sonra yiyen=%.1f (%s, eşik %.0f, pay %+.1f)"
                            + " kontrol=%.1f (%s)", ortam, anlik, yiyenSon, yiyenKademe, e.yanmaHasari(),
                    yiyenSon - e.yanmaHasari(), VucutSicakligi.al(kontrol), kontrolKademe));
            y.succeed();
        } finally {
            cikar(y, List.of(kontrol, yiyen));
            donemiGeriKoy.run();
        }
    }

    /**
     * Vanilla pişmiş yiyecek (kızarmış patates) çok küçük, kısa ısınma verir; çiğ patates vermez; {@code vanillaIsinma}
     * kapalıyken ısıtmaz. Kapalı yol config dosyasına yazmadan sınanır (K-E28).
     */
    @GameTest(template = "bos", batch = "yiyecek_vanilla")
    public static void vanillaPismisYiyecekKucukIsinma(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        ServerPlayer o = oyuncu(y, "vanilla-test", 0);
        try {
            EtkiAyarlari a = YiyecekAyarlari.etki();
            VucutSicakligi.ayarla(o, 0);
            o.getFoodData().setFoodLevel(2);
            vanillaYe(o, Items.BAKED_POTATO);
            double vucut = VucutSicakligi.al(o);
            dogrula(y, yakin(vucut, a.cokKucuk() * a.anlikOran()), "kızarmış patates çok küçük ısıtmalı: " + vucut);
            SureliEtki etki = sonEtki(o);
            dogrula(y, etki != null && etki.sure() == 30 * 20 && yakin(etki.toplam(), a.cokKucuk() * (1 - a.anlikOran())),
                    "kısa süreli kısım 30 sn olmalı");
            MobEffectInstance efekt = o.getEffect(YiyecekKayitlari.ICTEN_ISINMA.get());
            dogrula(y, efekt != null && efekt.getAmplifier() == 0 && efekt.getDuration() == 600, "İçten Isınma I 30 sn: " + efekt);
            vanillaYe(o, Items.POTATO);
            dogrula(y, VucutSicakligi.al(o) == vucut, "çiğ patates ısıtmamalı");
            YiyecekAyarlari.vanillaIsinmaTesti(false);
            vanillaYe(o, Items.COOKED_BEEF);
            dogrula(y, VucutSicakligi.al(o) == vucut, "vanillaIsinma=false iken ısıtmamalı");
            y.succeed();
        } finally {
            YiyecekAyarlari.vanillaIsinmaTesti(null);
            cikar(y, List.of(o));
        }
    }

    /** {@link YiyecekYendiEvent}: donarken sıcak yiyecek ("Çorba İçer Isınır" kanca) önceki ve sonraki kademeyi taşır. */
    @GameTest(template = "bos", batch = "yiyecek_olay")
    public static void donarkenSicakYiyecekOlayiKademeleriTasir(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        ServerPlayer o = oyuncu(y, "olay-test", 0);
        List<YiyecekYendiEvent> gelen = new ArrayList<>();
        Consumer<YiyecekYendiEvent> dinleyici = olay -> {
            if (olay.oyuncu() == o) {
                gelen.add(olay);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, YiyecekYendiEvent.class, dinleyici);
        try {
            VucutSicakligi.ayarla(o, -80);
            ye(o, "atesli_guvec", 1);
            dogrula(y, gelen.size() == 1, "tek olay gelmeli: " + gelen.size());
            YiyecekYendiEvent olay = gelen.get(0);
            dogrula(y, olay.tanim().tur().equals("dogalafetler:atesli_guvec") && olay.uygulandi()
                    && olay.verimCarpani() == 1.0, "olay içeriği: " + olay.tanim());
            dogrula(y, olay.oncekiKademe() == Kademe.DONUYOR && olay.sonrakiKademe() == Kademe.COK_SOGUK,
                    "kademeler: " + olay.oncekiKademe() + " -> " + olay.sonrakiKademe());
            y.succeed();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(dinleyici);
            cikar(y, List.of(o));
        }
    }

    /**
     * F-18: Cold Sweat yüklüyken (kendi sistemimiz OTOMATIK kipte kapalı) sıcak yiyecek Cold Sweat çekirdek sıcaklığını etkinin
     * toplamı kadar artırır, bizim vücut sıcaklığımız değişmez. Cold Sweat yüklü değilse (varsayılan GameTest) sınanacak yol
     * yoktur: test bunu log'a yazıp geçer; kanıt {@code -Puyum=coldsweat} koşusundadır. GameTest kapıyı Cold Sweat'siz saydığı için (K-E41)
 * test gövdesinde gerçek değere geçer, sonra öncekini geri koyar.
     */
    @GameTest(template = "bos", batch = "yiyecek_coldsweat")
    public static void coldSweatYukluykenYiyecekCekirdegeGider(GameTestHelper y) {
        Boolean oncekiKapi = SicaklikKapisi.coldSweatTesti(null);
        if (!SicaklikKapisi.coldSweatYuklu()) {
            SicaklikKapisi.coldSweatTesti(oncekiKapi);
            DogalAfetler.LOGGER.info("Yiyecek GameTest: Cold Sweat yüklü değil, F-18 köprüsü sınanmadı (-Puyum=coldsweat ister)");
            y.succeed();
            return;
        }
        kuraliAc(y.getLevel().getServer());
        ServerPlayer o = oyuncu(y, "coldsweat-test", 0);
        try {
            dogrula(y, !SicaklikKapisi.acikMi(y.getLevel().getServer()), "Cold Sweat yüklüyken kendi sistemimiz kapalı olmalı");
            ColdSweatKoprusu.yiyecekEtkisi(o, -ColdSweatKoprusu.cekirdek(o));
            double vucut = VucutSicakligi.al(o);
            List<YiyecekYendiEvent> gelen = new ArrayList<>();
            Consumer<YiyecekYendiEvent> dinleyici = olay -> {
                if (olay.oyuncu() == o) {
                    gelen.add(olay);
                }
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, YiyecekYendiEvent.class, dinleyici);
            try {
                ye(o, "atesli_guvec", 1);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(dinleyici);
            }
            dogrula(y, gelen.size() == 1, "tek yeme olayı gelmeli: " + gelen.size());
            double beklenen = gelen.get(0).etki().anlik() + gelen.get(0).etki().sureli();
            double cekirdek = ColdSweatKoprusu.cekirdek(o);
            dogrula(y, gelen.get(0).uygulandi() && beklenen > 0 && Math.abs(cekirdek - beklenen) < 1e-6,
                    "Cold Sweat çekirdeği " + cekirdek + ", beklenen " + beklenen);
            dogrula(y, VucutSicakligi.al(o) == vucut, "bizim vücut sıcaklığımız değişmemeli");
            DogalAfetler.LOGGER.info("Yiyecek GameTest: F-18 Cold Sweat çekirdeği 0 -> {} (etki {})", cekirdek, beklenen);
            y.succeed();
        } finally {
            SicaklikKapisi.coldSweatTesti(oncekiKapi);
            cikar(y, List.of(o));
        }
    }

    /** Azalan verim geçmişi ölümsüz kopyada (End dönüşü) taşınır, ölümde yeni hayat temiz başlar. */
    @GameTest(template = "bos", batch = "yiyecek_kopya")
    public static void gecmisKopyadaTasinirOlumdeSifirlanir(GameTestHelper y) {
        MinecraftServer server = y.getLevel().getServer();
        kuraliAc(server);
        List<ServerPlayer> izlenen = new ArrayList<>();
        try {
            ServerPlayer donen = oyuncu(y, "kopya-test", 0);
            izlenen.add(donen);
            ye(donen, "ayran", 1);
            ServerPlayer kopya = server.getPlayerList().respawn(donen, true);
            izlenen.add(kopya);
            dogrula(y, YiyecekAPI.kalanVerimTick(kopya, "dogalafetler:ayran") > 0, "ölümsüz kopyada geçmiş taşınmalı");
            ServerPlayer olen = oyuncu(y, "olum-test", 1);
            izlenen.add(olen);
            ye(olen, "ayran", 1);
            ServerPlayer yeni = server.getPlayerList().respawn(olen, false);
            izlenen.add(yeni);
            dogrula(y, YiyecekAPI.kalanVerimTick(yeni, "dogalafetler:ayran") == 0, "ölümde geçmiş sıfırlanmalı");
            y.succeed();
        } finally {
            cikar(y, izlenen);
        }
    }
}
