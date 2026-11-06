package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/**
 * Kasırga evreleri (tasarım notu §1-2). {@link #OLUSUM} siren aşamasıdır: huni yok, duvar bulutu alçalıp döner. AKTIF
 * aşamada doğuş 8 sn (toz girdabı → huni iner), olgun evre (seviyenin aktif süresi), ip evresi 10 sn (huni incelir ve
 * kıvrılır), dağılma 2 sn (solma). Süreler ölçekli afet tick'idir ({@code /dogalafet hiz} hızlandırır).
 */
public enum KasirgaEvresi {
    OLUSUM,
    DOGUS,
    OLGUN,
    IP,
    DAGILMA;

    public static final int DOGUS_TICK = 160;
    public static final int IP_TICK = 200;
    public static final int DAGILMA_TICK = 40;

    /** {@code ilerleme} evre içinde 0-1. */
    public record Durum(KasirgaEvresi evre, double ilerleme) {
    }

    /** AKTIF aşamanın toplam süresi (tick). */
    public static long toplamAktif(long olgunTick) {
        return DOGUS_TICK + Math.max(1, olgunTick) + IP_TICK + DAGILMA_TICK;
    }

    /** Evrenin AKTIF aşamadaki başlangıç tick'i ({@link #OLUSUM} için 0). */
    public static long baslangic(KasirgaEvresi evre, long olgunTick) {
        long olgun = Math.max(1, olgunTick);
        return switch (evre) {
            case OLUSUM, DOGUS -> 0;
            case OLGUN -> DOGUS_TICK;
            case IP -> DOGUS_TICK + olgun;
            case DAGILMA -> DOGUS_TICK + olgun + IP_TICK;
        };
    }

    /** AKTIF aşamada {@code gecen} tick sonraki evre; toplam süre dolduysa {@link #DAGILMA} ilerleme 1. */
    public static Durum aktifte(long gecen, long olgunTick) {
        long olgun = Math.max(1, olgunTick);
        long t = Math.max(0, gecen);
        if (t < DOGUS_TICK) {
            return new Durum(DOGUS, (double) t / DOGUS_TICK);
        }
        t -= DOGUS_TICK;
        if (t < olgun) {
            return new Durum(OLGUN, (double) t / olgun);
        }
        t -= olgun;
        if (t < IP_TICK) {
            return new Durum(IP, (double) t / IP_TICK);
        }
        t -= IP_TICK;
        return new Durum(DAGILMA, Math.min(1, (double) t / DAGILMA_TICK));
    }

    /** Sıra numarasından (ağ, NBT) güvenli çözüm. */
    public static KasirgaEvresi sirayla(int sira) {
        KasirgaEvresi[] hepsi = values();
        return hepsi[Math.max(0, Math.min(hepsi.length - 1, sira))];
    }

    /** Blok kaldırma bütçesinin bu evredeki payı: doğuşun ikinci yarısı ve ip evresi yarım, olgun tam. */
    public static double kaldirmaPayi(Durum d) {
        return switch (d.evre()) {
            case DOGUS -> d.ilerleme() > 0.5 ? 0.5 : 0;
            case OLGUN -> 1;
            case IP -> 0.5 * (1 - d.ilerleme());
            default -> 0;
        };
    }

    /** Hareket hızının bu evredeki payı (doğuşta ve ip evresinde yavaş). */
    public static double hizPayi(KasirgaEvresi evre) {
        return switch (evre) {
            case OLUSUM -> 0;
            case DOGUS -> 0.5;
            case IP -> 0.7;
            case DAGILMA -> 0.4;
            default -> 1;
        };
    }

    /** Çekim ve hasarın bu evredeki payı. */
    public static double guc(Durum d) {
        return switch (d.evre()) {
            case OLUSUM -> 0;
            case DOGUS -> 0.3 + 0.7 * d.ilerleme();
            case OLGUN -> 1;
            case IP -> 1 - 0.7 * d.ilerleme();
            case DAGILMA -> 0.3 * (1 - d.ilerleme());
        };
    }
}
