package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

/**
 * #19 "Çorba İçer Isınır" (N16): donma eşiğinin altındayken sıcak bir yiyecek yemek ve etkisi sürerken vücudun nötr
 * bölgeye dönmesi. Yiyecek sıcak sayılır: tanınan sıcak yiyecekse (etiket) ya da yedikten sonra
 * {@value #ADAY_PENCERESI} tick içinde vücuda taze bir ısınma etkisi eklendiyse. Zaman sunucu oyun tick'idir.
 */
public final class CorbaTakibi {
    /** Tanınmayan yiyecekten sonra ısınmanın gözlenebileceği süre (tick). */
    public static final int ADAY_PENCERESI = 60;
    /** Etki süresi bilinmeyen tanınan yiyecek için pencere: madde 14'ün en uzun sıcak yiyeceği (180 sn). */
    public static final int VARSAYILAN_SURE = 180 * 20;
    private static final long YOK = Long.MIN_VALUE;

    private long aday = YOK;
    private long bitis = YOK;

    /**
     * Oyuncu bir yiyeceği bitirdi.
     *
     * @param soguk        yerken vücut donma eşiğinin altında mıydı
     * @param sicakYiyecek yiyecek tanınan bir sıcak yiyecek mi
     * @param etkiSuresi   eklenen ısınma etkisinin süresi (tick; bilinmiyorsa 0)
     */
    public void yedi(long simdi, boolean soguk, boolean sicakYiyecek, int etkiSuresi) {
        if (!soguk) {
            aday = YOK;
            return;
        }
        if (sicakYiyecek) {
            aday = YOK;
            bitis = simdi + (etkiSuresi > 0 ? etkiSuresi : VARSAYILAN_SURE);
        } else {
            aday = simdi;
        }
    }

    /** Yedikten sonra vücuda taze bir ısınma etkisi eklendiği görüldü ({@code kalanSure} tick sürecek). */
    public void isinmaGozlendi(long simdi, int kalanSure) {
        if (aday != YOK && simdi - aday <= ADAY_PENCERESI) {
            bitis = simdi + Math.max(1, kalanSure);
        }
        aday = YOK;
    }

    /** Aday sıcaklık teyidi bekliyor mu (yeme anından sonra pencere içinde). */
    public boolean teyitBekliyor(long simdi) {
        if (aday != YOK && simdi - aday > ADAY_PENCERESI) {
            aday = YOK;
        }
        return aday != YOK;
    }

    /** Etki sürerken vücut nötrse bir kez true. */
    public boolean kontrol(long simdi, boolean notr) {
        if (bitis == YOK) {
            return false;
        }
        if (simdi > bitis) {
            bitis = YOK;
            return false;
        }
        if (notr) {
            bitis = YOK;
            return true;
        }
        return false;
    }
}
