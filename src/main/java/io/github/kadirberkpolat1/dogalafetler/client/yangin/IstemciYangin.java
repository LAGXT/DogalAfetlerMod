package io.github.kadirberkpolat1.dogalafetler.client.yangin;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.AtesDurumuPaketi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginAyarlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKayitlari;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * Sunucunun {@code yangin/ates_durumu} özetinden parçacık üretir (tasarım notu §5): LOD mesafesine kadar en yakın hücrelerde
 * büyük alev, kıvılcım ve hücre dumani; ötesinde chunk başına duman kümesi; her yangın chunk'ında duman sütunu. Kamera
 * arkasındaki hücreler parçacık üretmez. Kalite kademeleri {@link IstemciAyarlari#efektKalitesi()}'nden gelir.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciYangin {
    private static final AtesDurumuPaketi BOS = new AtesDurumuPaketi(List.of(), new long[0]);
    private static final double KUME_MESAFESI = 96;
    private static final double YAKIN_PARCACIK = 32;
    private static AtesDurumuPaketi son = BOS;
    private static EfektKalitesi kaliteZorlamasi;

    private IstemciYangin() {
    }

    /** Görsel kanıt otomasyonu için: {@code null} config'teki kaliteye döner. */
    static void kaliteZorla(EfektKalitesi kalite) {
        kaliteZorlamasi = kalite;
    }

    public static Consumer<AtesDurumuPaketi> alici() {
        return IstemciYangin::guncelle;
    }

    private static void guncelle(AtesDurumuPaketi paket) {
        son = paket;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (long b : paket.buharlar()) {
            double x = BlockPos.getX(b) + 0.5;
            double y = BlockPos.getY(b) + 0.2;
            double z = BlockPos.getZ(b) + 0.5;
            for (int i = 0; i < 8; i++) {
                level.addAlwaysVisibleParticle(ParticleTypes.CLOUD, x + level.random.nextGaussian() * 0.3, y,
                        z + level.random.nextGaussian() * 0.3, 0, 0.06 + level.random.nextDouble() * 0.06, 0);
            }
            level.playLocalSound(x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
                    2.2F + level.random.nextFloat() * 0.6F, false);
        }
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        son = BOS;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (olay.phase != TickEvent.Phase.END || level == null || mc.isPaused() || son.ozetler().isEmpty()) {
            return;
        }
        Kademe kademe = Kademe.al(kaliteZorlamasi != null ? kaliteZorlamasi : IstemciAyarlari.efektKalitesi());
        double yogunluk = YanginAyarlari.parcacikYogunlugu();
        double lod = YanginAyarlari.lodMesafesi();
        Camera kamera = mc.gameRenderer.getMainCamera();
        Vec3 goz = kamera.getPosition();
        Vector3f bakis = kamera.getLookVector();
        RandomSource r = level.random;

        List<double[]> yakinlar = new ArrayList<>();
        for (AtesDurumuPaketi.Ozet o : son.ozetler()) {
            double mesafe = Math.sqrt(goz.distanceToSqr(o.x(), o.y(), o.z()));
            if (o.sayi() >= 3 && r.nextDouble() < kademe.sutunOrani * yogunluk) {
                ekle(level, YanginKayitlari.SUTUN.get(), o.x() + r.nextGaussian() * 2, o.ustY() + 2.0,
                        o.z() + r.nextGaussian() * 2, 0, 0.18 + r.nextDouble() * 0.06, 0, true);
            }
            if (mesafe > lod || o.hucreler().length == 0) {
                if (mesafe <= KUME_MESAFESI && r.nextDouble() < 0.3 * yogunluk) {
                    ekle(level, YanginKayitlari.DUMAN.get(), o.x() + r.nextGaussian() * 2.5, o.ustY() + 1.5,
                            o.z() + r.nextGaussian() * 2.5, 0, 0.07, 0, true);
                }
                continue;
            }
            for (int kod : o.hucreler()) {
                double x = AtesDurumuPaketi.hucreX(o.cx(), kod) + 0.5;
                double y = AtesDurumuPaketi.hucreY(kod) + 0.5;
                double z = AtesDurumuPaketi.hucreZ(o.cz(), kod) + 0.5;
                double dx = x - goz.x;
                double dy = y - goz.y;
                double dz = z - goz.z;
                double d2 = dx * dx + dy * dy + dz * dz;
                if (d2 > lod * lod) {
                    continue;
                }
                double d = Math.sqrt(d2);
                if (d > 12 && (dx * bakis.x() + dy * bakis.y() + dz * bakis.z()) / d < 0.2) {
                    continue;
                }
                yakinlar.add(new double[] {x, y, z, d2});
            }
        }
        if (yakinlar.size() > kademe.alevHucresi) {
            yakinlar.sort((a, b) -> Double.compare(a[3], b[3]));
        }
        int adet = Math.min(yakinlar.size(), kademe.alevHucresi);
        for (int i = 0; i < adet; i++) {
            double[] h = yakinlar.get(i);
            boolean uzak = h[3] > YAKIN_PARCACIK * YAKIN_PARCACIK;
            if (r.nextDouble() < 0.18 * yogunluk) {
                ekle(level, YanginKayitlari.ALEV.get(), h[0] + r.nextGaussian() * 0.25, h[1] + 0.4 + r.nextDouble() * 0.5,
                        h[2] + r.nextGaussian() * 0.25, r.nextGaussian() * 0.01, 0.03 + r.nextDouble() * 0.04,
                        r.nextGaussian() * 0.01, uzak);
            }
            if (i % kademe.dumanAdimi == 0 && r.nextDouble() < 0.05 * kademe.dumanCarpani * yogunluk) {
                ekle(level, YanginKayitlari.DUMAN.get(), h[0] + r.nextGaussian() * 0.3, h[1] + 1.2, h[2] + r.nextGaussian() * 0.3,
                        0, 0.05 + r.nextDouble() * 0.03, 0, uzak);
            }
        }
        if (adet > 0) {
            double kivilcim = kademe.kivilcimSaniye / 20.0 * yogunluk;
            int sayi = Math.min(64, (int) kivilcim + (r.nextDouble() < kivilcim - (int) kivilcim ? 1 : 0));
            for (int i = 0; i < sayi; i++) {
                double[] h = yakinlar.get(r.nextInt(adet));
                ekle(level, ParticleTypes.LAVA, h[0] + r.nextGaussian() * 0.3, h[1] + 0.6, h[2] + r.nextGaussian() * 0.3,
                        0, 0, 0, h[3] > YAKIN_PARCACIK * YAKIN_PARCACIK);
            }
        }
    }

    private static void ekle(ClientLevel level, ParticleOptions tur, double x, double y, double z, double vx, double vy,
            double vz, boolean herZaman) {
        if (herZaman) {
            level.addAlwaysVisibleParticle(tur, x, y, z, vx, vy, vz);
        } else {
            level.addParticle(tur, x, y, z, vx, vy, vz);
        }
    }

    /** Tasarım notu §5 tablosu: alevli en yakın hücre, kıvılcım/sn, her kaç hücrede duman, sütun sıklığı. */
    private enum Kademe {
        DUSUK(40, 30, 4, 1.0, 0.10),
        ORTA(120, 80, 2, 1.0, 0.20),
        YUKSEK(250, 160, 1, 1.0, 0.30),
        ULTRA(500, 300, 1, 1.5, 0.45);

        final int alevHucresi;
        final int kivilcimSaniye;
        final int dumanAdimi;
        final double dumanCarpani;
        final double sutunOrani;

        Kademe(int alevHucresi, int kivilcimSaniye, int dumanAdimi, double dumanCarpani, double sutunOrani) {
            this.alevHucresi = alevHucresi;
            this.kivilcimSaniye = kivilcimSaniye;
            this.dumanAdimi = dumanAdimi;
            this.dumanCarpani = dumanCarpani;
            this.sutunOrani = sutunOrani;
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
