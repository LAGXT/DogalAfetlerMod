package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Ortam sıcaklığına çevre ve ekipman kaynaklı ek ısı kalemleri (hepsi °C, {@link OrtamSicakligi.Sonuc#ekle} ile eklenir):
 * <ul>
 * <li>{@link Katki#YUN_BLOKLARI}: yakındaki yün blokları ({@code güç · (1 − d/r)²}, toplamı sınırlı).</li>
 * <li>{@link Katki#LAV_ICINDE}: lavın içindeyken; yakın lav kaynağı katkısı ({@code kaynakUstSiniri}) ayrıca sürer.</li>
 * <li>{@link Katki#YAKIN_OYUNCULAR}: dip dibe duran canlı oyuncular birbirini ısıtır (diğer oyuncu sayısı × katkı, tavanlı).</li>
 * <li>{@link Katki#YUN}: yünlü zırh parçası ve taşınan yün eşya.</li>
 * <li>{@link Katki#TERMOSTAT}: termostatlı zırh parçası ve taşınan Termostat Plakası; yalnız ortam soğukken ısıtır
 * (vücudu nötre çeken termostatın soğuk yüzü), sıcakta ortamı ısıtmaz.</li>
 * <li>{@link Katki#ISITICI_ESYA}, {@link Katki#SOGUTUCU_ESYA}: kayıtlı ısıtıcı/soğutucu eşyalar; birden fazlası yığılır
 * ({@link KaynakYigilmasi}); Serin Kolye ve Soğutmalı zırh ortamı serin bir hedefe çeker ({@link #kayitliKaynaklar}).</li>
 * </ul>
 * Zırh ve eşya kaynakları çağıranın sayımıyla gelir; burada yalnız katkı ve sınır hesabı vardır.
 */
public final class EkIsi {
    private EkIsi() {
    }

    /**
     * @param yunBlokGucu              bir yün bloğunun kendi konumundaki katkısı (°C)
     * @param yunBlokYaricap           yün bloğu etki yarıçapı (blok)
     * @param yunBlokUstSiniri         yün bloklarının toplam üst sınırı (°C)
     * @param yunZirhBasina            yünlü her zırh parçasının katkısı (°C)
     * @param yunEsyaBasina            envanterdeki her yün eşya yığınının katkısı (°C; en çok 4 yığın)
     * @param termostatZirhBasina      termostatlı her zırh parçasının soğukta katkısı (°C)
     * @param termostatPlakasiBasina   envanterdeki her Termostat Plakası yığınının soğukta katkısı (°C; en çok 4 yığın)
     * @param termostatBaslangicC      bu mekan sıcaklığının (°C) altında termostat ısıtmaya başlar
     * @param termostatTamC            bu mekan sıcaklığı (°C) ve altında termostat tam güçtedir (&lt; başlangıç)
     * @param kisiselUstSiniri         bir kişisel kalemin (yün, termostat, ısıtıcı eşya) üst sınırı (°C)
     * @param kisiselAltSiniri         soğutucu eşyaların alt sınırı (°C, ≤ 0)
     * @param yakinOyuncuYaricap       oyuncuların birbirini ısıttığı uzaklık (blok)
     * @param yakinOyuncuBasina        yakındaki her diğer oyuncunun katkısı (°C)
     * @param yakinOyuncuUstSiniri     yakın oyuncu katkısının tavanı (°C)
     * @param lavIcinde                oyuncu lavın içindeyken eklenen ısı (°C)
     * @param yigilmaArtisi            yakın kaynaklarda ikinciden itibaren her ek kaynağın aynı yöndeki toplama kattığı oran
     * @param yigilmaEnCok             yığılmanın en çok çarpanı (toplam en çok bu katına çıkar)
     * @param yigilmaKarsiligi         karşı yöndeki bir kaynağın yığılma sayısından götürdüğü pay (0–1)
     */
    public record Ayarlar(double yunBlokGucu, double yunBlokYaricap, double yunBlokUstSiniri, double yunZirhBasina,
            double yunEsyaBasina, double termostatZirhBasina, double termostatPlakasiBasina,
            double termostatBaslangicC, double termostatTamC, double kisiselUstSiniri, double kisiselAltSiniri,
            double yakinOyuncuYaricap, double yakinOyuncuBasina, double yakinOyuncuUstSiniri, double lavIcinde,
            double yigilmaArtisi, double yigilmaEnCok, double yigilmaKarsiligi) {
        public static final Ayarlar VARSAYILAN = new Ayarlar(4, 3, 12, 3, 0.5, 3, 3, 18, 8, 25, -25, 2, 4, 16, 45, 0.25,
                2.0, 0.5);

        /** Yakın kaynakların ve kayıtlı ısıtıcı/soğutucu eşyaların yığılma ayarları ({@link KaynakYigilmasi}). */
        public KaynakYigilmasi.Ayarlar yigilma() {
            return new KaynakYigilmasi.Ayarlar(yigilmaArtisi, yigilmaEnCok, yigilmaKarsiligi);
        }

        public Ayarlar {
            if (termostatTamC >= termostatBaslangicC) {
                termostatTamC = termostatBaslangicC - 1;
            }
        }
    }

    /** En çok sayılan yün eşya / Termostat Plakası yığını. */
    public static final int EN_COK_ESYA_YIGINI = 4;

    /** Bir yün bloğunun {@code mesafe} bloktaki katkısı. */
    public static double yunBlogu(double mesafe, Ayarlar a) {
        return OrtamSicakligi.kaynakKatkisi(a.yunBlokGucu(), mesafe, a.yunBlokYaricap());
    }

    /** Yün bloklarının sınırlanmamış toplamından kalem değeri. */
    public static double yunBloklari(double toplam, Ayarlar a) {
        return Math.max(0, Math.min(a.yunBlokUstSiniri(), toplam));
    }

    /** Yünlü zırh parçaları ve yün eşya yığınları (sınırlı). */
    public static double yun(int yunluParca, int yunEsyaYigini, Ayarlar a) {
        double toplam = Math.max(0, yunluParca) * a.yunZirhBasina()
                + Math.min(EN_COK_ESYA_YIGINI, Math.max(0, yunEsyaYigini)) * a.yunEsyaBasina();
        return Math.min(a.kisiselUstSiniri(), toplam);
    }

    /** Termostatın ortam soğukluğuna göre gücü: başlangıç sıcaklığının üstünde 0, tam sıcaklık ve altında 1. */
    public static double termostatOrani(double mekanC, Ayarlar a) {
        double f = (a.termostatBaslangicC() - mekanC) / (a.termostatBaslangicC() - a.termostatTamC());
        return Math.max(0, Math.min(1, f));
    }

    /** Termostatlı zırh parçaları ve Termostat Plakası yığınları; {@code mekanC} ek kalemler eklenmeden önceki ortam. */
    public static double termostat(int termostatliParca, int plakaYigini, double mekanC, Ayarlar a) {
        double toplam = Math.max(0, termostatliParca) * a.termostatZirhBasina()
                + Math.min(EN_COK_ESYA_YIGINI, Math.max(0, plakaYigini)) * a.termostatPlakasiBasina();
        return Math.min(a.kisiselUstSiniri(), toplam) * termostatOrani(mekanC, a);
    }

    /**
     * Kayıtlı ısıtıcı/soğutucu kaynakların yığılmış ve kişisel sınıra kırpılmış kalemleri.
     *
     * @param isitici   {@link Katki#ISITICI_ESYA} değeri (≥ 0)
     * @param sogutucu  {@link Katki#SOGUTUCU_ESYA} değeri (≤ 0)
     * @param isiAdedi  ısıtıcı kaynak sayısı (menüde gösterilir)
     * @param soguAdedi soğutucu kaynak sayısı (sabit ve hedefe çeken)
     */
    public record Kayitli(double isitici, double sogutucu, int isiAdedi, int soguAdedi) {
    }

    /**
     * Kayıtlı ısıtıcı/soğutucu kaynakları yığar ({@link KaynakYigilmasi}: birden fazla kaynak toplamı artırır, birbirine zıt
     * olanlar yığılma paylarını kısmen götürür) ve kişisel sınıra kırpar.
     *
     * <p>Hedefe çeken soğutucular (Serin Kolye, Soğutmalı zırh) sabit güç vermez: ortam {@code hedefC}'nin üstündeyse
     * fark kadar, en çok yığılmış kapasite kadar soğutur, hedefin altına dokunmaz. {@code ortamC} bu soğutucudan önceki
     * ortamdır (model + öteki ek kalemler); ölçüm anında hesaplandığı için geri besleme ve zamanlama gecikmesi yoktur.
     *
     * @param isiToplam         ısıtıcı katkılarının toplamı (≥ 0)
     * @param isiSayisi         ısıtıcı kaynak sayısı
     * @param sabitSoguToplam   sabit soğutucu katkılarının toplamı (≤ 0)
     * @param sabitSoguSayisi   sabit soğutucu sayısı
     * @param hedefliKapasite   hedefe çeken soğutucuların toplam kapasitesi (≥ 0, °C)
     * @param hedefliSayisi     hedefe çeken soğutucu sayısı
     * @param ortamC            hedefe çeken soğutucudan önceki ortam (°C)
     * @param hedefC            hedefe çeken soğutucunun çektiği serin hedef (°C)
     */
    public static Kayitli kayitliKaynaklar(double isiToplam, int isiSayisi, double sabitSoguToplam, int sabitSoguSayisi,
            double hedefliKapasite, int hedefliSayisi, double ortamC, double hedefC, Ayarlar a) {
        int soguSayisi = Math.max(0, sabitSoguSayisi) + Math.max(0, hedefliSayisi);
        KaynakYigilmasi.Sonuc y = KaynakYigilmasi.birlestir(isiToplam, Math.max(0, isiSayisi), sabitSoguToplam, soguSayisi,
                a.yigilma());
        double isitici = Math.max(0, Math.min(a.kisiselUstSiniri(), y.isi()));
        double sabitSogu = y.soguk();
        double kapasite = Math.max(0, hedefliKapasite) * y.soguCarpani();
        double fark = Math.max(0, ortamC + isitici + sabitSogu - hedefC);
        double sogutucu = Math.max(a.kisiselAltSiniri(), sabitSogu - Math.min(kapasite, fark));
        return new Kayitli(isitici, Math.min(0, sogutucu), Math.max(0, isiSayisi), soguSayisi);
    }

    /** Bir ek kalemin dökümü: °C değerleri ve (varsa) kalemi oluşturan kaynak sayıları. */
    public record Olcum(java.util.Map<Katki, Double> katkilar, java.util.Map<Katki, Integer> adetler) {
    }

    /** {@code digerSayisi} yakın canlı diğer oyuncunun ısısı (tavanlı; kimse yoksa 0). */
    public static double yakinOyuncular(int digerSayisi, Ayarlar a) {
        return Math.min(a.yakinOyuncuUstSiniri(), Math.max(0, digerSayisi) * a.yakinOyuncuBasina());
    }

    /** Lavın içindeyken eklenen ısı, değilse 0. */
    public static double lavIcinde(boolean lavda, Ayarlar a) {
        return lavda ? a.lavIcinde() : 0;
    }
}
