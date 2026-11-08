package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetAsamaDegistiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.SirenDurdurEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.SirenIstegiEvent;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.SirenDurdurPaketi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.SirenPaketi;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sunucu tarafı siren (madde 3, 5, N11): çekirdeğin {@link SirenIstegiEvent}'ini {@code uyari/siren} paketine,
 * {@link SirenDurdurEvent}'ini {@code uyari/siren_durdur} paketine çevirir. Alıcıları ve kalan süreyi çekirdek seçer
 * (bölgesel afette alan + {@code sirenEkYaricap}, sunucu geneli afette her boyuttaki herkes; siren sürerken gelenlere 1 Hz
 * kalan süreyle). Ekrana hiçbir şey yazmaz (N6); olanı sunucu log'una yazar.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class SirenYoneticisi {
    private static long sonAfetNo = Long.MIN_VALUE;
    private static int sonAfetAlicisi;

    private SirenYoneticisi() {
    }

    @SubscribeEvent
    public static void sirenIstegi(SirenIstegiEvent olay) {
        List<ServerPlayer> oyuncular = olay.oyuncular();
        if (oyuncular.isEmpty()) {
            return;
        }
        AfetYoneticisi yonetici = AfetYoneticisi.al(oyuncular.get(0).server);
        long afetNo = afetNo(yonetici, olay.afetTuru());
        long tamSure = yonetici.zamanOlcegi().gercekTick(SirenTuru.sureTick(olay.afetTuru()));
        int toplam = (int) Math.max(olay.sureTick(), Math.min(SirenZarfi.EN_UZUN_TICK, tamSure));
        SirenPaketi paket = new SirenPaketi(olay.sirenTuru(), olay.afetTuru(), afetNo, toplam, olay.sureTick());
        for (ServerPlayer oyuncu : oyuncular) {
            AgKanali.oyuncuyaGonder(oyuncu, paket);
        }
        if (afetNo != sonAfetNo) {
            sonAfetNo = afetNo;
            sonAfetAlicisi = 0;
        }
        sonAfetAlicisi += oyuncular.size();
        DogalAfetler.LOGGER.info("[uyari] siren paketi gönderildi: {} {} toplam {} kalan {} tick, afetNo {} -> {}",
                olay.afetTuru().kimlik(), olay.sirenTuru().kimlik(), toplam, olay.sureTick(), afetNo, adlar(oyuncular));
    }

    @SubscribeEvent
    public static void sirenDurdur(SirenDurdurEvent olay) {
        List<ServerPlayer> oyuncular = olay.oyuncular();
        int fade = UyariAyarlari.fadeOutTick(SirenTuru.afetIcin(olay.afetTuru()));
        long afetNo = oyuncular.isEmpty() ? sonAfetNo : afetNo(AfetYoneticisi.al(oyuncular.get(0).server), olay.afetTuru());
        SirenDurdurPaketi paket = new SirenDurdurPaketi(afetNo, fade);
        for (ServerPlayer oyuncu : oyuncular) {
            AgKanali.oyuncuyaGonder(oyuncu, paket);
        }
        DogalAfetler.LOGGER.info("[uyari] siren durdurma paketi gönderildi: {} fade-out {} tick, afetNo {} -> {}",
                olay.afetTuru().kimlik(), fade, afetNo, adlar(oyuncular));
    }

    /** Özet: SIREN aşaması başlarken kaç oyuncuya siren gitti (oyuncusuz sunucuda 0; çekirdek o zaman olay yayınlamaz). */
    @SubscribeEvent
    public static void asamaDegisti(AfetAsamaDegistiEvent olay) {
        if (olay.yeni() != AfetAsamasi.SIREN || olay.eski() == AfetAsamasi.SIREN) {
            return;
        }
        long afetNo = olay.afet().baglam().baslangicSaati();
        int alici = afetNo == sonAfetNo ? sonAfetAlicisi : 0;
        DogalAfetler.LOGGER.info("[uyari] SIREN aşaması: {} afetNo {}, {} oyuncuya siren paketi gitti",
                olay.afet().tur().kimlik(), afetNo, alici);
    }

    private static long afetNo(AfetYoneticisi yonetici, AfetTuru tur) {
        return yonetici.aktifAfet(tur).map(afet -> afet.baglam().baslangicSaati())
                .orElse(sonAfetNo);
    }

    private static String adlar(List<ServerPlayer> oyuncular) {
        return oyuncular.stream().map(o -> o.getGameProfile().getName()).collect(Collectors.joining(", ", "[", "]"));
    }

    /** GameTest ve log için: sunucu kapanırken özet sıfırlanır. */
    static void sifirla() {
        sonAfetNo = Long.MIN_VALUE;
        sonAfetAlicisi = 0;
    }
}
