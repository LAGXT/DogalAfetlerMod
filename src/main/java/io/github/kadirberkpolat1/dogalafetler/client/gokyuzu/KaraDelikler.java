package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.vertex.BufferBuilder;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Derin uzaydaki kara delikler (gökyüzü-2; M87/EHT görünümü) ve dönen halkaları. Boylar belirgin farklıdır: uzaktakiler
 * {@code 0,4°}, yakındakiler {@code 1,7–3,4°}, tek bir <b>devasa</b> kara delik {@value #DEVASA_DERECE}° (en yakın sınıf; her
 * zaman çizilir, bakılması başarım verir). Bazılarının kutuplarından yukarı-aşağı kısa gama ışını jeti ({@code jet} &gt; 0;
 * jet uzunluğu kara delik yarıçapının 1,9–3×'ı) fışkırır. Katman katman, her katman tüm kara delikler için tek çizimde (en
 * fazla yedi çizim): jet (yumuşak) → gama ışını (ince, parlak, nöbetçi) → gölge (yıldızları örter) → akış dokusu (döner)
 * → eğik akresyon diski, dış ve iç katman (elips; iç katman daha hızlı döner, Doppler parlaklığı bir yanda) → ışık bükülmesi
 * (mercek) halkası (foton halkası + alt/üst yaylar). Disk düzlemi jet eksenine diktir. Düşük kalitede yalnız gölge ve mercek
 * halkası. Sayı: kaliteye göre taban ({@link #taban}); olay şiddeti yükselince ({@link GokyuzuCizim#kritik}) {@link #azami}
 * sayıya kadar artış, her yeni kara delik ayrı ayrı yumuşak belirir; devasa kara delik bunların dışında her zaman çizilir.
 * Normal gece (ay var) bu sınıfı hiç çağırılmaz.
 */
final class KaraDelikler {
    private static final ResourceLocation KARA_DELIK = GokyuzuCizim.doku("kara_delik");
    private static final ResourceLocation AKIS = GokyuzuCizim.doku("kara_delik_akis");
    private static final ResourceLocation JET = GokyuzuCizim.doku("kara_delik_jet");
    private static final ResourceLocation GAMA = GokyuzuCizim.doku("kara_delik_gama");
    private static final ResourceLocation DISK = GokyuzuCizim.doku("kara_delik_disk");
    private static final ResourceLocation MERCEK = GokyuzuCizim.doku("kara_delik_mercek");
    private static final ResourceLocation HALE = GokyuzuCizim.doku("hale");

    /** Dev kara deliğin açısal çapı (derece): sıradan yakın kara deliğin iki katından büyük. */
    static final float DEVASA_DERECE = 7.5f;
    /** Dış diskin yarı boyu (gölge kuadının yarı boyunun katı). */
    private static final float DISK_OLCEK = 2.8f;
    /** İç disk dış diskin bu katı boyda ve {@value #IC_HIZ} kat hızlı döner. */
    private static final float IC_OLCEK = 0.6f;
    private static final float IC_HIZ = 2.6f;
    private static final float MERCEK_OLCEK = 1.25f;
    /** Foton halkasının sıcak ışıltısı (hale dokusu) mercek kuadının bu katı genişliktedir. */
    private static final float PARILTI_OLCEK = 2.4f;
    /** Olay şiddeti bu eşiğin üzerinde ek kara delikler belirmeye başlar. */
    private static final float ESIK = 0.3f;

    /**
     * Gök küresinde sabit kara delik: açısal çap {@code derece}; jet ve Doppler yönü {@code donus}; akış dokusunun dönüşü
     * {@code akis} °/sn; disk eğimi {@code egim} (küçük/büyük eksen oranı); disk dönüşü {@code diskHiz} °/sn (işareti dönüş
     * yönü, tam sayı: 3600 sn'de tam tur sayısı tam olur); {@code jet}: kutup jetinin yarı uzunluğu (kara delik yarıçapının
     * katı, 0 = jet yok).
     */
    private record KaraDelik(float az, float el, float derece, float donus, float akis, float alfa, float egim,
            float diskHiz, float jet) {
        Vector3f yon() {
            return GokyuzuCizim.yon(az, el);
        }
    }

    /**
     * İlk dördü eski konumlarını korur (görsel otomasyon dizin 0-2'yi kadraj olarak kullanır). Boylar: 0 ve 2 yakın, 1 uzak.
     * Jetliler: 0, 2, 5, 7.
     */
    private static final KaraDelik[] LISTE = {
            new KaraDelik(75, -40, 3.4f, 25f, 14f, 1f, 0.30f, 10f, 2.6f),
            new KaraDelik(250, -32, 0.55f, -40f, -18f, 0.9f, 0.26f, -12f, 0f),
            new KaraDelik(165, -68, 2.3f, 70f, 22f, 0.9f, 0.34f, 9f, 2.2f),
            new KaraDelik(330, 55, 0.7f, 10f, -12f, 0.85f, 0.28f, -8f, 0f),
            new KaraDelik(60, -18, 0.45f, -65f, 16f, 0.85f, 0.32f, 11f, 0f),
            new KaraDelik(120, -28, 1.7f, 35f, -20f, 0.9f, 0.24f, -10f, 2.8f),
            new KaraDelik(222, -44, 0.5f, -15f, 25f, 0.8f, 0.36f, 12f, 0f),
            new KaraDelik(272, -52, 1.2f, 50f, -14f, 0.9f, 0.27f, -9f, 3.0f),
            new KaraDelik(350, -35, 0.4f, -30f, -16f, 0.8f, 0.33f, 8f, 0f),
            new KaraDelik(100, -52, 0.85f, 80f, 18f, 0.85f, 0.29f, -11f, 0f),
    };

    /** Devasa kara delik: gece göğünün tepesine yakın, dönen, kısa jetli; her zaman çizilir. */
    private static final KaraDelik DEVASA = new KaraDelik(355, -66, DEVASA_DERECE, 20f, 9f, 1f, 0.30f, 6f, 1.9f);

    private static final KaraDelik[] SEC = new KaraDelik[LISTE.length + 1];
    private static final float[] ALFA = new float[LISTE.length + 1];

    private KaraDelikler() {
    }

    /** {@code i}. kara deliğin gök çerçevesindeki birim yönü (dizin döngüsel). */
    static Vector3f yon(int i) {
        return LISTE[Math.floorMod(i, LISTE.length)].yon();
    }

    /** Devasa kara deliğin gök çerçevesindeki birim yönü (bakış algısı için). */
    static Vector3f devasaYon() {
        return DEVASA.yon();
    }

    /** Normal olay şiddetinde çizilen kara delik sayısı (kaliteye göre; devasa hariç). */
    static int taban(EfektKalitesi kalite) {
        return switch (kalite) {
            case DUSUK -> 1;
            case ORTA -> 2;
            case ULTRA -> 4;
            default -> 3;
        };
    }

    /** Yüksek olay şiddetinde çizilen en fazla kara delik (kaliteye göre, en fazla {@code LISTE.length}; devasa hariç). */
    static int azami(EfektKalitesi kalite) {
        return switch (kalite) {
            case DUSUK -> 3;
            case ORTA -> 5;
            case ULTRA -> LISTE.length;
            default -> 7;
        };
    }

    /**
     * Tüm kara delikleri çizer; toplamalı karma açık gelir ve açık kalır. {@code m}: derin uzay matrisi.
     *
     * @param kritik {@link GokyuzuCizim#kritik}
     */
    static void ciz(Matrix4f m, float uzay, float sn, float kritik, EfektKalitesi kalite) {
        boolean sade = kalite == EfektKalitesi.DUSUK;
        int taban = taban(kalite);
        int azami = azami(kalite);
        float f = Mth.clamp((kritik - ESIK) / (1f - ESIK), 0f, 1f);
        float gorunur = taban + (azami - taban) * f;
        int n = Math.min(azami, (int) Math.ceil(gorunur - 1e-3f));
        for (int i = 0; i < n; i++) {
            SEC[i] = LISTE[i];
            ALFA[i] = uzay * LISTE[i].alfa * Mth.clamp(gorunur - i, 0f, 1f);
        }
        SEC[n] = DEVASA;
        ALFA[n] = uzay * DEVASA.alfa;
        int say = n + 1;
        if (!sade) {
            boolean jetVar = false;
            for (int i = 0; i < say; i++) {
                jetVar |= SEC[i].jet > 0f;
            }
            if (jetVar) {
                BufferBuilder yumusak = GokyuzuCizim.baslat(JET);
                for (int i = 0; i < say; i++) {
                    KaraDelik k = SEC[i];
                    if (k.jet <= 0f) {
                        continue;
                    }
                    float yari = yari(k);
                    float titresim = 0.8f + 0.2f * Mth.sin(Mth.TWO_PI * sn / 8f + i * 2.1f);
                    GokyuzuCizim.egikKuadEkle(yumusak, m, k.yon(), yari * 0.34f, yari * k.jet, k.donus, 0f, 1f, 0.93f, 0.62f,
                            0.6f * ALFA[i] * titresim, 0.6f * ALFA[i] * titresim);
                }
                GokyuzuCizim.bitir(yumusak);
                // Ek katman: ince, parlak gama ışını çekirdeği; 3 sn'lik nöbetle boyu ve parlaklığı değişir.
                BufferBuilder gama = GokyuzuCizim.baslat(GAMA);
                for (int i = 0; i < say; i++) {
                    KaraDelik k = SEC[i];
                    if (k.jet <= 0f) {
                        continue;
                    }
                    float yari = yari(k);
                    float nobet = 0.5f + 0.5f * Mth.sin(Mth.TWO_PI * sn / 3f + i * 1.3f);
                    float boy = yari * k.jet * (0.78f + 0.22f * nobet);
                    float parlak = 0.55f + 0.45f * nobet;
                    GokyuzuCizim.egikKuadEkle(gama, m, k.yon(), yari * 0.24f, boy, k.donus, 0f, 1f, 1f, 0.85f,
                            ALFA[i] * parlak, ALFA[i] * parlak);
                }
                GokyuzuCizim.bitir(gama);
            }
        }
        GokyuzuCizim.normal();
        BufferBuilder golge = GokyuzuCizim.baslat(KARA_DELIK);
        for (int i = 0; i < say; i++) {
            KaraDelik k = SEC[i];
            float yari = yari(k);
            GokyuzuCizim.egikKuadEkle(golge, m, k.yon(), yari, yari, k.donus, 0f, 1f, 1f, 1f, ALFA[i], ALFA[i]);
        }
        GokyuzuCizim.bitir(golge);
        GokyuzuCizim.toplamali();
        if (!sade) {
            BufferBuilder akis = GokyuzuCizim.baslat(AKIS);
            for (int i = 0; i < say; i++) {
                KaraDelik k = SEC[i];
                float yari = yari(k);
                GokyuzuCizim.egikKuadEkle(akis, m, k.yon(), yari, yari, k.donus + sn * k.akis, 0f, 1f, 0.78f, 0.45f,
                        0.65f * ALFA[i], 0.65f * ALFA[i]);
            }
            GokyuzuCizim.bitir(akis);
            BufferBuilder disk = GokyuzuCizim.baslat(DISK);
            for (int i = 0; i < say; i++) {
                KaraDelik k = SEC[i];
                float yari = yari(k) * DISK_OLCEK;
                // Yaklaşan taraf (dönüş yönüne göre) parlak, uzaklaşan soluk: sol/sağ köşe alfası.
                float parlak = ALFA[i];
                float soluk = 0.3f * ALFA[i];
                float sol = k.diskHiz > 0f ? soluk : parlak;
                float sag = k.diskHiz > 0f ? parlak : soluk;
                GokyuzuCizim.egikKuadEkle(disk, m, k.yon(), yari, yari * k.egim, k.donus, sn * k.diskHiz, 1f, 1f, 1f, sol, sag);
            }
            for (int i = 0; i < say; i++) {
                KaraDelik k = SEC[i];
                float yari = yari(k) * DISK_OLCEK * IC_OLCEK;
                float parlak = ALFA[i];
                float soluk = 0.35f * ALFA[i];
                float sol = k.diskHiz > 0f ? soluk : parlak;
                float sag = k.diskHiz > 0f ? parlak : soluk;
                GokyuzuCizim.egikKuadEkle(disk, m, k.yon(), yari, yari * k.egim, k.donus, sn * k.diskHiz * IC_HIZ, 1f, 0.93f,
                        0.82f, sol, sag);
            }
            GokyuzuCizim.bitir(disk);
        }
        // Foton halkasının çevresinde sıcak, geniş bir ışıltı (gerçek kara delik görüntülerindeki parlak halka).
        BufferBuilder parilti = GokyuzuCizim.baslat(HALE);
        for (int i = 0; i < say; i++) {
            KaraDelik k = SEC[i];
            float yari = yari(k) * MERCEK_OLCEK * PARILTI_OLCEK;
            float nefes = 0.85f + 0.15f * Mth.sin(Mth.TWO_PI * sn / 8f + i * 1.7f);
            GokyuzuCizim.egikKuadEkle(parilti, m, k.yon(), yari, yari, 0f, 0f, 1f, 0.78f, 0.36f, 0.38f * ALFA[i] * nefes,
                    0.38f * ALFA[i] * nefes);
        }
        GokyuzuCizim.bitir(parilti);
        BufferBuilder mercek = GokyuzuCizim.baslat(MERCEK);
        for (int i = 0; i < say; i++) {
            KaraDelik k = SEC[i];
            float yari = yari(k) * MERCEK_OLCEK;
            float nefes = 0.85f + 0.15f * Mth.sin(Mth.TWO_PI * sn / 8f + i * 1.7f);
            GokyuzuCizim.egikKuadEkle(mercek, m, k.yon(), yari, yari, k.donus, 0f, 1f, 1f, 1f, ALFA[i] * nefes,
                    ALFA[i] * nefes);
        }
        GokyuzuCizim.bitir(mercek);
    }

    private static float yari(KaraDelik k) {
        return 100f * (float) Math.tan(Math.toRadians(k.derece) / 2);
    }
}
