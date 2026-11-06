package io.github.kadirberkpolat1.dogalafetler.client.kasirga;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.UcanBlokVarligi;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

/** Gerçek uçan blok: tam boy blok modeli, havada takla atarak ({@code FallingBlockRenderer} gibi {@code renderSingleBlock}). */
final class UcanBlokRenderer extends EntityRenderer<UcanBlokVarligi> {
    private final BlockRenderDispatcher bloklar;

    UcanBlokRenderer(EntityRendererProvider.Context baglam) {
        super(baglam);
        bloklar = baglam.getBlockRenderDispatcher();
        shadowRadius = 0;
    }

    @Override
    public void render(UcanBlokVarligi blok, float yaw, float kismi, PoseStack poz, MultiBufferSource tampon, int isik) {
        if (IstemciKasirga.gizli) {
            return;
        }
        float t = blok.tickCount + kismi;
        poz.pushPose();
        poz.translate(0, 0.49, 0);
        poz.mulPose(Axis.YP.rotationDegrees(t * 9 + blok.getId() * 37));
        poz.mulPose(Axis.XP.rotationDegrees(t * 6 + blok.getId() * 11));
        poz.translate(-0.5, -0.5, -0.5);
        bloklar.renderSingleBlock(blok.blok(), poz, tampon, isik, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        poz.popPose();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTextureLocation(UcanBlokVarligi blok) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
