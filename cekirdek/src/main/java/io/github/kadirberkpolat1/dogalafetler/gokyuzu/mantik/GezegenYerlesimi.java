package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Gezegenlerin gök çerçevesindeki yerleşimi ve görünür boyu (K-E84, kullanıcı şikâyeti: "gezegenler yan yana, dip dibe").
 * Eskiden gerçek yön, güneş karşıtı noktanın çevresindeki 40°'lik kapağa sıkıştırılıyordu; sekiz gezegen aynı küçük alanda
 * toplanıyor, kara delik ve galaksilerle iç içe giriyordu. Şimdi her gezegenin gökte kendi yuvası vardır: güneş karşıtı noktadan
 * ({@code kutup}, gece yarısı tepe) {@value #EN_AZ_KUTUP}°–{@value #EN_COK_KUTUP}° uzakta ve farklı azimutta (gece yarısı sekizi de
 * ufkun en az 19° üstünde). Yuvalar kara delik ve galaksi konumlarından ve birbirinden uzak olsun diye aranarak seçilmiştir
 * ({@code GezegenYerlesimiTest} en kötü sürüklenmede bile en az 7° boşluk arar). Gerçek yörünge hareketi korunur ama küçüktür:
 * gezegenin Dünya–Güneş çizgisine göre yanal ve boylamsal konumu (iki sayı, her biri −1…1) yuvanın çevresinde en çok
 * {@value #AZIMUT_SURUKLEME}° azimut ve {@value #KUTUP_SURUKLEME}° kutup kadar gezdirir. Saf Java'dır; Minecraft'a dokunmaz.
 */
public final class GezegenYerlesimi {
    public static final int SAYI = 8;
    /** Yuvanın çevresinde yörünge hareketinin gezdirdiği en büyük azimut ve kutup açısı (derece). */
    public static final float AZIMUT_SURUKLEME = 10f;
    public static final float KUTUP_SURUKLEME = 5f;
    /** Kutup açısının sınırları: tepeye çok yakın değil, ufka çok yakın da değil. */
    public static final float EN_AZ_KUTUP = 14f;
    public static final float EN_COK_KUTUP = 72f;
    /** Gerçek açısal çapın bu katı, karekökle sıkıştırılıp {@link #BOY_KATSAYISI} ile ölçeklenir. */
    public static final double OLCEK = 600.0;
    public static final float BOY_KATSAYISI = 2.4f;
    public static final double AU_KM = 149_597_870.7;

    /**
     * Yuvalar, {@code {kutup, azimut}} derece; sıra {@code Gezegenler.LISTE}: Merkür, Venüs, Mars, Jüpiter, Satürn, Uranüs,
     * Neptün, Plüton. Yönün tanımı {@link #yon}.
     */
    private static final float[][] YUVA = {
            {46f, 7f}, {64f, 40f}, {44f, 181f}, {24f, 273f}, {66f, 286f}, {66f, 78f}, {61f, 232f}, {26f, 79f}};

    private GezegenYerlesimi() {
    }

    public static float yuvaKutup(int gezegen) {
        return YUVA[gezegen][0];
    }

    public static float yuvaAzimut(int gezegen) {
        return YUVA[gezegen][1];
    }

    /**
     * {@code gezegen}in gök çerçevesindeki birim yönü ({@code hedef[ofset..ofset+2]}): güneş karşıtı nokta {@code −Y}'dir;
     * kutup açısı ondan, azimut {@code +X}'ten {@code +Z}'ye ölçülür.
     *
     * @param yanal gezegenin Dünya–Güneş çizgisine göre yanal konumu, {@code −1…1} (yörünge hareketi azimutu gezdirir)
     * @param boylamsal gezegenin Güneş yönündeki bileşeni, {@code −1…1} (kutup açısını gezdirir)
     */
    public static void yon(int gezegen, float yanal, float boylamsal, float[] hedef, int ofset) {
        float kutup = YUVA[gezegen][0] + KUTUP_SURUKLEME * clamp(boylamsal);
        kutup = Math.max(EN_AZ_KUTUP, Math.min(EN_COK_KUTUP, kutup));
        float azimut = YUVA[gezegen][1] + AZIMUT_SURUKLEME * clamp(yanal);
        double t = Math.toRadians(kutup);
        double a = Math.toRadians(azimut);
        hedef[ofset] = (float) (Math.sin(t) * Math.cos(a));
        hedef[ofset + 1] = (float) -Math.cos(t);
        hedef[ofset + 2] = (float) (Math.sin(t) * Math.sin(a));
    }

    private static float clamp(float v) {
        return v < -1f ? -1f : v > 1f ? 1f : v;
    }

    /**
     * Görünür açısal çap (derece). Gerçek çap ve Dünya'ya uzaklıktan çıkan açı {@link #OLCEK} ile büyütülür, karekökle sıkıştırılır:
     * yakın ve büyük gezegen belirgin büyük, uzak ve küçük olan küçük görünür; gerçekteki yüzlerce kat fark ekranda yaklaşık on
     * katın altına iner. En küçük boy uzaklıkla azalır (yakın gezegenin tabanı 2,3°, Plüton'unki 1,3°).
     */
    public static float gorunurDerece(double capKm, double mesafeAu) {
        double aci = 2.0 * Math.atan(0.5 * capKm / (mesafeAu * AU_KM));
        double ham = OLCEK * Math.toDegrees(aci);
        float taban = (float) (1.1 + 1.3 / (1.0 + mesafeAu / 8.0));
        return Math.max(taban, BOY_KATSAYISI * (float) Math.sqrt(ham));
    }
}
