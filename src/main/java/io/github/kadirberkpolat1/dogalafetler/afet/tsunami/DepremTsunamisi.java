package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.CekirdekAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Deprem → tsunami tetikleyicisi: bir deprem başlarken merkezçevresinde ({@code tsunami.aramaYaricapi}, varsayılan 96 blok) yeterli
 * açık deniz varsa, gerçek hayattaki gibi aynı merkezde ve depremle aynı seviyede bir tsunami başlatır. Başlatma komutla
 * başlatma gibidir ({@link AfetYoneticisi#konumdanBaslat}: doğal takvimi ve son seviye kuralını etkilemez); yuva kurallarına
 * uyulur: tsunami zaten sürüyorsa, sunucu geneli bir afet sürüyorsa, TPS kısıkken ya da (depremin kendisi sayılmadan)
 * eşzamanlı doğal afet sınırı doluysa başlatılmaz. Deniz yoksa sessizce vazgeçer.
 */
public final class DepremTsunamisi {
    private DepremTsunamisi() {
    }

    /** Depremin başlangıcında çağrılır; hata deprem sürecini bozmaz. */
    public static void tetikle(ServerLevel level, BlockPos merkez, int seviye) {
        try {
            dene(level, merkez, seviye);
        } catch (RuntimeException hata) {
            DogalAfetler.LOGGER.error("Deprem tsunami tetikleyicisi hata verdi (deprem etkilenmedi)", hata);
        }
    }

    private static void dene(ServerLevel level, BlockPos merkez, int seviye) {
        AfetYoneticisi y = AfetYoneticisi.al(level.getServer());
        if (y.aktifAfet(AfetTuru.TSUNAMI).isPresent() || TpsOlcer.kismaCarpani() < 1.0) {
            return;
        }
        int dogal = 0;
        for (Afet a : y.aktifAfetler()) {
            if (a.tur().sunucuGeneli()) {
                return;
            }
            if (a.tur() != AfetTuru.DEPREM && !a.baglam().elle()) {
                dogal++;
            }
        }
        if (dogal >= Math.max(1, CekirdekAyarlari.al().zamanlayici().eszamanliSinir(y.oyunGunu()))) {
            return;
        }
        TsunamiAyarlari a = TsunamiAyarlari.al();
        if (KiyiAnalizi.analiz(level, merkez, a.aramaYaricapi.get(), a.enAzDenizSutunu.get()).isEmpty()) {
            return;
        }
        AfetYoneticisi.BaslatmaSonucu sonuc = y.konumdanBaslat(AfetTuru.TSUNAMI, seviye, level, merkez);
        DogalAfetler.LOGGER.info("Deprem tsunamisi: merkez {} seviye {} -> {}", merkez.toShortString(), seviye, sonuc);
    }
}
