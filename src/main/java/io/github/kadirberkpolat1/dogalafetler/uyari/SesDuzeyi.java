package io.github.kadirberkpolat1.dogalafetler.uyari;

/**
 * Ses örneklerine verilen düzey (saf mantık). Siren {@code SoundSource.MASTER} ile çalar; Ana Ses'i ses motoru dinleyici
 * kazancı olarak uygular, burada yalnız "Sirenler" kaydırıcısı ({@code sirenSes}) çarpılır (S-01). Ortam sesleri kendi
 * vanilla kategorisinde çalar (Hava, Bloklar, Ortam); burada eğri, mesafe sönümü, ortam tavanı ve siren bastırması
 * çarpılır.
 */
public final class SesDuzeyi {
    private SesDuzeyi() {
    }

    /** {@code zarf × sirenSes / 100}, [0, 1]. */
    public static float siren(float zarf, int sirenSes) {
        return kirp(zarf * sirenSes / 100f);
    }

    /** {@code eğri × sönüm × ortamTavan / 100 × bastırma}, [0, 1]. */
    public static float ortam(float egri, float sonum, int ortamTavan, float bastirma) {
        return kirp(egri * sonum * ortamTavan / 100f * bastirma);
    }

    private static float kirp(float deger) {
        return Math.max(0f, Math.min(1f, deger));
    }
}
