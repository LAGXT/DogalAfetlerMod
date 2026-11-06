package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import java.util.Objects;

/**
 * Bir yiyeceğin sıcaklık etkisi tanımı (madde 14, 15). Miktar tanımda değil, kademeye göre config'tedir.
 *
 * @param tur              azalan verim türü: aynı tür 60 sn içinde tekrar yenirse etki azalır (eşya kimliği, ör.
 *                         {@code dogalafetler:sicak_salep}); farklı türler birbirinden bağımsızdır
 * @param yon              ısınma ya da serinleme
 * @param buyukluk         etki kademesi (durum efektinin amplifier'ı da buradan gelir)
 * @param sureSaniye       süreli kısmın süresi; durum efekti de bu kadar görünür
 * @param sicaktaGuclenir  yan etki: vücut ya da ortam sıcakken etki config çarpanıyla büyür (Ateşli Güveç)
 */
public record YiyecekTanimi(String tur, Yon yon, Buyukluk buyukluk, int sureSaniye, boolean sicaktaGuclenir) {
    public static final int EN_UZUN_SANIYE = 3600;

    public YiyecekTanimi {
        Objects.requireNonNull(tur, "tur");
        Objects.requireNonNull(yon, "yon");
        Objects.requireNonNull(buyukluk, "buyukluk");
        if (tur.isBlank()) {
            throw new IllegalArgumentException("tür boş olamaz");
        }
        if (sureSaniye < 1 || sureSaniye > EN_UZUN_SANIYE) {
            throw new IllegalArgumentException("süre 1–" + EN_UZUN_SANIYE + " sn olmalı: " + sureSaniye);
        }
    }

    public int sureTick() {
        return sureSaniye * 20;
    }

    /** Pişmiş buz balığı (madde 15): kendi başına orta ısınma. */
    public static YiyecekTanimi pismisBalik(String tur, int sureSaniye) {
        return new YiyecekTanimi(tur, Yon.ISINMA, Buyukluk.ORTA, sureSaniye, false);
    }

    /** Çiğ buz balığı (madde 15): hafif serinlik. */
    public static YiyecekTanimi cigBalik(String tur, int sureSaniye) {
        return new YiyecekTanimi(tur, Yon.SERINLIK, Buyukluk.KUCUK, sureSaniye, false);
    }
}
