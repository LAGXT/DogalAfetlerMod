package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuDurumu;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GunesEvresi;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Evreye göre gök kubbesi, sis, uzay katmanı ve ışık haritası tonu (docs/tasarim/gokyuzu.md renk tablosu). Hepsi
 * yalnız görseldir: ışık değerleri, gün/gece döngüsü ve spawn kuralları değişmez (madde 16).
 */
final class GokyuzuRenkleri {
    private GokyuzuRenkleri() {
    }

    /** Vanilla {@code getSkyColor}'daki gündüz çarpanı: öğlen 1, gece 0. */
    static float gunIsigi(float zaman) {
        return Mth.clamp(Mth.cos(zaman * Mth.TWO_PI) * 2f + 0.5f, 0f, 1f);
    }

    /** Vanilla {@code getStarBrightness} ölçeğinden önceki ham yıldız görünürlüğü (gece 1). */
    static float geceligi(float zaman) {
        return Mth.clamp(1f - (Mth.cos(zaman * Mth.TWO_PI) * 2f + 0.25f), 0f, 1f);
    }

    /** Süpernovanın ilk parlaması (birkaç yüzde ilerlemede söner). */
    static float flas(float p) {
        return (float) Math.exp(-p / 0.06);
    }

    /** Süpernovanın üstel sönümü (1–2 gün). */
    static float parlaklik(float p) {
        return (float) Math.exp(-p / 0.35);
    }

    static float yumusak(float a, float b, float x) {
        float t = Mth.clamp((x - a) / (b - a), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private static float[] kar(float[] a, float[] b, float t) {
        return new float[] {Mth.lerp(t, a[0], b[0]), Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2])};
    }

