package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * GameTest filtresi (K-E20): {@value #OZELLIK} sistem özelliğindeki Java regex'i, bir testin sınıfının basit adı, batch
 * adı ya da test fonksiyonunun adından birinde {@link java.util.regex.Matcher#find() find} ile uyarsa test koşar. Eşleme
 * büyük/küçük harfe duyarlıdır (sınıf ve metot adları kaynaktaki yazımıyla verilir).
 */
public final class OyunTestiFiltresi {
    public static final String OZELLIK = "dogalafetler.gametest.filtre";

    private final Pattern desen;

    private OyunTestiFiltresi(Pattern desen) {
        this.desen = desen;
    }

    /**
     * Özellik değerinden filtre. Değer yoksa ya da boşsa filtre yoktur (bütün testler koşar). Geçersiz regex açık hata
     * verir: sessizce bütün testleri koşmak yerine GameTest sunucusu açılmaz.
     */
    public static Optional<OyunTestiFiltresi> coz(String deger) {
        if (deger == null || deger.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new OyunTestiFiltresi(Pattern.compile(deger)));
        } catch (PatternSyntaxException hata) {
            throw new IllegalArgumentException(OZELLIK + " geçersiz regex: '" + deger + "' (" + hata.getDescription()
                    + ", konum " + hata.getIndex() + ")", hata);
        }
    }

    public String desen() {
        return desen.pattern();
    }

    public boolean uyar(String sinifAdi, String batchAdi, String testAdi) {
        return desen.matcher(sinifAdi).find() || desen.matcher(batchAdi).find() || desen.matcher(testAdi).find();
    }
}
