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
 * oranlarının toplamıdır, tür λ_t/Λ olasılıkla seçilir. Oyuncu sayısı hesaba girmez (madde 3). Dolu afet yuvası
 * (eşzamanlı doğal afet sınırı, {@link EszamanliAfetler}), genel bekleme, afet başına bekleme ve başlangıç beklemesi seçimi
 * engeller; geçici çarpan yalnız oranı çarpar. {@code afetSikligi} bütün takvimi ölçekler (11.4 "200 = iki katı"): oranı
 * çarpar, beklemeleri böler ({@link #beklemeOlcekle}). Oyun gününün takvim çarpanı ({@link ZamanlayiciAyarlari#gunCarpani},
 * K-E67) bölgesel afetlerin oranını aynı biçimde çarpar (beklemeler {@link BeklemeDurumu#afetBitti}'te). Dönem olayları
 * (güneş patlaması, ay çarpması) yalnız hiçbir afet sürmezken seçilebildiği için (bölgesel afetler sıklaştıkça ve eşzamanlı
 * sınır arttıkça boş an kalmaz) seçilme fırsatları azalır; bunu dengelemek için oranları gün çarpanının {@value #DONEM_GUN_USSU}
 * kuvvetiyle (karekök) artar (ayar sürümü 4: eski üs 0,25'ti, kullanıcı "neredeyse hiç çıkmıyor" dedi; ilk güneş patlaması
 * ortancası kapıdan (40. gün) sonra ~5-8 gün, kuyruğu 400 günü aşmaz).
 */
public final class AfetZamanlayicisi {
    /** Bir Minecraft günü (tick). */
    public static final long GUN_TICK = 24_000L;

    /** Dönem olaylarının oranına uygulanan gün çarpanı üssü (ayar sürümü 4: 0,25 → 0,5). */
    public static final double DONEM_GUN_USSU = 0.5;

    private static final AfetTuru[] TURLER = AfetTuru.values();

    /**
     * Bir kontrolün girdisi.
     *
     * @param yuvaDolu     doğal afet için boş yuva yok ({@link EszamanliAfetler#yuvaDolu})
     * @param saat         ölçekli afet saati
     * @param adimTick     önceki kontrolden beri geçen ölçekli tick
     * @param uygunTurler  kayıtlı, gamerule'ü açık ve tek seferlik kuralları geçen türler
     * @param carpan       türün geçici olasılık çarpanı (yoksa 1)
     * @param siklik       {@code afetSikligi} gamerule çarpanı (100 → 1.0)
     * @param zorla        olay olasılığı 1 sayılır, bekleme ve sıklık yok sayılır; uygunluk filtresi korunur (test ve komut)
     * @param garanti      ilk afet garantisi (AS-12): olay olasılığı 1 sayılır, beklemeler ve uygunluk korunur
     * @param gunCarpani   oyun gününün takvim çarpanı ({@link ZamanlayiciAyarlari#gunCarpani}; {@link #turGunCarpani})
     */
    public record Girdi(boolean yuvaDolu, long saat, long adimTick, BeklemeDurumu bekleme, Set<AfetTuru> uygunTurler,
            ToDoubleFunction<AfetTuru> carpan, double siklik, boolean zorla, boolean garanti, double gunCarpani) {
        public Girdi {
            Objects.requireNonNull(bekleme, "bekleme");
            Objects.requireNonNull(uygunTurler, "uygunTurler");
            Objects.requireNonNull(carpan, "carpan");
        }

        public Girdi(boolean yuvaDolu, long saat, long adimTick, BeklemeDurumu bekleme, Set<AfetTuru> uygunTurler,
                ToDoubleFunction<AfetTuru> carpan, double siklik, boolean zorla, boolean garanti) {
            this(yuvaDolu, saat, adimTick, bekleme, uygunTurler, carpan, siklik, zorla, garanti, 1.0);
        }

        public Girdi(boolean yuvaDolu, long saat, long adimTick, BeklemeDurumu bekleme, Set<AfetTuru> uygunTurler,
                ToDoubleFunction<AfetTuru> carpan, double siklik, boolean zorla) {
            this(yuvaDolu, saat, adimTick, bekleme, uygunTurler, carpan, siklik, zorla, false);
        }

        /** Aynı girdi, başka uygun tür kümesi ve zorlama ile. */
        public Girdi ile(Set<AfetTuru> turler, boolean zorlama) {
            return new Girdi(yuvaDolu, saat, adimTick, bekleme, turler, carpan, siklik, zorlama, garanti, gunCarpani);
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

    /** Türün etkin oranı (afet/gün), gün çarpanı 1. */
    public static double turOrani(ZamanlayiciAyarlari ayarlar, AfetTuru tur, double carpan, double siklik) {
        return turOrani(ayarlar, tur, carpan, siklik, 1.0);
    }

    /** Türün etkin oranı (afet/gün); {@code gunCarpani} türe göre uygulanır ({@link #turGunCarpani}). */
    public static double turOrani(ZamanlayiciAyarlari ayarlar, AfetTuru tur, double carpan, double siklik,
            double gunCarpani) {
        double toplam = ayarlar.toplamAgirlik();
        if (toplam <= 0) {
            return 0;
        }
        return ayarlar.gunlukAfetOrani() * Math.max(0, siklik) * turGunCarpani(tur, gunCarpani) * carpan
                * ayarlar.agirlik(tur) / toplam;
    }

    /**
     * Gün çarpanının türün oranına etkisi: bölgesel afette kendisi, dönem olayında (sunucu geneli) {@value #DONEM_GUN_USSU}
     * kuvveti (karekök).
     */
    public static double turGunCarpani(AfetTuru tur, double gunCarpani) {
        if (!(gunCarpani > 0) || Double.isInfinite(gunCarpani)) {
            return 1.0;
        }
        return tur.sunucuGeneli() ? Math.pow(gunCarpani, DONEM_GUN_USSU) : gunCarpani;
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
        if (girdi.yuvaDolu()) {
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
                oran = turOrani(ayarlar, tur, carpan, girdi.siklik(), girdi.gunCarpani());
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
