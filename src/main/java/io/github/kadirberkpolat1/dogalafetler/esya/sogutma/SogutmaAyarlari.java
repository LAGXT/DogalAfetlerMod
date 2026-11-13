package io.github.kadirberkpolat1.dogalafetler.esya.sogutma;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code sogutma} SERVER config bölümü: Soğutma Plakası'nın (zırh eki) ve Serin Kolye'nin ortamı çektiği serin hedef ve
 * güçleri. Termostat Plakası'nın tersidir: termostat vucudu nötre çeker, soğutma ortamı yalnız hedeften sıcakken hedefe doğru
 * serinletir (hedefin altına inmez, soğuk ortamı daha da soğutmaz).
 */
public final class SogutmaAyarlari {
    private static AyarKaydi.Bolum<SogutmaAyarlari> bolum;

    private final ForgeConfigSpec.DoubleValue hedefC;
    private final ForgeConfigSpec.DoubleValue plakaParcaBasinaC;
    private final ForgeConfigSpec.DoubleValue kolyeC;

    private SogutmaAyarlari(ForgeConfigSpec.Builder k) {
        hedefC = k.comment("Soğutucu eşyaların ortamı çektiği serin hedef sıcaklık (°C); bunun altındaki ortama dokunulmaz.")
                .defineInRange("hedefC", 20.0, -10.0, 40.0);
        plakaParcaBasinaC = k.comment("Soğutmalı (Soğutma Plakalı) her zırh parçasının ortamı en çok ne kadar serinlettiği (°C).")
                .defineInRange("plakaParcaBasinaC", 2.5, 0.0, 20.0);
        kolyeC = k.comment("Envanterde Serin Kolye taşıyan oyuncunun ortamının en çok serinlemesi (°C).")
                .defineInRange("kolyeC", 6.0, 0.0, 30.0);
    }

    public static void kaydet() {
        bolum = AyarKaydi.sunucuBolumu("sogutma", "Soğutucu eşyalar (Soğutma Plakası, Serin Kolye)", SogutmaAyarlari::new);
    }

    private static SogutmaAyarlari s() {
        return bolum.get();
    }

    public static double hedefC() {
        return s().hedefC.get();
    }

    public static double plakaParcaBasinaC() {
        return s().plakaParcaBasinaC.get();
    }

    public static double kolyeC() {
        return s().kolyeC.get();
    }
}
