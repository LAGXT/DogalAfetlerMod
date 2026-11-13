package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.OptionalDouble;

/**
 * Hareketli afetin (kasırga, tsunami) merkez izi: Gelişmiş Hava Radarı'nın "hareket yönü" (madde 13a). Sunucu her
 * saniye etki alanının güncel merkezini ekler; yön ve hız penceredeki en eski ile en yeni örnekten çıkar. Bir bloktan
 * az yer değiştiren afet durağandır (yön yok).
 */
public final class HareketIzi {
    private record Ornek(long tick, double x, double z) {
    }

    private final int pencere;
    private final Deque<Ornek> ornekler = new ArrayDeque<>();
    private long afetNo = Long.MIN_VALUE;

    public HareketIzi(int pencere) {
        this.pencere = Math.max(2, pencere);
    }

    /** Yeni afette iz sıfırlanır. {@code tick}: gerçek sunucu tick'i. */
    public void ekle(long afetNo, long tick, double x, double z) {
        if (afetNo != this.afetNo) {
            ornekler.clear();
            this.afetNo = afetNo;
        }
        ornekler.addLast(new Ornek(tick, x, z));
        while (ornekler.size() > pencere) {
            ornekler.removeFirst();
        }
    }

    public void sifirla() {
        ornekler.clear();
        afetNo = Long.MIN_VALUE;
    }

    private boolean hareketli() {
        if (ornekler.size() < 2) {
            return false;
        }
        Ornek a = ornekler.peekFirst();
        Ornek b = ornekler.peekLast();
        return b.tick() > a.tick() && Math.hypot(b.x() - a.x(), b.z() - a.z()) >= 1.0;
    }

    /** Hareket yönünün pusula açısı; durağansa boş. */
    public OptionalDouble yon() {
        if (!hareketli()) {
            return OptionalDouble.empty();
        }
        Ornek a = ornekler.peekFirst();
        Ornek b = ornekler.peekLast();
        return OptionalDouble.of(YonMesafe.pusulaAcisi(a.x(), a.z(), b.x(), b.z()));
    }

    /** Blok/sn (20 tick = 1 sn); durağansa 0. */
    public double hiz() {
        if (!hareketli()) {
            return 0;
        }
        Ornek a = ornekler.peekFirst();
        Ornek b = ornekler.peekLast();
        return Math.hypot(b.x() - a.x(), b.z() - a.z()) / ((b.tick() - a.tick()) / 20.0);
    }
}
