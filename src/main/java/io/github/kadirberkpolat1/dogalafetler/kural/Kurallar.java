package io.github.kadirberkpolat1.dogalafetler.kural;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.GameRules;

/**
 * Madde 11'deki gamerule'lar. Anahtarlar sınıf yüklenirken kaydedilir; {@link #kaydet()} sınıfı ortak kurulumda
 * ana thread'de yükler. {@code /gamerule <ad>} ile okunur/değiştirilir ve tab ile tamamlanır.
 */
public final class Kurallar {
    private static final GameRules.Category KATEGORI = GameRules.Category.MISC;

    public static final GameRules.Key<GameRules.BooleanValue> DOGAL_AFETLER_ACIK = mantiksal("dogalAfetlerAcik", true);
    public static final GameRules.Key<GameRules.BooleanValue> KASIRGA_AFETI = mantiksal("kasirgaAfeti", true);
    public static final GameRules.Key<GameRules.BooleanValue> TSUNAMI_AFETI = mantiksal("tsunamiAfeti", true);
    public static final GameRules.Key<GameRules.BooleanValue> DEPREM_AFETI = mantiksal("depremAfeti", true);
    public static final GameRules.Key<GameRules.BooleanValue> ASIT_YAGMURU_AFETI = mantiksal("asitYagmuruAfeti", true);
    public static final GameRules.Key<GameRules.BooleanValue> SINKHOLE_AFETI = mantiksal("sinkholeAfeti", true);
    public static final GameRules.Key<GameRules.BooleanValue> ORMAN_YANGINI_AFETI = mantiksal("ormanYanginiAfeti", true);
    public static final GameRules.Key<GameRules.BooleanValue> GUNES_PATLAMASI_AFETI = mantiksal("gunesPatlamasiAfeti", true);
    public static final GameRules.Key<GameRules.BooleanValue> AY_CARPMASI_AFETI = mantiksal("ayCarpmasiAfeti", true);
    public static final GameRules.Key<GameRules.IntegerValue> AFET_SIKLIGI = tamsayi("afetSikligi", KuralSinirlari.SIKLIK_VARSAYILAN);
    public static final GameRules.Key<GameRules.IntegerValue> AFET_MAKS_SIDDET = tamsayi("afetMaksSiddet", AfetTuru.EN_YUKSEK_SEVIYE);
    public static final GameRules.Key<GameRules.BooleanValue> AFET_BLOK_HASARI = mantiksal("afetBlokHasari", true);
    public static final GameRules.Key<GameRules.BooleanValue> SICAKLIK_SISTEMI = mantiksal("sicaklikSistemi", true);

    private static final Map<AfetTuru, GameRules.Key<GameRules.BooleanValue>> AFET_KURALLARI;

    static {
        Map<AfetTuru, GameRules.Key<GameRules.BooleanValue>> harita = new EnumMap<>(AfetTuru.class);
        harita.put(AfetTuru.KASIRGA, KASIRGA_AFETI);
        harita.put(AfetTuru.TSUNAMI, TSUNAMI_AFETI);
        harita.put(AfetTuru.DEPREM, DEPREM_AFETI);
        harita.put(AfetTuru.ASIT_YAGMURU, ASIT_YAGMURU_AFETI);
        harita.put(AfetTuru.SINKHOLE, SINKHOLE_AFETI);
        harita.put(AfetTuru.ORMAN_YANGINI, ORMAN_YANGINI_AFETI);
        harita.put(AfetTuru.GUNES_PATLAMASI, GUNES_PATLAMASI_AFETI);
        harita.put(AfetTuru.AY_CARPMASI, AY_CARPMASI_AFETI);
        AFET_KURALLARI = Collections.unmodifiableMap(harita);
    }

    /** Madde 11 sırasıyla bütün mantıksal kurallar. */
    public static final List<GameRules.Key<GameRules.BooleanValue>> MANTIKSAL_KURALLAR = List.of(
            DOGAL_AFETLER_ACIK, KASIRGA_AFETI, TSUNAMI_AFETI, DEPREM_AFETI, ASIT_YAGMURU_AFETI, SINKHOLE_AFETI,
            ORMAN_YANGINI_AFETI, GUNES_PATLAMASI_AFETI, AY_CARPMASI_AFETI, AFET_BLOK_HASARI, SICAKLIK_SISTEMI);

    /** Madde 11 sırasıyla bütün tamsayı kurallar. */
    public static final List<GameRules.Key<GameRules.IntegerValue>> TAMSAYI_KURALLAR = List.of(
            AFET_SIKLIGI, AFET_MAKS_SIDDET);

    private Kurallar() {
    }

    private static GameRules.Key<GameRules.BooleanValue> mantiksal(String ad, boolean varsayilan) {
        return GameRules.register(ad, KATEGORI, GameRules.BooleanValue.create(varsayilan));
    }

    private static GameRules.Key<GameRules.IntegerValue> tamsayi(String ad, int varsayilan) {
        return GameRules.register(ad, KATEGORI, GameRules.IntegerValue.create(varsayilan));
    }

    /** Sınıfı yükleyerek kuralları kaydeder; ortak kurulumda ana thread'de çağrılır. */
    public static void kaydet() {
        DogalAfetler.LOGGER.info("{} gamerule kaydedildi", MANTIKSAL_KURALLAR.size() + TAMSAYI_KURALLAR.size());
    }

