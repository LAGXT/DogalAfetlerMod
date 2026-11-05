package io.github.kadirberkpolat1.dogalafetler.su;

/**
 * Mağara suyunun yaşamı: dolma (yüzey {@code minY}'den {@code ustY}'ye sabit hızla), bekleme, buharlaşma (yüzey sabit
 * hızla iner) ve bitiş. Zaman oyun tick'i; deterministik olduğu için istemci aynı seviyeyi kendisi hesaplar.
 */
public final class MagaraZamani {
    public enum Asama {
        BASLAMADI, DOLUYOR, BEKLIYOR, BUHARLASIYOR, BITTI
    }

    private final double baslangic;
    private final double minY;
    private final double ustY;
    private final double dolmaHizi;
    private final double bekleme;
    private final double buharlasmaHizi;
    private final double dolmaBitis;
    private final double beklemeBitis;
    private final double bitis;

    /** Hızlar blok/tick, bekleme tick. */
    public MagaraZamani(double baslangic, double minY, double ustY, double dolmaHizi, double bekleme,
            double buharlasmaHizi) {
        this.baslangic = baslangic;
        this.minY = minY;
        this.ustY = ustY;
        this.dolmaHizi = dolmaHizi;
        this.bekleme = bekleme;
        this.buharlasmaHizi = buharlasmaHizi;
        this.dolmaBitis = baslangic + (ustY - minY) / dolmaHizi;
        this.beklemeBitis = dolmaBitis + bekleme;
        this.bitis = beklemeBitis + (ustY - minY) / buharlasmaHizi;
    }

    public Asama asama(double t) {
        if (t < baslangic) {
            return Asama.BASLAMADI;
        }
        if (t < dolmaBitis) {
            return Asama.DOLUYOR;
        }
        if (t < beklemeBitis) {
            return Asama.BEKLIYOR;
        }
        if (t < bitis) {
            return Asama.BUHARLASIYOR;
        }
        return Asama.BITTI;
    }

    /** Su yüzeyi; su yoksa NaN. */
    public double seviye(double t) {
        return switch (asama(t)) {
            case DOLUYOR -> minY + (t - baslangic) * dolmaHizi;
            case BEKLIYOR -> ustY;
            case BUHARLASIYOR -> ustY - (t - beklemeBitis) * buharlasmaHizi;
            default -> Double.NaN;
        };
    }

    public double baslangic() {
        return baslangic;
    }

    public double minY() {
        return minY;
    }

    public double ustY() {
        return ustY;
    }

    public double dolmaHizi() {
        return dolmaHizi;
    }

    public double bekleme() {
        return bekleme;
    }

    public double buharlasmaHizi() {
        return buharlasmaHizi;
    }
}
