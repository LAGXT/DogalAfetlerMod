package io.github.kadirberkpolat1.dogalafetler.su;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bir boyuttaki bütün sanal su: deniz yükselmesi (global artış + {@link DenizAlani}), dalgalar, mağara cepleri ve çekilmeler.
 * Hepsi aynı sorguya cevap verir: "bu sütunda {@code t} anında hangi aralıklar su?" ({@link #araliklar}). Render, fizik ve
 * ıslaklık bu tek sorguyu kullanır. Sunucuda otorite, istemcide aynı parametrelerden kurulmuş kopya. Zaman ölçekli afet
 * saatidir. Tek iş parçacığı (sorgular paylaşılan geçici nesne kullanır).
 */
public final class SanalSuDunyasi {
    /** Mağara cebi ve yaşamı; {@code donma} NaN değilse süreç durdurulmuştur ve seviye o andaki gibi kalır. */
    public record Magara(MagaraCebi cep, MagaraZamani zaman, double donma) {
        public double seviye(double t) {
            return zaman.seviye(Double.isNaN(donma) ? t : Math.min(t, donma));
        }

        public MagaraZamani.Asama asama(double t) {
            return zaman.asama(Double.isNaN(donma) ? t : Math.min(t, donma));
        }
    }

    private static final Dalga[] DALGA_YOK = new Dalga[0];
    private static final Magara[] MAGARA_YOK = new Magara[0];
    private static final Cekilme[] CEKILME_YOK = new Cekilme[0];
    private static final SelSuyu[] SEL_YOK = new SelSuyu[0];
    private static final IsiBolgesi[] ISI_YOK = new IsiBolgesi[0];

    private final DenizAlani alan;
    private double denizArtisi;
    private final Map<Integer, Dalga> dalgalar = new LinkedHashMap<>();
    private final Map<Integer, Magara> magaralar = new LinkedHashMap<>();
    private final Map<Integer, Cekilme> cekilmeler = new LinkedHashMap<>();
    private Dalga[] dalgaDizi = DALGA_YOK;
    private Magara[] magaraDizi = MAGARA_YOK;
    private volatile Cekilme[] cekilmeDizi = CEKILME_YOK;
    private final Map<Integer, SelSuyu> seller = new LinkedHashMap<>();
    private volatile SelSuyu[] selDizi = SEL_YOK;
    private volatile IsiBolgesi[] isiDizi = ISI_YOK;
    private final SuAraliklari gecici = new SuAraliklari();
    private final SuAraliklari selSuyu = new SuAraliklari();
    private final SuAraliklari kaliciSu = new SuAraliklari();
    private ZeminOkuyucu canliZemin;
    private long canliZeminSayisi;

    /** Sütunun şu anki zemini (dünyanın yükseklik haritası, yüklü chunk); bilinmiyorsa {@link Integer#MIN_VALUE}. */
    @FunctionalInterface
    public interface ZeminOkuyucu {
        int zemin(int x, int z);
    }

    public SanalSuDunyasi(DenizAlani alan) {
        this.alan = alan;
    }

    public DenizAlani alan() {
        return alan;
    }

    /**
     * Dalga aralığının tabanı için canlı zemin (SU-27, SU-36): dalga aralığı önce dünyanın şu anki zemininden başlar,
     * chunk yüklü değilse alanın okuduğu zeminden, o da yoksa dalga tabanından. Alanın zemini bayat olabilir (kodla
     * değişen blok olay atmaz, tazeleme periyodiktir); bayat zemin hendekteki varlığı kuru bırakırdı.
     */
    public void canliZemin(ZeminOkuyucu okuyucu) {
        canliZemin = okuyucu;
    }

    /** Dalga aralığı için canlı zeminin kaç kez kullanıldığı (test: SU-36 yolunun gerçekten kullanıldığı). */
    public long canliZeminSayisi() {
        return canliZeminSayisi;
    }

    public void denizArtisiAyarla(double artis) {
        denizArtisi = Math.max(0, artis);
    }

    public double denizArtisi() {
        return denizArtisi;
    }

    public void dalgaEkle(int id, Dalga d) {
        dalgalar.put(id, d);
        dalgaDizi = dalgalar.values().toArray(DALGA_YOK);
    }

    public boolean dalgaKaldir(int id) {
        boolean vardi = dalgalar.remove(id) != null;
        dalgaDizi = dalgalar.values().toArray(DALGA_YOK);
        return vardi;
    }

    public Map<Integer, Dalga> dalgalar() {
        return Collections.unmodifiableMap(dalgalar);
    }

    /** Dalgaların değişmez dizisi (render, tahsissiz). */
    public Dalga[] dalgaDizisi() {
        return dalgaDizi;
    }

    public void magaraEkle(int id, MagaraCebi cep, MagaraZamani zaman) {
        magaraKoy(id, new Magara(cep, zaman, Double.NaN));
    }

    public void magaraKoy(int id, Magara m) {
        magaralar.put(id, m);
        magaraDizi = magaralar.values().toArray(MAGARA_YOK);
    }

    public boolean magaraKaldir(int id) {
        boolean vardi = magaralar.remove(id) != null;
        magaraDizi = magaralar.values().toArray(MAGARA_YOK);
        return vardi;
    }

    /** Mağaranın yaşamını {@code t} anında dondurur (süreç durdu). */
    public void magaraDondur(int id, double t) {
        Magara m = magaralar.get(id);
        if (m != null && Double.isNaN(m.donma())) {
            magaraKoy(id, new Magara(m.cep(), m.zaman(), t));
        }
    }

    /** Donmuş mağarayı {@code t} anında kaldığı yerden sürdürür. */
    public void magaraCoz(int id, double t) {
        Magara m = magaralar.get(id);
        if (m != null && !Double.isNaN(m.donma())) {
            magaraKoy(id, new Magara(m.cep(), m.zaman().kaydir(t - m.donma()), Double.NaN));
        }
    }

    public Map<Integer, Magara> magaralar() {
        return Collections.unmodifiableMap(magaralar);
    }

    public void cekilmeAyarla(int id, Cekilme c) {
        cekilmeler.put(id, c);
        cekilmeDizi = cekilmeler.values().toArray(CEKILME_YOK);
    }

    public boolean cekilmeKaldir(int id) {
        boolean vardi = cekilmeler.remove(id) != null;
        cekilmeDizi = cekilmeler.values().toArray(CEKILME_YOK);
        return vardi;
    }

    public Map<Integer, Cekilme> cekilmeler() {
        return Collections.unmodifiableMap(cekilmeler);
    }

    /** Chunk derleme iş parçacıkları da okur (gerçek suyu gizleme); değişmez dizi. */
    public Cekilme[] cekilmeDizisi() {
        return cekilmeDizi;
    }

    /** Sel suyunu (taşkın) ekler ya da değiştirir; deniz artışına {@link SelSuyu#artis} kadar eklenir. */
    public void selEkle(int id, SelSuyu s) {
        seller.put(id, s);
        selDizi = seller.values().toArray(SEL_YOK);
    }

    public boolean selKaldir(int id) {
        boolean vardi = seller.remove(id) != null;
        selDizi = seller.values().toArray(SEL_YOK);
        return vardi;
    }

    public Map<Integer, SelSuyu> seller() {
        return Collections.unmodifiableMap(seller);
    }

    /** Selleri değişmez dizi olarak verir (render, tahsissiz). */
    public SelSuyu[] selDizisi() {
        return selDizi;
    }

    /** Sel suyunu buharlaştıran ısı kaynağı bölgelerini topluca değiştirir (sunucu tarar, istemci paketten alır). */
    public void isiBolgeleriAyarla(java.util.List<IsiBolgesi> yeni) {
        isiDizi = yeni.isEmpty() ? ISI_YOK : yeni.toArray(ISI_YOK);
    }

    public IsiBolgesi[] isiDizisi() {
        return isiDizi;
    }

    /** {@code t} anında bütün sellerin toplam yükselmesi (blok); ısı kaynakları hesaba katılmaz. */
    public double selArtisi(double t) {
        double toplam = 0;
        for (SelSuyu s : selDizi) {
            toplam += s.artis(t);
        }
        return toplam;
    }

    /**
     * {@code t} anındaki genel deniz yükselmesi: kalıcı artış ve sel toplamı, sistemin desteklediği en büyük artışla sınırlı.
     * Isı bölgeleri sütuna özgüdür ({@link #sutunArtisi}); render bu değeri "artış değişti mi" ölçütü olarak kullanır.
     */
    public double etkinArtis(double t) {
        return Math.min(DenizAlani.EN_COK_ARTIS, denizArtisi + selArtisi(t));
    }

    /** Sütunda ısı kaynaklarının sel suyunu söndürme oranı (0..1, en güçlü bölge). */
    public double isiOrani(double px, double pz) {
        double en = 0;
        for (IsiBolgesi b : isiDizi) {
            en = Math.max(en, b.oran(px, pz));
            if (en >= 1) {
                break;
            }
        }
        return en;
    }

    /** Sütunun {@code t} anındaki yükselmesi: kalıcı artış + sel × (1 − ısı kaynaklarının kuruluğu). */
    public double sutunArtisi(int x, int z, double t) {
        double sel = selArtisi(t);
        if (sel > 0 && isiDizi.length > 0) {
            sel *= 1 - isiOrani(x + 0.5, z + 0.5);
        }
        return Math.min(DenizAlani.EN_COK_ARTIS, denizArtisi + sel);
    }

    /**
     * {@code (x, y, z)} noktası sel suyunun içinde mi: sütunun sel yükselmesiyle ({@link #sutunArtisi}) kaplanan aralığında ve
     * kalıcı deniz artışının kaplamadığı yerde. Gerçek su, dalga ve mağara suyu sel sayılmaz; sel yoksa {@code false}.
     */
    public boolean selSuyundaMi(double x, double y, double z, double t) {
        if (selDizi.length == 0) {
            return false;
        }
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        double artis = sutunArtisi(bx, bz, t);
        if (!(artis > denizArtisi)) {
            return false;
        }
        selSuyu.temizle();
        alan.aralikEkle(bx, bz, artis, selSuyu);
        selSuyu.birlestir();
        if (Double.isNaN(selSuyu.ustuBul(y))) {
            return false;
        }
        if (!(denizArtisi > 0)) {
            return true;
        }
        kaliciSu.temizle();
        alan.aralikEkle(bx, bz, denizArtisi, kaliciSu);
        kaliciSu.birlestir();
        return Double.isNaN(kaliciSu.ustuBul(y));
    }

    /** {@code t} anında bitmiş sellerin kimlikleri (silmez). */
    public List<Integer> bitenSeller(double t) {
        List<Integer> sonuc = new ArrayList<>();
        seller.forEach((id, s) -> {
            if (s.bitti(t)) {
                sonuc.add(id);
            }
        });
        return sonuc;
    }

    /** Dalga, mağara, çekilme, sel ve ısı bölgelerini siler (deniz artışı dönemin alanıdır, ayrı ayarlanır). */
    public void temizle() {
        dalgalar.clear();
        magaralar.clear();
        cekilmeler.clear();
        seller.clear();
        dalgaDizi = DALGA_YOK;
        magaraDizi = MAGARA_YOK;
        cekilmeDizi = CEKILME_YOK;
        selDizi = SEL_YOK;
        isiDizi = ISI_YOK;
    }

    public boolean aktif() {
        return denizArtisi > 0 || dalgaDizi.length > 0 || magaraDizi.length > 0 || cekilmeDizi.length > 0
                || selDizi.length > 0;
    }

    /** Alan (sütun önbelleği) gerekiyor mu: deniz, sel, dalga tabanı ve çekilme gerçek sütun bilgisini okur. */
    public boolean alanGerekli() {
        return denizArtisi > 0 || dalgaDizi.length > 0 || cekilmeDizi.length > 0 || selDizi.length > 0;
    }

    /** {@code t} anında bitmiş dalgaların kimlikleri (silmez). */
    public List<Integer> bitenDalgalar(double t) {
        List<Integer> sonuc = new ArrayList<>();
        dalgalar.forEach((id, d) -> {
            if (d.bitti(t)) {
                sonuc.add(id);
            }
        });
        return sonuc;
    }

    /** {@code t} anında kurumuş mağaraların kimlikleri (silmez). */
    public List<Integer> bitenMagaralar(double t) {
        List<Integer> sonuc = new ArrayList<>();
        magaralar.forEach((id, m) -> {
            if (m.asama(t) == MagaraZamani.Asama.BITTI) {
                sonuc.add(id);
            }
        });
        return sonuc;
    }

    /** Sütunun ({@code x, z} blok) bütün su aralıkları; {@code a} temizlenip birleştirilmiş hâlde doldurulur. */
    public void araliklar(int x, int z, double t, SuAraliklari a) {
        a.temizle();
        double artis = selDizi.length == 0 ? denizArtisi : sutunArtisi(x, z, t);
        if (artis > 0) {
            alan.aralikEkle(x, z, artis, a);
        }
        Dalga[] dd = dalgaDizi;
        for (Dalga d : dd) {
            double y = d.yuzey(x + 0.5, z + 0.5, t);
            if (!Double.isNaN(y)) {
                int zemin = canliZemin == null ? Integer.MIN_VALUE : canliZemin.zemin(x, z);
                if (zemin != Integer.MIN_VALUE) {
                    canliZeminSayisi++;
                } else {
                    zemin = alan.zemin(x, z);
                }
                a.ekle(zemin == Integer.MIN_VALUE ? d.taban() : Math.min(zemin, y), y);
            }
        }
        for (Magara m : magaraDizi) {
            double seviye = m.seviye(t);
            if (!Double.isNaN(seviye)) {
                m.cep().aralikEkle(x, z, seviye, a);
            }
        }
        for (Cekilme c : cekilmeDizi) {
            if (alan.su(x, z) && c.icinde(x + 0.5, z + 0.5)) {
                a.ekle(alan.taban(x, z), alan.zemin(x, z) - c.dusus(x + 0.5, z + 0.5, t));
            }
        }
        a.birlestir();
    }

    public double batiklik(double x, double z, double minY, double maxY, double t) {
        if (!aktif()) {
            return 0;
        }
        araliklar((int) Math.floor(x), (int) Math.floor(z), t, gecici);
        return gecici.batiklikOrani(minY, maxY);
    }

    /** Ayak hizasından suyun üstüne yükseklik (vanilla {@code fluidHeight} karşılığı); suda değilse 0. */
    public double suYuksekligi(double x, double z, double minY, double maxY, double t) {
        if (!aktif()) {
            return 0;
        }
        araliklar((int) Math.floor(x), (int) Math.floor(z), t, gecici);
        return gecici.ayaktanYukseklik(minY, maxY);
    }

    /** {@code y}'yi içeren su aralığının üstü; {@code y} sanal suda değilse NaN. */
    public double ustu(double x, double y, double z, double t) {
        if (!aktif()) {
            return Double.NaN;
        }
        araliklar((int) Math.floor(x), (int) Math.floor(z), t, gecici);
        return gecici.ustuBul(y);
    }

    /** Sütundaki en üst sanal su yüzeyi; yoksa NaN. */
    public double enUstYuzey(int x, int z, double t) {
        if (!aktif()) {
            return Double.NaN;
        }
        araliklar(x, z, t, gecici);
        return gecici.adet() == 0 ? Double.NaN : gecici.ust(gecici.adet() - 1);
    }

    /**
     * Dalgaların sürükleme ivmesi; {@code cikti[0..1]} = (x, z), batmış pay ile çarpılır. Etki eden en güçlü dalgayı döner
     * (hasar için), etki yoksa {@code null}. Dalga tepesinin üstündeki varlık etkilenmez.
     */
    public Dalga itme(double x, double z, double minY, double maxY, double t, double[] cikti) {
        cikti[0] = 0;
        cikti[1] = 0;
        Dalga en = null;
        double enOran = 0;
        for (Dalga d : dalgaDizi) {
            double y = d.yuzey(x, z, t);
            if (Double.isNaN(y) || y <= minY) {
                continue;
            }
            double oran = Math.min(1, (y - minY) / Math.max(0.1, maxY - minY));
            cikti[0] += d.yonX() * d.ivme() * oran;
            cikti[1] += d.yonZ() * d.ivme() * oran;
            if (oran * d.ivme() > enOran) {
                enOran = oran * d.ivme();
                en = d;
            }
        }
        return en;
    }
}
