package io.github.kadirberkpolat1.dogalafetler.client.esya.radar;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.esya.sismograf.IstemciSismograf;
import io.github.kadirberkpolat1.dogalafetler.client.esya.sismograf.SismografEkrani;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.GostergeYerlesimi;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.RadarGorunumu;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarPaketi;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografPaketi;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Radar ve sismografın görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=radar} ile
 * (Gradle {@code -Potomasyon=radar}) ve tek oyunculu Quick Play dünyasında çalışır; üretimde etkisizdir. Komutları gömülü
 * sunucunun kendi komut kaynağıyla verir (oyuncu tuşu yok), sahneleri durum koşuluyla ilerletir, her karede radar
 * panelinin dikdörtgenini ve sıcaklık barı/action bar ile çakışma denetimini log'a yazar. Klavye, fare ve tekerlek girdileri
 * kurulumdan kapanışa kadar sayılır (sıfırdan farklıysa koşu geçersizdir). Afet türleri {@code -PoyunTestiAfeti} ile
 * kaydedilmiş olmalıdır ({@code deprem,sinkhole,kasirga,gunes_patlamasi}).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class RadarOtomasyonu {
    private static final boolean AKTIF = "radar".equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "[radar-otomasyon] ";

    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim;
    private static int sayac;
    private static int toplam;
    private static boolean bitti;

    private RadarOtomasyonu() {
    }

    @SubscribeEvent
    public static void tusa(InputEvent.Key olay) {
        if (AKTIF) {
            tus++;
            DogalAfetler.LOGGER.warn(ON + "GİRDİ tuş {} eylem {}", olay.getKey(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void fareye(InputEvent.MouseButton.Pre olay) {
        if (AKTIF) {
            fare++;
            DogalAfetler.LOGGER.warn(ON + "GİRDİ fare {} eylem {}", olay.getButton(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void tekerlege(InputEvent.MouseScrollingEvent olay) {
        if (AKTIF) {
            teker++;
            DogalAfetler.LOGGER.warn(ON + "GİRDİ tekerlek {}", olay.getScrollDelta());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (!AKTIF || bitti || olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PauseScreen) {
            DogalAfetler.LOGGER.info(ON + "kendi açmadığım duraklatma ekranı kapatıldı");
            mc.setScreen(null);
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null) {
            return;
        }
        if (mc.screen instanceof DeathScreen) {
            mc.player.respawn();
            mc.setScreen(null);
        }
        sayac++;
        toplam++;
        if (toplam > 20 * 600) {
            DogalAfetler.LOGGER.error(ON + "zaman aşımı (adım {}); kapanıyor", adim);
            kapat(mc);
            return;
        }
        String ad = mc.player.getGameProfile().getName();
        switch (adim) {
            case 0 -> {
                if (sayac < 100) {
                    return;
                }
                komut(server, "gamerule sendCommandFeedback false", "gamemode survival " + ad, "time set 6000",
                        "weather clear", "gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
                        "dogalafet stop", "dogalafet hiz 1", "clear " + ad, "recipe give " + ad + " *",
                        "effect give " + ad + " minecraft:resistance infinite 255 true",
                        "effect give " + ad + " minecraft:saturation infinite 255 true",
                        "dogalafet start kasirga 2 " + ad, "dogalafet stop", "dogalafet start sinkhole 4 " + ad,
                        "dogalafet stop", "item replace entity " + ad + " weapon.mainhand with dogalafetler:hava_radari",
                        "item replace entity " + ad + " hotbar.8 with dogalafetler:hassas_sismograf");
                mc.player.setXRot(12f);
                mc.player.setYRot(30f);
                sonraki("kurulum; oyuncu " + ad);
            }
            case 1 -> bekle(200, () -> IstemciRadar.durum().elde(), "radar_sakin");
            case 2 -> {
                komut(server, "dogalafet start deprem 3 " + ad, "execute as " + ad + " at @s run tp @s ~90 ~30 ~70");
                sonraki("deprem S3 başlatıldı, oyuncu ~114 blok öteye");
            }
            case 3 -> bekle(100, () -> IstemciSismograf.durum().algilandi()
                    && IstemciRadar.durum().gorunum() == RadarGorunumu.GIZLI, "hassas_erken");
            case 4 -> {
                komut(server, "dogalafet hiz 20");
                sonraki("erken bölüm hızlandırıldı (hiz 20, 30 tick)");
            }
            case 5 -> {
                if (sayac >= 30) {
                    komut(server, "dogalafet hiz 1");
                    sonraki("hiz 1");
                }
            }
            case 6 -> bekle(60, () -> IstemciRadar.durum().gorunum() == RadarGorunumu.AFET, "radar_normal");
            case 7 -> {
                komut(server, "item replace entity " + ad + " weapon.mainhand with dogalafetler:gelismis_hava_radari");
                sonraki("gelişmiş radar elde");
            }
            case 8 -> bekle(60, () -> IstemciRadar.durum().gelismis()
                    && IstemciRadar.durum().gorunum() == RadarGorunumu.AFET, "radar_gelismis");
            case 9 -> {
                komut(server, "item replace entity " + ad + " hotbar.8 with dogalafetler:sismograf");
                sonraki("normal sismograf hotbar'da");
            }
            case 10 -> bekle(80, () -> IstemciSismograf.durum().algilandi() && !IstemciSismograf.durum().hassas(),
                    "sismograf_actionbar");
            case 11 -> {
                IstemciSismograf.ekraniAc();
                sonraki("grafik ekranı açıldı: " + (mc.screen instanceof SismografEkrani));
            }
            case 12 -> bekle(120, () -> mc.screen instanceof SismografEkrani, "sismograf_ekran");
            case 13 -> {
                mc.setScreen(null);
                komut(server, "dogalafet stop", "dogalafet start gunes_patlamasi", "dogalafet asama ileri");
                sonraki("güneş patlaması SIREN'e geçirildi");
            }
            case 14 -> bekle(60, () -> IstemciRadar.durum().gorunum() == RadarGorunumu.ANOMALI, "radar_anomali");
            case 15 -> {
                komut(server, "dogalafet stop", "clear " + ad);
                sonraki("temizlendi");
            }
            case 16 -> {
                if (sayac >= 60) {
                    DogalAfetler.LOGGER.info(ON + "bitti; radar paketi {}, öncü darbe {}, action bar {}, FPS {}",
                            IstemciRadar.alinanPaket(), IstemciSismograf.darbeSayisi(), IstemciSismograf.actionBarSayisi(),
                            mc.getFps());
                    kapat(mc);
                }
            }
            default -> {
            }
        }
    }

    private static void kapat(Minecraft mc) {
        DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
        bitti = true;
        mc.stop();
    }

    private static void sonraki(String not) {
        DogalAfetler.LOGGER.info(ON + "adım {} (t={}): {}", adim, toplam, not);
        adim++;
        sayac = 0;
    }

    /** Koşul en az {@code enAz} tick sonra sağlanınca kare alır; 600 tick'te sağlanmazsa yine alır ve uyarır. */
    private static void bekle(int enAz, BooleanSupplier kosul, String kareAdi) {
        boolean tamam = kosul.getAsBoolean();
        if (sayac >= enAz && tamam || sayac >= 600) {
            if (!tamam) {
                DogalAfetler.LOGGER.warn(ON + "{}: koşul 600 tick'te sağlanmadı", kareAdi);
            }
            kare(Minecraft.getInstance(), kareAdi);
            sonraki("kare " + kareAdi);
        }
    }

    private static void komut(MinecraftServer server, String... komutlar) {
        server.execute(() -> {
            for (String k : komutlar) {
                int sonuc = server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withSuppressedOutput(), k);
                DogalAfetler.LOGGER.info(ON + "komut /{} -> {}", k, sonuc);
            }
        });
    }

    private static void kare(Minecraft mc, String ad) {
        String dosya = "radar-" + ad + ".png";
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> { });
        RadarPaketi r = IstemciRadar.durum();
        SismografPaketi s = IstemciSismograf.durum();
        int w = RadarGostergesi.sonEkranG;
        int h = RadarGostergesi.sonEkranY;
        String cakisma = "panel yok";
        if (RadarGostergesi.sonX >= 0) {
            int x1 = RadarGostergesi.sonX;
            int y1 = RadarGostergesi.sonY;
            int x2 = x1 + RadarGostergesi.sonG;
            int y2 = y1 + RadarGostergesi.sonY2;
            boolean orta = x1 < w / 2 + GostergeYerlesimi.HOTBAR_YARI && x2 > w / 2 - GostergeYerlesimi.HOTBAR_YARI
                    && y2 > h - GostergeYerlesimi.DURUM_UST;
            int yan = GostergeYerlesimi.HOTBAR_YARI + GostergeYerlesimi.HOTBAR_YAN;
            boolean altYazi = x1 < w / 2 + yan && x2 > w / 2 - yan && y2 > h - GostergeYerlesimi.ALT_YAZI_UST;
            cakisma = String.format(Locale.ROOT, "panel x=%d..%d y=%d..%d ekran %dx%d; sıcaklık barı/durum çakışması=%s,"
                    + " hotbar yanı/action bar çakışması=%s", x1, x2, y1, y2, w, h, orta ? "VAR" : "yok",
                    altYazi ? "VAR" : "yok");
        }
        DogalAfetler.LOGGER.info(ON + "kare {}: radar[görünüm={} tür={} aşama={} seviye={} kalan={} sn geçmiş={}]"
                + " sismograf[algılandı={} hassas={} yüzde={} seviye={} öncü={}] {}; FPS {}", dosya, r.gorunum(),
                r.tur().kimlik(), r.asama(), r.seviye(), r.kalanSn(), r.gecmis().size(), s.algilandi(), s.hassas(),
                s.yuzde(), s.seviye(), s.oncu(), cakisma, mc.getFps());
    }
}
