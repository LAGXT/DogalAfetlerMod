package io.github.kadirberkpolat1.dogalafetler.client.uyum.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kadirberkpolat1.dogalafetler.client.uyum.PunchyUyumu;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Punchy uyumu (K-E74): birinci kişi kol çizimini deprem el sarsıntısı dönüşümüyle sarar. Yalnız Punchy yüklüyken uygulanır
 * ({@code PunchyMixinEklentisi}); gerekçe ve isteğe bağlı olması için {@link PunchyUyumu}.
 */
@Mixin(GameRenderer.class)
public class GameRendererElMixin {
    @Redirect(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V"))
    private void dogalafetler$elSarsintisi(ItemInHandRenderer el, float kismi, PoseStack yigin,
            MultiBufferSource.BufferSource tampon, LocalPlayer oyuncu, int isik) {
        PunchyUyumu.elCiz(el, kismi, yigin, tampon, oyuncu, isik);
    }
}
