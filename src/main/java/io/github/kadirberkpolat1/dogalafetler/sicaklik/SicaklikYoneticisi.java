package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Esikler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Etkinlik;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.IsiAlisverisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Islaklik;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.OrtamSicakligi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Yalitim;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sunucu tarafı: her oyuncunun vücut sıcaklığını ayrı ayrı, varsayılan saniyede bir günceller (oyuncular tick'lere yayılır),
 * etkileri uygular (yavaşlama, hasar, parçacık) ve HUD paketini yalnız o oyuncuya yollar. Kapı kapalıyken hiçbir şey
 * yapmaz; kapanışta HUD'u gizleyen tek bir paket gider ve yavaşlama kaldırılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class SicaklikYoneticisi {
    static final UUID YAVASLAMA = UUID.fromString("6f1b4d2e-35a1-4c7e-9b0e-5d7a2c9e1f40");
    private static final String YAVASLAMA_ADI = "dogalafetler sicaklik yavaslama";
    /** Bu kadar yatay yol (blok/güncelleme) yürüme sayılır. */
    private static final double YURUME_ESIGI = 0.5;

    /** Oyuncunun kalıcı olmayan sunucu durumu (son konum, son gönderilen paket). */
    private static final class Gecici {
        Vec3 sonKonum;
        boolean hudAcik;
        float sonVucut = Float.NaN;
        float sonOrtam;
        Kademe sonKademe;
        long sonGonderim;
    }

    /** Bir oyuncunun o anki ısı durumu ({@code bilgi} komutu ve güncelleme aynı hesabı kullanır). */
    public record Analiz(OrtamSicakligi.Sonuc ortam, IsiAlisverisi.Girdi girdi, double egilim, double ortamBirim) {
    }

    private static final Map<UUID, Gecici> GECICI = new HashMap<>();

    private SicaklikYoneticisi() {
    }

    private static Gecici gecici(ServerPlayer oyuncu) {
        return GECICI.computeIfAbsent(oyuncu.getUUID(), k -> new Gecici());
    }

    @SubscribeEvent
    public static void sunucuTick(TickEvent.ServerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = olay.getServer();
        boolean acik = SicaklikKapisi.acikMi(server);
        int aralik = SicaklikAyarlari.guncellemeAraligiTick();
        for (ServerPlayer oyuncu : server.getPlayerList().getPlayers()) {
            if (!acik) {
                kapat(oyuncu);
            } else if (Math.floorMod(server.getTickCount() + oyuncu.getId(), aralik) == 0) {
                guncelle(oyuncu, aralik);
            }
        }
    }

    /** Analiz: ortam, yalıtım, etkinlik, ıslaklık ve eğilim (oyuncunun durumunu değiştirmez). */
    public static Analiz analiz(ServerPlayer oyuncu) {
        SicaklikAyarlari.Ozet ozet = SicaklikAyarlari.al();
        OrtamSicakligi.Sonuc ortam = OrtamOlcer.olc(oyuncu, ozet);
        Yalitim.Girdi yalitim = ZirhEkleri.yalitimGirdisi(oyuncu);
        IsiAlisverisi.Girdi girdi = new IsiAlisverisi.Girdi(VucutSicakligi.al(oyuncu), ortam.toplam(),
                etkinlik(oyuncu), islaklik(oyuncu), yalitim);
        return new Analiz(ortam, girdi, IsiAlisverisi.egilim(girdi, ozet.isi(), ozet.yalitim()),
                ozet.isi().esdeger(ortam.toplam()));
    }

    /**
     * Oyuncunun vücut sıcaklığını {@code tick} kadar ilerletir, etkileri uygular, gerekirse paket yollar. Kapı kontrolü
     * çağıranındır (GameTest'ler sahte oyuncular tick almadığı için bunu doğrudan çağırır).
     */
    public static void guncelle(ServerPlayer oyuncu, int tick) {
        if (oyuncu.isSpectator() || !oyuncu.isAlive()) {
            return;
        }
        SicaklikAyarlari.Ozet ozet = SicaklikAyarlari.al();
        Analiz a = analiz(oyuncu);
        Gecici g = gecici(oyuncu);
        g.sonKonum = oyuncu.position();
        double yeni = IsiAlisverisi.adim(a.girdi(), tick / 20.0, ozet.isi(), ozet.yalitim())
                + VucutSicakligi.etkileriIlerlet(oyuncu, tick);
        VucutSicakligi.yaz(oyuncu, yeni);
        double vucut = VucutSicakligi.al(oyuncu);
        etkileriUygula(oyuncu, vucut, tick, ozet.esikler());
        gonderGerekirse(oyuncu, g, vucut, a, ozet.esikler(), false);
    }

    private static Etkinlik etkinlik(ServerPlayer oyuncu) {
        if (oyuncu.isSprinting()) {
            return Etkinlik.KOSMA;
        }
        Gecici g = GECICI.get(oyuncu.getUUID());
        Vec3 simdi = oyuncu.position();
        if (g == null || g.sonKonum == null) {
            return Etkinlik.YURUME;
        }
        double dx = simdi.x - g.sonKonum.x;
        double dz = simdi.z - g.sonKonum.z;
        return dx * dx + dz * dz >= YURUME_ESIGI * YURUME_ESIGI ? Etkinlik.YURUME : Etkinlik.DURMA;
    }

    private static Islaklik islaklik(ServerPlayer oyuncu) {
        if (oyuncu.isInWater()) {
            return Islaklik.SUDA;
        }
        return oyuncu.isInWaterRainOrBubble() ? Islaklik.ISLAK : Islaklik.KURU;
    }

    private static void etkileriUygula(ServerPlayer oyuncu, double vucut, int tick, Esikler e) {
        yavaslamaAyarla(oyuncu, e.yavaslama(vucut));
        Kademe kademe = e.kademe(vucut);
        if (!kademe.hasarli()) {
            VucutSicakligi.hasarSayaciAyarla(oyuncu, 0);
            return;
        }
        int aralik = e.hasarAraligi(vucut);
        int sayac = VucutSicakligi.hasarSayaci(oyuncu) + tick;
        boolean donma = kademe == Kademe.DONUYOR;
        while (aralik > 0 && sayac >= aralik) {
            sayac -= aralik;
            oyuncu.hurt(SicaklikKayitlari.hasar(oyuncu.serverLevel(),
                    donma ? SicaklikKayitlari.DONMA : SicaklikKayitlari.ASIRI_ISINMA), SicaklikAyarlari.hasarMiktari());
        }
        VucutSicakligi.hasarSayaciAyarla(oyuncu, sayac);
        // Kendisine gönderilmez: gözün 0,6 blok altındaki 8×8 sprite birinci şahısta ekranda dev bloklu leke olur.
        ServerLevel level = oyuncu.serverLevel();
        ParticleOptions tur = donma ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME;
        for (ServerPlayer izleyen : level.players()) {
            if (izleyen != oyuncu) {
                level.sendParticles(izleyen, tur, false, oyuncu.getX(), oyuncu.getY() + 1.0, oyuncu.getZ(), donma ? 6 : 4,
                        0.3, 0.5, 0.3, 0.01);
            }
        }
    }

    private static void yavaslamaAyarla(ServerPlayer oyuncu, double oran) {
        AttributeInstance hiz = oyuncu.getAttribute(Attributes.MOVEMENT_SPEED);
        if (hiz == null) {
            return;
        }
        AttributeModifier mevcut = hiz.getModifier(YAVASLAMA);
        if (mevcut != null && Math.abs(mevcut.getAmount() + oran) < 1e-4) {
            return;
        }
        if (mevcut != null) {
            hiz.removeModifier(YAVASLAMA);
        }
        if (oran > 0) {
            hiz.addTransientModifier(new AttributeModifier(YAVASLAMA, YAVASLAMA_ADI, -oran,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void gonderGerekirse(ServerPlayer oyuncu, Gecici g, double vucut, Analiz a, Esikler e,
            boolean zorla) {
        long simdi = oyuncu.serverLevel().getGameTime();
        float v = (float) vucut;
        float o = (float) a.ortamBirim();
        Kademe kademe = e.kademe(vucut);
        boolean gerekli = zorla || !g.hudAcik || kademe != g.sonKademe || Float.isNaN(g.sonVucut)
                || Math.abs(v - g.sonVucut) >= 0.25f || Math.abs(o - g.sonOrtam) >= 0.5f
                || simdi - g.sonGonderim >= SicaklikAyarlari.paketTazelemeTick();
        if (!gerekli) {
            return;
        }
        AgKanali.oyuncuyaGonder(oyuncu, SicaklikDurumPaketi.acik(v, o, (float) a.egilim(), e));
        g.hudAcik = true;
        g.sonVucut = v;
        g.sonOrtam = o;
        g.sonKademe = kademe;
        g.sonGonderim = simdi;
    }

    /** Komut ve yiyecek gibi anında değişikliklerde HUD'u hemen tazeler (kapı açıksa). */
    static void hemenGonder(ServerPlayer oyuncu) {
        if (oyuncu.connection == null || !SicaklikKapisi.acikMi(oyuncu.server)) {
            return;
        }
        Esikler e = SicaklikAyarlari.al().esikler();
        gonderGerekirse(oyuncu, gecici(oyuncu), VucutSicakligi.al(oyuncu), analiz(oyuncu), e, true);
    }

    /** Kapı kapalı: HUD bir kez gizlenir, yavaşlama ve hasar sayacı kalkar; sonra paket gitmez. */
    private static void kapat(ServerPlayer oyuncu) {
        Gecici g = GECICI.get(oyuncu.getUUID());
        if (g == null || !g.hudAcik) {
            return;
        }
        g.hudAcik = false;
        g.sonVucut = Float.NaN;
        g.sonKademe = null;
        yavaslamaAyarla(oyuncu, 0);
        VucutSicakligi.hasarSayaciAyarla(oyuncu, 0);
        AgKanali.oyuncuyaGonder(oyuncu, SicaklikDurumPaketi.KAPALI);
    }

    @SubscribeEvent
    public static void girdi(PlayerEvent.PlayerLoggedInEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            GECICI.remove(oyuncu.getUUID());
            gecici(oyuncu).sonKonum = oyuncu.position();
            hemenGonder(oyuncu);
        }
    }

    @SubscribeEvent
    public static void cikti(PlayerEvent.PlayerLoggedOutEvent olay) {
        GECICI.remove(olay.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void kopya(PlayerEvent.Clone olay) {
        if (!olay.isWasDeath() && olay.getOriginal() instanceof ServerPlayer eski
                && olay.getEntity() instanceof ServerPlayer yeni) {
            VucutSicakligi.kopyala(eski, yeni);
        }
    }

    /** Madde 3: respawn'da vücut sıcaklığı ve etkiler sıfırlanır. */
    @SubscribeEvent
    public static void yenidenDogdu(PlayerEvent.PlayerRespawnEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu && !olay.isEndConquered()) {
            yenidenDogdu(oyuncu);
        }
    }

    /** Respawn sıfırlaması (GameTest doğrudan çağırır). */
    public static void yenidenDogdu(ServerPlayer oyuncu) {
        VucutSicakligi.sifirla(oyuncu);
        yavaslamaAyarla(oyuncu, 0);
        Gecici g = gecici(oyuncu);
        g.sonKonum = oyuncu.position();
        hemenGonder(oyuncu);
    }

    @SubscribeEvent
    public static void boyutDegisti(PlayerEvent.PlayerChangedDimensionEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            hemenGonder(oyuncu);
        }
    }

    @SubscribeEvent
    public static void sunucuBasladi(ServerStartedEvent olay) {
        DogalAfetler.LOGGER.info("Sıcaklık sistemi: {}", SicaklikKapisi.ozet(olay.getServer()));
    }

    @SubscribeEvent
    public static void sunucuDurdu(ServerStoppedEvent olay) {
        GECICI.clear();
        SicaklikKancalari.temizle();
    }
}
