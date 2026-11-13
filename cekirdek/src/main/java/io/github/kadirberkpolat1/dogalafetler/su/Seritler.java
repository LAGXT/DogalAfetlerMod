package io.github.kadirberkpolat1.dogalafetler.su;

import java.util.Arrays;

/**
 * Dalga cephesinin yanal şeritleri (K-E76, K-E77): dalga düz bir çizgi olarak ilerlemez; sunucu başlangıçta cephe boyunca eşit genişliği
 * olan şeritlerin her biri için kara yönünde ne kadar ilerleyebileceğini (engele, yükselişe çarpınca durur) ve engelin
 * çevresinden dolanarak sürenin yükseklik ölçeğini hesaplar. İstemci aynı çizgi içi formülle çizer, yeni paket yoktur.
 * <ul>
 * <li>{@code erisim}: şeridin ulaşabileceği en uzak cephe konumunun dalga {@code menzil}'ine oranı (0..1; şerit başına 16 bit, yüksek
 * bayt önce: 352 blokluk menzilde ~0,005 blok çözünürlük; bayt tabanlı kodlamada çözünürlük 1,4 blok olup su duvarın içine taşıyordu);</li>
 * <li>{@code olcek}: şeridin yükseklik çarpanı (0..1, bayt; engelin kenarında dolanan su daha alçak);</li>
 * <li>{@code karaBasi}: cephenin başlangıç çizgisinden bu uzaklıktan sonra (kara) yükseklik her blokta azalır ve menzilde
 * {@value #EN_AZ_AZALMA} çarpanına iner; sonra dalga söner. Sonsuz: azalma yok.</li>
 * </ul>
 * Erişim şeridin kendi genişliğinde sabittir (en yakın şerit: bir şeridin engelinin etkisi komşu şeride taşmaz, dalga engelsiz sütuna
 * ulaşır, engelli sütuna ulaşmaz); yükseklik ölçeği şerit ortalarında geçerlidir, aralarında doğrusal geçilir.
 * Değişmez; {@link #YOK} şeritsiz ve azalmasız eski dalgadır.
 */
public final class Seritler {
    public static final Seritler YOK = new Seritler(Double.POSITIVE_INFINITY, new byte[0], new byte[0]);
    /** Menzile varan cephedeki yükseklik çarpanı (son blokta); sonrasında dalga söner. */
    public static final double EN_AZ_AZALMA = 0.15;

    private final double karaBasi;
    /** şerit başına iki bayt (yüksek, alçak). */
    private final byte[] erisim;
    private final byte[] olcek;

    public Seritler(double karaBasi, byte[] erisim, byte[] olcek) {
        if (erisim.length != 2 * olcek.length) {
            throw new IllegalArgumentException("erişim dizisi ölçek dizisinin iki katı uzunlukta olmalı");
        }
        this.karaBasi = karaBasi;
        this.erisim = erisim.clone();
        this.olcek = olcek.clone();
    }

    /** Oran dizilerinden ({@code 0..1}) kurar. */
    public static Seritler of(double karaBasi, double[] erisimOran, double[] olcek) {
        byte[] e = new byte[2 * erisimOran.length];
        byte[] o = new byte[olcek.length];
        for (int i = 0; i < erisimOran.length; i++) {
            int v = (int) Math.round(Math.max(0, Math.min(1, erisimOran[i])) * 65535);
            e[2 * i] = (byte) (v >> 8);
            e[2 * i + 1] = (byte) v;
        }
        for (int i = 0; i < o.length; i++) {
            o[i] = (byte) Math.round(Math.max(0, Math.min(1, olcek[i])) * 255);
        }
        return new Seritler(karaBasi, e, o);
    }

    private static double oran(byte b) {
        return (b & 0xFF) / 255.0;
    }

    private double erisimOran(int i) {
        return (((erisim[2 * i] & 0xFF) << 8) | (erisim[2 * i + 1] & 0xFF)) / 65535.0;
    }

    public boolean yok() {
        return olcek.length == 0 && Double.isInfinite(karaBasi);
    }

    public int serit() {
        return olcek.length;
    }

    public double karaBasi() {
        return karaBasi;
    }

    public byte[] erisimBayt() {
        return erisim.clone();
    }

    public byte[] olcekBayt() {
        return olcek.clone();
    }

    /** {@code i}. şeridin ulaşabildiği en uzak cephe konumu (menzil oranıyla) ya da şeritsizse 1. */
    public double erisimOrani(int i) {
        return olcek.length == 0 ? 1.0 : erisimOran(i);
    }

    /** Yanal konumda ({@code u01}: 0 sol kenar, 1 sağ kenar) cephenin ulaşabileceği en uzak konum (blok); en yakın şerit. */
    public double erisim(double u01, double menzil) {
        int n = olcek.length;
        if (n == 0) {
            return menzil;
        }
        int i = Math.max(0, Math.min(n - 1, (int) Math.floor(u01 * n)));
        return menzil * erisimOran(i);
    }

    /** Yanal konumdaki yükseklik çarpanı (1: tam). */
    public double olcek(double u01) {
        return ara(olcek, u01);
    }

    /** Cephe {@code c} blok ilerlemişken kara azalması çarpanı (1: tam yükseklik). */
    public double azalma(double c, double menzil) {
        if (Double.isInfinite(karaBasi) || c <= karaBasi || !(menzil > karaBasi)) {
            return 1.0;
        }
        double f = Math.min(1.0, (c - karaBasi) / (menzil - karaBasi));
        return 1.0 - (1.0 - EN_AZ_AZALMA) * f;
    }

    private static double ara(byte[] d, double u01) {
        if (d.length == 0) {
            return 1.0;
        }
        double p = Math.max(0, Math.min(1, u01)) * d.length - 0.5;
        if (p <= 0) {
            return oran(d[0]);
        }
        if (p >= d.length - 1) {
            return oran(d[d.length - 1]);
        }
        int i = (int) p;
        double f = p - i;
        return oran(d[i]) * (1 - f) + oran(d[i + 1]) * f;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Seritler s && Double.compare(karaBasi, s.karaBasi) == 0 && Arrays.equals(erisim, s.erisim)
                && Arrays.equals(olcek, s.olcek);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Double.hashCode(karaBasi) + Arrays.hashCode(erisim)) + Arrays.hashCode(olcek);
    }

    @Override
    public String toString() {
        return yok() ? "Seritler.YOK" : "Seritler[karaBasi=" + karaBasi + ", serit=" + olcek.length + "]";
    }
}
