package io.github.kadirberkpolat1.dogalafetler.uyari.kategori;

import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Spike deneyi (reddedilen yol): enum kurucusuna erişim. Enum kurucusunun ilk iki parametresi derleyicinin ad ve sırasıdır. */
@Mixin(SoundSource.class)
public interface SoundSourceErisimci {
    @Invoker("<init>")
    static SoundSource dogalafetler$yeni(String sabitAdi, int sira, String ad) {
        throw new AssertionError("mixin uygulanmadı");
    }
}
