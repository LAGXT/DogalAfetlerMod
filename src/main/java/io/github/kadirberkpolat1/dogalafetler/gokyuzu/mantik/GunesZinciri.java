package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import java.util.Optional;

/**
 * Güneş zincirinin saf kuralları (bölüm D, madde 9): SÜPERNOVA (1–2 gün) → GÜNEŞSİZ (donma %100 olduktan gecikme kadar
 * sonra) → KIZIL CÜCE (ilk şafakta doğuş). Süreler ölçekli afet saatiyle ({@code zamanAdimi()}), şafak üst dünyanın
 * gün saatiyle (gün/gece döngüsü değişmez, madde 16) hesaplanır. Minecraft sınıfı kullanmaz.
 */
public final class GunesZinciri {
    public static final long GUN_TICK = 24_000L;
    /** Vanilla gün doğumu yelpazesinin başladığı saat; kızıl cüce bu pencerede doğar. */
    public static final long SAFAK_SAATI = 23_000L;
    public static final float DONMA_TAM = 100f;
    /** "Kutup Sığınağı" başarımının 3 günlük şartı (madde 12): gecikme bundan kısa olamaz (bölüm D). */
    public static final double EN_AZ_GECIKME_GUN = 3.0;
    private static final float DONMA_PAYI = 1e-3f;

    /** Sunucu config'inden gelen zincir ayarları. */
    public record Ayarlar(double supernovaSuresiGun, double kizilCuceGecikmeGun, long dogusSuresiTick,
            double kalintiGenislemeGun) {
        public static final Ayarlar VARSAYILAN = new Ayarlar(1.5, 7.0, 6_000L, 30.0);

        public Ayarlar {
            if (!(supernovaSuresiGun >= 1.0 && supernovaSuresiGun <= 2.0)) {
                throw new IllegalArgumentException("süpernova 1–2 gün olmalı: " + supernovaSuresiGun);
            }
            if (!(kizilCuceGecikmeGun >= EN_AZ_GECIKME_GUN)) {
                throw new IllegalArgumentException("kızıl cüce gecikmesi en az 3 gün olmalı: " + kizilCuceGecikmeGun);
            }
            if (dogusSuresiTick <= 0 || !(kalintiGenislemeGun > 0)) {
                throw new IllegalArgumentException("doğuş ve kalıntı süreleri pozitif olmalı");
            }
        }

        public long supernovaTick() {
            return Math.round(supernovaSuresiGun * GUN_TICK);
        }

        public long gecikmeTick() {
            return Math.round(kizilCuceGecikmeGun * GUN_TICK);
        }

        public long kalintiTick() {
            return Math.round(kalintiGenislemeGun * GUN_TICK);
        }
    }

    private GunesZinciri() {
    }

    /**
     * Evre ilerlemesinin dolma süresi (ölçekli tick): SÜPERNOVA sönümü, GÜNEŞSİZ kalıntı genişlemesi (evreyi bitirmez),
     * KIZIL CÜCE doğuşu. Süreci olmayan evrede -1.
     */
    public static long evreSuresi(GunesEvresi evre, Ayarlar ayar) {
        return switch (evre) {
            case SUPERNOVA -> ayar.supernovaTick();
            case GUNESSIZ -> ayar.kalintiTick();
            case KIZIL_CUCE -> ayar.dogusSuresiTick();
            default -> -1L;
        };
    }

    /** Zincirde bir sonraki evre; BÜYÜYEN'den SÜPERNOVA'ya geçiş güneş patlaması afetinindir. */
    public static Optional<GunesEvresi> sonraki(GunesEvresi evre) {
        return switch (evre) {
            case SUPERNOVA -> Optional.of(GunesEvresi.GUNESSIZ);
            case GUNESSIZ -> Optional.of(GunesEvresi.KIZIL_CUCE);
            default -> Optional.empty();
        };
    }

