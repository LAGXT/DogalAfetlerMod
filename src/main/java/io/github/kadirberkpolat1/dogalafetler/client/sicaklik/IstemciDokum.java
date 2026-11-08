package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDokumPaketi;
import java.util.function.Consumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Termometre menüsünün son dökümü ({@code sicaklik/dokum}; {@code DistExecutor.safeCallWhenOn}, K-C14). Menü açıkken
 * saniyede bir tazelenir; {@link #ESKIME} tick'ten eski döküm gösterilmez (menü o sürede durum paketine döner).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciDokum {
    /** İstek aralığının (20 tick) üç katı: iki yanıt kaybolsa da döküm ekranda kalır. */
    static final int ESKIME = 60;

    private static SicaklikDokumPaketi son;
    private static long alindi;

    private IstemciDokum() {
    }

    public static Consumer<SicaklikDokumPaketi> alici() {
        return IstemciDokum::al;
    }

    private static void al(SicaklikDokumPaketi paket) {
        son = paket;
        alindi = IstemciSicaklik.tick();
    }

    /** Güncel döküm; hiç gelmediyse ya da eskidiyse {@code null}. */
    public static SicaklikDokumPaketi guncel() {
        return son != null && IstemciSicaklik.tick() - alindi <= ESKIME ? son : null;
    }

    /** Menü yeniden açılınca önceki oturumun dökümü bir an bile görünmesin. */
    static void temizle() {
        son = null;
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        temizle();
    }
}
