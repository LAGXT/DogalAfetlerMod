package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * Afet yokken bir sonraki doğal afet seçimine kalan süre ({@code /dogalafet durum}, sıklık bildirimi). Zamanlayıcının
 * kendi kuralları kullanılır: başlangıç ve genel bekleme bitene dek seçim yok; sonra her uygun tür kendi beklemesi
 * bittiğinde oranıyla ({@link AfetZamanlayicisi#turOrani}) Poisson sürecine katılır. Toplam oran parça parça sabit olduğu
 * için ortalama ve yüzdelik kapalı biçimde hesaplanır. İlk afet garantisi (AS-12) varsa seçim en geç garanti anında (ya da
 * o anda hiçbir tür beklemeden çıkmamışsa ilk çıkanın anında) olur. Süreler ölçekli afet saati tick'idir; geçici çarpanlar
 * bugünkü değerleriyle sabit sayılır.
 *
 * @param enErken  seçimin mümkün olduğu ilk an (bekleme bitişi)
 * @param ortalama beklenen seçim anı
 * @param enGec    seçimin {@value #UST_YUZDELIK} olasılıkla gerçekleşmiş olacağı an
 */
public record SonrakiAfetTahmini(long enErken, long ortalama, long enGec) {
    /** Aralığın üst ucu: afet bu süre içinde %90 olasılıkla seçilir. */
    public static final double UST_YUZDELIK = 0.9;

    private record Katilim(long an, double oranTick) {
    }

    /** Garantisiz tahmin. */
    public static Optional<SonrakiAfetTahmini> hesapla(ZamanlayiciAyarlari ayarlar, long saat, BeklemeDurumu bekleme,
            Set<AfetTuru> uygunTurler, ToDoubleFunction<AfetTuru> carpan, double siklik) {
        return hesapla(ayarlar, saat, bekleme, uygunTurler, carpan, siklik, Long.MAX_VALUE);
    }

    /**
     * Uygun tür yoksa, sıklık 0 ise ya da bütün oranlar 0 ise boş (doğal afet seçilmez). {@code garantiKalan}: şimdiden ilk
     * afet garantisine kalan tick ({@link Long#MAX_VALUE}: garanti yok).
     */
    public static Optional<SonrakiAfetTahmini> hesapla(ZamanlayiciAyarlari ayarlar, long saat, BeklemeDurumu bekleme,
            Set<AfetTuru> uygunTurler, ToDoubleFunction<AfetTuru> carpan, double siklik, long garantiKalan) {
        long baslangic = Math.max(0, AfetZamanlayicisi.beklemeOlcekle(ayarlar.baslangicBeklemeTick(), siklik) - saat);
        long kapali = Math.max(baslangic, bekleme.genelKalan(saat));
        List<Katilim> katilimlar = new ArrayList<>();
        for (AfetTuru tur : uygunTurler) {
            double c = carpan.applyAsDouble(tur);
            if (!(c > 0) || Double.isInfinite(c)) {
                continue;
            }
            double oran = AfetZamanlayicisi.turOrani(ayarlar, tur, c, siklik);
            if (oran > 0) {
                katilimlar.add(new Katilim(Math.max(kapali, bekleme.turKalan(tur, saat)),
                        oran / AfetZamanlayicisi.GUN_TICK));
            }
        }
        if (katilimlar.isEmpty()) {
            return Optional.empty();
        }
        katilimlar.sort(Comparator.comparingLong(Katilim::an));
        double garanti = garantiKalan == Long.MAX_VALUE ? Double.POSITIVE_INFINITY
                : Math.max(Math.max(0, garantiKalan), katilimlar.get(0).an());
        double hedef = -Math.log(1.0 - UST_YUZDELIK);
        double birikim = 0;
        double egim = 0;
        double ortalama = katilimlar.get(0).an();
        double enGec = Double.NaN;
        for (int i = 0; i < katilimlar.size(); i++) {
            egim += katilimlar.get(i).oranTick();
            double bas = katilimlar.get(i).an();
            if (bas >= garanti) {
                break;
            }
            double son = i + 1 < katilimlar.size() ? katilimlar.get(i + 1).an() : Double.POSITIVE_INFINITY;
            double uzunluk = Math.min(son, garanti) - bas;
            if (uzunluk <= 0) {
                continue;
            }
            double hayatta = Math.exp(-birikim);
            ortalama += hayatta * (1.0 - Math.exp(-egim * uzunluk)) / egim;
            if (Double.isNaN(enGec) && birikim + egim * uzunluk >= hedef) {
                enGec = bas + (hedef - birikim) / egim;
            }
            birikim += egim * uzunluk;
        }
        if (Double.isNaN(enGec) || enGec > garanti) {
            enGec = garanti;
        }
        return Optional.of(new SonrakiAfetTahmini(katilimlar.get(0).an(), Math.round(ortalama), Math.round(enGec)));
    }
}
