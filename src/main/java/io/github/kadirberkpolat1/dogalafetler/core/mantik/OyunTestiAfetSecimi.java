package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * {@code -Ddogalafetler.oyunTestiAfeti=deprem,sinkhole} değerinden hangi türlerin oyun testi afeti olarak kaydedileceği
 * (K-E22). Gerçek afet kaydı önce yapılır; gerçek afeti kayıtlı tür atlanır, böylece Faz 4 afeti birleşince eski betiklerin
 * bayrağı modu düşürmez, yalnız etkisiz kalır. Bilinmeyen adlar yok sayılır, tekrarlar bir kez sayılır.
 */
public final class OyunTestiAfetSecimi {
    private OyunTestiAfetSecimi() {
    }

    /** Özellik değerinin seçimi: {@code kaydedilecek} kayıtsız türler, {@code atlanan} zaten kayıtlı (gerçek afet) türler. */
    public record Secim(List<AfetTuru> kaydedilecek, List<AfetTuru> atlanan) {
    }

    public static Secim sec(String deger, Predicate<AfetTuru> kayitliMi) {
        Set<AfetTuru> turler = new LinkedHashSet<>();
        if (deger != null) {
            for (String parca : deger.toLowerCase(Locale.ROOT).split(",")) {
                AfetTuru.kimlikIle(parca.trim()).ifPresent(turler::add);
            }
        }
        List<AfetTuru> kaydedilecek = new ArrayList<>();
        List<AfetTuru> atlanan = new ArrayList<>();
        for (AfetTuru tur : turler) {
            (kayitliMi.test(tur) ? atlanan : kaydedilecek).add(tur);
        }
        return new Secim(List.copyOf(kaydedilecek), List.copyOf(atlanan));
    }
}
