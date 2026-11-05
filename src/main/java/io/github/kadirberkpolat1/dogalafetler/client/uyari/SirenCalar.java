package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenIstemcisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenKategorisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenSesleri;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenTuru;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

/**
 * İstemcide aynı anda en çok bir siren çalar: yeni siren gelirse eskisi {@code KESME_TICK} içinde söner, yenisi
 * hemen başlar (kısa çapraz geçiş). Yalnız istemcinin ana thread'inden çağrılır (paket işleyicileri ana thread'e alınır).
 */
final class SirenCalar implements SirenIstemcisi {
    static final SirenCalar ORNEK = new SirenCalar();

    private SirenSesi etkin;

    private SirenCalar() {
    }

    @Override
    public void baslat(SirenTuru tur, int sureTick) {
        Minecraft mc = Minecraft.getInstance();
        if (etkin != null && !etkin.isStopped()) {
            DogalAfetler.LOGGER.info("[siren] yeni siren ({}) çalan sireni ({}) kesiyor", tur.kimlik(), etkin.tur().kimlik());
            etkin.kes();
        }
        SoundSource kaynak = SirenKategorisi.gercek().orElse(SoundSource.MASTER);
        etkin = new SirenSesi(tur, sureTick, SirenSesleri.olay(tur), kaynak);
        DogalAfetler.LOGGER.info("[siren] istemci: SirenPaketi alındı, {} {} tick, kaynak={}, sirenSes={}, kategori kaydırıcısı={}",
                tur.kimlik(), sureTick, kaynak.getName(), IstemciAyarlari.SIREN_SES.get(),
                mc.options.getSoundSourceVolume(kaynak));
        mc.getSoundManager().play(etkin);
    }

    @Override
    public void durdur() {
        if (etkin != null && !etkin.isStopped()) {
            DogalAfetler.LOGGER.info("[siren] istemci: SirenDurdurPaketi alındı, {} fade-out ({} tick)", etkin.tur().kimlik(),
                    etkin.tur().sondurmeTick());
            etkin.sondur("durdurma paketi");
        }
    }

    /** Sunucudan çıkışta fade beklemeden durur. */
    void hemenDurdur() {
        if (etkin != null) {
            Minecraft.getInstance().getSoundManager().stop(etkin);
            etkin = null;
        }
    }

    SirenSesi etkin() {
        return etkin;
    }
}
