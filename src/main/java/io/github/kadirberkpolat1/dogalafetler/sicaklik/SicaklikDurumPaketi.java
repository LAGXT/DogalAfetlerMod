package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Esikler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * {@code sicaklik/durum}: HUD ve ekran efektleri için yalnız o oyuncuya giden durum (madde 3, 7d). Değişince (en çok
 * güncelleme sıklığında, varsayılan 1 Hz), kademe değişiminde ve {@code set}'te hemen, ayrıca seyrek tazeleme. {@code acik}
 * false ise sistem kapalıdır: istemci HUD'u ve efektleri gizler. ~40 bayt.
 *
 * @param vucut   vücut sıcaklığı (vücut birimi)
 * @param ortam   ortamın vücut birimi karşılığı (HUD'daki ortam oku)
 * @param egilim  vücudun değişim hızı (birim/sn; ok yönü)
 */
public record SicaklikDurumPaketi(boolean acik, float vucut, float ortam, float egilim, Kademe kademe, float notrBant,
        float soguk, float cokSoguk, float donma, float sicak, float cokSicak, float yanma) {
    public static final String AD = "sicaklik/durum";
    public static final SicaklikDurumPaketi KAPALI = new SicaklikDurumPaketi(false, 0, 0, 0, Kademe.NOTR, 15, -30, -55,
            -75, 30, 55, 75);

    public static SicaklikDurumPaketi acik(float vucut, float ortam, float egilim, Esikler e) {
        return new SicaklikDurumPaketi(true, vucut, ortam, egilim, e.kademe(vucut), (float) e.notrBant(),
                (float) e.soguk(), (float) e.cokSoguk(), (float) e.donmaHasari(), (float) e.sicak(),
                (float) e.cokSicak(), (float) e.yanmaHasari());
    }

    static void kaydet() {
        AgKanali.kaydet(k -> k.sunucudanIstemciye(AD, SicaklikDurumPaketi.class, SicaklikDurumPaketi::yaz,
                SicaklikDurumPaketi::oku, SicaklikDurumPaketi::isle));
    }

    public void yaz(FriendlyByteBuf t) {
        t.writeBoolean(acik);
        t.writeFloat(vucut);
        t.writeFloat(ortam);
        t.writeFloat(egilim);
        t.writeByte(kademe.ordinal());
        t.writeFloat(notrBant);
        t.writeFloat(soguk);
        t.writeFloat(cokSoguk);
        t.writeFloat(donma);
        t.writeFloat(sicak);
        t.writeFloat(cokSicak);
        t.writeFloat(yanma);
    }

    public static SicaklikDurumPaketi oku(FriendlyByteBuf t) {
        return new SicaklikDurumPaketi(t.readBoolean(), t.readFloat(), t.readFloat(), t.readFloat(),
                Kademe.sirayla(t.readByte()), t.readFloat(), t.readFloat(), t.readFloat(), t.readFloat(), t.readFloat(),
                t.readFloat(), t.readFloat());
    }

    /** İstemci ana thread'inde: {@code client.sicaklik.IstemciSicaklik}'e N15 yoluyla (K-C14). */
    public static void isle(SicaklikDurumPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        Consumer<SicaklikDurumPaketi> alici = DistExecutor.safeCallWhenOn(Dist.CLIENT,
                () -> io.github.kadirberkpolat1.dogalafetler.client.sicaklik.IstemciSicaklikPaketleri::durumAlicisi);
        if (alici != null) {
            alici.accept(paket);
        }
    }
}
