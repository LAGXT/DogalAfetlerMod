package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import java.util.Optional;

/**
 * Pulsar zincirinin saf kuralları (pulsar-2): PULSAR ({@code pulsarGun} oyun günü) → KARADELİK_YUTMA ({@value #YUTMA_SN}
 * sn, ölçekli afet saatiyle) → pulsar yolunda GÜNEŞSİZ ({@code pulsarSonrasiGunessizGun} oyun günü + ilk şafak) →
 * KIZIL CÜCE doğuşu → NORMAL (eski güneş). Gün süreleri K-C29 oyun tick'iyle ({@link #oyunTick}) sayılır: {@code /time
 * set} ve {@code /dogalafet hiz} onları değiştirmez, uyku ve {@code /dogalafet gun} ofseti ilerletir. Kural duraklatması
 * sayılmaz (K-C46): kural açılınca başlangıçlar {@link #duraklama} kadar ileri çekilir. Minecraft sınıfı kullanmaz.
 */
public final class PulsarZinciri {
    public static final long GUN_TICK = GunesZinciri.GUN_TICK;
    public static final long SANIYE_TICK = 20L;
    /** Kara delik yutmasının süresi (sn). */
    public static final float YUTMA_SN = 90f;
    /** Yutmanın kör edici parlamasının başladığı saniye (parlama 55–60 sn); sunucu sarsıntısı ve sesi bu anda bir kez. */
    public static final float YUTMA_PARLAMA_SN = 55f;
    /**
     * Doğal yutmanın başlayabileceği gün saati penceresi [{@value #YUTMA_PENCERESI_BAS}, {@value #YUTMA_PENCERESI_SON}): pulsar
     * ve güneş yolunda 40° ötedeki kara delik 90 sn boyunca ufkun üstünde kalır (gece başlayan yutma görünmezdi).
     */
    public static final long YUTMA_PENCERESI_BAS = 3_000L;
    public static final long YUTMA_PENCERESI_SON = 7_200L;
    /** Pencere gelmezse (başka mod gün saatini kilitledi) pulsar günleri dolduktan sonra en çok bu kadar oyun tick'i beklenir. */
    public static final long YUTMA_EN_COK_BEKLEME = GUN_TICK;

    /** {@code asama ileri pulsar}'ın bir adımı. */
    public enum AsamaAdimi {
        /** Süreç bu evrede ilerletilemez. */
        YOK(null),
        /** PULSAR'da ısınma tamamlanır (evre değişmez). */
        ISINMA(GunesEvresi.PULSAR),
        KARADELIK(GunesEvresi.KARADELIK_YUTMA),
        GUNESSIZ(GunesEvresi.GUNESSIZ),
        KIZIL_CUCE(GunesEvresi.KIZIL_CUCE),
        /** Doğuş tamamlanır, eski güneş döner. */
        ESKI_GUNES(GunesEvresi.NORMAL);

        private final GunesEvresi hedef;

        AsamaAdimi(GunesEvresi hedef) {
            this.hedef = hedef;
        }

        /** Adımdan sonraki evre ({@link #YOK} için null). */
        public GunesEvresi hedef() {
            return hedef;
        }
    }

    private PulsarZinciri() {
    }

    /** K-C29 oyun tick'i: oyun zamanı + uyku birikimi + gün ofseti × 24000 (oyun gününün tick karşılığı). */
    public static long oyunTick(long oyunZamani, long uykuBirikimi, long gunOfseti) {
        return oyunZamani + uykuBirikimi + gunOfseti * GUN_TICK;
    }

    public static long sureTick(double gun) {
        return Math.max(1L, Math.round(gun * GUN_TICK));
    }

    /**
     * K-C46: durma anından kuralın açıldığı ana geçen süre (oyun tick'i ya da ölçekli saat); geri alınan ofsette 0. Süre
     * başlangıcı bu kadar ileri çekilince kalan süre ve yutma saniyesi durduğu andaki değerine döner.
     */
    public static long duraklama(long durma, long simdi) {
        return Math.max(0L, simdi - durma);
    }

    /** Başlangıçtan beri geçen oyun tick'i; geri alınan ofsette 0. */
    public static long gecen(long baslangic, long simdi) {
        return Math.max(0L, simdi - baslangic);
    }

    /** {@code sureGun} oyun günü doldu mu. */
    public static boolean doldu(long baslangic, long simdi, double sureGun) {
        return gecen(baslangic, simdi) >= sureTick(sureGun);
    }

    /** Kalan oyun tick'i (0 alt sınır). */
    public static long kalanTick(long baslangic, long simdi, double sureGun) {
        return Math.max(0L, sureTick(sureGun) - gecen(baslangic, simdi));
    }

    public static double kalanGun(long baslangic, long simdi, double sureGun) {
        return (double) kalanTick(baslangic, simdi, sureGun) / GUN_TICK;
    }

