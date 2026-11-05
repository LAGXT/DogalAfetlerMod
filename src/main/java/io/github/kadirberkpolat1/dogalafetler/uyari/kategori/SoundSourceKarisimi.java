package io.github.kadirberkpolat1.dogalafetler.uyari.kategori;

import io.github.kadirberkpolat1.dogalafetler.uyari.SirenKategorisi;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spike deneyi (reddedilen yol, .vibe/kararlar/spike-siren.md): {@code SoundSource}'un statik başlatıcısının sonunda
 * {@code $VALUES} dizisine yeni bir sabit eklenir. {@code values()} bu diziyi kopyaladığı için vanilla ses ekranı ve
 * {@code Options} yeni kategoriyi kendiliğinden görür. Forge 1.20.1'de {@code SoundSource} {@code IExtensibleEnum}
 * değildir; bu yüzden ancak mixin ile yapılabilir.
 */
@Mixin(SoundSource.class)
public abstract class SoundSourceKarisimi {
    @Shadow
    @Final
    @Mutable
    private static SoundSource[] $VALUES;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void dogalafetler$sirenKategorisiEkle(CallbackInfo bilgi) {
        SoundSource[] eski = $VALUES;
        SoundSource siren = SoundSourceErisimci.dogalafetler$yeni(
                SirenKategorisi.AD.toUpperCase(Locale.ROOT), eski.length, SirenKategorisi.AD);
        SoundSource[] yeni = Arrays.copyOf(eski, eski.length + 1);
        yeni[eski.length] = siren;
        $VALUES = yeni;
    }
}
