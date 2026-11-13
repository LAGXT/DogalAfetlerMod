package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Sıcaklık modelinin tek ısı/soğuk kaynağı kavramı: bir eşyanın ya da zırh ekinin ortam sıcaklığına kattığı sabit °C.
 * {@code guc} pozitifse ISITICI, negatifse SOGUTUCU (negatif katkı); birim °C'dir ve oyuncunun ortam sıcaklığına doğrudan
 * eklenir ({@link Katki#ISITICI_ESYA} ve {@link Katki#SOGUTUCU_ESYA} kalemleri termometre dökümünde ayrı satır olur).
 * Kayıt {@code sicaklik.IsiKaynaklari} üzerinden yapılır (eşya taşıma, zırh eki giyme).
 *
 * @param ad   hata iletisi ve günlük için kısa ad (küçük ASCII önerilir)
 * @param guc  °C katkısı: &gt; 0 ısıtıcı, &lt; 0 soğutucu; 0 olamaz
 */
public record IsiKaynagi(String ad, double guc) {
    /** Tek bir kaynağın katkısının mutlak üst sınırı (°C); config'teki sınırlar zaten toplamı kısar. */
    public static final double EN_BUYUK_GUC = 100;

    public enum Tur {
        ISITICI,
        SOGUTUCU
    }

    public IsiKaynagi {
        if (ad == null || ad.isBlank()) {
            throw new IllegalArgumentException("ısı kaynağının adı boş olamaz");
        }
        if (!Double.isFinite(guc) || guc == 0 || Math.abs(guc) > EN_BUYUK_GUC) {
            throw new IllegalArgumentException("ısı kaynağı gücü 0 olmayan, |güç| ≤ " + EN_BUYUK_GUC + " olmalı: " + guc);
        }
    }

    /** Ortamı {@code guc} °C ısıtan kaynak ({@code guc} &gt; 0). */
    public static IsiKaynagi isitici(String ad, double guc) {
        if (!(guc > 0)) {
            throw new IllegalArgumentException("ısıtıcı gücü pozitif olmalı: " + guc);
        }
        return new IsiKaynagi(ad, guc);
    }

    /** Ortamı {@code guc} °C soğutan kaynak ({@code guc} pozitif verilir, katkı negatiftir). */
    public static IsiKaynagi sogutucu(String ad, double guc) {
        if (!(guc > 0)) {
            throw new IllegalArgumentException("soğutucu gücü pozitif (büyüklük) verilmeli: " + guc);
        }
        return new IsiKaynagi(ad, -guc);
    }

    public Tur tur() {
        return guc > 0 ? Tur.ISITICI : Tur.SOGUTUCU;
    }

    /** Dökümdeki kalemi: ısıtıcılar {@link Katki#ISITICI_ESYA}, soğutucular {@link Katki#SOGUTUCU_ESYA}. */
    public Katki katki() {
        return tur() == Tur.ISITICI ? Katki.ISITICI_ESYA : Katki.SOGUTUCU_ESYA;
    }
}
