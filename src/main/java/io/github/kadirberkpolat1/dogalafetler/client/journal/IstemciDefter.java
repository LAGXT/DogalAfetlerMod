package io.github.kadirberkpolat1.dogalafetler.client.journal;

import io.github.kadirberkpolat1.dogalafetler.journal.DefterPaketi;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * {@code defter/veri} paketinin istemci alıcısı ({@code DefterPaketi.isle}, N15 yolu): defter ekranını açar. Ekran zaten
 * açıksa (ör. komut ikinci kez verildi) yalnız verisini tazeler; sekme ve sayfa yerinde kalır. Paket yalnız defter
 * açılırken gelir, burada dinleme ya da tick işi yoktur.
 */
public final class IstemciDefter {
    private IstemciDefter() {
    }

    /** {@code DistExecutor.safeCallWhenOn} alıcısı (K-C14 deseni: {@code IstemciDokum.alici}). */
    public static Consumer<DefterPaketi> alici() {
        return IstemciDefter::ac;
    }

    /** İstemci ana thread'inde. */
    private static void ac(DefterPaketi paket) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.screen instanceof DefterEkrani ekran) {
            ekran.guncelle(paket);
            return;
        }
        mc.setScreen(new DefterEkrani(paket));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }
}
