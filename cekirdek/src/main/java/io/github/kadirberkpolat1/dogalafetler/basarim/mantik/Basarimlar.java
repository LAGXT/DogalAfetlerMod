package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * "Doğal Afetler" başarım sekmesi: kök "Felaket Başlıyor", madde 12 listesindeki 26 başarım, N29'daki "Yeni Şafak" (27) ve
 * K-E74'ün 18 yeni başarımı (28–45: gökyüzü, kütük, tsunami ve yaratıcı başarımlar; toplam kök + 45 = 46). Madde 12 ve N29
 * başarımları şartnamedeki numara, tür ve gizlilikle birebirdir. Advancement kimliği {@code dogalafetler:dogalafetler/<yol>}.
 * Datagen (JSON, dil dosyaları) ve testler bu listeyi okur. Ebeveyn her zaman önce tanımlıdır; ağaç sekmenin yerleşimidir, ön koşul
 * değildir (vanilla: çocuk ebeveynden önce kazanılabilir). Mod eşyası ikonları ({@code dogalafetler:}) datagen anında kayıtlı
 * değilse vanilla yedeği kullanılır.
 */
public enum Basarimlar {
    KOK(0, "root", BasarimTuru.GOREV, null, false, null,
            "minecraft:bell", null,
            "Disaster Begins", "Felaket Başlıyor",
            "Hear your first disaster siren",
            "İlk afet sirenini duy"),
    ILK_RUZGAR(1, "ilk_ruzgar", BasarimTuru.GOREV, KOK, false, null,
            "minecraft:elytra", null,
            "First Wind", "İlk Rüzgâr",
            "Survive a tornado",
            "Bir kasırgadan sağ çık"),
    DALGA_AVCISI(2, "dalga_avcisi", BasarimTuru.GOREV, KOK, false, null,
            "minecraft:trident", null,
            "Wave Rider", "Dalga Avcısı",
            "Get swept away by a tsunami wave and survive",
            "Bir tsunami dalgasına kapılıp sağ çık"),
    SALLANAN_ZEMIN(3, "sallanan_zemin", BasarimTuru.GOREV, KOK, false, null,
            "minecraft:cracked_stone_bricks", null,
            "Shaking Ground", "Sallanan Zemin",
            "Survive an earthquake",
            "Bir depremden sağ çık"),
    DELIKTEN_DONUS(4, "delikten_donus", BasarimTuru.GOREV, KOK, false, null,
            "minecraft:ladder", null,
            "Back from the Pit", "Delikten Dönüş",
            "Fall into a collapsing sinkhole and survive",
            "Çöken bir sinkhole'a düşüp sağ çık"),
    YANMADAN_CIKTIM(5, "yanmadan_ciktim", BasarimTuru.GOREV, KOK, false, null,
            "minecraft:campfire", null,
            "Not Even Singed", "Yanmadan Çıktım",
            "Stay inside a wildfire for at least 30 seconds and survive",
            "Bir orman yangınının alanında en az 30 saniye kalıp sağ çık"),
    CATI_ALTI(6, "cati_alti", BasarimTuru.GOREV, KOK, false, null,
            "minecraft:brick_stairs", null,
            "Under a Roof", "Çatı Altı",
            "Get through an acid rain without taking any damage",
            "Bir asit yağmurunu hiç hasar almadan atlat"),
    GOKTEN_GELEN_MISAFIR(7, "gokten_gelen_misafir", BasarimTuru.GOREV, KOK, false, null,
            "dogalafetler:meteorit_parcasi", "minecraft:fire_charge",
            "Visitor from the Sky", "Gökten Gelen Misafir",
            "Pick up your first Meteorite Fragment",
            "İlk Meteorit Parçası'nı topla"),
    KOZMIK_SANS(22, "kozmik_sans", BasarimTuru.MEYDAN_OKUMA, GOKTEN_GELEN_MISAFIR, false, Unvan.KOZMIK_KASIF,
            "dogalafetler:kozmik_cekirdek", "minecraft:end_crystal",
            "Cosmic Luck", "Kozmik Şans",
            "Find your first Cosmic Core",
            "İlk Kozmik Çekirdek'i bul"),
    GUNESI_ATLATTIM(8, "gunesi_atlattim", BasarimTuru.HEDEF, KOK, false, null,
            "minecraft:sunflower", null,
            "Outlived the Sun", "Güneşi Atlattım",
            "Survive the five-day solar flare",
            "Güneş patlamasının 5 Minecraft günlük sürecinden sağ çık"),
    SUPERNOVA_TANIGI(9, "supernova_tanigi", BasarimTuru.HEDEF, GUNESI_ATLATTIM, false, null,
            "minecraft:nether_star", null,
            "Supernova Witness", "Süpernova Tanığı",
            "See the supernova in the sky",
            "Süpernovayı gökyüzünde gör"),
    BUZUL_CAGI(10, "buzul_cagi", BasarimTuru.HEDEF, SUPERNOVA_TANIGI, false, null,
            "minecraft:packed_ice", null,
            "Ice Age", "Buzul Çağı",
            "Be alive when the world freeze passes 50%",
            "Dünyanın donma ilerlemesi %50'yi geçerken hayatta ol"),
    KUTUP_SIGINAGI(11, "kutup_siginagi", BasarimTuru.MEYDAN_OKUMA, BUZUL_CAGI, false, Unvan.KUTUP_SAKINI,
            "minecraft:blue_ice", null,
            "Polar Refuge", "Kutup Sığınağı",
            "Stay alive for 3 Minecraft days after the freeze reaches 100%",
            "Donma %100'e ulaştıktan sonra 3 Minecraft günü hayatta kal"),
    BUZ_BALIKCISI(20, "buz_balikcisi", BasarimTuru.GOREV, BUZUL_CAGI, false, null,
            "minecraft:fishing_rod", null,
            "Ice Fisher", "Buz Balıkçısı",
            "Catch your first fish through an ice hole",
            "Buz deliğinden ilk balığını tut"),
    KUTUP_USTASI(21, "kutup_ustasi", BasarimTuru.HEDEF, BUZ_BALIKCISI, false, null,
            "minecraft:cod", null,
            "Polar Master", "Kutup Ustası",
            "Catch a total of 25 fish through ice holes",
            "Buz deliğinden toplam 25 balık tut"),
    AY_DUSTU(12, "ay_dustu", BasarimTuru.HEDEF, KOK, false, null,
            "minecraft:end_stone", null,
            "The Moon Fell", "Ay Düştü",
            "Survive the moon impact",
            "Ay çarpmasından sağ çık"),
    YILDIZLI_GECE(13, "yildizli_gece", BasarimTuru.GOREV, AY_DUSTU, false, null,
            "minecraft:spyglass", null,
            "Starry Night", "Yıldızlı Gece",
            "Watch the starry sky for at least 10 seconds on a night after the moon is gone",
            "Ay yok olduktan sonraki yıldızlı bir gecede gökyüzünü en az 10 saniye izle"),
    YUKSELEN_SULAR(14, "yukselen_sular", BasarimTuru.GOREV, AY_DUSTU, false, null,
            "minecraft:conduit", null,
            "Rising Waters", "Yükselen Sular",
            "Live through the sea level rise and stay alive",
            "Deniz seviyesinin yükselişini yaşa ve sağ kal"),
    RADAR_ALTINDA(16, "radar_altinda", BasarimTuru.GOREV, KOK, false, null,
            "dogalafetler:hava_radari", "minecraft:compass",
            "Under the Radar", "Radar Altında",
            "Craft your first Weather Radar",
            "İlk Hava Radarı'nı üret"),
    YER_DINLEYICI(15, "yer_dinleyici", BasarimTuru.GOREV, RADAR_ALTINDA, false, null,
            "dogalafetler:sismograf", "minecraft:sculk_sensor",
            "Ground Listener", "Yer Dinleyici",
            "Craft your first Seismograph",
            "İlk Sismograf'ı üret"),
    KATMANLI_GIYIM(17, "katmanli_giyim", BasarimTuru.GOREV, KOK, false, null,
            "minecraft:feather", null,
            "Layered Up", "Katmanlı Giyim",
            "Apply 5 Feather Upgrades to one armor piece",
            "Bir zırh parçasına 5 Tüy Upgrade'i uygula"),
    YUNLU_KAHRAMAN(18, "yunlu_kahraman", BasarimTuru.HEDEF, KATMANLI_GIYIM, false, null,
            "minecraft:white_wool", null,
            "Woolly Hero", "Yünlü Kahraman",
            "Wear 4 woolly armor pieces at once",
            "Aynı anda 4 parça yünlü zırh giy"),
    CORBA_ICER_ISINIR(19, "corba_icer_isinir", BasarimTuru.GOREV, YUNLU_KAHRAMAN, false, null,
            "dogalafetler:sicak_tavuk_corbasi", "minecraft:mushroom_stew",
            "Soup Warms the Soul", "Çorba İçer Isınır",
            "While freezing, eat a hot food and bring your body temperature back to normal",
            "Donarken sıcak bir yiyecek ye ve vücut sıcaklığını normale döndür"),
    FIRTINAYA_MEYDAN_OKU(23, "firtinaya_meydan_oku", BasarimTuru.HEDEF, KOK, false, null,
            "minecraft:lightning_rod", null,
            "Defy the Storm", "Fırtınaya Meydan Oku",
            "Survive a level 3 (Severe) or stronger disaster",
            "Seviye 3 (Şiddetli) ya da üzeri bir afetten sağ çık"),
    FELAKET_USTASI(24, "felaket_ustasi", BasarimTuru.MEYDAN_OKUMA, FIRTINAYA_MEYDAN_OKU, false, Unvan.FELAKET_USTASI,
            "minecraft:totem_of_undying", null,
            "Master of Disaster", "Felaket Ustası",
            "Survive a level 5 (Catastrophic) disaster",
            "Seviye 5 (Felaket) bir afetten sağ çık"),
    HEPSINI_ATLATTIM(25, "hepsini_atlattim", BasarimTuru.MEYDAN_OKUMA, FELAKET_USTASI, true, Unvan.HER_SEYI_ATLATAN,
            "minecraft:beacon", null,
            "Been Through It All", "Hepsini Atlattım",
            "Survive a tornado, tsunami, earthquake, acid rain, sinkhole, wildfire, solar flare and moon impact "
                    + "at least once each",
            "Kasırga, tsunami, deprem, asit yağmuru, sinkhole, orman yangını, güneş patlaması ve ay çarpmasının "
                    + "her birinden en az bir kez sağ çık"),
    BIRLIKTE_HAYATTA(26, "birlikte_hayatta", BasarimTuru.HEDEF, FIRTINAYA_MEYDAN_OKU, true, null,
            "minecraft:cake", null,
            "Surviving Together", "Birlikte Hayatta",
            "Survive the same disaster together with at least 3 players",
            "En az 3 oyuncu aynı afetin etki alanında birlikte sağ çıksın"),
    YENI_SAFAK(27, "yeni_safak", BasarimTuru.GOREV, SUPERNOVA_TANIGI, false, null,
            "minecraft:clock", null,
            "New Dawn", "Yeni Şafak",
            "Live to see the old sun return at the end of the pulsar chain",
            "Pulsar zincirinin sonunda eski güneşin geri dönüşünü yaşa"),
    // ---- K-E74: gökyüzü, kütük ve tsunami dilimlerinin başarımları (OzelBasarim.ver ile verilir)
    DEVASA(28, "devasa", BasarimTuru.GOREV, SUPERNOVA_TANIGI, false, null,
            "minecraft:ender_eye", null,
            "Colossal...", "Devasa...",
            "Stare at the supermassive black hole for 5 seconds",
            "Devasa kara deliğe 5 saniye boyunca bak"),
    ASTRONOM(29, "astronom", BasarimTuru.MEYDAN_OKUMA, YILDIZLI_GECE, true, Unvan.ASTRONOM,
            "minecraft:recovery_compass", null,
            "Astronomer", "Astronom",
            "Look at every planet in the sky for at least 3 seconds each",
            "Gökyüzündeki her gezegene en az 3 saniye bak"),
    CAYIR_CAYIR(30, "cayir_cayir", BasarimTuru.HEDEF, YANMADAN_CIKTIM, false, null,
            "minecraft:charcoal", null,
            "Scorching", "Cayır Cayır",
            "Let a charred log reach its maximum heat",
            "Kömürleşmiş bir kütüğün azami ısıya ulaşmasını sağla"),
    SIFON(31, "sifon", BasarimTuru.GOREV, DALGA_AVCISI, false, null,
            "minecraft:water_bucket", null,
            "Siphon", "Sifon",
            "Seal off tsunami water flowing into a cave or hole by placing a block in it",
            "Mağaraya ya da deliğe akan tsunami suyunun yolunu blokla kapat"),
    // ---- K-E74: yaratıcı başarımlar
    DOKUNULMAZ(32, "dokunulmaz", BasarimTuru.HEDEF, FIRTINAYA_MEYDAN_OKU, false, null,
            "minecraft:shield", null,
            "Untouchable", "Dokunulmaz",
            "Survive a level 4 or stronger disaster without taking any damage",
            "Seviye 4 ya da üzeri bir afetten hiç hasar almadan sağ çık"),
    UC_UST_USTE(33, "uc_ust_uste", BasarimTuru.HEDEF, FIRTINAYA_MEYDAN_OKU, false, null,
            "minecraft:golden_apple", null,
            "Hat Trick", "Üç Üst Üste",
            "Survive 3 disasters in a row without dying",
            "Ölmeden art arda 3 afetten sağ çık"),
    ON_GUN_ON_GECE(34, "on_gun_on_gece", BasarimTuru.HEDEF, KOK, false, null,
            "minecraft:daylight_detector", null,
            "Ten Days, Ten Nights", "On Gün On Gece",
            "Stay alive for 10 Minecraft days without dying",
            "Ölmeden 10 Minecraft günü hayatta kal"),
    SALLANAN_KULE(35, "sallanan_kule", BasarimTuru.GOREV, SALLANAN_ZEMIN, false, null,
            "minecraft:scaffolding", null,
            "Shaky Tower", "Sallanan Kule",
            "Stand on high ground for 5 seconds while an earthquake is shaking",
            "Deprem sürerken yüksek bir yerde 5 saniye dur"),
    YUKSEK_ZEMIN(36, "yuksek_zemin", BasarimTuru.GOREV, DALGA_AVCISI, false, null,
            "minecraft:stone_stairs", null,
            "Higher Ground", "Yüksek Zemin",
            "Survive a tsunami from ground higher than the wave crest",
            "Bir tsunamiden dalga tepesinden yüksek zeminde durarak sağ çık"),
    GOZUN_ICINDE(37, "gozun_icinde", BasarimTuru.GOREV, ILK_RUZGAR, false, null,
            "minecraft:phantom_membrane", null,
            "Taken for a Spin", "Bir Tur Attım",
            "Get lifted up by a tornado and survive it",
            "Kasırga seni havaya kaldırsın ve onu sağ atlat"),
    GOKTEN_ZIRH(38, "gokten_zirh", BasarimTuru.GOREV, GOKTEN_GELEN_MISAFIR, false, null,
            "minecraft:netherite_chestplate", null,
            "Sky-Forged", "Gökten Dövme",
            "Upgrade an armor piece with a Meteorite Fragment or a Cosmic Core",
            "Bir zırh parçasını Meteorit Parçası ya da Kozmik Çekirdek ile güçlendir"),
    KUTUP_HAZINESI(39, "kutup_hazinesi", BasarimTuru.GOREV, BUZ_BALIKCISI, false, null,
            "dogalafetler:kristal_levrek", "minecraft:salmon",
            "Rare Catch", "Nadir Av",
            "Catch your first special polar fish with the Polar Fishing Rod",
            "Kutup Oltası ile ilk özel kutup balığını tut"),
    AURORA_AVCISI(40, "aurora_avcisi", BasarimTuru.HEDEF, KUTUP_HAZINESI, false, null,
            "dogalafetler:aurora_baligi", "minecraft:tropical_fish",
            "Aurora Hunter", "Aurora Avcısı",
            "Catch an Aurora Fish",
            "Bir Aurora Balığı tut"),
    INCE_BUZ_KIRICI(41, "ince_buz_kirici", BasarimTuru.GOREV, BUZUL_CAGI, false, null,
            "minecraft:ice", null,
            "Ice Breaker", "Buz Kırıcı",
            "Break thin ice",
            "İnce buzu kır"),
    YINELEME_IKIZ(42, "yineleme_ikiz", BasarimTuru.GOREV, BUZ_BALIKCISI, false, null,
            "minecraft:salmon", null,
            "Twin Catch", "İkiz Tutuş",
            "Reel in a doubled catch with a rod enchanted with Repeat (Yineleme)",
            "Yineleme büyülü oltayla katlanan bir tutuş çek"),
    DEFTER_ILK_ACILIS(43, "defter_ilk_acilis", BasarimTuru.GOREV, KOK, false, null,
            "minecraft:book", null,
            "First Page", "İlk Sayfa",
            "Open the Disaster Journal for the first time",
            "Afet Defteri'ni ilk kez aç"),
    KOLYE_SERIN(44, "kolye_serin", BasarimTuru.GOREV, YUNLU_KAHRAMAN, false, null,
            "dogalafetler:serin_kolye", "minecraft:heart_of_the_sea",
            "Keep Cool", "Serin Tut",
            "Wear a Cool Necklace",
            "Bir Serin Kolye tak"),
    CIFTE_BELA(45, "cifte_bela", BasarimTuru.GOREV, KOK, false, null,
            "minecraft:tnt", null,
            "Double Trouble", "Çifte Bela",
            "Stay inside the zones of two disasters at once for 30 seconds",
            "İki afetin etki alanında aynı anda 30 saniye kal");

