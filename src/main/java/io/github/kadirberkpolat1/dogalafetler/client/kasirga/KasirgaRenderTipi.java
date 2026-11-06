package io.github.kadirberkpolat1.dogalafetler.client.kasirga;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Kasırganın yarı saydam bulut render tipi: vanilla {@code entity_translucent} şaderı (şader yazmadan), kendi atlasımız
 * bulanık filtreyle, arka yüz kesmesi yok, derinlik yazmaz (yarı saydam katmanlar birbirini kesmesin), yüklemede
 * dörtgenler kameraya göre sıralanır. {@code entityTranslucent}'tan tek farkı bulanık doku ve derinlik yazmaması.
 */
final class KasirgaRenderTipi extends RenderType {
    static final RenderType BULUT = create("dogalafetler_kasirga_bulut", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1 << 18, true, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(KasirgaDokusu.KONUM, true, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private KasirgaRenderTipi(String ad, VertexFormat bicim, VertexFormat.Mode kip, int boyut, boolean kirilma,
            boolean siralama, Runnable kur, Runnable temizle) {
        super(ad, bicim, kip, boyut, kirilma, siralama, kur, temizle);
    }
}
