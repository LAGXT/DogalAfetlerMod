package io.github.kadirberkpolat1.dogalafetler.network;

import io.github.kadirberkpolat1.dogalafetler.core.DenizSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Dünya döneminin istemcinin bilmesi gereken tam durumu (madde 3, 16): değişimde herkese (toplanarak), girişte ve boyut
 * değişiminde o oyuncuya, ayrıca 30 sn'de bir (istemci saati kaymasın). {@code saat} ölçekli afet saati, {@code hiz} onun
 * çarpanıdır; istemci evre geçişlerini {@code evreBaslangicSaati}'nden hesaplar. Afet geçmişi ve beklemeler gitmez.
 */
public record DonemPaketi(boolean ayVar, GunesEvresi gunesEvresi, float evreIlerlemesi, long evreBaslangicSaati,
        float donmaIlerlemesi, boolean cozulmeDonemi, float buharlasmaDerinligi, DenizSeviyesi deniz, long saat,
        double hiz) {
    /** İstemcinin sunucudan paket gelmeden önceki varsayımı (normal dünya). */
    public static final DonemPaketi VARSAYILAN = new DonemPaketi(true, GunesEvresi.NORMAL, 0f, 0L, 0f, false, 0f,
            DenizSeviyesi.VARSAYILAN, 0L, 1.0);

    public static DonemPaketi den(DunyaDonemi donem, double hiz) {
        return new DonemPaketi(donem.ayVar(), donem.gunesEvresi(), donem.evreIlerlemesi(), donem.evreBaslangicSaati(),
                donem.donmaIlerlemesi(), donem.cozulmeDonemi(), donem.buharlasmaDerinligi(), donem.deniz(), donem.saat(),
                hiz);
    }

    public void yaz(FriendlyByteBuf tampon) {
        tampon.writeBoolean(ayVar);
        tampon.writeEnum(gunesEvresi);
        tampon.writeFloat(evreIlerlemesi);
        tampon.writeLong(evreBaslangicSaati);
        tampon.writeFloat(donmaIlerlemesi);
        tampon.writeBoolean(cozulmeDonemi);
        tampon.writeFloat(buharlasmaDerinligi);
        tampon.writeFloat(deniz.hedefArtis());
        tampon.writeFloat(deniz.mevcutArtis());
        tampon.writeLong(deniz.baslangicSaati());
        tampon.writeBoolean(deniz.kalici());
        tampon.writeLong(saat);
        tampon.writeDouble(hiz);
    }

    public static DonemPaketi oku(FriendlyByteBuf tampon) {
        boolean ayVar = tampon.readBoolean();
        GunesEvresi evre = tampon.readEnum(GunesEvresi.class);
        float ilerleme = tampon.readFloat();
        long evreBaslangic = tampon.readLong();
        float donma = tampon.readFloat();
        boolean cozulme = tampon.readBoolean();
        float buharlasma = tampon.readFloat();
        DenizSeviyesi deniz = new DenizSeviyesi(tampon.readFloat(), tampon.readFloat(), tampon.readLong(),
                tampon.readBoolean());
        return new DonemPaketi(ayVar, evre, ilerleme, evreBaslangic, donma, cozulme, buharlasma, deniz,
                tampon.readLong(), tampon.readDouble());
    }

    /** İstemci ana thread'inde: istemci önbelleğine ({@code client.core.IstemciDonem}) N15 yoluyla iletilir. */
    public static void isle(DonemPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        Consumer<DonemPaketi> alici = DistExecutor.safeCallWhenOn(Dist.CLIENT,
                () -> io.github.kadirberkpolat1.dogalafetler.client.core.IstemciCekirdekPaketleri::donemAlicisi);
        if (alici != null) {
            alici.accept(paket);
        }
    }
}
