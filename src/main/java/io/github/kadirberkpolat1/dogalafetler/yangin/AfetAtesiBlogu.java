package io.github.kadirberkpolat1.dogalafetler.yangin;

import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Yangın yöneticisinin görünür ateşi (Y-01): vanilla ateş dokusu, ışık 15 ve {@code BaseFireBlock} temas hasarı vardır
 * ama kendi tick'i yoktur. Yayılma, sönme ve yakıtın tükenmesi yalnız {@link YanginYoneticisi} kuyruğundan gelir; böylece
 * {@code doFireTick} açıkken de bütçe, tavan ve {@code afetBlokHasari} aşılmaz.
 */
public final class AfetAtesiBlogu extends BaseFireBlock {
    public AfetAtesiBlogu(Properties ozellikler) {
        super(ozellikler, 1.0F);
    }

    @Override
    protected boolean canBurn(BlockState durum) {
        return true;
    }
}
