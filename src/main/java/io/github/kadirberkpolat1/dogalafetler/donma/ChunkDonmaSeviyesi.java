package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteTag;
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
import org.jetbrains.annotations.Nullable;

/**
 * Chunk başına donma seviyesi (0–100). Forge chunk capability'si olarak chunk NBT'sinin {@code ForgeCaps} alanına
 * chunk'la birlikte yazılır; böylece bloklar ve seviye aynı kayıtta, birlikte kaybolur ya da birlikte kalır.
 */
public final class ChunkDonmaSeviyesi {
    public static final Capability<ChunkDonmaSeviyesi> YETENEK = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final ResourceLocation KIMLIK = DogalAfetler.konum("donma_seviyesi");

    private byte seviye;

    public int seviye() {
        return seviye;
    }

    public void seviye(int yeni) {
        seviye = (byte) Math.max(0, Math.min(100, yeni));
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

    public static int oku(LevelChunk chunk) {
        return chunk.getCapability(YETENEK).map(ChunkDonmaSeviyesi::seviye).orElse(0);
    }

    public static void yaz(LevelChunk chunk, int seviye) {
        chunk.getCapability(YETENEK).ifPresent(v -> v.seviye(seviye));
        chunk.setUnsaved(true);
    }

    static final class Saglayici implements ICapabilitySerializable<ByteTag> {
        final ChunkDonmaSeviyesi veri = new ChunkDonmaSeviyesi();
        private final LazyOptional<ChunkDonmaSeviyesi> secenek = LazyOptional.of(() -> veri);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction yon) {
            return YETENEK.orEmpty(cap, secenek);
        }

        @Override
        public ByteTag serializeNBT() {
            return ByteTag.valueOf(veri.seviye);
        }

        @Override
        public void deserializeNBT(ByteTag etiket) {
            veri.seviye(etiket.getAsByte());
        }
    }
}
