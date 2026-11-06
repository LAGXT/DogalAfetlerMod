package io.github.kadirberkpolat1.dogalafetler.yangin.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.BitisNedeni;

/**
 * Ortak yangının Minecraft'a dokunmayan hesapları (madde 6, bölüm G): tick bütçesi ve TPS kısması, aktif tavan, yayılma
 * olasılığı (vanilla yanıcılık × rüzgâr × eğim × nem), yanma süresi, yağmurda sönme ve işleme önceliği.
 */
public final class YanginMantigi {
    /** Bekleyen hücrenin önceliğinin yarıya indiği süre: uzaktaki ateş de sonunda işlenir. */
    public static final int YASLANMA_TICK = 100;
    /** Vanilla {@code getFireSpreadSpeed} ölçeği: çimen/çiçek 60, yaprak/yün 30, odun 5. */
    public static final double YAYILMA_OLCEGI = 100.0;
    /** Yağan biyomda gökyüzüne kapalı (yaprak, çatı altı) ateşin sönme olasılığı çarpanı: nem yavaş işler. */
    public static final double KORUNAKLI_YAGMUR_ORANI = 0.3;
    /** Üstünde en çok bu kadar blok katı örtü (yaprak sayılmaz) olan ateş korunaklıdır; daha derini yeraltıdır. */
    public static final int KORUNAKLI_ORTU = 8;
    /** {@code alanTutustur}'un tick başına blok okuma bütçesi, hücre bütçesinin bu katı. */
    public static final int ALAN_OKUMA_KATSAYISI = 64;

    private YanginMantigi() {
    }

    /** Tick başına işlenecek hücre sayısı: config × kısma çarpanı, en az 1 (bütçe 0 ise 0). */
    public static int butce(int tickButcesi, double kismaCarpani) {
        if (tickButcesi <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.floor(tickButcesi * kismaCarpani));
    }

    /** Tavan aşıldıysa söndürülecek en eski hücre sayısı. */
    public static int tasanSayisi(int aktif, int tavan) {
        return Math.max(0, aktif - Math.max(0, tavan));
    }

    /**
     * Rüzgâr yönündeki yayılma güçlenir, karşı yöne zayıflar (yangın cephesi). Rüzgâr vektörünün boyu gücüdür (0–1,5).
     */
    public static double ruzgarKatsayisi(double ruzgarX, double ruzgarZ, int dx, int dz) {
        double guc = Math.hypot(ruzgarX, ruzgarZ);
        double yatay = Math.hypot(dx, dz);
        if (guc == 0 || yatay == 0) {
            return 1.0;
        }
        double kosinus = (ruzgarX * dx + ruzgarZ * dz) / (guc * yatay);
        return sinirla(1.0 + guc * kosinus, 0.25, 2.5);
    }

    /** Alev yukarı tırmanır (taç yangını), aşağı zor iner. */
    public static double egimKatsayisi(int dy) {
        return dy > 0 ? 1.6 : dy < 0 ? 0.5 : 1.0;
    }

    /** Nem 0 (kuru) – 1 (yağmur altında): ıslak yakıt %70'e kadar daha zor tutuşur. */
    public static double nemKatsayisi(double nem) {
        return 1.0 - 0.7 * sinirla(nem, 0, 1);
    }

    /** Komşu bloğun bir yayılma denemesinde tutuşma olasılığı; yayılma hızı 0 olan (yanmaz) blok asla tutuşmaz. */
    public static double tutusmaOlasiligi(int yayilmaHizi, double hizCarpani, double ruzgar, double egim, double nem) {
        if (yayilmaHizi <= 0 || hizCarpani <= 0) {
            return 0;
        }
        return sinirla(yayilmaHizi / YAYILMA_OLCEGI * hizCarpani * ruzgar * egim * nem, 0, 1);
    }

    /**
     * Yakıtın yanma süresi (tick): vanilla {@code getFlammability} ne kadar yüksekse o kadar çabuk tükenir. Çimen (100)
     * ~7 sn, yaprak (60) 9 sn, tahta (20) 15 sn, odun (5) ~22 sn.
     */
    public static int yanmaSuresiTick(int yanicilik) {
        return (int) Math.round(60 + 9600.0 / (Math.max(0, yanicilik) + 20));
    }

    /** Yoğunluğun (0–1) yayılma olasılığı çarpanı: tasarım notu §6 seviye tablosu ×0,5…×1,8. */
    public static double yayilmaCarpani(double yogunluk) {
        return 0.5 + 1.3 * sinirla(yogunluk, 0, 1);
    }

    /** Rüzgâr altına kıvılcım sıçramasının en uzak mesafesi (blok); 2'nin altı sıçrama yok demektir (0). */
    public static int sicramaMesafesi(double yogunluk) {
        int mesafe = (int) Math.round(12.0 * sinirla(yogunluk, 0, 1));
        return mesafe >= 2 ? mesafe : 0;
    }

    /** {@code alanTutustur}'da yüzey yakıtının tutuşma oranı: seviye 1'de de başlangıç alanı tutuşur. */
    public static double alanOrani(double yogunluk) {
        return 0.3 + 0.7 * sinirla(yogunluk, 0, 1);
    }

