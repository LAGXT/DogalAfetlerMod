package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Meteor ve yağmur config anahtarlarının göç zinciri (MT2-31): var olan dünyanın SERVER config'inde eski bir varsayılanda
 * kalan değer yeni varsayılana çekilir; hiçbir eski varsayılana eşit olmayan (elle değiştirilmiş) değere dokunulmaz.
 * Dosyanın sürümü {@value #SURUM_YOLU}'ndadır, dosyada yoksa 0 sayılır. Sonraki bir varsayılan değişikliği yeni sürümlü
 * {@link Adim} olarak eklenir ve {@link #GUNCEL} artar; eski sürümden gelen dosya adımları sırayla geçer (24 → 90 → 180).
 */
public final class MeteorAyarGocu {
    public static final String SURUM_YOLU = "meteor.ayarSurumu";
    public static final int GUNCEL = 1;
    private static final double EPS = 1e-9;

    /** {@code surum}'de {@code yol}'un varsayılanı {@code eskiler}'den birinden {@code yeni}'ye geçti. */
    public record Adim(int surum, String yol, Number yeni, List<Number> eskiler) {
    }

    /** Sürüme göre sıralı. Ara değerler (96, 240, 24, 90, 30) yalnız {@code is/meteor-2} yapılarında varsayılandı. */
    public static final List<Adim> ADIMLAR = List.of(
            adim(1, "meteor.varlikTavani", 448, 48, 96, 240),
            adim(1, "meteor.girisYuksekligi", 200, 140),
            adim(1, "meteor.yagmurBuyukOrani", 0.10, 0.05),
            adim(1, "gunes_patlamasi.buyukMeteorOrani", 0.10, 0.05),
            adim(1, "gunes_patlamasi.meteorYagmuruDakikaBasina", 180.0, 24.0, 90.0),
            adim(1, "gunes_patlamasi.meteorYagmuruSaniye", 600, 180),
            adim(1, "ay_carpmasi.meteorDakikaBasina", 180.0, 7.5, 30.0, 90.0),
            adim(1, "ay_carpmasi.meteorSuresiSn", 600, 90),
            adim(1, "ay_carpmasi.meteorBuyukOrani", 0.10, 0.04));

    private MeteorAyarGocu() {
    }

    /**
     * {@code surum}'den {@link #GUNCEL}'e geçerken değişecek anahtarlar, tablo sırasıyla: yol → yeni değer. {@code oku} dosyadaki
     * değeri verir (yoksa null: Forge eksik anahtarı zaten yeni varsayılanla doldurur). Sayı türü önemsizdir (96 = 96L = 96,0).
     */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        Map<String, Number> degisen = new LinkedHashMap<>();
        for (Adim a : ADIMLAR) {
            if (a.surum() <= surum || a.surum() > GUNCEL) {
                continue;
            }
            Object deger = degisen.containsKey(a.yol()) ? degisen.get(a.yol()) : oku.apply(a.yol());
            if (deger instanceof Number n && eskiVarsayilan(n, a)) {
                degisen.put(a.yol(), a.yeni());
            }
        }
        return degisen;
    }

    private static boolean eskiVarsayilan(Number deger, Adim a) {
        for (Number eski : a.eskiler()) {
            if (Math.abs(deger.doubleValue() - eski.doubleValue()) < EPS) {
                return true;
            }
        }
        return false;
    }

    private static Adim adim(int surum, String yol, Number yeni, Number... eskiler) {
        return new Adim(surum, yol, yeni, List.of(eskiler));
    }
}
