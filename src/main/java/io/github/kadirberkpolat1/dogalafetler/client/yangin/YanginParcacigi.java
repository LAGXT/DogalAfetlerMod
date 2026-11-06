package io.github.kadirberkpolat1.dogalafetler.client.yangin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Yangının kendi parçacıkları: tam parlak büyük alev, büyüyerek yükselen duman ve uzaktan görünen duman sütunu. */
final class YanginParcacigi extends TextureSheetParticle {
    private final SpriteSet kareler;
    private final Tur tur;
    private final float ilkBoy;

    enum Tur {
        ALEV,
        DUMAN,
        SUTUN
    }

    private YanginParcacigi(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
            SpriteSet kareler, Tur tur) {
        super(level, x, y, z);
        this.kareler = kareler;
        this.tur = tur;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.hasPhysics = false;
        this.gravity = 0;
        switch (tur) {
            case ALEV -> {
                lifetime = 10 + random.nextInt(10);
                quadSize = 0.35F + random.nextFloat() * 0.35F;
                friction = 0.92F;
            }
            case DUMAN -> {
                lifetime = 70 + random.nextInt(60);
                quadSize = 0.6F + random.nextFloat() * 0.6F;
                friction = 0.97F;
                float gri = 0.18F + random.nextFloat() * 0.14F;
                setColor(gri, gri, gri);
                alpha = 0.75F;
            }
            case SUTUN -> {
                lifetime = 200 + random.nextInt(80);
                quadSize = 2.5F + random.nextFloat() * 2.0F;
                friction = 0.995F;
                float gri = 0.16F + random.nextFloat() * 0.12F;
                setColor(gri, gri * 0.97F, gri * 0.95F);
                alpha = 0.65F;
            }
            default -> throw new IllegalStateException(tur.name());
        }
        ilkBoy = quadSize;
        setSpriteFromAge(kareler);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        setSpriteFromAge(kareler);
        float oran = age / (float) lifetime;
        if (tur == Tur.ALEV) {
            quadSize = ilkBoy * (1.0F - oran * 0.6F);
        } else {
            quadSize = ilkBoy * (1.0F + oran * (tur == Tur.SUTUN ? 2.0F : 1.5F));
            alpha = (tur == Tur.SUTUN ? 0.65F : 0.75F) * (1.0F - oran * oran);
            xd += 0.002;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float kismiTick) {
        return tur == Tur.ALEV ? 0xF000F0 : super.getLightColor(kismiTick);
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
            return new YanginParcacigi(level, x, y, z, vx, vy, vz, kareler, tur);
        }
    }
}
