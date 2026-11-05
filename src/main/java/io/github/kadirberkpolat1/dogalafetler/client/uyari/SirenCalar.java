package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenIstemcisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenKategorisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenNobeti;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenSesleri;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenTuru;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;

/**
 * İstemcide aynı anda en çok bir siren çalar: yeni siren gelirse eskisi {@code KESME_TICK} içinde söner, yenisi
 * hemen başlar (kısa çapraz geçiş). Vanilla boyut değişiminde ve {@code /stopsound}'da bütün sesleri keser; siren bunu
 * {@link #denetle()} ile fark edip aynı zarfla yeniden çalar (N11: Nether/End'deki oyuncu da duyar). Yalnız istemcinin
 * ana thread'inden çağrılır (paket işleyicileri ana thread'e alınır).
 */
final class SirenCalar implements SirenIstemcisi {
    static final SirenCalar ORNEK = new SirenCalar();

    private SirenSesi etkin;
    private SirenNobeti nobet;
    private ClientLevel calinanDunya;
    private int yenidenBaslatmaSayisi;

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
        nobet = new SirenNobeti();
        calinanDunya = mc.level;
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

    /** Her istemci tick'inde: ses motoru sireni kendiliğinden kestiyse aynı zarfla yeniden çalar. */
    void denetle() {
        Minecraft mc = Minecraft.getInstance();
        if (etkin == null || mc.level == null) {
            return;
        }
        SoundManager sesler = mc.getSoundManager();
        if (!nobet.yenidenCalinmali(etkin.isStopped(), etkin.zarfBitti(), sesler.isActive(etkin))) {
            return;
        }
        String neden = mc.level != calinanDunya ? "boyut değişimi" : "ses motoru durdurdu, ör. /stopsound";
        etkin = etkin.devam();
        calinanDunya = mc.level;
        sesler.play(etkin);
        yenidenBaslatmaSayisi++;
        DogalAfetler.LOGGER.info("[siren] siren yeniden başlatıldı ({}), zarf korundu t={} ses={} boyut={} isActive={}",
                neden, etkin.gecenTick(), String.format(Locale.ROOT, "%.4f", etkin.getVolume()),
                mc.level.dimension().location(), sesler.isActive(etkin));
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

    int yenidenBaslatmaSayisi() {
        return yenidenBaslatmaSayisi;
    }
}
