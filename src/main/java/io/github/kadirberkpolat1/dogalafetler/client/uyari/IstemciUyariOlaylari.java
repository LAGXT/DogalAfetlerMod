package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariAyarlari;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * İstemci olayları (N15): ses ayarları ekranına "Sirenler" kaydırıcısı (N5), siren ve ortam katmanlarının tick'i, ses
 * motorunun gerçekten çalmaya başladığının log'u ve sunucudan çıkış. Ekrana yazı çizmez (N6).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciUyariOlaylari {
    private IstemciUyariOlaylari() {
    }

    @SubscribeEvent
    public static void ekranKuruldu(ScreenEvent.Init.Post olay) {
        if (!(olay.getScreen() instanceof SoundOptionsScreen)) {
            return;
        }
        for (GuiEventListener oge : olay.getListenersList()) {
            if (oge instanceof OptionsList liste) {
                String yer = SirenKaydiricisi.ekle(Minecraft.getInstance().options, liste);
                DogalAfetler.LOGGER.info("[siren] Sirenler kaydırıcısı eklendi: {}, değer %{}", yer,
                        UyariAyarlari.sirenSes().get());
                return;
            }
        }
        DogalAfetler.LOGGER.warn("[siren] Müzik ve Sesler ekranında seçenek listesi yok; Sirenler kaydırıcısı eklenemedi");
    }

    /** Kaydırıcı değeri bellekte değişir; dosyaya ekran kapanırken bir kez yazılır. */
    @SubscribeEvent
    public static void ekranKapandi(ScreenEvent.Closing olay) {
        if (olay.getScreen() instanceof SoundOptionsScreen) {
            UyariAyarlari.sirenSes().save();
            DogalAfetler.LOGGER.info("[siren] uyari.sirenSes={} dogalafetler-client.toml'a yazıldı",
                    UyariAyarlari.sirenSes().get());
        }
    }

    @SubscribeEvent
    public static void tik(TickEvent.ClientTickEvent olay) {
        if (olay.phase == TickEvent.Phase.END) {
            SirenCalar.ORNEK.denetle();
            IstemciOrtam.ORNEK.tick();
        }
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        SirenCalar.ORNEK.hemenDurdur();
        IstemciOrtam.ORNEK.temizle();
    }

    /** Ses motoru OpenAL kaynağını gerçekten çalmaya başlattı (bellekteki ses); ses thread'inde gelir. */
    @SubscribeEvent
    public static void sesKaynagiBasladi(PlaySoundSourceEvent olay) {
        kaynakBasladi("PlaySoundSourceEvent", olay);
    }

    /** Akışlı (stream) ses için aynısı. */
    @SubscribeEvent
    public static void akisKaynagiBasladi(PlayStreamingSourceEvent olay) {
        kaynakBasladi("PlayStreamingSourceEvent", olay);
    }

    private static void kaynakBasladi(String olayAdi, SoundEvent.SoundSourceEvent olay) {
        SoundInstance ses = olay.getSound();
        if (ses instanceof SirenSesi siren) {
            DogalAfetler.LOGGER.info("[siren] {}: ses motoru çalıyor ({}, kaynak={}, devam={}, perde={})", olayAdi,
                    ses.getLocation(), ses.getSource().getName(), siren.devamOrnegi(), ses.getPitch());
        } else if (ses.getLocation().getNamespace().equals(DogalAfetler.MOD_ID)) {
            DogalAfetler.LOGGER.info("[ortam] {}: ses motoru çalıyor ({}, kaynak={})", olayAdi, ses.getLocation(),
                    ses.getSource().getName());
        }
    }
}
