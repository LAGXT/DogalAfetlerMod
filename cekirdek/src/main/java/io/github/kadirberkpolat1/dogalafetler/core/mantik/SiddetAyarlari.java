package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/**
 * Şiddet seçiminin ayarları (madde 10, N17). Seviye 1 baştan, 2–5 gün eşiklerinde açılır. Seviye L'nin taban ağırlığı {@code k^(L−1)}'dir; {@code k} (yüksek seviye
 * nadirlik katsayısı) gün ilerledikçe {@code nadirlikBaslangic}'tan {@code nadirlikSon}'a {@code nadirlikGecisGunu}
 * günde {@link SiddetSecici#ADIM_GUNU} günlük basamaklarla yükselir; ağırlık yüksek seviyelere kayar ve {@code k} 1'i
 * aşınca (oyun ilerleyince) yüksek seviyeler baskın olur. Yeni açılan seviye ağırlığının {@code acilisBaslangicOrani}
 * kadarıyla başlar ve {@code acilisRampaGunu} günde tam ağırlığa ulaşır. Varsayılanlar (ayar sürümü 4, kullanıcı isteği
 * "5. seviyeler daha sık gelmeli"): S2 gün 1, S3 gün 2, S4 gün 4, S5 gün 7'de açılır (oyun günü 20 dk sürer); k 100 günde
 * 0,9'dan 1,8'e çıkar. Art arda 4-5 seyreltmesi ({@link SiddetSecici#ART_ARDA_CARPANI}) ve yoğunluk yokken tablo olasılığı
 * (art arda seyreltmesi dahil gerçekleşen pay, Markov ortalaması): gün 10'da S5 ≈ %17, gün 30'da ≈ %23, gün 60'ta ≈ %31, gün
 * 100'de ≈ %41 (S4 ≈ %17, %20, %22, %23).
 */
public record SiddetAyarlari(int seviye2Gunu, int seviye3Gunu, int seviye4Gunu, int seviye5Gunu,
        double nadirlikBaslangic, double nadirlikSon, double nadirlikGecisGunu, double acilisRampaGunu,
        double acilisBaslangicOrani, boolean gelisimDahil, double puanBasinaGun) {
    public static final SiddetAyarlari VARSAYILAN = new SiddetAyarlari(1, 2, 4, 7, 0.9, 1.8, 100, 3, 0.4, false, 2.0);

    public SiddetAyarlari {
        seviye2Gunu = Math.max(0, seviye2Gunu);
        seviye3Gunu = Math.max(seviye2Gunu, seviye3Gunu);
        seviye4Gunu = Math.max(seviye3Gunu, seviye4Gunu);
        seviye5Gunu = Math.max(seviye4Gunu, seviye5Gunu);
        nadirlikBaslangic = sinirla(nadirlikBaslangic, 0.01, 2.0);
        nadirlikSon = sinirla(nadirlikSon, 0.01, 2.0);
        nadirlikGecisGunu = sinirla(nadirlikGecisGunu, 0, 1e6);
        acilisRampaGunu = sinirla(acilisRampaGunu, 0, 1e6);
        acilisBaslangicOrani = sinirla(acilisBaslangicOrani, 0, 1);
        puanBasinaGun = sinirla(puanBasinaGun, 0, 1e6);
    }

    private static double sinirla(double deger, double enAz, double enCok) {
        if (Double.isNaN(deger)) {
            return enAz;
        }
        return Math.max(enAz, Math.min(enCok, deger));
    }

    /** Seviyenin açıldığı oyun günü (seviye 1 için 0). */
    public int esik(int seviye) {
        return switch (seviye) {
            case 1 -> 0;
            case 2 -> seviye2Gunu;
            case 3 -> seviye3Gunu;
            case 4 -> seviye4Gunu;
            case 5 -> seviye5Gunu;
            default -> throw new IllegalArgumentException("Seviye 1-5 olmalı: " + seviye);
        };
    }

    public SiddetAyarlari gelisimIle(boolean dahil, double gunPerPuan) {
        return new SiddetAyarlari(seviye2Gunu, seviye3Gunu, seviye4Gunu, seviye5Gunu, nadirlikBaslangic, nadirlikSon,
                nadirlikGecisGunu, acilisRampaGunu, acilisBaslangicOrani, dahil, gunPerPuan);
    }
}
