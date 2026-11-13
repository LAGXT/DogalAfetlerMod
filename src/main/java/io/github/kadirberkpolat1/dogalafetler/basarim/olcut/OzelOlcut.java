package io.github.kadirberkpolat1.dogalafetler.basarim.olcut;

import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

/**
 * {@code ozel_basarim}: başka alt sistemlerin kendi olaylarından verdiği başarımların ortak ölçütü. Tetikleyen bir
 * anahtar verir ({@code OzelBasarim.ver}); JSON'daki {@code anahtar} alanı aynıysa tutar. Anahtar, başarımın advancement
 * yoludur ({@code Basarimlar#yol}); böylece her alt sistem kendi tetikleyicisini yazmak yerine tek giriş noktasını çağırır.
 */
public final class OzelOlcut extends SimpleCriterionTrigger<OzelOlcut.Ornek> {
    private static final String ALAN = "anahtar";
    private final ResourceLocation kimlik;

    public OzelOlcut(String ad) {
        this.kimlik = DogalAfetler.konum(ad);
    }

    @Override
    public ResourceLocation getId() {
        return kimlik;
    }

    @Override
    protected Ornek createInstance(JsonObject json, ContextAwarePredicate oyuncu, DeserializationContext baglam) {
        return new Ornek(kimlik, oyuncu, GsonHelper.getAsString(json, ALAN));
    }

    public void tetikle(ServerPlayer oyuncu, String anahtar) {
        trigger(oyuncu, ornek -> ornek.anahtar.equals(anahtar));
    }

    /** Datagen için örnek. */
    public Ornek ornek(String anahtar) {
        return new Ornek(kimlik, ContextAwarePredicate.ANY, anahtar);
    }

    public static final class Ornek extends AbstractCriterionTriggerInstance {
        private final String anahtar;

        Ornek(ResourceLocation kimlik, ContextAwarePredicate oyuncu, String anahtar) {
            super(kimlik, oyuncu);
            this.anahtar = anahtar;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext baglam) {
            JsonObject json = super.serializeToJson(baglam);
            json.addProperty(ALAN, anahtar);
            return json;
        }
    }
}
