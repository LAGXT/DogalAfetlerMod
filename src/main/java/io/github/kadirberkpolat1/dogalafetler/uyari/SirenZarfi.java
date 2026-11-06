package io.github.kadirberkpolat1.dogalafetler.uyari;

import java.util.Objects;

/**
 * Bir siren çalımının düzey ve perde zarfı (saf mantık; istemcideki ses örneği her tick {@link #ilerle()} çağırır).
 *
 * <ul>
 *   <li>Açılış: düzey kosinüs eğrisiyle 0'dan 1'e, perde {@link SirenProfili#acilisPerdesi()}'nden 1'e çıkar (mekanik
 *       sirenin motoru hızlanırken perdenin kalkması; dosya döngülü olduğu için spin-up kodla yapılır). Sonradan giren
 *       oyuncuda da açılış kısalmaz.</li>
 *   <li>Çalma: düzey toplam süre boyunca {@link SirenProfili#artisDesibel()} kadar yükselir; sonradan girenin zarfı
 *       geçen sürenin yerinden başlar ({@code toplam - kalan}).</li>
 *   <li>Söndürme: sunucunun durdurma paketiyle ({@link #sondur(int)}) düzey o anki değerinden üstel olarak -60 dB'e iner,
 *       perde %20 düşer (wind-down). Sürenin yetkilisi sunucudur; paket gelmezse kalan süre +
 *       {@value #GUVENLIK_PAYI_TICK} tick'te türün kendi fade-out'u başlar (güvenlik tavanı).</li>
 * </ul>
 */
public final class SirenZarfi {
    /** Yeni bir siren eskisini bu kadar kısa bir fade-out ile keser (tıkırtı olmasın diye anında değil). */
    public static final int KESME_TICK = 10;
    /** Durdurma paketi gelmezse kalan süreden sonra bu kadar daha çalar (S-06 B3: süre + 3 sn). */
    public static final int GUVENLIK_PAYI_TICK = 60;
    /** Paketle gelen sürenin üst sınırı (10 dk); bozuk paket sonsuz siren çalmasın. */
    public static final int EN_UZUN_TICK = 12_000;

    private static final double SONDURME_SONU = 0.001;
    private static final double KAPANIS_PERDE_DUSUSU = 0.2;
    private static final float EN_DUSUK_PERDE = 0.5f;

    private final SirenProfili profil;
    private final int toplamTick;
    private final int kalanBaslangic;
    private int tick;
    private int sondurmeBaslangici = -1;
    private int sondurmeSuresi;
    private boolean tavanaUlasti;
    private float sondurmeOncesiDuzey;
    private float sondurmeOncesiPerde;

    /**
     * @param toplamTick sirenin tam süresi (gerçek tick)
     * @param kalanTick bu istemcide çalınacak kalan süre; geç gelen oyuncuda toplamdan kısadır
     */
    public SirenZarfi(SirenProfili profil, int toplamTick, int kalanTick) {
        this.profil = Objects.requireNonNull(profil, "profil");
        this.toplamTick = toplamiKirp(toplamTick);
        this.kalanBaslangic = kalaniKirp(kalanTick, this.toplamTick);
    }

    /** Paketle gelen toplam süreyi [1, {@value #EN_UZUN_TICK}] aralığına kırpar. */
    public static int toplamiKirp(int toplamTick) {
        return Math.max(1, Math.min(EN_UZUN_TICK, toplamTick));
    }

    /** Kalan süreyi [1, toplam] aralığına kırpar (kalan &lt; toplam korunur). */
    public static int kalaniKirp(int kalanTick, int toplamTick) {
        return Math.max(1, Math.min(toplamTick, kalanTick));
    }

    public SirenProfili profil() {
        return profil;
    }

    public int toplamTick() {
        return toplamTick;
    }

    /** Çalma süresinden kalan (0'ın altına inmez; süre dolduktan sonra güvenlik payı içinde 0). */
    public int kalanTick() {
        return Math.max(0, kalanBaslangic - tick);
    }

    public int gecenTick() {
        return tick;
    }

    /** Bir tick ilerler; durdurma gelmeden güvenlik tavanı dolarsa türün fade-out'unu başlatır. */
    public void ilerle() {
        if (bitti()) {
            return;
        }
        tick++;
        if (!sonduruluyor() && tick >= kalanBaslangic + GUVENLIK_PAYI_TICK) {
            sondur(profil.sondurmeTick());
            tavanaUlasti = true;
        }
    }

    /**
     * {@code sure} tick'lik üstel söndürme başlatır. Zaten sönüyorsa yalnız daha erken bitecekse geçerlidir; düzey o anki
     * değerden devam eder, sıçramaz. Uygulandıysa {@code true}, yok sayıldıysa {@code false}.
     */
    public boolean sondur(int sure) {
        if (sure <= 0) {
            throw new IllegalArgumentException("Söndürme süresi pozitif olmalı: " + sure);
        }
        if (sonduruluyor() && tick + sure >= sondurmeBaslangici + sondurmeSuresi) {
            return false;
        }
        sondurmeOncesiDuzey = duzey();
        sondurmeOncesiPerde = perde();
        sondurmeBaslangici = tick;
        sondurmeSuresi = sure;
        return true;
    }

    public boolean sonduruluyor() {
        return sondurmeBaslangici >= 0;
    }

    /** Söndürme durdurma paketiyle değil güvenlik tavanıyla başladı. */
    public boolean tavanaUlasti() {
        return tavanaUlasti;
    }

    public boolean bitti() {
        return sonduruluyor() && tick - sondurmeBaslangici >= sondurmeSuresi;
    }

    /** 0–1 arası düzey çarpanı (oyuncunun siren ses ayarı bunun üstüne uygulanır, {@link SesDuzeyi#siren}). */
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
        float baslangic = profil.acilisPerdesi();
        return (float) (baslangic + (1.0 - baslangic) * yumusak(acilisIlerlemesi()));
    }

    private double acilisIlerlemesi() {
        return Math.min(1.0, tick / (double) profil.acilisTick());
    }

    private double sondurmeIlerlemesi() {
        return Math.min(1.0, (tick - sondurmeBaslangici) / (double) sondurmeSuresi);
    }

    private double artis() {
        if (profil.artisDesibel() == 0f) {
            return 1.0;
        }
        long gecenToplam = (long) toplamTick - kalanBaslangic + tick;
        double ilerleme = Math.min(gecenToplam, toplamTick) / (double) toplamTick;
        return Math.pow(10.0, profil.artisDesibel() * (ilerleme - 1.0) / 20.0);
    }

    private static double yumusak(double u) {
        return (1.0 - Math.cos(Math.PI * u)) / 2.0;
    }
}
