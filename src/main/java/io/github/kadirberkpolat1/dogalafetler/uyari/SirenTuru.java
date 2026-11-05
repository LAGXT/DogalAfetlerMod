package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Locale;
import java.util.Optional;

/**
 * Siren türleri ve zarf parametreleri (madde 5, docs/tasarim/ses.md §4). Saf Java'dır; Minecraft sınıflarına dokunmaz.
 *
 * <p>İstemciye giden siren paketi afeti değil siren türünü taşır: sesi çalmak için afet türü gerekmez ve paket
 * dinleyen bir istemci afetin türünü öğrenemez (türü önceden görmek Hava Radarı'nın avantajıdır, madde 4 ve 13).
 */
public enum SirenTuru {
    /** Birçok afetin ortak sireni: 280↔420 Hz wail; açılışta perde 140 Hz'den kalkar (spin-up), 2 sn fade-out. */
    STANDART("standart", "siren_standart", 24, 0.5f, 40, 0f),
    /** Güneş patlaması sireni: alçak ve yavaş; 3 sn'de belirir, süre boyunca +3 dB yükselir, 4 sn fade-out. */
    GUNES("gunes", "siren_gunes", 60, 1f, 80, 3f);

    public static final int SANIYE = 20;
    /** Madde 5: güneş patlaması dışındaki afetlerde siren 10 saniye. */
    public static final int STANDART_SURE_TICK = 10 * SANIYE;
    /** Güneş patlaması (tasarım notu) ve ay çarpması (K0-19) sireni 60 saniye. */
    public static final int UZUN_SURE_TICK = 60 * SANIYE;
    /** Paketle gelen süre bu aralığa kırpılır; bozuk paket sonsuz siren çalmasın. */
    public static final int EN_UZUN_TICK = 600 * SANIYE;

    private final String kimlik;
    private final String sesAdi;
    private final int acilisTick;
    private final float acilisPerdesi;
    private final int sondurmeTick;
    private final float artisDesibel;

    SirenTuru(String kimlik, String sesAdi, int acilisTick, float acilisPerdesi, int sondurmeTick, float artisDesibel) {
        this.kimlik = kimlik;
        this.sesAdi = sesAdi;
        this.acilisTick = acilisTick;
        this.acilisPerdesi = acilisPerdesi;
        this.sondurmeTick = sondurmeTick;
        this.artisDesibel = artisDesibel;
    }

    /** Komutlarda kullanılan ad. */
    public String kimlik() {
        return kimlik;
    }

    /** {@code sounds.json}'daki ses olayı ve {@code sounds/<ad>.ogg} dosyasının adı. */
    public String sesAdi() {
        return sesAdi;
    }

    /** Baştaki yükseliş (düzey ve perde) süresi. */
    public int acilisTick() {
        return acilisTick;
    }

    /** Açılışın başındaki perde çarpanı; OpenAL 0,5'in altını kırptığı için en az 0,5. */
    public float acilisPerdesi() {
        return acilisPerdesi;
    }

    /** Üstel fade-out süresi; sonunda düzey -60 dB olur ve ses durur. */
    public int sondurmeTick() {
        return sondurmeTick;
    }

    /** Çalma süresi boyunca düzeyin yavaşça yükseldiği miktar ("yaklaşıyor" hissi). */
    public float artisDesibel() {
        return artisDesibel;
    }

    public static SirenTuru afetIcin(AfetTuru afet) {
        return afet == AfetTuru.GUNES_PATLAMASI ? GUNES : STANDART;
    }

    public static int sureTick(AfetTuru afet) {
        return switch (afet) {
            case GUNES_PATLAMASI, AY_CARPMASI -> UZUN_SURE_TICK;
            default -> STANDART_SURE_TICK;
        };
    }

    /** Bağsız süreyi [1, {@link #EN_UZUN_TICK}] aralığına kırpar. */
    public static int sureyiKirp(int sureTick) {
        return Math.max(1, Math.min(EN_UZUN_TICK, sureTick));
    }

    public static Optional<SirenTuru> kimlikIle(String girdi) {
        if (girdi == null) {
            return Optional.empty();
        }
        String aranan = girdi.trim().toLowerCase(Locale.ROOT);
        for (SirenTuru tur : values()) {
            if (tur.kimlik.equals(aranan)) {
                return Optional.of(tur);
            }
        }
        return Optional.empty();
    }
}
