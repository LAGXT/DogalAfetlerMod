package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Afet seviyesinin ağırlıklı rastgele seçimi (madde 10). Seviye 4 ve 5 art arda gelmez: önceki doğal seviyeli afet 4 ya da
 * 5 idiyse bu seçimde ikisi de dıştadır ("aynı seviye art arda en fazla bir kez" bunun özel hâlidir; komutla başlatılan
 * afet önceki seviye sayılmaz, K-E67). {@code afetMaksSiddet} gamerule'ü üstü keser. Afet yoğunluğu yüksekse
 * ({@link AfetYogunlugu}) ağırlık düşük seviyelere kayar. Diziler sıfır tabanlıdır: indeks 0 = Seviye 1.
 */
public final class SiddetSecici {
    private static final int SEVIYE_SAYISI = AfetTuru.EN_YUKSEK_SEVIYE;

    /** Bir seviyenin olasılığının 0 olma nedeni ({@code /dogalafet gun} ve {@code durum} çıktısı). */
    public enum KapaliNeden {
        /** {@code afetMaksSiddet} gamerule'ünün üstünde. */
        MAKS,
        /** Oyun günü seviyenin açıldığı günün altında. */
        GUN,
        /** Oyun günü yetiyor ama gelişim kriteri açıkken etkin gün yetmiyor (madde 10). */
        GELISIM,
        /** Önceki doğal seviyeli afet 4 ya da 5 idi. */
        ART_ARDA
    }

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

    /** Normalleştirilmiş olasılıklar (toplam 1), yoğunluk zayıflatması olmadan. */
    public static double[] olasiliklar(SiddetAyarlari ayarlar, double gun, int maksSiddet, boolean oncekiYuksek) {
        return olasiliklar(ayarlar, gun, maksSiddet, oncekiYuksek, 1.0);
    }

    /** Normalleştirilmiş olasılıklar (toplam 1); {@code yuk} afet yoğunluğudur ({@link AfetYogunlugu#yuk}). */
    public static double[] olasiliklar(SiddetAyarlari ayarlar, double gun, int maksSiddet, boolean oncekiYuksek,
            double yuk) {
        double[] agirlik = agirliklar(ayarlar, gun, maksSiddet, oncekiYuksek);
        double toplam = 0;
        for (int i = 0; i < SEVIYE_SAYISI; i++) {
            agirlik[i] *= AfetYogunlugu.seviyeCarpani(i + 1, yuk);
            toplam += agirlik[i];
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

    /** Seviye (1–5) seçer, yoğunluk zayıflatması olmadan. */
    public static int sec(SiddetAyarlari ayarlar, double gun, int maksSiddet, boolean oncekiYuksek,
            RandomGenerator rastgele) {
        return sec(ayarlar, gun, maksSiddet, oncekiYuksek, 1.0, rastgele);
    }

    /** Seviye (1–5) seçer. */
    public static int sec(SiddetAyarlari ayarlar, double gun, int maksSiddet, boolean oncekiYuksek, double yuk,
            RandomGenerator rastgele) {
        double[] olasilik = olasiliklar(ayarlar, gun, maksSiddet, oncekiYuksek, yuk);
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
     * Seviyenin olasılığı neden 0; açıksa boş. {@code gercekGun} oyun günü, {@code etkinGun} gelişim kriterinden sonraki
     * gün ({@link #etkinGun}). Sıra: gamerule üst sınırı, gün eşiği, gelişim kriteri, art arda 4-5 kuralı.
     */
    public static Optional<KapaliNeden> kapaliNedeni(SiddetAyarlari ayarlar, double gercekGun, double etkinGun,
            int seviye, int maksSiddet, boolean oncekiYuksek) {
        if (seviye > Math.max(AfetTuru.EN_DUSUK_SEVIYE, maksSiddet)) {
            return Optional.of(KapaliNeden.MAKS);
        }
        int esik = ayarlar.esik(seviye);
        if (gercekGun < esik) {
            return Optional.of(KapaliNeden.GUN);
        }
        if (etkinGun < esik) {
            return Optional.of(KapaliNeden.GELISIM);
        }
        if (oncekiYuksek && yuksek(seviye)) {
            return Optional.of(KapaliNeden.ART_ARDA);
        }
        return Optional.empty();
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
