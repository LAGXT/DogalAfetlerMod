package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Gökyüzü config anahtarlarının göç zinciri (K-E61, {@code MeteorAyarGocu} kalıbı): var olan dünyanın SERVER config'inde
 * eski bir varsayılanda kalan değer yeni varsayılana çekilir; hiçbir eski varsayılana eşit olmayan (elle değiştirilmiş)
 * değere dokunulmaz. Dosyanın sürümü {@value #SURUM_YOLU}'ndadır, dosyada yoksa 0 sayılır. Sonraki bir varsayılan
 * değişikliği yeni sürümlü {@link Adim} olarak eklenir ve {@link #GUNCEL} artar.
 */
public final class GokyuzuAyarGocu {
    public static final String SURUM_YOLU = "gokyuzu.ayarSurumu";
    public static final int GUNCEL = 1;
    private static final double EPS = 1e-9;

    /** {@code surum}'de {@code yol}'un varsayılanı {@code eskiler}'den birinden {@code yeni}'ye geçti. */
    public record Adim(int surum, String yol, Number yeni, List<Number> eskiler) {
    }

    /** Sürüme göre sıralı. 1: pulsar-2 (ısınma 1 gün, yangın dakikada 24). */
    public static final List<Adim> ADIMLAR = List.of(
            adim(1, "gokyuzu.pulsarIsinmaGun", 1.0, 2.0),
            adim(1, "gokyuzu.pulsarYanginDakikaBasina", 24.0, 20.0));

    private GokyuzuAyarGocu() {
    }

    /**
     * {@code surum}'den {@link #GUNCEL}'e geçerken değişecek anahtarlar, tablo sırasıyla: yol → yeni değer. {@code oku} dosyadaki
     * değeri verir (yoksa null: Forge eksik anahtarı zaten yeni varsayılanla doldurur). Sayı türü önemsizdir (20 = 20L = 20,0).
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
