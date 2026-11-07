package io.github.kadirberkpolat1.dogalafetler.afet.ortak.mantik;

/** Bloğun afet yıkımındaki rolü ({@code AfetBloklari.rol}). */
public enum BlokRolu {
    /** {@code #dogalafetler:afet_dekor}: asılı/dayalı süs ve küçük yapı bloğu; sertliğe bakılmaz. */
    DEKOR,
    /** Envanterli blok varlığı (fıçı, sandık, fırın): kırılırsa içerik dökülür. */
    KAP,
    /** Afetin kendi sınıf tablosu karar verir. */
    DIGER
}
