package io.github.kadirberkpolat1.dogalafetler.config;

/**
 * Efekt kalitesi (madde 1, N13). Varsayılan {@link #YUKSEK} şartnamedeki "Kalite" modudur; {@link #DUSUK}
 * "Düşük Efekt" modudur; {@link #ULTRA} güçlü makineler için isteğe bağlı üst seviyedir.
 */
public enum EfektKalitesi {
    DUSUK,
    ORTA,
    YUKSEK,
    ULTRA
}
