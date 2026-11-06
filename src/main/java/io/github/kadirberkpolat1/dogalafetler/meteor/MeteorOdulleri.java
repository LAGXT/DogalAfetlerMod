package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZamani;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Meteor ödülleri (madde 13c): yalnız meteordan, yere düşen eşya olarak (adil kapış). Sıcak parçanın bitiş tick'i eşya
 * NBT'sinde {@value #SICAK_ETIKETI} (mutlak oyun tick'i, {@code getGameTime}; yeniden açılışta da geçerli, M-26): sıcakken alan
 * ya da üstüne gelen oyuncu yanar; su ya da kar/buz üstünde, ya da süre dolunca etiket silinir (soğuk parçalar envanterde
 * yığılır). Yakınlık takibine eşya dünyaya girerken alınır ({@link MeteorYoneticisi#varlikGirdi}). Ömür
 * {@code Item#getEntityLifespan} ile (M-07).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class MeteorOdulleri {
    public static final String SICAK_ETIKETI = "dogalafetler_sicak_bitis";

    private MeteorOdulleri() {
    }

    /** {@code konum}'a ödül eşyası bırakır; {@code sicakSaniye} &gt; 0 ise o kadar süre sıcaktır. */
    public static ItemEntity birak(ServerLevel level, Vec3 konum, ItemStack yigin, int sicakSaniye) {
        if (sicakSaniye > 0) {
            yigin.getOrCreateTag().putLong(SICAK_ETIKETI, OdulZamani.sicakBitis(level.getGameTime(), sicakSaniye));
        }
        ItemEntity e = new ItemEntity(level, konum.x, konum.y, konum.z, yigin, (level.random.nextDouble() - 0.5) * 0.08,
                0.25, (level.random.nextDouble() - 0.5) * 0.08);
        e.setPickUpDelay(20);
        level.addFreshEntity(e);
        return e;
    }

    public static long sicakBitis(ItemStack yigin) {
        CompoundTag t = yigin.getTag();
        return t != null && t.contains(SICAK_ETIKETI, Tag.TAG_LONG) ? t.getLong(SICAK_ETIKETI) : OdulZamani.SOGUK;
    }

    public static boolean sicakMi(ItemStack yigin, long simdi) {
        return OdulZamani.sicakMi(simdi, sicakBitis(yigin));
    }

    static void sogut(ItemStack yigin) {
        yigin.removeTagKey(SICAK_ETIKETI);
        if (yigin.getTag() != null && yigin.getTag().isEmpty()) {
            yigin.setTag(null);
        }
    }

    private static void sogut(ItemEntity e) {
        ItemStack kopya = e.getItem().copy();
        sogut(kopya);
        e.setItem(kopya);
    }

    @SubscribeEvent
    public static void alindi(EntityItemPickupEvent olay) {
        ItemStack yigin = olay.getItem().getItem();
        long bitis = sicakBitis(yigin);
        if (bitis == OdulZamani.SOGUK) {
            return;
        }
        Player oyuncu = olay.getEntity();
        sogut(yigin);
        if (OdulZamani.sicakMi(oyuncu.level().getGameTime(), bitis) && !oyuncu.isSpectator()) {
            oyuncu.setSecondsOnFire(MeteorAyarlari.yanmaSaniye());
        }
    }

    /**
     * Sıcak parçalar, her tick: süre doldu mu, su/kar/buz üstünde mi (M-31: suya değme iki tick sürüp sekebilir; seyrek
     * örnekleme kaçırır). {@code yakinlik} ise yanındaki oyuncular da yakılır (5 tick'te bir).
     */
    static void sicaklariIsle(ServerLevel level, List<ItemEntity> sicaklar, boolean yakinlik) {
        long simdi = level.getGameTime();
        for (Iterator<ItemEntity> it = sicaklar.iterator(); it.hasNext();) {
            ItemEntity e = it.next();
            if (!e.isAlive() || e.level() != level || sicakBitis(e.getItem()) == OdulZamani.SOGUK) {
                it.remove();
                continue;
            }
            if (!sicakMi(e.getItem(), simdi)) {
                sogut(e);
                it.remove();
            } else if (sogutucuUstunde(level, e)) {
                sogut(e);
                it.remove();
                level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f,
                        1.6f);
                level.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.2, e.getZ(), 8, 0.15, 0.1, 0.15, 0.02);
            } else if (yakinlik) {
                for (Player oyuncu : level.getEntitiesOfClass(Player.class, e.getBoundingBox().inflate(0.6))) {
                    if (!oyuncu.isSpectator()) {
                        oyuncu.setSecondsOnFire(MeteorAyarlari.yanmaSaniye());
                    }
                }
            }
        }
    }

    private static boolean sogutucuUstunde(ServerLevel level, ItemEntity e) {
        if (e.isInWater()) {
            return true;
        }
        BlockPos p = e.blockPosition();
        BlockState burada = level.getBlockState(p);
        BlockState alti = level.getBlockState(p.below());
        return burada.is(BlockTags.SNOW) || alti.is(BlockTags.SNOW) || alti.is(BlockTags.ICE);
    }
}
