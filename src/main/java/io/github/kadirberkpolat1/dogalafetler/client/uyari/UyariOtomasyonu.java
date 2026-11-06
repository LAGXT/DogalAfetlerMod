package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.uyari.HacimEgrisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariAyarlari;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariSesleri;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;

/**
 * İstemci otomasyonu (K0-21 rev. 2): klavye/fare olmadan uyarı kanıtı toplar. Yalnız {@code -Potomasyon=uyari}
 * ({@code -Ddogalafetler.otomasyon=uyari}) ile çalışır. Adımlar bir öncekinden {@code ms} sonra <b>ve</b> koşulu sağlanınca
 * çalışır (S-05). Komutlar entegre sunucuda çıktısı bastırılmış kaynakla verilir: sohbete yazı düşmez, ekran
 * görüntüsünde yalnız oyun kalır (N6). Her girdi olayı kurulumdan kapanışa log'a yazılır ve sayılır.
 *
 * <p>Ses (U-15, U-17): otomasyon dosyaya güvenmez. İstemci kurulumunda ({@code FMLClientSetupEvent}, ana thread kuyruğu;
 * ses motoru kaynak yüklemesinin sonunda, bundan sonra açılır) Ana Ses'i kendisi 0 yapar; ilk istemci tick'i yedek kapıdır.
 * Açık OpenAL aygıtı "Wave File Writer" (karışımı dosyaya yazan, hoparlörsüz aygıt) değilse Ana Ses'i 0 kaydedip oyunu
 * kapatır. Aygıt doğruysa Ana Ses %100 olur (vanilla Ana Ses 0 iken hiçbir sesi başlatmaz, siren ölçülemezdi). Seçenek
 * ekranı kapanırken ({@code OptionsSubScreen.removed()} → {@code options.save()}) Ana Ses o an 0'dır, sonraki tick'te geri
 * açılır; kapanışta 0 yazılıp kaydedilir. Böylece {@code options.txt}'e Ana Ses 1.0 hiç yazılmaz.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class UyariOtomasyonu {
    private static final String SENARYO = System.getProperty("dogalafetler.otomasyon", "");
    /** {@code uyari}: tek oyunculu tam senaryo; {@code uyari-sunucu}: dedicated server'a bağlı, sireni sunucu konsolu başlatır. */
    private static final boolean ETKIN = SENARYO.equals("uyari") || SENARYO.equals("uyari-sunucu");
    private static final String KATMAN = "uyari/ortam_test";
    private static final String KAYIT_AYGITI = "Wave File Writer";
    private static final long KOSUL_ZAMAN_ASIMI_MS = 40_000;
    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static long baslangic = -1;
    private static long oncekiAdimZamani;
    private static int sonraki;
    private static int tusSayisi;
    private static int fareSayisi;
    private static int tekerlekSayisi;
    private static int stopsoundOncesi;
    private static long tickSayaci;
    private static boolean anaSesSifirlandi;
    private static boolean aygitDogrulandi;
    private static boolean anaSesGeriAcilacak;

    private record Adim(long ms, String ad, BooleanSupplier kosul, Runnable is) {
    }

    private UyariOtomasyonu() {
    }

    /** Mod olay yolu: Ana Ses ses motoru açılmadan 0 olur (betiksiz koşuda dosyadaki değer hoparlöre ses çıkarmasın). */
    @Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Kurulum {
        private Kurulum() {
        }

        @SubscribeEvent
        public static void istemciKurulumu(FMLClientSetupEvent olay) {
            if (ETKIN) {
                olay.enqueueWork(() -> {
                    anaSes(0.0);
                    anaSesSifirlandi = true;
                    log("istemci kurulumu: Ana Ses %0 (ses motoru açılmadan, aygıt doğrulanana dek)");
                });
            }
        }
    }

    private static void plan() {
        adim(3000, "ses aygıtı denetimi, yaratıcı mod, gün, açık hava", () -> {
            if (!sesiAc()) {
                return;
            }
            komut("gamemode creative @s");
            komut("time set day");
            komut("weather clear");
        });
        adim(1000, "ortam test katmanı (20 blok doğuda, menzil 64)", UyariOtomasyonu::ortamBaslat);
        bekle("ortam katmanı çalıyor", () -> IstemciOrtam.ORNEK.caliyor(KATMAN) && IstemciOrtam.ORNEK.duzey(KATMAN) > 0.1f);
        adim(1500, "ortam düzeyi (siren öncesi)", () -> ortamLog("siren öncesi"));
        adim(500, "deprem 1 başlat", () -> komut("dogalafet start deprem 1"));
        adim(500, "siren aşamasına geç", () -> komut("dogalafet asama ileri"));
        adim(0, "çekim: siren çalarken ekranda yazı yok (40 tick)", () -> calan(40) && ekranYok(), () -> {
            cek("uyari-siren-calarken-yazi-yok");
            ortamLog("siren sırasında (-3 dB bekleniyor)");
        });
        adim(0, "Nether'e geç (45 tick; tam düzey geçişten önce ve sonra kayda girsin)", () -> calan(45),
                () -> komut("execute in minecraft:the_nether run tp @s ~ ~ ~"));
        bekle("siren Nether'de yeniden başladı", () -> SirenCalar.ORNEK.yenidenBaslatmaSayisi() >= 1);
        bekle("siren durdu (AKTIF aşama, fade-out)", UyariOtomasyonu::sustu);
        adim(500, "Overworld'e dön", () -> komut("execute in minecraft:overworld run tp @s ~ ~ ~"));
        adim(2000, "ilk afeti durdur", () -> komut("dogalafet stop"));
        bekle("Overworld yüklendi (yükleme ekranı kapandı)", UyariOtomasyonu::ekranYok);
        adim(1500, "altyazı aç, ikinci deprem", () -> {
            Minecraft.getInstance().options.showSubtitles().set(true);
            komut("dogalafet start deprem 1");
        });
        adim(500, "siren aşamasına geç", () -> komut("dogalafet asama ileri"));
        adim(0, "çekim: alt yazı açıkken (20 tick)", () -> calan(20) && ekranYok(), () -> cek("uyari-siren-altyazi-acik"));
        adim(300, "altyazı kapat", () -> Minecraft.getInstance().options.showSubtitles().set(false));
        adim(0, "stopsound @s master (50 tick)", () -> calan(50), () -> {
            stopsoundOncesi = SirenCalar.ORNEK.yenidenBaslatmaSayisi();
            komut("stopsound @s master");
        });
        bekle("siren /stopsound sonrası yeniden başladı",
                () -> SirenCalar.ORNEK.yenidenBaslatmaSayisi() > stopsoundOncesi && calan(0));
        adim(300, "ses ayarları ekranı", () -> {
            configKopyala("once");
            sesEkrani();
        });
        adim(1500, "çekim: ses ayarları", () -> cek("uyari-ses-ayarlari-sirenler"));
        adim(300, "Sirenler kaydırıcısını %40'a tıkla", () -> kaydiriciyaTikla(40));
        adim(1000, "ekranı kapat (config kaydedilir)", () -> Minecraft.getInstance().setScreen(null));
        adim(300, "config kopyası ve çalan sirenin düzeyi", () -> {
            configKopyala("sonra");
            SirenSesi etkin = SirenCalar.ORNEK.etkin();
            log("kaydırıcıdan sonra: sirenSes={} çalan siren zarf={} ses={}", UyariAyarlari.sirenSes().get(),
                    etkin == null ? "-" : SirenSesi.bicim(etkin.zarf().duzey()),
                    etkin == null ? "-" : SirenSesi.bicim(etkin.getVolume()));
        });
        bekle("ikinci siren durdu", UyariOtomasyonu::sustu);
        adim(500, "ortam katmanını durdur (40 tick fade)", () -> sunucuda(server -> OrtamSesi.durdur(server, KATMAN, 40)));
        bekle("ortam katmanı istemciden kalktı", () -> !IstemciOrtam.ORNEK.var(KATMAN));
        adim(500, "sirenSes varsayılana (28) döner", () -> {
            UyariAyarlari.sirenSes().set(UyariAyarlari.VARSAYILAN_SIREN_SES);
            UyariAyarlari.sirenSes().save();
            configKopyala("geri");
        });
        adim(500, "afeti durdur ve kapanış", () -> {
            komut("dogalafet stop");
            log("girdi sayacı: tuş={} fare={} tekerlek={}", tusSayisi, fareSayisi, tekerlekSayisi);
        });
        adim(1000, "çıkış", UyariOtomasyonu::cikis);
    }

    /** Dedicated server'a bağlı istemci: komut vermez, sunucu konsolunun başlattığı sireni bekler (ses kayda gider). */
    private static void sunucuPlani() {
        adim(500, "ses aygıtı denetimi (dedicated server'a bağlı)", () -> sesiAc());
        bekle("sunucunun sireni çalıyor (40 tick)", () -> calan(40));
        bekle("siren durdu (sunucunun durdurma paketi, fade-out)", UyariOtomasyonu::sustu);
        adim(1000, "çıkış", () -> {
            log("girdi sayacı: tuş={} fare={} tekerlek={}", tusSayisi, fareSayisi, tekerlekSayisi);
            cikis();
        });
    }

    /** Kayıt aygıtı doğruysa Ana Ses'i açar; değilse Ana Ses'i 0 kaydedip oyunu kapatır. */
    private static boolean sesiAc() {
        Minecraft mc = Minecraft.getInstance();
        String aygit = sesAygiti();
        if (!aygit.contains(KAYIT_AYGITI)) {
            DogalAfetler.LOGGER.error("[uyari-otomasyon] ses aygıtı '{}' kayıt aygıtı değil; Ana Ses %0, kapanış", aygit);
            sonraki = ADIMLAR.size();
            cikis();
            return false;
        }
        aygitDogrulandi = true;
        anaSes(1.0);
        log("ses: aygıt='{}' MASTER={} WEATHER={} AMBIENT={} altyazı={} sirenSes={} ortamTavan={}", aygit,
                mc.options.getSoundSourceVolume(SoundSource.MASTER), mc.options.getSoundSourceVolume(SoundSource.WEATHER),
                mc.options.getSoundSourceVolume(SoundSource.AMBIENT), mc.options.showSubtitles().get(),
                UyariAyarlari.sirenSes().get(), UyariAyarlari.ortamTavan());
        return true;
    }

    private static void anaSes(double deger) {
        Minecraft.getInstance().options.getSoundSourceOptionInstance(SoundSource.MASTER).set(deger);
    }

    /** Ana Ses 0 kaydedilir ve oyun kapanır (options.txt'e Ana Ses 1.0 kalmaz). */
    private static void cikis() {
        Minecraft mc = Minecraft.getInstance();
        anaSes(0.0);
        mc.options.save();
        log("Ana Ses %0 options.txt'e yazıldı; kapanış");
        mc.stop();
    }

    /** Seçenek ekranının {@code removed()}'ı seçenekleri kaydeder; Ana Ses o an 0, sonraki tick'te geri açılır. */
    @SubscribeEvent
    public static void ekranKapaniyor(ScreenEvent.Closing olay) {
        if (ETKIN && olay.getScreen() instanceof OptionsSubScreen) {
            anaSes(0.0);
            anaSesGeriAcilacak = aygitDogrulandi;
            log("seçenek ekranı kapanıyor: Ana Ses kayıt için %0");
        }
    }

    @SubscribeEvent
    public static void tik(TickEvent.ClientTickEvent olay) {
        if (!ETKIN) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!anaSesSifirlandi) {
            anaSesSifirlandi = true;
            anaSes(0.0);
            log("ilk tick: Ana Ses %0 (aygıt doğrulanana dek)");
        }
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        tickSayaci++;
        if (anaSesGeriAcilacak) {
            anaSesGeriAcilacak = false;
            anaSes(1.0);
        }
        if (baslangic < 0) {
            boolean hazir = mc.player != null && mc.level != null
                    && (SENARYO.equals("uyari-sunucu") || mc.screen == null && mc.getSingleplayerServer() != null);
            if (!hazir) {
                return;
            }
            if (SENARYO.equals("uyari")) {
                plan();
            } else {
                sunucuPlani();
            }
            baslangic = System.currentTimeMillis();
            oncekiAdimZamani = baslangic;
            tickSayaci = 0;
            log("dünyaya girildi; {} adım", ADIMLAR.size());
        }
        if (mc.screen instanceof PauseScreen) {
            DogalAfetler.LOGGER.warn("[uyari-otomasyon] otomasyonun açmadığı duraklatma menüsü kapatıldı");
            mc.setScreen(null);
        }
        long simdi = System.currentTimeMillis();
        while (sonraki < ADIMLAR.size()) {
            Adim adim = ADIMLAR.get(sonraki);
            long bekleme = simdi - oncekiAdimZamani;
            if (bekleme < adim.ms()) {
                return;
            }
            if (!adim.kosul().getAsBoolean()) {
                if (bekleme - adim.ms() < KOSUL_ZAMAN_ASIMI_MS) {
                    return;
                }
                DogalAfetler.LOGGER.warn("[uyari-otomasyon] koşul {} ms'de sağlanmadı, adım yine de çalışıyor: {}",
                        KOSUL_ZAMAN_ASIMI_MS, adim.ad());
            }
            sonraki++;
            oncekiAdimZamani = simdi;
            log("t={} ms (fps={}, tick/sn={}): {}", simdi - baslangic, mc.getFps(), simdi > baslangic
                    ? String.format(Locale.ROOT, "%.1f", tickSayaci * 1000.0 / (simdi - baslangic)) : "-", adim.ad());
            adim.is().run();
        }
    }

    @SubscribeEvent
    public static void tus(InputEvent.Key olay) {
        if (ETKIN) {
            tusSayisi++;
            DogalAfetler.LOGGER.warn("[uyari-otomasyon] GİRDİ tuş: key={} action={} isWindowActive={}", olay.getKey(),
                    olay.getAction(), Minecraft.getInstance().isWindowActive());
        }
    }

    @SubscribeEvent
    public static void fare(InputEvent.MouseButton.Pre olay) {
        if (ETKIN) {
            fareSayisi++;
            DogalAfetler.LOGGER.warn("[uyari-otomasyon] GİRDİ fare: düğme={} action={}", olay.getButton(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void tekerlek(InputEvent.MouseScrollingEvent olay) {
        if (ETKIN) {
            tekerlekSayisi++;
            DogalAfetler.LOGGER.warn("[uyari-otomasyon] GİRDİ tekerlek: {}", olay.getScrollDelta());
        }
    }

    private static void adim(long ms, String ad, Runnable is) {
        adim(ms, ad, () -> true, is);
    }

    private static void adim(long ms, String ad, BooleanSupplier kosul, Runnable is) {
        ADIMLAR.add(new Adim(ms, ad, kosul, is));
    }

    private static void bekle(String ad, BooleanSupplier kosul) {
        adim(0, ad, kosul, () -> {
        });
    }

    private static void log(String bicim, Object... degerler) {
        DogalAfetler.LOGGER.info("[uyari-otomasyon] " + bicim, degerler);
    }

    /** Ekranda menü ya da yükleme ekranı yok (boyut değişiminde "Loading terrain" kareye girmesin). */
    private static boolean ekranYok() {
        return Minecraft.getInstance().screen == null;
    }

    /** Etkin siren henüz sönmüyor ve en az {@code tick} tick geçti. */
    private static boolean calan(int tick) {
        SirenSesi ses = SirenCalar.ORNEK.etkin();
        return ses != null && !ses.isStopped() && !ses.zarf().sonduruluyor() && ses.zarf().gecenTick() >= tick;
    }

    private static boolean sustu() {
        SirenSesi ses = SirenCalar.ORNEK.etkin();
        return ses != null && ses.isStopped();
    }

    private static void sunucuda(java.util.function.Consumer<MinecraftServer> is) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        server.execute(() -> is.accept(server));
    }

    /** Entegre sunucuda oyuncu adına, çıktısı bastırılmış (sohbete yazı düşmez). */
    private static void komut(String komut) {
        String ad = Minecraft.getInstance().player.getGameProfile().getName();
        sunucuda(server -> {
            ServerPlayer oyuncu = server.getPlayerList().getPlayerByName(ad);
            int sonuc = server.getCommands().performPrefixedCommand(
                    oyuncu.createCommandSourceStack().withSuppressedOutput().withPermission(4), komut);
            log("komut /{} -> {}", komut, sonuc);
        });
    }

    /** Ölçüm için WEATHER (açık havada başka WEATHER sesi yok; AMBIENT'teki mağara/Nether sesleri kayda karışmasın). */
    private static void ortamBaslat() {
        Minecraft mc = Minecraft.getInstance();
        Vec3 konum = mc.player.position().add(20, 0, 0);
        ResourceKey<Level> boyut = mc.player.level().dimension();
        sunucuda(server -> OrtamSesi.baslat(server, KATMAN, OrtamSesi.AFETSIZ, UyariSesleri.ORTAM_TEST,
                SoundSource.WEATHER, OrtamSesi.Yer.nokta(boyut, konum, 64f), HacimEgrisi.giris(40, 1f)));
    }

    /**
     * Açık OpenAL aygıtının adı (bağlam yoksa boş). Vanilla {@code Library.getCurrentDeviceName} gibi
     * {@code ALC_ALL_DEVICES_SPECIFIER} sorulur: OpenAL Soft açık aygıt için {@code ALC_DEVICE_SPECIFIER}'da genel "OpenAL
     * Soft" adını döndürür.
     */
    private static String sesAygiti() {
        long baglam = ALC10.alcGetCurrentContext();
        if (baglam == 0L) {
            return "";
        }
        long aygit = ALC10.alcGetContextsDevice(baglam);
        String ad = ALC10.alcIsExtensionPresent(aygit, "ALC_ENUMERATE_ALL_EXT")
                ? ALC10.alcGetString(aygit, ALC11.ALC_ALL_DEVICES_SPECIFIER)
                : ALC10.alcGetString(aygit, ALC10.ALC_DEVICE_SPECIFIER);
        return ad == null ? "" : ad;
    }

    private static void ortamLog(String an) {
        log("ortam düzeyi ({}): {} (çalıyor={})", an, String.format(Locale.ROOT, "%.4f",
                IstemciOrtam.ORNEK.duzey(KATMAN)), IstemciOrtam.ORNEK.caliyor(KATMAN));
    }

    private static void cek(String ad) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, ad + ".png", mc.getMainRenderTarget(),
                mesaj -> log("ekran görüntüsü: {}", mesaj.getString()));
    }

    private static void sesEkrani() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new SoundOptionsScreen(null, mc.options));
    }

    /** Kaydırıcı düğmesine oyuncunun tıklayacağı yerden tıklar (düğme 4 piksel kenar bırakır). */
    private static void kaydiriciyaTikla(int yuzde) {
        Minecraft mc = Minecraft.getInstance();
        AbstractWidget dugme = null;
        if (mc.screen != null && SirenKaydiricisi.sonEklenen() != null) {
            for (GuiEventListener oge : mc.screen.children()) {
                if (oge instanceof OptionsList liste) {
                    dugme = liste.findOption(SirenKaydiricisi.sonEklenen());
                }
            }
        }
        if (dugme == null) {
            DogalAfetler.LOGGER.error("[uyari-otomasyon] Sirenler kaydırıcı düğmesi bulunamadı");
            return;
        }
        int once = UyariAyarlari.sirenSes().get();
        double x = dugme.getX() + 4 + (yuzde + 0.5) / 100.0 * (dugme.getWidth() - 8);
        dugme.onClick(x, dugme.getY() + dugme.getHeight() / 2.0);
        log("kaydırıcıya tıklandı: sirenSes {} -> {}, düğme \"{}\"", once, UyariAyarlari.sirenSes().get(),
                dugme.getMessage().getString());
    }

    private static void configKopyala(String ek) {
        Path oyun = Minecraft.getInstance().gameDirectory.toPath();
        try {
            Path hedef = oyun.resolve("uyari-kanit");
            Files.createDirectories(hedef);
            Path kopya = hedef.resolve("dogalafetler-client-" + ek + ".toml");
            Files.copy(oyun.resolve("config/dogalafetler-client.toml"), kopya, StandardCopyOption.REPLACE_EXISTING);
            Files.readAllLines(kopya).stream().filter(s -> s.trim().startsWith("sirenSes"))
                    .forEach(s -> log("dogalafetler-client.toml ({}): {}", ek, s.trim()));
        } catch (IOException hata) {
            DogalAfetler.LOGGER.error("[uyari-otomasyon] config kopyalanamadı", hata);
        }
    }
}
