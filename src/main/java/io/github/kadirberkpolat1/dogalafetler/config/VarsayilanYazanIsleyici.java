package io.github.kadirberkpolat1.dogalafetler.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Config dosyası yoksa onu spec'in varsayılanları ve açıklamalarıyla yazan dosya işleyicisi. Forge yeni dosyayı boş
 * oluşturup düzelttiğinde her anahtar için bir {@code Incorrect key ... corrected from null} WARN satırı yazar (yeni her
 * dünyada SERVER config için); dosya önceden doğru yazılınca düzeltme ve WARN olmaz. {@code defaultconfigs/} altında paket
 * yapımcısının dosyası varsa ona dokunulmaz (Forge onu kopyalar); var olan SERVER dosyası önce kayıtlı göçlerle güncellenir
 * ({@link AyarGocu}), sonra her zamanki gibi Forge düzeltir.
 */
final class VarsayilanYazanIsleyici extends ConfigFileTypeHandler {
    static final VarsayilanYazanIsleyici ORNEK = new VarsayilanYazanIsleyici();

    private VarsayilanYazanIsleyici() {
    }

    @Override
    public Function<ModConfig, CommentedFileConfig> reader(Path klasor) {
        Function<ModConfig, CommentedFileConfig> asil = super.reader(klasor);
        return config -> {
            Path dosya = klasor.resolve(config.getFileName());
            Path paketVarsayilani = FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath())
                    .resolve(config.getFileName());
            if (Files.exists(dosya)) {
                if (AyarKaydi.SUNUCU_DOSYASI.equals(config.getFileName())) {
                    String goc = AyarKaydi.sunucuDosyasiniGocEt(dosya);
                    if (goc != null) {
                        DogalAfetler.LOGGER.info(goc);
                    }
                }
            } else if (!Files.exists(paketVarsayilani) && config.getSpec() instanceof ForgeConfigSpec spec) {
                VarsayilanConfigYazici.yaz(spec, dosya);
                DogalAfetler.LOGGER.info("Config dosyası varsayılanlarla oluşturuldu: {}", dosya);
            }
            return asil.apply(config);
        };
    }
}
