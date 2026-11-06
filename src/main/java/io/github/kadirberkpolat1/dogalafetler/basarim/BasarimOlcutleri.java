package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.SagCikmaKosulu;
import io.github.kadirberkpolat1.dogalafetler.basarim.olcut.AfetOlcutu;
import io.github.kadirberkpolat1.dogalafetler.basarim.olcut.BasitOlcut;
import io.github.kadirberkpolat1.dogalafetler.basarim.olcut.EsikOlcutu;
import io.github.kadirberkpolat1.dogalafetler.basarim.olcut.EsyaOlcutu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;

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
    public static final EsyaOlcutu ESYA_TOPLANDI = new EsyaOlcutu("esya_toplandi");
    public static final EsyaOlcutu ESYA_URETILDI = new EsyaOlcutu("esya_uretildi");
    public static final EsikOlcutu TUY_UPGRADE = new EsikOlcutu("tuy_upgrade", "min", false);
    public static final EsikOlcutu YUNLU_PARCA = new EsikOlcutu("yunlu_parca", "min", false);
    public static final BasitOlcut CORBA_ICER_ISINIR = new BasitOlcut("corba_icer_isinir");
    public static final EsikOlcutu BALIK_TUTULDU = new EsikOlcutu("balik_tutuldu", "min", false);
    public static final EsikOlcutu BIRLIKTE_HAYATTA = new EsikOlcutu("birlikte_hayatta", "min", false);
    public static final BasitOlcut HEPSINI_ATLATTI = new BasitOlcut("hepsini_atlatti");

    private static final List<CriterionTrigger<?>> HEPSI = List.of(SIREN_DUYULDU, AFETTEN_SAG_CIKTI, SUPERNOVA_GORUNDU,
            YILDIZLI_GECE, DONMA_ILERLEMESI, KUTUP_SIGINAGI, DENIZ_YUKSELDI, ESYA_TOPLANDI, ESYA_URETILDI, TUY_UPGRADE,
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

    /** Advancement JSON'undaki ölçüt adı. */
    public static String olcutAdi(Basarimlar basarim) {
        return switch (basarim) {
            case KOK -> "siren";
            case GOKTEN_GELEN_MISAFIR, KOZMIK_SANS -> "topla";
            case RADAR_ALTINDA, YER_DINLEYICI -> "uret";
            case BUZ_BALIKCISI, KUTUP_USTASI -> "balik";
            default -> "olcut";
        };
    }

    /** Başarımın tek ölçütü (madde 12, N16; sayılar şartnameden). */
    public static CriterionTriggerInstance olcut(Basarimlar basarim) {
        return switch (basarim) {
            case KOK -> SIREN_DUYULDU.ornek();
            case ILK_RUZGAR -> AFETTEN_SAG_CIKTI.ornek(AfetTuru.KASIRGA, 0, null);
            case DALGA_AVCISI -> AFETTEN_SAG_CIKTI.ornek(AfetTuru.TSUNAMI, 0, SagCikmaKosulu.SURUKLENDI);
            case SALLANAN_ZEMIN -> AFETTEN_SAG_CIKTI.ornek(AfetTuru.DEPREM, 0, null);
            case DELIKTEN_DONUS -> AFETTEN_SAG_CIKTI.ornek(AfetTuru.SINKHOLE, 0, SagCikmaKosulu.CUKURA_DUSTU);
            case YANMADAN_CIKTIM -> AFETTEN_SAG_CIKTI.ornek(AfetTuru.ORMAN_YANGINI, 0, SagCikmaKosulu.YANGIN_ALANINDA);
            case CATI_ALTI -> AFETTEN_SAG_CIKTI.ornek(AfetTuru.ASIT_YAGMURU, 0, SagCikmaKosulu.HASARSIZ);
            case GOKTEN_GELEN_MISAFIR -> ESYA_TOPLANDI.ornek(BasarimKimlikleri.METEORIT_PARCASI);
            case KOZMIK_SANS -> ESYA_TOPLANDI.ornek(BasarimKimlikleri.KOZMIK_CEKIRDEK);
            case GUNESI_ATLATTIM -> AFETTEN_SAG_CIKTI.ornek(AfetTuru.GUNES_PATLAMASI, 0, null);
            case SUPERNOVA_TANIGI -> SUPERNOVA_GORUNDU.ornek();
            case BUZUL_CAGI -> DONMA_ILERLEMESI.ornek(50);
            case KUTUP_SIGINAGI -> KUTUP_SIGINAGI.ornek(3);
            case BUZ_BALIKCISI -> BALIK_TUTULDU.ornek(1);
            case KUTUP_USTASI -> BALIK_TUTULDU.ornek(25);
            case AY_DUSTU -> AFETTEN_SAG_CIKTI.ornek(AfetTuru.AY_CARPMASI, 0, null);
            case YILDIZLI_GECE -> YILDIZLI_GECE.ornek(10);
            case YUKSELEN_SULAR -> DENIZ_YUKSELDI.ornek();
            case RADAR_ALTINDA -> ESYA_URETILDI.ornek(BasarimKimlikleri.HAVA_RADARI);
            case YER_DINLEYICI -> ESYA_URETILDI.ornek(BasarimKimlikleri.SISMOGRAF);
            case KATMANLI_GIYIM -> TUY_UPGRADE.ornek(5);
            case YUNLU_KAHRAMAN -> YUNLU_PARCA.ornek(4);
            case CORBA_ICER_ISINIR -> CORBA_ICER_ISINIR.ornek();
            case FIRTINAYA_MEYDAN_OKU -> AFETTEN_SAG_CIKTI.ornek(null, 3, null);
            case FELAKET_USTASI -> AFETTEN_SAG_CIKTI.ornek(null, 5, null);
            case HEPSINI_ATLATTIM -> HEPSINI_ATLATTI.ornek();
            case BIRLIKTE_HAYATTA -> BIRLIKTE_HAYATTA.ornek(3);
        };
    }
}
