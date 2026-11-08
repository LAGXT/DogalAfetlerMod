package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

import java.util.List;
import java.util.SplittableRandom;

/**
 * Kasırganın rastgele yürüyüşü (AF2-20): kasırga kimseyi izlemez, {@link #ARALIK_EN_AZ}-{@link #ARALIK_EN_COK} ölçekli
 * tick'te bir yeni bir ara hedefe döner. Seçimlerin {@link #RASTGELE_ORANI}'ı {@link Tur#RASTGELE} (önceki bacağın yönü
 * ±{@link #SAPMA}, {@link #MESAFE_EN_AZ}-{@link #MESAFE_EN_COK} blok ötede), kalanı {@link Tur#OYUNCU}: rastgele bir
 * oyuncunun {@link #OYUNCU_EN_AZ}-{@link #OYUNCU_EN_COK} blok çevresinde sabit bir nokta (oyuncu yürüse de nokta güncellenmez).
 * Hiçbir ara hedef bir oyuncuya {@link #OYUNCUYA_EN_AZ} bloktan yakın değildir; uygun aday yoksa yüklü ve sınır içindeki
 * adaylardan oyunculara en uzak olanı alınır. Rota başlangıcından {@link #EN_COK_UZAKLIK} bloktan uzağa ara hedef seçilmez:
 * sınırı aşan rastgele aday başlangıca doğru ±{@link #SAPMA} yönle değiştirilir ("dönüş"), sınırı aşan oyuncu adayı rastgele
 * seçime düşer. Ara hedefe {@link #VARIS} bloktan yaklaşınca seçim erken yinelenir.
 *
 * <p>Belirlenimci: her seçim (tohum, seçim sayacı)'ndan türetilen kendi üretecinden çeker; {@link #yaz()} ile kaydedilen rota
 * {@link #oku} ile aynen sürer. Minecraft'a bağımlı değildir; yüklülük çağıranın {@link KonumKosulu}'dur. Açılar radyan,
 * x doğu, z güney.
 */
public final class KasirgaRotasi {
    public static final double EN_COK_UZAKLIK = 512;
    public static final int ARALIK_EN_AZ = 200;
    public static final int ARALIK_EN_COK = 400;
    public static final double RASTGELE_ORANI = 0.7;
    public static final double SAPMA = Math.toRadians(60);
    public static final double MESAFE_EN_AZ = 48;
    public static final double MESAFE_EN_COK = 128;
    public static final double OYUNCU_EN_AZ = 32;
    public static final double OYUNCU_EN_COK = 96;
    /** Ara hedefin herhangi bir oyuncuya en yakın uzaklığı. */
    public static final double OYUNCUYA_EN_AZ = 32;
    public static final double VARIS = 12;
    /** Bir türün aday denemesi; ikinci aşamada her yöne bunun iki katı aday denenir. */
    static final int DENEME = 12;
    private static final long KARISTIR = 0x9E3779B97F4A7C15L;

    public enum Tur {
        RASTGELE, OYUNCU
    }

    /** Ara hedef konumu yüklü mü (çağıranın dünyası; kasırgada entity-ticking). */
    @FunctionalInterface
    public interface KonumKosulu {
        boolean test(double x, double z);
    }

    /** Bir seçim: {@code no} seçim sayacı, {@code uzaklik} seçildiği konumdan, {@code baslangicaUzaklik} rota başlangıcından. */
    public record Secim(int no, double x, double z, Tur tur, boolean donus, double uzaklik, double baslangicaUzaklik) {
    }

    /** Kaydedilen rota durumu (NBT eşlemesi çağırandadır). */
    public record Durum(double baslangicX, double baslangicZ, long tohum, int sayac, double hedefX, double hedefZ,
            double sonYon, int kalanTick, Tur sonTur, boolean sonDonus) {
    }

    private final double baslangicX;
    private final double baslangicZ;
    private final long tohum;
    private int sayac;
    private double hedefX;
    private double hedefZ;
    private double sonYon;
    private int kalanTick;
    private Tur sonTur;
    private boolean sonDonus;

