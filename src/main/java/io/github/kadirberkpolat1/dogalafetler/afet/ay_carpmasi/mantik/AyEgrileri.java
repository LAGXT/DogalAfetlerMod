package io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik;

/**
 * Ay çarpmasının zaman çizelgesi ve görünüm eğrileri (docs/tasarim/ay_carpmasi.md §2). Saf Java: sunucu aşama zamanlarını
 * ve istemci çizimini aynı sabitlerden alır. Saniyeler ölçekli afet saatindendir ({@code /dogalafet hiz} hızlandırır):
 * yaklaşma için sirenin başından, çarpma sonrası için çarpma anından geçen saniye. Bütün eğriler yaklaşma ve
 * çarpma süresince monotondur (istemci saatinin sanilik geri düzeltmesi titreme yapmasın, K-C35).
 */
public final class AyEgrileri {
    /** Yaklaşma = siren süresi (K0-19). */
    public static final float YAKLASMA_SN = 60f;
    /** Parçalanma (çatlaklar, kopan parçalar) başlangıcı. */
    public static final float PARCALANMA_SN = 20f;
    /** Son yaklaşma: ay ufka iner, alt kenarı atmosferde kızarır. */
    public static final float SON_YAKLASMA_SN = 50f;
    /** Yaklaşma yönü: +X'ten (doğu) +Z'ye (güney) doğru derece; dünyada sabit, herkes aynı yönde görür. */
    public static final float AZIMUT_DERECE = 20f;
    public static final float YAKLASMA_EGIMI = 35f;
    public static final float SON_EGIM = 4f;
    public static final float EN_BUYUK_OLCEK = 14f;

    public static final float PARLAMA_SN = 0.5f;
    public static final float PARLAMA_TEPE = 0.7f;
    /** Ses: parlamadan sonra tam sessizlik, sonra güm (ışık sesten önce gelir). */
    public static final float GUM_SN = 1.5f;
    public static final float SARSINTI_BAS_SN = 3f;
    public static final float SARSINTI_SON_SN = 15f;
    /** Kamera sarsıntısının tepe genliği (derece; deprem S4, mesafeden bağımsız). */
    public static final float SARSINTI_DERECE = 2.5f;
    public static final float METEOR_BAS_SN = 10f;

    /**
     * Havadaki 3 boyutlu patlamanın ömrü (gökyüzü-2): ateş topu, şok kabuğu, enkaz, duman ve dünya parlaması bu sürede
     * sıfıra iner; sonra gelen oyuncu da hiçbir şey görmez (eski mantar sütunu bir gün kalıyordu).
     */
    public static final float PATLAMA_SN = 7f;
    /** Patlamanın kameradan göründüğü ilk yükseklik (derece) ve dünyadaki uzaklığı (blok), ateş topunun tepe yarıçapı (blok). */
    public static final float PATLAMA_EGIMI = 9f;
    public static final float PATLAMA_UZAKLIK = 900f;
    public static final float PATLAMA_YARICAP = 360f;
    /** Enkaz halkası: 3 Minecraft günü, alfa 0,6 → 0. */
    public static final float HALKA_SN = 3600f;
    public static final float HALKA_ALFA = 0.6f;

    private AyEgrileri() {
    }

    public static float sinirla(float x) {
        return x < 0f ? 0f : x > 1f ? 1f : x;
    }

    /** Smoothstep: 0 → 1, uçlarda eğimi 0. */
    public static float yumusak(float x) {
        float u = sinirla(x);
        return u * u * (3f - 2f * u);
    }

    /** Ayın vanilla ay boyuna oranı {@code 1 + 13·t³} (t = s / 60): son saniyelerde hızla büyür. */
    public static float olcek(float sn) {
        float t = sinirla(sn / YAKLASMA_SN);
        return 1f + (EN_BUYUK_OLCEK - 1f) * t * t * t;
    }

    /** Ayın ufuktan yüksekliği (derece): 50 sn'ye kadar 35°, son 10 sn'de 4°'ye iner. */
    public static float egim(float sn) {
        return YAKLASMA_EGIMI + (SON_EGIM - YAKLASMA_EGIMI) * yumusak((sn - SON_YAKLASMA_SN) / (YAKLASMA_SN - SON_YAKLASMA_SN));
    }

    /** Üç aşamalı çatlak katmanının alfası ({@code asama} 1–3): 20, 33 ve 46. sn'de 6 sn'de belirir. */
    public static float catlak(int asama, float sn) {
        float bas = PARCALANMA_SN + 13f * (Math.max(1, Math.min(3, asama)) - 1);
        return yumusak((sn - bas) / 6f);
    }

    /** Kopan parçaların yörünge yayı boyunca yayılması (0–1). */
    public static float parcaYayilmasi(float sn) {
        float u = sinirla((sn - PARCALANMA_SN) / (YAKLASMA_SN - PARCALANMA_SN));
        return (float) Math.pow(u, 1.5);
    }

    /** Atmosfere giren alt kenarın ve ufkun kızarması (0–1), son 10 sn. */
    public static float kizarma(float sn) {
        return yumusak((sn - SON_YAKLASMA_SN) / (YAKLASMA_SN - SON_YAKLASMA_SN));
    }

    /** Çarpma parlamasının ekran beyazlığı: 0,5 sn %70, sonra 0,25 sn'de söner. */
    public static float parlama(float snSonra) {
        if (snSonra < 0f) {
            return 0f;
        }
        return PARLAMA_TEPE * (1f - yumusak((snSonra - PARLAMA_SN) / 0.25f));
    }

