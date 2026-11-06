package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import java.util.Locale;

/** Yiyeceğin vücuda etkisinin yönü: ısınma ("İçten Isınma" efekti) ya da serinleme ("Serinlik" efekti). */
public enum Yon {
    ISINMA(1),
    SERINLIK(-1);

    private final int isaret;

    Yon(int isaret) {
        this.isaret = isaret;
    }

    /** Vücut birimine eklenen değerin işareti (+1 ısınır, −1 serinler). */
    public int isaret() {
        return isaret;
    }

    public String anahtar() {
        return name().toLowerCase(Locale.ROOT);
    }
}
