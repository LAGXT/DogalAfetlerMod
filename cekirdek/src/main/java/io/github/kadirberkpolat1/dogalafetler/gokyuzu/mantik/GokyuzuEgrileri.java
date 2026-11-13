package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;

/**
 * Gökyüzü evrelerinin boyut, parlaklık ve renk eğrileri (docs/tasarim/gokyuzu.md, spike G2). İstemci çizicisi bunları
 * evre ilerlemesi (0–1) ve evre başlangıcından beri geçen gerçek saniyeyle çağırır; eğriler zamana göre monotondur
 * (istemci saatinin küçük geri adımları görüntüyü geri sardırmaz). Minecraft sınıfı kullanmaz.
 */
public final class GokyuzuEgrileri {
    /** Süpernova koreografisi: şişmiş güneşin çökmesi (saniye). */
    public static final float SN_COKME_SN = 8f;
    /** Evre renk geçişinin süresi (ölçekli tick, 20 sn). */
    public static final long GECIS_TICK = 400L;

    private GokyuzuEgrileri() {
    }

    public static float kenetle(float x, float enAz, float enCok) {
        return Math.max(enAz, Math.min(enCok, x));
    }

    public static float yumusak(float a, float b, float x) {
        float t = kenetle((x - a) / (b - a), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }

    /** Büyüyen güneşin vanilla diske göre ölçeği: 1× → 3,2×. */
    public static float buyuyenOlcek(float p) {
        return 1f + 2.2f * kenetle(p, 0f, 1f);
    }

    /** Büyüyen güneşin rengi: beyaz-sarı → #FF9A3C. */
    public static float[] buyuyenRenk(float p) {
        float t = kenetle(p, 0f, 1f);
        return new float[] {1f, lerp(t, 0.97f, 0x9A / 255f), lerp(t, 0.88f, 0x3C / 255f)};
    }

    /** Çöken çekirdeğin vanilla diske göre ölçeği: şişmiş güneş (3,2×) {@value #SN_COKME_SN} sn'de noktaya iner. */
    public static float supernovaCekirdekOlcek(float saniye) {
        return lerp(yumusak(0f, SN_COKME_SN, saniye), buyuyenOlcek(1f), 0.25f);
    }

    /** Çökmeden hemen sonraki parlama (0–1): 0,4 sn'de tepe, ~6 sn'lik üstel sönüm. */
    public static float supernovaFlas(float saniye) {
        float t = saniye - SN_COKME_SN;
        if (t <= 0f) {
            return 0f;
        }
        return Math.min(1f, t / 0.4f) * (float) Math.exp(-t / 6.0);
    }

    /** 1–2 günlük sönümün parlaklığı. */
    public static float supernovaParlaklik(float p) {
        return (float) Math.exp(-kenetle(p, 0f, 1f) / 0.35);
    }

    /**
     * Genişleyen kabuğun yarı boyu (100 birim uzaklıkta gök birimi): çökmeden sonra ~10 sn'de açılır, sonra ilerlemeyle
     * 8 → 63 birime genişler.
     */
    public static float supernovaHalka(float saniye, float p) {
        float acilis = 1f - (float) Math.exp(-Math.max(0f, saniye - SN_COKME_SN) / 3.5);
        return acilis * (8f + 55f * (1f - (float) Math.exp(-kenetle(p, 0f, 1f) * 10f)));
    }

    /** Süpernova kalıntısının görünürlüğü süpernova ilerledikçe belirir. */
    public static float kalintiBelirme(float p) {
        return yumusak(0.08f, 1f, p);
    }

    /** Kalıntı nebulasının yavaş genişlemesi (güneşsiz evre ilerlemesiyle, tavan %130). */
    public static float kalintiGenisleme(float p) {
        return 1f + 0.3f * kenetle(p, 0f, 1f);
    }

    /**
     * Kalıntı nebulasının tek evredeki görünürlüğü: süpernovada belirir, güneşsiz dönemde tam, kızıl cüce ve pulsar göğünde
     * %85, yutmada dağılır. Pulsar yolundaki ({@code yol}) güneşsiz dönem ve doğuşta yok.
     */
    public static float kalintiPayi(GunesEvresi evre, float p, float saniye, boolean yol) {
        return switch (evre) {
            case SUPERNOVA -> kalintiBelirme(p);
            case GUNESSIZ -> yol ? 0f : 1f;
            case KIZIL_CUCE -> yol ? 0f : 0.85f;
            case PULSAR -> 0.85f;
            case KARADELIK_YUTMA -> 0.85f * KaraDelikYutmaEgrileri.kalinti(saniye);
            default -> 0f;
        };
    }

    /**
     * Evre geçişinde kalıntı nebulası: önceki evrenin (tamamlanmış) payından yenisine {@code gecis} ile. Pulsar yolundan
     * eski güneşe dönüşte paketin dönüş yolu bayrağı kalkar; önceki kızıl cücenin yolunu {@code gunesDondu} bildirir
     * ({@link #oncekiYol}), yoksa 20 sn kalıntı nebulası belirirdi (Ps4).
     */
    public static float kalintiGecisi(GunesEvresi evre, float p, float saniye, boolean yol, GunesEvresi onceki,
            boolean oncekiYol, float gecis) {
        float yeni = kalintiPayi(evre, p, saniye, yol);
        return onceki == null || gecis >= 1f ? yeni : lerp(gecis, kalintiPayi(onceki, 1f, 1e6f, oncekiYol), yeni);
    }

    /** Önceki evre pulsar yolunda mıydı: dönüş yolu bayrağı ya da (NORMAL'e dönüldüyse) güneş döndü bayrağı. */
    public static boolean oncekiYol(boolean donusYolu, boolean gunesDondu) {
        return donusYolu || gunesDondu;
    }

    /** Kızıl cüce diskinin doğuşta belirmesi: önce ufukta yalnız soğuk-kızıl ışık, sonra disk. */
    public static float kizilCuceDiskAlfa(float p) {
        return yumusak(0f, 0.2f, p);
    }

    /** Doğuş gününde ufuk yelpazesinin şiddeti (spike G2: 1,1 + 0,5·(1 − p)). */
    public static float kizilCuceUfukGucu(float p) {
        return 1.1f + 0.5f * (1f - kenetle(p, 0f, 1f));
    }

    /** Evre değişiminden beri geçen ölçekli tick'e göre renk geçişi (20 sn doğrusal). */
    public static float gecis(double gecenTick) {
        return kenetle((float) (gecenTick / GECIS_TICK), 0f, 1f);
    }

    public static float[] rgb(int renk) {
        return new float[] {((renk >> 16) & 0xFF) / 255f, ((renk >> 8) & 0xFF) / 255f, (renk & 0xFF) / 255f};
    }

    /** İki paketli rengin doğrusal karışımı (RGB 0–1). */
    public static float[] karistir(int a, int b, float t) {
        return karistir(rgb(a), rgb(b), t);
    }

    public static float[] karistir(float[] a, float[] b, float t) {
        float k = kenetle(t, 0f, 1f);
        return new float[] {lerp(k, a[0], b[0]), lerp(k, a[1], b[1]), lerp(k, a[2], b[2])};
    }
}
