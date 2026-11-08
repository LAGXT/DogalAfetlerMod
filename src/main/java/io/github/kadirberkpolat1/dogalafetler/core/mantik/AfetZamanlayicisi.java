package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;

/**
 * Afet zamanlayıcısı (madde 4): sabit takvim yoktur; her tür bağımsız bir Poisson süreci gibi davranır. Bir kontrol
 * aralığında ({@code adimTick}) en az bir afet çıkma olasılığı {@code 1 − e^(−Λ·adim/24000)}'dir; Λ uygun türlerin
 * oranlarının toplamıdır, tür λ_t/Λ olasılıkla seçilir. Oyuncu sayısı hesaba girmez (madde 3). Aynı anda iki afet,
 * genel bekleme, afet başına bekleme ve başlangıç beklemesi seçimi engeller; geçici çarpan yalnız oranı çarpar.
 * {@code afetSikligi} bütün takvimi ölçekler (11.4 "200 = iki katı"): oranı çarpar, beklemeleri böler ({@link #beklemeOlcekle}).
 */
public final class AfetZamanlayicisi {
    /** Bir Minecraft günü (tick). */
    public static final long GUN_TICK = 24_000L;

    private static final AfetTuru[] TURLER = AfetTuru.values();

    /**
     * Bir kontrolün girdisi.
     *
     * @param afetAktif    süren (ya da ön-uyarıdaki) bir afet var mı
     * @param saat         ölçekli afet saati
     * @param adimTick     önceki kontrolden beri geçen ölçekli tick
     * @param uygunTurler  kayıtlı, gamerule'ü açık ve tek seferlik kuralları geçen türler
     * @param carpan       türün geçici olasılık çarpanı (yoksa 1)
     * @param siklik       {@code afetSikligi} gamerule çarpanı (100 → 1.0)
     * @param zorla        olay olasılığı 1 sayılır, bekleme ve sıklık yok sayılır; uygunluk filtresi korunur (test ve komut)
     * @param garanti      ilk afet garantisi (AS-12): olay olasılığı 1 sayılır, beklemeler ve uygunluk korunur
     */
    public record Girdi(boolean afetAktif, long saat, long adimTick, BeklemeDurumu bekleme, Set<AfetTuru> uygunTurler,
            ToDoubleFunction<AfetTuru> carpan, double siklik, boolean zorla, boolean garanti) {
        public Girdi {
            Objects.requireNonNull(bekleme, "bekleme");
            Objects.requireNonNull(uygunTurler, "uygunTurler");
            Objects.requireNonNull(carpan, "carpan");
        }

        public Girdi(boolean afetAktif, long saat, long adimTick, BeklemeDurumu bekleme, Set<AfetTuru> uygunTurler,
                ToDoubleFunction<AfetTuru> carpan, double siklik, boolean zorla) {
            this(afetAktif, saat, adimTick, bekleme, uygunTurler, carpan, siklik, zorla, false);
        }

        /** Aynı girdi, başka uygun tür kümesi ve zorlama ile. */
        public Girdi ile(Set<AfetTuru> turler, boolean zorlama) {
            return new Girdi(afetAktif, saat, adimTick, bekleme, turler, carpan, siklik, zorlama, garanti);
        }
    }

    private AfetZamanlayicisi() {
    }

    /**
     * Bir bekleme süresinin {@code afetSikligi}'na göre etkin hâli: süre sıklığa bölünür (2 → yarı, 0,5 → iki kat). Sıklık 0'da
     * seçim zaten yoktur; süre olduğu gibi kalır.
     */
    public static long beklemeOlcekle(long tick, double siklik) {
        if (!(siklik > 0) || Double.isInfinite(siklik)) {
            return tick;
        }
        return Math.round(tick / siklik);
    }

    /** Türün etkin oranı (afet/gün). */
    public static double turOrani(ZamanlayiciAyarlari ayarlar, AfetTuru tur, double carpan, double siklik) {
        double toplam = ayarlar.toplamAgirlik();
        if (toplam <= 0) {
            return 0;
        }
        return ayarlar.gunlukAfetOrani() * Math.max(0, siklik) * carpan * ayarlar.agirlik(tur) / toplam;
    }

    /** {@code adimTick} boyunca en az bir olay olasılığı. */
    public static double olayOlasiligi(double gunlukToplamOran, long adimTick) {
        if (adimTick <= 0 || !(gunlukToplamOran > 0)) {
            return 0;
        }
        return 1.0 - Math.exp(-gunlukToplamOran * adimTick / GUN_TICK);
    }

    /** Bu kontrolde başlayacak afetin türü; afet yoksa boş. */
    public static Optional<AfetTuru> sec(ZamanlayiciAyarlari ayarlar, Girdi girdi, RandomGenerator rastgele) {
        if (girdi.afetAktif()) {
            return Optional.empty();
        }
        long saat = girdi.saat();
        if (!girdi.zorla() && (saat < beklemeOlcekle(ayarlar.baslangicBeklemeTick(), girdi.siklik())
                || girdi.bekleme().genelBeklemede(saat))) {
            return Optional.empty();
        }
        double[] oranlar = new double[TURLER.length];
        double toplam = 0;
        for (AfetTuru tur : TURLER) {
            if (!girdi.uygunTurler().contains(tur)) {
                continue;
            }
            double carpan = girdi.carpan().applyAsDouble(tur);
            if (!(carpan > 0) || Double.isInfinite(carpan)) {
                continue;
            }
            double oran;
            if (girdi.zorla()) {
                oran = ayarlar.agirlik(tur) * carpan;
            } else {
                if (girdi.bekleme().turBeklemede(tur, saat)) {
                    continue;
                }
                oran = turOrani(ayarlar, tur, carpan, girdi.siklik());
            }
            if (oran > 0) {
                oranlar[tur.ordinal()] = oran;
                toplam += oran;
            }
        }
        if (!(toplam > 0)) {
            return Optional.empty();
        }
        if (!girdi.zorla() && !girdi.garanti() && rastgele.nextDouble() >= olayOlasiligi(toplam, girdi.adimTick())) {
            return Optional.empty();
        }
        double secim = rastgele.nextDouble() * toplam;
        AfetTuru son = null;
        for (AfetTuru tur : TURLER) {
            double oran = oranlar[tur.ordinal()];
            if (oran <= 0) {
                continue;
            }
            son = tur;
            secim -= oran;
            if (secim < 0) {
                return Optional.of(tur);
            }
        }
        return Optional.ofNullable(son);
    }
}
