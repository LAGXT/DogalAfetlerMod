package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/**
 * Kasırganın varlıklara uyguladığı hız değişimi (tick başına, blok/tick; madde 10 "çekim gücü", bölüm A "nesneleri havaya
 * çeksin"). Varlık, huni blokları gibi huninin çevresinde yörüngeye alınır: siklonik (yukarıdan bakınca saat yönünün
 * tersi; x doğu, z güney iken teğet birim vektör {@code (sin θ, -cos θ)}), merkezcil ve yukarı bileşenli bir sarmal.
 *
 * <p>Model "hedef hız"dır, ivme değil: her tick varlığın hızı, bulunduğu yerin hedef yörünge hızına {@code karisim} oranında
 * yaklaştırılır ({@code ivme = (hedef - hız) · karışım}). Böylece sonuç yerde ya da havada, sürtünmeden ve oyuncunun koşu
 * girdisinden bağımsız aynıdır; güçlü seviyede karışım 1'e yakındır ve oyuncu girdisiyle yörüngeden çıkamaz.
 * <ul>
 * <li>Çekim bölgesi ({@code d < R}): hedef içe hız {@code V₀ · (0,5 + 0,5 · (1 - d/R))}, {@code V₀ = (0,18 + 0,10 · güç) ·
 * çarpan}; teğet hız çekirdeğe doğru {@code 1/√d} ile artar ve çekirdek sınırında {@code 1,15 · V₀}'dır (S1 0,32 → S5 0,90
 * blok/tick). Güç seviyeyle büyüdüğü ve yarıçap güçle orantılı olduğu için S5 çok uzaktan ve çok sert çeker.</li>
 * <li>Çekirdekte ({@code d < çekirdek}) varlık yörünge yarıçapına oturur: {@code r₀ = çekirdek · (0,5 + 0,4 · h/tavan)}, yani
 * yükseldikçe huninin genişleyen gövdesinde döner; hiçbir yerde dışa net savurma yoktur.</li>
 * <li>Kaldırma: çekirdeğin 1,2 katı içinde hedef dikey hız {@code 0,1 · (güç - 0,6) · √(1 - h/tavan)}; yerçekimini yenemeyen
 * zayıf kaldırma (S1) uygulanmaz. Tavanda varlık asılı kalır ve dönmeye devam eder.</li>
 * <li>Fırlatma ({@code firlatma > 0}, ömrün son %13'ü): tavana yakın ({@code h ≥ 0,75 · tavan}) huni içindeki varlığa dışa ve
 * yukarı büyük hız verilir; çekirdeğin 1,6 katından uzaktakiler serbest bırakılır (çekim {@code 1 - firlatma} ile söner).</li>
 * </ul>
 */
