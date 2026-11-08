package io.github.kadirberkpolat1.dogalafetler.network;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
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
 * (madde 2, 3). Alt sistemler mod kurucusunda {@link #kaydet} ile katılır; her paketin {@code <alt sistem>/<paket>}
 * biçiminde benzersiz bir adı vardır:
 *
 * <pre>{@code
 * AgKanali.kaydet(k -> k.sunucudanIstemciye("uyari/siren", SirenPaketi.class, SirenPaketi::yaz, SirenPaketi::oku,
 *         SirenPaketi::isle));
 * }</pre>
 *
 * Ortak kurulumda ({@code enqueueWork}) bütün tanımlar toplanır ve paket kimlikleri <b>ada göre sıralanmış</b> listeden
 * verilir; katılım sırası ya da sınıf yükleme sırası kimliği değiştirmez, iki tarafta aynı ad kümesi aynı kimlikleri verir.
 * Katılımlar koşulsuz olmalıdır (Dist denetimi yok). Paket ekleyen dal protokolü artırmaz; entegratör birleştirmede
 * {@link #PROTOKOL_SURUMU}'nu bir kez artırır (K-C47).
 */
public final class AgKanali {
    public static final String PROTOKOL_SURUMU = "9";

    public static final SimpleChannel KANAL = NetworkRegistry.newSimpleChannel(
            DogalAfetler.konum("ana"),
            () -> PROTOKOL_SURUMU,
            PROTOKOL_SURUMU::equals,
            PROTOKOL_SURUMU::equals);

    private static final Pattern AD = Pattern.compile("[a-z0-9_]+/[a-z0-9_]+");

    /** Paket tiplerini tanımlayan araç; işleyiciler alıcının ana thread'inde çalışır. */
    public interface Kaydedici {
        <M> void sunucudanIstemciye(String ad, Class<M> tur, BiConsumer<M, FriendlyByteBuf> yazici,
                Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici);

        <M> void istemcidenSunucuya(String ad, Class<M> tur, BiConsumer<M, FriendlyByteBuf> yazici,
                Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici);
    }

    /** Bir alt sistemin paket tanımları. */
    @FunctionalInterface
    public interface PaketKatkisi {
        void kaydet(Kaydedici kaydedici);
    }

    private record Tanim(String ad, Class<?> tur, KimlikleKayit kayit) {
    }

    @FunctionalInterface
    private interface KimlikleKayit {
        void kaydet(int kimlik);
    }

    private static final List<PaketKatkisi> KATKILAR = new CopyOnWriteArrayList<>();
    private static boolean kaydedildi;

    private AgKanali() {
    }

    /** Kanalı mod inşası sırasında oluşturur (sınıfı yükler). */
    public static void baslat() {
        DogalAfetler.LOGGER.debug("Ağ kanalı {} hazır, protokol sürümü {}", KANAL, PROTOKOL_SURUMU);
    }

    /** Bir alt sistemin paketlerini ekler; yalnız mod kurucusunda, koşulsuz çağrılır. */
    public static synchronized void kaydet(PaketKatkisi katki) {
        if (kaydedildi) {
            throw new IllegalStateException("Paketler ortak kurulumda kaydedildi; katılım mod kurucusunda eklenmeli");
        }
        KATKILAR.add(Objects.requireNonNull(katki, "katki"));
    }

    /** Ortak kurulumda (ana thread) bir kez: tanımları toplar, ada göre sıralar, kimlikleri o sırayla verir. */
    public static synchronized void paketleriKaydet() {
        if (kaydedildi) {
            return;
        }
        kaydedildi = true;
        Map<String, Tanim> tanimlar = new TreeMap<>();
        Set<Class<?>> siniflar = new HashSet<>();
        Kaydedici toplayici = new Kaydedici() {
            @Override
            public <M> void sunucudanIstemciye(String ad, Class<M> tur, BiConsumer<M, FriendlyByteBuf> yazici,
                    Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici) {
                ekle(ad, tur, kimlik -> kur(kimlik, tur, NetworkDirection.PLAY_TO_CLIENT, yazici, okuyucu, isleyici));
            }

            @Override
            public <M> void istemcidenSunucuya(String ad, Class<M> tur, BiConsumer<M, FriendlyByteBuf> yazici,
                    Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici) {
                ekle(ad, tur, kimlik -> kur(kimlik, tur, NetworkDirection.PLAY_TO_SERVER, yazici, okuyucu, isleyici));
            }

            private void ekle(String ad, Class<?> tur, KimlikleKayit kayit) {
                if (ad == null || !AD.matcher(ad).matches()) {
                    throw new IllegalArgumentException("Paket adı <alt sistem>/<paket> (küçük ASCII) olmalı: " + ad);
                }
                if (tanimlar.containsKey(ad)) {
                    throw new IllegalStateException("Aynı adlı iki paket: " + ad);
                }
                if (!siniflar.add(tur)) {
                    throw new IllegalStateException("Aynı paket sınıfı iki kez kaydedildi: " + tur.getName());
                }
                tanimlar.put(ad, new Tanim(ad, tur, kayit));
            }
        };
        toplayici.sunucudanIstemciye("cekirdek/donem", DonemPaketi.class, DonemPaketi::yaz, DonemPaketi::oku,
                DonemPaketi::isle);
        toplayici.sunucudanIstemciye("cekirdek/afet_durumu", AfetDurumPaketi.class, AfetDurumPaketi::yaz,
                AfetDurumPaketi::oku, AfetDurumPaketi::isle);
        for (PaketKatkisi katki : KATKILAR) {
            katki.kaydet(toplayici);
        }
        StringJoiner liste = new StringJoiner(", ");
        int kimlik = 0;
        for (Tanim tanim : tanimlar.values()) {
            tanim.kayit().kaydet(kimlik);
            liste.add(kimlik + "=" + tanim.ad());
            kimlik++;
        }
        DogalAfetler.LOGGER.info("{} ağ paketi kaydedildi (protokol {}): {}", kimlik, PROTOKOL_SURUMU, liste);
    }

    private static <M> void kur(int kimlik, Class<M> tur, NetworkDirection yon, BiConsumer<M, FriendlyByteBuf> yazici,
            Function<FriendlyByteBuf, M> okuyucu, BiConsumer<M, Supplier<NetworkEvent.Context>> isleyici) {
        KANAL.messageBuilder(tur, kimlik, yon)
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
