package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KaraDelikYutmaEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarEgrileri;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Evreye göre gök kubbesi, sis, uzay katmanı ve ışık haritası tonu (docs/tasarim/gokyuzu.md renk tablosu, spike G2/G5).
 * Evre değişiminde önceki evrenin son rengiyle 20 sn karıştırılır. Hepsi yalnız görseldir: ışık değerleri, gün/gece
 * döngüsü ve spawn kuralları değişmez (madde 16). Pulsar yolunda ({@code yol}) GUNESSIZ ve KIZIL_CUCE kalıntı nebulasız.
 */
final class GokyuzuRenkleri {
    private static final int GECE_AYSIZ = 0x05070F;
    private static final int BUYUYEN_GOK = 0xE8D9A8;
    private static final int BUYUYEN_SIS = 0xF0D8A0;
    private static final int SN_PARLAK_GOK = 0xDDEBFF;
    private static final int SN_PARLAK_SIS = 0xC8DAF5;
    private static final int SN_KARANLIK_GOK = 0x0A0E1C;
    private static final int SN_KARANLIK_SIS = 0x1A2030;
    private static final int GUNESSIZ_GOK = 0x03050C;
    private static final int GUNESSIZ_SIS = 0x0D1220;
    private static final int KC_GUNDUZ_GOK = 0xB07A68;
    private static final int KC_GUNDUZ_SIS = 0xC08870;
    private static final int KC_GECE_GOK = 0x120A10;
    private static final int KC_GECE_SIS = 0x1A0E12;
    /** Pulsar (PL-2): gündüz kavrulmuş turuncu-kızıl pus, gece koyu kan kırmızısı; sis yakın tonlarda. */
    private static final int PULSAR_GECE_GOK = 0x2A0608;
    private static final int PULSAR_GECE_SIS = 0x240608;
    private static final int PULSAR_GUNDUZ_GOK = 0xC0401A;
    private static final int PULSAR_GUNDUZ_SIS = 0xA83A1A;
    /** Kara delik yutması (PL-4): koyu mor → simsiyah, parlamada mor-beyaz. */
    private static final int KD_MOR_GOK = 0x1C0A2E;
    private static final int KD_MOR_SIS = 0x160822;
    private static final int KD_SIYAH_GOK = 0x000000;
    private static final int KD_SIYAH_SIS = 0x020104;
    private static final int KD_PARLAMA_GOK = 0xE8DAFF;
    private static final int KD_PARLAMA_SIS = 0xD8C8FF;
    private static final float[] PULSAR_ISIK_TAM = {1.1f, 1.3f, 0.72f, 0.5f};
    private static final float[] KD_ISIK_MOR = {0.6f, 0.9f, 0.7f, 1.2f};
    private static final float[] GUNESSIZ_ISIK = {0.35f, 0.75f, 0.85f, 1.15f};

    private GokyuzuRenkleri() {
    }

    /** Vanilla {@code getSkyColor}'daki gündüz çarpanı: öğlen 1, gece 0. */
    static float gunIsigi(float zaman) {
        return Mth.clamp(Mth.cos(zaman * Mth.TWO_PI) * 2f + 0.5f, 0f, 1f);
    }

    /** Vanilla yıldız görünürlüğünün ham hali (gece 1). */
    static float geceligi(float zaman) {
        return Mth.clamp(1f - (Mth.cos(zaman * Mth.TWO_PI) * 2f + 0.25f), 0f, 1f);
    }

    /** Süpernovanın göğü aydınlatma oranı; gök beyaza doymaz ki ışın ve halka okunsun. */
    static float supernovaAydinlatma(float p, float saniye, float gun) {
        return Mth.clamp(0.38f * GokyuzuEgrileri.supernovaParlaklik(p) * Math.max(gun, 0.3f)
                + 0.25f * GokyuzuEgrileri.supernovaFlas(saniye), 0f, 1f);
    }

