package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Madde 12 "sağ çıkmak": afetin etki alanındayken afet bitene kadar ölmemek. Çekirdek {@code AfetBittiEvent} ile etkilenen ve
 * sağ çıkanları verir (çekirdek §A5); başarımlar yalnız doğal bitişi sayar. {@code basarim.etkiAlaniPayi} sıfırdan
 * büyükse etki alanının o kadar dışında kalan oyuncuları başarım kendisi izler ({@link AfetIzi}); onlar da ölmemiş ve
 * bitince çevrim içiyse sağ çıkandır.
 */
public final class SagCikanTuretimi {
    private SagCikanTuretimi() {
    }

    /**
     * @param dogalBitti   {@code AfetBittiEvent.dogalBitti()}; değilse (stop, kural, iptal, hata) kimse sağ çıkmaz
     * @param etkilenenler olaydaki etkilenenler
     * @param sagCikanlar  olaydaki sağ çıkanlar (etkilenenlerle kesiştirilir)
     * @param payliAlanda  etki alanı payı içinde AKTIF aşamada bulunmuş oyuncular
     * @param payliOlenler bunlardan afet sürerken ölenler
     * @param cevrimici    bitişte çevrim içi oyuncular
     */
    public static Set<UUID> sagCikanlar(boolean dogalBitti, Set<UUID> etkilenenler, Set<UUID> sagCikanlar,
            Set<UUID> payliAlanda, Set<UUID> payliOlenler, Set<UUID> cevrimici) {
        Set<UUID> sonuc = new LinkedHashSet<>();
        if (!dogalBitti) {
            return sonuc;
        }
        for (UUID kimlik : sagCikanlar) {
            if (etkilenenler.contains(kimlik)) {
                sonuc.add(kimlik);
            }
        }
        for (UUID kimlik : payliAlanda) {
            if (!payliOlenler.contains(kimlik) && cevrimici.contains(kimlik)) {
                sonuc.add(kimlik);
            }
        }
        return sonuc;
    }

    /** {@code minSeviye} ≤ 0 ise her afet; aksi hâlde seviye en az {@code minSeviye} (seviyesiz afet 0'dır). */
    public static boolean seviyeYeterli(int seviye, int minSeviye) {
        return minSeviye <= 0 || seviye >= minSeviye;
    }

    /** N16 "Birlikte Hayatta": aynı afetten en az {@code esik} oyuncu sağ çıktı. */
    public static boolean birlikteHayatta(int sagCikanSayisi, int esik) {
        return sagCikanSayisi >= esik;
    }
}
