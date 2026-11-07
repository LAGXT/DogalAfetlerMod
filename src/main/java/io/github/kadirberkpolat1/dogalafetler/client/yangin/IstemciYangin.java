package io.github.kadirberkpolat1.dogalafetler.client.yangin;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.AtesDurumuPaketi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginAyarlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKayitlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
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
 * büyük alev, rüzgârla sürüklenen kıvılcım ve hücre dumani; ötesinde chunk başına duman kümesi; yangının üstünde,
 * kademe başına tavanlı duman sütunu ve tepesinde pirokümülüs. Kamera arkasındaki hücreler parçacık üretmez; özet
 * dünya (boyut) değişince ve çıkışta silinir. Parçacıklar vanilla'nın 32 blok kesimini atlayarak ({@code force}) eklenir:
 * mesafeyi ve sayıyı yalnız LOD, sütun mesafesi ve kademe sınırlar; vanilla parçacık ayarı (Tümü / Azaltılmış / En Az)
 * yoğunluğu 1 / 0,5 / 0,2 ile çarpar.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciYangin {
    private static final AtesDurumuPaketi BOS = new AtesDurumuPaketi(List.of(), new long[0]);
    private static final double KUME_MESAFESI = 96;
    private static final double[] VANILLA_AYAR_CARPANI = {1.0, 0.5, 0.2};
    private static final int SUTUN_OMRU = 400;
    private static final int KUME_OMRU = 360;
    private static final int PIROKUMULUS_ESIGI = 12;
    private static final double MERKEZ_SUTUN_PAYI = 0.6;
    private static final double KUME_KAYMASI = 18.0;
    private static AtesDurumuPaketi son = BOS;
    private static ClientLevel sonDunya;
    private static EfektKalitesi kaliteZorlamasi;
    /** Kendi alev/duman/sütun/kıvılcım üretimimiz (vanilla ateş dumanu hariç); kalite kanıtı için okunup sıfırlanır. */
    private static int uretilen;
    private static long saat;
    private static double[] ruzgar = {0, 0};

    private IstemciYangin() {
    }

    /** Görsel kanıt otomasyonu için: {@code null} config'teki kaliteye döner. */
    static void kaliteZorla(EfektKalitesi kalite) {
        kaliteZorlamasi = kalite;
    }

    /** Son çağrıdan beri ürettiğimiz parçacık sayısı; sayaç sıfırlanır. */
    static int uretileniAl() {
        int sayi = uretilen;
        uretilen = 0;
        return sayi;
    }

    /** Duraklatmada ilerlemeyen istemci saati (parçacık canlılık denetimi). */
    static long saat() {
        return saat;
    }

    /** Günün rüzgârı (sunucudaki yayılmayla aynı formül); duman, sütun ve kıvılcım sürüklenmesi. */
    static double[] ruzgar() {
        return ruzgar;
    }

    static int canliSutun() {
        return YanginParcacigi.SUTUNLAR.size();
    }

    static int canliKume() {
        return YanginParcacigi.KUMELER.size();
    }

    public static Consumer<AtesDurumuPaketi> alici() {
        return IstemciYangin::guncelle;
    }

    private static void guncelle(AtesDurumuPaketi paket) {
        ClientLevel level = Minecraft.getInstance().level;
        son = paket;
        sonDunya = level;
        if (level == null) {
            return;
        }
        for (long b : paket.buharlar()) {
            double x = BlockPos.getX(b) + 0.5;
            double y = BlockPos.getY(b) + 0.2;
            double z = BlockPos.getZ(b) + 0.5;
            for (int i = 0; i < 8; i++) {
                level.addParticle(ParticleTypes.CLOUD, true, x + level.random.nextGaussian() * 0.3, y,
                        z + level.random.nextGaussian() * 0.3, 0, 0.06 + level.random.nextDouble() * 0.06, 0);
            }
            level.playLocalSound(x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
                    2.2F + level.random.nextFloat() * 0.6F, false);
        }
    }

    private static void sifirla(ClientLevel level) {
        son = BOS;
        sonDunya = level;
        YanginParcacigi.SUTUNLAR.clear();
        YanginParcacigi.KUMELER.clear();
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        sifirla(null);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != sonDunya) {
            sifirla(level);
        }
        if (level == null || mc.isPaused()) {
            return;
        }
        saat++;
        YanginParcacigi.SUTUNLAR.removeIf(p -> !p.isAlive() || saat - p.sonGorulme > 3);
        YanginParcacigi.KUMELER.removeIf(p -> !p.isAlive() || saat - p.sonGorulme > 3);
        if (son.ozetler().isEmpty()) {
            return;
        }
        ruzgar = YanginMantigi.gununRuzgari(level.getGameTime() / 24000L);
        Kademe kademe = Kademe.al(kaliteZorlamasi != null ? kaliteZorlamasi : IstemciAyarlari.efektKalitesi());
        ParticleStatus vanillaAyari = mc.options.particles().get();
        double yogunluk = YanginAyarlari.parcacikYogunlugu() * VANILLA_AYAR_CARPANI[vanillaAyari.getId()];
        double lod = YanginAyarlari.lodMesafesi();
        double sutunMesafesi = YanginAyarlari.sutunMesafesi();
        int sutunTavani = Math.max(1, (int) Math.round(YanginAyarlari.sutunTavani() * kademe.sutunCarpani));
        Camera kamera = mc.gameRenderer.getMainCamera();
        Vec3 goz = kamera.getPosition();
        Vector3f bakis = kamera.getLookVector();
        RandomSource r = level.random;

        List<AtesDurumuPaketi.Ozet> sutunluk = new ArrayList<>();
        int sutunHucresi = 0;
        double merkezX = 0;
        double merkezY = 0;
        double merkezZ = 0;
        List<double[]> yakinlar = new ArrayList<>();
        for (AtesDurumuPaketi.Ozet o : son.ozetler()) {
            double mesafe = Math.sqrt(goz.distanceToSqr(o.x(), o.y(), o.z()));
            if (mesafe <= sutunMesafesi) {
                sutunluk.add(o);
                sutunHucresi += o.sayi();
                merkezX += o.x() * o.sayi();
                merkezY += o.ustY() * o.sayi();
                merkezZ += o.z() * o.sayi();
            }
            if (mesafe > lod || o.hucreler().length == 0) {
                if (mesafe <= KUME_MESAFESI && r.nextDouble() < 0.15 * yogunluk) {
                    ekle(level, YanginKayitlari.DUMAN.get(), o.x() + r.nextGaussian() * 2.5, o.ustY() + 1.5,
                            o.z() + r.nextGaussian() * 2.5, 0, 0.07, 0);
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
        sutunVeKume(level, r, sutunluk, sutunHucresi, merkezX, merkezY, merkezZ, sutunTavani, yogunluk);
        if (yakinlar.size() > kademe.alevHucresi) {
            yakinlar.sort((a, b) -> Double.compare(a[3], b[3]));
        }
        int adet = Math.min(yakinlar.size(), kademe.alevHucresi);
        for (int i = 0; i < adet; i++) {
            double[] h = yakinlar.get(i);
            if (r.nextDouble() < 0.18 * yogunluk) {
                ekle(level, YanginKayitlari.ALEV.get(), h[0] + r.nextGaussian() * 0.25, h[1] + 0.4 + r.nextDouble() * 0.5,
                        h[2] + r.nextGaussian() * 0.25, r.nextGaussian() * 0.01, 0.03 + r.nextDouble() * 0.04,
                        r.nextGaussian() * 0.01);
            }
            if (i % kademe.dumanAdimi == 0 && r.nextDouble() < 0.025 * kademe.dumanCarpani * yogunluk) {
                ekle(level, YanginKayitlari.DUMAN.get(), h[0] + r.nextGaussian() * 0.3, h[1] + 1.2, h[2] + r.nextGaussian() * 0.3,
                        0, 0.05 + r.nextDouble() * 0.03, 0);
            }
        }
        if (adet > 0) {
            double kivilcim = kademe.kivilcimSaniye / 20.0 * yogunluk;
            int sayi = Math.min(64, (int) kivilcim + (r.nextDouble() < kivilcim - (int) kivilcim ? 1 : 0));
            for (int i = 0; i < sayi; i++) {
                double[] h = yakinlar.get(r.nextInt(adet));
                ekle(level, YanginKayitlari.KIVILCIM.get(), h[0] + r.nextGaussian() * 0.3, h[1] + 0.4 + r.nextDouble() * 0.5,
                        h[2] + r.nextGaussian() * 0.3, ruzgar[0] * 0.05 + r.nextGaussian() * 0.02,
                        0.06 + r.nextDouble() * 0.08, ruzgar[1] * 0.05 + r.nextGaussian() * 0.02);
            }
        }
    }

    /**
     * Duman sütunu ve pirokümülüs (tasarım §2.5): yangının sütun mesafesi içindeki chunk'larından hücre sayısıyla ağırlıklı
     * seçilen yerde yükselen sütun; büyük yangında parçacıkların çoğu ağırlık merkezinde dar ve daha hızlı yükselir. Açık
     * renkli küme sütunun görünür ucunda, rüzgâr altına kaymış olarak doğar. Canlı sütun parçacığı kademe tavanını, küme
     * parçacığı tavanın yarısını aşmaz; üretim hızı tavanı ortalama ömre bölerek dolduracak kadardır.
     */
    private static void sutunVeKume(ClientLevel level, RandomSource r, List<AtesDurumuPaketi.Ozet> sutunluk, int hucre,
            double merkezX, double merkezY, double merkezZ, int tavan, double yogunluk) {
        if (hucre <= 0) {
            return;
        }
        double taban = merkezY / hucre + 2.0;
        boolean buyuk = hucre >= PIROKUMULUS_ESIGI;
        if (YanginParcacigi.SUTUNLAR.size() < tavan && r.nextDouble() < Math.min(1.0, 1.2 * tavan / SUTUN_OMRU) * yogunluk) {
            if (buyuk && r.nextDouble() < MERKEZ_SUTUN_PAYI) {
                double yayilim = Math.min(3.0, 1.0 + Math.sqrt(hucre) * 0.1);
                ekle(level, YanginKayitlari.SUTUN.get(), merkezX / hucre + r.nextGaussian() * yayilim, taban,
                        merkezZ / hucre + r.nextGaussian() * yayilim, 0, 0.21 + r.nextDouble() * 0.04, 0);
            } else {
                int secim = r.nextInt(hucre);
                for (AtesDurumuPaketi.Ozet o : sutunluk) {
                    secim -= o.sayi();
                    if (secim < 0) {
                        if (o.sayi() >= 3) {
                            ekle(level, YanginKayitlari.SUTUN.get(), o.x() + r.nextGaussian() * 1.5, o.ustY() + 2.0,
                                    o.z() + r.nextGaussian() * 1.5, 0, 0.17 + r.nextDouble() * 0.04, 0);
                        }
                        break;
                    }
                }
            }
        }
        int kumeTavani = Math.max(1, tavan / 2);
        if (buyuk && YanginParcacigi.KUMELER.size() < kumeTavani
                && r.nextDouble() < Math.min(1.0, 1.2 * kumeTavani / KUME_OMRU) * yogunluk) {
            double yayilim = 4.0 + Math.sqrt(hucre) * 0.25;
            ekle(level, YanginKayitlari.KUME.get(), merkezX / hucre + ruzgar[0] * KUME_KAYMASI + r.nextGaussian() * yayilim,
                    taban + YanginParcacigi.SUTUN_BOYU + r.nextDouble() * 10.0,
                    merkezZ / hucre + ruzgar[1] * KUME_KAYMASI + r.nextGaussian() * yayilim, ruzgar[0] * 0.03,
                    0.005 + r.nextDouble() * 0.01, ruzgar[1] * 0.03);
        }
    }

    private static void ekle(ClientLevel level, ParticleOptions tur, double x, double y, double z, double vx, double vy,
            double vz) {
        uretilen++;
        level.addParticle(tur, true, x, y, z, vx, vy, vz);
    }

    /**
     * Tasarım notu §5 tablosu: alevli en yakın hücre, kıvılcım/sn, her kaç hücrede duman, sütun tavanı çarpanı
     * ({@code yangin.sutunTavani} Yüksek'tir).
     */
    private enum Kademe {
        DUSUK(40, 30, 4, 1.0, 0.25),
        ORTA(120, 80, 2, 1.0, 0.5),
        YUKSEK(250, 160, 1, 1.0, 1.0),
        ULTRA(500, 300, 1, 1.5, 2.0);

        final int alevHucresi;
        final int kivilcimSaniye;
        final int dumanAdimi;
        final double dumanCarpani;
        final double sutunCarpani;

        Kademe(int alevHucresi, int kivilcimSaniye, int dumanAdimi, double dumanCarpani, double sutunCarpani) {
            this.alevHucresi = alevHucresi;
            this.kivilcimSaniye = kivilcimSaniye;
            this.dumanAdimi = dumanAdimi;
            this.dumanCarpani = dumanCarpani;
            this.sutunCarpani = sutunCarpani;
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
