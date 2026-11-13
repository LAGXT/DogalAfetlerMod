package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.adim;
import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.listeAdimi;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Sıcaklık config anahtarlarının göç tablosu (S3-09; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır. Varsayılan kaynak listesinin tek kaynağı da burasıdır ({@link #KAYNAKLAR}).
 */
public final class SicaklikAyarGocu {
    public static final String SURUM_YOLU = "sicaklik.ayarSurumu";
    public static final int GUNCEL = 2;

    /** Sürüm 0'ın (sıcaklık-2) varsayılan ısı/soğuk kaynakları. */
    public static final List<String> KAYNAKLAR_0 = List.of("minecraft:lava=40:6", "minecraft:fire=25:4",
            "minecraft:soul_fire=20:4", "minecraft:campfire=22:5", "minecraft:soul_campfire=16:5",
            "minecraft:furnace=10:3", "minecraft:blast_furnace=12:3", "minecraft:smoker=8:3",
            "minecraft:magma_block=12:3", "minecraft:torch=4:2", "minecraft:wall_torch=4:2", "minecraft:lantern=3:2",
            "minecraft:jack_o_lantern=2:2", "minecraft:ice=-4:3", "minecraft:packed_ice=-6:3",
            "minecraft:blue_ice=-10:4", "minecraft:snow_block=-2:2", "minecraft:powder_snow=-5:2");

    /** Güncel varsayılan kaynaklar: sürüm 0 + kömürleşmiş odunlar (S3-05, kısık ama sürekli ısı). */
    public static final List<String> KAYNAKLAR = ekle(KAYNAKLAR_0, "dogalafetler:komurlesmis_kutuk=2:2",
            "dogalafetler:komurlesmis_tahta=1.5:2");

    /**
     * Sürüme göre sıralı. 1: sıcaklık-3 (°C ölçeğine uyan eşikler). 2: hızlı ısı alışverişi (eşlenme hızı), güçlü tüy,
     * gerçekten hissedilen bunalma/yanma açlığı.
     */
    public static final List<SurumluGocPlani.Adim> ADIMLAR = List.of(
            adim(1, "sicaklik.esikler.notrBant", 10.0, 15.0),
            adim(1, "sicaklik.esikler.soguk", -20.0, -30.0),
            adim(1, "sicaklik.esikler.cokSoguk", -40.0, -55.0),
            adim(1, "sicaklik.esikler.donmaHasari", -70.0, -75.0),
            adim(1, "sicaklik.esikler.sicak", 20.0, 30.0),
            adim(1, "sicaklik.esikler.cokSicak", 50.0, 55.0),
            adim(1, "sicaklik.esikler.yanmaHasari", 70.0, 75.0),
            adim(2, "sicaklik.isi.esitlenmeHizi", 0.02, 0.006),
            adim(2, "sicaklik.yalitim.tuyBasina", 0.10, 0.04),
            adim(2, "sicaklik.esikler.bunalmaAclik", 0.15, 0.04),
            adim(2, "sicaklik.esikler.yanmaAclik", 0.30, 0.08));

    /** 1: kaynak listesine kömür satırları (eski varsayılan aynen duruyorsa). */
    public static final List<SurumluGocPlani.ListeAdimi> LISTE_ADIMLARI = List.of(
            listeAdimi(1, "sicaklik.kaynaklar", KAYNAKLAR, KAYNAKLAR_0));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, ADIMLAR, LISTE_ADIMLARI);

    private SicaklikAyarGocu() {
    }

    /** {@link SurumluGocPlani#plan} bu tabloyla. */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        return PLAN.plan(surum, oku);
    }

    /** {@link SurumluGocPlani#listePlani} bu tabloyla. */
    public static Map<String, List<String>> listePlani(int surum, Function<String, Object> oku) {
        return PLAN.listePlani(surum, oku);
    }

    private static List<String> ekle(List<String> liste, String... satirlar) {
        List<String> sonuc = new ArrayList<>(liste);
        sonuc.addAll(List.of(satirlar));
        return List.copyOf(sonuc);
    }
}
