package io.github.kadirberkpolat1.dogalafetler.donma;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Donma tablosunu veri paketlerinden yükler (N12): {@code data/<ad alanı>/donma_tablosu/*.json}. Dosyalar adına göre
 * sıralanıp birleştirilir. Biçim:
 *
 * <pre>{@code
 * {"donusumler": [
 *   {"tur": "su_sutunu", "kaynak": "minecraft:water", "hedef": "minecraft:ice"},
 *   {"tur": "yuzey", "kaynak": "minecraft:mud", "hedef": "minecraft:packed_mud", "esik": 60}
 * ]}
 * }</pre>
 *
 * Kayıtlı olmayan blok (yüklü olmayan mod) sessizce atlanır; tersinirliği bozan satır ({@link DonusumTablosu#ekle})
 * hata olarak log'a düşer ve atlanır.
 */
public final class DonmaTablosuYukleyici extends SimpleJsonResourceReloadListener {
    public static final String KLASOR = "donma_tablosu";
    private static final Gson GSON = new GsonBuilder().create();

    public record Tablo(DonusumTablosu<Block> yuzey, @Nullable Block suKaynak, @Nullable Block suHedef) {
        static final Tablo BOS = new Tablo(new DonusumTablosu<>(), null, null);
    }

    private static volatile Tablo etkin = Tablo.BOS;

    public DonmaTablosuYukleyici() {
        super(GSON, KLASOR);
    }

    public static Tablo etkin() {
        return etkin;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> dosyalar, ResourceManager yonetici, ProfilerFiller profil) {
        DonusumTablosu<Block> yuzey = new DonusumTablosu<>();
        Block suKaynak = null;
        Block suHedef = null;
        for (Map.Entry<ResourceLocation, JsonElement> dosya : new TreeMap<>(dosyalar).entrySet()) {
            try {
                for (JsonElement oge : GsonHelper.getAsJsonArray(GsonHelper.convertToJsonObject(dosya.getValue(), "kök"),
                        "donusumler")) {
                    JsonObject satir = GsonHelper.convertToJsonObject(oge, "dönüşüm");
                    String tur = GsonHelper.getAsString(satir, "tur");
                    Block kaynak = blok(GsonHelper.getAsString(satir, "kaynak"));
                    Block hedef = blok(GsonHelper.getAsString(satir, "hedef"));
                    if (kaynak == null || hedef == null) {
                        DogalAfetler.LOGGER.debug("[donma] {}: kayıtlı olmayan blok, satır atlandı: {}", dosya.getKey(), satir);
                        continue;
                    }
                    try {
                        switch (tur) {
                            case "su_sutunu" -> {
                                if (suKaynak != null) {
                                    throw new IllegalArgumentException("ikinci su_sutunu satırı");
                                }
                                if (yuzey.taniyor(kaynak) || yuzey.taniyor(hedef)) {
                                    throw new IllegalArgumentException("su sütunu blokları yüzey tablosunda da var");
                                }
                                suKaynak = kaynak;
                                suHedef = hedef;
                            }
                            case "yuzey" -> {
                                if (kaynak == suKaynak || kaynak == suHedef || hedef == suKaynak || hedef == suHedef) {
                                    throw new IllegalArgumentException("yüzey satırı su sütunu bloklarını kullanıyor");
                                }
                                yuzey.ekle(kaynak, hedef, GsonHelper.getAsInt(satir, "esik"));
                            }
                            default -> throw new IllegalArgumentException("bilinmeyen tür: " + tur);
                        }
                    } catch (IllegalArgumentException hata) {
                        DogalAfetler.LOGGER.error("[donma] {}: satır reddedildi ({}): {}", dosya.getKey(), hata.getMessage(),
                                satir);
                    }
                }
            } catch (RuntimeException hata) {
                DogalAfetler.LOGGER.error("[donma] {}: dosya okunamadı: {}", dosya.getKey(), hata.getMessage());
            }
        }
        etkin = new Tablo(yuzey, suKaynak, suHedef);
        DogalAfetler.LOGGER.info("[donma] Donma tablosu yüklendi: {} dosya, {} yüzey dönüşümü, su sütunu {} ↔ {}",
                dosyalar.size(), yuzey.boyut(), ad(suKaynak), ad(suHedef));
    }

    @Nullable
    private static Block blok(String kimlik) {
        ResourceLocation konum = ResourceLocation.tryParse(kimlik);
        if (konum == null || !ForgeRegistries.BLOCKS.containsKey(konum)) {
            return null;
        }
        return ForgeRegistries.BLOCKS.getValue(konum);
    }

    private static String ad(@Nullable Block blok) {
        return blok == null ? "yok" : String.valueOf(ForgeRegistries.BLOCKS.getKey(blok));
    }
}
