package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

/**
 * #14 "Yükselen Sular": deniz seviyesinin yükselişini yaşa ve sağ kal. Yükseliş sürerken ölen oyuncu o döngüde
 * ({@code DenizSeviyesi.baslangicSaati}) hak kazanmaz; yükseliş tamamlandığı an çevrim içi ve canlı olanlar kazanır.
 */
public final class DenizTakibi {
    public static final long OLUM_YOK = Long.MIN_VALUE;
    private static final float PAY = 1e-3f;

    private DenizTakibi() {
    }

    public static boolean yukseliyorMu(float hedef, float mevcut) {
        return hedef > 0 && mevcut > 0 && mevcut < hedef - PAY;
    }

    public static boolean tamamlandiMi(float hedef, float mevcut) {
        return hedef > 0 && mevcut >= hedef - PAY;
    }

    public static boolean hakKazandiMi(long olumDongusu, long dongu) {
        return olumDongusu != dongu;
    }
}
