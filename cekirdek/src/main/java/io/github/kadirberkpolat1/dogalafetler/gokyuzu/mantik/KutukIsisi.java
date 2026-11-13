package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Kömürleşmiş kütüğün ısı kademesi (blok durumu {@code isi}, 0…{@value #MAKS}) için saf kurallar: ışık seviyesi, ısı
 * kaynağı çarpanları, görünüm kademesi ve duman yoğunluğu. Her lav kovası bir kademe ekler; kar söndürür (0). Kademe
 * arttıkça ışık, ısı gücü ve etki yarıçapı artar; {@value #MAKS}'tan bir önceki kademe ({@link #COK_SICAK_ESIGI}) "çok
 * sıcak" görünür ve yakındaki oyuncunun ekranına ısı efekti getirir.
 */
public final class KutukIsisi {
    /** En yüksek kademe: bundan fazla lav kovası kabul edilmez. */
    public static final int MAKS = 6;
    /** Yeni yerleştirilen ve eski dünyalardaki kütüklerin kademesi (ılık: eski ısı kaynağının 3 katı). */
    public static final int VARSAYILAN = 2;
    /** Bu kademeden itibaren "sıcak" görünür. */
    public static final int SICAK_ESIGI = 3;
    /** Bu kademeden itibaren "çok sıcak" görünür ({@link #MAKS} − 1: sınırdan bir kova önce). */
    public static final int COK_SICAK_ESIGI = MAKS - 1;

    /** Kademeye göre doku/model görünümü. */
    public enum Gorunum {
        SONUK("sonuk"),
        ILIK("ilik"),
        SICAK("sicak"),
        COK_SICAK("cok_sicak");

        private final String ek;

        Gorunum(String ek) {
            this.ek = ek;
        }

        /** Model ve doku adı eki (ılık, mevcut dokunun adıdır: ek yok). */
        public String ek() {
            return this == ILIK ? "" : "_" + ek;
        }
    }

    private static final int[] ISIK = {0, 3, 5, 7, 9, 12, 14};
    private static final double[] GUC = {0, 1.5, 3, 4.5, 6, 8, 10};

    private KutukIsisi() {
    }

    /** Bir kademeyi {@code 0…MAKS} aralığına sıkıştırır. */
    public static int sinirla(int isi) {
        return Math.max(0, Math.min(MAKS, isi));
    }

    /** Blok ışık seviyesi: sönükte 0, en sıcakta 14. */
    public static int isik(int isi) {
        return ISIK[sinirla(isi)];
    }

    /** Config'teki kaynak gücünün kademedeki çarpanı; sönükte 0 (ısı vermez), varsayılan kademede 3. */
    public static double gucCarpani(int isi) {
        return GUC[sinirla(isi)];
    }

    /** Config'teki kaynak yarıçapının kademedeki çarpanı: her kademede %20 genişler (en çok 2,2 kat). */
    public static double yaricapCarpani(int isi) {
        int k = sinirla(isi);
        return k == 0 ? 1.0 : 1.0 + 0.2 * k;
    }

    public static Gorunum gorunum(int isi) {
        int k = sinirla(isi);
        if (k == 0) {
            return Gorunum.SONUK;
        }
        if (k >= COK_SICAK_ESIGI) {
            return Gorunum.COK_SICAK;
        }
        return k >= SICAK_ESIGI ? Gorunum.SICAK : Gorunum.ILIK;
    }

    /** Ekran yakınlık efektini ve yoğun parçacıkları açan kademe mi. */
    public static boolean cokSicak(int isi) {
        return sinirla(isi) >= COK_SICAK_ESIGI;
    }

    /**
     * İstemci {@code animateTick} çağrısı başına duman yoğunluğu (beklenen parçacık sayısı): sönükte 0, kademeyle artar.
     * Tam kısmı kadar parçacık kesin, kesir kısmı olasılıkla üretilir.
     */
    public static double dumanYogunlugu(int isi) {
        int k = sinirla(isi);
        return k == 0 ? 0.0 : 0.35 + 0.4 * (k - 1);
    }

    /** Çıtırtı sesinin çağrı başına olasılığı: yalnız sıcak kademeden itibaren. */
    public static double citirtiOlasiligi(int isi) {
        int k = sinirla(isi);
        return k >= SICAK_ESIGI ? 0.08 + 0.04 * (k - SICAK_ESIGI) : 0.0;
    }
}
