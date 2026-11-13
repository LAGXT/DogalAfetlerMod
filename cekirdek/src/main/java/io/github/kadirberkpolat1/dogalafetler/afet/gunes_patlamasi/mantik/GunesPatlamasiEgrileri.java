package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;

/**
 * Güneş patlamasının saf eğrileri (bölüm D, tasarım `gunes_patlamasi.md` §2): BÜYÜYEN evre ilerlemesi, meteor yoğunluğu,
 * kavrulma olasılığı ve başlama kapısı. Zamanlar ölçekli afet saati tick'idir.
 *
 * <p>Evre ilerlemesi gökyüzünde güneş ölçeğini ({@code 1 + 2,2·p}) ve ortam sıcaklığını ({@code artış·p}) sürer. Siren
 * boyunca {@code p = SIREN_SONU·τ²} (tasarım: güneş siren süresinde, 25 sn'de (K0-19 rev. K-E67) 1,0 → 1,6 kat, sona
 * doğru hızlanarak; τ siren süresine oran olduğu için eğri süreden bağımsızdır), AKTIF'in 5 gününde
 * {@code SIREN_SONU → 1} doğrusal (güneş 1,6 → 3,2 kat).
 */
public final class GunesPatlamasiEgrileri {
    public static final long GUN_TICK = 24_000L;
    /** Siren sonundaki ilerleme: gökyüzü ölçeği {@code 1 + 2,2·p} = 1,6. */
    public static final float SIREN_SONU = 0.6f / 2.2f;

    private GunesPatlamasiEgrileri() {
    }

    /** Siren boyunca evre ilerlemesi: 0 → {@link #SIREN_SONU}, karesel (sona doğru hızlanır). */
    public static float sirenIlerlemesi(long gecen, long sure) {
        float t = oran(gecen, sure);
        return SIREN_SONU * t * t;
    }

    /** AKTIF boyunca evre ilerlemesi: {@link #SIREN_SONU} → 1, doğrusal. */
    public static float aktifIlerlemesi(long gecen, long sure) {
        return SIREN_SONU + (1f - SIREN_SONU) * oran(gecen, sure);
    }

    /** Oyuncu başına dakikadaki meteor: {@code bas} → {@code son}, AKTIF boyunca doğrusal (1/40 sn → 1/12 sn). */
    public static double dakikaBasinaMeteor(long gecen, long sure, double bas, double son) {
        return bas + (son - bas) * oran(gecen, sure);
    }

    /**
     * Bir kavrulma denemesinin olasılığı: {@code baslangicGun}'den önce 0, sonra AKTIF'in sonuna kadar 0 → {@code tepe}
     * doğrusal.
     */
    public static double kavrulmaOlasiligi(long gecen, long sure, double baslangicGun, double tepe) {
        double bas = baslangicGun * GUN_TICK;
        if (gecen < bas || sure <= bas) {
            return 0;
        }
        return tepe * Math.min(1.0, (gecen - bas) / (sure - bas));
    }

    /** AKTIF süresi (ölçekli tick). */
    public static long aktifSure(double gun) {
        return Math.max(1L, Math.round(gun * GUN_TICK));
    }

    /** AKTIF'in kaçıncı günü (0'dan); alt aşama. */
    public static int gun(long gecen) {
        return (int) Math.max(0L, gecen / GUN_TICK);
    }

    /** AKTIF'teki gün sayısı (son gün kısa olabilir). */
    public static int gunSayisi(long sure) {
        return (int) Math.max(1L, (sure + GUN_TICK - 1) / GUN_TICK);
    }

    /**
     * Güneş patlaması bu dönemde başlayabilir mi: güneş NORMAL ya da kızıl cüce doğuşu tamamlanmış. Süpernova, güneşsiz dönem
     * ve süren doğuş zincirin elindedir; orada başlayan afet evreyi BÜYÜYEN yapıp zinciri bozardı.
     */
    public static boolean baslayabilir(GunesEvresi evre, float ilerleme) {
        return evre == GunesEvresi.NORMAL || evre == GunesEvresi.KIZIL_CUCE && ilerleme >= 1f;
    }

    private static float oran(long gecen, long sure) {
        if (sure <= 0) {
            return 1f;
        }
        return (float) Math.max(0.0, Math.min(1.0, (double) gecen / sure));
    }
}
