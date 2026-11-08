package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Kara delik yutmasının koreografisi (pulsar-2, PL-4): evrenin başından beri geçen saniyeden ({@link PulsarZinciri#YUTMA_SN}
 * sn). 0–{@value #YAKLASMA_SN} sn uzaktaki kara delik hızlanarak yaklaşır ({@value #BASLANGIC_CAPI}° →
 * {@value #EN_BUYUK_CAP}°); {@value #YAKLASMA_SN} sn'den yutuma kadar pulsar kara deliğe çekilip spagettileşir, titreşimi
 * zirve yapar; {@link PulsarZinciri#YUTMA_PARLAMA_SN} sn'de kör edici parlama (G2-4 zarfı: hızlı yükseliş,
 * {@value #PARLAMA_TEPE_SN} sn tepe, sönüm; {@value #PARLAMA_SURE_SN} sn'de 0) ve pulsar yok olur; yutumdan sonra kara
 * delik küçülüp uzaklaşır ve söner, gök simsiyah. Çaplar derece. Minecraft sınıfı kullanmaz.
 */
public final class KaraDelikYutmaEgrileri {
    public static final float YAKLASMA_SN = 20f;
    public static final float PARLAMA_SURE_SN = 5f;
    public static final float PARLAMA_TEPE_SN = 1.5f;
    /** Parlamanın tepeye çıkışı (G2-4 ile aynı). */
    static final float PARLAMA_YUKSELIS_SN = 0.1f;
    static final float PARLAMA_TAU_SN = 0.9f;
    public static final float BASLANGIC_CAPI = 1.5f;
    public static final float EN_BUYUK_CAP = 20f;
    /** Yutumdan sonra kara delik yavaş uzaklaşır: 90. sn'deki çap (72. sn'de hâlâ ~8°). */
    public static final float SON_CAP = 2f;
    /** Merceklenmenin tam güce çıktığı çap. */
    static final float MERCEK_TAM_CAP = 8f;
    /** Parlamanın tam ekran örtüsü (karışımlı): tepe, tepede kalış, iniş ve inişin sonu; altında silüet okunur. */
    public static final float ORTU_TEPE = 0.5f;
    static final float ORTU_TEPE_SN = 0.3f;
    static final float ORTU_INIS_SN = 1.2f;
    public static final float ORTU_TABAN = 0.15f;
    /** Yutumdan sonra ekran kenarında ilk kırağının en çok gücü. */
    public static final float KIRAGI_ILK = 0.45f;
    /** Kara deliğin güneş yolu boyunca pulsardan açısı (derece): ikisi aynı kadraja sığar. */
    public static final float KARADELIK_ACI = 40f;
    /** Pulsarın tamamen yutulduğu ve parlamanın bittiği saniye. */
    public static final float YUTUM_SN = PulsarZinciri.YUTMA_PARLAMA_SN + PARLAMA_SURE_SN;
    /** Pulsarın parlama tepesinde ilerlediği ayrım payı: kuyruk diske değer, çekirdek gölgenin kenarındadır. */
    static final float PARLAMADA_CEKIM = 0.8f;

    private KaraDelikYutmaEgrileri() {
    }

    /** Kara deliğin (akresyon diski dahil) görünen açısal çapı: açı uzaklıkla ters orantılı, yaklaşma hızlanır. */
    public static float karaDelikCapi(float s) {
        if (s <= 0f) {
            return BASLANGIC_CAPI;
        }
        if (s < YAKLASMA_SN) {
            float t = s / YAKLASMA_SN;
            float uzaklik = 1f - (1f - BASLANGIC_CAPI / EN_BUYUK_CAP) * t * (float) Math.sqrt(t);
            return BASLANGIC_CAPI / uzaklik;
        }
        if (s < YUTUM_SN) {
            return EN_BUYUK_CAP;
        }
        float t = kenetle((s - YUTUM_SN) / (PulsarZinciri.YUTMA_SN - YUTUM_SN));
        return EN_BUYUK_CAP / (1f + (EN_BUYUK_CAP / SON_CAP - 1f) * t * t);
    }

    /** Kara deliğin örtücülüğü: ilk 1,5 sn'de belirir, uzaklaşmanın son üçte birinde söner. */
    public static float karaDelikAlfa(float s) {
        float sonum = YUTUM_SN + (PulsarZinciri.YUTMA_SN - YUTUM_SN) * 0.55f;
        return yumusak(0f, 1.5f, s) * (1f - yumusak(sonum, PulsarZinciri.YUTMA_SN, s));
    }

    /** Pulsarın kara deliğe doğru katettiği ayrım payı 0–1: çekim hızlanır, parlamada yutulur. */
    public static float cekim(float s) {
        float parlama = PulsarZinciri.YUTMA_PARLAMA_SN;
        if (s <= YAKLASMA_SN) {
            return 0f;
        }
        if (s < parlama) {
            float e = (s - YAKLASMA_SN) / (parlama - YAKLASMA_SN);
            return PARLAMADA_CEKIM * e * e;
        }
        return PARLAMADA_CEKIM + (1f - PARLAMADA_CEKIM) * yumusak(parlama, parlama + PARLAMA_TEPE_SN, s);
    }

    /** Pulsar ile kara delik merkezi arasındaki açı (derece). */
    public static float ayrim(float s) {
        return KARADELIK_ACI * (1f - cekim(s));
    }

    /** Spagettileşme 0–1: çekirdek ve hale kara deliğe uzanan eğik kuyruğa dönüşür. */
    public static float spagetti(float s) {
        return yumusak(YAKLASMA_SN, PulsarZinciri.YUTMA_PARLAMA_SN, s);
    }

    /** Titreşim genliği çarpanı: çekimle 1'den 2,5'e (zirve parlamada). */
    public static float titresimCarpani(float s) {
        return 1f + 1.5f * spagetti(s);
    }

    /** Pulsarın görünürlüğü: parlamanın tepesinde söner, yutumda yoktur. */
    public static float pulsarAlfa(float s) {
        float parlama = PulsarZinciri.YUTMA_PARLAMA_SN;
        return 1f - yumusak(parlama, parlama + PARLAMA_TEPE_SN, s);
    }

    /**
     * Kör edici mor-beyaz parlama 0–1 (G2-4 zarfı): {@value #PARLAMA_YUKSELIS_SN} sn'de tam, {@value #PARLAMA_TEPE_SN} sn'ye
     * dek tutar, τ {@value #PARLAMA_TAU_SN} sn söner, {@value #PARLAMA_SURE_SN} sn'de 0.
     */
    public static float parlama(float s) {
        float x = s - PulsarZinciri.YUTMA_PARLAMA_SN;
        if (x < 0f || x >= PARLAMA_SURE_SN) {
            return 0f;
        }
        if (x < PARLAMA_YUKSELIS_SN) {
            return x / PARLAMA_YUKSELIS_SN;
        }
        if (x < PARLAMA_TEPE_SN) {
            return 1f;
        }
        double taban = Math.exp(-(PARLAMA_SURE_SN - PARLAMA_TEPE_SN) / PARLAMA_TAU_SN);
        return (float) ((Math.exp(-(x - PARLAMA_TEPE_SN) / PARLAMA_TAU_SN) - taban) / (1.0 - taban));
    }

    /**
     * Parlamanın tam ekran örtüsü 0–{@value #ORTU_TEPE}: {@value #PARLAMA_YUKSELIS_SN} sn'de tepe,
     * {@value #ORTU_TEPE_SN} sn'ye dek tutar, sonraki {@value #ORTU_INIS_SN} sn'de {@value #ORTU_TABAN}'e iner, sonra parlamayla söner.
     */
    public static float ekranOrtusu(float s) {
        float x = s - PulsarZinciri.YUTMA_PARLAMA_SN;
        if (x < 0f || x >= PARLAMA_SURE_SN) {
            return 0f;
        }
        if (x < PARLAMA_YUKSELIS_SN) {
            return ORTU_TEPE * x / PARLAMA_YUKSELIS_SN;
        }
        if (x < ORTU_TEPE_SN) {
            return ORTU_TEPE;
        }
        float inisSonu = ORTU_TEPE_SN + ORTU_INIS_SN;
        if (x < inisSonu) {
            return ORTU_TEPE + (ORTU_TABAN - ORTU_TEPE) * yumusak(ORTU_TEPE_SN, inisSonu, x);
        }
        return ORTU_TABAN * parlama(s);
    }

    /** Kütleçekim merceği gücü 0–1: çevre yıldız sönümü ve mercek halkası kara deliğin açısal çapıyla büyür. */
    public static float merceklenme(float s) {
        return kenetle((karaDelikCapi(s) - BASLANGIC_CAPI) / (MERCEK_TAM_CAP - BASLANGIC_CAPI)) * karaDelikAlfa(s);
    }

    /** Foton halkasının ek parlaklığı: mercekle artar, parlamada tavan. */
    public static float fotonHalkasi(float s) {
        return Math.max((0.35f + 0.65f * merceklenme(s)) * karaDelikAlfa(s), parlama(s));
    }

    /** Akresyon diskinin parlaklığı: pulsarın maddesiyle beslendikçe artar, yutumdan sonra o parlaklıkta söner. */
    public static float diskParlakligi(float s) {
        float besleme = s < YUTUM_SN ? 0.55f + 0.45f * spagetti(s) : 0.95f;
        return besleme * karaDelikAlfa(s);
    }

    /** Akresyon akış dokusunun dönüşü (derece/sn). */
    public static float diskDonusHizi(float s) {
        return 25f + 35f * spagetti(s);
    }

    /** Derin uzay katmanının alt sınırı: yutuma doğru 1'e çıkar, sonra %100 kalır. */
    public static float uzayTabani(float s) {
        return yumusak(45f, YUTUM_SN, s);
    }

    /** Göğün koyu mora dönüşü (yaklaşma boyunca). */
    public static float morluk(float s) {
        return yumusak(0f, YAKLASMA_SN, s);
    }

    /** Göğün simsiyaha dönüşü (yutuma doğru). */
    public static float karanlik(float s) {
        return yumusak(45f, YUTUM_SN, s);
    }

    /** Kalıntı nebulasının kalan payı: çekim boyunca dağılır, yutumdan önce yok olur. */
    public static float kalinti(float s) {
        return 1f - yumusak(YAKLASMA_SN, YUTUM_SN - 2f, s);
    }

    /** Akresyon kalıntısının mora dönüşü: pulsarın maddesi bitince disk mor söner. */
    public static float kalintiMorlugu(float s) {
        return yumusak(YUTUM_SN - 2f, YUTUM_SN + 2f, s);
    }

    /** Kara deliğin arkasındaki yıldızların merceklenmiş yayları: pulsar yutulunca belirir, mercekle güçlenir. */
    public static float mercekYaylari(float s) {
        return yumusak(YUTUM_SN - 3f, YUTUM_SN, s) * merceklenme(s);
    }

    /** Ufukta külün kızıl korları: parlamadan sonra kararan gökte belirir. */
    public static float korUfku(float s) {
        return yumusak(PulsarZinciri.YUTMA_PARLAMA_SN + 1f, YUTUM_SN + 2f, s);
    }

    /** İlk kırağı vinyeti 0–{@value #KIRAGI_ILK}: soğuk yutumdan sonra ekran kenarından başlar. */
    public static float kiragi(float s) {
        return KIRAGI_ILK * yumusak(YUTUM_SN, PulsarZinciri.YUTMA_SN - 6f, s);
    }

    private static float kenetle(float x) {
        return Math.max(0f, Math.min(1f, x));
    }

    private static float yumusak(float a, float b, float x) {
        float t = kenetle((x - a) / (b - a));
        return t * t * (3f - 2f * t);
    }
}
