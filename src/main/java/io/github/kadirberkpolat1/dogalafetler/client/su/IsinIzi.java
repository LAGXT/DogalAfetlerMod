package io.github.kadirberkpolat1.dogalafetler.client.su;

/**
 * Izgarada ışın yürüyüşü (Amanatides–Woo): başlangıç noktasının hücresinden çıkıp ışının kestiği birim küpleri sırayla,
 * köşe atlamadan verir; her adım tek eksende bir hücre ilerler ve ışın {@code erim} bloğu aşınca durur. Mağara suyunun
 * tavan yansıması bununla izlenir (SU-42): sabit yarım blok adım ince tavan yamalarını ve köşeleri atlıyordu, 12 blokluk
 * erim de uzak yüzeyin yansımasını tavana ulaştırmıyordu. Tahsissiz ve yeniden kullanılır; saf mantık, tek thread.
 */
final class IsinIzi {
    private int x;
    private int y;
    private int z;
    private int adimX;
    private int adimY;
    private int adimZ;
    private double tMaxX;
    private double tMaxY;
    private double tMaxZ;
    private double tDeltaX;
    private double tDeltaY;
    private double tDeltaZ;
    private double erim;
    private double t;

    /** Yürüyüşü {@code (px, py, pz)} hücresinde başlatır; yön sıfır olmamalı (birim olması gerekmez). */
    void baslat(double px, double py, double pz, double dx, double dy, double dz, double erim) {
        double boy = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= boy;
        dy /= boy;
        dz /= boy;
        x = (int) Math.floor(px);
        y = (int) Math.floor(py);
        z = (int) Math.floor(pz);
        adimX = (int) Math.signum(dx);
        adimY = (int) Math.signum(dy);
        adimZ = (int) Math.signum(dz);
        tDeltaX = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dx);
        tDeltaY = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dy);
        tDeltaZ = dz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dz);
        tMaxX = ilkSinir(px, x, dx);
        tMaxY = ilkSinir(py, y, dy);
        tMaxZ = ilkSinir(pz, z, dz);
        this.erim = erim;
        t = 0;
    }

    private static double ilkSinir(double p, int hucre, double d) {
        if (d > 0) {
            return (hucre + 1 - p) / d;
        }
        return d < 0 ? (p - hucre) / -d : Double.POSITIVE_INFINITY;
    }

    /** Işının girdiği sıradaki hücreye geçer; giriş noktası erimin ötesindeyse {@code false}. */
    boolean ilerle() {
        if (tMaxX < tMaxY && tMaxX < tMaxZ) {
            t = tMaxX;
            x += adimX;
            tMaxX += tDeltaX;
        } else if (tMaxY < tMaxZ) {
            t = tMaxY;
            y += adimY;
            tMaxY += tDeltaY;
        } else {
            t = tMaxZ;
            z += adimZ;
            tMaxZ += tDeltaZ;
        }
        return t <= erim;
    }

    int x() {
        return x;
    }

    int y() {
        return y;
    }

    int z() {
        return z;
    }

    /** Işının bu hücreye girdiği uzaklık (blok). */
    double t() {
        return t;
    }
}
