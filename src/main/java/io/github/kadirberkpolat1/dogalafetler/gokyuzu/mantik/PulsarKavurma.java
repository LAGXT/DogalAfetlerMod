package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Pulsar kavurmasının saf dönüşüm tablosu (PL-3 a): blok kategorisi × güç (0–1) × [0, 1) zar → hedef. Kategoriyi blok ve
 * etiketten sunucu tarafı ({@code gokyuzu.KavurmaKuyrugu}) çıkarır; tablo Minecraft sınıfı kullanmaz. Güç 0 iken hiçbir
 * şey değişmez. Kül birikimi (kül yağışının dünyadaki izi) ayrı bir zarla {@link #birikimKati} ile seçilir.
 */
public final class PulsarKavurma {
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

    /** Mevcut kül katına {@code ek} eklenince yeni kat; birikim {@link #BIRIKIM_TAVANI}'nı, yaprak külü 8'i geçmez. */
    public static int yeniKat(int mevcut, int ek, int tavan) {
        int t = Math.max(0, Math.min(KUL_EN_COK_KAT, tavan));
        return mevcut >= t ? mevcut : Math.min(t, mevcut + Math.max(0, ek));
    }
}
