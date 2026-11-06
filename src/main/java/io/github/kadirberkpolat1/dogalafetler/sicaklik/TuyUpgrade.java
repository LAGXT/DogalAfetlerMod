package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tüy Upgrade'i (madde 7e, N2): ana elde tüy, yardımcı elde zırh parçasıyken sağ tık (havaya ya da bir bloğa) zırhın
 * {@code dogalafetler.tuy} sayacını bir artırır ve bir tüy harcar (yaratıcı modda harcanmaz). Üst sınır config'te
 * ({@code tuyUstSiniri}, varsayılan 30). İki tarafta da olay iptal edilir: yardımcı eldeki zırh giyilmez, blok kullanılmaz;
 * değişiklik yalnız sunucuda yapılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class TuyUpgrade {
    /** Uygulama sonucu. */
    public enum Sonuc {
        /** Koşul yok (elde tüy ya da yardımcı elde zırh değil). */
        UYGUN_DEGIL,
        /** Üst sınırda: tüy harcanmadı. */
        SINIRDA,
        UYGULANDI
    }

    private TuyUpgrade() {
    }

    @SubscribeEvent
    public static void esyaKullanildi(PlayerInteractEvent.RightClickItem olay) {
        if (olay.getHand() == InteractionHand.MAIN_HAND && isle(olay.getEntity())) {
            olay.setCanceled(true);
            olay.setCancellationResult(InteractionResult.sidedSuccess(olay.getLevel().isClientSide()));
        }
    }

    @SubscribeEvent
    public static void blokKullanildi(PlayerInteractEvent.RightClickBlock olay) {
        if (olay.getHand() == InteractionHand.MAIN_HAND && isle(olay.getEntity())) {
            olay.setCanceled(true);
            olay.setCancellationResult(InteractionResult.sidedSuccess(olay.getLevel().isClientSide()));
        }
    }

    /** Koşul tutuyorsa true (olay iptal edilir); sunucuda upgrade'i uygular. */
    private static boolean isle(Player oyuncu) {
        ItemStack tuy = oyuncu.getMainHandItem();
        ItemStack zirh = oyuncu.getOffhandItem();
        if (!tuy.is(Items.FEATHER) || !ZirhEkleri.zirhMi(zirh)) {
            return false;
        }
        if (!oyuncu.level().isClientSide()) {
            uygula(oyuncu, tuy, zirh, SicaklikAyarlari.tuyUstSiniri());
        }
        return true;
    }

    /** Upgrade'i uygular: sayacı artırır, bir tüy harcar, ses ve parçacık (GameTest doğrudan çağırır). */
    public static Sonuc uygula(Player oyuncu, ItemStack tuy, ItemStack zirh, int ustSinir) {
        if (!tuy.is(Items.FEATHER) || !ZirhEkleri.zirhMi(zirh)) {
            return Sonuc.UYGUN_DEGIL;
        }
        int sayi = ZirhEkleri.tuySayisi(zirh);
        if (sayi >= ustSinir) {
            oyuncu.level().playSound(null, oyuncu.blockPosition(), SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                    SoundSource.PLAYERS, 0.5f, 0.6f);
            return Sonuc.SINIRDA;
        }
        ZirhEkleri.tuyAyarla(zirh, sayi + 1);
        if (!oyuncu.getAbilities().instabuild) {
            tuy.shrink(1);
        }
        oyuncu.level().playSound(null, oyuncu.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS,
                0.8f, 1.2f + oyuncu.getRandom().nextFloat() * 0.2f);
        if (oyuncu.level() instanceof ServerLevel level) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.FEATHER)),
                    oyuncu.getX(), oyuncu.getY() + 1.1, oyuncu.getZ(), 8, 0.25, 0.25, 0.25, 0.05);
        }
        return Sonuc.UYGULANDI;
    }
}
