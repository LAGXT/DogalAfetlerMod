package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import java.util.EnumMap;
import java.util.Map;

/**
 * Oyuncunun bulunduğu yerin ortam sıcaklığı (°C; madde 7a, 9, bölüm D ve G). Kalemler sırayla toplanır ve {@link Sonuc}
 * her kalemin katkısını ayrı tutar:
 * <ol>
 * <li>Yüzey: biyom ({@code 20 · biyomSıcaklığı − 2}; kurak biyomun ortalaması düşük), gün eğrisi (tepe öğleden sonra;
 * kurak biyomda geniş), yükseklik, yağış, güneş altı.</li>
 * <li>Yeraltı ve kapalı alan: gökyüzünü görmeyen noktada yüzey derinliğiyle yeraltı sabitine çekilir; sığ kapalı alan
 * daha dengeli bir değere kısmen çekilir.</li>
 * <li>Dünya dönemi (yalnız normal boyutlarda): güneş patlaması ısısı (gölgede ve derinde az), süpernova artığı, donma
 * ilerlemesiyle düşen sıcaklık, kızıl cüce ofseti ve kış bölgesi.</li>
 * <li>Nether/End sabitleri, su, yakın ısı/soğuk kaynakları (toplamı sınırlı).</li>
 * </ol>
 * Çevre ve ekipman ek kalemleri ({@link EkIsi}, {@link IsiKaynagi}) {@link Sonuc#ekle} ile, zamanla yumuşak geçiş
 * {@link #gecis} ile uygulanır; ikisi de bu hesabın dışında, sunucu tarafında çağrılır.
 */
public final class OrtamSicakligi {
    private OrtamSicakligi() {
    }

