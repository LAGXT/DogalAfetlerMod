package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import java.util.random.RandomGenerator;

/**
 * Gizli ön-uyarı penceresi (madde 4): afet seçildikten sonra siren çalmadan önceki süre, [enAz, enCok] aralığından düzgün
 * dağılımla. Hassas Sismograf'ın erken bildirimi bu sürenin önüne eklenir (yönetici). Varsayılanlarla afetin seçilmesinden
 * görünmesine (ön-uyarı + siren) ortalama ~30 sn geçer (K-E67). Sireni standart sirenden uzun olan sunucu geneli afette
 * pencere sirenin fazlası kadar kısalır ({@code sirenFazlasi}): seçimden AKTIF'e kadar geçen toplam bütün türlerde aynı
 * aralıkta kalır. Kısaltılmış pencere en az {@value #EN_AZ_KISA_TICK} tick'tir: erken bildirimden sonra normal ön-uyarı
 * bölümü (Hava Radarı ve Sismograf, madde 13) her türde vardır.
 */
public final class OnUyariPenceresi {
    /** Varsayılan pencere ve erken bildirim (tick; K-E67, eski 900/1800/600). */
    public static final int VARSAYILAN_EN_AZ = 200;
    public static final int VARSAYILAN_EN_COK = 360;
    public static final int VARSAYILAN_ERKEN_BILDIRIM = 100;
    /** Siren fazlasıyla kısaltılmış pencerenin alt sınırı (1 sn). */
    public static final long EN_AZ_KISA_TICK = 20;

    private OnUyariPenceresi() {
    }

    public static long sure(long enAz, long enCok, RandomGenerator rastgele) {
        return sure(enAz, enCok, 0, rastgele);
    }

    /** Pencereden çekilen süre; {@code sirenFazlasi} &gt; 0 ise o kadar kısaltılmış, en az {@value #EN_AZ_KISA_TICK}. */
    public static long sure(long enAz, long enCok, long sirenFazlasi, RandomGenerator rastgele) {
        long alt = Math.max(0, Math.min(enAz, enCok));
        long ust = Math.max(0, Math.max(enAz, enCok));
        long secilen = alt + rastgele.nextLong(ust - alt + 1);
        return sirenFazlasi > 0 ? Math.max(EN_AZ_KISA_TICK, secilen - sirenFazlasi) : secilen;
    }

    /** {@link #sure(long, long, long, RandomGenerator)}'nin beklenen değeri (tick). */
    public static double ortalama(long enAz, long enCok, long sirenFazlasi) {
        long alt = Math.max(0, Math.min(enAz, enCok));
        long ust = Math.max(0, Math.max(enAz, enCok));
        if (sirenFazlasi <= 0) {
            return (alt + ust) / 2.0;
        }
        long lo = alt - sirenFazlasi;
        long hi = ust - sirenFazlasi;
        long m = EN_AZ_KISA_TICK;
        if (hi <= m) {
            return m;
        }
        if (lo >= m) {
            return (lo + hi) / 2.0;
        }
        double toplam = m * (double) (m - lo + 1) + (hi * (hi + 1) - m * (m + 1)) / 2.0;
        return toplam / (hi - lo + 1);
    }

    /** {@link #sure(long, long, long, RandomGenerator)}'nin alabileceği en büyük değer (tick). */
    public static long enCok(long enAz, long enCok, long sirenFazlasi) {
        long ust = Math.max(0, Math.max(enAz, enCok));
        return sirenFazlasi > 0 ? Math.max(EN_AZ_KISA_TICK, ust - sirenFazlasi) : ust;
    }
}
