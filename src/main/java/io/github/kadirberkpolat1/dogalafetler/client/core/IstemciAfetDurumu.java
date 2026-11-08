package io.github.kadirberkpolat1.dogalafetler.client.core;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * İstemcideki aktif afet önbelleği: her tür için sunucudan son gelen {@link AfetDurumPaketi} (AS-8: aynı anda birden çok afet,
 * her türden en çok bir). Oyuncu etki alanının dışındaysa ya da afet yoksa o tür boştur. Yalnız istemci ana thread'inde
 * okunur; ekranda afet uyarısı yazdırmak için kullanılmaz (madde 5).
 */
public final class IstemciAfetDurumu {
    private static final Map<AfetTuru, AfetDurumPaketi> DURUM = new EnumMap<>(AfetTuru.class);
    private static int surum;

    private IstemciAfetDurumu() {
    }

    /** Birincil afet: en erken başlayan (tek afet varken o); türe bağlı okuyucu {@link #aktif(AfetTuru)} kullanır. */
    public static Optional<AfetDurumPaketi> aktif() {
        return DURUM.values().stream().min(Comparator.comparingLong(AfetDurumPaketi::afetNo));
    }

    public static Optional<AfetDurumPaketi> aktif(AfetTuru tur) {
        return Optional.ofNullable(DURUM.get(tur));
    }

    /** Bu oyuncunun gördüğü bütün afetler, başlama sırasıyla. */
    public static List<AfetDurumPaketi> hepsi() {
        List<AfetDurumPaketi> liste = new ArrayList<>(DURUM.values());
        liste.sort(Comparator.comparingLong(AfetDurumPaketi::afetNo));
        return liste;
    }

    /** Her yeni pakette ve sıfırlamada artar. */
    public static int surum() {
        return surum;
    }

    static void guncelle(AfetDurumPaketi paket) {
        if (paket.aktif()) {
            DURUM.put(paket.tur(), paket);
        } else {
            DURUM.remove(paket.tur());
        }
        surum++;
    }

    static void sifirla() {
        DURUM.clear();
        surum++;
    }
}
