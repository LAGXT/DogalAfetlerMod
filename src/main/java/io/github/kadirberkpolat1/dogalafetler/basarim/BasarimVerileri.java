package io.github.kadirberkpolat1.dogalafetler.basarim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.BasarimTuru;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.OdulTablosu;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Başarım veri paketi dosyaları (datagen, GOREV §3): sekmenin 27 advancement JSON'u
 * ({@code data/dogalafetler/advancements/dogalafetler/*.json}), "hedef" ödül tablosu ({@code rewards/hedef}) ve #19'un
 * sıcak yiyecek etiketi ({@code basarim/sicak_yiyecek}, isteğe bağlı girdiler). JSON vanilla kurucularıyla
 * ({@code Advancement.Builder}, {@code LootTable}) üretilir. Tek {@code DataProvider}: vanilla
 * {@code AdvancementProvider}'ın adı sabit ("Advancements") olduğu için başka bir alt sistemin sağlayıcısıyla
 * {@code Duplicate provider} çakışması olmasın (K-E5). Vanilla sohbet duyurusu kapalı ({@code announce_to_chat=false}):
 * duyuruyu mod ünvanla kendisi yapar (A-37). Mod eşyası ikonları ve ödül yiyeceği datagen anında kayıtlı
 * değilse vanilla yedeği yazılır; kardeş dallar birleşince {@code runData} gerçek eşyayı yazar.
 */
public final class BasarimVerileri implements DataProvider {
    private final PackOutput cikti;

    private BasarimVerileri(PackOutput cikti) {
        this.cikti = cikti;
    }

    static void ekle(GatherDataEvent olay) {
        PackOutput cikti = olay.getGenerator().getPackOutput();
        olay.getGenerator().addProvider(olay.includeServer(), new BasarimVerileri(cikti));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput onbellek) {
        Path veri = cikti.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(DogalAfetler.MOD_ID);
        List<CompletableFuture<?>> isler = new ArrayList<>();
        for (Basarimlar basarim : Basarimlar.values()) {
            isler.add(DataProvider.saveStable(onbellek, basarim(basarim),
                    veri.resolve("advancements/dogalafetler/" + basarim.yol() + ".json")));
        }
        isler.add(DataProvider.saveStable(onbellek, hedefOdulu(), veri.resolve("loot_tables/rewards/hedef.json")));
        isler.add(DataProvider.saveStable(onbellek, sicakYiyecekEtiketi(),
                veri.resolve("tags/items/basarim/sicak_yiyecek.json")));
        return CompletableFuture.allOf(isler.toArray(CompletableFuture[]::new));
    }

    /** Bir başarımın advancement JSON'u. */
    static JsonObject basarim(Basarimlar basarim) {
        Advancement.Builder yapici = Advancement.Builder.advancement();
        if (basarim.ebeveyn() != null) {
            yapici.parent(BasarimKimlikleri.basarim(basarim.ebeveyn()));
        }
        yapici.display(ikon(basarim), Component.translatable(basarim.adAnahtari()),
                Component.translatable(basarim.aciklamaAnahtari()),
                basarim == Basarimlar.KOK ? BasarimKimlikleri.ARKA_PLAN : null,
                FrameType.byName(basarim.tur().cerceve()), true, false, basarim.gizli());
        Map<String, CriterionTriggerInstance> olcutler = BasarimOlcutleri.olcutler(basarim);
        olcutler.forEach(yapici::addCriterion);
        if (olcutler.size() > 1) {
            yapici.requirements(RequirementsStrategy.OR);
        }
        AdvancementRewards.Builder odul = AdvancementRewards.Builder.experience(basarim.tur().varsayilanXp());
        if (basarim.tur() == BasarimTuru.HEDEF) {
            odul.addLootTable(BasarimKimlikleri.HEDEF_ODULU);
        }
        yapici.rewards(odul);
        return yapici.serializeToJson();
    }

    private static ItemStack ikon(Basarimlar basarim) {
        ResourceLocation kimlik = ResourceLocation.tryParse(basarim.ikon());
        Item esya = basarim.ikonYedegi() == null ? ForgeRegistries.ITEMS.getValue(kimlik)
                : BasarimKimlikleri.esyaYaDaYedek(kimlik,
                        ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(basarim.ikonYedegi())));
        if (esya == null || esya == Items.AIR) {
            throw new IllegalStateException("Başarım ikonu bulunamadı: " + basarim.yol() + " " + basarim.ikon());
        }
        return new ItemStack(esya);
    }

    /** "Hedef" ödülü: 1–2 sıcak yiyecek (Sıcak Salep; kayıtlı değilse fırınlanmış patates). */
    static JsonElement hedefOdulu() {
        Item yiyecek = BasarimKimlikleri.esyaYaDaYedek(BasarimKimlikleri.SICAK_SALEP, Items.BAKED_POTATO);
        LootTable tablo = LootTable.lootTable()
                .setParamSet(LootContextParamSets.ADVANCEMENT_REWARD)
                .setRandomSequence(BasarimKimlikleri.HEDEF_ODULU)
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(yiyecek).apply(SetItemCountFunction.setCount(
                                UniformGenerator.between(OdulTablosu.HEDEF_YIYECEK_EN_AZ,
                                        OdulTablosu.HEDEF_YIYECEK_EN_COK)))))
                .build();
        return LootDataType.TABLE.parser().toJsonTree(tablo);
    }

    private static JsonObject sicakYiyecekEtiketi() {
        JsonArray degerler = new JsonArray();
        for (ResourceLocation kimlik : BasarimKimlikleri.SICAK_YIYECEKLER) {
            JsonObject girdi = new JsonObject();
            girdi.addProperty("id", kimlik.toString());
            girdi.addProperty("required", false);
            degerler.add(girdi);
        }
        JsonObject etiket = new JsonObject();
        etiket.addProperty("replace", false);
        etiket.add("values", degerler);
        return etiket;
    }

    @Override
    public String getName() {
        return "Doğal Afetler başarımları";
    }
}
