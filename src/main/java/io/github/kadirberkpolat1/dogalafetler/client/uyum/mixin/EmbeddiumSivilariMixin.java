package io.github.kadirberkpolat1.dogalafetler.client.uyum.mixin;

import io.github.kadirberkpolat1.dogalafetler.client.su.SuGizleme;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Embeddium / Oculus uyumu (K-E83): tsunami öncesi çekilme gerçek suyun üst katmanlarını sorgu anında gizler. Vanilla'da bunu
 * {@code LiquidBlockRenderer.tesselate} üzerindeki {@code LiquidBlockRendererMixin} yapar; Embeddium chunk'ları kendi
 * {@code FluidRenderer}'ıyla örer ve vanilla sıvı çizicisini hiç çağırmaz, bu yüzden Embeddium (ve ona bağlı Oculus) yüklükken
 * çekilme gerçek suyu gizlemiyor, deniz yerinde kalıyordu. Aynı {@link SuGizleme} sorgusu burada da çağrılır.
 *
 * <p>Hedef Embeddium'un kendi sınıfıdır (dağıtım adlarıyla, {@code remap=false}; derleme için {@code src/uyumApi} taşlakları,
 * imza Embeddium 0.3.31 jar'ından); yalnız Embeddium yüklükken uygulanır ({@code PunchyMixinEklentisi}). {@code WorldSlice},
 * {@code BlockAndTintGetter}'ı uygular. Sınıf ya da imza değişmişse enjeksiyon etkisiz kalır ({@code defaultRequire 0}) ve
 * uyumsuzluk oyunu çökertmez.
 */
@Mixin(value = FluidRenderer.class, remap = false)
public abstract class EmbeddiumSivilariMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void dogalafetler$cekilme(WorldSlice dunya, FluidState sivi, BlockPos pos, BlockPos konum,
            ChunkBuildBuffers tamponlar, CallbackInfo ci) {
        if (SuGizleme.gizli(dunya, pos, sivi)) {
            ci.cancel();
        }
    }
}
