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
 * okunur (aşama, seviye; {@code ozel.y} yoğunluk, {@code ozel.a} aşınan bloklar) ve 1 Hz paket arasında yumuşatılır; görünen
 * yoğunluk seviyeyle ölçeklenir (S1 seyrek çise, S5 sağanak). Yağmur afetin boyutunun her yerinde yağar (AF2-01): alan
 * kenarı ve sis yarıçapı yoktur, çöl ve kar biyomunda da asit yağar. SIREN'de bulut tavanı ve sis toplanır, AKTIF'te yağmur
 * ({@link AsitYagmuruCizici}), sıçrama, cızlama dumancığı, yüzey buharı ve vanilla yağmur sesi gelir. Sunucu aynı anda
 * vanilla yağmurunu açtığından vanilla yağmur çizimi AKTIF boyunca ve bitişte sunucu yağmuru sönene dek istemcide bastırılır
 * ({@code ClientLevel.setRainLevel(0)}, donma istemcisinin kar kipiyle aynı yol); maviyle asit üst üste binmez. Parçacıklar
 * vanilla'nın 32 blok kesimini atlayarak ({@code force}) eklenir; sayıyı efekt kalitesi ve vanilla parçacık ayarı sınırlar.
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
    /** Asit bittikten sonra vanilla yağmurun bastırılmaya devam ettiği süre: sunucu yağmuru 0,01/tick ile söner. */
    private static final int SONUM_TICK = 100;

    private static boolean aktif;
    private static double yogunluk;
    /** Sunucunun gönderdiği yumuşatılmamış yoğunluk (AKTIF değilse 0): maruziyet sunucuyla aynı anda başlayıp biter. */
    private static double hamYogunluk;
    /** Paket AKTIF aşamada: sunucu vanilla yağmurunu açmıştır (ham yoğunluk ilk saniyede henüz 0 olabilir). */
    private static boolean yagiyor;
    private static double bulut;
    private static long[] asinan = new long[0];
    private static long afetNo;
    private static EfektKalitesi kaliteZorlamasi;
    private static int sesSayaci;
    private static float oncekiCan = -1;
    /** Vanilla yağmurun daha kaç tick bastırılacağı (AKTIF boyunca dolu, bitişte {@link #SONUM_TICK}'ten geri sayar). */
    private static int bastirmaKalan;

    private IstemciAsitYagmuru() {
    }

    /** Yağmur yoğunluğu 0–1 (çizici ve ekran efekti). */
    static double yogunluk() {
        return aktif ? yogunluk : 0;
    }

    static boolean aktif() {
        return aktif;
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
        hamYogunluk = 0;
        yagiyor = false;
        bulut = 0;
        asinan = new long[0];
        AsitParcacigi.BULUTLAR.clear();
        oncekiCan = -1;
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        sifirla();
        bastirmaKalan = 0;
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
        boolean durum = durumuOku(level);
        vanillaYagmuru(level);
        if (!durum) {
            AsitEkranEfekti.maruzGuncelle(false);
            return;
        }
        boolean maruz = maruzMu(level, mc.player);
        AsitEkranEfekti.maruzGuncelle(maruz);
        Camera kamera = mc.gameRenderer.getMainCamera();
        Vec3 goz = kamera.getPosition();
        Kademe k = kademe();
        double ayar = VANILLA_AYAR_CARPANI[mc.options.particles().get().getId()];
        RandomSource r = level.random;
        bulutTavani(level, goz, k, ayar, r);
        if (yogunluk > 0.01) {
            damlalar(level, goz, k, ayar, r);
            buhar(level, goz, k, ayar, r);
            sesler(level, goz, r);
        }
        asinanlar(level, goz, ayar, r);
        if (maruz) {
            maruzDumani(level, mc.player, ayar, r);
        }
        hasarIzle(mc.player);
    }

    /**
     * Sunucudaki hasar denetimiyle aynı koşul: AKTIF yağmur (ham yoğunluk > 0), hayatta, yaratıcı ve izleyici değil ve başın
     * üstü gökyüzüne açık; yağmur her yerde yağdığından konum koşulu yok (AF2-03).
     * Ekran efekti ve oyuncunun üstündeki cızlama bu sürece görünür.
     */
    static boolean maruzMu(ClientLevel level, LocalPlayer oyuncu) {
        return oyuncu != null && aktif && hamYogunluk > 0 && !oyuncu.isSpectator() && !oyuncu.isCreative()
                && oyuncu.isAlive() && acikta(level, oyuncu.getX(), oyuncu.getEyeY(), oyuncu.getZ());
    }

    /**
     * Asit AKTIF iken ve bittikten sonra {@link #SONUM_TICK} boyunca vanilla yağmur çizilmez (sunucunun açtığı yağmur 0,01/tick
     * ile sönerken mavi yağmur görünmesin). Paketler tick'ten önce işlendiğinden her tick sonunda sıfırlamak çizimi bastırır;
     * bırakılırken dünyada gerçekten yağmur varsa (önceki hava yağmurluydu) yağmur düzeyi geri gelir.
     */
    private static void vanillaYagmuru(ClientLevel level) {
        if (aktif && yagiyor) {
            bastirmaKalan = SONUM_TICK;
        }
        if (bastirmaKalan <= 0) {
            return;
        }
        bastirmaKalan--;
        level.setRainLevel(0f);
        if (bastirmaKalan == 0 && level.getLevelData().isRaining()) {
            level.setRainLevel(1f);
        }
    }

    /** Maruz oyuncunun başı ve omuzlarından seyrek cızlama dumanı (üçüncü kişi kamerada okunur). */
    private static void maruzDumani(ClientLevel level, LocalPlayer oyuncu, double ayar, RandomSource r) {
        if (r.nextDouble() > 0.35 * ayar) {
            return;
        }
        ekle(level, AsitKayitlari.DUMAN.get(), oyuncu.getX() + (r.nextDouble() - 0.5) * 0.7,
                oyuncu.getEyeY() + 0.25 + r.nextDouble() * 0.2, oyuncu.getZ() + (r.nextDouble() - 0.5) * 0.7, 0,
                0.03 + r.nextDouble() * 0.02, 0);
    }

    /** Paketten durumu okur, 1 Hz arasında yumuşatır; bu boyutta asit yağmuru yoksa sıfırlar. */
    private static boolean durumuOku(ClientLevel level) {
        Optional<AfetDurumPaketi> paket = IstemciAfetDurumu.aktif(AfetTuru.ASIT_YAGMURU).filter(p -> p.boyut().equals(level.dimension().location())
                && (p.asama() == AfetAsamasi.SIREN || p.asama() == AfetAsamasi.AKTIF));
        if (paket.isEmpty()) {
            if (aktif) {
                sifirla();
            }
            return false;
        }
        AfetDurumPaketi p = paket.get();
        if (!aktif || p.afetNo() != afetNo) {
            afetNo = p.afetNo();
            aktif = true;
        }
        double carpan = AsitKurallari.gorselCarpan(p.seviye());
        yagiyor = p.asama() == AfetAsamasi.AKTIF;
        hamYogunluk = yagiyor ? p.ozel().getFloat("y") : 0;
        double hedefYogunluk = hamYogunluk * carpan;
        double sirenIlerleme = p.asama() == AfetAsamasi.SIREN && p.kalanTick() >= 0
                ? 1.0 - Math.min(1.0, p.kalanTick() / (double) SIREN_TICK) : 1.0;
        double hedefBulut = p.asama() == AfetAsamasi.SIREN ? 0.7 * carpan * sirenIlerleme
                : Math.max(0.7 * carpan, hedefYogunluk);
        yogunluk += Mth.clamp(hedefYogunluk - yogunluk, -0.02, 0.02);
        bulut += Mth.clamp(hedefBulut - bulut, -0.01, 0.01);
        asinan = p.ozel().getLongArray("a");
        return true;
    }

    /** Bulut yüksekliğinde (kameraya en çok 60 blok yukarıda) rüzgârla sürüklenen koyu bulut tavanı; her yerde. */
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

    /**
     * Canı azaltan hasarın kaynağı {@code dogalafetler:asit} ise ekran efektini tetikler. İstemci kaynağı sunucunun hasar
     * paketinden bilir ({@code LivingEntity.handleDamageEvent} {@code lastDamageSource}'u yazar; can paketi ondan sonra gelir).
     */
    private static void hasarIzle(LocalPlayer oyuncu) {
        if (oyuncu == null) {
            return;
        }
        float can = oyuncu.getHealth();
        if (oncekiCan >= 0 && can < oncekiCan && oyuncu.getLastDamageSource() != null
                && oyuncu.getLastDamageSource().is(AsitKayitlari.ASIT)) {
            AsitEkranEfekti.tetikle();
        }
        oncekiCan = can;
    }

    private static void ekle(ClientLevel level, ParticleOptions tur, double px, double py, double pz, double vx, double vy,
            double vz) {
        level.addParticle(tur, true, px, py, pz, vx, vy, vz);
    }

    /** Sarı-yeşil sis her yerde, bulut tavanıyla birlikte toplanır; gündüz ve gece rengi güneş açısıyla karışır. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void sisRengi(ViewportEvent.ComputeFogColor olay) {
        double w = sisGucu();
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
        double w = sisGucu();
        if (w <= 0 || olay.isCanceled()) {
            return;
        }
        float kisalma = (float) (0.4 * w);
        olay.setFarPlaneDistance(olay.getFarPlaneDistance() * (1 - kisalma));
        olay.setNearPlaneDistance(olay.getNearPlaneDistance() * (1 - 1.5F * kisalma));
        olay.setCanceled(true);
    }

    private static double sisGucu() {
        if (!aktif || Minecraft.getInstance().level == null) {
            return 0;
        }
        return Mth.clamp(bulut, 0, 1);
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
