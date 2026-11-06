package io.github.kadirberkpolat1.dogalafetler.client.kasirga;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Kasırganın tek doku atlası (256×256, bellekte üretilir; dosya yok). Bütün katmanlar tek atlas ve tek render tipiyle
 * çizildiği için yarı saydam dörtgenlerin hepsi tek çizim çağrısında birlikte sıralanır.
 *
 * <ul>
 * <li>Satır 0-63: huni dış katman bulut gürültüsü; yatayda 256 pikselde periyodik (U kaydıkca sarılır).</li>
 * <li>Satır 64-127: huni iç katman, daha çizgili gürültü.</li>
 * <li>Satır 128-191: üç karelik toz/enkaz sprite'ı ve dördüncü karede yaprak/çimen parçası (64×64).</li>
 * <li>Satır 192-255: dört bulut topağı (64×64).</li>
 * </ul>
 * Renk beyaz-gri, asıl renk köşe rengiyle verilir.
 */
final class KasirgaDokusu {
    static final ResourceLocation KONUM = DogalAfetler.konum("dinamik/kasirga_atlas");
    static final int BOYUT = 256;
    static final float HUNI_A_V0 = 1f / BOYUT;
    static final float HUNI_A_V1 = 63f / BOYUT;
    static final float HUNI_B_V0 = 65f / BOYUT;
    static final float HUNI_B_V1 = 127f / BOYUT;
    static final float TOZ_V0 = 128f / BOYUT;
    static final float BULUT_V0 = 192f / BOYUT;
    static final float KARE = 64f / BOYUT;
    static final float YAPRAK_U0 = 3 * KARE;

    private static boolean hazir;

    private KasirgaDokusu() {
    }

    /** Render thread'inde ilk çizimde bir kez çağrılır. */
    static void hazirla() {
        if (hazir) {
            return;
        }
        hazir = true;
        long bas = System.nanoTime();
        NativeImage resim = new NativeImage(BOYUT, BOYUT, true);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < BOYUT; x++) {
                double u = (double) x / BOYUT;
                double v = y / 64.0;
                // Yatayda uzun, dikeyde sık: dönen huninin yatay yoğuşma şeritleri.
                double a = fbm(u * 4, v * 7, 4, 5, 11);
                double gri = 0.78 + 0.22 * a;
                yaz(resim, x, y, gri, kirp(0.58 + 0.9 * (a - 0.4)));
                double b = fbm(u * 6, v * 12, 6, 5, 23);
                double serit = 0.5 + 0.5 * Math.sin((v * 9 + b * 2.2) * Math.PI);
                double ib = 0.75 * b + 0.25 * serit;
                yaz(resim, x, y + 64, 0.62 + 0.38 * ib, kirp(0.68 + 0.7 * (ib - 0.4)));
            }
        }
        for (int kare = 0; kare < 4; kare++) {
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    double dx = (x - 31.5) / 30.0;
                    double dy = (y - 31.5) / 30.0;
                    double radyal = Math.max(0, 1 - Math.sqrt(dx * dx + dy * dy));
                    double n = fbm(x / 16.0 + kare * 1.7, y / 16.0 + kare * 0.9, 1 << 20, 4, 37 + kare);
                    if (kare == 3) {
                        // 35° eğik, uçları sivri mercek biçimli yaprak; orta damar koyu, bir ucunda sap.
                        double lx = (dx * 0.819 + dy * 0.574) / 0.9;
                        double ly = -dx * 0.574 + dy * 0.819;
                        double yari = 0.34 * (1 - lx * lx);
                        double yaprak = Math.abs(lx) < 1 ? puruzsuz(0, 0.05, yari - Math.abs(ly)) : 0;
                        double sap = lx < -0.95 && lx > -1.15 && Math.abs(ly) < 0.03 ? 1 : 0;
                        double damar = Math.abs(ly) < 0.025 ? 0.7 : 1;
                        yaz(resim, kare * 64 + x, 128 + y, (0.80 + 0.2 * n) * damar, Math.max(yaprak, sap));
                    } else {
                        double toz = kirp(Math.pow(radyal, 1.3) * (0.3 + 1.1 * n));
                        yaz(resim, kare * 64 + x, 128 + y, 0.82 + 0.18 * n, toz);
                    }
                    double m = fbm(x / 22.0 + kare * 3.1, y / 22.0, 1 << 20, 4, 71 + kare);
                    double bulut = puruzsuz(0.12, 0.62, Math.pow(radyal, 0.9) * (0.55 + 0.8 * m));
                    yaz(resim, kare * 64 + x, 192 + y, 0.80 + 0.20 * m, bulut);
                }
            }
        }
        DynamicTexture doku = new DynamicTexture(resim);
        Minecraft.getInstance().getTextureManager().register(KONUM, doku);
        DogalAfetler.LOGGER.info("[kasirga] doku atlası üretildi ({}x{}, {} ms)", BOYUT, BOYUT,
                (System.nanoTime() - bas) / 1_000_000);
    }

    private static void yaz(NativeImage resim, int x, int y, double gri, double alfa) {
        int g = (int) Math.round(kirp(gri) * 255);
        int a = (int) Math.round(kirp(alfa) * 255);
        resim.setPixelRGBA(x, y, (a << 24) | (g << 16) | (g << 8) | g);
    }

    private static double kirp(double d) {
        return Math.max(0, Math.min(1, d));
    }

    private static double puruzsuz(double a, double b, double x) {
        double t = kirp((x - a) / (b - a));
        return t * t * (3 - 2 * t);
    }

    /** Yatayda {@code periyot} hücrede bir sarılan değer gürültüsü toplamı (fBm), [0, 1]. */
    private static double fbm(double x, double y, int periyot, int oktav, int tohum) {
        double toplam = 0;
        double genlik = 0.5;
        double norm = 0;
        for (int o = 0; o < oktav; o++) {
            toplam += genlik * deger(x, y, periyot, tohum + o * 101);
            norm += genlik;
            x *= 2;
            y *= 2;
            periyot *= 2;
            genlik *= 0.5;
        }
        return toplam / norm;
    }

    private static double deger(double x, double y, int periyot, int tohum) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        int xa = Math.floorMod(x0, periyot);
        int xb = Math.floorMod(x0 + 1, periyot);
        double a = rastgele(xa, y0, tohum);
        double b = rastgele(xb, y0, tohum);
        double c = rastgele(xa, y0 + 1, tohum);
        double d = rastgele(xb, y0 + 1, tohum);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy;
    }

    private static double rastgele(int x, int y, int tohum) {
        int h = x * 374761393 + y * 668265263 + tohum * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535.0;
    }
}
