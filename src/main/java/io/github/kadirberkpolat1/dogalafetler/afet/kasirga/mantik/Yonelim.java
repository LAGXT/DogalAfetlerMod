package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/**
 * Kasırganın doğuş noktası ve yürüyüşü (tasarım notu §6 "Yönelim"). Kasırga hedefin {@link #dogusMesafesi} kadar uzağında
 * doğar ve hedefe doğru, her {@link #SAPMA_ARALIGI_TICK} tick'te yeniden çekilen ±35° sapmayla yürür; hedefin
 * {@link #GECIS_YARICAPI} bloğuna girince ana yönü donar ve hedefin üstünden geçip uzaklaşır. Açılar radyan, x doğu, z güney.
 */
public final class Yonelim {
    public static final double SAPMA = Math.toRadians(35);
    public static final int SAPMA_ARALIGI_TICK = 40;
    public static final double GECIS_YARICAPI = 12;
    /** Yön değişimi tick başına en çok bu kadar (radyan): kasırga keskin dönmez. */
    public static final double DONUS_HIZI = Math.toRadians(1.5);

    private Yonelim() {
    }

    /**
     * Doğuş mesafesi: siren alıcıları etki alanı + 64 blok içindekiler olduğundan (çekirdek §4, N11) hedef oyuncu sireni
     * duyacak kadar yakın: {@code [0,5·(R + 56), R + 56]} (S1 32-65, S5 54-107 blok). {@code u} [0, 1).
     */
    public static double dogusMesafesi(double cekimYaricapi, double u) {
        double ust = cekimYaricapi + 56;
        return ust * (0.5 + 0.5 * kirp(u));
    }

    /** Hedefe göre doğuş ofseti {x, z}; {@code aci} [0, 2π). */
    public static double[] dogusOfseti(double cekimYaricapi, double u, double aci) {
        double d = dogusMesafesi(cekimYaricapi, u);
        return new double[] {Math.cos(aci) * d, Math.sin(aci) * d};
    }

    /** Noktadan hedefe yön. */
    public static double yon(double x, double z, double hedefX, double hedefZ) {
        return Math.atan2(hedefZ - z, hedefX - x);
    }

    /** Ana yön çevresinde yeni hedef yön; {@code u} [0, 1) → [-35°, +35°]. */
    public static double sapmaliYon(double anaYon, double u) {
        return anaYon + (2 * kirp(u) - 1) * SAPMA;
    }

    /** Mevcut yönü hedef yöne en çok {@link #DONUS_HIZI} kadar döndürür (kısa yoldan). */
    public static double don(double mevcut, double hedef) {
        double fark = aciFarki(hedef, mevcut);
        return mevcut + Math.max(-DONUS_HIZI, Math.min(DONUS_HIZI, fark));
    }

    /** {@code a - b}, (-π, π] aralığında. */
    public static double aciFarki(double a, double b) {
        double f = (a - b) % (2 * Math.PI);
        if (f > Math.PI) {
            f -= 2 * Math.PI;
        } else if (f <= -Math.PI) {
            f += 2 * Math.PI;
        }
        return f;
    }

    private static double kirp(double u) {
        return Math.max(0, Math.min(1, u));
    }
}
