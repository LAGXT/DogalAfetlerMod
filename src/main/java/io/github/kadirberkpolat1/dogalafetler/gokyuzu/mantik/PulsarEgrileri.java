package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Pulsar döneminin saf eğrileri (gökyüzü-2): dünya etkilerinin ısınma gücü, ışınların dönüşü ve nabız. Pulsarın gerçek dönüşü
 * (Yengeç: 30 Hz) ekranda titreme olurdu; görünür dönüş {@value #DONUS_SN} sn'de bir turdur. Minecraft sınıfı kullanmaz.
 */
public final class PulsarEgrileri {
    /** Işınların bir tur dönüş süresi (saniye). */
    public static final float DONUS_SN = 4f;
    /** Işın ekseninin dönüş eksenine eğimi (radyan): eksen bakışa yakın geçince nabız olur. */
    public static final float ISIN_EGIMI = (float) Math.toRadians(52);

    private PulsarEgrileri() {
    }

    /** Dünya etkilerinin gücü 0–1: ısınma ilerlemesinin yumuşak basamağı. */
    public static float yogunluk(float ilerleme) {
        float t = Math.max(0f, Math.min(1f, ilerleme));
        return t * t * (3f - 2f * t);
    }

    /** Işının dönüş açısı (radyan, 0–2π): saniyeden. */
    public static float isinAcisi(double saniye) {
        double tur = saniye / DONUS_SN;
        return (float) ((tur - Math.floor(tur)) * 2 * Math.PI);
    }

    /**
     * Bir ışının bakış yönüyle hizasından (birim vektörlerin iç çarpımı, −1–1) nabız parlaklığı 0–1: yalnız ışın
     * gözlemciye dönükken keskin bir tepe (deniz feneri).
     */
    public static float nabiz(float hizalanma) {
        float h = Math.max(0f, Math.min(1f, hizalanma));
        float h2 = h * h;
        float h4 = h2 * h2;
        return h4 * h4 * h4;
    }
}
