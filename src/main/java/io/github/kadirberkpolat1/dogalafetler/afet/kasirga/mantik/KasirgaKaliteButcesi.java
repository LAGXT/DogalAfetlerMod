package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;

/**
 * Efekt kalitesine göre görsel bütçe (tasarım notu §5, spike SK-06) ve mesafe LOD'u. Yakındaki oyuncu (LOD eşiğinin
 * içinde) seçili kalitenin tamamını görür; ilk eşikten sonra taban sprite'ları yarıya iner, ikincide görsel enkaz durur,
 * üçüncüde yalnız huni ve duvar bulutu kalır.
 */
public record KasirgaKaliteButcesi(int huniDilim, int huniHalka, int tabanSprite, int gorselEnkazBlokBasina,
        int duvarSprite, int altGirdapEnFazla) {

    /** LOD eşikleri (blok; CLIENT {@code kasirga.lod*}). */
    public record Lod(double tabanYari, double enkazYok, double yalnizHuni) {
        public static final Lod VARSAYILAN = new Lod(96, 160, 256);

        public Lod {
            if (!(tabanYari > 0 && enkazYok >= tabanYari && yalnizHuni >= enkazYok)) {
                throw new IllegalArgumentException("LOD eşikleri artan olmalı: " + tabanYari + ", " + enkazYok + ", " + yalnizHuni);
            }
        }
    }

    public static KasirgaKaliteButcesi icin(EfektKalitesi kalite) {
        return switch (kalite) {
            case DUSUK -> new KasirgaKaliteButcesi(12, 16, 150, 1, 40, 0);
            case ORTA -> new KasirgaKaliteButcesi(16, 24, 350, 2, 80, 2);
            case YUKSEK -> new KasirgaKaliteButcesi(24, 32, 700, 4, 120, 4);
            case ULTRA -> new KasirgaKaliteButcesi(32, 48, 1400, 6, 200, 4);
        };
    }

    /** Seviye 1'de bütçenin %20'si (seyrek toz girdabı), seviye 5'te tamamı; mesafeye göre LOD. */
    public int tabanSpriteSayisi(int seviye, double mesafe, Lod lod) {
        if (mesafe > lod.yalnizHuni()) {
            return 0;
        }
        int sayi = tabanSprite * Math.max(1, Math.min(5, seviye)) / 5;
        return mesafe > lod.tabanYari() ? sayi / 2 : sayi;
    }

    /** Her gerçek uçan blok için istemcinin ürettiği yalnız görsel enkaz kopyası sayısı. */
    public int gorselEnkazSayisi(double mesafe, Lod lod) {
        return mesafe > lod.enkazYok() ? 0 : gorselEnkazBlokBasina;
    }

    /** Duvar bulutu sprite sayısı; yalnız-huni eşiğinden sonra da çizilir (ufku kaplayan bulut). */
    public int duvarSpriteSayisi(double mesafe, Lod lod) {
        return mesafe > lod.yalnizHuni() ? duvarSprite / 2 : duvarSprite;
    }

    /** Alt girdaplar yalnız seviye 4 (2-3 tane) ve 5'te (3-4 tane); kalite üst sınırı uygulanır. */
    public int altGirdapSayisi(int seviye, double mesafe, Lod lod) {
        if (mesafe > lod.yalnizHuni()) {
            return 0;
        }
        int istenen = seviye >= 5 ? 4 : seviye == 4 ? 3 : 0;
        return Math.min(istenen, altGirdapEnFazla);
    }
}
