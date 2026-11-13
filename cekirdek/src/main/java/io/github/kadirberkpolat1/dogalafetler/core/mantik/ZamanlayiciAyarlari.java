package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Afet zamanlayıcısının ayarları (madde 4, N17, K-E67). Her afetin oranı {@code gunlukAfetOrani × ağırlık / toplam ağırlık}'tır:
 * sekiz türün hepsi seçilebilirken bekleme dışındaki beklenen afet sayısı günde {@code gunlukAfetOrani}'dır (oyun günü 0'da).
 * Oyun günü ilerledikçe bölgesel afetlerin takvimi sıklaşır: {@link #gunCarpani} {@code afetSikligi} gibi oranı çarpar ve
 * yeni başlayan beklemeleri böler; {@code siklikArtisGunu}'nde {@code siklikArtisCarpani}'na varır. Dönem olaylarının (güneş
 * patlaması, ay çarpması) oranına yalnız karekökü uygulanır ({@link AfetZamanlayicisi#turGunCarpani}). Doğal afetler {@link #eszamanliSinir}
 * kadar aynı anda sürebilir. Süreler ölçekli afet saati tick'idir. Varsayılanlar config'in de varsayılanıdır (tek kaynak).
 */
public record ZamanlayiciAyarlari(double gunlukAfetOrani, long genelBeklemeTick, double siddetBeklemeCarpani,
        long ertelemeTick, long baslangicBeklemeTick, long ilkAfetGarantiTick, double siklikArtisCarpani,
        double siklikArtisGunu, int eszamanliIkiGunu, int eszamanliUcGunu, Map<AfetTuru, Integer> agirliklar,
        Map<AfetTuru, Long> turBeklemeleri) {
    public static final double VARSAYILAN_GUNLUK_ORAN = 5.5;
    public static final long VARSAYILAN_GENEL_BEKLEME = 1_500L;
    public static final double VARSAYILAN_SIDDET_CARPANI = 0.15;
    public static final long VARSAYILAN_ERTELEME = 6_000L;
    /** K-E67: yeni dünyada ilk afet 3 dakikadan önce gelmez. */
    public static final long VARSAYILAN_BASLANGIC_BEKLEMESI = 3_600L;
    /** AS-12, K-E67: dünyada henüz afet olmadıysa 8. dakikada doğal seçim garanti edilir (0: garanti yok). */
    public static final long VARSAYILAN_ILK_AFET_GARANTISI = 9_600L;
    /** K-E67: gün ilerledikçe takvimin sıklaşması; gün 0'da ×1, {@link #VARSAYILAN_SIKLIK_ARTIS_GUNU}'nde ×bu. */
    public static final double VARSAYILAN_SIKLIK_ARTIS_CARPANI = 10.0;
    public static final double VARSAYILAN_SIKLIK_ARTIS_GUNU = 30.0;
    /**
     * Ayar sürümü 4: nadir dönem olaylarının ağırlığı (sürüm 3: ay 50, güneş 35; sürüm 2: 32 ve 20) ve günlük oran birlikte arttı
     * (toplam ağırlık 685 → 750, günlük oran 5 → 5,5: altı bölgesel afetin oranı aynı kalır); yine de en nadir olanlardır.
     */
    public static final int VARSAYILAN_AY_AGIRLIGI = 90;
    public static final int VARSAYILAN_GUNES_AGIRLIGI = 60;
    /**
     * K-E80: sel sık afetlerin yarısı ağırlıkla başlar; yağmur, fırtına, tsunami ve ay çarpması olasılığını artırır
     * ({@code SelTetikleyicileri}); günlük oran aynı kaldığından diğer afetlerin payı az düşer.
     */
    public static final int VARSAYILAN_SEL_AGIRLIGI = 50;
    /** K-E67: bu günden itibaren iki, sonrakinden itibaren üç doğal afet aynı anda sürebilir. */
    public static final int VARSAYILAN_ESZAMANLI_IKI_GUNU = 15;
    public static final int VARSAYILAN_ESZAMANLI_UC_GUNU = 40;
    /** Bölgesel afetin kendi beklemesi (K-E67; eski 24 000). */
    public static final long VARSAYILAN_BOLGESEL_TUR_BEKLEMESI = 18_000L;

    public static final ZamanlayiciAyarlari VARSAYILAN = new ZamanlayiciAyarlari(VARSAYILAN_GUNLUK_ORAN,
            VARSAYILAN_GENEL_BEKLEME, VARSAYILAN_SIDDET_CARPANI, VARSAYILAN_ERTELEME, VARSAYILAN_BASLANGIC_BEKLEMESI,
            VARSAYILAN_ILK_AFET_GARANTISI, VARSAYILAN_SIKLIK_ARTIS_CARPANI, VARSAYILAN_SIKLIK_ARTIS_GUNU,
            VARSAYILAN_ESZAMANLI_IKI_GUNU, VARSAYILAN_ESZAMANLI_UC_GUNU, varsayilanAgirliklar(), varsayilanBeklemeler());

    public ZamanlayiciAyarlari {
        if (!(gunlukAfetOrani >= 0) || Double.isInfinite(gunlukAfetOrani)) {
            throw new IllegalArgumentException("gunlukAfetOrani geçersiz: " + gunlukAfetOrani);
        }
        if (!(siddetBeklemeCarpani >= 0) || Double.isInfinite(siddetBeklemeCarpani)) {
            throw new IllegalArgumentException("siddetBeklemeCarpani geçersiz: " + siddetBeklemeCarpani);
        }
        if (!(siklikArtisCarpani >= 1) || Double.isInfinite(siklikArtisCarpani)) {
            throw new IllegalArgumentException("siklikArtisCarpani geçersiz: " + siklikArtisCarpani);
        }
        if (!(siklikArtisGunu >= 0) || Double.isInfinite(siklikArtisGunu)) {
            throw new IllegalArgumentException("siklikArtisGunu geçersiz: " + siklikArtisGunu);
        }
        genelBeklemeTick = Math.max(0, genelBeklemeTick);
        ertelemeTick = Math.max(0, ertelemeTick);
        baslangicBeklemeTick = Math.max(0, baslangicBeklemeTick);
        ilkAfetGarantiTick = Math.max(0, ilkAfetGarantiTick);
        eszamanliIkiGunu = Math.max(0, eszamanliIkiGunu);
        eszamanliUcGunu = Math.max(eszamanliIkiGunu, eszamanliUcGunu);
        EnumMap<AfetTuru, Integer> a = new EnumMap<>(AfetTuru.class);
        EnumMap<AfetTuru, Long> b = new EnumMap<>(AfetTuru.class);
        for (AfetTuru tur : AfetTuru.values()) {
            a.put(tur, Math.max(0, agirliklar.getOrDefault(tur, 0)));
            b.put(tur, Math.max(0L, turBeklemeleri.getOrDefault(tur, 0L)));
        }
        agirliklar = Collections.unmodifiableMap(a);
        turBeklemeleri = Collections.unmodifiableMap(b);
    }

    /** Madde 4 nadirlik sırası: altı sık afet eşit, ay çarpması nadir, güneş patlaması en nadir. */
    public static int varsayilanAgirlik(AfetTuru tur) {
        return switch (tur) {
            case AY_CARPMASI -> VARSAYILAN_AY_AGIRLIGI;
            case GUNES_PATLAMASI -> VARSAYILAN_GUNES_AGIRLIGI;
            case SEL -> VARSAYILAN_SEL_AGIRLIGI;
            default -> 100;
        };
    }

    /** Aynı türün yeniden seçilebilmesi için beklenen süre (afet bittiğinden itibaren, gün 0'da). */
    public static long varsayilanTurBeklemesi(AfetTuru tur) {
        return switch (tur) {
            case AY_CARPMASI -> 240_000L;
            case GUNES_PATLAMASI -> 480_000L;
            default -> VARSAYILAN_BOLGESEL_TUR_BEKLEMESI;
        };
    }

    private static Map<AfetTuru, Integer> varsayilanAgirliklar() {
        EnumMap<AfetTuru, Integer> harita = new EnumMap<>(AfetTuru.class);
        for (AfetTuru tur : AfetTuru.values()) {
            harita.put(tur, varsayilanAgirlik(tur));
        }
        return harita;
    }

    private static Map<AfetTuru, Long> varsayilanBeklemeler() {
        EnumMap<AfetTuru, Long> harita = new EnumMap<>(AfetTuru.class);
        for (AfetTuru tur : AfetTuru.values()) {
            harita.put(tur, varsayilanTurBeklemesi(tur));
        }
        return harita;
    }

    public int agirlik(AfetTuru tur) {
        return agirliklar.get(tur);
    }

    public long turBekleme(AfetTuru tur) {
        return turBeklemeleri.get(tur);
    }

    public double toplamAgirlik() {
        double toplam = 0;
        for (int deger : agirliklar.values()) {
            toplam += deger;
        }
        return toplam;
    }

    /**
     * Madde 10: şiddetli afetten sonra bekleme uzar. Çarpan {@code 1 + siddetBeklemeCarpani × max(0, seviye − 2)};
     * seviyesiz sunucu geneli afetler en ağır seviye (5) sayılır.
     */
    public double siddetUzamasi(AfetTuru tur, int seviye) {
        int etkin = tur.seviyeli() ? seviye : AfetTuru.EN_YUKSEK_SEVIYE;
        return 1.0 + siddetBeklemeCarpani * Math.max(0, etkin - 2);
    }

    /**
     * K-E67: oyun gününün takvim çarpanı, gün 0'da 1'den {@code siklikArtisGunu}'nde {@code siklikArtisCarpani}'na doğrusal,
     * sonra sabit. Bölgesel afetlerin oranını çarpar, yeni başlayan genel ve bölgesel tür beklemelerini böler.
     */
    public double gunCarpani(double gun) {
        if (!(gun > 0)) {
            return 1.0;
        }
        double ilerleme = siklikArtisGunu <= 0 ? 1.0 : Math.min(1.0, gun / siklikArtisGunu);
        return 1.0 + (siklikArtisCarpani - 1.0) * ilerleme;
    }

    /** K-E67: o gün aynı anda sürebilecek doğal afet sayısı (1, 2 ya da 3). */
    public int eszamanliSinir(double gun) {
        if (gun >= eszamanliUcGunu) {
            return 3;
        }
        return gun >= eszamanliIkiGunu ? 2 : 1;
    }

    public ZamanlayiciAyarlari gunlukOranIle(double oran) {
        return new ZamanlayiciAyarlari(oran, genelBeklemeTick, siddetBeklemeCarpani, ertelemeTick, baslangicBeklemeTick,
                ilkAfetGarantiTick, siklikArtisCarpani, siklikArtisGunu, eszamanliIkiGunu, eszamanliUcGunu, agirliklar,
                turBeklemeleri);
    }

    public ZamanlayiciAyarlari genelBeklemeIle(long tick) {
        return new ZamanlayiciAyarlari(gunlukAfetOrani, tick, siddetBeklemeCarpani, ertelemeTick, baslangicBeklemeTick,
                ilkAfetGarantiTick, siklikArtisCarpani, siklikArtisGunu, eszamanliIkiGunu, eszamanliUcGunu, agirliklar,
                turBeklemeleri);
    }

    /** Bütün türlerin kendi beklemesini aynı değere çeker. */
    public ZamanlayiciAyarlari turBeklemeleriIle(long tick) {
        EnumMap<AfetTuru, Long> harita = new EnumMap<>(AfetTuru.class);
        for (AfetTuru tur : AfetTuru.values()) {
            harita.put(tur, tick);
        }
        return new ZamanlayiciAyarlari(gunlukAfetOrani, genelBeklemeTick, siddetBeklemeCarpani, ertelemeTick,
                baslangicBeklemeTick, ilkAfetGarantiTick, siklikArtisCarpani, siklikArtisGunu, eszamanliIkiGunu,
                eszamanliUcGunu, agirliklar, harita);
    }

    public ZamanlayiciAyarlari baslangicBeklemeIle(long tick) {
        return new ZamanlayiciAyarlari(gunlukAfetOrani, genelBeklemeTick, siddetBeklemeCarpani, ertelemeTick, tick,
                ilkAfetGarantiTick, siklikArtisCarpani, siklikArtisGunu, eszamanliIkiGunu, eszamanliUcGunu, agirliklar,
                turBeklemeleri);
    }

    public ZamanlayiciAyarlari ilkAfetGarantisiIle(long tick) {
        return new ZamanlayiciAyarlari(gunlukAfetOrani, genelBeklemeTick, siddetBeklemeCarpani, ertelemeTick,
                baslangicBeklemeTick, tick, siklikArtisCarpani, siklikArtisGunu, eszamanliIkiGunu, eszamanliUcGunu,
                agirliklar, turBeklemeleri);
    }
}
