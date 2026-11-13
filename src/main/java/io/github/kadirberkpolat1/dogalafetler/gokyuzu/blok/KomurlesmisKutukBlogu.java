package io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok;

import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarKayitlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KutukIsisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.DurumaBagliIsiKaynagi;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

/**
 * Kömürleşmiş kütük: eksenli sütun (pulsar kavurması ekseni taşır), yanmaz (yanıcılık kaydı yok). İsı kademesi
 * {@link #ISI} (0…{@value KutukIsisi#MAKS}; kurallar {@link KutukIsisi}) blok durumudur, bu yüzden sunucu ile istemci
 * arasında kendiliğinden eşlenir: lav kovasıyla sağ tık kademeyi bir artırır (kova boşaltılır, yaratıcıda tüketilmez;
 * sınırda daha fazla kova kabul edilmez), kar topuyla sağ tık ya da üzerine atılan kar topu söndürür (0: ısı ve ışık
 * yok). Kademe ışık seviyesini, config'teki ısı kaynağı gücünün ve yarıçapının çarpanını ({@link DurumaBagliIsiKaynagi}),
 * dokuyu ve duman yoğunluğunu belirler. Balta eksen korunarak {@code soyulmus_komurlesmis_kutuk}'a dönüştürür
 * ({@link #getToolModifiedState}); soğuk blok ısı yaymaz. "Suya konulunca"
 * kömür bloğuna döner: kütük suya bitişik bir yere konunca ({@link #onPlace}, eski blok kütük değilse) ya da su kütüğün
 * yanına gelince ya da akınca ({@link #neighborChanged}, değişen komşu suysa) {@value #EN_AZ_TICK}–{@value #EN_COK_TICK}
 * tick sonrasına planlanmış tick kurulur (dünya kaydıyla saklanır, {@code randomTickSpeed}'e bağlı değildir) ve
 * {@link #DONUSUYOR} true olur. Pulsar kavurmasının bir kütüğü yerinde kömürleştirmesi (eski blok {@code minecraft:logs})
 * ve suyun dışındaki komşu değişimleri dönüşümü başlatmaz: göl kıyısındaki kavrulmuş orman kendiliğinden kömür bloğuna
 * dönmez. Duman yalnız dönüşüm sürerken ({@link #DONUSUYOR} true; istemci planlanmış tick'i göremez) tüter. Su
 * kaldırılınca dönüşüm iptal olur ({@link #DONUSUYOR} false); tick geldiğinde su hâlâ bitişikse kömür bloğuna döner
 * (duman patlaması ve sönme sesi). Oyuncu eylemidir: {@code afetBlokHasari} kuralına bağlı değildir.
 */
public final class KomurlesmisKutukBlogu extends RotatedPillarBlock implements DurumaBagliIsiKaynagi {
    /** Suya değdikten sonra kömür bloğuna dönüşe kadar en az ve en çok bekleme (tick; 2–4 dakika). */
    public static final int EN_AZ_TICK = 2400;
    public static final int EN_COK_TICK = 4800;
    /** Kömür bloğuna dönüşüm sürüyor (tick planlı, su bitişik); eski dünyadaki ve kavurmanın koyduğu kütükler false. */
    public static final BooleanProperty DONUSUYOR = BooleanProperty.create("donusuyor");
    /** İsı kademesi: 0 sönük … {@value KutukIsisi#MAKS} en sıcak; eski dünyadaki ve kavurmanın koyduğu kütükler {@value KutukIsisi#VARSAYILAN}. */
    public static final IntegerProperty ISI = IntegerProperty.create("isi", 0, KutukIsisi.MAKS);