    /** Geçen / toplam, 0–1. */
    public static float ilerleme(long baslangic, long simdi, double sureGun) {
        return (float) Math.min(1.0, (double) gecen(baslangic, simdi) / sureTick(sureGun));
    }

    /** Kara delik yutmasının saniyesi: ölçekli afet saatinin evre başlangıcından farkı (istemci aynı andan hesaplar). */
    public static float yutmaSaniyesi(long evreBaslangicSaati, long saat) {
        return Math.max(0L, saat - evreBaslangicSaati) / (float) SANIYE_TICK;
    }

    public static float yutmaIlerlemesi(float saniye) {
        return Math.max(0f, Math.min(1f, saniye / YUTMA_SN));
    }

    public static boolean yutmaParlamasiGeldi(float saniye) {
        return saniye >= YUTMA_PARLAMA_SN;
    }

    public static boolean yutmaBitti(float saniye) {
        return saniye >= YUTMA_SN;
    }

    public static float yutmaKalanSaniye(float saniye) {
        return Math.max(0f, YUTMA_SN - saniye);
    }

    public static boolean yutmaPenceresinde(long gunSaati) {
        long saat = Math.floorMod(gunSaati, GUN_TICK);
        return saat >= YUTMA_PENCERESI_BAS && saat < YUTMA_PENCERESI_SON;
    }

    /**
     * Pulsar günleri dolunca yutma bu tick başlar mı: gün döngüsü kapalıysa hemen, açıksa gündüz penceresinde ya da
     * {@link #YUTMA_EN_COK_BEKLEME} dolunca. Komutla ({@code asama ileri}) geçiş beklemez.
     */
    public static boolean yutmaBaslar(long baslangic, long simdi, double sureGun, long gunSaati, boolean gunDongusu) {
        if (!doldu(baslangic, simdi, sureGun)) {
            return false;
        }
        return !gunDongusu || yutmaPenceresinde(gunSaati)
                || gecen(baslangic, simdi) >= sureTick(sureGun) + YUTMA_EN_COK_BEKLEME;
    }

    /**
     * Pulsar süreci aktif mi: PULSAR, KARADELİK_YUTMA ya da pulsar yolunda ({@code donusYolu}) güneşsiz dönem ve süren
     * kızıl cüce doğuşu.
     */
    public static boolean aktif(GunesEvresi evre, float ilerleme, boolean donusYolu) {
        return switch (evre) {
            case PULSAR, KARADELIK_YUTMA -> true;
            case GUNESSIZ -> donusYolu;
            case KIZIL_CUCE -> donusYolu && ilerleme < 1f;
            default -> false;
        };
    }

    /** Zincirde bir sonraki evre (pulsar yolunda); ısınma adımı sayılmaz. */
    public static Optional<GunesEvresi> sonraki(GunesEvresi evre, boolean donusYolu) {
        return switch (evre) {
            case PULSAR -> Optional.of(GunesEvresi.KARADELIK_YUTMA);
            case KARADELIK_YUTMA -> Optional.of(GunesEvresi.GUNESSIZ);
            case GUNESSIZ -> donusYolu ? Optional.of(GunesEvresi.KIZIL_CUCE) : Optional.empty();
            case KIZIL_CUCE -> donusYolu ? Optional.of(GunesEvresi.NORMAL) : Optional.empty();
            default -> Optional.empty();
        };
    }

    /** {@code asama ileri pulsar}: PULSAR'da ısınma 1'in altındaysa önce ısınma, yoksa bir sonraki evre. */
    public static AsamaAdimi asamaAdimi(GunesEvresi evre, float ilerleme, boolean donusYolu) {
        if (!aktif(evre, ilerleme, donusYolu)) {
            return AsamaAdimi.YOK;
        }
        if (evre == GunesEvresi.PULSAR && ilerleme < 1f) {
            return AsamaAdimi.ISINMA;
        }
        return switch (sonraki(evre, donusYolu).orElse(GunesEvresi.NORMAL)) {
            case KARADELIK_YUTMA -> AsamaAdimi.KARADELIK;
            case GUNESSIZ -> AsamaAdimi.GUNESSIZ;
            case KIZIL_CUCE -> AsamaAdimi.KIZIL_CUCE;
            default -> AsamaAdimi.ESKI_GUNES;
        };
    }

    /**
     * O aşamadayken geçerli takma ad ({@code asama ileri karadelik|gunessiz|kizilcuce}); PULSAR'ın ve pulsar yolu dışının
     * takma adı yok (kimlik {@code pulsar} her zaman geçer).
     */
    public static Optional<String> takmaAd(GunesEvresi evre, float ilerleme, boolean donusYolu) {
        if (!aktif(evre, ilerleme, donusYolu)) {
            return Optional.empty();
        }
        return switch (evre) {
            case KARADELIK_YUTMA -> Optional.of("karadelik");
            case GUNESSIZ -> Optional.of("gunessiz");
            case KIZIL_CUCE -> Optional.of("kizilcuce");
            default -> Optional.empty();
        };
    }
}
