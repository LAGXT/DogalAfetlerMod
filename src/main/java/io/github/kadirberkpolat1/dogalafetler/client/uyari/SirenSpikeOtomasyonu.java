package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenKategorisi;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Spike otomasyonu (K0-21): klavye/fare olmadan kanıt toplar. Yalnız {@code -Pspike=siren} ya da
 * {@code -Pspike=siren-kategori} ({@code -Ddogalafetler.spike}) ile çalışır; aksi halde hiçbir şey yapmaz. Zaman çizelgesi
 * dünyaya girişten (oyuncu var, ekran kapalı) itibaren duvar saatiyle yürür.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SirenSpikeOtomasyonu {
    private static final String SENARYO = System.getProperty("dogalafetler.spike", "");
    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static long baslangic = -1;
    private static int sonraki;
    private static Screen bizimEkran;

    private record Adim(long ms, String ad, Runnable is) {
    }

    static {
        switch (SENARYO) {
            case "siren" -> secilenYolPlani();
            case "siren-kategori" -> gercekKategoriPlani();
            default -> {
            }
        }
    }

    private SirenSpikeOtomasyonu() {
    }

    /** N5 yolu: siren MASTER + sirenSes; çalarken yazı yok, altyazı yalnız açıkken; fade-out; ses ayarları ekranı. */
    private static void secilenYolPlani() {
        adim(3000, "standart siren 10 sn", () -> komut("dogalafet spike siren standart 10"));
        adim(5000, "çekim: siren çalarken (altyazı kapalı)", () -> cek("siren-calarken-yazi-yok"));
        // Standart siren 240 tick (10 sn + 2 sn fade-out) sürer; yüklü makinede istemci ~16 tick/sn döndüğü için
        // duvar saatinde 15 sn'yi bulur. Güneş sireni onu kesmesin diye 19. saniyede gelir.
        adim(18500, "altyazı aç", () -> altyazi(true));
        adim(19000, "güneş sireni 20 sn", () -> komut("dogalafet spike siren gunes 20"));
        adim(20500, "çekim: altyazı açıkken", () -> cek("siren-gunes-yalniz-altyazi"));
        adim(21000, "altyazı kapat", () -> altyazi(false));
        adim(24000, "siren dur (fade-out)", () -> komut("dogalafet spike siren dur"));
        adim(30000, "ses ayarları ekranı", SirenSpikeOtomasyonu::sesEkrani);
        adim(31500, "çekim: ses ayarları", () -> cek("ses-ayarlari-sirenler"));
        adim(32000, "ekranı kapat", () -> Minecraft.getInstance().setScreen(null));
        adim(32500, "kapanış", SirenSpikeOtomasyonu::kapat);
    }

    /**
     * Reddedilen yol: mixin ile eklenen gerçek kategori; vanilla kaydırıcı, options.txt, yeni sirenin çalan sireni
     * tam düzeydeyken kesmesi ve standart sirenin kendiliğinden bitmesi.
     */
    private static void gercekKategoriPlani() {
        adim(3000, "ses ayarları ekranı", SirenSpikeOtomasyonu::sesEkrani);
        adim(4500, "çekim: ses ayarları (gerçek kategori)", () -> cek("ses-ayarlari-gercek-kategori"));
        adim(5000, "ekranı kapat", () -> Minecraft.getInstance().setScreen(null));
        adim(5500, "güneş sireni 20 sn", () -> komut("dogalafet spike siren gunes 20"));
        adim(9500, "standart siren güneşi keser", () -> komut("dogalafet spike siren standart 10"));
        adim(26000, "options.txt kaydet", SirenSpikeOtomasyonu::secenekleriKaydet);
        adim(26500, "kapanış", SirenSpikeOtomasyonu::kapat);
    }

    @SubscribeEvent
    public static void tik(TickEvent.ClientTickEvent olay) {
        if (ADIMLAR.isEmpty() || olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (baslangic < 0) {
            if (mc.player == null || mc.level == null || mc.screen != null) {
                return;
            }
            baslangic = System.currentTimeMillis();
            DogalAfetler.LOGGER.info("[spike-siren] dünyaya girildi; senaryo={}, SoundSource.values()={}", SENARYO,
                    Arrays.toString(SoundSource.values()));
        }
        // Pencere ilk belirdiğinde sahibinin masaüstünde kısa süre odak alabilir; oraya kaçan tuşlar duraklatma menüsü açarsa
        // tek oyunculu oyun durur ve zaman çizelgesi bozulur. Otomasyon klavye beklemediği için kendi açmadığı ekranı kapatır.
        if (mc.screen != null && mc.screen != bizimEkran) {
            DogalAfetler.LOGGER.warn("[spike-siren] otomasyonun açmadığı ekran kapatıldı: {}", mc.screen.getClass().getName());
            mc.setScreen(null);
        }
        long gecen = System.currentTimeMillis() - baslangic;
        while (sonraki < ADIMLAR.size() && ADIMLAR.get(sonraki).ms() <= gecen) {
            Adim adim = ADIMLAR.get(sonraki++);
            DogalAfetler.LOGGER.info("[spike-siren] t={} ms: {}", gecen, adim.ad());
            adim.is().run();
        }
    }

    private static void adim(long ms, String ad, Runnable is) {
        ADIMLAR.add(new Adim(ms, ad, is));
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
        bizimEkran = new SoundOptionsScreen(null, mc.options);
        mc.setScreen(bizimEkran);
        DogalAfetler.LOGGER.info("[spike-siren] ses ayarları açıldı; sirenSes={}, gerçek kategori={}",
                IstemciAyarlari.SIREN_SES.get(),
                SirenKategorisi.gercek().map(k -> k.getName() + " = " + mc.options.getSoundSourceVolume(k)).orElse("yok"));
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
