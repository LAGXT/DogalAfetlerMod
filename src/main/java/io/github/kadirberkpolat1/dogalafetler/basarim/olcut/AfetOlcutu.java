package io.github.kadirberkpolat1.dogalafetler.basarim.olcut;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.SagCikanTuretimi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.SagCikmaKosulu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Set;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

/**
 * {@code afetten_sag_cikti}: oyuncu doğal biten bir afetten sağ çıktı (madde 12, çekirdek §A5). JSON alanları isteğe
 * bağlıdır: {@code tur} (N18 afet adı), {@code min_seviye} (1–5; seviyesiz afet 0 sayılır), {@code kosul}
 * ({@link SagCikmaKosulu}: o afette ayrıca sağlanan koşul).
 */
public final class AfetOlcutu extends SimpleCriterionTrigger<AfetOlcutu.Ornek> {
    private final ResourceLocation kimlik;

    public AfetOlcutu(String ad) {
        this.kimlik = DogalAfetler.konum(ad);
    }

    @Override
    public ResourceLocation getId() {
        return kimlik;
    }

    @Override
    protected Ornek createInstance(JsonObject json, ContextAwarePredicate oyuncu, DeserializationContext baglam) {
        AfetTuru tur = null;
        if (json.has("tur")) {
            String ad = GsonHelper.getAsString(json, "tur");
            tur = AfetTuru.kimlikIle(ad).orElseThrow(() -> new JsonSyntaxException("Bilinmeyen afet türü: " + ad));
        }
        SagCikmaKosulu kosul = null;
        if (json.has("kosul")) {
            String ad = GsonHelper.getAsString(json, "kosul");
            kosul = SagCikmaKosulu.kimlikIle(ad).orElseThrow(() -> new JsonSyntaxException("Bilinmeyen koşul: " + ad));
        }
        return new Ornek(kimlik, oyuncu, tur, GsonHelper.getAsInt(json, "min_seviye", 0), kosul);
    }

    public void tetikle(ServerPlayer oyuncu, AfetTuru tur, int seviye, Set<SagCikmaKosulu> kosullar) {
        trigger(oyuncu, ornek -> ornek.saglar(tur, seviye, kosullar));
    }

    /** Datagen için örnek; boş alanlar null / 0. */
    public Ornek ornek(AfetTuru tur, int minSeviye, SagCikmaKosulu kosul) {
        return new Ornek(kimlik, ContextAwarePredicate.ANY, tur, minSeviye, kosul);
    }

    public static final class Ornek extends AbstractCriterionTriggerInstance {
        private final AfetTuru tur;
        private final int minSeviye;
        private final SagCikmaKosulu kosul;

        Ornek(ResourceLocation kimlik, ContextAwarePredicate oyuncu, AfetTuru tur, int minSeviye, SagCikmaKosulu kosul) {
            super(kimlik, oyuncu);
            this.tur = tur;
            this.minSeviye = minSeviye;
            this.kosul = kosul;
        }

        public boolean saglar(AfetTuru olayTuru, int seviye, Set<SagCikmaKosulu> kosullar) {
            return (tur == null || tur == olayTuru) && SagCikanTuretimi.seviyeYeterli(seviye, minSeviye)
                    && (kosul == null || kosullar.contains(kosul));
        }

        @Override
        public JsonObject serializeToJson(SerializationContext baglam) {
            JsonObject json = super.serializeToJson(baglam);
            if (tur != null) {
                json.addProperty("tur", tur.kimlik());
            }
            if (minSeviye > 0) {
                json.addProperty("min_seviye", minSeviye);
            }
            if (kosul != null) {
                json.addProperty("kosul", kosul.kimlik());
            }
            return json;
        }
    }
}
