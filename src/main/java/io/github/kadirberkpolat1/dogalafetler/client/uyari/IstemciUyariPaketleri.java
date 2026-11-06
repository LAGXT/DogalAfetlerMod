package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.OrtamDurdurPaketi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.OrtamPaketi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.SirenDurdurPaketi;
import io.github.kadirberkpolat1.dogalafetler.uyari.UyariPaketleri.SirenPaketi;
import java.util.function.Consumer;

/** Uyarı paketlerinin istemci alıcıları; paket işleyicisi bunları {@code DistExecutor.safeCallWhenOn} ile alır (K-C14). */
public final class IstemciUyariPaketleri {
    private IstemciUyariPaketleri() {
    }

    public static Consumer<SirenPaketi> sirenAlicisi() {
        return SirenCalar.ORNEK::istek;
    }

    public static Consumer<SirenDurdurPaketi> sirenDurdurAlicisi() {
        return SirenCalar.ORNEK::durdur;
    }

    public static Consumer<OrtamPaketi> ortamAlicisi() {
        return IstemciOrtam.ORNEK::paket;
    }

    public static Consumer<OrtamDurdurPaketi> ortamDurdurAlicisi() {
        return IstemciOrtam.ORNEK::durdur;
    }
}
