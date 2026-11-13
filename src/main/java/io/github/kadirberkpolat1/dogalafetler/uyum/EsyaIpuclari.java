package io.github.kadirberkpolat1.dogalafetler.uyum;

import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Tarifle üretilemeyen eşyaların "nasıl ve nerede elde edilir" bilgisi (K-E74): JEI bilgi sayfasının ({@code client.uyum.JeiUyumu})
 * metinleri. Ortak koddur (datagen aynı tabloyu okur); her eşyanın iki satırı vardır, {@code .nasil} ve {@code .nerede}.
 * Tabloda olmayan ve tarifi de olmayan bir mod eşyası için {@link #genel()} metni gösterilir ve günlüğe yazılır; yeni
 * bir tarifsiz eşya eklenince buraya bir satır eklenmelidir.
 *
 * <p>Metinler olasılık yüzdesi vermez (ayarları ve afet seviyeleri değişebilir); yalnız kuralı ve yeri söyler.
 */
public final class EsyaIpuclari {
    private static final String ONEK = "jei.dogalafetler.bilgi.";
    /** Satır başlıkları ("Nasıl:", "Nerede:"). */
    public static final String ETIKET_NASIL = "jei.dogalafetler.etiket.nasil";
    public static final String ETIKET_NEREDE = "jei.dogalafetler.etiket.nerede";
    private static final String GENEL = "genel";

    private record Girdi(String nasilEn, String nasilTr, String neredeEn, String neredeTr) {
    }

    private static final Map<String, Girdi> TABLO = new LinkedHashMap<>();

    private static final String DELIK_EN = "Open an ice hole while the world freeze is on: sneak and right-click surface ice with a pickaxe or an axe, "
            + "then cast a fishing rod into the hole.";
    private static final String DELIK_TR = "Dünya donması sürerken bir buz deliği aç: su üstündeki buza eğilerek kazma ya da baltayla sağ tıkla, "
            + "sonra oltayı deliğe at.";

    static {
        ekle("buz_parcasi",
                "Junk that sometimes comes up instead of a fish when ice fishing. Four Ice Shards craft into a block of Ice.",
                "Buz deliğinde balık tutarken balık yerine ara sıra çıkan çöp eşya. Dört Buz Parçası bir Buz bloğu yapar.",
                "Ice hole fishing with any rod. " + DELIK_EN,
                "Buz deliğinde herhangi bir olta ile. " + DELIK_TR);
        ekle("buzlu_inci",
                "Very rare fishing treasure; the centre of the Cool Necklace recipe.",
                "Çok nadir bir balıkçılık hazinesi; Serin Kolye tarifinin merkezi.",
                "Fish in an ice hole with any rod, or at open sea (rarely). " + DELIK_EN,
                "Buz deliğinde herhangi bir olta ile, ya da açık denizde (nadiren). " + DELIK_TR);
        ekle("tuz_kristali",
                "A fishing find that goes into the Cold Potion.",
                "Balıkçılıkta bulunan, Soğuk İksiri'ne giren bir malzeme.",
                "Fish in an ice hole with any rod, or at open sea (sometimes). " + DELIK_EN,
                "Buz deliğinde herhangi bir olta ile, ya da açık denizde (ara sıra). " + DELIK_TR);
        ekle("sogutma_parcasi",
                "The most common fishing find. Three Cooling Fragments and an iron ingot craft a Cooling Ingot (Cooling Plate, Cool Necklace, Polar Fishing Rod); one also goes into the Cold Potion.",
                "Balıkçılıkta en sık bulunan eşya. Üç Soğutma Parçası ve bir demir külçe bir Soğutma Külçesi yapar (Soğutma Plakası, Serin Kolye, Kutup Oltası); biri de Soğuk İksiri'ne girer.",
                "Fish in an ice hole with any rod, or at open sea (sometimes). " + DELIK_EN,
                "Buz deliğinde herhangi bir olta ile, ya da açık denizde (ara sıra). " + DELIK_TR);

        ekle("buz_alabaligi",
                "One of the two common ice fish. Cook it in a furnace, smoker or campfire.",
                "İki yaygın buz balığından biri. Fırında, tütsüleme fırınında ya da kamp ateşinde pişir.",
                "Ice hole fishing with any rod. " + DELIK_EN,
                "Buz deliğinde herhangi bir olta ile. " + DELIK_TR);
        ekle("kutup_morinasi",
                "One of the two common ice fish. Cook it in a furnace, smoker or campfire.",
                "İki yaygın buz balığından biri. Fırında, tütsüleme fırınında ya da kamp ateşinde pişir.",
                "Ice hole fishing with any rod. " + DELIK_EN,
                "Buz deliğinde herhangi bir olta ile. " + DELIK_TR);
        ekle("derin_soguk_somonu",
                "An uncommon ice fish, more filling than the common ones when cooked.",
                "Yaygın olanlardan seyrek bir buz balığı; pişince daha doyurucudur.",
                "Ice hole fishing with any rod. " + DELIK_EN,
                "Buz deliğinde herhangi bir olta ile. " + DELIK_TR);
        ekle("buz_kefali",
                "An uncommon ice fish, more filling than the common ones when cooked.",
                "Yaygın olanlardan seyrek bir buz balığı; pişince daha doyurucudur.",
                "Ice hole fishing with any rod. " + DELIK_EN,
                "Buz deliğinde herhangi bir olta ile. " + DELIK_TR);
        ekle("donmus_ringa",
                "The rarest ordinary ice fish and the most filling one when cooked.",
                "Sıradan buz balıklarının en nadiri; pişince en doyurucusu.",
                "Ice hole fishing with any rod. " + DELIK_EN,
                "Buz deliğinde herhangi bir olta ile. " + DELIK_TR);

        ekle("kristal_levrek",
                "A polar fish only the Polar Fishing Rod can hook. The most common of the three.",
                "Yalnız Kutup Oltası'nın tutabildiği bir kutup balığı. Üçünün en yaygını.",
                "With a Polar Fishing Rod, in an ice hole or at open sea. " + DELIK_EN,
                "Kutup Oltası ile, buz deliğinde ya da açık denizde. " + DELIK_TR);
        ekle("kutup_yilanbaligi",
                "A polar fish only the Polar Fishing Rod can hook. Uncommon.",
                "Yalnız Kutup Oltası'nın tutabildiği bir kutup balığı. Seyrek.",
                "With a Polar Fishing Rod, in an ice hole or at open sea. " + DELIK_EN,
                "Kutup Oltası ile, buz deliğinde ya da açık denizde. " + DELIK_TR);
        ekle("aurora_baligi",
                "A polar fish only the Polar Fishing Rod can hook. The rarest and most filling one.",
                "Yalnız Kutup Oltası'nın tutabildiği bir kutup balığı. En nadir ve en doyurucusu.",
                "With a Polar Fishing Rod, in an ice hole or at open sea. " + DELIK_EN,
                "Kutup Oltası ile, buz deliğinde ya da açık denizde. " + DELIK_TR);

        ekle("meteorit_parcasi",
                "Dropped by meteor impacts; large meteors drop it more often and in larger stacks. It is HOT right after the "
                        + "impact: cool it in water, snow or ice before picking it up, or you catch fire.",
                "Meteor çarpmalarında düşer; büyük meteorlarda daha sık ve daha bol düşer. Çarpma anında SİCAKTIR: almadan önce "
                        + "su, kar ya da buzla soğutman gerekir, yoksa yanırsın.",
                "On the floor of a fresh meteor crater, near the impact point. Meteors fall as a disaster or a random sky event.",
                "Yeni bir meteor kraterinin tabanında, çarpma noktasının yakınında. Meteorlar bir afet ya da rastgele gök olayı olarak düşer.");
        ekle("kozmik_cekirdek",
                "The rarest meteor reward: one core at most per impact, and only from a large meteor.",
                "En nadir meteor ödülü: çarpma başına en fazla bir tane ve yalnız büyük meteordan.",
                "At the very centre of a large meteor crater.",
                "Büyük bir meteor kraterinin tam merkezinde.");

        ekle("komurlesmis_kutuk",
                "Forms when the Pulsar era scorches wood: logs inside the scorched area turn into Charred Logs. Lava buckets "
                        + "raise its heat, snow cools it.",
                "Pulsar dönemi odunu kavururken oluşur: kavrulan alandaki kütükler Kömürleşmiş Kütük olur. Lav kovası ısısını artırır, "
                        + "kar söndürür.",
                "Forests inside the area scorched by a Pulsar era.",
                "Pulsar dönemi tarafından kavrulan alanlardaki ağaçlar.");
        ekle("soyulmus_komurlesmis_kutuk",
                "Strip a Charred Log with an axe. The stripped log gives off no heat or light.",
                "Kömürleşmiş Kütüğü baltayla soy. Soyulmuş kütük ısı ve ışık yaymaz.",
                "Charred Logs found in areas scorched by a Pulsar era.",
                "Pulsar dönemi tarafından kavrulan alanlardaki Kömürleşmiş Kütükler.");
        ekle("kul_katmani",
                "Ash piles up in layers (1-8) like snow. Eight layers make a Block of Ash.",
                "Kül kar gibi kat kat (1-8) birikir. Sekiz kat bir Kül Bloğu eder.",
                "On the ground in areas scorched by a Pulsar era; fallen leaves turn into ash.",
                "Pulsar dönemi tarafından kavrulan alanlarda yerde; yapraklar düşerken küle dönüşür.");
        ekle("kul_blogu",
                "Scorched dirt turns into a Block of Ash; eight Ash Layers also craft one. Dig it with a shovel.",
                "Kavrulan toprak Kül Bloğu olur; sekiz Kül Katmanı da bir tane yapar. Kürekle kazılır.",
                "On the ground in areas scorched by a Pulsar era.",
                "Pulsar dönemi tarafından kavrulan alanlarda yerde.");

        ekle("ince_buz",
                "Weak, dark ice that forms on top of water as the world freezes and again as it thaws. It melts in light and can be walked on.",
                "Dünya donarken ve çözülürken suyun üzerinde oluşan zayıf, koyu buz. Işıkta erir, üzerinde yürünebilir.",
                "On lakes, rivers and seas while the world freeze starts or ends.",
                "Dünya donması başlarken ya da biterken göl, nehir ve denizlerde.");
        ekle("kiragili_cimen",
                "Frosted grass of the world freeze. The default freeze table no longer makes it (grass gets a snow cover instead); "
                        + "it only survives in old worlds or when a data pack adds it to the freeze table.",
                "Dünya donmasının kırağılı çimeni. Varsayılan donma tablosu artık üretmez (çimen kar örtüsü alır); yalnız eski "
                        + "dünyalarda ya da bir veri paketi donma tablosuna eklediğinde kalır.",
                "Old worlds, or worlds whose data pack still maps grass to it.",
                "Eski dünyalarda ya da veri paketi çimeni hâlâ buna çevirenlerde.");

        ekle(GENEL,
                "This item has no crafting recipe; it comes from disasters, world events or fishing.",
                "Bu eşyanın üretim tarifi yok; afetlerden, dünya olaylarından ya da balıkçılıktan elde edilir.",
                "Found in the world after a Natural Disasters event or while fishing.",
                "Bir Doğal Afetler olayından sonra dünyada ya da balık tutarken bulunur.");
    }

    private EsyaIpuclari() {
    }

    private static void ekle(String esya, String nasilEn, String nasilTr, String neredeEn, String neredeTr) {
        if (TABLO.put(esya, new Girdi(nasilEn, nasilTr, neredeEn, neredeTr)) != null) {
            throw new IllegalStateException("eşya ipucu iki kez tanımlandı: " + esya);
        }
    }

    /** İpucu tablosundaki eşya yolları ({@code dogalafetler:<yol>}); genel metin dışında. */
    public static Set<String> esyalar() {
        Set<String> yollar = new LinkedHashSet<>(TABLO.keySet());
        yollar.remove(GENEL);
        return yollar;
    }

    public static boolean var(String esyaYolu) {
        return !GENEL.equals(esyaYolu) && TABLO.containsKey(esyaYolu);
    }

    /** {@code esyaYolu} için bilgi sayfası satırları (Nasıl, Nerede); tabloda yoksa ({@link #genel()}). */
    public static Component[] metin(String esyaYolu) {
        return var(esyaYolu) ? satirlar(esyaYolu) : genel();
    }

    /** Tarifsiz ama tabloda olmayan eşyalar için genel sayfa. */
    public static Component[] genel() {
        return satirlar(GENEL);
    }

    private static Component[] satirlar(String anahtar) {
        return new Component[] {
                Component.translatable(ETIKET_NASIL).withStyle(ChatFormatting.BOLD)
                        .append(Component.translatable(ONEK + anahtar + ".nasil")),
                Component.translatable(ETIKET_NEREDE).withStyle(ChatFormatting.BOLD)
                        .append(Component.translatable(ONEK + anahtar + ".nerede"))};
    }

    static void cevirileriYaz(CeviriYazici yazici) {
        yazici.ekle(ETIKET_NASIL, "How: ", "Nasıl: ");
        yazici.ekle(ETIKET_NEREDE, "Where: ", "Nerede: ");
        TABLO.forEach((esya, g) -> {
            yazici.ekle(ONEK + esya + ".nasil", g.nasilEn(), g.nasilTr());
            yazici.ekle(ONEK + esya + ".nerede", g.neredeEn(), g.neredeTr());
        });
    }
}