    public KomurlesmisKutukBlogu(Properties ozellikler) {
        super(ozellikler);
        registerDefaultState(defaultBlockState().setValue(DONUSUYOR, false).setValue(ISI, KutukIsisi.VARSAYILAN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> kurucu) {
        super.createBlockStateDefinition(kurucu);
        kurucu.add(DONUSUYOR, ISI);
    }

    @Override
    public double gucCarpani(BlockState durum) {
        return KutukIsisi.gucCarpani(durum.getValue(ISI));
    }

    @Override
    public double yaricapCarpani(BlockState durum) {
        return KutukIsisi.yaricapCarpani(durum.getValue(ISI));
    }

    /** Balta: eksen korunarak soyulmuş (soğuk, ısı yaymayan) kütüğe döner. */
    @Nullable
    @Override
    public BlockState getToolModifiedState(BlockState durum, UseOnContext baglam, ToolAction eylem, boolean benzetim) {
        if (eylem == ToolActions.AXE_STRIP) {
            return PulsarKayitlari.SOYULMUS_KOMURLESMIS_KUTUK.get().defaultBlockState().setValue(AXIS,
                    durum.getValue(AXIS));
        }
        return super.getToolModifiedState(durum, baglam, eylem, benzetim);
    }

    /**
     * Lav kovası +1 kademe (sınırda reddedilir, kova elde kalır); kar topu söndürür (zaten sönükse topu fırlatır). Gizlenerek
     * (eşya elde, {@code shift}) tıklanırsa vanilla gibi blok etkileşimi atlanır.
     */
    @Override
    public InteractionResult use(BlockState durum, Level level, BlockPos konum, Player oyuncu, InteractionHand el,
            BlockHitResult vurus) {
        ItemStack yigin = oyuncu.getItemInHand(el);
        int isi = durum.getValue(ISI);
        if (yigin.is(Items.LAVA_BUCKET)) {
            if (isi >= KutukIsisi.MAKS) {
                if (!level.isClientSide) {
                    oyuncu.displayClientMessage(Component.translatable("message.dogalafetler.komurlesmis_kutuk.dolu"), true);
                }
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide) {
                level.setBlock(konum, durum.setValue(ISI, isi + 1), Block.UPDATE_ALL);
                KomurlesmisKutukOlaylari.isiYukseldi(oyuncu, isi + 1);
                level.playSound(null, konum, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.gameEvent(oyuncu, GameEvent.FLUID_PLACE, konum);
                if (!oyuncu.getAbilities().instabuild) {
                    oyuncu.setItemInHand(el, ItemUtils.createFilledResult(yigin, oyuncu, new ItemStack(Items.BUCKET)));
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (yigin.is(Items.SNOWBALL) && isi > 0) {
            if (!level.isClientSide) {
                sondur(level, konum, durum);
                if (!oyuncu.getAbilities().instabuild) {
                    yigin.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(durum, level, konum, oyuncu, el, vurus);
    }

    /** Kütüğü söndürür (kademe 0): sönme sesi ve duman; zaten sönükse bir şey yapmaz. Yalnız sunucuda çağrılır. */
    public static void sondur(Level level, BlockPos konum, BlockState durum) {
        if (level.isClientSide || !durum.hasProperty(ISI) || durum.getValue(ISI) == 0) {
            return;
        }
        level.setBlock(konum, durum.setValue(ISI, 0), Block.UPDATE_ALL);
        level.playSound(null, konum, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f,
                1.5f + (level.random.nextFloat() - level.random.nextFloat()) * 0.3f);
        if (level instanceof ServerLevel sunucu) {
            sunucu.sendParticles(ParticleTypes.LARGE_SMOKE, konum.getX() + 0.5, konum.getY() + 1.0, konum.getZ() + 0.5, 10,
                    0.3, 0.2, 0.3, 0.02);
        }
    }

    /** Altı yüzden birine su (kaynak, akıntı ya da su tutan blok) değiyor mu. */
    public static boolean suyaDegiyor(BlockGetter level, BlockPos konum) {
        BlockPos.MutableBlockPos k = new BlockPos.MutableBlockPos();
        for (Direction yon : Direction.values()) {
            if (level.getFluidState(k.setWithOffset(konum, yon)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPlace(BlockState durum, Level level, BlockPos konum, BlockState eski, boolean hareketli) {
        super.onPlace(durum, level, konum, eski, hareketli);
        if (!eski.is(BlockTags.LOGS)) {
            planla(level, konum);
        }
    }

    @Override
    public void neighborChanged(BlockState durum, Level level, BlockPos konum, Block komsu, BlockPos komsuKonum,
            boolean hareketli) {
        super.neighborChanged(durum, level, konum, komsu, komsuKonum, hareketli);
        if (level.isClientSide) {
            return;
        }
        if (level.getFluidState(komsuKonum).is(FluidTags.WATER)) {
            planla(level, konum);
        } else if (durum.getValue(DONUSUYOR) && !suyaDegiyor(level, konum)) {
            level.setBlock(konum, durum.setValue(DONUSUYOR, false), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Su bitişikse dönüşümü başlatır: tick yoksa kurar (önce: kendi {@code setBlock}'unun {@link #onPlace}'i yeniden
     * kurmasın), {@link #DONUSUYOR}'u true yapar. Su geri geldiğinde eski tick hâlâ planlıysa o kullanılır.
     */
    private void planla(Level level, BlockPos konum) {
        if (level.isClientSide || !suyaDegiyor(level, konum)) {
            return;
        }
        if (!level.getBlockTicks().hasScheduledTick(konum, this)) {
            level.scheduleTick(konum, this, EN_AZ_TICK + level.random.nextInt(EN_COK_TICK - EN_AZ_TICK + 1));
        }
        BlockState durum = level.getBlockState(konum);
        if (durum.is(this) && !durum.getValue(DONUSUYOR)) {
            level.setBlock(konum, durum.setValue(DONUSUYOR, true), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void tick(BlockState durum, ServerLevel level, BlockPos konum, RandomSource r) {
        if (!suyaDegiyor(level, konum)) {
            if (durum.getValue(DONUSUYOR)) {
                level.setBlock(konum, durum.setValue(DONUSUYOR, false), Block.UPDATE_CLIENTS);
            }
            return;
        }
        level.setBlock(konum, Blocks.COAL_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        double x = konum.getX() + 0.5;
        double y = konum.getY() + 0.5;
        double z = konum.getZ() + 0.5;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.4, z, 16, 0.4, 0.3, 0.4, 0.02);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.5, z, 6, 0.3, 0.2, 0.3, 0.01);
        level.playSound(null, konum, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f,
                1.6f + (r.nextFloat() - r.nextFloat()) * 0.4f);
    }

    @Override
    public void animateTick(BlockState durum, Level level, BlockPos konum, RandomSource r) {
        if (durum.getValue(DONUSUYOR) && r.nextInt(3) == 0) {
            double x = konum.getX() + 0.2 + r.nextDouble() * 0.6;
            double z = konum.getZ() + 0.2 + r.nextDouble() * 0.6;
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, konum.getY() + 1.0, z, 0.0, 0.03,
                    0.0);
            if (r.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, x, konum.getY() + 1.0, z, 0.0, 0.02, 0.0);
            }
        }
        isiParcaciklari(durum.getValue(ISI), level, konum, r);
    }

    /**
     * Kademeye göre üstten yükselen duman (ses ve yoğunluk {@link KutukIsisi}'nden): ılıkta ince duman, sıcakta koyu duman,
     * çok sıcakta ayrıca her yüzden kıvılcım, alev ve lav damlası. Sönükte hiçbir şey yok.
     */
    private static void isiParcaciklari(int isi, Level level, BlockPos konum, RandomSource r) {
        double yogunluk = KutukIsisi.dumanYogunlugu(isi);
        if (yogunluk <= 0) {
            return;
        }
        int adet = (int) yogunluk + (r.nextDouble() < yogunluk - (int) yogunluk ? 1 : 0);
        boolean sicak = isi >= KutukIsisi.SICAK_ESIGI;
        boolean cokSicak = KutukIsisi.cokSicak(isi);
        for (int i = 0; i < adet; i++) {
            double x = konum.getX() + 0.15 + r.nextDouble() * 0.7;
            double z = konum.getZ() + 0.15 + r.nextDouble() * 0.7;
            ParticleOptions tur;
            if (!sicak) {
                tur = r.nextInt(3) == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.SMOKE;
            } else {
                tur = r.nextInt(3) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
            }
            level.addAlwaysVisibleParticle(tur, true, x, konum.getY() + 1.0, z, (r.nextDouble() - 0.5) * 0.01,
                    0.03 + 0.01 * isi, (r.nextDouble() - 0.5) * 0.01);
        }
        if (cokSicak) {
            for (int i = 0; i < 2; i++) {
                Direction yon = Direction.getRandom(r);
                double x = konum.getX() + 0.5 + yon.getStepX() * 0.55 + (yon.getStepX() == 0 ? (r.nextDouble() - 0.5) : 0);
                double y = konum.getY() + 0.5 + yon.getStepY() * 0.55 + (yon.getStepY() == 0 ? (r.nextDouble() - 0.5) : 0);
                double z = konum.getZ() + 0.5 + yon.getStepZ() * 0.55 + (yon.getStepZ() == 0 ? (r.nextDouble() - 0.5) : 0);
                level.addParticle(PulsarKayitlari.KOR.get(), x, y, z, yon.getStepX() * 0.02 + (r.nextDouble() - 0.5) * 0.02,
                        0.04 + r.nextDouble() * 0.03, yon.getStepZ() * 0.02 + (r.nextDouble() - 0.5) * 0.02);
            }
            if (r.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.FLAME, konum.getX() + 0.2 + r.nextDouble() * 0.6, konum.getY() + 1.0,
                        konum.getZ() + 0.2 + r.nextDouble() * 0.6, 0.0, 0.02, 0.0);
            }
            if (r.nextInt(5) == 0) {
                level.addParticle(ParticleTypes.LAVA, konum.getX() + 0.2 + r.nextDouble() * 0.6, konum.getY() + 1.0,
                        konum.getZ() + 0.2 + r.nextDouble() * 0.6, 0.0, 0.0, 0.0);
            }
        }
        if (r.nextDouble() < KutukIsisi.citirtiOlasiligi(isi)) {
            level.playLocalSound(konum.getX() + 0.5, konum.getY() + 0.5, konum.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE,
                    SoundSource.BLOCKS, 0.4f + r.nextFloat() * 0.3f, 0.6f + r.nextFloat() * 0.7f, false);
        }
    }
}
