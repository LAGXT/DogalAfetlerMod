package io.github.kadirberkpolat1.dogalafetler.client.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaEvresi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaVarligi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.UcanBlokVarligi;
import io.github.kadirberkpolat1.dogalafetler.client.afet.kasirga.KasirgaIstemciDurumu.HuniKipi;
import io.github.kadirberkpolat1.dogalafetler.client.afet.kasirga.KasirgaIstemciDurumu.Katman;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Spike otomasyonu (K0-21, {@code -Pspike=kasirga}): klavye/fare olmadan FPS/TPS ölçer, ekran görüntüsü alır ve oyunu
 * kapatır. Her adım bir öncekinden {@code ms} sonra ve koşulu (kasırga evresi, ışınlanma, ölçümün bitmesi, uçan blok
 * sayısı) sağlanınca çalışır. FPS kare süresinden hesaplanır (dikey eşitleme kapalı, kare sınırı yok).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class KasirgaSpikeOtomasyonu {
    private static final String SENARYO = System.getProperty("dogalafetler.spike", "");
    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static final long KOSUL_ZAMAN_ASIMI_MS = 40_000;
    private static final String ETIKET = "[spike-kasirga]";

    private static long baslangic = -1;
    private static long oncekiAdimZamani = -1;
    private static int sonraki;
    private static Boolean sonPencereEtkin;
    private static boolean sohbetiYakala;

    private static String olcumAdi;
    private static long olcumSureNs;
    private static long olcumBas;
    private static long sonKare;
    private static long[] kareler = new long[4096];
    private static int kareSayisi;
    private static boolean olcumBitti = true;

    private static double beklenenX = Double.NaN;
    private static double beklenenZ;

    private record Adim(long ms, String ad, BooleanSupplier kosul, Runnable is) {
    }

    static {
        if ("kasirga".equals(SENARYO)) {
            plan();
        }
    }

    private KasirgaSpikeOtomasyonu() {
    }

    private static void plan() {
        adim(1000, "hazırlık: seyirci, öğlen, yağmur, HUD gizli, vsync kapalı", KasirgaSpikeOtomasyonu::hazirlik);
        olcum(6000, "afetsiz-baslangic", 5000);
        tps();

        // S1: hareket ve enterpolasyon, yakın çekim.
        adim(500, "S1 başlat", () -> komut("dogalafet spike kasirga 1"));
        adim(8000, "S1 olgun evrede", () -> olgun(), () -> {
        });
        olcum(0, "S1-hareket-enterpolasyon-acik", 3000);
        adim(0, "enterpolasyon kapalı", () -> KasirgaVarligi.istemciEnterpolasyonu = false);
        olcum(300, "S1-hareket-enterpolasyon-kapali", 3000);
        adim(0, "enterpolasyon açık", () -> KasirgaVarligi.istemciEnterpolasyonu = true);
        kamera(28, -10);
        adim(3000, "çekim: S1 yakın", KasirgaSpikeOtomasyonu::isinlandi, () -> cek("s1-yakin"));
        olcum(300, "S1-yuksek", 5000);
        tps();
        dur();

        // S5 Yüksek: vitrin, kalkan bloklar, 100 blok.
        adim(1000, "S5 yüksek başlat", () -> komut("dogalafet spike kasirga 5 kalite yuksek"));
        adim(8000, "S5 olgun: hareketi durdur", () -> olgun(),
                () -> komut("dogalafet spike kasirga hareket false"));
        kamera(52, -12);
        adim(3000, "çekim: S5 vitrin yüksek", KasirgaSpikeOtomasyonu::isinlandi, () -> cek("s5-vitrin-yuksek"));
        olcum(300, "S5-yuksek", 5000);
        tps();
        kamera(30, -25);
        adim(2500, "çekim: kalkan bloklar", () -> isinlandi() && ucanBlokSayisi() >= 20, () -> cek("s5-kalkan-bloklar"));
        kamera(100, -14);
        adim(3000, "çekim: S5 100 blok yüksek", KasirgaSpikeOtomasyonu::isinlandi, () -> cek("s5-100blok-yuksek"));
        olcum(300, "S5-yuksek-100blok", 5000);

        // Soru 1: huni gövdesi teknikleri (ağ / sprite / karma), 52 ve 100 blok.
        for (HuniKipi kip : List.of(HuniKipi.SPRITE, HuniKipi.KARMA)) {
            String ad = kip.name().toLowerCase(java.util.Locale.ROOT);
            adim(300, "huni kipi " + ad, () -> KasirgaIstemciDurumu.huniKipi = kip);
            kamera(52, -12);
            adim(3000, "çekim: vitrin huni " + ad, KasirgaSpikeOtomasyonu::isinlandi, () -> cek("s5-vitrin-huni-" + ad));
            olcum(300, "S5-yuksek-huni-" + ad, 4000);
            kamera(100, -14);
            adim(3000, "çekim: 100 blok huni " + ad, KasirgaSpikeOtomasyonu::isinlandi,
                    () -> cek("s5-100blok-huni-" + ad));
            olcum(300, "S5-yuksek-100blok-huni-" + ad, 4000);
        }
        adim(300, "huni kipi ağ", () -> KasirgaIstemciDurumu.huniKipi = HuniKipi.AG);

        // S5 Ultra ve katman maliyetleri.
        adim(300, "kalite ultra", () -> komut("dogalafet spike kasirga kalite ultra"));
        kamera(52, -12);
        adim(3000, "çekim: S5 vitrin ultra", KasirgaSpikeOtomasyonu::isinlandi, () -> cek("s5-vitrin-ultra"));
        olcum(300, "S5-ultra", 5000);
        tps();
        for (Katman katman : Katman.values()) {
            String ad = katman.name().toLowerCase(java.util.Locale.ROOT);
            adim(300, "katman kapalı: " + ad, () -> KasirgaIstemciDurumu.ACIK.remove(katman));
            olcum(500, "S5-ultra-" + ad + "-kapali", 4000);
            adim(0, "katman açık: " + ad, () -> KasirgaIstemciDurumu.ACIK.add(katman));
        }

        // Soru 2: taban halkası kendi dörtgen listesiyle mi, vanilla parçacık motoruyla mı (700 / 1400).
        adim(300, "tüm katmanlar kapalı", () -> KasirgaIstemciDurumu.ACIK.clear());
        olcum(500, "S5-tum-katmanlar-kapali", 4000);
        adim(300, "yalnız taban, kalite yüksek (700)", () -> {
            KasirgaIstemciDurumu.ACIK.add(Katman.TABAN);
            komut("dogalafet spike kasirga kalite yuksek");
        });
        olcum(1000, "taban-700-kendi-dortgen", 4000);
        adim(300, "yalnız taban, kalite ultra (1400)", () -> komut("dogalafet spike kasirga kalite ultra"));
        olcum(1000, "taban-1400-kendi-dortgen", 4000);
        adim(300, "taban kapalı, 700 vanilla parçacık", () -> {
            KasirgaIstemciDurumu.ACIK.clear();
            TozParcacigiDeneyi.baslat(ilkKasirga(), 700);
        });
        olcum(1500, "taban-700-vanilla-parcacik", 4000);
        adim(300, "1400 vanilla parçacık", () -> {
            TozParcacigiDeneyi.bitir();
            TozParcacigiDeneyi.baslat(ilkKasirga(), 1400);
        });
        olcum(1500, "taban-1400-vanilla-parcacik", 4000);
        adim(300, "parçacıkları kaldır, bütün katmanlar açık", () -> {
            TozParcacigiDeneyi.bitir();
            KasirgaIstemciDurumu.ACIK.addAll(EnumSet.allOf(Katman.class));
        });

        // S5 Düşük.
        adim(500, "kalite düşük", () -> komut("dogalafet spike kasirga kalite dusuk"));
        adim(1500, "çekim: S5 vitrin düşük", () -> cek("s5-vitrin-dusuk"));
        olcum(300, "S5-dusuk", 5000);
        tps();
        dur();
        olcum(3000, "afetsiz-vitrin-noktasi", 5000);
        tps();
        adim(1500, "kapanış", KasirgaSpikeOtomasyonu::kapat);
    }

    // --- Plan yardımcıları ---

    private static void olcum(long ms, String ad, long sureMs) {
        adim(ms, "ölçüm: " + ad, () -> olcumuBaslat(ad, sureMs));
        adim(0, "ölçüm bitti: " + ad, () -> olcumBitti, () -> {
        });
    }

    private static void tps() {
        adim(200, "/forge tps", () -> {
            sohbetiYakala = true;
            komut("forge tps");
        });
        adim(1500, "/forge tps yakalama sonu", () -> sohbetiYakala = false);
    }

    private static void dur() {
        adim(500, "dur", () -> komut("dogalafet spike kasirga dur"));
        adim(1000, "istemcide kasırga ve uçan blok kalmadı", () -> KasirgaIstemciDurumu.KASIRGALAR.isEmpty()
                && ucanBlokSayisi() == 0, () -> DogalAfetler.LOGGER.info("{} istemci dur sonrası: kasirga={}, ucan_blok={},"
                + " gorsel_kopya={}", ETIKET, KasirgaIstemciDurumu.KASIRGALAR.size(), ucanBlokSayisi(),
                KasirgaIstemciDurumu.gorselEnkazSayisi()));
    }

    private static void kamera(double mesafe, float egim) {
        adim(500, "kamera " + mesafe + " blok, eğim " + egim, () -> {
            KasirgaVarligi k = ilkKasirga();
            if (k != null) {
                beklenenX = k.getX() + Math.cos(k.yon()) * mesafe;
                beklenenZ = k.getZ() + Math.sin(k.yon()) * mesafe;
            }
            komut("dogalafet spike kasirga kamera " + mesafe + " " + egim);
        });
    }

    private static void adim(long ms, String ad, Runnable is) {
        adim(ms, ad, () -> true, is);
    }

    private static void adim(long ms, String ad, BooleanSupplier kosul, Runnable is) {
        ADIMLAR.add(new Adim(ms, ad, kosul, is));
    }

    // --- Koşullar ---

    private static KasirgaVarligi ilkKasirga() {
        return KasirgaIstemciDurumu.KASIRGALAR.values().stream().findFirst().orElse(null);
    }

    private static boolean olgun() {
        KasirgaVarligi k = ilkKasirga();
        return k != null && k.evre() == KasirgaEvresi.OLGUN;
    }

    private static boolean isinlandi() {
        var oyuncu = Minecraft.getInstance().player;
        return !Double.isNaN(beklenenX) && oyuncu != null
                && Math.hypot(oyuncu.getX() - beklenenX, oyuncu.getZ() - beklenenZ) < 3;
    }

    private static int ucanBlokSayisi() {
        int sayi = 0;
        for (Entity varlik : Minecraft.getInstance().level.entitiesForRendering()) {
            if (varlik instanceof UcanBlokVarligi) {
                sayi++;
            }
        }
        return sayi;
    }

    // --- Olaylar ---

    @SubscribeEvent
    public static void giris(ClientPlayerNetworkEvent.LoggingIn olay) {
        if (!ADIMLAR.isEmpty()) {
            AgSayaci.tak(olay.getConnection());
        }
    }

    @SubscribeEvent
    public static void tik(TickEvent.ClientTickEvent olay) {
        if (ADIMLAR.isEmpty() || olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean pencereEtkin = mc.isWindowActive();
        if (sonPencereEtkin == null || sonPencereEtkin != pencereEtkin) {
            DogalAfetler.LOGGER.info("{} pencere odağı (isWindowActive): {}", ETIKET, pencereEtkin);
            sonPencereEtkin = pencereEtkin;
        }
        if (baslangic < 0) {
            if (mc.player == null || mc.level == null || mc.screen != null) {
                return;
            }
            baslangic = System.currentTimeMillis();
            oncekiAdimZamani = baslangic;
            DogalAfetler.LOGGER.info("{} dünyaya girildi; {} adım", ETIKET, ADIMLAR.size());
        }
        // Otomasyon klavye beklemediği için kendi açmadığı ekranı (ESC ile gelen PauseScreen) kapatır.
        if (mc.screen != null) {
            DogalAfetler.LOGGER.warn("{} otomasyonun açmadığı ekran kapatıldı: {}", ETIKET, mc.screen.getClass().getName());
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
                DogalAfetler.LOGGER.warn("{} koşul {} ms'de sağlanmadı, adım yine de çalışıyor: {}", ETIKET,
                        KOSUL_ZAMAN_ASIMI_MS, adim.ad());
            }
            sonraki++;
            oncekiAdimZamani = simdi;
            DogalAfetler.LOGGER.info("{} t={} ms: {}", ETIKET, simdi - baslangic, adim.ad());
            adim.is().run();
        }
    }

    /** Kare süresi ölçümü: her karenin başında bir önceki kareden geçen süre. */
    @SubscribeEvent
    public static void kare(TickEvent.RenderTickEvent olay) {
        if (olcumAdi == null || olay.phase != TickEvent.Phase.START) {
            return;
        }
        long simdi = System.nanoTime();
        if (olcumBas < 0) {
            olcumBas = simdi;
            sonKare = simdi;
            return;
        }
        if (kareSayisi == kareler.length) {
            kareler = Arrays.copyOf(kareler, kareler.length * 2);
        }
        kareler[kareSayisi++] = simdi - sonKare;
        sonKare = simdi;
        if (simdi - olcumBas >= olcumSureNs) {
            olcumuBitir();
        }
    }

    @SubscribeEvent
    public static void sohbet(ClientChatReceivedEvent olay) {
        if (sohbetiYakala) {
            DogalAfetler.LOGGER.info("{} sohbet: {}", ETIKET, olay.getMessage().getString());
        }
    }

    @SubscribeEvent
    public static void tus(InputEvent.Key olay) {
        if (!ADIMLAR.isEmpty()) {
            DogalAfetler.LOGGER.info("{} tuş olayı: key={} action={} isWindowActive={}", ETIKET, olay.getKey(),
                    olay.getAction(), Minecraft.getInstance().isWindowActive());
        }
    }

    // --- İşler ---

    private static void hazirlik() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.hideGui = true;
        mc.options.enableVsync().set(false);
        mc.getWindow().updateVsync(false);
        mc.options.framerateLimit().set(260);
        komut("gamemode spectator");
        komut("time set 6000");
        komut("weather rain");
        komut("gamerule doDaylightCycle false");
        komut("gamerule doWeatherCycle false");
        komut("tp @s ~ ~ ~ 0 -10");
        DogalAfetler.LOGGER.info("{} ayarlar: renderDistance={}, simulationDistance={}, grafik={}, parçacık={}, pencere={}x{}",
                ETIKET, mc.options.renderDistance().get(), mc.options.simulationDistance().get(),
                mc.options.graphicsMode().get(), mc.options.particles().get(), mc.getWindow().getWidth(),
                mc.getWindow().getHeight());
    }

    private static void olcumuBaslat(String ad, long sureMs) {
        olcumAdi = ad;
        olcumSureNs = sureMs * 1_000_000L;
        olcumBas = -1;
        kareSayisi = 0;
        olcumBitti = false;
        KasirgaIstatistik.sifirla();
        AgSayaci.sifirla();
        AgSayaci.olcuyor = true;
    }

    private static void olcumuBitir() {
        AgSayaci.olcuyor = false;
        Minecraft mc = Minecraft.getInstance();
        double saniye = (sonKare - olcumBas) / 1e9;
        long[] sirali = Arrays.copyOf(kareler, kareSayisi);
        Arrays.sort(sirali);
        double fps = kareSayisi / saniye;
        double dusuk = kareSayisi == 0 ? 0 : 1e9 / sirali[(int) ((kareSayisi - 1) * 0.99)];
        double ortanca = kareSayisi == 0 ? 0 : sirali[kareSayisi / 2] / 1e6;
        IntegratedServer sunucu = mc.getSingleplayerServer();
        float tickMs = sunucu == null ? -1 : sunucu.getAverageTickTime();
        KasirgaVarligi k = ilkKasirga();
        String kalite = k == null ? "-" : KasirgaIstemciDurumu.kalite(k).name();
        String satir = String.join("	", olcumAdi, String.format("%.1f", fps), String.format("%.1f", dusuk),
                String.format("%.2f", ortanca), Integer.toString(kareSayisi), String.format("%.2f", tickMs),
                kalite, Integer.toString(KasirgaIstemciDurumu.KASIRGALAR.size()), Integer.toString(ucanBlokSayisi()),
                Integer.toString(KasirgaIstemciDurumu.gorselEnkazSayisi()), KasirgaIstatistik.sayilar(),
                "parcacik=" + mc.particleEngine.countParticles(), KasirgaIstatistik.sureler(),
                KasirgaIstatistik.enterpolasyon(), AgSayaci.ozet(saniye));
        DogalAfetler.LOGGER.info("{} ÖLÇÜM {}: FPS ort={} %1-düşük={} ortanca kare={} ms ({} kare, {} sn); sunucu ort tick={} ms;"
                + " kalite={}; kasirga={} ucan_blok={} gorsel_kopya={}; {}; parcacik={}; CPU: {}; enterpolasyon: {}; ağ: {}",
                ETIKET, olcumAdi, String.format("%.1f", fps), String.format("%.1f", dusuk), String.format("%.2f", ortanca),
                kareSayisi, String.format("%.2f", saniye), String.format("%.2f", tickMs), kalite,
                KasirgaIstemciDurumu.KASIRGALAR.size(), ucanBlokSayisi(), KasirgaIstemciDurumu.gorselEnkazSayisi(),
                KasirgaIstatistik.sayilar(), mc.particleEngine.countParticles(), KasirgaIstatistik.sureler(),
                KasirgaIstatistik.enterpolasyon(), AgSayaci.ozet(saniye));
        try {
            Files.writeString(Path.of(mc.gameDirectory.getPath(), "spike-kasirga-olcumler.tsv"), satir + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException hata) {
            DogalAfetler.LOGGER.error("{} ölçüm dosyasına yazılamadı", ETIKET, hata);
        }
        olcumAdi = null;
        olcumBitti = true;
    }

    private static void komut(String komut) {
        Minecraft.getInstance().player.connection.sendCommand(komut);
    }

    private static void cek(String ad) {
        Minecraft mc = Minecraft.getInstance();
        DogalAfetler.LOGGER.info("{} çekim {}: kasirga={} ucan_blok={} gorsel_kopya={} {}", ETIKET, ad,
                KasirgaIstemciDurumu.KASIRGALAR.size(), ucanBlokSayisi(), KasirgaIstemciDurumu.gorselEnkazSayisi(),
                KasirgaIstatistik.sayilar());
        Screenshot.grab(mc.gameDirectory, "spike-kasirga-" + ad + ".png", mc.getMainRenderTarget(),
                mesaj -> DogalAfetler.LOGGER.info("{} ekran görüntüsü: {}", ETIKET, mesaj.getString()));
    }

    private static void kapat() {
        TozParcacigiDeneyi.bitir();
        DogalAfetler.LOGGER.info("{} kapanış", ETIKET);
        Minecraft.getInstance().stop();
    }
}
