package io.github.kadirberkpolat1.dogalafetler.meteor;

import com.mojang.authlib.GameProfile;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.esya.termostat.Termostat;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZamani;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKancalari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.VucutSicakligi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.ZirhEkleri;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Yalitim;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Meteor ve termostatın oyun içi testleri (GOREV §6 #2, #3). Sahne {@code meteor_sahne} (15x12x15 hava) kutusuna her test
 * kendisi kurulur: y 1–5 toprak, krater merkezi (7, 5, 7). Ödül zarı testte sabitlenir ({@link OdulZari.Sanslar}).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MeteorOyunTestleri {
    private static final String SAHNE = "meteor_sahne";
    private static final BlockPos MERKEZ = new BlockPos(7, 5, 7);
    private static final OdulZari.Sanslar HIC = new OdulZari.Sanslar(0, 0, 0);
    private static final OdulZari.Sanslar HEP = new OdulZari.Sanslar(1, 1, 1);

    private MeteorOyunTestleri() {
    }

    private static void dogrula(GameTestHelper y, boolean kosul, String mesaj) {
        if (!kosul) {
            y.fail(mesaj);
        }
    }

    /** Taban: y 1–5 toprak (tüm kutu), üstü hava. */
    private static void zemin(GameTestHelper y, Block blok) {
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 15; z++) {
                for (int k = 1; k <= 5; k++) {
                    y.setBlock(new BlockPos(x, k, z), blok);
                }
            }
        }
    }

    private static GameRules.BooleanValue blokHasari(GameTestHelper y, boolean deger) {
        GameRules.BooleanValue kural = y.getLevel().getGameRules().getRule(Kurallar.AFET_BLOK_HASARI);
        kural.set(deger, y.getLevel().getServer());
        return kural;
    }

    private static AABB kutu(GameTestHelper y) {
        return new AABB(y.absolutePos(BlockPos.ZERO), y.absolutePos(new BlockPos(15, 12, 15)));
    }

    /** Doğrudan çarpma (uçuşsuz): krater işi döner. */
    private static KraterIsi carp(GameTestHelper y, MeteorBoyutu boyut, YanginKaynagi kaynak, OdulZari.Sanslar sanslar) {
        BlockPos zemin = y.absolutePos(MERKEZ);
        return MeteorCarpmasi.carp(y.getLevel(), zemin, Vec3.atBottomCenterOf(zemin.above()), boyut, MeteorGorunumu.GUNES,
                kaynak, false, sanslar, false);
    }

    private static List<ItemEntity> esyalar(GameTestHelper y) {
        return y.getLevel().getEntitiesOfClass(ItemEntity.class, kutu(y));
    }

    private static void esyalariTemizle(GameTestHelper y) {
        for (ItemEntity e : esyalar(y)) {
            e.discard();
        }
    }

    @GameTest(template = SAHNE, batch = "meteor_dusur", timeoutTicks = 300)
    public static void dusenMeteorKraterAcarSertBlokKorunur(GameTestHelper y) {
        blokHasari(y, true);
        zemin(y, Blocks.DIRT);
        BlockPos sert = MERKEZ.offset(1, 0, 0);
        y.setBlock(sert, Blocks.OBSIDIAN);
        y.setBlock(MERKEZ.offset(0, -1, 1), Blocks.BEDROCK);
        ServerLevel level = y.getLevel();
        Optional<MeteorVarligi> m = MeteorAPI.dusur(level, y.absolutePos(MERKEZ), MeteorBoyutu.BUYUK, MeteorGorunumu.GUNES,
                YanginKaynagi.KOMUT, false, 90, 0, HIC);
        dogrula(y, m.isPresent(), "meteor varlığı oluşmalı");
        dogrula(y, m.get().getY() > y.absolutePos(MERKEZ).getY() + 100, "meteor yukarıdan başlamalı: " + m.get().getY());
        y.succeedWhen(() -> {
            y.assertTrue(m.get().isRemoved(), "meteor henüz çarpmadı");
            y.assertTrue(MeteorAPI.bekleyenKraterSayisi(level) == 0, "krater işi sürüyor");
            y.assertBlockPresent(Blocks.AIR, MERKEZ);
            y.assertBlockPresent(Blocks.AIR, MERKEZ.below(2));
            y.assertBlockPresent(Blocks.AIR, MERKEZ.offset(3, 0, 0));
            y.assertBlockPresent(Blocks.OBSIDIAN, sert);
            y.assertBlockPresent(Blocks.BEDROCK, MERKEZ.offset(0, -1, 1));
            y.assertBlockPresent(Blocks.MAGMA_BLOCK, MERKEZ.below(4));
            y.assertBlockPresent(Blocks.DIRT, MERKEZ.offset(-7, 0, 0));
            y.assertTrue(esyalar(y).isEmpty(), "zar 0'da ödül çıkmamalı");
        });
    }

    @GameTest(template = SAHNE, batch = "meteor_kural", timeoutTicks = 200)
    public static void blokHasariKapaliykenKraterYokHashAyni(GameTestHelper y) {
        GameRules.BooleanValue kural = blokHasari(y, false);
        zemin(y, Blocks.DIRT);
        y.setBlock(MERKEZ.above(), Blocks.TALL_GRASS);
        BlockPos a = y.absolutePos(BlockPos.ZERO);
        BlockPos b = y.absolutePos(new BlockPos(14, 11, 14));
        long once = SanalSu.bolgeHash(y.getLevel(), a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
        KraterIsi is = carp(y, MeteorBoyutu.BUYUK, YanginKaynagi.KOMUT, HIC);
        y.succeedWhen(() -> {
            y.assertTrue(is.bitti(), "krater işi bitmedi");
            kural.set(true, y.getLevel().getServer());
            long sonra = SanalSu.bolgeHash(y.getLevel(), a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
            y.assertTrue(once == sonra, "afetBlokHasari=false iken blok değişmemeli");
            y.assertTrue(is.kazilan() == 0, "kazılan " + is.kazilan());
        });
    }

    @GameTest(template = SAHNE, batch = "meteor_odul_kucuk", timeoutTicks = 200)
    public static void kucukMeteorKozmikCekirdekVermez(GameTestHelper y) {
        blokHasari(y, true);
        zemin(y, Blocks.STONE);
        KraterIsi is = carp(y, MeteorBoyutu.KUCUK, YanginKaynagi.KOMUT, HEP);
        dogrula(y, is.odul().parca() && !is.odul().cekirdek(), "küçük meteor zar 1'de yalnız parça: " + is.odul());
        y.succeedWhen(() -> {
            y.assertTrue(is.bitti(), "krater işi bitmedi");
            List<ItemEntity> e = esyalar(y);
            y.assertTrue(e.stream().anyMatch(i -> i.getItem().is(MeteorEsyaKayitlari.METEORIT_PARCASI.get())),
                    "Meteorit Parçası düşmeli");
            y.assertTrue(e.stream().noneMatch(i -> i.getItem().is(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get())),
                    "küçük meteor Kozmik Çekirdek vermemeli");
            esyalariTemizle(y);
        });
    }

    @GameTest(template = SAHNE, batch = "meteor_odul_buyuk", timeoutTicks = 200)
    public static void buyukMeteorZarBirdeIkisiniDeVerir(GameTestHelper y) {
        blokHasari(y, true);
        zemin(y, Blocks.STONE);
        KraterIsi is = carp(y, MeteorBoyutu.BUYUK, YanginKaynagi.KOMUT, HEP);
        y.succeedWhen(() -> {
            y.assertTrue(is.bitti(), "krater işi bitmedi");
            List<ItemEntity> e = esyalar(y);
            Optional<ItemEntity> cekirdek = e.stream()
                    .filter(i -> i.getItem().is(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get())).findFirst();
            y.assertTrue(e.stream().anyMatch(i -> i.getItem().is(MeteorEsyaKayitlari.METEORIT_PARCASI.get())),
                    "Meteorit Parçası düşmeli");
            y.assertTrue(cekirdek.isPresent(), "Kozmik Çekirdek düşmeli");
            BlockPos m = y.absolutePos(MERKEZ);
            y.assertTrue(Math.abs(cekirdek.get().getX() - (m.getX() + 0.5)) < 1.0
                    && Math.abs(cekirdek.get().getZ() - (m.getZ() + 0.5)) < 1.0, "çekirdek krater ortasında");
            y.assertTrue(cekirdek.get().getItem().hasFoil(), "çekirdek büyü parlamalı");
            y.assertTrue(cekirdek.get().lifespan == OdulZamani.omurTick(MeteorAyarlari.odulOmruSaniye()),
                    "ödül ömrü 10 dk: " + cekirdek.get().lifespan);
            esyalariTemizle(y);
        });
    }

    @GameTest(template = SAHNE, batch = "meteor_sicak", timeoutTicks = 520)
    public static void sicakParcaYakarYirmiSaniyeSonraYakmaz(GameTestHelper y) {
        zemin(y, Blocks.STONE);
        y.setBlock(new BlockPos(12, 5, 12), Blocks.WATER);
        ServerLevel level = y.getLevel();
        BlockPos m = y.absolutePos(BlockPos.ZERO);
        ServerPlayer o = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), "meteor-sicak"),
                m.getX() + 1.5, m.getY() + 6, m.getZ() + 13.5);
        o.setGameMode(GameType.SURVIVAL);
        ItemEntity sicak = MeteorOdulleri.birak(level, Vec3.atBottomCenterOf(y.absolutePos(new BlockPos(7, 6, 3))),
                new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get()), 20);
        ItemEntity bekleyen = MeteorOdulleri.birak(level, Vec3.atBottomCenterOf(y.absolutePos(new BlockPos(3, 6, 7))),
                new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get()), 20);
        ItemEntity suda = MeteorOdulleri.birak(level, Vec3.atCenterOf(y.absolutePos(new BlockPos(12, 5, 12))),
                new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get()), 20);
        try {
            dogrula(y, MeteorOdulleri.sicakMi(sicak.getItem(), level.getGameTime()), "yeni parça sıcak olmalı");
            o.clearFire();
            MinecraftForge.EVENT_BUS.post(new EntityItemPickupEvent(o, sicak));
            dogrula(y, o.getRemainingFireTicks() > 0, "sıcak parçayı alan oyuncu yanmalı");
            dogrula(y, MeteorOdulleri.sicakBitis(sicak.getItem()) == OdulZamani.SOGUK, "alınan parça soğumalı (yığılır)");
            sicak.discard();
            o.clearFire();
        } catch (RuntimeException hata) {
            level.getServer().getPlayerList().remove(o);
            throw hata;
        }
        y.runAfterDelay(40, () -> dogrula(y, MeteorOdulleri.sicakBitis(suda.getItem()) == OdulZamani.SOGUK,
                "suya düşen parça hemen soğumalı"));
        y.runAfterDelay(20L * OdulZamani.SICAK_SANIYE + 10, () -> {
            try {
                dogrula(y, !MeteorOdulleri.sicakMi(bekleyen.getItem(), level.getGameTime()), "20 sn sonra soğuk olmalı");
                dogrula(y, bekleyen.isAlive(), "parça 10 dk dolmadan kaybolmamalı");
                o.clearFire();
                MinecraftForge.EVENT_BUS.post(new EntityItemPickupEvent(o, bekleyen));
                dogrula(y, o.getRemainingFireTicks() <= 0, "soğumuş parça yakmamalı");
                esyalariTemizle(y);
                y.succeed();
            } finally {
                level.getServer().getPlayerList().remove(o);
            }
        });
    }

    @GameTest(template = SAHNE, batch = "meteor_yangin", timeoutTicks = 300)
    public static void meteorAtesiYanginYoneticisindeGorunurTemizBitisSondurur(GameTestHelper y) {
        blokHasari(y, true);
        zemin(y, Blocks.STONE);
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 15; z++) {
                double r = Math.hypot(x - MERKEZ.getX(), z - MERKEZ.getZ());
                if (r > 7.2 && r <= 9.0) {
                    y.setBlock(new BlockPos(x, 6, z), Blocks.OAK_PLANKS);
                }
            }
        }
        ServerLevel level = y.getLevel();
        YanginKaynagi kaynak = YanginKaynagi.afet(AfetTuru.GUNES_PATLAMASI, 900_000_000L + level.getGameTime());
        KraterIsi is = carp(y, MeteorBoyutu.BUYUK, kaynak, HIC);
        y.succeedWhen(() -> {
            y.assertTrue(is.bitti(), "krater işi bitmedi");
            y.assertTrue(YanginYoneticisi.bekleyenAlanSayisi(level) == 0, "alan tutuşturma sürüyor");
            int aktif = YanginYoneticisi.aktifSayi(level, kutu(y).inflate(2));
            y.assertTrue(aktif > 0, "meteor ateşi yangın yöneticisinde görünmeli");
            YanginYoneticisi.kaynakBitti(level.getServer(), kaynak, true);
            y.assertTrue(YanginYoneticisi.aktifSayi(level, kutu(y).inflate(2)) == 0,
                    "temiz kaynakBitti meteor ateşini söndürmeli");
        });
    }

    /** Tezgaha yığınları koyup tarifin sonucunu verir (eşleşme yoksa boş). */
    private static ItemStack tarifSonucu(ServerLevel level, ServerPlayer o, ItemStack... yiginlar) {
        TransientCraftingContainer tezgah = new TransientCraftingContainer(new CraftingMenu(0, o.getInventory()), 3, 3);
        for (int i = 0; i < yiginlar.length; i++) {
            tezgah.setItem(i, yiginlar[i].copy());
        }
        Optional<CraftingRecipe> tarif = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, tezgah, level);
        return tarif.map(t -> t.assemble(tezgah, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    @GameTest(template = "bos", batch = "meteor_tarif")
    public static void meteorVeTermostatTarifleri(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        BlockPos m = y.absolutePos(BlockPos.ZERO);
        ServerPlayer o = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), "meteor-tarif"),
                m.getX() + 0.5, m.getY(), m.getZ() + 0.5);
        try {
            ItemStack parca = new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get());
            ItemStack kulce = tarifSonucu(level, o, parca, parca, parca, new ItemStack(Items.IRON_INGOT));
            dogrula(y, kulce.is(MeteorEsyaKayitlari.METEORIT_KULCESI.get()) && kulce.getCount() == 1,
                    "3 parça + demir → külçe: " + kulce);
            dogrula(y, tarifSonucu(level, o, parca, parca, new ItemStack(Items.IRON_INGOT)).isEmpty(), "2 parça yetmez");
            ItemStack plaka = tarifSonucu(level, o, kulce, new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.REDSTONE),
                    new ItemStack(Items.IRON_INGOT));
            dogrula(y, plaka.is(MeteorEsyaKayitlari.TERMOSTAT_PLAKASI.get()), "külçe+altın+kızıltaş+demir → plaka: " + plaka);

            ItemStack kask = new ItemStack(Items.DIAMOND_HELMET);
            kask.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
            kask.setDamageValue(91);
            ZirhEkleri.tuyAyarla(kask, 12);
            ZirhEkleri.koy(kask, ZirhEkleri.YUNLU);
            kask.setHoverName(Component.literal("Gök Kaskı"));
            ItemStack sonuc = tarifSonucu(level, o, kask, plaka);
            dogrula(y, sonuc.is(Items.DIAMOND_HELMET) && sonuc.getCount() == 1, "aynı parça çıkmalı: " + sonuc);
            dogrula(y, ZirhEkleri.varMi(sonuc, Termostat.TERMOSTATLI), "termostatlı işaretlenmeli");
            dogrula(y, sonuc.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION) == 4, "büyü korunmalı");
            dogrula(y, sonuc.getDamageValue() == 91, "dayanıklılık korunmalı: " + sonuc.getDamageValue());
            dogrula(y, ZirhEkleri.tuySayisi(sonuc) == 12, "tüy korunmalı");
            dogrula(y, ZirhEkleri.varMi(sonuc, ZirhEkleri.YUNLU), "yün korunmalı");
            dogrula(y, sonuc.getHoverName().getString().equals("Gök Kaskı"), "ad korunmalı");
            dogrula(y, !ZirhEkleri.varMi(kask, Termostat.TERMOSTATLI), "girdi değişmemeli");
            dogrula(y, tarifSonucu(level, o, sonuc, plaka).isEmpty(), "ikinci termostat reddedilmeli");
            ItemStack yunlu = tarifSonucu(level, o, sonuc, new ItemStack(Items.WHITE_WOOL));
            dogrula(y, yunlu.isEmpty(), "zaten yünlü parça yeniden yünlenmez");
            ItemStack bot = tarifSonucu(level, o, new ItemStack(Items.LEATHER_BOOTS), plaka);
            dogrula(y, bot.is(Items.LEATHER_BOOTS) && ZirhEkleri.varMi(bot, Termostat.TERMOSTATLI), "deri bot termostatlı olur");
            y.succeed();
        } finally {
            level.getServer().getPlayerList().remove(o);
        }
    }

    @GameTest(template = "bos", batch = "meteor_termostat", timeoutTicks = 100)
    public static void termostatliZirhNotreCekerUctaAzalir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        BlockPos m = y.absolutePos(BlockPos.ZERO);
        ServerPlayer termostatli = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), "termostat-a"),
                m.getX() + 0.5, m.getY(), m.getZ() + 0.5);
        ServerPlayer duz = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), "termostat-b"),
                m.getX() + 0.7, m.getY(), m.getZ() + 0.5);
        try {
            for (ServerPlayer o : List.of(termostatli, duz)) {
                o.setGameMode(GameType.SURVIVAL);
                for (EquipmentSlot yuva : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                        EquipmentSlot.FEET)) {
                    ItemStack parca = new ItemStack(switch (yuva) {
                        case HEAD -> Items.IRON_HELMET;
                        case CHEST -> Items.IRON_CHESTPLATE;
                        case LEGS -> Items.IRON_LEGGINGS;
                        default -> Items.IRON_BOOTS;
                    });
                    if (o == termostatli) {
                        ZirhEkleri.koy(parca, Termostat.TERMOSTATLI);
                    }
                    o.setItemSlot(yuva, parca);
                }
            }
            dogrula(y, ZirhEkleri.yalitimGirdisi(termostatli).termostatParcasi() == 4, "4 termostatlı parça sayılmalı");
            dogrula(y, ZirhEkleri.yalitimGirdisi(duz).termostatParcasi() == 0, "düz zırh termostatsız");
            Yalitim.Ayarlar ayar = SicaklikAyarlari.al().yalitim();

            double ilimliFark = adim(termostatli, duz, -60);
            double ilimliE = SicaklikYoneticisi.analiz(duz).ortamBirim();
            double ilimliBeklenen = Yalitim.termostatCekimi(-60, ilimliE, 4, ayar);
            dogrula(y, ilimliFark > 0, "termostat −60'taki vücudu nötre çekmeli: " + ilimliFark);
            dogrula(y, Math.abs(ilimliFark - ilimliBeklenen) < 0.02,
                    "çekim fizikle aynı: " + ilimliFark + " / " + ilimliBeklenen + " (E=" + ilimliE + ")");

            SicaklikKancalari.ortamKaynagiEkle(level, new Vec3(m.getX() + 0.6, m.getY() + 1, m.getZ() + 0.5), -200, 16, 60);
            double ucE = SicaklikYoneticisi.analiz(duz).ortamBirim();
            double ucFark = adim(termostatli, duz, -100);
            double ucBeklenen = Yalitim.termostatCekimi(-100, ucE, 4, ayar);
            DogalAfetler.LOGGER.info("termostat GameTest: ılımlı E={} fark={}; uç E={} fark={}", ilimliE, ilimliFark, ucE,
                    ucFark);
            dogrula(y, ucE < ilimliE - 20, "soğuk kaynak ortamı düşürmeli: " + ilimliE + " → " + ucE);
            dogrula(y, ucFark > 0 && ucFark < ilimliFark, "uçta çekim azalır ama kalır: " + ucFark + " < " + ilimliFark);
            dogrula(y, Math.abs(ucFark - ucBeklenen) < 0.02, "uç çekim fizikle aynı: " + ucFark + " / " + ucBeklenen);
            dogrula(y, ucFark >= 4 * ayar.termostatCekimi() * ayar.termostatTaban() - 1e-6, "çekim tabanın altına inmez");
            y.succeed();
        } finally {
            level.getServer().getPlayerList().remove(termostatli);
            level.getServer().getPlayerList().remove(duz);
        }
    }

    /** İki oyuncuyu aynı vücut sıcaklığından 1 sn ilerletir; termostatlının fazladan nötre yaklaşması. */
    private static double adim(ServerPlayer termostatli, ServerPlayer duz, double vucut) {
        VucutSicakligi.ayarla(termostatli, vucut);
        VucutSicakligi.ayarla(duz, vucut);
        SicaklikYoneticisi.guncelle(termostatli, 20);
        SicaklikYoneticisi.guncelle(duz, 20);
        return VucutSicakligi.al(termostatli) - VucutSicakligi.al(duz);
    }
}
