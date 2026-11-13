package io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik;

import io.github.kadirberkpolat1.dogalafetler.su.DenizAlani;
import io.github.kadirberkpolat1.dogalafetler.su.SelSuyu;
import java.util.List;

/**
 * Selin (taşkın) saf kuralları: seviye tablosu, sel suyunun parametreleri ve ısı kaynaklarının buharlaştırma yarıçapı.
 * Deniz ve göl kütleleri {@code artisBlok} kadar yükselir, tepede bekler, geri çekilir; yalnız suyla bağlantılı alçak
 * arazi kaplanır ({@link DenizAlani}), yüksek arazi kuru kalır. Minecraft sınıflarına dokunmaz.
 */
public final class SelKurallari {
    public static final int SANIYE_TICK = 20;
    /** Sistemin desteklediği en büyük yükselme; kalıcı deniz artışıyla toplamı da bununla sınırlıdır. */
    public static final double EN_COK_ARTIS = DenizAlani.EN_COK_ARTIS;

    private SelKurallari() {
    }

    /** Bir seviyenin değerleri; süreler tick. */
    public record Seviye(int seviye, double artisBlok, int yukselmeTick, int zirveTick, int inmeTick) {
        public int toplamTick() {
            return yukselmeTick + zirveTick + inmeTick;
        }

        /** {@code baslangic} saatinde başlayan sel suyu. */
        public SelSuyu suyu(double baslangic) {
            return new SelSuyu(baslangic, yukselmeTick, zirveTick, inmeTick, artisBlok);
        }
    }

    /** Seviye 1–5 tablosu (config listeleri); saniye cinsinden süreler. */
    public record Tablo(List<Double> artisBlok, List<Integer> yukselmeSaniye, List<Integer> zirveSaniye,
            List<Integer> inmeSaniye) {
        /**
         * K-E81: S1 5 blok / 20 sn, S5 12 blok / 40 sn; aradaki seviyeler ikisi arasında doğrusal (kullanıcı: "sel çok yavaş
         * yükseliyor ve az yükseliyor, S1 karaya bile yetişmiyor"). Tepede kalış ve çekilme değişmedi.
         */
        public static final Tablo VARSAYILAN = new Tablo(List.of(5.0, 6.75, 8.5, 10.25, 12.0), List.of(20, 25, 30, 35, 40),
                List.of(60, 90, 120, 150, 210), List.of(60, 75, 90, 105, 120));

        public Seviye seviye(int seviye) {
            int s = Math.max(1, Math.min(5, seviye));
            int i = s - 1;
            return new Seviye(s, Math.max(0.5, Math.min(EN_COK_ARTIS, artisBlok.get(i))),
                    Math.max(1, yukselmeSaniye.get(i)) * SANIYE_TICK, Math.max(0, zirveSaniye.get(i)) * SANIYE_TICK,
                    Math.max(1, inmeSaniye.get(i)) * SANIYE_TICK);
        }
    }

    /** Sel suyunda boğulma, vanilla suya göre bu kadar kat hızlıdır (K-E81: 2–3 katın ortası). */
    public static final double BOGULMA_CARPANI = 2.5;

    /**
     * Sel suyunda bu tick'te tüketilecek hava: vanilla'nın tükettiği {@code taban} (0 ya da 1; Su Altı Solunumu bazı tick'leri
     * atlatır) {@code carpan} katına çıkar. Kesirli çarpan tick'e göre dağıtılır ({@code floor(c·(t+1)) − floor(c·t)}): ortalama
     * tam {@code carpan} olur, rastgelelik gerekmez. Taban 0 ise 0 kalır (soluma korunur); çarpan ≤ 1 tabanı değiştirmez.
     */
    public static int bogulmaTuketimi(int taban, long tick, double carpan) {
        if (taban <= 0 || !(carpan > 1)) {
            return Math.max(0, taban);
        }
        double c = carpan * taban;
        return (int) (Math.floor(c * (tick + 1)) - Math.floor(c * tick));
    }

    /**
     * Bir ısı kaynağının sel suyunu buharlaştırma yarıçapı (blok): sıcaklık sisteminin kaynak satırındaki yarıçap × çarpan;
     * gücü {@code enAzGuc}'un altındaki (ya da soğutan) kaynak buharlaştırmaz, 0 döner.
     */
    public static double buharlasmaYaricapi(double guc, double yaricap, double enAzGuc, double yaricapCarpani) {
        if (!(guc > 0) || guc < enAzGuc || !(yaricap > 0) || !(yaricapCarpani > 0)) {
            return 0;
        }
        return Math.min(12.0, yaricap * yaricapCarpani);
    }
}
