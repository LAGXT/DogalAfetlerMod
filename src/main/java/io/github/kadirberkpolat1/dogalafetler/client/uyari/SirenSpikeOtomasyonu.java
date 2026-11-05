package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenKategorisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenTuru;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Spike otomasyonu (K0-21): klavye/fare olmadan kanıt toplar. Yalnız {@code -Pspike=siren}, {@code siren-kategori} ya da
 * {@code siren-boyut} ({@code -Ddogalafetler.spike}) ile çalışır; aksi halde hiçbir şey yapmaz. Her adım bir
 * öncekinden {@code ms} sonra ve koşulu sağlanınca çalışır; ilk adım dünyaya girişten (oyuncu var, ekran kapalı) sayılır.
 * Koşullar siren durumuna bağlıdır, böylece yüklü makinede istemci yavaş dönse de sıra bozulmaz.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SirenSpikeOtomasyonu {
    private static final String SENARYO = System.getProperty("dogalafetler.spike", "");
    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static final long KOSUL_ZAMAN_ASIMI_MS = 40_000;
    private static long baslangic = -1;
    private static long oncekiAdimZamani = -1;
    private static int sonraki;
    private static Boolean sonPencereEtkin;

    private record Adim(long ms, String ad, BooleanSupplier kosul, Runnable is) {
    }

    static {
        switch (SENARYO) {
            case "siren" -> secilenYolPlani();
            case "siren-kategori" -> gercekKategoriPlani();
            case "siren-boyut" -> boyutPlani();
            default -> {
            }
        }
    }

    private SirenSpikeOtomasyonu() {
    }

    /** N5 yolu: siren MASTER + sirenSes; çalarken yazı yok, altyazı yalnız açıkken; fade-out; ses ayarları ekranı. */
    private static void secilenYolPlani() {
        adim(3000, "standart siren 10 sn", () -> komut("dogalafet spike siren standart 10"));
        adim(2000, "çekim: siren çalarken (altyazı kapalı)", () -> cek("siren-calarken-yazi-yok"));
        bekle("standart siren kendiliğinden bitti", () -> sustu(SirenTuru.STANDART));
        adim(500, "altyazı aç", () -> altyazi(true));
        adim(500, "güneş sireni 20 sn", () -> komut("dogalafet spike siren gunes 20"));
        adim(0, "çekim: altyazı açıkken (güneş sireni 30 tick çaldı)", () -> calan(SirenTuru.GUNES, 30),
                () -> cek("siren-gunes-yalniz-altyazi"));
        adim(500, "altyazı kapat", () -> altyazi(false));
        adim(0, "siren dur (fade-out; güneş sireni 80 tick çaldı)", () -> calan(SirenTuru.GUNES, 80),
                () -> komut("dogalafet spike siren dur"));
        adim(500, "ses ayarları ekranı", () -> sustu(SirenTuru.GUNES), SirenSpikeOtomasyonu::sesEkrani);
        adim(1500, "çekim: ses ayarları", () -> cek("ses-ayarlari-sirenler"));
        adim(500, "ekranı kapat", () -> Minecraft.getInstance().setScreen(null));
        adim(500, "kapanış", SirenSpikeOtomasyonu::kapat);
    }

    /**
     * Reddedilen yol: mixin ile eklenen gerçek kategori; vanilla kaydırıcı, options.txt, yeni sirenin çalan sireni
     * tam düzeydeyken kesmesi ve standart sirenin kendiliğinden bitmesi.
     */
    private static void gercekKategoriPlani() {
        adim(3000, "ses ayarları ekranı", SirenSpikeOtomasyonu::sesEkrani);
        adim(1500, "çekim: ses ayarları (gerçek kategori)", () -> cek("ses-ayarlari-gercek-kategori"));
        adim(500, "ekranı kapat", () -> Minecraft.getInstance().setScreen(null));
        adim(500, "güneş sireni 20 sn", () -> komut("dogalafet spike siren gunes 20"));
        adim(0, "standart siren güneşi keser", () -> calan(SirenTuru.GUNES, 60),
                () -> komut("dogalafet spike siren standart 10"));
        bekle("standart siren kendiliğinden bitti", () -> sustu(SirenTuru.STANDART));
        adim(500, "options.txt kaydet", SirenSpikeOtomasyonu::secenekleriKaydet);
        adim(500, "kapanış", SirenSpikeOtomasyonu::kapat);
    }

    /**
     * İnceleme B1: boyut değişimi ve {@code /stopsound} bütün sesleri keser; siren aynı zarfla yeniden başlamalı. B6:
     * "Sirenler" kaydırıcısı %40'a çekilince çalan sirenin düzeyi ve {@code dogalafetler-client.toml} değişmeli.
     */
    private static void boyutPlani() {
        adim(3000, "yaratıcı mod (Nether'de blok içinde boğulma hasarı olmasın)", () -> komut("gamemode creative @s"));
        adim(500, "güneş sireni 60 sn", () -> komut("dogalafet spike siren gunes 60"));
        adim(5000, "Nether'e geç (güneş sireni 100 tick çaldı)", () -> calan(SirenTuru.GUNES, 100),
                () -> komut("execute in minecraft:the_nether run tp @s ~ ~ ~"));
        bekle("siren Nether'de yeniden başladı", () -> yenidenBaslatma(1));
        adim(5000, "Overworld'e dön", () -> komut("execute in minecraft:overworld run tp @s ~ ~ ~"));
        bekle("siren Overworld'de yeniden başladı", () -> yenidenBaslatma(2));
        adim(3000, "stopsound @s master", () -> komut("stopsound @s master"));
        bekle("siren /stopsound sonrası yeniden başladı", () -> yenidenBaslatma(3));
        adim(2000, "ses ayarları ekranı", SirenSpikeOtomasyonu::sesEkrani);
        adim(1000, "Sirenler kaydırıcısını %40'a tıkla", () -> kaydiriciyaTikla(40));
        adim(2500, "ekranı kapat (config kaydedilir)", () -> Minecraft.getInstance().setScreen(null));
        adim(500, "config dosyası", SirenSpikeOtomasyonu::configSatiri);
        adim(1500, "siren dur (fade-out)", () -> komut("dogalafet spike siren dur"));
        bekle("güneş sireni durdu", () -> sustu(SirenTuru.GUNES));
        adim(500, "sirenSes varsayılana (28) döner", () -> {
            IstemciAyarlari.SIREN_SES.set(28);
            IstemciAyarlari.SIREN_SES.save();
            configSatiri();
        });
        adim(500, "kapanış", SirenSpikeOtomasyonu::kapat);
    }

    @SubscribeEvent
    public static void tik(TickEvent.ClientTickEvent olay) {
        if (ADIMLAR.isEmpty() || olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean pencereEtkin = mc.isWindowActive();
        if (sonPencereEtkin == null || sonPencereEtkin != pencereEtkin) {
            DogalAfetler.LOGGER.info("[spike-siren] pencere odağı (isWindowActive): {}", pencereEtkin);
            sonPencereEtkin = pencereEtkin;
        }
        if (baslangic < 0) {
            if (mc.player == null || mc.level == null || mc.screen != null) {
                return;
            }
            baslangic = System.currentTimeMillis();
            oncekiAdimZamani = baslangic;
            DogalAfetler.LOGGER.info("[spike-siren] dünyaya girildi; senaryo={}, SoundSource.values()={}", SENARYO,
                    Arrays.toString(SoundSource.values()));
        }
        // Pencereye ulaşan bir Escape duraklatma menüsü açarsa tek oyunculu oyun durur ve zaman çizelgesi bozulur;
        // otomasyon klavye beklemediği için onu kapatır. Boyut değişimindeki yükleme ekranına dokunulmaz.
        if (mc.screen instanceof PauseScreen) {
            DogalAfetler.LOGGER.warn("[spike-siren] otomasyonun açmadığı duraklatma menüsü kapatıldı");
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
                DogalAfetler.LOGGER.warn("[spike-siren] koşul {} ms'de sağlanmadı, adım yine de çalışıyor: {}",
                        KOSUL_ZAMAN_ASIMI_MS, adim.ad());
            }
            sonraki++;
            oncekiAdimZamani = simdi;
            DogalAfetler.LOGGER.info("[spike-siren] t={} ms: {}", simdi - baslangic, adim.ad());
            adim.is().run();
        }
    }

    /** Duraklatma menüsünü açan Escape'in kaynağını bulmak için her tuş olayı pencerenin odağıyla birlikte log'a yazılır. */
    @SubscribeEvent
    public static void tus(InputEvent.Key olay) {
        if (ADIMLAR.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        DogalAfetler.LOGGER.info("[spike-siren] tuş olayı: key={} scancode={} action={} mods={} isWindowActive={} ekran={}",
                olay.getKey(), olay.getScanCode(), olay.getAction(), olay.getModifiers(), mc.isWindowActive(),
                mc.screen == null ? "yok" : mc.screen.getClass().getSimpleName());
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

    /** Etkin siren bu türde, hâlâ çalıyor ve en az {@code tick} tick geçti. */
    private static boolean calan(SirenTuru tur, int tick) {
        SirenSesi ses = SirenCalar.ORNEK.etkin();
        return ses != null && !ses.isStopped() && ses.tur() == tur && ses.gecenTick() >= tick;
    }

    /** Son gelen siren bu türdeydi ve durdu. */
    private static boolean sustu(SirenTuru tur) {
        SirenSesi ses = SirenCalar.ORNEK.etkin();
        return ses != null && ses.tur() == tur && ses.isStopped();
    }

    private static boolean yenidenBaslatma(int enAz) {
        SirenSesi ses = SirenCalar.ORNEK.etkin();
        return SirenCalar.ORNEK.yenidenBaslatmaSayisi() >= enAz && ses != null && !ses.isStopped();
    }

    private static void komut(String komut) {
        Minecraft.getInstance().player.connection.sendCommand(komut);
    }

    private static void cek(String ad) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "spike-siren-" + ad + ".png", mc.getMainRenderTarget(),
                mesaj -> DogalAfetler.LOGGER.info("[spike-siren] ekran görüntüsü: {}", mesaj.getString()));
    }

    private static void altyazi(boolean acik) {
        Minecraft.getInstance().options.showSubtitles().set(acik);
    }

    private static void sesEkrani() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new SoundOptionsScreen(null, mc.options));
        DogalAfetler.LOGGER.info("[spike-siren] ses ayarları açıldı; sirenSes={}, gerçek kategori={}",
                IstemciAyarlari.SIREN_SES.get(),
                SirenKategorisi.gercek().map(k -> k.getName() + " = " + mc.options.getSoundSourceVolume(k)).orElse("yok"));
    }

    /** Kaydırıcı düğmesine oyuncunun tıklayacağı yerden tıklar (düğme 4 piksel kenar bırakır; değer floor(100 · konum)). */
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
            DogalAfetler.LOGGER.error("[spike-siren] Sirenler kaydırıcı düğmesi bulunamadı");
            return;
        }
        int once = IstemciAyarlari.SIREN_SES.get();
        double x = dugme.getX() + 4 + (yuzde + 0.5) / 100.0 * (dugme.getWidth() - 8);
        dugme.onClick(x, dugme.getY() + dugme.getHeight() / 2.0);
        DogalAfetler.LOGGER.info("[spike-siren] kaydırıcıya tıklandı (x={}): sirenSes {} -> {}, düğme \"{}\"",
                String.format(Locale.ROOT, "%.1f", x), once, IstemciAyarlari.SIREN_SES.get(),
                dugme.getMessage().getString());
    }

    private static void configSatiri() {
        Minecraft mc = Minecraft.getInstance();
        try {
            Files.readAllLines(mc.gameDirectory.toPath().resolve("config/dogalafetler-client.toml"), StandardCharsets.UTF_8)
                    .stream()
                    .filter(satir -> satir.trim().startsWith("sirenSes"))
                    .forEach(satir -> DogalAfetler.LOGGER.info("[spike-siren] dogalafetler-client.toml: {}", satir.trim()));
        } catch (IOException hata) {
            DogalAfetler.LOGGER.error("[spike-siren] dogalafetler-client.toml okunamadı", hata);
        }
    }

    private static void secenekleriKaydet() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.save();
        try {
            Files.readAllLines(mc.gameDirectory.toPath().resolve("options.txt"), StandardCharsets.UTF_8).stream()
                    .filter(satir -> satir.startsWith("soundCategory_"))
                    .forEach(satir -> DogalAfetler.LOGGER.info("[spike-siren] options.txt: {}", satir));
        } catch (IOException hata) {
            DogalAfetler.LOGGER.error("[spike-siren] options.txt okunamadı", hata);
        }
    }

    private static void kapat() {
        SirenSesi etkin = SirenCalar.ORNEK.etkin();
        DogalAfetler.LOGGER.info("[spike-siren] kapanış; son siren SoundManager.isActive={}",
                etkin != null && Minecraft.getInstance().getSoundManager().isActive(etkin));
        Minecraft.getInstance().stop();
    }
}
