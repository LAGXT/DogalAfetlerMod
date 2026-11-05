package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;

/**
 * Efekt kalitesine göre görsel bütçe ve mesafe LOD'u (tasarım notu §5). Yakındaki oyuncu (&lt; 96 blok) seçili
 * kalitenin tamamını görür; 96 ötesinde taban sprite'ları yarıya iner, 160 ötesinde görsel enkaz durur, 256 ötesinde
 * yalnız huni ve duvar bulutu kalır.
 */
public record KasirgaKaliteButcesi(int huniDilim, int huniHalka, int tabanSprite, int gorselEnkazBlokBasina,
        int duvarSprite, int altGirdapEnFazla) {

    public static final double LOD_TABAN_YARI = 96;
    public static final double LOD_ENKAZ_YOK = 160;
    public static final double LOD_YALNIZ_HUNI = 256;

    public static KasirgaKaliteButcesi icin(EfektKalitesi kalite) {
        return switch (kalite) {
            case DUSUK -> new KasirgaKaliteButcesi(12, 16, 150, 1, 40, 0);
            case ORTA -> new KasirgaKaliteButcesi(16, 24, 350, 2, 80, 2);
            case YUKSEK -> new KasirgaKaliteButcesi(24, 32, 700, 4, 120, 3);
            case ULTRA -> new KasirgaKaliteButcesi(32, 48, 1400, 6, 200, 4);
        };
    }

    /** Seviye 1'de bütçenin %20'si (seyrek halka), seviye 5'te tamamı; mesafeye göre LOD. */
    public int tabanSpriteSayisi(int seviye, double mesafe) {
        if (mesafe > LOD_YALNIZ_HUNI) {
            return 0;
        }
        int sayi = tabanSprite * seviye / 5;
        return mesafe > LOD_TABAN_YARI ? sayi / 2 : sayi;
    }

    /** Her gerçek uçan blok için istemcinin ürettiği yalnız görsel enkaz kopyası sayısı. */
    public int gorselEnkazSayisi(double mesafe) {
        return mesafe > LOD_ENKAZ_YOK ? 0 : gorselEnkazBlokBasina;
    }

    /** Alt girdaplar yalnız seviye 4 (2 tane) ve 5'te (4 tane) vardır; kalite üst sınırı uygulanır. */
    public int altGirdapSayisi(int seviye) {
        int istenen = seviye >= 5 ? 4 : seviye == 4 ? 2 : 0;
        return Math.min(istenen, altGirdapEnFazla);
    }
}
