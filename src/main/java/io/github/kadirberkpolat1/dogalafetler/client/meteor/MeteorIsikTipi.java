package io.github.kadirberkpolat1.dogalafetler.client.meteor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Meteor izi, plazma kabuğu ve yer diskinin sissiz additive çizimi (M-24): {@code RenderType.eyes} ile aynı gölgelendirici,
 * doku, toplamalı karışım ve yalnız renk yazımı; tek fark çizim sırasında sis başlangıcının sonsuza alınması ({@code eyes}
 * parça gölgelendiricisi rengi {@code linear_fog_fade} ile çarpar; gece görüş sisi 140 blok yükseklikten gelen izi
 * söndürüyordu). Sis eski değerine çizimden hemen sonra döner.
 */
final class MeteorIsikTipi extends RenderType {
    private static float eskiSis;

    private static final LayeringStateShard SISSIZ = new LayeringStateShard("dogalafetler_meteor_sissiz", () -> {
        eskiSis = RenderSystem.getShaderFogStart();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
    }, () -> RenderSystem.setShaderFogStart(eskiSis));

    static final Function<ResourceLocation, RenderType> ISIK = Util.memoize(doku -> create("dogalafetler_meteor_isik",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
            CompositeState.builder().setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(doku, false, false)).setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE).setLayeringState(SISSIZ).createCompositeState(false)));

    private MeteorIsikTipi(String ad, VertexFormat bicim, VertexFormat.Mode kip, int boyut, boolean dagilim,
            boolean siralama, Runnable kur, Runnable temizle) {
        super(ad, bicim, kip, boyut, dagilim, siralama, kur, temizle);
    }
}
