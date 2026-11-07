package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyEgrileri;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.AyDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.komut.DogalAfetKomutu;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Gökyüzü-2 oyun içi testleri: pulsar dönemi (başlar, döneme yazılır, kayıttan döner; donmayı çözer, buharlaştırır;
 * zincirin doğal doğuşu seçimi izler), güneş patlaması AKTIF'te gün saatinin sabitliği ve ay patlamasının sunucu
 * saatinden ömrü. Her test dönemi değiştirdiği için kendi {@code batch}'indedir; başta ve sonda {@code sifirla} (K-E15),
 * değiştirilen gamerule ve gün saati geri konur; config dosyasına yazılmaz (K-E28).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PulsarOyunTestleri {
    private static Boolean eskiGunDongusu;
    private static Boolean eskiBlokHasari;
    private static long eskiGunSaati = -1;

    private PulsarOyunTestleri() {
    }

    @AfterBatch(batch = "gokyuzu2_pulsar")
    public static void pulsarSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "gokyuzu2_cozulme")
    public static void cozulmeSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "gokyuzu2_gun_saati")
    public static void gunSaatiSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "gokyuzu2_ay_omru")
    public static void ayOmruSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "gokyuzu2_dogus")
    public static void dogusSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    private static void temizle(MinecraftServer server) {
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        yonetici.sifirla();
        if (eskiGunDongusu != null) {
            server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(eskiGunDongusu, server);
            eskiGunDongusu = null;
        }
        if (eskiBlokHasari != null) {
            server.getGameRules().getRule(Kurallar.AFET_BLOK_HASARI).set(eskiBlokHasari, server);
            eskiBlokHasari = null;
        }
        if (eskiGunSaati >= 0) {
            server.overworld().setDayTime(eskiGunSaati);
            eskiGunSaati = -1;
        }
    }

    private static AfetYoneticisi temizBasla(GameTestHelper y) {
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        yonetici.durdur();
        yonetici.sifirla();
        return yonetici;
    }

    private static int komut(GameTestHelper y, String komut) {
        MinecraftServer server = y.getLevel().getServer();
        try {
            return server.getCommands().getDispatcher().execute(komut, server.createCommandSourceStack()
                    .withSuppressedOutput().withPermission(DogalAfetKomutu.YETKI_SEVIYESI));
        } catch (CommandSyntaxException hata) {
            y.fail("/" + komut + " çalışmadı: " + hata.getMessage());
            return -1;
        }
    }

    /** Gün döngüsü, blok hasarı ve gün saati; eski değerleri {@link #temizle} geri koyar. */
    private static void kurallar(GameTestHelper y, boolean gunDongusu, long gunSaati) {
        MinecraftServer server = y.getLevel().getServer();
        GameRules.BooleanValue dongu = server.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        GameRules.BooleanValue hasar = server.getGameRules().getRule(Kurallar.AFET_BLOK_HASARI);
        eskiGunDongusu = dongu.get();
        eskiBlokHasari = hasar.get();
        eskiGunSaati = server.overworld().getDayTime();
        dongu.set(gunDongusu, server);
        hasar.set(false, server);
        server.overworld().setDayTime(gunSaati);
    }

    @GameTest(template = "bos", batch = "gokyuzu2_pulsar", timeoutTicks = 200)
    public static void pulsarBaslarDonemeYazilirKayittanDoner(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        ServerLevel ust = y.getLevel().getServer().overworld();
        PulsarSureci surec = PulsarSureci.al();
        y.assertTrue(komut(y, "dogalafet pulsar baslat") == 1, "pulsar baslat");
        y.assertTrue(donem.gunesEvresi() == GunesEvresi.PULSAR && surec.aktifMi(ust), "pulsar doğmalı: " + donem.ozet());
        y.assertTrue(komut(y, "dogalafet pulsar baslat") == 0, "ikinci baslat reddedilmeli");
        yonetici.hizAyarla(1000);
        y.succeedWhen(() -> {
            CompoundTag veri = donem.ekVeri(PulsarSureci.KIMLIK);
            y.assertTrue(veri.contains(PulsarSureci.DONGU) && veri.getLong(PulsarSureci.DONGU) == donem.evreBaslangicSaati(),
                    "döngü kimliği pulsar başlangıcı olmalı");
            y.assertTrue(donem.evreIlerlemesi() > 0f, "ısınma ilerlemeli: " + donem.ozet());
            DunyaDonemi geri = DunyaDonemi.yukle(donem.save(new CompoundTag()));
            y.assertTrue(geri.gunesEvresi() == GunesEvresi.PULSAR && geri.evreIlerlemesi() == donem.evreIlerlemesi()
                    && geri.evreBaslangicSaati() == donem.evreBaslangicSaati()
                    && geri.ekVeri(PulsarSureci.KIMLIK).getLong(PulsarSureci.DONGU) == donem.evreBaslangicSaati(),
                    "pulsar kayıt/okumada korunmalı");
            y.assertTrue(komut(y, "dogalafet asama ileri pulsar") == 1 && donem.evreIlerlemesi() == 1f,
                    "asama ileri ısınmayı tamamlar");
            y.assertTrue(komut(y, "dogalafet sifirla") == 1, "sifirla");
            y.assertTrue(donem.gunesEvresi() == GunesEvresi.NORMAL && !surec.aktifMi(ust), "sifirla sonrası NORMAL");
            yonetici.sifirla();
        });
    }

    @GameTest(template = "bos", batch = "gokyuzu2_cozulme", timeoutTicks = 300)
    public static void pulsarDonmayiCozerSuyuBuharlastirir(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        donem.gunesEvresiAyarla(GunesEvresi.GUNESSIZ);
        donem.donmaIlerlemesiAyarla(60f);
        y.assertTrue(komut(y, "dogalafet pulsar baslat") == 1, "pulsar baslat");
        yonetici.hizAyarla(1000);
        y.succeedWhen(() -> {
            y.assertTrue(donem.donmaIlerlemesi() < 59f, "donma geri çözülmeli: " + donem.ozet());
            y.assertTrue(donem.buharlasmaDerinligi() > 0f, "su buharlaşmalı: " + donem.ozet());
            y.assertTrue(!donem.cozulmeDonemi(), "pulsarda kalıcı kış (çözülme dönemi) yok");
            yonetici.sifirla();
        });
    }

    @GameTest(template = "bos", batch = "gokyuzu2_dogus", timeoutTicks = 400)
    public static void zincirinDogalDogusuSecimiIzler(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        ServerLevel ust = y.getLevel().getServer().overworld();
        kurallar(y, false, 1_000L);
        donem.gunesEvresiAyarla(GunesEvresi.SUPERNOVA);
        GunesEvresi[] beklenen = {null};
        yonetici.hizAyarla(1000);
        y.succeedWhen(() -> {
            if (beklenen[0] == null) {
                y.assertTrue(donem.ekVeri(GunesZinciriSureci.KIMLIK).contains("dongu"), "zincir döngüsü bekleniyor");
                beklenen[0] = GunesZinciriSureci.dogacakYildiz(ust, donem);
                y.assertTrue(beklenen[0] == GunesZinciriSureci.dogacakYildiz(ust, donem), "seçim belirlenimci");
                y.assertTrue(beklenen[0] == GunesEvresi.KIZIL_CUCE || beklenen[0] == GunesEvresi.PULSAR, "yeni yıldız");
                donem.gunesEvresiAyarla(GunesEvresi.GUNESSIZ);
                donem.donmaIlerlemesiAyarla(100f);
                y.fail("doğuş bekleniyor");
            }
            y.assertTrue(donem.gunesEvresi() == beklenen[0], "doğal doğuş seçimi izlemeli: " + beklenen[0] + " / "
                    + donem.ozet());
            temizle(y.getLevel().getServer());
        });
    }

    @GameTest(template = "bos", batch = "gokyuzu2_gun_saati", timeoutTicks = 300)
    public static void gunesPatlamasiAktifteGunSaatiSabitKalir(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        MinecraftServer server = y.getLevel().getServer();
        ServerLevel ust = server.overworld();
        kurallar(y, true, 18_000L);
        AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(AfetTuru.GUNES_PATLAMASI, null, null, y.getLevel(),
                y.absolutePos(BlockPos.ZERO));
        y.assertTrue(sonuc == AfetYoneticisi.BaslatmaSonucu.BASLADI, "güneş patlaması başlamadı: " + sonuc);
        yonetici.asamaIleri();
        yonetici.asamaIleri();
        y.assertTrue(yonetici.aktifAfet().filter(a -> a.asama() == AfetAsamasi.AKTIF).isPresent(), "AKTIF bekleniyordu");
        long[] sabit = {-1L};
        int[] tick = {0};
        y.succeedWhen(() -> {
            if (++tick[0] < 3) {
                y.fail("sabitleyici bekleniyor");
            }
            if (sabit[0] < 0) {
                sabit[0] = ust.getDayTime();
                y.assertTrue(sabit[0] == 30_000L, "gece AKTIF başlayınca ertesi öğleye geçmeli: " + sabit[0]);
                y.fail("sabit saat izleniyor");
            }
            y.assertTrue(ust.getDayTime() == sabit[0], "AKTIF'te gün saati sabit kalmalı: " + ust.getDayTime());
            if (tick[0] < 80) {
                y.fail("80 tick izleniyor");
            }
            y.assertTrue(server.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT), "doDaylightCycle'a dokunulmamalı");
            y.assertTrue(DunyaDonemi.al(server).ekVeri(GunSaatiSabitleyici.KIMLIK).getLong(GunSaatiSabitleyici.HEDEF)
                    == sabit[0], "sabit saat dönemde kalıcı");
            temizle(server);
        });
    }

    @GameTest(template = "bos", batch = "gokyuzu2_ay_omru", timeoutTicks = 200)
    public static void ayPatlamasiOmruDolununcaKaybolur(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        kurallar(y, false, 18_000L);
        AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(AfetTuru.AY_CARPMASI, null, null, y.getLevel(),
                y.absolutePos(BlockPos.ZERO));
        y.assertTrue(sonuc == AfetYoneticisi.BaslatmaSonucu.BASLADI, "ay çarpması başlamadı: " + sonuc);
        yonetici.asamaIleri();
        yonetici.asamaIleri();
        y.assertTrue(donem.ayDurumu() == AyDurumu.YOK && donem.ayOlaySaati() >= 0, "çarpma: " + donem.ozet());
        float ilk = (donem.saat() - donem.ayOlaySaati()) / 20f;
        y.assertTrue(AyEgrileri.patlamaGorunur(ilk), "çarpma anında patlama görünür: " + ilk);
        yonetici.hizAyarla(20);
        y.succeedWhen(() -> {
            float s = (donem.saat() - donem.ayOlaySaati()) / 20f;
            y.assertTrue(s > AyEgrileri.PATLAMA_SN, "ömür bekleniyor: " + s);
            y.assertTrue(!AyEgrileri.patlamaGorunur(s) && !AyEgrileri.carpmaIzleriSuruyor(s, false),
                    "ömür dolunca patlama ve izleri kaybolmalı: " + s);
            temizle(y.getLevel().getServer());
        });
    }
}
