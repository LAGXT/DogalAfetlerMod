package io.github.kadirberkpolat1.dogalafetler.network;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Modun tek ağ kanalı. Sunucu otoritelidir: afet durumu sunucuda tutulur, istemciye yalnız senkron paketleri gider
 * (madde 2, 3). Protokol değiştiğinde {@link #PROTOKOL_SURUMU} artırılır; farklı sürümler bağlanamaz.
 */
public final class AgKanali {
    public static final String PROTOKOL_SURUMU = "1";

    public static final SimpleChannel KANAL = NetworkRegistry.newSimpleChannel(
            DogalAfetler.konum("ana"),
            () -> PROTOKOL_SURUMU,
            PROTOKOL_SURUMU::equals,
            PROTOKOL_SURUMU::equals);

    private static final AtomicInteger SONRAKI_KIMLIK = new AtomicInteger();

    private AgKanali() {
    }

    /** Kanalı mod inşası sırasında oluşturur (sınıfı yükler). */
    public static void baslat() {
        DogalAfetler.LOGGER.debug("Ağ kanalı {} hazır, protokol sürümü {}", KANAL, PROTOKOL_SURUMU);
    }

    /** Sunucudan istemciye giden bir paket tipini kaydeder; işleyici istemcinin ana thread'inde çalışır. */
    public static <M> void sunucudanIstemciye(Class<M> tur, BiConsumer<M, FriendlyByteBuf> yazici,
            Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici) {
        kaydet(tur, NetworkDirection.PLAY_TO_CLIENT, yazici, okuyucu, isleyici);
    }

    /** İstemciden sunucuya giden bir paket tipini kaydeder; işleyici sunucunun ana thread'inde çalışır. */
    public static <M> void istemcidenSunucuya(Class<M> tur, BiConsumer<M, FriendlyByteBuf> yazici,
            Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici) {
        kaydet(tur, NetworkDirection.PLAY_TO_SERVER, yazici, okuyucu, isleyici);
    }

    private static <M> void kaydet(Class<M> tur, NetworkDirection yon, BiConsumer<M, FriendlyByteBuf> yazici,
            Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici) {
        KANAL.messageBuilder(tur, SONRAKI_KIMLIK.getAndIncrement(), yon)
                .encoder(yazici)
                .decoder(okuyucu)
                .consumerMainThread(isleyici)
                .add();
    }

    public static void oyuncuyaGonder(ServerPlayer oyuncu, Object mesaj) {
        KANAL.send(PacketDistributor.PLAYER.with(() -> oyuncu), mesaj);
    }

    public static void boyuttakilereGonder(ResourceKey<Level> boyut, Object mesaj) {
        KANAL.send(PacketDistributor.DIMENSION.with(() -> boyut), mesaj);
    }

    public static void herkeseGonder(Object mesaj) {
        KANAL.send(PacketDistributor.ALL.noArg(), mesaj);
    }

    public static void sunucuyaGonder(Object mesaj) {
        KANAL.sendToServer(mesaj);
    }
}
