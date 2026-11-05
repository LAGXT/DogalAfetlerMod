package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenTuru;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenZarfi;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Konumsuz, döngülü siren sesi. Dosya döngülüdür; süre, açılış ve fade-out {@link SirenZarfi} ile kodla yönetilir.
 * {@link SoundSource#MASTER} ile çalındığında düzey {@code sirenSes / 100} ile çarpılır (Ana Ses'i ses motoru uygular);
 * gerçek kategori deneyinde çarpanı ses motoru kategori kaydırıcısından alır.
 */
final class SirenSesi extends AbstractTickableSoundInstance {
    private final SirenZarfi zarf;
    private String bitisNedeni = "süre doldu";

    SirenSesi(SirenTuru tur, int sureTick, SoundEvent olay, SoundSource kaynak) {
        super(olay, kaynak, SoundInstance.createUnseededRandom());
        this.zarf = new SirenZarfi(tur, sureTick);
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = true;
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        guncelle();
    }

    SirenTuru tur() {
        return zarf.tur();
    }

    int gecenTick() {
        return zarf.gecenTick();
    }

    /** Zarf sessizden başlar; ses motoru düzeyi 0 olan sesi aksi halde hiç başlatmaz. */
    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        // Ses motoru durdurulan örneği kanal kapanana kadar bir iki tick daha çağırır.
        if (isStopped()) {
            return;
        }
        zarf.ilerle();
        guncelle();
        if (zarf.sonduruluyor()) {
            DogalAfetler.LOGGER.info("[siren] fade-out {}: tick={} zarf={} ses={} perde={}", zarf.tur().kimlik(),
                    zarf.gecenTick(), bicim(zarf.duzey()), bicim(volume), bicim(pitch));
        } else if (zarf.gecenTick() % 20 == 0) {
            DogalAfetler.LOGGER.info("[siren] çalıyor {}: tick={} zarf={} ses={} SoundManager.isActive={}",
                    zarf.tur().kimlik(), zarf.gecenTick(), bicim(zarf.duzey()), bicim(volume),
                    Minecraft.getInstance().getSoundManager().isActive(this));
        }
        if (zarf.bitti()) {
            DogalAfetler.LOGGER.info("[siren] durdu {}: {} (geçen {} tick, süre {} tick)", zarf.tur().kimlik(), bitisNedeni,
                    zarf.gecenTick(), zarf.sureTick());
            stop();
        }
    }

    /** Türün fade-out'uyla söndürür. */
    void sondur(String neden) {
        if (zarf.sondur()) {
            bitisNedeni = neden;
        }
    }

    /** Yeni siren eskisini kısa bir fade ile keser (zaten daha erken bitecekse dokunmaz). */
    void kes() {
        if (zarf.sondur(SirenZarfi.KESME_TICK)) {
            bitisNedeni = "yeni siren kesti";
        }
    }

    private void guncelle() {
        float carpan = source == SoundSource.MASTER ? IstemciAyarlari.SIREN_SES.get() / 100f : 1f;
        volume = zarf.duzey() * carpan;
        pitch = zarf.perde();
    }

    private static String bicim(float deger) {
        return String.format(Locale.ROOT, "%.4f", deger);
    }
}
