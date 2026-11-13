package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/**
 * Seviye tablosunun config çarpanları (madde 10 "config'den ayarlanabilir"; SERVER {@code kasirga.*}). Hepsi 1 iken tablo
 * tasarım notu §6'daki gibidir.
 *
 * @param boyut huni çapları, yükseklik, etek ve çekim yarıçapı
 * @param hiz hareket hızı
 * @param sure olgun evrenin süresi
 * @param cekim çekim kuvveti
 * @param blok tick başına blok kaldırma denemesi
 * @param hasar çekirdek hasarı ve enkaz çarpma hasarı
 */
public record Carpanlar(double boyut, double hiz, double sure, double cekim, double blok, double hasar) {
    public static final Carpanlar BIR = new Carpanlar(1, 1, 1, 1, 1, 1);

    public Carpanlar {
        if (!(boyut > 0 && hiz >= 0 && sure > 0 && cekim >= 0 && blok >= 0 && hasar >= 0)) {
            throw new IllegalArgumentException("Kasırga çarpanı geçersiz: " + boyut + ", " + hiz + ", " + sure + ", " + cekim
                    + ", " + blok + ", " + hasar);
        }
    }
}
