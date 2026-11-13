package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTablosu;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Yon;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Başarımların kimlikleri. Karşılaştırılan mod eşyaları (çekirdek dışındaki Faz 3 işçilerinin) yalnız
 * {@link ResourceLocation} ile anılır: sınıflarına bağlanmayız, eşya kayıtlı değilse ölçüt yalnız tutmaz.
 */
public final class BasarimKimlikleri {
    /** Madde 13c (meteor işçisi): #7 "Gökten Gelen Misafir". */
    public static final ResourceLocation METEORIT_PARCASI = DogalAfetler.konum("meteorit_parcasi");
    /** Madde 13c (meteor işçisi): #22 "Kozmik Şans". */
    public static final ResourceLocation KOZMIK_CEKIRDEK = DogalAfetler.konum("kozmik_cekirdek");
    /** Madde 13a (radar-sismograf işçisi): #16 "Radar Altında". */
    public static final ResourceLocation HAVA_RADARI = DogalAfetler.konum("hava_radari");
    /** Madde 13b (radar-sismograf işçisi): #15 "Yer Dinleyici". */
    public static final ResourceLocation SISMOGRAF = DogalAfetler.konum("sismograf");
    /** K-E74 "Nadir Av": Kutup Oltası'yla tutulan üç özel kutup balığının çiğ halleri ({@code BalikcilikKayitlari.OzelBalik}). */
    public static final ResourceLocation KRISTAL_LEVREK = DogalAfetler.konum("kristal_levrek");
    public static final ResourceLocation KUTUP_YILANBALIGI = DogalAfetler.konum("kutup_yilanbaligi");
    /** K-E74 "Aurora Avcısı": en nadir özel balık. */
    public static final ResourceLocation AURORA_BALIGI = DogalAfetler.konum("aurora_baligi");
    /** Madde 14 (yiyecek işçisi): "hedef" ödülünün sıcak yiyeceği. */
    public static final ResourceLocation SICAK_SALEP = DogalAfetler.konum("sicak_salep");
    /**
     * İçten ısıtan bütün mod yiyecek ve içecekleri (madde 14 ve sıcaklık-3, tablo sırasıyla); #19 için isteğe bağlı etiket
     * girdileri. Tablodan türetilir: yeni sıcak yiyecek kendiliğinden tanınır.
     */
    public static final List<ResourceLocation> SICAK_YIYECEKLER = YiyecekTablosu.KAYITLAR.stream()
            .filter(k -> k.tanim().yon() == Yon.ISINMA).map(k -> DogalAfetler.konum(k.ad())).toList();
    /** #19: tanınan sıcak yiyecekler (veri paketiyle genişler, N12). */
    public static final TagKey<Item> SICAK_YIYECEK = ItemTags.create(DogalAfetler.konum("basarim/sicak_yiyecek"));
    /** "Hedef" başarımlarının ödülü: 1–2 sıcak yiyecek (madde 12). */
    public static final ResourceLocation HEDEF_ODULU = DogalAfetler.konum("rewards/hedef");
    /** Sekme arka planı (16×16 döşeme, {@code tools/assets/basarim_ikon.py}). */
    public static final ResourceLocation ARKA_PLAN =
            DogalAfetler.konum("textures/gui/advancements/backgrounds/felaket.png");

    private BasarimKimlikleri() {
    }

    /** Advancement kimliği: {@code dogalafetler:dogalafetler/<yol>}. */
    public static ResourceLocation basarim(Basarimlar basarim) {
        return DogalAfetler.konum("dogalafetler/" + basarim.yol());
    }

    /** Yığının eşya kimliği (boş yığında {@code minecraft:air}). */
    public static ResourceLocation esya(ItemStack yigin) {
        ResourceLocation kimlik = ForgeRegistries.ITEMS.getKey(yigin.getItem());
        return kimlik == null ? ForgeRegistries.ITEMS.getKey(Items.AIR) : kimlik;
    }

    /** {@code kimlik} kayıtlıysa o eşya, değilse {@code yedek}. */
    public static Item esyaYaDaYedek(ResourceLocation kimlik, Item yedek) {
        return ForgeRegistries.ITEMS.containsKey(kimlik) ? ForgeRegistries.ITEMS.getValue(kimlik) : yedek;
    }
}
