package io.github.kadirberkpolat1.dogalafetler.network;

import io.github.kadirberkpolat1.dogalafetler.core.AyDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.DenizSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.core.PulsarYolu;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Dünya döneminin istemcinin bilmesi gereken tam durumu (madde 3, 16): değişimde herkese (toplanarak), girişte ve boyut
 * değişiminde o oyuncuya, ayrıca 30 sn'de bir (istemci saati kaymasın). {@code saat} ölçekli afet saati, {@code hiz} onun
 * çarpanıdır; istemci evre geçişlerini {@code evreBaslangicSaati}'nden, ay çarpmasının yaklaşma/çarpma görünümünü
 * {@code ayOlaySaati}'nden ({@link AyDurumu}) hesaplar. Afet geçmişi ve beklemeler gitmez. {@code bayraklar}: pulsar
 * yolu ({@link PulsarYolu}) ve {@code afetBlokHasari} gamerule'ü (pulsar-2).
 */
public record DonemPaketi(boolean ayVar, GunesEvresi gunesEvresi, float evreIlerlemesi, long evreBaslangicSaati,
        float donmaIlerlemesi, boolean cozulmeDonemi, float buharlasmaDerinligi, DenizSeviyesi deniz, long saat,
        double hiz, long ayOlaySaati, int bayraklar) {
    /** Güneşsiz dönem / kızıl cüce doğuşu pulsar yolunda ({@link PulsarYolu#donusYolu}). */
    public static final int BAYRAK_DONUS_YOLU = 1;
    /** Gamerule {@code afetBlokHasari} açık. */
    public static final int BAYRAK_BLOK_HASARI = 2;
    /** NORMAL'e pulsar yolundan dönüldü ({@link PulsarYolu#gunesDondu}). */
    public static final int BAYRAK_GUNES_DONDU = 4;
    /**
     * Pulsar süreci duraklatıldı ({@link PulsarYolu#durmaVar}, K-C46): ölçekli saat sürer ama yutma saniyesi donar; istemci
     * yutmanın saniyesini {@code evreIlerlemesi}'nden alır (açılınca sunucu evre başlangıcını ileri kaydırır).
     */
    public static final int BAYRAK_DURAKLATILDI = 8;

    /** İstemcinin sunucudan paket gelmeden önceki varsayımı (normal dünya, gamerule varsayılanı). */
    public static final DonemPaketi VARSAYILAN = new DonemPaketi(true, GunesEvresi.NORMAL, 0f, 0L, 0f, false, 0f,
            DenizSeviyesi.VARSAYILAN, 0L, 1.0, AyDurumu.OLAY_YOK, BAYRAK_BLOK_HASARI);

    /** Blok hasarı bilinmeyen yerler için (gamerule varsayılanı: açık). */
    public static DonemPaketi den(DunyaDonemi donem, double hiz) {
        return den(donem, hiz, true);
    }

    public static DonemPaketi den(DunyaDonemi donem, double hiz, boolean blokHasari) {
        int bayraklar = (PulsarYolu.donusYolu(donem) ? BAYRAK_DONUS_YOLU : 0) | (blokHasari ? BAYRAK_BLOK_HASARI : 0)
                | (PulsarYolu.gunesDondu(donem) ? BAYRAK_GUNES_DONDU : 0)
                | (PulsarYolu.durmaVar(donem) ? BAYRAK_DURAKLATILDI : 0);
        return new DonemPaketi(donem.ayVar(), donem.gunesEvresi(), donem.evreIlerlemesi(), donem.evreBaslangicSaati(),
                donem.donmaIlerlemesi(), donem.cozulmeDonemi(), donem.buharlasmaDerinligi(), donem.deniz(), donem.saat(),
                hiz, donem.ayOlaySaati(), bayraklar);
    }

    public AyDurumu ayDurumu() {
        return AyDurumu.den(ayVar, ayOlaySaati);
    }

    public boolean donusYolu() {
        return (bayraklar & BAYRAK_DONUS_YOLU) != 0;
    }

    public boolean blokHasari() {
        return (bayraklar & BAYRAK_BLOK_HASARI) != 0;
    }

    public boolean gunesDondu() {
        return (bayraklar & BAYRAK_GUNES_DONDU) != 0;
    }

    public boolean duraklatildi() {
        return (bayraklar & BAYRAK_DURAKLATILDI) != 0;
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
        tampon.writeLong(ayOlaySaati);
        tampon.writeVarInt(bayraklar);
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
                tampon.readLong(), tampon.readDouble(), Math.max(AyDurumu.OLAY_YOK, tampon.readLong()), tampon.readVarInt());
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
