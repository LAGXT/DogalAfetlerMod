package io.github.kadirberkpolat1.dogalafetler.core;

import java.util.List;
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
 * Durdurma kaydı çekirdektedir ({@link DunyaDonemi}, kalıcı; K-C41): {@code /dogalafet stop} aktif afet yokken süren
 * süreçleri {@link DurdurmaNedeni#KOMUT} ile durdurur, {@link #afetTuru()} doluysa ve o türün gamerule'ü (ya da
 * {@code dogalAfetlerAcik}) kapalıysa saniyede bir {@link DurdurmaNedeni#KURAL} ile durdurur. Kural ile duran süreç kural
 * yeniden açılınca kaldığı yerden sürer ({@link #devamEt}); {@code stop} ile duran kendiliğinden sürmez, ama
 * {@code /dogalafet asama ileri} durmuş süreci de ilerletir. Sahibi ilerlemeden önce
 * {@link AfetYoneticisi#surecDurdu} sorar, yeni bir döngü başlatırken {@link AfetYoneticisi#donemSureciBasladi} çağırır
 * (önceki döngünün kaydı silinir); {@link #aktifMi} false olunca çekirdek kaydı kendisi de siler. Ulaşılmış dönem
 * (aysız gökyüzü, donma ilerlemesi, kızıl cüce güneş) geri alınmaz. Yalnız sunucu ana thread'inde çağrılır; argüman
 * üst dünyadır.
 */
public interface DonemSureci {
    /** Çekirdeğin süreci durdurma nedeni. */
    enum DurdurmaNedeni {
        /** {@code /dogalafet stop} (ya da {@code sifirla}): kendiliğinden sürmez. */
        KOMUT,
        /** Bağlı afet türünün gamerule'ü ya da {@code dogalAfetlerAcik} kapandı: kural açılınca sürer. */
        KURAL
    }

    /** Kısa ASCII kimlik (ör. {@code donma}); log, komut ve durdurma kaydı için. Benzersiz ve kalıcı olmalı. */
    String kimlik();

    /** Komut çıktısındaki ad. */
    default Component ad() {
        return Component.literal(kimlik());
    }

    /** Bağlı olduğu afet türü (gamerule bağı); yoksa boş (gamerule ile durmaz, yalnız {@code stop} ile). */
    Optional<AfetTuru> afetTuru();

    /**
     * Süreç başlamış ve tamamlanmamış mı (sahibinin kendi durumu). Çekirdeğin durdurması bunu değiştirmez: durdurulmuş
     * ama bitmemiş süreç {@code true} döner.
     */
    boolean aktifMi(ServerLevel ustDunya);

    /** Çekirdek süreci durdurdu (kayıt çekirdekte); sahibi geçici etkilerini bırakır. Ulaşılmış dönem alanlarına dokunmaz. */
    default void durdur(ServerLevel ustDunya, DurdurmaNedeni neden) {
    }

    /** Kural yeniden açıldı: kural ile duran süreç kaldığı yerden sürer. */
    default void devamEt(ServerLevel ustDunya) {
    }

    /**
     * {@code /dogalafet asama ileri}: süreci bir sonraki aşamaya geçirip komut çıktısını döner; geçemezse boş. Durdurulmuş
     * süreçte de çağrılır; süreç durmuş kalır (yalnız bir aşama geçer).
     */
    default Optional<Component> asamaIleri(ServerLevel ustDunya) {
        return Optional.empty();
    }

    /** {@code /dogalafet durum}'da sürecin kendi satırı (aşama ve kalan süre); yoksa boş. Yalnız aktif süreçte sorulur. */
    default Optional<Component> durumSatiri(ServerLevel ustDunya) {
        return Optional.empty();
    }

    /** {@code asama ileri <süreç>} argümanı bu süreci seçiyor mu: kimlik ya da o an geçerli bir takma ad. */
    default boolean kimlikEsler(String ad) {
        return kimlik().equals(ad);
    }

    /** O an geçerli takma adlar (komut önerisi; {@link #kimlikEsler} bunları kabul eder). */
    default List<String> takmaAdlar(ServerLevel ustDunya) {
        return List.of();
    }
}
