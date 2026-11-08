package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import io.github.kadirberkpolat1.dogalafetler.su.Cekilme;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;

/**
 * Görsel otomasyonun kare anları (AF3-15, saf): sabit saniyeler yerine istemciye senkronlanan afet verisinden (çekilme,
 * dalga ve kıyı noktası {@code kx/kz}). Çekilmenin tamamlanma oranı, cephenin kıyıya kalan yolunun açıklığa oranı ve
 * cephenin kıyıyı geçmesinden bu yana geçen tick: sunucu zamanlaması (çekilme süresi, dalga gecikmesi, açıklık) değişse de
 * kare aynı evrede alınır. Zaman ölçekli afet saatidir (tick).
 */
final class TsunamiKareAnlari {
    private TsunamiKareAnlari() {
    }

    /** Çekilmenin tamamlanma oranı: başlamadan 0, süresi dolunca 1. */
    static double cekilmeIlerlemesi(Cekilme c, double t) {
        if (t <= c.baslangic()) {
            return 0;
        }
        return c.sure() <= 0 ? 1 : Math.min(1, (t - c.baslangic()) / c.sure());
    }

    /** Kıyı noktasının dalganın başlangıç çizgisine uzaklığı (açıklık, blok); kıyı çizginin gerisindeyse 0 ya da eksi. */
    static double aciklik(Dalga w, double kx, double kz) {
        return (kx - w.x0()) * w.yonX() + (kz - w.z0()) * w.yonZ();
    }

    /** Cephenin kıyıya kalan yolunun açıklığa oranı: doğuşta (ve öncesinde) 1, kıyıda ve sonrasında 0. */
    static double kalanOran(Dalga w, double kx, double kz, double t) {
        double a = aciklik(w, kx, kz);
        if (a <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, 1 - (t - w.baslangic()) * w.hiz() / a));
    }

    /** Cephenin kıyıyı geçmesinden bu yana geçen tick; kıyıya varmadan eksi. */
    static double kiyidanSonra(Dalga w, double kx, double kz, double t) {
        return t - w.baslangic() - Math.max(0, aciklik(w, kx, kz)) / w.hiz();
    }
}
