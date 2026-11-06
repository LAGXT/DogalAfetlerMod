package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Zırh yalıtımı (madde 7e, N3, N4). Toplam yalıtım dört zırh parçasının toplamıdır: giyilen her zırh parçası az, her yünlü
 * parça orta, her Tüy Upgrade'i küçük bir pay ekler. Yün ayrıca soğuma yönündeki hızı parça başına {@code yunSogumaOrani}
 * keser; aşırı soğukta bu kesinti {@code asiriSogukYunVerimi} ile küçülür ve donma yalnız yavaşlar. Termostat (madde 13c)
 * yalıtımı değiştirmez, vücudu nötre çeker; çekim uç ortamlarda azalır ama sıfıra inmez.
 */
public final class Yalitim {
    private Yalitim() {
    }

    /**
     * Giyilen zırhın yalıtım girdileri.
     *
     * @param zirhParcasi        giyilen zırh parçası sayısı (0–4)
     * @param yunluParca         bunlardan yünlü olanlar
     * @param toplamTuy          dört parçadaki Tüy Upgrade'lerinin toplamı
     * @param termostatParcasi   termostatlı parçalar (Faz 3 kancası)
     */
    public record Girdi(int zirhParcasi, int yunluParca, int toplamTuy, int termostatParcasi) {
        public static final Girdi CIPLAK = new Girdi(0, 0, 0, 0);

        public Girdi {
            if (zirhParcasi < 0 || zirhParcasi > 4) {
                throw new IllegalArgumentException("zırh parçası 0–4 olmalı: " + zirhParcasi);
            }
            if (yunluParca < 0 || yunluParca > zirhParcasi || termostatParcasi < 0 || termostatParcasi > zirhParcasi) {
                throw new IllegalArgumentException("yünlü/termostatlı parça zırh sayısını aşamaz");
            }
            if (toplamTuy < 0) {
                throw new IllegalArgumentException("tüy sayısı negatif: " + toplamTuy);
            }
        }
    }

    /**
     * @param zirhBasina            giyilen her zırh parçasının yalıtımı
     * @param yunBasina             her yünlü parçanın ek yalıtımı (sıcakta dezavantaj, N4)
     * @param tuyBasina             her Tüy Upgrade'inin yalıtımı
     * @param yunSogumaOrani        yünlü parça başına soğuma hızı kesintisi (N3: %25)
     * @param asiriSogukYunVerimi   aşırı soğukta yün kesintisinin kalanı (0–1; 1'in altında donma yalnız yavaşlar)
     * @param termostatCekimi       termostatlı parça başına nötre çekme hızı (vücut birimi/sn)
     * @param termostatIlimliSinir  bu ortam eşdeğerine (|E|, vücut birimi) kadar çekim tam
     * @param termostatUcSinir      bu |E|'den sonra çekim taban oranında
     * @param termostatTaban        uç ortamda kalan çekim oranı (&gt; 0: hiç kaybolmaz)
     */
    public record Ayarlar(double zirhBasina, double yunBasina, double tuyBasina, double yunSogumaOrani,
            double asiriSogukYunVerimi, double termostatCekimi, double termostatIlimliSinir, double termostatUcSinir,
            double termostatTaban) {
        public static final Ayarlar VARSAYILAN = new Ayarlar(0.05, 0.25, 0.04, 0.25, 0.75, 0.04, 40, 120, 0.25);

        public Ayarlar {
            if (termostatUcSinir <= termostatIlimliSinir) {
                termostatUcSinir = termostatIlimliSinir + 1;
            }
        }
    }

    /** Toplam yalıtım I (zırh + yün + tüy). */
    public static double toplam(Girdi g, Ayarlar a) {
        return kuruToplam(g, a) + g.yunluParca() * a.yunBasina();
    }

    /** Yünün yalıtım payı hariç toplam (soğuma yönünde yün kendi oranıyla ayrıca uygulanır). */
    public static double kuruToplam(Girdi g, Ayarlar a) {
        return g.zirhParcasi() * a.zirhBasina() + g.toplamTuy() * a.tuyBasina();
    }

    /** Soğuma hızının çarpanı: normalde {@code 1 − 0,25·n}; aşırı soğukta kesinti verimle küçülür. */
    public static double yunSogumaCarpani(int yunluParca, boolean asiriSoguk, Ayarlar a) {
        double kesinti = Math.min(1.0, yunluParca * a.yunSogumaOrani());
        if (asiriSoguk) {
            kesinti *= Math.max(0.0, Math.min(1.0, a.asiriSogukYunVerimi()));
        }
        return 1.0 - kesinti;
    }

    /**
     * Termostatın bu saniyedeki nötre çekme katkısı (vücut birimi/sn; N4, madde 13c). Etki ılımlı ortamda tam, ortam
     * eşdeğeri uçlara gittikçe doğrusal azalır, tabanda kalır; vücudu nötrün ötesine geçirmez.
     *
     * @param vucut   vücut sıcaklığı (vücut birimi)
     * @param ortamE  ortamın vücut birimi karşılığı
     */
    public static double termostatCekimi(double vucut, double ortamE, int termostatParcasi, Ayarlar a) {
        if (termostatParcasi <= 0 || vucut == 0) {
            return 0;
        }
        double uzaklik = Math.abs(ortamE);
        double oran;
        if (uzaklik <= a.termostatIlimliSinir()) {
            oran = 1.0;
        } else if (uzaklik >= a.termostatUcSinir()) {
            oran = a.termostatTaban();
        } else {
            double f = (uzaklik - a.termostatIlimliSinir()) / (a.termostatUcSinir() - a.termostatIlimliSinir());
            oran = 1.0 + (a.termostatTaban() - 1.0) * f;
        }
        double cekim = Math.min(Math.abs(vucut), termostatParcasi * a.termostatCekimi() * oran);
        return vucut < 0 ? cekim : -cekim;
    }
}
