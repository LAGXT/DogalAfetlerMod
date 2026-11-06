package io.github.kadirberkpolat1.dogalafetler.uyari;

import java.util.List;

/**
 * Ortam katmanının zamana göre hacmi (saf mantık): katman başladığından beri geçen gerçek tick'te noktalar arası doğrusal
 * ara değer; ilk noktadan önce ilk, son noktadan sonra son değer. Hacim 0–1 arasıdır (ortam tavanı ve mesafe sönümü
 * istemcide üstüne çarpılır). Katmanın kapanışı eğride değil, {@code OrtamSesi.durdur}'un fade süresindedir.
 *
 * <pre>{@code
 * HacimEgrisi.giris(60, 0.8f)                     // 3 sn'de 0 → 0,8
 * HacimEgrisi.of(new Nokta(0, 0f), new Nokta(40, 1f), new Nokta(400, 0.4f))  // yüksel, yavaşça alçal
 * }</pre>
 */
public record HacimEgrisi(List<Nokta> noktalar) {
    /** Ağa giden pakette en çok bu kadar nokta. */
    public static final int EN_COK_NOKTA = 16;

    /** {@code tick} katman başlangıcından beri geçen gerçek tick, {@code hacim} 0–1. */
    public record Nokta(int tick, float hacim) {
    }

    public HacimEgrisi {
        noktalar = List.copyOf(noktalar);
        if (noktalar.isEmpty() || noktalar.size() > EN_COK_NOKTA) {
            throw new IllegalArgumentException("Hacim eğrisi 1.." + EN_COK_NOKTA + " nokta olmalı: " + noktalar.size());
        }
        int onceki = -1;
        for (Nokta nokta : noktalar) {
            if (nokta.tick() <= onceki) {
                throw new IllegalArgumentException("Hacim eğrisinin tick'leri 0'dan başlayıp artmalı: " + noktalar);
            }
            if (!(nokta.hacim() >= 0f && nokta.hacim() <= 1f)) {
                throw new IllegalArgumentException("Hacim 0-1 arasında olmalı: " + nokta);
            }
            onceki = nokta.tick();
        }
    }

    public static HacimEgrisi of(Nokta... noktalar) {
        return new HacimEgrisi(List.of(noktalar));
    }

    public static HacimEgrisi sabit(float hacim) {
        return of(new Nokta(0, hacim));
    }

    /** {@code girisTick}'te 0'dan {@code hacim}'e çıkan, sonra sabit eğri. */
    public static HacimEgrisi giris(int girisTick, float hacim) {
        return of(new Nokta(0, 0f), new Nokta(Math.max(1, girisTick), hacim));
    }

    public float deger(long tick) {
        Nokta ilk = noktalar.get(0);
        if (tick <= ilk.tick()) {
            return ilk.hacim();
        }
        for (int i = 1; i < noktalar.size(); i++) {
            Nokta son = noktalar.get(i);
            if (tick <= son.tick()) {
                Nokta bas = noktalar.get(i - 1);
                float oran = (float) (tick - bas.tick()) / (son.tick() - bas.tick());
                return bas.hacim() + (son.hacim() - bas.hacim()) * oran;
            }
        }
        return noktalar.get(noktalar.size() - 1).hacim();
    }
}
