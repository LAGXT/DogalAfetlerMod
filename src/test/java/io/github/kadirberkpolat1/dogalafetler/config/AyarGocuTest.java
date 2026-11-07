package io.github.kadirberkpolat1.dogalafetler.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** MT2-31 / inceleme-4 Mt27: göç yazımı yedekli ve atomik; okuma/yazma hatasında dosya değişmez. */
class AyarGocuTest {
    private static final String ESKI = "[meteor]\n	varlikTavani = 96\n";
    private static final List<AyarGocu.Kayit> GOC = List.of(new AyarGocu.Kayit("meteor.ayarSurumu", 1,
            (surum, oku) -> Map.of("meteor.varlikTavani", 448)));

    @Test
    void yazmadanOnceYedekAlinirVarOlanYedekEzilmez(@TempDir Path klasor) throws IOException {
        Path dosya = klasor.resolve(AyarKaydi.SUNUCU_DOSYASI);
        Files.writeString(dosya, ESKI, StandardCharsets.UTF_8);
        String satir = AyarGocu.uygula(dosya, GOC);
        assertTrue(satir != null && satir.contains("meteor.ayarSurumu 0 → 1")
                && satir.contains(yedek.getFileName().toString()), "özet: " + satir);
        assertEquals(ESKI, Files.readString(yedek, StandardCharsets.UTF_8), "yedek özgün içerik");
        String yeni = Files.readString(dosya, StandardCharsets.UTF_8);
        assertTrue(yeni.contains("448") && yeni.contains("ayarSurumu"), yeni);
        try (Stream<Path> s = Files.list(klasor)) {
            assertEquals(2, s.count(), "geçici dosya kalmaz");
        }

        Files.writeString(dosya, ESKI, StandardCharsets.UTF_8);
        assertTrue(AyarGocu.uygula(dosya, GOC).contains(".bak-ayarSurumu0-2"));
        assertEquals(ESKI, Files.readString(yedek, StandardCharsets.UTF_8), "ilk yedek ezilmez");
        assertEquals(ESKI, Files.readString(klasor.resolve(AyarKaydi.SUNUCU_DOSYASI + ".bak-ayarSurumu0-2")));
        assertEquals(klasor.resolve(AyarKaydi.SUNUCU_DOSYASI + ".bak-ayarSurumu0-3"), AyarGocu.yedekYolu(dosya, 0));
    }

    @Test
    void bozukDosyaDegismezYedekYok(@TempDir Path klasor) throws IOException {
        Path dosya = klasor.resolve(AyarKaydi.SUNUCU_DOSYASI);
        byte[] bozuk = "[meteor\n	varlikTavani = = 96\n".getBytes(StandardCharsets.UTF_8);
        Files.write(dosya, bozuk);
        assertNull(AyarGocu.uygula(dosya, GOC));
        assertArrayEquals(bozuk, Files.readAllBytes(dosya));
        try (Stream<Path> s = Files.list(klasor)) {
            assertEquals(1, s.count(), "yedek ya da geçici dosya yok");
        }
    }

    @Test
    void yazilamayanKlasordeDosyaDegismez(@TempDir Path klasor) throws IOException {
        Path dosya = klasor.resolve(AyarKaydi.SUNUCU_DOSYASI);
        Files.writeString(dosya, ESKI, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(klasor, PosixFilePermissions.fromString("r-x------"));
        try {
            assumeFalse(Files.isWritable(klasor), "root: klasör izni sınanamaz");
            assertNull(AyarGocu.uygula(dosya, GOC));
            assertEquals(ESKI, Files.readString(dosya, StandardCharsets.UTF_8));
            try (Stream<Path> s = Files.list(klasor)) {
                assertEquals(1, s.count(), "yedek ya da geçici dosya yok");
            }
        } finally {
            Files.setPosixFilePermissions(klasor, PosixFilePermissions.fromString("rwx------"));
        }
    }
}
