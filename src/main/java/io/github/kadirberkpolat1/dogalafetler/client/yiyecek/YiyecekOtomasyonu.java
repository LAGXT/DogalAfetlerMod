package io.github.kadirberkpolat1.dogalafetler.client.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.sicaklik.IstemciSicaklik;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.SicaklikYiyecegi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=yiyecek} ile çalışır; üretimde etkisizdir.
 * Tek oyunculu dünyada: soğuk vücutla "önce" karesi; Ateşli Güveç, Tavuk Çorbası ve Salep gerçek yeme yoluyla yenir
 * (kullan tuşunun durumu programla basılı tutulur: istemci → sunucu eşya kullanma paketi, 32 tick, sunucuda
 * {@code finishUsingItem}); HUD'da efekt ikonları ve barın hareketi, envanterde efekt adları ve tooltip, 30 sn sonra süreli
 * ısınma, Ayran'la "Serinlik", ikinci Salep'le azalan verim. Tooltip için fare konumu yalnız oyunun iç durumunda
 * ({@code MouseHandler}) ayarlanır; işletim sistemi imleci oynatılmaz. Klavye, fare ve tekerlek girdileri kapanışa dek
 * sayılır (sıfırdan farklıysa koşu geçersiz).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class YiyecekOtomasyonu {
    private static final String MOD = System.getProperty("dogalafetler.otomasyon", "");
    private static final String ON = "YIYECEK_OTOMASYON ";
    /** Hotbar sırası (give sırasıyla dolar). */
    private static final List<String> HOTBAR = List.of("atesli_guvec", "sicak_tavuk_corbasi", "sicak_salep", "ayran",
            "meyve_serbeti", "isitici_cay", "sicak_cikolata", "buzlu_karpuz", "kutup_baligi_corbasi");
    private static final int GUVEC = 0;
    private static final int CORBA = 1;
    private static final int SALEP = 2;
    private static final int AYRAN = 3;

    private static int tus;
    private static int fare;
    private static int teker;
    private static int sayac;
    private static boolean basladi;
    private static final List<BooleanSupplier> ADIMLAR = new ArrayList<>();
    private static int adim;
    private static int adimSayac;

    /** Süren yeme: slot, baştaki adet, kullanım görüldü mü, ara kare alınacak mı. */
    private static int yemeSlot = -1;
    private static int yemeOnce;
    private static boolean kullanimGoruldu;
    private static String yemeKaresi;
    private static float yemeOncesiVucut;
    private static final List<String> ARTISLAR = new ArrayList<>();

    private YiyecekOtomasyonu() {
    }

    private static boolean aktif() {
        return MOD.equals("yiyecek");
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

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (!aktif()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (olay.phase == TickEvent.Phase.START) {
            // Tuş durumu tuş işlemeden (handleKeybinds) önce bırakılır: yeme bitince ikinci kez başlamasın.
            yemeyiIzle(mc);
            return;
        }
        if (mc.screen instanceof PauseScreen) {
            DogalAfetler.LOGGER.info(ON + "kendi açmadığım duraklatma ekranı kapatıldı");
            mc.setScreen(null);
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (mc.screen instanceof DeathScreen) {
            mc.player.respawn();
            mc.setScreen(null);
        }
        sayac++;
        if (!basladi) {
            if (!mc.player.hasPermissions(2) || !IstemciSicaklik.acik() || sayac < 100) {
                return;
            }
            basladi = true;
            DogalAfetler.LOGGER.info(ON + "başlıyor: oyuncu {}, OP, HUD paketi geldi", mc.player.getGameProfile().getName());
            senaryoyuKur(mc);
        }
        if (adim < ADIMLAR.size() && ADIMLAR.get(adim).getAsBoolean()) {
            adim++;
            adimSayac = 0;
        } else {
            adimSayac++;
        }
    }

    private static void senaryoyuKur(Minecraft mc) {
        String ad = mc.player.getGameProfile().getName();
        // Yeni oyuncuda boş hedefe effect clear / clear kırmızı hata yazar (sendCommandFeedback hatayı kapatmaz): yalnız gerekirse.
        List<String> komutlar = new ArrayList<>(List.of("gamerule sendCommandFeedback false", "time set 6000",
                "weather clear 6000", "gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
                "gamemode survival"));
        if (!mc.player.getActiveEffects().isEmpty()) {
            komutlar.add("effect clear @s");
        }
        if (!mc.player.getInventory().isEmpty()) {
            komutlar.add("clear @s");
        }
        for (String esya : HOTBAR) {
            komutlar.add("give @s " + DogalAfetler.MOD_ID + ":" + esya + " " + (esya.equals("sicak_salep") ? 3 : 2));
        }
        komutlar.add("dogalafet sicaklik set " + ad + " -45");
        for (String k : komutlar) {
            ADIMLAR.add(() -> {
                mc.player.connection.sendCommand(k);
                return true;
            });
        }
        ADIMLAR.add(() -> bekle(80));
        ADIMLAR.add(() -> {
            mc.player.setXRot(-6f);
            mc.player.getInventory().selected = GUVEC;
            return true;
        });
        ADIMLAR.add(() -> bekle(60));
        ADIMLAR.add(() -> {
            mc.gui.getChat().clearMessages(false);
            return true;
        });
        ADIMLAR.add(() -> bekle(5));
        ADIMLAR.add(() -> kare(mc, "once"));
        ADIMLAR.add(() -> ye(mc, GUVEC, "yerken"));
        ADIMLAR.add(() -> yemeBitti());
        ADIMLAR.add(() -> ye(mc, CORBA, null));
        ADIMLAR.add(() -> yemeBitti());
        ADIMLAR.add(() -> ye(mc, SALEP, null));
        ADIMLAR.add(() -> yemeBitti());
        ADIMLAR.add(() -> bekle(60));
        ADIMLAR.add(() -> kare(mc, "yedi"));
        ADIMLAR.add(() -> envanteriAc(mc));
        ADIMLAR.add(() -> fareyiSlotaGetir(mc, GUVEC));
        ADIMLAR.add(() -> bekle(60));
        ADIMLAR.add(() -> kare(mc, "envanter_guvec"));
        ADIMLAR.add(() -> fareyiSlotaGetir(mc, AYRAN));
        ADIMLAR.add(() -> bekle(60));
        ADIMLAR.add(() -> kare(mc, "envanter_ayran"));
        ADIMLAR.add(() -> {
            mc.setScreen(null);
            return true;
        });
        ADIMLAR.add(() -> bekle(400));
        ADIMLAR.add(() -> kare(mc, "sonra"));
        ADIMLAR.add(() -> ye(mc, AYRAN, null));
        ADIMLAR.add(() -> yemeBitti());
        ADIMLAR.add(() -> bekle(60));
        ADIMLAR.add(() -> kare(mc, "serinlik"));
        ADIMLAR.add(() -> ye(mc, SALEP, null));
        ADIMLAR.add(() -> yemeBitti());
        ADIMLAR.add(() -> {
            DogalAfetler.LOGGER.info(ON + "anında artışlar (istemci paketi; sıra: güveç, çorba, salep, ayran, ikinci salep): {}",
                    String.join(", ", ARTISLAR));
            DogalAfetler.LOGGER.info(ON + "bitti; FPS {}", mc.getFps());
            return true;
        });
        ADIMLAR.add(() -> bekle(100));
        ADIMLAR.add(() -> {
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            mc.stop();
            return true;
        });
    }

    private static boolean bekle(int tick) {
        return adimSayac >= tick;
    }

    /** Slotu seçer ve kullan tuşunu basılı tutar; {@link #yemeyiIzle} yeme bitince bırakır. */
    private static boolean ye(Minecraft mc, int slot, String araKare) {
        mc.player.getInventory().selected = slot;
        yemeSlot = slot;
        yemeOnce = mc.player.getInventory().getItem(slot).getCount();
        kullanimGoruldu = false;
        yemeKaresi = araKare;
        yemeOncesiVucut = IstemciSicaklik.durum().vucut();
        mc.options.keyUse.setDown(true);
        DogalAfetler.LOGGER.info(ON + "yeme başlıyor: {} (slot {}, adet {}), vücut {}", HOTBAR.get(slot), slot, yemeOnce,
                sayi(yemeOncesiVucut));
        return true;
    }

    private static boolean yemeBitti() {
        if (yemeSlot < 0) {
            return true;
        }
        if (adimSayac > 300) {
            Minecraft.getInstance().options.keyUse.setDown(false);
            DogalAfetler.LOGGER.warn(ON + "{} 300 tick'te yenmedi", HOTBAR.get(yemeSlot));
            yemeSlot = -1;
            return true;
        }
        return false;
    }

    private static void yemeyiIzle(Minecraft mc) {
        if (yemeSlot < 0 || mc.player == null) {
            return;
        }
        if (mc.player.isUsingItem()) {
            kullanimGoruldu = true;
            if (yemeKaresi != null && mc.player.getUseItemRemainingTicks() <= 14) {
                kare(mc, yemeKaresi);
                yemeKaresi = null;
            }
            return;
        }
        ItemStack yigin = mc.player.getInventory().getItem(yemeSlot);
        int adet = yigin.getItem() instanceof SicaklikYiyecegi ? yigin.getCount() : 0;
        if (kullanimGoruldu && adet < yemeOnce) {
            mc.options.keyUse.setDown(false);
            float artis = IstemciSicaklik.durum().vucut() - yemeOncesiVucut;
            ARTISLAR.add(HOTBAR.get(yemeSlot) + " " + sayi(artis));
            DogalAfetler.LOGGER.info(ON + "yendi: {} → kalan {}, vücut {} (anında {}), kademe {}, efektler [{}]", HOTBAR.get(yemeSlot),
                    adet, sayi(IstemciSicaklik.durum().vucut()), sayi(artis), IstemciSicaklik.durum().kademe(), efektler(mc));
            yemeSlot = -1;
        }
    }

    private static boolean envanteriAc(Minecraft mc) {
        mc.setScreen(new InventoryScreen(mc.player));
        return true;
    }

    /** Tooltip için yalnız oyunun fare durumunu hotbar slotunun ortasına koyar (OS imleci değişmez). */
    private static boolean fareyiSlotaGetir(Minecraft mc, int slot) {
        if (!(mc.screen instanceof InventoryScreen ekran)) {
            return adimSayac > 40;
        }
        double guiX = ekran.getGuiLeft() + 8 + slot * 18 + 8;
        double guiY = ekran.getGuiTop() + 142 + 8;
        double x = guiX * mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth();
        double y = guiY * mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight();
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, mc.mouseHandler, x, "f_91507_");
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, mc.mouseHandler, y, "f_91508_");
        ItemStack yigin = mc.player.getInventory().getItem(slot);
        String satirlar = yigin.getTooltipLines(mc.player, TooltipFlag.NORMAL).stream().map(Component::getString)
                .collect(Collectors.joining(" | "));
        DogalAfetler.LOGGER.info(ON + "tooltip slot {} ({}): {}", slot, HOTBAR.get(slot), satirlar);
        return true;
    }

    private static String efektler(Minecraft mc) {
        return mc.player.getActiveEffects().stream()
                .map(e -> Component.translatable(e.getEffect().getDescriptionId()).getString() + " " + (e.getAmplifier() + 1)
                        + " " + e.getDuration() / 20 + "sn")
                .sorted().collect(Collectors.joining(", "));
    }

    private static boolean kare(Minecraft mc, String ad) {
        String dosya = "yiyecek_" + ad + ".png";
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> { });
        DogalAfetler.LOGGER.info(ON + "kare {} (vücut {} kademe {}, ortam {}, efektler [{}], FPS {})", dosya,
                sayi(IstemciSicaklik.durum().vucut()), IstemciSicaklik.durum().kademe(), sayi(IstemciSicaklik.durum().ortam()),
                efektler(mc), mc.getFps());
        return true;
    }

    private static String sayi(float f) {
        return String.format(Locale.ROOT, "%.2f", f);
    }
}
