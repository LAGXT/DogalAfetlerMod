package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi.IstemciAy;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.ShaderKodu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Shader paketi köprüsü: güneş/ay dönemi, ışık tonu ve görsel afet bayrakları sis renginin görünmeyen kesrine yazılır
 * ({@link ShaderKodu}). En son çalışmalı: öteki sis rengi abonelikleri değerini bitirdikten sonra kodlar.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class ShaderKoprusu {
    private static final float[] NOTR_ISIK = {1f, 1f, 1f, 1f};

    private ShaderKoprusu() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void sisRengi(ViewportEvent.ComputeFogColor olay) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int evre = 0;
        boolean ayVar = true;
        boolean ayEtkin = false;
        float[] isik = NOTR_ISIK;
        int afetler = 0;
        if (level.dimension() == Level.OVERWORLD) {
            IstemciGokyuzu.Gorunum g = IstemciGokyuzu.gorunum((float) olay.getPartialTick());
            evre = g.evre().ordinal();
            if (g.evre() == GunesEvresi.NORMAL && g.onceki() != null && g.gecis() < 1f) {
                evre = ShaderKodu.EVRE_DONUS;
            }
            ayVar = GokyuzuCizici.ayGorunur(g);
            ayEtkin = IstemciAy.gokyuzuEtkin();
            isik = g.isik();
            afetler = bayrak(AfetTuru.ASIT_YAGMURU, ShaderKodu.ASIT) | bayrak(AfetTuru.TSUNAMI, ShaderKodu.TSUNAMI)
                    | bayrak(AfetTuru.SEL, ShaderKodu.SEL) | bayrak(AfetTuru.KASIRGA, ShaderKodu.KASIRGA)
                    | bayrak(AfetTuru.ORMAN_YANGINI, ShaderKodu.ORMAN_YANGINI);
        }
        float[] c = ShaderKodu.kodla(olay.getRed(), olay.getGreen(), olay.getBlue(), evre, ayVar, ayEtkin, isik, afetler);
        olay.setRed(c[0]);
        olay.setGreen(c[1]);
        olay.setBlue(c[2]);
    }

    /** Yalnız görünen aşamalar: ön uyarı ve sirende gökün değişmesi erken olur. */
    private static int bayrak(AfetTuru tur, int bit) {
        return IstemciAfetDurumu.aktif(tur)
                .filter(p -> p.asama() == AfetAsamasi.AKTIF || p.asama() == AfetAsamasi.BITIS)
                .isPresent() ? bit : 0;
    }
}
