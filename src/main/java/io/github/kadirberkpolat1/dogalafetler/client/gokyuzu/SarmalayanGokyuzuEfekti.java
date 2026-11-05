package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuDurumu;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GunesEvresi;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Kanca (b): kayıt anında {@code minecraft:overworld} için kayıtlı olan efekt nesnesini (vanilla ya da bizden önce
 * yüklenen başka bir mod) sarar. Durum vanilla iken (ay var + normal güneş) her çağrı iç nesneye gider; başka
 * durumda gökyüzünü {@link GokyuzuCizici} çizer.
 */
public final class SarmalayanGokyuzuEfekti extends DimensionSpecialEffects {
    private final DimensionSpecialEffects ic;

    public SarmalayanGokyuzuEfekti(DimensionSpecialEffects ic) {
        super(ic.getCloudHeight(), ic.hasGround(), ic.skyType(), ic.forceBrightLightmap(), ic.constantAmbientLight());
        this.ic = ic;
    }

    public DimensionSpecialEffects ic() {
        return ic;
    }

    private static boolean bizCiziyoruz(GokyuzuDurumu d) {
        return IstemciGokyuzu.kanca == IstemciGokyuzu.Kanca.SARMALAYICI && !d.vanillaMi();
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 renk, float parlaklik) {
        return ic.getBrightnessDependentFogColor(renk, parlaklik);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return ic.isFoggyAt(x, z);
    }

    @Override
    @Nullable
    public float[] getSunriseColor(float zaman, float pt) {
        float[] r = ic.getSunriseColor(zaman, pt);
        GokyuzuDurumu d = IstemciGokyuzu.durum();
        if (r == null || !bizCiziyoruz(d)) {
            return r;
        }
        return switch (d.evre()) {
            case GUNESSIZ, SUPERNOVA -> null;
            case KIZIL_CUCE -> {
                // Doğuş (ilerleme < 1): ufuktaki kızıl yay daha yoğun.
                float dogus = 1f - d.ilerleme();
                yield new float[] {1f, 0.28f + 0.1f * r[1], 0.12f, Math.min(1f, r[3] * (1.1f + 1.6f * dogus))};
            }
            default -> r;
        };
    }

    @Override
    public float getCloudHeight() {
        return ic.getCloudHeight();
    }

    @Override
    public boolean hasGround() {
        return ic.hasGround();
    }

    @Override
    public SkyType skyType() {
        return ic.skyType();
    }

    @Override
    public boolean forceBrightLightmap() {
        return ic.forceBrightLightmap();
    }

    @Override
    public boolean constantAmbientLight() {
        return ic.constantAmbientLight();
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float pt, PoseStack poz, Camera kamera, Matrix4f izdusum,
            boolean sisli, Runnable sisKur) {
        if (bizCiziyoruz(IstemciGokyuzu.durum())) {
            GokyuzuCizici.ciz(level, pt, poz, kamera, izdusum, sisli, sisKur);
            return true;
        }
        return ic.renderSky(level, ticks, pt, poz, kamera, izdusum, sisli, sisKur);
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float pt, PoseStack poz, double x, double y, double z,
            Matrix4f izdusum) {
        GokyuzuDurumu d = IstemciGokyuzu.durum();
        if (d.evre() == GunesEvresi.GUNESSIZ) {
            // Karanlık uzay göğünde parlak beyaz bulut tutarsız (tasarım notu §2 Bulutlar).
            return true;
        }
        return ic.renderClouds(level, ticks, pt, poz, x, y, z, izdusum);
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float pt, LightTexture isik, double x, double y,
            double z) {
        return ic.renderSnowAndRain(level, ticks, pt, isik, x, y, z);
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera kamera) {
        return ic.tickRain(level, ticks, kamera);
    }

    @Override
    public void adjustLightmapColors(ClientLevel level, float pt, float gokKararmasi, float blokKirmizi,
            float gokIsigi, int pikselX, int pikselY, Vector3f renk) {
        ic.adjustLightmapColors(level, pt, gokKararmasi, blokKirmizi, gokIsigi, pikselX, pikselY, renk);
        GokyuzuDurumu d = IstemciGokyuzu.durum();
        if (!d.vanillaMi()) {
            GokyuzuRenkleri.isikHaritasi(d, gokKararmasi, gokIsigi, renk);
        }
    }
}
