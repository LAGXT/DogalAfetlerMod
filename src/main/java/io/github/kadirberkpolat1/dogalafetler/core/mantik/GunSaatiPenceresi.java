package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Optional;

/**
 * Patlama anının gün saati penceresi (gökyüzü-2, kullanıcı isteği 26): doğal başlayan güneş patlaması AKTIF'e yalnız sabah
 * ({@link #SABAH}), ay çarpması yalnız gece ({@link #GECE}) geçer. Pencere üst dünyanın gün saatiyle ({@code getDayTime()})
 * sınanır, oyun günü sayacıyla değil: uyku ve {@code /time set} gün saatini değiştirir. Aralık {@code [bas, son)}, gün
 * saatinin 24000'e göre kalanı. Saf Java.
 */
public record GunSaatiPenceresi(long bas, long son) {
    public static final long GUN_TICK = 24_000L;
    /** Gün doğumu → öğle. */
    public static final GunSaatiPenceresi SABAH = new GunSaatiPenceresi(0L, 6_000L);
    /** Gece (vanilla 13000–23000, ayın gökte olduğu bölüm). */
    public static final GunSaatiPenceresi GECE = new GunSaatiPenceresi(13_000L, 23_000L);

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

    /**
     * Şimdiki gün saatinden sonra AKTIF'e {@code aktifeKalanGercekTick} (siren) varken patlamanın pencereye düşmesi için
     * kaç gerçek tick beklenmeli: 0 şimdi başlayabilir; pencere bugün kaçtıysa ertesi günün pencere başına.
     */
    public long bekleme(long gunSaati, long aktifeKalanGercekTick) {
        long varis = gunSaati + Math.max(0L, aktifeKalanGercekTick);
        return icinde(varis) ? 0L : Math.floorMod(bas - varis, GUN_TICK);
    }

    /**
     * ON_UYARI'nın sonunda SIREN'e geçiş beklemeli mi. Komutla başlayan afet ({@code elle}) ve gün döngüsü kapalı dünya
     * ({@code doDaylightCycle=false}: pencere hiç gelmeyebilir, kızıl cüce G19 ile aynı) beklemez.
     */
    public static boolean bekle(Optional<GunSaatiPenceresi> pencere, boolean elle, boolean gunDongusu, long gunSaati,
            long sirenGercekTick) {
        return pencere.isPresent() && !elle && gunDongusu && pencere.get().bekleme(gunSaati, sirenGercekTick) > 0;
    }
}
