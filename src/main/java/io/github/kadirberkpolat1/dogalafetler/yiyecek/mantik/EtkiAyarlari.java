package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

/**
 * Yiyecek etkilerinin sayıları (madde 14, N17). Miktarlar vücut birimidir: ölçeğin bir yanı 100 olduğu için "orta %15–25"
 * 15–25 birimdir (sıcaklık S-01). Kademe miktarı etkinin toplamıdır: {@code anlikOran} kadarı hemen, kalanı süre
 * boyunca giderek azalan hızla eklenir. Bileşen adları SERVER config anahtarlarıdır ({@code yiyecek.*}).
 *
 * @param anlikOran                  kademe miktarının hemen eklenen payı (0–1)
 * @param azalanVerimPenceresiSaniye aynı tür bu süre içinde tekrar yenirse etki azalır (0 = kapalı)
 * @param azalanVerimOrani           pencere içindeki tekrar yemede etkinin çarpanı (0,5 = %50 azalır)
 * @param yanEtkiCarpani             Ateşli Güveç sıcakta bu kadar güçlü ısıtır (≥ 1)
 * @param yanEtkiEsigi               vücut ya da ortam (vücut birimi) bu değer ve üstündeyse "sıcakta" sayılır
 */
public record EtkiAyarlari(double cokKucuk, double kucuk, double orta, double buyuk, double cokBuyuk, double anlikOran,
        int azalanVerimPenceresiSaniye, double azalanVerimOrani, double yanEtkiCarpani, double yanEtkiEsigi) {
    public static final EtkiAyarlari VARSAYILAN = new EtkiAyarlari(3, 8, 20, 35, 45, 0.25, 60, 0.5, 1.5, 20);

    public EtkiAyarlari {
        double[] kademeler = {cokKucuk, kucuk, orta, buyuk, cokBuyuk};
        for (int i = 0; i < kademeler.length; i++) {
            if (!(kademeler[i] >= 0 && kademeler[i] <= 100)) {
                throw new IllegalArgumentException("kademe miktarı 0–100 olmalı: " + kademeler[i]);
            }
            if (i > 0 && kademeler[i] < kademeler[i - 1]) {
                throw new IllegalArgumentException("kademeler sıralı olmalı: " + cokKucuk + " ≤ " + kucuk + " ≤ " + orta
                        + " ≤ " + buyuk + " ≤ " + cokBuyuk);
            }
        }
        if (!(anlikOran >= 0 && anlikOran <= 1)) {
            throw new IllegalArgumentException("anında oranı 0–1 olmalı: " + anlikOran);
        }
        if (azalanVerimPenceresiSaniye < 0 || azalanVerimPenceresiSaniye > YiyecekTanimi.EN_UZUN_SANIYE) {
            throw new IllegalArgumentException("azalan verim penceresi 0–3600 sn olmalı: " + azalanVerimPenceresiSaniye);
        }
        if (!(azalanVerimOrani >= 0 && azalanVerimOrani <= 1)) {
            throw new IllegalArgumentException("azalan verim oranı 0–1 olmalı: " + azalanVerimOrani);
        }
        if (!(yanEtkiCarpani >= 1 && yanEtkiCarpani <= 5)) {
            throw new IllegalArgumentException("yan etki çarpanı 1–5 olmalı: " + yanEtkiCarpani);
        }
        if (!(yanEtkiEsigi >= -100 && yanEtkiEsigi <= 100)) {
            throw new IllegalArgumentException("yan etki eşiği −100…100 olmalı: " + yanEtkiEsigi);
        }
    }

    /** Kademenin toplam miktarı (vücut birimi, işaretsiz). */
    public double miktar(Buyukluk b) {
        return switch (b) {
            case COK_KUCUK -> cokKucuk;
            case KUCUK -> kucuk;
            case ORTA -> orta;
            case BUYUK -> buyuk;
            case COK_BUYUK -> cokBuyuk;
        };
    }

    public long azalanVerimPenceresiTick() {
        return azalanVerimPenceresiSaniye * 20L;
    }
}
