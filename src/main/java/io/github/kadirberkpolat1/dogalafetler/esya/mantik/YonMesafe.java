package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

/**
 * Oyuncudan afet merkezine yatay yön ve mesafe. Minecraft'ta +X doğu, +Z güneydir; pusula açısı kuzeyden (−Z) saat yönünde
 * derecedir (doğu 90). Oyuncu yaw'ı Minecraft'ın kuralıdır: 0 güneye, 90 batıya, 180 kuzeye bakar.
 */
public final class YonMesafe {
    private YonMesafe() {
    }

    /** Yatay (XZ) mesafe, blok. */
    public static double mesafe(double oyuncuX, double oyuncuZ, double hedefX, double hedefZ) {
        return Math.hypot(hedefX - oyuncuX, hedefZ - oyuncuZ);
    }

    /** Hedefin pusula açısı [0, 360): kuzey 0, doğu 90, güney 180, batı 270. Aynı noktada 0. */
    public static double pusulaAcisi(double oyuncuX, double oyuncuZ, double hedefX, double hedefZ) {
        double dx = hedefX - oyuncuX;
        double dz = hedefZ - oyuncuZ;
        if (dx == 0 && dz == 0) {
            return 0;
        }
        double aci = Math.toDegrees(Math.atan2(dx, -dz));
        return aci < 0 ? aci + 360 : aci;
    }

    /** Oyuncunun bakış yönünün pusula açısı: yaw 180 (kuzey) → 0. */
    public static double bakisAcisi(float yaw) {
        double aci = (yaw + 180.0) % 360.0;
        return aci < 0 ? aci + 360 : aci;
    }

    /** Hedefin bakışa göre açısı (−180, 180]: 0 tam önde, +90 sağda, −90 solda. Göstergedeki ok bu açıyla döner. */
    public static double goreliAci(double pusula, float yaw) {
        return normalize(pusula - bakisAcisi(yaw));
    }

    /** Açıyı (−180, 180] aralığına getirir. */
    public static double normalize(double aci) {
        double a = aci % 360.0;
        if (a <= -180) {
            a += 360;
        } else if (a > 180) {
            a -= 360;
        }
        return a;
    }

    /** Sekiz ana yön: 0 kuzey, 1 kuzeydoğu, 2 doğu, … 7 kuzeybatı (dil anahtarı {@code esya.dogalafetler.yon.N}). */
    public static int sekizYon(double pusula) {
        return (int) Math.floorMod(Math.round(pusula / 45.0), 8L);
    }
}
