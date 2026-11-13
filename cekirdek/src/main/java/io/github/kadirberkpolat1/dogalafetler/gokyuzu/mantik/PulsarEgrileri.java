package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Pulsar döneminin ısınma eğrisi (gökyüzü-2): dünya etkilerinin ve görünümün gücü. Titreşim {@link PulsarTitresimi}'ndedir.
 * Minecraft sınıfı kullanmaz.
 */
public final class PulsarEgrileri {
    private PulsarEgrileri() {
    }

    /** Dünya etkilerinin gücü 0–1: ısınma ilerlemesinin yumuşak basamağı. */
    public static float yogunluk(float ilerleme) {
        float t = Math.max(0f, Math.min(1f, ilerleme));
        return t * t * (3f - 2f * t);
    }
}