    private static float[] tekGok(GunesEvresi evre, float p, float saniye, boolean ay, float[] v, float gun, float kc) {
        return switch (evre) {
            case NORMAL -> ay ? v : GokyuzuEgrileri.karistir(v, GokyuzuEgrileri.rgb(GECE_AYSIZ), (1f - gun) * 0.8f);
            case BUYUYEN -> {
                float[] b = GokyuzuEgrileri.rgb(BUYUYEN_GOK);
                float k = Math.max(gun, 0.05f);
                float[] r = GokyuzuEgrileri.karistir(v, new float[] {b[0] * k, b[1] * k, b[2] * k}, p * gun * 0.85f);
                yield ay ? r : GokyuzuEgrileri.karistir(r, GokyuzuEgrileri.rgb(GECE_AYSIZ), (1f - gun) * 0.8f);
            }
            case SUPERNOVA -> GokyuzuEgrileri.karistir(SN_KARANLIK_GOK, SN_PARLAK_GOK, supernovaAydinlatma(p, saniye, gun));
            case GUNESSIZ -> GokyuzuEgrileri.rgb(GUNESSIZ_GOK);
            case KIZIL_CUCE -> GokyuzuEgrileri.karistir(v, GokyuzuEgrileri.karistir(KC_GECE_GOK, KC_GUNDUZ_GOK, gun), kc);
            case PULSAR -> pulsarRengi(PULSAR_GECE_GOK, PULSAR_GUNDUZ_GOK, GUNESSIZ_GOK, p, gun);
            case KARADELIK_YUTMA -> yutmaRengi(pulsarRengi(PULSAR_GECE_GOK, PULSAR_GUNDUZ_GOK, GUNESSIZ_GOK, 1f, gun),
                    KD_MOR_GOK, KD_SIYAH_GOK, KD_PARLAMA_GOK, saniye);
        };
    }

    /** Pulsar pusu: gün ışığıyla geceden gündüze; ısınma başında güneşsiz dönemin karanlığından belirir. */
    private static float[] pulsarRengi(int gece, int gunduz, int karanlik, float p, float gun) {
        float[] pus = GokyuzuEgrileri.karistir(gece, gunduz, gun);
        return GokyuzuEgrileri.karistir(GokyuzuEgrileri.rgb(karanlik), pus, 0.35f + 0.65f * PulsarEgrileri.yogunluk(p));
    }

    /** Yutma: pulsar pusundan koyu mora (yaklaşma), simsiyaha (yutum), parlamada mor-beyaz. */
    private static float[] yutmaRengi(float[] pus, int mor, int siyah, int parlama, float s) {
        float[] r = GokyuzuEgrileri.karistir(pus, GokyuzuEgrileri.rgb(mor), KaraDelikYutmaEgrileri.morluk(s));
        r = GokyuzuEgrileri.karistir(r, GokyuzuEgrileri.rgb(siyah), KaraDelikYutmaEgrileri.karanlik(s));
        return GokyuzuEgrileri.karistir(r, GokyuzuEgrileri.rgb(parlama), 0.6f * KaraDelikYutmaEgrileri.parlama(s));
    }

    private static float[] tekSis(GunesEvresi evre, float p, float saniye, boolean ay, float[] v, float gun, float kc) {
        return switch (evre) {
            case NORMAL -> ay ? v : GokyuzuEgrileri.karistir(v, GokyuzuEgrileri.rgb(GECE_AYSIZ), (1f - gun) * 0.6f);
            case BUYUYEN -> {
                float[] b = GokyuzuEgrileri.rgb(BUYUYEN_SIS);
                float k = Math.max(gun, 0.15f);
                yield GokyuzuEgrileri.karistir(v, new float[] {b[0] * k, b[1] * k, b[2] * k}, p * gun * 0.7f);
            }
            case SUPERNOVA -> GokyuzuEgrileri.karistir(SN_KARANLIK_SIS, SN_PARLAK_SIS, supernovaAydinlatma(p, saniye, gun));
            case GUNESSIZ -> GokyuzuEgrileri.rgb(GUNESSIZ_SIS);
            case KIZIL_CUCE -> GokyuzuEgrileri.karistir(v, GokyuzuEgrileri.karistir(KC_GECE_SIS, KC_GUNDUZ_SIS, gun), kc);
            case PULSAR -> pulsarRengi(PULSAR_GECE_SIS, PULSAR_GUNDUZ_SIS, GUNESSIZ_SIS, p, gun);
            case KARADELIK_YUTMA -> yutmaRengi(pulsarRengi(PULSAR_GECE_SIS, PULSAR_GUNDUZ_SIS, GUNESSIZ_SIS, 1f, gun),
                    KD_MOR_SIS, KD_SIYAH_SIS, KD_PARLAMA_SIS, saniye);
        };
    }

