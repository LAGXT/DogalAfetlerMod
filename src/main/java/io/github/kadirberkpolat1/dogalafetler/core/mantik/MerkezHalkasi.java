package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import java.util.random.RandomGenerator;

/**
 * Doğal afet merkezinin hedef oyuncuya göre halkası (kullanıcı isteği: afet oyuncunun "eski" yerine ya da tam üstüne değil,
 * seçim anındaki taze konumundan {@code enAzChunk}–{@code enCokChunk} chunk uzağa kurulur; varsayılan 3–10 chunk = 48–160
 * blok). Uzaklık kuşbakışıdır (yatay), yön rastgeledir, uzaklık halka içinde düzgün dağılır. Saf Java.
 *
 * @param enAzChunk  en az uzaklık (chunk)
 * @param enCokChunk en çok uzaklık (chunk); {@code enAzChunk}'tan küçük olamaz
 */
public record MerkezHalkasi(int enAzChunk, int enCokChunk) {
    public static final int VARSAYILAN_EN_AZ = 3;
    public static final int VARSAYILAN_EN_COK = 10;
    public static final MerkezHalkasi VARSAYILAN = new MerkezHalkasi(VARSAYILAN_EN_AZ, VARSAYILAN_EN_COK);

    private static final double CHUNK_BLOK = 16.0;

    public MerkezHalkasi {
        enAzChunk = Math.max(0, enAzChunk);
        enCokChunk = Math.max(enAzChunk, enCokChunk);
    }

    public double enAzBlok() {
        return enAzChunk * CHUNK_BLOK;
    }

    public double enCokBlok() {
        return enCokChunk * CHUNK_BLOK;
    }

    /** Oyuncudan halka içinde rastgele yönde bir yatay ofset {@code {dx, dz}} (blok). */
    public double[] ofset(RandomGenerator rastgele) {
        double mesafe = enAzBlok() + rastgele.nextDouble() * (enCokBlok() - enAzBlok());
        double aci = rastgele.nextDouble() * Math.PI * 2;
        return new double[] {Math.cos(aci) * mesafe, Math.sin(aci) * mesafe};
    }

    /** Yatay ofset halkanın içinde mi ({@code tolerans}: blok yuvarlama payı). */
    public boolean icinde(double dx, double dz, double tolerans) {
        double mesafe = Math.sqrt(dx * dx + dz * dz);
        return mesafe >= enAzBlok() - tolerans && mesafe <= enCokBlok() + tolerans;
    }
}
