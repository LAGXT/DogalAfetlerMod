package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;

/**
 * Madde 4 tek seferlik olayları: ay yoksa ay çarpması seçilmez; güneş patlaması yalnız normal güneşte (ya da
 * {@code gunesPatlamasiTekrarlanabilir} açıksa kızıl cücede) seçilir; büyüyen, süpernova ve güneşsiz evrelerde güneş yoktur
 * ya da patlamaktadır. İkisi de kendi gelişim kriterini bekler (bölüm D: "oyuncular yeterince geliştiğinde"; bölüm H:
 * "güneş patlaması ile aynı mantıkta"). Diğer afetler her dönemde seçilebilir.
 */
public final class TekSeferlikKurallar {
    private TekSeferlikKurallar() {
    }

    public static boolean uygunMu(AfetTuru tur, boolean ayVar, GunesEvresi evre, double gun, double gelisimPuani,
            TekSeferlikAyarlari ayarlar) {
        return switch (tur) {
            case AY_CARPMASI -> ayVar && ayarlar.ayCarpmasiKriteri().saglandi(gun, ayarlar.ayCarpmasiMinGun(),
                    gelisimPuani, ayarlar.ayCarpmasiPuanEsigi());
            case GUNES_PATLAMASI -> (evre == GunesEvresi.NORMAL
                    || (evre == GunesEvresi.KIZIL_CUCE && ayarlar.gunesPatlamasiTekrarlanabilir()))
                    && ayarlar.gunesPatlamasiKriteri().saglandi(gun, ayarlar.gunesPatlamasiMinGun(), gelisimPuani,
                            ayarlar.gunesPatlamasiPuanEsigi());
            default -> true;
        };
    }
}
