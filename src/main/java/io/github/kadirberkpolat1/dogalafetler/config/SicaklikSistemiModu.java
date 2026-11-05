package io.github.kadirberkpolat1.dogalafetler.config;

/**
 * Kendi sıcaklık sistemimizin Cold Sweat gibi başka bir sıcaklık moduyla birlikte nasıl davranacağı (madde 2, N8).
 * Saf Java'dır.
 */
public enum SicaklikSistemiModu {
    /** Başka bir sıcaklık modu (Cold Sweat) yüklüyse kapalı, değilse açık. */
    OTOMATIK,
    /** Her zaman açık (çift hasar riski kullanıcının seçimidir). */
    ACIK,
    /** Her zaman kapalı. */
    KAPALI;

    /** Yalnız config seçimine göre sistemin açık olup olmadığı. */
    public boolean etkinMi(boolean baskaSicaklikModuYuklu) {
        return switch (this) {
            case OTOMATIK -> !baskaSicaklikModuYuklu;
            case ACIK -> true;
            case KAPALI -> false;
        };
    }

    /** N8: sistem ancak {@code sicaklikSistemi} gamerule'ü açıksa ve config izin veriyorsa çalışır. */
    public boolean calisirMi(boolean sicaklikKuraliAcik, boolean baskaSicaklikModuYuklu) {
        return sicaklikKuraliAcik && etkinMi(baskaSicaklikModuYuklu);
    }
}
