package io.github.kadirberkpolat1.dogalafetler.network;

import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Aktif afetin istemciye giden durumu (madde 3: herkes aynı afeti aynı yerde, aynı aşamada görür). Aşama değişiminde ve
 * {@code cekirdek.ag.afetPaketAraligiTick} aralığında (1 Hz) yalnız etki alanı + ek mesafe içindeki oyunculara, sunucu
 * geneli afette herkese gider; ON_UYARI gizlidir (madde 4). Alandan çıkan ve afet biten oyunculara o türün {@link #yok}
 * paketi gider. Aynı anda birden çok afet sürebilir (AS-8, her türden en çok bir); istemci türe göre saklar.
 * {@code merkez} etki alanının güncel merkezidir (hareketli afette yürür; sunucu geneli afette başlangıç merkezi);
 * {@code kalanTick} gerçek tick'tir (-1 bilinmiyor); {@code afetNo} afetin başlangıç saati (yeni afeti ayırmak için);
 * {@code ozel} afetin kendi görüntü parametreleri.
 */
public record AfetDurumPaketi(boolean aktif, AfetTuru tur, int seviye, BlockPos merkez, ResourceLocation boyut,
        float yaricap, boolean global, AfetAsamasi asama, int altAsama, long kalanTick, long afetNo, CompoundTag ozel) {
    /** {@code tur} türünden afet bu oyuncu için yok (bitti ya da alandan çıktı). */
    public static AfetDurumPaketi yok(AfetTuru tur) {
        return new AfetDurumPaketi(false, tur, 0, BlockPos.ZERO, Level.OVERWORLD.location(), 0f, false, AfetAsamasi.BITIS,
                0, -1L, 0L, new CompoundTag());
    }

    /** {@code alan}: afetin bu tick'teki etki alanı (yönetici hatasız okuduğunu doğrular). */
    public static AfetDurumPaketi den(Afet afet, EtkiAlani alan, long kalanGercekTick, CompoundTag ozel) {
        boolean global = alan.global() || afet.tur().sunucuGeneli();
        BlockPos merkez = global ? afet.baglam().merkez() : BlockPos.containing(alan.x(), alan.y(), alan.z());
        return new AfetDurumPaketi(true, afet.tur(), afet.seviye(), merkez, afet.baglam().boyut().location(),
                alan.global() ? Float.POSITIVE_INFINITY : (float) alan.yaricap(), global, afet.asama(), afet.altAsama(),
                kalanGercekTick, afet.baglam().baslangicSaati(), ozel);
    }

    public void yaz(FriendlyByteBuf tampon) {
        tampon.writeBoolean(aktif);
        tampon.writeEnum(tur);
        if (!aktif) {
            return;
        }
        tampon.writeVarInt(seviye);
        tampon.writeBlockPos(merkez);
        tampon.writeResourceLocation(boyut);
        tampon.writeFloat(yaricap);
        tampon.writeBoolean(global);
        tampon.writeEnum(asama);
        tampon.writeVarInt(altAsama);
        tampon.writeLong(kalanTick);
        tampon.writeLong(afetNo);
        tampon.writeNbt(ozel);
    }

    public static AfetDurumPaketi oku(FriendlyByteBuf tampon) {
        boolean aktif = tampon.readBoolean();
        AfetTuru tur = tampon.readEnum(AfetTuru.class);
        if (!aktif) {
            return yok(tur);
        }
        int seviye = tampon.readVarInt();
        BlockPos merkez = tampon.readBlockPos();
        ResourceLocation boyut = tampon.readResourceLocation();
        float yaricap = tampon.readFloat();
        boolean global = tampon.readBoolean();
        AfetAsamasi asama = tampon.readEnum(AfetAsamasi.class);
        int altAsama = tampon.readVarInt();
        long kalan = tampon.readLong();
        long afetNo = tampon.readLong();
        CompoundTag ozel = tampon.readNbt();
        return new AfetDurumPaketi(true, tur, seviye, merkez, boyut, yaricap, global, asama, altAsama, kalan, afetNo,
                ozel == null ? new CompoundTag() : ozel);
    }

    /** İstemci ana thread'inde: istemci önbelleğine ({@code client.core.IstemciAfetDurumu}) N15 yoluyla iletilir. */
    public static void isle(AfetDurumPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        Consumer<AfetDurumPaketi> alici = DistExecutor.safeCallWhenOn(Dist.CLIENT,
                () -> io.github.kadirberkpolat1.dogalafetler.client.core.IstemciCekirdekPaketleri::afetAlicisi);
        if (alici != null) {
            alici.accept(paket);
        }
    }
}
