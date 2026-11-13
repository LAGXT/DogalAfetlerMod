package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

/**
 * Yağmur meteorlarının ödül sınırı (şef kararı, inceleme-meteor-2-2 Mt10; {@code .vibe/kararlar/meteor-2.md} MT2-20): ödül yalnız
 * büyük bantta ve kaynak (afet) başına, oyuncu başına en çok {@code tavan} eşya. Zar ({@link OdulZari}) önce atılır, bu sınır
 * sonucu kısar: tavana sığmayan fazla parçalar önce atılır; Kozmik Çekirdek en son kısılır (ilk sığan eşyadır). K-E74: tavan
 * (varsayılan 8) değişmez, yalnız her büyük meteorun eşyası artar.
 */
public final class OdulTavani {
    private OdulTavani() {
    }

    /** {@code verilen}: bu kaynak ve oyuncu için şimdiye kadar bırakılan ödül eşyası. */
    public static OdulZari.Sonuc uygula(OdulZari.Sonuc zar, boolean buyuk, int verilen, int tavan) {
        if (!buyuk || zar.bos() || verilen >= tavan) {
            return OdulZari.Sonuc.YOK;
        }
        int kalan = tavan - verilen;
        int parca = Math.min(zar.parcaAdedi(), kalan - (zar.cekirdek() ? 1 : 0));
        if (parca == zar.parcaAdedi()) {
            return zar;
        }
        return new OdulZari.Sonuc(parca >= 1, zar.cekirdek(), Math.max(0, parca - 1));
    }
}
