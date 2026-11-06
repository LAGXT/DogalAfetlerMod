package io.github.kadirberkpolat1.dogalafetler.donma;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.DonusumTablosu;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Donma tablosunu veri paketlerinden yükler (N12): {@code data/<ad alanı>/donma/*.json}. Dosyalar adına göre
 * sıralanıp birleştirilir. Biçim:
 *
 * <pre>{@code
 * {"donusumler": [
 *   {"tur": "su_sutunu", "kaynak": "minecraft:water", "hedef": "minecraft:ice", "ince": "dogalafetler:ince_buz"},
 *   {"tur": "yuzey", "kaynak": "minecraft:grass_block", "hedef": "dogalafetler:kiragili_cimen",
 *    "esik": 10, "cozulmeEsigi": 50}
 * ]}
 * }</pre>
 *
 * Yüzey satırı kuralları: hedef yalnız donmayla oluşan blok olabilir, yani {@code dogalafetler} ad alanında; başka bir
 * hedef (doğada ya da oyuncu elinde de bulunan) çözülmede oyuncunun bloğunu köke çevirirdi, bu yüzden ancak
 * {@code "yabanciHedefOnayi": true} ile kabul edilir. Kaynak ve hedefin blok durumu özellik adları aynı olmalı (özellikler
 * kopyalanır); çift bloklar (kapı, uzun bitki, yatak) reddedilir; eşikler titreşimden büyük olmalı. {@code cozulmeEsigi}
 * verilmezse {@code esik}. Kayıtlı olmayan blok (yüklü olmayan mod) sessizce atlanır; kurala ya da tersinirliğe aykırı satır
 * log'a düşer ve atlanır.
 */
public final class DonmaTablosuYukleyici extends SimpleJsonResourceReloadListener {
    public static final String KLASOR = "donma";
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Etkin tablo. {@code suInce}: su sütununun ilk/son evresindeki ince buz (yoksa {@code suHedef}).
     * {@code esikDonma}/{@code esikCozulme}: yüzey satırlarının eşikleri (eşik aralığı atlaması için).
     */
    public record Tablo(DonusumTablosu<Block> yuzey, @Nullable Block suKaynak, @Nullable Block suHedef,
            @Nullable Block suInce, int[] esikDonma, int[] esikCozulme) {
        static final Tablo BOS = new Tablo(new DonusumTablosu<>(), null, null, null, new int[0], new int[0]);
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
        Block suInce = null;
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
                                if (satir.has("ince")) {
                                    suInce = blok(GsonHelper.getAsString(satir, "ince"));
                                    if (suInce == null || suInce == hedef || yuzey.taniyor(suInce)) {
                                        throw new IllegalArgumentException("ince buz bloğu geçersiz");
                                    }
                                }
                            }
                            case "yuzey" -> {
                                if (kaynak == suKaynak || kaynak == suHedef || hedef == suKaynak || hedef == suHedef
                                        || kaynak == suInce || hedef == suInce) {
                                    throw new IllegalArgumentException("yüzey satırı su sütunu bloklarını kullanıyor");
                                }
                                yuzeySatiriniDenetle(kaynak, hedef, GsonHelper.getAsBoolean(satir, "yabanciHedefOnayi", false));
                                int esik = GsonHelper.getAsInt(satir, "esik");
                                int cozulme = GsonHelper.getAsInt(satir, "cozulmeEsigi", esik);
                                int titresim = DonmaAyarlari.titresimGuvenli();
                                if (esik <= titresim) {
                                    throw new IllegalArgumentException("eşik (" + esik + ") titreşimden (" + titresim
                                            + ") büyük olmalı; yoksa seviye 0'da donuk kalır");
                                }
                                yuzey.ekle(kaynak, hedef, esik, cozulme);
                            }
                            default -> throw new IllegalArgumentException("bilinmeyen tür: " + tur);
                        }
                    } catch (IllegalArgumentException hata) {
                        DogalAfetler.LOGGER.warn("[donma] {}: satır reddedildi ({}): {}", dosya.getKey(), hata.getMessage(),
                                satir);
                    }
                }
            } catch (RuntimeException hata) {
                DogalAfetler.LOGGER.error("[donma] {}: dosya okunamadı: {}", dosya.getKey(), hata.getMessage());
            }
        }
        int n = yuzey.boyut();
        int[] esikDonma = new int[n];
        int[] esikCozulme = new int[n];
        int i = 0;
        for (DonusumTablosu.Donusum<Block> d : yuzey.donusumler()) {
            esikDonma[i] = d.esik();
            esikCozulme[i++] = d.cozulmeEsik();
        }
        etkin = new Tablo(yuzey, suKaynak, suHedef, suInce != null ? suInce : suHedef, esikDonma, esikCozulme);
        DonmaZamanlayici.tabloDegisti();
        DogalAfetler.LOGGER.info("[donma] Donma tablosu yüklendi: {} dosya, {} yüzey dönüşümü, su sütunu {} ↔ {} (ince {})",
                dosyalar.size(), yuzey.boyut(), ad(suKaynak), ad(suHedef), ad(suInce));
    }

    static void yuzeySatiriniDenetle(Block kaynak, Block hedef, boolean yabanciHedefOnayi) {
        ResourceLocation hedefAdi = ForgeRegistries.BLOCKS.getKey(hedef);
        if (!yabanciHedefOnayi && (hedefAdi == null || !DogalAfetler.MOD_ID.equals(hedefAdi.getNamespace()))) {
            throw new IllegalArgumentException("hedef " + hedefAdi + " donmayla oluşan kendi bloğumuz değil; çözülmede "
                    + "oyuncunun bloğu da köke döner (gerekirse \"yabanciHedefOnayi\": true)");
        }
        BlockState k = kaynak.defaultBlockState();
        BlockState h = hedef.defaultBlockState();
        if (ciftBlok(k) || ciftBlok(h)) {
            throw new IllegalArgumentException("çift blok (kapı, uzun bitki, yatak) tabloya alınamaz");
        }
        Set<String> ka = adlar(k);
        Set<String> ha = adlar(h);
        if (!ka.equals(ha)) {
            throw new IllegalArgumentException("blok durumu özellikleri farklı: " + ka + " ≠ " + ha
                    + " (geri dönüşte özellik kaybolur)");
        }
    }

    private static boolean ciftBlok(BlockState s) {
        return s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || s.hasProperty(BlockStateProperties.BED_PART);
    }

    private static Set<String> adlar(BlockState s) {
        return s.getProperties().stream().map(Property::getName).collect(Collectors.toSet());
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
