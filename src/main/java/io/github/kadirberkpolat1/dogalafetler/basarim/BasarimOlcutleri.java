package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.SagCikmaKosulu;
import io.github.kadirberkpolat1.dogalafetler.basarim.olcut.AfetOlcutu;
import io.github.kadirberkpolat1.dogalafetler.basarim.olcut.BasitOlcut;
import io.github.kadirberkpolat1.dogalafetler.basarim.olcut.EsikOlcutu;
import io.github.kadirberkpolat1.dogalafetler.basarim.olcut.EsyaOlcutu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.PlayerPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Başarımların ölçüt tetikleyicileri ve her başarımın ölçütü. Nesneler sınıf yüklenince kurulur (yan etkisi yok);
 * vanilla {@code CriteriaTriggers} kaydı {@link #kaydet()} ile ortak kurulumun ana thread kuyruğunda yapılır
 * (kayıt haritası thread-safe değil, mod kurucuları paralel koşar). Advancement'lar sunucu açılırken yüklendiği için
 * bu yeterince erkendir.
 */
public final class BasarimOlcutleri {
    public static final BasitOlcut SIREN_DUYULDU = new BasitOlcut("siren_duyuldu");
    public static final AfetOlcutu AFETTEN_SAG_CIKTI = new AfetOlcutu("afetten_sag_cikti");
    public static final BasitOlcut SUPERNOVA_GORUNDU = new BasitOlcut("supernova_gorundu");
    public static final EsikOlcutu YILDIZLI_GECE = new EsikOlcutu("yildizli_gece", "saniye", false);
    public static final EsikOlcutu DONMA_ILERLEMESI = new EsikOlcutu("donma_ilerlemesi", "esik", true);
    public static final EsikOlcutu KUTUP_SIGINAGI = new EsikOlcutu("kutup_siginagi", "gun", false);
    public static final BasitOlcut DENIZ_YUKSELDI = new BasitOlcut("deniz_yukseldi");
    public static final EsyaOlcutu ESYA_URETILDI = new EsyaOlcutu("esya_uretildi");
    public static final EsikOlcutu TUY_UPGRADE = new EsikOlcutu("tuy_upgrade", "min", false);
    public static final EsikOlcutu YUNLU_PARCA = new EsikOlcutu("yunlu_parca", "min", false);
    public static final BasitOlcut CORBA_ICER_ISINIR = new BasitOlcut("corba_icer_isinir");
    public static final EsikOlcutu BALIK_TUTULDU = new EsikOlcutu("balik_tutuldu", "min", false);
    public static final EsikOlcutu BIRLIKTE_HAYATTA = new EsikOlcutu("birlikte_hayatta", "min", false);
    public static final BasitOlcut HEPSINI_ATLATTI = new BasitOlcut("hepsini_atlatti");

    private static final List<CriterionTrigger<?>> HEPSI = List.of(SIREN_DUYULDU, AFETTEN_SAG_CIKTI, SUPERNOVA_GORUNDU,
            YILDIZLI_GECE, DONMA_ILERLEMESI, KUTUP_SIGINAGI, DENIZ_YUKSELDI, ESYA_URETILDI, TUY_UPGRADE,
            YUNLU_PARCA, CORBA_ICER_ISINIR, BALIK_TUTULDU, BIRLIKTE_HAYATTA, HEPSINI_ATLATTI);

    private BasarimOlcutleri() {
    }

    /** Ortak kurulumda, ana thread'de bir kez. */
    static void kaydet() {
        for (CriterionTrigger<?> tetikleyici : HEPSI) {
            CriteriaTriggers.register(tetikleyici);
        }
    }

    public static List<CriterionTrigger<?>> hepsi() {
        return HEPSI;
    }

    /**
     * Başarımın ölçütleri (JSON'daki ad → örnek; madde 12, N16; sayılar şartnameden). Birden çoksa herhangi biri yeter
     * ({@code requirements} OR). Eşya sahipliği vanilla {@code minecraft:inventory_changed} ile (A-36): yerden alma, sandık,
     * yaratıcı envanter hepsi sayılır. Üretim başarımları hayatta kalmada üretimle, yaratıcı kipte eşya envantere girince.
     */
    public static Map<String, CriterionTriggerInstance> olcutler(Basarimlar basarim) {
        return switch (basarim) {
            case KOK -> tek("siren", SIREN_DUYULDU.ornek());
            case ILK_RUZGAR -> tek(AFETTEN_SAG_CIKTI.ornek(AfetTuru.KASIRGA, 0, null));
            case DALGA_AVCISI -> tek(AFETTEN_SAG_CIKTI.ornek(AfetTuru.TSUNAMI, 0, SagCikmaKosulu.SURUKLENDI));
            case SALLANAN_ZEMIN -> tek(AFETTEN_SAG_CIKTI.ornek(AfetTuru.DEPREM, 0, null));
            case DELIKTEN_DONUS -> tek(AFETTEN_SAG_CIKTI.ornek(AfetTuru.SINKHOLE, 0, SagCikmaKosulu.CUKURA_DUSTU));
            case YANMADAN_CIKTIM -> tek(AFETTEN_SAG_CIKTI.ornek(AfetTuru.ORMAN_YANGINI, 0,
                    SagCikmaKosulu.YANGIN_ALANINDA));
            case CATI_ALTI -> tek(AFETTEN_SAG_CIKTI.ornek(AfetTuru.ASIT_YAGMURU, 0, SagCikmaKosulu.HASARSIZ));
            case GOKTEN_GELEN_MISAFIR -> tek("topla", envanterde(BasarimKimlikleri.METEORIT_PARCASI, null));
            case KOZMIK_SANS -> tek("topla", envanterde(BasarimKimlikleri.KOZMIK_CEKIRDEK, null));
            case GUNESI_ATLATTIM -> tek(AFETTEN_SAG_CIKTI.ornek(AfetTuru.GUNES_PATLAMASI, 0, null));
            case SUPERNOVA_TANIGI -> tek(SUPERNOVA_GORUNDU.ornek());
            case BUZUL_CAGI -> tek(DONMA_ILERLEMESI.ornek(50));
            case KUTUP_SIGINAGI -> tek(KUTUP_SIGINAGI.ornek(3));
            case BUZ_BALIKCISI -> tek("balik", BALIK_TUTULDU.ornek(1));
            case KUTUP_USTASI -> tek("balik", BALIK_TUTULDU.ornek(25));
            case AY_DUSTU -> tek(AFETTEN_SAG_CIKTI.ornek(AfetTuru.AY_CARPMASI, 0, null));
            case YILDIZLI_GECE -> tek(YILDIZLI_GECE.ornek(10));
            case YUKSELEN_SULAR -> tek(DENIZ_YUKSELDI.ornek());
            case RADAR_ALTINDA -> uretYaDaYaratici(BasarimKimlikleri.HAVA_RADARI);
            case YER_DINLEYICI -> uretYaDaYaratici(BasarimKimlikleri.SISMOGRAF);
            case KATMANLI_GIYIM -> tek(TUY_UPGRADE.ornek(5));
            case YUNLU_KAHRAMAN -> tek(YUNLU_PARCA.ornek(4));
            case CORBA_ICER_ISINIR -> tek(CORBA_ICER_ISINIR.ornek());
            case FIRTINAYA_MEYDAN_OKU -> tek(AFETTEN_SAG_CIKTI.ornek(null, 3, null));
            case FELAKET_USTASI -> tek(AFETTEN_SAG_CIKTI.ornek(null, 5, null));
            case HEPSINI_ATLATTIM -> tek(HEPSINI_ATLATTI.ornek());
            case BIRLIKTE_HAYATTA -> tek(BIRLIKTE_HAYATTA.ornek(3));
        };
    }

    private static Map<String, CriterionTriggerInstance> tek(CriterionTriggerInstance ornek) {
        return tek("olcut", ornek);
    }

    private static Map<String, CriterionTriggerInstance> tek(String ad, CriterionTriggerInstance ornek) {
        return Map.of(ad, ornek);
    }

    private static Map<String, CriterionTriggerInstance> uretYaDaYaratici(ResourceLocation esya) {
        Map<String, CriterionTriggerInstance> olcutler = new LinkedHashMap<>();
        olcutler.put("uret", ESYA_URETILDI.ornek(esya));
        olcutler.put("yaratici", envanterde(esya, GameType.CREATIVE));
        return olcutler;
    }

    /** Eşya envantere girdi ({@code oyunKipi} null değilse yalnız o kipte). Eşya datagen anında kayıtlı olmalı. */
    private static CriterionTriggerInstance envanterde(ResourceLocation kimlik, GameType oyunKipi) {
        Item esya = ForgeRegistries.ITEMS.getValue(kimlik);
        if (esya == null || esya == Items.AIR) {
            throw new IllegalStateException("Başarım eşyası kayıtlı değil: " + kimlik);
        }
        ContextAwarePredicate oyuncu = oyunKipi == null ? ContextAwarePredicate.ANY
                : EntityPredicate.wrap(EntityPredicate.Builder.entity()
                        .subPredicate(PlayerPredicate.Builder.player().setGameType(oyunKipi).build()).build());
        return new InventoryChangeTrigger.TriggerInstance(oyuncu, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY,
                MinMaxBounds.Ints.ANY, new ItemPredicate[] {ItemPredicate.Builder.item().of(esya).build()});
    }
}
