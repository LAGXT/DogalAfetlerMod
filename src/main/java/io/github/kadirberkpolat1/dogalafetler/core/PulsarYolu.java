package io.github.kadirberkpolat1.dogalafetler.core;

import net.minecraft.nbt.CompoundTag;

/**
 * Pulsar zincirinin dönemdeki kalıcı durumu (pulsar-2), {@code DunyaDonemi.ekVeri("pulsar")} üstünde; anahtarlar yalnız
 * burada. Pulsar yolu evre değil bayraktır: kara delik yutmasından sonraki {@link GunesEvresi#GUNESSIZ} (buzul kıyamet) ve
 * {@link GunesEvresi#KIZIL_CUCE} doğuşu süpernova yolundakilerle aynı evrelerdir, {@link #donusYolu} ile ayrılır (doğuş
 * bitince eski güneş döner). {@link #gunesDondu} NORMAL'e bu yoldan dönüldüğünü ve donmanın 0'a çözüldüğünü söyler. İki
 * bayrak {@code DonemPaketi.bayraklar} ile istemciye gider: değişimleri acil senkron ister, evreyle birlikte yazılırsa
 * aynı pakette gider. Süreler K-C29 oyun tick'iyle ({@code oyun zamanı + uyku birikimi + gün ofseti × 24000}) tutulur.
 * Yazanı {@code gokyuzu.PulsarSureci}, {@code GunesZinciriSureci} ve sıfırlamadır; diğerleri yalnız okur.
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

    /** Yeni pulsar döngüsü: döngü kimliği ve oyun tick'i başlangıcı birlikte yazılır, yutma bayrağı silinir. */
    public static void yeniDongu(DunyaDonemi donem, long dongu, long oyunTick) {
        CompoundTag veri = veri(donem);
        veri.putLong(DONGU, dongu);
        veri.putLong(BASLANGIC, oyunTick);
        veri.remove(YUTMA_PATLAMASI);
        donem.ekVeriDegisti(ALT_SISTEM);
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

    /**
     * {@code kizilcuce baslat}, {@code pulsar baslat} ve zincirin doğal doğuşu: yol bayrakları ve aşama kayıtları silinir;
     * döngü kimliği kalır (biten döngünün yangın/meteor kaynağı ona bakar, yeni PULSAR üstüne yazar).
     */
    public static void temizle(DunyaDonemi donem) {
        CompoundTag veri = veri(donem);
        boolean bayrak = veri.contains(DONUS_YOLU) || veri.contains(GUNES_DONDU);
        boolean kayit = veri.contains(BASLANGIC) || veri.contains(GUNESSIZ_BASLANGIC) || veri.contains(YUTMA_PATLAMASI);
        veri.remove(DONUS_YOLU);
        veri.remove(GUNES_DONDU);
        veri.remove(BASLANGIC);
        veri.remove(GUNESSIZ_BASLANGIC);
        veri.remove(YUTMA_PATLAMASI);
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
