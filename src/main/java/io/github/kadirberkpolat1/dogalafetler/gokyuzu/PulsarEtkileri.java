package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.GunesPatlamasiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.GunesPatlamasiSesleri;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.PulsarYolu;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAPI;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorGorunumu;
import io.github.kadirberkpolat1.dogalafetler.uyari.HacimEgrisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginAyarlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.server.ServerStoppedEvent;

/**
 * Pulsar döneminin dünya etkileri (PL-3; {@code PulsarSureci} evre PULSAR iken her tick çağırır): kavurucu rüzgâr
 * ({@code OrtamSesi} katmanı {@value #RUZGAR_KATMANI}), {@code afetBlokHasari} açıkken kavurma/kül/lav
 * ({@link KavurmaKuyrugu}, tek bütçe) ve yangın odakları (ortak yangın sistemi; gerçek tick'te merkez başına
 * {@code pulsarYanginDakikaBasina/1200 × güç × TPS kısması}, aktif ateş yangın tavanının altındayken), kapıdan
 * bağımsız meteor yağmuru (oyuncu başına dakikada {@code pulsarMeteorDakikaBasina × güç}, kızıl-beyaz
 * {@link MeteorGorunumu#PULSAR}; krater kuralı meteorun kendisinde). Yağmur kaydedilmez: kimliği geçicidir, yoksa
 * yeniden kurulur; ödül tavanı (MT2-20) ve uçan meteor tavanı kaynak {@link #kaynak} ile paylaşılır. Kül yağışı
 * istemcidedir.
 */
public final class PulsarEtkileri {
    static final String RUZGAR_KATMANI = "gokyuzu/pulsar";
    /** Pulsar yolundaki buzul kıyametin tipi rüzgârı (kasırga rüzgârı döngüsü; donmanın kendi ortam katmanı yok). */
    static final String BUZUL_KATMANI = "gokyuzu/buzul";
    private static final float BUZUL_RUZGAR_DUZEYI = 0.55f;
    /** Yangın odağının merkezden uzaklığı (blok). */
    private static final int YANGIN_EN_YAKIN = 16;
    private static final int YANGIN_EN_UZAK = 96;
    /** Bir yangın denemesinde yanıcı sütun aranırken bakılan en çok sütun. */
    private static final int YANGIN_SUTUN_DENEMESI = 4;

    private static int yagmurKimlik = -1;
    /** Son tick'in kaynağı: {@code sifirla} dönemi temizledikten sonra gelen {@link #bitir} de onu durdurur. */
    private static YanginKaynagi sonKaynak;

    private PulsarEtkileri() {
    }

    /** Yangın ve meteor kaynağı: güneş patlaması türü + pulsar döngüsü (gerçek bir afetin numarasıyla çakışmaz). */
    public static YanginKaynagi kaynak(DunyaDonemi donem) {
        return YanginKaynagi.afet(AfetTuru.GUNES_PATLAMASI, PulsarYolu.dongu(donem));
    }

    public static void tick(MinecraftServer server, ServerLevel ust, DunyaDonemi donem, float guc, int adim) {
        if (!OrtamSesi.aktifMi(RUZGAR_KATMANI)) {
            OrtamSesi.baslat(server, RUZGAR_KATMANI, OrtamSesi.AFETSIZ, GunesPatlamasiSesleri.KAVRULMA_RUZGAR,
                    SoundSource.WEATHER, OrtamSesi.Yer.global(), HacimEgrisi.giris(200, 0.85f));
        }
        YanginKaynagi kaynak = kaynak(donem);
        sonKaynak = kaynak;
        meteorYagmuru(ust, GokyuzuAyarlari.pulsarMeteorDakikaBasina() * Math.max(0f, guc), kaynak);
        if (!(guc > 0f) || !Kurallar.blokHasariAcik(ust.getGameRules())) {
            return;
        }
        KavurmaKuyrugu.tick(ust, guc, adim);
        double oran = GokyuzuAyarlari.pulsarYanginDakikaBasina() / 1200.0 * guc * KavurmaKuyrugu.kisma();
        if (oran <= 0 || YanginYoneticisi.aktifSayi(ust) >= YanginAyarlari.aktifTavan()) {
            return;
        }
        RandomSource r = ust.getRandom();
        for (BlockPos merkez : KavurmaKuyrugu.merkezler(ust)) {
            if (r.nextDouble() < oran) {
                yanginCikar(ust, merkez, r, kaynak);
            }
        }
    }

