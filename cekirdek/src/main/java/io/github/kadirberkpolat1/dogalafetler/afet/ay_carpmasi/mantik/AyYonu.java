package io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik;

/**
 * Yaklaşan ayın gökte nereden geleceği (K-E83): ay, olayın başladığı anda vanilla ayın bulunduğu yerden gelir, sabit bir
 * yönden değil. Vanilla ayın yönü gün zamanından ve (gölgelendirici paketi açıksa) paketin {@code sunPathRotation} eğiminden
 * hesaplanır: dünya çerçevesinde {@code Ry(-90) · Rz(eğim) · Rx(zaman · 360°)} dönüşü ayı {@code (0, -1, 0)}'dan taşır
 * (vanilla ve Iris/Oculus aynı sırayı uygular). Saf Java; Minecraft sınıflarına dokunmaz. Eksenler: +X doğu, +Y yukarı,
 * +Z güney.
 */
public final class AyYonu {
    /** Gökte ayın başlangıç yönü: +X'ten +Z'ye derece ve ufuktan yükseklik (derece). */
    public record Baslangic(float azimutDerece, float egimDerece) {
    }

    private AyYonu() {
    }

    /**
     * Vanilla ayın birim yönü ({@code x, y, z}).
     *
     * @param zaman {@code ClientLevel.getTimeOfDay} (0 öğle, 0,5 gece yarısı)
     * @param egimDerece gölgelendirici paketinin güneş yolu eğimi; paket yoksa 0
     */
    public static double[] vanillaAy(double zaman, double egimDerece) {
        double teta = zaman * 2.0 * Math.PI;
        double fi = Math.toRadians(egimDerece);
        double c = Math.cos(teta);
        return new double[] {Math.sin(teta), -c * Math.cos(fi), c * Math.sin(fi)};
    }

    /**
     * Yaklaşmanın başlangıç yönü: vanilla ayın azimutu ve yüksekliği. Yükseklik {@link AyEgrileri#EN_AZ_BASLANGIC_EGIMI} ile
     * {@link AyEgrileri#EN_COK_BASLANGIC_EGIMI} arasına kırpılır (ufkun altındaki ay, gündüz komutla başlatılınca, ufka yakın
     * doğar). Ay tam tepedeyse (ya da tam altta) azimut belirsizdir; ayın gidişinden alınır.
     */
    public static Baslangic baslangic(double zaman, double egimDerece) {
        double[] d = vanillaAy(zaman, egimDerece);
        double[] yon = Math.hypot(d[0], d[2]) < 0.05 ? vanillaAy(zaman + 0.01, egimDerece) : d;
        double azimut = Math.toDegrees(Math.atan2(yon[2], yon[0]));
        double egim = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, d[1]))));
        egim = Math.max(AyEgrileri.EN_AZ_BASLANGIC_EGIMI, Math.min(AyEgrileri.EN_COK_BASLANGIC_EGIMI, egim));
        return new Baslangic((float) azimut, (float) egim);
    }
}
