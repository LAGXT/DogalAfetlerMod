package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import java.util.EnumSet;
import java.util.Set;

/**
 * Bir oyuncunun süren afetteki izi: etki alanı payı için kendi "alanda bulundu / öldü" takibi ve o afete özgü ipuçları
 * ({@link Ipucu}). Her ipucu iki ayrı kaynakta tutulur: afetin olayı ({@code olay*}) ve başarımın yedek denetiminin tahmini
 * ({@code tahmin*}); afet bitince her ipucu için tek kaynak seçilir ({@link #kosullar}, A-31), iki kaynak toplanmaz. Afet
 * numarası ({@code AfetBaglami} başlangıç saati) değişince sıfırlanır. Oyuncu verisinde saklanır.
 */
public final class AfetIzi {
    /** Madde 12 #5: orman yangını alanının içinde en az 30 saniye. */
    public static final int YANGIN_SANIYESI = 30;
    public static final long AFET_YOK = Long.MIN_VALUE;
    public static final long SANIYE_YOK = Long.MIN_VALUE;

    private long no = AFET_YOK;
    public boolean alanda;
    public boolean oldu;
    /** Afetin olayıyla gelen ipuçları ({@link Ipucu#bit()} maskesi). */
    public int olayBitleri;
    /** Yedek denetimin tahmin ettiği ipuçları. */
    public int tahminBitleri;
    /** Yangın alanında sayılan saniyeler, kaynak başına. */
    public int yanginOlay;
    public int yanginTahmin;
    /** Kaynağın son saydığı saniye (aynı saniye ikinci kez sayılmaz). */
    public long yanginOlaySon = SANIYE_YOK;
    public long yanginTahminSon = SANIYE_YOK;

    public long no() {
        return no;
    }

    /** İzi {@code afetNo}'ya bağlar; başka bir afete aitse sıfırlayıp true döner. */
    public boolean afeteBagla(long afetNo) {
        if (no == afetNo) {
            return false;
        }
        no = afetNo;
        alanda = false;
        oldu = false;
        olayBitleri = 0;
        tahminBitleri = 0;
        yanginOlay = 0;
        yanginTahmin = 0;
        yanginOlaySon = SANIYE_YOK;
        yanginTahminSon = SANIYE_YOK;
        return true;
    }

    /** Kayıttan geri kurulum. */
    public void numaraAyarla(long afetNo) {
        no = afetNo;
    }

    /**
     * Bir ipucunu kaynağına yazar; iz değiştiyse true. {@link Ipucu#YANGIN_ALANINDA} saniye sayar: {@code saniye} (oyun
     * saniyesi) kaynağın son saydığı saniyeyse sayılmaz.
     */
    public boolean isaretle(Ipucu ipucu, boolean olaydan, long saniye) {
        if (ipucu == Ipucu.YANGIN_ALANINDA) {
            if (olaydan) {
                if (yanginOlaySon == saniye) {
                    return false;
                }
                yanginOlaySon = saniye;
                yanginOlay++;
            } else {
                if (yanginTahminSon == saniye) {
                    return false;
                }
                yanginTahminSon = saniye;
                yanginTahmin++;
            }
            return true;
        }
        int eski = olaydan ? olayBitleri : tahminBitleri;
        if ((eski & ipucu.bit()) != 0) {
            return false;
        }
        if (olaydan) {
            olayBitleri |= ipucu.bit();
        } else {
            tahminBitleri |= ipucu.bit();
        }
        return true;
    }

    /** {@code ipucu} seçilen kaynakta var mı ({@code olayli} ise olay, değilse tahmin). */
    private boolean var(Ipucu ipucu, Set<Ipucu> olayli) {
        int bitler = olayli.contains(ipucu) ? olayBitleri : tahminBitleri;
        return (bitler & ipucu.bit()) != 0;
    }

    /** Bu afette sağlanan koşullar; {@code olayli}: tek kaynağı afetin olayı olan ipuçları ({@link IpucuKaynaklari}). */
    public Set<SagCikmaKosulu> kosullar(Set<Ipucu> olayli) {
        Set<SagCikmaKosulu> sonuc = EnumSet.noneOf(SagCikmaKosulu.class);
        if (var(Ipucu.SURUKLENDI, olayli)) {
            sonuc.add(SagCikmaKosulu.SURUKLENDI);
        }
        if (var(Ipucu.CUKURA_DUSTU, olayli)) {
            sonuc.add(SagCikmaKosulu.CUKURA_DUSTU);
        }
        int yangin = olayli.contains(Ipucu.YANGIN_ALANINDA) ? yanginOlay : yanginTahmin;
        if (yangin >= YANGIN_SANIYESI) {
            sonuc.add(SagCikmaKosulu.YANGIN_ALANINDA);
        }
        if (!var(Ipucu.HASAR_ALDI, olayli)) {
            sonuc.add(SagCikmaKosulu.HASARSIZ);
        }
        return sonuc;
    }
}
