package io.github.kadirberkpolat1.dogalafetler.core;

import net.minecraft.nbt.CompoundTag;

/**
 * Pulsar zincirinin dönemdeki kalıcı durumu (pulsar-2), {@code DunyaDonemi.ekVeri("pulsar")} üstünde; anahtarlar yalnız
 * burada. Pulsar yolu evre değil bayraktır: kara delik yutmasından sonraki {@link GunesEvresi#GUNESSIZ} (buzul kıyamet) ve
 * {@link GunesEvresi#KIZIL_CUCE} doğuşu süpernova yolundakilerle aynı evrelerdir, {@link #donusYolu} ile ayrılır (doğuş
 * bitince eski güneş döner). {@link #gunesDondu} NORMAL'e bu yoldan dönüldüğünü ve donmanın 0'a çözüldüğünü söyler. İki
 * bayrak ve durma anının varlığı {@code DonemPaketi.bayraklar} ile istemciye gider: değişimleri acil senkron ister,
 * evreyle birlikte yazılırsa aynı pakette gider. Süreler K-C29 oyun tick'iyle ({@code oyun zamanı + uyku birikimi + gün
 * ofseti × 24000}) tutulur. Süreç durdurulunca durma anı yazılır, kural açılınca süreler o kadar ileri çekilir
 * (K-C46). {@link #zincirTukendi} kalıcıdır: yalnız {@code sifirla} siler (madde 4 tek seferlik). Yazanı
 * {@code gokyuzu.PulsarSureci}, {@code GunesZinciriSureci} ve sıfırlamadır; diğerleri yalnız okur.
 */
public final class PulsarYolu {
    public static final String ALT_SISTEM = "pulsar";
    /** Döngü kimliği: PULSAR'ın {@code evreBaslangicSaati} (gökyüzü-2'den beri aynı anahtar; eski kayıtlar okunur). */
    public static final String DONGU = "dongu";
    /** PULSAR'ın başladığı K-C29 oyun tick'i (15 günlük süre buradan). */
    public static final String BASLANGIC = "baslangic";
    /** Pulsar yolundaki güneşsiz dönemin başladığı K-C29 oyun tick'i. */
    public static final String GUNESSIZ_BASLANGIC = "gunessizBaslangic";
    /** Kara delik yutmasının 55. sn sarsıntısı ve sesi bu döngüde çaldı. */
    public static final String YUTMA_PATLAMASI = "yutmaPatlamasi";
    public static final String DONUS_YOLU = "donusYolu";
    public static final String GUNES_DONDU = "gunesDondu";
    /** Eski güneş pulsar yolundan döndü: güneş patlaması yalnız {@code tekrarlanabilir} açıkken yeniden seçilir. */
    public static final String ZINCIR_TUKENDI = "zincirTukendi";
    /** Süreç durdurulduğu andaki K-C29 oyun tick'i ve ölçekli afet saati (K-C46). */
    public static final String DURMA_OYUN_TICK = "durmaOyunTick";
    public static final String DURMA_SAATI = "durmaSaati";

    private PulsarYolu() {
    }

    /** Güneşsiz dönem ya da kızıl cüce doğuşu pulsar yolunda: doğuş bitince eski güneş döner. */
    public static boolean donusYolu(DunyaDonemi donem) {
        return veri(donem).getBoolean(DONUS_YOLU);
    }

    /** NORMAL'e pulsar yolundan dönüldü; donma 0'a çözülünce silinir. */
    public static boolean gunesDondu(DunyaDonemi donem) {
        return veri(donem).getBoolean(GUNES_DONDU);
    }

    /** PULSAR'ın döngü kimliği (= PULSAR evre başlangıç saati); yangın ve meteor kaynağı. Hiç pulsar olmadıysa 0. */
    public static long dongu(DunyaDonemi donem) {
        return veri(donem).getLong(DONGU);
    }

    public static boolean donguVar(DunyaDonemi donem) {
        return veri(donem).contains(DONGU);
    }

    public static boolean baslangicVar(DunyaDonemi donem) {
        return veri(donem).contains(BASLANGIC);
    }

    public static long baslangic(DunyaDonemi donem) {
        return veri(donem).getLong(BASLANGIC);
    }

    public static boolean gunessizBaslangicVar(DunyaDonemi donem) {
        return veri(donem).contains(GUNESSIZ_BASLANGIC);
    }

    public static long gunessizBaslangic(DunyaDonemi donem) {
        return veri(donem).getLong(GUNESSIZ_BASLANGIC);
    }

    public static boolean yutmaPatlamasiOldu(DunyaDonemi donem) {
        return veri(donem).getBoolean(YUTMA_PATLAMASI);
    }

    /** Eski güneş bu dünyada pulsar yolundan döndü (kalıcı; {@code sifirla} siler). */
    public static boolean zincirTukendi(DunyaDonemi donem) {
        return veri(donem).getBoolean(ZINCIR_TUKENDI);
    }

    public static boolean durmaVar(DunyaDonemi donem) {
        return veri(donem).contains(DURMA_OYUN_TICK);
    }

    public static long durmaOyunTick(DunyaDonemi donem) {
        return veri(donem).getLong(DURMA_OYUN_TICK);
    }

    public static long durmaSaati(DunyaDonemi donem) {
        return veri(donem).getLong(DURMA_SAATI);
    }

