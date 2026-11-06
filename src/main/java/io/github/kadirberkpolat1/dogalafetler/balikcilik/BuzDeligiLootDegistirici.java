package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.LootAgirliklari.Kategori;
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
 * Global loot değiştiricisi {@code dogalafetler:buz_deligi_balikcilik} (madde 2, madde 15): vanilla balıkçılık tablosu
 * yuvarlandığında ({@code LootTableIdCondition minecraft:gameplay/fishing}) olta {@code buz_deligi} bloğundaysa sonucu
 * {@link #tablo} tablosunun sonucuyla değiştirir; başka her balıkçılıkta vanilla sonucu dokunulmadan geçer. Vanilla
 * eşya varlığını, XP'yi, istatistiği ve {@code fishing_rod_hooked} ölçütünü bizim eşyalarımızla kendisi üretir.
 */
public final class BuzDeligiLootDegistirici extends LootModifier {
    public static final Supplier<Codec<BuzDeligiLootDegistirici>> CODEC = Suppliers.memoize(
            () -> RecordCodecBuilder.create(i -> codecStart(i).and(ResourceLocation.CODEC.fieldOf("tablo")
                    .forGetter(m -> m.tablo)).apply(i, BuzDeligiLootDegistirici::new)));

    /** Balıkçılık tablosu: tek {@code balikcilik_kategorisi} girdisi ({@link KategoriGirdisi}). */
    public static final ResourceLocation TABLO = DogalAfetler.konum("gameplay/buz_deligi_balikcilik");

    private final ResourceLocation tablo;

    public BuzDeligiLootDegistirici(LootItemCondition[] kosullar, ResourceLocation tablo) {
        super(kosullar);
        this.tablo = tablo;
    }

    /** Kategorinin içerik tablosu ({@code dogalafetler:gameplay/buz_deligi/<kategori>}). */
    public static ResourceLocation kategoriTablosu(Kategori k) {
        return DogalAfetler.konum("gameplay/buz_deligi/" + k.ad());
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> uretilen, LootContext baglam) {
        Entity varlik = baglam.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(varlik instanceof FishingHook olta) || !BuzBalikciligi.delikteMi(olta)) {
            return uretilen;
        }
        ObjectArrayList<ItemStack> yeni = new ObjectArrayList<>();
        baglam.getResolver().getLootTable(tablo).getRandomItemsRaw(baglam,
                LootTable.createStackSplitter(baglam.getLevel(), yeni::add));
        return yeni;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
