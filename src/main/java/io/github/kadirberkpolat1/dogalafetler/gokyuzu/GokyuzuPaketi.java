package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Sunucu → istemci gökyüzü durumu. Yalnız durum değişince ve oyuncu girişinde gider (madde 3: gereksiz paket yok).
 * İstemci tarafı kendi alıcısını {@link #istemciAlicisi} ile kurar; böylece bu ortak sınıf istemci sınıfını import
 * etmez (N15).
 */
public record GokyuzuPaketi(GokyuzuDurumu durum) {
    private static volatile Consumer<GokyuzuDurumu> istemciAlicisi = durum -> {
    };

    /** İstemci kurulumu çağırır; adanmış sunucuda hiç çağrılmaz. */
    public static void istemciAlicisi(Consumer<GokyuzuDurumu> alici) {
        istemciAlicisi = alici;
    }

    public static void yaz(GokyuzuPaketi paket, FriendlyByteBuf tampon) {
        paket.durum.yaz(tampon);
    }

    public static GokyuzuPaketi oku(FriendlyByteBuf tampon) {
        return new GokyuzuPaketi(GokyuzuDurumu.oku(tampon));
    }

    public static void isle(GokyuzuPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        istemciAlicisi.accept(paket.durum);
        baglam.get().setPacketHandled(true);
    }
}
