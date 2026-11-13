package snownee.jade.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Jade API taslağı (jar'a girmez): bakılan blok; {@code getLevel/getPlayer} gerçekte {@code Accessor} üst aracındadır. */
public interface BlockAccessor {
    BlockState getBlockState();

    BlockPos getPosition();

    Level getLevel();

    Player getPlayer();
}
