package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Punchy birinci şahıs sıcaklık entegrasyonunun saf, tahsissiz (zero-allocation) karar ve dalga mantığı.
 * <ul>
 * <li>Dört anormal sıcaklık durumu ({@link SicaklikDurumu#BUNALMA}, {@link SicaklikDurumu#YANMA},
 *     {@link SicaklikDurumu#USUME}, {@link SicaklikDurumu#DONMA}) için klip/an (moment) eşlemesi,</li>
 * <li>Punchy kol kaplamaları (yanık/kırağı yoğunluğu),</li>
 * <li>Sürekli ama çatışmayı bozmayan mikro-titreme ve sıcak soluma kol salınımı,</li>
 * <li>Yalnız oyuncu boştayken tetiklenen durum geçişi ve periyodik tepki zamanlayıcısı.</li>
 * </ul>
 */
public final class PunchySicaklikMantigi {
    /** Durum değişiminde art arda spam önleme alt sınırı (5 sn = 100 tick). */
    public static final long GECIS_BEKLEME_TICK = 100L;
    /** Ağır kademelerde (YANMA / DONMA) periyodik tepki aralığı (28 sn = 560 tick). */
    public static final long AGIR_TEKRAR_TICK = 560L;
    /** Orta kademelerde (BUNALMA / USUME) periyodik tepki aralığı (38 sn = 760 tick). */
    public static final long ORTA_TEKRAR_TICK = 760L;

    private PunchySicaklikMantigi() {
    }

    /** İlgili sıcaklık durumunun birinci şahıs tepki klip adı; normalde {@code null}. */
    public static String klipAdi(SicaklikDurumu durum) {
        if (durum == null) {
            return null;
        }
        return switch (durum) {
            case NORMAL -> null;
            case BUNALMA -> "vz_sicaklik_bunalma";
            case YANMA -> "vz_sicaklik_yanma";
            case USUME -> "vz_sicaklik_usume";
            case DONMA -> "vz_sicaklik_donma";
        };
    }

    /** İlgili sıcaklık durumunun Punchy {@code moment} kimliği; normalde {@code null}. */
    public static String momentAdi(SicaklikDurumu durum) {
        if (durum == null) {
            return null;
        }
        return switch (durum) {
            case NORMAL -> null;
            case BUNALMA -> "dogalafetler:sicaklik_bunalma";
            case YANMA -> "dogalafetler:sicaklik_yanma";
            case USUME -> "dogalafetler:sicaklik_usume";
            case DONMA -> "dogalafetler:sicaklik_donma";
        };
    }

    /** Punchy birinci şahıs kol yanık/kızarıklık katmanı yoğunluğu (0..1). */
    public static float yanikYogunlugu(SicaklikDurumu durum) {
        if (durum == SicaklikDurumu.YANMA) {
            return 0.90f;
        }
        if (durum == SicaklikDurumu.BUNALMA) {
            return 0.35f;
        }
        return 0f;
    }

    /** Punchy birinci şahıs kol kırağı/buz katmanı yoğunluğu (0..1). */
    public static float kiragiYogunlugu(SicaklikDurumu durum) {
        if (durum == SicaklikDurumu.DONMA) {
            return 0.95f;
        }
        if (durum == SicaklikDurumu.USUME) {
            return 0.45f;
        }
        return 0f;
    }

    /** Kare başına tahsissiz kol eğim (pitch, derece) katkısı: soğukta titreme, sıcakta ağır nefes çöküşü. */
    public static float kolEgimDerece(SicaklikDurumu durum, double saniye, boolean solKol) {
        if (durum == null || durum == SicaklikDurumu.NORMAL) {
            return 0f;
        }
        double faz = solKol ? 1.15 : 0.0;
        return switch (durum) {
            case USUME -> (float) (0.55 * Math.sin(2.0 * Math.PI * 10.5 * saniye + faz));
            case DONMA -> (float) (1.15 * Math.sin(2.0 * Math.PI * 14.5 * saniye + faz));
            case BUNALMA -> (float) (0.75 * Math.sin(2.0 * Math.PI * 0.42 * saniye));
            case YANMA -> (float) (1.25 * Math.sin(2.0 * Math.PI * 0.68 * saniye)
                    + 0.25 * Math.sin(2.0 * Math.PI * 7.5 * saniye + faz));
            case NORMAL -> 0f;
        };
    }

    /** Kare başına tahsissiz kol yalpa (roll, derece) katkısı. */
    public static float kolYalpaDerece(SicaklikDurumu durum, double saniye, boolean solKol) {
        if (durum == null || durum == SicaklikDurumu.NORMAL) {
            return 0f;
        }
        float isaret = solKol ? -1f : 1f;
        return switch (durum) {
            case USUME -> isaret * (float) (0.45 * Math.cos(2.0 * Math.PI * 11.5 * saniye));
            case DONMA -> isaret * (float) (0.95 * Math.cos(2.0 * Math.PI * 15.5 * saniye));
            case BUNALMA -> isaret * (float) (0.30 * Math.cos(2.0 * Math.PI * 0.42 * saniye));
            case YANMA -> isaret * (float) (0.55 * Math.cos(2.0 * Math.PI * 0.68 * saniye));
            case NORMAL -> 0f;
        };
    }

    /**
     * Anormal sıcaklık tepki klibi bu tick'te oynatılmalı mı?
     * Oyuncu meşgulse (saldırı, eşya kullanımı, kazma, açık menü veya devam eden klip) asla araya girmez;
     * boşa çıktığı ilk tick'te bekleyen geçişi ya da periyodik tekrarı tetikler.
     */
    public static boolean tepkiTetiklenmeli(
            SicaklikDurumu sonOynatilanDurum,
            SicaklikDurumu guncelDurum,
            long simdikiTick,
            long sonOynatmaTick,
            boolean oyuncuMesgul) {
        if (oyuncuMesgul || guncelDurum == null || guncelDurum == SicaklikDurumu.NORMAL) {
            return false;
        }
        long gecen = sonOynatmaTick < 0 ? Long.MAX_VALUE : Math.max(0L, simdikiTick - sonOynatmaTick);
        if (guncelDurum != sonOynatilanDurum) {
            return gecen >= GECIS_BEKLEME_TICK;
        }
        long periyot = (guncelDurum == SicaklikDurumu.YANMA || guncelDurum == SicaklikDurumu.DONMA)
                ? AGIR_TEKRAR_TICK
                : ORTA_TEKRAR_TICK;
        return gecen >= periyot;
    }
}
