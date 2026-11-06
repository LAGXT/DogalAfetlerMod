package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Yon;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

/**
 * "İçten Isınma" / "Serinlik" durum efekti (madde 14): yalnız gösterge. Sıcaklığı efekt değil
 * {@code VucutSicakligi}'nın süreli etkisi uygular; efekt ikonla kalan süreyi ve amplifier'la büyüklük kademesini gösterir.
 * Bu yüzden süt onu silmez (sıcaklık etkisi sürerken ikonun kaybolması yanıltırdı). Durumdan bağımsız iyi ya da kötü
 * olmadığı için (soğukta kurtarır, sıcakta pişirir) kategori nötrdür.
 */
public final class SicaklikEtkisi extends MobEffect {
    public static final int ISINMA_RENGI = 0xE8782C;
    public static final int SERINLIK_RENGI = 0x6FCDEB;

    private final Yon yon;

    SicaklikEtkisi(Yon yon) {
        super(MobEffectCategory.NEUTRAL, yon == Yon.ISINMA ? ISINMA_RENGI : SERINLIK_RENGI);
        this.yon = yon;
    }

    public Yon yon() {
        return yon;
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}
