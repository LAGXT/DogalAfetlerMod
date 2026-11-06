package io.github.kadirberkpolat1.dogalafetler.veri;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ParticleDescriptionProvider;

/**
 * Bütün alt sistemlerin parçacık tanımlarını ({@code assets/dogalafetler/particles/<ad>.json}) yazan tek sağlayıcı (K-E25).
 * Doku kareleri {@code textures/particle/} altında bulunmalıdır; Forge eksik dokuda datagen'i durdurur.
 */
final class ParcacikTanimlari extends ParticleDescriptionProvider {
    /** Bir parçacığın doku kökü ve kare sayısı. */
    record Tanim(Supplier<? extends ParticleType<?>> tur, String doku, int adet) {
        Tanim {
            if (adet < 1) {
                throw new IllegalArgumentException("parçacık karesi en az 1: " + doku);
            }
        }
    }

    private final List<Tanim> tanimlar;

    ParcacikTanimlari(PackOutput cikti, ExistingFileHelper dosyalar, List<Tanim> tanimlar) {
        super(cikti, dosyalar);
        this.tanimlar = tanimlar;
    }

    @Override
    protected void addDescriptions() {
        for (Tanim t : tanimlar) {
            spriteSet(t.tur().get(), DogalAfetler.konum(t.doku()), t.adet(), false);
        }
    }

    /** DataGenerator sağlayıcıları ada göre ayırır (K-E5). */
    @Override
    public String getName() {
        return "Doğal Afetler parçacık tanımları";
    }
}
