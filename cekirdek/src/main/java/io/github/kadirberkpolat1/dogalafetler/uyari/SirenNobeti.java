package io.github.kadirberkpolat1.dogalafetler.uyari;

/**
 * Ses motorunun kendiliğinden kestiği sireni ya da ortam katmanını yeniden çalma kararı (saf mantık, K0-27). Boyut
 * değişiminde ({@code Minecraft.setLevel} → {@code SoundManager.stop()}), {@code /stopsound} ve kaynak yeniden yüklemede
 * vanilla bütün sesleri durdurur; ses örneği bunu bilmez. İstemci her tick'te bir kez sorar; zarf aynı kalır, yalnız ses
 * örneği yenilenir. Ana Ses 0 iken vanilla sesi hiç başlatmaz; bu "motor kesti" sayılmaz, denenmez.
 */
public final class SirenNobeti {
    /** Çalamayan bir ses motorunda (aygıt yok, motor yüklü değil) her tick yeniden denenmesin diye aralık. */
    public static final int DENEME_ARALIGI_TICK = 20;

    private int sonDenemedenBeri = DENEME_ARALIGI_TICK;

    /**
     * @param ornekDurduruldu ses örneğini zarf bitince biz durdurduk
     * @param zarfBitti siren zarfı bitti
     * @param motordaCaliyor ses motoru örneği hâlâ çalıyor ({@code SoundManager.isActive})
     * @param anaSesKapali Ana Ses 0 (vanilla örneği başlatmaz)
     * @return {@code true} ise ses aynı zarfla yeniden çalınmalı (deneme sayılır)
     */
    public boolean yenidenCalinmali(boolean ornekDurduruldu, boolean zarfBitti, boolean motordaCaliyor,
            boolean anaSesKapali) {
        if (sonDenemedenBeri < DENEME_ARALIGI_TICK) {
            sonDenemedenBeri++;
        }
        if (ornekDurduruldu || zarfBitti || motordaCaliyor || anaSesKapali
                || sonDenemedenBeri < DENEME_ARALIGI_TICK) {
            return false;
        }
        sonDenemedenBeri = 0;
        return true;
    }
}
