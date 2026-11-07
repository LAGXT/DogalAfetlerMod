package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import io.github.kadirberkpolat1.dogalafetler.veri.SesSaglayici;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Kara delik yutmasının sesi ve sarsıntısı (pulsar-2 PL-4): {@code KARADELIK_YUTMA} evresinin parlama anında
 * ({@code PulsarSureci}, kalıcı bayrakla bir kez) {@link #yutmaPatlamasi} bütün çevrim içi oyunculara {@code gokyuzu/yutma}
 * paketini yollar; istemci kütleçekim dalgası sesini konumsuz çalar ve tek seferlik ekran sarsıntısını başlatır
 * ({@code client.gokyuzu.PulsarSarsintisi}). Oyuncu nerede olursa olsun duyar (gök aşaması herkeste aynıdır). Dosya
 * {@code tools/assets/ses_uret.py} ile üretilir; {@code sounds.json} ve altyazı datagen'dir.
 */
public final class PulsarSesleri {
    public static final ResourceLocation KARADELIK_YUTMA_KIMLIGI = DogalAfetler.konum("karadelik_yutma");
    static final String ALTYAZI = "subtitles.dogalafetler.karadelik_yutma";
    /** Sarsıntının tepe genliği (derece; ay çarpması 2,5) ve süresi (gerçek tick; ses 8 sn, parlama 55–60 sn). */
    static final float SARSINTI_DERECE = 3f;
    static final int SARSINTI_TICK = 100;

    private static final DeferredRegister<SoundEvent> KAYIT = DeferredRegister.create(Registries.SOUND_EVENT,
            DogalAfetler.MOD_ID);
    public static final RegistryObject<SoundEvent> KARADELIK_YUTMA = KAYIT.register(KARADELIK_YUTMA_KIMLIGI.getPath(),
            () -> SoundEvent.createVariableRangeEvent(KARADELIK_YUTMA_KIMLIGI));

    private PulsarSesleri() {
    }

    static void kaydet(IEventBus modBus) {
        KAYIT.register(modBus);
        AgKanali.kaydet(k -> k.sunucudanIstemciye("gokyuzu/yutma", YutmaPaketi.class, YutmaPaketi::yaz, YutmaPaketi::oku,
                YutmaPaketi::isle));
        DataGenerators.sesEkle(PulsarSesleri::tanimla);
        DataGenerators.ceviriEkle(PulsarSesleri::cevirileriYaz);
    }

    /** Sunucu ana thread'inde: bütün çevrim içi oyunculara (her boyutta) yutma sesi ve sarsıntısı. */
    public static void yutmaPatlamasi(MinecraftServer server) {
        int oyuncu = server.getPlayerList().getPlayerCount();
        AgKanali.herkeseGonder(new YutmaPaketi(SARSINTI_DERECE, SARSINTI_TICK));
        DogalAfetler.LOGGER.info("Pulsar: kara delik yutma patlaması (kütleçekim dalgası sesi + {} tick sarsıntı) {} oyuncuya",
                SARSINTI_TICK, oyuncu);
    }

    private static void tanimla(SesSaglayici saglayici) {
        saglayici.tanimla(KARADELIK_YUTMA_KIMLIGI.getPath(), SoundDefinition.definition().subtitle(ALTYAZI)
                .with(SoundDefinition.Sound.sound(KARADELIK_YUTMA_KIMLIGI, SoundDefinition.SoundType.SOUND)));
    }

    private static void cevirileriYaz(CeviriYazici yazici) {
        yazici.ekle(ALTYAZI, "Black hole swallows the pulsar", "Kara delik pulsarı yutuyor");
    }

    /**
     * {@code gokyuzu/yutma} (sunucu → istemci, yutma başına bir kez).
     *
     * @param siddet sarsıntının tepe genliği (derece; okurken 0–10'a kırpılır)
     * @param sureTick sarsıntının süresi (gerçek tick; okurken 1–400'e kırpılır)
     */
    public record YutmaPaketi(float siddet, int sureTick) {
        public static void yaz(YutmaPaketi p, FriendlyByteBuf t) {
            t.writeFloat(p.siddet);
            t.writeVarInt(p.sureTick);
        }

        public static YutmaPaketi oku(FriendlyByteBuf t) {
            return new YutmaPaketi(Math.max(0f, Math.min(10f, t.readFloat())), Math.max(1, Math.min(400, t.readVarInt())));
        }

        /** İstemci ana thread'inde: {@code client.gokyuzu.PulsarSarsintisi}'na N15 yoluyla iletilir. */
        public static void isle(YutmaPaketi paket, Supplier<NetworkEvent.Context> baglam) {
            Consumer<YutmaPaketi> alici = DistExecutor.safeCallWhenOn(Dist.CLIENT,
                    () -> io.github.kadirberkpolat1.dogalafetler.client.gokyuzu.PulsarSarsintisi::alici);
            if (alici != null) {
                alici.accept(paket);
            }
        }
    }
}
