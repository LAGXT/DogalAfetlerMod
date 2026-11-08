package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Kamerayı gecikmeyle izleyen dünya çapası (pulsar-2 tur 4, G2-3 kalıbı): nesne kökten {@code uzaklik} blok ötede,
 * yönü güneş yolundan gelir (günlük hareket korunur). Kök kameraya {@code 1 − exp(−dt/tau)} ile yetişir: oyuncu
 * uçarken paralaks görülür, uzun yolculukta nesne kaybolmaz. Işınlanma ya da boyut değişiminde (kök–kamera farkı
 * 2×uzaklıktan büyük) kök kameraya sıçrar. Konum bloklarda hesaplanır, kamera merkezli tek ölçekle
 * {@value #SIGDIRMA} birimlik gök küresine sığdırılır (açı ve paralaks doğru, arazi önüne geçer). Yalnız render
 * thread'i.
 */
public final class GokCapasi {
    public static final GokCapasi PULSAR = new GokCapasi(800f, 60f);
    public static final GokCapasi KARADELIK = new GokCapasi(800f, 120f);
    /** Nesne merkezinin kameraya uzaklığı (gök birimi); çevresindeki geometri ~95 birimi aşmasın. */
    static final float SIGDIRMA = 90f;

    private final float uzaklik;
    private final float tau;
    private boolean kurulu;
    private double kokX;
    private double kokY;
    private double kokZ;
    private long sonNanos;

    private GokCapasi(float uzaklik, float tau) {
        this.uzaklik = uzaklik;
        this.tau = tau;
    }

    /** Karede bir kez; ilk çağrıda kök = kamera. */
    public void guncelle(Vec3 kamera, long nanos) {
        if (!kurulu) {
            kokX = kamera.x;
            kokY = kamera.y;
            kokZ = kamera.z;
            sonNanos = nanos;
            kurulu = true;
            return;
        }
        double dt = Math.max(0L, nanos - sonNanos) * 1e-9;
        sonNanos = nanos;
        double k = 1.0 - Math.exp(-dt / tau);
        kokX += (kamera.x - kokX) * k;
        kokY += (kamera.y - kokY) * k;
        kokZ += (kamera.z - kokZ) * k;
        double dx = kamera.x - kokX;
        double dy = kamera.y - kokY;
        double dz = kamera.z - kokZ;
        if (dx * dx + dy * dy + dz * dz > 4.0 * uzaklik * uzaklik) {
            kokX = kamera.x;
            kokY = kamera.y;
            kokZ = kamera.z;
        }
    }

    /** Nesnenin kameradan konumu (bloklar). */
    private Vector3f goreli(Vector3f dunyaYonu, Vec3 kamera) {
        Vector3f g = new Vector3f(dunyaYonu).normalize().mul(uzaklik);
        if (kurulu) {
            g.add((float) (kokX - kamera.x), (float) (kokY - kamera.y), (float) (kokZ - kamera.z));
        }
        return g;
    }

    /** Kameraya göre merkez, gök birimine ölçekli (|merkez| = {@value #SIGDIRMA}). */
    public Vector3f merkez(Vector3f dunyaYonu, Vec3 kamera) {
        Vector3f g = goreli(dunyaYonu, kamera);
        float l = g.length();
        return l < 1e-3f ? new Vector3f(dunyaYonu).normalize().mul(SIGDIRMA) : g.mul(SIGDIRMA / l);
    }

    /** 1 blok = kaç gök birimi ({@value #SIGDIRMA} / mesafe). */
    public float birim(Vector3f dunyaYonu, Vec3 kamera) {
        return SIGDIRMA / Math.max(1f, goreli(dunyaYonu, kamera).length());
    }

    /** Nesnenin kökten uzaklığı (blok). */
    public float uzaklik() {
        return uzaklik;
    }

    /** Çıkış ve evre değişimi: sonraki {@link #guncelle} kökü kameraya koyar. */
    public void sifirla() {
        kurulu = false;
    }
}
