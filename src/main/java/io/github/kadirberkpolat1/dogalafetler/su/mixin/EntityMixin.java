package io.github.kadirberkpolat1.dogalafetler.su.mixin;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sanal suyu vanilla sıvı durumuna ekler: {@code wasTouchingWater}, su yüksekliği ve göz sıvısı. Böylece yüzme
 * hareketi, zıplayarak yükselme, yüzme pozu, hava kaybı, boğulma, düşme hasarının sıfırlanması, ateşin sönmesi ve mob
 * yapay zekâsının suyu tanıması vanilla kodundan gelir. Gerçek suda vanilla değeri korunur ({@code max}). Gerekçe: Forge
 * 47.4.28'de bu iki metot olay atmaz ve olay yolu yüzme/mob yapay zekâsını karşılamadı (docs/KARARLAR.md K0-25, SU-03).
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private static boolean dogalafetler$ilk = true;

    @Shadow
    protected boolean wasTouchingWater;
    @Shadow
    protected Object2DoubleMap<TagKey<Fluid>> fluidHeight;
    @Shadow
    protected boolean firstTick;
    @Shadow(remap = false)
    protected Object2DoubleMap<FluidType> forgeFluidTypeHeight;
    @Shadow(remap = false)
    private FluidType forgeFluidTypeOnEyes;

    @Shadow
    protected abstract void doWaterSplashEffect();

    @Inject(method = "updateInWaterStateAndDoFluidPushing", at = @At("RETURN"), cancellable = true)
    private void dogalafetler$sanalSu(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        double h = SanalSu.mixinSuYuksekligi(self);
        if (h <= 0) {
            return;
        }
        if (dogalafetler$ilk) {
            dogalafetler$ilk = false;
            DogalAfetler.LOGGER.info("Sanal su: Entity kancası ilk kez çalıştı ({}, yükseklik {})", self, h);
        }
        fluidHeight.put(FluidTags.WATER, Math.max(h, fluidHeight.getDouble(FluidTags.WATER)));
        FluidType su = ForgeMod.WATER_TYPE.get();
        forgeFluidTypeHeight.put(su, Math.max(h, forgeFluidTypeHeight.getDouble(su)));
        if (!wasTouchingWater && !firstTick) {
            doWaterSplashEffect();
        }
        wasTouchingWater = true;
        self.resetFallDistance();
        self.clearFire();
        SanalSu.itmeUygula(self);
        cir.setReturnValue(true);
    }

    @Inject(method = "updateFluidOnEyes", at = @At("TAIL"))
    private void dogalafetler$goz(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (SanalSu.gozSudaMi(self)) {
            forgeFluidTypeOnEyes = ForgeMod.WATER_TYPE.get();
        }
    }
}
