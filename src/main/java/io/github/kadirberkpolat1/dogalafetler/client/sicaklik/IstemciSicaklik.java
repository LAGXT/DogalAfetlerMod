package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikParcaciklari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikDurumu;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sunucudan gelen sıcaklık durumunun istemci önbelleği (§A6). Paketler saniyede bir gelir; HUD ve efektler ani sıçramasın
 * diye gösterilen vücut, hissedilen eşdeğeri, hissedilen °C ve ıslaklık her istemci tick'inde hedefe yumuşakça yaklaşır ve
 * kare arasında {@code partialTick} ile ara değerlenir. Durum (üşüme, donma, bunalma, yanma) değiştiği tick HUD
 * parlaması için saklanır. Çıkışta sıfırlanır.
 * <p>
 * Oyuncunun kendi parçacıkları (sunucu bunları oyuncunun kendisine göndermez, S-08 rev.) yalnız kamera birinci şahıs
 * değilken üretilir: üşüme ve donmada ~50 tick'te bir nefes (4–6 buhar bulutçuğu; dünya donması sürerken üretilmez, onun
 * nefesi zaten çizilir), bunalma ve yanmada ter damlası, ıslakken düşen su damlası, hasarlı kademede alev ya da kar tanesi.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciSicaklik {
    private static final float YAKLASMA = 0.25f;
    /** Hasarlı kademe alev/kar tanesi aralığı (tick): sunucunun varsayılan güncelleme aralığı. */
    private static final int PARCACIK_ARALIGI = 20;
    /** Nefes döngüsü (tick); her nefeste 2 tick arayla 4–6 bulutçuk. */
    static final int NEFES_DONGUSU = 50;

    private static SicaklikDurumPaketi durum = SicaklikDurumPaketi.KAPALI;
    private static float oncekiVucut;
    private static float vucut;
    private static float oncekiOrtam;
    private static float ortam;
    private static float oncekiHissedilen;
    private static float hissedilen;
    private static float oncekiIslaklik;
    private static float islaklik;
    private static boolean ilk = true;
    private static long tick;
    private static SicaklikDurumu sonDurum = SicaklikDurumu.NORMAL;
    private static long degisimTick = -1;
    private static int nefesKalan;
    /** Görsel kanıt: kendi üretilen parçacık sayısı ve son nefes/ter tick'i (otomasyon log'a yazar). */
    private static int kendiParcacik;
    private static long sonNefes = -1;
    private static long sonTer = -1;
    /** Görsel kanıt: son {@value #TER_HALKASI} ter damlasının tick'i (halka). */
    private static final int TER_HALKASI = 8;
    private static final long[] TER_TICKLERI = new long[TER_HALKASI];
    private static int terSirasi;

    private IstemciSicaklik() {
    }

    static void guncelle(SicaklikDurumPaketi paket) {
        SicaklikDurumu yeni = paket.kademe().durum();
        if (!ilk && paket.acik() && yeni != sonDurum) {
            degisimTick = tick;
        }
        sonDurum = yeni;
        durum = paket;
        if (ilk || !paket.acik()) {
            vucut = oncekiVucut = paket.vucut();
            ortam = oncekiOrtam = paket.ortamBirim();
            hissedilen = oncekiHissedilen = paket.hissedilen();
            islaklik = oncekiIslaklik = paket.islaklik();
            ilk = !paket.acik();
        }
    }

    public static SicaklikDurumPaketi durum() {
        return durum;
    }

    public static boolean acik() {
        return durum.acik();
    }

    /** Paketteki kademenin oyuncuya görünen durumu. */
    public static SicaklikDurumu sicaklikDurumu() {
        return durum.kademe().durum();
    }

    /** Gösterilen (yumuşatılmış) vücut sıcaklığı, vücut birimi. */
    public static float vucut(float partialTick) {
        return Mth.lerp(partialTick, oncekiVucut, vucut);
    }

    /** Hissedilen sıcaklığın vücut birimi karşılığı (HUD üçgeni). */
    public static float ortam(float partialTick) {
        return Mth.lerp(partialTick, oncekiOrtam, ortam);
    }

    /** Hissedilen sıcaklık, °C. */
    public static float hissedilen(float partialTick) {
        return Mth.lerp(partialTick, oncekiHissedilen, hissedilen);
    }

    /** Islaklık 0–1. */
    public static float islaklik(float partialTick) {
        return Mth.lerp(partialTick, oncekiIslaklik, islaklik);
    }

    /** Durumun son değiştiği istemci tick'i; hiç değişmediyse -1. */
    public static long durumDegisimi() {
        return degisimTick;
    }

    /** İstemci tick sayacı (titreşim ve yanıp sönme zamanlaması). */
    public static long tick() {
        return tick;
    }

    static int kendiParcacikSayisi() {
        return kendiParcacik;
    }

    static long sonNefesTick() {
        return sonNefes;
    }

    static long sonTerTick() {
        return sonTer;
    }

    /** Son {@code pencere} tick içinde üretilen kendi ter damlası sayısı (en çok {@value #TER_HALKASI}). */
    static int sonTerSayisi(long pencere) {
        int n = 0;
        for (int i = 0; i < Math.min(terSirasi, TER_HALKASI); i++) {
            if (tick - TER_TICKLERI[i] <= pencere) {
                n++;
            }
        }
        return n;
    }

    @SubscribeEvent
    public static void istemciTick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        tick++;
        oncekiVucut = vucut;
        oncekiOrtam = ortam;
        oncekiHissedilen = hissedilen;
        oncekiIslaklik = islaklik;
        vucut += (durum.vucut() - vucut) * YAKLASMA;
        ortam += (durum.ortamBirim() - ortam) * YAKLASMA;
        hissedilen += (durum.hissedilen() - hissedilen) * YAKLASMA;
        islaklik += (durum.islaklik() - islaklik) * YAKLASMA;
        kendiParcaciklari(Minecraft.getInstance());
    }

    /** Dünya donması (üst dünyada G > 0) kendi nefesini herkese çiziyor; çift buhar olmasın. */
    private static boolean dunyaDonuyor(Minecraft mc) {
        return mc.level.dimension() == Level.OVERWORLD && IstemciDonem.durum().donmaIlerlemesi() > 0f;
    }

    private static void kendiParcaciklari(Minecraft mc) {
        LocalPlayer o = mc.player;
        if (o == null || mc.level == null || mc.isPaused() || o.isSpectator() || !durum.acik()) {
            nefesKalan = 0;
            return;
        }
        // Birinci şahısta göğüsteki ya da ağızdaki sprite ekranda dev bir leke olur.
        boolean ucuncu = mc.options.getCameraType() != CameraType.FIRST_PERSON;
        SicaklikDurumu sd = durum.kademe().durum();
        RandomSource r = o.getRandom();
        if ((sd == SicaklikDurumu.USUME || sd == SicaklikDurumu.DONMA) && !dunyaDonuyor(mc)) {
            if (tick % NEFES_DONGUSU == 0) {
                nefesKalan = 4 + r.nextInt(3);
            }
        } else {
            nefesKalan = 0;
        }
        if (nefesKalan > 0 && tick % 2 == 0) {
            nefesKalan--;
            if (ucuncu) {
                nefes(mc, o, r);
            }
        }
        if (!ucuncu) {
            return;
        }
        if (sd == SicaklikDurumu.BUNALMA || sd == SicaklikDurumu.YANMA) {
            float olasilik = sd == SicaklikDurumu.YANMA ? 0.4f : 0.2f;
            if (r.nextFloat() < olasilik) {
                ter(mc, o, r);
            }
        }
        float w = durum.islaklik();
        if (w >= 0.05f && r.nextFloat() < 0.25f * w) {
            mc.level.addParticle(ParticleTypes.FALLING_WATER, o.getX() + (r.nextDouble() - 0.5) * 0.6,
                    o.getY() + 0.2 + r.nextDouble() * 1.5, o.getZ() + (r.nextDouble() - 0.5) * 0.6, 0, 0, 0);
            kendiParcacik++;
        }
        if (tick % PARCACIK_ARALIGI == 0 && durum.kademe().hasarli()) {
            hasarParcaciklari(mc, o, r);
        }
    }

    private static void nefes(Minecraft mc, LocalPlayer o, RandomSource r) {
        Vec3 bak = o.getViewVector(1f);
        Vec3 agiz = o.getEyePosition().add(bak.scale(0.3)).add(0, -0.12, 0);
        mc.level.addParticle(SicaklikParcaciklari.NEFES_BUHARI.get(), agiz.x, agiz.y, agiz.z,
                bak.x * 0.06 + (r.nextDouble() - 0.5) * 0.012, bak.y * 0.04 + 0.004,
                bak.z * 0.06 + (r.nextDouble() - 0.5) * 0.012);
        kendiParcacik++;
        sonNefes = tick;
    }

    /** Baş ve omuz hizasından, gövde silüetinin hemen dışına (3. şahısta model damlayı örtmesin). */
    private static void ter(Minecraft mc, LocalPlayer o, RandomSource r) {
        double aci = r.nextDouble() * Math.PI * 2;
        double yaricap = 0.3 + r.nextDouble() * 0.12;
        mc.level.addParticle(SicaklikParcaciklari.TER_DAMLASI.get(), o.getX() + Math.cos(aci) * yaricap,
                o.getY() + 1.25 + r.nextDouble() * 0.5, o.getZ() + Math.sin(aci) * yaricap, Math.cos(aci) * 0.01, 0,
                Math.sin(aci) * 0.01);
        kendiParcacik++;
        sonTer = tick;
        TER_TICKLERI[terSirasi++ % TER_HALKASI] = tick;
    }

    /** Hasarlı kademede sunucunun diğer oyunculara gönderdiğiyle aynı sayı ve saçılımda alev ya da kar tanesi. */
    private static void hasarParcaciklari(Minecraft mc, LocalPlayer o, RandomSource r) {
        boolean donma = durum.kademe() == Kademe.DONUYOR;
        ParticleOptions tur = donma ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME;
        for (int i = donma ? 6 : 4; i > 0; i--) {
            mc.level.addParticle(tur, o.getX() + r.nextGaussian() * 0.3, o.getY() + 1.0 + r.nextGaussian() * 0.5,
                    o.getZ() + r.nextGaussian() * 0.3, r.nextGaussian() * 0.01, r.nextGaussian() * 0.01,
                    r.nextGaussian() * 0.01);
            kendiParcacik++;
        }
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        durum = SicaklikDurumPaketi.KAPALI;
        ilk = true;
        vucut = oncekiVucut = ortam = oncekiOrtam = 0;
        hissedilen = oncekiHissedilen = islaklik = oncekiIslaklik = 0;
        sonDurum = SicaklikDurumu.NORMAL;
        degisimTick = -1;
        nefesKalan = 0;
    }
}
