package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import java.util.Arrays;

/**
 * Donma adımlarının eşikleri, yönlü (histerezisli). Donarken bir öğe donma eşiğinde gelir, çözülürken çözülme eşiğinin
 * altında kalkar; çözülme eşikleri madde 9 ÇÖZÜLME sırasını verir: önce kar (55–95), sonra kırağı (tablo, 50), sonra
 * buz kıyıdan içeri ({@code max(enAz, taban − k·uzaklık)}: kıyıda 45, ortada 10).
 *
 * <p>Seviye sütunun etkin seviyesidir (sütun hedefi + sabit titreşim, ±{@code titresimGenligi}). Titreşim bütün eşiklerden
 * küçük olmalı; yoksa seviye 0'da bazı sütunlar donuk kalır ve çöz(0) başlangıca dönmez.
 */
public final class DonmaEsikleri {
    public static final DonmaEsikleri VARSAYILAN = new DonmaEsikleri(new int[] {25, 55, 70, 85, 95},
            new int[] {55, 65, 75, 85, 95}, 20, 20, 2, 15, 4, 45, 3, 10, 10, 8, 6);

    private final int[] karEsikleri;
    private final int[] karCozulmeEsikleri;
    private final int ilkBuzEsigi;
    private final int buzKalinlikAdimi;
    private final int kiyiKatsayisi;
    private final int kiyiUzaklikSiniri;
    private final int maksBuzKalinligi;
    private final int buzCozulmeTabani;
    private final int buzCozulmeKiyiKatsayisi;
    private final int buzCozulmeEnAz;
    private final int buzCozulmeAdimi;
    private final int titresimGenligi;
    private final int inceBuzPayi;

    public DonmaEsikleri(int[] karEsikleri, int[] karCozulmeEsikleri, int ilkBuzEsigi, int buzKalinlikAdimi,
            int kiyiKatsayisi, int kiyiUzaklikSiniri, int maksBuzKalinligi, int buzCozulmeTabani,
            int buzCozulmeKiyiKatsayisi, int buzCozulmeEnAz, int buzCozulmeAdimi, int titresimGenligi) {
        this(karEsikleri, karCozulmeEsikleri, ilkBuzEsigi, buzKalinlikAdimi, kiyiKatsayisi, kiyiUzaklikSiniri,
                maksBuzKalinligi, buzCozulmeTabani, buzCozulmeKiyiKatsayisi, buzCozulmeEnAz, buzCozulmeAdimi,
                titresimGenligi, 0);
    }

    /**
     * @param inceBuzPayi ilk buz katmanının ilk (donarken) ve son (çözülürken) {@code inceBuzPayi} puanında üst blok
     *        {@code ince_buz}'dur (tasarım notu §2: su → ince buz → buz); kalınlık adımlarından küçük olmalı
     */
    public DonmaEsikleri(int[] karEsikleri, int[] karCozulmeEsikleri, int ilkBuzEsigi, int buzKalinlikAdimi,
            int kiyiKatsayisi, int kiyiUzaklikSiniri, int maksBuzKalinligi, int buzCozulmeTabani,
            int buzCozulmeKiyiKatsayisi, int buzCozulmeEnAz, int buzCozulmeAdimi, int titresimGenligi, int inceBuzPayi) {
        if (karEsikleri.length == 0 || karEsikleri.length > 7 || karCozulmeEsikleri.length != karEsikleri.length) {
            throw new IllegalArgumentException("kar eşiği sayısı 1..7 olmalı ve çözülme eşikleriyle aynı");
        }
        for (int i = 0; i < karEsikleri.length; i++) {
            if (i > 0 && (karEsikleri[i] <= karEsikleri[i - 1] || karCozulmeEsikleri[i] < karCozulmeEsikleri[i - 1])) {
                throw new IllegalArgumentException("kar eşikleri artmalı: " + Arrays.toString(karEsikleri) + " / "
                        + Arrays.toString(karCozulmeEsikleri));
            }
            if (karCozulmeEsikleri[i] < karEsikleri[i]) {
                throw new IllegalArgumentException("çözülme eşiği donma eşiğinden küçük: katman " + (i + 1));
            }
        }
        if (buzKalinlikAdimi <= 0 || buzCozulmeAdimi <= 0 || kiyiKatsayisi < 0 || buzCozulmeKiyiKatsayisi < 0
                || kiyiUzaklikSiniri < 0 || maksBuzKalinligi < 0 || titresimGenligi < 0
                || buzCozulmeEnAz > buzCozulmeTabani || inceBuzPayi < 0 || inceBuzPayi >= buzKalinlikAdimi
                || inceBuzPayi >= buzCozulmeAdimi) {
            throw new IllegalArgumentException("geçersiz adım ya da sınır");
        }
        int enDusuk = Math.min(Math.min(karEsikleri[0], karCozulmeEsikleri[0]), Math.min(ilkBuzEsigi, buzCozulmeEnAz));
        if (titresimGenligi >= enDusuk) {
            throw new IllegalArgumentException("titreşim (" + titresimGenligi + ") en düşük eşikten (" + enDusuk
                    + ") küçük olmalı");
        }
        this.karEsikleri = karEsikleri.clone();
        this.karCozulmeEsikleri = karCozulmeEsikleri.clone();
        this.ilkBuzEsigi = ilkBuzEsigi;
        this.buzKalinlikAdimi = buzKalinlikAdimi;
        this.kiyiKatsayisi = kiyiKatsayisi;
        this.kiyiUzaklikSiniri = kiyiUzaklikSiniri;
        this.maksBuzKalinligi = maksBuzKalinligi;
        this.buzCozulmeTabani = buzCozulmeTabani;
        this.buzCozulmeKiyiKatsayisi = buzCozulmeKiyiKatsayisi;
        this.buzCozulmeEnAz = buzCozulmeEnAz;
        this.buzCozulmeAdimi = buzCozulmeAdimi;
        this.titresimGenligi = titresimGenligi;
        this.inceBuzPayi = inceBuzPayi;
    }

