package io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini.mantik;

import java.util.ArrayList;
import java.util.List;

/**
 * Orman yangını afetinin saf kuralları (bölüm G, madde 10, tasarım notu §6): seviye tablosu, etki alanının büyümesi, sıcak
 * kare özeti ve erken doğal bitiş. Yangının kendisi ortak {@code YanginYoneticisi}'ndedir. Minecraft sınıflarına dokunmaz.
 */
public final class OrmanKurallari {
    /** Sıcak kare ızgarasının kenarı (blok): sunucu yanan hücreleri bu karelerde sayar, istemci sis ve ısıyı bundan çıkarır. */
    public static final int KARE = 32;
    /** Etki alanı yanan alanın bu kadar dışına uzanır (siren ve "sağ çıkan" için pay). */
    public static final int ALAN_PAYI = 16;
    /** Etki alanı yarıçapının tavanı (blok); sayma ızgarasını da sınırlar. */
    public static final int EN_BUYUK_ALAN = 192;
    /** "Yanmadan Çıktım": yanan hücreye bu mesafede (blok) olan oyuncu yangın alanındadır (tasarım §6, başarım A-13). */
    public static final int YANGIN_YAKINLIGI = 8;
    /** İstemci: sis bu mesafede (blok) başlar, ısı dalgalanması bu mesafede (tasarım §2 "Ortam"). */
    public static final double SIS_MENZILI = 48;
    public static final double ISI_MENZILI = 24;

    private OrmanKurallari() {
    }

    /** Bir seviyenin değerleri: başlangıç alanı yarıçapı, yangın yoğunluğu 0–1, AKTIF süresi (tick). */
    public record Seviye(int yaricap, double yogunluk, int sureTick) {
    }

    /** Seviye 1–5 tablosu (config listeleri). */
    public record Tablo(List<Integer> yaricap, List<Double> yogunluk, List<Integer> sureSaniye) {
        public static final Tablo VARSAYILAN = new Tablo(List.of(3, 6, 10, 16, 24), List.of(0.0, 0.2, 0.4, 0.7, 1.0),
                List.of(120, 180, 240, 360, 480));

        public Seviye seviye(int seviye) {
            int i = Math.max(1, Math.min(5, seviye)) - 1;
            return new Seviye(yaricap.get(i), Math.max(0, Math.min(1, yogunluk.get(i))), sureSaniye.get(i) * 20);
        }
    }

    /** Sıcak kare: merkez XZ ve yanan hücre sayısı. */
    public record Kare(int x, int z, int sayi) {
    }

    /** Etki alanı yarıçapı: başlangıç alanı + pay ya da yanan karelerin en uzağı + pay; küçülmez, tavanlıdır. */
    public static double alanYaricapi(double onceki, double enUzakKare, int baslangicYaricapi) {
        double yeni = Math.max(baslangicYaricapi, enUzakKare) + ALAN_PAYI;
        return Math.min(EN_BUYUK_ALAN, Math.max(onceki, yeni));
    }

    /** Kare merkezinin uzaklığına yarım köşegen eklenir: karedeki en uzak hücre de alanda kalır. */
    public static double kareUzakligi(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz) + KARE * Math.sqrt(0.5);
    }

    /** En kalabalık {@code tavan} kare, (x, z, sayı) üçlüleri olarak; istemciye 1 Hz paketle gider. */
    public static int[] sicakKareler(List<Kare> kareler, int tavan) {
        List<Kare> sirali = new ArrayList<>(kareler);
        sirali.sort((a, b) -> Integer.compare(b.sayi(), a.sayi()));
        int n = Math.min(Math.max(0, tavan), sirali.size());
        int[] sonuc = new int[n * 3];
        for (int i = 0; i < n; i++) {
            Kare k = sirali.get(i);
            sonuc[i * 3] = k.x();
            sonuc[i * 3 + 1] = k.z();
            sonuc[i * 3 + 2] = k.sayi();
        }
        return sonuc;
    }

    /** Karelerden en yakınının kenarına yatay uzaklık (kare yarım kenarı düşülür); kare yoksa sonsuz. */
    public static double enYakin(int[] kareler, double x, double z) {
        double en = Double.POSITIVE_INFINITY;
        for (int i = 0; i + 2 < kareler.length; i += 3) {
            double dx = Math.max(0, Math.abs(kareler[i] - x) - KARE / 2.0);
            double dz = Math.max(0, Math.abs(kareler[i + 1] - z) - KARE / 2.0);
            en = Math.min(en, Math.sqrt(dx * dx + dz * dz));
        }
        return en;
    }

    /** Yakınlık ağırlığı 0–1: mesafe 0'da 1, {@code menzil}'de 0 (yumuşak). */
    public static double yakinlikAgirligi(double mesafe, double menzil) {
        if (!(mesafe < menzil) || menzil <= 0) {
            return 0;
        }
        double t = 1 - Math.max(0, mesafe) / menzil;
        return t * t * (3 - 2 * t);
    }

    /** Yangının büyüklüğüne göre sis gücü: 40 hücrede tam (tek çalı yangını gökyüzünü boyamaz). */
    public static double buyuklukCarpani(int hucre) {
        return Math.max(0, Math.min(1, hucre / 40.0));
    }

    /**
     * Erken doğal bitiş: başlangıç alanı tutuştuktan sonra alan {@code esikSaniye} boyunca hiç yanan hücre görmediyse
     * yangın sönmüştür (yağmur, seyrek yakıt). {@code esikSaniye} 0 erken bitişi kapatır.
     */
    public static boolean sondu(int sifirSaniye, int esikSaniye) {
        return esikSaniye > 0 && sifirSaniye >= esikSaniye;
    }
}
