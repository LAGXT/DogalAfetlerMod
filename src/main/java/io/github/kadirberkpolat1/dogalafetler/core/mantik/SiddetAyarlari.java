package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/**
 * Şiddet seçiminin ayarları (madde 10, N17). Seviye 1 baştan, 2–5 gün eşiklerinde açılır. Seviye L'nin taban ağırlığı
 * {@code k^(L−1)}'dir; {@code k} (yüksek seviye nadirlik katsayısı, &lt; 1) gün ilerledikçe {@code nadirlikBaslangic}'tan
 * {@code nadirlikSon}'a {@code nadirlikGecisGunu} günde doğrusal yükselir, böylece ağırlık yavaşça yüksek seviyelere kayar
 * ama yüksek seviye her zaman daha nadirdir. Yeni açılan seviye ağırlığının {@code acilisBaslangicOrani} kadarıyla başlar
 * ve {@code acilisRampaGunu} günde tam ağırlığa ulaşır (hemen baskın olmaz). Varsayılanlar (K-E67, kullanıcı isteği "seviye 4-5
 * gelmiyor"): S2 gün 1, S3 gün 4, S4 gün 9, S5 gün 16'da açılır; açılış payı %40'tan 3 günde tama çıkar; k 60 günde
 * 0,5'ten 0,8'e yükselir (gün 20'de S4 ≈ %9, gün 40'ta S5 ≈ %9, gün 60+'da S4+S5 ≈ %27).
 */
public record SiddetAyarlari(int seviye2Gunu, int seviye3Gunu, int seviye4Gunu, int seviye5Gunu,
        double nadirlikBaslangic, double nadirlikSon, double nadirlikGecisGunu, double acilisRampaGunu,
        double acilisBaslangicOrani, boolean gelisimDahil, double puanBasinaGun) {
    public static final SiddetAyarlari VARSAYILAN = new SiddetAyarlari(1, 4, 9, 16, 0.5, 0.8, 60, 3, 0.4, false, 2.0);

    public SiddetAyarlari {
        seviye2Gunu = Math.max(0, seviye2Gunu);
        seviye3Gunu = Math.max(seviye2Gunu, seviye3Gunu);
        seviye4Gunu = Math.max(seviye3Gunu, seviye4Gunu);
        seviye5Gunu = Math.max(seviye4Gunu, seviye5Gunu);
        nadirlikBaslangic = sinirla(nadirlikBaslangic, 0.01, 1.0);
        nadirlikSon = sinirla(nadirlikSon, 0.01, 1.0);
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
