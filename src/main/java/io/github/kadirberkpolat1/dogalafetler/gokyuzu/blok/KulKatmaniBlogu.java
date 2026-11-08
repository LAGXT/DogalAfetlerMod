package io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok;

import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarKayitlari;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pulsar külü (PL-3 a): kar katmanı gibi 1–8 kat, ama erimez (rastgele tick yok; özellikleri kaydı kurar). Üstünde
 * yürüyen varlık istemcide {@code kul_tozu} kaldırır; 1 katlı külün çarpışma şekli yok, bu yüzden adım değil
 * {@link #entityInside} kullanılır. Pulsar güçlüyken ({@link PulsarKayitlari#korGorunur} kancası, istemci kurulumu
 * doldurur) seyrek {@code kor} kıvılcımı yükselir.
 */
public final class KulKatmaniBlogu extends SnowLayerBlock {
    /** {@code animateTick} başına kor olasılığının paydası. */
    private static final int KOR_SEYREKLIGI = 24;
    /** Toz için yatay hızın karesi alt sınırı (durana toz kalkmaz). */
    private static final double TOZ_HIZI = 1e-3;

    public KulKatmaniBlogu(Properties ozellikler) {
        super(ozellikler);
    }

    @Override
    public void entityInside(BlockState durum, Level level, BlockPos konum, Entity varlik) {
        if (!level.isClientSide || varlik.getDeltaMovement().horizontalDistanceSqr() < TOZ_HIZI
                || level.random.nextInt(3) != 0) {
            return;
        }
        double ust = konum.getY() + durum.getValue(LAYERS) * 2 / 16.0;
        level.addParticle(PulsarKayitlari.KUL_TOZU.get(), varlik.getX() + (level.random.nextDouble() - 0.5) * 0.4, ust,
                varlik.getZ() + (level.random.nextDouble() - 0.5) * 0.4, (level.random.nextDouble() - 0.5) * 0.06,
                0.02 + level.random.nextDouble() * 0.03, (level.random.nextDouble() - 0.5) * 0.06);
    }

    @Override
    public void animateTick(BlockState durum, Level level, BlockPos konum, RandomSource r) {
        Supplier<Boolean> kanca = PulsarKayitlari.korGorunur;
        if (kanca == null || r.nextInt(KOR_SEYREKLIGI) != 0 || !Boolean.TRUE.equals(kanca.get())) {
            return;
        }
        double ust = konum.getY() + durum.getValue(LAYERS) * 2 / 16.0;
        level.addParticle(PulsarKayitlari.KOR.get(), konum.getX() + r.nextDouble(), ust, konum.getZ() + r.nextDouble(),
                (r.nextDouble() - 0.5) * 0.02, 0.04 + r.nextDouble() * 0.04, (r.nextDouble() - 0.5) * 0.02);
    }
}
