package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Vücut durumu parçacıkları (§A8). Nefes buharı: ağızdan çıkan yarı saydam bulutçuk; hızla yavaşlar, büyür, hafifçe
 * yükselir ve söner. Ter damlası: baş ve omuz hizasından düşen küçük damla; yerçekimiyle hızlanır, zemine değince
 * sıçrama karesine geçip kısa sürede kaybolur.
 */
final class SicaklikParcacigi extends TextureSheetParticle {
    private final SpriteSet kareler;
    private final Tur tur;
    private final float ilkBoy;
    private final float ilkAlfa;
    /** Ter damlasının zemine değdiği yaş; -1 havada. */
    private int yereDegdi = -1;

    enum Tur {
        BUHAR,
        TER
    }

    private SicaklikParcacigi(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
            SpriteSet kareler, Tur tur) {
        super(level, x, y, z);
        this.kareler = kareler;
        this.tur = tur;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        switch (tur) {
            case BUHAR -> {
                lifetime = 22 + random.nextInt(10);
                quadSize = 0.07F + random.nextFloat() * 0.04F;
                hasPhysics = false;
                gravity = -0.012F;
                friction = 0.86F;
                float ton = 0.92F + random.nextFloat() * 0.08F;
                setColor(ton, ton, Math.min(1F, ton + 0.04F));
                alpha = 0.55F;
                setSpriteFromAge(kareler);
            }
            case TER -> {
                lifetime = 26 + random.nextInt(10);
                quadSize = 0.035F + random.nextFloat() * 0.02F;
                hasPhysics = true;
                gravity = 0.55F;
                friction = 0.98F;
                setColor(1F, 0.97F, 0.9F);
                alpha = 0.85F;
                setSprite(kareler.get(random.nextInt(3), 3));
            }
            default -> throw new IllegalStateException(tur.name());
        }
        ilkBoy = quadSize;
        ilkAlfa = alpha;
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float oran = age / (float) lifetime;
        switch (tur) {
            case BUHAR -> {
                setSpriteFromAge(kareler);
                quadSize = ilkBoy * (1.0F + oran * 1.6F);
                // İlk birkaç tick'te yoğun, sonra yumuşakça söner.
                alpha = ilkAlfa * (oran < 0.15F ? 1.0F : (1.0F - oran) / 0.85F);
            }
            case TER -> {
                if (onGround && yereDegdi < 0) {
                    yereDegdi = age;
                    setSprite(kareler.get(3, 3));
                    xd = 0;
                    zd = 0;
                }
                if (yereDegdi >= 0) {
                    int gecen = age - yereDegdi;
                    alpha = ilkAlfa * Math.max(0F, 1.0F - gecen / 6.0F);
                    if (gecen >= 6) {
                        remove();
                    }
                } else if (oran > 0.8F) {
                    alpha = ilkAlfa * (1.0F - oran) / 0.2F;
                }
            }
            default -> {
            }
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
            return new SicaklikParcacigi(level, x, y, z, vx, vy, vz, kareler, tur);
        }
    }
}
