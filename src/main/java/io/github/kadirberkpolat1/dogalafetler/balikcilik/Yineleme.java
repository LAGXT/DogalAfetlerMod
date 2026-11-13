package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.YinelemeSansi;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import io.github.kadirberkpolat1.dogalafetler.basarim.OzelBasarim;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.player.ItemFishedEvent;

/**
 * Yineleme (EN Duplicate): olta hazine büyüsü, 3 seviye. Olta ile (buz deliğinde ya da açık denizde, her olta) tutulan her
 * yiğın I %2, II %6, III %10 şansla ikiye katlanır ({@link YinelemeSansi}). Yalnız olta çekilirken ({@code ItemFishedEvent})
 * çalışır; meteor ödülü ve diğer loot yolları etkilenmez. Forge olayının {@code getDrops()} listesi kopyadır (vanilla eşyaları
 * ayrı listeden doğurur), bu yüzden kopya yiğın vanilla'nın fırlatma formülüyle ayrı bir eşya varlığı olarak doğar;
 * tek bir noktadan çağrıldığı için çift sayım olmaz. Büyülü kitap yalnız hazine havuzlarından gelir ({@code treasure}):
 * büyü masasında ve köylü ticaretinde çıkmaz.
 */
public final class Yineleme extends Enchantment {
    public Yineleme() {
        super(Rarity.VERY_RARE, EnchantmentCategory.FISHING_ROD, new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }

    @Override
    public int getMaxLevel() {
        return YinelemeSansi.EN_COK_SEVIYE;
    }

    @Override
    public int getMinCost(int seviye) {
        return 15 + (seviye - 1) * 9;
    }

    @Override
    public int getMaxCost(int seviye) {
        return getMinCost(seviye) + 50;
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    /**
     * Vanilla çekme ({@code FishingHook.retrieve}) olayından sonra: elde tutulan olta Yineleme taşıyorsa her yiğın şansla
     * kopyalanır. {@link BuzBalikciligi#balikTutuldu} çağırır; buz deliğinde önce çekilen delik işaretlenir, böylece kopya da deliğin üstüne alınır.
     */
    static void kopyala(ItemFishedEvent olay) {
        if (!(olay.getEntity() instanceof ServerPlayer oyuncu)) {
            return;
        }
        int seviye = EnchantmentHelper.getTagEnchantmentLevel(BalikcilikKayitlari.YINELEME.get(),
                KutupOltasi.tutulanOlta(oyuncu));
        if (seviye <= 0) {
            return;
        }
        FishingHook olta = olay.getHookEntity();
        RandomSource rastgele = olta.level().random;
        boolean kopyalandi = false;
        for (ItemStack yigin : olay.getDrops()) {
            if (!yigin.isEmpty() && YinelemeSansi.kopyalanir(seviye, rastgele.nextDouble())) {
                firlat(olta, oyuncu, yigin.copy());
                kopyalandi = true;
            }
        }
        if (kopyalandi && olta.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT, olta.getX(), olta.getY() + 0.3, olta.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
            OzelBasarim.ver(oyuncu, OzelBasarim.YINELEME_IKIZ);
        }
    }

    /** Vanilla'nın çekilen eşyayı oyuncuya fırlatma formülü ({@code FishingHook.retrieve}). */
    private static void firlat(FishingHook olta, ServerPlayer oyuncu, ItemStack kopya) {
        ItemEntity esya = new ItemEntity(olta.level(), olta.getX(), olta.getY(), olta.getZ(), kopya);
        double dx = oyuncu.getX() - olta.getX();
        double dy = oyuncu.getY() - olta.getY();
        double dz = oyuncu.getZ() - olta.getZ();
        esya.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
        olta.level().addFreshEntity(esya);
    }
}
