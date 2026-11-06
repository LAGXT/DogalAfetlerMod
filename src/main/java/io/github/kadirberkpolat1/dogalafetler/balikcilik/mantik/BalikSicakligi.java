package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

/**
 * Buz balığının sıcaklık etkisi (madde 14, 15): sayılar yiyecek sisteminin kademeleriyle aynıdır
 * ({@code yiyecek.kademe.kucuk} 8, {@code orta} 20, {@code buyuk} 35 vücut birimi; {@code yiyecek.anlikOran} 0,25). Çiğ
 * balık küçük serinlik (60 sn), pişmiş balık orta ısınma (90 sn), pişmiş Donmuş Ringa büyük ısınma (120 sn). Toplamın
 * {@link #ANLIK_ORAN}'ı yenince, kalanı süre boyunca azalan hızla eklenir (yiyecek F-01 ile aynı biçim). Yiyecek API'si
 * ana dala girince bu tablo {@code YiyecekAPI.cigBalik/pismisBalik} ile değiştirilir (karar B-19).
 */
public enum BalikSicakligi {
    CIG_KUCUK(-8, 60),
    PISMIS_ORTA(20, 90),
    RINGA_BUYUK(35, 120);

    /** Yiyecek sisteminin anlık payı ({@code yiyecek.anlikOran}). */
    public static final double ANLIK_ORAN = 0.25;

    private final double toplam;
    private final int sureSn;

    BalikSicakligi(double toplam, int sureSn) {
        this.toplam = toplam;
        this.sureSn = sureSn;
    }

    /** Etkinin tamamı (vücut birimi; negatif serinletir). */
    public double toplam() {
        return toplam;
    }

    /** Yenince hemen eklenen kısım. */
    public double anlik() {
        return toplam * ANLIK_ORAN;
    }

    /** Süre boyunca azalan hızla eklenen kısım. */
    public double sureli() {
        return toplam - anlik();
    }

    public int sureTick() {
        return sureSn * 20;
    }
}