    /**
     * @param biyomSicakligi  vanilla biyom sıcaklığı ({@code Biome.getBaseTemperature()}; ova 0,8, çöl 2,0)
     * @param kurak           yağışsız sıcak biyom (çöl, savan, kanyon): gün-gece farkı büyük
     * @param gunSaati        gün saati tick'i (0–23999; 6000 öğlen)
     * @param yuzeyY          sütunun yüzey yüksekliği (heightmap); derinlik = yuzeyY − y
     * @param gokyuzu         oyuncunun başı gökyüzünü görüyor mu
     * @param kaynaklar       yakın kaynakların sınırlanmamış °C toplamı
     * @param evreIlerlemesi  güneş evresinin ilerlemesi (0–1)
     * @param donma           global donma ilerlemesi G (0–100)
     * @param kisAgirligi     kızıl cüce döneminde kalıcı kış bölgesi ağırlığı (0–1)
     * @param kaynakAdedi     {@code kaynaklar}'a katılan yakın blok kaynaklarının sayısı (ısı ve soğuk; yalnız menüde gösterilir)
     */
    public record Girdi(Boyut boyut, double biyomSicakligi, boolean kurak, long gunSaati, int y, int yuzeyY,
            boolean gokyuzu, Yagis yagis, boolean suda, double kaynaklar, GunesEvresi gunesEvresi, double evreIlerlemesi,
            double donma, double kisAgirligi, int kaynakAdedi) {
        /** Açık havada, yüzeyde, normal dünyada bir nokta. */
        public static Girdi yuzey(double biyomSicakligi, boolean kurak, long gunSaati, int y) {
            return new Girdi(Boyut.NORMAL, biyomSicakligi, kurak, gunSaati, y, y, true, Yagis.YOK, false, 0,
                    GunesEvresi.NORMAL, 0, 0, 0, 0);
        }

        public Girdi boyut(Boyut b) {
            return new Girdi(b, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi, kaynakAdedi);
        }

        public Girdi gunSaati(long t) {
            return new Girdi(boyut, biyomSicakligi, kurak, t, y, yuzeyY, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi, kaynakAdedi);
        }

        public Girdi y(int yeni) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, yeni, yuzeyY, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi, kaynakAdedi);
        }

        public Girdi yuzeyY(int yeni) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yeni, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi, kaynakAdedi);
        }

        public Girdi gokyuzu(boolean g) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, g, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi, kaynakAdedi);
        }

        public Girdi yagis(Yagis yeni) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yeni, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi, kaynakAdedi);
        }

        public Girdi suda(boolean s) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, s, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi, kaynakAdedi);
        }

        public Girdi kaynaklar(double k) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, suda, k,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi, kaynakAdedi);
        }

        public Girdi kaynakAdedi(int n) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi, n);
        }

        public Girdi evre(GunesEvresi e, double ilerleme) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, suda, kaynaklar, e,
                    ilerleme, donma, kisAgirligi, kaynakAdedi);
        }

        public Girdi donma(double g) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, g, kisAgirligi, kaynakAdedi);
        }

        public Girdi kisAgirligi(double w) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, w, kaynakAdedi);
        }
    }

    /**
     * Katsayılar (hepsi SERVER config'te; gerekçeler KARARLAR N17 tablosunda).
     *
     * @param tepeSaati            gün eğrisinin tepe tick'i (6000 öğlen; 7000 öğleden bir saat sonra)
     * @param yukseklikBaslangici  bu Y'nin üstünde blok başına {@code yukseklikKatsayisi} °C düşer
     * @param yeraltiGecisDerinligi bu kadar blok derinlikte sıcaklık tamamen yeraltı sabitine çekilir
     * @param golgeOrani           güneş patlaması ısısının gölgede kalan oranı
     * @param yeraltiKorumasi      tam derinlikte dünya dönemi etkisinin sönen oranı (0–1)
     * @param gecisSaniyesi        ortam sıcaklığının yeni hedefine üstel geçiş zaman sabiti (sn; biyom/konum değişinca
     *                             sıcaklık anında zıplamaz, {@link #gecis})
     */
    public record Ayarlar(double biyomCarpani, double biyomOfseti, double kurakOrtalamaDususu, double gunGeceGenligi,
            double kurakGenlikCarpani, long tepeSaati, double yukseklikBaslangici, double yukseklikKatsayisi,
            double yagmur, double kar, double firtina, double gunes, double yeraltiSicakligi,
            double yeraltiGecisDerinligi, double kapaliAlanHedefi, double kapaliAlanOrani, double su,
            double kaynakUstSiniri, double kaynakAltSiniri, double nether, double end, double gunesPatlamasiArtisi,
            double golgeOrani, double yeraltiKorumasi, double supernovaKalintisi, double donmaDususu,
            double kizilCuceOfseti, double kizilCuceGunesCarpani, double kisBolgesiDususu, double gecisSaniyesi) {
        public static final Ayarlar VARSAYILAN = new Ayarlar(20, -2, 10, 6, 3.5, 7000, 64, 0.06, -4, -7, -7, 4, 12,
                24, 16, 0.3, -6, 45, -25, 52, -6, 38, 0.45, 0.8, 15, 55, -8, 0.6, 25, 12);
    }

    /**
     * Hesabın sonucu: toplam °C, kalem kalem katkılar ve (varsa) kalemi oluşturan yakın kaynak sayıları.
     *
     * @param adetler yalnız menü dökümü için: kalem → yığılan kaynak sayısı (ısı kaynakları, ısıtıcı/soğutucu eşyalar); sayı toplama girmez
     */
    public record Sonuc(double toplam, Map<Katki, Double> katkilar, Map<Katki, Integer> adetler) {
        public Sonuc(double toplam, Map<Katki, Double> katkilar) {
            this(toplam, katkilar, Map.of());
        }

        public double katki(Katki k) {
            return katkilar.getOrDefault(k, 0.0);
        }

        /** Kalemin kaynak sayısı (yoksa 0). */
        public int adet(Katki k) {
            return adetler.getOrDefault(k, 0);
        }

        /**
         * Bu sonuca ek kalemler ekler (aynı kalem varsa üstüne yazar, sıfır değer kalem açmaz); toplam yeniden hesaplanır.
         * {@code EkIsi} kalemleri (yün, lav, yakın oyuncu, zırh/eşya ısısı) ve {@code IsiKaynagi} kayıtları buradan girer;
         * {@code ek.adetler()} varsa kaynak sayıları da (yalnız değeri sıfır olmayan kalemler için) eklenir.
         */
        public Sonuc ekle(EkIsi.Olcum ek) {
            EnumMap<Katki, Double> k = new EnumMap<>(Katki.class);
            k.putAll(katkilar);
            EnumMap<Katki, Integer> n = new EnumMap<>(Katki.class);
            n.putAll(adetler);
            for (Map.Entry<Katki, Double> e : ek.katkilar().entrySet()) {
                if (e.getValue() == 0) {
                    k.remove(e.getKey());
                    n.remove(e.getKey());
                } else {
                    k.put(e.getKey(), e.getValue());
                    Integer adet = ek.adetler().get(e.getKey());
                    if (adet != null && adet > 0) {
                        n.put(e.getKey(), adet);
                    } else {
                        n.remove(e.getKey());
                    }
                }
            }
            return sonuc(k, n);
        }
    }

    /**
     * Ortam sıcaklığının yumuşak geçişi: her kalem önceki değerinden hedefine üstel yaklaşır ({@code 1 − e^(−saniye/tau)}).
     * Kalemler doğrusal toplanıldığı için toplam da aynı eğriyle gider ve döküm kalemleri toplamla tutarlı kalır.
     * Biyoma ya da Nether'a girmek ortamı anında zıplatmaz; {@code saniye = 0} öncekini aynen verir.
     *
     * @param onceki   en son gösterilen sonuç ({@code null}: ilk ölçüm, hedef aynen döner)
     * @param tau      zaman sabiti (sn); 0 ya da eksi anlık geçiş demektir
     */
    public static Sonuc gecis(Sonuc onceki, Sonuc hedef, double saniye, double tau) {
        if (onceki == null || tau <= 0) {
            return hedef;
        }
        double alfa = saniye <= 0 ? 0 : 1 - Math.exp(-saniye / tau);
        EnumMap<Katki, Double> k = new EnumMap<>(Katki.class);
        for (Katki kalem : Katki.values()) {
            double p = onceki.katki(kalem);
            double h = hedef.katki(kalem);
            double v = p + (h - p) * alfa;
            if (Math.abs(v - h) < GECIS_HASSASIYETI) {
                v = h;
            }
            if (v != 0) {
                k.put(kalem, v);
            }
        }
        // Kaynak sayıları anı ölçümdür (yumuşatılmaz); yalnız dökümde görünen kalemler için taşınır.
        EnumMap<Katki, Integer> adetler = new EnumMap<>(Katki.class);
        for (Map.Entry<Katki, Integer> e : hedef.adetler().entrySet()) {
            if (k.containsKey(e.getKey())) {
                adetler.put(e.getKey(), e.getValue());
            }
        }
        return sonuc(k, adetler);
    }

    /** Geçişte hedefe bu kadar (°C) yaklaşan kalem hedefe oturur. */
    private static final double GECIS_HASSASIYETI = 0.005;

    /** Bir kaynağın {@code mesafe} bloktaki katkısı: {@code güç · (1 − d/r)²}, yarıçapın dışında 0. */
    public static double kaynakKatkisi(double guc, double mesafe, double yaricap) {
        if (yaricap <= 0 || mesafe >= yaricap) {
            return 0;
        }
        double f = 1 - Math.max(0, mesafe) / yaricap;
        return guc * f * f;
    }

    /** Gün eğrisi: tepe saatinde 1, 12 saat sonra −1. */
    public static double gunEgrisi(long gunSaati, long tepeSaati) {
        double faz = ((gunSaati - tepeSaati) % 24000 + 24000) % 24000 / 24000.0;
        return Math.cos(2 * Math.PI * faz);
    }

    /** Güneşin gökyüzünde olup olmadığı (06:00–18:00; 0 → 06:00, 12000 → 18:00), 0–1 yükseklik. */
    private static double gunesYuksekligi(long gunSaati) {
        long t = ((gunSaati % 24000) + 24000) % 24000;
        if (t >= 12000) {
            return 0;
        }
        return Math.sin(Math.PI * t / 12000.0);
    }

    /** Pulsar döneminin ısısı: {@code gunesPatlamasiArtisi}'nın katı (ayrı config anahtarı yerine; pulsar-2 PL-3 c). */
    public static final double PULSAR_ARTIS_CARPANI = 2.0;
    /** Pulsarda sudan serinlemenin payı (sıcak suyun serinletmesi yarıya iner). */
    public static final double PULSAR_SU_ORANI = 0.5;
    /** Kara delik yutmasında pulsar ısısı bu evre ilerlemesinde 0'a söner (90 sn'nin 60. saniyesi). */
    public static final double YUTMA_SONME_ILERLEMESI = 0.667;

    public static Sonuc hesapla(Girdi g, Ayarlar a) {
        EnumMap<Katki, Double> k = new EnumMap<>(Katki.class);
        if (g.boyut() != Boyut.NORMAL) {
            k.put(Katki.BOYUT, g.boyut() == Boyut.NETHER ? a.nether() : a.end());
            sonKalemler(g, a, k);
            return sonuc(k, adetler(g));
        }
        GunesEvresi evre = g.gunesEvresi() == null ? GunesEvresi.NORMAL : g.gunesEvresi();
        boolean gunesVar = evre != GunesEvresi.GUNESSIZ;
        double gunesCarpani = evre == GunesEvresi.KIZIL_CUCE ? a.kizilCuceGunesCarpani() : 1.0;

        k.put(Katki.BIYOM, a.biyomCarpani() * g.biyomSicakligi() + a.biyomOfseti()
                - (g.kurak() ? a.kurakOrtalamaDususu() : 0));
        double genlik = a.gunGeceGenligi() * (g.kurak() ? a.kurakGenlikCarpani() : 1);
        k.put(Katki.GUN_GECE, gunesVar ? genlik * gunesCarpani * gunEgrisi(g.gunSaati(), a.tepeSaati()) : -genlik);
        k.put(Katki.YUKSEKLIK, -a.yukseklikKatsayisi() * Math.max(0, g.y() - a.yukseklikBaslangici()));
        k.put(Katki.YAGIS, switch (g.yagis()) {
            case YOK -> 0.0;
            case YAGMUR -> a.yagmur();
            case KAR -> a.kar();
            case FIRTINA -> a.firtina();
        });
        double gunes = 0;
        if (gunesVar && g.gokyuzu() && g.yagis() == Yagis.YOK) {
            gunes = a.gunes() * gunesCarpani * gunesYuksekligi(g.gunSaati());
        }
        k.put(Katki.GUNES, gunes);

        double yuzey = toplam(k);
        double derinlikOrani = 0;
        if (!g.gokyuzu()) {
            double derinlik = Math.max(0, g.yuzeyY() - g.y());
            derinlikOrani = Math.min(1.0, derinlik / Math.max(1e-9, a.yeraltiGecisDerinligi()));
            double yeralti = (a.yeraltiSicakligi() - yuzey) * derinlikOrani;
            k.put(Katki.YERALTI, yeralti);
            double kapali = (a.kapaliAlanHedefi() - (yuzey + yeralti)) * a.kapaliAlanOrani() * (1 - derinlikOrani);
            k.put(Katki.KAPALI_ALAN, kapali);
        }

        double koruma = 1 - a.yeraltiKorumasi() * derinlikOrani;
        double evreKatkisi = switch (evre) {
            case BUYUYEN -> a.gunesPatlamasiArtisi() * sinirla01(g.evreIlerlemesi())
                    * (g.gokyuzu() ? 1.0 : a.golgeOrani());
            case SUPERNOVA -> a.supernovaKalintisi() * (1 - sinirla01(g.evreIlerlemesi()));
            // Pulsar: buzul kıyametinin tersi, ısınmayla güneş patlaması tepesinin 2 katı (vars. +76 °C açık havada).
            case PULSAR -> PULSAR_ARTIS_CARPANI * a.gunesPatlamasiArtisi() * sinirla01(g.evreIlerlemesi())
                    * (g.gokyuzu() ? 1.0 : a.golgeOrani());
            // Kara delik yutması: pulsarın tam ısısı parlama sonuna kadar doğrusal söner.
            case KARADELIK_YUTMA -> PULSAR_ARTIS_CARPANI * a.gunesPatlamasiArtisi()
                    * sinirla01(1 - g.evreIlerlemesi() / YUTMA_SONME_ILERLEMESI) * (g.gokyuzu() ? 1.0 : a.golgeOrani());
            default -> 0.0;
        };
        k.put(Katki.GUNES_EVRESI, evreKatkisi * koruma);
        k.put(Katki.DONMA, -a.donmaDususu() * Math.max(0, Math.min(100, g.donma())) / 100.0 * koruma);
        if (evre == GunesEvresi.KIZIL_CUCE) {
            k.put(Katki.KIZIL_CUCE, a.kizilCuceOfseti() * koruma);
            k.put(Katki.KIS_BOLGESI, -a.kisBolgesiDususu() * sinirla01(g.kisAgirligi()) * koruma);
        }
        sonKalemler(g, a, k);
        return sonuc(k, adetler(g));
    }

    private static void sonKalemler(Girdi g, Ayarlar a, EnumMap<Katki, Double> k) {
        if (g.suda()) {
            boolean pulsar = g.boyut() == Boyut.NORMAL && g.gunesEvresi() == GunesEvresi.PULSAR;
            k.put(Katki.SU, a.su() * (pulsar ? PULSAR_SU_ORANI : 1.0));
        }
        if (g.kaynaklar() != 0) {
            k.put(Katki.KAYNAKLAR, Math.max(a.kaynakAltSiniri(), Math.min(a.kaynakUstSiniri(), g.kaynaklar())));
        }
    }

    private static Sonuc sonuc(EnumMap<Katki, Double> k, Map<Katki, Integer> adetler) {
        return new Sonuc(toplam(k), Map.copyOf(k), Map.copyOf(adetler));
    }

    /** Yakın blok kaynaklarının sayısı: yalnız {@link Katki#KAYNAKLAR} satırı açıksa. */
    private static Map<Katki, Integer> adetler(Girdi g) {
        return g.kaynaklar() != 0 && g.kaynakAdedi() > 0 ? Map.of(Katki.KAYNAKLAR, g.kaynakAdedi()) : Map.of();
    }

    private static double toplam(Map<Katki, Double> k) {
        double t = 0;
        for (double d : k.values()) {
            t += d;
        }
        return t;
    }

    private static double sinirla01(double d) {
        return Math.max(0, Math.min(1, d));
    }
}
