package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Zırh + malzeme şekilsiz tarifi (N3): herhangi bir {@code ArmorItem} (vanilla ya da modlu) ile bir malzeme birleşince aynı
 * parça NBT'sine {@code ek} yazılı olarak çıkar; NBT, büyüler, dayanıklılık ve diğer ekler korunur. Parça zaten o eki
 * taşıyorsa sonuç yoktur. Aynı tip Faz 3'teki Termostat Plakası için de kullanılır (yalnız JSON: {@code ek} ve
 * {@code malzeme} değişir):
 *
 * <pre>{@code
 * {"type": "dogalafetler:zirh_eki", "ek": "yunlu", "malzeme": {"tag": "minecraft:wool"}}
 * }</pre>
 */
public final class ZirhEkiTarifi extends CustomRecipe {
    private final ZirhEkleri.Ek ek;
    private final Ingredient malzeme;

    public ZirhEkiTarifi(ResourceLocation kimlik, CraftingBookCategory kategori, ZirhEkleri.Ek ek, Ingredient malzeme) {
        super(kimlik, kategori);
        this.ek = ek;
        this.malzeme = malzeme;
    }

    public ZirhEkleri.Ek ek() {
        return ek;
    }

    public Ingredient malzeme() {
        return malzeme;
    }

    @Override
    public boolean matches(CraftingContainer tezgah, Level level) {
        return !bul(tezgah).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer tezgah, RegistryAccess kayitlar) {
        ItemStack zirh = bul(tezgah);
        if (zirh.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack sonuc = zirh.copyWithCount(1);
        ZirhEkleri.koy(sonuc, ek);
        return sonuc;
    }

    /** Tezgahta tam bir eksiz zırh ve tam bir malzeme varsa zırh, yoksa boş yığın. */
    private ItemStack bul(CraftingContainer tezgah) {
        ItemStack zirh = ItemStack.EMPTY;
        int malzemeSayisi = 0;
        for (int i = 0; i < tezgah.getContainerSize(); i++) {
            ItemStack yigin = tezgah.getItem(i);
            if (yigin.isEmpty()) {
                continue;
            }
            if (ZirhEkleri.zirhMi(yigin)) {
                if (!zirh.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                zirh = yigin;
            } else if (malzeme.test(yigin)) {
                malzemeSayisi++;
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (zirh.isEmpty() || malzemeSayisi != 1 || ZirhEkleri.varMi(zirh, ek)) {
            return ItemStack.EMPTY;
        }
        return zirh;
    }

    @Override
    public boolean canCraftInDimensions(int genislik, int yukseklik) {
        return genislik * yukseklik >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SicaklikKayitlari.ZIRH_EKI_TARIFI.get();
    }

    /** JSON ve ağ biçimi. */
    public static final class Serilestirici implements RecipeSerializer<ZirhEkiTarifi> {
        @Override
        public ZirhEkiTarifi fromJson(ResourceLocation kimlik, JsonObject json) {
            CraftingBookCategory kategori = CraftingBookCategory.CODEC.byName(
                    GsonHelper.getAsString(json, "category", null), CraftingBookCategory.EQUIPMENT);
            ZirhEkleri.Ek ek = ZirhEkleri.ek(GsonHelper.getAsString(json, "ek"));
            Ingredient malzeme = Ingredient.fromJson(GsonHelper.getNonNull(json, "malzeme"), false);
            return new ZirhEkiTarifi(kimlik, kategori, ek, malzeme);
        }

        @Override
        public ZirhEkiTarifi fromNetwork(ResourceLocation kimlik, FriendlyByteBuf tampon) {
            CraftingBookCategory kategori = tampon.readEnum(CraftingBookCategory.class);
            ZirhEkleri.Ek ek = ZirhEkleri.ek(tampon.readUtf());
            return new ZirhEkiTarifi(kimlik, kategori, ek, Ingredient.fromNetwork(tampon));
        }

        @Override
        public void toNetwork(FriendlyByteBuf tampon, ZirhEkiTarifi tarif) {
            tampon.writeEnum(tarif.category());
            tampon.writeUtf(tarif.ek.anahtar());
            tarif.malzeme.toNetwork(tampon);
        }
    }
}
