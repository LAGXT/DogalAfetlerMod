package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import java.util.random.RandomGenerator;

/**
 * Gizli ön-uyarı penceresi (madde 4): afet seçildikten sonra siren çalmadan önceki süre, [enAz, enCok] aralığından düzgün
 * dağılımla. Hassas Sismograf'ın erken bildirimi bu sürenin önüne eklenir (yönetici).
 */
public final class OnUyariPenceresi {
    private OnUyariPenceresi() {
    }

    public static long sure(long enAz, long enCok, RandomGenerator rastgele) {
        long alt = Math.max(0, Math.min(enAz, enCok));
        long ust = Math.max(0, Math.max(enAz, enCok));
        return alt + rastgele.nextLong(ust - alt + 1);
    }
}
