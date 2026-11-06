package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Uyarı alt sisteminin ağ paketleri (yalnız sunucudan istemciye). İstemci alıcıları {@code client.uyari} paketindedir ve
 * çekirdeğin yoluyla alınır ({@code DistExecutor.safeCallWhenOn}, K-C14); ortak kod istemci sınıfı yüklemez (N15).
 */
public final class UyariPaketleri {
    private static final int KIMLIK_SINIRI = 64;

    private UyariPaketleri() {
    }

    static void kaydet(AgKanali.Kaydedici k) {
        k.sunucudanIstemciye("uyari/siren", SirenPaketi.class, SirenPaketi::yaz, SirenPaketi::oku, SirenPaketi::isle);
        k.sunucudanIstemciye("uyari/siren_durdur", SirenDurdurPaketi.class, SirenDurdurPaketi::yaz,
                SirenDurdurPaketi::oku, SirenDurdurPaketi::isle);
        k.sunucudanIstemciye("uyari/ortam", OrtamPaketi.class, OrtamPaketi::yaz, OrtamPaketi::oku, OrtamPaketi::isle);
        k.sunucudanIstemciye("uyari/ortam_durdur", OrtamDurdurPaketi.class, OrtamDurdurPaketi::yaz,
                OrtamDurdurPaketi::oku, OrtamDurdurPaketi::isle);
    }

    private static <M> void ilet(Supplier<DistExecutor.SafeCallable<Consumer<M>>> alici, M paket) {
        Consumer<M> tuketici = DistExecutor.safeCallWhenOn(Dist.CLIENT, alici);
        if (tuketici != null) {
            tuketici.accept(paket);
        }
    }

    /**
     * Siren başlasın: {@code toplamTick} sirenin tam süresi, {@code kalanTick} bu oyuncuda çalınacak kısmı (geç gelende
     * kısa); ikisi de gerçek tick. {@code afetNo} afetin başlangıç saati: aynı afetin tekrar isteği yok sayılır.
     */
    public record SirenPaketi(SirenTuru tur, AfetTuru afet, long afetNo, int toplamTick, int kalanTick) {
        void yaz(FriendlyByteBuf t) {
            t.writeEnum(tur);
            t.writeEnum(afet);
            t.writeLong(afetNo);
            t.writeVarInt(toplamTick);
            t.writeVarInt(kalanTick);
        }

        static SirenPaketi oku(FriendlyByteBuf t) {
            SirenTuru tur = t.readEnum(SirenTuru.class);
            AfetTuru afet = t.readEnum(AfetTuru.class);
            long afetNo = t.readLong();
            int toplam = SirenZarfi.toplamiKirp(t.readVarInt());
            return new SirenPaketi(tur, afet, afetNo, toplam, SirenZarfi.kalaniKirp(t.readVarInt(), toplam));
        }

        static void isle(SirenPaketi paket, Supplier<NetworkEvent.Context> baglam) {
            ilet(() -> io.github.kadirberkpolat1.dogalafetler.client.uyari.IstemciUyariPaketleri::sirenAlicisi, paket);
        }
    }

    /** Afetin ({@code afetNo}) sireni {@code fadeTick} içinde üstel sönsün (afet asıl etkisine geçti, madde 5). */
    public record SirenDurdurPaketi(long afetNo, int fadeTick) {
        void yaz(FriendlyByteBuf t) {
            t.writeLong(afetNo);
            t.writeVarInt(fadeTick);
        }

        static SirenDurdurPaketi oku(FriendlyByteBuf t) {
            return new SirenDurdurPaketi(t.readLong(), Math.max(1, Math.min(400, t.readVarInt())));
        }

        static void isle(SirenDurdurPaketi paket, Supplier<NetworkEvent.Context> baglam) {
            ilet(() -> io.github.kadirberkpolat1.dogalafetler.client.uyari.IstemciUyariPaketleri::sirenDurdurAlicisi, paket);
        }
    }

