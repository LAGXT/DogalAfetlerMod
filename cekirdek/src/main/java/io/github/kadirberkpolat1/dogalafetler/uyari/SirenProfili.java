package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;

/**
 * Siren türünün ses dosyası ve zarf parametreleri (madde 5, docs/tasarim/ses.md §4, spike-siren S-02). Saf Java'dır.
 * Dosya döngülüdür; süre, açılış ve fade-out kodla yönetilir ({@link SirenZarfi}). Ay çarpması standart profili, güneş
 * patlaması kendi profili 25 sn çalar (K0-19 rev. K-E67); açılış ve söndürme süreden bağımsız birkaç saniyedir.
 */
public enum SirenProfili {
    /** 280↔420 Hz wail; açılışta perde 140 Hz'den kalkar (spin-up), varsayılan fade-out 2 sn. */
    STANDART("siren_standart", 200, 24, 0.5f, 40, 0f, false),
    /** Alçak ve yavaş; 3 sn'de belirir, süre boyunca +3 dB yükselir, varsayılan fade-out 4 sn. */
    GUNES("siren_gunes", 400, 60, 1f, 80, 3f, true);

    private final String sesAdi;
    private final int dosyaTick;
    private final int acilisTick;
    private final float acilisPerdesi;
    private final int sondurmeTick;
    private final float artisDesibel;
    private final boolean akisla;

    SirenProfili(String sesAdi, int dosyaTick, int acilisTick, float acilisPerdesi, int sondurmeTick, float artisDesibel,
            boolean akisla) {
        this.sesAdi = sesAdi;
        this.dosyaTick = dosyaTick;
        this.acilisTick = acilisTick;
        this.acilisPerdesi = acilisPerdesi;
        this.sondurmeTick = sondurmeTick;
        this.artisDesibel = artisDesibel;
        this.akisla = akisla;
    }

    public static SirenProfili icin(SirenTuru tur) {
        return tur == SirenTuru.GUNES ? GUNES : STANDART;
    }

    /** {@code sounds.json}'daki ses olayı ve {@code sounds/<ad>.ogg} dosyasının adı. */
    public String sesAdi() {
        return sesAdi;
    }

    /** Ses dosyasının bir döngü turu (tick). */
    public int dosyaTick() {
        return dosyaTick;
    }

    /** Baştaki yükseliş (düzey ve perde) süresi. */
    public int acilisTick() {
        return acilisTick;
    }

    /** Açılışın başındaki perde çarpanı; OpenAL 0,5'in altını kırptığı için en az 0,5. */
    public float acilisPerdesi() {
        return acilisPerdesi;
    }

    /** Türün varsayılan fade-out süresi (sunucu config'i durdurma paketinde başka süre gönderebilir). */
    public int sondurmeTick() {
        return sondurmeTick;
    }

    /** Çalma süresi boyunca düzeyin yavaşça yükseldiği miktar ("yaklaşıyor" hissi). */
    public float artisDesibel() {
        return artisDesibel;
    }

    /** 10 sn'den uzun döngüler akışla çalınır (ses.md §5). */
    public boolean akisla() {
        return akisla;
    }

    /** {@code sureTick} boyunca dosyanın kaç tur çalınacağı (yukarı yuvarlanır). */
    public int donguSayisi(int sureTick) {
        return Math.max(1, (sureTick + dosyaTick - 1) / dosyaTick);
    }
}
