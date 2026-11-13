package io.github.kadirberkpolat1.dogalafetler.core;

import java.util.Locale;
import java.util.Optional;

/**
 * Afet yaşam döngüsünün aşamaları (GOREV §4): gizli ön-uyarı penceresi (madde 4), siren (madde 5), afetin asıl etkisi
 * ve temizlenip söndüğü bitiş. Uzun afetlerin alt aşamaları {@link Afet#altAsama()} ile tutulur. Saf Java'dır.
 */
public enum AfetAsamasi {
    /** Afet kararlaştırıldı; oyunculara bildirim yok, yalnız Hava Radarı ve Sismograf görür. */
    ON_UYARI("on_uyari"),
    /** Siren çalıyor (etkilenebilecek oyunculara). */
    SIREN("siren"),
    /** Afetin asıl etkisi; siren fade-out ile durdu. */
    AKTIF("aktif"),
    /** Afet sönüyor; geçici efektler kapanıyor. */
    BITIS("bitis");

    private final String kimlik;

    AfetAsamasi(String kimlik) {
        this.kimlik = kimlik;
    }

    public String kimlik() {
        return kimlik;
    }

    /** Dil dosyasındaki ad anahtarı: {@code asama.dogalafetler.<kimlik>}. */
    public String ceviriAnahtari() {
        return "asama.dogalafetler." + kimlik;
    }

    /** Sıradaki aşama; {@link #BITIS}'ten sonra yoktur. */
    public Optional<AfetAsamasi> sonraki() {
        return ordinal() + 1 < values().length ? Optional.of(values()[ordinal() + 1]) : Optional.empty();
    }

    public static Optional<AfetAsamasi> kimlikIle(String girdi) {
        if (girdi == null) {
            return Optional.empty();
        }
        String aranan = girdi.trim().toLowerCase(Locale.ROOT);
        for (AfetAsamasi asama : values()) {
            if (asama.kimlik.equals(aranan)) {
                return Optional.of(asama);
            }
        }
        return Optional.empty();
    }
}
