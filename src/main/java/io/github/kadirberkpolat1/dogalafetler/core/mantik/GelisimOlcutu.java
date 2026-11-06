package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/**
 * "Oyuncunun geliştiği" ölçütü (madde 3, bölüm D). Oyuncu puanı XP seviyesi + zırh puanıdır (tam demir zırh 15, elmas
 * 20); çevrim içi oyuncuların ortalamasına ya da en gelişmiş oyuncuya göre toplanır (config).
 */
public enum GelisimOlcutu {
    ORTALAMA,
    EN_GELISMIS;

    public static double puan(int xpSeviyesi, int zirhPuani) {
        return Math.max(0, xpSeviyesi) + Math.max(0, zirhPuani);
    }

    /** Oyuncu puanlarını toplar; oyuncu yoksa 0. */
    public double topla(double[] puanlar) {
        if (puanlar.length == 0) {
            return 0;
        }
        double toplam = 0;
        double enCok = 0;
        for (double puan : puanlar) {
            toplam += puan;
            enCok = Math.max(enCok, puan);
        }
        return this == ORTALAMA ? toplam / puanlar.length : enCok;
    }
}
