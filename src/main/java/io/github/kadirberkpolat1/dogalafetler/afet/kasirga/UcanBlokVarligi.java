package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.RankineProfili;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Kasırganın koparıp taşıdığı gerçek blok (tasarım notu §2 katman 5, spike SK-03). Önce kasırga çevresinde fiziksiz sarmal
 * yörüngede (Rankine, siklonik) 25-60 blok yükselir, sonra teğet hızını koruyarak dışa savrulur ve vanilla çarpışmasıyla
 * düşer; yere değince gerçek blok olarak kalır (kütükler yatay: "devrik kütük"). Uçarken çarptığı canlıya bir kez hasar
 * verir. {@link BlockState} ve kasırga varlığının kimliği istemciye gider; istemci yalnız görsel enkaz kopyaları üretir.
 */
public class UcanBlokVarligi extends Entity {
    private static final EntityDataAccessor<BlockState> BLOK = SynchedEntityData.defineId(UcanBlokVarligi.class,
            EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Integer> KASIRGA = SynchedEntityData.defineId(UcanBlokVarligi.class,
            EntityDataSerializers.INT);

    public static final int SENKRON_ARALIGI = 2;
    private static final int EN_UZUN_OMUR = 600;
    private static final int CARPMA_ARALIGI = 4;

    /**
     * Blok durumu ve kasırga kimliği istemciye ulaşınca bir kez çağrılır; istemci kurulumu bağlar (N15: ortak kod istemci
     * sınıfını tanımaz). {@code AddEntity} paketinden sonra {@code SetEntityData} geldiği için giriş olayı yerine bu kanca.
     */
    public static volatile Consumer<UcanBlokVarligi> istemciVeriKancasi = blok -> {
    };

    private Kasirga sahip;
    private double aci;
    private double yaricap;
    private double yukseklik;
    private double hedefYukseklik;
    private double dikeyHiz;
    private boolean serbest;
    private int yas;
    private final IntSet vurulanlar = new IntOpenHashSet();
    private boolean istemciyeBildirildi;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private int lerpAdim;

    public UcanBlokVarligi(EntityType<? extends UcanBlokVarligi> tur, Level level) {
        super(tur, level);
        noPhysics = true;
    }

    static UcanBlokVarligi olustur(ServerLevel level, BlockState durum, Kasirga sahip, int kasirgaKimligi, double aci,
            double yaricap, double yukseklik) {
        UcanBlokVarligi blok = new UcanBlokVarligi(KasirgaKayitlari.UCAN_BLOK.get(), level);
        blok.entityData.set(BLOK, durum);
        blok.entityData.set(KASIRGA, kasirgaKimligi);
        blok.sahip = sahip;
        blok.aci = aci;
        blok.yaricap = yaricap;
        blok.yukseklik = yukseklik;
        KasirgaSeviyesi s = sahip.seviyeBilgisi();
        blok.hedefYukseklik = Math.min(25 + level.getRandom().nextDouble() * 35, s.yukseklik() * 0.6);
        blok.dikeyHiz = 3 + level.getRandom().nextDouble() * 4;
        blok.konumla();
        return blok;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(BLOK, Blocks.DIRT.defaultBlockState());
        entityData.define(KASIRGA, -1);
    }

    public BlockState blok() {
        return entityData.get(BLOK);
    }

    /** Bağlı kasırga varlığının ağ kimliği (-1: yok). */
    public int kasirgaKimligi() {
        return entityData.get(KASIRGA);
    }

    Kasirga sahip() {
        return sahip;
    }

    @Override
    public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> degerler) {
        super.onSyncedDataUpdated(degerler);
        if (level().isClientSide && !istemciyeBildirildi && kasirgaKimligi() >= 0) {
            istemciyeBildirildi = true;
            istemciVeriKancasi.accept(this);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpAdim > 0) {
                setPos(getX() + (lerpX - getX()) / lerpAdim, getY() + (lerpY - getY()) / lerpAdim,
                        getZ() + (lerpZ - getZ()) / lerpAdim);
                lerpAdim--;
            }
            return;
        }
        if (++yas > EN_UZUN_OMUR) {
            yereBirak();
            return;
        }
        if (yas % CARPMA_ARALIGI == 0) {
            carp();
        }
        if (!serbest) {
            if (sahip == null || sahip.sonlandi() || !sahip.bloklariTasiyor()) {
                birak(0.3);
            } else {
                yorungeAdimi();
            }
            return;
        }
        Vec3 hiz = getDeltaMovement().add(0, -0.04, 0).scale(0.98);
        setDeltaMovement(hiz);
        if (isleyenAlanDisinda(position().add(hiz))) {
            return;
        }
        move(MoverType.SELF, hiz);
        if (onGround()) {
            yereKon(blockPosition());
            discard();
        } else if (getY() < level().getMinBuildHeight()) {
            discard();
        }
    }

    private void yorungeAdimi() {
        KasirgaSeviyesi s = sahip.seviyeBilgisi();
        RankineProfili profil = s.rankine();
        double hedefYaricap = s.tabanYaricapi() * 0.9 + yukseklik * 0.08;
        yaricap += (hedefYaricap - yaricap) * 0.05;
        // Siklonik: yukarıdan bakınca saat yönünün tersi (x doğu, z güney olduğu için açı azalır).
        aci -= profil.acisalHiz(yaricap) / 20;
        yukseklik += dikeyHiz / 20;
        if (isleyenAlanDisinda(yorungeKonumu())) {
            return;
        }
        konumla();
        if (yukseklik >= hedefYukseklik) {
            birak(profil.tegetHiz(yaricap) / 20);
        }
    }

    private Vec3 yorungeKonumu() {
        Vec3 merkez = sahip.konum();
        return new Vec3(merkez.x + Math.cos(aci) * yaricap, merkez.y + yukseklik, merkez.z + Math.sin(aci) * yaricap);
    }

    private void konumla() {
        setPos(yorungeKonumu());
    }

    /**
     * Kn4: sonraki konum varlık işlemeyen yerdeyse blok şimdiki (işleyen, yüklü) yerinde bırakılır; oraya giden {@code noSave}
     * varlık donar ve chunk boşalınca bloğuyla silinirdi.
     */
    private boolean isleyenAlanDisinda(Vec3 hedef) {
        if (level() instanceof ServerLevel sl && !sl.isPositionEntityTicking(BlockPos.containing(hedef))) {
            yereBirak();
            return true;
        }
        return false;
    }

    /** Teğet hızını koruyarak dışa ve hafif yukarı savrulur, sonra vanilla çarpışmasıyla düşer. */
    private void birak(double tegetHiz) {
        serbest = true;
        noPhysics = false;
        double disa = 0.25 + random.nextDouble() * 0.25;
        setDeltaMovement(Math.sin(aci) * tegetHiz + Math.cos(aci) * disa, 0.2,
                -Math.cos(aci) * tegetHiz + Math.sin(aci) * disa);
    }

    /** Uçan blok çarptığı canlıya bir kez hasar verir (2 + 0,5·seviye, hasar çarpanıyla). */
    private void carp() {
        if (sahip == null || !(level() instanceof ServerLevel sl)) {
            return;
        }
        double miktar = (2 + 0.5 * sahip.seviye()) * KasirgaAyarlari.carpanlar().hasar();
        if (miktar <= 0) {
            return;
        }
        for (LivingEntity hedef : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.3),
                e -> e.isAlive() && !e.isSpectator() && !vurulanlar.contains(e.getId()))) {
            vurulanlar.add(hedef.getId());
            hedef.hurt(KasirgaKayitlari.hasar(sl), (float) miktar);
        }
    }

    /** Yere değdiği yerde gerçek blok; olmuyorsa kırılmış gibi eşyasını düşürür. Kütükler yatay yatırılır. */
    private void yereKon(BlockPos konum) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        BlockState durum = blok();
        if (durum.is(BlockTags.LOGS) && durum.hasProperty(RotatedPillarBlock.AXIS)) {
            durum = durum.setValue(RotatedPillarBlock.AXIS, random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z);
        }
        if (sl.isLoaded(konum) && sl.getBlockState(konum).canBeReplaced() && sl.getFluidState(konum).isEmpty()
                && durum.canSurvive(sl, konum)) {
            sl.setBlock(konum, durum, Block.UPDATE_ALL);
            SanalSu.chunkDegisti(sl, new ChunkPos(konum));
        } else {
            esyaOlarakDus();
        }
    }

    /**
     * Afet biterken ya da sunucu kapanırken havadaki blok: altındaki zemine konur (olmuyorsa eşyası düşer) ve varlık atlır;
     * dünyada geçici varlık kalmaz, blok kaybolmaz.
     */
    void yereBirak() {
        if (level() instanceof ServerLevel sl && !isRemoved()) {
            BlockPos ust = blockPosition();
            if (sl.isLoaded(ust)) {
                int zemin = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, ust.getX(), ust.getZ());
                yereKon(new BlockPos(ust.getX(), Math.min(ust.getY(), Math.max(zemin, sl.getMinBuildHeight())), ust.getZ()));
            }
        }
        discard();
    }

    private void esyaOlarakDus() {
        if (level() instanceof ServerLevel sl && getY() >= sl.getMinBuildHeight()) {
            Block.dropResources(blok(), sl, blockPosition());
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int adim, boolean isinlanma) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpAdim = SENKRON_ARALIGI;
    }

    @Override
    public boolean hurt(DamageSource kaynak, float miktar) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag etiket) {
        // Kaydedilmez (noSave): sunucu kapanırken afet havadaki blokları yere bırakır.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag etiket) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
