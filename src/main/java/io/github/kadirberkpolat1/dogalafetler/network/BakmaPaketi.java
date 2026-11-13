package io.github.kadirberkpolat1.dogalafetler.network;

import io.github.kadirberkpolat1.dogalafetler.gokyuzu.BakmaIlerlemesi;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * {@code gokyuzu/bakma} (C2S): istemci gökyüzünde bir bakma görevini tamamlayınca bir kez yollar (spam yok: devasa kara
 * delik için 5 sn kesintisiz, her gezegen için toplam 3 sn). {@code tur}: {@code BakmaKurallari.TUR_DEVASA} ya da
 * {@code TUR_GEZEGEN}; {@code hedef}: gezegen numarası (devasa için 0). Sunucu {@link BakmaIlerlemesi} ile biriktirir ve
 * başarımı verir.
 */
public record BakmaPaketi(int tur, int hedef) {
    public static final String AD = "gokyuzu/bakma";

    public void yaz(FriendlyByteBuf tampon) {
        tampon.writeByte(tur);
        tampon.writeByte(hedef);
    }

    public static BakmaPaketi oku(FriendlyByteBuf tampon) {
        return new BakmaPaketi(tampon.readUnsignedByte(), tampon.readUnsignedByte());
    }

    /** Sunucu ana thread'inde. */
    public static void isle(BakmaPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        ServerPlayer oyuncu = baglam.get().getSender();
        if (oyuncu != null) {
            BakmaIlerlemesi.isle(oyuncu, paket.tur(), paket.hedef());
        }
    }
}
