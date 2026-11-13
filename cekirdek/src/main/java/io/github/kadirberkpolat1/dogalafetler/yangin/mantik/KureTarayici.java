package io.github.kadirberkpolat1.dogalafetler.yangin.mantik;

/**
 * Bir kürenin konumlarını (merkeze göre ofset) bütçeyle, kağıt üstünde sırayla ve kaldığı yerden verir: büyük
 * {@code alanTutustur} birkaç tick'e yayılır. Her konum bir kez ziyaret edilir; alttan üste, satır satır.
 */
public final class KureTarayici {
    /** Bir konumu işler ve harcadığı blok okuma sayısını döndürür (en az 1 sayılır). */
    @FunctionalInterface
    public interface Ziyaretci {
        int ziyaret(int dx, int dy, int dz);
    }

    private final int yaricap;
    private final int kenar;
    private final long toplam;
    private long sira;

    public KureTarayici(int yaricap) {
        this.yaricap = Math.max(0, yaricap);
        this.kenar = 2 * this.yaricap + 1;
        this.toplam = (long) kenar * kenar * kenar;
    }

    public boolean bitti() {
        return sira >= toplam;
    }

    /**
     * Bütçe bitene kadar küre içindeki konumları ziyaret eder ve harcanan okumayı döndürür. Başlanan konum yarım kalmaz:
     * son konum bütçeyi aşabilir. Küre dışındaki konumlar okuma harcamadan atlanır.
     */
    public int ilerle(int butce, Ziyaretci ziyaretci) {
        int harcanan = 0;
        int r2 = yaricap * yaricap;
        while (sira < toplam) {
            int dx = (int) (sira % kenar) - yaricap;
            int dz = (int) (sira / kenar % kenar) - yaricap;
            int dy = (int) (sira / ((long) kenar * kenar)) - yaricap;
            if (dx * dx + dy * dy + dz * dz > r2) {
                sira++;
                continue;
            }
            if (harcanan >= butce) {
                break;
            }
            sira++;
            harcanan += Math.max(1, ziyaretci.ziyaret(dx, dy, dz));
        }
        return harcanan;
    }
}
