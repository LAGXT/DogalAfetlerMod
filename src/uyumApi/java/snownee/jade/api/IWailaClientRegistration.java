package snownee.jade.api;

import net.minecraft.world.level.block.Block;

/** Jade API taslağı (jar'a girmez): istemci kayıt aracı, Jade 11.x imzasıyla. */
public interface IWailaClientRegistration {
    void registerBlockComponent(IBlockComponentProvider provider, Class<? extends Block> blockClass);
}
