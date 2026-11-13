package io.github.kadirberkpolat1.dogalafetler.uyari;

/**
 * İstemciye gelen siren isteğinin kararı (saf mantık, S-02). Çekirdek siren sürerken yeni giren ya da yeniden bağlanan
 * oyunculara 1 Hz'de kalan süreyle istek gönderir; aynı afetin ({@code afetNo}) sireni zaten çalıyorsa istek yok sayılır,
 * çalmıyorsa kalan süreyle başlar. Başka bir afetin sireni gelirse eskisi kısa fade ile kesilir. Durdurma almış (sönen)
 * siren isteği engellemez: aynı afetin sireni durdurmadan sonra bir daha istenmez, gelen istek aynı saatte başlamış yeni
 * bir afettir ({@code stop} + {@code start} aynı tick'te) ve eskisi kendi fade'iyle söner.
 */
public enum SirenKarari {
    YOK_SAY,
    BASLAT,
    KES_VE_BASLAT;

    /**
     * @param caliyor istemcide zarfı bitmemiş bir siren var
     * @param sonuyor o siren durdurma almış, fade-out'ta
     * @param calanAfetNo çalan sirenin afet numarası (afetin başlangıç saati)
     * @param gelenAfetNo paketteki afet numarası
     */
    public static SirenKarari istek(boolean caliyor, boolean sonuyor, long calanAfetNo, long gelenAfetNo) {
        if (!caliyor || sonuyor) {
            return BASLAT;
        }
        return calanAfetNo == gelenAfetNo ? YOK_SAY : KES_VE_BASLAT;
    }

    /** Durdurma yalnız çalan sirenin afetine aitse uygulanır: eski afetin geç kalan durdurması yeni sireni söndürmez. */
    public static boolean durdurUygulanir(boolean caliyor, long calanAfetNo, long gelenAfetNo) {
        return caliyor && calanAfetNo == gelenAfetNo;
    }
}
