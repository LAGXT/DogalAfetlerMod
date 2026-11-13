package io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik;

/**
 * Bir sinkhole seviyesinin parametreleri ve aşama zaman çizelgesi (madde 10 "çukurun çapı ve derinliği artar"; tasarım notu
 * sinkhole §2, §6). Alt aşamalar: {@link #CATLAMA} → {@link #SARKMA} → {@link #COKUS} (çekirdek) → {@link #GERI_CEKILME}
 * (halkalar sırayla) → {@link #DURULMA}. Süreler ölçekli tick'tir. Saf Java'dır.
 *
 * @param cap çukur çapı (blok)
 * @param derinlik merkez derinliği (yüzeyden, blok)
 * @param catlamaTick titreme ve çatlama süresi (kaçma fırsatı)
 * @param cokusTick çekirdek çöküşü + basamaklı geri çekilmenin toplam süresi
 * @param basamak basamak sayısı (halka sayısı; 1 = tek adımlı dik delik)
 * @param dusenBlokHasari çöken halkadaki canlılara bir kez verilen hasar (can puanı)
 */
public record SinkholeSeviyesi(int seviye, int cap, int derinlik, int catlamaTick, int cokusTick, int basamak,
        float dusenBlokHasari) {
    public static final int CATLAMA = 0;
    public static final int SARKMA = 1;
    public static final int COKUS = 2;
    public static final int GERI_CEKILME = 3;
    public static final int DURULMA = 4;
    public static final int SARKMA_TICK = 60;
    public static final int DURULMA_TICK = 280;

    private static final int[] CAP = {6, 10, 16, 28, 48};
    private static final int[] DERINLIK = {6, 10, 16, 28, 46};
    private static final int[] CATLAMA_SN = {4, 4, 5, 6, 8};
    private static final int[] COKUS_SN = {3, 4, 6, 9, 13};
    private static final int[] BASAMAK = {1, 1, 2, 3, 4};
    private static final float[] HASAR = {0, 0, 4, 7, 12};

    public static SinkholeSeviyesi varsayilan(int seviye) {
        int i = Math.max(1, Math.min(5, seviye)) - 1;
        return new SinkholeSeviyesi(i + 1, CAP[i], DERINLIK[i], CATLAMA_SN[i] * 20, COKUS_SN[i] * 20, BASAMAK[i], HASAR[i]);
    }

    public double yaricap() {
        return cap / 2.0;
    }

    /** Çekirdek çöküşünün süresi: tek halkada çöküşün tamamı, çok halkada yarısı. */
    public int cekirdekTick() {
        return basamak <= 1 ? cokusTick : cokusTick / 2;
    }

    /** Her dış halkanın köşelenme süresi. */
    public int halkaTick() {
        return basamak <= 1 ? 0 : (cokusTick - cekirdekTick()) / (basamak - 1);
    }

    /** Alt aşamanın süresi. */
    public int sure(int altAsama) {
        return switch (altAsama) {
            case CATLAMA -> catlamaTick;
            case SARKMA -> SARKMA_TICK;
            case COKUS -> cekirdekTick();
            case GERI_CEKILME -> basamak <= 1 ? 0 : cokusTick - cekirdekTick();
            default -> DURULMA_TICK;
        };
    }

    /** AKTIF aşamasının toplam süresi. */
    public int aktifTick() {
        int toplam = 0;
        for (int a = CATLAMA; a <= DURULMA; a++) {
            toplam += sure(a);
        }
        return toplam;
    }
}
