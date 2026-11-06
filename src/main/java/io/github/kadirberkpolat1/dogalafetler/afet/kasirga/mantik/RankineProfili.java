package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/**
 * Rankine girdabı: çekirdek ({@code r ≤ r0}) katı cisim gibi döner ({@code ω = ω0}), dışı potansiyel girdaptır
 * ({@code ω = ω0·(r0/r)²}). Teğet hız çekirdek sınırında en büyüktür ve süreklidir.
 */
public record RankineProfili(double cekirdekAcisalHiz, double cekirdekYaricapi) {
    public RankineProfili {
        if (cekirdekYaricapi <= 0 || cekirdekAcisalHiz < 0) {
            throw new IllegalArgumentException("Rankine parametresi geçersiz: ω0=" + cekirdekAcisalHiz + ", r0="
                    + cekirdekYaricapi);
        }
    }

    /** Açısal hız (rad/sn). */
    public double acisalHiz(double r) {
        if (r <= cekirdekYaricapi) {
            return cekirdekAcisalHiz;
        }
        double oran = cekirdekYaricapi / r;
        return cekirdekAcisalHiz * oran * oran;
    }

    /** Teğet hız (blok/sn). */
    public double tegetHiz(double r) {
        return acisalHiz(r) * Math.max(0, r);
    }
}
