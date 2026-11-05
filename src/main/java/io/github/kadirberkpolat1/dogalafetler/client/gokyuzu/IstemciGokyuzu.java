package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuDurumu;

/** İstemcideki gökyüzü önbelleği: sunucudan gelen son durum ve spike'ın kanca seçimi. Yalnız render thread'i okur. */
public final class IstemciGokyuzu {
    /** Spike'ta karşılaştırılan iki kanca: (b) sarmalayıcı efekt ve (c) AFTER_SKY üstüne çizim. */
    public enum Kanca {
        SARMALAYICI,
        AFTER_SKY
    }

    private static volatile GokyuzuDurumu durum = GokyuzuDurumu.VARSAYILAN;
    static volatile Kanca kanca = Kanca.SARMALAYICI;
    /** AFTER_SKY kancasında vanilla ayın üstüne gökyüzü renginde örtü çizilsin mi (ay gizleme denemesi). */
    static volatile boolean ayOrtusu = false;

    private IstemciGokyuzu() {
    }

    public static GokyuzuDurumu durum() {
        return durum;
    }

    public static void durumAl(GokyuzuDurumu yeni) {
        durum = yeni;
        DogalAfetler.LOGGER.info("[gokyuzu] istemci durumu aldı: {}", yeni);
    }

    static void sifirla() {
        durum = GokyuzuDurumu.VARSAYILAN;
    }
}
