package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import com.mojang.authlib.GameProfile;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Sıcaklık sisteminin oyun içi testleri (GOREV §6 #3). Sahte oyuncular tick almaz; güncelleme doğrudan çağrılır. */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SicaklikOyunTestleri {
    private SicaklikOyunTestleri() {
    }

    /**
     * K-E41: GameTest sunucusunda Cold Sweat sıcaklık kapısında yok sayılır ({@code -Puyum=coldsweat} ile de sıcaklık, yiyecek,
     * balıkçılık ve başarım testleri kendi sistemimizi sınar); sunucu durunca gerçek değere döner. Cold Sweat yolunu sınayan
     * F-18 testi kendi gövdesinde gerçek değere geçer.
     */
    @Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
    public static final class ColdSweatKapisi {
        private ColdSweatKapisi() {
        }

        @SubscribeEvent
        public static void basladi(ServerStartedEvent olay) {
            if (olay.getServer() instanceof GameTestServer && SicaklikKapisi.coldSweatYuklu()) {
                SicaklikKapisi.coldSweatTesti(false);
                DogalAfetler.LOGGER.info("GameTest: Cold Sweat yüklü, sıcaklık kapısında yok sayılıyor (K-E41)");
            }
        }

        @SubscribeEvent
        public static void durdu(ServerStoppedEvent olay) {
            SicaklikKapisi.coldSweatTesti(null);
        }
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
     * Sahte oyuncu tick almadığı için {@code ServerPlayer.spawnInvulnerableTime} (60) hiç düşmez ve {@code hurt} reddedilir;
     * gerçek oyuncuda 3 sn sonra sıfırdır. SRG adı {@code build/createSrgToMcp/output.srg}'den.
     */
    private static void hasarAlabilir(ServerPlayer o) {
        ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, o, 0, "f_8921_");
        o.invulnerableTime = 0;
    }

    @GameTest(template = "bos", batch = "sicaklik_tuy")
    public static void tuyUpgradeBirTuyHarcarSayaciArtirirUstSinirdaDurur(GameTestHelper y) {
        ServerPlayer o = oyuncu(y, "tuy-test", 0);
        try {
            ItemStack tuy = new ItemStack(Items.FEATHER, 5);
            ItemStack zirh = new ItemStack(Items.IRON_CHESTPLATE);
            o.setItemInHand(InteractionHand.MAIN_HAND, tuy);
            o.setItemInHand(InteractionHand.OFF_HAND, zirh);
            // Gerçek yol: sağ tık olayı (havaya), yardımcı eldeki zırh giyilmez.
            InteractionResult sonuc = ForgeHooks.onItemRightClick(o, InteractionHand.MAIN_HAND);
            dogrula(y, sonuc != null && sonuc.consumesAction(), "sağ tık iptal edilmedi: " + sonuc);
            ItemStack eldeki = o.getOffhandItem();
            dogrula(y, ZirhEkleri.tuySayisi(eldeki) == 1, "sayac 1 olmalı: " + ZirhEkleri.tuySayisi(eldeki));
            dogrula(y, o.getMainHandItem().getCount() == 4, "1 tüy harcanmalı: " + o.getMainHandItem().getCount());
            dogrula(y, o.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "zırh giyilmemeli");
            // Üst sınır (3 ile): iki uygulama daha, sonra tüy harcanmaz.
            dogrula(y, TuyUpgrade.uygula(o, o.getMainHandItem(), eldeki, 3) == TuyUpgrade.Sonuc.UYGULANDI, "2. uygulama");
            dogrula(y, TuyUpgrade.uygula(o, o.getMainHandItem(), eldeki, 3) == TuyUpgrade.Sonuc.UYGULANDI, "3. uygulama");
            dogrula(y, TuyUpgrade.uygula(o, o.getMainHandItem(), eldeki, 3) == TuyUpgrade.Sonuc.SINIRDA, "sınır");
            dogrula(y, ZirhEkleri.tuySayisi(eldeki) == 3, "sınırda 3: " + ZirhEkleri.tuySayisi(eldeki));
            dogrula(y, o.getMainHandItem().getCount() == 2, "sınırda tüy harcanmaz: " + o.getMainHandItem().getCount());
            // Koşul yoksa (yardımcı elde zırh değil) olay iptal edilmez.
            o.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
            InteractionResult bos = ForgeHooks.onItemRightClick(o, InteractionHand.MAIN_HAND);
            dogrula(y, bos == null, "zırhsız sağ tık iptal edilmemeli: " + bos);
            y.succeed();
        } finally {
            cikar(y, List.of(o));
        }
    }

    @GameTest(template = "bos", batch = "sicaklik_tarif")
    public static void yunTarifiOzellikleriKorurIkinciUygulamayiReddeder(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        ServerPlayer o = oyuncu(y, "tarif-test", 0);
        try {
            ItemStack bot = new ItemStack(Items.DIAMOND_BOOTS);
            bot.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 3);
            bot.setDamageValue(57);
            ZirhEkleri.tuyAyarla(bot, 7);
            bot.setHoverName(net.minecraft.network.chat.Component.literal("Kutup Botu"));

            ItemStack sonuc = tarifSonucu(level, o, bot, new ItemStack(Items.LIGHT_BLUE_WOOL));
            dogrula(y, !sonuc.isEmpty(), "zırh + yün tarifi eşleşmedi");
            dogrula(y, sonuc.is(Items.DIAMOND_BOOTS) && sonuc.getCount() == 1, "aynı parça çıkmalı: " + sonuc);
            dogrula(y, ZirhEkleri.varMi(sonuc, ZirhEkleri.YUNLU), "yünlü işaretlenmeli");
            dogrula(y, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, sonuc) == 3,
                    "büyü korunmalı");
            dogrula(y, sonuc.getDamageValue() == 57, "dayanıklılık korunmalı: " + sonuc.getDamageValue());
            dogrula(y, ZirhEkleri.tuySayisi(sonuc) == 7, "tüy upgrade'i korunmalı: " + ZirhEkleri.tuySayisi(sonuc));
            dogrula(y, sonuc.getHoverName().getString().equals("Kutup Botu"), "ad korunmalı");
            dogrula(y, !ZirhEkleri.varMi(bot, ZirhEkleri.YUNLU), "girdi yığını değişmemeli");

            dogrula(y, tarifSonucu(level, o, sonuc, new ItemStack(Items.WHITE_WOOL)).isEmpty(),
                    "zaten yünlü parçaya ikinci yün uygulanmamalı");
            dogrula(y, tarifSonucu(level, o, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.STICK)).isEmpty(),
                    "yün dışı malzeme eşleşmemeli");
            ItemStack ikiZirh = tarifSonucu(level, o, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.WHITE_WOOL),
                    new ItemStack(Items.IRON_BOOTS));
            dogrula(y, ikiZirh.isEmpty(), "iki zırh parçası eşleşmemeli");
            y.succeed();
        } finally {
            cikar(y, List.of(o));
        }
    }

    /** Tezgaha yığınları koyup zırh eki tarifinin sonucunu verir (eşleşme yoksa boş). */
    private static ItemStack tarifSonucu(ServerLevel level, ServerPlayer o, ItemStack... yiginlar) {
        TransientCraftingContainer tezgah = new TransientCraftingContainer(new CraftingMenu(0, o.getInventory()), 3, 3);
        for (int i = 0; i < yiginlar.length; i++) {
            tezgah.setItem(i * 2, yiginlar[i].copy());
        }
        Optional<CraftingRecipe> tarif = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, tezgah, level);
        if (tarif.isEmpty() || !(tarif.get() instanceof ZirhEkiTarifi)) {
            return ItemStack.EMPTY;
        }
        return tarif.get().assemble(tezgah, level.registryAccess());
    }

    @GameTest(template = "bos", batch = "sicaklik_bagimsiz")
    public static void onOyuncununSicakligiBirbirindenBagimsiz(GameTestHelper y) {
        List<ServerPlayer> oyuncular = new ArrayList<>();
        try {
            for (int i = 0; i < 10; i++) {
                ServerPlayer o = oyuncu(y, "sicaklik-" + i, i * 0.2);
                VucutSicakligi.ayarla(o, -90 + 20 * i);
                oyuncular.add(o);
            }
            // Biri yünlü tam zırhlı ve koşuyor, biri suda değil ama ıslak değil: her biri kendi durumuyla hesaplanır.
            ServerPlayer giyimli = oyuncular.get(9);
            for (EquipmentSlot yuva : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                    EquipmentSlot.FEET)) {
                ItemStack parca = new ItemStack(switch (yuva) {
                    case HEAD -> Items.LEATHER_HELMET;
                    case CHEST -> Items.LEATHER_CHESTPLATE;
                    case LEGS -> Items.LEATHER_LEGGINGS;
                    default -> Items.LEATHER_BOOTS;
                });
                ZirhEkleri.koy(parca, ZirhEkleri.YUNLU);
                ZirhEkleri.tuyAyarla(parca, 30);
                giyimli.setItemSlot(yuva, parca);
            }
            double[] once = new double[10];
            for (int i = 0; i < 10; i++) {
                once[i] = VucutSicakligi.al(oyuncular.get(i));
            }
            for (ServerPlayer o : oyuncular) {
                SicaklikYoneticisi.guncelle(o, 20);
            }
            double[] sonra = new double[10];
            for (int i = 0; i < 10; i++) {
                sonra[i] = VucutSicakligi.al(oyuncular.get(i));
                dogrula(y, sonra[i] != once[i], "oyuncu " + i + " güncellenmedi: " + once[i]);
                if (i > 0) {
                    dogrula(y, sonra[i] > sonra[i - 1], "sıra bozuldu: " + sonra[i - 1] + " / " + sonra[i]);
                }
            }
            dogrula(y, sonra[0] > once[0], "-90'daki oyuncu ılık ortamda ısınmalı: " + sonra[0]);
            dogrula(y, ZirhEkleri.yalitimGirdisi(giyimli).toplamTuy() == 120, "tüy toplamı dört parçanınki");
            dogrula(y, ZirhEkleri.yalitimGirdisi(oyuncular.get(8)).zirhParcasi() == 0, "yalıtım oyuncuya özgü");
            // Birinin değişmesi diğerlerini etkilemez.
            VucutSicakligi.ayarla(oyuncular.get(0), 50);
            for (int i = 1; i < 10; i++) {
                dogrula(y, VucutSicakligi.al(oyuncular.get(i)) == sonra[i], "oyuncu " + i + " etkilendi");
            }
            // Aynı ortamda yalıtımlı oyuncu çıplak eşine göre daha çok ısınır (madde 7e, N4).
            VucutSicakligi.ayarla(oyuncular.get(8), 50);
            VucutSicakligi.ayarla(giyimli, 50);
            SicaklikYoneticisi.guncelle(oyuncular.get(8), 20);
            SicaklikYoneticisi.guncelle(giyimli, 20);
            dogrula(y, VucutSicakligi.al(giyimli) > VucutSicakligi.al(oyuncular.get(8)),
                    "yalıtımlı oyuncu ılıkta daha sıcak kalmalı");
            y.succeed();
        } finally {
            cikar(y, oyuncular);
        }
    }

    @GameTest(template = "bos", batch = "sicaklik_respawn")
    public static void respawndaSifirlanirBoyutDonusundeKorunur(GameTestHelper y) {
        MinecraftServer server = y.getLevel().getServer();
        kuraliAc(server);
        List<ServerPlayer> izlenen = new ArrayList<>();
        try {
            ServerPlayer olen = oyuncu(y, "respawn-test", 0);
            izlenen.add(olen);
            VucutSicakligi.ayarla(olen, -85);
            VucutSicakligi.etkiEkle(olen, 0, 1800, 30);
            ServerPlayer yeni = server.getPlayerList().respawn(olen, false);
            izlenen.add(yeni);
            dogrula(y, VucutSicakligi.al(yeni) == 0, "respawn'da vücut nötr olmalı: " + VucutSicakligi.al(yeni));
            dogrula(y, VucutSicakligi.etkiler(yeni).isEmpty(), "respawn'da süreli etkiler silinmeli");

            ServerPlayer donen = oyuncu(y, "end-test", 1);
            izlenen.add(donen);
            VucutSicakligi.ayarla(donen, 42);
            ServerPlayer kopya = server.getPlayerList().respawn(donen, true);
            izlenen.add(kopya);
            dogrula(y, VucutSicakligi.al(kopya) == 42, "ölümsüz dönüşte korunmalı: " + VucutSicakligi.al(kopya));
            y.succeed();
        } finally {
            cikar(y, izlenen);
        }
    }

    @GameTest(template = "bos", batch = "sicaklik_kural", timeoutTicks = 200)
    public static void gameruleKapaliykenGuncellemeYok(GameTestHelper y) {
        MinecraftServer server = y.getLevel().getServer();
        GameRules.BooleanValue kural = server.getGameRules().getRule(Kurallar.SICAKLIK_SISTEMI);
        ServerPlayer o = oyuncu(y, "kural-test", 0);
        kural.set(false, server);
        VucutSicakligi.yaz(o, -60);
        y.runAfterDelay(45, () -> {
            double kapaliyken = VucutSicakligi.al(o);
            kural.set(true, server);
            if (kapaliyken != -60) {
                cikar(y, List.of(o));
                y.fail("kural kapalıyken vücut değişti: " + kapaliyken);
                return;
            }
            y.runAfterDelay(45, () -> {
                double acikken = VucutSicakligi.al(o);
                cikar(y, List.of(o));
                if (acikken == -60) {
                    y.fail("kural açılınca güncelleme başlamadı");
                } else {
                    y.succeed();
                }
            });
        });
    }

    /** Madde 7c: donma ve aşırı ısınma hasarı kendi kaynaklarıyla verilir; Aşırı ısınma {@code is_fire}: Ateşe Direnç korur. */
    @GameTest(template = "bos", batch = "sicaklik_hasar")
    public static void hasarKaynaklariVeAteseDirenc(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        ServerPlayer donan = oyuncu(y, "donma-test", 0);
        ServerPlayer yanan = oyuncu(y, "yanma-test", 1);
        ServerPlayer korunan = oyuncu(y, "direnc-test", 2);
        try {
            for (ServerPlayer o : List.of(donan, yanan, korunan)) {
                hasarAlabilir(o);
            }
            korunan.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200));
            VucutSicakligi.yaz(donan, -95);
            VucutSicakligi.yaz(yanan, 95);
            VucutSicakligi.yaz(korunan, 95);
            float can = donan.getHealth();
            // 100 tick: eşik üstündeki aralık (~65 tick) en az bir vuruş verir.
            for (ServerPlayer o : List.of(donan, yanan, korunan)) {
                SicaklikYoneticisi.guncelle(o, 100);
            }
            DamageSource donma = donan.getLastDamageSource();
            dogrula(y, donan.getHealth() < can, "donma hasarı yok: " + donan.getHealth());
            dogrula(y, donma != null && donma.is(SicaklikKayitlari.DONMA) && donma.is(DamageTypeTags.IS_FREEZING),
                    "donma kaynağı yanlış: " + donma);
            DamageSource yanma = yanan.getLastDamageSource();
            dogrula(y, yanan.getHealth() < can, "aşırı ısınma hasarı yok: " + yanan.getHealth());
            dogrula(y, yanma != null && yanma.is(SicaklikKayitlari.ASIRI_ISINMA) && yanma.is(DamageTypeTags.IS_FIRE),
                    "aşırı ısınma kaynağı yanlış: " + yanma);
            dogrula(y, korunan.getHealth() == can, "Ateşe Direnç korumalı: " + korunan.getHealth());
            dogrula(y, VucutSicakligi.al(korunan) > 75, "korunan hâlâ aşırı sıcak: " + VucutSicakligi.al(korunan));
            y.succeed();
        } finally {
            cikar(y, List.of(donan, yanan, korunan));
        }
    }

    /**
     * Hasarlı kademedeki oyuncunun alev parçacığı 32 blok içindeki diğer oyuncuya gider, kendisine gitmez (birinci şahısta
     * göğüsteki sprite ekranı kaplar; üçüncü şahısta istemci kendisi üretir).
     */
    @GameTest(template = "bos", batch = "sicaklik_hasar")
    public static void hasarParcacigiKendisineGitmezYanindakineGider(GameTestHelper y) {
        kuraliAc(y.getLevel().getServer());
        ServerPlayer yanan = oyuncu(y, "parcacik-yanan", 0);
        ServerPlayer izleyen = oyuncu(y, "parcacik-izleyen", 3);
        try {
            VucutSicakligi.yaz(yanan, 95);
            VucutSicakligi.yaz(izleyen, 0);
            parcacikPaketleri(yanan);
            parcacikPaketleri(izleyen);
            SicaklikYoneticisi.guncelle(yanan, 20);
            List<ClientboundLevelParticlesPacket> kendisine = parcacikPaketleri(yanan);
            long yanindakine = parcacikPaketleri(izleyen).stream()
                    .filter(p -> p.getParticle().getType() == ParticleTypes.FLAME).count();
            dogrula(y, kendisine.isEmpty(), "hasarlı oyuncuya kendi parçacığı gitti: " + kendisine.size() + " paket");
            dogrula(y, yanindakine >= 1, "yanındaki oyuncuya alev parçacığı gitmedi (vücut " + VucutSicakligi.al(yanan)
                    + ")");
            y.succeed();
        } finally {
            cikar(y, List.of(yanan, izleyen));
        }
    }

    /** Sahte oyuncunun kanalına giden vanilla parçacık paketleri (eskiden yeniye); kanalın giden kuyruğu boşalır. */
    private static List<ClientboundLevelParticlesPacket> parcacikPaketleri(ServerPlayer o) {
        EmbeddedChannel kanal = (EmbeddedChannel) o.connection.connection.channel();
        List<ClientboundLevelParticlesPacket> sonuc = new ArrayList<>();
        for (Object mesaj = kanal.readOutbound(); mesaj != null; mesaj = kanal.readOutbound()) {
            if (mesaj instanceof ClientboundLevelParticlesPacket paket) {
                sonuc.add(paket);
            }
        }
        return sonuc;
    }

    /** Madde 7c ve 3: yavaşlama değiştiricisi soğukta eklenir; respawn'da ve kapı kapanınca kaldırılır. */
    @GameTest(template = "bos", batch = "sicaklik_yavaslama", timeoutTicks = 100)
    public static void yavaslamaSoguktaEklenirRespawnVeKuraldaKalkar(GameTestHelper y) {
        MinecraftServer server = y.getLevel().getServer();
        GameRules.BooleanValue kural = kuraliAc(server);
        ServerPlayer o = oyuncu(y, "yavaslama-test", 0);
        AttributeInstance hiz = o.getAttribute(Attributes.MOVEMENT_SPEED);
        try {
            VucutSicakligi.yaz(o, -60);
            SicaklikYoneticisi.guncelle(o, 20);
            dogrula(y, hiz.getModifier(SicaklikYoneticisi.YAVASLAMA) != null, "soğukta yavaşlama eklenmedi");
            dogrula(y, hiz.getModifier(SicaklikYoneticisi.YAVASLAMA).getAmount() < 0, "değiştirici hızı azaltmalı");
            SicaklikYoneticisi.yenidenDogdu(o);
            dogrula(y, hiz.getModifier(SicaklikYoneticisi.YAVASLAMA) == null, "respawn'da yavaşlama kalkmadı");
            VucutSicakligi.yaz(o, -60);
            SicaklikYoneticisi.guncelle(o, 20);
            dogrula(y, hiz.getModifier(SicaklikYoneticisi.YAVASLAMA) != null, "ikinci soğukta yavaşlama eklenmedi");
            kural.set(false, server);
        } catch (RuntimeException hata) {
            kural.set(true, server);
            cikar(y, List.of(o));
            throw hata;
        }
        y.runAfterDelay(5, () -> {
            try {
                dogrula(y, hiz.getModifier(SicaklikYoneticisi.YAVASLAMA) == null, "kural kapanınca yavaşlama kalkmadı");
                y.succeed();
            } finally {
                kural.set(true, server);
                cikar(y, List.of(o));
            }
        });
    }

    /** T6: kapı kapalıyken yiyecek etkisi hiçbir şey yazmaz; açıkken anında ve süreli kısım uygulanır. */
    @GameTest(template = "bos", batch = "sicaklik_etki")
    public static void yiyecekEtkisiKapiKapaliykenYokSayilir(GameTestHelper y) {
        MinecraftServer server = y.getLevel().getServer();
        GameRules.BooleanValue kural = kuraliAc(server);
        ServerPlayer o = oyuncu(y, "etki-test", 0);
        try {
            VucutSicakligi.yaz(o, 0);
            kural.set(false, server);
            boolean kapali = VucutSicakligi.etkiEkle(o, 20, 600, 30);
            kural.set(true, server);
            dogrula(y, !kapali, "kapalıyken etki uygulanmamalı");
            dogrula(y, VucutSicakligi.al(o) == 0, "kapalıyken vücut değişti: " + VucutSicakligi.al(o));
            dogrula(y, VucutSicakligi.etkiler(o).isEmpty(), "kapalıyken süreli etki kaydedildi");
            boolean acik = VucutSicakligi.etkiEkle(o, 20, 600, 30);
            dogrula(y, acik && VucutSicakligi.al(o) == 20, "açıkken anında kısım: " + VucutSicakligi.al(o));
            dogrula(y, VucutSicakligi.etkiler(o).size() == 1, "açıkken süreli etki kaydedilmeli");
            y.succeed();
        } finally {
            kural.set(true, server);
            cikar(y, List.of(o));
        }
    }
}
