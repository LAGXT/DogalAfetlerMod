package io.github.kadirberkpolat1.dogalafetler.client.meteor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Meteor parçacıkları: tam parlak plazma (güneş beyaz-sarı → turuncu-kızıl, ay mavi-beyaz → mavi), 8 sn'de dağılan gri
 * duman şeridi, düşen kıvılcım, çarpmanın şok halkası tozu ve ödüllerin yıldız parıltısı (altın / mor-mavi). Dumanın
 * üretimdeki yukarı hızı sürtünmeyle durmaz, ısıyla ((1 − yaş oranı)²) azalan uç hız olarak kalır: çarpma dumanı yükselir,
 * şerit dumanı havada asılı kalır.
 */
final class MeteorParcacigi extends TextureSheetParticle {
    enum Tur {
        PLAZMA,
        PLAZMA_AY,
        DUMAN,
        KIVILCIM,
        KIVILCIM_AY,
        TOZ,
        PARILTI,
        KOZMIK
    }

    private final SpriteSet kareler;
    private final Tur tur;
    private final float ilkBoy;
    private final float faz;
    private final float kaldirma;
    private final float r0;
    private final float g0;
    private final float b0;

    private MeteorParcacigi(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
            SpriteSet kareler, Tur tur) {
        super(level, x, y, z);
        this.kareler = kareler;
        this.tur = tur;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.hasPhysics = false;
        this.gravity = 0;
        this.faz = random.nextFloat() * Mth.TWO_PI;
        float r = 1;
        float g = 1;
        float b = 1;
        float kaldir = 0;
        switch (tur) {
            case PLAZMA, PLAZMA_AY -> {
                lifetime = 10 + random.nextInt(12);
                quadSize = 0.675F + random.nextFloat() * 0.75F;
                friction = 0.9F;
            }
            case DUMAN -> {
                lifetime = 140 + random.nextInt(40);
                quadSize = 0.9F + random.nextFloat() * 0.8F;
                friction = 0.96F;
                kaldir = (float) Math.max(0.0, vy) * (1.0F - friction);
                float gri = 0.32F + random.nextFloat() * 0.14F;
                r = gri;
                g = gri;
                b = gri;
                alpha = 0.55F;
            }
            case KIVILCIM, KIVILCIM_AY -> {
                lifetime = 18 + random.nextInt(24);
                quadSize = 0.09F + random.nextFloat() * 0.105F;
                friction = 0.95F;
                gravity = 0.35F;
                hasPhysics = true;
            }
            case TOZ -> {
                lifetime = 28 + random.nextInt(14);
                quadSize = 0.9F + random.nextFloat() * 0.9F;
                friction = 0.86F;
                float t = random.nextFloat() * 0.1F;
                r = 0.47F + t;
                g = 0.40F + t;
                b = 0.33F + t;
                alpha = 0.8F;
            }
            case PARILTI -> {
                lifetime = 16 + random.nextInt(10);
                quadSize = 0.07F + random.nextFloat() * 0.06F;
                friction = 0.9F;
                r = 1.0F;
                g = 0.72F + random.nextFloat() * 0.2F;
                b = 0.25F + random.nextFloat() * 0.15F;
            }
            case KOZMIK -> {
                lifetime = 24 + random.nextInt(14);
                quadSize = 0.08F + random.nextFloat() * 0.07F;
                friction = 0.92F;
                float k = random.nextFloat();
                r = 0.55F - 0.25F * k;
                g = 0.35F + 0.3F * k;
                b = 1.0F;
            }
            default -> throw new IllegalStateException(tur.name());
        }
        kaldirma = kaldir;
        r0 = r;
        g0 = g;
        b0 = b;
        setColor(r, g, b);
        ilkBoy = quadSize;
        pickSprite(kareler);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float oran = age / (float) lifetime;
        switch (tur) {
            case PLAZMA -> {
                quadSize = ilkBoy * (1.0F - oran * 0.8F);
                setColor(1.0F, Mth.lerp(oran, 0.95F, 0.35F), Mth.lerp(oran, 0.7F, 0.08F));
                alpha = 1.0F - oran * oran;
            }
            case PLAZMA_AY -> {
                quadSize = ilkBoy * (1.0F - oran * 0.8F);
                setColor(Mth.lerp(oran, 0.9F, 0.35F), Mth.lerp(oran, 0.96F, 0.6F), 1.0F);
                alpha = 1.0F - oran * oran;
            }
            case DUMAN -> {
                quadSize = ilkBoy * (1.0F + oran * 2.6F);
                alpha = 0.55F * (1.0F - oran) * Math.min(1.0F, age / 6.0F);
                yd += kaldirma * (1.0F - oran) * (1.0F - oran);
            }
            case KIVILCIM -> {
                setColor(1.0F, 0.85F - 0.6F * oran, Math.max(0.05F, 0.5F - 0.5F * oran));
                alpha = oran < 0.6F ? 1.0F : 1.0F - (oran - 0.6F) / 0.4F;
            }
            case KIVILCIM_AY -> {
                setColor(0.85F - 0.3F * oran, 0.93F - 0.2F * oran, 1.0F);
                alpha = oran < 0.6F ? 1.0F : 1.0F - (oran - 0.6F) / 0.4F;
            }
            case TOZ -> {
                quadSize = ilkBoy * (1.0F + oran * 1.8F);
                alpha = 0.8F * (1.0F - oran);
            }
            case PARILTI, KOZMIK -> {
                float parilti = 0.5F + 0.5F * Mth.sin(faz + age * 0.9F);
                alpha = parilti * (oran < 0.7F ? 1.0F : 1.0F - (oran - 0.7F) / 0.3F);
                quadSize = ilkBoy * (0.6F + 0.6F * parilti);
                setColor(r0, g0, b0);
            }
            default -> {
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float kismiTick) {
        return tur == Tur.DUMAN || tur == Tur.TOZ ? super.getLightColor(kismiTick) : LightTexture.FULL_BRIGHT;
    }

    static final class Saglayici implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet kareler;
        private final Tur tur;

        Saglayici(SpriteSet kareler, Tur tur) {
            this.kareler = kareler;
            this.tur = tur;
        }

        @Override
        public Particle createParticle(SimpleParticleType tip, ClientLevel level, double x, double y, double z, double vx,
                double vy, double vz) {
            return new MeteorParcacigi(level, x, y, z, vx, vy, vz, kareler, tur);
        }
    }
}
