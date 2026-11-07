package io.github.kadirberkpolat1.dogalafetler.client.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikTutulduEvent;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikcilikKayitlari;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikcilikKayitlari.Balik;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BuzBalikciligi;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BuzDeligiBlogu;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaZamanlayici;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.YiyecekKayitlari;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Balıkçılık görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=balikcilik} ile, tek oyunculu
 * Quick Play dünyasında çalışır; üretimde etkisizdir. Dünyayı {@code /dogalafet donma 60} ile dondurur, oyuncunun
 * yanında karla çevrili 7x7 donmuş gölet kurar ve sırayla: delik açılışı (sunucu yolu, gerçek Forge sağ tık olayı),
 * deliğin yakın planı, iki donma aşaması, delikteki olta (üçüncü ve birinci şahıs), gerçek ısırmayı bekleyip çekme,
 * tutulan balık elde ve balık tooltip'leri karelerini alır. Sonda vitrin anı (Ba8): gün batımında, buz yüzeyinden ~1,6 blok
 * göz yüksekliğinde deliğe hafif aşağı bakan alçak açıyla yeni atışın gerçek ısırma anı ve elde Donmuş Ringa. Tooltip karesi
 * gerçek {@code GuiGraphics.renderTooltip} ile bu sınıfın çizdiği katmandır (fare imleci oynatılmaz). Kareler A-34 kapısından
 * alınır ({@link GorselOtomasyon#kareIste}): bekleyen kare varken adım ilerlemez, karedeki sahne (delik aşaması, tooltip
 * katmanı) ancak sonraki adımda değişir. Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class BalikcilikOtomasyonu {
    private static final String MOD = System.getProperty("dogalafetler.otomasyon", "");
    private static final String ON = "BALIKCILIK_OTOMASYON ";

    private record Adim(String ad, Runnable is, BooleanSupplier hazir, int enCokTick) {
    }

    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static int tus;
    private static int fare;
    private static int teker;
    private static int sayac;
    private static int adim = -1;
    private static int adimTick;
    private static int bosTick;
    /** Göletin köşesi (x0, y yüzey buzu, z0); delik (x0+3, y, z0+3). */
    private static BlockPos kose;
    private static FishingHook olta;
    private static long atisTick;
    private static boolean tooltip;
    private static boolean dokuVitrini;
    private static boolean dinleyiciVar;

    private BalikcilikOtomasyonu() {
    }

    private static boolean aktif() {
        return MOD.equals("balikcilik");
    }

    @SubscribeEvent
    public static void tusa(InputEvent.Key olay) {
        if (aktif()) {
            tus++;
            DogalAfetler.LOGGER.warn(ON + "girdi: tuş {} eylem {}", olay.getKey(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void fareye(InputEvent.MouseButton.Pre olay) {
        if (aktif()) {
            fare++;
            DogalAfetler.LOGGER.warn(ON + "girdi: fare {} eylem {}", olay.getButton(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void tekerlege(InputEvent.MouseScrollingEvent olay) {
        if (aktif()) {
            teker++;
            DogalAfetler.LOGGER.warn(ON + "girdi: tekerlek {}", olay.getScrollDelta());
        }
    }

    /** Tooltip karesi: gerçek eşya tooltip'leri (ad + balıkçılık satırı) ekranın sol yarısında. */
    @SubscribeEvent
    public static void arayuz(RenderGuiEvent.Post olay) {
        if (!aktif()) {
            return;
        }
        if (dokuVitrini) {
            dokuVitrini(olay.getGuiGraphics());
            return;
        }
        if (!tooltip) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = olay.getGuiGraphics();
        ItemStack[] yiginlar = {new ItemStack(Balik.BUZ_ALABALIGI.cig()), new ItemStack(Balik.DONMUS_RINGA.cig()),
                new ItemStack(Balik.KUTUP_MORINASI.pismis()), new ItemStack(Balik.DONMUS_RINGA.pismis()),
                new ItemStack(BalikcilikKayitlari.BUZ_PARCASI.get())};
        g.fill(8, 8, 250, 30 + yiginlar.length * 42, 0xA0101820);
        for (int i = 0; i < yiginlar.length; i++) {
            int y = 18 + i * 42;
            g.pose().pushPose();
            g.pose().translate(16, y - 4, 0);
            g.pose().scale(2f, 2f, 1f);
            g.renderItem(yiginlar[i], 0, 0);
            g.pose().popPose();
            g.renderTooltip(mc.font, yiginlar[i], 44, y + 12);
        }
    }

    /**
     * Doku vitrini (görsel cila, istek 12a): üst sıra beş çiğ balık, orta sıra pişmişleri, alt sıra çorba ve karşılaştırma için
     * vanilla morina, somon, pişmişleri ve mantar çorbası; 4× büyütülmüş.
     */
    private static void dokuVitrini(GuiGraphics g) {
        Balik[] baliklar = Balik.values();
        List<ItemStack[]> siralar = new ArrayList<>();
        ItemStack[] cig = new ItemStack[baliklar.length];
        ItemStack[] pismis = new ItemStack[baliklar.length];
        for (int i = 0; i < baliklar.length; i++) {
            cig[i] = new ItemStack(baliklar[i].cig());
            pismis[i] = new ItemStack(baliklar[i].pismis());
        }
        siralar.add(cig);
        siralar.add(pismis);
        siralar.add(new ItemStack[] {new ItemStack(YiyecekKayitlari.esya("kutup_baligi_corbasi")), new ItemStack(Items.COD),
            new ItemStack(Items.SALMON), new ItemStack(Items.COOKED_COD), new ItemStack(Items.COOKED_SALMON),
            new ItemStack(Items.MUSHROOM_STEW)});
        g.fill(8, 8, 8 + 6 * 76 + 12, 8 + siralar.size() * 76 + 12, 0xC0202830);
        for (int s = 0; s < siralar.size(); s++) {
            ItemStack[] sira = siralar.get(s);
            for (int i = 0; i < sira.length; i++) {
                g.pose().pushPose();
                g.pose().translate(20 + i * 76, 20 + s * 76, 0);
                g.pose().scale(4f, 4f, 1f);
                g.renderItem(sira[i], 0, 0);
                g.pose().popPose();
            }
        }
    }

    private static void yakalandi(BalikTutulduEvent olay) {
        DogalAfetler.LOGGER.info(ON + "BalikTutulduEvent: {} → {} (balık {}), delik {}", olay.oyuncu().getGameProfile().getName(),
                olay.yakalananlar(), olay.balikSayisi(), olay.delik().toShortString());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (!aktif() || olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PauseScreen) {
            DogalAfetler.LOGGER.info(ON + "kendi açmadığım duraklatma ekranı kapatıldı");
            mc.setScreen(null);
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            return;
        }
        sayac++;
        bosTick = DonmaZamanlayici.kuyrukBos() ? bosTick + 1 : 0;
        if (adim < 0) {
            if (sayac < 200 || !mc.player.hasPermissions(2)) {
                return;
            }
            if (!dinleyiciVar) {
                MinecraftForge.EVENT_BUS.addListener(BalikcilikOtomasyonu::yakalandi);
                dinleyiciVar = true;
            }
            plan(mc);
            adim = 0;
            adimTick = 0;
            DogalAfetler.LOGGER.info(ON + "başlıyor: {} adım", ADIMLAR.size());
            ADIMLAR.get(0).is().run();
            return;
        }
        if (adim >= ADIMLAR.size() || GorselOtomasyon.kareBekliyor()) {
            return;
        }
        adimTick++;
        Adim a = ADIMLAR.get(adim);
        boolean hazir = a.hazir().getAsBoolean();
        if (hazir || adimTick >= a.enCokTick()) {
            if (!hazir) {
                DogalAfetler.LOGGER.warn(ON + "{}: {} tick'te koşul sağlanmadı, devam", a.ad(), adimTick);
            }
            adim++;
            adimTick = 0;
            if (adim < ADIMLAR.size()) {
                ADIMLAR.get(adim).is().run();
            }
        }
    }

    private static void ekle(String ad, Runnable is, BooleanSupplier hazir, int enCokTick) {
        ADIMLAR.add(new Adim(ad, is, hazir, enCokTick));
    }

    private static void komut(Minecraft mc, String k) {
        DogalAfetler.LOGGER.info(ON + "komut /{}", k);
        mc.player.connection.sendCommand(k);
    }

    private static BooleanSupplier bekle(int tick) {
        return () -> adimTick >= tick;
    }

    private static BooleanSupplier yetisti(int enAz) {
        return () -> adimTick >= enAz && bosTick >= 60;
    }

    private static MinecraftServer sunucu(Minecraft mc) {
        return mc.getSingleplayerServer();
    }

    private static ServerPlayer sunucuOyuncusu(MinecraftServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    private static BlockPos delik() {
        return kose.offset(3, 0, 3);
    }

    private static void plan(Minecraft mc) {
        ekle("kurulum", () -> {
            GorselOtomasyon.cizimAyari();
            mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
            mc.options.hideGui = true;
            for (String k : new String[] {"gamemode creative", "gamerule doDaylightCycle false", "time set 9500",
                    "weather clear 100000", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                    "dogalafet donma 60 donma"}) {
                komut(mc, k);
            }
        }, yetisti(100), 3600);
        ekle("gölet kur", () -> {
            MinecraftServer s = sunucu(mc);
            kose = s.submit(() -> goletKur(s)).join();
            DogalAfetler.LOGGER.info(ON + "gölet köşesi {} delik {}", kose.toShortString(), delik().toShortString());
            bak(mc, delik().getX() - 2.0, delik().getY() + 1, delik().getZ() + 0.5);
        }, bekle(80), 100);
        ekle("delik aç", () -> {
            MinecraftServer s = sunucu(mc);
            String sonuc = s.submit(() -> {
                ServerPlayer o = sunucuOyuncusu(s);
                ItemStack kazma = new ItemStack(Items.DIAMOND_PICKAXE);
                o.setItemInHand(InteractionHand.MAIN_HAND, kazma);
                o.setShiftKeyDown(true);
                BlockPos p = delik();
                var e = ForgeHooks.onRightClickBlock(o, InteractionHand.MAIN_HAND, p,
                        new BlockHitResult(Vec3.upFromBottomCenterOf(p, 1.0), Direction.UP, p, false));
                o.setShiftKeyDown(false);
                return "iptal=" + e.isCanceled() + " sonuç=" + e.getCancellationResult() + " blok="
                        + o.serverLevel().getBlockState(p) + " zamanlanmış="
                        + o.serverLevel().getBlockTicks().hasScheduledTick(p, BalikcilikKayitlari.BUZ_DELIGI.get());
            }).join();
            DogalAfetler.LOGGER.info(ON + "delik açıldı: {}", sonuc);
        }, bekle(3), 5);
        ekle("açılış karesi", () -> kare(mc, "acilis"), bekle(40), 60);
        ekle("yakın plan", () -> bak(mc, delik().getX() - 0.9, delik().getY() + 1, delik().getZ() + 0.5), bekle(40), 60);
        ekle("yakın plan karesi", () -> kare(mc, "delik_yakin"), bekle(10), 20);
        ekle("donma aşama 2", () -> asama(mc, 2), bekle(20), 30);
        ekle("donma aşama 2 karesi", () -> kare(mc, "delik_donuyor_2"), bekle(10), 20);
        ekle("donma aşama 3", () -> asama(mc, 3), bekle(20), 30);
        ekle("donma aşama 3 karesi", () -> kare(mc, "delik_donuyor_3"), bekle(20), 30);
        ekle("olta at", () -> {
            asama(mc, 0);
            bak(mc, delik().getX() - 2.6, delik().getY() + 1, delik().getZ() + 0.5);
            MinecraftServer s = sunucu(mc);
            olta = s.submit(() -> oltaAt(s)).join();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }, oltaDelikte(mc), 200);
        ekle("olta üçüncü şahıs", () -> {
            DogalAfetler.LOGGER.info(ON + "olta delikte yüzüyor; kalan bekleme {} tick",
                    sunucu(mc).submit(() -> BuzBalikciligi.kalanBekleme(olta)).join());
            kare(mc, "olta_delikte_ucuncu_sahis");
        }, bekle(10), 20);
        ekle("olta birinci şahıs", () -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
        }, bekle(20), 30);
        ekle("olta birinci şahıs karesi", () -> kare(mc, "olta_delikte_birinci_sahis"), bekle(5), 10);
        ekle("gerçek ısırmayı bekle", () -> { }, () -> sunucu(mc).submit(() -> isirmaVar(olta)).join(), 1500);
        ekle("ısırma", () -> {
            long gecen = sunucu(mc).submit(() -> sunucuOyuncusu(sunucu(mc)).serverLevel().getGameTime() - atisTick).join();
            DogalAfetler.LOGGER.info(ON + "ısırma: atıştan {} tick ({} sn) sonra", gecen,
                    String.format(Locale.ROOT, "%.1f", gecen / 20.0));
            kare(mc, "isirma");
        }, bekle(2), 5);
        ekle("çek", () -> {
            MinecraftServer s = sunucu(mc);
            int hasar = s.submit(() -> olta.retrieve(sunucuOyuncusu(s).getMainHandItem())).join();
            DogalAfetler.LOGGER.info(ON + "çekildi; olta hasarı {}", hasar);
        }, bekle(4), 6);
        ekle("çekiliş karesi", () -> kare(mc, "balik_cekiliyor"), bekle(40), 60);
        ekle("balık elde", () -> {
            MinecraftServer s = sunucu(mc);
            String ad = s.submit(() -> {
                ServerPlayer o = sunucuOyuncusu(s);
                for (int i = 0; i < o.getInventory().getContainerSize(); i++) {
                    ItemStack y = o.getInventory().getItem(i);
                    if (!y.isEmpty() && !y.is(Items.FISHING_ROD) && !y.is(Items.DIAMOND_PICKAXE)) {
                        o.setItemInHand(InteractionHand.MAIN_HAND, y.copy());
                        return y.toString();
                    }
                }
                return "envanterde yakalanan yok";
            }).join();
            DogalAfetler.LOGGER.info(ON + "elde: {}", ad);
        }, bekle(20), 30);
        ekle("balık elde karesi", () -> kare(mc, "balik_elde"), bekle(10), 20);
        ekle("tooltip'ler", () -> tooltip = true, bekle(10), 20);
        ekle("tooltip karesi", () -> kare(mc, "tooltipler"), bekle(5), 10);
        ekle("doku vitrini", () -> {
            tooltip = false;
            dokuVitrini = true;
        }, bekle(10), 20);
        ekle("doku vitrini karesi", () -> kare(mc, "doku_vitrini"), bekle(5), 10);
        ekle("vitrin kamera", () -> {
            dokuVitrini = false;
            mc.options.hideGui = true;
            komut(mc, "time set 12500");
            bak(mc, delik().getX() + 5.0, delik().getY() + 1, delik().getZ() + 0.5);
        }, bekle(40), 60);
        ekle("vitrin olta", () -> {
            MinecraftServer s = sunucu(mc);
            olta = s.submit(() -> oltaAt(s)).join();
        }, oltaDelikte(mc), 200);
        ekle("vitrin gerçek ısırmayı bekle", () -> { }, () -> sunucu(mc).submit(() -> isirmaVar(olta)).join(), 1500);
        ekle("vitrin ısırma karesi", () -> kare(mc, "vitrin_isirma"), bekle(2), 5);
        ekle("vitrin çek", () -> {
            MinecraftServer s = sunucu(mc);
            int hasar = s.submit(() -> olta.retrieve(sunucuOyuncusu(s).getMainHandItem())).join();
            DogalAfetler.LOGGER.info(ON + "vitrin: çekildi; olta hasarı {}", hasar);
        }, bekle(40), 60);
        ekle("vitrin ringa", () -> {
            MinecraftServer s = sunucu(mc);
            s.submit(() -> sunucuOyuncusu(s).setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(Balik.DONMUS_RINGA.cig()))).join();
            mc.options.hideGui = false;
        }, bekle(20), 30);
        ekle("vitrin ringa karesi", () -> kare(mc, "vitrin_ringa"), bekle(10), 20);
        ekle("kapanış", () -> {
            komut(mc, "forge tps");
            DogalAfetler.LOGGER.info(ON + "bitti; FPS {}", mc.getFps());
        }, bekle(40), 60);
        ekle("çıkış", () -> {
            mc.options.chatVisibility().set(ChatVisiblity.FULL);
            GorselOtomasyon.kareOzetiYaz("balikcilik");
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            mc.stop();
        }, bekle(1), 2);
    }

    /** Sunucu thread'inde: olta ele verilir, şamandıra deliğin üstünden suya bırakılır. */
    private static FishingHook oltaAt(MinecraftServer s) {
        ServerPlayer o = sunucuOyuncusu(s);
        o.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FISHING_ROD));
        FishingHook h = new FishingHook(o, o.serverLevel(), 0, 0);
        BlockPos p = delik();
        h.moveTo(p.getX() + 0.5, p.getY() + 1.4, p.getZ() + 0.5, 0f, 0f);
        h.setDeltaMovement(0, -0.12, 0);
        o.serverLevel().addFreshEntity(h);
        atisTick = o.serverLevel().getGameTime();
        return h;
    }

    /** En az 40 tick geçti ve şamandıra delikteki suda yüzüyor. */
    private static BooleanSupplier oltaDelikte(Minecraft mc) {
        return () -> adimTick >= 40 && sunucu(mc).submit(() -> olta.isInWater() && BuzBalikciligi.delikteMi(olta)).join();
    }

    private static boolean isirmaVar(FishingHook h) {
        try {
            Field f = ObfuscationReflectionHelper.findField(FishingHook.class, "f_37089_");
            return !h.isRemoved() && f.getInt(h) > 0;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private static void asama(Minecraft mc, int a) {
        MinecraftServer s = sunucu(mc);
        s.submit(() -> {
            ServerLevel l = s.overworld();
            BlockState d = l.getBlockState(delik());
            if (d.is(BalikcilikKayitlari.BUZ_DELIGI.get())) {
                l.setBlock(delik(), d.setValue(BuzDeligiBlogu.ASAMA, a), Block.UPDATE_CLIENTS);
            }
        }).join();
        DogalAfetler.LOGGER.info(ON + "delik aşaması {}", a);
    }

    /**
     * Oyuncunun 4 blok doğusunda karla çevrili 7x7 donmuş gölet (yüzey buz, altında iki blok su) ve batı kıyısında
     * yanan kamp ateşi. Dönüş: göletin kuzeybatı köşesi (buz seviyesinde).
     */
    private static BlockPos goletKur(MinecraftServer s) {
        ServerLevel l = s.overworld();
        BlockPos o = sunucuOyuncusu(s).blockPosition();
        int x0 = o.getX() + 4;
        int z0 = o.getZ() - 3;
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x0 + 3, z0 + 3) - 1;
        for (int x = x0 - 6; x <= x0 + 12; x++) {
            for (int z = z0 - 6; z <= z0 + 12; z++) {
                for (int yy = y - 5; yy <= y + 14; yy++) {
                    Block b = yy > y ? Blocks.AIR : yy == y ? Blocks.SNOW_BLOCK : Blocks.DIRT;
                    l.setBlock(new BlockPos(x, yy, z), b.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int x = x0; x <= x0 + 6; x++) {
            for (int z = z0; z <= z0 + 6; z++) {
                l.setBlock(new BlockPos(x, y - 2, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                l.setBlock(new BlockPos(x, y - 1, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                l.setBlock(new BlockPos(x, y, z), Blocks.ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        l.setBlock(new BlockPos(x0 - 2, y + 1, z0 + 5), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true),
                Block.UPDATE_ALL);
        return new BlockPos(x0, y, z0);
    }

    /** Oyuncuyu ayak konumuna ışınlar ve deliğin su yüzeyinin ortasına baktırır. */
    private static void bak(Minecraft mc, double x, double y, double z) {
        BlockPos d = delik();
        double hx = d.getX() + 0.5;
        double hy = d.getY() + 0.9;
        double hz = d.getZ() + 0.5;
        double gozY = y + 1.62;
        double dx = hx - x;
        double dz = hz - z;
        double yatay = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(gozY - hy, yatay));
        komut(mc, String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
    }

    private static void kare(Minecraft mc, String ad) {
        GorselOtomasyon.kareIste("balikcilik", "balikcilik_" + ad + ".png", k -> DogalAfetler.LOGGER.info(
                ON + "kare {} (FPS {}, oyuncu {})", k.dosya(), k.fps(), mc.player.blockPosition().toShortString()));
    }
}
