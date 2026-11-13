package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.ZirhEkleri;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Meteor eşyalarının zırh yükseltmeleri (Termostatlı zırhla aynı desen: {@code dogalafetler:zirh_eki} şekilsiz tarifi, işlenmiş
 * tezgahta zırh + malzeme; eşyaların tarifi {@code MeteorVerileri}'nde). İki ek vardır, hepsi parça başına toplanır:
 * <ul>
 * <li>{@link #METEORITLI} (Meteorit Parçası): +{@value #METEORITLI_ZIRH} zırh, +{@value #METEORITLI_SERTLIK} zırh sertliği,
 * +{@value #METEORITLI_ITME} geri tepme direnci; yanık/lav hasarı parça başına %8 (en çok %32) azalır.</li>
 * <li>{@link #KOZMIK} (Kozmik Çekirdek): +{@value #KOZMIK_ZIRH} zırh, +{@value #KOZMIK_SERTLIK} zırh sertliği,
 * +{@value #KOZMIK_ITME} geri tepme direnci; ateş hasarı parça başına %10 (çok %40), patlama (meteor çarpması dahil)
 * %12 (çok %48) ve düşme hasarı %12 (çok %48) azalır; giyen oyuncunun çevresinde hafif bir parlama (END_ROD) belirir.</li>
 * </ul>
 * Zırh özniteliği, ekin yalnız parçanın kendi yuvasında geçerlidir ({@link ItemAttributeModifierEvent}); hasar azalması
 * giyilen parçaların ekini sayar ({@link LivingHurtEvent}; zırh ve büyü azaltmasından önce). Sıcaklık yalıtımına katkısı yoktur.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class MeteorZirhi {
    public static final ZirhEkleri.Ek METEORITLI = new ZirhEkleri.Ek("meteoritli", "tooltip.dogalafetler.meteoritli");
    public static final ZirhEkleri.Ek KOZMIK = new ZirhEkleri.Ek("kozmik", "tooltip.dogalafetler.kozmik_zirh");

    static final double METEORITLI_ZIRH = 1.0;
    static final double METEORITLI_SERTLIK = 0.5;
    static final double METEORITLI_ITME = 0.05;
    static final double KOZMIK_ZIRH = 2.0;
    static final double KOZMIK_SERTLIK = 1.0;
    static final double KOZMIK_ITME = 0.1;
    /** Parça başına hasar azalması ve en yüksek toplam azalma (ek, hasar türü). */
    static final double METEORITLI_ATES = 0.08;
    static final double METEORITLI_ATES_TAVAN = 0.32;
    static final double KOZMIK_ATES = 0.10;
    static final double KOZMIK_ATES_TAVAN = 0.40;
    static final double KOZMIK_PATLAMA = 0.12;
    static final double KOZMIK_DUSME = 0.12;
    static final double KOZMIK_TAVAN = 0.48;
    private static final int PARLAMA_ARALIGI = 10;
    private static final String ACIKLAMA_PARCA = "tooltip.dogalafetler.meteorit_parcasi";
    private static final String ACIKLAMA_CEKIRDEK = "tooltip.dogalafetler.kozmik_cekirdek_zirh";

    private MeteorZirhi() {
    }

    /** Mod kurucusunda bir kez (Meteor.kaydet). */
    static void kaydet() {
        ZirhEkleri.kaydet(METEORITLI);
        ZirhEkleri.kaydet(KOZMIK);
        DataGenerators.ceviriEkle(MeteorZirhi::cevirileriYaz);
    }

    /** Eşya ipucu satırlarının anahtarları (eşya sınıfları kullanır). */
    static String parcaAciklamasi() {
        return ACIKLAMA_PARCA;
    }

    static String cekirdekZirhAciklamasi() {
        return ACIKLAMA_CEKIRDEK;
    }

    static void cevirileriYaz(CeviriYazici y) {
        y.ekle(METEORITLI.ceviriAnahtari(), "Meteoritic (+armor, fire resistance)",
                "Meteoritli (+zırh, ateş direnci)");
        y.ekle(KOZMIK.ceviriAnahtari(), "Cosmic (+armor, fire/blast/fall resistance, glow)",
                "Kozmik (+zırh, ateş/patlama/düşme direnci, parlama)");
        y.ekle(ACIKLAMA_PARCA, "Craft with an armor piece: strengthens it (armor, toughness, fire resistance)",
                "Bir zırh parçasıyla birleştir: zırhı güçlendirir (zırh, sertlik, ateş direnci)");
        y.ekle(ACIKLAMA_CEKIRDEK, "Craft with an armor piece: cosmic strength (armor, fire/blast/fall resistance, glow)",
                "Bir zırh parçasıyla birleştir: kozmik güç (zırh, ateş/patlama/düşme direnci, parlama)");
    }

    /** Giyilen dört zırh yuvasında {@code ek} taşıyan parça sayısı. */
    static int parcaSayisi(LivingEntity varlik, ZirhEkleri.Ek ek) {
        int sayi = 0;
        for (EquipmentSlot yuva : EquipmentSlot.values()) {
            if (yuva.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack yigin = varlik.getItemBySlot(yuva);
                if (ZirhEkleri.zirhMi(yigin) && ZirhEkleri.varMi(yigin, ek)) {
                    sayi++;
                }
            }
        }
        return sayi;
    }

    /** {@code parca} parçanın toplam azaltması (parça başına {@code oran}, en çok {@code tavan}) uygulanmış hasar. */
    static float azalt(float miktar, int parca, double oran, double tavan) {
        return (float) (miktar * (1 - Math.min(tavan, parca * oran)));
    }

    @SubscribeEvent
    public static void oznitelikler(ItemAttributeModifierEvent olay) {
        ItemStack yigin = olay.getItemStack();
        if (!(yigin.getItem() instanceof ArmorItem zirh) || olay.getSlotType() != zirh.getEquipmentSlot()) {
            return;
        }
        if (ZirhEkleri.varMi(yigin, METEORITLI)) {
            ekle(olay, METEORITLI, Attributes.ARMOR, METEORITLI_ZIRH);
            ekle(olay, METEORITLI, Attributes.ARMOR_TOUGHNESS, METEORITLI_SERTLIK);
            ekle(olay, METEORITLI, Attributes.KNOCKBACK_RESISTANCE, METEORITLI_ITME);
        }
        if (ZirhEkleri.varMi(yigin, KOZMIK)) {
            ekle(olay, KOZMIK, Attributes.ARMOR, KOZMIK_ZIRH);
            ekle(olay, KOZMIK, Attributes.ARMOR_TOUGHNESS, KOZMIK_SERTLIK);
            ekle(olay, KOZMIK, Attributes.KNOCKBACK_RESISTANCE, KOZMIK_ITME);
        }
    }

    private static void ekle(ItemAttributeModifierEvent olay, ZirhEkleri.Ek ek, Attribute oznitelik, double deger) {
        String ad = DogalAfetler.MOD_ID + ":" + ek.anahtar() + ":" + olay.getSlotType().getName() + ":"
                + oznitelik.getDescriptionId();
        olay.addModifier(oznitelik, new AttributeModifier(UUID.nameUUIDFromBytes(ad.getBytes(StandardCharsets.UTF_8)),
                ad, deger, AttributeModifier.Operation.ADDITION));
    }

    @SubscribeEvent
    public static void hasar(LivingHurtEvent olay) {
        LivingEntity varlik = olay.getEntity();
        DamageSource kaynak = olay.getSource();
        boolean ates = kaynak.is(DamageTypeTags.IS_FIRE);
        boolean patlama = kaynak.is(DamageTypeTags.IS_EXPLOSION);
        boolean dusme = kaynak.is(DamageTypeTags.IS_FALL);
        if (!ates && !patlama && !dusme) {
            return;
        }
        float miktar = olay.getAmount();
        if (ates) {
            miktar = azalt(miktar, parcaSayisi(varlik, METEORITLI), METEORITLI_ATES, METEORITLI_ATES_TAVAN);
            miktar = azalt(miktar, parcaSayisi(varlik, KOZMIK), KOZMIK_ATES, KOZMIK_ATES_TAVAN);
        }
        if (patlama) {
            miktar = azalt(miktar, parcaSayisi(varlik, KOZMIK), KOZMIK_PATLAMA, KOZMIK_TAVAN);
        }
        if (dusme) {
            miktar = azalt(miktar, parcaSayisi(varlik, KOZMIK), KOZMIK_DUSME, KOZMIK_TAVAN);
        }
        olay.setAmount(miktar);
    }

    /** Kozmik parça giyen oyuncunun çevresinde hafif parlama (sunucu parçacığı, parça sayısı kadar). */
    @SubscribeEvent
    public static void parlama(TickEvent.PlayerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END || !(olay.player instanceof ServerPlayer oyuncu)
                || oyuncu.tickCount % PARLAMA_ARALIGI != 0 || !oyuncu.isAlive() || oyuncu.isSpectator()) {
            return;
        }
        int parca = parcaSayisi(oyuncu, KOZMIK);
        if (parca > 0) {
            oyuncu.serverLevel().sendParticles(ParticleTypes.END_ROD, oyuncu.getX(), oyuncu.getY() + 1.0, oyuncu.getZ(), parca,
                    0.35, 0.7, 0.35, 0.01);
        }
    }
}
