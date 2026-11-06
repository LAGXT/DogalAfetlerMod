package io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi;

import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.AyCarpmasiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyEgrileri;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.core.AyDurumu;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import java.util.function.DoubleSupplier;

/**
 * İstemcinin gördüğü ay çarpması: {@link IstemciDonem}'deki ay durumu ve olay saatinden (G8). Yaklaşma, parlama, sarsıntı,
 * mantar sütunu ve enkaz halkası bu saatten hesaplandığı için sonradan giren oyuncu aynı kareyi görür; ek paket yoktur.
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

    private IstemciAy() {
    }

    public static Durum durum(float kismiTick) {
        DonemPaketi d = IstemciDonem.durum();
        AyDurumu ay = d.ayDurumu();
        if (ay == AyDurumu.VAR || d.ayOlaySaati() < 0) {
            return new Durum(ay, Float.NaN);
        }
        return new Durum(ay, (float) ((IstemciDonem.saat(kismiTick) - d.ayOlaySaati()) / 20.0));
    }

    /**
     * Gökyüzünü vanilla yerine ortak çizici çizmeli mi: ay yaklaşırken (vanilla ay gizlenir, gündüz de) ve çarpmadan sonra
     * mantar sütunu ya da enkaz halkası sürerken.
     */
    public static boolean gokyuzuEtkin() {
        Durum d = durum(0f);
        return d.yaklasiyor() || d.carpti() && AyEgrileri.carpmaIzleriSuruyor(d.saniye(), halkaAcik());
    }

    /** Yaklaşan ay çizilirken vanilla ayın yerine o görünür. */
    public static boolean ayiGizle() {
        return IstemciDonem.durum().ayDurumu() == AyDurumu.YAKLASIYOR;
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