    /** {@code OzelBasarim.ver} ile verilen başarımlar: ölçüt {@code ozel_basarim}, anahtar = {@link #yol()}. */
    private static final Set<Basarimlar> OZEL = EnumSet.of(DEVASA, ASTRONOM, CAYIR_CAYIR, SIFON, GOZUN_ICINDE,
            GOKTEN_ZIRH, INCE_BUZ_KIRICI, YINELEME_IKIZ, DEFTER_ILK_ACILIS, KOLYE_SERIN, SALLANAN_KULE, YUKSEK_ZEMIN,
            CIFTE_BELA);

    private final int no;
    private final String yol;
    private final BasarimTuru tur;
    private final Basarimlar ebeveyn;
    private final String ikon;
    private final String ikonYedegi;
    private final boolean gizli;
    private final Unvan unvan;
    private final String ingilizceAd;
    private final String turkceAd;
    private final String ingilizceAciklama;
    private final String turkceAciklama;

    Basarimlar(int no, String yol, BasarimTuru tur, Basarimlar ebeveyn, boolean gizli, Unvan unvan, String ikon,
            String ikonYedegi, String ingilizceAd, String turkceAd, String ingilizceAciklama, String turkceAciklama) {
        this.no = no;
        this.yol = yol;
        this.tur = tur;
        this.ebeveyn = ebeveyn;
        this.ikon = ikon;
        this.ikonYedegi = ikonYedegi;
        this.gizli = gizli;
        this.unvan = unvan;
        this.ingilizceAd = ingilizceAd;
        this.turkceAd = turkceAd;
        this.ingilizceAciklama = ingilizceAciklama;
        this.turkceAciklama = turkceAciklama;
    }

