package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Kasırganın koparıp taşıdığı gerçek blok (tasarım notu §2 katman 5). {@link BlockState} ve bağlı olduğu kasırganın
 * kimliği {@link SynchedEntityData} ile istemciye gider; istemci bu kimlikle yalnız görsel enkaz kopyaları üretir.
 * Sunucuda önce kasırga çevresinde sarmal yörüngede yükselir (fiziksiz), hedef yükseklikte dışa savrulur ve vanilla
 * çarpışmasıyla düşer; yere değince gerçek blok olarak kalır.
 */
public class UcanBlokVarligi extends Entity {
    private static final EntityDataAccessor<BlockState> BLOK = SynchedEntityData.defineId(UcanBlokVarligi.class,
            EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Integer> KASIRGA = SynchedEntityData.defineId(UcanBlokVarligi.class,
            EntityDataSerializers.INT);

    public static final int SENKRON_ARALIGI = 2;
    private static final int EN_UZUN_OMUR = 600;

    private KasirgaVarligi kasirga;
    private double aci;
    private double yaricap;
    private double yukseklik;
    private double hedefYukseklik;
    private double dikeyHiz;
    private boolean serbest;
    private int yas;

    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private int lerpAdim;

    public UcanBlokVarligi(EntityType<? extends UcanBlokVarligi> tur, Level seviye) {
        super(tur, seviye);
        noPhysics = true;
    }

    static UcanBlokVarligi olustur(ServerLevel seviye, BlockState durum, KasirgaVarligi kasirga, double aci,
            double yaricap, double yukseklik) {
        UcanBlokVarligi blok = new UcanBlokVarligi(KasirgaVarliklari.UCAN_BLOK.get(), seviye);
        blok.entityData.set(BLOK, durum);
        blok.entityData.set(KASIRGA, kasirga.getId());
        blok.kasirga = kasirga;
        blok.aci = aci;
        blok.yaricap = yaricap;
        blok.yukseklik = yukseklik;
        KasirgaSeviyesi s = kasirga.seviyeBilgisi();
        blok.hedefYukseklik = Math.min(25 + seviye.getRandom().nextDouble() * 35, s.yukseklik() * 0.6);
        blok.dikeyHiz = 3 + seviye.getRandom().nextDouble() * 4;
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

    public int kasirgaKimligi() {
        return entityData.get(KASIRGA);
    }

    /**
     * Blok durumu ve kasırga kimliği istemciye ulaşınca bir kez çağrılır. İstemci kurulumu bağlar; ortak kod istemci
     * sınıfını tanımaz (N15). Varlık dünyaya girdiğinde veri henüz gelmemiş olur (AddEntity paketinden sonra
     * SetEntityData gelir), bu yüzden giriş olayı yerine bu kanca kullanılır.
     */
    public static volatile Consumer<UcanBlokVarligi> istemciVeriKancasi = blok -> {
    };
    private boolean istemciyeBildirildi;

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
        long bas = System.nanoTime();
        sunucuTik();
        KasirgaSunucuIstatistik.ucanBlokTiki(System.nanoTime() - bas);
    }

    private void sunucuTik() {
        if (++yas > EN_UZUN_OMUR) {
            discard();
            return;
        }
        if (!serbest) {
            if (kasirga == null || kasirga.isRemoved()) {
                birak(0.3);
            } else {
                yorungeAdimi();
            }
            return;
        }
        Vec3 hiz = getDeltaMovement().add(0, -0.04, 0).scale(0.98);
        setDeltaMovement(hiz);
        move(MoverType.SELF, hiz);
        if (onGround()) {
            yereKon();
            discard();
        } else if (getY() < level().getMinBuildHeight()) {
            discard();
        }
    }

    private void yorungeAdimi() {
        KasirgaSeviyesi s = kasirga.seviyeBilgisi();
        RankineProfili profil = kasirga.rankine();
        double hedefYaricap = s.tabanYaricapi() * 0.9 + yukseklik * 0.08;
        yaricap += (hedefYaricap - yaricap) * 0.05;
        // Siklonik: yukarıdan bakınca saat yönünün tersi (x doğu, z güney olduğu için açı azalır).
        aci -= profil.acisalHiz(yaricap) / 20;
        yukseklik += dikeyHiz / 20;
        konumla();
        if (yukseklik >= hedefYukseklik) {
            birak(profil.tegetHiz(yaricap) / 20);
        }
    }

    private void konumla() {
        setPos(kasirga.getX() + Math.cos(aci) * yaricap, kasirga.getY() + yukseklik,
                kasirga.getZ() + Math.sin(aci) * yaricap);
    }

    /** Teğet hızını koruyarak dışa ve hafif yukarı savrulur, sonra vanilla çarpışmasıyla düşer. */
    private void birak(double tegetHiz) {
        serbest = true;
        noPhysics = false;
        double disa = 0.25 + random.nextDouble() * 0.25;
        setDeltaMovement(Math.sin(aci) * tegetHiz + Math.cos(aci) * disa, 0.2,
                -Math.cos(aci) * tegetHiz + Math.sin(aci) * disa);
    }

    private void yereKon() {
        BlockPos konum = blockPosition();
        BlockState durum = blok();
        if (level().getBlockState(konum).canBeReplaced() && durum.canSurvive(level(), konum)) {
            level().setBlock(konum, durum, 3);
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag etiket) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
