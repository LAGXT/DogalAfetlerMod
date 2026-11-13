package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

/**
 * Sütun hedef seviyesi (madde 9 YENİ YÜKLENEN CHUNK'LAR ve ÇÖZÜLME). Donma döneminde hedef global ilerlemedir; donma
 * döneminde maske uygulanmaz, yoksa kış bölgeleri süpernovadan hemen sonra aniden donardı. Kızıl cüce çözülmesinde sütunun
 * kış ağırlığı {@code w} hedefi %100'e çeker, ılık ofset ise kış dışı sütunu daha erken indirir:
 * {@code T = G + (100 − G)·w − ofset·(1 − w)}.
 */
public final class DonmaHedefi {
    private DonmaHedefi() {
    }

    public static int sutunHedefi(int globalIlerleme, boolean cozulmeDonemi, double kisAgirligi, double ofset) {
        int g = Math.max(0, Math.min(100, globalIlerleme));
        if (!cozulmeDonemi) {
            return g;
        }
        double w = Math.max(0.0, Math.min(1.0, kisAgirligi));
        double t = g + (100 - g) * w - Math.max(0.0, ofset) * (1.0 - w);
        return (int) Math.max(0, Math.min(100, Math.round(t)));
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

    /** Yönlü değer: donarken ({@code yon > 0}) azalmaz, çözülürken ({@code yon < 0}) artmaz, {@code yon == 0} değişmez. */
    public static int yonlu(int mevcut, int istenen, int yon) {
        if (yon > 0) {
            return Math.max(mevcut, istenen);
        }
        if (yon < 0) {
            return Math.min(mevcut, istenen);
        }
        return mevcut;
    }

    /** Bir chunk adımının yönü: dönem değişmediyse G farkının işareti (sütun hedefi G ile azalmaz), değiştiyse 0 (bilinmez). */
    public static int adimYonu(int eskiG, boolean eskiCozulme, int yeniG, boolean yeniCozulme) {
        return eskiCozulme == yeniCozulme ? Integer.signum(yeniG - eskiG) : 0;
    }

    /**
     * Eşik aralığı atlaması ({@link EsikKumesi}) bu adımda güvenli mi. Tam bir adımdan sonra her sütun o adımın yönündeki
     * fonksiyonun değerindedir; aynı yönde devam ederken aralıkta eşik yoksa sütun değişmez. Yön dönünce (donma →
     * çözülme) sütun öbür fonksiyondadır ve aralıkta eşik olmasa da değişebilir; yarım adımda ve dönem değişiminde de
     * sütunlar farklı noktadadır. Hiç işlenmemiş chunk (G 0, donma dönemi) doğal durumdadır, donarken atlanabilir.
     *
     * @param sonYon chunk'ın son tamamlanan adımının yönü ({@link #adimYonu}; 0 bilinmiyor)
     */
    public static boolean atlamaSerbest(int sonYon, int eskiG, boolean eskiCozulme, int yeniG, boolean yeniCozulme,
            boolean yarim) {
        int yon = adimYonu(eskiG, eskiCozulme, yeniG, yeniCozulme);
        if (yarim || yon == 0) {
            return false;
        }
        return sonYon == yon || sonYon == 0 && yon > 0 && eskiG == 0 && !eskiCozulme;
    }
}
