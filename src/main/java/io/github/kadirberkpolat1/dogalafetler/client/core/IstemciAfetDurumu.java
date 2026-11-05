package io.github.kadirberkpolat1.dogalafetler.client.core;

import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import java.util.Optional;

/**
 * İstemcideki aktif afet önbelleği: sunucudan son gelen {@link AfetDurumPaketi}. Oyuncu etki alanının dışındaysa ya da afet
 * yoksa boştur. Yalnız istemci ana thread'inde okunur; ekranda afet uyarısı yazdırmak için kullanılmaz (madde 5).
 */
public final class IstemciAfetDurumu {
    private static AfetDurumPaketi durum = AfetDurumPaketi.YOK;
    private static int surum;

    private IstemciAfetDurumu() {
    }

    public static Optional<AfetDurumPaketi> aktif() {
        return durum.aktif() ? Optional.of(durum) : Optional.empty();
    }

    /** Her yeni pakette ve sıfırlamada artar. */
    public static int surum() {
        return surum;
    }

    static void guncelle(AfetDurumPaketi paket) {
        durum = paket;
        surum++;
    }

    static void sifirla() {
        durum = AfetDurumPaketi.YOK;
        surum++;
    }
}
