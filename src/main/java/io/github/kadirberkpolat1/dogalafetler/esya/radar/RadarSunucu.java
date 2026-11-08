package io.github.kadirberkpolat1.dogalafetler.esya.radar;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.GecmisKaydi;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.Gorunurluk;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.HareketIzi;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.RadarGorunumu;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.SureBicimi;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.YonMesafe;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hava Radarı'nın sunucu tarafı (madde 3, 4, 13a, N7). Her {@code esya.radar.paketAraligiTick}'te (1 Hz) radarı ana ya da
 * yan elinde tutan her oyuncu için durumu hesaplar ve önceki gönderilenden farklıysa yalnız o oyuncuya gönderir; radarı
 * bırakan oyuncuya tek {@link RadarPaketi#YOK} gider, radarı olmayana hiçbir şey gitmez.
 *
 * <p>Kaynak çekirdektir: {@code aktifAfet()} (ON_UYARI dahil), {@code normalOnUyariBasladi()}, korumalı {@code
 * kalanOku}/{@code etkiAlani} (çekirdek §1: {@code asamaKalanTick()}/{@code etkiAlani()} doğrudan çağrılmaz) ve
 * {@code DunyaDonemi.sonAfetler()}. Hareketli afetin güncel merkezi her saniye etki alanından okunur; Gelişmiş radarın
 * hareket yönü bu izden çıkar.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class RadarSunucu {
    private static final Map<UUID, RadarPaketi> SON = new HashMap<>();
    /** Tek aktif afet yuvası: iz afet numarasıyla sıfırlanır. 6 örnek = son 5 saniye. */
    private static final HareketIzi IZ = new HareketIzi(6);

    private RadarSunucu() {
    }

    @SubscribeEvent
    public static void sunucuTick(TickEvent.ServerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = olay.getServer();
        if (server.getTickCount() % RadarAyarlari.paketAraligiTick() != 0) {
            return;
        }
        izGuncelle(server);
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
        IZ.sifirla();
    }

    /** Durumu hesaplar; son gönderilenden farklıysa oyuncuya gönderir. Radarı hiç tutmamış oyuncuya paket gitmez. */
    public static void guncelle(ServerPlayer oyuncu) {
        RadarPaketi yeni = durum(oyuncu);
        RadarPaketi eski = SON.get(oyuncu.getUUID());
        if (yeni.equals(eski) || eski == null && !yeni.elde()) {
            return;
        }
        if (yeni.elde()) {
            SON.put(oyuncu.getUUID(), yeni);
        } else {
            SON.remove(oyuncu.getUUID());
        }
        AgKanali.oyuncuyaGonder(oyuncu, yeni);
    }

    private static void izGuncelle(MinecraftServer server) {
        Optional<Afet> afet = AfetYoneticisi.al(server).aktifAfet();
        if (afet.isEmpty()) {
            IZ.sifirla();
            return;
        }
        AfetYoneticisi.al(server).etkiAlani(afet.get()).filter(alan -> !alan.global())
                .ifPresent(alan -> IZ.ekle(afet.get().baglam().baslangicSaati(), server.getTickCount(), alan.x(), alan.z()));
    }

    /** Oyuncunun radarının göreceği durum (göndermez). */
    public static RadarPaketi durum(ServerPlayer oyuncu) {
        HavaRadari radar = HavaRadari.eldeki(oyuncu);
        if (radar == null) {
            return RadarPaketi.YOK;
        }
        boolean gelismis = radar.gelismis();
        int menzil = RadarAyarlari.menzil(gelismis);
        AfetYoneticisi yonetici = AfetYoneticisi.al(oyuncu.server);
        List<RadarPaketi.Gecmis> gecmis = gelismis ? gecmis(yonetici) : List.of();
        Optional<Afet> secili = yonetici.aktifAfet();
        if (secili.isEmpty()) {
            return RadarPaketi.sakin(gelismis, menzil, gecmis);
        }
        Afet afet = secili.get();
        Optional<EtkiAlani> okunan = yonetici.etkiAlani(afet);
        if (okunan.isEmpty()) {
            return RadarPaketi.sakin(gelismis, menzil, gecmis);
        }
        EtkiAlani alan = okunan.get();
        // N7 gökyüzü anomalisi yalnız sunucu geneli afette (güneş, ay). Her yerde süren bölgesel afet (asit, AF2-06) bölgesel
        // görünürlük kuralıyla (ön-uyarının gizli başı saklı, aynı boyut) mesafesiz "her yer" olarak görünür.
        boolean gokyuzu = afet.tur().sunucuGeneli();
        boolean global = alan.global() || gokyuzu;
        boolean herYer = global && !gokyuzu;
        boolean ayniBoyut = oyuncu.level().dimension().equals(afet.baglam().boyut());
        double mesafe = global ? 0 : YonMesafe.mesafe(oyuncu.getX(), oyuncu.getZ(), alan.x(), alan.z());
        RadarGorunumu gorunum = Gorunurluk.radar(afet.asama(), afet.normalOnUyariBasladi(), gokyuzu, ayniBoyut, mesafe,
                menzil);
        if (gorunum == RadarGorunumu.GIZLI) {
            return RadarPaketi.sakin(gelismis, menzil, gecmis);
        }
        long kalanSn = SureBicimi.saniye(yonetici.zamanOlcegi().gercekTick(yonetici.kalanOku(afet)));
        int x = herYer ? RadarPaketi.HER_YER : global ? 0 : (int) Math.floor(alan.x());
        int z = herYer ? RadarPaketi.HER_YER : global ? 0 : (int) Math.floor(alan.z());
        float yon = Float.NaN;
        float hiz = 0;
        Optional<double[]> araHedef = afet.yolHedefi()
                .filter(h -> YonMesafe.mesafe(alan.x(), alan.z(), h[0], h[1]) >= 1.0);
        if (gelismis && !global && araHedef.isPresent()) {
            // Yol tahmini: afetin yöneldiği ara hedef (kasırga rotası), izden daha erken döner.
            double[] h = araHedef.get();
            yon = Math.round(YonMesafe.pusulaAcisi(alan.x(), alan.z(), h[0], h[1]) / 5.0) * 5f % 360f;
            hiz = Math.round(IZ.hiz() * 2) / 2f;
        } else if (gelismis && !global && IZ.yon().isPresent()) {
            OptionalDouble iz = IZ.yon();
            yon = Math.round(iz.getAsDouble() / 5.0) * 5f % 360f;
            hiz = Math.round(IZ.hiz() * 2) / 2f;
        }
        return new RadarPaketi(true, gelismis, menzil, gorunum, afet.tur(), afet.asama(), x, z,
                gelismis ? afet.seviye() : 0, kalanSn, yon, hiz, gecmis);
    }

    /** Son 5 afet; yaşı gerçek dakika (dakikada en çok bir kez değişir, paketi saniyede bir tazelemez). */
    private static List<RadarPaketi.Gecmis> gecmis(AfetYoneticisi yonetici) {
        long simdi = yonetici.donem().saat();
        List<RadarPaketi.Gecmis> liste = new ArrayList<>();
        for (GecmisKaydi kayit : yonetici.donem().sonAfetler()) {
            if (liste.size() >= RadarPaketi.GECMIS_SINIRI) {
                break;
            }
            long gercek = yonetici.zamanOlcegi().gercekTick(Math.max(0, simdi - kayit.saat()));
            liste.add(new RadarPaketi.Gecmis(kayit.tur(), kayit.seviye(), (int) Math.min(Integer.MAX_VALUE, gercek / 1200)));
        }
        return liste;
    }
}
