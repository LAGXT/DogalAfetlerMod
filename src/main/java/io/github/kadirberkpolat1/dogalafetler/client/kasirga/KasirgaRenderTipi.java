package io.github.kadirberkpolat1.dogalafetler.client.kasirga;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Kasırganın yarı saydam bulut render tipi: vanilla {@code entity_translucent} şaderi (şader yazmadan), kendi atlasımız
 * bulanık filtreyle, arka yüz kesmesi yok, derinlik yazmaz (yarı saydam katmanlar birbirini kesmesin), yüklemede
 * dörtgenler kameraya göre sıralanır. {@code entityTranslucent}'tan farkları: bulanık doku, derinlik yazmaması ve sis yok.
 *
 * <p>Sis (K-E86): vanilla sis {@code entity_translucent}'ta rengi sis rengine çekip alfayı korur; sis bütünüyle bastığında
 * kasırga düz, opak bir sis renkli siluete dönüşür, arkasındaki gök (yıldızlar, gezegenler, kara delikler) kapanırdı.
 * Çizim sırasında sis uzaklıkları sonsuza alınır (sonra eski değerler geri yazılır); uzaklık etkisi renk yerine alfada
 * köşe başına verilir ({@code UzaklikSolmasi}).
 */
final class KasirgaRenderTipi extends RenderType {
    private static float eskiSisBaslangici;
    private static float eskiSisBitisi;

    /** Çizim boşken sisi kapatır (sis uzaklıkları çok büyük), çizim bitince eski sis uzaklıklarını geri koyar. */
    private static final LayeringStateShard SISSIZ = new LayeringStateShard("dogalafetler_kasirga_sissiz", () -> {
        eskiSisBaslangici = RenderSystem.getShaderFogStart();
        eskiSisBitisi = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(1.0e9f);
        RenderSystem.setShaderFogEnd(2.0e9f);
    }, () -> {
        RenderSystem.setShaderFogStart(eskiSisBaslangici);
        RenderSystem.setShaderFogEnd(eskiSisBitisi);
    });

    static final RenderType BULUT = create("dogalafetler_kasirga_bulut", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1 << 18, true, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(KasirgaDokusu.KONUM, true, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setLayeringState(SISSIZ)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private KasirgaRenderTipi(String ad, VertexFormat bicim, VertexFormat.Mode kip, int boyut, boolean kirilma,
            boolean siralama, Runnable kur, Runnable temizle) {
        super(ad, bicim, kip, boyut, kirilma, siralama, kur, temizle);
    }
}
