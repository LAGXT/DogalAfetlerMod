package io.github.kadirberkpolat1.dogalafetler.yangin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Sanal su altındaki blok tutuşmaz ve yanan hücre söner (madde 8). Entegratör {@code SanalSu.suSeviyesi(level, x, z)} ile
 * bağlar: {@code (l, k) -> { double y = SanalSu.suSeviyesi(l, k.getX(), k.getZ()); return !Double.isNaN(y) && y > k.getY(); }}.
 */
@FunctionalInterface
public interface SanalSuSorgusu {
    SanalSuSorgusu BOS = (level, konum) -> false;

    boolean suAltinda(ServerLevel level, BlockPos konum);
}
