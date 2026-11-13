package io.github.kadirberkpolat1.dogalafetler.client.uyum.mixin;

import io.github.kadirberkpolat1.dogalafetler.balikcilik.KutupOltasi;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import punchy.client.render.FishingLineAttachment;

/**
 * Punchy uyumu (K-E79): Punchy birinci kişide misinayı oltanın MODEL ucuna bağlar ({@code FishingLineAttachment}: ucu ürettiği
 * model köşelerinden yakalar, sonra dünya koordinatına çevirir). Yakalama üç yerde yalnız vanilla oltaya ({@code stack.is(
 * Items.FISHING_ROD)}) izin verir: {@code original$begin} (yakalamayı açar), {@code capture} (ucu hesaplar) ve
 * {@code getWorldAnchor} (misinanın başlayacağı noktayı verir). Kutup Oltası ayrı bir eşya olduğundan ucu hiç yakalanmıyor,
 * Punchy vanilla olta için ayarlı kol-kavrama yedeğine düşüyor ve misina oltadan ayrı, havada çiziliyordu. Bu üç denetimde Kutup
 * Oltası da vanilla olta sayılır; ucun bulunması Punchy'nin vanilla oltadaki koduyla aynıdır (modellerimiz vanilla olta
 * modelinin çocuğu ve dokusu vanilla maskesiyle aynı, bkz. {@code tools/assets/kutup_dokular.py}).
 *
 * <p>Hedef Punchy'nin kendi sınıfıdır (dağıtım adlarıyla, {@code remap=false}; derleme için {@code src/uyumApi} taslağı); yalnız Punchy yüklüyken uygulanır
 * ({@code PunchyMixinEklentisi}). Çağrı hedefi Forge çalışma zamanı (SRG) adıdır: {@code ItemStack.is(Item)} = {@code m_150930_}
 * (Punchy bayt kodunda böyle görünür); Punchy başka bir Minecraft sürümüne taşınırsa eşleşme olmaz ve mixin sessizce etkisiz kalır
 * ({@code defaultRequire 0}), misina vanilla yedeğine düşer.
 */
@Mixin(value = FishingLineAttachment.class, remap = false)
public abstract class FishingLineAttachmentMixin {
    @Redirect(method = {"original$begin", "capture", "getWorldAnchor"}, remap = false,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/world/item/ItemStack;m_150930_(Lnet/minecraft/world/item/Item;)Z"))
    private static boolean dogalafetler$olta(ItemStack yigin, Item esya) {
        return yigin.is(esya) || esya == Items.FISHING_ROD && KutupOltasi.mi(yigin);
    }
}