    /** Yeni pulsar döngüsü: döngü kimliği ve oyun tick'i başlangıcı birlikte yazılır, yutma bayrağı silinir. */
    public static void yeniDongu(DunyaDonemi donem, long dongu, long oyunTick) {
        CompoundTag veri = veri(donem);
        boolean durma = veri.contains(DURMA_OYUN_TICK);
        veri.putLong(DONGU, dongu);
        veri.putLong(BASLANGIC, oyunTick);
        veri.remove(YUTMA_PATLAMASI);
        veri.remove(DURMA_OYUN_TICK);
        veri.remove(DURMA_SAATI);
        if (durma) {
            istemciyeBildir(donem);
        } else {
            donem.ekVeriDegisti(ALT_SISTEM);
        }
    }

    /** Eski kayıtta (gökyüzü-2) yalnız döngü kimliği vardır: başlangıç ilk tick'te yazılır. */
    public static void baslangicAyarla(DunyaDonemi donem, long oyunTick) {
        veri(donem).putLong(BASLANGIC, oyunTick);
        donem.ekVeriDegisti(ALT_SISTEM);
    }

    public static void gunessizBaslangicAyarla(DunyaDonemi donem, long oyunTick) {
        veri(donem).putLong(GUNESSIZ_BASLANGIC, oyunTick);
        donem.ekVeriDegisti(ALT_SISTEM);
    }

    public static void yutmaPatlamasiAyarla(DunyaDonemi donem, boolean oldu) {
        if (bayrakYaz(donem, YUTMA_PATLAMASI, oldu)) {
            donem.ekVeriDegisti(ALT_SISTEM);
        }
    }

    public static void donusYoluAyarla(DunyaDonemi donem, boolean yol) {
        if (bayrakYaz(donem, DONUS_YOLU, yol)) {
            istemciyeBildir(donem);
        }
    }

    public static void gunesDonduAyarla(DunyaDonemi donem, boolean dondu) {
        if (bayrakYaz(donem, GUNES_DONDU, dondu)) {
            istemciyeBildir(donem);
        }
    }

    public static void zincirTukendiAyarla(DunyaDonemi donem, boolean tukendi) {
        if (bayrakYaz(donem, ZINCIR_TUKENDI, tukendi)) {
            donem.ekVeriDegisti(ALT_SISTEM);
        }
    }

    /** Durma anını yazar (durdurmada ve durmuşken evre değişince: yeni evrenin süresi o andan sayılır). */
    public static void durmaAyarla(DunyaDonemi donem, long oyunTick, long saat) {
        CompoundTag veri = veri(donem);
        veri.putLong(DURMA_OYUN_TICK, oyunTick);
        veri.putLong(DURMA_SAATI, saat);
        istemciyeBildir(donem);
    }

    /**
     * Kural açıldı (K-C46): durma anı silinir, evrenin oyun günü süresi (PULSAR'da {@link #BASLANGIC}, güneşsiz dönemde
     * {@link #GUNESSIZ_BASLANGIC}) durduğu süre kadar ileri çekilir. Yutma saniyesi ölçekli saatle evre başlangıcından
     * sayıldığı için onu sahibi {@code DunyaDonemi.evreBaslangiciniKaydir} ile kaydırır.
     *
     * @param ileriOyunTick durduğu süre (K-C29 oyun tick'i, ≥ 0)
     */
    public static void durmaBitti(DunyaDonemi donem, long ileriOyunTick) {
        CompoundTag veri = veri(donem);
        String sure = switch (donem.gunesEvresi()) {
            case PULSAR -> BASLANGIC;
            case GUNESSIZ -> GUNESSIZ_BASLANGIC;
            default -> null;
        };
        if (sure != null && veri.contains(sure)) {
            veri.putLong(sure, veri.getLong(sure) + ileriOyunTick);
        }
        veri.remove(DURMA_OYUN_TICK);
        veri.remove(DURMA_SAATI);
        istemciyeBildir(donem);
    }

    /**
     * {@code kizilcuce baslat}, {@code pulsar baslat} ve zincirin doğal doğuşu: yol bayrakları, aşama kayıtları ve durma
     * anı silinir; döngü kimliği (biten döngünün yangın/meteor kaynağı ona bakar, yeni PULSAR üstüne yazar) ve
     * {@link #zincirTukendi} kalır.
     */
    public static void temizle(DunyaDonemi donem) {
        CompoundTag veri = veri(donem);
        boolean bayrak = veri.contains(DONUS_YOLU) || veri.contains(GUNES_DONDU) || veri.contains(DURMA_OYUN_TICK);
        boolean kayit = veri.contains(BASLANGIC) || veri.contains(GUNESSIZ_BASLANGIC) || veri.contains(YUTMA_PATLAMASI);
        veri.remove(DONUS_YOLU);
        veri.remove(GUNES_DONDU);
        veri.remove(BASLANGIC);
        veri.remove(GUNESSIZ_BASLANGIC);
        veri.remove(YUTMA_PATLAMASI);
        veri.remove(DURMA_OYUN_TICK);
        veri.remove(DURMA_SAATI);
        if (bayrak) {
            istemciyeBildir(donem);
        } else if (kayit) {
            donem.ekVeriDegisti(ALT_SISTEM);
        }
    }

    private static CompoundTag veri(DunyaDonemi donem) {
        return donem.ekVeri(ALT_SISTEM);
    }

    /** Bayrak yalnız {@code true} iken saklanır; değiştiyse true. */
    private static boolean bayrakYaz(DunyaDonemi donem, String anahtar, boolean deger) {
        CompoundTag veri = veri(donem);
        if (veri.getBoolean(anahtar) == deger) {
            return false;
        }
        if (deger) {
            veri.putBoolean(anahtar, true);
        } else {
            veri.remove(anahtar);
        }
        return true;
    }

    private static void istemciyeBildir(DunyaDonemi donem) {
        donem.senkronIste(true);
        donem.ekVeriDegisti(ALT_SISTEM);
    }
}
