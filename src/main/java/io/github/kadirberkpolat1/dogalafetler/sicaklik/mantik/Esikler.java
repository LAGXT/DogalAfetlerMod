package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Vücut sıcaklığı eşikleri ve kademeli etkiler (madde 7c). Değerler vücut birimidir (−100…+100). Soğukta önce yavaşlama
 * ({@code soguk}), sonra buz kenarı ({@code cokSoguk}), sonra donma hasarı ({@code donmaHasari}); sıcakta önce titreme
 * ({@code sicak}, {@code cokSicak}), sonra aşırı ısınma hasarı ({@code yanmaHasari}). Hasar aralığı (tick) eşikten
 * ölçek ucuna doğrusal kısalır. Dört durum (S3-04): {@code soguk} üşüme (36 °C), {@code cokSoguk} donma (35 °C),
 * {@code sicak} bunalma (38 °C), {@code cokSicak} yanma (39,5 °C); bunalma ve yanma ek açlık tüketir.
 *
 * @param notrBant          HUD'daki nötr bantın yarı genişliği (yalnız görünüm)
 * @param enCokYavaslama    ölçek ucundaki hareket hızı kesintisi (0–1)
 * @param bunalmaAclik      bunalmada ek açlık yorgunluğu (exhaustion/sn; vanilla 4 = 1 açlık puanı)
 * @param yanmaAclik        yanmada ek açlık yorgunluğu (exhaustion/sn)
 */
public record Esikler(double notrBant, double soguk, double cokSoguk, double donmaHasari, double sicak,
        double cokSicak, double yanmaHasari, double enCokYavaslama, int donmaAralikBasi, int donmaAralikSonu,
        int yanmaAralikBasi, int yanmaAralikSonu, double bunalmaAclik, double yanmaAclik) {
    public static final Esikler VARSAYILAN = new Esikler(10, -20, -40, -70, 20, 50, 70, 0.5, 120, 40, 120, 40, 0.04,
            0.08);

    public Esikler {
        if (!(donmaHasari <= cokSoguk && cokSoguk <= soguk && soguk < 0 && 0 < sicak && sicak <= cokSicak
                && cokSicak <= yanmaHasari)) {
            throw new IllegalArgumentException("eşikler sıralı olmalı: " + donmaHasari + " ≤ " + cokSoguk + " ≤ "
                    + soguk + " < 0 < " + sicak + " ≤ " + cokSicak + " ≤ " + yanmaHasari);
        }
    }

    public Kademe kademe(double vucut) {
        if (vucut <= donmaHasari) {
            return Kademe.DONUYOR;
        }
        if (vucut <= cokSoguk) {
            return Kademe.COK_SOGUK;
        }
        if (vucut <= soguk) {
            return Kademe.SOGUK;
        }
        if (vucut >= yanmaHasari) {
            return Kademe.YANIYOR;
        }
        if (vucut >= cokSicak) {
            return Kademe.COK_SICAK;
        }
        if (vucut >= sicak) {
            return Kademe.SICAK;
        }
        return Kademe.NOTR;
    }

    /** Kademenin ek açlık yorgunluğu (exhaustion/sn): bunalmada {@code bunalmaAclik}, yanmada {@code yanmaAclik}. */
    public double aclik(double vucut) {
        return switch (kademe(vucut).durum()) {
            case BUNALMA -> bunalmaAclik;
            case YANMA -> yanmaAclik;
            default -> 0;
        };
    }

    /** Hareket hızı kesintisi (0–{@code enCokYavaslama}): soğuk eşiğinden ölçek ucuna doğrusal. */
    public double yavaslama(double vucut) {
        if (vucut > soguk) {
            return 0;
        }
        double f = (soguk - vucut) / (soguk + IsiAlisverisi.OLCEK);
        return enCokYavaslama * Math.min(1.0, f);
    }

    /** Hasar aralığı (tick); hasar kademesinde değilse 0. */
    public int hasarAraligi(double vucut) {
        if (vucut <= donmaHasari) {
            return aralik(donmaHasari - vucut, donmaHasari + IsiAlisverisi.OLCEK, donmaAralikBasi, donmaAralikSonu);
        }
        if (vucut >= yanmaHasari) {
            return aralik(vucut - yanmaHasari, IsiAlisverisi.OLCEK - yanmaHasari, yanmaAralikBasi, yanmaAralikSonu);
        }
        return 0;
    }

    private static int aralik(double derinlik, double genislik, int bas, int son) {
        double f = genislik <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, derinlik / genislik));
        return Math.max(1, (int) Math.round(bas + (son - bas) * f));
    }
}