    /** Şartnamedeki numara (kök 0). */
    public int no() {
        return no;
    }

    /** Advancement yolunun son parçası: {@code dogalafetler:dogalafetler/<yol>}. */
    public String yol() {
        return yol;
    }

    public BasarimTuru tur() {
        return tur;
    }

    /** Sekmedeki ebeveyn; kökte null. */
    public Basarimlar ebeveyn() {
        return ebeveyn;
    }

    public String ikon() {
        return ikon;
    }

    /** Mod eşyası ikonunun vanilla yedeği; vanilla ikonda null. */
    public String ikonYedegi() {
        return ikonYedegi;
    }

    public boolean gizli() {
        return gizli;
    }

    /** Meydan okumaların verdiği ünvan; diğerlerinde null. */
    public Unvan unvan() {
        return unvan;
    }

    public String ingilizceAd() {
        return ingilizceAd;
    }

    public String turkceAd() {
        return turkceAd;
    }

    public String ingilizceAciklama() {
        return ingilizceAciklama;
    }

    public String turkceAciklama() {
        return turkceAciklama;
    }

    public String adAnahtari() {
        return "advancements.dogalafetler." + yol + ".title";
    }

    public String aciklamaAnahtari() {
        return "advancements.dogalafetler." + yol + ".description";
    }

    public static Optional<Basarimlar> numarayla(int no) {
        for (Basarimlar b : values()) {
            if (b.no == no) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    /** Advancement yolundan ({@code dogalafetler/<yol>} ya da {@code <yol>}). */
    public static Optional<Basarimlar> yoldan(String yol) {
        String ad = yol.startsWith("dogalafetler/") ? yol.substring("dogalafetler/".length()) : yol;
        for (Basarimlar b : values()) {
            if (b.yol.equals(ad)) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    /** Başarım {@code OzelBasarim.ver} ile mi veriliyor (ölçüt {@code ozel_basarim}, anahtar = {@link #yol()}). */
    public boolean ozelMi() {
        return OZEL.contains(this);
    }

    /** {@code OzelBasarim.ver} anahtarından başarım; anahtar bilinmiyorsa ya da o başarım özel değilse boş. */
    public static Optional<Basarimlar> ozelAnahtardan(String anahtar) {
        if (anahtar == null) {
            return Optional.empty();
        }
        for (Basarimlar b : OZEL) {
            if (b.yol.equals(anahtar)) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }
}
