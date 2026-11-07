package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.BasarimTuru;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code basarim} SERVER config bölümü (N17): "sağ çıkma" etki alanı payı, ödül XP'leri ve kozmetik ünvanın
 * görünürlüğü. Şartnamenin verdiği sayılar (30 sn, 3 gün, %50, 5 tüy, 4 yün, 25 balık, 3 oyuncu, 10 sn) advancement
 * JSON'undadır ve veri paketiyle değiştirilebilir.
 */
public final class BasarimAyarlari {
    private static AyarKaydi.Bolum<Sunucu> sunucu;

    private BasarimAyarlari() {
    }

    public static void kaydet() {
        sunucu = AyarKaydi.sunucuBolumu("basarim", "Başarımlar (madde 12, N16)", Sunucu::new);
    }

    /** Afetin etki alanının bu kadar blok dışı da "sağ çıkma" için alan sayılır (0: çekirdeğin etkilenen tanımı). */
    public static double etkiAlaniPayi() {
        return sunucu.get().etkiAlaniPayi.get();
    }

    public static int xp(BasarimTuru tur) {
        Sunucu s = sunucu.get();
        return switch (tur) {
            case GOREV -> s.gorevXp.get();
            case HEDEF -> s.hedefXp.get();
            case MEYDAN_OKUMA -> s.meydanOkumaXp.get();
        };
    }

    public static boolean unvanGoster() {
        return sunucu.get().unvanGoster.get();
    }

    private static final class Sunucu {
        private final ForgeConfigSpec.IntValue etkiAlaniPayi;
        private final ForgeConfigSpec.IntValue gorevXp;
        private final ForgeConfigSpec.IntValue hedefXp;
        private final ForgeConfigSpec.IntValue meydanOkumaXp;
        private final ForgeConfigSpec.BooleanValue unvanGoster;

        private Sunucu(ForgeConfigSpec.Builder k) {
            etkiAlaniPayi = k.comment("Madde 12 'sağ çıkmak': afetin etki alanının bu kadar blok dışında AKTIF aşamada",
                    "bulunan oyuncular da afet bitene kadar ölmezse sağ çıkmış sayılır.",
                    "0 = çekirdeğin etkilenen tanımı (yalnız etki alanı).")
                    .defineInRange("etkiAlaniPayi", 0, 0, 256);
            k.comment("Başarım XP ödülleri. Advancement JSON'u varsayılanı verir; buradaki değer farklıysa fark",
                    "kazanılırken eklenir ya da geri alınır. Hedef başarımlar ayrıca 1-2 sıcak yiyecek verir.")
                    .push("odul");
            gorevXp = k.comment("Görev başarımlarının XP'si.")
                    .defineInRange("gorevXp", BasarimTuru.GOREV.varsayilanXp(), 0, 1000);
            hedefXp = k.comment("Hedef başarımlarının XP'si.")
                    .defineInRange("hedefXp", BasarimTuru.HEDEF.varsayilanXp(), 0, 1000);
            meydanOkumaXp = k.comment("Meydan okuma başarımlarının XP'si (en büyük ödül; ayrıca kozmetik ünvan).")
                    .defineInRange("meydanOkumaXp", BasarimTuru.MEYDAN_OKUMA.varsayilanXp(), 0, 1000);
            k.pop();
            unvanGoster = k.comment("Meydan okumaların kozmetik ünvanı modun başarım duyurusunda oyuncu adının önünde",
                    "görünsün (N16, A-37; görünen ad ve tab listesi öneksizdir).")
                    .define("unvanGoster", true);
        }
    }
}
