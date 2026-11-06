package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge veri yolunda istemci olayları: sis rengi ve son çare kanca (c) yolu. */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class GokyuzuIstemciOlaylari {
    private GokyuzuIstemciOlaylari() {
    }

    /** Sis ve temizleme rengi (ufuk ve uzaktaki arazi). Kanca seçiminden bağımsız çalışır. */
    @SubscribeEvent
    public static void sisRengi(ViewportEvent.ComputeFogColor olay) {
        ClientLevel level = Minecraft.getInstance().level;
        IstemciGokyuzu.Gorunum d = IstemciGokyuzu.gorunum((float) olay.getPartialTick());
        if (level == null || d.vanillaMi() || level.dimension() != Level.OVERWORLD
                || olay.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float gun = GokyuzuRenkleri.gunIsigi(level.getTimeOfDay((float) olay.getPartialTick()));
        float[] c = GokyuzuRenkleri.sisRengi(d, new float[] {olay.getRed(), olay.getGreen(), olay.getBlue()}, gun);
        olay.setRed(c[0]);
        olay.setGreen(c[1]);
        olay.setBlue(c[2]);
    }

    @SubscribeEvent
    public static void asama(RenderLevelStageEvent olay) {
        if (olay.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        if (IstemciGokyuzu.kanca == IstemciGokyuzu.Kanca.AFTER_SKY) {
            GokyuzuCizici.afterSkyCiz(olay.getPoseStack(), olay.getProjectionMatrix(), olay.getPartialTick(),
                    olay.getCamera());
        }
    }
}