    private KasirgaRotasi(Durum d) {
        baslangicX = d.baslangicX();
        baslangicZ = d.baslangicZ();
        tohum = d.tohum();
        sayac = Math.max(0, d.sayac());
        hedefX = d.hedefX();
        hedefZ = d.hedefZ();
        sonYon = d.sonYon();
        kalanTick = d.kalanTick();
        sonTur = d.sonTur() != null ? d.sonTur() : Tur.RASTGELE;
        sonDonus = d.sonDonus();
    }

    /** Yeni rota: ara hedef henüz yok (başlangıç noktası), ilk {@link #secimZamani} çağrısı seçim ister. */
    public static KasirgaRotasi yeni(double baslangicX, double baslangicZ, double ilkYon, long tohum) {
        return new KasirgaRotasi(new Durum(baslangicX, baslangicZ, tohum, 0, baslangicX, baslangicZ, ilkYon, 0, Tur.RASTGELE,
                false));
    }

    public static KasirgaRotasi oku(Durum d) {
        return new KasirgaRotasi(d);
    }

    public Durum yaz() {
        return new Durum(baslangicX, baslangicZ, tohum, sayac, hedefX, hedefZ, sonYon, kalanTick, sonTur, sonDonus);
    }

    public double baslangicX() {
        return baslangicX;
    }

    public double baslangicZ() {
        return baslangicZ;
    }

    public long tohum() {
        return tohum;
    }

    /** Yapılan seçim sayısı (son ara hedefin numarası; 0 = henüz seçilmedi). */
    public int sayac() {
        return sayac;
    }

    public double hedefX() {
        return hedefX;
    }

    public double hedefZ() {
        return hedefZ;
    }

    /** Son bacağın yönü (seçildiği konumdan ara hedefe). */
    public double sonYon() {
        return sonYon;
    }

    public int kalanTick() {
        return kalanTick;
    }

    public Tur sonTur() {
        return sonTur;
    }

    public boolean sonDonus() {
        return sonDonus;
    }

    /** Noktanın rota başlangıcına yatay uzaklığı. */
    public double baslangicaUzaklik(double x, double z) {
        return Math.hypot(x - baslangicX, z - baslangicZ);
    }

    /**
     * Zamanlayıcıyı {@code adim} ölçekli tick ilerletir; süre dolduysa, ara hedefe {@link #VARIS} bloktan yaklaşıldıysa ya da
     * henüz seçim yoksa {@code true}: çağıran {@link #yeniAraHedef} ile seçer.
     */
    public boolean secimZamani(int adim, double x, double z) {
        kalanTick -= Math.max(0, adim);
        return sayac == 0 || kalanTick <= 0 || Math.hypot(hedefX - x, hedefZ - z) <= VARIS;
    }

