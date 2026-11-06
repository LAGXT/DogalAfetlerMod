package io.github.kadirberkpolat1.dogalafetler.esya.sismograf;

import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * {@code esya/sismograf}: sismografı taşıyan oyuncuya giden algılama durumu (madde 3, 13b). Durum değişince (en çok
 * {@code esya.sismograf.paketAraligiTick}, varsayılan 1 Hz) yalnız o oyuncuya; sismograf bırakılınca tek {@link #YOK}.
 * Tahmini merkez sunucuda hata payıyla saptırılmıştır; yön ve mesafeyi istemci oyuncunun konumuyla hesaplar. Seviye yalnız
 * Hassas Sismograf'ta gönderilir (madde 10). ~25 bayt.
 *
 * @param yuzde   tahmini şiddet 1–100
 * @param oncu    öncü sarsıntı şiddeti 0–1 (ses, titreme ve dalga genliği)
 * @param titreme sunucunun titreme çarpanı 0–1
 * @param afetNo  afetin başlangıç saati (dalga tohumu)
 */
public record SismografPaketi(boolean tasiyor, boolean hassas, boolean algilandi, AfetTuru tur, AfetAsamasi asama,
        int tahminX, int tahminZ, int yuzde, int seviye, float oncu, float titreme, long afetNo) {
    public static final String AD = "esya/sismograf";
    public static final SismografPaketi YOK = new SismografPaketi(false, false, false, AfetTuru.DEPREM, AfetAsamasi.BITIS,
            0, 0, 0, 0, 0f, 0f, 0L);

    /** Taşınıyor ama sismik afet yok: grafik yalnız zemin gürültüsü çizer. */
    public static SismografPaketi sakin(boolean hassas, float titreme) {
        return new SismografPaketi(true, hassas, false, AfetTuru.DEPREM, AfetAsamasi.BITIS, 0, 0, 0, 0, 0f, titreme, 0L);
    }

    public static void kaydet() {
        AgKanali.kaydet(k -> k.sunucudanIstemciye(AD, SismografPaketi.class, SismografPaketi::yaz, SismografPaketi::oku,
                SismografPaketi::isle));
    }

    public void yaz(FriendlyByteBuf t) {
        t.writeBoolean(tasiyor);
        if (!tasiyor) {
            return;
        }
        t.writeBoolean(hassas);
        t.writeFloat(titreme);
        t.writeBoolean(algilandi);
        if (!algilandi) {
            return;
        }
        t.writeEnum(tur);
        t.writeEnum(asama);
        t.writeVarInt(tahminX);
        t.writeVarInt(tahminZ);
        t.writeVarInt(yuzde);
        t.writeVarInt(seviye);
        t.writeFloat(oncu);
        t.writeLong(afetNo);
    }

    public static SismografPaketi oku(FriendlyByteBuf t) {
        if (!t.readBoolean()) {
            return YOK;
        }
        boolean hassas = t.readBoolean();
        float titreme = t.readFloat();
        if (!t.readBoolean()) {
            return sakin(hassas, titreme);
        }
        return new SismografPaketi(true, hassas, true, t.readEnum(AfetTuru.class), t.readEnum(AfetAsamasi.class),
                t.readVarInt(), t.readVarInt(), t.readVarInt(), t.readVarInt(), t.readFloat(), titreme, t.readLong());
    }

    /** İstemci ana thread'inde: {@code client.esya.sismograf.IstemciSismograf}'a N15 yoluyla (K-C14). */
    public static void isle(SismografPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        Consumer<SismografPaketi> alici = DistExecutor.safeCallWhenOn(Dist.CLIENT,
                () -> io.github.kadirberkpolat1.dogalafetler.client.esya.sismograf.IstemciSismograf::alici);
        if (alici != null) {
            alici.accept(paket);
        }
    }
}
