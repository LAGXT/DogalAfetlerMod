package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;

/**
 * Derleme zamanı taşlak (jar'a girmez): Embeddium 0.3.31'in {@code FluidRenderer} sınıfının, mixin'imizin
 * ({@code client.uyum.mixin.EmbeddiumSivilariMixin}) hedeflediği genel {@code render} yöntemi. İmza Embeddium 0.3.31+mc1.20.1
 * jar'ından {@code javap} ile alındı: {@code render(WorldSlice, FluidState, BlockPos, BlockPos, ChunkBuildBuffers)V}. Mixin
 * anotasyon işlemcisi hedef sınıfı derleme sırasında görmek ister; gerçek sınıf çalışma anında Embeddium'dan gelir.
 */
public class FluidRenderer {
    public void render(WorldSlice dunya, FluidState sivi, BlockPos pos, BlockPos konum, ChunkBuildBuffers tamponlar) {
    }
}
