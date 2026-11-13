package io.github.kadirberkpolat1.dogalafetler.client.uyum;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.SarsintiZarfi;
import io.github.kadirberkpolat1.dogalafetler.client.deprem.IstemciSarsinti;
import java.util.ArrayDeque;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Deprem sırasında karakter sallanması (K-E74, Punchy uyumu). Punchy'nin üçüncü kişi animasyonları için açık bir API'si yoktur
 * (kendi {@code PlayerRenderer} kancası vardır); bu yüzden en az riskli yol izlenir: kamera sarsıntısının
 * ({@link IstemciSarsinti}) aynı genliği ve fazlarıyla, oyuncu modelinin {@link PoseStack}'ine çizimden hemen önce bir ek
 * dönüşüm eklenir. Dönüşüm Punchy'nin (ve Not Enough Animations gibi modların) kendi kemik animasyonlarının ÜSTÜNE binen,
 * dünya eşleşmeli bir yalpadır; hiçbir animasyonu bozmaz ve mod olmasa da (düz üçüncü kişi görünümü, başkalarının modeli)
 * çalışır. Her oyuncu kendi konumundaki genlikle sallanır. Birinci kişi kol sarsıntısı için {@link PunchyUyumu} ve
 * {@code client.uyum.mixin}. Genlik kullanıcının {@code deprem.sarsintiCarpani} ayarıyla ölçeklenir (0 = kapalı).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class GovdeSarsintisi {
    /** Bu genliğin (derece) altında gövde sallanmaz. */
    private static final double ESIK = 0.15;
    /** Yalpa ve dönüşün derece sınırları: abartı sarsıntıda bile karakter ters dönmez. */
    private static final double YALPA_SINIRI = 7;
    private static final double SALINIM_SINIRI = 5;
    /** Modelin dönme eksenini kalça hizasına taşıyan yükseklik (blok). */
    private static final double KALCA = 0.9;

    /** Çizimde ({@code Pre}) push yapıldı mı bilgisi, iç içe çizimlerde de eşleşsin diye yığın (yalnız istemci çizim kolu). */
    private static final ArrayDeque<Boolean> ITILDI = new ArrayDeque<>(4);

    private GovdeSarsintisi() {
    }

    /** Son sırada çalışır: başka bir işleyici çizimi iptal edip eşleşmesiz push bırakmasın. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void once(RenderPlayerEvent.Pre olay) {
        Player oyuncu = olay.getEntity();
        float kismi = olay.getPartialTick();
        double genlik = oyuncu.isSpectator() ? 0 : IstemciSarsinti.genlikDerece(oyuncu, kismi);
        if (genlik <= ESIK) {
            ITILDI.push(Boolean.FALSE);
            return;
        }
        uygula(olay.getPoseStack(), genlik, IstemciSarsinti.sarsintiSaati(kismi));
        ITILDI.push(Boolean.TRUE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void sonra(RenderPlayerEvent.Post olay) {
        if (!ITILDI.isEmpty() && ITILDI.pop()) {
            olay.getPoseStack().popPose();
        }
    }

    /** Kamera titremesiyle aynı dalga: kalçada yalpa (roll), hafif dönüş (yaw) ve yatay sarsılma. */
    private static void uygula(PoseStack yigin, double genlik, double t) {
        float yalpa = (float) clamp(genlik * 1.6 * SarsintiZarfi.yuvarlanma(t), YALPA_SINIRI);
        float salinim = (float) clamp(genlik * 1.1 * SarsintiZarfi.titreme(t, 0), SALINIM_SINIRI);
        double kayma = Math.min(0.08, genlik * 0.012);
        yigin.pushPose();
        yigin.translate(kayma * SarsintiZarfi.titreme(t, 1), 0, kayma * SarsintiZarfi.titreme(t + 0.21, 0));
        yigin.translate(0, KALCA, 0);
        yigin.mulPose(Axis.ZP.rotationDegrees(yalpa));
        yigin.mulPose(Axis.YP.rotationDegrees(salinim));
        yigin.translate(0, -KALCA, 0);
    }

    private static double clamp(double deger, double sinir) {
        return Math.max(-sinir, Math.min(sinir, deger));
    }
}
