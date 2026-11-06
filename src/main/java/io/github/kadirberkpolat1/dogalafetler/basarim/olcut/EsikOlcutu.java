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
 * Sayısal eşikli ölçüt: tetikleyen o anki değeri verir, JSON'daki eşik ({@code alan}) sağlanınca tutar. Eşik veri
 * paketindedir (madde 12'deki 5 tüy, 4 yünlü, 25 balık, 3 oyuncu, %50 donma, 3 gün, N16'daki 10 saniye).
 * {@code kesin} ise değer eşiği geçmeli ("%50'yi geçsin"), değilse en az eşik kadar olmalı.
 */
public final class EsikOlcutu extends SimpleCriterionTrigger<EsikOlcutu.Ornek> {
    private final ResourceLocation kimlik;
    private final String alan;
    private final boolean kesin;

    public EsikOlcutu(String ad, String alan, boolean kesin) {
        this.kimlik = DogalAfetler.konum(ad);
        this.alan = alan;
        this.kesin = kesin;
    }

    @Override
    public ResourceLocation getId() {
        return kimlik;
    }

    @Override
    protected Ornek createInstance(JsonObject json, ContextAwarePredicate oyuncu, DeserializationContext baglam) {
        return new Ornek(kimlik, oyuncu, alan, kesin, GsonHelper.getAsDouble(json, alan));
    }

    public void tetikle(ServerPlayer oyuncu, double deger) {
        trigger(oyuncu, ornek -> ornek.saglar(deger));
    }

    /** Datagen için örnek. */
    public Ornek ornek(double esik) {
        return new Ornek(kimlik, ContextAwarePredicate.ANY, alan, kesin, esik);
    }

    public static final class Ornek extends AbstractCriterionTriggerInstance {
        private final String alan;
        private final boolean kesin;
        private final double esik;

        Ornek(ResourceLocation kimlik, ContextAwarePredicate oyuncu, String alan, boolean kesin, double esik) {
            super(kimlik, oyuncu);
            this.alan = alan;
            this.kesin = kesin;
            this.esik = esik;
        }

        public boolean saglar(double deger) {
            return kesin ? deger > esik : deger >= esik;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext baglam) {
            JsonObject json = super.serializeToJson(baglam);
            if (esik == Math.rint(esik) && Math.abs(esik) < 1e15) {
                json.addProperty(alan, (long) esik);
            } else {
                json.addProperty(alan, esik);
            }
            return json;
        }
    }
}
