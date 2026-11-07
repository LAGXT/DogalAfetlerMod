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
 * Gökyüzü-2 istek 26 oyun içi testleri: doğal başlayan güneş patlaması AKTIF'e yalnız sabah, ay çarpması yalnız gece geçer
 * (ON_UYARI'da bekler); komutla başlayan beklemez. Doğal başlatma paket özel {@code secimleBaslat} ile yapıldığı için sınıf
 * {@code core} paketindedir. Her test kendi batch'inde; başta ve sonda {@code sifirla} (K-E15), değiştirilen gamerule ve gün
 * saati geri konur; config dosyasına yazılmaz (K-E28). {@code /dogalafet hiz 1000}: ON_UYARI ve siren birkaç tick sürer.
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PatlamaPenceresiOyunTestleri {
    /** ON_UYARI süresi dolduktan sonra beklemenin sürdüğü gözlenen tick. */
    private static final int BEKLEME_TICK = 20;
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

    /**
     * Doğal başlayan afet ON_UYARI süresi dolduktan sonra {@value #BEKLEME_TICK} tick ON_UYARI'da kalır; sonra gün saati
     * pencere başının {@code 10} tick öncesine alınır ve AKTIF'e geçtiği andaki gün saati pencerede olmalı.
     */
    private static void pencereyeKadarBekler(GameTestHelper y, AfetTuru tur, long baslangicSaati) {
        AfetYoneticisi yonetici = kur(y, baslangicSaati);
        MinecraftServer server = y.getLevel().getServer();
        ServerLevel ust = server.overworld();
        GunSaatiPenceresi pencere = GunSaatiPenceresi.dogalPatlama(tur).orElseThrow();
        y.assertTrue(yonetici.secimleBaslat(tur) == AfetYoneticisi.BaslatmaSonucu.BASLADI, tur.kimlik() + " başlamadı");
        Afet afet = yonetici.aktifAfet().orElseThrow();
        y.assertTrue(!afet.baglam().elle(), "seçimle (doğal) başlamalı");
        yonetici.hizAyarla(1000);
        int[] bekleyen = {0};
        long[] aktifSaat = {-1L};
        y.onEachTick(() -> {
            if (aktifSaat[0] < 0 && afet.asama() == AfetAsamasi.AKTIF) {
                aktifSaat[0] = ust.getDayTime();
            }
        });
        int[] sonra = {0};
        y.succeedWhen(() -> {
            if (bekleyen[0] < BEKLEME_TICK) {
                y.assertTrue(afet.asama() == AfetAsamasi.ON_UYARI, "pencere dışında ON_UYARI'da beklemeliydi: "
                        + afet.asama() + ", dayTime " + ust.getDayTime());
                if (afet.asamaKalanTick() == 0 && ++bekleyen[0] == BEKLEME_TICK) {
                    long gun = Math.floorDiv(ust.getDayTime(), GunSaatiPenceresi.GUN_TICK) * GunSaatiPenceresi.GUN_TICK;
                    long hedef = gun + Math.floorMod(pencere.bas() - 10, GunSaatiPenceresi.GUN_TICK);
                    DogalAfetler.LOGGER.info("Pencere GameTest ({}): {} tick beklendi, dayTime {} -> {}", tur.kimlik(),
                            BEKLEME_TICK, ust.getDayTime(), hedef);
                    ust.setDayTime(hedef);
                }
                y.fail("ON_UYARI'da pencere bekleniyor");
            }
            y.assertTrue(aktifSaat[0] >= 0, "pencere başında AKTIF bekleniyordu: " + afet.asama() + ", dayTime "
                    + ust.getDayTime());
            y.assertTrue(pencere.icinde(aktifSaat[0]), "AKTIF'e geçiş pencerede olmalı: dayTime " + aktifSaat[0]);
            if (++sonra[0] < 3) {
                y.fail("gün saati sabitleyicisi bekleniyor");
            }
            if (tur == AfetTuru.AY_CARPMASI) {
                y.assertTrue(yonetici.donem().ayDurumu() == AyDurumu.YOK, "ay çarptı: " + yonetici.donem().ozet());
            } else {
                y.assertTrue(pencere.icinde(ust.getDayTime()), "doğal güneş patlamasında öğleye zıplama yok: "
                        + ust.getDayTime());
            }
            DogalAfetler.LOGGER.info("Pencere GameTest ({}): AKTIF dayTime {} (pencere [{}, {}))", tur.kimlik(), aktifSaat[0],
                    pencere.bas(), pencere.son());
            temizle(server);
        });
    }

    @GameTest(template = "bos", batch = "pencere_gunes_dogal", timeoutTicks = 400)
    public static void dogalGunesPatlamasiGeceBaslarSabahPatlar(GameTestHelper y) {
        pencereyeKadarBekler(y, AfetTuru.GUNES_PATLAMASI, 18_000L);
    }

    @GameTest(template = "bos", batch = "pencere_ay_dogal", timeoutTicks = 400)
    public static void dogalAyCarpmasiGunduzBaslarGecePatlar(GameTestHelper y) {
        pencereyeKadarBekler(y, AfetTuru.AY_CARPMASI, 6_000L);
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
