package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.LootAgirliklari.Kategori;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;

/**
 * Kutup Oltası buz deliğinde çok nadir bir şey (nadir, hazine ya da özel katman) tutacaksa şamandıra (olta ucu) kırmızı yerine
 * sarı çizilir. Katman bekleme başında sunucuda seçilir ({@link BuzBalikciligi}); istemciye vanilla'nın kendi varlık
 * senkronuyla gider: {@code FishingHook} üzerine özel ad olarak {@link #ISARET} yazılır (ad etiketi çizilmez: şamandıra
 * işaret edilemez, çünkü görünmez), istemci çizicisi ({@code client.balikcilik.SariSamandiraCizici}) bunu okur. Yeni ağ paketi yoktur;
 * sonradan izlemeye başlayan oyuncu da işareti alır.
 */
public final class NadirSamandira {
    /** Özel ad olarak yazılan işaret (oyuncuya hiç gösterilmez). */
    public static final String ISARET = "dogalafetler:nadir_samandira";

    private NadirSamandira() {
    }

    /** Bu katman çok nadir sayılır mı: nadir balık, hazine (büyülü kitap) ve özel kutup balığı. */
    public static boolean nadirMi(@Nullable Kategori kategori) {
        return kategori == Kategori.NADIR || kategori == Kategori.HAZINE || kategori == Kategori.OZEL;
    }

    /** İşareti yazar ya da siler; yalnız değiştiğinde yazar (gereksiz senkron yok). Sunucu tarafı. */
    static void isaretle(FishingHook olta, boolean nadir) {
        if (isaretli(olta) != nadir) {
            olta.setCustomName(nadir ? Component.literal(ISARET) : null);
        }
    }

    /** Şamandıra nadir bir şey için işaretli mi (iki tarafta da çalışır). */
    public static boolean isaretli(Entity olta) {
        Component ad = olta.getCustomName();
        return ad != null && ISARET.equals(ad.getString());
    }
}
