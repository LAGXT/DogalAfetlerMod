package io.github.kadirberkpolat1.dogalafetler.meteor;

import com.mojang.authlib.GameProfile;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.DonemAlani;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.olay.DonemDegistiEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraftforge.eventbus.api.EventPriority;
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

    /**
     * Taban: y 1–5 toprak (tüm kutu), üstü hava. Kalıcı GameTest dünyasında (K-C24) önceki koşudan bu sahnede kalmış
     * krater işleri atılır; testler yalnız kendi sahnelerindeki işleri sayar.
     */
    private static void zemin(GameTestHelper y, Block blok) {
        MeteorYoneticisi.kraterleriUnut(y.getLevel(), sahneAlani(y));
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

    /** Sahnenin krater merkezleri (yangın alanı ve yağmur halkası payıyla). */
    private static AABB sahneAlani(GameTestHelper y) {
        return kutu(y).inflate(4);
    }

    private static int bekleyenKrater(GameTestHelper y) {
        return MeteorYoneticisi.bekleyenKraterSayisi(y.getLevel(), sahneAlani(y));
    }

    /** Doğrudan çarpma (uçuşsuz): krater işi döner. */
    private static KraterIsi carp(GameTestHelper y, MeteorBoyutu boyut, YanginKaynagi kaynak, OdulZari.Sanslar sanslar) {
        BlockPos zemin = y.absolutePos(MERKEZ);
        return MeteorCarpmasi.carp(y.getLevel(), zemin, Vec3.atBottomCenterOf(zemin.above()), boyut, MeteorGorunumu.GUNES,
                kaynak, false, sanslar, false);
    }

    /**
     * Test geçince sahne boşaltılır: ateş (yangın alanı kutunun 2 blok dışına taşar), ödül eşyaları ve kutudaki bloklar.
     * GameTest dünyası koşular arasında saklanır (K-C24) ve test sayısı değişince batch konumları kayar; kalan yakıt ya da
     * ateş başka bir testin sahnesine değer.
     */
    private static void sahneyiTemizle(GameTestHelper y) {
        YanginYoneticisi.sondur(y.getLevel(), kutu(y).inflate(4));
        esyalariTemizle(y);
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 15; z++) {
                for (int k = 0; k < 12; k++) {
                    y.setBlock(new BlockPos(x, k, z), Blocks.AIR);
                }
            }
        }
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
            y.assertTrue(bekleyenKrater(y) == 0, "krater işi sürüyor");
            y.assertBlockPresent(Blocks.AIR, MERKEZ);
            y.assertBlockPresent(Blocks.AIR, MERKEZ.below(2));
            y.assertBlockPresent(Blocks.AIR, MERKEZ.offset(3, 0, 0));
            y.assertBlockPresent(Blocks.OBSIDIAN, sert);
            y.assertBlockPresent(Blocks.BEDROCK, MERKEZ.offset(0, -1, 1));
            y.assertBlockPresent(Blocks.MAGMA_BLOCK, MERKEZ.below(4));
            y.assertBlockPresent(Blocks.DIRT, MERKEZ.offset(-7, 0, 0));
            y.assertTrue(esyalar(y).isEmpty(), "zar 0'da ödül çıkmamalı");
            sahneyiTemizle(y);
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
            y.assertTrue(YanginYoneticisi.bekleyenAlanSayisi(y.getLevel()) == 0, "alan tutuşturma sürüyor");
            YanginYoneticisi.sondur(y.getLevel(), kutu(y).inflate(4));
            kural.set(true, y.getLevel().getServer());
            long sonra = SanalSu.bolgeHash(y.getLevel(), a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
            y.assertTrue(once == sonra, "afetBlokHasari=false iken blok değişmemeli");
            y.assertTrue(is.kazilan() == 0, "kazılan " + is.kazilan());
            sahneyiTemizle(y);
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
            sahneyiTemizle(y);
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
            sahneyiTemizle(y);
        });
    }

    /** Gerçek alma yolu ({@code ItemEntity.playerTouch} → {@code EntityItemPickupEvent} → envanter) ve yakınlık yanması. */
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
            dogrula(y, MeteorYoneticisi.sicakTakipte(level, sicak), "bırakılan sıcak parça takipte olmalı");
            o.clearFire();
            sicak.setNoPickUpDelay();
            sicak.playerTouch(o);
            dogrula(y, sicak.isRemoved(), "parça envantere alınmalı");
            dogrula(y, o.getRemainingFireTicks() > 0, "sıcak parçayı alan oyuncu yanmalı");
            int slot = o.getInventory().findSlotMatchingItem(new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get()));
            dogrula(y, slot >= 0, "envanterde etiketsiz (soğumuş, yığılabilir) parça olmalı: " + slot);
            o.clearFire();
        } catch (RuntimeException hata) {
            level.getServer().getPlayerList().remove(o);
            throw hata;
        }
        y.runAfterDelay(15, () -> {
            ItemEntity yakin = MeteorOdulleri.birak(level, new Vec3(o.getX(), o.getY() + 0.1, o.getZ()),
                    new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get()), 20);
            yakin.setPickUpDelay(32767);
        });
        // Su hücresinin ortasında doğan parça yalnız ilk iki tick suya değer, sonra seker ve kenardaki taşa düşebilir (M-31).
        y.runAfterDelay(5, () -> dogrula(y, MeteorOdulleri.sicakBitis(suda.getItem()) == OdulZamani.SOGUK,
                "suya düşen parça hemen soğumalı"));
        y.runAfterDelay(40, () -> dogrula(y, o.getRemainingFireTicks() > 0,
                "sıcak parçanın yanına gelen oyuncu yanmalı"));
        y.runAfterDelay(20L * OdulZamani.SICAK_SANIYE + 30, () -> {
            try {
                dogrula(y, !MeteorOdulleri.sicakMi(bekleyen.getItem(), level.getGameTime()), "20 sn sonra soğuk olmalı");
                dogrula(y, bekleyen.isAlive(), "parça 10 dk dolmadan kaybolmamalı");
                o.clearFire();
                bekleyen.setNoPickUpDelay();
                bekleyen.playerTouch(o);
                dogrula(y, bekleyen.isRemoved(), "soğuk parça alınmalı");
                dogrula(y, o.getRemainingFireTicks() <= 0, "soğumuş parça yakmamalı");
                sahneyiTemizle(y);
                y.succeed();
            } finally {
                level.getServer().getPlayerList().remove(o);
            }
        });
    }

    /**
     * M-26: sıcak parça kaydedilip (ör. sunucu kapanışı) yeniden yüklenince NBT'deki mutlak bitiş tick'inden takibe
     * alınır ve yanına gelen yanar; süresi kayıtlıyken dolmuş parça dünyaya girerken soğur.
     */
    @GameTest(template = SAHNE, batch = "meteor_sicak_kayit", timeoutTicks = 100)
    public static void sicakParcaKaydedilipOkununcaTakipSurer(GameTestHelper y) {
        zemin(y, Blocks.STONE);
        ServerLevel level = y.getLevel();
        BlockPos m = y.absolutePos(new BlockPos(7, 6, 7));
        ServerPlayer o = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), "meteor-kayit"),
                m.getX() + 0.5, m.getY(), m.getZ() + 0.5);
        o.setGameMode(GameType.SURVIVAL);
        ItemEntity ilk = MeteorOdulleri.birak(level, Vec3.atBottomCenterOf(m),
                new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get()), 20);
        CompoundTag kayit = new CompoundTag();
        ilk.save(kayit);
        ilk.discard();
        MeteorYoneticisi.sicakTakibiniUnut(level);
        ItemEntity okunan = yukle(level, kayit);
        try {
            dogrula(y, MeteorOdulleri.sicakMi(okunan.getItem(), level.getGameTime()), "okunan parça sıcak kalmalı");
            dogrula(y, MeteorYoneticisi.sicakTakipte(level, okunan), "okunan sıcak parça takibe alınmalı");
            CompoundTag eski = kayit.copy();
            eski.getCompound("Item").getCompound("tag").putLong(MeteorOdulleri.SICAK_ETIKETI, level.getGameTime() - 1);
            ItemEntity sogumus = yukle(level, eski);
            dogrula(y, MeteorOdulleri.sicakBitis(sogumus.getItem()) == OdulZamani.SOGUK,
                    "süresi dolmuş kayıt dünyaya girerken soğumalı");
            dogrula(y, !MeteorYoneticisi.sicakTakipte(level, sogumus), "soğuk parça takipte olmamalı");
            o.clearFire();
        } catch (RuntimeException hata) {
            level.getServer().getPlayerList().remove(o);
            throw hata;
        }
        y.runAfterDelay(12, () -> {
            try {
                dogrula(y, o.getRemainingFireTicks() > 0, "okunan sıcak parçanın yanındaki oyuncu yanmalı");
                sahneyiTemizle(y);
                y.succeed();
            } finally {
                level.getServer().getPlayerList().remove(o);
            }
        });
    }

    private static ItemEntity yukle(ServerLevel level, CompoundTag kayit) {
        Entity varlik = EntityType.loadEntityRecursive(kayit, level, e -> e);
        if (!(varlik instanceof ItemEntity e)) {
            throw new IllegalStateException("eşya varlığı okunamadı: " + varlik);
        }
        e.setUUID(UUID.randomUUID());
        e.setPickUpDelay(32767);
        level.addFreshEntity(e);
        return e;
    }

    /** M-02: suya (gerçek su) düşen meteor buhar çıkarır; krater, ateş ve ödül yok, sahne değişmez. */
    @GameTest(template = SAHNE, batch = "meteor_su", timeoutTicks = 200)
    public static void suyaDusenMeteorKraterAcmaz(GameTestHelper y) {
        blokHasari(y, true);
        zemin(y, Blocks.STONE);
        for (int x = 1; x < 14; x++) {
            for (int z = 1; z < 14; z++) {
                y.setBlock(new BlockPos(x, 5, z), Blocks.WATER);
            }
        }
        ServerLevel level = y.getLevel();
        BlockPos a = y.absolutePos(BlockPos.ZERO);
        BlockPos b = y.absolutePos(new BlockPos(14, 11, 14));
        long once = SanalSu.bolgeHash(level, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
        List<MeteorCarptiEvent> olaylar = new ArrayList<>();
        Consumer<MeteorCarptiEvent> dinleyici = olaylar::add;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, MeteorCarptiEvent.class, dinleyici);
        Optional<MeteorVarligi> m = MeteorAPI.dusur(level, y.absolutePos(MERKEZ), MeteorBoyutu.BUYUK, MeteorGorunumu.GUNES,
                YanginKaynagi.KOMUT, false, 90, 0, HEP);
        dogrula(y, m.isPresent(), "meteor varlığı oluşmalı");
        y.succeedWhen(() -> {
            y.assertTrue(m.get().isRemoved() && !olaylar.isEmpty(), "meteor henüz çarpmadı");
            MinecraftForge.EVENT_BUS.unregister(dinleyici);
            MeteorCarptiEvent olay = olaylar.get(0);
            y.assertTrue(olay.suya(), "çarpma suya olmalı");
            y.assertTrue(olay.odul().bos(), "suda ödül zarı atılmaz (zar 1'de bile): " + olay.odul());
            y.assertTrue(bekleyenKrater(y) == 0, "suda krater işi olmamalı");
            y.assertTrue(esyalar(y).isEmpty(), "suda ödül düşmemeli");
            y.assertTrue(YanginYoneticisi.aktifSayi(level, kutu(y).inflate(4)) == 0, "suda ateş olmamalı");
            long sonra = SanalSu.bolgeHash(level, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
            y.assertTrue(once == sonra, "suya düşen meteor sahneyi değiştirmemeli");
            sahneyiTemizle(y);
        });
    }

    /**
     * Yağmur: iki oyuncuya sırayla (fark ≤ 1) ve uçan meteor tavanı ({@code meteor.varlikTavani} 48) aşılmaz. Dağıtım
     * halkası 0–2 blok ve giriş dik: meteorlar sahneye düşer.
     */
    @GameTest(template = SAHNE, batch = "meteor_yagmur", timeoutTicks = 300)
    public static void yagmurOyuncularaDengeliTavanAsilmaz(GameTestHelper y) {
        zemin(y, Blocks.STONE);
        ServerLevel level = y.getLevel();
        BlockPos m = y.absolutePos(BlockPos.ZERO);
        ServerPlayer a = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), "meteor-yagmur-a"),
                m.getX() + 3.5, m.getY() + 6, m.getZ() + 7.5);
        ServerPlayer b = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), "meteor-yagmur-b"),
                m.getX() + 11.5, m.getY() + 6, m.getZ() + 7.5);
        List<MeteorCarptiEvent> olaylar = new ArrayList<>();
        Consumer<MeteorCarptiEvent> dinleyici = olaylar::add;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, MeteorCarptiEvent.class, dinleyici);
        int[] enCok = {0};
        int tavan = MeteorAyarlari.varlikTavani();
        // Tick başına 2 × 36000/1200 = 60 meteor isteği: ilk tick'te tavan dolar.
        MeteorAPI.yagmur(level, List.of(a, b), 2, 36000, 0, YanginKaynagi.KOMUT, 0, 2, 90);
        y.onEachTick(() -> enCok[0] = Math.max(enCok[0], MeteorAPI.aktifMeteorSayisi(level)));
        y.succeedWhen(() -> {
            y.assertTrue(MeteorAPI.aktifYagmurSayisi(level) == 0, "yağmur sürüyor");
            y.assertTrue(MeteorAPI.aktifMeteorSayisi(level) == 0 && bekleyenKrater(y) == 0,
                    "meteorlar düşmedi");
            y.assertTrue(olaylar.size() == enCok[0], "her uçan meteor çarpmalı: " + olaylar.size() + " / " + enCok[0]);
            MinecraftForge.EVENT_BUS.unregister(dinleyici);
            int aSayisi = 0;
            int bSayisi = 0;
            for (MeteorCarptiEvent olay : olaylar) {
                if (olay.konum().distSqr(a.blockPosition()) < olay.konum().distSqr(b.blockPosition())) {
                    aSayisi++;
                } else {
                    bSayisi++;
                }
            }
            DogalAfetler.LOGGER.info("meteor yağmur GameTest: en çok uçan {}/{}, çarpma A={} B={}", enCok[0], tavan,
                    aSayisi, bSayisi);
            y.assertTrue(enCok[0] == tavan, "uçan meteor tavanı dolmalı ve aşılmamalı: " + enCok[0] + " / " + tavan);
            y.assertTrue(Math.abs(aSayisi - bSayisi) <= 1, "dağılım dengeli olmalı: " + aSayisi + " / " + bSayisi);
            level.getServer().getPlayerList().remove(a);
            level.getServer().getPlayerList().remove(b);
            sahneyiTemizle(y);
        });
    }

    /**
     * M-04: {@code afetBlokHasari=false} iken krater yok, meteor ateşi yangın yöneticisinde yanar ama yakıtı tüketmez
     * (blok değişmez); ödül eşya olduğu için düşer.
     */
    @GameTest(template = SAHNE, batch = "meteor_kural_ates", timeoutTicks = 300)
    public static void blokHasariKapaliykenAtesYanarBlokDegismez(GameTestHelper y) {
        GameRules.BooleanValue kural = blokHasari(y, false);
        zemin(y, Blocks.DIRT);
        List<BlockPos> tahtalar = new ArrayList<>();
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 15; z++) {
                double r = Math.hypot(x - MERKEZ.getX(), z - MERKEZ.getZ());
                if (r > 5.6 && r <= 7.0) {
                    y.setBlock(new BlockPos(x, 5, z), Blocks.OAK_PLANKS);
                    tahtalar.add(new BlockPos(x, 5, z));
                }
            }
        }
        ServerLevel level = y.getLevel();
        BlockPos a = y.absolutePos(BlockPos.ZERO);
        BlockPos b = y.absolutePos(new BlockPos(14, 11, 14));
        long once = SanalSu.bolgeHash(level, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
        YanginKaynagi kaynak = YanginKaynagi.afet(AfetTuru.AY_CARPMASI, 910_000_000L + level.getGameTime());
        KraterIsi is = carp(y, MeteorBoyutu.BUYUK, kaynak, HEP);
        int[] gorulen = {0};
        y.onEachTick(() -> gorulen[0] = Math.max(gorulen[0], YanginYoneticisi.aktifSayi(level, kutu(y).inflate(4))));
        y.runAfterDelay(120, () -> {
            try {
                y.assertTrue(is.bitti() && is.kazilan() == 0, "krater açılmamalı: kazılan " + is.kazilan());
                y.assertTrue(gorulen[0] > 0, "meteor ateşi yangın yöneticisinde yanmalı");
                for (BlockPos t : tahtalar) {
                    y.assertBlockPresent(Blocks.OAK_PLANKS, t);
                }
                long sonra = SanalSu.bolgeHash(level, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
                y.assertTrue(once == sonra, "blok hasarı kapalıyken 120 tick yanan ateş blok değiştirmemeli");
                List<ItemEntity> e = esyalar(y);
                y.assertTrue(e.stream().anyMatch(i -> i.getItem().is(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get()))
                        && e.stream().anyMatch(i -> i.getItem().is(MeteorEsyaKayitlari.METEORIT_PARCASI.get())),
                        "ödüller eşya olarak düşmeli");
                YanginYoneticisi.kaynakBitti(level.getServer(), kaynak, true);
                sahneyiTemizle(y);
                y.succeed();
            } finally {
                kural.set(true, level.getServer());
            }
        });
    }

    /**
     * M-25: krater işinin sıradaki hücresi yüklü olmayan chunk'taysa iş bekler (kayıttan aynen okunur), chunk yüklenince
     * sürer; zamanı gelmiş magma soğuması da yüklenene kadar kayıtta kalır. Bitişte merkez chunk'ı yüklü değilse bitiş
     * (ödül, yangın, olay) bekler, yüklenince tamamlanır (Md1: senkron chunk yüklemesi yok).
     */
    @GameTest(template = SAHNE, batch = "meteor_bekleyen", timeoutTicks = 200)
    public static void kraterIsiYukluDegilkenBeklerSonraTamamlanir(GameTestHelper y) {
        blokHasari(y, true);
        zemin(y, Blocks.DIRT);
        ServerLevel level = y.getLevel();
        BlockPos merkez = y.absolutePos(MERKEZ);
        int sinirX = merkez.getX() + 3;
        BlockPos magma = y.absolutePos(new BlockPos(1, 6, 1));
        y.setBlock(new BlockPos(1, 6, 1), Blocks.MAGMA_BLOCK);
        // Aşama 0: x ≥ sınır ve magma yüklü değil; aşama 1: bitiş sırasında merkez chunk'ı yüklü değil, magma hücresi
        // hariç: sahne kökü batch sayısıyla chunk sınırına göre kayar (K-C24), magma merkezle aynı chunk'a düşebilir.
        int[] asama = {0};
        KraterIsi[] isRef = {null};
        BiPredicate<ServerLevel, BlockPos> onceki = MeteorYoneticisi.yukluKaynagiAyarla((l, p) -> {
            if (asama[0] == 0 && (p.getX() >= sinirX || p.equals(magma))) {
                return false;
            }
            if (asama[0] == 1 && isRef[0] != null && isRef[0].bitiste() && !p.equals(magma)
                    && p.getX() >> 4 == merkez.getX() >> 4 && p.getZ() >> 4 == merkez.getZ() >> 4) {
                return false;
            }
            return l.isLoaded(p);
        });
        KraterIsi is;
        try {
            MeteorYoneticisi.sogumaEkle(level, magma, level.getGameTime() + 2, Blocks.BLACKSTONE.defaultBlockState());
            is = carp(y, MeteorBoyutu.BUYUK, YanginKaynagi.KOMUT, HEP);
            isRef[0] = is;
        } catch (RuntimeException hata) {
            MeteorYoneticisi.yukluKaynagiAyarla(onceki);
            throw hata;
        }
        y.runAfterDelay(30, () -> {
            try {
                dogrula(y, !is.bitti() && is.bekliyor(), "krater işi yüklü olmayan hücrede beklemeli");
                dogrula(y, MeteorYoneticisi.bekleyenChunkKraterSayisi(level) >= 1, "bekleyen krater sayılmalı");
                y.assertBlockPresent(Blocks.DIRT, MERKEZ.offset(4, 0, 0));
                y.assertBlockPresent(Blocks.MAGMA_BLOCK, new BlockPos(1, 6, 1));
                MeteorVerisi veri = MeteorYoneticisi.veri(level);
                dogrula(y, veri.sogumalar.stream().anyMatch(s -> s.konum().equals(magma)), "soğuma kayıtta kalmalı");
                MeteorVerisi okunan = MeteorVerisi.yukle(veri.save(new CompoundTag()));
                int i = veri.kraterler.indexOf(is);
                dogrula(y, i >= 0 && okunan.kraterler.size() == veri.kraterler.size()
                        && okunan.kraterler.get(i).ozet().equals(is.ozet()), "krater işi kayıttan aynen okunmalı: "
                        + is.ozet() + " / " + (i >= 0 ? okunan.kraterler.get(i).ozet() : "yok"));
                dogrula(y, okunan.sogumalar.size() == veri.sogumalar.size(), "soğumalar kayıttan okunmalı");
                asama[0] = 1;
            } catch (RuntimeException hata) {
                MeteorYoneticisi.yukluKaynagiAyarla(onceki);
                throw hata;
            }
        });
        y.runAfterDelay(60, () -> {
            try {
                dogrula(y, is.bitiste() && !is.bitti() && is.bekliyor(),
                        "merkez chunk'ı yüklü değilken bitiş beklemeli: " + is.ozet());
                dogrula(y, esyalar(y).isEmpty(), "bitiş beklerken ödül düşmemeli");
                y.assertBlockPresent(Blocks.AIR, MERKEZ.offset(4, 0, 0));
                y.assertBlockPresent(Blocks.BLACKSTONE, new BlockPos(1, 6, 1));
            } finally {
                MeteorYoneticisi.yukluKaynagiAyarla(onceki);
            }
        });
        y.runAfterDelay(90, () -> {
            dogrula(y, is.bitti(), "chunk yüklenince krater işi tamamlanmalı");
            dogrula(y, esyalar(y).size() == 2, "bitişte çekirdek ve parça düşmeli: " + esyalar(y).size());
            sahneyiTemizle(y);
            y.succeed();
        });
    }

    @GameTest(template = SAHNE, batch = "meteor_yangin", timeoutTicks = 300)
    public static void meteorAtesiYanginYoneticisindeGorunurTemizBitisSondurur(GameTestHelper y) {
        blokHasari(y, true);
        zemin(y, Blocks.STONE);
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 15; z++) {
                double r = Math.hypot(x - MERKEZ.getX(), z - MERKEZ.getZ());
                if (r > 5.6 && r <= 7.0) {
                    y.setBlock(new BlockPos(x, 5, z), Blocks.OAK_PLANKS);
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
            sahneyiTemizle(y);
        });
    }

    /** K-E11: sıfırlama uçan meteoru ve yağmuru kaldırır, sıcak parçayı soğutur (yalnız meteor dinleyicisi çağrılır). */
    @GameTest(template = SAHNE, batch = "meteor_sifirla", timeoutTicks = 100)
    public static void sifirlamaUcanMeteorVeSicakParcayiTemizler(GameTestHelper y) {
        zemin(y, Blocks.STONE);
        ServerLevel level = y.getLevel();
        Optional<MeteorVarligi> m = MeteorAPI.dusur(level, y.absolutePos(MERKEZ), MeteorBoyutu.KUCUK, MeteorGorunumu.GUNES,
                YanginKaynagi.KOMUT, false, 90, 0, HIC);
        int yagmur = MeteorAPI.yagmur(level, List.of(), 2000, 1, 0, YanginKaynagi.KOMUT);
        ItemEntity sicak = MeteorOdulleri.birak(level, Vec3.atBottomCenterOf(y.absolutePos(new BlockPos(3, 6, 3))),
                new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get()), 20);
        dogrula(y, m.isPresent() && !m.get().isRemoved(), "meteor uçuyor olmalı");
        dogrula(y, MeteorAPI.aktifYagmurSayisi(level) >= 1, "yağmur başlamalı: " + yagmur);
        MeteorYoneticisi.donemDegisti(new DonemDegistiEvent(DunyaDonemi.al(level), DonemAlani.SIFIRLAMA));
        dogrula(y, m.get().isRemoved(), "uçan meteor kalkmalı");
        dogrula(y, MeteorAPI.aktifMeteorSayisi(level) == 0, "uçan meteor sayısı 0");
        dogrula(y, MeteorAPI.aktifYagmurSayisi(level) == 0, "yağmur durmalı");
        dogrula(y, MeteorOdulleri.sicakBitis(sicak.getItem()) == OdulZamani.SOGUK, "sıcak parça soğumalı");
        dogrula(y, sicak.isAlive(), "yerdeki ödül kalır");
        sahneyiTemizle(y);
        y.succeed();
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
            termostatli.setGameMode(GameType.SURVIVAL);
            duz.setGameMode(GameType.SURVIVAL);
            giydir(termostatli, true, false);
            giydir(duz, false, false);
            dogrula(y, ZirhEkleri.yalitimGirdisi(termostatli).termostatParcasi() == 4, "4 termostatlı parça sayılmalı");
            dogrula(y, ZirhEkleri.yalitimGirdisi(duz).termostatParcasi() == 0, "düz zırh termostatsız");
            Yalitim.Ayarlar ayar = SicaklikAyarlari.al().yalitim();

            double ilimliFark = adim(termostatli, duz, -60);
            double ilimliE = SicaklikYoneticisi.analiz(duz).ortamBirim();
            double ilimliBeklenen = Yalitim.termostatCekimi(-60, ilimliE, 4, ayar);
            dogrula(y, ilimliFark > 0, "termostat −60'taki vücudu nötre çekmeli: " + ilimliFark);
            dogrula(y, Math.abs(ilimliFark - ilimliBeklenen) < 0.02,
                    "çekim fizikle aynı: " + ilimliFark + " / " + ilimliBeklenen + " (E=" + ilimliE + ")");

            // Termostat + yün + tüy birlikte (madde 13c "hepsi birlikte dengeli", N4): sıcakta çok yalıtımlı ikisi de ısınır,
            // termostatlı fizikteki kadar az; yalıtım girdisi üç eki birlikte sayar, termostat tam koruma vermez.
            giydir(termostatli, true, true);
            giydir(duz, false, true);
            Yalitim.Girdi girdi = ZirhEkleri.yalitimGirdisi(termostatli);
            dogrula(y, girdi.termostatParcasi() == 4 && girdi.yunluParca() == 4 && girdi.toplamTuy() == 120,
                    "termostat, yün ve tüy birlikte sayılmalı: " + girdi);
            SicaklikKancalari.ortamKaynagiEkle(level, new Vec3(m.getX() + 0.6, m.getY() + 1, m.getZ() + 0.5), 45, 16, 40);
            double sicakE = SicaklikYoneticisi.analiz(duz).ortamBirim();
            double sicakFark = adim(termostatli, duz, 60);
            double sicakBeklenen = Yalitim.termostatCekimi(60, sicakE, 4, ayar);
            DogalAfetler.LOGGER.info("termostat GameTest (yün + 30 tüy): sıcak E={} fark={} vücut {} / {}", sicakE, sicakFark,
                    VucutSicakligi.al(termostatli), VucutSicakligi.al(duz));
            dogrula(y, sicakFark < 0, "termostat sıcakta nötre (aşağı) çekmeli: " + sicakFark);
            dogrula(y, Math.abs(sicakFark - sicakBeklenen) < 0.02, "sıcak çekim fizikle aynı: " + sicakFark + " / "
                    + sicakBeklenen + " (E=" + sicakE + ")");
            dogrula(y, VucutSicakligi.al(termostatli) > 60, "çok yalıtımlı termostatlı da sıcakta ısınır (tam koruma yok)");
            giydir(termostatli, true, false);
            giydir(duz, false, false);

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

    /** Dört demir zırh parçası; {@code termostat}: termostatlı, {@code yunTuy}: yünlü ve parça başına 30 tüy. */
    private static void giydir(ServerPlayer o, boolean termostat, boolean yunTuy) {
        for (EquipmentSlot yuva : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack parca = new ItemStack(switch (yuva) {
                case HEAD -> Items.IRON_HELMET;
                case CHEST -> Items.IRON_CHESTPLATE;
                case LEGS -> Items.IRON_LEGGINGS;
                default -> Items.IRON_BOOTS;
            });
            if (termostat) {
                ZirhEkleri.koy(parca, Termostat.TERMOSTATLI);
            }
            if (yunTuy) {
                ZirhEkleri.koy(parca, ZirhEkleri.YUNLU);
                ZirhEkleri.tuyAyarla(parca, 30);
            }
            o.setItemSlot(yuva, parca);
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

    /**
     * KOM.13 (K-E18): {@code /dogalafet odul meteor} argümansız komutu verene birer Meteorit Parçası ve Kozmik Çekirdek, tür ve
     * adetle adı verilen oyuncuya o kadarını verir; eşyalar soğuktur; bilinmeyen tür hiçbir şey vermez; OP olmayan komutu
     * görmez.
     */
    @GameTest(template = "bos", batch = "meteor_odul_komut")
    public static void odulKomutuMeteorEsyalariniVerir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        BlockPos m = y.absolutePos(BlockPos.ZERO);
        ServerPlayer o = CekirdekOyunTestleri.sahteOyuncu(y, new GameProfile(UUID.randomUUID(), "odul-test"), m.getX() + 0.5,
                m.getY(), m.getZ() + 0.5);
        try {
            var dagitici = level.getServer().getCommands().getDispatcher();
            var oyuncudan = o.createCommandSourceStack().withPermission(2).withSuppressedOutput();
            var konsol = level.getServer().createCommandSourceStack().withSuppressedOutput();
            int r1 = dagitici.execute("dogalafet odul meteor", oyuncudan);
            dogrula(y, r1 == 2 && say(o, MeteorEsyaKayitlari.METEORIT_PARCASI.get()) == 1
                    && say(o, MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get()) == 1, "argümansız: birer parça ve çekirdek: " + r1);
            int r2 = dagitici.execute("dogalafet odul meteor cekirdek 3 odul-test", konsol);
            dogrula(y, r2 == 3 && say(o, MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get()) == 4
                    && say(o, MeteorEsyaKayitlari.METEORIT_PARCASI.get()) == 1, "cekirdek 3: " + r2);
            int r3 = dagitici.execute("dogalafet odul meteor yildiz 2 odul-test", konsol);
            dogrula(y, r3 == 0 && say(o, MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get()) == 4, "bilinmeyen tür vermemeli");
            for (ItemStack yigin : o.getInventory().items) {
                dogrula(y, yigin.isEmpty() || MeteorOdulleri.sicakBitis(yigin) == OdulZamani.SOGUK, "komut ödülü soğuk olmalı");
            }
            boolean reddedildi = false;
            try {
                dagitici.execute("dogalafet odul meteor", o.createCommandSourceStack().withPermission(0).withSuppressedOutput());
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException hata) {
                reddedildi = true;
            }
            dogrula(y, reddedildi, "OP olmayan oyuncu komutu kullanamamalı");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException hata) {
            y.fail("komut çalışmadı: " + hata.getMessage());
        } finally {
            level.getServer().getPlayerList().remove(o);
        }
        y.succeed();
    }

    private static int say(ServerPlayer o, net.minecraft.world.item.Item esya) {
        return o.getInventory().countItem(esya);
    }
}
