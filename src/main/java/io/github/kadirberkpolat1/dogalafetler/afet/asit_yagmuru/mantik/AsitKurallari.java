package io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik;

import java.util.List;

/**
 * Asit yağmurunun saf kuralları (bölüm E, madde 10, tasarım notu §6): seviye tablosu, yoğunluk eğrisi, öldürmeyen hasar,
 * aşındırma hızı, oyuncu çevresindeki örnekleme bütçesi, vanilla yağmurunun tutulması ve blok sınıfının dönüşümü.
 * Minecraft sınıflarına dokunmaz.
 */
public final class AsitKurallari {
    /** AKTIF başında yağmurun tam yoğunluğa çıktığı süre (10 sn). */
    public static final int GIRIS_TICK = 200;
    /** AKTIF sonunda yağmurun seyreldiği süre (15 sn). */
    public static final int CIKIS_TICK = 300;
    /** Bu yoğunluğun altında (giriş ve çıkış çisesi) aşındırma yok; oyuncu maruziyeti her yoğunlukta (> 0) işler. */
    public static final double ETKI_ESIGI = 0.15;
    /** Maruziyet hasarının aralığı: bir saniye (ölçekli tick). */
    public static final int SANIYE_TICK = 20;
    /** Çatlak örtüsünün en kısa ve en uzun süresi (3–5 sn, tasarım §2 "Aşınma"). */
    public static final int CATLAK_EN_AZ = 60;
    public static final int CATLAK_EN_COK = 100;
    /** Yağmur süresi bunun altına inince vanilla yağmuru yeniden {@link #YAGMUR_SURESI_TICK} kadar açılır (AF2-02). */
    public static final int YAGMUR_YENILEME_TICK = 200;
    public static final int YAGMUR_SURESI_TICK = 1200;

    private AsitKurallari() {
    }

    /**
     * Bir seviyenin değerleri; süreler tick. {@code yaricap}: yağmur her yerde yağar, aşınma her oyuncunun bu yarıçapı içinden
     * örneklenir (AF2-05). Maruziyet hasarı seviyeden bağımsızdır ({@code hasarSaniye}).
     */
    public record Seviye(int yaricap, int sureTick, double asindirmaSaniye, boolean tasAsinir) {
    }

    /** Seviye 1–5 tablosu (config listeleri); {@code tasSeviyesi} kırıktaş ve taş tuğlanın aşınmaya başladığı seviye. */
    public record Tablo(List<Integer> yaricap, List<Integer> sureSaniye, List<Double> asindirma, int tasSeviyesi) {
        public static final Tablo VARSAYILAN = new Tablo(List.of(32, 48, 64, 96, 128), List.of(60, 90, 120, 180, 240),
                List.of(2.0, 4.0, 7.0, 11.0, 16.0), 3);

        public Seviye seviye(int seviye) {
            int s = Math.max(1, Math.min(5, seviye));
            int i = s - 1;
            return new Seviye(yaricap.get(i), sureSaniye.get(i) * 20, asindirma.get(i), s >= tasSeviyesi);
        }
    }

    /** Bir maruziyet denetiminin sonucu: sayaç (ölçekli tick) ve bu denetimde hasar verilip verilmeyeceği. */
    public record Maruziyet(int sayac, boolean hasar) {
    }

    /** Aşınabilir blok sınıfları; eşleme Minecraft tarafında ({@code AsitAsindirici.sinif}). */
    public enum Sinif {
        IGNE_YAPRAK, YAPRAK, OT, CICEK, CIMEN_BLOGU, TOPRAK, EKIN, AHSAP, YUN, KIRIKTAS, TAS_TUGLA, KALSIT, BAKIR, DIGER
    }

    /** Aşınmanın sonucu (tasarım §2 tablosu). */
    public enum Donusum {
        YOK, YOK_OLUR, OLU_CALI, TOPRAK, KABA_TOPRAK, YOSUNLU_KIRIKTAS, CATLAK_TAS_TUGLA, CAKIL, OKSITLEN
    }

    /** {@code dayanikli}: {@code dogalafetler:asit_dayanikli} etiketinde; etiket tablonun önündedir (N12). */
    public static Donusum donusum(Sinif sinif, boolean tasAsinir, boolean dayanikli) {
        if (dayanikli) {
            return Donusum.YOK;
        }
        return switch (sinif) {
            case IGNE_YAPRAK, YAPRAK, CICEK, EKIN, AHSAP, YUN -> Donusum.YOK_OLUR;
            case OT -> Donusum.OLU_CALI;
            case CIMEN_BLOGU -> Donusum.TOPRAK;
            case TOPRAK -> Donusum.KABA_TOPRAK;
            case KIRIKTAS -> tasAsinir ? Donusum.YOSUNLU_KIRIKTAS : Donusum.YOK;
            case TAS_TUGLA -> tasAsinir ? Donusum.CATLAK_TAS_TUGLA : Donusum.YOK;
            case KALSIT -> Donusum.CAKIL;
            case BAKIR -> Donusum.OKSITLEN;
            case DIGER -> Donusum.YOK;
        };
    }

    /** Aday kuyruğa girme olasılığı: iğne yapraklılar önce yapraksız kalır (Kara Orman gözlemi, tasarım §1). */
    public static double kabulOlasiligi(Sinif sinif) {
        return sinif == Sinif.YAPRAK ? 0.7 : 1.0;
    }

