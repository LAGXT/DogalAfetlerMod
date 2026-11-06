package io.github.kadirberkpolat1.dogalafetler.veri;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Datagen kayıt noktası. Ortak dosyalar (dil dosyaları, sounds.json, vanilla blok etiketleri) tek bir provider tarafından
 * yazılır ve alt sistemler bunlara katkı ekler; kendi dosyalarını yazan provider'lar (tarif, loot, advancement,
 * {@code dogalafetler:*} etiketleri) {@link #saglayiciEkle} ile eklenir. Kayıtlar mod inşasında yapılır,
 * {@link GatherDataEvent}'te okunur.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGenerators {
    /** {@link GatherDataEvent} geldiğinde kendi provider'ını {@code olay.getGenerator()}'a ekleyen katkı. */
    @FunctionalInterface
    public interface SaglayiciEkleyici {
        void ekle(GatherDataEvent olay);
    }

    private static final List<Consumer<CeviriYazici>> CEVIRILER = new CopyOnWriteArrayList<>();
    private static final List<Consumer<SesSaglayici>> SESLER = new CopyOnWriteArrayList<>();
    private static final List<SaglayiciEkleyici> SAGLAYICILAR = new CopyOnWriteArrayList<>();
    private static final Map<TagKey<Block>, List<Supplier<? extends Block>>> VANILLA_BLOK_ETIKETLERI = new LinkedHashMap<>();

    private DataGenerators() {
    }

    public static void ceviriEkle(Consumer<CeviriYazici> katki) {
        CEVIRILER.add(Objects.requireNonNull(katki, "katki"));
    }

    public static void sesEkle(Consumer<SesSaglayici> katki) {
        SESLER.add(Objects.requireNonNull(katki, "katki"));
    }

    public static void saglayiciEkle(SaglayiciEkleyici ekleyici) {
        SAGLAYICILAR.add(Objects.requireNonNull(ekleyici, "ekleyici"));
    }

    /**
     * Vanilla ({@code minecraft:*}) bir blok etiketine katkı (K-E12): bütün katkılar tek sağlayıcıda ({@link OrtakEtiketler})
     * yazılır. Alt sistemin kendi etiket sağlayıcısı vanilla etiket yazmaz. Bloklar datagen'de çözülür.
     */
    @SafeVarargs
    public static synchronized void vanillaBlokEtiketiEkle(TagKey<Block> etiket, Supplier<? extends Block>... bloklar) {
        if (!"minecraft".equals(etiket.location().getNamespace())) {
            throw new IllegalArgumentException("vanilla olmayan etiket ortak sağlayıcıya eklenemez: " + etiket.location());
        }
        List<Supplier<? extends Block>> liste = VANILLA_BLOK_ETIKETLERI.computeIfAbsent(etiket, e -> new ArrayList<>());
        for (Supplier<? extends Block> blok : bloklar) {
            liste.add(Objects.requireNonNull(blok, "blok"));
        }
    }

    @SubscribeEvent
    public static void verileriTopla(GatherDataEvent olay) {
        DataGenerator uretici = olay.getGenerator();
        PackOutput cikti = uretici.getPackOutput();
        boolean istemci = olay.includeClient();

        uretici.addProvider(istemci, new DilSaglayici(cikti, DilSaglayici.INGILIZCE, CEVIRILER));
        uretici.addProvider(istemci, new DilSaglayici(cikti, DilSaglayici.TURKCE, CEVIRILER));
        uretici.addProvider(istemci, new SesSaglayici(cikti, olay.getExistingFileHelper(), SESLER));
        uretici.addProvider(olay.includeServer(), new OrtakEtiketler(cikti, olay.getLookupProvider(),
                olay.getExistingFileHelper(), VANILLA_BLOK_ETIKETLERI));

        for (SaglayiciEkleyici ekleyici : SAGLAYICILAR) {
            ekleyici.ekle(olay);
        }
    }
}
