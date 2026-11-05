package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.random.RandomGenerator;

/**
 * Afet seviyesinin ağırlıklı rastgele seçimi (madde 10). Seviye 4 ve 5 art arda gelmez: önceki seviyeli afet 4 ya da
 * 5 idiyse bu seçimde ikisi de dıştadır ("aynı seviye art arda en fazla bir kez" bunun özel hâlidir).
 * {@code afetMaksSiddet} gamerule'ü üstü keser. Diziler sıfır tabanlıdır: indeks 0 = Seviye 1.
 */
public final class SiddetSecici {
    private static final int SEVIYE_SAYISI = AfetTuru.EN_YUKSEK_SEVIYE;

    private SiddetSecici() {
    }

    /** Seviye 4 ve 5 (Yıkıcı, Felaket). */
    public static boolean yuksek(int seviye) {
        return seviye >= 4;
    }

    /** Ham ağırlıklar; açılmamış, kesilmiş ya da art arda yasaklı seviyenin ağırlığı 0. */
    public static double[] agirliklar(SiddetAyarlari ayarlar, double gun, int maksSiddet, boolean oncekiYuksek) {
        double[] agirlik = new double[SEVIYE_SAYISI];
        int maks = Math.max(AfetTuru.EN_DUSUK_SEVIYE, Math.min(SEVIYE_SAYISI, maksSiddet));
        double ilerleme = ayarlar.nadirlikGecisGunu() <= 0 ? 1.0
                : Math.max(0, Math.min(1, gun / ayarlar.nadirlikGecisGunu()));
        double k = ayarlar.nadirlikBaslangic() + (ayarlar.nadirlikSon() - ayarlar.nadirlikBaslangic()) * ilerleme;
        for (int seviye = 1; seviye <= maks; seviye++) {
            int esik = ayarlar.esik(seviye);
            if (gun < esik || (oncekiYuksek && yuksek(seviye))) {
                continue;
            }
            double rampa = 1.0;
            if (seviye > 1 && ayarlar.acilisRampaGunu() > 0) {
                double r0 = ayarlar.acilisBaslangicOrani();
                rampa = Math.min(1.0, r0 + (1.0 - r0) * (gun - esik) / ayarlar.acilisRampaGunu());
            }
            agirlik[seviye - 1] = Math.pow(k, seviye - 1) * rampa;
        }
        return agirlik;
    }

    /** Normalleştirilmiş olasılıklar (toplam 1). */
    public static double[] olasiliklar(SiddetAyarlari ayarlar, double gun, int maksSiddet, boolean oncekiYuksek) {
        double[] agirlik = agirliklar(ayarlar, gun, maksSiddet, oncekiYuksek);
        double toplam = 0;
        for (double deger : agirlik) {
            toplam += deger;
        }
        double[] olasilik = new double[SEVIYE_SAYISI];
        if (toplam <= 0) {
            olasilik[0] = 1.0;
            return olasilik;
        }
        for (int i = 0; i < SEVIYE_SAYISI; i++) {
            olasilik[i] = agirlik[i] / toplam;
        }
        return olasilik;
    }

    /** Seviye (1–5) seçer. */
    public static int sec(SiddetAyarlari ayarlar, double gun, int maksSiddet, boolean oncekiYuksek,
            RandomGenerator rastgele) {
        double[] olasilik = olasiliklar(ayarlar, gun, maksSiddet, oncekiYuksek);
        double secim = rastgele.nextDouble();
        int son = 1;
        for (int i = 0; i < SEVIYE_SAYISI; i++) {
            if (olasilik[i] <= 0) {
                continue;
            }
            son = i + 1;
            secim -= olasilik[i];
            if (secim < 0) {
                return i + 1;
            }
        }
        return son;
    }

    /**
     * Madde 10'daki isteğe bağlı oyuncu gelişimi kriteri: açıkken şiddet için kullanılan gün, gelişim puanının
     * {@code puanBasinaGun} katıyla sınırlanır (gelişmemiş oyuncular geç oyunda da hafif afet görür).
     */
    public static double etkinGun(SiddetAyarlari ayarlar, double gun, double gelisimPuani) {
        if (!ayarlar.gelisimDahil()) {
            return gun;
        }
        return Math.min(gun, Math.max(0, gelisimPuani) * ayarlar.puanBasinaGun());
    }
}
