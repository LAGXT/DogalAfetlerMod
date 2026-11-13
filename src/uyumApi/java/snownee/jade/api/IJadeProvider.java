package snownee.jade.api;

import net.minecraft.resources.ResourceLocation;

/** Jade API taslağı (jar'a girmez): her sağlayıcının kimliği (Jade yapılandırma anahtarı buradan türer). */
public interface IJadeProvider {
    ResourceLocation getUid();
}
