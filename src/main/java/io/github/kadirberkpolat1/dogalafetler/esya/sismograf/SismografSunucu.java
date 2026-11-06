package io.github.kadirberkpolat1.dogalafetler.esya.sismograf;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.Gorunurluk;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.MerkezTahmini;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.OncuSarsinti;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.SiddetTahmini;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.YonMesafe;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sismografın sunucu tarafı (madde 3, 4, 13b). Her {@code esya.sismograf.paketAraligiTick}'te (1 Hz) sismografı hotbar'da
 * ya da yan elde taşıyan oyuncular için algılamayı hesaplar ve değişince yalnız o oyuncuya gönderir; bırakana tek
 * {@link SismografPaketi#YOK}. Yalnız deprem ve sinkhole; ON_UYARI'da normal sismograf {@code normalOnUyariBasladi()}
 * sonrası, Hassas Sismograf erken bölümden ({@code esya.sismograf.hassasErkenOran}) itibaren algılar. Şiddet yüzdesi ve
 * merkez afet başına sabit bir hatayla tahmin edilir (Hassas'ta dar).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class SismografSunucu {
    private static final Map<UUID, SismografPaketi> SON = new HashMap<>();
    private static final long HASSAS_TOHUM = 0x48A55A5L;

    private SismografSunucu() {
    }

    @SubscribeEvent
    public static void sunucuTick(TickEvent.ServerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = olay.getServer();
        if (server.getTickCount() % SismografAyarlari.paketAraligiTick() != 0) {
            return;
        }
        for (ServerPlayer oyuncu : server.getPlayerList().getPlayers()) {
            guncelle(oyuncu);
        }
    }

    @SubscribeEvent
    public static void oyuncuCikti(PlayerEvent.PlayerLoggedOutEvent olay) {
        SON.remove(olay.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void sunucuDurdu(ServerStoppedEvent olay) {
        SON.clear();
    }

    /** Durumu hesaplar; son gönderilenden farklıysa oyuncuya gönderir. Sismografı hiç taşımamış oyuncuya paket gitmez. */
    public static void guncelle(ServerPlayer oyuncu) {
        SismografPaketi yeni = durum(oyuncu);
        SismografPaketi eski = SON.get(oyuncu.getUUID());
        if (yeni.equals(eski) || eski == null && !yeni.tasiyor()) {
            return;
        }
        if (yeni.tasiyor()) {
            SON.put(oyuncu.getUUID(), yeni);
        } else {
            SON.remove(oyuncu.getUUID());
        }
        AgKanali.oyuncuyaGonder(oyuncu, yeni);
    }

    /** Oyuncunun sismografının göreceği durum (göndermez). */
    public static SismografPaketi durum(ServerPlayer oyuncu) {
        Sismograf sismograf = Sismograf.tasinan(oyuncu);
        if (sismograf == null) {
            return SismografPaketi.YOK;
        }
        boolean hassas = sismograf.hassas();
        float titreme = SismografAyarlari.titreme();
        AfetYoneticisi yonetici = AfetYoneticisi.al(oyuncu.server);
        Optional<Afet> secili = yonetici.aktifAfet();
        if (secili.isEmpty() || !Gorunurluk.sismikTur(secili.get().tur())) {
            return SismografPaketi.sakin(hassas, titreme);
        }
        Afet afet = secili.get();
        Optional<EtkiAlani> okunan = yonetici.etkiAlani(afet);
        if (okunan.isEmpty()) {
            return SismografPaketi.sakin(hassas, titreme);
        }
        EtkiAlani alan = okunan.get();
        boolean ayniBoyut = oyuncu.level().dimension().equals(afet.baglam().boyut());
        double mesafe = YonMesafe.mesafe(oyuncu.getX(), oyuncu.getZ(), alan.x(), alan.z());
        if (!Gorunurluk.sismograf(afet.tur(), afet.asama(), afet.asamaGecenTick(), afet.baglam().erkenBildirimSuresi(),
                hassas, SismografAyarlari.hassasErkenOran(), ayniBoyut, mesafe, SismografAyarlari.menzil(hassas))) {
            return SismografPaketi.sakin(hassas, titreme);
        }
        double ilerleme = afet.asama() == AfetAsamasi.ON_UYARI
                ? afet.asamaGecenTick() / (double) Math.max(1, afet.baglam().onUyariSuresi()) : 1.0;
        long afetNo = afet.baglam().baslangicSaati();
        long tohum = afetNo ^ (hassas ? HASSAS_TOHUM : 0);
        int seviye = afet.seviye();
        int yuzde = SiddetTahmini.yuzde(seviye, ilerleme, hassas, tohum);
        double[] merkez = MerkezTahmini.tahmin(alan.x(), alan.z(), SismografAyarlari.merkezHatasi(hassas), tohum);
        float oncu = Math.round(OncuSarsinti.siddet(ilerleme, seviye) * 50) / 50f;
        return new SismografPaketi(true, hassas, true, afet.tur(), afet.asama(), (int) Math.floor(merkez[0]),
                (int) Math.floor(merkez[1]), yuzde, hassas ? seviye : 0, oncu, titreme, afetNo);
    }
}
