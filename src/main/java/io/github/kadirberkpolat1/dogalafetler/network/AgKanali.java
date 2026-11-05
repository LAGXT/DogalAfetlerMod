package io.github.kadirberkpolat1.dogalafetler.network;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
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
 * Modun tek ağ kanalı (K0-07). Sunucu otoritelidir: afet durumu sunucuda tutulur, istemciye yalnız senkron paketleri gider
 * (madde 2, 3). Paket kimlikleri kayıt sırasıyla verildiği için kayıt her çalışmada aynı sıradadır: alt sistemler mod
 * kurucusunda {@link #kaydet} ile katkı ekler, çekirdek ortak kurulumda ({@code enqueueWork}) önce kendi paketlerini, sonra
 * katkıları eklendikleri sırayla kaydeder:
 *
 * <pre>{@code
 * AgKanali.kaydet(k -> {
 *     k.sunucudanIstemciye(SirenPaketi.class, SirenPaketi::yaz, SirenPaketi::oku, SirenPaketi::isle);
 * });
 * }</pre>
 *
 * Protokol değiştiğinde {@link #PROTOKOL_SURUMU} artırılır; farklı sürümler bağlanamaz.
 */
public final class AgKanali {
    public static final String PROTOKOL_SURUMU = "2";

    public static final SimpleChannel KANAL = NetworkRegistry.newSimpleChannel(
            DogalAfetler.konum("ana"),
            () -> PROTOKOL_SURUMU,
            PROTOKOL_SURUMU::equals,
            PROTOKOL_SURUMU::equals);

    /** Paket tiplerini kanala kaydeden araç; işleyiciler alıcının ana thread'inde çalışır. */
    public interface Kaydedici {
        <M> void sunucudanIstemciye(Class<M> tur, BiConsumer<M, FriendlyByteBuf> yazici,
                Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici);

        <M> void istemcidenSunucuya(Class<M> tur, BiConsumer<M, FriendlyByteBuf> yazici,
                Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici);
    }

    /** Bir alt sistemin paket kayıtları. */
    @FunctionalInterface
    public interface PaketKatkisi {
        void kaydet(Kaydedici kaydedici);
    }

    private static final List<PaketKatkisi> KATKILAR = new CopyOnWriteArrayList<>();
    private static boolean kaydedildi;
    private static int sonrakiKimlik;

    private AgKanali() {
    }

    /** Kanalı mod inşası sırasında oluşturur (sınıfı yükler). */
    public static void baslat() {
        DogalAfetler.LOGGER.debug("Ağ kanalı {} hazır, protokol sürümü {}", KANAL, PROTOKOL_SURUMU);
    }

    /** Bir alt sistemin paketlerini kayıt sırasına ekler; mod kurucusunda çağrılır. */
    public static synchronized void kaydet(PaketKatkisi katki) {
        if (kaydedildi) {
            throw new IllegalStateException("Paketler ortak kurulumda kaydedildi; katkı mod kurucusunda eklenmeli");
        }
        KATKILAR.add(Objects.requireNonNull(katki, "katki"));
    }

    /** Ortak kurulumda (ana thread) bir kez: önce çekirdek paketleri, sonra katkılar eklendikleri sırayla. */
    public static synchronized void paketleriKaydet() {
        if (kaydedildi) {
            return;
        }
        kaydedildi = true;
        Kaydedici kaydedici = new Kaydedici() {
            @Override
            public <M> void sunucudanIstemciye(Class<M> tur, BiConsumer<M, FriendlyByteBuf> yazici,
                    Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici) {
                ekle(tur, NetworkDirection.PLAY_TO_CLIENT, yazici, okuyucu, isleyici);
            }

            @Override
            public <M> void istemcidenSunucuya(Class<M> tur, BiConsumer<M, FriendlyByteBuf> yazici,
                    Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici) {
                ekle(tur, NetworkDirection.PLAY_TO_SERVER, yazici, okuyucu, isleyici);
            }
        };
        kaydedici.sunucudanIstemciye(DonemPaketi.class, DonemPaketi::yaz, DonemPaketi::oku, DonemPaketi::isle);
        kaydedici.sunucudanIstemciye(AfetDurumPaketi.class, AfetDurumPaketi::yaz, AfetDurumPaketi::oku,
                AfetDurumPaketi::isle);
        for (PaketKatkisi katki : KATKILAR) {
            katki.kaydet(kaydedici);
        }
        DogalAfetler.LOGGER.info("{} ağ paketi kaydedildi (protokol {})", sonrakiKimlik, PROTOKOL_SURUMU);
    }

    private static <M> void ekle(Class<M> tur, NetworkDirection yon, BiConsumer<M, FriendlyByteBuf> yazici,
            Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici) {
        KANAL.messageBuilder(tur, sonrakiKimlik++, yon)
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
