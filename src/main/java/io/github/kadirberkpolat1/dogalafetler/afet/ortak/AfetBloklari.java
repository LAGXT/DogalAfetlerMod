package io.github.kadirberkpolat1.dogalafetler.afet.ortak;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.ortak.mantik.BlokRolu;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Afetlerin ortak blok rolleri (N12). {@code #dogalafetler:afet_dekor}: asılı ya da dayalı süs ve küçük yapı blokları (fener,
 * zincir, meşale, tabela, bayrak, saksı, mum, halı, düğme, basınç plakası, merdiven, ahşap kapı, kapak ve çit); afet
 * bunları sertliğe bakmadan kırar ya da düşürür, veri paketiyle genişler. Demir kapı/kapak ve nether tuğla çit yapının sağlam
 * parçasıdır, etikette değil. Envanterli blok varlıkları (fıçı, sandık, fırın) {@link BlokRolu#KAP}: kırılırsa içerik
 * dökülür. Bu etiketin tek yazarı bu sınıfın sağlayıcısıdır (K-E12).
 */
public final class AfetBloklari {
    public static final TagKey<Block> DEKOR = TagKey.create(Registries.BLOCK, DogalAfetler.konum("afet_dekor"));
    private static final Direction[] YONLER = Direction.values();

    private AfetBloklari() {
    }

    /** Mod kurucusundan: etiket sağlayıcısı. */
    public static void kaydet() {
        DataGenerators.saglayiciEkle(olay -> olay.getGenerator().addProvider(olay.includeServer(),
                new Saglayici(olay.getGenerator().getPackOutput(), olay.getLookupProvider(), olay.getExistingFileHelper())));
    }

    /** Bloğun afet yıkımındaki rolü: dekor etiketi önce, sonra envanterli blok varlığı. */
    public static BlokRolu rol(Level l, BlockPos p, BlockState s) {
        if (s.is(DEKOR)) {
            return BlokRolu.DEKOR;
        }
        return s.hasBlockEntity() && l.getBlockEntity(p) instanceof Container ? BlokRolu.KAP : BlokRolu.DIGER;
    }

    /** Kapı, kapak, çit, çit kapısı: dekor etiketinde ama yapının parçası (deprem bunları eşya olarak düşürmez). */
    public static boolean yapiDekoru(BlockState s) {
        return s.is(BlockTags.DOORS) || s.is(BlockTags.TRAPDOORS) || s.is(BlockTags.FENCES) || s.is(BlockTags.FENCE_GATES);
    }

    /**
     * {@code p}'deki blok komşu güncellemesi olmadan silindikten sonra altı komşudan dayanağı gidenleri ({@code canSurvive}
     * yanlış: duvar meşalesi, asılı fener, halı, kapının üst yarısı) {@code dusen}'e verir. Hava, sıvı ve yüklü olmayan
     * komşu atlanır; verilen konum {@code m}'nin paylaşılan nesnesidir, saklanacaksa {@code immutable()} alınır.
     */
    public static void dayanaksizKomsular(ServerLevel l, BlockPos p, BlockPos.MutableBlockPos m, Consumer<BlockPos> dusen) {
        for (Direction d : YONLER) {
            m.setWithOffset(p, d);
            if (!l.isLoaded(m)) {
                continue;
            }
            BlockState s = l.getBlockState(m);
            if (!s.isAir() && s.getFluidState().isEmpty() && !s.canSurvive(l, m)) {
                dusen.accept(m);
            }
        }
    }

    private static final class Saglayici extends BlockTagsProvider {
        Saglayici(PackOutput cikti, CompletableFuture<HolderLookup.Provider> arama, ExistingFileHelper dosyalar) {
            super(cikti, arama, DogalAfetler.MOD_ID, dosyalar);
        }

        @Override
        public String getName() {
            return "Afet ortak blok etiketleri";
        }

        @Override
        protected void addTags(HolderLookup.Provider arama) {
            tag(DEKOR).addTags(BlockTags.SIGNS, BlockTags.ALL_HANGING_SIGNS, BlockTags.BANNERS, BlockTags.FLOWER_POTS,
                    BlockTags.CANDLES, BlockTags.WOOL_CARPETS, BlockTags.BUTTONS, BlockTags.PRESSURE_PLATES,
                    BlockTags.WOODEN_DOORS, BlockTags.WOODEN_TRAPDOORS, BlockTags.WOODEN_FENCES, BlockTags.FENCE_GATES)
                    .add(Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.CHAIN, Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH,
                            Blocks.SOUL_WALL_TORCH, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH, Blocks.LADDER,
                            Blocks.LEVER, Blocks.TRIPWIRE_HOOK, Blocks.END_ROD, Blocks.MOSS_CARPET, Blocks.LIGHTNING_ROD);
        }
    }
}
