package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik;

/**
 * Güneş patlamasının istemci görsellerinin saf eğrileri (tasarım §2): aurora perdesi, ısı dalgalanması, yaprak/çimen
 * kuruluğu ve kızıl cüce şafak tonu. Girdi BUYUYEN evre ilerlemesi {@code p} (siren 0 → 0,273, 5 gün 0,273 → 1).
 */
public final class GunesGorselEgrileri {
    /** Yaprak ve çimen rengi bu ilerlemeden (2. günün sonu) itibaren kurur. */
    public static final float KURULUK_BASI = GunesPatlamasiEgrileri.aktifIlerlemesi(2, 5);
    /** Kuruluğun biyom rengine en çok karışma payı (sürecin sonunda). */
    public static final float KURULUK_TAVANI = 0.72f;
    /** Kurumuş çimen (soluk saman) ve yaprak (kahverengi) rengi. */
    public static final int KURU_CIMEN = 0xB59A55;
    public static final int KURU_YAPRAK = 0x8A6A2E;
    /** Chunk'ların yeniden örülmesi için kuruluk basamağı sayısı. */
    public static final int KURULUK_BASAMAGI = 8;

    private GunesGorselEgrileri() {
    }

    /**
     * Aurora perdesinin gücü 0–1: sirende ilerlemeyle %35'ten tama açılır; yalnız gece ({@code gece} 0–1) ve yağmur
     * azaldıkça.
     */
    public static float aurora(float ilerleme, float gece, float yagmur) {
        float acilis = kenetle(ilerleme / GunesPatlamasiEgrileri.SIREN_SONU);
        return kenetle((0.35f + 0.65f * acilis) * kenetle(gece) * (1f - kenetle(yagmur)));
    }

    /** Isı dalgalanması 0–1: AKTIF'te ilerlemeyle artar; gündüz ve göğü gören konumda. */
    public static float isiDalgasi(float ilerleme, float gunIsigi, boolean gokGorunur) {
        if (!gokGorunur) {
            return 0f;
        }
        float aktif = kenetle((ilerleme - GunesPatlamasiEgrileri.SIREN_SONU) / (1f - GunesPatlamasiEgrileri.SIREN_SONU));
        return kenetle((0.25f + 0.75f * aktif) * kenetle(gunIsigi) * kenetle(ilerleme / GunesPatlamasiEgrileri.SIREN_SONU));
    }

    /** Yaprak/çimen kuruluğu 0–{@link #KURULUK_TAVANI}: 2. günün sonundan sürecin sonuna yumuşak geçiş. */
    public static float kuruluk(float ilerleme) {
        float t = kenetle((ilerleme - KURULUK_BASI) / (1f - KURULUK_BASI));
        return KURULUK_TAVANI * t * t * (3f - 2f * t);
    }

    /** Kuruluğun basamağı (0–{@link #KURULUK_BASAMAGI}); değişince chunk'lar yeniden örülür. */
    public static int kurulukBasamagi(float kuruluk) {
        return Math.round(kenetle(kuruluk / KURULUK_TAVANI) * KURULUK_BASAMAGI);
    }

    /** Basamağın karışma payı (basamaklı: aradaki değerler yeniden örmeden renk değiştirmesin). */
    public static float basamakPayi(int basamak) {
        return KURULUK_TAVANI * Math.max(0, Math.min(KURULUK_BASAMAGI, basamak)) / KURULUK_BASAMAGI;
    }

    /** RGB doğrusal karışım; {@code -1} (renk yok) olduğu gibi kalır. */
    public static int karistir(int renk, int hedef, float t) {
        if (renk == -1 || t <= 0f) {
            return renk;
        }
        float u = kenetle(t);
        int r = Math.round(((renk >> 16) & 0xFF) * (1f - u) + ((hedef >> 16) & 0xFF) * u);
        int g = Math.round(((renk >> 8) & 0xFF) * (1f - u) + ((hedef >> 8) & 0xFF) * u);
        int b = Math.round((renk & 0xFF) * (1f - u) + (hedef & 0xFF) * u);
        return (renk & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Kızıl cüce doğuşunun sıcak ekran tonu: doğuşun ortasında tepe (0,14), başta ve sonda 0. */
    public static float safakTonu(float dogusIlerlemesi) {
        float p = kenetle(dogusIlerlemesi);
        return p >= 1f ? 0f : 0.14f * (float) Math.sin(Math.PI * p);
    }

    /** Perde ışını deseni 0,1–1: U yönünde kayan iki sinüsün çarpımı (doku yok, şader yok). */
    public static float isin(int dilim, float zaman, int katman) {
        float a = (float) Math.sin(dilim * 0.9f + zaman * 0.011f + katman * 1.7f);
        float b = (float) Math.sin(dilim * 0.37f - zaman * 0.007f + katman * 0.6f);
        return 0.55f + 0.45f * a * b;
    }

    private static float kenetle(float x) {
        return x < 0f ? 0f : Math.min(1f, x);
    }
}