    /**
     * Sıçrayan kıvılcımın yatay kayması: rüzgâr yönünden {@code sapmaDerece} sapan doğrultuda {@code mesafe} blok.
     * Rüzgârsızken doğrultu yalnız sapmadır.
     */
    public static int[] sicramaKaymasi(double ruzgarX, double ruzgarZ, double sapmaDerece, int mesafe) {
        double aci = (ruzgarX == 0 && ruzgarZ == 0 ? 0 : Math.atan2(ruzgarZ, ruzgarX)) + Math.toRadians(sapmaDerece);
        return new int[] {(int) Math.round(Math.cos(aci) * mesafe), (int) Math.round(Math.sin(aci) * mesafe)};
    }

    /**
     * Yağmurdaki hücrenin bir işlenişte sönme olasılığı; yaşlı ateş daha kolay söner (vanilla 0,2 + yaş). Korunaklı
     * hücre {@link #KORUNAKLI_YAGMUR_ORANI} ile çarpılır: vanilla yalnız yağmur gören ateşi söndürür, bölüm G tüm yangını.
     */
    public static double yagmurSonmeOlasiligi(double taban, double yasOrani, boolean korunakli) {
        double olasilik = sinirla(taban + 0.3 * sinirla(yasOrani, 0, 1), 0, 1);
        return korunakli ? olasilik * KORUNAKLI_YAGMUR_ORANI : olasilik;
    }

    /**
     * Ateş konumunun yağış durumu (Y-08). {@code ortu}: konumun üstündeki en yüksek katı bloğun (yaprak hariç) üstüne
     * kadar blok sayısı; 0 ya da eksi, üstünde yalnız yaprak olduğunu söyler. Yağmur gören ateş açık; yağan biyomda üstü
     * en çok {@link #KORUNAKLI_ORTU} blokla örtülü ateş korunaklı; daha derindeki (maden, mağara) ve yağışı yağmur olmayan
     * biyomdaki ateş yağmurdan etkilenmez.
     */
    public static YagisDurumu yagisDurumu(boolean dunyadaYagmur, boolean yagmurGoruyor, boolean yagmurluBiyom, int ortu) {
        if (!dunyadaYagmur) {
            return YagisDurumu.YOK;
        }
        if (yagmurGoruyor) {
            return YagisDurumu.ACIK;
        }
        return yagmurluBiyom && ortu <= KORUNAKLI_ORTU ? YagisDurumu.KORUNAKLI : YagisDurumu.YOK;
    }

    /**
     * Afet bitiş nedeninden yangın kipi (Y-27): {@code /dogalafet stop}, {@code sifirla} ({@link BitisNedeni#KOMUT}) ve
     * ön-uyarıda iptal ateşi hemen söndürür; doğal bitiş, gamerule ve hata ateşi yanıp bitmeye bırakır. Çekirdeğin
     * {@code Afet.durdur(level, temiz)} bayrağıyla aynı anlamda değildir (çekirdekte doğal bitiş {@code true}).
     */
    public static boolean temizBitis(BitisNedeni neden) {
        return neden == BitisNedeni.KOMUT || neden == BitisNedeni.IPTAL;
    }

    /** {@code alanTutustur}'un tick başına okuma bütçesi: hücre bütçesiyle (config × TPS kısması) orantılı. */
    public static int alanOkumaButcesi(int butce) {
        return Math.max(0, butce) * ALAN_OKUMA_KATSAYISI;
    }

    /** Küçük puan önce işlenir: oyuncuya yakın önce, bekledikçe uzaktaki de öne geçer. */
    public static double oncelik(double mesafeKare, long bekleyenTick) {
        return Math.sqrt(Math.max(0, mesafeKare)) / (1.0 + Math.max(0, bekleyenTick) / (double) YASLANMA_TICK);
    }

    /** Yanıp biten yakıtın sonucu: vanilla {@code FireBlock} gibi havaya döner; blok hasarı kapalıysa dokunulmaz. */
    public static YanmaSonucu yanmaSonucu(boolean blokHasari) {
        return blokHasari ? YanmaSonucu.HAVA : YanmaSonucu.DEGISMEZ;
    }

    /** Kıvılcım sıçrama yarıçapı (blok): yoğunluk 0–1 → 1–3. */
    public static int kivilcimYaricapi(double yogunluk) {
        return 1 + (int) Math.floor(sinirla(yogunluk, 0, 1) * 2.0 + 1e-9);
    }

    /** Günün rüzgârı: oyun gününden türeyen sabit yön, güç 0,6; aynı gün aynı cephe. */
    public static double[] gununRuzgari(long oyunGunu) {
        double aci = Math.floorMod(oyunGunu * 2_654_435_761L, 360L) * Math.PI / 180.0;
        return new double[] {Math.cos(aci) * 0.6, Math.sin(aci) * 0.6};
    }

    private static double sinirla(double deger, double alt, double ust) {
        return Math.max(alt, Math.min(ust, deger));
    }

    public enum YagisDurumu {
        YOK,
        KORUNAKLI,
        ACIK
    }

    public enum YanmaSonucu {
        HAVA,
        DEGISMEZ
    }
}
