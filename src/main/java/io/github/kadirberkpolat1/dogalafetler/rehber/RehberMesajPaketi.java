package io.github.kadirberkpolat1.dogalafetler.rehber;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Sunucu → istemci: oyuncu karakteri şimdi bu rehber cümlesini söylüyor ({@link RehberMesaji#anahtar()}). Yalnız o oyuncuya
 * gider; yazıyı ve oyuncunun kendi ağız buharını istemci çizer, başkalarının gördüğü buharı sunucu gönderir. Tanınmayan
 * anahtar sessizce yok sayılır.
 */
public record RehberMesajPaketi(String anahtar) {
    private static final int EN_COK_UZUNLUK = 32;

    public void yaz(FriendlyByteBuf tampon) {
        tampon.writeUtf(anahtar, EN_COK_UZUNLUK);
    }

    public static RehberMesajPaketi oku(FriendlyByteBuf tampon) {
        return new RehberMesajPaketi(tampon.readUtf(EN_COK_UZUNLUK));
    }

    /** İstemci ana thread'inde: {@code client.rehber.IstemciRehber}'e iletilir (N15). */
    public static void isle(RehberMesajPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        Consumer<RehberMesajPaketi> alici = DistExecutor.safeCallWhenOn(Dist.CLIENT,
                () -> io.github.kadirberkpolat1.dogalafetler.client.rehber.IstemciRehber::alici);
        if (alici != null) {
            alici.accept(paket);
        }
    }
}
