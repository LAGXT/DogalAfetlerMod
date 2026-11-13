package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

/**
 * Güneş patlamasındaki buharlaşma (N9, N10). Global derinlik 5 günlük süreçte 0'dan {@code enCok} bloka çıkar; her sütun
 * yüzeyden aşağı en çok bu kadar kurur, sığ göl tamamen kuruyabilir. Ay çarpmasından kalan sanal deniz yükselmesi
 * ("pay") önce tüketilir: gerçek su ancak {@code derinlik − pay} kadar kurur (N10). Sütunun hedefi kaldırılmış ve kalan
 * suyun toplamından hesaplanır; böylece derinlik düşünce (süpernova) aynı kural suyu geri getirir.
 */
public final class BuharlasmaHedefi {
    private BuharlasmaHedefi() {
    }

    public static float derinlikAdimi(float derinlik, int adim, double sureGun, double enCok) {
        if (!(sureGun > 0.0)) {
            throw new IllegalArgumentException("süre pozitif olmalı: " + sureGun);
        }
        return (float) Math.min(enCok, derinlik + enCok * adim / (sureGun * DonmaIlerlemesi.GUN_TICK));
    }

    /** Derinlik bu eşiği aşınca ilk blok kurur (istemci buharı da bu eşikten çıkar; efekt çıkan su gerçekten azalır). */
    public static final double ESIK = 0.01;

    /**
     * Gerçek suya uygulanan tam blok derinliği: kısmi derinlik yukarı yuvarlanır ({@code ESIK} üstü ilk blok), böylece buhar
     * çıkar çıkmaz ilk blok kurur; derinlik {@code enCok} (tam sayı) olunca en çok o kadar blok kalkar.
     */
    public static int etkinDerinlik(float derinlik, double sanalPay) {
        return (int) Math.ceil(Math.max(0.0, derinlik - Math.max(0.0, sanalPay)) - ESIK);
    }

    /** {@code orijinalSuDerinligi} blokluk su sütununun üstten kaç bloğu kurumuş olmalı. */
    public static int kaldirilacak(int etkinDerinlik, int orijinalSuDerinligi) {
        return Math.max(0, Math.min(etkinDerinlik, orijinalSuDerinligi));
    }
}
