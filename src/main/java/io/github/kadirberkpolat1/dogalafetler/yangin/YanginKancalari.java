package io.github.kadirberkpolat1.dogalafetler.yangin;

import java.util.Objects;

/** Henüz birleşmemiş kardeş sistemlere bağlanma noktaları; varsayılanlar etkisizdir. */
public final class YanginKancalari {
    private static volatile IsiKaynagiBildirici isiKaynagi = IsiKaynagiBildirici.BOS;
    private static volatile SanalSuSorgusu sanalSu = SanalSuSorgusu.BOS;

    private YanginKancalari() {
    }

    public static void isiKaynagiAyarla(IsiKaynagiBildirici bildirici) {
        isiKaynagi = Objects.requireNonNull(bildirici, "bildirici");
    }

    public static void sanalSuAyarla(SanalSuSorgusu sorgu) {
        sanalSu = Objects.requireNonNull(sorgu, "sorgu");
    }

    static IsiKaynagiBildirici isiKaynagi() {
        return isiKaynagi;
    }

    static SanalSuSorgusu sanalSu() {
        return sanalSu;
    }
}
