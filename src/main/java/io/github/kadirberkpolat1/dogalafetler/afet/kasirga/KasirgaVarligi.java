package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.Carpanlar;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaEvresi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaSeviyesi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Kasırganın görüntü taşıyıcısı (tasarım notu §2 "Sunucu", spike SK-04): görünmez, fiziksiz, kaydedilmez. Konumunu ve
 * evresini sahibi {@link Kasirga} afeti her tick yazar; vanilla izleyici konumu {@link #SENKRON_ARALIGI} tick'te bir (4 Hz)
 * gönderir, seviye, evre, evre ilerlemesi, yön, boyut çarpanı, çekim çarpanı ve kaldırma tavanı {@link SynchedEntityData}'dadır
 * (evre ilerlemesi 4 Hz). İstemci konumu {@link #lerpTo} ile 5 tick'e yayar; oyuncunun istemcisi çekimi bu veriden
 * hesaplar ({@link #cekimKaynagi}, AF3-01). Sahibi yoksa ya da bittiyse kendini atar.
 */
public class KasirgaVarligi extends Entity {
    private static final EntityDataAccessor<Integer> SEVIYE = SynchedEntityData.defineId(KasirgaVarligi.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EVRE = SynchedEntityData.defineId(KasirgaVarligi.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> EVRE_ILERLEME = SynchedEntityData.defineId(KasirgaVarligi.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> YON = SynchedEntityData.defineId(KasirgaVarligi.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OLCEK = SynchedEntityData.defineId(KasirgaVarligi.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CEKIM = SynchedEntityData.defineId(KasirgaVarligi.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TAVAN = SynchedEntityData.defineId(KasirgaVarligi.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FIRLATMA = SynchedEntityData.defineId(KasirgaVarligi.class,
            EntityDataSerializers.FLOAT);

    public static final int SENKRON_ARALIGI = 5;

    private Kasirga sahip;
    private KasirgaSeviyesi onbellek;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private int lerpAdim;

    public KasirgaVarligi(EntityType<? extends KasirgaVarligi> tur, Level level) {
        super(tur, level);
        noPhysics = true;
        noCulling = true;
    }

    void bagla(Kasirga kasirga, int seviye, float olcek, float cekim, float tavan) {
        sahip = kasirga;
        entityData.set(SEVIYE, Mth.clamp(seviye, 1, 5));
        entityData.set(OLCEK, olcek);
        entityData.set(CEKIM, cekim);
        entityData.set(TAVAN, tavan);
    }

    /** Sahibin her tick'teki yazımı; evre ilerlemesi yalnız evre değişince ve 4 Hz'de kirlenir (ağ). */
    void guncelle(KasirgaEvresi.Durum durum, float yon, float firlatma) {
        boolean evreDegisti = durum.evre().ordinal() != entityData.get(EVRE);
        if (evreDegisti || tickCount % SENKRON_ARALIGI == 0) {
            entityData.set(EVRE, durum.evre().ordinal());
            entityData.set(EVRE_ILERLEME, (float) durum.ilerleme());
            entityData.set(YON, yon);
            entityData.set(FIRLATMA, firlatma);
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SEVIYE, 1);
        entityData.define(EVRE, KasirgaEvresi.OLUSUM.ordinal());
        entityData.define(EVRE_ILERLEME, 0f);
        entityData.define(YON, 0f);
        entityData.define(OLCEK, 1f);
        entityData.define(CEKIM, 1f);
        entityData.define(TAVAN, 0f);
        entityData.define(FIRLATMA, 0f);
    }

    public int seviye() {
        return Mth.clamp(entityData.get(SEVIYE), 1, 5);
    }

    /** Görüntü boyutları (sunucunun boyut çarpanı dahil); seviye ya da çarpan değişince yeniden kurulur. */
    public KasirgaSeviyesi seviyeBilgisi() {
        float olcek = Mth.clamp(entityData.get(OLCEK), 0.25f, 3f);
        KasirgaSeviyesi s = onbellek;
        if (s == null || s.seviye() != seviye() || Math.abs(s.yukseklik() - KasirgaSeviyesi.icin(seviye()).yukseklik() * olcek) > 1e-3) {
            s = KasirgaSeviyesi.icin(seviye(), new Carpanlar(olcek, 1, 1, 1, 1, 1));
            onbellek = s;
        }
        return s;
    }

    public KasirgaEvresi evre() {
        return KasirgaEvresi.sirayla(entityData.get(EVRE));
    }

    public float evreIlerleme() {
        return entityData.get(EVRE_ILERLEME);
    }

    /** Hareket yönü (radyan; x doğu, z güney). */
    public float yon() {
        return entityData.get(YON);
    }

    /**
     * Çekim girdisi: konum, boyutlar, kaldırma tavanı; çekim çarpanı evre gücü ile fırlatma rampasının büyüğü × sunucunun
     * {@code cekimCarpani}'dır (kasırga yok olurken çekim fırlatma bitene dek tam kalır).
     */
    public KasirgaCekimi.Kaynak cekimKaynagi() {
        double firlatma = entityData.get(FIRLATMA);
        double guc = Math.max(KasirgaEvresi.guc(new KasirgaEvresi.Durum(evre(), evreIlerleme())), firlatma);
        double cekim = entityData.get(CEKIM);
        return new KasirgaCekimi.Kaynak(position(), seviyeBilgisi(), guc * cekim, entityData.get(TAVAN), firlatma * cekim);
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
        } else if (sahip == null || sahip.sonlandi() || sahip.varlik() != this) {
            discard();
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
    public AABB getBoundingBoxForCulling() {
        KasirgaSeviyesi s = seviyeBilgisi();
        double r = s.duvarBulutuYaricapi();
        return new AABB(getX() - r, getY() - 2, getZ() - r, getX() + r, getY() + s.yukseklik() + 12, getZ() + r);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double mesafeKare) {
        return mesafeKare < 512 * 512;
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
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag etiket) {
        // Kaydedilmez (noSave): durum afetin NBT'sindedir.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag etiket) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
