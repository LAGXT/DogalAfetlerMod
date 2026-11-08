package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.VucutOlcegi;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2, S3-07, S3-10). Yalnız {@code -Ddogalafetler.otomasyon=sicaklik|sicaklik_coldsweat}
 * ile çalışır; üretimde etkisizdir. {@code sicaklik}: oyuncu OP olunca vücut sıcaklığını (°C) ve ıslaklığı komutla ayarlayıp
 * sahneleri sırayla çeker: birinci şahısta HUD normal, ıslak, donma, bunalma, yanma (iki nabız fazı); üçüncü şahısta
 * aynı kameradan normal karşılaştırma karesi, üşümede ağız buharı, bunalmada ter ve kırmızı kenar; termometre
 * menüsü; terin gök, çimen ve kum önünde üç kare (zemin oyuncunun altına {@code fill} ile serilir, kamera açısı arka
 * planı seçer). Birinci şahıs sahnelerinde oyuncunun kendi parçacık sayısı log'a yazılır (0 olmalı); her kare satırı
 * hapın ekran konumunu ve sayı rengini de yazar (piksel ölçümü {@code tools/senaryo/sicaklik_olcum.py}). Sonra sunucu
 * konsolunun komutları için bekler ve kapanır.
 * {@code sicaklik_coldsweat}: Cold Sweat yüklü tek oyunculu dünyada kapı durumunu loglar ve bir görüntü alır. Klavye, fare
 * ve tekerlek girdileri kapanışa kadar sayılır (sıfırdan farklıysa koşu geçersizdir). Kendi açmadığı duraklatma ekranını
 * kapatır; adımlar durum koşuluyla ilerler.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SicaklikOtomasyonu {
    private static final String MOD = System.getProperty("dogalafetler.otomasyon", "");
    private static final String ON = "SICAKLIK_OTOMASYON ";
    /** Sahne başında komutların etkisi ve HUD yumuşatması otursun diye beklenen tick. */
    private static final int OTURMA = 40;

    private enum Kip {
        HUD,
        /** Üçüncü şahıs önden HUD karesi: aynı kameradaki buhar, ter ve kenar karelerinin karşılaştırma zemini. */
        HUD_3P,
        /** İkinci kare farklı bir titreşim ya da nabız fazından alınır. */
        HUD_IKI_KARE,
        MENU,
        BUHAR,
        TER
    }

    /**
     * @param bakis  oyuncunun baş eğimi; üçüncü şahıs önden kamerada artı değer kamerayı göğe, eksi zemine çevirir
     * @param zemin  sahne başında oyuncunun altına serilen blok; {@code null} dokunulmaz
     */
    private record Sahne(String ad, double santigrat, int islaklik, Kip kip, float bakis, String zemin) {
    }

    private static final List<Sahne> SAHNELER = List.of(new Sahne("normal", 37.0, 0, Kip.HUD, 8f, null),
            new Sahne("islak", 36.8, 80, Kip.HUD, 8f, null),
            new Sahne("donma", 34.4, 0, Kip.HUD, 8f, null),
            new Sahne("bunalma_1p", 38.6, 0, Kip.HUD, 8f, null),
            new Sahne("normal_3p", 37.0, 0, Kip.HUD_3P, 0f, null),
            new Sahne("usume", 35.6, 0, Kip.BUHAR, 0f, null),
            new Sahne("bunalma", 38.6, 0, Kip.TER, 0f, null),
            new Sahne("yanma", 40.0, 0, Kip.HUD_IKI_KARE, 8f, null),
            new Sahne("menu", 35.6, 60, Kip.MENU, 8f, null),
            new Sahne("ter_gok", 38.8, 0, Kip.TER, 35f, null),
            new Sahne("ter_cimen", 38.8, 0, Kip.TER, -35f, "minecraft:grass_block"),
            new Sahne("ter_kum", 38.8, 0, Kip.TER, -35f, "minecraft:sand"));

    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim = -1;
    private static int sayac;
    private static int sahne;
    /** İlk karede çizilmiş bantın tick'i, ofseti ve nabzı (ikinci kare farklı bir fazdan alınır). */
    private static long ilkTick;
    private static int ilkDx;
    private static int ilkDy;
    private static float ilkNabiz;
    /** Sahne başındaki kendi parçacık sayacı ve istemci tick'i. */
    private static int sahneParcacik;
    private static long sahneTick;

    private SicaklikOtomasyonu() {
    }

    private static boolean aktif() {
        return MOD.startsWith("sicaklik");
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
        if (!aktif() || olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
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
        if (MOD.equals("sicaklik_coldsweat")) {
            coldSweat(mc);
        } else {
            hud(mc);
        }
    }

    private static void komut(Minecraft mc, String format, Object... argumanlar) {
        mc.player.connection.sendCommand(String.format(Locale.ROOT, format, argumanlar));
    }

    private static void hud(Minecraft mc) {
        String ad = mc.player.getGameProfile().getName();
        if (adim < 0) {
            if (!mc.player.hasPermissions(2) || !IstemciSicaklik.acik() || sayac < 100) {
                return;
            }
            DogalAfetler.LOGGER.info(ON + "başlıyor: oyuncu {}, OP, HUD paketi geldi", ad);
            adim = 0;
            sayac = 0;
        }
        if (adim == 0) {
            if (sahne >= SAHNELER.size()) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                komut(mc, "dogalafet sicaklik set %s %.1f", ad, VucutOlcegi.NORMAL_C);
                komut(mc, "dogalafet sicaklik islaklik %s 0", ad);
                DogalAfetler.LOGGER.info(ON + "bitti; FPS {}", mc.getFps());
                adim = 2;
                sayac = 0;
                return;
            }
            Sahne s = SAHNELER.get(sahne);
            boolean ucuncu = s.kip() == Kip.BUHAR || s.kip() == Kip.TER || s.kip() == Kip.HUD_3P;
            mc.options.setCameraType(ucuncu ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON);
            mc.player.setXRot(s.bakis());
            if (s.zemin() != null) {
                komut(mc, "fill ~-8 ~-1 ~-8 ~8 ~-1 ~8 %s", s.zemin());
            }
            komut(mc, "dogalafet sicaklik set %s %.1f", ad, s.santigrat());
            komut(mc, "dogalafet sicaklik islaklik %s %d", ad, s.islaklik());
            sahneParcacik = IstemciSicaklik.kendiParcacikSayisi();
            sahneTick = IstemciSicaklik.tick();
            adim = 1;
            sayac = 0;
        } else if (adim == 1) {
            sahneAdimi(mc, SAHNELER.get(sahne));
        } else if (adim == 2 && sayac >= 400) {
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            adim = 3;
            mc.stop();
        }
    }

    private static void sonraki() {
        sahne++;
        adim = 0;
    }

    private static void sahneAdimi(Minecraft mc, Sahne s) {
        switch (s.kip()) {
            case HUD, HUD_3P -> {
                if (sayac == OTURMA) {
                    kare(mc, s.ad());
                    sonraki();
                }
            }
            case HUD_IKI_KARE -> {
                if (sayac == OTURMA) {
                    kare(mc, s.ad());
                    ilkTick = SicaklikHud.sonTick;
                    ilkDx = SicaklikHud.sonDx;
                    ilkDy = SicaklikHud.sonDy;
                    ilkNabiz = SicaklikHud.sonNabiz;
                } else if (sayac > OTURMA) {
                    // Gizli workspace'te istemci ~1 FPS çizer: sabit tick farkı aynı çizimi ya da aynı fazı yakalayabilir.
                    boolean yeniFaz = SicaklikHud.sonTick != ilkTick && (SicaklikHud.sonDx != ilkDx
                            || SicaklikHud.sonDy != ilkDy || Math.abs(SicaklikHud.sonNabiz - ilkNabiz) > 0.15f);
                    if (yeniFaz || sayac >= OTURMA + 160) {
                        if (!yeniFaz) {
                            DogalAfetler.LOGGER.warn(ON + "{}: 160 tick'te farklı titreşim/nabız fazı çizilmedi", s.ad());
                        }
                        kare(mc, s.ad() + "_b");
                        sonraki();
                    }
                }
            }
            case MENU -> {
                if (sayac == OTURMA) {
                    TermometreEkrani.ac();
                    DogalAfetler.LOGGER.info(ON + "termometre menüsü açıldı: {}", mc.screen);
                } else if (sayac == OTURMA + 60) {
                    kare(mc, s.ad());
                    mc.setScreen(null);
                    sonraki();
                }
            }
            case BUHAR -> ucuncuSahis(mc, s, IstemciSicaklik.sonNefesTick(), 3);
            case TER -> ucuncuSahis(mc, s, IstemciSicaklik.sonTerTick(), 2);
            default -> throw new IllegalStateException(s.kip().name());
        }
    }

    /** Parçacık yeni üretilmişken (ekranda görünürken) kare; 300 tick'te üretilmezse uyarıp yine de çeker. */
    private static void ucuncuSahis(Minecraft mc, Sahne s, long sonParcacik, int gecikme) {
        long simdi = IstemciSicaklik.tick();
        boolean taze = sonParcacik > sahneTick && simdi - sonParcacik >= gecikme && simdi - sonParcacik <= gecikme + 3;
        if (sayac >= OTURMA && taze || sayac >= OTURMA + 300) {
            if (!taze) {
                DogalAfetler.LOGGER.warn(ON + "{}: 300 tick'te kendi parçacığı üretilmedi", s.ad());
            }
            kare(mc, s.ad());
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            sonraki();
        }
    }

    private static void coldSweat(Minecraft mc) {
        if (adim < 0 && sayac >= 200) {
            DogalAfetler.LOGGER.info(ON + "coldsweat: cold_sweat yüklü={}, bizim HUD paketi açık={}",
                    ModList.get().isLoaded("cold_sweat"), IstemciSicaklik.acik());
            mc.player.connection.sendCommand("dogalafet sicaklik bilgi " + mc.player.getGameProfile().getName());
            adim = 0;
            sayac = 0;
        } else if (adim == 0 && sayac == 60) {
            kare(mc, "coldsweat");
            DogalAfetler.LOGGER.info(ON + "coldsweat: 60 tick sonra bizim HUD paketi açık={}", IstemciSicaklik.acik());
        } else if (adim == 0 && sayac >= 100) {
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            adim = 3;
            mc.stop();
        }
    }

    private static void kare(Minecraft mc, String ad) {
        String dosya = "sicaklik_" + ad + ".png";
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> { });
        var d = IstemciSicaklik.durum();
        DogalAfetler.LOGGER.info(ON + "kare {} (vücut {} °C kademe {} durum {}, ıslaklık {}, hissedilen {} °C, mekan {} °C, "
                + "eğilim {}; FPS {}; bant dx={} dy={} parlama={} nabız={} şevron={} metin='{}' ikonlar=[{}] üçgen x={} "
                + "çizim tick'i {}; kamera {}, sahnede kendi parçacığı {}; hap x={} y={} ölçek={} sayı rengi={})", dosya,
                sayi(VucutOlcegi.santigrat(d.vucut())), d.kademe(), d.kademe().durum(), sayi(d.islaklik()),
                sayi(d.hissedilen()), sayi(d.mekan()), sayi(d.egilim()), mc.getFps(), SicaklikHud.sonDx, SicaklikHud.sonDy,
                sayi(SicaklikHud.sonParlama), sayi(SicaklikHud.sonNabiz), SicaklikHud.sonSevron, SicaklikHud.sonMetin,
                SicaklikHud.sonIkonlar, SicaklikHud.sonUcgen, SicaklikHud.sonTick, mc.options.getCameraType(),
                IstemciSicaklik.kendiParcacikSayisi() - sahneParcacik, SicaklikHud.sonHapX, SicaklikHud.sonHapY,
                mc.getWindow().getGuiScale(),
                String.format(Locale.ROOT, "%06x", HudCizimi.sayiRengi(d.kademe().durum()) & 0xFFFFFF));
    }

    private static String sayi(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }
}
