package io.github.kadirberkpolat1.dogalafetler.donma;

/**
 * Chunk hedef seviyesi (madde 9 YENİ YÜKLENEN CHUNK'LAR ve ÇÖZÜLME). Donma döneminde hedef global ilerlemedir; kızıl cüce
 * çözülmesinde kış ağırlığı {@code w} hedefi globalden %100'e doğru çeker: {@code T = G + (100 − G) · w}. Donma
 * döneminde maske uygulanmaz; yoksa kış bölgeleri süpernovadan hemen sonra aniden donardı (madde 9: "aniden donmasın").
 */
public final class DonmaHedefi {
    private DonmaHedefi() {
    }

    public static int hedef(int globalIlerleme, boolean cozulmeDonemi, double kisAgirligi) {
        int g = Math.max(0, Math.min(100, globalIlerleme));
        if (!cozulmeDonemi) {
            return g;
        }
        double w = Math.max(0.0, Math.min(1.0, kisAgirligi));
        return (int) Math.round(g + (100 - g) * w);
    }

    /** Soğuk biyom her zaman tam kıştır; diğerlerinde maske ağırlığı. */
    public static double kisAgirligi(double maskeAgirligi, boolean sogukBiyom) {
        return sogukBiyom ? 1.0 : Math.max(0.0, Math.min(1.0, maskeAgirligi));
    }

    /** Hedefe en çok {@code adim} kadar yaklaşan bir sonraki seviye. */
    public static int siradakiSeviye(int mevcut, int hedef, int adim) {
        if (mevcut < hedef) {
            return Math.min(hedef, mevcut + adim);
        }
        if (mevcut > hedef) {
            return Math.max(hedef, mevcut - adim);
        }
        return mevcut;
    }
}
