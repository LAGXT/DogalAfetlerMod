package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * Afet GameTest'lerinin batch sonu temizliği (K-E15, GP18): {@code succeedWhen}/{@code runAfterDelay} kullanan test başarısız
 * bitince kendi temizliğini çalıştırmaz. Bu yardımcıyla eklenen sahte oyuncu, olay dinleyicisi ve {@code afetBlokHasari}
 * değişikliği, test sınıfının {@code @AfterBatch} metodundan çağrılan {@link #batchSonu} ile geri alınır. Batch'ler sırayla
 * koşar; durum batch'ler arasında taşınmaz. Yalnız GameTest.
 */
public final class OyunTestiTemizligi {
    private static Boolean eskiBlokHasari;
    private static final List<ServerPlayer> OYUNCULAR = new ArrayList<>();
    private static final List<Object> DINLEYICILER = new ArrayList<>();

    private OyunTestiTemizligi() {
    }

    /** Batch sonunda listeden çıkarılan sahte oyuncu ({@link CekirdekOyunTestleri#sahteOyuncu}). */
    public static ServerPlayer oyuncu(GameTestHelper y, double x, double yy, double z) {
        ServerPlayer o = CekirdekOyunTestleri.sahteOyuncu(y, x, yy, z);
        OYUNCULAR.add(o);
        return o;
    }

    /** Batch sonunda kaldırılan olay dinleyicisi ({@code MinecraftForge.EVENT_BUS}). */
    public static <T extends Event> void dinle(Class<T> tur, Consumer<T> dinleyici) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, tur, dinleyici);
        DINLEYICILER.add(dinleyici);
    }

    /** {@code afetBlokHasari}'nı ayarlar; batch'teki ilk çağrıdan önceki değer batch sonunda geri gelir. Önceki değeri döner. */
    public static boolean blokHasari(ServerLevel level, boolean deger) {
        var kural = level.getGameRules().getRule(Kurallar.AFET_BLOK_HASARI);
        boolean eski = kural.get();
        if (eskiBlokHasari == null) {
            eskiBlokHasari = eski;
        }
        kural.set(deger, level.getServer());
        return eski;
    }

    /** Geçen ya da başarısız batch'in sonu: afet durur, kural, dinleyiciler ve sahte oyuncular geri döner. */
    public static void batchSonu(ServerLevel level) {
        AfetYoneticisi.al(level.getServer()).durdur();
        if (eskiBlokHasari != null) {
            level.getGameRules().getRule(Kurallar.AFET_BLOK_HASARI).set(eskiBlokHasari, level.getServer());
            eskiBlokHasari = null;
        }
        DINLEYICILER.forEach(MinecraftForge.EVENT_BUS::unregister);
        DINLEYICILER.clear();
        for (ServerPlayer o : OYUNCULAR) {
            if (level.getServer().getPlayerList().getPlayer(o.getUUID()) == o) {
                level.getServer().getPlayerList().remove(o);
            }
        }
        OYUNCULAR.clear();
    }
}
