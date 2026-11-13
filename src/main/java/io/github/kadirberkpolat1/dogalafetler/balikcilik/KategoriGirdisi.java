package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.LootAgirliklari;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.LootAgirliklari.Kategori;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Loot girdisi {@code dogalafetler:balikcilik_kategorisi}: kategoriyi config ağırlıklarıyla ({@link LootAgirliklari}, şans
 * dahil) seçer ve o kategorinin tablosunu yuvarlar. Balıkçılık tablosu ({@code dogalafetler:gameplay/buz_deligi_balikcilik})
 * bu tek girdiden oluşur; kategori tablolarının içeriği veri paketiyle değiştirilebilir (N12), oranlar config'ten gelir
 * (N17). JSON: {@code {"type": "dogalafetler:balikcilik_kategorisi", "tablolar": {"yaygin": "...", ...}}}; tablosu
 * verilmeyen kategori seçilirse bir şey düşmez.
 */
public final class KategoriGirdisi extends LootPoolSingletonContainer {
    private final Map<Kategori, ResourceLocation> tablolar;

    private KategoriGirdisi(Map<Kategori, ResourceLocation> tablolar, int agirlik, int kalite, LootItemCondition[] kosullar,
            LootItemFunction[] islevler) {
        super(agirlik, kalite, kosullar, islevler);
        this.tablolar = new EnumMap<>(tablolar);
    }

    /** Datagen kurucusu. */
    public static LootPoolSingletonContainer.Builder<?> girdi(Map<Kategori, ResourceLocation> tablolar) {
        return simpleBuilder((agirlik, kalite, kosullar, islevler) -> new KategoriGirdisi(tablolar, agirlik, kalite, kosullar,
                islevler));
    }

    @Override
    public LootPoolEntryType getType() {
        return BalikcilikKayitlari.KATEGORI_GIRDISI.get();
    }

    @Override
    public void createItemStack(Consumer<ItemStack> cikti, LootContext baglam) {
        // Olta buz deliğinde bekleme başlarken kategoriyi çoktan seçtiyse (bekleme süresi nadirliğe göre) o kullanılır.
        boolean ozelAcik = KutupOltasi.mi(baglam.getParamOrNull(LootContextParams.TOOL));
        Kategori k = baglam.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof FishingHook olta
                ? BuzBalikciligi.secilenKategori(olta, ozelAcik) : null;
        if (k == null) {
            k = BalikcilikAyarlari.lootAgirliklari().sec(baglam.getRandom().nextDouble(), baglam.getLuck(), ozelAcik);
        }
        ResourceLocation tablo = tablolar.get(k);
        if (tablo != null) {
            baglam.getResolver().getLootTable(tablo).getRandomItemsRaw(baglam, cikti);
        }
    }

    @Override
    public void validate(ValidationContext dogrulama) {
        super.validate(dogrulama);
        for (Map.Entry<Kategori, ResourceLocation> e : tablolar.entrySet()) {
            LootDataId<LootTable> kimlik = new LootDataId<>(LootDataType.TABLE, e.getValue());
            if (dogrulama.hasVisitedElement(kimlik)) {
                dogrulama.reportProblem("Table " + e.getValue() + " is recursively called");
                continue;
            }
            dogrulama.resolver().getElementOptional(kimlik).ifPresentOrElse(
                    t -> t.validate(dogrulama.enterElement("->{" + e.getKey().ad() + "=" + e.getValue() + "}", kimlik)),
                    () -> dogrulama.reportProblem("Unknown loot table called " + e.getValue()));
        }
    }

    public static final class Serializer extends LootPoolSingletonContainer.Serializer<KategoriGirdisi> {
        @Override
        public void serializeCustom(JsonObject json, KategoriGirdisi girdi, JsonSerializationContext baglam) {
            super.serializeCustom(json, girdi, baglam);
            JsonObject t = new JsonObject();
            for (Map.Entry<Kategori, ResourceLocation> e : girdi.tablolar.entrySet()) {
                t.addProperty(e.getKey().ad(), e.getValue().toString());
            }
            json.add("tablolar", t);
        }

        @Override
        protected KategoriGirdisi deserialize(JsonObject json, JsonDeserializationContext baglam, int agirlik, int kalite,
                LootItemCondition[] kosullar, LootItemFunction[] islevler) {
            JsonObject t = GsonHelper.getAsJsonObject(json, "tablolar");
            Map<Kategori, ResourceLocation> tablolar = new EnumMap<>(Kategori.class);
            for (Kategori k : Kategori.values()) {
                if (t.has(k.ad())) {
                    String ad = GsonHelper.getAsString(t, k.ad());
                    ResourceLocation kimlik = ResourceLocation.tryParse(ad);
                    if (kimlik == null) {
                        throw new JsonSyntaxException("geçersiz loot tablosu kimliği: " + ad);
                    }
                    tablolar.put(k, kimlik);
                }
            }
            return new KategoriGirdisi(tablolar, agirlik, kalite, kosullar, islevler);
        }
    }
}
