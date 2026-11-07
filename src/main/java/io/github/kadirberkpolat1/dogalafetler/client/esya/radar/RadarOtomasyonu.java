package io.github.kadirberkpolat1.dogalafetler.client.esya.radar;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.client.esya.sismograf.IstemciSismograf;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.client.esya.sismograf.SismografEkrani;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.GostergeYerlesimi;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.RadarGorunumu;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarPaketi;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografPaketi;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
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
 * panelinin dikdörtgenini ve sıcaklık barı/action bar ile çakışma denetimini log'a yazar. Kareler A-34 kapısından alınır
 * ({@link GorselOtomasyon#kareIste}); bekleyen kare varken adım ilerlemez. Kurulum beş kısa afeti başlatıp durdurur:
 * Gelişmiş panel karesinde ({@code radar_gelismis_panel}, R-18) şiddet + hareket satırı ve 5 geçmiş dolu, panel en yüksek
 * halindedir. Sonda aynı kamerada afetsiz {@code OLCUM} pencereleri (R-19, K-E29 biçimi; her biri 100 tick, öncesinde FPS
 * kapısı): eşyasız taban {@code radar_yok_taban}, normal radar elde {@code radar_elde}, Gelişmiş radar elde
 * {@code radar_gelismis}, sismograf grafik ekranı {@code sismograf_ekran}. Klavye, fare ve tekerlek girdileri kurulumdan
 * kapanışa kadar sayılır (sıfırdan farklıysa koşu geçersizdir). Afet türleri {@code -PoyunTestiAfeti} ile kaydedilmiş
 * olmalıdır ({@code deprem,sinkhole,kasirga,gunes_patlamasi}).
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
    /** Ölçüm adımının evresi (0 koşul, 1 FPS kapısı, 2 pencere) ve evrenin başladığı {@code sayac}. */
    private static int olcumEvresi;
    private static int olcumTick;
    private static final AtomicBoolean NORMAL_PENCERE = new AtomicBoolean();

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
        if (GorselOtomasyon.kareBekliyor()) {
            return;
        }
        sayac++;
        toplam++;
        if (toplam > 20 * 900) {
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
                GorselOtomasyon.cizimAyari();
                komut(server, "gamerule sendCommandFeedback false", "gamemode survival " + ad, "time set 6000",
                        "weather clear", "gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
                        "dogalafet stop", "dogalafet hiz 1", "clear " + ad, "recipe give " + ad + " *",
                        "effect give " + ad + " minecraft:resistance infinite 255 true",
                        "effect give " + ad + " minecraft:saturation infinite 255 true",
                        "dogalafet start kasirga 2 " + ad, "dogalafet stop", "dogalafet start sinkhole 4 " + ad,
                        "dogalafet stop", "dogalafet start kasirga 4 " + ad, "dogalafet stop",
                        "dogalafet start sinkhole 2 " + ad, "dogalafet stop", "dogalafet start kasirga 1 " + ad,
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
                NORMAL_PENCERE.set(false);
                komut(server, "dogalafet hiz 20");
                sonraki("erken bölüm hızlandırıldı (hiz 20)");
            }
            case 5 -> {
                // İstemci tick'i değil sunucunun kendi durumu: yüklü makinede gömülü sunucu saniyelerce geride kalabilir.
                if (sayac % 10 == 0) {
                    server.execute(() -> NORMAL_PENCERE.set(AfetYoneticisi.al(server).aktifAfet()
                            .map(Afet::normalOnUyariBasladi).orElse(false)));
                }
                if (NORMAL_PENCERE.get() || sayac >= 2400) {
                    komut(server, "dogalafet hiz 1");
                    sonraki("hiz 1; sunucuda normal ön-uyarı penceresi başladı=" + NORMAL_PENCERE.get());
                }
            }
            case 6 -> bekle(60, () -> IstemciRadar.durum().gorunum() == RadarGorunumu.AFET, "radar_normal");
            case 7 -> {
                komut(server, "item replace entity " + ad + " weapon.mainhand with dogalafetler:gelismis_hava_radari");
                sonraki("gelişmiş radar elde");
            }
            case 8 -> bekle(60, () -> IstemciRadar.durum().gelismis()
                    && IstemciRadar.durum().gorunum() == RadarGorunumu.AFET, "radar_gelismis");
            case 9 -> bekle(20, () -> IstemciRadar.durum().gelismis() && IstemciRadar.durum().gorunum() == RadarGorunumu.AFET
                    && IstemciRadar.durum().gecmis().size() >= RadarPaketi.GECMIS_SINIRI, "radar_gelismis_panel");
            case 10 -> {
                komut(server, "item replace entity " + ad + " hotbar.8 with dogalafetler:sismograf");
                sonraki("normal sismograf hotbar'da");
            }
            case 11 -> bekle(80, () -> IstemciSismograf.durum().algilandi() && !IstemciSismograf.durum().hassas(),
                    "sismograf_actionbar");
            case 12 -> {
                IstemciSismograf.ekraniAc();
                sonraki("grafik ekranı açıldı: " + (mc.screen instanceof SismografEkrani));
            }
            case 13 -> bekle(120, () -> mc.screen instanceof SismografEkrani, "sismograf_ekran");
            case 14 -> {
                mc.setScreen(null);
                komut(server, "dogalafet stop", "dogalafet start gunes_patlamasi", "dogalafet asama ileri");
                sonraki("güneş patlaması SIREN'e geçirildi");
            }
            case 15 -> bekle(60, () -> IstemciRadar.durum().gorunum() == RadarGorunumu.ANOMALI, "radar_anomali");
            case 16 -> {
                komut(server, "dogalafet stop", "clear " + ad);
                mc.player.setXRot(12f);
                mc.player.setYRot(30f);
                sonraki("temizlendi; ölçüm kamerası");
            }
            case 17 -> olc("radar_yok_taban", null, () -> !IstemciRadar.durum().elde());
            case 18 -> {
                komut(server, "item replace entity " + ad + " weapon.mainhand with dogalafetler:hava_radari");
                sonraki("ölçüm: normal radar elde");
            }
            case 19 -> olc("radar_elde", "radar_yok_taban",
                    () -> IstemciRadar.durum().elde() && !IstemciRadar.durum().gelismis());
            case 20 -> {
                komut(server, "item replace entity " + ad + " weapon.mainhand with dogalafetler:gelismis_hava_radari");
                sonraki("ölçüm: Gelişmiş radar elde");
            }
            case 21 -> olc("radar_gelismis", "radar_yok_taban", () -> IstemciRadar.durum().gelismis());
            case 22 -> {
                komut(server, "clear " + ad, "item replace entity " + ad + " hotbar.8 with dogalafetler:sismograf");
                sonraki("ölçüm: radar yok, sismograf hotbar'da");
            }
            case 23 -> {
                if (sayac >= 20) {
                    IstemciSismograf.ekraniAc();
                    sonraki("ölçüm: grafik ekranı açıldı: " + (mc.screen instanceof SismografEkrani));
                }
            }
            case 24 -> olc("sismograf_ekran", "radar_yok_taban", () -> mc.screen instanceof SismografEkrani);
            case 25 -> {
                mc.setScreen(null);
                komut(server, "clear " + ad);
                sonraki("ölçümler bitti");
            }
            case 26 -> {
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
        GorselOtomasyon.kareOzetiYaz("radar");
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
            kare(kareAdi);
            sonraki("kare " + kareAdi);
        }
    }

    /**
     * Ölçüm adımı: koşul en az 40 tick sonra sağlanınca (600 tick'te sağlanmazsa uyarıyla) A-34 FPS kapısı
     * ({@link GorselOtomasyon#akici}; {@value GorselOtomasyon#KAPI_SN} sn'de açılmazsa uyarıyla), ardından 100 tick'lik
     * {@code OLCUM} penceresi.
     */
    private static void olc(String sahne, String taban, BooleanSupplier kosul) {
        if (olcumEvresi == 0) {
            boolean tamam = kosul.getAsBoolean();
            if (sayac >= 40 && tamam || sayac >= 600) {
                if (!tamam) {
                    DogalAfetler.LOGGER.warn(ON + "{}: koşul 600 tick'te sağlanmadı", sahne);
                }
                olcumEvresi = 1;
                olcumTick = sayac;
            }
        } else if (olcumEvresi == 1) {
            boolean akici = GorselOtomasyon.akici();
            if (akici || sayac - olcumTick >= GorselOtomasyon.KAPI_SN * 20) {
                if (!akici) {
                    DogalAfetler.LOGGER.warn(ON + "{}: FPS {} < {} ({} sn); ölçüm yine de alınıyor", sahne,
                            Minecraft.getInstance().getFps(), GorselOtomasyon.AKICI_FPS, GorselOtomasyon.KAPI_SN);
                }
                GorselOtomasyon.olcumBasla("radar", sahne, taban);
                olcumEvresi = 2;
                olcumTick = sayac;
            }
        } else if (sayac - olcumTick >= 100) {
            GorselOtomasyon.olcumBitir();
            olcumEvresi = 0;
            sonraki("ölçüm " + sahne);
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

    private static void kare(String ad) {
        GorselOtomasyon.kareIste("radar", "radar-" + ad + ".png", k -> kareSatiri(k.dosya(), k.fps()));
    }

    private static void kareSatiri(String dosya, int fps) {
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
            GostergeYerlesimi.Alt alt = RadarGostergesi.sonAlt;
            boolean orta = x1 < w / 2 + GostergeYerlesimi.HOTBAR_YARI && x2 > w / 2 - GostergeYerlesimi.HOTBAR_YARI
                    && y2 > h - alt.durumUst();
            boolean altYazi = x1 < w / 2 + alt.altYaziYari() && x2 > w / 2 - alt.altYaziYari() && y2 > h - alt.altYaziUst();
            cakisma = String.format(Locale.ROOT, "panel x=%d..%d y=%d..%d ekran %dx%d; durum sütunu üstü alttan %d, action bar"
                    + " üstü alttan %d yarı genişlik %d; sıcaklık barı/durum çakışması=%s, action bar çakışması=%s", x1, x2, y1,
                    y2, w, h, alt.durumUst(), alt.altYaziUst(), alt.altYaziYari(), orta ? "VAR" : "yok",
                    altYazi ? "VAR" : "yok");
        }
        DogalAfetler.LOGGER.info(ON + "kare {}: radar[görünüm={} tür={} aşama={} seviye={} kalan={} sn geçmiş={}]"
                + " sismograf[algılandı={} hassas={} yüzde={} seviye={} öncü={}] {}; FPS {}", dosya, r.gorunum(),
                r.tur().kimlik(), r.asama(), r.seviye(), r.kalanSn(), r.gecmis().size(), s.algilandi(), s.hassas(),
                s.yuzde(), s.seviye(), s.oncu(), cakisma, fps);
    }
}
