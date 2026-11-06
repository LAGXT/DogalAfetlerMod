package io.github.kadirberkpolat1.dogalafetler.client.asit_yagmuru;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Asit yağmurunun parçacıkları (tasarım notu §2): bloklara çarpınca sıçrayan sarımsı yeşil damla, 0,6 sn'de yükselip dağılan
 * cızlama dumancığı, zeminde sürünen düşük yoğunluklu buhar ve alanın üstünde sürüklenen koyu zeytin renkli bulut tavanı.
 * Bulutlar tavan sayımı için canlı listede tutulur.
 */
final class AsitParcacigi extends TextureSheetParticle {
    /** Canlı bulut tavanı parçacıkları; {@link IstemciAsitYagmuru} her tick ayıklar. */
    static final List<AsitParcacigi> BULUTLAR = new ArrayList<>();

    private final SpriteSet kareler;
    private final Tur tur;
    private final float ilkBoy;
    private final float ilkAlfa;

    enum Tur {
        SICRAMA,
        DUMAN,
        BUHAR,
        BULUT
    }

    private AsitParcacigi(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
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
            case SICRAMA -> {
                lifetime = 5 + random.nextInt(5);
                quadSize = 0.05F + random.nextFloat() * 0.05F;
                gravity = 0.9F;
                friction = 0.9F;
                xd += (random.nextFloat() - 0.5F) * 0.08F;
                zd += (random.nextFloat() - 0.5F) * 0.08F;
                setColor(0.78F, 0.88F, 0.29F);
                alpha = 0.9F;
            }
            case DUMAN -> {
                lifetime = 10 + random.nextInt(5);
                quadSize = 0.12F + random.nextFloat() * 0.12F;
                friction = 0.92F;
                setColor(0.95F, 0.97F, 0.78F);
                alpha = 0.55F;
            }
            case BUHAR -> {
                lifetime = 40 + random.nextInt(30);
                quadSize = 0.5F + random.nextFloat() * 0.4F;
                friction = 0.96F;
                setColor(0.84F, 0.9F, 0.68F);
                alpha = 0.22F;
            }
            case BULUT -> {
                lifetime = 300 + random.nextInt(140);
                quadSize = 12F + random.nextFloat() * 10F;
                friction = 1.0F;
                float gri = 0.85F + random.nextFloat() * 0.15F;
                setColor(0.36F * gri, 0.38F * gri, 0.21F * gri);
                alpha = 0;
                BULUTLAR.add(this);
            }
            default -> throw new IllegalStateException(tur.name());
        }
        ilkBoy = quadSize;
        ilkAlfa = tur == Tur.BULUT ? 0.78F : alpha;
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
        switch (tur) {
            case DUMAN -> {
                quadSize = ilkBoy * (1.0F + oran * 1.8F);
                alpha = ilkAlfa * (1.0F - oran);
            }
            case BUHAR -> {
                quadSize = ilkBoy * (1.0F + oran * 0.8F);
                alpha = ilkAlfa * (float) Math.sin(Math.PI * oran);
            }
            case BULUT -> alpha = ilkAlfa * Math.min(1.0F, oran * 6.0F) * (oran < 0.75F ? 1.0F : (1.0F - oran) / 0.25F);
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
            return new AsitParcacigi(level, x, y, z, vx, vy, vz, kareler, tur);
        }
    }
}
