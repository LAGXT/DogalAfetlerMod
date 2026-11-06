package io.github.kadirberkpolat1.dogalafetler.client.yangin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Yangının kendi parçacıkları: tam parlak büyük alev, büyüyerek yükselen duman, alttan turuncu aydınlanan duman sütunu,
 * rüzgârla sürüklenip sönen kıvılcım ve sütunun tepesinde açık renkli pirokümülüs kümesi. Sütun ve küme parçacıkları
 * tavan sayımı için canlı listelerde tutulur.
 */
final class YanginParcacigi extends TextureSheetParticle {
    /** Canlı sütun ve küme parçacıkları; {@link IstemciYangin} her tick ayıklar. */
    static final List<YanginParcacigi> SUTUNLAR = new ArrayList<>();
    static final List<YanginParcacigi> KUMELER = new ArrayList<>();

    private final SpriteSet kareler;
    private final Tur tur;
    private final float ilkBoy;
    private final float gri;
    long sonGorulme;

    enum Tur {
        ALEV,
        DUMAN,
        SUTUN,
        KIVILCIM,
        KUME
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
        float g = 0;
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
                g = 0.18F + random.nextFloat() * 0.14F;
                alpha = 0.75F;
            }
            case SUTUN -> {
                lifetime = 200 + random.nextInt(80);
                quadSize = 2.5F + random.nextFloat() * 2.0F;
                friction = 0.995F;
                g = 0.16F + random.nextFloat() * 0.12F;
                alpha = 0.65F;
                SUTUNLAR.add(this);
            }
            case KIVILCIM -> {
                lifetime = 20 + random.nextInt(26);
                quadSize = 0.05F + random.nextFloat() * 0.05F;
                friction = 0.96F;
                gravity = -0.06F;
                setColor(1.0F, 0.85F, 0.45F);
            }
            case KUME -> {
                lifetime = 300 + random.nextInt(120);
                quadSize = 5.0F + random.nextFloat() * 4.0F;
                friction = 0.99F;
                g = 0.72F + random.nextFloat() * 0.16F;
                alpha = 0;
                KUMELER.add(this);
            }
            default -> throw new IllegalStateException(tur.name());
        }
        gri = g;
        if (g > 0) {
            setColor(g, g, g);
        }
        ilkBoy = quadSize;
        sonGorulme = IstemciYangin.saat();
        setSpriteFromAge(kareler);
    }

    @Override
    public void tick() {
        super.tick();
        sonGorulme = IstemciYangin.saat();
        if (removed) {
            return;
        }
        setSpriteFromAge(kareler);
        float oran = age / (float) lifetime;
        double[] ruzgar = IstemciYangin.ruzgar();
        switch (tur) {
            case ALEV -> quadSize = ilkBoy * (1.0F - oran * 0.6F);
            case DUMAN -> {
                quadSize = ilkBoy * (1.0F + oran * 1.5F);
                alpha = 0.75F * (1.0F - oran * oran);
                surukle(ruzgar, 0.004);
            }
            case SUTUN -> {
                quadSize = ilkBoy * (1.0F + oran * 2.0F);
                alpha = 0.65F * (1.0F - oran * oran);
                float sicak = Math.max(0.0F, 1.0F - oran * 4.0F);
                setColor(gri + 0.30F * sicak, gri + 0.10F * sicak, Math.max(0.0F, gri - 0.04F * sicak));
                surukle(ruzgar, 0.002);
            }
            case KIVILCIM -> {
                setColor(1.0F - 0.15F * oran, 0.85F - 0.65F * oran, Math.max(0.05F, 0.45F - 0.45F * oran));
                float sonum = oran < 0.6F ? 1.0F : 1.0F - (oran - 0.6F) / 0.4F;
                alpha = sonum * (0.6F + 0.4F * random.nextFloat());
                surukle(ruzgar, 0.006);
            }
            case KUME -> {
                quadSize = ilkBoy * (1.0F + oran * 0.6F);
                alpha = 0.55F * Math.min(1.0F, oran * 8.0F) * (oran < 0.6F ? 1.0F : 1.0F - (oran - 0.6F) / 0.4F);
                surukle(ruzgar, 0.001);
            }
            default -> {
            }
        }
    }

    private void surukle(double[] ruzgar, double kat) {
        xd += ruzgar[0] * kat;
        zd += ruzgar[1] * kat;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float kismiTick) {
        return switch (tur) {
            case ALEV, KIVILCIM -> LightTexture.FULL_BRIGHT;
            case SUTUN, KUME -> {
                int isik = super.getLightColor(kismiTick);
                int blok = Math.max(LightTexture.block(isik), tur == Tur.KUME ? 9 : 11);
                yield LightTexture.pack(blok, LightTexture.sky(isik));
            }
            default -> super.getLightColor(kismiTick);
        };
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
