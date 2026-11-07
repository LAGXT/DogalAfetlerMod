package io.github.kadirberkpolat1.dogalafetler.client.su;

import io.github.kadirberkpolat1.dogalafetler.su.Cekilme;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;

/** Çekilme: gerçek suyun üst katmanlarını tesselatörde atlatır (chunk derleme iş parçacıklarında çağrılır). */
public final class SuGizleme {
    private static final AtomicLong GIZLENEN = new AtomicLong();

    private SuGizleme() {
    }

    /** Şimdiye kadar atlatılan sıvı bloğu (kanca çalışıyor mu tanısı). */
    public static long gizlenenSayisi() {
        return GIZLENEN.get();
    }

    public static boolean gizli(BlockAndTintGetter g, BlockPos pos, FluidState fs) {
        if (gizliMi(g, pos, fs)) {
            GIZLENEN.incrementAndGet();
            return true;
        }
        return false;
    }

    /** Gerçek su bloğu çekilmenin gizlediği üst katmanlarda mı (sayaçsız; yağmur da sorar). */
    public static boolean gizliMi(BlockGetter g, BlockPos pos, FluidState fs) {
        SanalSuDunyasi d = SanalSu.istemci;
        if (d == null || !fs.is(FluidTags.WATER)) {
            return false;
        }
        int k = 0;
        for (Cekilme c : d.cekilmeDizisi()) {
            k = Math.max(k, c.gizlenenKatman(pos.getX() + 0.5, pos.getZ() + 0.5));
        }
        if (k <= 0) {
            return false;
        }
        BlockPos.MutableBlockPos m = pos.mutable();
        for (int j = 1; j <= k; j++) {
            m.setY(pos.getY() + j);
            if (!g.getFluidState(m).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }
}
