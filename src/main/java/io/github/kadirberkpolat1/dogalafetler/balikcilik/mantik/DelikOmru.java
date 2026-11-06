package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

/**
 * Buz deliğinin ömrü (madde 15: "4–5 dakika sonra yavaşça yeniden donarak eski buz haline dönsün") ve chunk başına
 * açık delik sınırı. Delik açık aşamada {@link #ilkGecikme} kadar kalır, sonra {@link #SON_ASAMA} donma aşamasının
 * her biri {@link #asamaGecikmesi} sürer; son aşamadan sonra eski buz geri gelir. Toplam tam ömürdür.
 */
public final class DelikOmru {
    /** Donma aşaması sayısı (blok durumu 0 açık, 1–3 giderek kapanan buz zarı). */
    public static final int SON_ASAMA = 3;

    /**
     * @param enAzTick  ömrün alt sınırı
     * @param enCokTick ömrün üst sınırı (dahil)
     * @param asamaTick bir donma aşamasının istenen süresi; ömrün dörtte birini aşamaz
     */
    public record Ayarlar(int enAzTick, int enCokTick, int asamaTick) {
        public Ayarlar {
            if (enAzTick <= 0 || enCokTick < enAzTick || asamaTick <= 0) {
                throw new IllegalArgumentException("delik ömrü: 0 < enAz <= enCok, aşama > 0 olmalı (" + enAzTick + ", "
                        + enCokTick + ", " + asamaTick + ")");
            }
        }
    }

    private DelikOmru() {
    }

    /** {@code u} ∈ [0, 1) düzgün zarıyla ömür (uçlar dahil). */
    public static int omurTick(double u, Ayarlar a) {
        int aralik = a.enCokTick() - a.enAzTick() + 1;
        int ek = (int) Math.floor(Math.max(0.0, Math.min(u, Math.nextDown(1.0))) * aralik);
        return a.enAzTick() + Math.min(ek, aralik - 1);
    }

    /**
     * Bir donma aşamasının süresi: istenen süre, en kısa ömrün dörtte birini aşmaz (açık aşama en az o kadar sürer).
     * Deliğin kendi ömrüne bağlı değildir: blok ömrü saklamaz, yalnız aşamasını bilir.
     */
    public static int asamaGecikmesi(Ayarlar a) {
        return Math.max(1, Math.min(a.asamaTick(), a.enAzTick() / (SON_ASAMA + 1)));
    }

    /** Açılıştan ilk donma aşamasına kadar. */
    public static int ilkGecikme(int omurTick, Ayarlar a) {
        return Math.max(1, omurTick - SON_ASAMA * asamaGecikmesi(a));
    }

    /** Bu aşamadaki zamanlanmış tick deliği eski buza mu döndürür (yoksa bir sonraki aşamaya mı geçirir). */
    public static boolean buzaDonerMi(int asama) {
        return asama >= SON_ASAMA;
    }

    /**
     * Son aşamada delik buz yerine su mu olur: global donma ilerlemesi 0 (sıfırlanmış ya da tamamen çözülmüş dünya). Donma
     * süreci deliğin sütununu açıkken atladığı için geri gelen buz bir daha çözülmezdi (karar B-20).
     */
    public static boolean suyaDonerMi(double donmaIlerlemesi) {
        return donmaIlerlemesi <= 0;
    }

    /** Chunk'ta {@code acik} delik varken yenisi açılabilir mi (sınır 0 = hiç). */
    public static boolean acilabilirMi(int acik, int sinir) {
        return acik < sinir;
    }
}
