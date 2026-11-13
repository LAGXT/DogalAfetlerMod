package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Gökyüzüne bakma başarımlarının ortak kuralları (istemci sayar, sunucu doğrular ve biriktirir). Devasa kara deliğe
 * {@value #DEVASA_TIK} tick (5 sn) bakmak (10 tick'e kadar bir an kopma sayacı sıfırlamaz) {@code devasa}; {@value #GEZEGEN_SAYISI}
 * gezegenin hepsine (Merkür…Neptün ve cüce Plüton) toplam {@value #GEZEGEN_TIK} tick (3 sn) bakmak {@code astronom} verir.
 * Gezegen ilerlemesi bir bit maskesidir (sunucuda oyuncunun kalıcı verisinde): bit {@code i} = {@code i}. gezegen tamamlandı.
 */
public final class BakmaKurallari {
    /** Paket türü: devasa kara deliğe 5 sn bakıldı. */
    public static final int TUR_DEVASA = 0;
    /** Paket türü: {@code hedef} numaralı gezegene 3 sn bakıldı. */
    public static final int TUR_GEZEGEN = 1;
    public static final int DEVASA_TIK = 100;
    public static final int GEZEGEN_TIK = 60;
    /** Başarım için bakılması gereken gezegen sayısı (Merkür, Venüs, Mars, Jüpiter, Satürn, Uranüs, Neptün, Plüton). */
    public static final int GEZEGEN_SAYISI = 8;

    private BakmaKurallari() {
    }

    public static int tamMaske() {
        return (1 << GEZEGEN_SAYISI) - 1;
    }

    public static boolean gecerliGezegen(int gezegen) {
        return gezegen >= 0 && gezegen < GEZEGEN_SAYISI;
    }

    /** Geçersiz numara maskeyi değiştirmez. */
    public static int maskeyeEkle(int maske, int gezegen) {
        return gecerliGezegen(gezegen) ? maske | (1 << gezegen) : maske;
    }

    public static boolean maskedeVar(int maske, int gezegen) {
        return gecerliGezegen(gezegen) && (maske & (1 << gezegen)) != 0;
    }

    public static boolean tamMi(int maske) {
        return (maske & tamMaske()) == tamMaske();
    }
}
