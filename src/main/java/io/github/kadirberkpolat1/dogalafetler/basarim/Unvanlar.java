package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.BasarimTuru;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Unvan;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Kozmetik ünvan (N16, A-37): oyuncu verisinde saklanır ve yalnız modun kendi başarım duyurusunda oyuncu adının önünde
 * altın renkli {@code [<ünvan>]} olarak görünür; görünen ad, tab listesi, ölüm ve vanilla duyurular öneksizdir. Mesaj
 * çevrilebilir bileşendir: her istemci kendi dilinde görür. {@code basarim.unvanGoster=false} iken önek eklenmez (veri
 * korunur).
 */
public final class Unvanlar {
    private Unvanlar() {
    }

    /** Önek: {@code [<ünvan>] } (altın). */
    public static MutableComponent onek(Unvan unvan) {
        return Component.literal("[").append(Component.translatable(unvan.ceviriAnahtari())).append("] ")
                .withStyle(ChatFormatting.GOLD);
    }

    public static void ayarla(ServerPlayer oyuncu, Optional<Unvan> unvan) {
        BasarimVerisi.unvanYaz(oyuncu, unvan);
    }

    /** Duyuru anahtarı: {@code chat.dogalafetler.basarim.<gorev|hedef|meydan_okuma>} (argümanlar: önek, ad, başarım). */
    public static String duyuruAnahtari(BasarimTuru tur) {
        return "chat.dogalafetler.basarim." + tur.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Modun başarım duyurusu: {@code [Ünvan] <oyuncu> <başarım>} türün fiiliyle; {@code basarim} vanilla sohbet bileşenidir
     * (çerçeve rengi: görev/hedef yeşil, meydan okuma mor; üzerine gelince açıklama).
     */
    public static Component duyuru(ServerPlayer oyuncu, BasarimTuru tur, Component basarim) {
        Component onek = BasarimAyarlari.unvanGoster()
                ? BasarimVerisi.unvan(oyuncu).<Component>map(Unvanlar::onek).orElse(Component.empty())
                : Component.empty();
        return Component.translatable(duyuruAnahtari(tur), onek, oyuncu.getDisplayName(), basarim);
    }
}
