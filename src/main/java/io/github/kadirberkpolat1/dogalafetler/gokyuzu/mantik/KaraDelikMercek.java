package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Schwarzschild kara deliğinin ince-mercek yaklaşımı (pulsar-2 tur 4, şef kararı B). Uzunluklar Schwarzschild yarıçapı
 * (rs) biriminde, merceğin düzleminde; gözlemci çok uzakta. Kaynak merceğin {@code derinlik} rs arkasındaysa Einstein
 * yarıçapı θE = √(2·derinlik); kaynak açısı β için iki görüntü θ± = (β ± √(β² + 4θE²)) / 2: artı kaynakla aynı
 * yanda, eksi karşı yanda. θ+/θE sınıf yüklenirken bir kez hesaplanan sapma tablosundan okunur (çizim karesinde kök
 * alınmaz); θ− = θ+ − β. Eğik ince diskin arka yarısı böylece gölgenin üstünde ve altında iki yay olarak görünür.
 * Minecraft sınıfı kullanmaz.
 */
public final class KaraDelikMercek {
    /** Gölge yarıçapı (√27/2 rs ≈ 2,598). */
    public static final float GOLGE_RS = 2.6f;
    /** İnce foton halkası: gölgenin hemen dışı. */
    public static final float FOTON_RS = 2.65f;
    /** Akresyon diskinin iç kenarı (en iç kararlı yörünge, 3 rs) ve dış kenarı. */
    public static final float DISK_IC_RS = 3f;
    public static final float DISK_DIS_RS = 10f;
    /** Uzak yıldızların Einstein halkası, disk dış yarıçapına oranla (PL-27 silüetindeki halkayla aynı). */
    public static final float YILDIZ_EINSTEIN = 1.45f;
    /** Bükülen yıldız alanının kaynak düzlemindeki yarıçapı, disk dış yarıçapına oranla. */
    public static final float YILDIZ_ALANI = 2.5f;
    public static final float DOPPLER_YAKLASAN = 1f;
    public static final float DOPPLER_UZAKLASAN = 0.3f;

    /** Sapma tablosu: u = β/θE ∈ [0, {@value #TABLO_U_EN}], {@value #TABLO_ADIM} adım (doğrusal ara değerde hata < 2·10⁻⁶). */
    static final float TABLO_U_EN = 16f;
    static final int TABLO_ADIM = 2048;
    private static final float[] ARTI = new float[TABLO_ADIM + 1];

    static {
        for (int i = 0; i <= TABLO_ADIM; i++) {
            ARTI[i] = (float) kesinArti((double) i * TABLO_U_EN / TABLO_ADIM);
        }
    }

    private KaraDelikMercek() {
    }

    private static double kesinArti(double u) {
        return (u + Math.sqrt(u * u + 4.0)) / 2.0;
    }

    /** θ+/θE, u = β/θE için (tablodan; tablonun dışında kesin formül). */
    public static float artiOrani(float u) {
        float a = Math.abs(u);
        if (a >= TABLO_U_EN) {
            return (float) kesinArti(a);
        }
        float k = a / TABLO_U_EN * TABLO_ADIM;
        int i = (int) k;
        return ARTI[i] + (ARTI[i + 1] - ARTI[i]) * (k - i);
    }

    /** Kaynak merceğin {@code derinlik} rs arkasındayken Einstein yarıçapı (rs); önündeki kaynak merceklenmez (0). */
    public static float einsteinYaricapi(float derinlik) {
        return (float) Math.sqrt(2.0 * Math.max(0f, derinlik));
    }

    /** Ana görüntünün merkeze uzaklığı θ+ (kaynakla aynı yanda); β ≥ 0. */
    public static float goruntuArti(float beta, float thetaE) {
        if (thetaE <= 1e-6f) {
            return beta;
        }
        return thetaE * artiOrani(beta / thetaE);
    }

