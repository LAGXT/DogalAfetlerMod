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
        switch (SENARYO) {
            case "kasirga" -> plan();
            case "kasirga-tablo" -> tabloPlani();
            case "kasirga-v2" -> v2Plani();
            default -> {
            }
        }
    }

    private KasirgaSpikeOtomasyonu() {
    }

    /**
     * Yalnız FPS tablosu (soru 5): S1 ve S5 aynı vitrin karesinde (52 blok, eğim -12°), kasırga hareketsiz; her
     * turda katmansız / Düşük / Yüksek / Ultra sırayla ölçülür (sistem yükündeki kayma her seçeneğe dağılır).
     * Sonda kasırga durdurulup aynı noktada afetsiz ölçülür.
     */
    private static void tabloPlani() {
        adim(1000, "hazırlık", KasirgaSpikeOtomasyonu::hazirlik);
        adim(15000, "dünya yüklemesi bitti (sunucu ort tick < 15 ms)", () -> sunucuTickMs() < 15, () -> {
        });
        for (int seviye : new int[] {1, 5}) {
            adim(500, "S" + seviye + " başlat", () -> komut("dogalafet spike kasirga " + seviye + " kalite yuksek"));
            adim(8000, "S" + seviye + " olgun: hareketi durdur", KasirgaSpikeOtomasyonu::olgun,
                    () -> komut("dogalafet spike kasirga hareket false"));
            kamera(52, -12);
            adim(3000, "vitrin noktasında", KasirgaSpikeOtomasyonu::isinlandi, () -> {
            });
            for (int tur = 1; tur <= 3; tur++) {
                adim(300, "katmansız", () -> KasirgaIstemciDurumu.ACIK.clear());
                olcum(700, "T-S" + seviye + "-katmansiz-" + tur, 4000);
                adim(0, "katmanlar açık", () -> KasirgaIstemciDurumu.ACIK.addAll(EnumSet.allOf(Katman.class)));
                for (String kalite : List.of("dusuk", "yuksek", "ultra")) {
                    adim(300, "kalite " + kalite, () -> komut("dogalafet spike kasirga kalite " + kalite));
                    olcum(1000, "T-S" + seviye + "-" + kalite + "-" + tur, 4000);
                }
            }
            tps();
            dur();
            for (int tur = 1; tur <= 2; tur++) {
                olcum(tur == 1 ? 3000 : 500, "T-S" + seviye + "-afetsiz-" + tur, 4000);
            }
            tps();
        }
        adim(1500, "kapanış", KasirgaSpikeOtomasyonu::kapat);
    }

    /**
     * Vitrin düzeltmesi (inceleme B2) ve ölçüm kapsamı (B9): S1 yakın ve S5 vitrin (52 blok, zemin +10, eğim -8°)
     * yeni etekle, her karenin katmansız kontrolüyle; S5'te 52 blokta iki iç içe tur, sonra etek halkasının içinde (20
     * blok) ve huni çekirdeğinde (5 blok); kasırga durdurulunca aynı vitrin noktasında afetsiz.
     */
    private static void v2Plani() {
        adim(1000, "hazırlık", KasirgaSpikeOtomasyonu::hazirlik);
        adim(15000, "dünya yüklemesi bitti (sunucu ort tick < 15 ms)", () -> sunucuTickMs() < 15, () -> {
        });
        adim(500, "S1 başlat", () -> komut("dogalafet spike kasirga 1 kalite yuksek"));
        adim(8000, "S1 olgun: hareketi durdur", KasirgaSpikeOtomasyonu::olgun,
                () -> komut("dogalafet spike kasirga hareket false"));
        kamera(28, -10, 5);
        adim(4000, "çekim: S1 yakın v2", KasirgaSpikeOtomasyonu::isinlandi, () -> cek("s1-yakin-v2"));
        katmansizCekim("s1-yakin-katmansiz-v2");
        olcum(300, "V2-S1-yuksek", 4000);
        dur();

        adim(1000, "S5 yüksek başlat", () -> komut("dogalafet spike kasirga 5 kalite yuksek"));
        adim(8000, "S5 olgun: hareketi durdur", KasirgaSpikeOtomasyonu::olgun,
                () -> komut("dogalafet spike kasirga hareket false"));
        kamera(52, -8, 10);
        adim(4000, "çekim: S5 vitrin yüksek v2", () -> isinlandi() && ucanBlokSayisi() >= 20,
                () -> cek("s5-vitrin-yuksek-v2"));
        katmansizCekim("s5-vitrin-katmansiz-v2");
        adim(300, "yalnız taban katmanı (etek ölçüm karesi)", () -> {
            KasirgaIstemciDurumu.ACIK.clear();
            KasirgaIstemciDurumu.ACIK.add(Katman.TABAN);
        });
        adim(800, "çekim: yalnız etek", () -> cek("s5-vitrin-yalniz-etek-v2"));
        adim(300, "bütün katmanlar açık", () -> KasirgaIstemciDurumu.ACIK.addAll(EnumSet.allOf(Katman.class)));
        for (String kalite : List.of("ultra", "dusuk")) {
            adim(300, "kalite " + kalite, () -> komut("dogalafet spike kasirga kalite " + kalite));
            adim(1500, "çekim: S5 vitrin " + kalite + " v2", () -> cek("s5-vitrin-" + kalite + "-v2"));
        }
        for (int tur = 1; tur <= 2; tur++) {
            adim(300, "katmansız", () -> KasirgaIstemciDurumu.ACIK.clear());
            olcum(700, "V2-S5-katmansiz-" + tur, 4000);
            adim(0, "katmanlar açık", () -> KasirgaIstemciDurumu.ACIK.addAll(EnumSet.allOf(Katman.class)));
            for (String kalite : List.of("dusuk", "yuksek", "ultra")) {
                adim(300, "kalite " + kalite, () -> komut("dogalafet spike kasirga kalite " + kalite));
                olcum(1000, "V2-S5-" + kalite + "-" + tur, 4000);
            }
        }
        tps();
        for (int mesafe : new int[] {20, 5}) {
            kamera(mesafe, -8, 10);
            adim(3000, mesafe + " blokta", KasirgaSpikeOtomasyonu::isinlandi, () -> {
            });
            adim(300, "kalite yüksek", () -> komut("dogalafet spike kasirga kalite yuksek"));
            adim(1200, "çekim: S5 " + mesafe + " blok", () -> cek("s5-" + mesafe + "blok-yuksek-v2"));
            adim(300, "katmansız", () -> KasirgaIstemciDurumu.ACIK.clear());
            olcum(700, "V2-S5-" + mesafe + "blok-katmansiz", 4000);
            adim(0, "katmanlar açık", () -> KasirgaIstemciDurumu.ACIK.addAll(EnumSet.allOf(Katman.class)));
            for (String kalite : List.of("yuksek", "ultra")) {
                adim(300, "kalite " + kalite, () -> komut("dogalafet spike kasirga kalite " + kalite));
                olcum(1000, "V2-S5-" + mesafe + "blok-" + kalite, 4000);
            }
        }
        tps();
        kamera(52, -8, 10);
        adim(3000, "vitrin noktasında", KasirgaSpikeOtomasyonu::isinlandi, () -> {
        });
        adim(300, "kalite yüksek", () -> komut("dogalafet spike kasirga kalite yuksek"));
        olcum(1000, "V2-S5-yuksek-son", 4000);
        dur();
        for (int tur = 1; tur <= 2; tur++) {
            olcum(tur == 1 ? 3000 : 500, "V2-afetsiz-" + tur, 4000);
        }
        tps();
        adim(1500, "kapanış", KasirgaSpikeOtomasyonu::kapat);
    }

    private static void plan() {
        adim(1000, "hazırlık: seyirci, öğlen, yağmur, HUD gizli, vsync kapalı", KasirgaSpikeOtomasyonu::hazirlik);
        // Girişteki chunk yüklemesi FPS'i ve tick'i bastırır; temel ölçüm dünya oturunca alınır.
        adim(15000, "dünya yüklemesi bitti (sunucu ort tick < 15 ms)", () -> sunucuTickMs() < 15, () -> {
        });
        olcum(0, "afetsiz-baslangic", 5000);
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
        katmansizCekim("s5-vitrin-katmansiz");
        tps();
        // Kamera taban halkasının (yarıçapı ~31 blok) dışında, yukarı bakar: bloklar 25-60 blok yukarıda döner.
        kamera(48, -30);
        adim(2500, "çekim: kalkan bloklar", () -> isinlandi() && ucanBlokSayisi() >= 20, () -> cek("s5-kalkan-bloklar"));
        adim(300, "yalnız huni ve bloklar (kanıt karesi)", () -> {
            KasirgaIstemciDurumu.ACIK.remove(Katman.TABAN);
            KasirgaIstemciDurumu.ACIK.remove(Katman.DUVAR);
            KasirgaIstemciDurumu.ACIK.remove(Katman.ALT_GIRDAP);
        });
        adim(800, "çekim: kalkan bloklar (taban/duvar kapalı)", () -> cek("s5-kalkan-bloklar-yalniz"));
        adim(300, "bütün katmanlar açık", () -> KasirgaIstemciDurumu.ACIK.addAll(EnumSet.allOf(Katman.class)));
        kamera(100, -14);
        adim(3000, "çekim: S5 100 blok yüksek", KasirgaSpikeOtomasyonu::isinlandi, () -> cek("s5-100blok-yuksek"));
        olcum(300, "S5-yuksek-100blok", 5000);
        katmansizCekim("s5-100blok-katmansiz");

        // Soru 1: huni gövdesi teknikleri (ağ / sprite / karma), 52 ve 100 blok.
        for (HuniKipi kip : List.of(HuniKipi.SPRITE, HuniKipi.KARMA)) {
            String ad = kip.name().toLowerCase(java.util.Locale.ROOT);
            adim(300, "huni kipi " + ad, () -> KasirgaIstemciDurumu.huniKipi = kip);
            kamera(52, -12);
            adim(3000, "çekim: vitrin huni " + ad, KasirgaSpikeOtomasyonu::isinlandi, () -> cek("s5-vitrin-huni-" + ad));
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

        // FPS tablosu: aynı vitrin karesinde iç içe geçmiş turlar; sistem yükündeki kayma her seçeneğe dağılır,
        // raporda tur ortancası kullanılır. "katmansız" = aynı kare, kasırga görseli yok (afetsiz karşılığı).
        kamera(52, -12);
        adim(2000, "vitrin noktasında", KasirgaSpikeOtomasyonu::isinlandi, () -> {
        });
        for (int tur = 1; tur <= 3; tur++) {
            final int t = tur;
            adim(300, "tablo turu " + t + ": katmansız", () -> KasirgaIstemciDurumu.ACIK.clear());
            olcum(700, "tablo-katmansiz-" + t, 4000);
            adim(0, "katmanlar açık", () -> KasirgaIstemciDurumu.ACIK.addAll(EnumSet.allOf(Katman.class)));
            for (String kalite : List.of("dusuk", "yuksek", "ultra")) {
                adim(300, "kalite " + kalite, () -> komut("dogalafet spike kasirga kalite " + kalite));
                olcum(1000, "tablo-" + kalite + "-" + t, 4000);
            }
        }
        // Soru 1: huni gövdesi teknikleri, Yüksek, iç içe iki tur.
        adim(300, "kalite yüksek", () -> komut("dogalafet spike kasirga kalite yuksek"));
        for (int tur = 1; tur <= 2; tur++) {
            for (HuniKipi kip : HuniKipi.values()) {
                String ad = kip.name().toLowerCase(java.util.Locale.ROOT);
                adim(300, "huni kipi " + ad, () -> KasirgaIstemciDurumu.huniKipi = kip);
                olcum(500, "huni-" + ad + "-" + tur, 3500);
            }
        }
        adim(300, "huni kipi ağ", () -> KasirgaIstemciDurumu.huniKipi = HuniKipi.AG);
        // Katman maliyetleri (Ultra), iç içe iki tur: her katman tek başına kapatılır.
        adim(300, "kalite ultra", () -> komut("dogalafet spike kasirga kalite ultra"));
        for (int tur = 1; tur <= 2; tur++) {
            olcum(700, "katman-hepsi-acik-" + tur, 3500);
            for (Katman katman : Katman.values()) {
                String ad = katman.name().toLowerCase(java.util.Locale.ROOT);
                adim(300, "katman kapalı: " + ad, () -> KasirgaIstemciDurumu.ACIK.remove(katman));
                olcum(500, "katman-" + ad + "-kapali-" + tur, 3500);
                adim(0, "katman açık: " + ad, () -> KasirgaIstemciDurumu.ACIK.add(katman));
            }
        }

        // S5 Düşük.
        adim(500, "kalite düşük", () -> komut("dogalafet spike kasirga kalite dusuk"));
        adim(1500, "çekim: S5 vitrin düşük", () -> cek("s5-vitrin-dusuk"));
        olcum(300, "S5-dusuk", 5000);
        tps();
        dur();
        for (int tur = 1; tur <= 2; tur++) {
            olcum(tur == 1 ? 3000 : 500, "afetsiz-vitrin-noktasi-" + tur, 4000);
        }
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

    /** Aynı kareden kasırgasız kontrol görüntüsü: fark analizi kasırganın kapladığı alanı gösterir. */
    private static void katmansizCekim(String ad) {
        adim(300, "bütün katmanlar kapalı (kontrol)", () -> KasirgaIstemciDurumu.ACIK.clear());
        adim(800, "çekim: " + ad, () -> cek(ad));
        adim(300, "bütün katmanlar açık", () -> KasirgaIstemciDurumu.ACIK.addAll(EnumSet.allOf(Katman.class)));
    }

    private static void kamera(double mesafe, float egim) {
        kamera(mesafe, egim, 3);
    }

    private static void kamera(double mesafe, float egim, double yukseklik) {
        adim(500, "kamera " + mesafe + " blok, eğim " + egim + ", zemin +" + yukseklik, () -> {
            KasirgaVarligi k = ilkKasirga();
            if (k != null) {
                beklenenX = k.getX() + Math.cos(k.yon()) * mesafe;
                beklenenZ = k.getZ() + Math.sin(k.yon()) * mesafe;
            }
            komut("dogalafet spike kasirga kamera " + mesafe + " " + egim + " " + yukseklik);
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

    private static float sunucuTickMs() {
        IntegratedServer sunucu = Minecraft.getInstance().getSingleplayerServer();
        return sunucu == null ? 0 : sunucu.getAverageTickTime();
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
        // Sabit başlangıç: güneye bakar, kasırga 60 blok önde (20, ~66, 65) karada doğar (koşu 2'de rastgele doğuş
        // noktasından bakılınca S1 suyun üstünde doğdu ve hiç blok kaldırmadı).
        komut("tp @s 20 80 5 0 -10");
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
