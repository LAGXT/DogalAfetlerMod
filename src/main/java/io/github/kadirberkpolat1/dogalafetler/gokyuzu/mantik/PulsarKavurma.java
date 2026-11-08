package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Pulsar kavurmasının saf dönüşüm tablosu (PL-3 a): blok kategorisi × güç (0–1) × [0, 1) zar → hedef. Kategoriyi blok ve
 * etiketten sunucu tarafı ({@code gokyuzu.KavurmaKuyrugu}) çıkarır; tablo Minecraft sınıfı kullanmaz. Güç 0 iken hiçbir
 * şey değişmez. Kül birikimi (kül yağışının dünyadaki izi) ayrı bir zarla {@link #birikimKati} ile seçilir. Örnekleme
 * (Ps5): deneme sayısı alanla orantılıdır ({@link #DENEME_YOGUNLUGU}, yarıçaptan bağımsız); bir isabet sütunu yanan
 * örtüsüyle ({@link #altinaSurer}) zemine kadar kavurur.
 */
public final class PulsarKavurma {
    /** Varsayılan {@code pulsarKavrulmaOlasiligi}: tam güçte bir denemenin sütun seçme olasılığı. */
    public static final double VARSAYILAN_OLASILIK = 0.6;
    /** Üç oyun günü (oyun tick'i). */
    public static final long UC_GUN_TICK = 3L * 24_000L;
    /**
     * Tam güçte, varsayılan olasılıkla üç oyun gününde bir yüzey sütununa düşen ortalama isabet (Poisson λ). Bir isabet
     * sütunu zemine kadar kavurur (çimen kalmaz), ikincisi kaba toprağı kül bloğuna çevirir: 96 blok yarıçaptaki ~29 bin
     * sütunda hiç isabet almayan sütun beklentisi 29 000 · e^−16 ≈ 0,003, iki isabetten az alan payı 17 · e^−16 ≈ 2·10⁻⁶.
     */
    public static final double UC_GUN_ISABET = 16.0;
    /** Sütun ve oyun tick'i başına deneme: merkez başına tick'teki deneme {@code adım · yoğunluk · πr²}. */
    public static final double DENEME_YOGUNLUGU = UC_GUN_ISABET / (VARSAYILAN_OLASILIK * UC_GUN_TICK);

    /**
     * Bir merkezin bu tick'teki deneme planı.
     *
     * @param sayi yapılacak deneme
     * @param artan sonraki tick'e kalan kesirli deneme (0 ≤ artan < 1)
     * @param etkinAdim gerçekten örneklenen oyun tick'i (tavan kırptıysa adımdan küçük; eşdeğer gün bununla birikir)
     */
    public record Denemeler(int sayi, double artan, double etkinAdim) {
    }

    /** Kumun cama dönme olasılığı (dönüşüm denemesi başına). */
    public static final double CAM_OLASILIGI = 0.02;
    /** Tam güçte bir yüzey denemesinin kül katmanı biriktirme olasılığı. */
    public static final double BIRIKIM_OLASILIGI = 0.04;
    /** Birikimin yüzeyde ulaşabildiği en çok kat (yaprak külü 8'e kadar çıkarabilir). */
    public static final int BIRIKIM_TAVANI = 3;
    /** Kül katmanının en çok katı (kar katmanı gibi). */
    public static final int KUL_EN_COK_KAT = 8;

    /** Dönüşüm açısından blok sınıfı. */
    public enum Kategori {
        /** Çimen, podzol, miselyum, yosun bloğu. */
        CIMEN,
        /** Toprak, kaba toprak. */
        TOPRAK,
        /** {@code #minecraft:leaves}. */
        YAPRAK,
        /** {@code #minecraft:logs}. */
        KUTUK,
        /** Çiçek, ot, fidan, eğrelti, ölü çalı, mantar, kaktüs. */
        BITKI,
        /** Kar katmanı, kar bloğu, toz kar. */
        KAR,
        BUZ,
        KUM,
        DIGER
    }

    /** Dönüşümün sonucu; {@link #YOK} blok değişmez. */
    public enum Hedef {
        YOK(0),
        KABA_TOPRAK(1),
        KUL_BLOGU(1),
        /** Yaprak havaya döner, altındaki zemine bir kat kül katmanı düşer. */
        HAVA_VE_KUL(2),
        /** Eksen korunur. */
        KOMURLESMIS_KUTUK(1),
        HAVA(1),
        SU(1),
        CAM(1);

        private final int islem;

        Hedef(int islem) {
            this.islem = islem;
        }

        /** Hedefin en çok blok yazma sayısı (bütçeden düşülür). */
        public int islem() {
            return islem;
        }
    }

    private PulsarKavurma() {
    }

    /**
     * @param guc pulsar gücü 0–1 (ısınma eğrisi); toprak bu olasılıkla kül bloğuna döner
     * @param zar [0, 1) düzgün sayı
     */
    public static Hedef hedef(Kategori kategori, float guc, double zar) {
        if (kategori == null || !(guc > 0f)) {
            return Hedef.YOK;
        }
        return switch (kategori) {
            case CIMEN -> Hedef.KABA_TOPRAK;
            case TOPRAK -> zar < guc ? Hedef.KUL_BLOGU : Hedef.YOK;
            case YAPRAK -> Hedef.HAVA_VE_KUL;
            case KUTUK -> Hedef.KOMURLESMIS_KUTUK;
            case BITKI, KAR -> Hedef.HAVA;
            case BUZ -> Hedef.SU;
            case KUM -> zar < CAM_OLASILIGI ? Hedef.CAM : Hedef.YOK;
            case DIGER -> Hedef.YOK;
        };
    }

    /**
     * Kül yüzeyine (kül bloğu, kaba toprak ya da ince kül katmanı) eklenecek kat: {@code zar < BIRIKIM_OLASILIGI · guc}
     * ise 1–3 (zarın o aralıktaki yerinden), değilse 0.
     */
    public static int birikimKati(float guc, double zar) {
        double p = BIRIKIM_OLASILIGI * Math.max(0f, Math.min(1f, guc));
        if (!(p > 0) || zar >= p) {
            return 0;
        }
        return 1 + Math.min(BIRIKIM_TAVANI - 1, (int) (zar / p * BIRIKIM_TAVANI));
    }

    /** Yarıçap içindeki sütun sayısı (πr²). */
    public static double alan(int yaricap) {
        return Math.PI * yaricap * (double) yaricap;
    }

    /**
     * Bu tick'in denemeleri: {@code istenen = artan + adım · DENEME_YOGUNLUGU · πr²}, {@code sayi = ⌊istenen⌋}. {@code tavan}
     * aşılırsa (yüksek {@code /dogalafet hiz}) fazlası atılır, kesir birikmez ve etkin adım {@code adım · tavan / istenen}'e
     * küçülür: eşdeğer gün yalnız gerçekten örneklenen süreyi sayar.
     */
    public static Denemeler denemeler(double artan, int adim, int yaricap, int tavan) {
        if (adim <= 0) {
            return new Denemeler(0, artan, 0);
        }
        double istenen = Math.max(0, artan) + adim * DENEME_YOGUNLUGU * alan(yaricap);
        int sayi = (int) Math.floor(istenen);
        if (sayi <= tavan) {
            return new Denemeler(sayi, istenen - sayi, adim);
        }
        int t = Math.max(0, tavan);
        return new Denemeler(t, 0, adim * (double) t / istenen);
    }

    /**
     * Sonuçtan sonra sütun taraması aşağı sürer mi: yanan örtü (yaprak, bitki, kar, gövde) kalkınca altı da aynı isabette
     * kavrulur; zemin ({@link Hedef#YOK} dahil), su ve cam taramayı bitirir.
     */
    public static boolean altinaSurer(Hedef hedef) {
        return hedef == Hedef.HAVA || hedef == Hedef.HAVA_VE_KUL || hedef == Hedef.KOMURLESMIS_KUTUK;
    }

    /** Mevcut kül katına {@code ek} eklenince yeni kat; birikim {@link #BIRIKIM_TAVANI}'nı, yaprak külü 8'i geçmez. */
    public static int yeniKat(int mevcut, int ek, int tavan) {
        int t = Math.max(0, Math.min(KUL_EN_COK_KAT, tavan));
        return mevcut >= t ? mevcut : Math.min(t, mevcut + Math.max(0, ek));
    }
}