    private static float[] olc(float[] a, float k) {
        return new float[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static float[] hex(int rgb) {
        return new float[] {((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f};
    }

    private static final float[] GECE_AYSIZ = hex(0x05070F);
    private static final float[] BUYUYEN_GOK = hex(0xE8D9A8);
    private static final float[] BUYUYEN_SIS = hex(0xF0D8A0);
    private static final float[] SN_PARLAK_GOK = hex(0xDDEBFF);
    private static final float[] SN_PARLAK_SIS = hex(0xC8DAF5);
    private static final float[] SN_KARANLIK_GOK = hex(0x0A0E1C);
    private static final float[] SN_KARANLIK_SIS = hex(0x1A2030);
    private static final float[] GUNESSIZ_GOK = hex(0x03050C);
    private static final float[] GUNESSIZ_SIS = hex(0x0D1220);
    private static final float[] KC_GUNDUZ_GOK = hex(0xB07A68);
    private static final float[] KC_GUNDUZ_SIS = hex(0xC08870);
    private static final float[] KC_GECE_GOK = hex(0x120A10);
    private static final float[] KC_GECE_SIS = hex(0x1A0E12);

    /**
     * Süpernovanın göğü aydınlatma oranı (gece güneş ufkun altında olduğu için zayıf). Gök beyaza doymaz: ışınlar ve
     * halka toplamalı çizildiği için arka plan orta tonda kalmalı.
     */
    private static float supernovaAydinlatma(float p, float gun) {
        return Mth.clamp(0.5f * parlaklik(p) * Math.max(gun, 0.3f) + 0.1f * flas(p), 0f, 1f);
    }

    static float[] gokRengi(GokyuzuDurumu d, float[] vanilla, float gun) {
        float p = d.ilerleme();
        return switch (d.evre()) {
            case NORMAL -> d.ayVar() ? vanilla : kar(vanilla, GECE_AYSIZ, (1f - gun) * 0.8f);
            case BUYUYEN -> {
                float[] r = kar(vanilla, olc(BUYUYEN_GOK, Math.max(gun, 0.05f)), p * gun * 0.85f);
                yield d.ayVar() ? r : kar(r, GECE_AYSIZ, (1f - gun) * 0.8f);
            }
            case SUPERNOVA -> kar(SN_KARANLIK_GOK, SN_PARLAK_GOK, supernovaAydinlatma(p, gun));
            case GUNESSIZ -> GUNESSIZ_GOK;
            case KIZIL_CUCE -> kar(KC_GECE_GOK, KC_GUNDUZ_GOK, gun);
        };
    }

    static float[] sisRengi(GokyuzuDurumu d, float[] vanilla, float gun) {
        float p = d.ilerleme();
        return switch (d.evre()) {
            case NORMAL -> d.ayVar() ? vanilla : kar(vanilla, GECE_AYSIZ, (1f - gun) * 0.6f);
            case BUYUYEN -> kar(vanilla, olc(BUYUYEN_SIS, Math.max(gun, 0.15f)), p * gun * 0.7f);
            case SUPERNOVA -> kar(SN_KARANLIK_SIS, SN_PARLAK_SIS, supernovaAydinlatma(p, gun));
            case GUNESSIZ -> GUNESSIZ_SIS;
            case KIZIL_CUCE -> kar(KC_GECE_SIS, KC_GUNDUZ_SIS, gun);
        };
    }

    /** Derin uzay katmanının (yıldızlar, Samanyolu, galaksi, nebula) görünürlüğü. */
    static float uzayAlfa(GokyuzuDurumu d, float zaman) {
        float gun = gunIsigi(zaman);
        float p = d.ilerleme();
        return switch (d.evre()) {
            case NORMAL, BUYUYEN -> d.ayVar() ? geceligi(zaman) * geceligi(zaman) * 0.5f : geceligi(zaman);
            case SUPERNOVA -> Mth.clamp(1f - 0.75f * parlaklik(p) * Math.max(gun, 0.25f), 0.15f, 1f);
            case GUNESSIZ -> 1f;
            case KIZIL_CUCE -> Mth.lerp(gun, 1f, 0.45f);
        };
    }

    /** Samanyolu, galaksiler ve gece nebulaları çizilsin mi (ayın olduğu normal/büyüyen gecede yalnız yıldız). */
    static boolean zengin(GokyuzuDurumu d) {
        return !(d.ayVar() && (d.evre() == GunesEvresi.NORMAL || d.evre() == GunesEvresi.BUYUYEN));
    }

    /** Süpernova kalıntısı nebulasının görünürlüğü. */
    static float kalintiAlfa(GokyuzuDurumu d) {
        return switch (d.evre()) {
            case SUPERNOVA -> yumusak(0.08f, 1f, d.ilerleme());
            case GUNESSIZ -> 1f;
            case KIZIL_CUCE -> 0.85f;
            default -> 0f;
        };
    }

    /**
     * Işık haritası rengini tonlar: yalnız gökyüzü ışığının RENK katkısı ölçeklenir. Işık seviyeleri (0–15), mob
     * spawn ve vanilla mantığı bu dokuyu kullanmaz; bunlar değişmez.
     */
    static void isikHaritasi(GokyuzuDurumu d, float gokKararmasi, float gokIsigi, Vector3f renk) {
        float k;
        float tr;
        float tg;
        float tb;
        switch (d.evre()) {
            case GUNESSIZ -> {
                k = 0.35f;
                tr = 0.75f;
                tg = 0.85f;
                tb = 1.15f;
            }
            case SUPERNOVA -> {
                k = Mth.lerp(parlaklik(d.ilerleme()), 0.4f, 1.1f);
                tr = 0.9f;
                tg = 0.95f;
                tb = 1.1f;
            }
            case KIZIL_CUCE -> {
                k = 0.75f;
                tr = 1.1f;
                tg = 0.82f;
                tb = 0.68f;
            }
            default -> {
                return;
            }
        }
        // Vanilla'da gökyüzü katkısı = (s, s, 1) * gokIsigi, s = kararma + (1 - kararma) * 0.35.
        float s = gokKararmasi + (1f - gokKararmasi) * 0.35f;
        float gr = s * gokIsigi;
        float gb = gokIsigi;
        renk.set(Math.max(0f, renk.x - gr + gr * k * tr),
                Math.max(0f, renk.y - gr + gr * k * tg),
                Math.max(0f, renk.z - gb + gb * k * tb));
    }
}
