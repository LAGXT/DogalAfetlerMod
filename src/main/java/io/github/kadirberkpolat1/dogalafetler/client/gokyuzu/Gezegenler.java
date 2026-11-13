package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GezegenYerlesimi;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Derin uzayda güneş sisteminin gezegenleri, gerçek sırayla: Merkür, Venüs, Mars, Jüpiter, Satürn (halkalı), Uranüs, Neptün ve
 * isteğe bağlı cüce Plüton (Dünya göğündeyiz, Ay yok). Her gezegen eğimli, çekirdek düzlemli dairesel yörüngede (gerçek yarı büyük
 * eksen, periyot, düğüm ve eğim; J2000 ortalama boylamdan başlar) gezer; bir Dünya yılı {@value #YIL_SN} sn'ye sıkıştırılır
 * (oyun saatinden, sarılmadan; yani periyotlar gerçek oranlı: Jupiter 11,86 yıl). Konum, gezegenin Dünya'dan görünür yönüdür
 * ({@code gezegen − Dünya}); yön, güneşin (+Y) üzerine kilitli bir gök çerçevesine (ekliptik eğikliği 23,4°) çevrilir.
 * K-E84: yerleşim artık göğün tek bir küçük kapağına sıkışmaz; her gezegen güneş karşıtı noktanın çevresinde kendi yuvasındadır
 * ve gerçek yörünge hareketi yuvanın çevresinde küçük bir sürüklenme olarak kalır ({@link GezegenYerlesimi}). Görünür açısal
 * çap gerçek çap ve Dünya'ya uzaklıktan çıkar, karekökle sıkıştırılır ({@link GezegenYerlesimi#gorunurDerece}): yakın ve büyük
 * gezegen (Jüpiter, Satürn, yaklaşan Venüs) iri, uzak ve küçük olan (Uranüs, Neptün, Plüton) küçük görünür; en küçük boy
 * uzaklıkla azalır. Kendi ekseninde dönüş süreleri de gerçek oranlıdır (Jüpiter 60 sn; Venüs, Uranüs, Plüton ters).
 *
 * <p>Her gezegen küçük bir enlem-boylam küresidir (görünür boyuta göre 10×20, 6×12 ya da 4×8 hücre; yalnız gözlemciye
 * bakan yarısı, kenarda alfa söner); ışık güneş yönünden gelir (Venüs ve Merkür evre gösterir). Halkalı gezegende
 * halkanın arka yarısı gövdeden önce, ön yarısı sonra çizilir; Jüpiter'in iki, Satürn'ün bir uydusu vardır (hepsi tek
 * çizimde). Uzaktan yakına sıralı çizilir; toplam çizim en fazla gezegen + 2 halka + 1 uydu. Normal karma ile çizilir
 * (yıldızların üstüne, onları örter); toplamalı karmayı geri kurar.
 */
final class Gezegenler {
    /** Dizin = {@link #LISTE} sırası: Merkür, Venüs, Mars (col), Jüpiter (gaz), Satürn (halkalı), Uranüs, Neptün (buz), Plüton. */
    private static final ResourceLocation[] YUZEY = {GokyuzuCizim.doku("gezegen_merkur"), GokyuzuCizim.doku("gezegen_venus"),
            GokyuzuCizim.doku("gezegen_col"), GokyuzuCizim.doku("gezegen_gaz"), GokyuzuCizim.doku("gezegen_halkali"),
            GokyuzuCizim.doku("gezegen_uranus"), GokyuzuCizim.doku("gezegen_buz"), GokyuzuCizim.doku("gezegen_pluton")};
    private static final ResourceLocation HALKA = GokyuzuCizim.doku("gezegen_halka");
    private static final ResourceLocation UYDU = GokyuzuCizim.doku("gezegen_uydu");
    private static final ResourceLocation HALE = GokyuzuCizim.doku("hale");

    /** Bir Dünya yılının gerçek saniyesi (zaman sıkıştırma). */
    static final double YIL_SN = 3600.0;
    /** Gezegen çevresinde (gövde yarıçapının katı) toplamalı hale: gezegeni yıldızlardan ayırır. */
    private static final float HALE_OLCEK = 4.2f;
    private static final float HALE_ALFA = 0.4f;
    /** Gece tarafının en az parlaklığı (eskiden 0,05: karanlık yüz diski görünmez yapıyordu). */
    private static final float EN_AZ_PARLAKLIK = 0.22f;
    /** İki gezegenin diskleri arasında en az bırakılan boşluk (derece). */
    private static final float AYIRMA_PAYI = 3.5f;
    /**
     * {@link Durum#ayir} güvenlik sınırı: yuvalar ve sürüklenme zaten {@link GezegenYerlesimi#EN_COK_KUTUP}°'nin içinde kalır; iki
     * gövde yine de çakışırsa (büyük gezegen yakınken) birbirinden itilir ve bu açının dışına çıkmaz.
     */
    private static final float KAP_DERECE = 78f;
    /** Halka dış yarıçapı / gezegen yarıçapı (halka dokusunda iç yarıçap dokunun 0,5'i). */
    private static final float HALKA_OLCEK = 2.3f;
    /** Ekliptik eğikliği: güneş yönü +Y, ekliptik düzlemi günün düzleminden bu kadar eğik. */
    private static final double EGIKLIK = Math.toRadians(23.4);
    private static final double COS_E = Math.cos(EGIKLIK);
    private static final double SIN_E = Math.sin(EGIKLIK);

    // Dünya yörüngesi (J2000 ortalama boylam, düğüm ve eğim sıfır).
    private static final double DUNYA_A = 1.00000;
    private static final double DUNYA_PERIYOT = 1.00002;
    private static final double DUNYA_L0 = Math.toRadians(100.46);

    /**
     * Bir gezegen. {@code doku}: {@link #YUZEY} dizini; {@code a}: yörünge yarı büyük ekseni (AU); {@code periyot}: yıl;
     * {@code l0}/{@code dugum}/{@code egim}: J2000 ortalama boylam, yükselen düğüm ve yörünge eğimi (radyan); {@code capKm}:
     * gerçek çap; {@code kutupEgim}: kutup ekseninin gök düzlemindeki eğimi (derece); {@code aciklik}: kutbun gözlemciye
     * dönme açısı (halka bu kadar açık görünür); {@code donusSn}: kendi ekseninde tur süresi (negatif: ters); {@code uyduSn}:
     * uydu tur süreleri.
     */
    private record Gezegen(int doku, double a, double periyot, double l0, double dugum, double egim, double capKm,
            float kutupEgim, float aciklik, double donusSn, boolean halka, double[] uyduSn) {
    }

    private static Gezegen gez(int doku, double a, double periyot, double l0Derece, double dugumDerece, double egimDerece,
            double capKm, float kutupEgim, float aciklik, double donusSn, boolean halka, double... uyduSn) {
        return new Gezegen(doku, a, periyot, Math.toRadians(l0Derece), Math.toRadians(dugumDerece),
                Math.toRadians(egimDerece), capKm, kutupEgim, aciklik, donusSn, halka, uyduSn);
    }

    /** Gerçek güneş sistemi sırası; dönüş süreleri gerçek dönüş süresi × 6,04 sn/saat (Jüpiter 9,93 sa = 60 sn). */
    private static final Gezegen[] LISTE = {
            gez(0, 0.38710, 0.24085, 252.25, 48.33, 7.00, 4879, 10f, 5f, 8500, false),
            gez(1, 0.72333, 0.61520, 181.98, 76.68, 3.39, 12104, 0f, 3f, -35000, false),
            gez(2, 1.52371, 1.88085, 355.45, 49.56, 1.85, 6779, 25f, 15f, 149, false),
            gez(3, 5.20260, 11.8620, 34.40, 100.46, 1.30, 139820, -12f, 6f, 60, false, 257, 515),
            gez(4, 9.55491, 29.4571, 49.94, 113.66, 2.49, 116460, 20f, 17f, 64, true, 2312),
            gez(5, 19.2184, 84.0205, 313.23, 74.00, 0.77, 50724, 82f, 25f, -104, false),
            gez(6, 30.1104, 164.801, 304.88, 131.78, 1.77, 49244, -28f, 14f, 97, false),
            gez(7, 39.4821, 247.94, 238.93, 110.30, 17.14, 2377, 30f, 20f, -924, false),
    };

    /** Listedeki gezegen sayısı (Plüton dahil). */
    static final int SAYI = LISTE.length;

    /** Enlem-boylam ızgarası (önceden hesaplanmış trigonometri). */
    private static final class Izgara {
        final int enlem;
        final int boylam;
        final int nokta;
        final float[] cosL;
        final float[] sinL;
        final float[] cosP;
        final float[] sinP;

        Izgara(int enlem, int boylam) {
            this.enlem = enlem;
            this.boylam = boylam;
            this.nokta = (enlem + 1) * (boylam + 1);
            cosL = new float[boylam + 1];
            sinL = new float[boylam + 1];
            cosP = new float[enlem + 1];
            sinP = new float[enlem + 1];
            for (int j = 0; j <= boylam; j++) {
                float l = Mth.TWO_PI * j / boylam;
                cosL[j] = Mth.cos(l);
                sinL[j] = Mth.sin(l);
            }
            for (int i = 0; i <= enlem; i++) {
                float p = Mth.HALF_PI - Mth.PI * i / enlem;
                cosP[i] = Mth.cos(p);
                sinP[i] = Mth.sin(p);
            }
        }
    }

    private static final Izgara BUYUK = new Izgara(10, 20);
    private static final Izgara ORTA = new Izgara(6, 12);
    private static final Izgara KUCUK = new Izgara(4, 8);

    private static final float[] X = new float[BUYUK.nokta];
    private static final float[] Y = new float[BUYUK.nokta];
    private static final float[] Z = new float[BUYUK.nokta];
    private static final float[] A = new float[BUYUK.nokta];
    private static final float[] S = new float[BUYUK.nokta];
    private static final float[] UX = new float[8];
    private static final float[] UY = new float[8];
    private static final float[] UZ = new float[8];
    private static final float[] UH = new float[8];
    private static final int[] UI = new int[8];

    private static final Durum CIZIM = new Durum();
    private static final Cerceve[] CERCEVE = new Cerceve[SAYI];
    private static final int[] SIRA = new int[SAYI];
    /** Gezegenin tüm çiziminin (hale köşegeni, halka, uydular; en genişi halkalı gezegenin haleyi) yarıçap katı olarak sınırı. */
    private static final float KAPSAM_OLCEK = HALE_OLCEK * HALKA_OLCEK * 0.6f * 1.5f;
    private static final Matrix4f KLIP = new Matrix4f();
    private static final Vector4f KLIP_V = new Vector4f();
    private static final boolean[] GORUNUR = new boolean[SAYI];

    static {
        for (int i = 0; i < SAYI; i++) {
            CERCEVE[i] = new Cerceve();
        }
    }

    /**
     * Bir andaki gök durumu: gezegenlerin gök çerçevesindeki birim yönü ({@code yon}, gezegen başına x,y,z), güneşe yönü
     * ({@code isik}), Dünya'ya uzaklık (AU) ve görünür açısal çap (derece). Çizim ve bakış algısı aynı hesabı kullanır.
     */
    static final class Durum {
        final float[] yon = new float[3 * SAYI];
        final float[] isik = new float[3 * SAYI];
        final float[] derece = new float[SAYI];
        final float[] mesafe = new float[SAYI];
        private final double[] h = new double[3];
        private final Vector3f eski = new Vector3f();
        private final Vector3f yeni = new Vector3f();
        private final Vector3f isikV = new Vector3f();
        private final Quaternionf dondur = new Quaternionf();
        private final float[] once = new float[3 * SAYI];

        /** {@code t}: oyun dünyasının yaşı (sn, sarılmaz). */
        void hesapla(double t) {
            double yil = t / YIL_SN;
            konum(DUNYA_A, DUNYA_PERIYOT, DUNYA_L0, 0.0, 0.0, yil, h);
            double ex = h[0];
            double ey = h[1];
            double ez = h[2];
            double gunes = Math.atan2(-ey, -ex);
            double cs = Math.cos(gunes);
            double ss = Math.sin(gunes);
            for (int i = 0; i < SAYI; i++) {
                Gezegen g = LISTE[i];
                konum(g.a, g.periyot, g.l0, g.dugum, g.egim, yil, h);
                double vx = h[0] - ex;
                double vy = h[1] - ey;
                double vz = h[2] - ez;
                double d = Math.sqrt(vx * vx + vy * vy + vz * vz);
                // Güneş yönü (−Dünya) +X'e dönecek şekilde ekliptik z etrafında döndür, sonra göğe eğik düzlemle taşı.
                double rx = vx * cs + vy * ss;
                double ry = -vx * ss + vy * cs;
                gok(yon, i, rx / d, ry / d, vz / d);
                double lx = -(h[0] * cs + h[1] * ss);
                double ly = -(-h[0] * ss + h[1] * cs);
                double lz = -h[2];
                double lb = Math.sqrt(lx * lx + ly * ly + lz * lz);
                gok(isik, i, lx / lb, ly / lb, lz / lb);
                derece[i] = GezegenYerlesimi.gorunurDerece(g.capKm, d);
                mesafe[i] = (float) d;
                yerles(i, (float) (ry / d), (float) (rx / d));
            }
            ayir();
        }

        /**
         * {@code i}. gezegeni kendi yuvasına yerleştirir ({@link GezegenYerlesimi}); ışık yönü aynı dönüşle taşınır (evre
         * görünümü bozulmaz). {@code yanal}/{@code boylamsal}: gerçek yörünge hareketinin yuvanın çevresindeki sürüklenmesi.
         */
        private void yerles(int i, float yanal, float boylamsal) {
            eski.set(yon[3 * i], yon[3 * i + 1], yon[3 * i + 2]);
            GezegenYerlesimi.yon(i, yanal, boylamsal, yon, 3 * i);
            yeni.set(yon[3 * i], yon[3 * i + 1], yon[3 * i + 2]);
            dondur.rotationTo(eski, yeni);
            isikV.set(isik[3 * i], isik[3 * i + 1], isik[3 * i + 2]);
            dondur.transform(isikV);
            isik[3 * i] = isikV.x;
            isik[3 * i + 1] = isikV.y;
            isik[3 * i + 2] = isikV.z;
        }

        /** {@code i}. gezegenin görünür yarıçapı (derece; halkalı gezegende halka dahil). */
        private float diskYaricap(int i) {
            return 0.5f * derece[i] * (LISTE[i].halka ? HALKA_OLCEK : 1f);
        }

        /**
         * Kapağa sıkıştırılmış yönlerde çakışan ya da çok yakın gezegen çiftlerini büyük daire boyunca birbirinden iter (diskleri
         * ve {@value #AYIRMA_PAYI}° boşluk sığana dek, en çok 16 tur) ve yönleri kapak içinde tutar. Durumsuzdur: aynı zamandan
         * aynı sonuç çıkar, bakış algısı ve çizim aynı yönü görür. Işık yönü her gezegenin yer değiştirmesiyle birlikte döner.
         */
        private void ayir() {
            System.arraycopy(yon, 0, once, 0, yon.length);
            float kap = KAP_DERECE * Mth.DEG_TO_RAD;
            for (int tur = 0; tur < 16; tur++) {
                boolean degisti = false;
                for (int i = 0; i < SAYI; i++) {
                    for (int j = i + 1; j < SAYI; j++) {
                        float gerekli = (diskYaricap(i) + diskYaricap(j) + AYIRMA_PAYI) * Mth.DEG_TO_RAD;
                        float c = Mth.clamp(yon[3 * i] * yon[3 * j] + yon[3 * i + 1] * yon[3 * j + 1]
                                + yon[3 * i + 2] * yon[3 * j + 2], -1f, 1f);
                        float aci = (float) Math.acos(c);
                        if (aci >= gerekli) {
                            continue;
                        }
                        degisti = true;
                        float adim = 0.5f * (gerekli - aci) + 1e-3f;
                        itele(i, j, c, adim);
                        itele(j, i, c, adim);
                    }
                }
                for (int i = 0; i < SAYI; i++) {
                    kapIcineAl(i, kap);
                }
                if (!degisti) {
                    break;
                }
            }
            for (int i = 0; i < SAYI; i++) {
                eski.set(once[3 * i], once[3 * i + 1], once[3 * i + 2]);
                yeni.set(yon[3 * i], yon[3 * i + 1], yon[3 * i + 2]);
                dondur.rotationTo(eski, yeni);
                isikV.set(isik[3 * i], isik[3 * i + 1], isik[3 * i + 2]);
                dondur.transform(isikV);
                isik[3 * i] = isikV.x;
                isik[3 * i + 1] = isikV.y;
                isik[3 * i + 2] = isikV.z;
            }
        }

        /** {@code i}. gezegeni {@code j}'den uzağa, büyük daire boyunca {@code adim} radyan iter ({@code c}: iki yönün iç çarpımı). */
        private void itele(int i, int j, float c, float adim) {
            float x = yon[3 * i];
            float y = yon[3 * i + 1];
            float z = yon[3 * i + 2];
            // i'deki, j'den uzağa bakan teğet: -(pj - c·pi) normalize.
            float tx = c * x - yon[3 * j];
            float ty = c * y - yon[3 * j + 1];
            float tz = c * z - yon[3 * j + 2];
            float boy = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (boy < 1e-5f) {
                // Aynı yönde: i'ye göre rastgele ama belirleyici bir dik yön (i'nin numarasına göre).
                tx = i % 2 == 0 ? 1f : -1f;
                ty = 0f;
                tz = 0f;
                float d = tx * x;
                tx -= d * x;
                ty -= d * y;
                tz -= d * z;
                boy = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
                if (boy < 1e-5f) {
                    tx = 0f;
                    ty = 0f;
                    tz = 1f;
                    d = z;
                    tx -= d * x;
                    ty -= d * y;
                    tz -= d * z;
                    boy = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
                }
            }
            float ca = Mth.cos(adim);
            float sa = Mth.sin(adim);
            float nx = ca * x + sa * tx / boy;
            float ny = ca * y + sa * ty / boy;
            float nz = ca * z + sa * tz / boy;
            float nb = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            yon[3 * i] = nx / nb;
            yon[3 * i + 1] = ny / nb;
            yon[3 * i + 2] = nz / nb;
        }

        /** {@code i}. gezegen güneş karşıtı noktadan {@code kap} radyandan uzaktaysa kapağın kenarına çeker (azimut korunur). */
        private void kapIcineAl(int i, float kap) {
            float x = yon[3 * i];
            float y = yon[3 * i + 1];
            float z = yon[3 * i + 2];
            if ((float) Math.acos(Mth.clamp(-y, -1f, 1f)) <= kap) {
                return;
            }
            float yatay = (float) Math.sqrt(x * x + z * z);
            float s = Mth.sin(kap);
            yon[3 * i] = yatay < 1e-4f ? s : x / yatay * s;
            yon[3 * i + 1] = -Mth.cos(kap);
            yon[3 * i + 2] = yatay < 1e-4f ? 0f : z / yatay * s;
        }
    }

    /** Ekliptik döndürülmüş (güneş +X, düzlem y) vektörü gök çerçevesine yazar: +X → +Y, +Y → eğik yatay, +Z → eğik normal. */
    private static void gok(float[] hedef, int i, double x, double y, double z) {
        hedef[3 * i] = (float) (-y * SIN_E + z * COS_E);
        hedef[3 * i + 1] = (float) x;
        hedef[3 * i + 2] = (float) (y * COS_E + z * SIN_E);
    }

    /** Dairesel, eğik yörüngede güneş merkezli konum (AU, ekliptik). {@code yil}: yıl; periyot oranı sarılmış faz verir. */
    private static void konum(double a, double periyot, double l0, double dugum, double egim, double yil, double[] o) {
        double l = l0 + (yil / periyot % 1.0) * (2.0 * Math.PI);
        double u = l - dugum;
        double cu = Math.cos(u);
        double su = Math.sin(u);
        double co = Math.cos(dugum);
        double so = Math.sin(dugum);
        double ci = Math.cos(egim);
        double si = Math.sin(egim);
        o[0] = a * (co * cu - so * su * ci);
        o[1] = a * (so * cu + co * su * ci);
        o[2] = a * su * si;
    }

    /** Gezegenin gök çerçevesi: konum, gözlemci eksenleri, kutup ve ışık. Her karede yeniden doldurulur (ayrılama yok). */
    private static final class Cerceve {
        final Vector3f yon = new Vector3f();
        final Vector3f z = new Vector3f();
        final Vector3f ur = new Vector3f();
        final Vector3f vr = new Vector3f();
        final Vector3f kutup = new Vector3f();
        final Vector3f e2 = new Vector3f();
        final Vector3f isik = new Vector3f();
        float yaricap;
        private final Vector3f u = new Vector3f();
        private final Vector3f v = new Vector3f();
        private final Vector3f gecici = new Vector3f();

        void guncelle(Gezegen g, Durum d, int i) {
            yon.set(d.yon[3 * i], d.yon[3 * i + 1], d.yon[3 * i + 2]);
            gecici.set(Math.abs(yon.y) > 0.9f ? 1f : 0f, Math.abs(yon.y) > 0.9f ? 0f : 1f, 0f);
            u.set(gecici).cross(yon).normalize();
            v.set(yon).cross(u).normalize();
            z.set(yon).negate();
            float a = g.kutupEgim * Mth.DEG_TO_RAD;
            float e = g.aciklik * Mth.DEG_TO_RAD;
            ur.set(u).mul(Mth.cos(a));
            ur.sub(gecici.set(v).mul(Mth.sin(a)));
            vr.set(v).mul(Mth.cos(a));
            vr.add(gecici.set(u).mul(Mth.sin(a)));
            kutup.set(vr).mul(Mth.cos(e));
            kutup.add(gecici.set(z).mul(Mth.sin(e)));
            e2.set(kutup).cross(ur);
            isik.set(d.isik[3 * i], d.isik[3 * i + 1], d.isik[3 * i + 2]);
            yaricap = 100f * (float) Math.tan(Math.toRadians(d.derece[i]) / 2);
        }
    }

    private Gezegenler() {
    }

    /** Oyun dünyasının yaşı (sn, sarılmaz; kare ara değerli); dünya yoksa 0. Yalnız istemci ana thread'i. */
    static double zaman() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0.0 : (mc.level.getGameTime() + mc.getFrameTime()) / 20.0;
    }

    /** {@code i}. gezegene bakarken kabul edilen bakma yarıçapı (derece): görünür yarıçap (halkalıda halka dahil). */
    static float gorunurYaricap(int i, float derece) {
        return 0.5f * derece * (LISTE[i].halka ? HALKA_OLCEK : 1f);
    }

    private static Izgara izgara(float derece, EfektKalitesi kalite) {
        int seviye = derece < 0.7f ? 2 : derece < 1.8f ? 1 : 0;
        if (kalite == EfektKalitesi.DUSUK) {
            seviye = Math.max(seviye, 1);
        }
        return seviye == 0 ? BUYUK : seviye == 1 ? ORTA : KUCUK;
    }

    /**
     * {@code m}: derin uzay matrisi, {@code izdusum}: izdüşüm matrisi; çıkışta toplamalı karma açık, shader rengi sıfırlanmış.
     * K-E85: görüş hacminin dışında kalan gezegenin (hale, halka ve uyduları dahil) ağı hiç kurulmaz; ağlar her karede vertex vertex
     * kurulduğundan gezegenler gökyüzü çiziminin CPU süresinin büyük kısmıydı. Görüntü değişmez.
     */
    static void ciz(Matrix4f m, Matrix4f izdusum, float uzay, EfektKalitesi kalite) {
        if (uzay <= 0.01f) {
            return;
        }
        int n = SAYI;
        double t = zaman();
        CIZIM.hesapla(t);
        KLIP.set(izdusum).mul(m);
        float p00 = Math.abs(izdusum.m00());
        float p11 = Math.abs(izdusum.m11());
        boolean hicbiri = true;
        for (int i = 0; i < n; i++) {
            CERCEVE[i].guncelle(LISTE[i], CIZIM, i);
            SIRA[i] = i;
            GORUNUR[i] = gorusIcinde(CERCEVE[i], p00, p11);
            hicbiri &= !GORUNUR[i];
        }
        if (hicbiri) {
            GokyuzuCizim.toplamali();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            return;
        }
        // Uzaktan yakına (yakın gezegen uzaktakini örter): eklemeli sıralama, en fazla sekiz eleman.
        for (int i = 1; i < n; i++) {
            int s = SIRA[i];
            int j = i - 1;
            while (j >= 0 && CIZIM.mesafe[SIRA[j]] < CIZIM.mesafe[s]) {
                SIRA[j + 1] = SIRA[j];
                j--;
            }
            SIRA[j + 1] = s;
        }
        haleler(m, uzay);
        GokyuzuCizim.normal();
        for (int q = 0; q < n; q++) {
            int i = SIRA[q];
            if (!GORUNUR[i]) {
                continue;
            }
            Gezegen g = LISTE[i];
            Cerceve c = CERCEVE[i];
            if (g.halka) {
                halkaYarim(m, c, true, uzay);
            }
            govde(m, i, c, izgara(CIZIM.derece[i], kalite), t, uzay);
            if (g.halka) {
                halkaYarim(m, c, false, uzay);
            }
        }
        uydular(m, n, t, uzay);
        GokyuzuCizim.toplamali();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** Gezegenin ağı (hale, halka ve uydular dahil) kareye girebilir mi: {@link #KAPSAM_OLCEK} yarıçaplı küre kırpma uzayında sınanır. */
    private static boolean gorusIcinde(Cerceve c, float p00, float p11) {
        float s = c.yaricap * KAPSAM_OLCEK + 0.5f;
        KLIP.transform(c.yon.x * 100f, c.yon.y * 100f, c.yon.z * 100f, 1f, KLIP_V);
        float w = KLIP_V.w;
        if (w < -s) {
            return false;
        }
        if (w <= s) {
            return true;
        }
        float pay = w + s;
        return Math.abs(KLIP_V.x) <= pay + s * p00 && Math.abs(KLIP_V.y) <= pay + s * p11;
    }

    /** Her gezegenin arkasına toplamalı yumuşak hale (tek çizim); gövdeler hemen ardından normal karmayla çizilir. */
    private static void haleler(Matrix4f m, float uzay) {
        GokyuzuCizim.toplamali();
        BufferBuilder bb = GokyuzuCizim.baslat(HALE);
        float a = HALE_ALFA * uzay;
        for (int i = 0; i < SAYI; i++) {
            if (!GORUNUR[i]) {
                continue;
            }
            Cerceve c = CERCEVE[i];
            float h = c.yaricap * HALE_OLCEK * (LISTE[i].halka ? HALKA_OLCEK * 0.6f : 1f);
            float ox = c.yon.x * 100f;
            float oy = c.yon.y * 100f;
            float oz = c.yon.z * 100f;
            float ax = c.ur.x * h;
            float ay = c.ur.y * h;
            float az = c.ur.z * h;
            float bx = c.vr.x * h;
            float by = c.vr.y * h;
            float bz = c.vr.z * h;
            bb.vertex(m, ox - ax - bx, oy - ay - by, oz - az - bz).uv(0f, 0f).color(0.78f, 0.88f, 1f, a).endVertex();
            bb.vertex(m, ox + ax - bx, oy + ay - by, oz + az - bz).uv(1f, 0f).color(0.78f, 0.88f, 1f, a).endVertex();
            bb.vertex(m, ox + ax + bx, oy + ay + by, oz + az + bz).uv(1f, 1f).color(0.78f, 0.88f, 1f, a).endVertex();
            bb.vertex(m, ox - ax + bx, oy - ay + by, oz - az + bz).uv(0f, 1f).color(0.78f, 0.88f, 1f, a).endVertex();
        }
        GokyuzuCizim.bitir(bb);
    }

    private static void govde(Matrix4f m, int dizin, Cerceve c, Izgara iz, double t, float uzay) {
        Gezegen g = LISTE[dizin];
        float donus = (float) (t / g.donusSn % 1.0 * Mth.TWO_PI);
        float cs = Mth.cos(donus);
        float ss = Mth.sin(donus);
        Vector3f e1 = c.ur;
        Vector3f e2 = c.e2;
        Vector3f p = c.kutup;
        Vector3f z = c.z;
        Vector3f l = c.isik;
        float r = c.yaricap;
        float cx = c.yon.x * 100f;
        float cy = c.yon.y * 100f;
        float cz = c.yon.z * 100f;
        int adim = iz.boylam + 1;
        for (int i = 0; i <= iz.enlem; i++) {
            for (int j = 0; j <= iz.boylam; j++) {
                int k = i * adim + j;
                float cl = iz.cosL[j] * cs - iz.sinL[j] * ss;
                float sl = iz.sinL[j] * cs + iz.cosL[j] * ss;
                float ex = iz.cosP[i] * cl;
                float ey = iz.sinP[i];
                float ez = iz.cosP[i] * sl;
                float nx = e1.x * ex + p.x * ey + e2.x * ez;
                float ny = e1.y * ex + p.y * ey + e2.y * ez;
                float nz = e1.z * ex + p.z * ey + e2.z * ez;
                X[k] = cx + r * nx;
                Y[k] = cy + r * ny;
                Z[k] = cz + r * nz;
                float yuz = nx * z.x + ny * z.y + nz * z.z;
                if (yuz <= 0f) {
                    A[k] = 0f;
                    S[k] = 0f;
                    continue;
                }
                float d = nx * l.x + ny * l.y + nz * l.z;
                float tt = Mth.clamp((d + 0.08f) / 0.55f, 0f, 1f);
                S[k] = (EN_AZ_PARLAKLIK + (1f - EN_AZ_PARLAKLIK) * tt * tt * (3f - 2f * tt)) * (0.7f + 0.3f * yuz);
                float kenar = Mth.clamp(yuz / 0.22f, 0f, 1f);
                A[k] = kenar * kenar * (3f - 2f * kenar) * uzay;
            }
        }
        BufferBuilder bb = GokyuzuCizim.baslat(YUZEY[g.doku]);
        for (int i = 0; i < iz.enlem; i++) {
            for (int j = 0; j < iz.boylam; j++) {
                int k00 = i * adim + j;
                int k10 = k00 + adim;
                if (A[k00] + A[k00 + 1] + A[k10] + A[k10 + 1] <= 0.004f) {
                    continue;
                }
                float u0 = (float) j / iz.boylam;
                float u1 = (float) (j + 1) / iz.boylam;
                float v0 = (float) i / iz.enlem;
                float v1 = (float) (i + 1) / iz.enlem;
                kose(bb, m, k00, u0, v0);
                kose(bb, m, k00 + 1, u1, v0);
                kose(bb, m, k10 + 1, u1, v1);
                kose(bb, m, k10, u0, v1);
            }
        }
        GokyuzuCizim.bitir(bb);
    }

    private static void kose(BufferBuilder bb, Matrix4f m, int k, float u, float v) {
        bb.vertex(m, X[k], Y[k], Z[k]).uv(u, v).color(S[k], S[k], S[k], A[k]).endVertex();
    }

    /**
     * Halkanın arka ({@code arka}: kutup yönündeki, gövdenin ardındaki) ya da ön yarısı: elips, büyük eksen kutba dik,
     * küçük/büyük eksen oranı kutbun gözlemciye açıklığı (|kutup·bakış|).
     */
    private static void halkaYarim(Matrix4f m, Cerceve c, boolean arka, float uzay) {
        float rr = c.yaricap * HALKA_OLCEK;
        float oran = Math.abs(c.kutup.dot(c.z));
        float ux = c.ur.x * rr;
        float uy = c.ur.y * rr;
        float uz = c.ur.z * rr;
        float vx = c.vr.x * rr * oran;
        float vy = c.vr.y * rr * oran;
        float vz = c.vr.z * rr * oran;
        float ox = c.yon.x * 100f;
        float oy = c.yon.y * 100f;
        float oz = c.yon.z * 100f;
        float s0 = arka ? 0f : -1f;
        float s1 = arka ? 1f : 0f;
        float v0 = arka ? 0.5f : 0f;
        float v1 = arka ? 1f : 0.5f;
        float a = 0.92f * uzay;
        BufferBuilder bb = GokyuzuCizim.baslat(HALKA);
        bb.vertex(m, ox - ux + s0 * vx, oy - uy + s0 * vy, oz - uz + s0 * vz).uv(0f, v0).color(1f, 0.97f, 0.9f, a).endVertex();
        bb.vertex(m, ox + ux + s0 * vx, oy + uy + s0 * vy, oz + uz + s0 * vz).uv(1f, v0).color(1f, 0.97f, 0.9f, a).endVertex();
        bb.vertex(m, ox + ux + s1 * vx, oy + uy + s1 * vy, oz + uz + s1 * vz).uv(1f, v1).color(1f, 0.97f, 0.9f, a).endVertex();
        bb.vertex(m, ox - ux + s1 * vx, oy - uy + s1 * vy, oz - uz + s1 * vz).uv(0f, v1).color(1f, 0.97f, 0.9f, a).endVertex();
        GokyuzuCizim.bitir(bb);
    }

    /** Tüm uydular tek çizimde; gövdenin arkasındaki (ekranda gövde diskinin içinde kalan) uydu çizilmez. */
    private static void uydular(Matrix4f m, int n, double t, float uzay) {
        int say = 0;
        for (int i = 0; i < n; i++) {
            if (!GORUNUR[i]) {
                continue;
            }
            Gezegen g = LISTE[i];
            Cerceve ce = CERCEVE[i];
            float oran = Math.abs(ce.kutup.dot(ce.z));
            for (int k = 0; k < g.uyduSn.length && say < UX.length; k++) {
                float psi = (float) (t / g.uyduSn[k] % 1.0 * Mth.TWO_PI) + k * 2.1f + i;
                float cp = Mth.cos(psi);
                float sp = Mth.sin(psi);
                float yor = ce.yaricap * ((g.halka ? 3.3f : 2.4f) + 0.7f * k);
                float ekran2 = (cp * cp + sp * sp * oran * oran) * yor * yor;
                if (sp < 0f && ekran2 < ce.yaricap * ce.yaricap * 1.04f) {
                    continue;
                }
                UX[say] = ce.yon.x * 100f + yor * (ce.ur.x * cp + ce.e2.x * sp);
                UY[say] = ce.yon.y * 100f + yor * (ce.ur.y * cp + ce.e2.y * sp);
                UZ[say] = ce.yon.z * 100f + yor * (ce.ur.z * cp + ce.e2.z * sp);
                UH[say] = 0.12f * ce.yaricap + 0.1f;
                UI[say] = i;
                say++;
            }
        }
        if (say == 0) {
            return;
        }
        BufferBuilder bb = GokyuzuCizim.baslat(UYDU);
        for (int q = 0; q < say; q++) {
            Cerceve ce = CERCEVE[UI[q]];
            float h = UH[q];
            float ax = ce.ur.x * h;
            float ay = ce.ur.y * h;
            float az = ce.ur.z * h;
            float bx = ce.vr.x * h;
            float by = ce.vr.y * h;
            float bz = ce.vr.z * h;
            bb.vertex(m, UX[q] - ax - bx, UY[q] - ay - by, UZ[q] - az - bz).uv(0f, 0f).color(1f, 1f, 1f, uzay).endVertex();
            bb.vertex(m, UX[q] + ax - bx, UY[q] + ay - by, UZ[q] + az - bz).uv(1f, 0f).color(1f, 1f, 1f, uzay).endVertex();
            bb.vertex(m, UX[q] + ax + bx, UY[q] + ay + by, UZ[q] + az + bz).uv(1f, 1f).color(1f, 1f, 1f, uzay).endVertex();
            bb.vertex(m, UX[q] - ax + bx, UY[q] - ay + by, UZ[q] - az + bz).uv(0f, 1f).color(1f, 1f, 1f, uzay).endVertex();
        }
        GokyuzuCizim.bitir(bb);
    }
}
