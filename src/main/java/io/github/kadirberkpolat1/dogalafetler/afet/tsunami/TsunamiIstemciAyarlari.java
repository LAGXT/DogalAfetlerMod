package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code tsunami} CLIENT bölümü (N13): yalnız oyuncunun kendi istemcisindeki tsunami efektleri. Oynanış değerleri SERVER
 * bölümündedir ({@link TsunamiAyarlari}).
 */
public final class TsunamiIstemciAyarlari {
    private static AyarKaydi.Bolum<Istemci> istemci;

    private TsunamiIstemciAyarlari() {
    }

    static void kaydet() {
        istemci = AyarKaydi.istemciBolumu("tsunami", "Tsunami efektleri (madde 1, N13)", Istemci::new);
    }

    /** Tsunaminin kendi yağmuru (damla, halka, yağmur sesi ve gri-mavi yağmur sisi) açık mı; varsayılan kapalı (AF3-14). */
    public static boolean yagmur() {
        return istemci.get().yagmur.get();
    }

    private static final class Istemci {
        private final ForgeConfigSpec.BooleanValue yagmur;

        private Istemci(ForgeConfigSpec.Builder k) {
            yagmur = k.comment("Tsunami AKTIF ve bitiş aşamasında kendi yağmurunu yağdırır: dalga yönüne eğik damlalar, yüzeyde halkalar,",
                    "yağmur sesi ve görüşü %70'e indiren gri-mavi sis. Kapalıyken (varsayılan) hiçbiri üretilmez ve çizilmez; dalga,",
                    "dalga tepesi serpintisi ve köpük yağmur değildir, her zaman görünür.")
                    .define("yagmur", false);
        }
    }
}
