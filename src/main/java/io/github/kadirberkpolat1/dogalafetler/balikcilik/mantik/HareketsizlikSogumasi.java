package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

/**
 * Madde 15 risk/denge: buz üstünde hareketsiz durarak balık tutan oyuncunun çevresine giderek güçlenen bir soğuk ortam
 * kaynağı (°C). Sıcaklık sistemi bunu diğer ortam girdileriyle birlikte yalıtımdan geçirir; bu yüzden kalın giyinmek ve
 * ateş yakmak işe yarar. Sayaç saniyede bir ilerler, oyuncu yürüyünce sıfırlanır.
 */
public final class HareketsizlikSogumasi {
    /** Saniyede bu kadardan az yatay yol (blok) hareketsiz sayılır (oltayı çekerken küçük kaymalar hareket değil). */
    public static final double HAREKET_ESIGI = 0.25;

    /**
     * @param enCokC     tam güçteki soğuma (°C, pozitif yazılır; kaynak negatif uygulanır)
     * @param tamSureSn  tam güce ulaşana kadar hareketsiz saniye
     */
    public record Ayarlar(double enCokC, int tamSureSn) {
        public Ayarlar {
            if (enCokC < 0 || tamSureSn <= 0) {
                throw new IllegalArgumentException("hareketsizlik soğuması ayarı geçersiz");
            }
        }
    }

    private HareketsizlikSogumasi() {
    }

    /** Kaynağın °C gücü (0 ya da negatif): hareketsiz saniyeyle doğrusal artar, tamSure'de tavana varır. */
    public static double guc(int hareketsizSn, Ayarlar a) {
        if (hareketsizSn <= 0 || a.enCokC() == 0) {
            return 0.0;
        }
        return -a.enCokC() * Math.min(1.0, hareketsizSn / (double) a.tamSureSn());
    }

    /** Son bir saniyedeki yatay yolun karesiyle hareketsiz mi. */
    public static boolean hareketsizMi(double yatayYolKaresi) {
        return yatayYolKaresi < HAREKET_ESIGI * HAREKET_ESIGI;
    }

    public static int sonrakiSayac(int sayac, boolean hareketsiz) {
        return hareketsiz ? sayac + 1 : 0;
    }
}
