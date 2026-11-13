package io.github.kadirberkpolat1.dogalafetler.uyari;

/**
 * Siren çalarken ortam seslerinin alçalması (saf mantık, ses.md §4 kural 4): siren açıkça duyulsun, toplam düzey
 * artmasın. Desibel cinsinden doğrusal geçiş, {@value #GECIS_TICK} tick'te 0 dB ↔ {@value #SIREN_BASTIRMA_DB} dB.
 */
public final class Bastirma {
    public static final float SIREN_BASTIRMA_DB = -3f;
    public static final int GECIS_TICK = 20;

    private float desibel;

    /** Bir tick ilerler ve yeni çarpanı döndürür. */
    public float ilerle(boolean sirenCaliyor) {
        float hedef = sirenCaliyor ? SIREN_BASTIRMA_DB : 0f;
        float adim = -SIREN_BASTIRMA_DB / GECIS_TICK;
        if (desibel > hedef) {
            desibel = Math.max(hedef, desibel - adim);
        } else if (desibel < hedef) {
            desibel = Math.min(hedef, desibel + adim);
        }
        return carpan();
    }

    public float carpan() {
        return (float) Math.pow(10.0, desibel / 20.0);
    }
}
