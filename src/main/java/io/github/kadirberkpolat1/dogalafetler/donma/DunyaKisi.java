package io.github.kadirberkpolat1.dogalafetler.donma;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.Tags;

/**
 * {@link KisAlani} için dünya biyom örneği: chunk ortasında deniz seviyesinin bir üstündeki gürültü biyomu
 * ({@code getUncachedNoiseBiome}). Chunk yüklemez ve yalnız seed'e bağlıdır: chunk yüklü olsun olmasın aynı sonucu verir.
 * Gürültü yüzeyi ({@code getBaseHeight}) bütün bir gürültü sütunu hesapladığı için kullanılmaz (ölçümde chunk başına
 * onlarca ms); dağlarda bu yükseklikte mağara biyomu çıkabilir, ama orada yükseklik bileşeni zaten tam kıştır. Soğuk =
 * karlı biyom ({@code Tags.Biomes.IS_SNOWY} ya da taban sıcaklık &lt; 0,15). Ana thread'de kullanılır.
 */
final class DunyaKisi implements KisAlani.ChunkOrnegi {
    private record Ornek(boolean soguk, float sicaklik) {
    }

    private static final int ONBELLEK_SINIRI = 200_000;
    private static KisAlani alan;
    private static ServerLevel alanDunyasi;

    private final ServerLevel level;
    private final Long2ObjectOpenHashMap<Ornek> onbellek = new Long2ObjectOpenHashMap<>();

    private DunyaKisi(ServerLevel level) {
        this.level = level;
    }

    static KisAlani alan(ServerLevel level) {
        if (alan == null || alanDunyasi != level) {
            KisMaskesi maske = new KisMaskesi(level.getSeed(), DonmaAyarlari.kisOrani, DonmaAyarlari.kisDalgaBoyu,
                    DonmaAyarlari.kisBant);
            alan = new KisAlani(maske, new DunyaKisi(level), DonmaAyarlari.kisAyar);
            alanDunyasi = level;
        }
        return alan;
    }

    static void sifirla() {
        alan = null;
        alanDunyasi = null;
    }

    /** Chunk yüklemeden gürültü yüzeyi. */
    static int tabanYuksekligi(ServerLevel level, int x, int z) {
        ServerChunkCache c = level.getChunkSource();
        return c.getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, c.randomState());
    }

    private Ornek ornek(int cx, int cz) {
        long anahtar = ChunkPos.asLong(cx, cz);
        Ornek o = onbellek.get(anahtar);
        if (o == null) {
            int x = cx * 16 + 8;
            int z = cz * 16 + 8;
            int y = level.getSeaLevel() + 1;
            Holder<Biome> biyom = level.getUncachedNoiseBiome(x >> 2, y >> 2, z >> 2);
            float sicaklik = biyom.value().getBaseTemperature();
            o = new Ornek(biyom.is(Tags.Biomes.IS_SNOWY) || sicaklik < 0.15f, sicaklik);
            if (onbellek.size() >= ONBELLEK_SINIRI) {
                onbellek.clear();
            }
            onbellek.put(anahtar, o);
        }
        return o;
    }

    @Override
    public boolean soguk(int cx, int cz) {
        return ornek(cx, cz).soguk();
    }

    @Override
    public double sicaklik(int cx, int cz) {
        return ornek(cx, cz).sicaklik();
    }
}
