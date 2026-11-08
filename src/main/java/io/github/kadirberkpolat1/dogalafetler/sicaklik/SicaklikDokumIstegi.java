package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * {@code sicaklik/dokum_iste} (C2S, boş): termometre menüsü açıkken istemci 20 tick'te bir yollar (S3-08, sözleşme §A6).
 * Sunucu kapı açıkken ve oyuncu başına en çok {@link #EN_KISA_ARALIK} tick'te bir {@link SicaklikDokumPaketi} ile yanıtlar;
 * kapı kapalıyken ya da sınır aşılınca istek sessizce düşer (analiz çevre taraması yapar, sık istek sunucuyu yormasın).
 */
public record SicaklikDokumIstegi() {
    public static final String AD = "sicaklik/dokum_iste";
    public static final int EN_KISA_ARALIK = 10;

    /** Son yanıtın sunucu tick'i; yeniden doğan oyuncu yeni nesnedir, eskisi kendiliğinden düşer. Yalnız ana thread. */
    private static final Map<ServerPlayer, Integer> SON_YANIT = new WeakHashMap<>();

    static void kaydet() {
        AgKanali.kaydet(k -> k.istemcidenSunucuya(AD, SicaklikDokumIstegi.class, SicaklikDokumIstegi::yaz,
                SicaklikDokumIstegi::oku, SicaklikDokumIstegi::isle));
    }

    public void yaz(FriendlyByteBuf t) {
    }

    public static SicaklikDokumIstegi oku(FriendlyByteBuf t) {
        return new SicaklikDokumIstegi();
    }

    /** Sunucu ana thread'inde. */
    public static void isle(SicaklikDokumIstegi istek, Supplier<NetworkEvent.Context> baglam) {
        ServerPlayer oyuncu = baglam.get().getSender();
        if (oyuncu != null) {
            yanitla(oyuncu);
        }
    }

    /**
     * İsteği karşılar: kapı açık, oyuncu canlı ve son yanıttan beri en az {@link #EN_KISA_ARALIK} tick geçtiyse dökümü kurup
     * oyuncuya yollar ve döner; yoksa {@code null} (hiçbir şey gitmez).
     */
    public static SicaklikDokumPaketi yanitla(ServerPlayer oyuncu) {
        if (!SicaklikKapisi.acikMi(oyuncu.server) || oyuncu.isSpectator() || !oyuncu.isAlive()) {
            return null;
        }
        int simdi = oyuncu.server.getTickCount();
        Integer son = SON_YANIT.get(oyuncu);
        if (son != null && simdi - son < EN_KISA_ARALIK) {
            return null;
        }
        SON_YANIT.put(oyuncu, simdi);
        SicaklikDokumPaketi paket = SicaklikDokumPaketi.analizden(SicaklikYoneticisi.analiz(oyuncu),
                VucutSicakligi.al(oyuncu));
        AgKanali.oyuncuyaGonder(oyuncu, paket);
        return paket;
    }
}