public final class Cekim {
    /** Çekim hızının güçten bağımsız tabanı ve güç başına artışı: {@code V₀ = (TABAN_HIZ + HIZ_ARTISI · güç) · çarpan}. */
    public static final double TABAN_HIZ = 0.18;
    public static final double HIZ_ARTISI = 0.10;
    /** Çekim yarıçapının kenarındaki içe hızın çekirdek sınırındakine oranı. */
    public static final double KENAR_ORANI = 0.5;
    /** Çekirdek sınırında teğet hız / {@code V₀}. */
    public static final double TEGET_CARPANI = 1.15;
    /** Çekim yarıçapının bu payında çekim sıfırdan tam değerine açılır (kenarda ani sıçrama olmasın). */
    public static final double KENAR_SOLMA = 0.15;
    /** Hedef dikey kaldırma hızı {@code KALDIRMA_ARTISI · (güç - KALDIRMA_ESIGI)} (blok/tick). */
    public static final double KALDIRMA_ARTISI = 0.10;
    public static final double KALDIRMA_ESIGI = 0.6;
    /** Kaldırmanın uygulandığı yatay bölge: çekirdeğin bu katı (tam güç 1,2 katında). */
    public static final double KALDIRMA_BOLGESI = 2.0;
    /** Yerçekimi 0,08 ve dikey sürünme 0,98: havada bir tick'te kaybedilen dikey hız. */
    public static final double YERCEKIMI_KAYBI = 0.08 * 0.98;
    /** Fırlatma: yatay hız {@code (TABAN + ARTIS · güç) · firlatma}, dikey {@code (TABAN + ARTIS · güç)}. */
    public static final double FIRLATMA_YATAY_TABAN = 0.9;
    public static final double FIRLATMA_YATAY_ARTISI = 0.4;
    public static final double FIRLATMA_DIKEY_TABAN = 0.55;
    public static final double FIRLATMA_DIKEY_ARTISI = 0.18;
    /** Fırlatma tavanın bu payından yukarıda başlar; ramp bu eşiğin altındayken uygulanmaz. */
    public static final double FIRLATMA_TEPE = 0.75;
    public static final double FIRLATMA_ESIGI = 0.15;
    /** Fırlatmada serbest kalan bölge: çekirdeğin bu katından uzaktaki çekim söner. */
    public static final double SERBEST_BOLGE = 1.6;
    /** Vanilla yatay sürtünmeleri: yerde (blok sürtünmesi 0,6 × 0,91), havada canlı. */
    public static final double YER_SURTUNMESI = 0.6 * 0.91;
    public static final double HAVA_SURTUNMESI = 0.91;
    /** Yatay hız tavanı (blok/tick); fırlatmada {@link #EN_COK_YATAY_FIRLATMA}'ya kadar açılır. */
    public static final double EN_COK_YATAY = 1.4;
    /** Dikey hız tavanı (blok/tick); fırlatmada {@link #EN_COK_DIKEY_FIRLATMA}'ya kadar açılır. */
    public static final double EN_COK_DIKEY = 0.9;
    /** Fırlatma tavanları: sunucunun "çok hızlı hareket" denetiminin (tick başına ~10 blok) çok altında. */
    public static final double EN_COK_YATAY_FIRLATMA = 3.4;
    public static final double EN_COK_DIKEY_FIRLATMA = 2.4;

    private Cekim() {
    }

    /**
     * @param dx         varlığın kasırga eksenine göre doğu ofseti
     * @param dz         varlığın kasırga eksenine göre güney ofseti
     * @param yukseklik  varlığın kasırga zemininden yüksekliği
     * @param vx         varlığın şu anki doğu hızı
     * @param vy         varlığın şu anki dikey hızı
     * @param vz         varlığın şu anki güney hızı
     * @param s          kasırganın seviye tablosu (çekirdek, çekim yarıçapı, güç)
     * @param carpan     evre gücü × {@code cekimCarpani} (0 = etkisiz)
     * @param tavan      kaldırmanın durduğu yükseklik ({@link KasirgaSeviyesi#kaldirmaTavani})
     * @param firlatma   fırlatma rampası 0-1 × {@code cekimCarpani} (ömrün son %13'ünde artar; 0 = fırlatma yok)
     * @return {ax, ay, az} eklenecek hız; çekim yarıçapı dışında sıfır
     */
    public static double[] ivme(double dx, double dz, double yukseklik, double vx, double vy, double vz,
            KasirgaSeviyesi s, double carpan, double tavan, double firlatma) {
        double yaricap = s.cekimYaricapi();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d >= yaricap || carpan <= 0 && firlatma <= 0) {
            return new double[3];
        }
        double cekirdek = s.tabanYaricapi();
        double guc = s.guc();
        double ux = d > 1e-6 ? dx / d : 1;
        double uz = d > 1e-6 ? dz / d : 0;
        double tx = uz;
        double tz = -ux;
        double solma = Math.min(1, (yaricap - d) / (KENAR_SOLMA * yaricap));
        double fk = Math.min(1, Math.max(0, firlatma));
        boolean serbest = d >= SERBEST_BOLGE * cekirdek;
        double birak = serbest ? 1 - fk : 1;

