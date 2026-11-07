package io.github.kadirberkpolat1.dogalafetler.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SERVER config dosyasının göçü (MT2-31). Forge var olan dosyada aralık içindeki değere dokunmaz ve eksik anahtarı, mod
 * olaylarından önce, varsayılanla doldurur: eski varsayılanda kalan değer öyle kalır, sürüm anahtarı olmayan eski dosya da
 * {@code ModConfigEvent}'ten bakınca yeni dosyadan ayırt edilemez. Bu yüzden göç dosya işleyicisinde, Forge dosyayı okuyup
 * izlemeye almadan önce yapılır: dosya ayrıştırılır, sürümü eski her göçün planı uygulanır, değişiklik varsa dosya bir kez
 * yazılır (art arda {@code ConfigValue.set} dosya izleyicisiyle yarışır, K-E28). Çalışırken elle düzenlenen dosya (Forge'un
 * yeniden yüklemesi) göç ettirilmez.
 */
final class AyarGocu {
    record Kayit(String surumYolu, int guncel, AyarKaydi.GocPlani plan) {
    }

    private AyarGocu() {
    }

    /** Değişiklik varsa dosyayı yazar ve tek satırlık özeti döndürür; dosya yoksa ya da göçler güncelse null. */
    static String uygula(Path dosya, List<Kayit> kayitlar) {
        if (kayitlar.isEmpty() || !Files.isRegularFile(dosya)) {
            return null;
        }
        CommentedConfig icerik = TomlFormat.newConfig(LinkedHashMap::new);
        try (Reader okuyucu = Files.newBufferedReader(dosya, StandardCharsets.UTF_8)) {
            TomlFormat.instance().createParser().parse(okuyucu, icerik, ParsingMode.REPLACE);
        } catch (IOException | ParsingException hata) {
            DogalAfetler.LOGGER.warn("Config göçü atlandı, {} okunamadı: {}", dosya, hata.toString());
            return null;
        }
        List<String> ozet = new ArrayList<>();
        for (Kayit k : kayitlar) {
            Object okunan = icerik.get(k.surumYolu());
            int surum = okunan instanceof Number n ? n.intValue() : 0;
            if (surum >= k.guncel()) {
                continue;
            }
            List<String> degisen = new ArrayList<>();
            for (Map.Entry<String, ?> e : k.plan().plan(surum, icerik::get).entrySet()) {
                degisen.add(e.getKey() + " " + icerik.get(e.getKey()) + " → " + e.getValue());
                icerik.set(e.getKey(), e.getValue());
            }
            icerik.set(k.surumYolu(), k.guncel());
            ozet.add(k.surumYolu() + " " + surum + " → " + k.guncel()
                    + (degisen.isEmpty() ? ", değişen yok" : ": " + String.join(", ", degisen)));
        }
        if (ozet.isEmpty()) {
            return null;
        }
        TomlFormat.instance().createWriter().write(icerik, dosya, WritingMode.REPLACE);
        return "Config göçü " + dosya.getFileName() + ": " + String.join("; ", ozet);
    }
}
