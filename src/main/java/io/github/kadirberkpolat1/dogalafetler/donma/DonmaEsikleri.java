package io.github.kadirberkpolat1.dogalafetler.donma;

import java.util.Arrays;

/**
 * Donma adımlarının eşikleri (madde 9 KADEMELİ DONMA, tasarım notu §2). Seviye sütunun etkin seviyesidir: chunk seviyesi
 * + sütuna özgü sabit titreşim (±{@code titresimGenligi}); titreşim değişimin rastgele sırayla yayılıyor gibi görünmesini
 * sağlar ama aynı sütun her zaman aynı değeri alır. Titreşim en düşük eşikten küçük olmalıdır; yoksa seviye 0'da
 * bazı sütunlar donuk kalır ve çöz(0) başlangıca dönmez.
 */
public final class DonmaEsikleri {
    public static final DonmaEsikleri VARSAYILAN =
            new DonmaEsikleri(10, new int[] {25, 55, 70, 85, 95}, 20, 20, 2, 15, 4, 8);

    private final int kiragiEsigi;
    private final int[] karEsikleri;
    private final int ilkBuzEsigi;
    private final int buzKalinlikAdimi;
    private final int kiyiKatsayisi;
    private final int kiyiUzaklikSiniri;
    private final int maksBuzKalinligi;
    private final int titresimGenligi;

    public DonmaEsikleri(int kiragiEsigi, int[] karEsikleri, int ilkBuzEsigi, int buzKalinlikAdimi, int kiyiKatsayisi,
            int kiyiUzaklikSiniri, int maksBuzKalinligi, int titresimGenligi) {
        if (karEsikleri.length == 0 || karEsikleri.length > 7) {
            throw new IllegalArgumentException("kar katmanı eşiği sayısı 1..7 olmalı");
        }
        for (int i = 1; i < karEsikleri.length; i++) {
            if (karEsikleri[i] <= karEsikleri[i - 1]) {
                throw new IllegalArgumentException("kar katmanı eşikleri artmalı: " + Arrays.toString(karEsikleri));
            }
        }
        if (buzKalinlikAdimi <= 0 || kiyiKatsayisi < 0 || kiyiUzaklikSiniri < 0 || maksBuzKalinligi < 0
                || titresimGenligi < 0) {
            throw new IllegalArgumentException("negatif ya da sıfır adım");
        }
        int enDusuk = Math.min(kiragiEsigi, Math.min(karEsikleri[0], ilkBuzEsigi));
        if (titresimGenligi >= enDusuk) {
            throw new IllegalArgumentException("titreşim (" + titresimGenligi + ") en düşük eşikten (" + enDusuk
                    + ") küçük olmalı");
        }
        this.kiragiEsigi = kiragiEsigi;
        this.karEsikleri = karEsikleri.clone();
        this.ilkBuzEsigi = ilkBuzEsigi;
        this.buzKalinlikAdimi = buzKalinlikAdimi;
        this.kiyiKatsayisi = kiyiKatsayisi;
        this.kiyiUzaklikSiniri = kiyiUzaklikSiniri;
        this.maksBuzKalinligi = maksBuzKalinligi;
        this.titresimGenligi = titresimGenligi;
    }

    /**
     * Zeminin üstündeki kar katmanı sayısı. {@code tekKat}: çimen ve misel gibi yayılan bloklar; vanilla bunların üstünde
     * 1'den fazla katman görünce rastgele tick'te toprağa çevirir ({@code SpreadingSnowyDirtBlock.canBeGrass}), bu da
     * geri dönüşsüz olurdu.
     */
    public int karKatmani(int seviye, boolean tekKat) {
        int k = 0;
        for (int esik : karEsikleri) {
            if (seviye >= esik) {
                k++;
            }
        }
        return tekKat ? Math.min(k, 1) : k;
    }

    public boolean kiragi(int seviye) {
        return seviye >= kiragiEsigi;
    }

    /** Kıyıya uzaklığı {@code kiyiUzakligi} olan su sütununda hedef buz kalınlığı (sütunun su derinliğiyle ayrıca sınırlanır). */
    public int buzKalinligi(int seviye, int kiyiUzakligi) {
        int c = Math.max(0, Math.min(kiyiUzakligi, kiyiUzaklikSiniri));
        int taban = ilkBuzEsigi + kiyiKatsayisi * c;
        if (seviye < taban) {
            return 0;
        }
        return Math.min(maksBuzKalinligi, 1 + (seviye - taban) / buzKalinlikAdimi);
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

    public int kiragiEsigi() {
        return kiragiEsigi;
    }

    public int[] karEsikleri() {
        return karEsikleri.clone();
    }

    public int ilkBuzEsigi() {
        return ilkBuzEsigi;
    }

    public int buzKalinlikAdimi() {
        return buzKalinlikAdimi;
    }

    public int kiyiKatsayisi() {
        return kiyiKatsayisi;
    }
}
