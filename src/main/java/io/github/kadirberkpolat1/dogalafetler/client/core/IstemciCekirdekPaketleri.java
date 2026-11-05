package io.github.kadirberkpolat1.dogalafetler.client.core;

import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import java.util.function.Consumer;

/**
 * Çekirdek paketlerinin istemci alıcıları. Paket işleyicisi bunları {@code DistExecutor.safeCallWhenOn} ile alır
 * ({@code safeRunWhenOn} parametre taşıyamaz: Forge geliştirme ortamında yakalayan lambda'yı reddeder).
 */
public final class IstemciCekirdekPaketleri {
    private IstemciCekirdekPaketleri() {
    }

    public static Consumer<DonemPaketi> donemAlicisi() {
        return IstemciDonem::guncelle;
    }

    public static Consumer<AfetDurumPaketi> afetAlicisi() {
        return IstemciAfetDurumu::guncelle;
    }
}
