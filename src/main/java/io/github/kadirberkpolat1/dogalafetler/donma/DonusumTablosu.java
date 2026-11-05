package io.github.kadirberkpolat1.dogalafetler.donma;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * İki yönlü dönüşüm tablosu (madde 9): her dönüşüm {@code kaynak → hedef}, eşik seviyesinde uygulanır. Bir blok
 * en çok bir dönüşümün kaynağı ve en çok birinin hedefi olabilir; böylece her durumun tek bir kökü (orijinal bloğu)
 * vardır ve çözülme orijinal kayıt tutmadan tersine yürüyüp bulunur. Durum yalnız kök ve seviyenin fonksiyonudur.
 */
public final class DonusumTablosu<T> {
    public record Donusum<T>(T kaynak, T hedef, int esik) {
    }

    private final Map<T, Donusum<T>> kaynaktan = new LinkedHashMap<>();
    private final Map<T, Donusum<T>> hedeften = new LinkedHashMap<>();

    public void ekle(T kaynak, T hedef, int esik) {
        Objects.requireNonNull(kaynak, "kaynak");
        Objects.requireNonNull(hedef, "hedef");
        if (kaynak.equals(hedef)) {
            throw new IllegalArgumentException("blok kendine dönüşemez: " + kaynak);
        }
        if (kaynaktan.containsKey(kaynak)) {
            throw new IllegalArgumentException("aynı kaynak iki dönüşümde: " + kaynak);
        }
        if (hedeften.containsKey(hedef)) {
            throw new IllegalArgumentException("aynı hedef iki dönüşümde (tersi belirsiz olur): " + hedef);
        }
        for (T t = hedef; t != null; ) {
            if (t.equals(kaynak)) {
                throw new IllegalArgumentException("döngü: " + kaynak + " → " + hedef);
            }
            Donusum<T> sonraki = kaynaktan.get(t);
            t = sonraki == null ? null : sonraki.hedef();
        }
        Donusum<T> onceki = hedeften.get(kaynak);
        if (onceki != null && onceki.esik() >= esik) {
            throw new IllegalArgumentException("zincirde eşik artmalı: " + onceki + " sonra " + kaynak + " → " + hedef);
        }
        Donusum<T> sonraki = kaynaktan.get(hedef);
        if (sonraki != null && sonraki.esik() <= esik) {
            throw new IllegalArgumentException("zincirde eşik artmalı: " + kaynak + " → " + hedef + " sonra " + sonraki);
        }
        Donusum<T> donusum = new Donusum<>(kaynak, hedef, esik);
        kaynaktan.put(kaynak, donusum);
        hedeften.put(hedef, donusum);
    }

    public boolean taniyor(T blok) {
        return kaynaktan.containsKey(blok) || hedeften.containsKey(blok);
    }

    /** Zinciri geriye yürüyerek orijinal bloğu bulur; tanınmayan blok kendisidir. */
    public T kok(T blok) {
        T k = blok;
        Donusum<T> d;
        while ((d = hedeften.get(k)) != null) {
            k = d.kaynak();
        }
        return k;
    }

    /** Seviyedeki durum: kökten başlayıp eşiği aşılan dönüşümler uygulanır. */
    public T durum(T mevcut, int seviye) {
        T s = kok(mevcut);
        Donusum<T> d;
        while ((d = kaynaktan.get(s)) != null && d.esik() <= seviye) {
            s = d.hedef();
        }
        return s;
    }

    /** Kökten {@code blok}'a kadar zincirdeki adım sayısı (kök 0). */
    public int adim(T blok) {
        int n = 0;
        T k = blok;
        Donusum<T> d;
        while ((d = hedeften.get(k)) != null) {
            k = d.kaynak();
            n++;
        }
        return n;
    }

    /**
     * Yönlü durum: donarken ({@code yon > 0}) zincirde yalnız ileri, çözülürken ({@code yon < 0}) yalnız geri gidilir;
     * {@code yon == 0} dokunmaz. Böylece donma başlarken doğal hedef bloklar (ör. soğuk biyomun buzu) eritilmez.
     */
    public T durum(T mevcut, int seviye, int yon) {
        if (yon == 0) {
            return mevcut;
        }
        T istenen = durum(mevcut, seviye);
        int fark = adim(istenen) - adim(mevcut);
        return (yon > 0 ? fark > 0 : fark < 0) ? istenen : mevcut;
    }

    public Collection<Donusum<T>> donusumler() {
        return Collections.unmodifiableCollection(kaynaktan.values());
    }

    public int boyut() {
        return kaynaktan.size();
    }
}
