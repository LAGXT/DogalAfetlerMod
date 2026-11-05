package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Gökyüzü durumunun dünya verisi (madde 16). Overworld'ün veri deposunda {@code data/dogalafetler_gokyuzu.dat}
 * olarak durur; boyut ne olursa olsun sunucu genelinde tektir (N11).
 */
public final class GokyuzuVerisi extends SavedData {
    public static final String AD = "dogalafetler_gokyuzu";

    private GokyuzuDurumu durum = GokyuzuDurumu.VARSAYILAN;

    public static GokyuzuVerisi al(MinecraftServer sunucu) {
        return sunucu.overworld().getDataStorage().computeIfAbsent(GokyuzuVerisi::yukle, GokyuzuVerisi::new, AD);
    }

    private static GokyuzuVerisi yukle(CompoundTag etiket) {
        GokyuzuVerisi veri = new GokyuzuVerisi();
        veri.durum = GokyuzuDurumu.nbtOku(etiket.getCompound("durum"));
        DogalAfetler.LOGGER.info("[gokyuzu] SavedData okundu ({}): {}", AD, veri.durum);
        return veri;
    }

    @Override
    public CompoundTag save(CompoundTag etiket) {
        etiket.put("durum", durum.nbtYaz(new CompoundTag()));
        DogalAfetler.LOGGER.info("[gokyuzu] SavedData yazıldı ({}): {}", AD, durum);
        return etiket;
    }

    public GokyuzuDurumu durum() {
        return durum;
    }

    public void durumAyarla(GokyuzuDurumu yeni) {
        if (!yeni.equals(durum)) {
            durum = yeni;
            setDirty();
        }
    }
}
