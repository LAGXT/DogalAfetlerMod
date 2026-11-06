package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Vanilla şekilsiz tarif + {@code kalan_birakmayan}: listedeki malzemelerin vanilla kalanı (ör. bal şişesinin boş şişesi)
 * geri verilmez. Isıtıcı Çay'da bal şişesi ikinci çayın şişesidir (1 cam şişe + bal şişesi → 2 çay); vanilla kalanı bir şişeyi
 * ikiye çoğaltırdı. Diğer malzemelerin kalanı (süt kovası → kova) vanilla gibidir. JSON: vanilla {@code crafting_shapeless}
 * alanları + {@code "kalan_birakmayan": ["minecraft:honey_bottle"]}.
 */
public final class KalanSecimliTarif extends ShapelessRecipe {
    public static final String ALAN = "kalan_birakmayan";

    private final List<Item> kalanBirakmayan;

    KalanSecimliTarif(ResourceLocation kimlik, String grup, CraftingBookCategory kategori, ItemStack sonuc,
            NonNullList<Ingredient> malzemeler, List<Item> kalanBirakmayan) {
        super(kimlik, grup, kategori, sonuc, malzemeler);
        this.kalanBirakmayan = List.copyOf(kalanBirakmayan);
    }

    public List<Item> kalanBirakmayan() {
        return kalanBirakmayan;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer tezgah) {
        NonNullList<ItemStack> kalan = super.getRemainingItems(tezgah);
        for (int i = 0; i < kalan.size(); i++) {
            if (kalanBirakmayan.contains(tezgah.getItem(i).getItem())) {
                kalan.set(i, ItemStack.EMPTY);
            }
        }
        return kalan;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return YiyecekKayitlari.KALAN_SECIMLI_TARIF.get();
    }

    private static KalanSecimliTarif kur(ShapelessRecipe temel, List<Item> kalanBirakmayan) {
        return new KalanSecimliTarif(temel.getId(), temel.getGroup(), temel.category(),
                temel.getResultItem(RegistryAccess.EMPTY), temel.getIngredients(), kalanBirakmayan);
    }

    /** Vanilla şekilsiz seriçiye ekli alan; ağda vanilla gövdesinin ardından eşya kimlikleri gider. */
    public static final class Serilestirici implements RecipeSerializer<KalanSecimliTarif> {
        @Override
        public KalanSecimliTarif fromJson(ResourceLocation kimlik, JsonObject json) {
            ShapelessRecipe temel = RecipeSerializer.SHAPELESS_RECIPE.fromJson(kimlik, json);
            List<Item> liste = new ArrayList<>();
            JsonArray dizi = GsonHelper.getAsJsonArray(json, ALAN);
            for (JsonElement e : dizi) {
                ResourceLocation esya = ResourceLocation.tryParse(GsonHelper.convertToString(e, ALAN));
                if (esya == null || !ForgeRegistries.ITEMS.containsKey(esya)) {
                    throw new JsonSyntaxException("Bilinmeyen eşya '" + e + "' (" + ALAN + ")");
                }
                liste.add(ForgeRegistries.ITEMS.getValue(esya));
            }
            return kur(temel, liste);
        }

        @Override
        public KalanSecimliTarif fromNetwork(ResourceLocation kimlik, FriendlyByteBuf tampon) {
            ShapelessRecipe temel = RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(kimlik, tampon);
            int n = tampon.readVarInt();
            List<Item> liste = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                liste.add(tampon.readRegistryIdUnsafe(ForgeRegistries.ITEMS));
            }
            return kur(temel, liste);
        }

        @Override
        public void toNetwork(FriendlyByteBuf tampon, KalanSecimliTarif tarif) {
            RecipeSerializer.SHAPELESS_RECIPE.toNetwork(tampon, tarif);
            tampon.writeVarInt(tarif.kalanBirakmayan.size());
            for (Item esya : tarif.kalanBirakmayan) {
                tampon.writeRegistryIdUnsafe(ForgeRegistries.ITEMS, esya);
            }
        }
    }
}
