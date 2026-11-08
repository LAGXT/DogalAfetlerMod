package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.NefesBuhari;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Vücut durumu parçacıkları (§A8, S3-10, S3-11). Nefes buharı: ağızdan öne savrulan, açık gri kenarlı yoğun bulutçuk;
 * hızla yavaşlar, ömrünün ilk yarısında opak kalır, sonra büyüyerek yumuşakça söner ({@code NefesBuhari}). Ter damlası:
 * baş ve omuz hizasından düşen damla (lacivert kontür, parlak gövde, düşüş izi:
 * gökte, çimende ve kumda seçilir); yerçekimiyle hızlanır, zemine değince sıçrama karesine geçip kısa sürede kaybolur.
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
                lifetime = 26 + random.nextInt(10);
                quadSize = 0.16F + random.nextFloat() * 0.06F;
                hasPhysics = false;
                gravity = -0.01F;
                friction = 0.88F;
                float ton = 0.96F + random.nextFloat() * 0.04F;
                setColor(ton, ton, 1F);
                alpha = NefesBuhari.ALFA;
                setSpriteFromAge(kareler);
            }
            case TER -> {
                lifetime = 26 + random.nextInt(10);
                quadSize = 0.07F + random.nextFloat() * 0.03F;
                hasPhysics = true;
                gravity = 0.4F;
                friction = 0.98F;
                alpha = 1F;
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
                quadSize = ilkBoy * NefesBuhari.buyume(oran);
                alpha = ilkAlfa * NefesBuhari.opaklik(oran);
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
