package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

/**
 * Chunk yetişme kuyruğunun tick bütçesi (madde 3 TPS, madde 9 ÖNCELİK; spike K-D2). Bütçe birimlidir: sütun taraması 1, blok
 * değişimi {@code blokMaliyeti}; tick başına en çok {@code temelButce · kısma} blok. Bir sütun ancak kalan birim en kötü
 * durumdaki sütunu (rezerv) karşılıyorsa işlenir; böylece tick'in blok sayısı sınırı hiç aşmaz. İkinci güvence süre
 * tavanıdır; bir tick tavanı aşarsa sonraki tick'in tavanı aşım kadar düşer (en az dörtte biri kalır).
 */
public record YetismeButcesi(int temelButce, int blokMaliyeti, long tavanNs) {
    public YetismeButcesi {
        if (temelButce < 1 || blokMaliyeti < 1 || tavanNs < 1) {
            throw new IllegalArgumentException("bütçe, maliyet ve tavan pozitif olmalı");
        }
    }

    /** Bu tick'te değişebilecek en çok blok. */
    public int blokSiniri(double kisma) {
        return Math.max(1, (int) Math.floor(temelButce * Math.max(0.0, Math.min(1.0, kisma))));
    }

    /** Bu tick'in birim bütçesi. */
    public int birim(double kisma) {
        return blokSiniri(kisma) * blokMaliyeti;
    }

    /** {@code enCokDegisim} blok değiştirebilen sütunun en kötü maliyeti. */
    public int rezerv(int enCokDegisim) {
        return sutunMaliyeti(enCokDegisim);
    }

    public int sutunMaliyeti(int degisen) {
        return 1 + degisen * blokMaliyeti;
    }

    /** Kalan birim en kötü sütunu karşılıyor mu. */
    public boolean sutunSigar(int kalanBirim, int rezerv) {
        return kalanBirim >= rezerv;
    }

    /** Önceki tick'in süre aşımı düşülmüş süre tavanı. */
    public long sureSiniriNs(long oncekiAsimNs) {
        return Math.max(tavanNs / 4, tavanNs - Math.max(0L, oncekiAsimNs));
    }
}
