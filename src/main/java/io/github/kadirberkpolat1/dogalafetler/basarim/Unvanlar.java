package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Unvan;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Kozmetik ünvan (N16): oyuncu verisinde saklanır, sohbet adının ({@code PlayerEvent.NameFormat}) ve tab listesindeki
 * adın ({@code PlayerEvent.TabListNameFormat}) önünde altın renkli {@code [<ünvan>]} olarak görünür. Ad çevrilebilir
 * bileşendir: her istemci kendi dilinde görür. {@code basarim.unvanGoster=false} iken önek eklenmez (veri korunur).
 */
public final class Unvanlar {
    private Unvanlar() {
    }

    /** Önek: {@code [<ünvan>] } (altın). */
    public static MutableComponent onek(Unvan unvan) {
        return Component.literal("[").append(Component.translatable(unvan.ceviriAnahtari())).append("] ")
                .withStyle(ChatFormatting.GOLD);
    }

    /** {@code ad}'ın önüne önek; adın kendi biçimi korunur. */
    public static Component onekli(Unvan unvan, Component ad) {
        return Component.empty().append(onek(unvan)).append(ad);
    }

    public static void ayarla(ServerPlayer oyuncu, Optional<Unvan> unvan) {
        BasarimVerisi.unvanYaz(oyuncu, unvan);
        yenile(oyuncu);
    }

    /** Sohbet adını ve tab listesi adını yeniden hesaplatır (tab adı değişince herkese paket gider). */
    public static void yenile(ServerPlayer oyuncu) {
        oyuncu.refreshDisplayName();
        oyuncu.refreshTabListName();
    }

    public static void hepsiniYenile(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer oyuncu : server.getPlayerList().getPlayers()) {
            yenile(oyuncu);
        }
    }
}
