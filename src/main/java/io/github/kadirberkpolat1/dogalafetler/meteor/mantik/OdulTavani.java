package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

/**
 * Yağmur meteorlarının ödül sınırı (şef kararı, inceleme-meteor-2-2 Mt10; {@code .vibe/kararlar/meteor-2.md} MT2-20): ödül yalnız
 * büyük bantta ve kaynak (afet) başına, oyuncu başına en çok {@code tavan} eşya. Zar ({@link OdulZari}) önce atılır, bu sınır
 * sonucu kısar; iki eşya tavana sığmazsa Kozmik Çekirdek kalır.
 */
public final class OdulTavani {
    private OdulTavani() {
    }

    /** {@code verilen}: bu kaynak ve oyuncu için şimdiye kadar bırakılan ödül eşyası. */
    public static OdulZari.Sonuc uygula(OdulZari.Sonuc zar, boolean buyuk, int verilen, int tavan) {
        if (!buyuk || zar.bos() || verilen >= tavan) {
            return OdulZari.Sonuc.YOK;
        }
        if (zar.parca() && zar.cekirdek() && verilen + 2 > tavan) {
            return new OdulZari.Sonuc(false, true);
        }
        return zar;
    }

    public static int adet(OdulZari.Sonuc s) {
        return (s.parca() ? 1 : 0) + (s.cekirdek() ? 1 : 0);
    }
}