    static float[] gokRengi(IstemciGokyuzu.Gorunum g, float[] vanilla, float gun) {
        float[] yeni = tekGok(g.evre(), g.ilerleme(), g.saniye(), g.ayVar(), vanilla, gun, g.renkGucu());
        if (g.onceki() == null || g.gecis() >= 1f) {
            return yeni;
        }
        float[] eski = tekGok(g.onceki(), 1f, 1e6f, g.ayVar(), vanilla, gun, g.renkGucu());
        return GokyuzuEgrileri.karistir(eski, yeni, g.gecis());
    }

    static float[] sisRengi(IstemciGokyuzu.Gorunum g, float[] vanilla, float gun) {
        float[] yeni = tekSis(g.evre(), g.ilerleme(), g.saniye(), g.ayVar(), vanilla, gun, g.renkGucu());
        if (g.onceki() == null || g.gecis() >= 1f) {
            return yeni;
        }
        float[] eski = tekSis(g.onceki(), 1f, 1e6f, g.ayVar(), vanilla, gun, g.renkGucu());
        return GokyuzuEgrileri.karistir(eski, yeni, g.gecis());
    }

    private static float tekUzay(GunesEvresi evre, float p, float saniye, boolean ay, float zaman) {
        float gun = gunIsigi(zaman);
        float gece = geceligi(zaman);
        return switch (evre) {
            case NORMAL, BUYUYEN -> ay ? gece * gece * 0.5f : gece;
            case SUPERNOVA -> Mth.clamp(1f - 0.75f * GokyuzuEgrileri.supernovaParlaklik(p) * Math.max(gun, 0.25f)
                    - 0.6f * GokyuzuEgrileri.supernovaFlas(saniye), 0.15f, 1f);
            case GUNESSIZ -> 1f;
            case KIZIL_CUCE -> Mth.lerp(gun, 1f, 0.45f);
            case PULSAR -> Mth.lerp(gun, 1f, 0.5f);
            case KARADELIK_YUTMA -> Math.max(Mth.lerp(gun, 1f, 0.5f), KaraDelikYutmaEgrileri.uzayTabani(saniye));
        };
    }

    /** Derin uzay katmanının (yıldız, Samanyolu, galaksi, nebula) görünürlüğü. */
    static float uzayAlfa(IstemciGokyuzu.Gorunum g, float zaman) {
        float yeni = tekUzay(g.evre(), g.ilerleme(), g.saniye(), g.ayVar(), zaman);
        if (g.onceki() == null || g.gecis() >= 1f) {
            return yeni;
        }
        return Mth.lerp(g.gecis(), tekUzay(g.onceki(), 1f, 1e6f, g.ayVar(), zaman), yeni);
    }

    /** Samanyolu, galaksi ve gece nebulaları (ayın olduğu normal/büyüyen gecede yalnız yıldız). */
    static boolean zengin(IstemciGokyuzu.Gorunum g) {
        return !(g.ayVar() && (g.evre() == GunesEvresi.NORMAL || g.evre() == GunesEvresi.BUYUYEN));
    }

