package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

/**
 * Zemin sütununun kar örtüsü (madde 9 "kar katmanı ekle ↔ kaldır"). Çimen ve misel gibi yayılan zeminler ({@code tekKat})
 * blok olarak değişmez, üstüne en çok bir kat kar gelir (vanilla birden fazla katmanın altındaki çimeni topraklaştırır) ve
 * kar varken karlı çimen olarak görünür. Doğal zeminde kar donarken kar eşikleriyle gelir, çözülürken çözülme eşikleriyle
 * kalkar; yapı blokları yapı karı eşiklerini kullanır. Katman yalnız adımın yönünde değişir.
 */
public final class ZeminKari {
    private ZeminKari() {
    }

    public static int katman(DonmaEsikleri e, boolean yapi, boolean tekKat, int mevcut, int seviye, int yon) {
        if (yon == 0) {
            return mevcut;
        }
        int istenen = yapi ? e.yapiKarKatmani(seviye, yon)
                : yon > 0 ? e.karKatmani(seviye, tekKat) : e.karKatmaniCozulme(seviye, tekKat);
        return DonmaHedefi.yonlu(mevcut, istenen, yon);
    }
}