    /** Süreç başlamış ve tamamlanmamış mı: süpernova, güneşsiz dönem ya da süren kızıl cüce doğuşu. */
    public static boolean aktif(GunesEvresi evre, float ilerleme) {
        return evre == GunesEvresi.SUPERNOVA || evre == GunesEvresi.GUNESSIZ
                || evre == GunesEvresi.KIZIL_CUCE && ilerleme < 1f;
    }

    /** {@code adim} ölçekli tick sonra ilerleme (0–1); süresiz evrede değişmez. */
    public static float ilerlet(float ilerleme, int adim, long sure) {
        if (sure <= 0 || adim <= 0) {
            return ilerleme;
        }
        return (float) Math.min(1.0, ilerleme + (double) adim / sure);
    }

    /** Donma %100'deyken geçen ölçekli süre; donma %100'ün altına düşerse (ör. komutla) sıfırdan başlar. */
    public static long kapiIlerlet(long gecen, float donma, int adim) {
        return donma >= DONMA_TAM - DONMA_PAYI ? gecen + Math.max(0, adim) : 0L;
    }

    public static boolean kapiAcik(long gecen, Ayarlar ayar) {
        return gecen >= ayar.gecikmeTick();
    }

    /** Kızıl cücenin bu tick doğup doğmayacağı ve nedeni (log'a yazılır). */
    public enum DogusNedeni {
        /** Kapı kapalı ya da şafak bekleniyor. */
        BEKLE,
        /** Gün saati şafak penceresinde. */
        SAFAK,
        /** Uyku ya da zaman atlaması şafağı geçti. */
        SAFAK_GECILDI,
        /** {@code doDaylightCycle=false}: şafak hiç gelmez, o anki gün saatinde doğar (şef kararı, G19). */
        GUN_DONGUSU_KAPALI;

        public boolean dogar() {
            return this != BEKLE;
        }
    }

    /**
     * Kapı açıkken doğuş nedeni: gün döngüsü kapalıysa beklenmez, açıksa şafak penceresi ya da şafağı geçen atlama
     * ({@link #safakGecti}) gerekir. Gün saatine dokunulmaz (madde 16).
     */
    public static DogusNedeni dogusNedeni(boolean kapiAcik, long oncekiGunSaati, long gunSaati, boolean gunDongusu) {
        if (!kapiAcik) {
            return DogusNedeni.BEKLE;
        }
        if (!gunDongusu) {
            return DogusNedeni.GUN_DONGUSU_KAPALI;
        }
        if (safakMi(gunSaati)) {
            return DogusNedeni.SAFAK;
        }
        return safakGecti(oncekiGunSaati, gunSaati) ? DogusNedeni.SAFAK_GECILDI : DogusNedeni.BEKLE;
    }

    /** Gün saati şafak penceresinde mi ({@value #SAFAK_SAATI}–{@value #GUN_TICK}). */
    public static boolean safakMi(long gunSaati) {
        return Math.floorMod(gunSaati, GUN_TICK) >= SAFAK_SAATI;
    }

    /**
     * {@code onceki} ile {@code simdi} arasında bir şafak anı ({@value #SAFAK_SAATI}) geçildi mi: uyku geceyi atlayınca gün
     * saati 13000'den 24000'e sıçrar ve şafak penceresi hiç görülmez. Geri giden saat ({@code /time set}) ve önceki saatin
     * bilinmemesi ({@link Long#MIN_VALUE}) şafak sayılmaz.
     */
    public static boolean safakGecti(long onceki, long simdi) {
        if (onceki == Long.MIN_VALUE || simdi <= onceki) {
            return false;
        }
        return Math.floorDiv(simdi - SAFAK_SAATI, GUN_TICK) > Math.floorDiv(onceki - SAFAK_SAATI, GUN_TICK);
    }

    /** Bir sonraki şafağa kalan gün saati tick'i (şafak penceresindeyse 0). */
    public static long safagaKalan(long gunSaati) {
        long saat = Math.floorMod(gunSaati, GUN_TICK);
        return saat >= SAFAK_SAATI ? 0L : SAFAK_SAATI - saat;
    }
}
