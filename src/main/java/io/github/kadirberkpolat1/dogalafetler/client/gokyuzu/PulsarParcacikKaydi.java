package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarKayitlari;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Pulsar parçacıklarının istemci sağlayıcıları (mod veri yolu) ve kül katmanının kor kancasının dolumu: kor yalnız evre
 * PULSAR ve {@code afetBlokHasari} açıkken görünür ({@link IstemciDonem}).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PulsarParcacikKaydi {
    private PulsarParcacikKaydi() {
    }

    @SubscribeEvent
    public static void parcaciklar(RegisterParticleProvidersEvent olay) {
        olay.registerSpriteSet(PulsarKayitlari.KUL_YAGISI.get(), s -> new Saglayici(s, Tur.KUL_YAGISI));
        olay.registerSpriteSet(PulsarKayitlari.KOR.get(), s -> new Saglayici(s, Tur.KOR));
        olay.registerSpriteSet(PulsarKayitlari.KUL_TOZU.get(), s -> new Saglayici(s, Tur.KUL_TOZU));
    }

    @SubscribeEvent
    public static void kurulum(FMLClientSetupEvent olay) {
        PulsarKayitlari.korGorunur = () -> {
            DonemPaketi d = IstemciDonem.durum();
            return d.gunesEvresi() == GunesEvresi.PULSAR && d.blokHasari();
        };
    }

    private enum Tur {
        KUL_YAGISI,
        KOR,
        KUL_TOZU
    }

    /**
     * Kül pulu: dört kareden biri doğuşta seçilir; dikey hız kendi uç hızına (0,02–0,05 blok/tick) yaklaşır, yatay hız
     * iki eksende farklı frekanslı salınıma; yavaşça döner, ömrü 120–200 tick, yere değince söner. Kor: tam parlak,
     * kısa ömürlü, yükselir ve turuncudan koyu kızıla soluklar. Kül tozu: yerden kısa savrulur, sürtünmeyle durur.
     */
    private static final class KulParcacigi extends TextureSheetParticle {
        private static final int KARE_SAYISI = 4;
        private static final int SONME_SURESI = 10;

        private final Tur tur;
        private final SpriteSet kareler;
        private final float ucHizi;
        private final float salinimFazi;
        private final float salinimHizi;
        private final float salinimGenligi;
        private final float donmeHizi;
        private final float tamAlfa;
        private final float ilkBoy;
        private int sonme = -1;

        private KulParcacigi(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                SpriteSet kareler, Tur tur) {
            super(level, x, y, z);
            this.tur = tur;
            this.kareler = kareler;
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.gravity = 0;
            float faz = random.nextFloat() * Mth.TWO_PI;
            switch (tur) {
                case KUL_YAGISI -> {
                    hasPhysics = true;
                    friction = 1.0F;
                    lifetime = 120 + random.nextInt(81);
                    quadSize = 0.05F + random.nextFloat() * 0.06F;
                    ucHizi = 0.02F + random.nextFloat() * 0.03F;
                    salinimHizi = 0.06F + random.nextFloat() * 0.08F;
                    salinimGenligi = 0.015F + random.nextFloat() * 0.02F;
                    donmeHizi = (random.nextFloat() - 0.5F) * 0.12F;
                    tamAlfa = 0.75F + random.nextFloat() * 0.2F;
                    float g = 0.10F + random.nextFloat() * 0.25F;
                    setColor(g, g * 0.97F, g * 0.95F);
                    setSprite(kareler.get(random.nextInt(KARE_SAYISI), KARE_SAYISI - 1));
                }
                case KOR -> {
                    hasPhysics = false;
                    friction = 0.96F;
                    lifetime = 14 + random.nextInt(18);
                    quadSize = 0.04F + random.nextFloat() * 0.04F;
                    ucHizi = 0;
                    salinimHizi = 0.3F + random.nextFloat() * 0.3F;
                    salinimGenligi = 0.004F;
                    donmeHizi = 0;
                    tamAlfa = 1.0F;
                    gravity = -0.02F;
                    setColor(1.0F, 0.55F, 0.15F);
                    setSpriteFromAge(kareler);
                }
                default -> {
                    hasPhysics = true;
                    friction = 0.86F;
                    lifetime = 10 + random.nextInt(11);
                    quadSize = 0.08F + random.nextFloat() * 0.08F;
                    ucHizi = 0;
                    salinimHizi = 0;
                    salinimGenligi = 0;
                    donmeHizi = (random.nextFloat() - 0.5F) * 0.2F;
                    tamAlfa = 0.6F + random.nextFloat() * 0.2F;
                    gravity = 0.02F;
                    float g = 0.28F + random.nextFloat() * 0.2F;
                    setColor(g, g, g * 0.96F);
                    setSprite(kareler.get(random.nextInt(KARE_SAYISI), KARE_SAYISI - 1));
                }
            }
            salinimFazi = faz;
            ilkBoy = quadSize;
            roll = oRoll = random.nextFloat() * Mth.TWO_PI;
            alpha = tur == Tur.KUL_YAGISI ? 0 : tamAlfa;
        }

        @Override
        public void tick() {
            oRoll = roll;
            super.tick();
            if (removed) {
                return;
            }
            float oran = age / (float) lifetime;
            roll += donmeHizi;
            switch (tur) {
                case KUL_YAGISI -> {
                    if (sonme < 0 && (onGround || y == yo)) {
                        sonme = SONME_SURESI;
                    }
                    if (sonme >= 0) {
                        alpha = tamAlfa * sonme / SONME_SURESI;
                        xd = 0;
                        yd = 0;
                        zd = 0;
                        roll = oRoll;
                        if (sonme-- == 0) {
                            remove();
                        }
                        return;
                    }
                    float faz = salinimFazi + age * salinimHizi;
                    xd += (salinimGenligi * Mth.sin(faz) - xd) * 0.15;
                    zd += (salinimGenligi * Mth.sin(faz * 0.7F + 2.1F) - zd) * 0.15;
                    yd += (-ucHizi - yd) * 0.1;
                    alpha = tamAlfa * Math.min(1.0F, age / 6.0F) * Math.min(1.0F, (lifetime - age) / 20.0F);
                }
                case KOR -> {
                    setSpriteFromAge(kareler);
                    xd += salinimGenligi * Mth.sin(salinimFazi + age * salinimHizi);
                    setColor(1.0F - 0.25F * oran, 0.55F - 0.45F * oran, Math.max(0.02F, 0.15F - 0.15F * oran));
                    quadSize = ilkBoy * (1.0F - 0.5F * oran);
                    alpha = tamAlfa * (oran < 0.5F ? 1.0F : 1.0F - (oran - 0.5F) * 2.0F) * (0.7F + 0.3F * random.nextFloat());
                }
                default -> {
                    quadSize = ilkBoy * (1.0F + oran);
                    alpha = tamAlfa * (1.0F - oran);
                }
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        @Override
        public int getLightColor(float kismiTick) {
            return tur == Tur.KOR ? LightTexture.FULL_BRIGHT : super.getLightColor(kismiTick);
        }
    }

    private static final class Saglayici implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet kareler;
        private final Tur tur;

        Saglayici(SpriteSet kareler, Tur tur) {
            this.kareler = kareler;
            this.tur = tur;
        }

        @Override
        public Particle createParticle(SimpleParticleType tip, ClientLevel level, double x, double y, double z,
                double vx, double vy, double vz) {
            return new KulParcacigi(level, x, y, z, vx, vy, vz, kareler, tur);
        }
    }
}
