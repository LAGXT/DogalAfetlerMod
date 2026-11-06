package io.github.kadirberkpolat1.dogalafetler.donma;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Kardes alt sistemlere giden kancalar (Faz 2b'de dallar henüz birleşmediği için arayüz; varsayılanı boş). Entegratör
 * birleştirmede mod kurucusunda bir kez bağlar ({@code .vibe/kararlar/donma.md} §A):
 *
 * <pre>{@code
 * DisKancalar.ayarla(new DisKancalar() {
 *     public void suYuzeyiDegisti(ServerLevel l, ChunkPos p) { SanalSu.chunkDegisti(l, p); }        // K0-30
 *     public boolean sanalSuVarMi(ServerLevel l, int x, int y, int z) { return y < SanalSu.suSeviyesi(l, x, z); }
 *     public double buharlasmaPayi(MinecraftServer s) { return DenizYukselmesiSureci.buharlasmaPayi(s); } // N10
 * });
 * }</pre>
 *
 * Kalıcı kış ağırlığının sıcaklığa ve {@code kizilcuce bilgi} çıktısına verilmesi ters yöndedir: {@link DonmaApi#kisAgirligi}
 * ve {@link DonmaApi#kisBolgesiSatirlari} sağlayıcı olarak dışarı açıktır. Yalnız sunucu ana thread'inde çağrılır.
 */
public interface DisKancalar {
    DisKancalar BOS = new DisKancalar() {
    };

    /** Donma ya da buharlaşma chunk'taki gerçek su yüzeyini değiştirdi (bayrak-2 blok değişimi olay atmaz). */
    default void suYuzeyiDegisti(ServerLevel level, ChunkPos chunk) {
    }

    /** Bu blok konumu sanal suyun (deniz yükselmesi, mağara suyu) altında mı; altındaki sütun donmaz. */
    default boolean sanalSuVarMi(ServerLevel level, int x, int y, int z) {
        return false;
    }

    /** Buharlaşmanın önce tüketeceği sanal deniz yükselmesi (blok, N10); gerçek su yalnız derinlik − pay kadar kurur. */
    default double buharlasmaPayi(MinecraftServer server) {
        return 0.0;
    }

    /** Bağlı kancalar. */
    static DisKancalar al() {
        return Tutucu.kancalar;
    }

    /** Mod kurucusunda bir kez (entegratör). */
    static void ayarla(DisKancalar kancalar) {
        Tutucu.kancalar = Objects.requireNonNull(kancalar, "kancalar");
    }

    final class Tutucu {
        private static volatile DisKancalar kancalar = BOS;

        private Tutucu() {
        }
    }
}
