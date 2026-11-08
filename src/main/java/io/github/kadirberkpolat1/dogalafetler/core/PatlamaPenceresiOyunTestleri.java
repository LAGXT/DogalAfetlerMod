package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.GunSaatiPenceresi;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Gökyüzü-2 istek 26 oyun içi testleri, K-E67 biçimiyle: doğal güneş patlaması yalnız sabah, ay çarpması yalnız gece
 * seçilebilir (pencere seçimde, {@code AfetYoneticisi.secimCarpani}); seçilen afet ön-uyarıda beklemeden ~30 sn sonra
 * pencerede patlar; komutla başlayan pencereye bakmaz. Doğal başlatma paket özel {@code secimleBaslat} ile yapıldığı için
 * sınıf {@code core} paketindedir. Her test kendi batch'inde; başta ve sonda {@code sifirla} (K-E15), değiştirilen gamerule
 * ve gün saati geri konur; config dosyasına yazılmaz (K-E28).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PatlamaPenceresiOyunTestleri {
    /** Seçimden AKTIF'e en çok süre (gerçek tick, hız 1): varsayılan ön-uyarı en çok 160 + siren 500, üstüne pay. */
    private static final int GORUNME_TICK = 700;
    private static final List<Runnable> GERI_AL = new ArrayList<>();

    private PatlamaPenceresiOyunTestleri() {
    }

    @AfterBatch(batch = "pencere_gunes_dogal")
    public static void gunesSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "pencere_ay_dogal")
    public static void aySonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "pencere_gunes_otuz")
    public static void otuzSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "pencere_komut")
    public static void komutSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    private static void temizle(MinecraftServer server) {
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        yonetici.sifirla();
        for (int i = GERI_AL.size() - 1; i >= 0; i--) {
            GERI_AL.get(i).run();
        }
        GERI_AL.clear();
    }

    private static void kural(MinecraftServer server, GameRules.Key<GameRules.BooleanValue> anahtar, boolean deger) {
        GameRules.BooleanValue k = server.getGameRules().getRule(anahtar);
        boolean eski = k.get();
        GERI_AL.add(() -> k.set(eski, server));
        k.set(deger, server);
    }

    /** Temiz dönem, gün döngüsü açık, blok hasarı kapalı, afet kuralları açık, gün saati {@code gunSaati}. */
    private static AfetYoneticisi kur(GameTestHelper y, long gunSaati) {
        MinecraftServer server = y.getLevel().getServer();
        temizle(server);
        ServerLevel ust = server.overworld();
        long eskiSaat = ust.getDayTime();
        GERI_AL.add(() -> ust.setDayTime(eskiSaat));
        kural(server, GameRules.RULE_DAYLIGHT, true);
        kural(server, Kurallar.AFET_BLOK_HASARI, false);
        kural(server, Kurallar.DOGAL_AFETLER_ACIK, true);
        kural(server, Kurallar.GUNES_PATLAMASI_AFETI, true);
        kural(server, Kurallar.AY_CARPMASI_AFETI, true);
        ust.setDayTime(gunSaati);
        return AfetYoneticisi.al(server);
    }

    /** Seçilebilirlik: doğal seçim çarpanı pencere dışında 0, içinde > 0; gün döngüsü kapalıyken pencere yok. */
    private static void yalnizPenceredeSecilir(GameTestHelper y, AfetTuru tur, long[] secilir, long[] secilmez) {
        AfetYoneticisi yonetici = kur(y, secilir[0]);
        MinecraftServer server = y.getLevel().getServer();
        ServerLevel ust = server.overworld();
        for (long saat : secilir) {
            ust.setDayTime(saat);
            y.assertTrue(yonetici.secimCarpani(tur) > 0, tur.kimlik() + " gün saati " + saat + "'de seçilebilmeli");
        }
        for (long saat : secilmez) {
            ust.setDayTime(saat);
            y.assertTrue(yonetici.secimCarpani(tur) == 0, tur.kimlik() + " gün saati " + saat + "'de seçilmemeli");
        }
        kural(server, GameRules.RULE_DAYLIGHT, false);
        y.assertTrue(yonetici.secimCarpani(tur) > 0, "gün döngüsü kapalı: pencere hiç gelmeyebilir, seçilebilir");
        temizle(server);
        y.succeed();
    }

    @GameTest(template = "bos", batch = "pencere_gunes_dogal")
    public static void dogalGunesPatlamasiYalnizSabahSecilir(GameTestHelper y) {
        yalnizPenceredeSecilir(y, AfetTuru.GUNES_PATLAMASI, new long[] {0L, 1_000L, 5_100L},
                new long[] {5_300L, 6_000L, 18_000L, 23_900L});
    }

    @GameTest(template = "bos", batch = "pencere_ay_dogal")
    public static void dogalAyCarpmasiYalnizGeceSecilir(GameTestHelper y) {
        yalnizPenceredeSecilir(y, AfetTuru.AY_CARPMASI, new long[] {13_000L, 18_000L, 22_100L},
                new long[] {22_300L, 6_000L, 12_900L});
    }

    /**
     * Sabah seçilen doğal güneş patlaması ön-uyarıda beklemeden hız 1'de {@value #GORUNME_TICK} tick içinde AKTIF'e
     * geçer ve patlama anı pencerededir.
     */
    @GameTest(template = "bos", batch = "pencere_gunes_otuz", timeoutTicks = 900)
    public static void dogalGunesPatlamasiOtuzSaniyedePenceredePatlar(GameTestHelper y) {
        AfetYoneticisi yonetici = kur(y, 1_000L);
        MinecraftServer server = y.getLevel().getServer();
        ServerLevel ust = server.overworld();
        y.assertTrue(yonetici.secimCarpani(AfetTuru.GUNES_PATLAMASI) > 0, "sabah seçilebilmeli");
        y.assertTrue(yonetici.secimleBaslat(AfetTuru.GUNES_PATLAMASI) == AfetYoneticisi.BaslatmaSonucu.BASLADI,
                "güneş patlaması başlamadı");
        Afet afet = yonetici.aktifAfet().orElseThrow();
        y.assertTrue(!afet.baglam().elle(), "seçimle (doğal) başlamalı");
        long bas = ust.getGameTime();
        long[] aktif = {-1L, -1L};
        y.onEachTick(() -> {
            if (aktif[0] < 0 && afet.asama() == AfetAsamasi.AKTIF) {
                aktif[0] = ust.getGameTime() - bas;
                aktif[1] = ust.getDayTime();
            }
        });
        y.succeedWhen(() -> {
            y.assertTrue(aktif[0] >= 0, "AKTIF bekleniyor: " + afet.asama());
            DogalAfetler.LOGGER.info("Pencere GameTest: doğal güneş patlaması seçimden {} tick sonra AKTIF, dayTime {}",
                    aktif[0], aktif[1]);
            y.assertTrue(aktif[0] <= GORUNME_TICK, "seçimden AKTIF'e " + aktif[0] + " tick");
            y.assertTrue(GunSaatiPenceresi.SABAH.icinde(aktif[1]), "patlama sabah penceresinde: " + aktif[1]);
            temizle(server);
        });
    }

    @GameTest(template = "bos", batch = "pencere_komut", timeoutTicks = 200)
    public static void komutlaBaslayanPatlamaPencereyiBeklemez(GameTestHelper y) {
        AfetYoneticisi yonetici = kur(y, 18_000L);
        MinecraftServer server = y.getLevel().getServer();
        ServerLevel ust = server.overworld();
        y.assertTrue(yonetici.elleBaslat(AfetTuru.GUNES_PATLAMASI, null, null, y.getLevel(), y.absolutePos(BlockPos.ZERO))
                == AfetYoneticisi.BaslatmaSonucu.BASLADI, "komutla başlamadı");
        Afet afet = yonetici.aktifAfet().orElseThrow();
        yonetici.hizAyarla(1000);
        long[] aktifSaat = {-1L};
        int[] sonra = {0};
        y.onEachTick(() -> {
            if (aktifSaat[0] < 0 && afet.asama() == AfetAsamasi.AKTIF) {
                aktifSaat[0] = ust.getDayTime();
            }
        });
        y.succeedWhen(() -> {
            y.assertTrue(aktifSaat[0] >= 0, "komutla başlayan afet gece hemen AKTIF olmalı: " + afet.asama());
            y.assertTrue(!GunSaatiPenceresi.SABAH.icinde(aktifSaat[0]), "pencereye bakılmadı (gece): " + aktifSaat[0]);
            if (++sonra[0] < 3) {
                y.fail("gün saati sabitleyicisi bekleniyor");
            }
            y.assertTrue(Math.floorMod(ust.getDayTime(), GunSaatiPenceresi.GUN_TICK) == 6_000L,
                    "pencere dışındaki komutlu patlama öğleye geçmeli (istek 25): " + ust.getDayTime());
            temizle(server);
        });
    }
}
