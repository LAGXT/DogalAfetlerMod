package io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi;

import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.AyCarpmasiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyEgrileri;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.core.AyDurumu;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import java.util.function.DoubleSupplier;

/**
 * İstemcinin gördüğü ay çarpması: {@link IstemciDonem}'deki ay durumu ve olay saatinden (G8). Yaklaşma, parlama, sarsıntı,
 * 3 boyutlu patlama ve enkaz halkası bu saatten hesaplandığı için sonradan giren oyuncu aynı kareyi görür; ek paket yoktur.
 * Gökyüzü sarmalayıcısı ({@code client.gokyuzu}) {@link #gokyuzuEtkin()} ve {@link #ayiGizle()} ile sorar. Yalnız istemci
 * ana thread'inde.
 */
public final class IstemciAy {
    /** Ay durumu ve olaydan beri geçen ölçekli saniye (olay saati yoksa NaN). */
    public record Durum(AyDurumu ay, float saniye) {
        boolean yaklasiyor() {
            return ay == AyDurumu.YAKLASIYOR && !Float.isNaN(saniye);
        }

        boolean carpti() {
            return ay == AyDurumu.YOK && !Float.isNaN(saniye);
        }
    }

    /**
     * Yalnız görsel otomasyon ({@code -Potomasyon=ay_carpmasi}): kare alınana dek gösterilen ay durumu ve saniyesi; null =
     * canlı. pc2'de FPS &lt; 15 iken kare kapısı 60 sn bekler ({@code /dogalafet hiz} en az 0,1: 6 patlama saniyesi);
     * sabit saat karenin istenen anı göstermesini sağlar.
     */
    private static volatile Durum sabit;

    private IstemciAy() {
    }

    static void sabitle(Durum d) {
        sabit = d;
    }

    /** Sabit saatten bağımsız, paketten tahmin edilen gerçek durum. */
    static Durum canli(float kismiTick) {
        DonemPaketi d = IstemciDonem.durum();
        AyDurumu ay = d.ayDurumu();
        if (ay == AyDurumu.VAR || d.ayOlaySaati() < 0) {
            return new Durum(ay, Float.NaN);
        }
        return new Durum(ay, (float) ((IstemciDonem.saat(kismiTick) - d.ayOlaySaati()) / 20.0));
    }

    public static Durum durum(float kismiTick) {
        Durum s = sabit;
        return s != null ? s : canli(kismiTick);
    }

    /**
     * Gökyüzünü vanilla yerine ortak çizici çizmeli mi: ay yaklaşırken (vanilla ay gizlenir, gündüz de) ve çarpmadan sonra
     * patlama ya da enkaz halkası sürerken.
     */
    public static boolean gokyuzuEtkin() {
        Durum d = durum(0f);
        return d.yaklasiyor() || d.carpti() && AyEgrileri.carpmaIzleriSuruyor(d.saniye(), halkaAcik());
    }

    /** Yaklaşan ay çizilirken vanilla ayın yerine o görünür. */
    public static boolean ayiGizle() {
        return durum(0f).ay() == AyDurumu.YAKLASIYOR;
    }

    static boolean halkaAcik() {
        try {
            return AyCarpmasiAyarlari.halkaAcik();
        } catch (IllegalStateException yuklenmedi) {
            // Sunucu config'i dünyaya girmeden eşitlenmez; varsayılan açık.
            return true;
        }
    }

    static double ayar(DoubleSupplier deger) {
        try {
            return deger.getAsDouble();
        } catch (IllegalStateException yuklenmedi) {
            return 1.0;
        }
    }
}
