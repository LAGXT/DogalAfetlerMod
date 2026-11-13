package io.github.kadirberkpolat1.dogalafetler.client.su;

import io.github.kadirberkpolat1.dogalafetler.su.Cekilme;
import io.github.kadirberkpolat1.dogalafetler.su.DenizAlani;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;

/**
 * Gerçek suyun tesselatörde atlatılan blokları (chunk derleme iş parçacıklarında çağrılır): çekilmenin gizlediği üst katmanlar
 * ve sel ya da deniz yükselmesinin sanal suyla örttüğü sütunların en üst su bloğu ({@link #ortu}).
 */
public final class SuGizleme {
    private static final AtomicLong GIZLENEN = new AtomicLong();
    private static final Long2IntOpenHashMap BOS = yeniOrtu();
    /**
     * Sanal suyla örtülen gerçek su sütunları: sütun anahtarı ({@link DenizAlani#anahtar}) → en üst gerçek su bloğunun Y'si. Sanal su
     * gerçek suyun yüzeyinden başlar; gerçek yüzey de çizilince iki su yüzeyi üst üste görünüyordu. Ana iş parçacığında kurulur
     * ({@link IstemciSu}), yayınlandıktan sonra değişmez; derleme iş parçacıkları yalnız okur.
     */
    private static volatile Long2IntOpenHashMap ortu = BOS;

    private SuGizleme() {
    }

    static Long2IntOpenHashMap yeniOrtu() {
        Long2IntOpenHashMap m = new Long2IntOpenHashMap();
        m.defaultReturnValue(Integer.MIN_VALUE);
        return m;
    }

    static Long2IntOpenHashMap ortu() {
        return ortu;
    }

    /** Yeni örtü kümesini yayınlar (değiştirilmemeli); boş ise örtü yok. */
    static void ortuAyarla(Long2IntOpenHashMap yeni) {
        ortu = yeni.isEmpty() ? BOS : yeni;
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

    /** Gerçek su bloğu çizilmiyor mu: çekilmenin gizlediği üst katmanlarda ya da sanal suyun örttüğü yüzeyde (sayaçsız; yağmur da sorar). */
    public static boolean gizliMi(BlockGetter g, BlockPos pos, FluidState fs) {
        SanalSuDunyasi d = SanalSu.istemci;
        if (d == null || !fs.is(FluidTags.WATER)) {
            return false;
        }
        Long2IntOpenHashMap o = ortu;
        if (!o.isEmpty() && o.get(DenizAlani.anahtar(pos.getX(), pos.getZ())) == pos.getY()) {
            return true;
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
