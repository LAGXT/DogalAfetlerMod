package punchy.client.render;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Derleme zamanı taşlak (jar'a girmez): Punchy 3.2'nin {@code punchy.client.render.FishingLineAttachment} sınıfının, mixin'imizin
 * ({@code client.uyum.mixin.FishingLineAttachmentMixin}) hedeflediği üç genel yöntemin imzaları. İmzalar Punchy 3.2a jar'ından
 * {@code javap} ile alınmıştır; Mixin anotasyon işlemcisi hedef sınıfı derleme sırasında görmek ister. Gerçek çalışma zamanı sınıfı
 * Punchy'den gelir.
 */
public final class FishingLineAttachment {
    private FishingLineAttachment() {
    }

    public static void original$begin(LocalPlayer oyuncu, HumanoidArm kol, ItemStack yigin) {
    }

    public static void capture(BakedModel model, ItemStack yigin, Matrix4f poz) {
    }

    public static Vec3 getWorldAnchor(LocalPlayer oyuncu, HumanoidArm kol) {
        return null;
    }
}
