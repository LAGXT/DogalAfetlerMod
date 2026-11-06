package io.github.kadirberkpolat1.dogalafetler.esya;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.HavaRadari;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarAyarlari;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarCevirileri;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarIstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarPaketi;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarSismografVerileri;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.Sismograf;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografAyarlari;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografCevirileri;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografIstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografPaketi;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import io.github.kadirberkpolat1.dogalafetler.veri.SesSaglayici;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Hava Radarı ve Sismograf'ın (madde 13a-b) kayıtları ve alt sistemin mod kurucusundan çağrılan tek kayıt noktası (K-C33):
 * dört eşya, öncü sarsıntı sesi, config bölümleri ({@code esya.radar}, {@code esya.sismograf}), iki ağ paketi
 * ({@code esya/radar}, {@code esya/sismograf}) ve datagen katkıları. Tarif kimlikleri sabittir; başarım işçisi (#15 "Yer
 * Dinleyici", #16 "Radar Altında") bunları kullanır. Gelişmiş tarifler Kozmik Çekirdeği etiketle ister
 * ({@code #dogalafetler:kozmik_cekirdek}; eşya ve etiket meteor işçisinindir).
 */
public final class EsyaKayitlari {
    private static final DeferredRegister<Item> ESYALAR = DeferredRegister.create(ForgeRegistries.ITEMS, DogalAfetler.MOD_ID);
    private static final DeferredRegister<SoundEvent> SESLER = DeferredRegister.create(Registries.SOUND_EVENT,
            DogalAfetler.MOD_ID);

    public static final RegistryObject<HavaRadari> HAVA_RADARI = ESYALAR.register("hava_radari", () -> new HavaRadari(false));
    public static final RegistryObject<HavaRadari> GELISMIS_HAVA_RADARI = ESYALAR.register("gelismis_hava_radari",
            () -> new HavaRadari(true));
    public static final RegistryObject<Sismograf> SISMOGRAF = ESYALAR.register("sismograf", () -> new Sismograf(false));
    public static final RegistryObject<Sismograf> HASSAS_SISMOGRAF = ESYALAR.register("hassas_sismograf",
            () -> new Sismograf(true));

    /** Öncü sarsıntı sesi: yalnız sismografı taşıyanın istemcisinde çalar. */
    public static final ResourceLocation ONCU_SARSINTI_ADI = DogalAfetler.konum("esya.oncu_sarsinti");
    public static final String ONCU_SARSINTI_ALTYAZI = "subtitles.dogalafetler.esya.oncu_sarsinti";
    public static final RegistryObject<SoundEvent> ONCU_SARSINTI = SESLER.register(ONCU_SARSINTI_ADI.getPath(),
            () -> SoundEvent.createVariableRangeEvent(ONCU_SARSINTI_ADI));

    /** Tarif kimlikleri (sabit; başarım işçisi kullanır). */
    public static final ResourceLocation TARIF_HAVA_RADARI = DogalAfetler.konum("hava_radari");
    public static final ResourceLocation TARIF_GELISMIS_HAVA_RADARI = DogalAfetler.konum("gelismis_hava_radari");
    public static final ResourceLocation TARIF_SISMOGRAF = DogalAfetler.konum("sismograf");
    public static final ResourceLocation TARIF_HASSAS_SISMOGRAF = DogalAfetler.konum("hassas_sismograf");

    /** Kozmik Çekirdek etiketi (meteor işçisi doldurur). */
    public static final TagKey<Item> KOZMIK_CEKIRDEK = ItemTags.create(DogalAfetler.konum("kozmik_cekirdek"));

    private EsyaKayitlari() {
    }

    public static void kaydet(IEventBus modBus) {
        ESYALAR.register(modBus);
        SESLER.register(modBus);
        modBus.addListener(EsyaKayitlari::sekmeyeEkle);
        RadarAyarlari.kaydet();
        RadarIstemciAyarlari.kaydet();
        SismografAyarlari.kaydet();
        SismografIstemciAyarlari.kaydet();
        RadarPaketi.kaydet();
        SismografPaketi.kaydet();
        DataGenerators.ceviriEkle(RadarCevirileri::yaz);
        DataGenerators.ceviriEkle(SismografCevirileri::yaz);
        DataGenerators.sesEkle(EsyaKayitlari::sesleriTanimla);
        DataGenerators.saglayiciEkle(RadarSismografVerileri::ekle);
    }

    private static void sekmeyeEkle(BuildCreativeModeTabContentsEvent olay) {
        if (olay.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            olay.accept(HAVA_RADARI);
            olay.accept(GELISMIS_HAVA_RADARI);
            olay.accept(SISMOGRAF);
            olay.accept(HASSAS_SISMOGRAF);
        }
    }

    private static void sesleriTanimla(SesSaglayici saglayici) {
        saglayici.tanimla(ONCU_SARSINTI_ADI.getPath(), SoundDefinition.definition()
                .subtitle(ONCU_SARSINTI_ALTYAZI)
                .with(SoundDefinition.Sound.sound(DogalAfetler.konum("esya_oncu_sarsinti"), SoundDefinition.SoundType.SOUND)));
    }
}
