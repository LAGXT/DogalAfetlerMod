package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GunesZinciri;
import io.github.kadirberkpolat1.dogalafetler.komut.DogalAfetKomutu;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.network.ICustomPacket;
import net.minecraftforge.network.NetworkDirection;

/**
 * Güneş zinciri sürecinin oyun içi testleri (madde 16, bölüm D, GOREV §6 #8 ve #12). Her test dünya dönemini değiştirdiği
 * için kendi {@code batch}'indedir, başta ve sonda dönemi sıfırlar. Süreç gerçek sunucu tick'inde koşar;
 * {@code /dogalafet hiz 1000} 1,5 günlük süpernovayı 36 tick'e, 7 günlük gecikmeyi 168 tick'e indirir.
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GokyuzuOyunTestleri {
    private static final GunesZinciriSureci SUREC = GunesZinciriSureci.al();

    private GokyuzuOyunTestleri() {
    }

    private static int komut(GameTestHelper yardimci, String komut) {
        MinecraftServer server = yardimci.getLevel().getServer();
        try {
            return server.getCommands().getDispatcher().execute(komut, server.createCommandSourceStack()
                    .withSuppressedOutput().withPermission(DogalAfetKomutu.YETKI_SEVIYESI));
        } catch (CommandSyntaxException hata) {
            yardimci.fail("/" + komut + " çalışmadı: " + hata.getMessage());
            return -1;
        }
    }

    private static AfetYoneticisi temizBasla(GameTestHelper yardimci) {
        AfetYoneticisi yonetici = AfetYoneticisi.al(yardimci.getLevel().getServer());
        yonetici.sifirla();
        return yonetici;
    }

    @GameTest(template = "bos", batch = "gokyuzu_supernova", timeoutTicks = 300)
    public static void supernovaSuresiDolunucaGunessizeGecer(GameTestHelper yardimci) {
        AfetYoneticisi yonetici = temizBasla(yardimci);
        DunyaDonemi donem = yonetici.donem();
        donem.gunesEvresiAyarla(GunesEvresi.SUPERNOVA);
        long dongu = donem.evreBaslangicSaati();
        yonetici.hizAyarla(1000);
        yardimci.succeedWhen(() -> {
            yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.GUNESSIZ,
                    "süpernova süresi dolunca GÜNEŞSİZ bekleniyordu: " + donem.ozet());
            yardimci.assertTrue(donem.ekVeri(GunesZinciriSureci.KIMLIK).getLong(GunesZinciriSureci.DONGU) == dongu,
                    "döngü kimliği süpernova başlangıcı olmalı");
            yardimci.assertTrue(SUREC.aktifMi(yardimci.getLevel().getServer().overworld()), "güneşsiz dönemde süreç sürmeli");
            yonetici.sifirla();
        });
    }

    @GameTest(template = "bos", batch = "gokyuzu_asama")
    public static void asamaIleriZincirEvreleriniSiraylaGecirir(GameTestHelper yardimci) {
        AfetYoneticisi yonetici = temizBasla(yardimci);
        DunyaDonemi donem = yonetici.donem();
        ServerLevel ust = yardimci.getLevel().getServer().overworld();
        try {
            donem.gunesEvresiAyarla(GunesEvresi.SUPERNOVA);
            yardimci.assertTrue(komut(yardimci, "dogalafet asama ileri") == 1, "asama ileri (süpernova)");
            yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.GUNESSIZ, "SÜPERNOVA → GÜNEŞSİZ");
            yardimci.assertTrue(komut(yardimci, "dogalafet asama ileri") == 1, "asama ileri (güneşsiz)");
            yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.KIZIL_CUCE && donem.evreIlerlemesi() == 0f,
                    "GÜNEŞSİZ → KIZIL CÜCE doğuşu");
            yardimci.assertTrue(SUREC.aktifMi(ust), "doğuş sürerken süreç aktif");
            yardimci.assertTrue(komut(yardimci, "dogalafet asama ileri") == 1, "asama ileri (doğuş)");
            yardimci.assertTrue(donem.evreIlerlemesi() == 1f && !SUREC.aktifMi(ust), "doğuş tamam, süreç bitti");
            yardimci.assertTrue(komut(yardimci, "dogalafet asama ileri") == 0, "tamamlanmış zincirde asama ileri 0");
            yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.KIZIL_CUCE, "kızıl cüce kalıcı");
        } finally {
            yonetici.sifirla();
        }
        yardimci.succeed();
    }

    @GameTest(template = "bos", batch = "gokyuzu_kizilcuce", timeoutTicks = 600)
    public static void donmaTamVeGecikmeSonrasiIlkSafaktaKizilCuceDogar(GameTestHelper yardimci) {
        AfetYoneticisi yonetici = temizBasla(yardimci);
        DunyaDonemi donem = yonetici.donem();
        MinecraftServer server = yardimci.getLevel().getServer();
        ServerLevel ust = server.overworld();
        GameRules.BooleanValue gunDongusu = server.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        boolean oncekiDongu = gunDongusu.get();
        long oncekiSaat = ust.getDayTime();
        Runnable temizle = () -> {
            gunDongusu.set(oncekiDongu, server);
            ust.setDayTime(oncekiSaat);
            yonetici.sifirla();
        };
        gunDongusu.set(true, server);
        ust.setDayTime(1_000);
        donem.gunesEvresiAyarla(GunesEvresi.GUNESSIZ);
        donem.donmaIlerlemesiAyarla(100f);
        yonetici.hizAyarla(1000);
        boolean[] safakVerildi = {false};
        yardimci.succeedWhen(() -> {
            if (!safakVerildi[0]) {
                yardimci.assertTrue(GunesZinciri.kapiAcik(GunesZinciriSureci.kapiSayaci(donem), GokyuzuAyarlari.zincir()),
                        "donma %100 + 7 gün bekleniyor: " + GunesZinciriSureci.kapiSayaci(donem));
                if (donem.gunesEvresi() != GunesEvresi.GUNESSIZ) {
                    temizle.run();
                    yardimci.fail("kızıl cüce şafaktan önce doğdu (gün saati " + ust.getDayTime() + ")");
                }
                safakVerildi[0] = true;
                ust.setDayTime(Math.floorDiv(ust.getDayTime(), 24_000L) * 24_000L + 24_000L);
                yardimci.fail("kapı açık; uykudaki gibi sabaha atlandı, şafak geçildi");
            }
            // Doğal doğuş kızıl cüce ya da (pulsarOlasiligi ile) pulsar; seçim dünya tohumu ve döngüden belirlenir.
            GunesEvresi beklenen = GunesZinciriSureci.dogacakYildiz(ust, donem);
            yardimci.assertTrue(donem.gunesEvresi() == beklenen,
                    "uyku şafağı atlayınca da yeni yıldız (" + beklenen.kimlik() + ") doğmalı: " + donem.ozet());
            temizle.run();
        });
    }

    @GameTest(template = "bos", batch = "gokyuzu_stop", timeoutTicks = 200)
    public static void stopSureciDurdururKayitVeOkumaEvreyiKorur(GameTestHelper yardimci) {
        AfetYoneticisi yonetici = temizBasla(yardimci);
        DunyaDonemi donem = yonetici.donem();
        ServerLevel ust = yardimci.getLevel().getServer().overworld();
        donem.gunesEvresiAyarla(GunesEvresi.SUPERNOVA);
        donem.evreIlerlemesiAyarla(0.3f);
        yonetici.hizAyarla(1000);
        float[] durdugu = {-1f};
        long[] durduguSaat = {0L};
        yardimci.succeedWhen(() -> {
            if (durdugu[0] < 0f) {
                yardimci.assertTrue(donem.ekVeri(GunesZinciriSureci.KIMLIK).contains(GunesZinciriSureci.DONGU),
                        "süreç henüz tick almadı");
                yardimci.assertTrue(komut(yardimci, "dogalafet stop") == 1, "afet yokken stop süreci durdurmalı");
                yardimci.assertTrue(yonetici.durdurmaNedeni(SUREC).equals(Optional.of(DonemSureci.DurdurmaNedeni.KOMUT)),
                        "durdurma nedeni KOMUT");
                durdugu[0] = donem.evreIlerlemesi();
                durduguSaat[0] = donem.saat();
                yardimci.fail("durduktan sonra birkaç tick bekleniyor");
            }
            yardimci.assertTrue(donem.saat() - durduguSaat[0] >= 10_000, "en az 10 tick (ölçekli 10000) beklenmeli");
            yardimci.assertTrue(donem.evreIlerlemesi() == durdugu[0], "durmuş süreç ilerlememeli");
            yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.SUPERNOVA, "ulaşılmış süpernova geri alınmamalı");

            DunyaDonemi geri = DunyaDonemi.yukle(donem.save(new CompoundTag()));
            yardimci.assertTrue(geri.gunesEvresi() == GunesEvresi.SUPERNOVA && geri.evreIlerlemesi() == durdugu[0]
                    && geri.evreBaslangicSaati() == donem.evreBaslangicSaati(), "evre kayıt/okumada korunmalı (§6 #8)");
            yardimci.assertTrue(geri.surecDurdurmasi(GunesZinciriSureci.KIMLIK)
                    .equals(Optional.of(DonemSureci.DurdurmaNedeni.KOMUT)), "durdurma kaydı kalıcı olmalı");
            yardimci.assertTrue(geri.ekVeri(GunesZinciriSureci.KIMLIK).getLong(GunesZinciriSureci.DONGU)
                    == donem.evreBaslangicSaati(), "döngü kimliği kalıcı olmalı");

            yardimci.assertTrue(komut(yardimci, "dogalafet sifirla") == 1, "sifirla");
            yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.NORMAL && !yonetici.surecDurdu(SUREC)
                    && !SUREC.aktifMi(ust), "sifirla sonrası NORMAL ve kayıtsız");
            yonetici.sifirla();
        });
    }

    @GameTest(template = "bos", batch = "gokyuzu_giris")
    public static void sonradanGirenOyuncuGokyuzuDurumunuAlir(GameTestHelper yardimci) {
        AfetYoneticisi yonetici = temizBasla(yardimci);
        DunyaDonemi donem = yonetici.donem();
        MinecraftServer server = yardimci.getLevel().getServer();
        donem.ayVarAyarla(false);
        donem.gunesEvresiAyarla(GunesEvresi.GUNESSIZ);
        donem.evreIlerlemesiAyarla(0.25f);
        ICustomPacket<?> ornek = (ICustomPacket<?>) AgKanali.KANAL.toVanillaPacket(DonemPaketi.den(donem, 1.0),
                NetworkDirection.PLAY_TO_CLIENT);
        byte kimlik = ByteBufUtil.getBytes(ornek.getInternalData())[0];
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(yardimci, 0.5, 2, 0.5);
        try {
            EmbeddedChannel kanal = (EmbeddedChannel) oyuncu.connection.connection.channel();
            DonemPaketi son = null;
            for (Object mesaj = kanal.readOutbound(); mesaj != null; mesaj = kanal.readOutbound()) {
                if (mesaj instanceof ICustomPacket<?> ozel && ozel.getName().equals(ornek.getName())) {
                    byte[] bayt = ByteBufUtil.getBytes(ozel.getInternalData());
                    if (bayt.length > 1 && bayt[0] == kimlik) {
                        son = DonemPaketi.oku(new FriendlyByteBuf(Unpooled.wrappedBuffer(bayt, 1, bayt.length - 1)));
                    }
                }
            }
            yardimci.assertTrue(son != null, "girişte dönem paketi gitmedi");
            yardimci.assertTrue(!son.ayVar() && son.gunesEvresi() == GunesEvresi.GUNESSIZ && son.evreIlerlemesi() == 0.25f
                    && son.evreBaslangicSaati() == donem.evreBaslangicSaati(), "sonradan giren yanlış gökyüzü aldı: " + son);
        } finally {
            server.getPlayerList().remove(oyuncu);
            yonetici.sifirla();
        }
        yardimci.succeed();
    }

    @GameTest(template = "bos", batch = "gokyuzu_komut")
    public static void kizilCuceKomutlariCalisir(GameTestHelper yardimci) {
        AfetYoneticisi yonetici = temizBasla(yardimci);
        DunyaDonemi donem = yonetici.donem();
        ServerLevel ust = yardimci.getLevel().getServer().overworld();
        try {
            for (GunesEvresi evre : new GunesEvresi[] {GunesEvresi.NORMAL, GunesEvresi.SUPERNOVA, GunesEvresi.GUNESSIZ}) {
                donem.gunesEvresiAyarla(evre);
                yardimci.assertTrue(komut(yardimci, "dogalafet kizilcuce bilgi") == 1, "bilgi (" + evre.kimlik() + ")");
            }
            donem.donmaIlerlemesiAyarla(100f);
            yardimci.assertTrue(komut(yardimci, "dogalafet kizilcuce bilgi") == 1, "bilgi (donma 100)");
            yardimci.assertTrue(komut(yardimci, "dogalafet kizilcuce baslat") == 1, "baslat");
            yardimci.assertTrue(donem.gunesEvresi() == GunesEvresi.KIZIL_CUCE && donem.evreIlerlemesi() == 0f
                    && SUREC.aktifMi(ust), "baslat doğuşu hemen başlatmalı");
            yardimci.assertTrue(komut(yardimci, "dogalafet kizilcuce baslat") == 0, "ikinci baslat reddedilmeli");
            yardimci.assertTrue(komut(yardimci, "dogalafet kizilcuce bilgi") == 1, "bilgi (doğuş)");
        } finally {
            yonetici.sifirla();
        }
        yardimci.succeed();
    }
}