    /**
     * Yeni ara hedef seçer (zamanlayıcı yeniden kurulur). {@code oyuncular} {x, z} konumları; {@code yuklu} aday konumun
     * yüklülüğü. Sıra: türün adayları, sonra her yöne rastgele adaylar; geçerli (yüklü, sınır içinde, oyunculara en az
     * {@link #OYUNCUYA_EN_AZ}) ilk aday alınır. Geçerli yoksa yüklü adaylardan oyunculara en uzak olan, yüklü aday da yoksa
     * sınır içindeki adaylardan oyunculara en uzak olan, o da yoksa başlangıç noktası.
     */
    public Secim yeniAraHedef(double x, double z, List<double[]> oyuncular, KonumKosulu yuklu) {
        sayac++;
        SplittableRandom r = new SplittableRandom(tohum ^ (sayac * KARISTIR));
        kalanTick = ARALIK_EN_AZ + r.nextInt(ARALIK_EN_COK - ARALIK_EN_AZ + 1);
        Arama a = new Arama(oyuncular, yuklu);
        if (!oyuncular.isEmpty() && r.nextDouble() >= RASTGELE_ORANI) {
            for (int i = 0; i < DENEME && a.secilen == null; i++) {
                double[] o = oyuncular.get(r.nextInt(oyuncular.size()));
                double aci = r.nextDouble() * 2 * Math.PI;
                double d = OYUNCU_EN_AZ + r.nextDouble() * (OYUNCU_EN_COK - OYUNCU_EN_AZ);
                a.dene(o[0] + Math.cos(aci) * d, o[1] + Math.sin(aci) * d, Tur.OYUNCU, false);
            }
        }
        for (int i = 0; i < DENEME && a.secilen == null; i++) {
            double yon = sonYon + (2 * r.nextDouble() - 1) * SAPMA;
            double d = MESAFE_EN_AZ + r.nextDouble() * (MESAFE_EN_COK - MESAFE_EN_AZ);
            double cx = x + Math.cos(yon) * d;
            double cz = z + Math.sin(yon) * d;
            boolean donus = baslangicaUzaklik(cx, cz) > EN_COK_UZAKLIK;
            if (donus) {
                yon = Math.atan2(baslangicZ - z, baslangicX - x) + (2 * r.nextDouble() - 1) * SAPMA;
                cx = x + Math.cos(yon) * d;
                cz = z + Math.sin(yon) * d;
            }
            a.dene(cx, cz, Tur.RASTGELE, donus);
        }
        for (int i = 0; i < 2 * DENEME && a.secilen == null; i++) {
            double yon = r.nextDouble() * 2 * Math.PI;
            double d = MESAFE_EN_AZ + r.nextDouble() * (MESAFE_EN_COK - MESAFE_EN_AZ);
            double cx = x + Math.cos(yon) * d;
            double cz = z + Math.sin(yon) * d;
            a.dene(cx, cz, Tur.RASTGELE, false);
        }
        Aday s = a.secilen != null ? a.secilen : a.enIyiYuklu != null ? a.enIyiYuklu : a.enIyi != null ? a.enIyi
                : new Aday(baslangicX, baslangicZ, Tur.RASTGELE, true, 0);
        hedefX = s.x;
        hedefZ = s.z;
        sonTur = s.tur;
        sonDonus = s.donus;
        double uzaklik = Math.hypot(s.x - x, s.z - z);
        if (uzaklik > 1e-9) {
            sonYon = Math.atan2(s.z - z, s.x - x);
        }
        return new Secim(sayac, s.x, s.z, s.tur, s.donus, uzaklik, baslangicaUzaklik(s.x, s.z));
    }

    private record Aday(double x, double z, Tur tur, boolean donus, double oyuncuyaUzaklik) {
    }

    /** Bir seçimin adayları: ilk geçerli aday, yoksa en iyi yüklü ve en iyi (sınır içi) aday. */
    private final class Arama {
        final List<double[]> oyuncular;
        final KonumKosulu yuklu;
        Aday secilen;
        Aday enIyiYuklu;
        Aday enIyi;

        Arama(List<double[]> oyuncular, KonumKosulu yuklu) {
            this.oyuncular = oyuncular;
            this.yuklu = yuklu;
        }

        void dene(double cx, double cz, Tur tur, boolean donus) {
            if (baslangicaUzaklik(cx, cz) > EN_COK_UZAKLIK) {
                return;
            }
            double enYakin = Double.POSITIVE_INFINITY;
            for (double[] o : oyuncular) {
                enYakin = Math.min(enYakin, Math.hypot(cx - o[0], cz - o[1]));
            }
            Aday aday = new Aday(cx, cz, tur, donus, enYakin);
            if (yuklu.test(cx, cz)) {
                if (enYakin >= OYUNCUYA_EN_AZ) {
                    secilen = aday;
                } else if (enIyiYuklu == null || enYakin > enIyiYuklu.oyuncuyaUzaklik) {
                    enIyiYuklu = aday;
                }
            } else if (enIyi == null || enYakin > enIyi.oyuncuyaUzaklik) {
                enIyi = aday;
            }
        }
    }
}
