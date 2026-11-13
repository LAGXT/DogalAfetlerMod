package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/** Geçici olasılık çarpanının normale dönüş biçimi (madde 4). */
public enum SonumTipi {
    /** Sönüm süresi boyunca tepe değerden 1'e düz çizgiyle iner. */
    DOGRUSAL,
    /** Başta hızlı, sonra yavaş iner; sürenin sonunda tam 1 olur. */
    USTEL
}