    /** AKTIF'te geçen ölçekli tick'e göre yağmur yoğunluğu 0–1: 10 sn'de yoğunlaşır, son 15 sn'de seyrelir. */
    public static double yogunluk(long gecen, long sure) {
        if (sure <= 0 || gecen < 0 || gecen >= sure) {
            return 0;
        }
        double giris = Math.min(1.0, gecen / (double) GIRIS_TICK);
        double cikis = Math.min(1.0, (sure - gecen) / (double) CIKIS_TICK);
        return Math.min(giris, cikis);
    }

    /**
     * Seviyenin görsel yoğunluk çarpanı (tasarım §2: S1 seyrek çise, S5 yoğun sağanak): 0,25 / 0,55 / 0,7 / 0,85 / 1. S1 doğrusal
     * dizinin dışında (AOi-2, K-E39): 0,4'te çise değil hafif yağmur görünüyordu.
     */
    public static double gorselCarpan(int seviye) {
        int s = Math.max(1, Math.min(5, seviye));
        return s == 1 ? 0.25 : 0.4 + 0.15 * (s - 1);
    }

    /** Öldürmeme garantisi: hasar canı {@code enAzCan}'ın altına indirmez; 0 dönerse hasar verilmez. */
    public static float hasarMiktari(float can, float hasar, float enAzCan) {
        return Math.max(0f, Math.min(hasar, can - enAzCan));
    }

    /**
     * Açıkta geçen {@code adim} ölçekli tick sayaca eklenir; bir saniye dolunca hasar verilir ve bir saniye düşülür. Artan
     * birikim bir saniyeyle sınırlı: {@code /dogalafet hiz} ya da gecikmiş denetim hasarı patlatmaz (denetim başına en çok bir).
     */
    public static Maruziyet maruziyet(int sayac, int adim) {
        int s = Math.max(0, sayac) + Math.max(0, adim);
        if (s < SANIYE_TICK) {
            return new Maruziyet(s, false);
        }
        return new Maruziyet(Math.min(s - SANIYE_TICK, SANIYE_TICK), true);
    }

    /** Oyuncunun başının üstü gökyüzüne açık mı: {@code MOTION_BLOCKING} yüksekliği göz bloğunun üstünde değilse. */
    public static boolean acikta(int yukseklikHaritasi, double gozY) {
        return yukseklikHaritasi <= (int) Math.floor(gozY);
    }

    /**
     * Dönüşüm birikimi: saniyede {@code hiz} blok × oyuncu sayısı × yoğunluk × TPS kısması (her oyuncunun çevresi aynı hızla
     * aşınır, AF2-05); birikim 1'i geçince bir aday kuyruğa girer. Tavan oyuncu başına 2: örneklenen sütunlarda aday bulunmayan
     * tick'lerde hız sonradan patlamaz.
     */
    public static double birikimEkle(double birikim, double hiz, int oyuncu, double yogunluk, double kisma, int adim) {
        int n = Math.max(1, oyuncu);
        return Math.min(2.0 * n, birikim + hiz * n / 20.0 * clamp(yogunluk) * Math.max(0, kisma) * Math.max(0, adim));
    }

    /** Bir oyuncunun çevresinde tick başına örneklenen yüzey sütunu: hıza göre 3–16 (tasarım §5), TPS kısmasıyla azalır, en az 1. */
    public static int ornekSayisi(double hiz, double kisma) {
        double taban = 3 + (Math.max(2, Math.min(16, hiz)) - 2) * 13.0 / 14.0;
        return Math.max(1, (int) Math.round(taban * Math.max(0, Math.min(1, kisma))));
    }

    /**
     * Tick'in örnekleme bütçesi: oyuncu başına {@link #ornekSayisi}, toplamı {@code blokButcesi} × TPS kısmasıyla sınırlı (en az 1);
     * oyuncu yoksa 0 (aşınma yalnız oyuncuların çevresindeki yüklü chunk'larda, AF2-05).
     */
    public static int ornekButcesi(double hiz, double kisma, int oyuncu, int blokButcesi) {
        if (oyuncu <= 0 || blokButcesi <= 0) {
            return 0;
        }
        int tavan = Math.max(1, (int) Math.round(blokButcesi * Math.max(0, Math.min(1, kisma))));
        return Math.min(tavan, ornekSayisi(hiz, kisma) * oyuncu);
    }

    /**
     * Vanilla havası asidin yağmurunu kesmiş mi (AF2-02): yağmıyorsa, gök gürültüsü başladıysa (asidin "zehirli sessizliği",
     * tasarım §4) ya da açık hava sayacı kurulduysa ({@code /weather clear}).
     */
    public static boolean yagmurKesildi(boolean yagiyor, boolean gokGurultusu, int acikSure) {
        return !yagiyor || gokGurultusu || acikSure > 0;
    }

    /**
     * AKTIF asit yağmurunda vanilla yağmuru yeniden açılmalı mı: kesildiyse ({@link #yagmurKesildi}) ya da yağmur süresi
     * {@link #YAGMUR_YENILEME_TICK}'in altına indiyse. Vanilla'nın yağmuru kesmesi ve {@code /weather} bu yüzden bir tick'te geri
     * alınır.
     */
    public static boolean yagmurYenilenmeli(boolean yagiyor, boolean gokGurultusu, int yagmurSuresi, int acikSure) {
        return yagmurKesildi(yagiyor, gokGurultusu, acikSure) || yagmurSuresi < YAGMUR_YENILEME_TICK;
    }

    /** Çatlak örtüsü aşaması 0–9 ({@code destroyBlockProgress}). */
    public static int catlakAsamasi(long gecen, int sure) {
        if (sure <= 0) {
            return 9;
        }
        return (int) Math.max(0, Math.min(9, gecen * 10 / sure));
    }

    private static double clamp(double t) {
        return Math.max(0, Math.min(1, t));
    }
}
