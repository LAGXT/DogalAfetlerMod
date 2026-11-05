package io.github.kadirberkpolat1.dogalafetler.su;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bir boyuttaki bütün sanal su: deniz yükselmesi (global artış + {@link DenizSeviyesiAlani}), tsunami dalgaları, mağara
 * cepleri ve çekilme. Hepsi aynı sorguya cevap verir: "bu sütunda hangi aralıklar su?" ({@link #araliklar}). Render,
 * fizik ve ıslaklık bu tek sorguyu kullanır. Sunucuda otorite, istemcide aynı parametrelerden kurulmuş kopya.
 */
public final class SanalSuDunyasi {
    public record Magara(MagaraCebi cep, MagaraZamani zaman) {
    }

    private final DenizSeviyesiAlani alan;
    private double denizHedef;
    private long denizBaslangic;
    private long denizSure;
    private final List<TsunamiDalgasi> dalgalar = new ArrayList<>();
    private final List<Magara> magaralar = new ArrayList<>();
    private Cekilme cekilme;
    private final SuAraliklari gecici = new SuAraliklari();

    public SanalSuDunyasi(DenizSeviyesiAlani alan) {
        this.alan = alan;
    }

    public DenizSeviyesiAlani alan() {
        return alan;
    }

    public void denizAyarla(double hedef, long baslangic, long sure) {
        this.denizHedef = hedef;
        this.denizBaslangic = baslangic;
        this.denizSure = sure;
    }

    public double denizHedef() {
        return denizHedef;
    }

    public long denizBaslangic() {
        return denizBaslangic;
    }

    public long denizSure() {
        return denizSure;
    }

    /** {@code t} anındaki deniz artışı (doğrusal yükselme). */
    public double artis(double t) {
        if (denizHedef <= 0) {
            return 0;
        }
        if (denizSure <= 0) {
            return denizHedef;
        }
        double o = Math.max(0, Math.min(1, (t - denizBaslangic) / denizSure));
        return denizHedef * o;
    }

    public void dalgaEkle(TsunamiDalgasi d) {
        dalgalar.add(d);
    }

    public List<TsunamiDalgasi> dalgalar() {
        return Collections.unmodifiableList(dalgalar);
    }

    public void magaraEkle(MagaraCebi cep, MagaraZamani zaman) {
        magaralar.add(new Magara(cep, zaman));
    }

    public void magaraZamaniDegistir(int i, MagaraZamani z) {
        magaralar.set(i, new Magara(magaralar.get(i).cep(), z));
    }

    public List<Magara> magaralar() {
        return Collections.unmodifiableList(magaralar);
    }

    public void cekilmeAyarla(Cekilme c) {
        this.cekilme = c;
    }

    public Cekilme cekilme() {
        return cekilme;
    }

    public void temizle() {
        denizHedef = 0;
        dalgalar.clear();
        magaralar.clear();
        cekilme = null;
    }

    public boolean aktif() {
        return denizHedef > 0 || !dalgalar.isEmpty() || !magaralar.isEmpty() || cekilme != null;
    }

    /** Alan (sütun önbelleği) gerekiyor mu: deniz, dalga tabanı ve çekilme gerçek sütun bilgisini okur. */
    public boolean alanGerekli() {
        return denizHedef > 0 || !dalgalar.isEmpty() || cekilme != null;
    }

    /** Biten dalga ve mağaraları siler. */
    public boolean temizlikYap(double t) {
        boolean d = dalgalar.removeIf(x -> x.bitti(t));
        boolean m = magaralar.removeIf(x -> x.zaman().asama(t) == MagaraZamani.Asama.BITTI);
        return d || m;
    }

    /** Sütunun ({@code x, z} blok) bütün su aralıkları; {@code a} temizlenip birleştirilmiş hâlde doldurulur. */
    public void araliklar(int x, int z, double t, SuAraliklari a) {
        a.temizle();
        double art = artis(t);
        if (art > 0) {
            alan.aralikEkle(x, z, art, a);
        }
        for (int i = 0; i < dalgalar.size(); i++) {
            TsunamiDalgasi d = dalgalar.get(i);
            double y = d.yuzey(x + 0.5, z + 0.5, t);
            if (!Double.isNaN(y)) {
                int zemin = alan.zemin(x, z);
                a.ekle(zemin == Integer.MIN_VALUE ? d.taban() : zemin, y);
            }
        }
        for (int i = 0; i < magaralar.size(); i++) {
            Magara m = magaralar.get(i);
            double seviye = m.zaman().seviye(t);
            if (!Double.isNaN(seviye)) {
                m.cep().aralikEkle(x, z, seviye, a);
            }
        }
        Cekilme c = cekilme;
        if (c != null && alan.su(x, z) && c.icinde(x + 0.5, z + 0.5)) {
            double ust = alan.zemin(x, z) - c.dusus(x + 0.5, z + 0.5, t);
            a.ekle(alan.taban(x, z), ust);
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

    /** Dalgaların sürükleme ivmesi; {@code cikti[0..1]} = (x, z). Etki varsa {@code true}. */
    public boolean itme(double x, double z, double minY, double maxY, double t, double[] cikti) {
        cikti[0] = 0;
        cikti[1] = 0;
        boolean var = false;
        for (int i = 0; i < dalgalar.size(); i++) {
            TsunamiDalgasi d = dalgalar.get(i);
            double y = d.yuzey(x, z, t);
            if (Double.isNaN(y) || y <= minY) {
                continue;
            }
            double oran = Math.min(1, (y - minY) / Math.max(0.1, maxY - minY));
            cikti[0] += d.yonX() * d.ivme() * oran;
            cikti[1] += d.yonZ() * d.ivme() * oran;
            var = true;
        }
        return var;
    }
}
