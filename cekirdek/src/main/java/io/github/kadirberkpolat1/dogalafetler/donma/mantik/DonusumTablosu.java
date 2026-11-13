package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * İki yönlü dönüşüm tablosu (madde 9): her dönüşüm {@code kaynak → hedef}; donarken {@code esik}'te uygulanır, çözülürken
 * {@code cozulmeEsik}'in altında geri alınır ({@code cozulmeEsik ≥ esik}: histerezis). Bir blok en çok bir dönüşümün kaynağı
 * ve en çok birinin hedefi olabilir; böylece her durumun tek bir kökü (orijinal bloğu) vardır ve çözülme orijinal kayıt
 * tutmadan tersine yürüyüp bulunur.
 */
public final class DonusumTablosu<T> {
    public record Donusum<T>(T kaynak, T hedef, int esik, int cozulmeEsik) {
    }

    private final Map<T, Donusum<T>> kaynaktan = new LinkedHashMap<>();
    private final Map<T, Donusum<T>> hedeften = new LinkedHashMap<>();

    public void ekle(T kaynak, T hedef, int esik) {
        ekle(kaynak, hedef, esik, esik);
    }

    public void ekle(T kaynak, T hedef, int esik, int cozulmeEsik) {
        Objects.requireNonNull(kaynak, "kaynak");
        Objects.requireNonNull(hedef, "hedef");
        if (kaynak.equals(hedef)) {
            throw new IllegalArgumentException("blok kendine dönüşemez: " + kaynak);
        }
        if (cozulmeEsik < esik) {
            throw new IllegalArgumentException("çözülme eşiği (" + cozulmeEsik + ") donma eşiğinden (" + esik + ") küçük");
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
        if (onceki != null && (onceki.esik() >= esik || onceki.cozulmeEsik() > cozulmeEsik)) {
            throw new IllegalArgumentException("zincirde eşikler artmalı: " + onceki + " sonra " + kaynak + " → " + hedef);
        }
        Donusum<T> sonraki = kaynaktan.get(hedef);
        if (sonraki != null && (sonraki.esik() <= esik || sonraki.cozulmeEsik() < cozulmeEsik)) {
            throw new IllegalArgumentException("zincirde eşikler artmalı: " + kaynak + " → " + hedef + " sonra " + sonraki);
        }
        Donusum<T> donusum = new Donusum<>(kaynak, hedef, esik, cozulmeEsik);
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

    /** Donma yönünde seviyedeki durum: kökten başlayıp donma eşiği aşılan dönüşümler uygulanır. */
    public T durum(T mevcut, int seviye) {
        return yuru(kok(mevcut), seviye, false);
    }

    private T yuru(T s, int seviye, boolean cozulme) {
        Donusum<T> d;
        while ((d = kaynaktan.get(s)) != null && (cozulme ? d.cozulmeEsik() : d.esik()) <= seviye) {
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
     * Yönlü durum: donarken ({@code yon > 0}) donma eşikleriyle yalnız ileri, çözülürken ({@code yon < 0}) çözülme
     * eşikleriyle yalnız geri gidilir; {@code yon == 0} dokunmaz.
     */
    public T durum(T mevcut, int seviye, int yon) {
        if (yon == 0) {
            return mevcut;
        }
        T istenen = yuru(kok(mevcut), seviye, yon < 0);
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
