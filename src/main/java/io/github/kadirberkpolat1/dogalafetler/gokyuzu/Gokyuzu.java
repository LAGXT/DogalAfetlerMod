package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuOlcutleri;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Gökyüzü durumunun sunucu tarafı okuma kısa yolları (madde 16). Başarım işçisi N16 ölçütleri için kullanır:
 * "Süpernova Tanığı" {@link #supernovayiGoruyorMu}, "Yıldızlı Gece" {@link #yildizliGeceMi} (10 saniyelik süre sayacı
 * başarımındır; tanımlar {@code mantik.GokyuzuOlcutleri}). Durum değişimini dinlemek için {@code core.olay.DonemDegistiEvent}
 * ({@code alan() == GUNES_EVRESI} ya da {@code AY}). Yalnız sunucu ana thread'inde.
 */
public final class Gokyuzu {
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

    /** Overworld gün saatinde gece mi (vanilla 13000–23000, güneşsiz dönemde de). */
    public static boolean geceMi(ServerLevel ustDunya) {
        return GokyuzuOlcutleri.geceMi(ustDunya.getDayTime());
    }

    /** N16 "Süpernovayı gökyüzünde gör": süpernova evresinde göğü gören konumda. */
    public static boolean supernovayiGoruyorMu(ServerPlayer oyuncu) {
        return GokyuzuOlcutleri.supernovaTanigi(gunesEvresi(oyuncu.server), gokyuzunuGoruyorMu(oyuncu));
    }

    /** N16 "Yıldızlı Gece": ay yok + gece (gündüz saymaz) + göğü gören konum. */
    public static boolean yildizliGeceMi(ServerPlayer oyuncu) {
        return GokyuzuOlcutleri.yildizliGece(ayVar(oyuncu.server), oyuncu.server.overworld().getDayTime(),
                gokyuzunuGoruyorMu(oyuncu));
    }
}
