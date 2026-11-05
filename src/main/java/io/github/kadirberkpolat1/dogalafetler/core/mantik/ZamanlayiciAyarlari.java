package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Afet zamanlayıcısının ayarları (madde 4, N17). Her afetin oranı {@code gunlukAfetOrani × ağırlık / toplam ağırlık}'tır:
 * sekiz türün hepsi seçilebilirken bekleme dışındaki beklenen afet sayısı günde {@code gunlukAfetOrani}'dır. Süreler ölçekli
 * afet saati tick'idir. Varsayılanlar config'in de varsayılanıdır (tek kaynak).
 */
public record ZamanlayiciAyarlari(double gunlukAfetOrani, long genelBeklemeTick, double siddetBeklemeCarpani,
        long ertelemeTick, long baslangicBeklemeTick, Map<AfetTuru, Integer> agirliklar,
        Map<AfetTuru, Long> turBeklemeleri) {
    public static final double VARSAYILAN_GUNLUK_ORAN = 0.8;
    public static final long VARSAYILAN_GENEL_BEKLEME = 12_000L;
    public static final double VARSAYILAN_SIDDET_CARPANI = 0.25;
    public static final long VARSAYILAN_ERTELEME = 6_000L;
    public static final long VARSAYILAN_BASLANGIC_BEKLEMESI = 24_000L;

    public static final ZamanlayiciAyarlari VARSAYILAN = new ZamanlayiciAyarlari(VARSAYILAN_GUNLUK_ORAN,
            VARSAYILAN_GENEL_BEKLEME, VARSAYILAN_SIDDET_CARPANI, VARSAYILAN_ERTELEME, VARSAYILAN_BASLANGIC_BEKLEMESI,
            varsayilanAgirliklar(), varsayilanBeklemeler());

    public ZamanlayiciAyarlari {
        if (!(gunlukAfetOrani >= 0) || Double.isInfinite(gunlukAfetOrani)) {
            throw new IllegalArgumentException("gunlukAfetOrani geçersiz: " + gunlukAfetOrani);
        }
        if (!(siddetBeklemeCarpani >= 0) || Double.isInfinite(siddetBeklemeCarpani)) {
            throw new IllegalArgumentException("siddetBeklemeCarpani geçersiz: " + siddetBeklemeCarpani);
        }
        genelBeklemeTick = Math.max(0, genelBeklemeTick);
        ertelemeTick = Math.max(0, ertelemeTick);
        baslangicBeklemeTick = Math.max(0, baslangicBeklemeTick);
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
            case AY_CARPMASI -> 32;
            case GUNES_PATLAMASI -> 20;
            default -> 100;
        };
    }

    /** Aynı türün yeniden seçilebilmesi için beklenen süre (afet bittiğinden itibaren). */
    public static long varsayilanTurBeklemesi(AfetTuru tur) {
        return switch (tur) {
            case AY_CARPMASI -> 240_000L;
            case GUNES_PATLAMASI -> 480_000L;
            default -> 48_000L;
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

    public ZamanlayiciAyarlari gunlukOranIle(double oran) {
        return new ZamanlayiciAyarlari(oran, genelBeklemeTick, siddetBeklemeCarpani, ertelemeTick, baslangicBeklemeTick,
                agirliklar, turBeklemeleri);
    }

    public ZamanlayiciAyarlari genelBeklemeIle(long tick) {
        return new ZamanlayiciAyarlari(gunlukAfetOrani, tick, siddetBeklemeCarpani, ertelemeTick, baslangicBeklemeTick,
                agirliklar, turBeklemeleri);
    }

    /** Bütün türlerin kendi beklemesini aynı değere çeker. */
    public ZamanlayiciAyarlari turBeklemeleriIle(long tick) {
        EnumMap<AfetTuru, Long> harita = new EnumMap<>(AfetTuru.class);
        for (AfetTuru tur : AfetTuru.values()) {
            harita.put(tur, tick);
        }
        return new ZamanlayiciAyarlari(gunlukAfetOrani, genelBeklemeTick, siddetBeklemeCarpani, ertelemeTick,
                baslangicBeklemeTick, agirliklar, harita);
    }

    public ZamanlayiciAyarlari baslangicBeklemeIle(long tick) {
        return new ZamanlayiciAyarlari(gunlukAfetOrani, genelBeklemeTick, siddetBeklemeCarpani, ertelemeTick, tick,
                agirliklar, turBeklemeleri);
    }
}
