package io.github.kadirberkpolat1.dogalafetler.client.asit_yagmuru;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.AsitKayitlari;
import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Asit yağmurunun istemci durumu ve tick efektleri (tasarım notu §2, §4, §5). Durum çekirdeğin {@code AfetDurumPaketi}'nden
 * okunur (merkez, yarıçap, aşama; {@code ozel.y} yoğunluk, {@code ozel.a} aşınan bloklar) ve 1 Hz paket arasında yumuşatılır.
 * SIREN'de bulut tavanı ve sis toplanır, AKTIF'te yağmur ({@link AsitYagmuruCizici}), sıçrama, cızlama dumancığı, yüzey
 * buharı ve vanilla yağmur sesi gelir. Parçacıklar vanilla'nın 32 blok kesimini atlayarak ({@code force}) eklenir; sayıyı
 * efekt kalitesi ve vanilla parçacık ayarı sınırlar.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciAsitYagmuru {
    private static final double[] VANILLA_AYAR_CARPANI = {1.0, 0.5, 0.2};
    /** Gündüz ve gece sis rengi (tasarım §2: {@code #9AA35A}, {@code #2F3420}). */
    private static final float[] GUNDUZ = {0x9A / 255f, 0xA3 / 255f, 0x5A / 255f};
    private static final float[] GECE = {0x2F / 255f, 0x34 / 255f, 0x20 / 255f};
    private static final double BULUT_MENZILI = 96;
    private static final double ASINMA_MENZILI = 48;
    private static final int SIREN_TICK = 200;

    private static boolean aktif;
    private static double x;
    private static double z;
    private static double yaricap;
    private static double yogunluk;
    private static double bulut;
    private static long[] asinan = new long[0];
    private static long afetNo;
    private static EfektKalitesi kaliteZorlamasi;
    private static int sesSayaci;
    private static float oncekiCan = -1;

    private IstemciAsitYagmuru() {
    }

    /** Yağmur yoğunluğu 0–1 (çizici ve ekran efekti). */
    static double yogunluk() {
        return aktif ? yogunluk : 0;
    }

    static boolean aktif() {
        return aktif;
    }

    /** Yağmurun bu XZ'deki alan ağırlığı 0–1 (kenarda 16 blokta söner). */
    static double alanAgirligi(double px, double pz) {
        return aktif ? AsitKurallari.alanAgirligi(Math.hypot(px - x, pz - z), yaricap) : 0;
    }

    static Kademe kademe() {
        return Kademe.al(kaliteZorlamasi != null ? kaliteZorlamasi : IstemciAyarlari.efektKalitesi());
    }

    /** Görsel kanıt otomasyonu için: {@code null} config'teki kaliteye döner. */
    static void kaliteZorla(EfektKalitesi kalite) {
        kaliteZorlamasi = kalite;
    }

    /** Oyuncunun başı gökyüzüne açık mı (sunucudaki hasar denetimiyle aynı kural). */
    static boolean acikta(ClientLevel level, double px, double gozY, double pz) {
        return AsitKurallari.acikta(level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(px), Mth.floor(pz)), gozY);
    }

    private static void sifirla() {
        aktif = false;
        yogunluk = 0;
        bulut = 0;
        asinan = new long[0];
        AsitParcacigi.BULUTLAR.clear();
        oncekiCan = -1;
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        sifirla();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            if (aktif) {
                sifirla();
            }
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        AsitParcacigi.BULUTLAR.removeIf(p -> !p.isAlive());
        if (!durumuOku(level)) {
            return;
        }
        Camera kamera = mc.gameRenderer.getMainCamera();
        Vec3 goz = kamera.getPosition();
        Kademe k = kademe();
        double ayar = VANILLA_AYAR_CARPANI[mc.options.particles().get().getId()];
        RandomSource r = level.random;
        bulutTavani(level, goz, k, ayar, r);
        if (yogunluk > 0.01 && AsitKurallari.sisAgirligi(Math.hypot(goz.x - x, goz.z - z), yaricap) > 0) {
            damlalar(level, goz, k, ayar, r);
            buhar(level, goz, k, ayar, r);
            sesler(level, goz, r);
        }
        asinanlar(level, goz, ayar, r);
        hasarIzle(mc.player, level);
    }

    /** Paketten durumu okur, 1 Hz arasında yumuşatır; bu boyutta asit yağmuru yoksa sıfırlar. */
    private static boolean durumuOku(ClientLevel level) {
        Optional<AfetDurumPaketi> paket = IstemciAfetDurumu.aktif().filter(p -> p.tur() == AfetTuru.ASIT_YAGMURU
                && p.boyut().equals(level.dimension().location())
                && (p.asama() == AfetAsamasi.SIREN || p.asama() == AfetAsamasi.AKTIF));
        if (paket.isEmpty()) {
            if (aktif) {
                sifirla();
            }
            return false;
        }
        AfetDurumPaketi p = paket.get();
        double hedefX = p.merkez().getX() + 0.5;
        double hedefZ = p.merkez().getZ() + 0.5;
        if (!aktif || p.afetNo() != afetNo) {
            x = hedefX;
            z = hedefZ;
            afetNo = p.afetNo();
            aktif = true;
        }
        x += (hedefX - x) * 0.05;
        z += (hedefZ - z) * 0.05;
        yaricap = p.yaricap();
        double hedefYogunluk = p.asama() == AfetAsamasi.AKTIF ? p.ozel().getFloat("y") : 0;
        double sirenIlerleme = p.asama() == AfetAsamasi.SIREN && p.kalanTick() >= 0
                ? 1.0 - Math.min(1.0, p.kalanTick() / (double) SIREN_TICK) : 1.0;
        double hedefBulut = p.asama() == AfetAsamasi.SIREN ? 0.7 * sirenIlerleme : Math.max(0.7, hedefYogunluk);
        yogunluk += Mth.clamp(hedefYogunluk - yogunluk, -0.02, 0.02);
        bulut += Mth.clamp(hedefBulut - bulut, -0.01, 0.01);
        asinan = p.ozel().getLongArray("a");
        return true;
    }

    /** Alanın üstünde, bulut yüksekliğinde (kameraya en çok 60 blok yukarıda) rüzgârla sürüklenen koyu bulut tavanı. */
    private static void bulutTavani(ClientLevel level, Vec3 goz, Kademe k, double ayar, RandomSource r) {
        int tavan = (int) Math.round(k.bulut * bulut * ayar);
        if (AsitParcacigi.BULUTLAR.size() >= tavan || r.nextDouble() > 0.35) {
            return;
        }
        float bulutY = level.effects().getCloudHeight();
        double y = Double.isNaN(bulutY) ? goz.y + 40 : Math.min(bulutY, goz.y + 60);
        double aci = r.nextDouble() * Math.PI * 2;
        double uzaklik = Math.sqrt(r.nextDouble()) * BULUT_MENZILI;
        double px = goz.x + Math.cos(aci) * uzaklik;
        double pz = goz.z + Math.sin(aci) * uzaklik;
        if (AsitKurallari.sisAgirligi(Math.hypot(px - x, pz - z), yaricap) < r.nextDouble()) {
            return;
        }
        double[] ruzgar = YanginMantigi.gununRuzgari(level.getGameTime() / 24000L);
        ekle(level, AsitKayitlari.BULUT.get(), px, y + r.nextGaussian() * 3, pz, ruzgar[0] * 0.03, 0, ruzgar[1] * 0.03);
    }

    /** Yağmur yarıçapı içinde zemine çarpan damlaların sıçraması ve olasılıkla cızlama dumancığı. */
    private static void damlalar(ClientLevel level, Vec3 goz, Kademe k, double ayar, RandomSource r) {
        int sayi = (int) Math.round(k.sicrama * yogunluk * ayar);
        double menzil = k.yagmurYaricapi + 2;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < sayi; i++) {
            double px = goz.x + (r.nextDouble() * 2 - 1) * menzil;
            double pz = goz.z + (r.nextDouble() * 2 - 1) * menzil;
            if (alanAgirligi(px, pz) < r.nextDouble()) {
                continue;
            }
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(px), Mth.floor(pz));
            if (Math.abs(h - goz.y) > 24) {
                continue;
            }
            p.set(px, h - 1, pz);
            VoxelShape sekil = level.getBlockState(p).getShape(level, p);
            double ust = sekil.isEmpty() ? h : h - 1 + sekil.max(Direction.Axis.Y);
            ekle(level, AsitKayitlari.SICRAMA.get(), px, ust + 0.02, pz, 0, 0.12 + r.nextDouble() * 0.06, 0);
            if (r.nextDouble() < k.dumanOlasiligi) {
                ekle(level, AsitKayitlari.DUMAN.get(), px, ust + 0.1, pz, 0, 0.03 + r.nextDouble() * 0.02, 0);
            }
        }
    }

    /** Katı yüzeylerde zeminin 0,3 blok üstünde sürünen hafif buhar (Orta ve üstü). */
    private static void buhar(ClientLevel level, Vec3 goz, Kademe k, double ayar, RandomSource r) {
        if (k.buharYaricapi <= 0 || r.nextDouble() > 0.6 * yogunluk * ayar) {
            return;
        }
        double px = goz.x + (r.nextDouble() * 2 - 1) * k.buharYaricapi;
        double pz = goz.z + (r.nextDouble() * 2 - 1) * k.buharYaricapi;
        if (alanAgirligi(px, pz) < r.nextDouble()) {
            return;
        }
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(px), Mth.floor(pz));
        double[] ruzgar = YanginMantigi.gununRuzgari(level.getGameTime() / 24000L);
        ekle(level, AsitKayitlari.BUHAR.get(), px, h + 0.3, pz, ruzgar[0] * 0.01, 0.004, ruzgar[1] * 0.01);
    }

    /** Aşınan bloklardan yoğun cızlama dumanları: bloğun "eridiği" okunur. */
    private static void asinanlar(ClientLevel level, Vec3 goz, double ayar, RandomSource r) {
        for (long a : asinan) {
            double bx = BlockPos.getX(a) + 0.5;
            double by = BlockPos.getY(a) + 1.0;
            double bz = BlockPos.getZ(a) + 0.5;
            if (goz.distanceToSqr(bx, by, bz) > ASINMA_MENZILI * ASINMA_MENZILI || r.nextDouble() > 0.8 * ayar) {
                continue;
            }
            ekle(level, AsitKayitlari.DUMAN.get(), bx + (r.nextDouble() - 0.5) * 0.9, by, bz + (r.nextDouble() - 0.5) * 0.9,
                    0, 0.04 + r.nextDouble() * 0.03, 0);
        }
    }

    /**
     * Vanilla yağmur sesi (perde 0,9; çatı altında boğuk {@code weather.rain.above}) ve seyrek, kısık, tiz cızlama
     * ({@code block.fire.extinguish}, perde 1,8–2,0). Uzak gök gürültüsü yok (tasarım §4).
     */
    private static void sesler(ClientLevel level, Vec3 goz, RandomSource r) {
        if (--sesSayaci > 0 || yogunluk < 0.1) {
            return;
        }
        sesSayaci = 3 + r.nextInt(3);
        double px = goz.x + r.nextInt(21) - 10;
        double pz = goz.z + r.nextInt(21) - 10;
        if (alanAgirligi(px, pz) <= 0) {
            return;
        }
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(px), Mth.floor(pz));
        if (h > goz.y + 1 && !acikta(level, goz.x, goz.y, goz.z)) {
            level.playLocalSound(px, h, pz, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, (float) (0.1 * yogunluk),
                    0.45F, false);
        } else {
            level.playLocalSound(px, h, pz, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, (float) (0.2 * yogunluk), 0.9F,
                    false);
        }
        if (r.nextInt(3) == 0) {
            level.playLocalSound(px, h, pz, SoundEvents.FIRE_EXTINGUISH, SoundSource.WEATHER, (float) (0.05 * yogunluk),
                    1.8F + r.nextFloat() * 0.2F, false);
        }
    }

    /** Asit canını azalttığı an (açıkta, AKTIF alanda) ekran efektini tetikler. */
    private static void hasarIzle(LocalPlayer oyuncu, ClientLevel level) {
        if (oyuncu == null) {
            return;
        }
        float can = oyuncu.getHealth();
        if (oncekiCan >= 0 && can < oncekiCan && yogunluk >= AsitKurallari.ETKI_ESIGI
                && alanAgirligi(oyuncu.getX(), oyuncu.getZ()) > 0 && acikta(level, oyuncu.getX(), oyuncu.getEyeY(), oyuncu.getZ())) {
            AsitEkranEfekti.tetikle();
        }
        oncekiCan = can;
    }

    private static void ekle(ClientLevel level, ParticleOptions tur, double px, double py, double pz, double vx, double vy,
            double vz) {
        level.addParticle(tur, true, px, py, pz, vx, vy, vz);
    }

    /** Sarı-yeşil sis: alan içinde tam, 1,5R'de sıfır; gündüz ve gece rengi güneş açısıyla karışır. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void sisRengi(ViewportEvent.ComputeFogColor olay) {
        double w = sisGucu(olay.getCamera());
        if (w <= 0) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        float aci = level.getTimeOfDay((float) olay.getPartialTick());
        float gun = Mth.clamp(Mth.cos(aci * Mth.TWO_PI) * 2.0F + 0.5F, 0.0F, 1.0F);
        float k = (float) (0.8 * w);
        olay.setRed(Mth.lerp(k, olay.getRed(), Mth.lerp(gun, GECE[0], GUNDUZ[0])));
        olay.setGreen(Mth.lerp(k, olay.getGreen(), Mth.lerp(gun, GECE[1], GUNDUZ[1])));
        olay.setBlue(Mth.lerp(k, olay.getBlue(), Mth.lerp(gun, GECE[2], GUNDUZ[2])));
    }

    /** Yağmur perdesi: görüş mesafesi yoğunlukla %40'a kadar kısalır (Forge değişikliği yalnız iptal edilen olayda uygular). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void sis(ViewportEvent.RenderFog olay) {
        double w = sisGucu(olay.getCamera());
        if (w <= 0 || olay.isCanceled()) {
            return;
        }
        float kisalma = (float) (0.4 * w);
        olay.setFarPlaneDistance(olay.getFarPlaneDistance() * (1 - kisalma));
        olay.setNearPlaneDistance(olay.getNearPlaneDistance() * (1 - 1.5F * kisalma));
        olay.setCanceled(true);
    }

    private static double sisGucu(Camera kamera) {
        if (!aktif || Minecraft.getInstance().level == null) {
            return 0;
        }
        Vec3 goz = kamera.getPosition();
        return AsitKurallari.sisAgirligi(Math.hypot(goz.x - x, goz.z - z), yaricap) * Mth.clamp(bulut, 0, 1);
    }

    /** Tasarım §5 tablosu: yağmur yarıçapı, sıçrama/tick, cızlama olasılığı, bulut tavanı, yüzey buharı yarıçapı. */
    enum Kademe {
        DUSUK(5, 20, 0.08, 30, 0),
        ORTA(10, 50, 0.15, 60, 8),
        YUKSEK(10, 100, 0.25, 120, 16),
        ULTRA(15, 180, 0.35, 200, 24);

        final int yagmurYaricapi;
        final int sicrama;
        final double dumanOlasiligi;
        final int bulut;
        final int buharYaricapi;

        Kademe(int yagmurYaricapi, int sicrama, double dumanOlasiligi, int bulut, int buharYaricapi) {
            this.yagmurYaricapi = yagmurYaricapi;
            this.sicrama = sicrama;
            this.dumanOlasiligi = dumanOlasiligi;
            this.bulut = bulut;
            this.buharYaricapi = buharYaricapi;
        }

        static Kademe al(EfektKalitesi kalite) {
            return switch (kalite) {
                case DUSUK -> DUSUK;
                case ORTA -> ORTA;
                case YUKSEK -> YUKSEK;
                case ULTRA -> ULTRA;
            };
        }
    }
}
