package snownee.jade.api;

import net.minecraft.network.chat.Component;

/** Jade API taslağı (jar'a girmez): ipucu kutusu; gerçekte {@code add(Component)} varsayılan yöntemdir. */
public interface ITooltip {
    void add(Component component);
}
