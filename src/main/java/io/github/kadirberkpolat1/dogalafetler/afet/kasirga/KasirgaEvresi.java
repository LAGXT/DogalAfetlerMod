package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

/**
 * Kasırga yaşam döngüsü (tasarım notu §1-2): doğuş 8 sn (toz girdabı → huni iner), olgun evre (seviyenin aktif
 * süresi), ip evresi 10 sn (huni incelir ve kıvrılır), dağılma 2 sn (solma).
 */
public enum KasirgaEvresi {
    DOGUS,
    OLGUN,
    IP,
    DAGILMA;

    public static final int DOGUS_TICK = 160;
    public static final int IP_TICK = 200;
    public static final int DAGILMA_TICK = 40;
    /** Aktif süre sınırsız: olgun evre {@code dur} komutuna kadar sürer (spike ölçümleri için). */
    public static final int SINIRSIZ = Integer.MAX_VALUE;

    public record Durum(KasirgaEvresi evre, double ilerleme, boolean bitti) {
    }

    public static Durum hesapla(long yasTick, int aktifTick) {
        if (yasTick < DOGUS_TICK) {
            return new Durum(DOGUS, (double) yasTick / DOGUS_TICK, false);
        }
        long t = yasTick - DOGUS_TICK;
        if (aktifTick == SINIRSIZ || t < aktifTick) {
            return new Durum(OLGUN, aktifTick == SINIRSIZ ? 0 : (double) t / aktifTick, false);
        }
        t -= aktifTick;
        if (t < IP_TICK) {
            return new Durum(IP, (double) t / IP_TICK, false);
        }
        t -= IP_TICK;
        if (t < DAGILMA_TICK) {
            return new Durum(DAGILMA, (double) t / DAGILMA_TICK, false);
        }
        return new Durum(DAGILMA, 1, true);
    }

    public static KasirgaEvresi sirayla(int sira) {
        KasirgaEvresi[] hepsi = values();
        return hepsi[Math.max(0, Math.min(hepsi.length - 1, sira))];
    }
}
