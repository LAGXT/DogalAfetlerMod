package io.github.kadirberkpolat1.dogalafetler.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VarsayilanConfigYaziciTest {
    @Test
    void yazilanDosyaSpeceGoreDogrudurVeVarsayilanlariTasir(@TempDir Path klasor) throws Exception {
        ForgeConfigSpec.Builder k = new ForgeConfigSpec.Builder();
        k.comment("Çekirdek bölümü").push("cekirdek");
        k.comment("Alt bölüm").push("zamanlayici");
        k.comment("Tamsayı").defineInRange("tam", 20, 1, 1200);
        k.comment("Uzun").defineInRange("uzun", 12_000L, 0L, 100_000_000L);
        k.comment("Ondalık").defineInRange("ondalik", 0.8, 0.0, 100.0);
        k.pop();
        k.comment("Mantıksal").define("mantiksal", false);
        k.comment("Enum").defineEnum("kalite", EfektKalitesi.YUKSEK);
        k.comment("Liste").defineListAllowEmpty(List.of("boyutlar"), () -> List.of("minecraft:overworld"),
                o -> o instanceof String);
        k.pop();
        ForgeConfigSpec spec = k.build();

        Path dosya = klasor.resolve("serverconfig/dogalafetler-server.toml");
        VarsayilanConfigYazici.yaz(spec, dosya);
        assertTrue(Files.exists(dosya));

        CommentedConfig okunan = TomlFormat.instance().createParser().parse(dosya, FileNotFoundAction.THROW_ERROR);
        assertTrue(spec.isCorrect(okunan), "yazılan dosyayı Forge yeniden düzeltmeye kalkmamalı:\n" + Files.readString(dosya));
        assertEquals(20, okunan.<Number>get("cekirdek.zamanlayici.tam").intValue());
        assertEquals(12_000L, okunan.<Number>get("cekirdek.zamanlayici.uzun").longValue());
        assertEquals(0.8, okunan.<Number>get("cekirdek.zamanlayici.ondalik").doubleValue(), 1e-12);
        assertEquals("YUKSEK", okunan.get("cekirdek.kalite").toString());
        assertEquals(List.of("minecraft:overworld"), okunan.get("cekirdek.boyutlar"));
        assertTrue(Files.readString(dosya).contains("#Tamsayı"), "açıklamalar dosyada olmalı");
    }
}
