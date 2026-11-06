package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;

/**
 * N16 gökyüzü başarım ölçütleri. "Gece" vanilla gün saatinin gece aralığıdır ({@value #GECE_BASI}–{@value #GECE_SONU}),
 * güneşsiz dönemde de: gündüz yıldızlar görünse de "Yıldızlı Gece" saymaz. Minecraft sınıfı kullanmaz.
 */
public final class GokyuzuOlcutleri {
    public static final long GECE_BASI = 13_000L;
    public static final long GECE_SONU = 23_000L;

    private GokyuzuOlcutleri() {
    }

    public static boolean geceMi(long gunSaati) {
        long saat = Math.floorMod(gunSaati, GunesZinciri.GUN_TICK);
        return saat >= GECE_BASI && saat < GECE_SONU;
    }

    /** "Yıldızlı Gece": ay yok, gece ve gökyüzünü gören konum (10 saniyelik sayacı başarım işçisi tutar). */
    public static boolean yildizliGece(boolean ayVar, long gunSaati, boolean gokyuzunuGoruyor) {
        return !ayVar && gokyuzunuGoruyor && geceMi(gunSaati);
    }

    /** "Süpernovayı gökyüzünde gör": süpernova evresinde gökyüzünü gören konum. */
    public static boolean supernovaTanigi(GunesEvresi evre, boolean gokyuzunuGoruyor) {
        return evre == GunesEvresi.SUPERNOVA && gokyuzunuGoruyor;
    }
}
