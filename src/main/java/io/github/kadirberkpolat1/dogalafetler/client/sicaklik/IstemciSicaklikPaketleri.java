package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import java.util.function.Consumer;

/** Sıcaklık paketinin istemci alıcısı ({@code DistExecutor.safeCallWhenOn}, K-C14). */
public final class IstemciSicaklikPaketleri {
    private IstemciSicaklikPaketleri() {
    }

    public static Consumer<SicaklikDurumPaketi> durumAlicisi() {
        return IstemciSicaklik::guncelle;
    }
}
