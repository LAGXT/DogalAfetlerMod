package io.github.kadirberkpolat1.dogalafetler.donma;

/**
 * Spike ayarları. Faz 2'de SERVER config'e taşınır; önerilen anahtarlar ve gerekçeleri
 * {@code .vibe/kararlar/spike-donma.md}'de. Bazıları deney için {@code /dogalafet spike donma ...} ile değiştirilir.
 */
public final class DonmaAyarlari {
    /** Tick başına en çok blok değişimi (TPS kısmasından önce). */
    public static volatile int temelButce = 256;
    /** Bir chunk geçişinde global ilerlemenin (G) en çok ilerlediği miktar. */
    public static volatile int seviyeAdimi = 5;
    public static volatile double tpsEsigi = 18.0;
    public static volatile double tpsKritikEsigi = 15.0;
    /** İşçinin bir tick'te harcayabileceği en uzun süre; bütçeden bağımsız ikinci güvence. */
    public static volatile int tickMaksMs = 4;
    /** Bütçe birimi: bir blok değişimi {@code BLOK_MALIYETI}, bir sütun taraması 1 birim. */
    public static final int BLOK_MALIYETI = 8;
    public static volatile double kisOrani = KisMaskesi.VARSAYILAN_ORAN;
    public static volatile double kisDalgaBoyu = KisMaskesi.VARSAYILAN_DALGA_BOYU;
    public static volatile double kisBant = KisMaskesi.VARSAYILAN_BANT;
    /** Y geçişi ({@code kisYGecisAlt/Ust}) ve ılık ofseti. */
    public static volatile KisAlani.Ayar kisAyar = KisAlani.Ayar.VARSAYILAN;
    public static volatile DonmaEsikleri esikler = DonmaEsikleri.VARSAYILAN;

    private DonmaAyarlari() {
    }
}
