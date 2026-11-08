package io.github.kadirberkpolat1.dogalafetler.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Bir config bölümünün sürümlü varsayılan göç tablosu (MT2-31, K-E61): var olan dünyanın SERVER config'inde eski bir
 * varsayılanda kalan değer yeni varsayılana çekilir; hiçbir eski varsayılana eşit olmayan (elle değiştirilmiş) değere
 * dokunulmaz. Dosyanın sürümü {@link #surumYolu()}'ndadır, dosyada yoksa 0 sayılır. Sonraki bir varsayılan değişikliği yeni
 * sürümlü {@link Adim} olarak eklenir ve {@link #guncel()} artar; eski sürümden gelen dosya adımları sırayla geçer
 * (24 → 90 → 180). Bölümler yalnız tabloyu verir; plan nesnesi {@link AyarKaydi#sunucuGocu}'ya verilir, dosya işi {@code AyarGocu}'da. Minecraft sınıfı
 * kullanmaz.
 */
public final class SurumluGocPlani {
    private static final double EPS = 1e-9;

    /** {@code surum}'de {@code yol}'un varsayılanı {@code eskiler}'den birinden {@code yeni}'ye geçti. */
    public record Adim(int surum, String yol, Number yeni, List<Number> eskiler) {
        public Adim {
            Objects.requireNonNull(yol, "yol");
            Objects.requireNonNull(yeni, "yeni");
            eskiler = List.copyOf(eskiler);
        }
    }

    /**
     * {@code surum}'de liste değerli {@code yol}'un varsayılanı {@code eskiler}'den birinden {@code yeni}'ye geçti. Liste
     * yalnız eski bir varsayılana öğe öğe, sırasıyla birebir eşitse değişir (elle eklenmiş tek satır listeyi korur).
     */
    public record ListeAdimi(int surum, String yol, List<String> yeni, List<List<String>> eskiler) {
        public ListeAdimi {
            Objects.requireNonNull(yol, "yol");
            yeni = List.copyOf(yeni);
            List<List<String>> kopya = new ArrayList<>();
            for (List<String> eski : eskiler) {
                kopya.add(List.copyOf(eski));
            }
            eskiler = List.copyOf(kopya);
        }
    }

    private final String surumYolu;
    private final int guncel;
    private final List<Adim> adimlar;
    private final List<ListeAdimi> listeAdimlari;

    /**
     * @throws IllegalArgumentException tablo sürüme göre sıralı değilse, bir adımın sürümü 1–{@code guncel} dışındaysa, aynı
     *         sürümde bir yol iki kez geçiyorsa, eski değer listesi boşsa ya da yeni değeri içeriyorsa veya adım sürüm
     *         anahtarının kendisini taşıyorsa
     */
    public SurumluGocPlani(String surumYolu, int guncel, List<Adim> adimlar) {
        this(surumYolu, guncel, adimlar, List.of());
    }

    /** Sayı adımlarına ek olarak liste adımlarıyla; liste adımları sayılarla aynı kurallarla denetlenir. */
    public SurumluGocPlani(String surumYolu, int guncel, List<Adim> adimlar, List<ListeAdimi> listeAdimlari) {
        this.surumYolu = Objects.requireNonNull(surumYolu, "surumYolu");
        this.guncel = guncel;
        this.adimlar = List.copyOf(adimlar);
        this.listeAdimlari = List.copyOf(listeAdimlari);
        int listeOnceki = 1;
        for (int i = 0; i < this.listeAdimlari.size(); i++) {
            ListeAdimi a = this.listeAdimlari.get(i);
            if (a.surum() < listeOnceki || a.surum() > guncel) {
                throw new IllegalArgumentException("Liste göç adımı sırasız ya da sürüm dışı: " + a.yol());
            }
            listeOnceki = a.surum();
            if (a.yol().equals(surumYolu) || a.eskiler().isEmpty() || a.eskiler().contains(a.yeni())) {
                throw new IllegalArgumentException("Geçersiz liste göç adımı: " + a.yol());
            }
            for (int j = 0; j < i; j++) {
                ListeAdimi b = this.listeAdimlari.get(j);
                if (b.surum() == a.surum() && b.yol().equals(a.yol())) {
                    throw new IllegalArgumentException("Aynı sürümde iki liste adımı: " + a.yol());
                }
            }
        }
        int onceki = 1;
        for (int i = 0; i < this.adimlar.size(); i++) {
            Adim a = this.adimlar.get(i);
            if (a.surum() < onceki || a.surum() > guncel) {
                throw new IllegalArgumentException("Göç adımı sırasız ya da sürüm dışı: " + a);
            }
            onceki = a.surum();
            if (a.yol().equals(surumYolu) || a.eskiler().isEmpty() || eskiVarsayilan(a.yeni(), a)) {
                throw new IllegalArgumentException("Geçersiz göç adımı: " + a);
            }
            for (int j = 0; j < i; j++) {
                Adim b = this.adimlar.get(j);
                if (b.surum() == a.surum() && b.yol().equals(a.yol())) {
                    throw new IllegalArgumentException("Aynı sürümde iki adım: " + a.yol());
                }
            }
        }
    }

    public static Adim adim(int surum, String yol, Number yeni, Number... eskiler) {
        return new Adim(surum, yol, yeni, List.of(eskiler));
    }

    @SafeVarargs
    public static ListeAdimi listeAdimi(int surum, String yol, List<String> yeni, List<String>... eskiler) {
        return new ListeAdimi(surum, yol, yeni, List.of(eskiler));
    }

    public String surumYolu() {
        return surumYolu;
    }

    public int guncel() {
        return guncel;
    }

    /** Sürüme göre sıralı tablo. */
    public List<Adim> adimlar() {
        return adimlar;
    }

    /**
     * {@code surum}'den {@link #guncel()}'e geçerken değişecek anahtarlar, tablo sırasıyla: yol → yeni değer. {@code oku}
     * dosyadaki değeri verir (yoksa null: Forge eksik anahtarı zaten yeni varsayılanla doldurur). Sayı türü önemsizdir
     * (96 = 96L = 96,0); sayı olmayan değer atlanır.
     */
    public Map<String, Number> plan(int surum, Function<String, Object> oku) {
        Map<String, Number> degisen = new LinkedHashMap<>();
        for (Adim a : adimlar) {
            if (a.surum() <= surum) {
                continue;
            }
            Object deger = degisen.containsKey(a.yol()) ? degisen.get(a.yol()) : oku.apply(a.yol());
            if (deger instanceof Number n && eskiVarsayilan(n, a)) {
                degisen.put(a.yol(), a.yeni());
            }
        }
        return degisen;
    }

    /** Sürüme göre sıralı liste tablosu. */
    public List<ListeAdimi> listeAdimlari() {
        return listeAdimlari;
    }

    /**
     * {@link #plan}'ın liste karşılığı: dosyadaki liste ({@code oku}; liste değilse ya da yoksa atlanır) bir eski varsayılana
     * öğe öğe birebir eşitse yol → yeni liste. Öğeler {@code toString} ile karşılaştırılır (TOML okuyucusu dizgi verir).
     */
    public Map<String, List<String>> listePlani(int surum, Function<String, Object> oku) {
        Map<String, List<String>> degisen = new LinkedHashMap<>();
        for (ListeAdimi a : listeAdimlari) {
            if (a.surum() <= surum) {
                continue;
            }
            Object deger = degisen.containsKey(a.yol()) ? degisen.get(a.yol()) : oku.apply(a.yol());
            if (deger instanceof List<?> liste && a.eskiler().contains(dizgiler(liste))) {
                degisen.put(a.yol(), a.yeni());
            }
        }
        return degisen;
    }

    private static List<String> dizgiler(List<?> liste) {
        List<String> sonuc = new ArrayList<>(liste.size());
        for (Object o : liste) {
            sonuc.add(String.valueOf(o));
        }
        return sonuc;
    }

    private static boolean eskiVarsayilan(Number deger, Adim a) {
        for (Number eski : a.eskiler()) {
            if (Math.abs(deger.doubleValue() - eski.doubleValue()) < EPS) {
                return true;
            }
        }
        return false;
    }
}
