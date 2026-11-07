package io.github.kadirberkpolat1.dogalafetler.client.donma;

import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Donmanın kar tanesi ve buz kristali. Dört kareden biri doğuşta rastgele seçilir, yaşla değişmez (0 nokta kümesi,
 * 1 küçük yıldız, 2 dallı yıldız, 3 yuvarlak tane).
 *
 * <p>Kar: dikey hız kendi uç hızına (0,04–0,09 blok/tick) yaklaşır; yatay hız günün rüzgârı ile iki eksende farklı
 * frekanslı salınımın toplamına yaklaşır (her tanenin fazı, frekansı, genliği ve rüzgâr payı kendinedir); yavaşça
 * döner. Ömrü altındaki zemine ulaşmaya yetecek kadardır. Yere, tavana ya da suya değince durur ve {@value #SONME_SURESI}
 * tickte söner. Kristal: yalnız küçük iki kare, neredeyse asılı süzülür ve parlaklığı salınımla pırıldar.
 */
final class KarTanesiParcacigi extends TextureSheetParticle {
    private static final int KARE_SAYISI = 4;
    /** Karenin dörtgen yarı boyu (blok); taneye ±%20 eklenir. */
    private static final float[] BOYLAR = {0.06F, 0.08F, 0.12F, 0.10F};
    /** Günün rüzgârı (güç 0,6) bu katla blok/tick sürüklenmeye çevrilir: kar ~0,024, kristal ~0,009. */
    private static final double KAR_RUZGAR_KATI = 0.04;
    private static final double KRISTAL_RUZGAR_KATI = 0.015;
    private static final int SONME_SURESI = 8;
    private static final int EN_UZUN_OMUR = 600;
    /** Parçacıklar tek istemci iş parçacığında tick alır; sıvı sorgusu için paylaşılır. */
    private static final BlockPos.MutableBlockPos KONUM = new BlockPos.MutableBlockPos();

    private final Tur tur;
    private final float ucHizi;
    private final float salinimFazi;
    private final float salinimHizi;
    private final float salinimGenligi;
    private final float donmeHizi;
    private final double ruzgarX;
    private final double ruzgarZ;
    private final float tamAlfa;
    private int sonme = -1;

    enum Tur {
        KAR,
        KRISTAL
    }

    private KarTanesiParcacigi(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
            SpriteSet kareler, Tur tur) {
        super(level, x, y, z);
        this.tur = tur;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.hasPhysics = true;
        this.gravity = 0;
        this.friction = 1.0F;
        setSize(0.1F, 0.1F);
        double ruzgarPayi = 0.7 + random.nextDouble() * 0.6;
        int kare;
        if (tur == Tur.KAR) {
            kare = random.nextInt(KARE_SAYISI);
            quadSize = BOYLAR[kare] * (0.8F + random.nextFloat() * 0.4F);
            ucHizi = 0.04F + random.nextFloat() * 0.05F;
            salinimHizi = 0.10F + random.nextFloat() * 0.15F;
            salinimGenligi = 0.02F + random.nextFloat() * 0.02F;
            donmeHizi = (random.nextFloat() - 0.5F) * 0.1F;
            ruzgarPayi *= KAR_RUZGAR_KATI;
            tamAlfa = 0.85F + random.nextFloat() * 0.15F;
            int zemin = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            lifetime = Math.min(EN_UZUN_OMUR, (int) (Math.max(0.0, y - zemin) / ucHizi) + 40);
        } else {
            kare = random.nextInt(2);
            quadSize = 0.025F + random.nextFloat() * 0.02F;
            ucHizi = 0.005F + random.nextFloat() * 0.01F;
            salinimHizi = 0.04F + random.nextFloat() * 0.06F;
            salinimGenligi = 0.006F + random.nextFloat() * 0.006F;
            donmeHizi = (random.nextFloat() - 0.5F) * 0.06F;
            ruzgarPayi *= KRISTAL_RUZGAR_KATI;
            tamAlfa = 0.9F;
            lifetime = 60 + random.nextInt(61);
        }
        double[] ruzgar = YanginMantigi.gununRuzgari(level.getGameTime() / 24000L);
        ruzgarX = ruzgar[0] * ruzgarPayi;
        ruzgarZ = ruzgar[1] * ruzgarPayi;
        salinimFazi = random.nextFloat() * Mth.TWO_PI;
        roll = oRoll = random.nextFloat() * Mth.TWO_PI;
        alpha = 0;
        setColor(0.90F + random.nextFloat() * 0.08F, 0.95F + random.nextFloat() * 0.05F, 1.0F);
        setSprite(kareler.get(kare, KARE_SAYISI - 1));
    }

    @Override
    public void tick() {
        oRoll = roll;
        super.tick();
        if (removed) {
            return;
        }
        if (sonme < 0 && (onGround || y == yo || !level.getFluidState(KONUM.set(x, y, z)).isEmpty())) {
            sonme = SONME_SURESI;
        }
        if (sonme >= 0) {
            alpha = tamAlfa * sonme / SONME_SURESI;
            xd = 0;
            yd = 0;
            zd = 0;
            if (sonme-- == 0) {
                remove();
            }
            return;
        }
        roll += donmeHizi;
        float faz = salinimFazi + age * salinimHizi;
        xd += (ruzgarX + salinimGenligi * Mth.sin(faz) - xd) * 0.2;
        zd += (ruzgarZ + salinimGenligi * Mth.sin(faz * 0.8F + 1.7F) - zd) * 0.2;
        yd += (-ucHizi - yd) * 0.1;
        alpha = tamAlfa * Math.min(1.0F, age / 4.0F) * Math.min(1.0F, (lifetime - age) / 20.0F);
        if (tur == Tur.KRISTAL) {
            alpha *= 0.55F + 0.45F * Mth.sin(faz * 3.0F);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    static final class Saglayici implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet kareler;
        private final Tur tur;

        Saglayici(SpriteSet kareler, Tur tur) {
            this.kareler = kareler;
            this.tur = tur;
        }

        @Override
        public Particle createParticle(SimpleParticleType tip, ClientLevel level, double x, double y, double z,
                double vx, double vy, double vz) {
            return new KarTanesiParcacigi(level, x, y, z, vx, vy, vz, kareler, tur);
        }
    }
}
