package io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** K-E81: eski varsayılan sel tablosunda kalan config yeni varsayılana çekilir, elle değiştirilene dokunulmaz. */
class SelAyarGocuTest {
    private static final List<Double> ESKI_ARTIS = List.of(2.0, 3.0, 4.0, 5.5, 7.5);
    private static final List<Integer> ESKI_YUKSELME = List.of(45, 60, 75, 90, 105);

    @Test
    void eskiVarsayilanSelTablosuYeniVarsayilanaGecer() {
        Map<String, Object> dosya = new LinkedHashMap<>();
        dosya.put("sel.artisBlok", ESKI_ARTIS);
        dosya.put("sel.yukselmeSaniye", ESKI_YUKSELME);
        Map<String, List<String>> plan = SelAyarGocu.listePlani(0, dosya::get);
        assertEquals(List.of("5.0", "6.75", "8.5", "10.25", "12.0"), plan.get("sel.artisBlok"));
        assertEquals(List.of("20", "25", "30", "35", "40"), plan.get("sel.yukselmeSaniye"));
    }

    @Test
    void yeniVarsayilanTabloSelKurallariylaUstUsteDurur() {
        SelKurallari.Tablo v = SelKurallari.Tablo.VARSAYILAN;
        Map<String, List<String>> plan = SelAyarGocu.listePlani(0, Map.<String, Object>of("sel.artisBlok", ESKI_ARTIS,
                "sel.yukselmeSaniye", ESKI_YUKSELME)::get);
        assertEquals(v.artisBlok().stream().map(d -> Double.toString(d)).toList(), plan.get("sel.artisBlok"));
        assertEquals(v.yukselmeSaniye().stream().map(i -> Integer.toString(i)).toList(), plan.get("sel.yukselmeSaniye"));
    }

    @Test
    void elleDegistirilenVeGuncelDosyayaDokunulmaz() {
        assertTrue(SelAyarGocu.listePlani(0, Map.<String, Object>of("sel.artisBlok", List.of(2.0, 3.0, 4.0, 5.5, 9.0),
                "sel.yukselmeSaniye", List.of(45, 60, 75, 90, 100))::get).isEmpty(), "elle değiştirilmiş");
        assertTrue(SelAyarGocu.listePlani(SelAyarGocu.GUNCEL, Map.<String, Object>of("sel.artisBlok", ESKI_ARTIS,
                "sel.yukselmeSaniye", ESKI_YUKSELME)::get).isEmpty(), "güncel sürüm dosyası");
    }
}
