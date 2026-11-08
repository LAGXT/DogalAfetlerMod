package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyEgrileri;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.AyDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.core.PulsarYolu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.TekSeferlikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarZinciri;
import io.github.kadirberkpolat1.dogalafetler.komut.DogalAfetKomutu;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Gökyüzü-2 ve pulsar-2 oyun içi testleri: pulsar dönemi (başlar, döneme yazılır, kayıttan döner; donmayı çözer,
 * buharlaştırır; zincirin doğal doğuşu seçimi izler), pulsar zinciri (PULSAR → KARADELİK_YUTMA → GÜNEŞSİZ → KIZIL CÜCE
 * → NORMAL: {@code asama ileri}, oyun günü süreleri, 90 sn yutma, eski güneşin dönüşü, {@code sifirla}, {@code durum}
 * satırı, kayıt gidiş-dönüşü, K-C46 kural duraklaması, dönüşten sonra tek seferlik güneş patlaması), güneş patlaması
 * AKTIF'te gün saatinin sabitliği ve ay patlamasının sunucu saatinden ömrü. Pulsarın dünya etkileri {@code PulsarDunyaOyunTestleri}'ndedir. Her test dönemi değiştirdiği için kendi
 * {@code batch}'indedir; başta ve sonda {@code sifirla} (K-E15), değiştirilen gamerule ve gün saati geri konur; config
 * dosyasına yazılmaz (K-E28). Zincir testleri blok hasarını kapatır (etkiler süreci sarsmasın).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PulsarOyunTestleri {
    private static Boolean eskiGunDongusu;
    private static Boolean eskiBlokHasari;
    private static Boolean eskiGunesKurali;
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

    @AfterBatch(batch = "pulsar2_asama")
    public static void asamaSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "pulsar2_gun")
    public static void gunSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "pulsar2_yutma")
    public static void yutmaSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "pulsar2_donus")
    public static void donusSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "pulsar2_sifirla")
    public static void sifirlaSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "pulsar2_durum")
    public static void durumSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "pulsar2_kayit")
    public static void kayitSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "pulsar2_kural_gun")
    public static void kuralGunSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "pulsar2_kural_yutma")
    public static void kuralYutmaSonrasi(ServerLevel level) {
        temizle(level.getServer());
    }

    @AfterBatch(batch = "pulsar2_tek_seferlik")
    public static void tekSeferlikSonrasi(ServerLevel level) {
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
        if (eskiGunesKurali != null) {
            server.getGameRules().getRule(Kurallar.GUNES_PATLAMASI_AFETI).set(eskiGunesKurali, server);
            eskiGunesKurali = null;
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

    /** {@code gunesPatlamasiAfeti}; ilk eski değeri {@link #temizle} geri koyar. */
    private static void gunesKurali(GameTestHelper y, boolean acik) {
        MinecraftServer server = y.getLevel().getServer();
        GameRules.BooleanValue kural = server.getGameRules().getRule(Kurallar.GUNES_PATLAMASI_AFETI);
        if (eskiGunesKurali == null) {
            eskiGunesKurali = kural.get();
        }
        kural.set(acik, server);
    }

    /** PULSAR'ın kalan oyun tick'i (K-C29). */
    private static long pulsarKalan(ServerLevel ust, DunyaDonemi donem) {
        return PulsarZinciri.kalanTick(PulsarYolu.baslangic(donem), PulsarSureci.oyunTick(ust, donem),
                GokyuzuAyarlari.pulsarGun());
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
        yonetici.hizAyarla(20);
        y.succeedWhen(() -> {
            y.assertTrue(PulsarYolu.donguVar(donem) && PulsarYolu.dongu(donem) == donem.evreBaslangicSaati(),
                    "döngü kimliği pulsar başlangıcı olmalı");
            y.assertTrue(donem.evreIlerlemesi() > 0f && donem.evreIlerlemesi() < 1f, "ısınma ilerlemeli: " + donem.ozet());
            DunyaDonemi geri = DunyaDonemi.yukle(donem.save(new CompoundTag()));
            y.assertTrue(geri.gunesEvresi() == GunesEvresi.PULSAR && geri.evreIlerlemesi() == donem.evreIlerlemesi()
                    && geri.evreBaslangicSaati() == donem.evreBaslangicSaati()
                    && PulsarYolu.dongu(geri) == donem.evreBaslangicSaati(),
                    "pulsar kayıt/okumada korunmalı");
            y.assertTrue(komut(y, "dogalafet asama ileri pulsar") == 1 && donem.evreIlerlemesi() == 1f
                    && donem.gunesEvresi() == GunesEvresi.PULSAR, "asama ileri önce ısınmayı tamamlar");
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

    @GameTest(template = "bos", batch = "pulsar2_asama", timeoutTicks = 100)
    public static void asamaIleriPulsarZinciriniEskiGuneseGoturur(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        MinecraftServer server = y.getLevel().getServer();
        ServerLevel ust = server.overworld();
        kurallar(y, true, 6_000L);
        y.assertTrue(komut(y, "dogalafet pulsar baslat") == 1, "pulsar baslat");
        y.assertTrue(komut(y, "dogalafet asama ileri karadelik") == 0, "takma ad yalnız kendi aşamasında geçer");
        y.assertTrue(komut(y, "dogalafet asama ileri pulsar") == 1 && donem.gunesEvresi() == GunesEvresi.PULSAR
                && donem.evreIlerlemesi() == 1f, "önce ısınma tamamlanır: " + donem.ozet());
        y.assertTrue(komut(y, "dogalafet asama ileri pulsar") == 1
                && donem.gunesEvresi() == GunesEvresi.KARADELIK_YUTMA && !PulsarYolu.donusYolu(donem),
                "kara delik yutması: " + donem.ozet());
        y.assertTrue(komut(y, "dogalafet asama ileri karadelik") == 1 && donem.gunesEvresi() == GunesEvresi.GUNESSIZ
                && PulsarYolu.donusYolu(donem) && PulsarYolu.gunessizBaslangicVar(donem), "buzul kıyamet: " + donem.ozet());
        y.assertTrue(PulsarSureci.al().aktifMi(ust) && !GunesZinciriSureci.al().aktifMi(ust),
                "pulsar yolunda güneşsiz dönem pulsar sürecinin, güneş zinciri devre dışı");
        y.assertTrue(DonemPaketi.den(donem, 1.0, false).donusYolu() && !DonemPaketi.den(donem, 1.0, false).blokHasari(),
                "paket bayrakları");
        y.assertTrue(komut(y, "dogalafet asama ileri gunessiz") == 1 && donem.gunesEvresi() == GunesEvresi.KIZIL_CUCE
                && PulsarYolu.donusYolu(donem) && donem.cozulmeDonemi(), "kızıl cüce doğuşu: " + donem.ozet());
        y.assertTrue(komut(y, "dogalafet asama ileri kizilcuce") == 1 && donem.gunesEvresi() == GunesEvresi.NORMAL
                && !PulsarYolu.donusYolu(donem) && PulsarYolu.gunesDondu(donem), "eski güneş: " + donem.ozet());
        y.assertTrue(DonemPaketi.den(donem, 1.0, true).gunesDondu() && !DonemPaketi.den(donem, 1.0, true).donusYolu(),
                "paket güneş döndü bayrağı");
        y.assertTrue(komut(y, "dogalafet asama ileri pulsar") == 0, "zincir bitti");
        y.succeedWhen(() -> {
            y.assertTrue(!PulsarYolu.gunesDondu(donem) && !donem.cozulmeDonemi(),
                    "donma 0'ken dönüş bayrağı ve çözülme dönemi kapanmalı: " + donem.ozet());
            temizle(server);
        });
    }

    @GameTest(template = "bos", batch = "pulsar2_gun", timeoutTicks = 100)
    public static void pulsarOyunGunuDoluncaKaraDelikBaslar(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        kurallar(y, true, 6_000L);
        y.assertTrue(komut(y, "dogalafet pulsar baslat") == 1, "pulsar baslat");
        int[] adim = {0};
        y.succeedWhen(() -> {
            if (adim[0] == 0) {
                y.assertTrue(PulsarYolu.baslangicVar(donem) && PulsarYolu.dongu(donem) == donem.evreBaslangicSaati(),
                        "başlangıç döngü başında yazılmalı");
                donem.gunOfsetiAyarla(donem.gunOfseti() + GokyuzuAyarlari.pulsarGun() - 1);
                adim[0] = 1;
                y.fail("bir gün kala");
            }
            if (adim[0] == 1) {
                y.assertTrue(donem.gunesEvresi() == GunesEvresi.PULSAR, "süre dolmadan evre değişmemeli: " + donem.ozet());
                donem.gunOfsetiAyarla(donem.gunOfseti() + 1);
                adim[0] = 2;
                y.fail("süre doluyor");
            }
            y.assertTrue(donem.gunesEvresi() == GunesEvresi.KARADELIK_YUTMA,
                    "pulsarGun oyun günü dolunca kara delik: " + donem.ozet());
            temizle(y.getLevel().getServer());
        });
    }

    @GameTest(template = "bos", batch = "pulsar2_yutma", timeoutTicks = 200)
    public static void karaDelikDoksanSaniyedeBuzulaGecer(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        kurallar(y, true, 6_000L);
        y.assertTrue(komut(y, "dogalafet pulsar baslat") == 1, "pulsar baslat");
        y.assertTrue(komut(y, "dogalafet asama ileri pulsar") == 1 && komut(y, "dogalafet asama ileri pulsar") == 1
                && donem.gunesEvresi() == GunesEvresi.KARADELIK_YUTMA, "kara delik: " + donem.ozet());
        int once = PulsarSureci.yutmaPatlamasiSayisi();
        boolean[] parlamaGoruldu = {false};
        yonetici.hizAyarla(50);
        y.succeedWhen(() -> {
            if (donem.gunesEvresi() == GunesEvresi.KARADELIK_YUTMA) {
                float sn = PulsarZinciri.yutmaSaniyesi(donem.evreBaslangicSaati(), donem.saat());
                if (PulsarYolu.yutmaPatlamasiOldu(donem)) {
                    y.assertTrue(sn >= PulsarZinciri.YUTMA_PARLAMA_SN, "parlama 55. sn'den önce: " + sn);
                    parlamaGoruldu[0] = true;
                }
                y.fail("yutma sürüyor: " + sn + " sn");
            }
            y.assertTrue(donem.gunesEvresi() == GunesEvresi.GUNESSIZ && PulsarYolu.donusYolu(donem),
                    "90 sn sonra pulsar yolunda güneşsiz dönem: " + donem.ozet());
            y.assertTrue(parlamaGoruldu[0] && PulsarYolu.yutmaPatlamasiOldu(donem), "parlama yutma sürerken gelmeli");
            y.assertTrue(PulsarSureci.yutmaPatlamasiSayisi() - once == 1,
                    "55. sn sarsıntısı bir kez: " + (PulsarSureci.yutmaPatlamasiSayisi() - once));
            y.assertTrue(DonemPaketi.den(donem, 50.0).donusYolu(), "evreyle aynı pakette dönüş yolu bayrağı");
            temizle(y.getLevel().getServer());
        });
    }

    @GameTest(template = "bos", batch = "pulsar2_donus", timeoutTicks = 400)
    public static void buzulKiyamettenSonraEskiGunesDoner(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        MinecraftServer server = y.getLevel().getServer();
        ServerLevel ust = server.overworld();
        kurallar(y, false, 1_000L);
        y.assertTrue(komut(y, "dogalafet pulsar baslat") == 1, "pulsar baslat");
        for (String surec : new String[] {"pulsar", "pulsar", "karadelik"}) {
            y.assertTrue(komut(y, "dogalafet asama ileri " + surec) == 1, "asama ileri " + surec);
        }
        y.assertTrue(donem.gunesEvresi() == GunesEvresi.GUNESSIZ && PulsarYolu.donusYolu(donem), "buzul: " + donem.ozet());
        donem.donmaIlerlemesiAyarla(50f);
        yonetici.hizAyarla(1000);
        int[] adim = {0};
        boolean[] kizilCuce = {false};
        y.succeedWhen(() -> {
            if (adim[0] == 0) {
                y.assertTrue(donem.gunesEvresi() == GunesEvresi.GUNESSIZ, "süre dolmadan buzul sürmeli: " + donem.ozet());
                donem.gunOfsetiAyarla(donem.gunOfseti() + GokyuzuAyarlari.pulsarSonrasiGunessizGun());
                adim[0] = 1;
                y.fail("buzul süresi doldu");
            }
            if (donem.gunesEvresi() == GunesEvresi.KIZIL_CUCE) {
                y.assertTrue(PulsarYolu.donusYolu(donem) && donem.cozulmeDonemi() && !GunesZinciriSureci.al().aktifMi(ust),
                        "pulsar yolunda kızıl cüce doğuşu: " + donem.ozet());
                kizilCuce[0] = true;
                y.fail("doğuş sürüyor");
            }
            y.assertTrue(kizilCuce[0] && donem.gunesEvresi() == GunesEvresi.NORMAL,
                    "gün döngüsü kapalıyken kızıl cüce doğup eski güneşe dönmeli: " + donem.ozet());
            if (adim[0] == 1) {
                y.assertTrue(PulsarYolu.gunesDondu(donem) && donem.donmaIlerlemesi() > 1f,
                        "eski güneş döndü, donma çözülüyor: " + donem.ozet());
                adim[0] = 2;
                y.fail("çözülme bekleniyor");
            }
            y.assertTrue(donem.donmaIlerlemesi() <= 1e-3f && !PulsarYolu.gunesDondu(donem) && !donem.cozulmeDonemi(),
                    "donma 0'a çözülünce bayrak silinmeli: " + donem.ozet());
            temizle(server);
        });
    }

    @GameTest(template = "bos", batch = "pulsar2_sifirla", timeoutTicks = 100)
    public static void sifirlaHerAsamadanNormaleDoner(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        ServerLevel ust = y.getLevel().getServer().overworld();
        kurallar(y, true, 6_000L);
        for (int adim = 0; adim <= 5; adim++) {
            y.assertTrue(komut(y, "dogalafet pulsar baslat") == 1, "pulsar baslat");
            for (int i = 0; i < adim; i++) {
                y.assertTrue(komut(y, "dogalafet asama ileri pulsar") == 1, "asama ileri " + i + "/" + adim);
            }
            String once = donem.gunesEvresi().kimlik() + " ilerleme " + donem.evreIlerlemesi();
            y.assertTrue(komut(y, "dogalafet sifirla") == 1, "sifirla");
            y.assertTrue(donem.gunesEvresi() == GunesEvresi.NORMAL && donem.ekVeri(PulsarSureci.KIMLIK).isEmpty()
                    && !PulsarSureci.al().aktifMi(ust) && !DonemPaketi.den(donem, 1.0).donusYolu()
                    && !DonemPaketi.den(donem, 1.0).gunesDondu(), "sifirla (" + once + "): " + donem.ozet());
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "pulsar2_durum", timeoutTicks = 100)
    public static void durumSatiriKalanGunuYazar(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        ServerLevel ust = y.getLevel().getServer().overworld();
        PulsarSureci surec = PulsarSureci.al();
        kurallar(y, true, 6_000L);
        y.assertTrue(surec.durumSatiri(ust).isEmpty(), "zincir yokken durum satırı yok");
        y.assertTrue(komut(y, "dogalafet pulsar baslat") == 1, "pulsar baslat");
        y.succeedWhen(() -> {
            y.assertTrue(PulsarYolu.baslangicVar(donem), "başlangıç bekleniyor");
            int gun = GokyuzuAyarlari.pulsarGun();
            y.assertTrue(sayiVar(surec.durumSatiri(ust), gun), "başta kalan " + gun + " gün: " + sayilar(surec.durumSatiri(ust)));
            y.assertTrue(komut(y, "dogalafet durum") == 1, "durum");
            donem.gunOfsetiAyarla(donem.gunOfseti() + 5);
            y.assertTrue(sayiVar(surec.durumSatiri(ust), gun - 5), "5 gün sonra: " + sayilar(surec.durumSatiri(ust)));
            y.assertTrue(komut(y, "dogalafet asama ileri pulsar") == 1 && komut(y, "dogalafet asama ileri pulsar") == 1
                    && sayiVar(surec.durumSatiri(ust), PulsarZinciri.YUTMA_SN), "yutmada kalan saniye: "
                    + sayilar(surec.durumSatiri(ust)));
            y.assertTrue(komut(y, "dogalafet asama ileri karadelik") == 1
                    && sayiVar(surec.durumSatiri(ust), GokyuzuAyarlari.pulsarSonrasiGunessizGun()),
                    "buzulda kalan gün: " + sayilar(surec.durumSatiri(ust)));
            temizle(y.getLevel().getServer());
        });
    }

    @GameTest(template = "bos", batch = "pulsar2_kayit", timeoutTicks = 100)
    public static void zincirKayittanAsamaVeBaslangiciKorur(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        kurallar(y, true, 6_000L);
        y.assertTrue(komut(y, "dogalafet pulsar baslat") == 1, "pulsar baslat");
        y.succeedWhen(() -> {
            y.assertTrue(PulsarYolu.baslangicVar(donem), "başlangıç bekleniyor");
            kayitKorur(y, donem, "pulsar");
            y.assertTrue(komut(y, "dogalafet asama ileri pulsar") == 1 && komut(y, "dogalafet asama ileri pulsar") == 1,
                    "kara deliğe");
            PulsarYolu.yutmaPatlamasiAyarla(donem, true);
            kayitKorur(y, donem, "karadelik");
            y.assertTrue(komut(y, "dogalafet asama ileri karadelik") == 1, "buzula");
            kayitKorur(y, donem, "gunessiz");
            y.assertTrue(komut(y, "dogalafet asama ileri gunessiz") == 1, "kızıl cüceye");
            donem.evreIlerlemesiAyarla(0.4f);
            kayitKorur(y, donem, "kizilcuce");
            y.assertTrue(komut(y, "dogalafet asama ileri kizilcuce") == 1, "eski güneşe");
            kayitKorur(y, donem, "normal");
            temizle(y.getLevel().getServer());
        });
    }

    /**
     * K-C46: PULSAR'ın 3. gününde {@code gunesPatlamasiAfeti} kapanır, kapalıyken 2 oyun günü (gün ofseti) ve birkaç tick
     * geçer; durum satırı duran kalanı gösterir. Kural açılınca kalan oyun tick'i durduğu andakiyle aynıdır, yutma
     * başlamaz.
     */
    @GameTest(template = "bos", batch = "pulsar2_kural_gun", timeoutTicks = 100)
    public static void kuralKapaliPulsarKalanGunuKorur(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        ServerLevel ust = y.getLevel().getServer().overworld();
        PulsarSureci surec = PulsarSureci.al();
        kurallar(y, true, 6_000L);
        y.assertTrue(komut(y, "dogalafet pulsar baslat") == 1, "pulsar baslat");
        int[] adim = {0};
        int[] bekle = {0};
        long[] kalan = {0L};
        List<Double> durumDurmada = new ArrayList<>();
        y.succeedWhen(() -> {
            if (adim[0] == 0) {
                y.assertTrue(PulsarYolu.baslangicVar(donem), "başlangıç bekleniyor");
                donem.gunOfsetiAyarla(donem.gunOfseti() + 3);
                gunesKurali(y, false);
                yonetici.kurallariDenetle();
                kalan[0] = pulsarKalan(ust, donem);
                y.assertTrue(yonetici.durdurmaNedeni(surec).equals(Optional.of(DonemSureci.DurdurmaNedeni.KURAL))
                        && PulsarYolu.durmaVar(donem), "kural kapanınca KURAL ile durmalı: " + donem.ozet());
                durumDurmada.addAll(sayilar(surec.durumSatiri(ust)));
                donem.gunOfsetiAyarla(donem.gunOfseti() + 2);
                adim[0] = 1;
                y.fail("kural kapalı");
            }
            if (adim[0] == 1) {
                y.assertTrue(sayilar(surec.durumSatiri(ust)).equals(durumDurmada),
                        "duran sürecin kalanı azalmamalı: " + durumDurmada + " / " + sayilar(surec.durumSatiri(ust)));
                if (++bekle[0] < 10) {
                    y.fail("kapalıyken zaman geçiyor");
                }
                gunesKurali(y, true);
                yonetici.kurallariDenetle();
                y.assertTrue(!yonetici.surecDurdu(surec) && !PulsarYolu.durmaVar(donem), "kural açılınca sürmeli");
                y.assertTrue(pulsarKalan(ust, donem) == kalan[0], "kalan oyun tick'i değişmemeli: " + kalan[0] + " → "
                        + pulsarKalan(ust, donem));
                adim[0] = 2;
                y.fail("sürüyor");
            }
            y.assertTrue(donem.gunesEvresi() == GunesEvresi.PULSAR, "kural açılınca yutma başlamamalı: " + donem.ozet());
            y.assertTrue(pulsarKalan(ust, donem) < kalan[0] && pulsarKalan(ust, donem) > kalan[0] - 100,
                    "süre açılıştan sonra yeniden azalır: " + pulsarKalan(ust, donem));
            temizle(y.getLevel().getServer());
        });
    }

    /**
     * K-C46: yutmanın ~30. sn'sinde kural kapanır, kapalıyken ölçekli saat 90 sn'den fazla ilerler; açılınca saniye
     * kaldığı yerden sürer, 55. sn parlaması yutma sürerken bir kez gelir, sonra güneşsiz dönem.
     */
    @GameTest(template = "bos", batch = "pulsar2_kural_yutma", timeoutTicks = 300)
    public static void kuralKapaliYutmaKaldigiYerdenSurer(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        PulsarSureci surec = PulsarSureci.al();
        kurallar(y, true, 6_000L);
        y.assertTrue(komut(y, "dogalafet pulsar baslat") == 1, "pulsar baslat");
        y.assertTrue(komut(y, "dogalafet asama ileri pulsar") == 1 && komut(y, "dogalafet asama ileri pulsar") == 1
                && donem.gunesEvresi() == GunesEvresi.KARADELIK_YUTMA, "kara delik: " + donem.ozet());
        int once = PulsarSureci.yutmaPatlamasiSayisi();
        int[] adim = {0};
        float[] snDurma = {0f};
        boolean[] parlamaGoruldu = {false};
        yonetici.hizAyarla(50);
        y.succeedWhen(() -> {
            float sn = PulsarZinciri.yutmaSaniyesi(donem.evreBaslangicSaati(), donem.saat());
            if (adim[0] == 0) {
                if (sn < 30f) {
                    y.fail("30. sn bekleniyor: " + sn);
                }
                gunesKurali(y, false);
                yonetici.kurallariDenetle();
                y.assertTrue(yonetici.surecDurdu(surec) && PulsarYolu.durmaVar(donem), "yutma kuralla durmalı");
                snDurma[0] = PulsarZinciri.yutmaSaniyesi(donem.evreBaslangicSaati(), PulsarYolu.durmaSaati(donem));
                y.assertTrue(snDurma[0] < PulsarZinciri.YUTMA_PARLAMA_SN && !PulsarYolu.yutmaPatlamasiOldu(donem),
                        "parlamadan önce durmalı: " + snDurma[0]);
                yonetici.hizAyarla(1000);
                adim[0] = 1;
                y.fail("kural kapalı");
            }
            if (adim[0] == 1) {
                y.assertTrue(donem.gunesEvresi() == GunesEvresi.KARADELIK_YUTMA && !PulsarYolu.yutmaPatlamasiOldu(donem),
                        "kapalıyken yutma ilerlememeli: " + donem.ozet());
                if (donem.saat() - PulsarYolu.durmaSaati(donem) < 2 * PulsarZinciri.YUTMA_SN * PulsarZinciri.SANIYE_TICK) {
                    y.fail("kapalıyken saat ilerliyor");
                }
                yonetici.hizAyarla(50);
                gunesKurali(y, true);
                yonetici.kurallariDenetle();
                float simdi = PulsarZinciri.yutmaSaniyesi(donem.evreBaslangicSaati(), donem.saat());
                y.assertTrue(!yonetici.surecDurdu(surec) && Math.abs(simdi - snDurma[0]) < 1e-3f,
                        "saniye kaldığı yerden: " + snDurma[0] + " → " + simdi);
                adim[0] = 2;
                y.fail("yutma sürüyor");
            }
            if (donem.gunesEvresi() == GunesEvresi.KARADELIK_YUTMA) {
                if (PulsarYolu.yutmaPatlamasiOldu(donem)) {
                    y.assertTrue(sn >= PulsarZinciri.YUTMA_PARLAMA_SN, "parlama 55. sn'den önce: " + sn);
                    parlamaGoruldu[0] = true;
                }
                y.fail("yutma sürüyor: " + sn + " sn");
            }
            y.assertTrue(donem.gunesEvresi() == GunesEvresi.GUNESSIZ && PulsarYolu.donusYolu(donem),
                    "yutma bitince güneşsiz dönem: " + donem.ozet());
            y.assertTrue(parlamaGoruldu[0] && PulsarSureci.yutmaPatlamasiSayisi() - once == 1,
                    "55. sn parlaması yutma sürerken bir kez: " + (PulsarSureci.yutmaPatlamasiSayisi() - once));
            temizle(y.getLevel().getServer());
        });
    }

    /**
     * Madde 4 (şef kararı): eski güneş pulsar yolundan dönünce seçim turunun tek seferlik süzgeci güneş patlamasını
     * varsayılanla geçirmez (dönüş bayrağı kalksa ve kayıttan dönülse de), {@code tekrarlanabilir} açıkken
     * geçirir; pulsar yolundaki doğuşta tekrar ayarıyla da geçirmez. {@code sifirla} kaydı siler.
     */
    @GameTest(template = "bos", batch = "pulsar2_tek_seferlik", timeoutTicks = 200)
    public static void eskiGunesDonuncePatlamaYenidenSecilmez(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        kurallar(y, true, 6_000L);
        TekSeferlikAyarlari varsayilan = TekSeferlikAyarlari.VARSAYILAN;
        TekSeferlikAyarlari tekrar = varsayilan.gunesTekrarIle(true);
        y.assertTrue(yonetici.tekSeferlikUygun(AfetTuru.GUNES_PATLAMASI, 1000, 1000, varsayilan),
                "zincirden önce seçilebilir");
        y.assertTrue(komut(y, "dogalafet pulsar baslat") == 1, "pulsar baslat");
        for (String surec : new String[] {"pulsar", "pulsar", "karadelik", "gunessiz"}) {
            y.assertTrue(komut(y, "dogalafet asama ileri " + surec) == 1, "asama ileri " + surec);
        }
        y.assertTrue(donem.gunesEvresi() == GunesEvresi.KIZIL_CUCE && PulsarYolu.donusYolu(donem), "doğuş: " + donem.ozet());
        y.assertTrue(!yonetici.tekSeferlikUygun(AfetTuru.GUNES_PATLAMASI, 1000, 1000, tekrar),
                "pulsar yolundaki doğuşta tekrar ayarıyla da seçilmez");
        y.assertTrue(komut(y, "dogalafet asama ileri kizilcuce") == 1 && donem.gunesEvresi() == GunesEvresi.NORMAL
                && PulsarYolu.zincirTukendi(donem), "eski güneş: " + donem.ozet());
        y.assertTrue(!yonetici.tekSeferlikUygun(AfetTuru.GUNES_PATLAMASI, 1000, 1000, varsayilan),
                "dönüşten sonra varsayılanla seçilmez");
        y.assertTrue(yonetici.tekSeferlikUygun(AfetTuru.GUNES_PATLAMASI, 1000, 1000, tekrar),
                "tekrarlanabilir açıkken eskisi gibi seçilir");
        y.succeedWhen(() -> {
            y.assertTrue(!PulsarYolu.gunesDondu(donem), "dönüş bayrağının kalkması bekleniyor: " + donem.ozet());
            y.assertTrue(!yonetici.tekSeferlikUygun(AfetTuru.GUNES_PATLAMASI, 1000, 1000, varsayilan)
                    && PulsarYolu.zincirTukendi(DunyaDonemi.yukle(donem.save(new CompoundTag()))),
                    "çözülme bitince ve kayıttan dönünce de tükendi");
            yonetici.sifirla();
            y.assertTrue(!PulsarYolu.zincirTukendi(donem)
                    && yonetici.tekSeferlikUygun(AfetTuru.GUNES_PATLAMASI, 1000, 1000, varsayilan), "sifirla kaydı siler");
            temizle(y.getLevel().getServer());
        });
    }

    /** Dönem NBT gidiş-dönüşünde (kayıt/yükleme) evre, süre başlangıçları ve pulsar yolu bayrakları korunur. */
    private static void kayitKorur(GameTestHelper y, DunyaDonemi donem, String asama) {
        DunyaDonemi geri = DunyaDonemi.yukle(donem.save(new CompoundTag()));
        y.assertTrue(geri.gunesEvresi() == donem.gunesEvresi() && geri.evreIlerlemesi() == donem.evreIlerlemesi()
                && geri.evreBaslangicSaati() == donem.evreBaslangicSaati() && geri.saat() == donem.saat()
                && PulsarYolu.dongu(geri) == PulsarYolu.dongu(donem)
                && PulsarYolu.baslangic(geri) == PulsarYolu.baslangic(donem)
                && PulsarYolu.gunessizBaslangicVar(geri) == PulsarYolu.gunessizBaslangicVar(donem)
                && PulsarYolu.gunessizBaslangic(geri) == PulsarYolu.gunessizBaslangic(donem)
                && PulsarYolu.yutmaPatlamasiOldu(geri) == PulsarYolu.yutmaPatlamasiOldu(donem)
                && PulsarYolu.donusYolu(geri) == PulsarYolu.donusYolu(donem)
                && PulsarYolu.gunesDondu(geri) == PulsarYolu.gunesDondu(donem)
                && PulsarYolu.zincirTukendi(geri) == PulsarYolu.zincirTukendi(donem),
                "kayıt gidiş-dönüşü (" + asama + "): " + donem.ozet() + " / " + geri.ozet());
    }

    /** Satırın iç içe çeviri argümanlarında {@code beklenen}'e 0,01 yakın bir sayı var mı. */
    private static boolean sayiVar(Optional<Component> satir, double beklenen) {
        return sayilar(satir).stream().anyMatch(sayi -> Math.abs(sayi - beklenen) < 0.01);
    }

    private static List<Double> sayilar(Optional<Component> satir) {
        List<Double> sonuc = new ArrayList<>();
        satir.ifPresent(metin -> sayilariTopla(metin, sonuc));
        return sonuc;
    }

    private static void sayilariTopla(Object arguman, List<Double> sonuc) {
        if (arguman instanceof Component metin) {
            if (metin.getContents() instanceof TranslatableContents ceviri) {
                for (Object ic : ceviri.getArgs()) {
                    sayilariTopla(ic, sonuc);
                }
            } else {
                sayilariTopla(metin.getString(), sonuc);
            }
            for (Component kardes : metin.getSiblings()) {
                sayilariTopla(kardes, sonuc);
            }
        } else if (arguman != null) {
            try {
                sonuc.add(Double.parseDouble(String.valueOf(arguman)));
            } catch (NumberFormatException sayiDegil) {
                // metin argümanı (evre adı vb.)
            }
        }
    }
}
