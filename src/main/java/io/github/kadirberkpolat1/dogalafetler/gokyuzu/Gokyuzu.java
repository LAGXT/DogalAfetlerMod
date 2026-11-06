package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Gökyüzü durumunun sunucu tarafı okuma kısa yolları (madde 16). Başarım işçisi N16 ölçütleri için kullanır:
 * "Süpernova Tanığı" {@link #supernovayiGoruyorMu}, "Yıldızlı Gece" {@link #yildizliGeceyiGoruyorMu} (10 saniyelik
 * süre sayacı başarımındır). Durum değişimini dinlemek için {@code core.olay.DonemDegistiEvent} ({@code alan() ==
 * GUNES_EVRESI} ya da {@code AY}). Yalnız sunucu ana thread'inde.
 */
public final class Gokyuzu {
    private static final long GECE_BASI = 13_000L;
    private static final long GECE_SONU = 23_000L;

    private Gokyuzu() {
    }

    public static GunesEvresi gunesEvresi(MinecraftServer server) {
        return DunyaDonemi.al(server).gunesEvresi();
    }

    public static float evreIlerlemesi(MinecraftServer server) {
        return DunyaDonemi.al(server).evreIlerlemesi();
    }

    public static boolean ayVar(MinecraftServer server) {
        return DunyaDonemi.al(server).ayVar();
    }

    /** Oyuncu Overworld'de ve göz hizasının üstü göğe açık mı (N16 "gökyüzünü gören konum"). */
    public static boolean gokyuzunuGoruyorMu(ServerPlayer oyuncu) {
        Level level = oyuncu.level();
        return level.dimension() == Level.OVERWORLD && level.canSeeSky(BlockPos.containing(oyuncu.getEyePosition()));
    }

    /** Overworld gün saatinde gece mi (13000–23000; gün/gece döngüsü vanilla). */
    public static boolean geceMi(ServerLevel ustDunya) {
        long saat = Math.floorMod(ustDunya.getDayTime(), 24_000L);
        return saat >= GECE_BASI && saat < GECE_SONU;
    }

    /** N16 "Süpernovayı gökyüzünde gör": süpernova evresinde göğü gören konumda. */
    public static boolean supernovayiGoruyorMu(ServerPlayer oyuncu) {
        return gunesEvresi(oyuncu.server) == GunesEvresi.SUPERNOVA && gokyuzunuGoruyorMu(oyuncu);
    }

    /** N16 "Yıldızlı Gece": ay yokken gece, göğü gören konumda. */
    public static boolean yildizliGeceyiGoruyorMu(ServerPlayer oyuncu) {
        return !ayVar(oyuncu.server) && geceMi(oyuncu.server.overworld()) && gokyuzunuGoruyorMu(oyuncu);
    }
}
