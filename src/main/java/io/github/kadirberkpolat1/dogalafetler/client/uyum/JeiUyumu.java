package io.github.kadirberkpolat1.dogalafetler.client.uyum;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.uyum.EsyaIpuclari;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Just Enough Items uyumu (K-E74): tarifle üretilemeyen eşyaların (meteorit parçası, kozmik çekirdek, buz balıkları, Buzlu İnci,
 * Tuz Kristali, Soğutma Parçası, kül, kömürleşmiş kütük...) JEI bilgi sayfasına "nasıl ve nerede elde edilir" metni eklenir.
 * Metinler {@link EsyaIpuclari} tablosundadır (TR/EN, datagen). Tabloda olan eşyalar tarifleri olsa da (örn. kül katmanı ↔ kül bloğu
 * birbirinden üretilir ama ilk hamı dünyada bulunur) bilgi alır; tabloda olmayan ve hiçbir tarifin çıktısı olmayan bir mod eşyası genel
 * metni alır ve günlüğe yazılır. Bu sınıf yalnız JEI yüklüyken Forge'un tarama verisiyle bulunur; JEI yoksa hiç yüklenmez.
 * API imzaları {@code src/uyumApi} taslaklarından derlenir (JEI jar'ı derleme sınıf yolunda değildir).
 */
@JeiPlugin
public final class JeiUyumu implements IModPlugin {
    private static final ResourceLocation KIMLIK = DogalAfetler.konum("jei");

    @Override
    public ResourceLocation getPluginUid() {
        return KIMLIK;
    }

    @Override
    public void registerRecipes(IRecipeRegistration kayit) {
        Set<Item> tarifli = tarifliEsyalar();
        int tablodan = 0;
        List<String> genel = new ArrayList<>();
        for (ResourceLocation kimlik : ForgeRegistries.ITEMS.getKeys()) {
            if (!DogalAfetler.MOD_ID.equals(kimlik.getNamespace())) {
                continue;
            }
            Item esya = ForgeRegistries.ITEMS.getValue(kimlik);
            if (esya == null) {
                continue;
            }
            if (EsyaIpuclari.var(kimlik.getPath())) {
                kayit.addItemStackInfo(new ItemStack(esya), EsyaIpuclari.metin(kimlik.getPath()));
                tablodan++;
            } else if (tarifli != null && !tarifli.contains(esya)) {
                kayit.addItemStackInfo(new ItemStack(esya), EsyaIpuclari.genel());
                genel.add(kimlik.getPath());
            }
        }
        DogalAfetler.LOGGER.info("[uyum] JEI bilgi sayfası: {} eşya ipucu tablosundan, {} eşya genel metinle{}", tablodan, genel.size(),
                genel.isEmpty() ? "" : " (EsyaIpuclari'na eklenmeli): " + genel);
    }

    /** Herhangi bir tarifin (üretim, fırın, özel) çıktısı olan mod eşyaları; dünya yoksa {@code null} (genel metin eklenmez). */
    private static Set<Item> tarifliEsyalar() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        Set<Item> esyalar = new HashSet<>();
        RegistryAccess erisim = level.registryAccess();
        for (Recipe<?> tarif : level.getRecipeManager().getRecipes()) {
            ItemStack cikti = tarif.getResultItem(erisim);
            if (!cikti.isEmpty() && DogalAfetler.MOD_ID.equals(ForgeRegistries.ITEMS.getKey(cikti.getItem()).getNamespace())) {
                esyalar.add(cikti.getItem());
            }
        }
        return esyalar;
    }
}
