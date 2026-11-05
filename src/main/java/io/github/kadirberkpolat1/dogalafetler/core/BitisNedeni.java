package io.github.kadirberkpolat1.dogalafetler.core;

/** Bir afetin neden bittiği ({@code AfetBittiEvent}). Başarımlar yalnız {@link #DOGAL} bitişi saymalıdır. */
public enum BitisNedeni {
    /** Afet kendi süresini tamamladı. */
    DOGAL,
    /** {@code /dogalafet stop} ya da {@code sifirla}. */
    KOMUT,
    /** Sunucu geneli afetin gamerule'ü süreç sürerken kapatıldı (madde 11). */
    KURAL,
    /** Ön-uyarıdaki (henüz duyurulmamış) afet gamerule kapatıldığı için iptal edildi. */
    IPTAL,
    /** Afet kodu hata verdi ya da boyutu yok; afet güvenle kaldırıldı. */
    HATA
}