    /** Patlama çizilir mi: çarpmadan sonra {@link #PATLAMA_SN} sn. */
    public static boolean patlamaGorunur(float snSonra) {
        return snSonra >= 0f && snSonra < PATLAMA_SN;
    }

    /** Ateş topunun büyümesi 0–1: hızlı açılır, yavaşlar. */
    public static float patlamaBuyume(float snSonra) {
        return snSonra <= 0f ? 0f : 1f - (float) Math.exp(-snSonra / 0.6);
    }

    /** Renk sıcaklığı 1 (beyaz-sarı) → 0 (koyu kızıl), 3,5 sn. */
    public static float patlamaSicakligi(float snSonra) {
        return 1f - yumusak(snSonra / 3.5f);
    }

    /** Ateş topu alfası: 0,12 sn'de tam, 0,9 sn'den 4,5 sn'ye söner. */
    public static float atesTopuAlfa(float snSonra) {
        if (!patlamaGorunur(snSonra)) {
            return 0f;
        }
        return yumusak(snSonra / 0.12f) * (1f - yumusak((snSonra - 0.9f) / 3.6f));
    }

    /** Şok kabuğunun büyümesi 0–1 (ateş topundan hızlı). */
    public static float kabukBuyume(float snSonra) {
        return snSonra <= 0f ? 0f : 1f - (float) Math.exp(-snSonra / 0.45);
    }

    /** Şok kabuğu alfası: anında belirir, 2,5 sn'de söner. */
    public static float kabukAlfa(float snSonra) {
        if (!patlamaGorunur(snSonra)) {
            return 0f;
        }
        return yumusak(snSonra / 0.08f) * (1f - yumusak((snSonra - 0.3f) / 2.2f));
    }

    /** Enkazın merkezden yolu (ateş topu yarıçapı katı, en çok 1,9). */
    public static float enkazYolu(float snSonra) {
        return snSonra <= 0f ? 0f : 1.9f * (1f - (float) Math.exp(-snSonra / 1.8));
    }

    /** Enkaz alfası: 2 sn tam, 6,2 sn'de 0. */
    public static float enkazAlfa(float snSonra) {
        if (!patlamaGorunur(snSonra)) {
            return 0f;
        }
        return 1f - yumusak((snSonra - 2f) / 4.2f);
    }

    /** Duman alfası: 0,6–2,2 sn'de belirir, 3,2 sn'den sonra söner, {@link #PATLAMA_SN} − 0,3 sn'de 0. */
    public static float dumanAlfa(float snSonra) {
        if (!patlamaGorunur(snSonra)) {
            return 0f;
        }
        return yumusak((snSonra - 0.6f) / 1.6f) * (1f - yumusak((snSonra - 3.2f) / (PATLAMA_SN - 0.3f - 3.2f)));
    }

    /**
     * Dünya parlaması 0–1 (gök, sis ve ışık haritası; {@code ay_carpmasi.parlama} çarpar): 0,1 sn'de tam, 0,5 sn tutar, sonra
     * 0,9 sn üstel sönüm ve 4,5 sn'de kesin 0.
     */
    public static float dunyaParlamasi(float snSonra) {
        if (!patlamaGorunur(snSonra)) {
            return 0f;
        }
        float sonum = snSonra < 0.5f ? 1f : (float) Math.exp(-(snSonra - 0.5f) / 0.9);
        return yumusak(snSonra / 0.1f) * sonum * (1f - yumusak((snSonra - 3f) / 1.5f));
    }

    /** Kamera sarsıntısının gücü (0–1): 3–15 sn, 0,5 sn'de girer, son 5 sn'de çıkar. */
    public static float sarsinti(float snSonra) {
        if (snSonra < SARSINTI_BAS_SN || snSonra > SARSINTI_SON_SN) {
            return 0f;
        }
        return yumusak((snSonra - SARSINTI_BAS_SN) / 0.5f) * (1f - yumusak((snSonra - (SARSINTI_SON_SN - 5f)) / 5f));
    }

    /** Enkaz halkasının alfası: 20–80 sn'de belirir, 3 günde 0,6'dan 0'a doğrusal iner. */
    public static float halkaAlfa(float snSonra) {
        if (snSonra <= 20f || snSonra >= HALKA_SN) {
            return 0f;
        }
        return HALKA_ALFA * yumusak((snSonra - 20f) / 60f) * (1f - snSonra / HALKA_SN);
    }

    /** Çarpmadan sonra gökte çizilecek bir şey kaldı mı (patlama ya da halka). */
    public static boolean carpmaIzleriSuruyor(float snSonra, boolean halkaAcik) {
        return snSonra >= 0f && (snSonra < PATLAMA_SN || halkaAcik && snSonra < HALKA_SN);
    }

    /** Kalite sırasına (0 Düşük … 3 Ultra) göre ay parçası sayısı (tasarım §5). */
    public static int parcaSayisi(int kalite) {
        return new int[] {15, 25, 40, 70}[Math.max(0, Math.min(3, kalite))];
    }

    /** Patlamanın enkaz parçası, duman topağı ve küre enlem dilimi sayıları (kalite sırasıyla). */
    public static int enkazSayisi(int kalite) {
        return new int[] {12, 24, 40, 64}[Math.max(0, Math.min(3, kalite))];
    }

    public static int dumanSayisi(int kalite) {
        return new int[] {4, 7, 10, 14}[Math.max(0, Math.min(3, kalite))];
    }

    public static int kureDilimi(int kalite) {
        return new int[] {8, 10, 14, 18}[Math.max(0, Math.min(3, kalite))];
    }
}
