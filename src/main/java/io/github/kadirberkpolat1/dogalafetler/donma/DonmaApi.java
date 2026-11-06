package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaTablosuYukleyici.Tablo;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.DonmaHedefi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.KisAlani;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.Tags;

/**
 * Donma sisteminin diğer alt sistemlere açık sorguları ({@code .vibe/kararlar/donma.md} §A). Hiçbiri chunk yüklemez; yalnız
 * sunucu ana thread'inde çağrılır.
 */
public final class DonmaApi {
    private DonmaApi() {
    }

    /**
     * Balıkçılık (madde 15): {@code konum} su üstündeki buz mu — buz ya da ince buz ve altında (en çok kalın buz kadar
     * buzdan sonra) su kaynağı var. Doğal ve oyuncunun su üstüne koyduğu buz da sayılır; altı su olmayan buz sayılmaz.
     */
    public static boolean buzYuzeyiMi(ServerLevel level, BlockPos konum) {
        Tablo tablo = DonmaTablosuYukleyici.etkin();
        if (tablo.suKaynak() == null || !level.isLoaded(konum)) {
            return false;
        }
        BlockPos.MutableBlockPos p = konum.mutable();
        int sinir = DonmaAyarlari.esikler().maksBuzKalinligi() + 2;
        for (int d = 0; d < sinir; d++) {
            BlockState s = level.getBlockState(p);
            if (s.is(tablo.suHedef()) || s.is(tablo.suInce())) {
                p.move(0, -1, 0);
                continue;
            }
            return d > 0 && s.is(tablo.suKaynak()) && s.getFluidState().isSource();
        }
        return false;
    }

    /**
     * Sıcaklık (madde 7, K0-24): sütunun kalıcı kış ağırlığı 0–1 (maske ⊕ soğuk biyom oranı ⊕ yükseklik bandı). Üst dünya
     * dışında 0. Yüzey Y'si yüklü chunk'ın yapraksız {@code MOTION_BLOCKING_NO_LEAVES} yükseklikten (K-D28), chunk yüklü
     * değilse {@code konum}'dan.
     */
    public static double kisAgirligi(Level level, BlockPos konum) {
        if (!(level instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) {
            return 0.0;
        }
        return KisBolgesi.alan(sl).chunk(konum.getX() >> 4, konum.getZ() >> 4)
                .agirlik(konum.getX(), konum.getZ(), yuzeyY(sl, konum));
    }

    /** Sütunun şimdiki hedef donma seviyesi (0–100; dönem ve kış ağırlığıyla). */
    public static int sutunHedefi(ServerLevel level, BlockPos konum) {
        DunyaDonemi d = DunyaDonemi.al(level);
        KisAlani.Chunk k = KisBolgesi.alan(level).chunk(konum.getX() >> 4, konum.getZ() >> 4);
        int y = yuzeyY(level, konum);
        return DonmaHedefi.sutunHedefi(DonmaZamanlayici.hedefG(d), d.cozulmeDonemi(),
                k.agirlik(konum.getX(), konum.getZ(), y), k.ofset(konum.getX(), konum.getZ()));
    }

    /**
     * Tsunami (N10, N14): {@code merkez} çevresindeki yüklü sütunlarda en az {@code enAz} açık (donmamış) su yüzeyi var mı.
     * 4 blokta bir örneklenir; yüklü olmayan chunk sayılmaz.
     */
    public static boolean acikSuVarMi(ServerLevel level, BlockPos merkez, int yaricap, int enAz) {
        Tablo tablo = DonmaTablosuYukleyici.etkin();
        if (tablo.suKaynak() == null) {
            return false;
        }
        int sayi = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dz = -yaricap; dz <= yaricap; dz += 4) {
            for (int dx = -yaricap; dx <= yaricap; dx += 4) {
                int x = merkez.getX() + dx;
                int z = merkez.getZ() + dz;
                LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (chunk == null) {
                    continue;
                }
                BlockState s = chunk.getBlockState(p.set(x, chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15,
                        z & 15), z));
                if (s.is(tablo.suKaynak()) && s.getFluidState().isSource() && ++sayi >= enAz) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@code /dogalafet kizilcuce bilgi} katkısı (KOMUTLAR): komutu yazanın sütunu kalıcı kış bölgesinde mi. */
    public static List<Component> kisBolgesiSatirlari(CommandSourceStack kaynak) {
        ServerLevel level = kaynak.getServer().overworld();
        BlockPos konum = BlockPos.containing(kaynak.getPosition());
        double w = kisAgirligi(level, konum);
        String anahtar = w >= 0.999 ? DonmaCevirileri.KIS_EVET : w <= 0.001 ? DonmaCevirileri.KIS_HAYIR
                : DonmaCevirileri.KIS_SINIR;
        return List.of(Component.translatable(anahtar, konum.getX(), konum.getZ(),
                String.format(Locale.ROOT, "%.3f", w), sutunHedefi(level, konum)));
    }

    /**
     * En yakın kalıcı kış sınırı (görsel tur için): {@code (x, z)} çevresinde 16 blokluk halkalarla, maske ağırlığının 0,5'i
     * 16 blok içinde (doğu ya da güney) kestiği, okyanus, dağ ve soğuk biyom olmayan ilk nokta. Yalnız maske gürültüsü
     * ve aday noktada bir gürültü biyomu okunur; chunk yüklenmez. Bulunamazsa {@code null}.
     */
    public static BlockPos kisSiniriBul(ServerLevel level, int x, int z, int yaricap) {
        KisAlani alan = KisBolgesi.alan(level);
        int y = level.getSeaLevel() + 1;
        for (int r = 0; r <= yaricap; r += 16) {
            int adim = Math.max(1, (int) Math.ceil(2 * Math.PI * r / 16));
            for (int k = 0; k < adim; k++) {
                double a = 2 * Math.PI * k / adim;
                int px = x + (int) Math.round(r * Math.cos(a));
                int pz = z + (int) Math.round(r * Math.sin(a));
                double w = alan.maskeNoktada(px, pz) - 0.5;
                if (w * (alan.maskeNoktada(px + 16, pz) - 0.5) > 0 && w * (alan.maskeNoktada(px, pz + 16) - 0.5) > 0) {
                    continue;
                }
                Holder<Biome> b = level.getUncachedNoiseBiome(px >> 2, y >> 2, pz >> 2);
                if (!b.is(BiomeTags.IS_OCEAN) && !b.is(BiomeTags.IS_MOUNTAIN) && !b.is(Tags.Biomes.IS_SNOWY)
                        && b.value().getBaseTemperature() >= 0.3f) {
                    return new BlockPos(px, y, pz);
                }
            }
        }
        return null;
    }

    /** Kış ağırlığının zemini: yapraksız en üst hareket engelleyen blok (D7); chunk yüklü değilse {@code konum}. */
    static int yuzeyY(ServerLevel level, BlockPos konum) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(konum.getX() >> 4, konum.getZ() >> 4);
        return chunk == null ? konum.getY()
                : chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, konum.getX() & 15, konum.getZ() & 15);
    }
}
