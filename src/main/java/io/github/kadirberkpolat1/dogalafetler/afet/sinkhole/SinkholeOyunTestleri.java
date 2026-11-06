package io.github.kadirberkpolat1.dogalafetler.afet.sinkhole;

import static io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremOyunTestleri.SAHNE;
import static io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremOyunTestleri.aktifBaslat;
import static io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremOyunTestleri.batchSonu;
import static io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremOyunTestleri.blokHasari;
import static io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremOyunTestleri.dinle;
import static io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremOyunTestleri.dusenSayisi;
import static io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremOyunTestleri.kutuHash;
import static io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremOyunTestleri.oyuncu;
import static io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremOyunTestleri.sahneyiTemizle;
import static io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremOyunTestleri.zemin;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.CukurPlani;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Sinkhole'un oyun içi testleri (GOREV §6 #1–#4, #3, #16 #4 "Delikten Dönüş" ipucu). Sahne depremin {@code deprem_sahne}
 * kutusudur; yüzey y 11, merkez kutunun ortasına sabitlenir. Her test kendi batch'indedir; batch sonu temizliği depremle
 * ortak ({@code DepremOyunTestleri.batchSonu}, K-E15).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SinkholeOyunTestleri {
    private static final int YUZEY = 11;
    private static final BlockPos MERKEZ = new BlockPos(16, YUZEY + 1, 16);

    private SinkholeOyunTestleri() {
    }

    @AfterBatch(batch = "sinkhole_cokus")
    public static void cokusSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    @AfterBatch(batch = "sinkhole_kapali")
    public static void kapaliSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    @AfterBatch(batch = "sinkhole_stop")
    public static void stopSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    private static Sinkhole baslat(GameTestHelper y, int seviye, ServerPlayer oyuncu) {
        AfetYoneticisi.al(y.getLevel().getServer()).durdur();
        BlockPos merkez = y.absolutePos(MERKEZ);
        return aktifBaslat(y, AfetTuru.SINKHOLE, seviye, merkez, () -> Sinkhole.sabitMerkezli(merkez), oyuncu);
    }

    /**
     * Seviye 1: çatlama → sarkma → çöküş → durulma doğal sırayla biter; çekirdekte yüzey ve altı boş, planın her sütunu
     * işlendi, çukurun 4 blok dışı sağlam; merkezde duran oyuncu için {@code CUKURA_DUSTU} bir kez yayınlandı.
     */
    @GameTest(template = SAHNE, batch = "sinkhole_cokus", timeoutTicks = 900)
    public static void sinkholeCokerCukuraDolarIpucuVerir(GameTestHelper y) {
        zemin(y, YUZEY);
        boolean eski = blokHasari(y, true);
        BlockPos m = y.absolutePos(MERKEZ);
        ServerPlayer oyuncu = oyuncu(y, m.getX() + 0.5, m.getY(), m.getZ() + 0.5);
        List<BasarimIpucuEvent> ipuclari = new ArrayList<>();
        Consumer<BasarimIpucuEvent> dinleyici = olay -> {
            if (olay.oyuncu() == oyuncu) {
                ipuclari.add(olay);
            }
        };
        dinle(BasarimIpucuEvent.class, dinleyici);
        Sinkhole sinkhole = baslat(y, 1, oyuncu);
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        y.succeedWhen(() -> {
            y.assertTrue(yonetici.aktifAfet().isEmpty(), "sinkhole sürüyor");
            MinecraftForge.EVENT_BUS.unregister(dinleyici);
            y.assertTrue(sinkhole.altAsama() == SinkholeSeviyesi.DURULMA, "durulmaya ulaşmadan bitti");
            y.assertTrue(ipuclari.size() == 1 && ipuclari.get(0).ipucu() == Ipucu.CUKURA_DUSTU,
                    "CUKURA_DUSTU ipucu sayısı " + ipuclari.size());
            CukurPlani.Plan plan = sinkhole.plan();
            for (CukurPlani.Sutun c : plan.sutunlar()) {
                if (Math.hypot(c.dx(), c.dz()) <= 1.5) {
                    for (int k = YUZEY; k >= YUZEY - 2; k--) {
                        y.assertTrue(y.getBlockState(new BlockPos(16 + c.dx(), k, 16 + c.dz())).isAir(),
                                "çekirdek boş değil: " + c + " y " + k);
                    }
                }
            }
            y.assertTrue(sinkhole.isler().degisenBlok() >= plan.sutunlar().size(), "çöküş blok değiştirmedi");
            int r = (int) Math.ceil(sinkhole.seviyeBilgisi().yaricap()) + 4;
            for (BlockPos p : new BlockPos[] {new BlockPos(16 + r, YUZEY, 16), new BlockPos(16 - r, YUZEY, 16),
                    new BlockPos(16, YUZEY, 16 + r), new BlockPos(16, YUZEY, 16 - r)}) {
                y.assertTrue(y.getBlockState(p).is(Blocks.GRASS_BLOCK), "çukur dışı bozuldu: " + p);
            }
            y.assertTrue(dusenSayisi(y) == 0, "düşen blok havada kaldı");
            blokHasari(y, eski);
            y.getLevel().getServer().getPlayerList().remove(oyuncu);
            sahneyiTemizle(y);
        });
    }

    /**
     * {@code afetBlokHasari=false}: çatlama, sarkma ve çöküş aşamalarından geçilir, kutunun hash'i aynı kalır; {@code stop}
     * örtüleri, ortam sesini ve afeti bırakmaz.
     */
    @GameTest(template = SAHNE, batch = "sinkhole_kapali", timeoutTicks = 300)
    public static void sinkholeBlokHasariKapaliVeStopTemiz(GameTestHelper y) {
        zemin(y, YUZEY);
        boolean eski = blokHasari(y, false);
        long once = kutuHash(y);
        BlockPos m = y.absolutePos(MERKEZ);
        ServerPlayer oyuncu = oyuncu(y, m.getX() + 12.5, m.getY(), m.getZ() + 0.5);
        Sinkhole sinkhole = baslat(y, 2, oyuncu);
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        y.runAfterDelay(20, () -> {
            y.assertTrue(sinkhole.isler().ortuSayisi() > 0, "çatlama örtüsü yok");
            y.assertTrue(yonetici.asamaIleri() && yonetici.asamaIleri(), "alt aşama ilerlemedi");
            y.assertTrue(sinkhole.altAsama() == SinkholeSeviyesi.COKUS, "çöküşte değil: " + sinkhole.altAsama());
        });
        y.runAfterDelay(40, () -> {
            y.assertTrue(kutuHash(y) == once, "blok hasarı kapalıyken kutu değişti");
            y.assertTrue(sinkhole.isler().degisenBlok() == 0, "değişen blok " + sinkhole.isler().degisenBlok());
            y.assertTrue(yonetici.durdur(), "stop afet bulamadı");
        });
        y.runAfterDelay(42, () -> {
            y.assertTrue(yonetici.aktifAfet().isEmpty(), "stop sonrası afet sürüyor");
            y.assertTrue(!OrtamSesi.aktifMi(Sinkhole.SES_KATMANI), "ortam katmanı kaldı");
            y.assertTrue(sinkhole.isler().ortuSayisi() == 0, "çatlak örtüsü kaldı");
            blokHasari(y, eski);
            y.getLevel().getServer().getPlayerList().remove(oyuncu);
            sahneyiTemizle(y);
            y.succeed();
        });
    }

    /** Çöküş sürerken {@code stop}: havadaki düşen bloklar kaldırılır. */
    @GameTest(template = SAHNE, batch = "sinkhole_stop", timeoutTicks = 300)
    public static void sinkholeCokusteStopDusenleriKaldirir(GameTestHelper y) {
        zemin(y, YUZEY);
        boolean eski = blokHasari(y, true);
        BlockPos m = y.absolutePos(MERKEZ);
        ServerPlayer oyuncu = oyuncu(y, m.getX() + 12.5, m.getY(), m.getZ() + 0.5);
        Sinkhole sinkhole = baslat(y, 2, oyuncu);
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        y.assertTrue(yonetici.asamaIleri() && yonetici.asamaIleri(), "alt aşama ilerlemedi");
        boolean[] durdu = new boolean[1];
        y.succeedWhen(() -> {
            if (!durdu[0]) {
                y.assertTrue(sinkhole.isler().canliDusenSayisi() > 0, "çöküş düşen blok üretmedi");
                durdu[0] = yonetici.durdur();
            }
            y.assertTrue(yonetici.aktifAfet().isEmpty(), "stop sonrası afet sürüyor");
            y.assertTrue(sinkhole.isler().canliDusenSayisi() == 0 && dusenSayisi(y) == 0, "düşen blok kaldı");
            blokHasari(y, eski);
            y.getLevel().getServer().getPlayerList().remove(oyuncu);
            sahneyiTemizle(y);
        });
    }
}
