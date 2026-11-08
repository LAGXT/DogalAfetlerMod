package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import java.util.Optional;

/**
 * Patlama anının gün saati penceresi (gökyüzü-2, kullanıcı isteği 26): doğal güneş patlaması yalnız sabah ({@link #SABAH}),
 * ay çarpması yalnız gece ({@link #GECE}) patlar. K-E67'den beri pencere seçimde uygulanır: tür yalnız seçimden görünmeye
 * (AKTIF) kadar geçecek en uzun süre ({@link #gorunmePayi}) de pencereye sığıyorsa seçilebilir, ön-uyarıda bekleme yoktur
 * (seçimden görünmeye ~30 sn). Pencerenin seçilebilir payı küçük olduğu için oranı o pay kadar artırılır
 * ({@link #dogalSecimCarpani}): günlük ortalama sıklık değişmez. Pencere üst dünyanın gün saatiyle ({@code getDayTime()})
 * sınanır, oyun günü sayacıyla değil: uyku ve {@code /time set} gün saatini değiştirir. Aralık {@code [bas, son)}, gün
 * saatinin 24000'e göre kalanı. Saf Java.
 */
public record GunSaatiPenceresi(long bas, long son) {
    public static final long GUN_TICK = 24_000L;
    /** Gün doğumu → öğle. */
    public static final GunSaatiPenceresi SABAH = new GunSaatiPenceresi(0L, 6_000L);
    /** Gece (vanilla 13000–23000, ayın gökte olduğu bölüm). */
    public static final GunSaatiPenceresi GECE = new GunSaatiPenceresi(13_000L, 23_000L);
    /** Görünme payına eklenen emniyet (tick): varsayılanlarla pay 800 tick, pencerenin son ~40 sn'sinde seçim yok. */
    public static final long EMNIYET_TICK = 140L;

    public GunSaatiPenceresi {
        if (bas < 0 || son > GUN_TICK || bas >= son) {
            throw new IllegalArgumentException("pencere [" + bas + ", " + son + ") 0–24000 içinde ve boş olmamalı");
        }
    }

    /** Türün doğal patlama penceresi; pencere kuralı olmayan türde boş. */
    public static Optional<GunSaatiPenceresi> dogalPatlama(AfetTuru tur) {
        return switch (tur) {
            case GUNES_PATLAMASI -> Optional.of(SABAH);
            case AY_CARPMASI -> Optional.of(GECE);
            default -> Optional.empty();
        };
    }

    public boolean icinde(long gunSaati) {
        long t = Math.floorMod(gunSaati, GUN_TICK);
        return t >= bas && t < son;
    }

    /** Şimdi seçilen afet {@code gorunmePayi} gerçek tick sonra hâlâ pencerede görünür mü (seçim anı da pencerede). */
    public boolean secilebilir(long gunSaati, long gorunmePayi) {
        long pay = Math.max(0L, gorunmePayi);
        return pay < son - bas && icinde(gunSaati) && icinde(gunSaati + pay);
    }

    /** Günün seçilebilir payı (0–1): {@code (son − bas − pay) / 24000}. */
    public double secimPayi(long gorunmePayi) {
        return Math.max(0L, son - bas - Math.max(0L, gorunmePayi)) / (double) GUN_TICK;
    }

    /**
     * Seçimden AKTIF'e en uzun süre + {@link #EMNIYET_TICK} (ölçekli tick): erken bildirim + kısaltılmış ön-uyarı penceresinin
     * en çoğu ({@link OnUyariPenceresi#enCok}) + siren.
     */
    public static long gorunmePayi(AfetTuru tur, long erkenBildirim, long onUyariEnAz, long onUyariEnCok) {
        int siren = SirenTuru.sureTick(tur);
        return Math.max(0L, erkenBildirim)
                + OnUyariPenceresi.enCok(onUyariEnAz, onUyariEnCok, siren - SirenTuru.STANDART_SURE_TICK) + siren
                + EMNIYET_TICK;
    }

    /**
     * Doğal seçimde türün oran çarpanı: pencere kuralı yoksa ya da gün döngüsü kapalıysa ({@code doDaylightCycle=false}:
     * pencere hiç gelmeyebilir) 1; seçilebilirken {@code 1 / secimPayi} (günlük ortalama sıklık korunur); değilse 0 (seçilmez).
     * {@code gorunmePayiGercek}: {@link #gorunmePayi}'nın gerçek tick karşılığı ({@code /dogalafet hiz}).
     */
    public static double dogalSecimCarpani(AfetTuru tur, long gunSaati, long gorunmePayiGercek, boolean gunDongusu) {
        Optional<GunSaatiPenceresi> pencere = dogalPatlama(tur);
        if (pencere.isEmpty() || !gunDongusu) {
            return 1.0;
        }
        GunSaatiPenceresi p = pencere.get();
        return p.secilebilir(gunSaati, gorunmePayiGercek) ? 1.0 / p.secimPayi(gorunmePayiGercek) : 0.0;
    }
}
