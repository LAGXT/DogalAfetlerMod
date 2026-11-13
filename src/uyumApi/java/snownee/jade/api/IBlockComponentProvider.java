package snownee.jade.api;

import snownee.jade.api.config.IPluginConfig;

/** Jade API taslağı (jar'a girmez): bakılan bloğun ipucu satırlarını ekleyen sağlayıcı. */
public interface IBlockComponentProvider extends IToggleableProvider {
    void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config);
}