    /** İkinci görüntünün merkeze uzaklığı |θ−| (kaynağın karşı yanında); β ≥ 0. */
    public static float goruntuEksi(float beta, float thetaE) {
        return goruntuArti(beta, thetaE) - beta;
    }

    /** Büyütme μ± = (u² + 2) / (2u√(u² + 4)) ± 1/2 (u = β/θE; halkada sonsuza gitmesin diye u ≥ 0,001). */
    public static float buyutme(float u, boolean arti) {
        double a = Math.max(Math.abs(u), 1e-3);
        double m = (a * a + 2.0) / (2.0 * a * Math.sqrt(a * a + 4.0));
        return (float) (arti ? m + 0.5 : m - 0.5);
    }

    /** Görüntünün teğetsel uzaması |θ|/β (halkaya yakın yıldızlar yaya döner). */
    public static float tegetselUzama(float theta, float beta) {
        return Math.abs(theta) / Math.max(beta, 1e-4f);
    }

    /** Görüntünün radyal sıkışması dθ/dβ = θ² / (θ² + θE²). */
    public static float radyalUzama(float theta, float thetaE) {
        float t2 = theta * theta;
        return t2 / Math.max(t2 + thetaE * thetaE, 1e-8f);
    }

    /**
     * Eğik ince diskte bir noktanın görüntüsü. {@code r} rs; {@code aziDerece} disk düzleminde azimut (0 görüntü
     * düzleminde sağ, 90 merceğin tam arkası); {@code egimDerece} bakışın disk düzlemiyle açısı (gözlemci diskin
     * üstünde: arka yarı yukarıda görünür). Çıkış {x, y, derinlik, büyütme}: x sağ, y yukarı (görüntü düzleminde, rs),
     * derinlik bakış yönünde (arkası +). Ön yarı (derinlik ≤ 0) merceklenmez. {@code ikincil}: karşı yandaki ikinci
     * görüntü; ön yarıda ya da θE ≈ 0'da yoktur (false).
     */
    public static boolean diskGoruntusu(float r, float aziDerece, float egimDerece, boolean ikincil, float[] cikis) {
        double fi = Math.toRadians(aziDerece);
        double i = Math.toRadians(egimDerece);
        float x = (float) (r * Math.cos(fi));
        float y = (float) (r * Math.sin(fi) * Math.sin(i));
        float z = (float) (r * Math.sin(fi) * Math.cos(i));
        cikis[2] = z;
        float thetaE = einsteinYaricapi(z);
        if (z <= 0f || thetaE <= 1e-6f) {
            if (ikincil) {
                return false;
            }
            cikis[0] = x;
            cikis[1] = y;
            cikis[3] = 1f;
            return true;
        }
        float beta = (float) Math.sqrt(x * x + y * y);
        float bx = beta > 1e-6f ? x / beta : 0f;
        float by = beta > 1e-6f ? y / beta : 1f;
        float theta = ikincil ? -goruntuEksi(beta, thetaE) : goruntuArti(beta, thetaE);
        cikis[0] = bx * theta;
        cikis[1] = by * theta;
        cikis[3] = buyutme(beta / thetaE, !ikincil);
        return true;
    }

    /** Disk maddesinin (azimut artan yönde döner) bakış doğrultusunda bize yaklaşma kosinüsü −cos φ · cos i. */
    public static float yaklasmaCos(float aziDerece, float egimDerece) {
        return (float) (-Math.cos(Math.toRadians(aziDerece)) * Math.cos(Math.toRadians(egimDerece)));
    }

    /** Doppler parlaklık çarpanı: yaklaşan yan {@value #DOPPLER_YAKLASAN}, uzaklaşan {@value #DOPPLER_UZAKLASAN}, arası cos'la. */
    public static float doppler(float yaklasmaCos) {
        float c = Math.max(-1f, Math.min(1f, yaklasmaCos));
        return DOPPLER_UZAKLASAN + (DOPPLER_YAKLASAN - DOPPLER_UZAKLASAN) * (c + 1f) * 0.5f;
    }
}
