package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import java.util.Locale;
import java.util.Optional;

/**
 * Bir afetten sağ çıkanın o afette ayrıca sağladığı koşullar (madde 12 #2, #4, #5, #6). Advancement JSON'undaki
 * {@code afetten_sag_cikti} ölçütünün {@code kosul} alanı bu kimliklerden biridir.
 */
public enum SagCikmaKosulu {
    /** Tsunami dalgası tarafından sürüklendi (#2 "Dalga Avcısı"). */
    SURUKLENDI("suruklendi"),
    /** Sinkhole çökerken çukura düştü (#4 "Delikten Dönüş"). */
    CUKURA_DUSTU("cukura_dustu"),
    /** Orman yangını alanının içinde en az {@value AfetIzi#YANGIN_SANIYESI} saniye kaldı (#5 "Yanmadan Çıktım"). */
    YANGIN_ALANINDA("yangin_alaninda"),
    /** Etki alanındayken hiç hasar almadı (#6 "Çatı Altı"). */
    HASARSIZ("hasarsiz");

    private final String kimlik;

    SagCikmaKosulu(String kimlik) {
        this.kimlik = kimlik;
    }

    public String kimlik() {
        return kimlik;
    }

    public static Optional<SagCikmaKosulu> kimlikIle(String girdi) {
        if (girdi == null) {
            return Optional.empty();
        }
        String aranan = girdi.trim().toLowerCase(Locale.ROOT);
        for (SagCikmaKosulu k : values()) {
            if (k.kimlik.equals(aranan)) {
                return Optional.of(k);
            }
        }
        return Optional.empty();
    }
}
