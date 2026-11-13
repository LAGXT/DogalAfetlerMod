package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import java.util.List;
import java.util.Locale;

/**
 * {@code /dogalafet odul meteor [tür] [adet]} isteği (KOMUTLAR KOM.13, K-E18): kaç Meteorit Parçası ve kaç Kozmik Çekirdek
 * verileceği. Tür {@code parca}, {@code cekirdek} ya da {@code hepsi} (ikisinden {@code adet}'er); argümansız istek ikisinden
 * birer tanedir.
 */
public record OdulIstegi(int parca, int cekirdek) {
    public static final String PARCA = "parca";
    public static final String CEKIRDEK = "cekirdek";
    public static final String HEPSI = "hepsi";
    public static final List<String> TURLER = List.of(PARCA, CEKIRDEK, HEPSI);
    public static final int EN_COK_ADET = 64;
    public static final OdulIstegi VARSAYILAN = new OdulIstegi(1, 1);

    /** {@code tur} büyük/küçük harf duyarsız; bilinmeyen tür ya da 1–{@value #EN_COK_ADET} dışı adet hatadır. */
    public static OdulIstegi coz(String tur, int adet) {
        if (adet < 1 || adet > EN_COK_ADET) {
            throw new IllegalArgumentException("adet 1–" + EN_COK_ADET + " olmalı: " + adet);
        }
        return switch (tur == null ? "" : tur.toLowerCase(Locale.ROOT)) {
            case PARCA -> new OdulIstegi(adet, 0);
            case CEKIRDEK -> new OdulIstegi(0, adet);
            case HEPSI -> new OdulIstegi(adet, adet);
            default -> throw new IllegalArgumentException("bilinmeyen ödül türü: " + tur);
        };
    }

    /** {@code tur} geçerli bir tür adı mı. */
    public static boolean gecerliTur(String tur) {
        return tur != null && TURLER.contains(tur.toLowerCase(Locale.ROOT));
    }
}
