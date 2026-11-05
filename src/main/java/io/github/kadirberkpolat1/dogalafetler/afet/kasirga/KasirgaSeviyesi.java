package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

/**
 * Kasırga seviye tablosu (madde 10, tasarım notu §6) ve görsel türetilmiş boyutlar. Saf mantık: Minecraft sınıfına
 * dokunmaz. Uzunluklar blok, hız blok/sn.
 *
 * @param tepeCapi       huni gövdesinin tepe çapı; yoğuşma hunisi yukarı doğru genişler (taban çapından büyük)
 * @param altBoslukOrani görünen huninin yerden başladığı yükseklik / huni yüksekliği (S1 yere tam değmez)
 */
public record KasirgaSeviyesi(int seviye, double tabanCapi, double yukseklik, double hiz, int aktifSureSn,
        double cekimYaricapi, int blokKaldirmaTick, double sertlikSiniri, double cekirdekHasari, double tepeCapi,
        double enkazYuksekligi, double altBoslukOrani) {

    private static final KasirgaSeviyesi[] TABLO = {
            new KasirgaSeviyesi(1, 6, 50, 2.0, 60, 9, 2, 0.6, 1.0, 10, 6, 0.12),
            new KasirgaSeviyesi(2, 10, 65, 2.6, 90, 15, 4, 2.0, 1.5, 17, 8, 0.06),
            new KasirgaSeviyesi(3, 16, 80, 3.2, 120, 24, 8, 3.0, 2.0, 27, 10, 0),
            new KasirgaSeviyesi(4, 24, 95, 3.8, 150, 36, 14, 4.0, 3.0, 46, 12, 0),
            new KasirgaSeviyesi(5, 34, 110, 4.4, 180, 51, 20, 5.0, 4.0, 55, 14, 0),
    };

    public static KasirgaSeviyesi icin(int seviye) {
        if (seviye < 1 || seviye > TABLO.length) {
            throw new IllegalArgumentException("Kasırga seviyesi 1-5 olmalı: " + seviye);
        }
        return TABLO[seviye - 1];
    }

    public double tabanYaricapi() {
        return tabanCapi / 2;
    }

    public double tepeYaricapi() {
        return tepeCapi / 2;
    }

    /** Gövdenin orta yükseklikteki ({@code h = 0,5}) genişliği. */
    public double ortaGenislik() {
        return 2 * HuniProfili.yaricap(tabanYaricapi(), tepeYaricapi(), 0.5);
    }

    /** Kama: orta yükseklikteki genişlik yüksekliğin en az %40'ı. */
    public boolean kamaMi() {
        return ortaGenislik() >= 0.4 * yukseklik;
    }

    /** Taban enkaz halkasının dış yarıçapı: çapı huni taban çapının 1,8 katı. */
    public double enkazYaricapi() {
        return tabanYaricapi() * 1.8;
    }

    /** Duvar bulutunun yarıçapı: çapı taban çapının 6 katı. */
    public double duvarBulutuYaricapi() {
        return tabanCapi * 3;
    }

    /** Huni yüzeyinin çekirdek açısal hızı (rad/sn): teğet hız 6 + 3·seviye blok/sn, taban yarıçapında. */
    public double cekirdekAcisalHizi() {
        return (6 + 3.0 * seviye) / tabanYaricapi();
    }
}
