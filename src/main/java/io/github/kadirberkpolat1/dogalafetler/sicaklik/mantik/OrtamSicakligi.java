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
     */
    public record Girdi(Boyut boyut, double biyomSicakligi, boolean kurak, long gunSaati, int y, int yuzeyY,
            boolean gokyuzu, Yagis yagis, boolean suda, double kaynaklar, GunesEvresi gunesEvresi, double evreIlerlemesi,
            double donma, double kisAgirligi) {
        /** Açık havada, yüzeyde, normal dünyada bir nokta. */
        public static Girdi yuzey(double biyomSicakligi, boolean kurak, long gunSaati, int y) {
            return new Girdi(Boyut.NORMAL, biyomSicakligi, kurak, gunSaati, y, y, true, Yagis.YOK, false, 0,
                    GunesEvresi.NORMAL, 0, 0, 0);
        }

        public Girdi boyut(Boyut b) {
            return new Girdi(b, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi);
        }

        public Girdi gunSaati(long t) {
            return new Girdi(boyut, biyomSicakligi, kurak, t, y, yuzeyY, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi);
        }

        public Girdi y(int yeni) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, yeni, yuzeyY, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi);
        }

        public Girdi yuzeyY(int yeni) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yeni, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi);
        }

        public Girdi gokyuzu(boolean g) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, g, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi);
        }

        public Girdi yagis(Yagis yeni) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yeni, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi);
        }

        public Girdi suda(boolean s) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, s, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi);
        }

        public Girdi kaynaklar(double k) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, suda, k,
                    gunesEvresi, evreIlerlemesi, donma, kisAgirligi);
        }

        public Girdi evre(GunesEvresi e, double ilerleme) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, suda, kaynaklar, e,
                    ilerleme, donma, kisAgirligi);
        }

        public Girdi donma(double g) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, g, kisAgirligi);
        }

        public Girdi kisAgirligi(double w) {
            return new Girdi(boyut, biyomSicakligi, kurak, gunSaati, y, yuzeyY, gokyuzu, yagis, suda, kaynaklar,
                    gunesEvresi, evreIlerlemesi, donma, w);
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
     */
    public record Ayarlar(double biyomCarpani, double biyomOfseti, double kurakOrtalamaDususu, double gunGeceGenligi,
            double kurakGenlikCarpani, long tepeSaati, double yukseklikBaslangici, double yukseklikKatsayisi,
            double yagmur, double kar, double firtina, double gunes, double yeraltiSicakligi,
            double yeraltiGecisDerinligi, double kapaliAlanHedefi, double kapaliAlanOrani, double su,
            double kaynakUstSiniri, double kaynakAltSiniri, double nether, double end, double gunesPatlamasiArtisi,
            double golgeOrani, double yeraltiKorumasi, double supernovaKalintisi, double donmaDususu,
            double kizilCuceOfseti, double kizilCuceGunesCarpani, double kisBolgesiDususu) {
        public static final Ayarlar VARSAYILAN = new Ayarlar(20, -2, 10, 6, 3.5, 7000, 64, 0.06, -4, -7, -7, 4, 12,
                24, 16, 0.3, -6, 45, -25, 52, -6, 38, 0.45, 0.8, 15, 55, -8, 0.6, 25);
    }

    /** Hesabın sonucu: toplam °C ve kalem kalem katkılar. */
    public record Sonuc(double toplam, Map<Katki, Double> katkilar) {
        public double katki(Katki k) {
            return katkilar.getOrDefault(k, 0.0);
        }
    }

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

    public static Sonuc hesapla(Girdi g, Ayarlar a) {
        EnumMap<Katki, Double> k = new EnumMap<>(Katki.class);
        if (g.boyut() != Boyut.NORMAL) {
            k.put(Katki.BOYUT, g.boyut() == Boyut.NETHER ? a.nether() : a.end());
            sonKalemler(g, a, k);
            return sonuc(k);
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
            default -> 0.0;
        };
        k.put(Katki.GUNES_EVRESI, evreKatkisi * koruma);
        k.put(Katki.DONMA, -a.donmaDususu() * Math.max(0, Math.min(100, g.donma())) / 100.0 * koruma);
        if (evre == GunesEvresi.KIZIL_CUCE) {
            k.put(Katki.KIZIL_CUCE, a.kizilCuceOfseti() * koruma);
            k.put(Katki.KIS_BOLGESI, -a.kisBolgesiDususu() * sinirla01(g.kisAgirligi()) * koruma);
        }
        sonKalemler(g, a, k);
        return sonuc(k);
    }

    private static void sonKalemler(Girdi g, Ayarlar a, EnumMap<Katki, Double> k) {
        if (g.suda()) {
            k.put(Katki.SU, a.su());
        }
        if (g.kaynaklar() != 0) {
            k.put(Katki.KAYNAKLAR, Math.max(a.kaynakAltSiniri(), Math.min(a.kaynakUstSiniri(), g.kaynaklar())));
        }
    }

    private static Sonuc sonuc(EnumMap<Katki, Double> k) {
        return new Sonuc(toplam(k), Map.copyOf(k));
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
