package io.github.kadirberkpolat1.dogalafetler.client.afet.kasirga;

import java.util.EnumMap;
import java.util.Map;

/**
 * Spike ölçümü (render thread): kare başına çizilen dörtgen/sprite/kopya sayıları, katman başına CPU süresi (köşe
 * üretimi; GPU maliyeti FPS farkıyla ölçülür) ve kasırganın çizilen konumunun kareden kareye adımı (enterpolasyon
 * pürüzsüzlüğü).
 */
final class KasirgaIstatistik {
    static int huniDortgen;
    static int huniSprite;
    static int altGirdapDortgen;
    static int tabanSprite;
    static int duvarSprite;
    static int gorselEnkaz;
    static long sonCizimNs;

    private static final Map<KasirgaIstemciDurumu.Katman, long[]> SURE = new EnumMap<>(KasirgaIstemciDurumu.Katman.class);
    private static int kare;
    private static double oncekiX = Double.NaN;
    private static double oncekiZ;
    private static double adimToplam;
    private static double adimKareToplam;
    private static double adimEnBuyuk;
    private static int adimSayisi;
    private static int durgunKare;

    private KasirgaIstatistik() {
    }

    static void kareBasi() {
        huniDortgen = 0;
        huniSprite = 0;
        altGirdapDortgen = 0;
        tabanSprite = 0;
        duvarSprite = 0;
        gorselEnkaz = 0;
        kare++;
        sonCizimNs = System.nanoTime();
    }

    static void sure(KasirgaIstemciDurumu.Katman katman, long ns) {
        SURE.computeIfAbsent(katman, k -> new long[1])[0] += ns;
    }

    static void konum(double x, double z) {
        if (!Double.isNaN(oncekiX)) {
            double adim = Math.hypot(x - oncekiX, z - oncekiZ);
            adimToplam += adim;
            adimKareToplam += adim * adim;
            adimEnBuyuk = Math.max(adimEnBuyuk, adim);
            adimSayisi++;
            if (adim < 1e-6) {
                durgunKare++;
            }
        }
        oncekiX = x;
        oncekiZ = z;
    }

    static void sifirla() {
        SURE.clear();
        kare = 0;
        oncekiX = Double.NaN;
        adimToplam = 0;
        adimKareToplam = 0;
        adimEnBuyuk = 0;
        adimSayisi = 0;
        durgunKare = 0;
    }

    static String sayilar() {
        return "huni_dortgen=" + huniDortgen + " huni_sprite=" + huniSprite + " alt_girdap_dortgen=" + altGirdapDortgen
                + " taban_sprite=" + tabanSprite + " duvar_sprite=" + duvarSprite + " gorsel_enkaz=" + gorselEnkaz;
    }

    static String sureler() {
        if (kare == 0) {
            return "kasırga çizilmedi";
        }
        StringBuilder sb = new StringBuilder();
        SURE.forEach((katman, ns) -> sb.append(katman.name().toLowerCase(java.util.Locale.ROOT)).append('=')
                .append(String.format("%.3f", ns[0] / 1e6 / kare)).append("ms "));
        return sb.append("(kare başı CPU, ").append(kare).append(" kare)").toString();
    }

    static String enterpolasyon() {
        if (adimSayisi == 0) {
            return "yok";
        }
        double ort = adimToplam / adimSayisi;
        double sapma = Math.sqrt(Math.max(0, adimKareToplam / adimSayisi - ort * ort));
        return String.format("kare adımı ort=%.4f blok, sapma/ort=%.2f, en büyük/ort=%.1f, durgun kare=%%%.0f (%d kare)",
                ort, ort > 0 ? sapma / ort : 0, ort > 0 ? adimEnBuyuk / ort : 0, 100.0 * durgunKare / adimSayisi,
                adimSayisi);
    }
}
