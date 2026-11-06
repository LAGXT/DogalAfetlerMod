package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import java.util.Objects;

/**
 * Tek seferlik ve geç oyun afetlerinin ayarları (madde 4, bölüm D, H, N17). Güneş patlaması ve ay çarpması kendi
 * {@link GelisimKriteri}'ne göre seçilebilir olur: oyun günü eşiği, gelişim puanı eşiği ya da ikisinin birleşimi.
 */
public record TekSeferlikAyarlari(boolean gunesPatlamasiTekrarlanabilir, GelisimKriteri gunesPatlamasiKriteri,
        int gunesPatlamasiMinGun, double gunesPatlamasiPuanEsigi, GelisimKriteri ayCarpmasiKriteri, int ayCarpmasiMinGun,
        double ayCarpmasiPuanEsigi) {
    public static final TekSeferlikAyarlari VARSAYILAN = new TekSeferlikAyarlari(false, GelisimKriteri.GUN, 40, 35,
            GelisimKriteri.GUN, 25, 20);

    public TekSeferlikAyarlari {
        Objects.requireNonNull(gunesPatlamasiKriteri, "gunesPatlamasiKriteri");
        Objects.requireNonNull(ayCarpmasiKriteri, "ayCarpmasiKriteri");
    }

    public TekSeferlikAyarlari gunesTekrarIle(boolean tekrarlanabilir) {
        return new TekSeferlikAyarlari(tekrarlanabilir, gunesPatlamasiKriteri, gunesPatlamasiMinGun,
                gunesPatlamasiPuanEsigi, ayCarpmasiKriteri, ayCarpmasiMinGun, ayCarpmasiPuanEsigi);
    }

    public TekSeferlikAyarlari kriterlerIle(GelisimKriteri gunes, GelisimKriteri ay) {
        return new TekSeferlikAyarlari(gunesPatlamasiTekrarlanabilir, gunes, gunesPatlamasiMinGun, gunesPatlamasiPuanEsigi,
                ay, ayCarpmasiMinGun, ayCarpmasiPuanEsigi);
    }
}
