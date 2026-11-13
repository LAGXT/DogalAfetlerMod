package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.adim;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Çekirdek config anahtarlarının göç tablosu (K-E61, K-C2 rev. 2; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır.
 */
public final class CekirdekAyarGocu {
    public static final String SURUM_YOLU = "cekirdek.ayarSurumu";
    public static final int GUNCEL = 4;

    /**
     * Sürüme göre sıralı. Sürüm 1: kullanıcı ilk afeti ilk oyun gününde görsün (AS-11). Sürüm 2 (K-E67): afetler daha erken
     * ve günle sıklaşarak gelir, seçimden görünmeye ~30 sn, seviye 4-5 erken açılır. Sürüm 3 (K-E72): seviye 4-5 daha erken açılır ve nadirlik katsayısı 1'i aşıyor, nadir dönem
     * olaylarının ağırlığı ve gün çarpanı yükselir. Sürüm 4 (kullanıcı isteği "5. seviyeler daha sık, ay çarpması ve güneş
     * patlaması neredeyse hiç çıkmıyor"): nadirlik katsayısı 0,9'dan 1,8'e 100 günde çıkar (art arda 4-5 artık yasak değil,
     * seyreltilir; kod, ayar değil), ay ağırlığı 90, güneş 60, günlük oran 5,5 olur (dönem olayı gün çarpanı üssü de kodda). Önceki
     * sürümlerin değerleri sabit sayıdır (o günün varsayılanı); {@link SiddetAyarlari#VARSAYILAN} ve
     * {@link ZamanlayiciAyarlari#VARSAYILAN} sürüm 4'ün değerleridir (değişmeyen alanları göç tablosunda doğrudan okunur).
     */
    public static final List<SurumluGocPlani.Adim> ADIMLAR = adimlar();

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, ADIMLAR);

    private CekirdekAyarGocu() {
    }

    private static List<SurumluGocPlani.Adim> adimlar() {
        List<SurumluGocPlani.Adim> liste = new ArrayList<>();
        liste.add(adim(1, "cekirdek.zamanlayici.baslangicBeklemeTick", 6_000L, 24_000L));
        liste.add(adim(1, "cekirdek.zamanlayici.genelBeklemeTick", 6_000L, 12_000L));
        for (AfetTuru tur : AfetTuru.values()) {
            if (!tur.sunucuGeneli()) {
                liste.add(adim(1, turBeklemesi(tur), 24_000L, 48_000L));
            }
        }

        ZamanlayiciAyarlari z = ZamanlayiciAyarlari.VARSAYILAN;
        liste.add(adim(2, "cekirdek.zamanlayici.gunlukAfetOrani", 5.0, 0.8));
        liste.add(adim(2, "cekirdek.zamanlayici.genelBeklemeTick", z.genelBeklemeTick(), 6_000L));
        liste.add(adim(2, "cekirdek.zamanlayici.siddetBeklemeCarpani", z.siddetBeklemeCarpani(), 0.25));
        liste.add(adim(2, "cekirdek.zamanlayici.baslangicBeklemeTick", z.baslangicBeklemeTick(), 6_000L));
        liste.add(adim(2, "cekirdek.zamanlayici.ilkAfetGarantiTick", z.ilkAfetGarantiTick(), 24_000L));
        for (AfetTuru tur : AfetTuru.values()) {
            if (!tur.sunucuGeneli()) {
                liste.add(adim(2, turBeklemesi(tur), z.turBekleme(tur), 24_000L));
            }
        }
        liste.add(adim(2, "cekirdek.onUyari.enAzTick", OnUyariPenceresi.VARSAYILAN_EN_AZ, 900));
        liste.add(adim(2, "cekirdek.onUyari.enCokTick", OnUyariPenceresi.VARSAYILAN_EN_COK, 1800));
        liste.add(adim(2, "cekirdek.onUyari.erkenBildirimTick", OnUyariPenceresi.VARSAYILAN_ERKEN_BILDIRIM, 600));
        liste.add(adim(2, "cekirdek.siddet.seviye2Gunu", 1, 10));
        liste.add(adim(2, "cekirdek.siddet.seviye3Gunu", 4, 25));
        liste.add(adim(2, "cekirdek.siddet.seviye4Gunu", 9, 50));
        liste.add(adim(2, "cekirdek.siddet.seviye5Gunu", 16, 80));
        liste.add(adim(2, "cekirdek.siddet.nadirlikBaslangic", 0.5, 0.35));
        liste.add(adim(2, "cekirdek.siddet.nadirlikSon", 0.8, 0.6));
        liste.add(adim(2, "cekirdek.siddet.nadirlikGecisGunu", 60.0, 120.0));
        liste.add(adim(2, "cekirdek.siddet.acilisRampaGunu", 3.0, 15.0));
        liste.add(adim(2, "cekirdek.siddet.acilisBaslangicOrani", 0.4, 0.2));

        SiddetAyarlari s = SiddetAyarlari.VARSAYILAN;
        liste.add(adim(3, "cekirdek.siddet.seviye3Gunu", s.seviye3Gunu(), 4));
        liste.add(adim(3, "cekirdek.siddet.seviye4Gunu", s.seviye4Gunu(), 9));
        liste.add(adim(3, "cekirdek.siddet.seviye5Gunu", s.seviye5Gunu(), 16));
        liste.add(adim(3, "cekirdek.siddet.nadirlikBaslangic", 0.7, 0.5));
        liste.add(adim(3, "cekirdek.siddet.nadirlikSon", 1.5, 0.8));
        liste.add(adim(3, "cekirdek.siddet.nadirlikGecisGunu", 150.0, 60.0));
        liste.add(adim(3, "cekirdek.zamanlayici.siklikArtisCarpani", z.siklikArtisCarpani(), 8.0));
        liste.add(adim(3, "cekirdek.zamanlayici.siklikArtisGunu", z.siklikArtisGunu(), 45.0));
        liste.add(adim(3, agirlik(AfetTuru.AY_CARPMASI), 50, 32));
        liste.add(adim(3, agirlik(AfetTuru.GUNES_PATLAMASI), 35, 20));

        liste.add(adim(4, "cekirdek.siddet.nadirlikBaslangic", s.nadirlikBaslangic(), 0.7));
        liste.add(adim(4, "cekirdek.siddet.nadirlikSon", s.nadirlikSon(), 1.5));
        liste.add(adim(4, "cekirdek.siddet.nadirlikGecisGunu", s.nadirlikGecisGunu(), 150.0));
        liste.add(adim(4, "cekirdek.zamanlayici.gunlukAfetOrani", z.gunlukAfetOrani(), 5.0));
        liste.add(adim(4, agirlik(AfetTuru.AY_CARPMASI), z.agirlik(AfetTuru.AY_CARPMASI), 50));
        liste.add(adim(4, agirlik(AfetTuru.GUNES_PATLAMASI), z.agirlik(AfetTuru.GUNES_PATLAMASI), 35));
        return List.copyOf(liste);
    }

    private static String turBeklemesi(AfetTuru tur) {
        return "cekirdek.afetler." + tur.kimlik() + ".beklemeTick";
    }

    private static String agirlik(AfetTuru tur) {
        return "cekirdek.afetler." + tur.kimlik() + ".agirlik";
    }

    /** {@link SurumluGocPlani#plan} bu tabloyla. */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        return PLAN.plan(surum, oku);
    }
}
