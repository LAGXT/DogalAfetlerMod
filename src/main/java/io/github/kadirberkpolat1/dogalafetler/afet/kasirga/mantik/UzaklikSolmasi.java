package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/**
 * Kasırga bulut katmanlarının uzaklıkla solması (K-E86). Vanilla sis, {@code entity_translucent} şaderında rengi sis rengine
 * çekip alfayı korur: uzaktaki kasırga düz, opak bir sis renkli siluete dönüşür ve arkasındaki gök (yıldızlar, gezegenler,
 * kara delikler) görünmez olurdu. Bunun yerine renk aynı kalır, alfa uzaklıkla düşer; gök silueti delmez, arkadan seçilir.
 * Saf Java'dır (Minecraft sınıflarına dokunmaz).
 */
public final class UzaklikSolmasi {
    /** Solmanın başladığı uzaklık, sis bitişinin katı. */
    public static final float BASLANGIC_CARPANI = 0.9f;
    /** Solmanın bittiği uzaklık, sis bitişinin katı. */
    public static final float BITIS_CARPANI = 3.0f;
    /** En uzakta kalan alfa oranı: kasırga hiçbir uzaklıkta tamamen kaybolmaz. */
    public static final float EN_AZ_ORAN = 0.45f;

    private UzaklikSolmasi() {
    }

    /** Sis bitişinden (blok) solmanın başlangıç uzaklığı; sis yoksa ({@code sisBitisi <= 1}) solma yoktur ({@code +∞}). */
    public static float baslangic(float sisBitisi) {
        return sisBitisi > 1f ? sisBitisi * BASLANGIC_CARPANI : Float.POSITIVE_INFINITY;
    }

    /** Alfa çarpanı: başlangıca dek 1, bitişte {@link #EN_AZ_ORAN}, arası yumuşak (smoothstep). */
    public static float carpan(float uzaklik, float sisBitisi) {
        float basla = baslangic(sisBitisi);
        if (!(uzaklik > basla)) {
            return 1f;
        }
        float bitir = sisBitisi * BITIS_CARPANI;
        float k = Math.max(0f, Math.min(1f, (uzaklik - basla) / (bitir - basla)));
        return 1f - (1f - EN_AZ_ORAN) * k * k * (3f - 2f * k);
    }
}
