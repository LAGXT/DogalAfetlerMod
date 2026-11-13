package io.github.kadirberkpolat1.dogalafetler.su;

/**
 * Sel (taşkın) suyunun parametreleri: deniz ve göl kütlelerinin yüzeyi {@code baslangic}'tan itibaren {@code yukselme} süresince
 * {@code hedef} blok yükselir, {@code zirve} süresince orada kalır, {@code inme} süresince geri çekilir. Hepsi ölçekli afet
 * saati tick'idir. Su aniden yükselmeye başlar (ilk saniyelerde hemen görünür, sinüs biçiminde yavaşlayarak tepeye çıkar:
 * seçimden görünmeye 30 saniye kuralı, K-E67) ve çekilirken yumuşak (smoothstep) iner; sunucu ve istemci aynı parametrelerden aynı artışı hesaplar
 * (hücre değil parametre gider, SU-01). Artış deniz kütlesinin kendi sınırsız taşkınına ve göl kütlelerinin taşma ağzına tabidir
 * ({@link DenizAlani}); yani sel yalnız suyla bağlantılı alçak araziyi kaplar.
 *
 * @param baslangic yükselmenin başladığı saat
 * @param yukselme  tepeye çıkış süresi (tick)
 * @param zirve     tepede kalış süresi (tick)
 * @param inme      geri çekilme süresi (tick)
 * @param hedef     tepedeki yükselme (blok)
 */
public record SelSuyu(double baslangic, double yukselme, double zirve, double inme, double hedef) {
    /** Sel suyunun "göründü" sayıldığı yükselme (blok): kıyıda su çizgisinin belirgin kalktığı ilk nokta. */
    public static final double GORUNUR_ARTIS = 0.1;

    public SelSuyu {
        yukselme = Math.max(0, yukselme);
        zirve = Math.max(0, zirve);
        inme = Math.max(0, inme);
        hedef = Math.max(0, hedef);
    }

    /** Seli oluşturan afetin toplam süresi (tick). */
    public double toplamSure() {
        return yukselme + zirve + inme;
    }

    /** Selin tamamen çekildiği saat. */
    public double bitis() {
        return baslangic + toplamSure();
    }

    public boolean bitti(double t) {
        return t >= bitis();
    }

    /** {@code t} anında deniz yüzeyinin üstündeki sel yükselmesi (blok); başlamadan önce ve bittikten sonra 0. */
    public double artis(double t) {
        double gecen = t - baslangic;
        if (gecen <= 0 || gecen >= toplamSure()) {
            return 0;
        }
        if (gecen < yukselme) {
            return hedef * Math.sin(Math.PI / 2 * (gecen / yukselme));
        }
        gecen -= yukselme;
        if (gecen <= zirve) {
            return hedef;
        }
        gecen -= zirve;
        return hedef * (1 - yumusat(gecen / inme));
    }

    /** Başlangıçtan sonra yükselmenin {@link #GORUNUR_ARTIS} bloğa ulaştığı süre (tick); hiç ulaşmazsa sonsuz. */
    public double gorunmeGecikmesi() {
        if (!(hedef >= GORUNUR_ARTIS)) {
            return Double.POSITIVE_INFINITY;
        }
        return yukselme * 2 / Math.PI * Math.asin(GORUNUR_ARTIS / hedef);
    }

    private static double yumusat(double u) {
        double k = Math.max(0, Math.min(1, u));
        return k * k * (3 - 2 * k);
    }
}
