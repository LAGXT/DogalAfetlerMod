package io.github.kadirberkpolat1.dogalafetler.basarim.olcut;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
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
 * Eşya kimliğiyle ölçüt (toplandı, üretildi). Kimlik {@link ResourceLocation} olarak saklanır ve kayıt defterinde
 * aranmaz: vanilla {@code inventory_changed} gibi eşya yüklemi kayıtsız eşyada advancement'ı yüklenemez yapardı;
 * burada eşya henüz yoksa ölçüt yalnız tutmaz (kardeş işçilerin eşyaları Faz 3'te paralel geliyor).
 */
public final class EsyaOlcutu extends SimpleCriterionTrigger<EsyaOlcutu.Ornek> {
    private final ResourceLocation kimlik;

    public EsyaOlcutu(String ad) {
        this.kimlik = DogalAfetler.konum(ad);
    }

    @Override
    public ResourceLocation getId() {
        return kimlik;
    }

    @Override
    protected Ornek createInstance(JsonObject json, ContextAwarePredicate oyuncu, DeserializationContext baglam) {
        String metin = GsonHelper.getAsString(json, "esya");
        ResourceLocation esya = ResourceLocation.tryParse(metin);
        if (esya == null) {
            throw new JsonSyntaxException("Geçersiz eşya kimliği: " + metin);
        }
        return new Ornek(kimlik, oyuncu, esya);
    }

    public void tetikle(ServerPlayer oyuncu, ResourceLocation esya) {
        trigger(oyuncu, ornek -> ornek.esya.equals(esya));
    }

    /** Datagen için örnek. */
    public Ornek ornek(ResourceLocation esya) {
        return new Ornek(kimlik, ContextAwarePredicate.ANY, esya);
    }

    public static final class Ornek extends AbstractCriterionTriggerInstance {
        private final ResourceLocation esya;

        Ornek(ResourceLocation kimlik, ContextAwarePredicate oyuncu, ResourceLocation esya) {
            super(kimlik, oyuncu);
            this.esya = esya;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext baglam) {
            JsonObject json = super.serializeToJson(baglam);
            json.addProperty("esya", esya.toString());
            return json;
        }
    }
}
