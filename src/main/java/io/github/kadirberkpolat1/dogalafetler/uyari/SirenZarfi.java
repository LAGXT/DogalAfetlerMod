package io.github.kadirberkpolat1.dogalafetler.uyari;

import java.util.Objects;

/**
 * Bir siren çalımının düzey ve perde zarfı (saf mantık; istemcideki ses örneği her tick {@link #ilerle()} çağırır).
 *
 * <ul>
 *   <li>Açılış: düzey kosinüs eğrisiyle 0'dan 1'e, perde {@link SirenTuru#acilisPerdesi()}'nden 1'e çıkar (mekanik
 *       sirenin motoru hızlanırken perdenin kalkması; ses dosyası döngülü olduğu için spin-up kodla yapılır).</li>
 *   <li>Çalma: düzey süre boyunca {@link SirenTuru#artisDesibel()} kadar yavaşça yükselir (güneş sireni).</li>
 *   <li>Söndürme: süre dolunca ya da {@link #sondur()} çağrılınca düzey o anki değerinden üstel olarak -60 dB'e iner,
 *       perde %20 düşer (wind-down); söndürme bitince {@link #bitti()} doğru olur.</li>
 * </ul>
 */
public final class SirenZarfi {
    /** Yeni bir siren eskisini bu kadar kısa bir fade-out ile keser (tıkırtı olmasın diye anında değil). */
    public static final int KESME_TICK = 10;

    private static final double SONDURME_SONU = 0.001;
    private static final double KAPANIS_PERDE_DUSUSU = 0.2;
    private static final float EN_DUSUK_PERDE = 0.5f;

    private final SirenTuru tur;
    private final int sureTick;
    private int tick;
    private int sondurmeBaslangici = -1;
    private int sondurmeSuresi;
    private float sondurmeOncesiDuzey;
    private float sondurmeOncesiPerde;

    public SirenZarfi(SirenTuru tur, int sureTick) {
        this.tur = Objects.requireNonNull(tur, "tur");
        if (sureTick <= 0) {
            throw new IllegalArgumentException("Siren süresi pozitif olmalı: " + sureTick);
        }
        this.sureTick = sureTick;
    }

    public SirenTuru tur() {
        return tur;
    }

    public int sureTick() {
        return sureTick;
    }

    public int gecenTick() {
        return tick;
    }

    /** Bir tick ilerler; süre dolunca türün fade-out'unu kendiliğinden başlatır. */
    public void ilerle() {
        if (bitti()) {
            return;
        }
        tick++;
        if (!sonduruluyor() && tick >= sureTick) {
            sondur();
        }
    }

    /** Türün fade-out süresiyle söndürmeye başlar. */
    public void sondur() {
        sondur(tur.sondurmeTick());
    }

    /**
     * {@code sure} tick'lik üstel söndürme başlatır. Zaten sönüyorsa yalnız daha erken bitecekse geçerlidir; düzey
     * o anki değerden devam eder, sıçramaz.
     */
    public void sondur(int sure) {
        if (sure <= 0) {
            throw new IllegalArgumentException("Söndürme süresi pozitif olmalı: " + sure);
        }
        if (sonduruluyor() && tick + sure >= sondurmeBaslangici + sondurmeSuresi) {
            return;
        }
        sondurmeOncesiDuzey = duzey();
        sondurmeOncesiPerde = perde();
        sondurmeBaslangici = tick;
        sondurmeSuresi = sure;
    }

    public boolean sonduruluyor() {
        return sondurmeBaslangici >= 0;
    }

    public boolean bitti() {
        return sonduruluyor() && tick - sondurmeBaslangici >= sondurmeSuresi;
    }

    /** 0–1 arası düzey çarpanı (oyuncunun siren ses ayarı bunun üstüne uygulanır). */
    public float duzey() {
        if (sonduruluyor()) {
            return (float) (sondurmeOncesiDuzey * Math.pow(SONDURME_SONU, sondurmeIlerlemesi()));
        }
        return (float) (yumusak(acilisIlerlemesi()) * artis());
    }

    /** Perde çarpanı; 0,5–1 arası. */
    public float perde() {
        if (sonduruluyor()) {
            double perde = sondurmeOncesiPerde * (1.0 - KAPANIS_PERDE_DUSUSU * sondurmeIlerlemesi());
            return (float) Math.max(EN_DUSUK_PERDE, perde);
        }
        float baslangic = tur.acilisPerdesi();
        return (float) (baslangic + (1.0 - baslangic) * yumusak(acilisIlerlemesi()));
    }

    private double acilisIlerlemesi() {
        return Math.min(1.0, tick / (double) tur.acilisTick());
    }

    private double sondurmeIlerlemesi() {
        return Math.min(1.0, (tick - sondurmeBaslangici) / (double) sondurmeSuresi);
    }

    private double artis() {
        if (tur.artisDesibel() == 0f) {
            return 1.0;
        }
        double ilerleme = Math.min(tick, sureTick) / (double) sureTick;
        return Math.pow(10.0, tur.artisDesibel() * (ilerleme - 1.0) / 20.0);
    }

    private static double yumusak(double u) {
        return (1.0 - Math.cos(Math.PI * u)) / 2.0;
    }
}
