package io.github.kadirberkpolat1.dogalafetler.veri;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/** Kayıtlı bütün çeviri katkılarından tek bir dil dosyası üretir; aynı anahtar iki kez yazılırsa datagen hata verir. */
final class DilSaglayici extends LanguageProvider {
    static final String INGILIZCE = "en_us";
    static final String TURKCE = "tr_tr";

    private final String dil;
    private final List<Consumer<CeviriYazici>> katkilar;

    DilSaglayici(PackOutput cikti, String dil, List<Consumer<CeviriYazici>> katkilar) {
        super(cikti, DogalAfetler.MOD_ID, dil);
        this.dil = dil;
        this.katkilar = katkilar;
    }

    @Override
    protected void addTranslations() {
        boolean turkce = TURKCE.equals(dil);
        CeviriYazici yazici = (anahtar, ingilizce, turkceMetin) -> add(anahtar, turkce ? turkceMetin : ingilizce);
        for (Consumer<CeviriYazici> katki : katkilar) {
            katki.accept(yazici);
        }
    }
}
