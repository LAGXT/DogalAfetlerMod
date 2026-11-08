package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import java.util.random.RandomGenerator;

/**
 * Gizli ön-uyarı penceresi (madde 4): afet seçildikten sonra siren çalmadan önceki süre, [enAz, enCok] aralığından düzgün
 * dağılımla. Hassas Sismograf'ın erken bildirimi bu sürenin önüne eklenir (yönetici). Varsayılanlarla afetin seçilmesinden
 * görünmesine (ön-uyarı + siren) ortalama ~30 sn geçer (K-E67). Sireni standart sirenden uzun olan sunucu geneli afette
 * pencere sirenin fazlası kadar kısalır ({@code sirenFazlasi}, en az 0): seçimden AKTIF'e kadar geçen toplam bütün
 * türlerde aynı aralıkta kalır.
 */
public final class OnUyariPenceresi {
    /** Varsayılan pencere ve erken bildirim (tick; K-E67, eski 900/1800/600). */
    public static final int VARSAYILAN_EN_AZ = 200;
    public static final int VARSAYILAN_EN_COK = 360;
    public static final int VARSAYILAN_ERKEN_BILDIRIM = 100;

    private OnUyariPenceresi() {
    }

    public static long sure(long enAz, long enCok, RandomGenerator rastgele) {
        return sure(enAz, enCok, 0, rastgele);
    }

    /** Pencereden çekilen süre, {@code sirenFazlasi} kadar kısaltılmış (en az 0). */
    public static long sure(long enAz, long enCok, long sirenFazlasi, RandomGenerator rastgele) {
        long alt = Math.max(0, Math.min(enAz, enCok));
        long ust = Math.max(0, Math.max(enAz, enCok));
        return Math.max(0, alt + rastgele.nextLong(ust - alt + 1) - Math.max(0, sirenFazlasi));
    }

    /** {@link #sure(long, long, long, RandomGenerator)}'nin beklenen değeri (tick). */
    public static double ortalama(long enAz, long enCok, long sirenFazlasi) {
        long alt = Math.max(0, Math.min(enAz, enCok)) - Math.max(0, sirenFazlasi);
        long ust = Math.max(0, Math.max(enAz, enCok)) - Math.max(0, sirenFazlasi);
        if (ust <= 0) {
            return 0;
        }
        if (alt >= 0) {
            return (alt + ust) / 2.0;
        }
        long adet = ust - alt + 1;
        return ust * (ust + 1) / 2.0 / adet;
    }

    /** {@link #sure(long, long, long, RandomGenerator)}'nin alabileceği en büyük değer (tick). */
    public static long enCok(long enAz, long enCok, long sirenFazlasi) {
        return Math.max(0, Math.max(0, Math.max(enAz, enCok)) - Math.max(0, sirenFazlasi));
    }
}
