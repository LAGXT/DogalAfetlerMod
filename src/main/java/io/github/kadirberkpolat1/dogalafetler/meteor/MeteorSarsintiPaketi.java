package io.github.kadirberkpolat1.dogalafetler.meteor;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * {@code meteor/sarsinti}: bir çarpma (sunucu → istemci, 256 blok içindeki oyunculara, çarpma başına bir kez). İstemci
 * çarpma parçacıklarını, parlamayı ve {@code siddet} &gt; 0 ise ekran sarsıntısını o oyuncuya özgü çalar (madde 3).
 *
 * @param siddet bu oyuncunun sarsıntı şiddeti (0–1; mesafeyle, {@code Sarsinti.siddet})
 */
public record MeteorSarsintiPaketi(double x, double y, double z, byte boyut, byte gorunum, boolean suya, float siddet) {
    public static void yaz(MeteorSarsintiPaketi p, FriendlyByteBuf t) {
        t.writeDouble(p.x);
        t.writeDouble(p.y);
        t.writeDouble(p.z);
        t.writeByte(p.boyut);
        t.writeByte(p.gorunum);
        t.writeBoolean(p.suya);
        t.writeFloat(p.siddet);
    }

    public static MeteorSarsintiPaketi oku(FriendlyByteBuf t) {
        return new MeteorSarsintiPaketi(t.readDouble(), t.readDouble(), t.readDouble(), t.readByte(), t.readByte(),
                t.readBoolean(), Math.max(0f, Math.min(1f, t.readFloat())));
    }

    public MeteorBoyutu boyutu() {
        return MeteorBoyutu.kimlik(boyut);
    }

    public MeteorGorunumu gorunumu() {
        return MeteorGorunumu.kimlik(gorunum);
    }

    /** İstemci ana thread'inde: {@code client.meteor.IstemciMeteor}'a N15 yoluyla iletilir. */
    public static void isle(MeteorSarsintiPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        Consumer<MeteorSarsintiPaketi> alici = DistExecutor.safeCallWhenOn(Dist.CLIENT,
                () -> io.github.kadirberkpolat1.dogalafetler.client.meteor.IstemciMeteor::alici);
        if (alici != null) {
            alici.accept(paket);
        }
    }
}
