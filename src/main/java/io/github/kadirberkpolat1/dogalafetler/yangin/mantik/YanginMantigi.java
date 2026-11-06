package io.github.kadirberkpolat1.dogalafetler.yangin.mantik;

/**
 * Ortak yangının Minecraft'a dokunmayan hesapları (madde 6, bölüm G): tick bütçesi ve TPS kısması, aktif tavan, yayılma
 * olasılığı (vanilla yanıcılık × rüzgâr × eğim × nem), yanma süresi, yağmurda sönme ve işleme önceliği.
 */
public final class YanginMantigi {
    /** Bekleyen hücrenin önceliğinin yarıya indiği süre: uzaktaki ateş de sonunda işlenir. */
    public static final int YASLANMA_TICK = 100;
    /** Vanilla {@code getFireSpreadSpeed} ölçeği: çimen/çiçek 60, yaprak/yün 30, odun 5. */
    public static final double YAYILMA_OLCEGI = 100.0;

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
     * Yakıtın yanma süresi (tick): vanilla {@code getFlammability} ne kadar yüksekse o kadar çabuk tükenir. Yaprak (60)
     * ~6 sn, odun (5) ~13 sn, çimen (100) ~5 sn.
     */
    public static int yanmaSuresiTick(int yanicilik) {
        return (int) Math.round(60 + 4800.0 / (Math.max(0, yanicilik) + 20));
    }

    /** Yağmur altındaki hücrenin bir işlenişte sönme olasılığı; yaşlı ateş daha kolay söner (vanilla 0,2 + yaş). */
    public static double yagmurSonmeOlasiligi(double taban, double yasOrani) {
        return sinirla(taban + 0.3 * sinirla(yasOrani, 0, 1), 0, 1);
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

    public enum YanmaSonucu {
        HAVA,
        DEGISMEZ
    }
}
