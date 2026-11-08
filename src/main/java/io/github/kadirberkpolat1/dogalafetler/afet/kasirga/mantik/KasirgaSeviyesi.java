package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/**
 * Kasırga seviye tablosu (madde 10, tasarım notu §6) ve türetilmiş boyutlar. Uzunluklar blok, hız blok/sn, süre sn,
 * hasar can/sn. Bütün boyutlar seviyenin {@link #guc} katsayısıyla doğru orantılıdır (tur 3, AF3-05): taban/tepe çapı,
 * yükseklik, çekim yarıçapı, etek yüksekliği güç 1 boyutunun {@code guc} katıdır; yıkım yarıçapı, etek, duvar bulutu,
 * kaldırma tavanı ve uçan blok yüksekliği bunlardan türer. Huninin biçimi (yükseklik / çap) bu yüzden her seviyede aynıdır.
 *
 * @param guc            seviye gücü: boyutların ve çekim kuvvetinin ortak katsayısı (S1 1 → S5 4,8)
 * @param tepeCapi       huni gövdesinin tepe çapı; yoğuşma hunisi yukarı doğru genişler
 * @param enkazYuksekligi taban enkaz eteğinin yüksekliği (S1 3,5 → S5 16,8)
 * @param altBoslukOrani görünen huninin yerden başladığı yükseklik / huni yüksekliği (S1 yere tam değmez)
 * @param zeminKazar     toprak/kum/taş zemin bloklarını da kaldırır (yalnız S4-5: EF4-5 zemini sıyırır)
 */
public record KasirgaSeviyesi(int seviye, double guc, double tabanCapi, double tepeCapi, double yukseklik, double hiz,
        int aktifSureSn, double cekimYaricapi, int blokKaldirmaTick, double sertlikSiniri, double cekirdekHasari,
        double enkazYuksekligi, double altBoslukOrani, boolean zeminKazar) {

    /** Güç 1 boyutları (blok); seviye satırı bunların {@code guc} katıdır. */
    static final double TABAN_CAPI = 10;
    static final double TEPE_CAPI = 16;
    static final double YUKSEKLIK = 30;
    static final double CEKIM_YARICAPI = 15;
    static final double ENKAZ_YUKSEKLIGI = 3.5;
    /** Kaldırma tavanı / huni yüksekliği: config'in S5 varsayılanı {@value #S5_KALDIRMA_TAVANI} blok bundan gelir. */
    static final double KALDIRMA_ORANI = 0.25;
    /** {@code kasirga.kaldirmaTavani} varsayılanı: boyut çarpanı 1'de S5 hunisinin yüksekliğinin dörtte biri (144 · 0,25). */
    public static final double S5_KALDIRMA_TAVANI = 36;

    private static final KasirgaSeviyesi[] TABLO = {
        satir(1, 1.0, 2.0, 60, 2, 0.6, 1.0, 0.12, false),
        satir(2, 1.6, 2.6, 90, 4, 2.0, 1.5, 0.06, false),
        satir(3, 2.4, 3.2, 120, 8, 3.0, 2.0, 0, false),
        satir(4, 3.4, 3.8, 150, 16, 4.0, 3.0, 0, true),
        satir(5, 4.8, 4.4, 180, 32, 6.0, 6.0, 0, true),
    };

    private static KasirgaSeviyesi satir(int seviye, double guc, double hiz, int aktifSureSn, int blokKaldirmaTick,
            double sertlikSiniri, double cekirdekHasari, double altBoslukOrani, boolean zeminKazar) {
        return new KasirgaSeviyesi(seviye, guc, TABAN_CAPI * guc, TEPE_CAPI * guc, YUKSEKLIK * guc, hiz, aktifSureSn,
                CEKIM_YARICAPI * guc, blokKaldirmaTick, sertlikSiniri, cekirdekHasari, ENKAZ_YUKSEKLIGI * guc, altBoslukOrani,
                zeminKazar);
    }

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
        return new KasirgaSeviyesi(s.seviye, s.guc, s.tabanCapi * c.boyut(), s.tepeCapi * c.boyut(), s.yukseklik * c.boyut(),
                s.hiz * c.hiz(), Math.max(1, (int) Math.round(s.aktifSureSn * c.sure())), s.cekimYaricapi * c.boyut(),
                (int) Math.round(s.blokKaldirmaTick * c.blok()), s.sertlikSiniri, s.cekirdekHasari * c.hasar(),
                s.enkazYuksekligi * c.boyut(), s.altBoslukOrani, s.zeminKazar);
    }

    /**
     * Kaldırılan varlığın kasırga zemininden en çok yükseldiği yükseklik: {@code s5Tavani} (config, boyut 1'de S5) huni
     * yüksekliğiyle orantılı (boyut çarpanı dahil). Varsayılanla S1 7,5 · S3 18 · S5 36 blok.
     */
    public double kaldirmaTavani(double s5Tavani) {
        return s5Tavani * yukseklik / (YUKSEKLIK * TABLO[TABLO.length - 1].guc);
    }

    /** Gerçek ve görsel uçan bloğun yörünge yüksekliği: huni yüksekliğinin %20-55'i ({@code r} ∈ [0, 1)). */
    public double ucusYuksekligi(double r) {
        return yukseklik * (0.2 + 0.35 * r);
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
