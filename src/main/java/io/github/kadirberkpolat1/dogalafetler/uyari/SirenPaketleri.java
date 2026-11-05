package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Siren ağ paketleri. Sunucu sireni etkilenebilecek oyunculara gönderir (madde 5); istemci tarafı kendini
 * {@link #istemciyiBagla} ile bağlar. Ortak kod istemci sınıfı import etmez (N15): dedicated server'da bağlı istemci
 * yoktur ve bu paketler zaten yalnız istemciye gider.
 */
public final class SirenPaketleri {
    /** Sireni başlatır; aynı anda çalan siren varsa yenisi eskisini keser. */
    public record SirenPaketi(SirenTuru tur, int sureTick) {
        void yaz(FriendlyByteBuf tampon) {
            tampon.writeEnum(tur);
            tampon.writeVarInt(sureTick);
        }

        static SirenPaketi oku(FriendlyByteBuf tampon) {
            return new SirenPaketi(tampon.readEnum(SirenTuru.class), SirenTuru.sureyiKirp(tampon.readVarInt()));
        }
    }

    /** Çalan sireni türünün fade-out'uyla söndürür (afet aslı etkisine geçtiğinde, madde 5). */
    public record SirenDurdurPaketi() {
        void yaz(FriendlyByteBuf tampon) {
        }

        static SirenDurdurPaketi oku(FriendlyByteBuf tampon) {
            return new SirenDurdurPaketi();
        }
    }

    private static volatile SirenIstemcisi istemci = SirenIstemcisi.YOK;

    private SirenPaketleri() {
    }

    static void kaydet() {
        AgKanali.sunucudanIstemciye(SirenPaketi.class, SirenPaketi::yaz, SirenPaketi::oku,
                (paket, baglam) -> istemci.baslat(paket.tur(), paket.sureTick()));
        AgKanali.sunucudanIstemciye(SirenDurdurPaketi.class, SirenDurdurPaketi::yaz, SirenDurdurPaketi::oku,
                (paket, baglam) -> istemci.durdur());
    }

    /** İstemci kurulumunda çağrılır. */
    public static void istemciyiBagla(SirenIstemcisi yeni) {
        istemci = Objects.requireNonNull(yeni, "yeni");
    }

    /** Spike: etkilenebilecek oyuncular = tüm oyuncular. Faz 2'de çekirdeğin etki alanı listesine gider. */
    public static void herkeseBaslat(SirenTuru tur, int sureTick) {
        AgKanali.herkeseGonder(new SirenPaketi(tur, sureTick));
    }

    public static void herkeseDurdur() {
        AgKanali.herkeseGonder(new SirenDurdurPaketi());
    }
}
