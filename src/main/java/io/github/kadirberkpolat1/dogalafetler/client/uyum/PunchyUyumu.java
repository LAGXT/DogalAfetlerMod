package io.github.kadirberkpolat1.dogalafetler.client.uyum;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.SarsintiZarfi;
import io.github.kadirberkpolat1.dogalafetler.client.core.EkranEfektKapisi;
import io.github.kadirberkpolat1.dogalafetler.client.deprem.IstemciSarsinti;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Punchy uyumunun birinci kişi tarafı (K-E74). Punchy kolları kendi modeliyle {@code ItemInHandRenderer.renderHandsWithItems}'in
 * BAŞINDA çizer ve vanilla kol çizimini iptal eder; bu yüzden {@code RenderHandEvent} (deprem el sarsıntısı,
 * {@code IstemciSarsinti#el}) Punchy kollarına ulaşmaz. {@code client.uyum.mixin.GameRendererElMixin} yalnız Punchy yüklüyken
 * ({@code PunchyMixinEklentisi}) {@code GameRenderer#renderItemInHand} içindeki bu çağrıyı sarar: çizimden ÖNCE eşleşmeli
 * bir dönüşüm eklenir, böylece Punchy'ye de vanilla'ya da dokunmadan üstüne binen bir el sarsıntısı olur. Punchy'nin
 * kendi kamera animasyonu ({@code enableCameraAnimations}) {@code Camera#setup}'ta çalışır; kamera sarsıntımız
 * ({@code ViewportEvent.ComputeCameraAngles}) ondan sonra eklenir, çakışmaz.
 */
public final class PunchyUyumu {
    /** Bu genliğin (derece) altında el sallanmaz. */
    private static final double ESIK = 0.15;
    private static final double YALPA_SINIRI = 3;

    private PunchyUyumu() {
    }

    /**
     * {@code renderHandsWithItems} çağrısının yerine geçer: sarsıntı varsa kol çiziminin önüne eşleşmeli bir dönüşüm koyar,
     * yoksa doğrudan çağırır.
     */
    public static void elCiz(ItemInHandRenderer el, float kismi, PoseStack yigin, MultiBufferSource.BufferSource tampon,
            LocalPlayer oyuncu, int isik) {
        double genlik = EkranEfektKapisi.izinli(oyuncu) ? IstemciSarsinti.genlikDerece(oyuncu, kismi) : 0;
        if (genlik <= ESIK) {
            el.renderHandsWithItems(kismi, yigin, tampon, oyuncu, isik);
            return;
        }
        double t = IstemciSarsinti.sarsintiSaati(kismi);
        double olcek = Math.min(1, genlik / 2.0) * 0.014;
        float yalpa = (float) Math.max(-YALPA_SINIRI, Math.min(YALPA_SINIRI, genlik * 0.8 * SarsintiZarfi.yuvarlanma(t)));
        yigin.pushPose();
        yigin.translate(olcek * SarsintiZarfi.titreme(t, 1), olcek * SarsintiZarfi.titreme(t + 0.37, 0), 0);
        yigin.mulPose(Axis.ZP.rotationDegrees(yalpa));
        try {
            el.renderHandsWithItems(kismi, yigin, tampon, oyuncu, isik);
        } finally {
            yigin.popPose();
        }
    }
}
