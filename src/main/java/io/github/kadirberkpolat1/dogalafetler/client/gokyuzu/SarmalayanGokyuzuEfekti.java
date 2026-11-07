package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi.IstemciAy;
import io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi.IstemciAyEfektleri;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuEgrileri;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Kanca (b): {@code minecraft:overworld} efekt kimliğine kayıtlı nesneyi (vanilla ya da başka bir modun efekti) sarar.
 * Gökyüzünü yalnız Overworld boyutunda ve durum vanilla değilken {@link GokyuzuCizici} çizer; diğer her durumda ve
 * aynı efekt kimliğini kullanan mod boyutlarında her çağrı iç nesneye gider (N11: başka boyutun göğü değişmez).
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

    private static boolean overworldMu(@Nullable ClientLevel level) {
        return level != null && level.dimension() == Level.OVERWORLD;
    }

    /**
     * Normal güneş + ay yok iken tam gündüzde (yıldızlar görünmezken, ay ufkun en az ~22° altında) gök vanilla ile
     * aynıdır; çizim iç nesneye bırakılır ki başka modun ya da şader paketinin gündüz göğü korunsun. Ay çarpmasının
     * yaklaşması ve çarpma izleri (patlama, enkaz halkası) gündüz de bizim çizimimizdir.
     */
    private static boolean bizCiziyoruz(@Nullable ClientLevel level, float zaman, IstemciGokyuzu.Gorunum d) {
        if (IstemciGokyuzu.kanca != IstemciGokyuzu.Kanca.SARMALAYICI || !overworldMu(level) || d.vanillaMi()) {
            return false;
        }
        return IstemciAy.gokyuzuEtkin() || !(d.evre() == GunesEvresi.NORMAL && (d.onceki() == null || d.gecis() >= 1f)
                && GokyuzuRenkleri.geceligi(zaman) <= 0f);
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
        IstemciGokyuzu.Gorunum d = IstemciGokyuzu.gorunum(pt);
        if (r == null || !bizCiziyoruz(Minecraft.getInstance().level, zaman, d)) {
            return r;
        }
        return switch (d.evre()) {
            case GUNESSIZ, SUPERNOVA -> null;
            case KIZIL_CUCE -> {
                // Doğuş (ilerleme < 1): ufuktaki kızıl yay daha yoğun; renk gücü 0 iken vanilla yelpaze.
                float k = d.renkGucu();
                yield new float[] {Mth.lerp(k, r[0], 1f), Mth.lerp(k, r[1], 0.28f + 0.1f * r[1]), Mth.lerp(k, r[2], 0.12f),
                        Math.min(1f, r[3] * Mth.lerp(k, 1f, GokyuzuEgrileri.kizilCuceUfukGucu(d.ilerleme())))};
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
        if (bizCiziyoruz(level, level.getTimeOfDay(pt), IstemciGokyuzu.gorunum(pt))) {
            GokyuzuCizici.ciz(level, pt, poz, kamera, izdusum, sisli, sisKur);
            return true;
        }
        return ic.renderSky(level, ticks, pt, poz, kamera, izdusum, sisli, sisKur);
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float pt, PoseStack poz, double x, double y, double z,
            Matrix4f izdusum) {
        IstemciGokyuzu.Gorunum d = IstemciGokyuzu.gorunum(pt);
        if (bizCiziyoruz(level, level.getTimeOfDay(pt), d) && d.evre() == GunesEvresi.GUNESSIZ) {
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
        IstemciGokyuzu.Gorunum d = IstemciGokyuzu.gorunum(pt);
        if (overworldMu(level) && !d.vanillaMi()) {
            GokyuzuRenkleri.isikHaritasi(d, gokKararmasi, gokIsigi, renk);
            IstemciAyEfektleri.isikHaritasi(pt, renk);
        }
    }
}
