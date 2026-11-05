package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/**
 * Tek seferlik ve geç oyun afetlerinin ayarları (madde 4, bölüm D, H, N17). Güneş patlaması ve ay çarpması oyuncular
 * yeterince gelişince seçilir: oyun günü ve gelişim puanı ({@link GelisimOlcutu}) eşikleri.
 */
public record TekSeferlikAyarlari(boolean gunesPatlamasiTekrarlanabilir, int gunesPatlamasiEnErkenGun,
        double gunesPatlamasiPuanEsigi, int ayCarpmasiEnErkenGun, double ayCarpmasiPuanEsigi) {
    public static final TekSeferlikAyarlari VARSAYILAN = new TekSeferlikAyarlari(false, 60, 35, 30, 20);

    public TekSeferlikAyarlari gunesTekrarIle(boolean tekrarlanabilir) {
        return new TekSeferlikAyarlari(tekrarlanabilir, gunesPatlamasiEnErkenGun, gunesPatlamasiPuanEsigi,
                ayCarpmasiEnErkenGun, ayCarpmasiPuanEsigi);
    }
}
