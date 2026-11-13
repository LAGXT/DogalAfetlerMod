package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.DelikOmru;
import io.github.kadirberkpolat1.dogalafetler.donma.ChunkDonmaVerisi;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaBloklari;
import io.github.kadirberkpolat1.dogalafetler.journal.Defter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Buz deliği açma (madde 15): oyuncu kazma ya da baltayla (modlu aletler {@code ToolActions} ile) eğilerek su üstündeki
 * buza sağ tıklayınca, balıkçılık o noktada aktifse ve chunk'taki açık delik sayısı sınırın altındaysa buz
 * {@code buz_deligi} olur, alet {@code balikcilik.aletHasari} dayanıklılık kaybeder. Yalnız sunucuda işlenir: istemci
 * paketi her durumda yollar, kol sallama sunucunun başarılı sonucundan gelir.
 */
public final class BuzDeligiAcma {
    static final String SINIR_MESAJI = "message.dogalafetler.buz_deligi.sinir";

    private BuzDeligiAcma() {
    }

    static void sagTik(PlayerInteractEvent.RightClickBlock olay) {
        if (olay.getLevel().isClientSide || !(olay.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Player oyuncu = olay.getEntity();
        ItemStack alet = olay.getItemStack();
        if (!oyuncu.isSecondaryUseActive() || !aletMi(alet)) {
            return;
        }
        BlockPos konum = olay.getPos();
        BlockState buz = level.getBlockState(konum);
        if (!buz.is(Blocks.ICE) && !buz.is(DonmaBloklari.INCE_BUZ.get()) || !level.isEmptyBlock(konum.above())
                || !BuzBalikciligi.aktifMi(level, konum)) {
            return;
        }
        olay.setCanceled(true);
        if (!DelikOmru.acilabilirMi(acikDelikSayisi(level, new ChunkPos(konum)), BalikcilikAyarlari.chunkBasinaDelik())) {
            oyuncu.displayClientMessage(Component.translatable(SINIR_MESAJI), true);
            olay.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        level.setBlock(konum, BalikcilikKayitlari.BUZ_DELIGI.get().defaultBlockState()
                .setValue(BuzDeligiBlogu.INCE, buz.is(DonmaBloklari.INCE_BUZ.get()))
                .setValue(BuzDeligiBlogu.DONMA, donmaBuzuMu(level, konum)), Block.UPDATE_ALL);
        Defter.buzDeligiAcildi(oyuncu);
        alet.hurtAndBreak(BalikcilikAyarlari.aletHasari(), oyuncu, o -> o.broadcastBreakEvent(olay.getHand()));
        oyuncu.awardStat(Stats.ITEM_USED.get(alet.getItem()));
        double x = konum.getX() + 0.5;
        double y = konum.getY() + 1.0;
        double z = konum.getZ() + 0.5;
        level.playSound(null, konum, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.8f, 1.25f);
        level.playSound(null, konum, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.35f, 1.4f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, buz), x, y, z, 28, 0.3, 0.08, 0.3, 0.08);
        level.sendParticles(ParticleTypes.SPLASH, x, y - 0.05, z, 14, 0.25, 0.05, 0.25, 0.1);
        level.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 0.1, z, 8, 0.3, 0.1, 0.3, 0.02);
        olay.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Sütunun üst buzunu donma sistemi mi koydu (chunk verisindeki buz biti, donma §A.6). */
    public static boolean donmaBuzuMu(ServerLevel level, BlockPos konum) {
        ChunkDonmaVerisi v = ChunkDonmaVerisi.al(level.getChunkAt(konum));
        return v != null && v.buz(ChunkDonmaVerisi.indeks(konum.getX(), konum.getZ()));
    }

    /** Kazma ya da balta (vanilla ve {@code ToolActions} bildiren modlu aletler). */
    public static boolean aletMi(ItemStack alet) {
        return !alet.isEmpty() && (alet.canPerformAction(ToolActions.PICKAXE_DIG) || alet.canPerformAction(ToolActions.AXE_DIG));
    }

    /**
     * Chunk'taki açık delik sayısı: yalnız paletinde {@code buz_deligi} olabilecek bölümler taranır (delik açma nadir bir
     * oyuncu eylemidir; ayrı sayacın kayıt ve senkron yükü yoktur). Chunk yüklü değilse 0.
     */
    public static int acikDelikSayisi(ServerLevel level, ChunkPos chunk) {
        LevelChunk c = level.getChunkSource().getChunkNow(chunk.x, chunk.z);
        if (c == null) {
            return 0;
        }
        Block delik = BalikcilikKayitlari.BUZ_DELIGI.get();
        int n = 0;
        for (LevelChunkSection b : c.getSections()) {
            if (b.hasOnlyAir() || !b.maybeHas(d -> d.is(delik))) {
                continue;
            }
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (b.getBlockState(x, y, z).is(delik)) {
                            n++;
                        }
                    }
                }
            }
        }
        return n;
    }
}
