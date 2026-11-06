package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import java.util.EnumSet;
import java.util.Set;

/**
 * Faz 4 afetinin başarıma bildirdiği "o afette ayrıca" olaylar (madde 12 #2, #4, #5, #6; {@code BasarimIpucuEvent}). Oyuncu
 * verisinde bit maskesi olarak saklanır ({@link #bit()}): sıra değiştirilmez, yeni ipucu yalnız sona eklenir.
 */
public enum Ipucu {
    /** Tsunami: oyuncu dalga tarafından sürüklendi (#2 Dalga Avcısı). */
    SURUKLENDI,
    /** Sinkhole: oyuncu çökme sırasında çukura düştü (#4 Delikten Dönüş). */
    CUKURA_DUSTU,
    /** Orman yangını: oyuncu bu saniye yanan alanın içinde (saniyede en çok bir sayılır; #5 Yanmadan Çıktım). */
    YANGIN_ALANINDA,
    /** Asit yağmuru: oyuncu afetten hasar aldı (#6 Çatı Altı'yı bozar). */
    HASAR_ALDI;

    public int bit() {
        return 1 << ordinal();
    }

    public static Set<Ipucu> bitlerden(int bitler) {
        Set<Ipucu> sonuc = EnumSet.noneOf(Ipucu.class);
        for (Ipucu ipucu : values()) {
            if ((bitler & ipucu.bit()) != 0) {
                sonuc.add(ipucu);
            }
        }
        return sonuc;
    }
}
