package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

/**
 * Kasırga seviye tablosu (madde 10, tasarım notu §6) ve görsel türetilmiş boyutlar. Saf mantık: Minecraft sınıfına
 * dokunmaz. Uzunluklar blok, hız blok/sn.
 *
 * @param tepeCarpani    huni tepe yarıçapı / taban yarıçapı; 2'nin altı kama biçimidir
 * @param altBoslukOrani görünen huninin yerden başladığı yükseklik / huni yüksekliği (S1 yere tam değmez)
 */
public record KasirgaSeviyesi(int seviye, double tabanCapi, double yukseklik, double hiz, int aktifSureSn,
        double cekimYaricapi, int blokKaldirmaTick, double sertlikSiniri, double cekirdekHasari, double tepeCarpani,
        double enkazYuksekligi, double altBoslukOrani) {

    private static final KasirgaSeviyesi[] TABLO = {
            new KasirgaSeviyesi(1, 6, 50, 2.0, 60, 9, 2, 0.6, 1.0, 3.0, 6, 0.12),
            new KasirgaSeviyesi(2, 10, 65, 2.6, 90, 15, 4, 2.0, 1.5, 2.6, 8, 0.06),
            new KasirgaSeviyesi(3, 16, 80, 3.2, 120, 24, 8, 3.0, 2.0, 2.2, 10, 0),
            new KasirgaSeviyesi(4, 24, 95, 3.8, 150, 36, 14, 4.0, 3.0, 1.95, 12, 0),
            new KasirgaSeviyesi(5, 34, 110, 4.4, 180, 51, 20, 5.0, 4.0, 1.75, 14, 0),
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
        return tabanYaricapi() * tepeCarpani;
    }

    /** Kama: taban yarıçapı tepe yarıçapının yarısından büyük. */
    public boolean kamaMi() {
        return tabanYaricapi() > tepeYaricapi() / 2;
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
