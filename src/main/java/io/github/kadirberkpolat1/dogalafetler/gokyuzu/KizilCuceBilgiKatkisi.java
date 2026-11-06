package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * {@code /dogalafet kizilcuce bilgi} çıktısına başka alt sistemin eklediği satırlar (KOMUTLAR: "oyuncunun bulunduğu
 * chunk'ın kalıcı kış bölgesi olup olmadığı" donma işçisinindir). Mod kurucusunda kaydedilir:
 *
 * <pre>{@code
 * KizilCuceBilgiKatkisi.ekle(kaynak -> List.of(Component.translatable("...kis_bolgesi", ...)));
 * }</pre>
 *
 * Hiç katkı yoksa komut "donma sistemi yüklü değil" satırını yazar.
 */
@FunctionalInterface
public interface KizilCuceBilgiKatkisi {
    /** Komutu yazan kaynağın (oyuncu olabilir) konumuna göre satırlar; sunucu ana thread'inde çağrılır. */
    List<Component> satirlar(CommandSourceStack kaynak);

    static void ekle(KizilCuceBilgiKatkisi katki) {
        GokyuzuKomutlari.katkiEkle(katki);
    }
}
