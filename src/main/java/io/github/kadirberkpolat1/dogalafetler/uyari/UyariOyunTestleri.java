package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.OyunTestiAfeti;
import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.OrtamDurdurPaketi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.OrtamPaketi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.SirenDurdurPaketi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.SirenPaketi;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.network.ICustomPacket;
import net.minecraftforge.network.NetworkDirection;

/**
 * Uyarı alt sisteminin oyun içi testleri: çekirdeğin siren olayları sahte oyuncunun istemcisine {@code uyari/siren} ve
 * {@code uyari/siren_durdur} olarak gider; siren sırasında gelen kalan süreyle alır; alan dışındaki almaz (K-C30); ortam
 * katmanı gider ve afet bitince söner. Paketler sahte oyuncunun gömülü kanalından okunur (çekirdek §13). Tek afet
 * yuvasını kullandıkları için her biri kendi {@code batch}'indedir.
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UyariOyunTestleri {
    private UyariOyunTestleri() {
    }

    /** Sahte oyuncunun kanalına giden mod paketleri (ham, eskiden yeniye); kanalın giden kuyruğu boşalır. */
    private static List<byte[]> gelenler(ServerPlayer oyuncu) {
        EmbeddedChannel kanal = (EmbeddedChannel) oyuncu.connection.connection.channel();
        String ad = ozel(new SirenDurdurPaketi(0L, 1)).getName().toString();
        List<byte[]> sonuc = new ArrayList<>();
        for (Object mesaj = kanal.readOutbound(); mesaj != null; mesaj = kanal.readOutbound()) {
            if (mesaj instanceof ICustomPacket<?> paket && paket.getName().toString().equals(ad)) {
                sonuc.add(ByteBufUtil.getBytes(paket.getInternalData()));
            }
        }
        return sonuc;
    }

    private static ICustomPacket<?> ozel(Object mesaj) {
        return (ICustomPacket<?>) AgKanali.KANAL.toVanillaPacket(mesaj, NetworkDirection.PLAY_TO_CLIENT);
    }

    /** Ham paketlerden {@code ornek}'in türünde olanları çözer (ilk bayt paket kimliği). */
    private static <M> List<M> coz(List<byte[]> ham, Object ornek, Function<FriendlyByteBuf, M> okuyucu) {
        byte kimlik = ByteBufUtil.getBytes(ozel(ornek).getInternalData())[0];
        List<M> sonuc = new ArrayList<>();
        for (byte[] bayt : ham) {
            if (bayt.length > 0 && bayt[0] == kimlik) {
                sonuc.add(okuyucu.apply(new FriendlyByteBuf(Unpooled.wrappedBuffer(Arrays.copyOfRange(bayt, 1,
                        bayt.length)))));
            }
        }
        return sonuc;
    }

    private static List<SirenPaketi> sirenler(List<byte[]> ham) {
        return coz(ham, new SirenPaketi(SirenTuru.STANDART, AfetTuru.DEPREM, 0L, 1, 1), SirenPaketi::oku);
    }

    private static List<SirenDurdurPaketi> durdurmalar(List<byte[]> ham) {
        return coz(ham, new SirenDurdurPaketi(0L, 1), SirenDurdurPaketi::oku);
    }

    /** Ortak sahne: yuva boş, test afeti DEPREM için geçici kayıtlı, oyuncu alanın ortasında. */
    private record Sahne(GameTestHelper yardimci, MinecraftServer server, AfetYoneticisi yonetici, BlockPos merkez,
            AfetKaydi.GeciciKayit kayit, List<ServerPlayer> oyuncular) {
        static Sahne kur(GameTestHelper yardimci) {
            ServerLevel level = yardimci.getLevel();
            AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
            yonetici.durdur();
            AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(AfetTuru.DEPREM,
                    () -> new OyunTestiAfeti(AfetTuru.DEPREM, 16, 100));
            return new Sahne(yardimci, level.getServer(), yonetici, yardimci.absolutePos(BlockPos.ZERO), kayit,
                    new ArrayList<>());
        }

        ServerPlayer oyuncu(double dx, double dz) {
            ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(yardimci, merkez.getX() + 0.5 + dx, merkez.getY(),
                    merkez.getZ() + 0.5 + dz);
            oyuncular.add(oyuncu);
            return oyuncu;
        }

        Afet baslat(ServerPlayer hedef) {
            yardimci.assertTrue(yonetici.elleBaslat(AfetTuru.DEPREM, 1, hedef, yardimci.getLevel(), merkez)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "afet başlamadı");
            return yonetici.aktifAfet().orElseThrow();
        }

        void temizle() {
            yonetici.durdur();
            kayit.close();
            oyuncular.forEach(o -> server.getPlayerList().remove(o));
        }

        /** Hata olursa sahneyi temizleyip yeniden atar (sonraki batch bozulmasın). */
        void koru(Runnable is) {
            try {
                is.run();
            } catch (RuntimeException hata) {
                temizle();
                throw hata;
            }
        }
    }

    @GameTest(template = "bos", batch = "uyari_siren", timeoutTicks = 100)
    public static void sirenAsamasiPaketeAktifDurdurmayaDonusur(GameTestHelper yardimci) {
        Sahne sahne = Sahne.kur(yardimci);
        sahne.koru(() -> {
            ServerPlayer oyuncu = sahne.oyuncu(0, 0);
            Afet afet = sahne.baslat(oyuncu);
            long afetNo = afet.baglam().baslangicSaati();
            gelenler(oyuncu);
            yardimci.assertTrue(sahne.yonetici().asamaIleri() && afet.asama() == AfetAsamasi.SIREN, "SIREN'e geçmedi");
            List<SirenPaketi> siren = sirenler(gelenler(oyuncu));
            yardimci.assertTrue(siren.size() == 1, "SIREN başlarken tek siren paketi beklendi: " + siren.size());
            SirenPaketi paket = siren.get(0);
            yardimci.assertTrue(paket.tur() == SirenTuru.STANDART && paket.afet() == AfetTuru.DEPREM
                    && paket.afetNo() == afetNo && paket.toplamTick() == SirenTuru.STANDART_SURE_TICK
                    && paket.kalanTick() == SirenTuru.STANDART_SURE_TICK, "siren paketi içeriği: " + paket);
            yardimci.assertTrue(sahne.yonetici().asamaIleri() && afet.asama() == AfetAsamasi.AKTIF, "AKTIF'e geçmedi");
            List<SirenDurdurPaketi> durdur = durdurmalar(gelenler(oyuncu));
            yardimci.assertTrue(durdur.size() == 1 && durdur.get(0).afetNo() == afetNo
                    && durdur.get(0).fadeTick() == UyariAyarlari.fadeOutTick(SirenTuru.STANDART),
                    "AKTIF'e geçişte fade-out'lu tek durdurma paketi beklendi: " + durdur);
        });
        sahne.temizle();
        yardimci.succeed();
    }

    @GameTest(template = "bos", batch = "uyari_gec_gelen", timeoutTicks = 200)
    public static void sirenSirasindaGelenOyuncuKalanSureyleAlir(GameTestHelper yardimci) {
        Sahne sahne = Sahne.kur(yardimci);
        ServerPlayer[] ikinci = new ServerPlayer[1];
        sahne.koru(() -> {
            Afet afet = sahne.baslat(sahne.oyuncu(0, 0));
            sahne.yonetici().asamaIleri();
            yardimci.assertTrue(afet.asama() == AfetAsamasi.SIREN, "SIREN'e geçmedi");
        });
        List<SirenPaketi> alinan = new ArrayList<>();
        yardimci.runAfterDelay(10, () -> sahne.koru(() -> ikinci[0] = sahne.oyuncu(3, 3)));
        yardimci.succeedWhen(() -> {
            yardimci.assertTrue(ikinci[0] != null, "ikinci oyuncu henüz girmedi");
            alinan.addAll(sirenler(gelenler(ikinci[0])));
            yardimci.assertTrue(!alinan.isEmpty(), "sonradan gelene siren henüz gitmedi");
            SirenPaketi paket = alinan.get(0);
            yardimci.assertTrue(paket.kalanTick() < paket.toplamTick() && paket.kalanTick() > 0
                    && paket.toplamTick() == SirenTuru.STANDART_SURE_TICK, "kalan süre < toplam olmalı: " + paket);
            sahne.temizle();
        });
    }

    @GameTest(template = "bos", batch = "uyari_alan", timeoutTicks = 100)
    public static void alanDisindakiOyuncuyaSirenGitmez(GameTestHelper yardimci) {
        Sahne sahne = Sahne.kur(yardimci);
        ServerPlayer[] ikili = new ServerPlayer[2];
        sahne.koru(() -> {
            ikili[0] = sahne.oyuncu(0, 0);
            // Etki alanı 16 + sirenEkYaricap 64 = 80 blok; 200 blok ötesi alan dışı.
            ikili[1] = sahne.oyuncu(200, 0);
            sahne.baslat(ikili[0]);
            sahne.yonetici().asamaIleri();
        });
        yardimci.runAfterDelay(30, () -> sahne.koru(() -> {
            yardimci.assertTrue(sirenler(gelenler(ikili[0])).size() == 1, "alandaki oyuncu sireni almalı");
            yardimci.assertTrue(sirenler(gelenler(ikili[1])).isEmpty(), "alan dışındaki oyuncuya siren gitmemeli");
            sahne.temizle();
            yardimci.succeed();
        }));
    }

    @GameTest(template = "bos", batch = "uyari_ortam", timeoutTicks = 100)
    public static void ortamKatmaniGiderGecGeleneSurerAfetBitinceSoner(GameTestHelper yardimci) {
        Sahne sahne = Sahne.kur(yardimci);
        String kimlik = "uyari/ortam_test";
        ServerPlayer[] ikili = new ServerPlayer[2];
        sahne.koru(() -> {
            ikili[0] = sahne.oyuncu(0, 0);
            long afetNo = sahne.baslat(ikili[0]).baglam().baslangicSaati();
            gelenler(ikili[0]);
            OrtamSesi.baslat(sahne.server(), kimlik, afetNo, UyariSesleri.ORTAM_TEST, SoundSource.AMBIENT,
                    OrtamSesi.Yer.nokta(yardimci.getLevel().dimension(), Vec3.atCenterOf(sahne.merkez()), 32f),
                    HacimEgrisi.giris(20, 1f));
            List<OrtamPaketi> ortam = coz(gelenler(ikili[0]), ornekOrtam(), OrtamPaketi::oku);
            yardimci.assertTrue(ortam.size() == 1 && ortam.get(0).kimlik().equals(kimlik)
                    && ortam.get(0).afetNo() == afetNo && ortam.get(0).menzil() == 32f
                    && ortam.get(0).egri().equals(HacimEgrisi.giris(20, 1f)), "ortam paketi: " + ortam);
        });
        yardimci.runAfterDelay(10, () -> sahne.koru(() -> {
            ikili[1] = sahne.oyuncu(2, 2);
            List<OrtamPaketi> gec = coz(gelenler(ikili[1]), ornekOrtam(), OrtamPaketi::oku);
            yardimci.assertTrue(gec.size() == 1 && gec.get(0).gecenTick() >= 9, "sonradan girene geçen süreyle: " + gec);
            gelenler(ikili[0]);
            sahne.yonetici().durdur();
            List<OrtamDurdurPaketi> durdur = coz(gelenler(ikili[0]), new OrtamDurdurPaketi(kimlik, 1),
                    OrtamDurdurPaketi::oku);
            yardimci.assertTrue(durdur.size() == 1 && durdur.get(0).kimlik().equals(kimlik)
                    && durdur.get(0).fadeTick() == OrtamSesi.VARSAYILAN_FADE_TICK, "afet bitince katman sönmeli: " + durdur);
            yardimci.assertFalse(OrtamSesi.aktifMi(kimlik), "katman sunucuda kalmamalı");
            sahne.temizle();
            yardimci.succeed();
        }));
    }

    private static OrtamPaketi ornekOrtam() {
        return new OrtamPaketi("uyari/ornek", 0L, UyariSesleri.ORTAM_TEST, SoundSource.AMBIENT, null, 0, 0, 0, 0f,
                HacimEgrisi.sabit(1f), 0);
    }
}
