package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * {@code -Ddogalafetler.oyunTestiAfeti=deprem,sinkhole} değerinden hangi türlerin oyun testi afeti olarak kaydedileceği
 * (K-E22, K-E23). Gerçek afet kaydı önce yapılır; gerçek afeti kayıtlı tür atlanır, böylece Faz 4 afeti birleşince eski betiklerin
 * bayrağı modu düşürmez, yalnız etkisiz kalır. {@code !} önekli tür ({@code !deprem}) gerçek afeti kayıtlıysa onun yerine geçer:
 * çekirdeğin yaşam döngüsü senaryosu hangi afetler birleşmiş olursa olsun aynı taklitle koşar (yalnız geliştirme sunucusu).
 * Bilinmeyen adlar yok sayılır, tekrarlar bir kez sayılır.
 */
public final class OyunTestiAfetSecimi {
    /** Gerçek kaydın yerine geçmek için tür adının öneki (K-E23). */
    public static final String EZ_ONEKI = "!";

    private OyunTestiAfetSecimi() {
    }

    /**
     * Özellik değerinin seçimi: {@code kaydedilecek} kayıtsız türler, {@code atlanan} zaten kayıtlı (gerçek afet) türler,
     * {@code ezilecek} kayıtlı olup {@code !} ile gerçek kaydın yerine istenen türler.
     */
    public record Secim(List<AfetTuru> kaydedilecek, List<AfetTuru> atlanan, List<AfetTuru> ezilecek) {
    }

    public static Secim sec(String deger, Predicate<AfetTuru> kayitliMi) {
        Map<AfetTuru, Boolean> turler = new LinkedHashMap<>();
        if (deger != null) {
            for (String parca : deger.toLowerCase(Locale.ROOT).split(",")) {
                String ad = parca.trim();
                boolean ez = ad.startsWith(EZ_ONEKI);
                AfetTuru.kimlikIle(ez ? ad.substring(EZ_ONEKI.length()).trim() : ad)
                        .ifPresent(tur -> turler.merge(tur, ez, Boolean::logicalOr));
            }
        }
        List<AfetTuru> kaydedilecek = new ArrayList<>();
        List<AfetTuru> atlanan = new ArrayList<>();
        List<AfetTuru> ezilecek = new ArrayList<>();
        turler.forEach((tur, ez) -> {
            if (!kayitliMi.test(tur)) {
                kaydedilecek.add(tur);
            } else {
                (ez ? ezilecek : atlanan).add(tur);
            }
        });
        return new Secim(List.copyOf(kaydedilecek), List.copyOf(atlanan), List.copyOf(ezilecek));
    }
}
