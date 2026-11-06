package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import java.util.Locale;

/**
 * Etki büyüklüğü kademesi (madde 14: küçük %5–10, orta %15–25, büyük %30–40). Miktarı config verir
 * ({@link EtkiAyarlari#miktar}). {@code COK_KUCUK} vanilla pişmiş yiyeceklerin "çok küçük" ısınması, {@code COK_BUYUK}
 * Ateşli Güveç'in "çok büyük" ısınmasıdır.
 */
public enum Buyukluk {
    COK_KUCUK,
    KUCUK,
    ORTA,
    BUYUK,
    COK_BUYUK;

    /** Durum efektinin amplifier'ı: çok küçük ve küçük 0 (I), orta 1 (II), büyük 2 (III), çok büyük 3 (IV). */
    public int seviye() {
        return Math.max(0, ordinal() - 1);
    }

    public String anahtar() {
        return name().toLowerCase(Locale.ROOT);
    }
}
