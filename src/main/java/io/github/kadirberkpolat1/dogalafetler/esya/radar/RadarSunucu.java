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
 * <p>Kaynak çekirdektir: {@code aktifAfetler()} (ON_UYARI dahil; birden çoksa oyuncunun gördüğü en yakını), {@code normalOnUyariBasladi()}, korumalı {@code
 * kalanOku}/{@code etkiAlani} (çekirdek §1: {@code asamaKalanTick()}/{@code etkiAlani()} doğrudan çağrılmaz) ve
 * {@code DunyaDonemi.sonAfetler()}. Hareketli afetin güncel merkezi her saniye etki alanından okunur; Gelişmiş radarın
 * hareket yönü bu izden çıkar.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class RadarSunucu {
    private static final Map<UUID, RadarPaketi> SON = new HashMap<>();
    /** Tek iz ({@link #izAfeti}): afet numarasıyla sıfırlanır. 6 örnek = son 5 saniye. */
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
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        Optional<Afet> afet = izAfeti(yonetici);
        if (afet.isEmpty()) {
            IZ.sifirla();
            return;
        }
        yonetici.etkiAlani(afet.get()).ifPresent(
                alan -> IZ.ekle(afet.get().baglam().baslangicSaati(), server.getTickCount(), alan.x(), alan.z()));
    }

    /** Hareket izinin izlediği afet: sürenlerin ilk bölgesel olanı (tek iz; başka afet seçilirse yön gösterilmez). */
    private static Optional<Afet> izAfeti(AfetYoneticisi yonetici) {
        for (Afet afet : yonetici.aktifAfetler()) {
            if (!afet.tur().sunucuGeneli() && yonetici.etkiAlani(afet).filter(alan -> !alan.global()).isPresent()) {
                return Optional.of(afet);
            }
        }
        return Optional.empty();
    }

    /** Oyuncunun radarının gösterdiği afet: görünen afetlerin en yakını (AS-10; eşitlikte önce başlayan). */
    private record Secim(Afet afet, EtkiAlani alan, boolean global, RadarGorunumu gorunum, double mesafe) {
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
        Secim secim = null;
        for (Afet aday : yonetici.aktifAfetler()) {
            Optional<EtkiAlani> okunan = yonetici.etkiAlani(aday);
            if (okunan.isEmpty()) {
                continue;
            }
            EtkiAlani a = okunan.get();
            boolean g = a.global() || aday.tur().sunucuGeneli();
            boolean ayniBoyut = oyuncu.level().dimension().equals(aday.baglam().boyut());
            double m = g ? 0 : YonMesafe.mesafe(oyuncu.getX(), oyuncu.getZ(), a.x(), a.z());
            RadarGorunumu gr = Gorunurluk.radar(aday.asama(), aday.normalOnUyariBasladi(), g, ayniBoyut, m, menzil);
            if (gr != RadarGorunumu.GIZLI && (secim == null || m < secim.mesafe())) {
                secim = new Secim(aday, a, g, gr, m);
            }
        }
        if (secim == null) {
            return RadarPaketi.sakin(gelismis, menzil, gecmis);
        }
        Afet afet = secim.afet();
        EtkiAlani alan = secim.alan();
        boolean global = secim.global();
        RadarGorunumu gorunum = secim.gorunum();
        long kalanSn = SureBicimi.saniye(yonetici.zamanOlcegi().gercekTick(yonetici.kalanOku(afet)));
        int x = global ? 0 : (int) Math.floor(alan.x());
        int z = global ? 0 : (int) Math.floor(alan.z());
        float yon = Float.NaN;
        float hiz = 0;
        if (gelismis && !global && IZ.yon().isPresent() && izAfeti(yonetici).orElse(null) == afet) {
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
