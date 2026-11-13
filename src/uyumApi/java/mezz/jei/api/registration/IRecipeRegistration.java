package mezz.jei.api.registration;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** JEI API taslağı (jar'a girmez): bilgi sayfası ekleme, JEI 15.x'teki {@code addItemStackInfo} imzasıyla. */
public interface IRecipeRegistration {
    void addItemStackInfo(ItemStack ingredient, Component... descriptionComponents);
}