    /** Aynı eşikler, titreşimsiz (deterministik GameTest sahneleri için). */
    public DonmaEsikleri titresimsiz() {
        return new DonmaEsikleri(karEsikleri, karCozulmeEsikleri, ilkBuzEsigi, buzKalinlikAdimi, kiyiKatsayisi,
                kiyiUzaklikSiniri, maksBuzKalinligi, buzCozulmeTabani, buzCozulmeKiyiKatsayisi, buzCozulmeEnAz,
                buzCozulmeAdimi, 0, inceBuzPayi);
    }

    /**
     * Donma yönünde zeminin üstündeki kar katmanı sayısı. {@code tekKat}: çimen ve misel gibi yayılan bloklar; vanilla
     * bunların üstünde 1'den fazla katman görünce rastgele tick'te toprağa çevirir ({@code SpreadingSnowyDirtBlock.canBeGrass}).
     */
    public int karKatmani(int seviye, boolean tekKat) {
        return katman(karEsikleri, seviye, tekKat);
    }

    /** Çözülme yönünde kalan kar katmanı sayısı. */
    public int karKatmaniCozulme(int seviye, boolean tekKat) {
        return katman(karCozulmeEsikleri, seviye, tekKat);
    }

    private static int katman(int[] esikler, int seviye, boolean tekKat) {
        int k = 0;
        for (int esik : esikler) {
            if (seviye >= esik) {
                k++;
            }
        }
        return tekKat ? Math.min(k, 1) : k;
    }

    private int uzaklik(int kiyiUzakligi) {
        return Math.max(0, Math.min(kiyiUzakligi, kiyiUzaklikSiniri));
    }

    /** Donma yönünde buz kalınlığı: kıyıda önce ve kalın (sütunun su derinliğiyle ayrıca sınırlanır). */
    public int buzKalinligi(int seviye, int kiyiUzakligi) {
        int taban = ilkBuzEsigi + kiyiKatsayisi * uzaklik(kiyiUzakligi);
        if (seviye < taban) {
            return 0;
        }
        return Math.min(maksBuzKalinligi, 1 + (seviye - taban) / buzKalinlikAdimi);
    }

    /** Çözülme yönünde kalan buz kalınlığı: kıyıda önce açılır, ortada en son. */
    public int buzKalinligiCozulme(int seviye, int kiyiUzakligi) {
        int ilk = Math.max(buzCozulmeEnAz, buzCozulmeTabani - buzCozulmeKiyiKatsayisi * uzaklik(kiyiUzakligi));
        if (seviye < ilk) {
            return 0;
        }
        return Math.min(maksBuzKalinligi, 1 + (seviye - ilk) / buzCozulmeAdimi);
    }

    /**
     * Donma yönünde su sütununun buz seviyesi: 0 buz yok, 1 üst blok ince buz, {@code k + 1} üstte {@code k} blok buz
     * ({@link SuSutunu#planlaSeviye}). İnce buz ilk katmanın ilk {@code inceBuzPayi} puanıdır.
     */
    public int buzSeviyesi(int seviye, int kiyiUzakligi) {
        int taban = ilkBuzEsigi + kiyiKatsayisi * uzaklik(kiyiUzakligi);
        return seviyeye(buzKalinligi(seviye, kiyiUzakligi), seviye - taban);
    }

    /** Çözülme yönünde buz seviyesi: buz → ince buz (çürük buz) → su. */
    public int buzSeviyesiCozulme(int seviye, int kiyiUzakligi) {
        int ilk = Math.max(buzCozulmeEnAz, buzCozulmeTabani - buzCozulmeKiyiKatsayisi * uzaklik(kiyiUzakligi));
        return seviyeye(buzKalinligiCozulme(seviye, kiyiUzakligi), seviye - ilk);
    }

    private int seviyeye(int kalinlik, int esikUstu) {
        if (kalinlik == 0) {
            return 0;
        }
        return kalinlik == 1 && esikUstu < inceBuzPayi ? 1 : kalinlik + 1;
    }

    /** Sütuna özgü sabit titreşim, [-genlik, genlik]. */
    public static int titresim(long seed, int x, int z, int genlik) {
        if (genlik <= 0) {
            return 0;
        }
        long h = KisMaskesi.karistir(seed ^ (x * 0x9E3779B97F4A7C15L) ^ KisMaskesi.karistir(z * 0xD1B54A32D192ED03L));
        return (int) Math.floorMod(h, 2L * genlik + 1) - genlik;
    }

    public int titresimGenligi() {
        return titresimGenligi;
    }

    public int kiyiUzaklikSiniri() {
        return kiyiUzaklikSiniri;
    }

    public int maksBuzKalinligi() {
        return maksBuzKalinligi;
    }

    public int buzCozulmeTabani() {
        return buzCozulmeTabani;
    }

    public int[] karCozulmeEsikleri() {
        return karCozulmeEsikleri.clone();
    }

    public int[] karEsikleri() {
        return karEsikleri.clone();
    }

    public int inceBuzPayi() {
        return inceBuzPayi;
    }
}
