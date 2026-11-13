package me.jellysquid.mods.sodium.client.world;

import net.minecraft.world.level.BlockAndTintGetter;

/**
 * Derleme zamanı taşlak (jar'a girmez): Embeddium 0.3.31'in {@code WorldSlice} sınıfı; yalnız {@link BlockAndTintGetter}'ı uyguladığı bilgisi
 * (İmza {@code javap} ile Embeddium 0.3.31+mc1.20.1 jar'ından alındı). Gerçek sınıf çalışma anında Embeddium'dan gelir.
 */
public abstract class WorldSlice implements BlockAndTintGetter {
}
