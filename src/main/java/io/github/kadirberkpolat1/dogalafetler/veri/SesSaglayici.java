package io.github.kadirberkpolat1.dogalafetler.veri;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.common.data.SoundDefinitionsProvider;

/**
 * {@code assets/dogalafetler/sounds.json} üreticisi. Alt sistemler ses tanımlarını {@link #tanimla} ile ekler. Hiç tanım
 * yokken Forge dosya yazmaz; ilk ses eklendiğinde dosya oluşur.
 */
public final class SesSaglayici extends SoundDefinitionsProvider {
    private final List<Consumer<SesSaglayici>> katkilar;

    SesSaglayici(PackOutput cikti, ExistingFileHelper mevcutDosyalar, List<Consumer<SesSaglayici>> katkilar) {
        super(cikti, DogalAfetler.MOD_ID, mevcutDosyalar);
        this.katkilar = katkilar;
    }

    @Override
    public void registerSounds() {
        for (Consumer<SesSaglayici> katki : katkilar) {
            katki.accept(this);
        }
    }

    /** {@code sounds.json}'a bir ses olayı ekler; {@code ad} mod ad alanı olmadan yazılır (ör. {@code siren.genel}). */
    public void tanimla(String ad, SoundDefinition tanim) {
        add(ad, tanim);
    }
}
