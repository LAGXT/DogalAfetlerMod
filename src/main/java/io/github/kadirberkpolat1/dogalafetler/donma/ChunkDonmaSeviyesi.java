package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Chunk başına donma durumu: son işlenen global ilerleme ({@code sonG}, 0–100), o işlemin dönemi (donma / çözülme) ve
 * sütun başına "bizim koyduğumuz" bit kümeleri (kar katmanı, su sütunu buzu; 256'şar bit). Sütunun hedefi
 * {@code sonG}, dönem ve sütunun kış ağırlığından yeniden hesaplanır; yön G farkından gelir. Bit işaretli olmayan kar ve
 * buza (doğal ya da oyuncunun) dokunulmaz. Forge capability'si olarak chunk NBT'sinin {@code ForgeCaps} alanına chunk'la
 * birlikte yazılır (~70 bayt/chunk).
 */
public final class ChunkDonmaSeviyesi {
    public static final Capability<ChunkDonmaSeviyesi> YETENEK = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final ResourceLocation KIMLIK = DogalAfetler.konum("donma_seviyesi");

    private byte sonG;
    private boolean cozulme;
    private final long[] kar = new long[4];
    private final long[] buz = new long[4];

    public int sonG() {
        return sonG;
    }

    public boolean cozulme() {
        return cozulme;
    }

    public void durum(int g, boolean cozulmeDonemi) {
        sonG = (byte) Math.max(0, Math.min(100, g));
        cozulme = cozulmeDonemi;
    }

    /** Chunk içi sütun indeksi, 0–255. */
    public static int indeks(int x, int z) {
        return (z & 15) << 4 | (x & 15);
    }

    public boolean kar(int i) {
        return bit(kar, i);
    }

    public void kar(int i, boolean deger) {
        bit(kar, i, deger);
    }

    public boolean buz(int i) {
        return bit(buz, i);
    }

    public void buz(int i, boolean deger) {
        bit(buz, i, deger);
    }

    private static boolean bit(long[] k, int i) {
        return (k[i >>> 6] >>> (i & 63) & 1L) != 0;
    }

    private static void bit(long[] k, int i, boolean deger) {
        if (deger) {
            k[i >>> 6] |= 1L << (i & 63);
        } else {
            k[i >>> 6] &= ~(1L << (i & 63));
        }
    }

    static void kaydet(RegisterCapabilitiesEvent olay) {
        olay.register(ChunkDonmaSeviyesi.class);
    }

    static void ekle(AttachCapabilitiesEvent<LevelChunk> olay) {
        Level level = olay.getObject().getLevel();
        if (level == null || level.isClientSide() || level.dimension() != Level.OVERWORLD) {
            return;
        }
        olay.addCapability(KIMLIK, new Saglayici());
    }

    @Nullable
    public static ChunkDonmaSeviyesi al(LevelChunk chunk) {
        return chunk.getCapability(YETENEK).orElse(null);
    }

    public static int oku(LevelChunk chunk) {
        ChunkDonmaSeviyesi c = al(chunk);
        return c == null ? 0 : c.sonG();
    }

    static final class Saglayici implements ICapabilitySerializable<Tag> {
        final ChunkDonmaSeviyesi veri = new ChunkDonmaSeviyesi();
        private final LazyOptional<ChunkDonmaSeviyesi> secenek = LazyOptional.of(() -> veri);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @org.jetbrains.annotations.Nullable Direction yon) {
            return YETENEK.orEmpty(cap, secenek);
        }

        @Override
        public Tag serializeNBT() {
            CompoundTag etiket = new CompoundTag();
            etiket.putByte("g", veri.sonG);
            etiket.putBoolean("cozulme", veri.cozulme);
            etiket.putLongArray("kar", veri.kar);
            etiket.putLongArray("buz", veri.buz);
            return etiket;
        }

        @Override
        public void deserializeNBT(Tag etiket) {
            if (etiket instanceof NumericTag eski) {
                // İlk spike biçimi: yalnız seviye baytı (run-server/test dünyasında kaldı).
                veri.durum(eski.getAsByte(), false);
                return;
            }
            if (etiket instanceof CompoundTag c) {
                veri.durum(c.getByte("g"), c.getBoolean("cozulme"));
                kopyala(c.getLongArray("kar"), veri.kar);
                kopyala(c.getLongArray("buz"), veri.buz);
            }
        }

        private static void kopyala(long[] kaynak, long[] hedef) {
            System.arraycopy(kaynak, 0, hedef, 0, Math.min(kaynak.length, hedef.length));
        }
    }
}