    /**
     * Bir ortam katmanının tam durumu (başlatma ve konum güncellemesi aynı paket). {@code boyut} null ise katman
     * konumsuzdur (her boyutta, sönümsüz); değilse {@code menzil} blokta doğrusal söner. {@code gecenTick} katmanın
     * başlangıcından beri geçen gerçek tick (sonradan giren eğriyi doğru yerden alır).
     */
    public record OrtamPaketi(String kimlik, long afetNo, ResourceLocation ses, SoundSource kategori,
            ResourceLocation boyut, double x, double y, double z, float menzil, HacimEgrisi egri, int gecenTick) {
        void yaz(FriendlyByteBuf t) {
            t.writeUtf(kimlik, KIMLIK_SINIRI);
            t.writeLong(afetNo);
            t.writeResourceLocation(ses);
            t.writeUtf(kategori.getName(), 32);
            t.writeBoolean(boyut != null);
            if (boyut != null) {
                t.writeResourceLocation(boyut);
                t.writeDouble(x);
                t.writeDouble(y);
                t.writeDouble(z);
                t.writeFloat(menzil);
            }
            t.writeVarInt(egri.noktalar().size());
            for (HacimEgrisi.Nokta nokta : egri.noktalar()) {
                t.writeVarInt(nokta.tick());
                t.writeFloat(nokta.hacim());
            }
            t.writeVarInt(gecenTick);
        }

        static OrtamPaketi oku(FriendlyByteBuf t) {
            String kimlik = t.readUtf(KIMLIK_SINIRI);
            long afetNo = t.readLong();
            ResourceLocation ses = t.readResourceLocation();
            SoundSource kategori = kategori(t.readUtf(32));
            ResourceLocation boyut = null;
            double x = 0;
            double y = 0;
            double z = 0;
            float menzil = 0f;
            if (t.readBoolean()) {
                boyut = t.readResourceLocation();
                x = t.readDouble();
                y = t.readDouble();
                z = t.readDouble();
                menzil = t.readFloat();
            }
            int adet = t.readVarInt();
            if (adet < 1 || adet > HacimEgrisi.EN_COK_NOKTA) {
                throw new IllegalArgumentException("Ortam paketinde geçersiz eğri boyu: " + adet);
            }
            List<HacimEgrisi.Nokta> noktalar = new ArrayList<>(adet);
            for (int i = 0; i < adet; i++) {
                noktalar.add(new HacimEgrisi.Nokta(t.readVarInt(), t.readFloat()));
            }
            return new OrtamPaketi(kimlik, afetNo, ses, kategori, boyut, x, y, z, menzil, new HacimEgrisi(noktalar),
                    Math.max(0, t.readVarInt()));
        }

        static void isle(OrtamPaketi paket, Supplier<NetworkEvent.Context> baglam) {
            ilet(() -> io.github.kadirberkpolat1.dogalafetler.client.uyari.IstemciUyariPaketleri::ortamAlicisi, paket);
        }

        /** Kategori adıyla taşınır (sıra numarası değil, S-01); bilinmeyen ad {@code AMBIENT}. */
        private static SoundSource kategori(String ad) {
            for (SoundSource kaynak : SoundSource.values()) {
                if (kaynak.getName().equals(ad.toLowerCase(Locale.ROOT))) {
                    return kaynak;
                }
            }
            return SoundSource.AMBIENT;
        }
    }

    /** Ortam katmanı {@code fadeTick} içinde sönsün ve kalksın. */
    public record OrtamDurdurPaketi(String kimlik, int fadeTick) {
        void yaz(FriendlyByteBuf t) {
            t.writeUtf(kimlik, KIMLIK_SINIRI);
            t.writeVarInt(fadeTick);
        }

        static OrtamDurdurPaketi oku(FriendlyByteBuf t) {
            return new OrtamDurdurPaketi(t.readUtf(KIMLIK_SINIRI), Math.max(1, Math.min(1200, t.readVarInt())));
        }

        static void isle(OrtamDurdurPaketi paket, Supplier<NetworkEvent.Context> baglam) {
            ilet(() -> io.github.kadirberkpolat1.dogalafetler.client.uyari.IstemciUyariPaketleri::ortamDurdurAlicisi, paket);
        }
    }
}
