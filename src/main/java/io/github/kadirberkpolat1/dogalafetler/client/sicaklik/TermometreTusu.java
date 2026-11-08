package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikMenuCevirileri;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Termometre menüsünün tuşu (S3-08; varsayılan K, Kontroller'de "Doğal Afetler" kategorisi). Oyunda ekran yokken basılınca
 * menüyü açar; sıcaklık kapısı kapalıysa (sistem kapalı ya da Cold Sweat'e bırakılmış) menü yerine aksiyon barında kısa bir
 * not çıkar. Menü açıkken aynı tuş onu kapatır ({@link TermometreEkrani#keyPressed}).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TermometreTusu {
    /** Mod inşası paralel iş parçacıklarında yürür; tuş, kayıt olayında (ana thread) kurulur. */
    public static final Lazy<KeyMapping> TUS = Lazy.of(() -> new KeyMapping(SicaklikMenuCevirileri.TUS,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, SicaklikMenuCevirileri.KATEGORI));

    private TermometreTusu() {
    }

    @SubscribeEvent
    public static void tuslariKaydet(RegisterKeyMappingsEvent olay) {
        olay.register(TUS.get());
    }

    /** Forge olay veriyolunda istemci tick'i: tuşun birikmiş basışlarını tüketir. */
    @Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
    public static final class Dinleyici {
        private Dinleyici() {
        }

        @SubscribeEvent
        public static void istemciTick(TickEvent.ClientTickEvent olay) {
            if (olay.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            while (TUS.get().consumeClick()) {
                if (mc.screen != null || mc.level == null || mc.player == null) {
                    continue;
                }
                if (IstemciSicaklik.acik()) {
                    TermometreEkrani.ac();
                } else {
                    mc.player.displayClientMessage(Component.translatable(SicaklikMenuCevirileri.KAPALI), true);
                }
            }
        }
    }
}
