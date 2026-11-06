package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import java.util.Optional;

/**
 * "Doğal Afetler" başarım sekmesi: kök "Felaket Başlıyor" ve madde 12 listesindeki 26 başarım, şartnamedeki numara, tür
 * ve gizlilikle birebir. Advancement kimliği {@code dogalafetler:dogalafetler/<yol>}. Datagen (JSON, dil dosyaları) ve
 * testler bu listeyi okur. Ebeveyn her zaman önce tanımlıdır; ağaç sekmenin yerleşimidir, ön koşul değildir
 * (vanilla: çocuk ebeveynden önce kazanılabilir). Mod eşyası ikonları ({@code dogalafetler:}) datagen anında kayıtlı
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
            "En az 3 oyuncu aynı afetin etki alanında birlikte sağ çıksın");

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
}
