package io.github.kadirberkpolat1.dogalafetler.esya.radar;

import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/** Radarın ve ortak yön anahtarlarının dil metinleri (datagen; madde 13a, N6: yalnız elde tutulurken çizilen gösterge). */
public final class RadarCevirileri {
    public static final String ON = "esya.dogalafetler.radar.";
    private static final String[][] YONLER = {{"N", "K"}, {"NE", "KD"}, {"E", "D"}, {"SE", "GD"}, {"S", "G"}, {"SW", "GB"},
        {"W", "B"}, {"NW", "KB"}};

    private RadarCevirileri() {
    }

    /** Sekiz yönün kısa adı (0 kuzey … 7 kuzeybatı). */
    public static String yonAnahtari(int yon) {
        return "esya.dogalafetler.yon." + Math.floorMod(yon, 8);
    }

    public static void yaz(CeviriYazici y) {
        y.ekle("item.dogalafetler.hava_radari", "Weather Radar", "Hava Radarı");
        y.ekle("item.dogalafetler.hava_radari.ipucu", "Held: shows the approaching disaster, its direction, distance and time.",
                "Elde tutulunca yaklaşan afetin adını, yönünü, uzaklığını ve süresini gösterir.");
        y.ekle("item.dogalafetler.gelismis_hava_radari", "Advanced Weather Radar", "Gelişmiş Hava Radarı");
        y.ekle("item.dogalafetler.gelismis_hava_radari.ipucu",
                "Longer range; also shows severity, movement and the last 5 disasters.",
                "Daha uzun menzil; şiddet seviyesini, hareket yönünü ve son 5 afeti de gösterir.");
        y.ekle(ON + "sakin", "Sky is calm", "Gökyüzü sakin");
        y.ekle(ON + "menzil", "Range %s m", "Menzil %s m");
        y.ekle(ON + "anomali", "Sky anomaly", "Gökyüzü anomalisi");
        y.ekle(ON + "mesafe", "%s m", "%s m");
        y.ekle(ON + "varis", "ETA %s", "Varış %s");
        y.ekle(ON + "basliyor", "Begins in %s", "Başlamasına %s");
        y.ekle(ON + "kalan", "Ends in %s", "Kalan %s");
        y.ekle(ON + "seviye", "Severity: %s", "Şiddet: %s");
        y.ekle(ON + "durgun", "Stationary", "Durağan");
        y.ekle(ON + "hiz", "Moving %s m/s", "Hareket %s m/sn");
        y.ekle(ON + "gecmis", "Recent disasters", "Son afetler");
        y.ekle(ON + "gecmis_bos", "None yet", "Henüz yok");
        y.ekle(ON + "seviye_kisa", "L%s", "S%s");
        y.ekle(ON + "yas.simdi", "now", "şimdi");
        y.ekle(ON + "yas.dk", "%sm", "%sdk");
        y.ekle(ON + "yas.sa", "%sh", "%ssa");
        y.ekle(ON + "yas.gun", "%sd", "%sg");
        for (int i = 0; i < YONLER.length; i++) {
            y.ekle(yonAnahtari(i), YONLER[i][0], YONLER[i][1]);
        }
    }
}
