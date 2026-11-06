package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenKarari;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenNobeti;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenProfili;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenZarfi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariAyarlari;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.SirenDurdurPaketi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.SirenPaketi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;

/**
 * İstemcide aynı anda en çok bir siren çalar (S-02). Aynı afetin tekrar isteği yok sayılır; başka afetin sireni gelirse
 * eskisi {@link SirenZarfi#KESME_TICK}'te söner, yenisi hemen başlar. Sireni sunucunun durdurma paketi bitirir (madde 5);
 * istemci süresi yalnız güvenlik tavanıdır. Vanilla boyut değişiminde ve {@code /stopsound}'da bütün sesleri keser;
 * siren bunu {@link #denetle()} ile fark edip aynı zarfla yeniden çalar (K0-27, N11). Yalnız istemci ana thread'inde.
 */
final class SirenCalar {
    static final SirenCalar ORNEK = new SirenCalar();

    private SirenSesi etkin;
    private SirenNobeti nobet = new SirenNobeti();
    private ClientLevel calinanDunya;
    private int yenidenBaslatmaSayisi;

    private SirenCalar() {
    }

    void istek(SirenPaketi paket) {
        boolean caliyor = caliyor();
        SirenKarari karar = SirenKarari.istek(caliyor, caliyor && etkin.zarf().sonduruluyor(),
                caliyor ? etkin.afetNo() : 0L, paket.afetNo());
        if (karar == SirenKarari.YOK_SAY) {
            DogalAfetler.LOGGER.info("[siren] istek yok sayıldı: afetNo {} zaten çalıyor", paket.afetNo());
            return;
        }
        if (karar == SirenKarari.KES_VE_BASLAT && etkin.sondur(SirenZarfi.KESME_TICK, "yeni siren kesti")) {
            DogalAfetler.LOGGER.info("[siren] yeni siren (afetNo {}) çalan sireni (afetNo {}) kesiyor", paket.afetNo(),
                    etkin.afetNo());
        }
        SirenProfili profil = SirenProfili.icin(paket.tur());
        etkin = new SirenSesi(new SirenZarfi(profil, paket.toplamTick(), paket.kalanTick()), paket.afetNo());
        nobet = new SirenNobeti();
        Minecraft mc = Minecraft.getInstance();
        calinanDunya = mc.level;
        DogalAfetler.LOGGER.info("[siren] istemci: siren paketi alındı: {} ({}), toplam {} kalan {} tick ({} döngü), "
                        + "afetNo {}, sirenSes={}", profil.sesAdi(), paket.afet().kimlik(), paket.toplamTick(),
                paket.kalanTick(), profil.donguSayisi(paket.kalanTick()), paket.afetNo(), UyariAyarlari.sirenSes().get());
        mc.getSoundManager().play(etkin);
    }

    void durdur(SirenDurdurPaketi paket) {
        boolean caliyor = etkin != null && !etkin.isStopped();
        if (!SirenKarari.durdurUygulanir(caliyor, caliyor ? etkin.afetNo() : 0L, paket.afetNo())) {
            DogalAfetler.LOGGER.info("[siren] durdurma paketi yok sayıldı (afetNo {}, çalan {})", paket.afetNo(),
                    caliyor ? etkin.afetNo() : "yok");
            return;
        }
        if (etkin.sondur(paket.fadeTick(), "durdurma paketi")) {
            DogalAfetler.LOGGER.info("[siren] istemci: durdurma paketi alındı, {} fade-out {} tick (geçen {} tick)",
                    etkin.zarf().profil().sesAdi(), paket.fadeTick(), etkin.zarf().gecenTick());
        }
    }

    /** Her istemci tick'inde: ses motoru sireni kendiliğinden kestiyse aynı zarfla yeniden çalar. */
    void denetle() {
        Minecraft mc = Minecraft.getInstance();
        if (etkin == null || mc.level == null) {
            return;
        }
        SoundManager sesler = mc.getSoundManager();
        boolean anaSesKapali = mc.options.getSoundSourceVolume(SoundSource.MASTER) <= 0f;
        if (!nobet.yenidenCalinmali(etkin.isStopped(), etkin.zarf().bitti(), sesler.isActive(etkin), anaSesKapali)) {
            return;
        }
        String neden = mc.level != calinanDunya ? "boyut değişimi" : "ses motoru durdurdu, ör. /stopsound";
        etkin = etkin.devam();
        calinanDunya = mc.level;
        sesler.play(etkin);
        yenidenBaslatmaSayisi++;
        DogalAfetler.LOGGER.info("[siren] siren yeniden başlatıldı ({}), zarf korundu t={} zarf={} boyut={} isActive={}",
                neden, etkin.zarf().gecenTick(), SirenSesi.bicim(etkin.zarf().duzey()), mc.level.dimension().location(),
                sesler.isActive(etkin));
    }

    /** Sunucudan çıkışta fade beklemeden durur. */
    void hemenDurdur() {
        if (etkin != null) {
            Minecraft.getInstance().getSoundManager().stop(etkin);
            DogalAfetler.LOGGER.info("[siren] sunucudan çıkış: siren hemen durdu");
            etkin = null;
        }
    }

    /** Zarfı bitmemiş bir siren var (ortam sesleri bu sürede -3 dB alçalır). */
    boolean caliyor() {
        return etkin != null && !etkin.zarf().bitti() && !etkin.isStopped();
    }

    SirenSesi etkin() {
        return etkin;
    }

    int yenidenBaslatmaSayisi() {
        return yenidenBaslatmaSayisi;
    }
}
