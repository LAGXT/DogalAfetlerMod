package io.github.kadirberkpolat1.dogalafetler.yangin;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Objects;

/**
 * Ateşin sahibi: afet türü ve {@code afetNo} (başarımlar, istatistik ve {@link YanginYoneticisi#kaynakBitti}). Türü
 * {@code null} olan kaynak komut ya da test tutuşturmasıdır.
 */
public record YanginKaynagi(AfetTuru tur, long afetNo) {
    public static final YanginKaynagi KOMUT = new YanginKaynagi(null, 0L);

    public static YanginKaynagi afet(AfetTuru tur, long afetNo) {
        return new YanginKaynagi(Objects.requireNonNull(tur, "tur"), afetNo);
    }

    String turKimligi() {
        return tur == null ? "" : tur.kimlik();
    }

    static YanginKaynagi oku(String kimlik, long afetNo) {
        AfetTuru tur = AfetTuru.kimlikIle(kimlik).orElse(null);
        return tur == null && afetNo == 0L ? KOMUT : new YanginKaynagi(tur, afetNo);
    }
}
