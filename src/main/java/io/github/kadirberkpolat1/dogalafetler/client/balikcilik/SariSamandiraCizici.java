package io.github.kadirberkpolat1.dogalafetler.client.balikcilik;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.NadirSamandira;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.FishingHook;

/**
 * Vanilla şamandıra çizicisi; sunucu şamandırayı nadir bir şey için işaretlediyse ({@link NadirSamandira}) vanilla'nın kırmızı
 * şamandıra dokusu yerine sarı dokuyu kullanır. Çizimin kendisi vanilla'nındır (konum, misina, dönüş): yalnız vanilla dokusuna
 * istenen {@code entityCutout} çizim türü sarı dokunun türüyle değiştirilir; misina ({@code lineStrip}) ve diğer her şey aynı kalır.
 * İşaretsiz şamandıra ve başka oyuncuların oltaları tamamen vanilla yoldan çizilir.
 */
public final class SariSamandiraCizici extends FishingHookRenderer {
    private static final RenderType VANILLA_TUR = RenderType.entityCutout(new ResourceLocation("textures/entity/fishing_hook.png"));
    private static final RenderType SARI_TUR = RenderType.entityCutout(DogalAfetler.konum("textures/entity/sari_samandira.png"));

    public SariSamandiraCizici(EntityRendererProvider.Context baglam) {
        super(baglam);
    }

    @Override
    public void render(FishingHook olta, float yaw, float kismiTick, PoseStack poz, MultiBufferSource tampon, int isik) {
        super.render(olta, yaw, kismiTick, poz, NadirSamandira.isaretli(olta) ? sariTampon(tampon) : tampon, isik);
    }

    /** Vanilla şamandıra türünü sarı türle değiştiren sarmal; diğer türler aynen geçer. */
    private static MultiBufferSource sariTampon(MultiBufferSource asil) {
        return tur -> asil.getBuffer(tur.equals(VANILLA_TUR) ? SARI_TUR : tur);
    }
}
