package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import java.util.EnumSet;
import java.util.Set;

/**
 * Bir oyuncunun süren afetteki izi: o afete özgü koşullar (sürüklendi, çukura düştü, yangın alanında geçen saniye,
 * hasar aldı) ve etki alanı payı için kendi "alanda bulundu / öldü" takibi. Afet numarası ({@code AfetBaglami}
 * başlangıç saati) değişince sıfırlanır. Oyuncu verisinde saklanır; sunucu yeniden açılınca süren afetle birlikte
 * sürer.
 */
public final class AfetIzi {
    /** Madde 12 #5: orman yangını alanının içinde en az 30 saniye. */
    public static final int YANGIN_SANIYESI = 30;
    public static final long AFET_YOK = Long.MIN_VALUE;

    private long no = AFET_YOK;
    public boolean alanda;
    public boolean oldu;
    public boolean suruklendi;
    public boolean cukuraDustu;
    public int yanginSaniye;
    public boolean hasarAldi;

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
        suruklendi = false;
        cukuraDustu = false;
        yanginSaniye = 0;
        hasarAldi = false;
        return true;
    }

    /** Kayıttan geri kurulum. */
    public void numaraAyarla(long afetNo) {
        no = afetNo;
    }

    /** Bu afette sağlanan koşullar. */
    public Set<SagCikmaKosulu> kosullar() {
        Set<SagCikmaKosulu> sonuc = EnumSet.noneOf(SagCikmaKosulu.class);
        if (suruklendi) {
            sonuc.add(SagCikmaKosulu.SURUKLENDI);
        }
        if (cukuraDustu) {
            sonuc.add(SagCikmaKosulu.CUKURA_DUSTU);
        }
        if (yanginSaniye >= YANGIN_SANIYESI) {
            sonuc.add(SagCikmaKosulu.YANGIN_ALANINDA);
        }
        if (!hasarAldi) {
            sonuc.add(SagCikmaKosulu.HASARSIZ);
        }
        return sonuc;
    }
}
