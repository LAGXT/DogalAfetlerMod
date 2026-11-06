package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.MonotonIlerleme;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import java.util.Locale;
import net.minecraft.Util;

/**
 * İstemcinin karede gördüğü gökyüzü: {@link IstemciDonem}'deki ay/evre/ilerleme ve evre başlangıcından türetilir. Geçişler
 * ölçekli afet saatinden hesaplandığı için herkes (sonradan giren dahil) aynı kareyi görür; önceki evre zincirin sabit
 * sırasından bilinir. Yalnız istemci ana thread'inde.
 */
public final class IstemciGokyuzu {
    /** Etkin kanca kademesi (K0-26): sarmalayıcı (1. ve 2. kademe) ya da son çare {@code AFTER_SKY}. */
    enum Kanca { SARMALAYICI, AFTER_SKY }

    /** Bir karenin gökyüzü durumu. {@code onceki} null ise geçiş yoktur. */
    record Gorunum(GunesEvresi evre, GunesEvresi onceki, float gecis, float ilerleme, float saniye, boolean ayVar,
            float boyutOrani, float renkGucu) {
        boolean vanillaMi() {
            return evre == GunesEvresi.NORMAL && ayVar && (onceki == null || gecis >= 1f);
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "evre=%s onceki=%s gecis=%.2f ilerleme=%.3f sn=%.1f ay=%s", evre.kimlik(),
                    onceki == null ? "-" : onceki.kimlik(), gecis, ilerleme, saniye, ayVar);
        }
    }

    static volatile Kanca kanca = Kanca.SARMALAYICI;
    private static final MonotonIlerleme ILERLEME = new MonotonIlerleme();
    private static long sonGuncelleme = Util.getMillis();

    private IstemciGokyuzu() {
    }

    /** Zincirin sabit sırasında bu evreden önceki evre; NORMAL'e dönüş (sıfırlama) anındır. */
    static GunesEvresi oncekiEvre(GunesEvresi evre) {
        return switch (evre) {
            case BUYUYEN -> GunesEvresi.NORMAL;
            case SUPERNOVA -> GunesEvresi.BUYUYEN;
            case GUNESSIZ -> GunesEvresi.SUPERNOVA;
            case KIZIL_CUCE -> GunesEvresi.GUNESSIZ;
            case NORMAL -> null;
        };
    }

    static Gorunum gorunum(float kismiTick) {
        DonemPaketi d = IstemciDonem.durum();
        long simdi = Util.getMillis();
        float gecenTick = (simdi - sonGuncelleme) / 50f;
        sonGuncelleme = simdi;
        long anahtar = d.evreBaslangicSaati() * 8 + d.gunesEvresi().ordinal();
        float ilerleme = ILERLEME.guncelle(anahtar, d.evreIlerlemesi(), gecenTick);
        double gecen = Math.max(0.0, IstemciDonem.saat(kismiTick) - d.evreBaslangicSaati());
        float boyut = 0.6f;
        float renk = 1f;
        try {
            boyut = (float) GokyuzuAyarlari.kizilCuceBoyutOrani();
            renk = (float) GokyuzuAyarlari.kizilCuceRenkGucu();
        } catch (IllegalStateException yuklenmedi) {
            // Sunucu config'i dünyaya girmeden eşitlenmez; o ana kadar varsayılan görünüm.
        }
        return new Gorunum(d.gunesEvresi(), oncekiEvre(d.gunesEvresi()), GokyuzuEgrileri.gecis(gecen), ilerleme,
                (float) (gecen / 20.0), d.ayVar(), boyut, renk);
    }
}
