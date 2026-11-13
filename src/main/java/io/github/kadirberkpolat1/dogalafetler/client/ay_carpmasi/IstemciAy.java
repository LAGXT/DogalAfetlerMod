package io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.AyCarpmasiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyEgrileri;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyYonu;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.client.uyum.IrisUyumu;
import io.github.kadirberkpolat1.dogalafetler.core.AyDurumu;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import java.util.function.DoubleSupplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.GameRules;

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

    /** Yedek başlangıç yönü: olay saati bilinmiyorsa (özgün sabit yön). */
    private static final AyYonu.Baslangic YEDEK_BASLANGIC = new AyYonu.Baslangic(AyEgrileri.AZIMUT_DERECE,
            AyEgrileri.YAKLASMA_EGIMI);
    private static AyYonu.Baslangic baslangicOnbellek;
    private static double baslangicAnahtari = Double.NaN;

    /**
     * Yaklaşan ayın gökteki başlangıç yönü (K-E83): olayın (sirenin) başladığı anda vanilla ayın bulunduğu yer. Gün zamanı o
     * ana geri sarılır ({@code şimdiki gün zamanı - geçen tick}; gün döngüsü kapalıysa gün zamanı sabittir) ve gölgelendirici
     * paketinin güneş yolu eğimi uygulanır ({@link IrisUyumu}); sonuç olay başına bir kez hesaplanıp saklanır, böylece yaklaşma
     * ve çarpma sonrası patlama aynı yönü kullanır. Çarpmadan sonra giren oyuncu için yaklaşmanın başlangıcı
     * {@link AyEgrileri#YAKLASMA_SN} öncesi sayılır.
     */
    static AyYonu.Baslangic baslangicYonu(ClientLevel level, float kismiTick) {
        Durum c = canli(kismiTick);
        if (level == null || Float.isNaN(c.saniye())) {
            if (c.ay() == AyDurumu.VAR) {
                baslangicOnbellek = null;
            }
            return baslangicOnbellek != null ? baslangicOnbellek : YEDEK_BASLANGIC;
        }
        double afetTick = (c.saniye() + (c.carpti() ? AyEgrileri.YAKLASMA_SN : 0f)) * 20.0;
        double anahtar = IstemciDonem.saat(kismiTick) - afetTick;
        if (baslangicOnbellek == null || Math.abs(anahtar - baslangicAnahtari) > 100.0) {
            double gercekTick = afetTick / Math.max(0.05, IstemciDonem.durum().hiz());
            double gun = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT) ? level.getDayTime() - gercekTick
                    : level.getDayTime();
            float zaman = level.dimensionType().timeOfDay((long) Math.floor(gun));
            baslangicOnbellek = AyYonu.baslangic(zaman, IrisUyumu.gokEgimiDerece());
            baslangicAnahtari = anahtar;
            DogalAfetler.LOGGER.info("[ay_carpmasi] yaklaşma yönü: vanilla ay azimut {}°, yükseklik {}° (gün zamanı {}, paket eğimi {}°)",
                    Math.round(baslangicOnbellek.azimutDerece()), Math.round(baslangicOnbellek.egimDerece()),
                    String.format(java.util.Locale.ROOT, "%.3f", zaman), IrisUyumu.gokEgimiDerece());
        }
        return baslangicOnbellek;
    }

    /** Önbelleğe alınan başlangıç yönü silinir (dünyadan çıkış, sıfırlama). */
    static void yonuSifirla() {
        baslangicOnbellek = null;
        baslangicAnahtari = Double.NaN;
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
