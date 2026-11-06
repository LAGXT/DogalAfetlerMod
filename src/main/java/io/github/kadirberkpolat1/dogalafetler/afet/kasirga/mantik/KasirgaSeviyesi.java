package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/**
 * Kasırga seviye tablosu (madde 10, tasarım notu §6) ve türetilmiş boyutlar. Uzunluklar blok, hız blok/sn, süre sn,
 * hasar can/sn.
 *
 * @param tepeCapi       huni gövdesinin tepe çapı; yoğuşma hunisi yukarı doğru genişler (kama: orta genişlik ≈ %40-50 yükseklik)
 * @param enkazYuksekligi taban enkaz eteğinin yüksekliği (S1 6 → S5 14)
 * @param altBoslukOrani görünen huninin yerden başladığı yükseklik / huni yüksekliği (S1 yere tam değmez)
 * @param zeminKazar     toprak/kum/taş zemin bloklarını da kaldırır (yalnız S4-5: EF4-5 zemini sıyırır)
 */
public record KasirgaSeviyesi(int seviye, double tabanCapi, double tepeCapi, double yukseklik, double hiz,
        int aktifSureSn, double cekimYaricapi, int blokKaldirmaTick, double sertlikSiniri, double cekirdekHasari,
        double enkazYuksekligi, double altBoslukOrani, boolean zeminKazar) {

    private static final KasirgaSeviyesi[] TABLO = {
        new KasirgaSeviyesi(1, 6, 10, 50, 2.0, 60, 9, 2, 0.6, 1.0, 6, 0.12, false),
        new KasirgaSeviyesi(2, 10, 17, 65, 2.6, 90, 15, 4, 2.0, 1.5, 8, 0.06, false),
        new KasirgaSeviyesi(3, 16, 27, 80, 3.2, 120, 24, 8, 3.0, 2.0, 10, 0, false),
        new KasirgaSeviyesi(4, 24, 46, 95, 3.8, 150, 36, 14, 4.0, 3.0, 12, 0, true),
        new KasirgaSeviyesi(5, 34, 55, 110, 4.4, 180, 51, 20, 5.0, 4.0, 14, 0, true),
    };

    /** Tablodaki satır (çarpanlar 1). */
    public static KasirgaSeviyesi icin(int seviye) {
        if (seviye < 1 || seviye > TABLO.length) {
            throw new IllegalArgumentException("Kasırga seviyesi 1-5 olmalı: " + seviye);
        }
        return TABLO[seviye - 1];
    }

    /** Config çarpanları uygulanmış satır. */
    public static KasirgaSeviyesi icin(int seviye, Carpanlar c) {
        KasirgaSeviyesi s = icin(seviye);
        if (c.equals(Carpanlar.BIR)) {
            return s;
        }
        return new KasirgaSeviyesi(s.seviye, s.tabanCapi * c.boyut(), s.tepeCapi * c.boyut(), s.yukseklik * c.boyut(),
                s.hiz * c.hiz(), Math.max(1, (int) Math.round(s.aktifSureSn * c.sure())), s.cekimYaricapi * c.boyut(),
                (int) Math.round(s.blokKaldirmaTick * c.blok()), s.sertlikSiniri, s.cekirdekHasari * c.hasar(),
                s.enkazYuksekligi * c.boyut(), s.altBoslukOrani, s.zeminKazar);
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

    /** Taban enkaz eteğinin zemindeki yarıçapı: çapı huni taban çapının 1,8 katı. */
    public double etekYaricapi() {
        return tabanYaricapi() * 1.8;
    }

    /** Duvar bulutunun yarıçapı: çapı taban çapının 6 katı. */
    public double duvarBulutuYaricapi() {
        return tabanCapi * 3;
    }

    /** Çekirdek hasarının işlediği yarıçap: huni tabanı + 1,5 blok. */
    public double hasarYaricapi() {
        return tabanYaricapi() + 1.5;
    }

    /** Huni yüzeyinin çekirdek açısal hızı (rad/sn): teğet hız 6 + 3·seviye blok/sn, taban yarıçapında. */
    public double cekirdekAcisalHizi() {
        return (6 + 3.0 * seviye) / tabanYaricapi();
    }

    public RankineProfili rankine() {
        return new RankineProfili(cekirdekAcisalHizi(), tabanYaricapi());
    }

    /** Olgun evrenin süresi (tick). */
    public long aktifSureTick() {
        return aktifSureSn * 20L;
    }
}
