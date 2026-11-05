package io.github.kadirberkpolat1.dogalafetler.veri;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Datagen kayıt noktası. Ortak dosyalar (dil dosyaları, sounds.json) tek bir provider tarafından yazılır ve alt
 * sistemler bunlara katkı ekler; kendi dosyalarını yazan provider'lar (tarif, loot, advancement, etiket)
 * {@link #saglayiciEkle} ile eklenir. Kayıtlar mod inşasında yapılır, {@link GatherDataEvent}'te okunur.
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

    @SubscribeEvent
    public static void verileriTopla(GatherDataEvent olay) {
        DataGenerator uretici = olay.getGenerator();
        PackOutput cikti = uretici.getPackOutput();
        boolean istemci = olay.includeClient();

        uretici.addProvider(istemci, new DilSaglayici(cikti, DilSaglayici.INGILIZCE, CEVIRILER));
        uretici.addProvider(istemci, new DilSaglayici(cikti, DilSaglayici.TURKCE, CEVIRILER));
        uretici.addProvider(istemci, new SesSaglayici(cikti, olay.getExistingFileHelper(), SESLER));

        for (SaglayiciEkleyici ekleyici : SAGLAYICILAR) {
            ekleyici.ekle(olay);
        }
    }
}
