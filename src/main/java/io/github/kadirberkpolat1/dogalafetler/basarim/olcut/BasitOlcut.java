package io.github.kadirberkpolat1.dogalafetler.basarim.olcut;

import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Alansız ölçüt: olay olduğunda tutar (ilk siren, süpernova, deniz yükselmesi, çorba, hepsini atlattı). */
public final class BasitOlcut extends SimpleCriterionTrigger<BasitOlcut.Ornek> {
    private final ResourceLocation kimlik;

    public BasitOlcut(String ad) {
        this.kimlik = DogalAfetler.konum(ad);
    }

    @Override
    public ResourceLocation getId() {
        return kimlik;
    }

    @Override
    protected Ornek createInstance(JsonObject json, ContextAwarePredicate oyuncu, DeserializationContext baglam) {
        return new Ornek(kimlik, oyuncu);
    }

    public void tetikle(ServerPlayer oyuncu) {
        trigger(oyuncu, ornek -> true);
    }

    /** Datagen için örnek. */
    public Ornek ornek() {
        return new Ornek(kimlik, ContextAwarePredicate.ANY);
    }

    public static final class Ornek extends AbstractCriterionTriggerInstance {
        Ornek(ResourceLocation kimlik, ContextAwarePredicate oyuncu) {
            super(kimlik, oyuncu);
        }
    }
}
