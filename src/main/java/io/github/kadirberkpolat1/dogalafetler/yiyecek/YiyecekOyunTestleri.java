package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import com.mojang.authlib.GameProfile;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKancalari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.VucutSicakligi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Esikler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SureliEtki;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.EtkiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTablosu;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

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
        ItemStack yigin = new ItemStack(esya);
        ItemStack kopya = yigin.copy();
        ItemStack sonuc = yigin.finishUsingItem(o.level(), o);
        ForgeEventFactory.onItemUseFinish(o, kopya, 0, sonuc);
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
            dogrula(y, o.getActiveEffects().isEmpty(), "kapı kapalıyken efekt verildi");
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
                TransientCraftingContainer tezgah = tezgah(o, ornek.girdi());
                Optional<CraftingRecipe> tarif = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, tezgah, level);
                dogrula(y, tarif.isPresent() && tarif.get().getId().equals(DogalAfetler.konum(ornek.ad())),
                        ornek.ad() + " tarifi eşleşmedi: " + tarif.map(Recipe::getId));
                ItemStack sonuc = tarif.get().assemble(tezgah, level.registryAccess());
                dogrula(y, sonuc.is(YiyecekKayitlari.esya(ornek.ad())) && sonuc.getCount() == ornek.adet(),
                        ornek.ad() + " sonucu: " + sonuc);
                List<Item> kalan = new ArrayList<>();
                for (ItemStack k : tarif.get().getRemainingItems(tezgah)) {
                    if (!k.isEmpty()) {
                        kalan.add(k.getItem());
                    }
                }
                dogrula(y, sayim(kalan).equals(sayim(ornek.kalan())), ornek.ad() + " kalanları: " + kalan);
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
        ServerPlayer kontrol = oyuncu(y, "guvec-kontrol", 0);
        ServerPlayer yiyen = oyuncu(y, "guvec-yiyen", 1);
        try {
            EtkiAyarlari a = YiyecekAyarlari.etki();
            Esikler e = SicaklikAyarlari.al().esikler();
            // Geçici kaynak 2 tick yaşar: bu test aynı tick'te biter, sonraki batch'lere ısı bırakmaz.
            SicaklikKancalari.ortamKaynagiEkle(level, Vec3.atCenterOf(y.absolutePos(BlockPos.ZERO)).add(0.5, 0, 0), 45, 10, 2);
            double ortam = SicaklikYoneticisi.analiz(yiyen).ortamBirim();
            dogrula(y, ortam >= a.yanEtkiEsigi(), "sahne sıcak olmalı: ortam " + ortam);
            VucutSicakligi.ayarla(kontrol, 50);
            VucutSicakligi.ayarla(yiyen, 50);
            Kademe once = e.kademe(50);
            ye(yiyen, "atesli_guvec", 1);
            double anlik = VucutSicakligi.al(yiyen) - 50;
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
        }
    }

    /** Vanilla pişmiş yiyecek (kızarmış patates) çok küçük, kısa ısınma verir; çiğ patates vermez; config kapatır. */
    @GameTest(template = "bos", batch = "yiyecek_vanilla")
    public static void vanillaPismisYiyecekKucukIsinma(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        ServerPlayer o = oyuncu(y, "vanilla-test", 0);
        ForgeConfigSpec.BooleanValue ayar = YiyecekAyarlari.bolum().vanillaIsinma;
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
            ayar.set(false);
            vanillaYe(o, Items.COOKED_BEEF);
            dogrula(y, VucutSicakligi.al(o) == vucut, "vanillaIsinma=false iken ısıtmamalı");
            y.succeed();
        } finally {
            ayar.set(true);
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
