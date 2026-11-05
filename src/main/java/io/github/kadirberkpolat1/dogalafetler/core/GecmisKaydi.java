package io.github.kadirberkpolat1.dogalafetler.core;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Son afetler geçmişinin bir satırı (Gelişmiş Hava Radarı, madde 13a). {@code saat} afetin bittiği ölçekli afet saatidir. */
public record GecmisKaydi(AfetTuru tur, int seviye, BlockPos merkez, ResourceKey<Level> boyut, long saat) {
    public CompoundTag yaz() {
        CompoundTag etiket = new CompoundTag();
        etiket.putString("tur", tur.kimlik());
        etiket.putInt("seviye", seviye);
        etiket.put("merkez", NbtUtils.writeBlockPos(merkez));
        etiket.putString("boyut", boyut.location().toString());
        etiket.putLong("saat", saat);
        return etiket;
    }

    public static Optional<GecmisKaydi> oku(CompoundTag etiket) {
        Optional<AfetTuru> tur = AfetTuru.kimlikIle(etiket.getString("tur"));
        ResourceLocation boyut = ResourceLocation.tryParse(etiket.getString("boyut"));
        if (tur.isEmpty() || boyut == null) {
            return Optional.empty();
        }
        return Optional.of(new GecmisKaydi(tur.get(), etiket.getInt("seviye"), NbtUtils.readBlockPos(etiket.getCompound("merkez")),
                ResourceKey.create(Registries.DIMENSION, boyut), etiket.getLong("saat")));
    }
}
