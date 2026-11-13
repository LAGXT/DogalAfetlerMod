package io.github.kadirberkpolat1.dogalafetler.core;

/**
 * Ayın gökyüzü durumu (bölüm H, madde 16; gökyüzü G8): {@code VAR} normal ay, {@code YAKLASIYOR} ay çarpması sireni
 * sürerken yaklaşan ve parçalanan ay, {@code YOK} çarpmadan sonra. {@link DunyaDonemi#ayOlaySaati()} YAKLASIYOR'da
 * yaklaşmanın, YOK'ta çarpmanın ölçekli afet saatidir (istemci yaklaşma, parlama, sarsıntı ve enkaz halkasını bu saatten
 * hesaplar); elle {@code ayVarAyarla} ile kaldırılan ayda -1'dir. Saf Java'dır.
 */
public enum AyDurumu {
    VAR,
    YAKLASIYOR,
    YOK;

    /** Olay saati yok (normal ay ya da elle kaldırılmış ay). */
    public static final long OLAY_YOK = -1L;

    /** {@code ayVar} ve olay saatinden durum. */
    public static AyDurumu den(boolean ayVar, long olaySaati) {
        if (!ayVar) {
            return YOK;
        }
        return olaySaati >= 0 ? YAKLASIYOR : VAR;
    }
}
