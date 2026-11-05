package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Sunucu otoriteli kasırga (tasarım notu §2 "Sunucu"): görünmez, fiziksiz. Konum vanilla izleyiciyle {@link #SENKRON_ARALIGI}
 * tick'te bir (4 Hz) gider; seviye, evre, evre ilerlemesi, yön ve kalite {@link SynchedEntityData} ile, evre ilerlemesi
 * de 4 Hz'de güncellenir. İstemci aradaki konumu {@link #lerpTo} ile 5 tick'e yayarak enterpole eder.
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
    /** -1: istemcinin kendi {@code efektKalitesi} ayarı; 0-3: spike komutuyla zorlanan kalite sırası. */
    private static final EntityDataAccessor<Integer> KALITE = SynchedEntityData.defineId(KasirgaVarligi.class,
            EntityDataSerializers.INT);

    public static final int SENKRON_ARALIGI = 5;
    public static final int EN_COK_UCAN_BLOK = 40;
    /** Spike: kasırga ölçümler boyunca yüklü alanda kalsın diye doğduğu noktadan en çok bu kadar uzaklaşır. */
    private static final double EVDEN_EN_UZAK = 40;
    private static final float SAPMA = (float) Math.toRadians(35);
    /** İstemci enterpolasyonu (spike karşılaştırması için kapatılabilir; yalnız istemci thread'i yazar). */
    public static volatile boolean istemciEnterpolasyonu = true;

    private long yas;
    private int aktifTick = KasirgaEvresi.SINIRSIZ;
    private double evX;
    private double evZ;
    private float anaYon;
    private boolean hareketli = true;
    private final List<UcanBlokVarligi> ucanBloklar = new ArrayList<>();
    private int toplamKaldirilan;

    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private int lerpAdim;

    public KasirgaVarligi(EntityType<? extends KasirgaVarligi> tur, Level seviye) {
        super(tur, seviye);
        noPhysics = true;
        noCulling = true;
    }

    /** Sunucuda doğuştan hemen önce çağrılır. */
    public void kur(int seviye, int kaliteSirasi, float yon) {
        entityData.set(SEVIYE, Mth.clamp(seviye, 1, 5));
        entityData.set(KALITE, kaliteSirasi);
        entityData.set(YON, yon);
        anaYon = yon;
        evX = getX();
        evZ = getZ();
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SEVIYE, 1);
        entityData.define(EVRE, KasirgaEvresi.DOGUS.ordinal());
        entityData.define(EVRE_ILERLEME, 0f);
        entityData.define(YON, 0f);
        entityData.define(KALITE, -1);
    }

    public int seviye() {
        return Mth.clamp(entityData.get(SEVIYE), 1, 5);
    }

    public KasirgaSeviyesi seviyeBilgisi() {
        return KasirgaSeviyesi.icin(seviye());
    }

    public KasirgaEvresi evre() {
        return KasirgaEvresi.sirayla(entityData.get(EVRE));
    }

    public float evreIlerleme() {
        return entityData.get(EVRE_ILERLEME);
    }

    public float yon() {
        return entityData.get(YON);
    }

    public int kaliteSirasi() {
        return entityData.get(KALITE);
    }

    public void kaliteSiralamasiniAyarla(int sira) {
        entityData.set(KALITE, sira);
    }

    public void hareketiAyarla(boolean acik) {
        hareketli = acik;
    }

    public int ucanBlokSayisi() {
        return ucanBloklar.size();
    }

    public int toplamKaldirilan() {
        return toplamKaldirilan;
    }

    public RankineProfili rankine() {
        KasirgaSeviyesi s = seviyeBilgisi();
        return new RankineProfili(s.cekirdekAcisalHizi(), s.tabanYaricapi());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            istemciEnterpolasyonuAdimi();
            return;
        }
        long bas = System.nanoTime();
        sunucuTik();
        KasirgaSunucuIstatistik.kasirgaTiki(System.nanoTime() - bas);
    }

    private void sunucuTik() {
        yas++;
        KasirgaEvresi.Durum durum = KasirgaEvresi.hesapla(yas, aktifTick);
        if (durum.bitti()) {
            discard();
            return;
        }
        boolean evreDegisti = durum.evre().ordinal() != entityData.get(EVRE);
        if (evreDegisti || yas % SENKRON_ARALIGI == 0) {
            entityData.set(EVRE, durum.evre().ordinal());
            entityData.set(EVRE_ILERLEME, (float) durum.ilerleme());
        }
        hareketEt(durum);
        ucanBloklar.removeIf(Entity::isRemoved);
        boolean kaldirabilir = durum.evre() == KasirgaEvresi.OLGUN
                || (durum.evre() == KasirgaEvresi.DOGUS && durum.ilerleme() > 0.5);
        if (kaldirabilir && Kurallar.blokHasariAcik(level().getGameRules())) {
            bloklariKaldir();
        }
    }

    private void hareketEt(KasirgaEvresi.Durum durum) {
        if (!hareketli) {
            return;
        }
        float yon = yon();
        if (yas % 40 == 0) {
            double dx = evX - getX();
            double dz = evZ - getZ();
            if (dx * dx + dz * dz > EVDEN_EN_UZAK * EVDEN_EN_UZAK) {
                anaYon = (float) Mth.atan2(dz, dx);
            }
            float sapma = Mth.wrapDegrees((float) Math.toDegrees(yon - anaYon + (random.nextFloat() - 0.5f) * 0.6f));
            yon = anaYon + Mth.clamp((float) Math.toRadians(sapma), -SAPMA, SAPMA);
            entityData.set(YON, yon);
        }
        double carpan = switch (durum.evre()) {
            case DOGUS -> 0.5;
            case IP -> 0.7;
            default -> 1.0;
        };
        double adim = seviyeBilgisi().hiz() / 20.0 * carpan;
        double nx = getX() + Math.cos(yon) * adim;
        double nz = getZ() + Math.sin(yon) * adim;
        double zemin = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(nx), Mth.floor(nz));
        setPos(nx, getY() + (zemin - getY()) * 0.2, nz);
    }

    /** Tick başına seviye tablosundaki sayı kadar deneme; TPS &lt; 18 ise yarıya, &lt; 15 ise dörtte bire iner. */
    private void bloklariKaldir() {
        ServerLevel sl = (ServerLevel) level();
        KasirgaSeviyesi s = seviyeBilgisi();
        int deneme = s.blokKaldirmaTick();
        float ortMs = sl.getServer().getAverageTickTime();
        if (ortMs > 1000f / 15) {
            deneme = Math.max(1, deneme / 4);
        } else if (ortMs > 1000f / 18) {
            deneme = Math.max(1, deneme / 2);
        }
        for (int i = 0; i < deneme && ucanBloklar.size() < EN_COK_UCAN_BLOK; i++) {
            double aci = random.nextDouble() * Math.PI * 2;
            double r = s.cekimYaricapi() * 0.6 * Math.sqrt(random.nextDouble());
            int x = Mth.floor(getX() + Math.cos(aci) * r);
            int z = Mth.floor(getZ() + Math.sin(aci) * r);
            BlockPos konum = new BlockPos(x, sl.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
            BlockState durum = sl.getBlockState(konum);
            if (durum.isAir() || !durum.getFluidState().isEmpty() || durum.hasBlockEntity()) {
                continue;
            }
            float sertlik = durum.getDestroySpeed(sl, konum);
            if (sertlik < 0 || sertlik > s.sertlikSiniri()) {
                continue;
            }
            sl.removeBlock(konum, false);
            double yaricap = Math.hypot(konum.getX() + 0.5 - getX(), konum.getZ() + 0.5 - getZ());
            double baslangicAcisi = Math.atan2(konum.getZ() + 0.5 - getZ(), konum.getX() + 0.5 - getX());
            UcanBlokVarligi blok = UcanBlokVarligi.olustur(sl, durum, this, baslangicAcisi, yaricap,
                    konum.getY() - getY());
            sl.addFreshEntity(blok);
            ucanBloklar.add(blok);
            toplamKaldirilan++;
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int adim, boolean isinlanma) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpAdim = istemciEnterpolasyonu ? SENKRON_ARALIGI : 1;
    }

    private void istemciEnterpolasyonuAdimi() {
        if (lerpAdim <= 0) {
            return;
        }
        setPos(getX() + (lerpX - getX()) / lerpAdim, getY() + (lerpY - getY()) / lerpAdim,
                getZ() + (lerpZ - getZ()) / lerpAdim);
        lerpAdim--;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        KasirgaSeviyesi s = seviyeBilgisi();
        double r = s.duvarBulutuYaricapi();
        return new AABB(getX() - r, getY(), getZ() - r, getX() + r, getY() + s.yukseklik() + 12, getZ() + r);
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
    protected void readAdditionalSaveData(CompoundTag etiket) {
        // Spike varlığı kaydedilmez (EntityType.noSave).
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag etiket) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
