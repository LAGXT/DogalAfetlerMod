package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.DelikOmru;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaBloklari;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Buz Deliği (madde 15): su üstündeki buzun kazma/baltayla açılmış hali. Blok içinde kaynak su sıvı durumu taşır
 * ({@link #getFluidState}): vanilla sıvı çizicisi deliğin ortasında biyom renkli, hareketli su yüzeyini çizer ve olta
 * içinde vanilla gibi yüzer; buz çerçeve modelden gelir. Blok bir {@code LiquidBlockContainer} değildir: su ne yayılır ne
 * kovayla alınır. Oyuncular için tam blok gibi çarpışır (üstünde yürünür, düşülmez); olta, eşya ve diğer varlıklar için
 * çarpışmasızdır (olta deliğe girer, komşu buz blokları onu deliğin sütununda tutar).
 *
 * <p>Ömür zamanlanmış blok tick'iyle işler (chunk'la kaydedilir): açılınca {@link DelikOmru#ilkGecikme}, sonra üç donma
 * aşamasının ({@link #ASAMA} 1–3, giderek kapanan buz zarı) her biri {@link DelikOmru#asamaGecikmesi}; son aşamadan sonra
 * eski buz ({@link #INCE}: donma sisteminin ince buzu ya da buz) geri gelir; global donma ilerlemesi 0 ise (sıfırlanmış
 * dünya) su kalır (B-20). Donma sisteminin sütun biti ("bizim buz") deliğe dokunulmadan kalır; donma süreci üstü buz ya
 * da su olmayan sütuna dokunmaz (donma §A).
 */
public final class BuzDeligiBlogu extends Block {
    /** 0 açık; 1–3 yeniden donma aşamaları. */
    public static final IntegerProperty ASAMA = IntegerProperty.create("asama", 0, DelikOmru.SON_ASAMA);
    /** Delik donma sisteminin ince buzunda açıldı; yeniden donunca ince buz olur. */
    public static final BooleanProperty INCE = BooleanProperty.create("ince");

    public BuzDeligiBlogu(Properties ozellikler) {
        super(ozellikler);
        registerDefaultState(stateDefinition.any().setValue(ASAMA, 0).setValue(INCE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> kurucu) {
        kurucu.add(ASAMA, INCE);
    }

    /** Deliğin yerine gelecek eski buz. */
    public static BlockState eskiBuz(BlockState delik) {
        return delik.getValue(INCE) ? DonmaBloklari.INCE_BUZ.get().defaultBlockState() : Blocks.ICE.defaultBlockState();
    }

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState durum) {
        return Fluids.WATER.getSource(false);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState durum, BlockGetter level, BlockPos konum, CollisionContext baglam) {
        return baglam instanceof EntityCollisionContext e && e.getEntity() instanceof Player ? Shapes.block() : Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getVisualShape(BlockState durum, BlockGetter level, BlockPos konum, CollisionContext baglam) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getShadeBrightness(BlockState durum, BlockGetter level, BlockPos konum) {
        return 1.0f;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState durum, BlockGetter level, BlockPos konum) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState durum, Level level, BlockPos konum, BlockState eski, boolean hareket) {
        if (!eski.is(this) && level instanceof ServerLevel sl) {
            DelikOmru.Ayarlar a = BalikcilikAyarlari.delikOmru();
            int omur = DelikOmru.omurTick(sl.random.nextDouble(), a);
            sl.scheduleTick(konum, this, durum.getValue(ASAMA) == 0 ? DelikOmru.ilkGecikme(omur, a)
                    : DelikOmru.asamaGecikmesi(a));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState durum, ServerLevel level, BlockPos konum, RandomSource rastgele) {
        int asama = durum.getValue(ASAMA);
        double x = konum.getX() + 0.5;
        double y = konum.getY() + 0.95;
        double z = konum.getZ() + 0.5;
        if (DelikOmru.buzaDonerMi(asama)) {
            if (DelikOmru.suyaDonerMi(DunyaDonemi.al(level).donmaIlerlemesi())) {
                level.setBlock(konum, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                level.playSound(null, konum, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.3f, 1.2f);
                level.sendParticles(ParticleTypes.SPLASH, x, y, z, 10, 0.25, 0.05, 0.25, 0.1);
                return;
            }
            BlockState buz = eskiBuz(durum);
            level.setBlock(konum, buz, Block.UPDATE_ALL);
            level.playSound(null, konum, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.7f, 0.6f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, buz), x, y, z, 16, 0.3, 0.05, 0.3, 0.02);
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 0.1, z, 10, 0.3, 0.1, 0.3, 0.01);
            return;
        }
        level.setBlock(konum, durum.setValue(ASAMA, asama + 1), Block.UPDATE_CLIENTS);
        level.scheduleTick(konum, this, DelikOmru.asamaGecikmesi(BalikcilikAyarlari.delikOmru()));
        level.playSound(null, konum, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.35f, 0.5f + 0.15f * asama);
        level.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 0.1, z, 4 + 3 * asama, 0.25, 0.05, 0.25, 0.005);
    }

    @Override
    public void animateTick(BlockState durum, Level level, BlockPos konum, RandomSource rastgele) {
        int asama = durum.getValue(ASAMA);
        double x = konum.getX() + 0.2 + rastgele.nextDouble() * 0.6;
        double z = konum.getZ() + 0.2 + rastgele.nextDouble() * 0.6;
        if (asama > 0 && rastgele.nextFloat() < 0.12f * asama) {
            level.addParticle(ParticleTypes.SNOWFLAKE, x, konum.getY() + 0.95, z, 0, 0.005, 0);
        } else if (asama == 0 && rastgele.nextFloat() < 0.08f) {
            level.addParticle(ParticleTypes.BUBBLE_POP, x, konum.getY() + 0.9, z, 0, 0.02, 0);
        }
    }

    /** Oyuncu deliği kırarsa yerinde su kalır (deliğin altı hep sudur; eşyası yoktur, İpek Dokunuş da bir şey vermez). */
    @Override
    public void playerDestroy(Level level, Player oyuncu, BlockPos konum, BlockState durum, @Nullable BlockEntity be,
            ItemStack alet) {
        super.playerDestroy(level, oyuncu, konum, durum, be, alet);
        if (!level.dimensionType().ultraWarm()) {
            level.setBlockAndUpdate(konum, Blocks.WATER.defaultBlockState());
        }
    }
}