    /** Pulsar yolundaki buzul kıyamet: tipi rüzgârı çalar (evre değişince, {@code sifirla} ve durdurmada {@link #bitir}). */
    public static void buzul(MinecraftServer server) {
        if (!OrtamSesi.aktifMi(BUZUL_KATMANI)) {
            OrtamSesi.baslat(server, BUZUL_KATMANI, OrtamSesi.AFETSIZ, KasirgaKayitlari.RUZGAR, SoundSource.WEATHER,
                    OrtamSesi.Yer.global(), HacimEgrisi.giris(200, BUZUL_RUZGAR_DUZEYI));
        }
    }

    /** Rüzgâr söner; pulsarın ateşleri yayılmadan yanıp biter, meteor yağmuru durur (uçanlar düşmeyi tamamlar). */
    public static void bitir(MinecraftServer server, DunyaDonemi donem, String neden) {
        boolean ruzgar = OrtamSesi.aktifMi(RUZGAR_KATMANI);
        if (ruzgar) {
            OrtamSesi.durdur(server, RUZGAR_KATMANI, 80);
        }
        if (OrtamSesi.aktifMi(BUZUL_KATMANI)) {
            OrtamSesi.durdur(server, BUZUL_KATMANI, 80);
        }
        YanginKaynagi kaynak = kaynak(donem);
        int durdu = durdur(server, kaynak);
        if (sonKaynak != null && !sonKaynak.equals(kaynak)) {
            durdu += durdur(server, sonKaynak);
        }
        boolean etkiVardi = ruzgar || durdu > 0 || sonKaynak != null;
        yagmurKimlik = -1;
        sonKaynak = null;
        if (etkiVardi) {
            DogalAfetler.LOGGER.info("Pulsar etkileri bitti: {} (rüzgâr {}, {} meteor yağmuru durdu)", neden,
                    ruzgar ? "söndü" : "yoktu", durdu);
        }
    }

    private static int durdur(MinecraftServer server, YanginKaynagi kaynak) {
        YanginYoneticisi.kaynakBitti(server, kaynak, false);
        return MeteorAPI.yagmurlariDurdur(server, kaynak, false);
    }

    /** Yağmur varsa yoğunluğu güncellenir (0'da durmaz, birikim kalır), yoksa yoğunluk pozitifken kurulur. */
    private static void meteorYagmuru(ServerLevel ust, double dakikaBasina, YanginKaynagi kaynak) {
        if (yagmurKimlik >= 0 && MeteorAPI.yagmurYogunluguAyarla(ust, yagmurKimlik, dakikaBasina)) {
            return;
        }
        yagmurKimlik = dakikaBasina > 0 ? MeteorAPI.yagmur(ust, List.of(), Integer.MAX_VALUE, dakikaBasina,
                GunesPatlamasiAyarlari.buyukMeteorOrani(), MeteorGorunumu.PULSAR, kaynak) : -1;
    }

    /** Merkezin çevresinde göğe açık ilk yanıcı yüzeyi (yaprak, çimen, odun) tutuşturur. */
    private static void yanginCikar(ServerLevel level, BlockPos merkez, RandomSource r, YanginKaynagi kaynak) {
        for (int i = 0; i < YANGIN_SUTUN_DENEMESI; i++) {
            double aci = r.nextDouble() * Math.PI * 2;
            double uzaklik = YANGIN_EN_YAKIN + r.nextDouble() * (YANGIN_EN_UZAK - YANGIN_EN_YAKIN);
            int x = merkez.getX() + (int) Math.round(Math.cos(aci) * uzaklik);
            int z = merkez.getZ() + (int) Math.round(Math.sin(aci) * uzaklik);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            BlockPos ust = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
            if (YanginYoneticisi.yanabilir(level.getBlockState(ust), level, ust)
                    && YanginYoneticisi.tutustur(level, ust, kaynak, 1.0) > 0) {
                return;
            }
        }
    }

    /** Test okuması: pulsar yağmurunun kimliği (-1: yok). */
    static int yagmurKimlik() {
        return yagmurKimlik;
    }

    public static void sunucuDurdu(ServerStoppedEvent olay) {
        yagmurKimlik = -1;
        sonKaynak = null;
    }
}
