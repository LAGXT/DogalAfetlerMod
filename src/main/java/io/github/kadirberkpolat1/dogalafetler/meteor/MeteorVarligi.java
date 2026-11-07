package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorOlcegi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZari;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.Yorunge;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Düşen meteor (sunucu otoriteli). Sabit hızla düz bir çizgide iner; sunucu her tick yolunu blok blok örnekler ve ilk
 * katı bloka (yaprak ve çarpışmasız bitkiler hariç) ya da suya (gerçek ya da sanal) çarpınca {@link MeteorCarpmasi}'nı
 * başlatıp kaybolur. İstemci aynı hızla ilerletir (sunucu 2 tick'te bir düzeltir), iz parçacıklarını üretir ve tahmini
 * iniş noktasını (yer parlama diski) hesaplar. Ölçek ({@link MeteorOlcegi}) ve görünüm senkron veridir; kaydedilmez.
 */
public final class MeteorVarligi extends Entity {
    private static final EntityDataAccessor<Float> OLCEK = SynchedEntityData.defineId(MeteorVarligi.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> GORUNUM = SynchedEntityData.defineId(MeteorVarligi.class,
            EntityDataSerializers.BYTE);
    private static final double ORNEK_ADIMI = 0.4;
    /** Hedefine hiç ulaşamayan (yüklü olmayan chunk'tan geçen) meteor bu yaşta atılır. */
    private static final int EN_COK_YAS = 600;
    /** İz parçacıkları bu mesafeye kadar; ötesinde yalnız iz şeridi çizilir (tasarım §5 LOD). */
    private static final double PARCACIK_MESAFESI = 192;

    private YanginKaynagi kaynak = YanginKaynagi.KOMUT;
    private boolean sarsinti = true;
    private OdulZari.Sanslar sanslar;
    /** Yağmurun hedef oyuncusu (ödül tavanı; yalnız sunucu, null: tek düşürülen meteor). */
    private UUID odulOyuncu;
    private int yas;
    private Vec3 inis;
    private double inisKalanTick = -1;

    public MeteorVarligi(EntityType<? extends MeteorVarligi> tur, Level level) {
        super(tur, level);
        noPhysics = true;
    }

    static MeteorVarligi olustur(ServerLevel level, Yorunge y, double olcek, MeteorGorunumu gorunum,
            YanginKaynagi kaynak, boolean sarsinti, OdulZari.Sanslar sanslar, UUID odulOyuncu) {
        MeteorVarligi m = new MeteorVarligi(MeteorKayitlari.METEOR.get(), level);
        m.setPos(y.baslangicX(), y.baslangicY(), y.baslangicZ());
        m.setDeltaMovement(y.hizX(), y.hizY(), y.hizZ());
        m.entityData.set(OLCEK, (float) olcek);
        m.entityData.set(GORUNUM, (byte) gorunum.ordinal());
        m.kaynak = kaynak;
        m.sarsinti = sarsinti;
        m.sanslar = sanslar;
        m.odulOyuncu = odulOyuncu;
        return m;
    }

    /** Sürekli ölçek ({@value MeteorOlcegi#KUCUK_ALT}–{@value MeteorOlcegi#UST}). */
    public double olcek() {
        return entityData.get(OLCEK);
    }

    /** Ödül sınıfı (ölçeğin bantından). */
    public MeteorBoyutu boyut() {
        return MeteorBoyutu.olcekten(olcek());
    }

    public MeteorGorunumu gorunum() {
        return MeteorGorunumu.kimlik(entityData.get(GORUNUM));
    }

    public YanginKaynagi kaynak() {
        return kaynak;
    }

    /** Varlığın yaşı (tick); istemcide iz uzunluğu bununla sınırlanır. */
    public int yas() {
        return yas;
    }

    /** İstemci: tahmini iniş noktası (yoksa {@code null}). */
    public Vec3 inis() {
        return inis;
    }

    /** İstemci: inişe kalan tahmini tick (bilinmiyorsa −1). */
    public double inisKalanTick() {
        return inisKalanTick;
    }

    /** İz şeridinin uzunluğu (blok): ölçek 0,5'te 80, 2,5'te 140 (MT2-32; eskiden 40–72), uçtuğu yoldan uzun olmaz. */
    public double izUzunlugu() {
        double tam = MeteorOlcegi.ara(olcek(), 86, 125, 80, 140);
        return Math.min(tam, (yas + 1) * getDeltaMovement().length());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OLCEK, (float) MeteorOlcegi.KUCUK_TIPIK);
        entityData.define(GORUNUM, (byte) 0);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 konum = position();
        Vec3 hiz = getDeltaMovement();
        yas++;
        if (level() instanceof ServerLevel sunucu) {
            if (yas > EN_COK_YAS) {
                discard();
                return;
            }
            Carpisma c = carpismaAra(sunucu, konum, hiz);
            if (c != null) {
                discard();
                MeteorCarpmasi.carp(sunucu, c.zemin(), c.nokta(), olcek(), gorunum(), kaynak, sarsinti,
                        sanslar != null ? sanslar : MeteorAyarlari.sanslar(), c.suya(), odulOyuncu);
                return;
            }
        } else {
            izParcaciklari(konum, hiz);
            if (yas % 4 == 1) {
                inisHesapla(konum, hiz);
            } else if (inisKalanTick > 0) {
                inisKalanTick--;
            }
        }
        setPos(konum.add(hiz));
    }

    record Carpisma(BlockPos zemin, Vec3 nokta, boolean suya) {
    }

    /** {@code a}'dan {@code a + v}'ye ilk çarpılan blok ya da su (yoksa {@code null}). */
    static Carpisma carpismaAra(ServerLevel level, Vec3 a, Vec3 v) {
        int adim = Math.max(1, (int) Math.ceil(v.length() / ORNEK_ADIMI));
        BlockPos son = null;
        for (int i = 1; i <= adim; i++) {
            Vec3 q = a.add(v.scale(i / (double) adim));
            BlockPos p = BlockPos.containing(q);
            if (p.equals(son)) {
                continue;
            }
            son = p;
            if (q.y < level.getMinBuildHeight()) {
                return new Carpisma(p.atY(level.getMinBuildHeight()), q, false);
            }
            if (q.y >= level.getMaxBuildHeight() || !level.isLoaded(p)) {
                continue;
            }
            FluidState sivi = level.getFluidState(p);
            if (sivi.is(FluidTags.WATER) && q.y <= p.getY() + sivi.getHeight(level, p) + 1e-3) {
                return new Carpisma(p, q, true);
            }
            double sanal = SanalSu.suSeviyesi(level, p.getX(), p.getZ());
            if (!Double.isNaN(sanal) && q.y < sanal) {
                return new Carpisma(p, q, true);
            }
            BlockState durum = level.getBlockState(p);
            if (durum.isAir() || durum.is(BlockTags.LEAVES) || durum.getCollisionShape(level, p).isEmpty()) {
                continue;
            }
            return new Carpisma(p, q, false);
        }
        return null;
    }

    private void inisHesapla(Vec3 konum, Vec3 hiz) {
        double h = hiz.length();
        if (h < 1e-6) {
            return;
        }
        BlockHitResult sonuc = level().clip(new ClipContext(konum, konum.add(hiz.scale(48)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, this));
        if (sonuc.getType() == HitResult.Type.MISS) {
            inis = null;
            inisKalanTick = -1;
        } else {
            inis = sonuc.getLocation();
            inisKalanTick = inis.distanceTo(konum) / h;
        }
    }

    private void izParcaciklari(Vec3 konum, Vec3 hiz) {
        Player yakin = level().getNearestPlayer(this, PARCACIK_MESAFESI);
        if (yakin == null) {
            return;
        }
        double yogunluk = MeteorAyarlari.izYogunlugu() * kaliteCarpani(IstemciAyarlari.efektKalitesi());
        if (yogunluk <= 0) {
            return;
        }
        boolean ay = gorunum() == MeteorGorunumu.AY;
        double olcek = MeteorOlcegi.ara(olcek(), 1.0, 2.4, 0.6, 3.0);
        SimpleParticleType plazma = (ay ? MeteorKayitlari.PLAZMA_AY : MeteorKayitlari.PLAZMA).get();
        SimpleParticleType kivilcim = (ay ? MeteorKayitlari.KIVILCIM_AY : MeteorKayitlari.KIVILCIM).get();
        Vec3 geri = hiz.scale(-1);
        int plazmaSayisi = adet(5 * olcek * yogunluk);
        for (int i = 0; i < plazmaSayisi; i++) {
            Vec3 p = konum.add(geri.scale(random.nextDouble())).add(sacilma(0.35 * olcek));
            level().addParticle(plazma, p.x, p.y, p.z, hiz.x * 0.04 + sacilmaTek(0.02), hiz.y * 0.04 + sacilmaTek(0.02),
                    hiz.z * 0.04 + sacilmaTek(0.02));
        }
        int dumanSayisi = adet(1.6 * olcek * yogunluk);
        for (int i = 0; i < dumanSayisi; i++) {
            Vec3 p = konum.add(geri.scale(0.5 + random.nextDouble())).add(sacilma(0.5 * olcek));
            level().addParticle(MeteorKayitlari.DUMAN.get(), p.x, p.y, p.z, sacilmaTek(0.01), 0.004, sacilmaTek(0.01));
        }
        int kivilcimSayisi = adet(2.2 * olcek * yogunluk);
        for (int i = 0; i < kivilcimSayisi; i++) {
            Vec3 p = konum.add(geri.scale(random.nextDouble())).add(sacilma(0.4 * olcek));
            level().addParticle(kivilcim, p.x, p.y, p.z, hiz.x * 0.12 + sacilmaTek(0.15), hiz.y * 0.12 + sacilmaTek(0.15),
                    hiz.z * 0.12 + sacilmaTek(0.15));
        }
    }

    private int adet(double beklenen) {
        int tam = (int) beklenen;
        return tam + (random.nextDouble() < beklenen - tam ? 1 : 0);
    }

    private Vec3 sacilma(double r) {
        return new Vec3(sacilmaTek(r), sacilmaTek(r), sacilmaTek(r));
    }

    private double sacilmaTek(double r) {
        return (random.nextDouble() * 2 - 1) * r;
    }

    /** Efekt kalitesinin parçacık çarpanı (tasarım §5: Düşük 20 / Orta 40 / Yüksek 80 / Ultra 140 iz noktası). */
    public static double kaliteCarpani(EfektKalitesi k) {
        return switch (k) {
            case DUSUK -> 0.25;
            case ORTA -> 0.5;
            case YUKSEK -> 1.0;
            case ULTRA -> 1.75;
        };
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 hiz = getDeltaMovement();
        double h = hiz.length();
        AABB kutu = getBoundingBox().inflate(3);
        if (h < 1e-6) {
            return kutu;
        }
        Vec3 kuyruk = position().subtract(hiz.scale(izUzunlugu() / h));
        return kutu.minmax(new AABB(kuyruk, kuyruk).inflate(3));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double mesafeKare) {
        return mesafeKare < 512 * 512;
    }

    @Override
    public boolean shouldBeSaved() {
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
