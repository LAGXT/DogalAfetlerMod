package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Esikler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Etkinlik;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Hissedilen;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.IsiAlisverisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Islaklik;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Katki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.OrtamSicakligi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikDurumu;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SolEl;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Yalitim;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.YiyecekKayitlari;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sunucu tarafı: her oyuncunun vücut sıcaklığını ayrı ayrı, varsayılan saniyede bir günceller (oyuncular tick'lere yayılır).
 * Akış (§A2–A5): ıslaklık adımı → mekan sıcaklığı → hissedilen (ıslaklık, rüzgâr, sol el) → vücut adımı → etkiler
 * (yavaşlama, hasar, bunalma/yanma açlığı, gösterge efektleri, Buz Kovası erimesi, diğer oyunculara parçacık) ve HUD
 * paketini yalnız o oyuncuya yollar. Kapı kapalıyken hiçbir şey yapmaz; kapanışta HUD'u gizleyen tek bir paket gider ve
 * yavaşlama kaldırılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class SicaklikYoneticisi {
    static final UUID YAVASLAMA = UUID.fromString("6f1b4d2e-35a1-4c7e-9b0e-5d7a2c9e1f40");
    private static final String YAVASLAMA_ADI = "dogalafetler sicaklik yavaslama";
    /** Bu kadar yatay yol (blok/güncelleme) yürüme sayılır. */
    private static final double YURUME_ESIGI = 0.5;
    /** Gösterge efektlerinin süresi (tick); her güncellemede tazelenir. */
    static final int GOSTERGE_SURESI = 60;
    /** Sol eldeki Buz Kovasının erime sayacı (eşya NBT'si, {@link Hissedilen#ERIME_SINIRI}'nda su kovasına döner). */
    public static final String ERIME_ETIKETI = "dogalafetler.erime";

    /** Oyuncunun kalıcı olmayan sunucu durumu (son konum, son gönderilen paket). */
    private static final class Gecici {
        Vec3 sonKonum;
        boolean hudAcik;
        float sonVucut = Float.NaN;
        float sonOrtam;
        float sonMekan;
        float sonHissedilen;
        float sonIslaklik;
        SolEl sonSolEl;
        Kademe sonKademe;
        long sonGonderim;
        int buharSayaci;
    }

    /**
     * Bir oyuncunun o anki ısı durumu ({@code bilgi} komutu, menü dökümü ve güncelleme aynı hesabı kullanır).
     *
     * @param girdi       vücut adımının girdisi (ortam = hissedilen °C)
     * @param ortamBirim  hissedilenin vücut birimi karşılığı
     * @param yalitim     toplam yalıtım
     * @param kurumaHizi  saniyede kuruma (ıslanırken 0)
     */
    public record Analiz(OrtamSicakligi.Sonuc mekan, Hissedilen.Sonuc hissedilen, IsiAlisverisi.Girdi girdi,
            double egilim, double ortamBirim, double yalitim, double kurumaHizi) {
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

    /** Analiz: mekan, hissedilen, yalıtım, etkinlik, ıslaklık ve eğilim (oyuncunun durumunu değiştirmez). */
    public static Analiz analiz(ServerPlayer oyuncu) {
        SicaklikAyarlari.Ozet ozet = SicaklikAyarlari.al();
        OrtamSicakligi.Girdi og = OrtamOlcer.girdi(oyuncu, ozet);
        return analiz(oyuncu, ozet, og, OrtamSicakligi.hesapla(og, ozet.ortam()), VucutSicakligi.islaklik(oyuncu));
    }

    private static Analiz analiz(ServerPlayer oyuncu, SicaklikAyarlari.Ozet ozet, OrtamSicakligi.Girdi og,
            OrtamSicakligi.Sonuc mekan, double islaklik) {
        Hissedilen.Sonuc his = Hissedilen.hesapla(
                OrtamOlcer.hissedilenGirdisi(oyuncu, og, mekan.toplam(), islaklik, solEl(oyuncu)), ozet.hissedilen());
        Yalitim.Girdi yalitim = ZirhEkleri.yalitimGirdisi(oyuncu);
        IsiAlisverisi.Girdi girdi = new IsiAlisverisi.Girdi(VucutSicakligi.al(oyuncu), his.toplam(), etkinlik(oyuncu),
                islaklik, og.suda(), yalitim);
        double kuruma = OrtamOlcer.islaklikKaynagi(oyuncu, og) == Islaklik.Kaynak.KURU
                ? Islaklik.kurumaHizi(mekan.toplam(), mekan.katki(Katki.KAYNAKLAR), ozet.islaklik()) : 0;
        return new Analiz(mekan, his, girdi, IsiAlisverisi.egilim(girdi, ozet.isi(), ozet.yalitim()),
                ozet.isi().esdeger(his.toplam()), Yalitim.toplam(yalitim, ozet.yalitim()), kuruma);
    }

    /**
     * Oyuncunun ıslaklığını ve vücut sıcaklığını {@code tick} kadar ilerletir, etkileri uygular, gerekirse paket yollar. Kapı
     * kontrolü çağıranındır (GameTest'ler sahte oyuncular tick almadığı için bunu doğrudan çağırır).
     */
    public static void guncelle(ServerPlayer oyuncu, int tick) {
        if (oyuncu.isSpectator() || !oyuncu.isAlive()) {
            return;
        }
        SicaklikAyarlari.Ozet ozet = SicaklikAyarlari.al();
        double saniye = tick / 20.0;
        OrtamSicakligi.Girdi og = OrtamOlcer.girdi(oyuncu, ozet);
        OrtamSicakligi.Sonuc mekan = OrtamSicakligi.hesapla(og, ozet.ortam());
        double islaklik = Islaklik.adim(VucutSicakligi.islaklik(oyuncu), OrtamOlcer.islaklikKaynagi(oyuncu, og),
                oyuncu.isOnFire(), mekan.toplam(), mekan.katki(Katki.KAYNAKLAR), saniye, ozet.islaklik());
        VucutSicakligi.islaklikYaz(oyuncu, islaklik);
        Analiz a = analiz(oyuncu, ozet, og, mekan, VucutSicakligi.islaklik(oyuncu));
        Gecici g = gecici(oyuncu);
        g.sonKonum = oyuncu.position();
        double yeni = IsiAlisverisi.adim(a.girdi(), saniye, ozet.isi(), ozet.yalitim())
                + VucutSicakligi.etkileriIlerlet(oyuncu, tick);
        VucutSicakligi.yaz(oyuncu, yeni);
        double vucut = VucutSicakligi.al(oyuncu);
        etkileriUygula(oyuncu, vucut, tick, ozet.esikler());
        gostergeleriUygula(oyuncu, a);
        buzKovasiniErit(oyuncu, a.mekan().toplam(), saniye, ozet.hissedilen());
        parcaciklariGonder(oyuncu, g, ozet.esikler().kademe(vucut).durum(), a.girdi().islaklik());
        gonderGerekirse(oyuncu, g, vucut, a, ozet.esikler(), false);
    }

    /** Sol eldeki sıcaklık eşyası (ana eldeki sayılmaz). */
    public static SolEl solEl(ServerPlayer oyuncu) {
        ItemStack sol = oyuncu.getOffhandItem();
        if (sol.is(Items.LAVA_BUCKET)) {
            return SolEl.LAV_KOVASI;
        }
        return sol.is(YiyecekKayitlari.BUZ_KOVASI.get()) ? SolEl.BUZ_KOVASI : SolEl.YOK;
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

    private static void etkileriUygula(ServerPlayer oyuncu, double vucut, int tick, Esikler e) {
        yavaslamaAyarla(oyuncu, e.yavaslama(vucut));
        double aclik = e.aclik(vucut) * tick / 20.0;
        if (aclik > 0) {
            oyuncu.causeFoodExhaustion((float) aclik);
        }
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

    /** Islak ve sol el gösterge efektleri (yalnız ikon; her güncellemede {@value #GOSTERGE_SURESI} tick tazelenir). */
    private static void gostergeleriUygula(ServerPlayer oyuncu, Analiz a) {
        MobEffect islak = SicaklikKayitlari.ISLAK.get();
        if (Islaklik.gorunur(a.girdi().islaklik())) {
            gosterge(oyuncu, islak);
        } else if (oyuncu.hasEffect(islak)) {
            oyuncu.removeEffect(islak);
        }
        switch (a.hissedilen().solEl()) {
            case BUZ_KOVASI -> gosterge(oyuncu, YiyecekKayitlari.SERINLIK.get());
            case LAV_KOVASI -> gosterge(oyuncu, YiyecekKayitlari.ICTEN_ISINMA.get());
            case YOK -> {
            }
        }
    }

    /** Yiyecekten gelen daha güçlü ya da uzun efekti vanilla birleştirmesi korur. */
    private static void gosterge(ServerPlayer oyuncu, MobEffect efekt) {
        oyuncu.addEffect(new MobEffectInstance(efekt, GOSTERGE_SURESI, 0, false, false, true));
    }

    /** Sol eldeki Buz Kovası erime eşiğinin üstünde erir; sınıra varınca su kovasına döner (S3-05). */
    private static void buzKovasiniErit(ServerPlayer oyuncu, double mekanC, double saniye, Hissedilen.Ayarlar a) {
        ItemStack sol = oyuncu.getOffhandItem();
        double artis = Hissedilen.erimeArtisi(mekanC, a) * saniye;
        if (artis <= 0 || !sol.is(YiyecekKayitlari.BUZ_KOVASI.get())) {
            return;
        }
        double erime = sol.getOrCreateTag().getDouble(ERIME_ETIKETI) + artis;
        if (erime >= Hissedilen.ERIME_SINIRI) {
            oyuncu.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.WATER_BUCKET));
        } else {
            sol.getOrCreateTag().putDouble(ERIME_ETIKETI, erime);
        }
    }

    /**
     * Diğer oyunculara durum parçacıkları (S-08 rev.: kendisine gitmez, üçüncü şahısta istemci kendisi üretir): üşüme ve
     * donmada 2–3 güncellemede bir ağızdan nefes buharı (üst dünyada dünya donması sürerken onun nefesi zaten
     * çizilir), bunalma ve yanmada baş/omuz çevresinde ter damlası, ıslakken damlayan su.
     */
    private static void parcaciklariGonder(ServerPlayer oyuncu, Gecici g, SicaklikDurumu durum, double islaklik) {
        ServerLevel level = oyuncu.serverLevel();
        boolean buhar = false;
        boolean donmaNefesi = level.dimension() == Level.OVERWORLD && DunyaDonemi.al(level).donmaIlerlemesi() > 0;
        if (durum.soguk() && !donmaNefesi) {
            g.buharSayaci++;
            if (g.buharSayaci >= 2 + oyuncu.getRandom().nextInt(2)) {
                g.buharSayaci = 0;
                buhar = true;
            }
        } else {
            g.buharSayaci = 0;
        }
        boolean ter = durum.sicak();
        boolean damla = Islaklik.gorunur(islaklik);
        if (!buhar && !ter && !damla) {
            return;
        }
        Vec3 bakis = oyuncu.getLookAngle();
        Vec3 agiz = oyuncu.getEyePosition().add(bakis.scale(0.35)).add(0, -0.12, 0);
        int damlaSayisi = 1 + (int) (islaklik * 3);
        int terSayisi = durum == SicaklikDurumu.YANMA ? 6 : 4;
        for (ServerPlayer izleyen : level.players()) {
            if (izleyen == oyuncu) {
                continue;
            }
            if (buhar) {
                level.sendParticles(izleyen, SicaklikParcaciklari.NEFES_BUHARI.get(), false, agiz.x, agiz.y, agiz.z, 0,
                        bakis.x, bakis.y, bakis.z, 0.04);
            }
            if (ter) {
                level.sendParticles(izleyen, SicaklikParcaciklari.TER_DAMLASI.get(), false, oyuncu.getX(),
                        oyuncu.getY() + oyuncu.getBbHeight() * 0.8, oyuncu.getZ(), terSayisi, 0.35, 0.2, 0.35, 0);
            }
            if (damla) {
                level.sendParticles(izleyen, ParticleTypes.FALLING_WATER, false, oyuncu.getX(),
                        oyuncu.getY() + oyuncu.getBbHeight() * 0.5, oyuncu.getZ(), damlaSayisi, 0.3, 0.5, 0.3, 0);
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
        float mekan = (float) a.mekan().toplam();
        float his = (float) a.hissedilen().toplam();
        float w = (float) a.girdi().islaklik();
        SolEl el = a.hissedilen().solEl();
        Kademe kademe = e.kademe(vucut);
        boolean gerekli = zorla || !g.hudAcik || kademe != g.sonKademe || el != g.sonSolEl || Float.isNaN(g.sonVucut)
                || Math.abs(v - g.sonVucut) >= 0.25f || Math.abs(o - g.sonOrtam) >= 0.5f
                || Math.abs(mekan - g.sonMekan) >= 0.5f || Math.abs(his - g.sonHissedilen) >= 0.5f
                || Math.abs(w - g.sonIslaklik) >= 0.02f
                || simdi - g.sonGonderim >= SicaklikAyarlari.paketTazelemeTick();
        if (!gerekli) {
            return;
        }
        AgKanali.oyuncuyaGonder(oyuncu, SicaklikDurumPaketi.acik(v, o, mekan, his, (float) a.egilim(), w, el, e));
        g.hudAcik = true;
        g.sonVucut = v;
        g.sonOrtam = o;
        g.sonMekan = mekan;
        g.sonHissedilen = his;
        g.sonIslaklik = w;
        g.sonSolEl = el;
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
        g.sonSolEl = null;
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

    /** Madde 3: respawn'da vücut sıcaklığı, etkiler ve ıslaklık sıfırlanır. */
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
        oyuncu.removeEffect(SicaklikKayitlari.ISLAK.get());
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
