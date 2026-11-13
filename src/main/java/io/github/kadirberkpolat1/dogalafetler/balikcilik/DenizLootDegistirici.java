package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Global loot değtiricisi {@code dogalafetler:deniz_balikciligi} (madde 3): açık denizde (vanilla
 * {@code gameplay/fishing}, olta buz deliğinde değilken) {@link #olasilik} ihtimalle vanilla sonucu {@link #tablo}
 * tablosunun sonucuyla değiştirir; şans gelmezse vanilla sonuç olduğu gibi kalır. Tablolar ve koşullar veri paketindedir:
 * her olta ile eşya (Soğutma Parçası, Tuz Kristali, Buzlu İnci) ve yalnız Kutup Oltası ({@code MatchTool}) ile özel kutup
 * balıkları. Buz deliğinde bütün sonucu {@link BuzDeligiLootDegistirici} verir; burada dokunulmaz.
 */
public final class DenizLootDegistirici extends LootModifier {
    public static final Supplier<Codec<DenizLootDegistirici>> CODEC = Suppliers.memoize(
            () -> RecordCodecBuilder.create(i -> codecStart(i)
                    .and(ResourceLocation.CODEC.fieldOf("tablo").forGetter(m -> m.tablo))
                    .and(Codec.floatRange(0.0f, 1.0f).fieldOf("olasilik").forGetter(m -> m.olasilik))
                    .apply(i, DenizLootDegistirici::new)));

    /** Açık deniz eşyası (her olta ile). */
    public static final ResourceLocation ESYA_TABLOSU = DogalAfetler.konum("gameplay/deniz/esya");
    /** Açık deniz özel balığı (yalnız Kutup Oltası). */
    public static final ResourceLocation OZEL_TABLOSU = DogalAfetler.konum("gameplay/deniz/ozel");
    /** Açık deniz Yineleme büyülü kitabı (her olta ile, çok nadir). */
    public static final ResourceLocation YINELEME_TABLOSU = DogalAfetler.konum("gameplay/deniz/yineleme");

    private final ResourceLocation tablo;
    private final float olasilik;

    public DenizLootDegistirici(LootItemCondition[] kosullar, ResourceLocation tablo, float olasilik) {
        super(kosullar);
        this.tablo = tablo;
        this.olasilik = olasilik;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> uretilen, LootContext baglam) {
        Entity varlik = baglam.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (varlik instanceof FishingHook olta && BuzBalikciligi.delikteMi(olta)) {
            return uretilen;
        }
        if (baglam.getRandom().nextFloat() >= olasilik) {
            return uretilen;
        }
        ObjectArrayList<ItemStack> yeni = new ObjectArrayList<>();
        baglam.getResolver().getLootTable(tablo).getRandomItemsRaw(baglam,
                LootTable.createStackSplitter(baglam.getLevel(), yeni::add));
        return yeni.isEmpty() ? uretilen : yeni;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
