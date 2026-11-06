package io.github.kadirberkpolat1.dogalafetler.client.su.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kadirberkpolat1.dogalafetler.client.su.SuGizleme;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tsunami öncesi çekilme: Forge 47.4.28'de sıvı tesselasyonunu atlatan olay yok (docs/KARARLAR.md K0-25, SU-05). */
@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void dogalafetler$cekilme(BlockAndTintGetter level, BlockPos pos, VertexConsumer tuketici, BlockState blok,
            FluidState sivi, CallbackInfo ci) {
        if (SuGizleme.gizli(level, pos, sivi)) {
            ci.cancel();
        }
    }
}
