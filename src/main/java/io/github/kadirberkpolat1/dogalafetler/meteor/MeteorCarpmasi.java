package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZari;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.Sarsinti;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKancalari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

/**
 * Çarpma anı: sarsıntı/görsel paketi, gümbürtü, krater sıcaklığı ve krater işi. Krater blokları, yangın ve ödüller
 * {@link KraterIsi}'nde tick'lere yayılır. Suya düşen meteor buhar çıkarır; krater, ateş ve ödül yoktur.
 */
final class MeteorCarpmasi {
    /** Çarpılan yüzeyin üstünde boşaltılan blok (bitki, kar, asılı blok). */
    static final int UST_TEMIZLIK = 2;
    /** Görsel paket bu mesafedeki oyunculara gider (iz/çarpma görünürlüğü). */
    static final double GORSEL_MENZIL = 256;

    private MeteorCarpmasi() {
    }

    static KraterIsi carp(ServerLevel level, BlockPos zemin, Vec3 nokta, MeteorBoyutu boyut, MeteorGorunumu gorunum,
            YanginKaynagi kaynak, boolean sarsinti, OdulZari.Sanslar sanslar, boolean suya) {
        paketGonder(level, nokta, boyut, gorunum, suya, sarsinti);
        boolean ay = gorunum == MeteorGorunumu.AY;
        if (suya) {
            level.playSound(null, nokta.x, nokta.y, nokta.z, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS,
                    boyut.buyuk() ? 6f : 3f, ay ? 0.7f : 0.6f);
            level.playSound(null, nokta.x, nokta.y, nokta.z, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS,
                    boyut.buyuk() ? 6f : 3f, 0.5f);
            level.sendParticles(ParticleTypes.CLOUD, nokta.x, nokta.y + 0.5, nokta.z, boyut.buyuk() ? 60 : 24, 1.2, 0.6,
                    1.2, 0.06);
            DogalAfetler.LOGGER.info("meteor suya düştü: {} ({}, {}): buhar, krater yok", zemin.toShortString(),
                    boyut, level.dimension().location());
            MinecraftForge.EVENT_BUS.post(new MeteorCarptiEvent(level, zemin, boyut, kaynak, OdulZari.Sonuc.YOK, true));
            return null;
        }
        level.playSound(null, nokta.x, nokta.y, nokta.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                boyut.buyuk() ? 10f : 5f, (boyut.buyuk() ? 0.55f : 0.75f) * (ay ? 1.1f : 1f));
        double yaricap = MeteorAyarlari.kraterYaricapi(boyut);
        double derinlik = MeteorAyarlari.kraterDerinligi(boyut);
        int sicaklikSaniye = MeteorAyarlari.kraterSicaklikSaniye();
        if (sicaklikSaniye > 0) {
            SicaklikKancalari.ortamKaynagiEkle(level, Vec3.atCenterOf(zemin), MeteorAyarlari.kraterSicakligi(boyut),
                    Math.min(64, Math.max(6, yaricap * 4)), sicaklikSaniye * 20);
        }
        boolean blokHasari = Kurallar.blokHasariAcik(level.getGameRules());
        long tohum = level.random.nextLong();
        OdulZari.Sonuc odul = OdulZari.at(boyut.buyuk(), sanslar, new Random(tohum ^ 0x5DEECE66DL));
        KraterIsi is = new KraterIsi(zemin, yaricap, derinlik, tohum, boyut, gorunum, kaynak, odul, blokHasari);
        MeteorYoneticisi.kraterEkle(level, is);
        return is;
    }

    private static void paketGonder(ServerLevel level, Vec3 nokta, MeteorBoyutu boyut, MeteorGorunumu gorunum,
            boolean suya, boolean sarsinti) {
        int menzil = MeteorAyarlari.sarsintiMenzili(boyut);
        double temel = (boyut.buyuk() ? 0.8 : 0.35) * (suya ? 0.5 : 1.0);
        for (ServerPlayer oyuncu : level.players()) {
            double mesafe = oyuncu.position().distanceTo(nokta);
            if (mesafe > GORSEL_MENZIL) {
                continue;
            }
            float siddet = sarsinti ? (float) Sarsinti.siddet(mesafe, menzil, temel) : 0f;
            AgKanali.oyuncuyaGonder(oyuncu, new MeteorSarsintiPaketi(nokta.x, nokta.y, nokta.z, (byte) boyut.ordinal(),
                    (byte) gorunum.ordinal(), suya, siddet));
        }
    }
}
