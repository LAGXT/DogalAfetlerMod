package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/**
 * Afet numarası (afetNo, AS-16): {@code AfetBaglami.baslangicSaati} olarak taşınır; siren, ortam sesi, yangın kaynağı, başarım
 * izi ve istemci paketi afeti bununla ayırır. Ölçekli afet saatine eşit ya da büyüktür ve dünyada kesin artar: aynı tick'te
 * başlayan iki afet farklı numara alır. Sayaç {@value #ADIM}'şer ilerler; afet {@code no + 1}'i kendi alt kimliği için
 * kullanabilir (tsunami ikinci dalgası). Saf Java'dır.
 */
public final class AfetNumarasi {
    public static final int ADIM = 2;

    private AfetNumarasi() {
    }

    /** {@code sonraki}: dönemde saklanan sıradaki en küçük numara. */
    public static long ver(long saat, long sonraki) {
        return Math.max(saat, sonraki);
    }

    /** Verilen numaradan sonra saklanacak sıradaki en küçük numara. */
    public static long sonraki(long verilen) {
        return verilen + ADIM;
    }

    /** Sayaçsız eski kayıt: süren afetlerin numarası kayıttaki saatten büyük olamaz, sayaç onun bir adım ötesinden başlar. */
    public static long eskiKayittan(long saat) {
        return saat + ADIM;
    }
}
