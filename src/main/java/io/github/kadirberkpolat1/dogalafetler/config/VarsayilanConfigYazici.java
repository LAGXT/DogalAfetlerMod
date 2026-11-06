package io.github.kadirberkpolat1.dogalafetler.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import net.minecraftforge.common.ForgeConfigSpec;

/** Bir config spec'inin bütün anahtarlarını varsayılan değer ve açıklamalarıyla, tanım sırasıyla TOML dosyasına yazar. */
final class VarsayilanConfigYazici {
    private VarsayilanConfigYazici() {
    }

    static void yaz(ForgeConfigSpec spec, Path dosya) {
        CommentedConfig icerik = TomlFormat.newConfig(LinkedHashMap::new);
        spec.correct(icerik);
        try {
            Path klasor = dosya.toAbsolutePath().getParent();
            if (klasor != null) {
                Files.createDirectories(klasor);
            }
            TomlFormat.instance().createWriter().write(icerik, dosya, WritingMode.REPLACE);
        } catch (IOException hata) {
            throw new UncheckedIOException("Config dosyası yazılamadı: " + dosya, hata);
        }
    }
}