    private static float tekKalinti(GunesEvresi evre, float p, float saniye, boolean yol) {
        return switch (evre) {
            case SUPERNOVA -> GokyuzuEgrileri.kalintiBelirme(p);
            case GUNESSIZ -> yol ? 0f : 1f;
            case KIZIL_CUCE -> yol ? 0f : 0.85f;
            case PULSAR -> 0.85f;
            case KARADELIK_YUTMA -> 0.85f * KaraDelikYutmaEgrileri.kalinti(saniye);
            default -> 0f;
        };
    }

    static float kalintiAlfa(IstemciGokyuzu.Gorunum g) {
        float yeni = tekKalinti(g.evre(), g.ilerleme(), g.saniye(), g.pulsarYolu());
        return g.onceki() == null || g.gecis() >= 1f ? yeni
                : Mth.lerp(g.gecis(), tekKalinti(g.onceki(), 1f, 1e6f, g.pulsarYolu()), yeni);
    }

    private static float[] tekIsik(GunesEvresi evre, float p, float saniye) {
        return switch (evre) {
            case GUNESSIZ -> GUNESSIZ_ISIK.clone();
            case SUPERNOVA -> new float[] {Mth.lerp(GokyuzuEgrileri.supernovaParlaklik(p), 0.4f, 1.1f), 0.9f, 0.95f, 1.1f};
            case KIZIL_CUCE -> new float[] {0.75f, 1.1f, 0.82f, 0.68f};
            case PULSAR -> new float[] {Mth.lerp(PulsarEgrileri.yogunluk(p), 0.85f, PULSAR_ISIK_TAM[0]), PULSAR_ISIK_TAM[1],
                    PULSAR_ISIK_TAM[2], PULSAR_ISIK_TAM[3]};
            case KARADELIK_YUTMA -> {
                float mor = KaraDelikYutmaEgrileri.morluk(saniye);
                float karanlik = KaraDelikYutmaEgrileri.karanlik(saniye);
                float[] t = new float[4];
                for (int i = 0; i < 4; i++) {
                    t[i] = Mth.lerp(karanlik, Mth.lerp(mor, PULSAR_ISIK_TAM[i], KD_ISIK_MOR[i]), GUNESSIZ_ISIK[i]);
                }
                yield t;
            }
            default -> new float[] {1f, 1f, 1f, 1f};
        };
    }

    /** Işık haritası tonu {ölçek, kırmızı, yeşil, mavi}; görünüm başına bir kez hesaplanır. */
    static float[] isikTonu(GunesEvresi evre, GunesEvresi onceki, float gecis, float ilerleme, float saniye,
            float renkGucu) {
        float[] t = tekIsik(evre, ilerleme, saniye);
        if (onceki != null && gecis < 1f) {
            float[] e = tekIsik(onceki, 1f, 1e6f);
            if (onceki == GunesEvresi.KIZIL_CUCE) {
                for (int i = 0; i < 4; i++) {
                    e[i] = Mth.lerp(renkGucu, 1f, e[i]);
                }
            }
            for (int i = 0; i < 4; i++) {
                t[i] = Mth.lerp(gecis, e[i], t[i]);
            }
        }
        if (evre == GunesEvresi.KIZIL_CUCE) {
            for (int i = 0; i < 4; i++) {
                t[i] = Mth.lerp(renkGucu, 1f, t[i]);
            }
        }
        return t;
    }

    /**
     * Işık haritası dokusunda yalnız gökyüzü ışığının RENK katkısını tonlar (G5). Işık seviyeleri, mob spawn ve vanilla
     * mantığı bu dokuyu okumaz.
     */
    static void isikHaritasi(IstemciGokyuzu.Gorunum g, float gokKararmasi, float gokIsigi, Vector3f renk) {
        float[] t = g.isik();
        float s = gokKararmasi + (1f - gokKararmasi) * 0.35f;
        float gr = s * gokIsigi;
        float gb = gokIsigi;
        renk.set(Math.max(0f, renk.x - gr + gr * t[0] * t[1]), Math.max(0f, renk.y - gr + gr * t[0] * t[2]),
                Math.max(0f, renk.z - gb + gb * t[0] * t[3]));
    }
}
