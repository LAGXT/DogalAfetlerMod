package io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Örtü-çökme sinkhole'unun planı (tasarım notu sinkhole §1–§2): dik ama pürüzlü duvarlı, iç içe halkalarla basamaklanan
 * dairesel çukur. Halka 0 çekirdektir (tam derinlik, ilk çöker); dış halkalar sırayla köşelenir ve her biri bir basamak
 * bırakır. Yüzeyde çukura eş merkezli çatlak halkaları (r = 0,35R, 0,7R, 1,05R) ve onları birleştiren radyal çatlaklar;
 * çöküşten sonra kenarın 1–3 blok dışında çatlak halkası (çimen → kaba toprak). Koordinatlar merkeze göredir. Saf Java'dır.
 */
public final class CukurPlani {
    /** Bir çukur sütunu: merkeze göre konum, yüzeyden derinlik ve halka (0 = çekirdek). */
    public record Sutun(int dx, int dz, int derinlik, int halka) {
    }

    /** Plan: sütunlar halka ve merkeze uzaklık sırasında; çatlak ve kenar halkası hücreleri {dx, dz}. */
    public record Plan(List<Sutun> sutunlar, List<int[]> catlaklar, List<int[]> kenar, int halkaSayisi) {
        public List<Sutun> halka(int h) {
            return sutunlar.stream().filter(s -> s.halka() == h).toList();
        }
    }

    private CukurPlani() {
    }

    public static Plan olustur(SinkholeSeviyesi s, long tohum) {
        SplittableRandom r = new SplittableRandom(tohum);
        double yaricap = s.yaricap();
        int halkaSayisi = Math.max(1, s.basamak());
        double faz1 = r.nextDouble() * Math.PI * 2;
        double faz2 = r.nextDouble() * Math.PI * 2;
        double purus = Math.min(1.0, yaricap / 6);
        int sinir = (int) Math.ceil(yaricap + 1);
        List<Sutun> sutunlar = new ArrayList<>();
        for (int dx = -sinir; dx <= sinir; dx++) {
            for (int dz = -sinir; dz <= sinir; dz++) {
                double rr = Math.hypot(dx, dz);
                double aci = Math.atan2(dz, dx);
                double kenar = yaricap + purus * (0.6 * Math.sin(3 * aci + faz1) + 0.4 * Math.sin(5 * aci + faz2));
                if (rr > kenar) {
                    continue;
                }
                double oran = rr / Math.max(0.5, kenar);
                int halka = halkaSayisi == 1 || oran < 0.5 ? 0
                        : Math.min(halkaSayisi - 1, 1 + (int) ((oran - 0.5) / (0.5 / (halkaSayisi - 1))));
                double derinlikOrani = 1 - (double) halka / halkaSayisi;
                int derinlik = Math.max(1, (int) Math.round(s.derinlik() * derinlikOrani) - (r.nextInt(4) == 0 ? 1 : 0));
                if (s.seviye() >= 4 && rr < 1.6) {
                    derinlik += 3;
                }
                sutunlar.add(new Sutun(dx, dz, derinlik, halka));
            }
        }
        sutunlar.sort(Comparator.comparingInt(Sutun::halka).thenComparingDouble(c -> Math.hypot(c.dx(), c.dz()))
                .thenComparingInt(Sutun::dx).thenComparingInt(Sutun::dz));
        Set<Long> catlak = new LinkedHashSet<>();
        double[] halkalar = s.seviye() == 1 ? new double[] {1.05} : new double[] {0.35, 0.7, 1.05};
        for (double h : halkalar) {
            double rr = h * yaricap;
            int adim = (int) Math.ceil(2 * Math.PI * rr * 1.5);
            for (int i = 0; i < adim; i++) {
                double aci = i * 2 * Math.PI / adim;
                if (r.nextInt(6) != 0) {
                    catlak.add(anahtar((int) Math.round(Math.cos(aci) * rr), (int) Math.round(Math.sin(aci) * rr)));
                }
            }
        }
        int isin = 6 + s.seviye();
        for (int i = 0; i < isin; i++) {
            double aci = i * 2 * Math.PI / isin + r.nextDouble() * 0.4;
            for (double rr = 0.3 * yaricap; rr <= 1.1 * yaricap; rr += 0.5) {
                catlak.add(anahtar((int) Math.round(Math.cos(aci) * rr), (int) Math.round(Math.sin(aci) * rr)));
            }
        }
        List<int[]> kenar = new ArrayList<>();
        for (int dx = -sinir - 3; dx <= sinir + 3; dx++) {
            for (int dz = -sinir - 3; dz <= sinir + 3; dz++) {
                double rr = Math.hypot(dx, dz);
                if (rr > yaricap + 1 && rr <= yaricap + 3 && r.nextInt(5) < 2) {
                    kenar.add(new int[] {dx, dz});
                }
            }
        }
        return new Plan(List.copyOf(sutunlar), catlak.stream().map(CukurPlani::coz).toList(), List.copyOf(kenar),
                halkaSayisi);
    }

    private static long anahtar(int dx, int dz) {
        return ((long) dx << 32) ^ (dz & 0xFFFFFFFFL);
    }

    private static int[] coz(long a) {
        return new int[] {(int) (a >> 32), (int) a};
    }
}