    /** Afet türünün kendi aç/kapa kuralı. */
    public static GameRules.Key<GameRules.BooleanValue> afetKurali(AfetTuru tur) {
        return AFET_KURALLARI.get(tur);
    }

    /** Afet yeni seçilebilir mi: genel anahtar ve türün anahtarı birlikte açık olmalı. */
    public static boolean afetSecilebilir(GameRules kurallar, AfetTuru tur) {
        return kurallar.getBoolean(DOGAL_AFETLER_ACIK) && kurallar.getBoolean(afetKurali(tur));
    }

    /** {@code afetSikligi}'nın olasılıklara uygulanacak çarpanı (100 → 1.0). */
    public static double siklikCarpani(GameRules kurallar) {
        return KuralSinirlari.siklikCarpani(kurallar.getInt(AFET_SIKLIGI));
    }

    /** {@code afetMaksSiddet}, [1, 5] aralığında. */
    public static int maksSiddet(GameRules kurallar) {
        return KuralSinirlari.maksSiddet(kurallar.getInt(AFET_MAKS_SIDDET));
    }

    public static boolean blokHasariAcik(GameRules kurallar) {
        return kurallar.getBoolean(AFET_BLOK_HASARI);
    }

    public static boolean sicaklikKuraliAcik(GameRules kurallar) {
        return kurallar.getBoolean(SICAKLIK_SISTEMI);
    }

    /** Dünya oluşturma ekranındaki kural adları ve açıklamaları ({@code gamerule.<ad>[.description]}). */
    public static void cevirileriYaz(CeviriYazici y) {
        kural(y, DOGAL_AFETLER_ACIK, "Natural disasters", "Doğal afetler",
                "Turns the whole disaster system on or off.", "Bütün afet sistemini açar ya da kapatır.");
        kural(y, KASIRGA_AFETI, "Tornadoes", "Kasırgalar",
                "Allows tornadoes to be chosen.", "Kasırganın seçilmesine izin verir.");
        kural(y, TSUNAMI_AFETI, "Tsunamis", "Tsunamiler",
                "Allows tsunamis to be chosen.", "Tsunaminin seçilmesine izin verir.");
        kural(y, DEPREM_AFETI, "Earthquakes", "Depremler",
                "Allows earthquakes to be chosen.", "Depremin seçilmesine izin verir.");
        kural(y, ASIT_YAGMURU_AFETI, "Acid rain", "Asit yağmuru",
                "Allows acid rain to be chosen.", "Asit yağmurunun seçilmesine izin verir.");
        kural(y, SINKHOLE_AFETI, "Sinkholes", "Sinkhole'lar",
                "Allows sinkholes to be chosen.", "Sinkhole'un seçilmesine izin verir.");
        kural(y, ORMAN_YANGINI_AFETI, "Wildfires", "Orman yangınları",
                "Allows wildfires to be chosen.", "Orman yangınının seçilmesine izin verir.");
        kural(y, GUNES_PATLAMASI_AFETI, "Solar flare", "Güneş patlaması",
                "Allows the solar flare to be chosen. Turning it off stops an unfinished process; reached eras stay.",
                "Güneş patlamasının seçilmesine izin verir. Kapatılınca bitmemiş süreç durur; ulaşılmış dönemler kalır.");
        kural(y, AY_CARPMASI_AFETI, "Moon impact", "Ay çarpması",
                "Allows the moon impact to be chosen. Turning it off stops an unfinished process; reached eras stay.",
                "Ay çarpmasının seçilmesine izin verir. Kapatılınca bitmemiş süreç durur; ulaşılmış dönemler kalır.");
        kural(y, AFET_SIKLIGI, "Disaster frequency (%)", "Afet sıklığı (%)",
                "100 = normal, 50 = half, 200 = double (0-1000); waits scale too. Effect: /dogalafet siklik",
                "100 = normal, 50 = yarı, 200 = iki katı (0-1000); beklemeler de ölçeklenir. Etkisi: /dogalafet siklik");
        kural(y, AFET_MAKS_SIDDET, "Maximum disaster level", "En yüksek afet seviyesi",
                "Highest level a disaster can reach (1-5).", "Bir afetin çıkabileceği en yüksek seviye (1-5).");
        kural(y, AFET_BLOK_HASARI, "Disaster block damage", "Afet blok hasarı",
                "When off, disasters do not break, burn, freeze or thaw blocks.",
                "Kapalıyken afetler blok yıkmaz, yakmaz, dondurmaz, çözmez.");
        kural(y, SICAKLIK_SISTEMI, "Body temperature", "Vücut sıcaklığı",
                "Turns the body temperature system and its HUD on or off.",
                "Vücut sıcaklığı sistemini ve göstergesini açar ya da kapatır.");
    }

    private static void kural(CeviriYazici y, GameRules.Key<?> anahtar, String ingilizceAd, String turkceAd,
            String ingilizceAciklama, String turkceAciklama) {
        y.ekle(anahtar.getDescriptionId(), ingilizceAd, turkceAd);
        y.ekle(anahtar.getDescriptionId() + ".description", ingilizceAciklama, turkceAciklama);
    }
}
