package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;

/**
 * Madde 4 tek seferlik olayları: ay yoksa ay çarpması seçilmez; güneş patlaması yalnız normal güneşte (ya da
 * {@code gunesPatlamasiTekrarlanabilir} açıksa kızıl cücede) seçilir; büyüyen, süpernova ve güneşsiz evrelerde güneş yoktur
 * ya da patlamaktadır. Pulsar zinciri (pulsar-2, şef kararı): pulsar yolunun kızıl cüce doğuşuna ulaşıp eski güneşe dönen
 * dünyada ({@code zincirTukendi}) güneş patlaması yalnız {@code tekrarlanabilir} açıkken yeniden seçilir; pulsar yolundaki
 * doğuşta ({@code donusYolu}) hiç seçilmez (doğuş yarıda kalırdı). İkisi de kendi gelişim kriterini bekler (bölüm D:
 * "oyuncular yeterince geliştiğinde"; bölüm H: "güneş patlaması ile aynı mantıkta"). Diğer afetler her dönemde
 * seçilebilir.
 */
public final class TekSeferlikKurallar {
    private TekSeferlikKurallar() {
    }

    /**
     * @param donusYolu güneşsiz dönem ya da kızıl cüce doğuşu pulsar yolunda ({@code PulsarYolu.donusYolu})
     * @param zincirTukendi eski güneş pulsar yolundan döndü ({@code PulsarYolu.zincirTukendi}, kalıcı)
     */
    public static boolean uygunMu(AfetTuru tur, boolean ayVar, GunesEvresi evre, boolean donusYolu,
            boolean zincirTukendi, double gun, double gelisimPuani, TekSeferlikAyarlari ayarlar) {
        return switch (tur) {
            case AY_CARPMASI -> ayVar && ayarlar.ayCarpmasiKriteri().saglandi(gun, ayarlar.ayCarpmasiMinGun(),
                    gelisimPuani, ayarlar.ayCarpmasiPuanEsigi());
            case GUNES_PATLAMASI -> gunesSecilebilir(evre, donusYolu, zincirTukendi,
                    ayarlar.gunesPatlamasiTekrarlanabilir())
                    && ayarlar.gunesPatlamasiKriteri().saglandi(gun, ayarlar.gunesPatlamasiMinGun(), gelisimPuani,
                            ayarlar.gunesPatlamasiPuanEsigi());
            default -> true;
        };
    }

    private static boolean gunesSecilebilir(GunesEvresi evre, boolean donusYolu, boolean zincirTukendi,
            boolean tekrarlanabilir) {
        return switch (evre) {
            case NORMAL -> !zincirTukendi || tekrarlanabilir;
            case KIZIL_CUCE -> tekrarlanabilir && !donusYolu;
            default -> false;
        };
    }
}
