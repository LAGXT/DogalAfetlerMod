package io.github.kadirberkpolat1.dogalafetler.core;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Bir afetin başlangıç bağlamı: tür, seviye (seviyesiz afette 0), merkez, boyut, hedef oyuncu, başlangıç anı (ölçekli afet
 * saati) ve yöneticinin seçtiği ön-uyarı süreleri. {@code onUyariSuresi} erken bölümü de içerir; ilk
 * {@code erkenBildirimSuresi} tick'i yalnız Hassas Sismograf görür. {@code elle}: {@code /dogalafet start} ile
 * başlatıldı (gamerule ve bekleme kurallarını atlar).
 *
 * @param hedefOyuncu sunucu geneli afette ve komutla konuma başlatılan afette {@code null}
 */
public record AfetBaglami(AfetTuru tur, int seviye, BlockPos merkez, ResourceKey<Level> boyut, UUID hedefOyuncu,
        long baslangicSaati, long onUyariSuresi, long erkenBildirimSuresi, boolean elle) {
    public AfetBaglami {
        Objects.requireNonNull(tur, "tur");
        Objects.requireNonNull(merkez, "merkez");
        Objects.requireNonNull(boyut, "boyut");
        if (tur.seviyeli() ? seviye < AfetTuru.EN_DUSUK_SEVIYE || seviye > AfetTuru.EN_YUKSEK_SEVIYE : seviye != 0) {
            throw new IllegalArgumentException(tur.kimlik() + " için geçersiz seviye: " + seviye);
        }
        merkez = merkez.immutable();
        onUyariSuresi = Math.max(0, onUyariSuresi);
        erkenBildirimSuresi = Math.max(0, Math.min(erkenBildirimSuresi, onUyariSuresi));
    }

    public Optional<UUID> hedef() {
        return Optional.ofNullable(hedefOyuncu);
    }
}
