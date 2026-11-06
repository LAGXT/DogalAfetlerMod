package io.github.kadirberkpolat1.dogalafetler.core;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Afet yuvası dışında süren bir dönem süreci (madde 11, bölüm D ve H): süpernova → güneşsiz dönem → donma → kızıl cüce,
 * ay sonrası deniz yükselmesi, mağara suyunun kuruması gibi. Sürecin sahibi olan alt sistem mod kurucusunda kaydeder:
 *
 * <pre>{@code
 * AfetYoneticisi.donemSureciKaydet(new DonmaSureci());
 * }</pre>
 *
 * Çekirdek: {@code /dogalafet stop} aktif afet yokken {@link #aktifMi} olan süreçleri {@link #durdurur}; {@link #afetTuru()}
 * doluysa ve o türün gamerule'ü (ya da {@code dogalAfetlerAcik}) kapalıysa aynı yol saniyede bir çalışır;
 * {@code /dogalafet durum} süren süreçleri listeler; {@code /dogalafet asama ileri} aktif afet yokken süreçlere
 * {@link #asamaIleri} sorar. Ulaşılmış dönem (aysız gökyüzü, donma ilerlemesi, kızıl cüce güneş) geri alınmaz; yalnız
 * tamamlanmamış süreç durur. {@code afetTuru = GUNES_PATLAMASI} olan süreç durdurulunca çekirdek tamamlanmamış
 * {@code BUYUYEN} evresini {@code NORMAL}'e döndürür. Yalnız sunucu ana thread'inde çağrılır; argüman üst dünyadır.
 */
public interface DonemSureci {
    /** Kısa ASCII kimlik (ör. {@code donma}); log ve komut için. */
    String kimlik();

    /** Komut çıktısındaki ad. */
    default Component ad() {
        return Component.literal(kimlik());
    }

    /** Bağlı olduğu afet türü (gamerule bağı); yoksa boş (gamerule ile durmaz, yalnız {@code stop} ile). */
    Optional<AfetTuru> afetTuru();

    /** Süreç şu an sürüyor mu. Durdurulmuş süreç (kalıcı olarak, sahibinin verisinde) {@code false} döner. */
    boolean aktifMi(ServerLevel ustDunya);

    /** Süreci durdurur; bir sonraki {@link #aktifMi} {@code false} dönmelidir. Ulaşılmış dönem alanlarına dokunmaz. */
    void durdur(ServerLevel ustDunya);

    /** {@code /dogalafet asama ileri}: süreci bir sonraki aşamaya geçirip komut çıktısını döner; geçemezse boş. */
    default Optional<Component> asamaIleri(ServerLevel ustDunya) {
        return Optional.empty();
    }
}
