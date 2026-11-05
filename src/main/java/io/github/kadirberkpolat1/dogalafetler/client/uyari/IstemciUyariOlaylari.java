package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenKategorisi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** İstemci olayları: ses ayarları ekranı, siren başlangıç kanıtları ve sunucudan çıkış. */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciUyariOlaylari {
    private IstemciUyariOlaylari() {
    }

    @SubscribeEvent
    public static void ekranKuruldu(ScreenEvent.Init.Post olay) {
        if (!(olay.getScreen() instanceof SoundOptionsScreen)) {
            return;
        }
        if (SirenKategorisi.gercek().isPresent()) {
            DogalAfetler.LOGGER.info("[siren] gerçek kategori var; vanilla ekran kaydırıcısını kendisi ekledi");
            return;
        }
        for (GuiEventListener oge : olay.getListenersList()) {
            if (oge instanceof OptionsList liste) {
                String yer = SirenKaydiricisi.ekle(Minecraft.getInstance().options, liste);
                DogalAfetler.LOGGER.info("[siren] Sirenler kaydırıcısı eklendi: {}, değer %{}", yer,
                        IstemciAyarlari.SIREN_SES.get());
                return;
            }
        }
        DogalAfetler.LOGGER.warn("[siren] Müzik ve Sesler ekranında seçenek listesi yok; Sirenler kaydırıcısı eklenemedi");
    }

    /** Kaydırıcı değeri bellekte değişir; dosyaya ekran kapanırken bir kez yazılır. */
    @SubscribeEvent
    public static void ekranKapandi(ScreenEvent.Closing olay) {
        if (olay.getScreen() instanceof SoundOptionsScreen && SirenKategorisi.gercek().isEmpty()) {
            IstemciAyarlari.SIREN_SES.save();
            DogalAfetler.LOGGER.info("[siren] sirenSes={} dogalafetler-client.toml'a yazıldı", IstemciAyarlari.SIREN_SES.get());
        }
    }

    @SubscribeEvent
    public static void sesIstendi(PlaySoundEvent olay) {
        if (bizim(olay.getOriginalSound())) {
            DogalAfetler.LOGGER.info("[siren] PlaySoundEvent: {}", olay.getOriginalSound().getLocation());
        }
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

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        SirenCalar.ORNEK.hemenDurdur();
    }

    private static void kaynakBasladi(String olayAdi, SoundEvent.SoundSourceEvent olay) {
        SoundInstance ses = olay.getSound();
        if (bizim(ses)) {
            DogalAfetler.LOGGER.info("[siren] {}: siren başladı ({}, kaynak={}, ilk ses={}, perde={})", olayAdi,
                    ses.getLocation(), ses.getSource().getName(), ses.getVolume(), ses.getPitch());
        }
    }

    private static boolean bizim(SoundInstance ses) {
        return ses instanceof SirenSesi;
    }
}
