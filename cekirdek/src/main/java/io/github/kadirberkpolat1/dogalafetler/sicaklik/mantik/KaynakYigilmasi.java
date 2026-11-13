package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Yakın ısı ve soğuk kaynaklarının yığılması (K-E74). Tek kaynak her zamanki gibi katkı verir; kullanıcının yakınında
 * birden fazla kaynak varsa o yöndeki toplam katkı kaynak sayısıyla artar: {@code çarpan = 1 + artış · (n − 1)}, en çok
 * {@code enCokCarpan}. Böylece üst sınır vardır ve ortam hedefi uçlara kaçmaz (toplam ayrıca config'teki kaynak sınırlarına
 * kırpılır).
 *
 * <p>Isıtıcı ve soğutucu bir arada ise biri diğerinin yığılma payını kısmen götürür: etkin sayı
 * {@code n − karşılık · (karşı yöndeki sayı)} olur; ek olarak katkılar zaten işaretli toplanır, yani ısı ile soğuk birbirini
 * doğrusal da keser. Sayı, kaynakların <em>ağırlıklı</em> sayısıdır ({@link #agirlik}): çok zayıf (uzak) bir kaynak yok
 * sayılmaz ama tam sayılmaz da, bu yüzden oyuncu yürürken çarpan sıçramaz.
 */
public final class KaynakYigilmasi {
    /** Bir kaynak bu katkıdan (°C, mutlak) itibaren tam kaynak sayılır; altında katkısıyla orantılı. */
    public static final double TAM_KAYNAK_C = 2.0;
    /** Menüde sayılan en küçük kaynak katkısı (°C, mutlak). */
    public static final double SAYIM_ESIGI_C = 0.25;

    private KaynakYigilmasi() {
    }

    /**
     * @param kaynakBasinaArtis ikinciden itibaren her ek kaynağın aynı yöndeki toplama kattığı oran (0,25 = %25)
     * @param enCokCarpan       yığılmanın en çok çarpanı (≥ 1; 2 = toplam en çok iki katı)
     * @param karsilikOrani     karşı yönde bir kaynağın etkin sayıdan götürdüğü pay (0 = birbirini etkilemez, 1 = tam)
     */
    public record Ayarlar(double kaynakBasinaArtis, double enCokCarpan, double karsilikOrani) {
        public static final Ayarlar VARSAYILAN = new Ayarlar(0.25, 2.0, 0.5);
    }

    /**
     * Yığılmış toplamlar.
     *
     * @param isi          ısıtıcıların yığılmış toplamı (≥ 0)
     * @param soguk        soğutucuların yığılmış toplamı (≤ 0)
     * @param isiCarpani   ısıtıcıların uygulanan çarpanı (≥ 1)
     * @param soguCarpani  soğutucuların uygulanan çarpanı (≥ 1)
     */
    public record Sonuc(double isi, double soguk, double isiCarpani, double soguCarpani) {
        public double toplam() {
            return isi + soguk;
        }
    }

    /** Kaynağın sayıdaki ağırlığı (0–1): katkısı {@link #TAM_KAYNAK_C} ve üstündeyse 1, altında orantılı. */
    public static double agirlik(double katkiC) {
        double m = Math.abs(katkiC);
        return m >= TAM_KAYNAK_C ? 1.0 : m / TAM_KAYNAK_C;
    }

    /** Menüde ve sayımda bu katkı bir kaynak olarak görünür mü. */
    public static boolean sayilir(double katkiC) {
        return Math.abs(katkiC) >= SAYIM_ESIGI_C;
    }

    /** {@code etkinSayi} kaynağın çarpanı: 1 ya da daha az kaynakta 1, sonra doğrusal artar, {@code enCokCarpan}'da durur. */
    public static double carpan(double etkinSayi, Ayarlar a) {
        double tavan = Math.max(1.0, a.enCokCarpan());
        double artis = Math.max(0.0, a.kaynakBasinaArtis());
        if (!(etkinSayi > 1.0)) {
            return 1.0;
        }
        return Math.min(tavan, 1.0 + artis * (etkinSayi - 1.0));
    }

    /**
     * Isıtıcı ve soğutucu toplamlarını yığar.
     *
     * @param isi          ısıtıcı katkılarının toplamı (≥ 0)
     * @param isiSayisi    ısıtıcıların (ağırlıklı) sayısı
     * @param soguk        soğutucu katkılarının toplamı (≤ 0)
     * @param soguSayisi   soğutucuların (ağırlıklı) sayısı
     */
    public static Sonuc birlestir(double isi, double isiSayisi, double soguk, double soguSayisi, Ayarlar a) {
        double karsilik = Math.max(0.0, Math.min(1.0, a.karsilikOrani()));
        double etkinIsi = Math.max(0.0, isiSayisi - karsilik * soguSayisi);
        double etkinSogu = Math.max(0.0, soguSayisi - karsilik * isiSayisi);
        double isiCarpani = carpan(etkinIsi, a);
        double soguCarpani = carpan(etkinSogu, a);
        return new Sonuc(isi * isiCarpani, soguk * soguCarpani, isiCarpani, soguCarpani);
    }
}
