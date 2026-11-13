package mezz.jei.api;

import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

/** JEI API taslağı: yalnız Doğal Afetler'in kullandığı üyeler, gerçek JEI 15.x imzalarıyla birebir (jar'a girmez). */
public interface IModPlugin {
    ResourceLocation getPluginUid();

    default void registerRecipes(IRecipeRegistration registration) {
    }
}
