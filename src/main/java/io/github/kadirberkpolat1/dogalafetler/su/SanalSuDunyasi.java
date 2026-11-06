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

    private final DenizAlani alan;
    private double denizArtisi;
    private final Map<Integer, Dalga> dalgalar = new LinkedHashMap<>();
    private final Map<Integer, Magara> magaralar = new LinkedHashMap<>();
    private final Map<Integer, Cekilme> cekilmeler = new LinkedHashMap<>();
    private Dalga[] dalgaDizi = DALGA_YOK;
    private Magara[] magaraDizi = MAGARA_YOK;
    private volatile Cekilme[] cekilmeDizi = CEKILME_YOK;
    private final SuAraliklari gecici = new SuAraliklari();
    private ZeminOkuyucu zeminYedek;

    /** Alanın henüz okumadığı sütunun zemini (dünyadan, yüklü chunk); bilinmiyorsa {@link Integer#MIN_VALUE}. */
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
     * Dalga aralığının tabanı için yedek zemin (SU-27): alan sütunu okumadan (okuma bütçesi, uzak bölge) dalga geçerse
     * aralık dalga tabanından değil gerçek zeminden başlar; yoksa tabanın altındaki varlık kuru kalır.
     */
    public void zeminYedek(ZeminOkuyucu okuyucu) {
        zeminYedek = okuyucu;
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

    /** Dalga, mağara ve çekilmeleri siler (deniz artışı dönemin alanıdır, ayrı ayarlanır). */
    public void temizle() {
        dalgalar.clear();
        magaralar.clear();
        cekilmeler.clear();
        dalgaDizi = DALGA_YOK;
        magaraDizi = MAGARA_YOK;
        cekilmeDizi = CEKILME_YOK;
    }

    public boolean aktif() {
        return denizArtisi > 0 || dalgaDizi.length > 0 || magaraDizi.length > 0 || cekilmeDizi.length > 0;
    }

    /** Alan (sütun önbelleği) gerekiyor mu: deniz, dalga tabanı ve çekilme gerçek sütun bilgisini okur. */
    public boolean alanGerekli() {
        return denizArtisi > 0 || dalgaDizi.length > 0 || cekilmeDizi.length > 0;
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
        if (denizArtisi > 0) {
            alan.aralikEkle(x, z, denizArtisi, a);
        }
        Dalga[] dd = dalgaDizi;
        for (Dalga d : dd) {
            double y = d.yuzey(x + 0.5, z + 0.5, t);
            if (!Double.isNaN(y)) {
                int zemin = alan.zemin(x, z);
                if (zemin == Integer.MIN_VALUE && zeminYedek != null) {
                    zemin = zeminYedek.zemin(x, z);
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
