package io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik;

/**
 * Bir deprem seviyesinin parametreleri (madde 10 "Richter hissi 4.0 → 9.0"; tasarım notu deprem §6). Değerler config'ten
 * gelir ({@code DepremAyarlari}); {@link #varsayilan} N17 tablosundaki varsayılanlardır. Saf Java'dır.
 *
 * @param uzunluk yarık uzunluğu (blok)
 * @param genislik merkezdeki yarık genişliği (blok)
 * @param derinlik merkezdeki yarık derinliği (yüzeyden, blok)
 * @param sureTick sarsıntı süresi (ölçekli tick)
 * @param kameraGenligi yakındaki oyuncunun kamera titreme genliği (derece)
 * @param yikimYaricapi sağlam olmayan blokların yıkıldığı yarıçap (blok); aynı zamanda etki alanıdır
 * @param hasar yarığa 8 blok içindeki canlılara 2 saniyede bir verilen hasar (can puanı; 2 = bir kalp)
 */
public record DepremSeviyesi(int seviye, int uzunluk, int genislik, int derinlik, int sureTick, double kameraGenligi,
        int yikimYaricapi, float hasar) {
    private static final int[] UZUNLUK = {24, 48, 96, 200, 400};
    private static final int[] GENISLIK = {1, 2, 3, 5, 9};
    private static final int[] DERINLIK = {4, 8, 14, 26, 44};
    private static final int[] SURE_SANIYE = {8, 15, 25, 45, 90};
    private static final double[] GENLIK = {0.3, 0.8, 1.5, 2.8, 4.6};
    private static final int[] YARICAP = {16, 32, 48, 84, 160};
    private static final float[] HASAR = {0, 0, 0, 1.5f, 4};

    public static DepremSeviyesi varsayilan(int seviye) {
        int i = Math.max(1, Math.min(5, seviye)) - 1;
        return new DepremSeviyesi(i + 1, UZUNLUK[i], GENISLIK[i], DERINLIK[i], SURE_SANIYE[i] * 20, GENLIK[i], YARICAP[i],
                HASAR[i]);
    }

    /** Yarığın açılma hızı (blok/sn): seviyeyle 7,2'den 12'ye (tasarım notu: 6–12 blok/sn). */
    public double acilmaHizi() {
        return 6 + 1.2 * seviye;
    }

    /** Sağlam olmayan blokların yıkım yoğunluğu çarpanı: S1 "birkaç yaprak", S5 "ahşap köy çökmüş". */
    public double yikimYogunlugu() {
        return new double[] {0.15, 0.4, 0.7, 1.15, 2.0}[seviye - 1];
    }

    /** Heyelan (eğimli toprak/çakıl/kum kayması) yalnız S4–5. */
    public boolean heyelan() {
        return seviye >= 4;
    }
}
