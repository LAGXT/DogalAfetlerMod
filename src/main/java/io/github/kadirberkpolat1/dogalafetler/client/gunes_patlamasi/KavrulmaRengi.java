package io.github.kadirberkpolat1.dogalafetler.client.gunes_patlamasi;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik.GunesGorselEgrileri;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Yaprak ve çimen kuruması (tasarım §2 "yapraklar kahverengiye döner", blok değişmez): BUYUYEN evrenin 2. gününden sonra
 * biyom rengi istemcide soluk samana (çimen) ve kahverengiye (yaprak) karışır. Renk işleyicileri vanilla'nın
 * {@code BlockColors.createDefault} kayıtlarının aynısını hesaplayıp karışımı ekler; kuruluk 0 iken vanilla rengiyle bire
 * bir aynıdır. Dünya dışında (eşya simgesi, {@code level == null}) dokunulmaz. Karışım 8 basamaklıdır; basamak
 * değişince chunk'lar bir kez yeniden örülür (5 günlük süreçte en çok 8 + dönüş).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class KavrulmaRengi {
    /** Geçerli karışım payı; chunk örme thread'leri okur. */
    private static volatile float pay;
    private static int basamak;

    private KavrulmaRengi() {
    }

    @SubscribeEvent
    public static void renkler(RegisterColorHandlersEvent.Block olay) {
        olay.register((s, l, p, i) -> cimen(l != null && p != null ? BiomeColors.getAverageGrassColor(l, p)
                : GrassColor.get(0.5, 1.0), l), Blocks.GRASS_BLOCK, Blocks.FERN, Blocks.GRASS, Blocks.POTTED_FERN);
        olay.register((s, l, p, i) -> cimen(l != null && p != null ? BiomeColors.getAverageGrassColor(l,
                s.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? p.below() : p) : -1, l),
                Blocks.LARGE_FERN, Blocks.TALL_GRASS);
        olay.register((s, l, p, i) -> yaprak(l != null && p != null ? BiomeColors.getAverageFoliageColor(l, p)
                : FoliageColor.getDefaultColor(), l), Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES,
                Blocks.DARK_OAK_LEAVES, Blocks.VINE, Blocks.MANGROVE_LEAVES);
        olay.register((s, l, p, i) -> yaprak(FoliageColor.getEvergreenColor(), l), Blocks.SPRUCE_LEAVES);
        olay.register((s, l, p, i) -> yaprak(FoliageColor.getBirchColor(), l), Blocks.BIRCH_LEAVES);
    }

    private static int cimen(int renk, BlockAndTintGetter l) {
        return l == null ? renk : GunesGorselEgrileri.karistir(renk, GunesGorselEgrileri.KURU_CIMEN, pay);
    }

    private static int yaprak(int renk, BlockAndTintGetter l) {
        return l == null ? renk : GunesGorselEgrileri.karistir(renk, GunesGorselEgrileri.KURU_YAPRAK, pay);
    }

    /** İstemci tick'i: kuruluk basamağı değişti mi; değiştiyse karışımı günceller ve chunk'ları yeniden örer. */
    static void denetle(Minecraft mc, DonemPaketi d) {
        float kuruluk = d.gunesEvresi() == GunesEvresi.BUYUYEN ? GunesGorselEgrileri.kuruluk(d.evreIlerlemesi()) : 0f;
        int yeni = GunesGorselEgrileri.kurulukBasamagi(kuruluk);
        if (yeni == basamak) {
            return;
        }
        basamak = yeni;
        pay = GunesGorselEgrileri.basamakPayi(yeni);
        if (mc.level != null) {
            mc.levelRenderer.allChanged();
        }
        DogalAfetler.LOGGER.info("[gunes] yaprak/çimen kuruluğu basamak {} (pay {})", yeni, pay);
    }

    static int basamak() {
        return basamak;
    }
}
