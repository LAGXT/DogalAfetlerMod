package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Global donma ilerlemesi G (0–100) ve çözülme dönemi bayrağı (madde 9: SavedData'da). Faz 2'de çekirdeğin dünya
 * dönemi verisine taşınır (GOREV §4); spike kendi dosyasını kullanır.
 */
public final class DonmaVerisi extends SavedData {
    private static final String AD = DogalAfetler.MOD_ID + "_donma_spike";

    private int ilerleme;
    private boolean cozulme;

    public static DonmaVerisi al(MinecraftServer sunucu) {
        return sunucu.overworld().getDataStorage().computeIfAbsent(DonmaVerisi::yukle, DonmaVerisi::new, AD);
    }

    private static DonmaVerisi yukle(CompoundTag etiket) {
        DonmaVerisi veri = new DonmaVerisi();
        veri.ilerleme = Math.max(0, Math.min(100, etiket.getInt("ilerleme")));
        veri.cozulme = etiket.getBoolean("cozulme");
        return veri;
    }

    @Override
    public CompoundTag save(CompoundTag etiket) {
        etiket.putInt("ilerleme", ilerleme);
        etiket.putBoolean("cozulme", cozulme);
        return etiket;
    }

    public int ilerleme() {
        return ilerleme;
    }

    public void ilerleme(int yeni) {
        ilerleme = Math.max(0, Math.min(100, yeni));
        setDirty();
    }

    public boolean cozulme() {
        return cozulme;
    }

    public void cozulme(boolean yeni) {
        cozulme = yeni;
        setDirty();
    }
}
