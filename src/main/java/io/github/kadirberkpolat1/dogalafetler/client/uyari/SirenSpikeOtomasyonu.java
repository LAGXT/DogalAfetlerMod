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
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Spike otomasyonu (K0-21): klavye/fare olmadan kanıt toplar. Yalnız {@code -Pspike=siren} ya da
 * {@code -Pspike=siren-kategori} ({@code -Ddogalafetler.spike}) ile çalışır; aksi halde hiçbir şey yapmaz. Her adım bir
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
    private static Screen bizimEkran;
    private static Boolean sonPencereEtkin;

    private record Adim(long ms, String ad, BooleanSupplier kosul, Runnable is) {
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
        // Pencereye ulaşan bir Escape duraklatma menüsü açarsa tek oyunculu oyun durur ve zaman çizelgesi bozulur.
        // Otomasyon klavye beklemediği için kendi açmadığı ekranı kapatır; tuşun kaynağı {@link #tus} log'unda.
        if (mc.screen != null && mc.screen != bizimEkran) {
            DogalAfetler.LOGGER.warn("[spike-siren] otomasyonun açmadığı ekran kapatıldı: {}", mc.screen.getClass().getName());
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
