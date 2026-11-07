package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorOlcegi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZari;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.Sarsinti;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKancalari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

/**
 * Çarpma anı: sarsıntı/görsel paketi, krater sıcaklığı ve krater işi. Patlama sesini paketi alan istemci menzil ve mesafe
 * kazancıyla çalar ({@code client.meteor.MeteorSesi}). Krater blokları, yangın ve ödüller {@link KraterIsi}'nde tick'lere
 * yayılır. Suya düşen meteor sıçrama ve buhar çıkarır; krater, ateş ve ödül yoktur. Büyüklüğe bağlı her değer sürekli
 * ölçekten ({@link MeteorOlcegi#ara}); ödül sınıfı ölçeğin bantıdır. Yağmur meteorunun ({@code odulOyuncu} dolu) ödülü yalnız büyük
 * bantta ve afet başına, oyuncu başına tavanlıdır ({@code mantik.OdulTavani}).
 */
final class MeteorCarpmasi {
    /** Çarpılan yüzeyin üstünde boşaltılan blok (bitki, kar, asılı blok). */
    static final int UST_TEMIZLIK = 2;
    /** Görsel paket bu mesafedeki oyunculara gider (iz/çarpma görünürlüğü). */
    static final double GORSEL_MENZIL = 256;

    private MeteorCarpmasi() {
    }

    static KraterIsi carp(ServerLevel level, BlockPos zemin, Vec3 nokta, double olcek, MeteorGorunumu gorunum,
            YanginKaynagi kaynak, boolean sarsinti, OdulZari.Sanslar sanslar, boolean suya, UUID odulOyuncu) {
        paketGonder(level, nokta, olcek, gorunum, suya, sarsinti);
        boolean ay = gorunum == MeteorGorunumu.AY;
        MeteorBoyutu boyut = MeteorBoyutu.olcekten(olcek);
        if (suya) {
            float hacim = (float) MeteorOlcegi.ara(olcek, 3, 6, 2, 8);
            level.playSound(null, nokta.x, nokta.y, nokta.z, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, hacim,
                    ay ? 0.7f : 0.6f);
            level.playSound(null, nokta.x, nokta.y, nokta.z, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, hacim, 0.5f);
            level.sendParticles(ParticleTypes.CLOUD, nokta.x, nokta.y + 0.5, nokta.z,
                    (int) Math.round(MeteorOlcegi.ara(olcek, 24, 60, 12, 80)), 1.2, 0.6, 1.2, 0.06);
            DogalAfetler.LOGGER.info("meteor suya düştü: {} ({}, ölçek {}, {}): buhar, krater yok", zemin.toShortString(),
                    boyut, (float) olcek, level.dimension().location());
            MinecraftForge.EVENT_BUS.post(new MeteorCarptiEvent(level, zemin, olcek, kaynak, OdulZari.Sonuc.YOK, true));
            return null;
        }
        double yaricap = MeteorAyarlari.kraterYaricapi(olcek);
        double derinlik = MeteorAyarlari.kraterDerinligi(olcek);
        int sicaklikSaniye = MeteorAyarlari.kraterSicaklikSaniye();
        if (sicaklikSaniye > 0) {
            SicaklikKancalari.ortamKaynagiEkle(level, Vec3.atCenterOf(zemin), MeteorAyarlari.kraterSicakligi(olcek),
                    Math.min(64, Math.max(6, yaricap * 4)), sicaklikSaniye * 20);
        }
        boolean blokHasari = Kurallar.blokHasariAcik(level.getGameRules());
        long tohum = level.random.nextLong();
        OdulZari.Sonuc odul = OdulZari.at(boyut.buyuk(), sanslar, new Random(tohum ^ 0x5DEECE66DL));
        if (odulOyuncu != null) {
            odul = MeteorYoneticisi.odulSinirla(level, kaynak, odulOyuncu, odul, boyut.buyuk());
        }
        KraterIsi is = new KraterIsi(zemin, yaricap, derinlik, tohum, olcek, gorunum, kaynak, odul, blokHasari);
        MeteorYoneticisi.kraterEkle(level, is);
        return is;
    }

    private static void paketGonder(ServerLevel level, Vec3 nokta, double olcek, MeteorGorunumu gorunum,
            boolean suya, boolean sarsinti) {
        double menzil = MeteorAyarlari.sarsintiMenzili(olcek);
        double temel = MeteorOlcegi.ara(olcek, 0.35, 0.8, 0.2, 1.0) * (suya ? 0.5 : 1.0);
        for (ServerPlayer oyuncu : level.players()) {
            double mesafe = oyuncu.position().distanceTo(nokta);
            if (mesafe > GORSEL_MENZIL) {
                continue;
            }
            float siddet = sarsinti ? (float) Sarsinti.siddet(mesafe, menzil, temel) : 0f;
            AgKanali.oyuncuyaGonder(oyuncu, new MeteorSarsintiPaketi(nokta.x, nokta.y, nokta.z, (float) olcek,
                    (byte) gorunum.ordinal(), suya, siddet));
        }
    }
}
