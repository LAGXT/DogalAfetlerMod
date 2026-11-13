package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Pulsar ışın konilerinin strobu (pulsar-2, PL-28, PL-29). Referans videoda ses zarfı 10,87 Hz vuruş, görüntünün parlaklığı
 * 4,46 Hz şişip iner. Her vuruşta koni kısa ve keskin parlar ({@value #YUKSELIS_SN} sn yükseliş, ~{@value #SONUM_SN} sn'de sönüm),
 * tepe yüksekliğini {@value #STROB_HZ} Hz yavaş şişme belirler. Vuruş saniyesi ses döngüsünden gelir
 * ({@code pulsar_vurus} 8,0 sn'de tam 87 vuruş, dikişsiz): koni ses ile aynı fazda parlar.
 *
 * <p>Fotosensitivite: 10,87 Hz 3–30 Hz ışık duyarlılığı bandındadır. Titreşim ayarı 1'de koni hiç tam kapanmaz (en düşük
 * {@value #EN_DUSUK}, tepe 1); ayar ölçekler, 0'da sabit 1 (strob yok). Düşük kademede yalnız çift vuruşlar parlar
 * (vuruş yarıya iner, faz sesle aynı kalır). Minecraft sınıfı kullanmaz.
 */
public final class PulsarStrob {
    /** Ses döngüsünün boyu ve içindeki vuruş sayısı: döngü dikşsiz olsun diye vuruş frekansı bunlardan türer. */
    public static final float DONGU_SN = 8f;
    public static final int DONGU_VURUS = 87;
    /** 87 / 8 = 10,875 Hz (ölçülen 10,87). */
    public static final float VURUS_HZ = DONGU_VURUS / DONGU_SN;
    /** Döngüde tam 36 şişme (4,5 Hz; ölçülen 4,46): vuruş saniyesi 8 sn'de sarılınca şişme sıçramasın. */
    public static final float STROB_HZ = 36 / DONGU_SN;
    /** Ayar 1'de koni parlaklığının alt sınırı (PL-29). */
    public static final float EN_DUSUK = 0.45f;
    public static final float YUKSELIS_SN = 0.004f;
    /** Tepenin ~%5'e indiği süre (üstel sönüm, zaman sabiti bunun üçte biri). */
    public static final float SONUM_SN = 0.03f;
    /** Yavaş şişmenin en düşük çarpanı: çukurdaki vuruş da okunur kalır. */
    static final float SISME_TABAN = 0.6f;

    private PulsarStrob() {
    }

    /** Son (Düşük'te son çift) vuruştan beri geçen süreye göre keskin tepe 0–1. */
    public static float tepe(double vurusSn, boolean dusukKademe) {
        double f = dusukKademe ? VURUS_HZ * 0.5 : VURUS_HZ;
        double x = vurusSn * f;
        double gecen = (x - Math.floor(x)) / f;
        if (gecen < YUKSELIS_SN) {
            return (float) (gecen / YUKSELIS_SN);
        }
        return (float) Math.exp(-(gecen - YUKSELIS_SN) * 3.0 / SONUM_SN);
    }

    /** {@value #STROB_HZ} Hz yavaş şişme: {@value #SISME_TABAN}–1. */
    public static float sisme(double vurusSn) {
        double c = 0.5 + 0.5 * Math.cos(2.0 * Math.PI * STROB_HZ * vurusSn);
        return (float) (SISME_TABAN + (1.0 - SISME_TABAN) * c);
    }

    /**
     * Koni parlaklığı 0–1. {@code vurusSn} ses döngüsündeki konum (sn); NaN ise faz bilinmediğinden sabit 1.
     * {@code titresimAyari} 0–1'e kenetlenir.
     */
    public static float koniParlakligi(double vurusSn, float titresimAyari, boolean dusukKademe) {
        float ayar = Float.isNaN(titresimAyari) ? 0f : Math.max(0f, Math.min(1f, titresimAyari));
        if (ayar <= 0f || Double.isNaN(vurusSn)) {
            return 1f;
        }
        float ham = EN_DUSUK + (1f - EN_DUSUK) * tepe(vurusSn, dusukKademe) * sisme(vurusSn);
        return 1f - ayar * (1f - ham);
    }
}
