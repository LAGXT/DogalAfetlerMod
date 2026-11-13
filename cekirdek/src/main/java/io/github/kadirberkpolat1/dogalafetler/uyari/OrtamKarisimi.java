package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Eşzamanlı ortam katmanı tavanı (saf mantık, ses.md §4 kural 3 ve §5 performans tablosu): istemcide en çok
 * {@link #tavan(EfektKalitesi)} katman, aynı ses dosyasından en çok {@value #TUR_TAVANI} katman çalar; fazlası en sessizden
 * düşer. Duyulmayan (düzeyi 0) katman kanal tutmaz.
 */
public final class OrtamKarisimi {
    public static final int TUR_TAVANI = 6;

    /** {@code kimlik} katmanın benzersiz adı, {@code ses} ses olayının kimliği, {@code duzey} o anki etkin düzey. */
    public record Aday(String kimlik, String ses, float duzey) {
    }

    private OrtamKarisimi() {
    }

    public static int tavan(EfektKalitesi kalite) {
        return switch (kalite) {
            case DUSUK -> 8;
            case ORTA -> 12;
            case YUKSEK -> 16;
            case ULTRA -> 20;
        };
    }

    /** Çalmasına izin verilen katmanların kimlikleri (düzeye göre azalan; eşitlikte kimliğe göre, belirlenimci). */
    public static Set<String> secilenler(List<Aday> adaylar, int tavan, int turTavani) {
        List<Aday> sirali = new ArrayList<>(adaylar);
        sirali.sort(Comparator.comparing(Aday::duzey).reversed().thenComparing(Aday::kimlik));
        Set<String> secilen = new HashSet<>();
        Map<String, Integer> turSayisi = new HashMap<>();
        for (Aday aday : sirali) {
            if (secilen.size() >= tavan) {
                break;
            }
            if (aday.duzey() <= 0f) {
                continue;
            }
            int sayi = turSayisi.getOrDefault(aday.ses(), 0);
            if (sayi >= turTavani) {
                continue;
            }
            turSayisi.put(aday.ses(), sayi + 1);
            secilen.add(aday.kimlik());
        }
        return secilen;
    }
}
