package io.github.kadirberkpolat1.dogalafetler.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SERVER config dosyasının göçü (MT2-31). Forge var olan dosyada aralık içindeki değere dokunmaz ve eksik anahtarı, mod
 * olaylarından önce, varsayılanla doldurur: eski varsayılanda kalan değer öyle kalır, sürüm anahtarı olmayan eski dosya da
 * {@code ModConfigEvent}'ten bakınca yeni dosyadan ayırt edilemez. Bu yüzden göç dosya işleyicisinde, Forge dosyayı okuyup
 * izlemeye almadan önce yapılır: dosya ayrıştırılır, sürümü eski her göçün planı uygulanır, değişiklik varsa dosya bir kez
 * yazılır (art arda {@code ConfigValue.set} dosya izleyicisiyle yarışır, K-E28). Yazım kesilmesin diye yeni içerik aynı
 * klasörde geçici dosyaya yazılır, özgün dosya {@code <dosya>.bak-ayarSurumu<eski>}'ye (varsa {@code -2}, {@code -3}, …)
 * kopyalanır, sonra geçici dosya atomik taşınır. Okuma ya da yazma hatasında tek WARN, dosyaya dokunulmaz, Forge eski
 * değerlerle sürer. Çalışırken elle düzenlenen dosya (Forge'un yeniden yüklemesi) göç ettirilmez. Bölümler yalnız
 * tablolarını ({@link SurumluGocPlani}) verir; dosya, yedek ve atomik yazım tek yerde, burada (K-E61 rev.).
 */
final class AyarGocu {
    private AyarGocu() {
    }

    /** Değişiklik varsa dosyayı yazar ve tek satırlık özeti döndürür; dosya yoksa, göçler güncelse ya da hata olursa null. */
    static String uygula(Path dosya, List<SurumluGocPlani> planlar) {
        if (planlar.isEmpty() || !Files.isRegularFile(dosya)) {
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
        int enEski = Integer.MAX_VALUE;
        for (SurumluGocPlani k : planlar) {
            Object okunan = icerik.get(k.surumYolu());
            int surum = okunan instanceof Number n ? n.intValue() : 0;
            if (surum >= k.guncel()) {
                continue;
            }
            enEski = Math.min(enEski, surum);
            List<String> degisen = new ArrayList<>();
            for (Map.Entry<String, Number> e : k.plan(surum, icerik::get).entrySet()) {
                degisen.add(e.getKey() + " " + icerik.get(e.getKey()) + " → " + e.getValue());
                icerik.set(e.getKey(), e.getValue());
            }
            for (Map.Entry<String, List<String>> e : k.listePlani(surum, icerik::get).entrySet()) {
                degisen.add(e.getKey() + " eski varsayılan liste → yeni varsayılan (" + e.getValue().size() + " satır)");
                icerik.set(e.getKey(), new ArrayList<>(e.getValue()));
            }
            icerik.set(k.surumYolu(), k.guncel());
            ozet.add(k.surumYolu() + " " + surum + " → " + k.guncel()
                    + (degisen.isEmpty() ? ", değişen yok" : ": " + String.join(", ", degisen)));
        }
        if (ozet.isEmpty()) {
            return null;
        }
        Path gecici = null;
        try {
            gecici = Files.createTempFile(dosya.toAbsolutePath().getParent(), dosya.getFileName().toString(), ".gecici");
            TomlFormat.instance().createWriter().write(icerik, gecici, WritingMode.REPLACE);
            Path yedek = yedekYolu(dosya, enEski);
            Files.copy(dosya, yedek, StandardCopyOption.COPY_ATTRIBUTES);
            Files.move(gecici, dosya, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return "Config göçü " + dosya.getFileName() + " (yedek " + yedek.getFileName() + "): "
                    + String.join("; ", ozet);
        } catch (IOException | UncheckedIOException | WritingException hata) {
            DogalAfetler.LOGGER.warn("Config göçü yazılamadı, {} değişmedi (eski değerlerle sürüyor): {}", dosya,
                    hata.toString());
            return null;
        } finally {
            if (gecici != null) {
                try {
                    Files.deleteIfExists(gecici);
                } catch (IOException silinemedi) {
                    DogalAfetler.LOGGER.warn("Config göçünün geçici dosyası silinemedi: {}", gecici);
                }
            }
        }
    }

    /** {@code <dosya>.bak-ayarSurumu<surum>}; varsa üzerine yazılmaz, {@code -2}, {@code -3}, … eklenir. */
    static Path yedekYolu(Path dosya, int surum) {
        String ad = dosya.getFileName() + ".bak-ayarSurumu" + surum;
        Path yedek = dosya.resolveSibling(ad);
        for (int i = 2; Files.exists(yedek); i++) {
            yedek = dosya.resolveSibling(ad + "-" + i);
        }
        return yedek;
    }
}