        double v0 = (TABAN_HIZ + HIZ_ARTISI * guc) * carpan;
        double karisim = Math.min(0.9, (0.30 + 0.1 * guc) * carpan) * solma * birak;
        double tavanOrani = tavan > 0 ? Math.min(1, Math.max(0, yukseklik) / tavan) : 1;
        double yorunge = cekirdek * (0.5 + 0.4 * tavanOrani);
        double ice = v0 * (KENAR_ORANI + (1 - KENAR_ORANI) * (1 - d / yaricap));
        double tegetCekirdek = TEGET_CARPANI * v0;
        double teget = d >= cekirdek ? tegetCekirdek * Math.max(0.35, Math.sqrt(cekirdek / d))
                : tegetCekirdek * Math.min(1, 0.25 + 0.75 * d / cekirdek);
        double radyal;
        if (d >= cekirdek) {
            radyal = -ice;
        } else if (d > yorunge) {
            radyal = -ice * (d - yorunge) / (cekirdek - yorunge);
        } else {
            radyal = Math.min(0.6 * v0, 0.25 * (yorunge - d));
        }
        radyal -= teget * teget / (2 * Math.max(d, 2));
        double hedefX = ux * radyal + tx * teget;
        double hedefZ = uz * radyal + tz * teget;
        double ax = (hedefX - vx) * karisim;
        double az = (hedefZ - vz) * karisim;
        double ay = 0;

        if (fk > FIRLATMA_ESIGI && tavan > 0 && d < SERBEST_BOLGE * cekirdek && yukseklik >= FIRLATMA_TEPE * tavan) {
            double yatay = (FIRLATMA_YATAY_TABAN + FIRLATMA_YATAY_ARTISI * guc) * fk;
            double dikey = (FIRLATMA_DIKEY_TABAN + FIRLATMA_DIKEY_ARTISI * guc) * fk;
            ax = (ux * yatay + tx * 0.5 * yatay - vx) * 0.6;
            az = (uz * yatay + tz * 0.5 * yatay - vz) * 0.6;
            return new double[] {ax, (dikey - vy) * 0.6, az};
        }

        double bolge = Math.min(1, Math.max(0, (KALDIRMA_BOLGESI * cekirdek - d) / (0.8 * cekirdek)));
        double dikeyKarisim = Math.min(0.9, (0.3 + 0.1 * guc) * carpan);
        if (bolge > 0 && tavan > 0 && dikeyKarisim > 0) {
            double asili = YERCEKIMI_KAYBI * (1 - dikeyKarisim) / dikeyKarisim;
            double kaldirma = Math.max(0, KALDIRMA_ARTISI * (guc - KALDIRMA_ESIGI)) * carpan * bolge
                    * (0.55 + 0.45 * (1 - Math.min(d, cekirdek) / cekirdek));
            if (kaldirma > asili) {
                double hedefY = tavanOrani < 1 ? Math.max(asili, kaldirma * Math.sqrt(1 - tavanOrani)) : asili;
                ay = Math.max(0, (hedefY - vy) * dikeyKarisim);
            }
        }
        return new double[] {ax, ay, az};
    }

    /** Hızı tavanlara kırpar (yerinde). */
    public static double[] kirp(double[] hiz) {
        return kirp(hiz, new double[3], 0);
    }

    /**
     * Çekim eklenmiş hızı tavanlara kırpar (yerinde), ama çekimden önceki hızın ({@code onceki}) büyüklüğünden aşağı değil:
     * tavanın üstünde süzülen (elytra, havai fişek) ya da itilen varlık çekim yarıçapına girince yavaşlatılmaz, çekim yalnız onu
     * daha da hızlandıramaz (denetim P2-3). {@code firlatma} (0-1) tavanları {@link #EN_COK_YATAY_FIRLATMA} ve
     * {@link #EN_COK_DIKEY_FIRLATMA}'ya doğru açar: tepeden fırlatılan varlık normal tavanla yavaşlatılmaz.
     */
    public static double[] kirp(double[] hiz, double[] onceki, double firlatma) {
        double fk = Math.min(1, Math.max(0, firlatma));
        double yatayTavan = EN_COK_YATAY + fk * (EN_COK_YATAY_FIRLATMA - EN_COK_YATAY);
        double dikeyTavan = EN_COK_DIKEY + fk * (EN_COK_DIKEY_FIRLATMA - EN_COK_DIKEY);
        double tavan = Math.max(yatayTavan, Math.sqrt(onceki[0] * onceki[0] + onceki[2] * onceki[2]));
        double yatay = Math.sqrt(hiz[0] * hiz[0] + hiz[2] * hiz[2]);
        if (yatay > tavan) {
            hiz[0] *= tavan / yatay;
            hiz[2] *= tavan / yatay;
        }
        hiz[1] = Math.max(Math.min(-3.9, onceki[1]), Math.min(Math.max(dikeyTavan, onceki[1]), hiz[1]));
        return hiz;
    }
}
