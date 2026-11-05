package io.github.kadirberkpolat1.dogalafetler.uyari;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.sounds.SoundSource;

/**
 * Spike deneyi: {@code SoundSource}'a mixin ile eklenen gerçek "Sirenler" kategorisi. Yalnız
 * {@code -PsirenKategori=gercek} ile açılan {@code dogalafetler_siren_kategori.mixins.json} yüklüyse vardır; yoksa siren
 * {@link SoundSource#MASTER} ile çalınır ve düzeyi CLIENT config'teki {@code sirenSes} ile çarpılır (N5, seçilen yol).
 */
public final class SirenKategorisi {
    /** Vanilla bu adı {@code options.txt}'e {@code soundCategory_<ad>} ve dil anahtarına {@code soundCategory.<ad>} yazar. */
    public static final String AD = "dogalafetler_siren";
    public static final String DIL_ANAHTARI = "soundCategory." + AD;

    private static final Optional<SoundSource> GERCEK = Arrays.stream(SoundSource.values())
            .filter(kaynak -> kaynak.getName().equals(AD))
            .findFirst();

    private SirenKategorisi() {
    }

    public static Optional<SoundSource> gercek() {
        return GERCEK;
    }
}
