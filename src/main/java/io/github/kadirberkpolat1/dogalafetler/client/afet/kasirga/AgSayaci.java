package io.github.kadirberkpolat1.dogalafetler.client.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.stream.Collectors;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;

/**
 * Spike ağ ölçümü: istemciye gelen paketleri sınıfına göre sayar ve her paketi bir kez {@link FriendlyByteBuf}'a
 * yazarak boyutunu ölçer (tek oyunculuda yerel kanal paketleri seri hale getirmeden taşır; bu yüzden bayt sayısı
 * dedicated sunucudaki yükün sıkıştırmasız karşılığıdır). Yalnız ölçüm penceresi açıkken sayar.
 */
final class AgSayaci extends ChannelInboundHandlerAdapter {
    private static final String AD = "dogalafetler_ag_sayaci";
    private static final Map<String, AtomicLongArray> SAYAC = new ConcurrentHashMap<>();
    static volatile boolean olcuyor;

    static void tak(Connection baglanti) {
        ChannelPipeline boru = baglanti.channel().pipeline();
        if (boru.get(AD) != null) {
            return;
        }
        try {
            boru.addBefore("packet_handler", AD, new AgSayaci());
            DogalAfetler.LOGGER.info("[spike-kasirga] ağ sayacı takıldı: {}", boru.names());
        } catch (RuntimeException hata) {
            DogalAfetler.LOGGER.warn("[spike-kasirga] ağ sayacı takılamadı: {} ({})", hata.toString(), boru.names());
        }
    }

    static void sifirla() {
        SAYAC.clear();
    }

    /** Pencere boyunca saniye başına paket ve bayt; varlık paketleri ayrıca, en çok bayt taşıyan 8 sınıf. */
    static String ozet(double saniye) {
        long paket = 0;
        long bayt = 0;
        long varlikPaket = 0;
        long varlikBayt = 0;
        for (Map.Entry<String, AtomicLongArray> g : SAYAC.entrySet()) {
            paket += g.getValue().get(0);
            bayt += g.getValue().get(1);
            if (varlikPaketi(g.getKey())) {
                varlikPaket += g.getValue().get(0);
                varlikBayt += g.getValue().get(1);
            }
        }
        String enCok = SAYAC.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, AtomicLongArray> g) -> g.getValue().get(1)).reversed())
                .limit(8)
                .map(g -> String.format("%s=%.1f/sn,%.0fB/sn", g.getKey(), g.getValue().get(0) / saniye,
                        g.getValue().get(1) / saniye))
                .collect(Collectors.joining(" "));
        return String.format("toplam %.1f paket/sn %.0f B/sn; varlık paketleri %.1f paket/sn %.0f B/sn; [%s]",
                paket / saniye, bayt / saniye, varlikPaket / saniye, varlikBayt / saniye, enCok);
    }

    private static boolean varlikPaketi(String ad) {
        return ad.contains("MoveEntity") || ad.contains("TeleportEntity") || ad.contains("SetEntityData")
                || ad.contains("AddEntity") || ad.contains("RemoveEntities") || ad.contains("SetEntityMotion")
                || ad.contains("RotateHead");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object mesaj) throws Exception {
        if (olcuyor && mesaj instanceof Packet<?> paket) {
            say(paket);
        }
        super.channelRead(ctx, mesaj);
    }

    private static void say(Packet<?> paket) {
        if (paket instanceof ClientboundBundlePacket demet) {
            demet.subPackets().forEach(AgSayaci::say);
            return;
        }
        String ad = paket.getClass().getName();
        ad = ad.substring(ad.lastIndexOf('.') + 1);
        AtomicLongArray sayac = SAYAC.computeIfAbsent(ad, k -> new AtomicLongArray(2));
        sayac.incrementAndGet(0);
        sayac.addAndGet(1, boyut(paket));
    }

    /** Paket kimliği (1 bayt VarInt) + gövde; yazılamayan paket 0 sayılır. */
    private static int boyut(Packet<?> paket) {
        FriendlyByteBuf tampon = new FriendlyByteBuf(Unpooled.buffer());
        try {
            paket.write(tampon);
            return tampon.writerIndex() + 1;
        } catch (RuntimeException hata) {
            return 0;
        } finally {
            tampon.release();
        }
    }
}
