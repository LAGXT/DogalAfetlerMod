package io.github.kadirberkpolat1.dogalafetler.yangin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * {@code yangin/ates_durumu}: oyuncunun yakınındaki yangının chunk başına özeti (2 Hz, yalnız menzildeki oyunculara) ve
 * yağmurla sönen hücrelerin buhar konumları. Yakın chunk'ların hücreleri {@link #hucre} biçiminde gelir.
 */
public record AtesDurumuPaketi(List<Ozet> ozetler, long[] buharlar) {
    private static final int OZET_TAVANI = 1024;
    private static final int HUCRE_TAVANI = 256;
    private static final int BUHAR_TAVANI = 256;

    /** Chunk özeti: hücre sayısı, hücrelerin ağırlık merkezi, en üst Y ve (yakınsa) hücreler. */
    public record Ozet(int cx, int cz, int sayi, float x, float y, float z, int ustY, int[] hucreler) {
    }

    /** Chunk içi hücre: {@code ((y + 2048) << 8) | (yerelX << 4) | yerelZ}. */
    public static int hucre(int x, int y, int z) {
        return ((y + 2048) << 8) | ((x & 15) << 4) | (z & 15);
    }

    public static int hucreX(int cx, int kod) {
        return (cx << 4) | ((kod >> 4) & 15);
    }

    public static int hucreY(int kod) {
        return (kod >>> 8) - 2048;
    }

    public static int hucreZ(int cz, int kod) {
        return (cz << 4) | (kod & 15);
    }

    public static void yaz(AtesDurumuPaketi paket, FriendlyByteBuf tampon) {
        tampon.writeVarInt(paket.ozetler.size());
        for (Ozet o : paket.ozetler) {
            tampon.writeInt(o.cx);
            tampon.writeInt(o.cz);
            tampon.writeVarInt(o.sayi);
            tampon.writeFloat(o.x);
            tampon.writeFloat(o.y);
            tampon.writeFloat(o.z);
            tampon.writeInt(o.ustY);
            tampon.writeVarIntArray(o.hucreler);
        }
        tampon.writeLongArray(paket.buharlar);
    }

    public static AtesDurumuPaketi oku(FriendlyByteBuf tampon) {
        int adet = tampon.readVarInt();
        if (adet < 0 || adet > OZET_TAVANI) {
            throw new IllegalArgumentException("yangin/ates_durumu: özet sayısı " + adet);
        }
        List<Ozet> ozetler = new ArrayList<>(adet);
        for (int i = 0; i < adet; i++) {
            ozetler.add(new Ozet(tampon.readInt(), tampon.readInt(), tampon.readVarInt(), tampon.readFloat(),
                    tampon.readFloat(), tampon.readFloat(), tampon.readInt(), tampon.readVarIntArray(HUCRE_TAVANI)));
        }
        return new AtesDurumuPaketi(List.copyOf(ozetler), tampon.readLongArray(null, BUHAR_TAVANI));
    }

    /** İstemci ana thread'inde: {@code client.yangin.IstemciYangin}'e N15 yoluyla iletilir. */
    public static void isle(AtesDurumuPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        Consumer<AtesDurumuPaketi> alici = DistExecutor.safeCallWhenOn(Dist.CLIENT,
                () -> io.github.kadirberkpolat1.dogalafetler.client.yangin.IstemciYangin::alici);
        if (alici != null) {
            alici.accept(paket);
        }
    }
}
