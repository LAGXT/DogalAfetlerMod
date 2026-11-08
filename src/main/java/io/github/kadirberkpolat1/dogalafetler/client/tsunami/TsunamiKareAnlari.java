package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import io.github.kadirberkpolat1.dogalafetler.su.Cekilme;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import java.util.ArrayList;
import java.util.List;

/**
 * Görsel otomasyonun kare anları (AF3-15, saf): sabit saniyeler yerine istemciye senkronlanan afet verisinden (çekilme,
 * dalga ve kıyı noktası {@code kx/kz}). Çekilmenin tamamlanma oranı, cephenin kıyıya kalan yolunun açıklığa oranı ve
 * cephenin kıyıyı geçmesinden bu yana geçen tick: sunucu zamanlaması (çekilme süresi, dalga gecikmesi, açıklık) değişse de
 * kare aynı evrede alınır. Zaman ölçekli afet saatidir (tick). Kareleri bu evrelere sıralayan adım listesi de buradadır
 * ({@link #tsunamiAdimlari}); {@link TsunamiOtomasyonu} onu yürütür.
 */
final class TsunamiKareAnlari {
    /** {@code f:} ölçüm penceresinin uzunluğu (istemci tick'i); pencere sürerken sonraki adımlar bekler. */
    static final int OLCUM_TICK = 100;

    private TsunamiKareAnlari() {
    }

    /** Çekilmenin tamamlanma oranı: başlamadan 0, süresi dolunca 1. */
    static double cekilmeIlerlemesi(Cekilme c, double t) {
        if (t <= c.baslangic()) {
            return 0;
        }
        return c.sure() <= 0 ? 1 : Math.min(1, (t - c.baslangic()) / c.sure());
    }

    /** Kıyı noktasının dalganın başlangıç çizgisine uzaklığı (açıklık, blok); kıyı çizginin gerisindeyse 0 ya da eksi. */
    static double aciklik(Dalga w, double kx, double kz) {
        return (kx - w.x0()) * w.yonX() + (kz - w.z0()) * w.yonZ();
    }

    /** Cephenin kıyıya kalan yolunun açıklığa oranı: doğuşta (ve öncesinde) 1, kıyıda ve sonrasında 0. */
    static double kalanOran(Dalga w, double kx, double kz, double t) {
        double a = aciklik(w, kx, kz);
        if (a <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, 1 - (t - w.baslangic()) * w.hiz() / a));
    }

    /** Cephenin kıyıyı geçmesinden bu yana geçen tick; kıyıya varmadan eksi. */
    static double kiyidanSonra(Dalga w, double kx, double kz, double t) {
        return t - w.baslangic() - Math.max(0, aciklik(w, kx, kz)) / w.hiz();
    }

    /** Tabanlı {@code f:} adımı: {@code taban} aynı kameranın afetsiz ölçümü ({@code OLCUM ... taban=}, K-E29 (b)). */
    static String olc(String ad, String taban) {
        return "f:" + ad + " " + taban;
    }

    /**
     * Bir tsunami koşusu: vitrin yeniden kurulur, kamera evlerin arasından denize bakar ({@code vitrin kamera tepe}), afet
     * AKTIF'e ilerletilir. Ts7: çekilme (S5'te ufuk da) kıyının arkasından yüksekten denize bakan kameradan, S1'in ufuk ve
     * vitrin kareleri alçak kameradan; ölçümler ve sonraki kareler afetsiz tabanla aynı tepe kamerasından. AKTIF'ten sonraki
     * kare anları senkron evreden ({@code e:}; AF3-15): çekilme %42'sinde, ufuk cephenin kıyıya kalan yolu açıklığın
     * %86'sıyken, vitrin %22'siyken, çarpma cephe kıyıyı geçtikten 60 tick, karada 160 tick sonra. K-E67 zamanlamasında (çekilme
     * 8 sn, dalga AKTIF + 2 sn, kıyı + 10 sn) dalga doğuştan 8 sn'de kıyıya varır: ufuk ile vitrin arasına {@value #OLCUM_TICK}
     * tick'lik ölçüm penceresi sığmaz (sığdırılsa vitrin dalga kıyıya vurduktan sonra alınır). Bu yüzden kareler arasında
     * ölçüm yok; dalga sırasındaki pencere çarpma ile karada arasında ({@code -carpma}, eski {@code -yaklasma}), S1'in alçak
     * kamerası ufuk eşiğinden önce kurulur.
     */
    static List<String> tsunamiAdimlari(int seviye, String on) {
        // Komut geri bildirimi ön-uyarı karesinin sohbetine düşmesin diye o kareye dek kapalı.
        List<String> a = new ArrayList<>(List.of("k:dogalafet su vitrin", "b:300", "k:time set 10500",
                "k:dogalafet su vitrin kamera tepe", "b:200", "f:" + on + "-afetsiz", "s:" + on + "-once",
                "k:gamerule sendCommandFeedback false", "k:dogalafet start tsunami " + seviye, "b:50", "k:gamemode creative",
                "k:item replace entity @s weapon.mainhand with dogalafetler:gelismis_hava_radari", "b:8", "g:1",
                "s:" + on + "-onuyari", "g:0", "k:item replace entity @s weapon.mainhand with air", "v:tepe",
                "k:gamerule sendCommandFeedback true", "k:dogalafet asama ileri", "b:60", "s:" + on + "-siren",
                "k:dogalafet asama ileri", "e:cekilme=0.42", "v:deniz", "b:10", "s:" + on + "-cekilme"));
        if (seviye == 1) {
            a.addAll(List.of("v:alcak", "b:10", "e:kalan=0.86", "s:" + on + "-ufuk", "e:kalan=0.22", "s:" + on + "-vitrin",
                    "v:tepe", "b:9", "e:kiyi=60"));
        } else {
            a.addAll(List.of("e:kalan=0.86", "s:" + on + "-ufuk", "v:tepe", "b:9", "e:kalan=0.22", "s:" + on + "-vitrin",
                    "e:kiyi=60"));
        }
        a.addAll(List.of("s:" + on + "-carpma", olc(on + "-carpma", on + "-afetsiz"), "e:kiyi=160", "s:" + on + "-karada",
                olc(on + "-karada", on + "-afetsiz"), "k:dogalafet stop", "b:100", "s:" + on + "-sonra",
                "k:dogalafet su temizle", "b:40"));
        return a;
    }
}
