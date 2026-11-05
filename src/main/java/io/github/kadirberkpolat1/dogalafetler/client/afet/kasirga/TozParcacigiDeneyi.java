package io.github.kadirberkpolat1.dogalafetler.client.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaVarligi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.RankineProfili;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Spike karşılaştırması (soru 2): taban enkaz halkasını renderer'ın kendi dörtgen listesi yerine vanilla parçacık
 * motoruyla (TextureSheetParticle, PARTICLE_SHEET_TRANSLUCENT) çizer. Yörünge matematiği renderer'daki taban
 * halkasıyla aynıdır; fark yalnız çizim yolu (20 Hz parçacık tick'i + parçacık motorunun kendi tabanı).
 */
final class TozParcacigiDeneyi {
    private static final List<TozParcacigi> PARCACIKLAR = new ArrayList<>();

    private TozParcacigiDeneyi() {
    }

    static void baslat(KasirgaVarligi kasirga, int adet) {
        Minecraft mc = Minecraft.getInstance();
        // Parçacık atlası ModelManager'ın atlas kümesinde yok (getTextureAtlas NPE verir); ParticleEngine onu
        // TextureManager'a kaydeder.
        TextureAtlas atlas = (TextureAtlas) mc.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        for (int i = 0; i < adet; i++) {
            TozParcacigi p = new TozParcacigi(mc.level, kasirga, i);
            p.dokuyuAyarla(atlas.getSprite(ResourceLocation.fromNamespaceAndPath("minecraft", "big_smoke_" + (i % 4 + 2))));
            mc.particleEngine.add(p);
            PARCACIKLAR.add(p);
        }
    }

    static void bitir() {
        PARCACIKLAR.forEach(TozParcacigi::remove);
        PARCACIKLAR.clear();
    }

    static int sayi() {
        return PARCACIKLAR.size();
    }

    static final class TozParcacigi extends TextureSheetParticle {
        private final KasirgaVarligi kasirga;
        private final double yaricap;
        private final double aci0;
        private final double faz;
        private final double dikeyHiz;
        private final double hMax;
        private final double hizCarpani;
        private final float tabanBoyut;

        TozParcacigi(ClientLevel seviye, KasirgaVarligi kasirga, int i) {
            super(seviye, kasirga.getX(), kasirga.getY(), kasirga.getZ());
            this.kasirga = kasirga;
            KasirgaSeviyesi s = kasirga.seviyeBilgisi();
            int tohum = kasirga.getId() * 7919;
            double rT = s.tabanYaricapi();
            double u1 = KasirgaRenderer.rastgele(i, 1, tohum);
            yaricap = rT * (0.45 + 1.35 * Math.pow(u1, 0.8));
            faz = KasirgaRenderer.rastgele(i, 2, tohum);
            dikeyHiz = 1.5 + 2.5 * KasirgaRenderer.rastgele(i, 3, tohum);
            aci0 = KasirgaRenderer.rastgele(i, 4, tohum) * Math.PI * 2;
            hizCarpani = 0.85 + 0.3 * KasirgaRenderer.rastgele(i, 5, tohum);
            hMax = s.enkazYuksekligi() * (1.15 - 0.75 * (yaricap / rT - 0.45) / 1.35);
            tabanBoyut = (float) Mth.clamp(rT * (0.16 + 0.2 * KasirgaRenderer.rastgele(i, 6, tohum)), 0.7, 6);
            lifetime = Integer.MAX_VALUE;
            hasPhysics = false;
            gravity = 0;
            setColor(0.50f, 0.43f, 0.35f);
            konumla();
            xo = x;
            yo = y;
            zo = z;
        }

        void dokuyuAyarla(net.minecraft.client.renderer.texture.TextureAtlasSprite doku) {
            setSprite(doku);
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (kasirga.isRemoved()) {
                remove();
                return;
            }
            age++;
            konumla();
        }

        private void konumla() {
            double t = kasirga.tickCount / 20.0;
            RankineProfili rk = kasirga.rankine();
            double fy = faz + t * dikeyHiz / hMax;
            fy -= Math.floor(fy);
            double aci = aci0 - rk.acisalHiz(yaricap) * t * hizCarpani;
            setPos(kasirga.getX() + Math.cos(aci) * yaricap, kasirga.getY() + fy * hMax,
                    kasirga.getZ() + Math.sin(aci) * yaricap);
            quadSize = tabanBoyut * (float) (0.7 + 0.6 * fy);
            alpha = (float) (0.85 * Math.pow(Math.sin(Math.PI * fy), 0.7));
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}
