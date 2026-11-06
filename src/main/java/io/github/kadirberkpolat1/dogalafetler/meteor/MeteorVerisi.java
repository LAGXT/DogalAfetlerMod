package io.github.kadirberkpolat1.dogalafetler.meteor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Boyut başına bitmemiş krater işleri ve magma astarının soğuması ({@code dogalafetler_meteor}, M-25): chunk'ı boşalan ya da
 * sunucu kapanırken süren iş kaybolmaz, chunk yüklenince tamamlanır. Zamanlar oyun tick'i ({@code getGameTime}).
 */
final class MeteorVerisi extends SavedData {
    static final String AD = "dogalafetler_meteor";
    private static final int BICIM_SURUMU = 1;

    record Soguma(BlockPos konum, long zaman, BlockState yeni) {
    }

    final List<KraterIsi> kraterler = new ArrayList<>();
    final List<Soguma> sogumalar = new ArrayList<>();

    static MeteorVerisi yukle(CompoundTag etiket) {
        MeteorVerisi veri = new MeteorVerisi();
        ListTag kraterler = etiket.getList("kraterler", Tag.TAG_COMPOUND);
        for (int i = 0; i < kraterler.size(); i++) {
            veri.kraterler.add(KraterIsi.oku(kraterler.getCompound(i)));
        }
        ListTag sogumalar = etiket.getList("sogumalar", Tag.TAG_COMPOUND);
        for (int i = 0; i < sogumalar.size(); i++) {
            CompoundTag s = sogumalar.getCompound(i);
            Block blok = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(s.getString("yeni")));
            veri.sogumalar.add(new Soguma(BlockPos.of(s.getLong("konum")), s.getLong("zaman"), blok.defaultBlockState()));
        }
        return veri;
    }

    @Override
    public CompoundTag save(CompoundTag etiket) {
        etiket.putInt("surum", BICIM_SURUMU);
        ListTag kraterListesi = new ListTag();
        for (KraterIsi is : kraterler) {
            CompoundTag t = new CompoundTag();
            is.yaz(t);
            kraterListesi.add(t);
        }
        etiket.put("kraterler", kraterListesi);
        ListTag sogumaListesi = new ListTag();
        for (Soguma s : sogumalar) {
            CompoundTag t = new CompoundTag();
            t.putLong("konum", s.konum().asLong());
            t.putLong("zaman", s.zaman());
            t.putString("yeni", BuiltInRegistries.BLOCK.getKey(s.yeni().getBlock()).toString());
            sogumaListesi.add(t);
        }
        etiket.put("sogumalar", sogumaListesi);
        return etiket;
    }
}
