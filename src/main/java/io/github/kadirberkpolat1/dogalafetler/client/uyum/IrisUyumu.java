package io.github.kadirberkpolat1.dogalafetler.client.uyum;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.uyum.Uyum;
import java.lang.reflect.Method;

/**
 * Iris / Oculus (gölgelendirici paketi) uyumu (K-E83). Mod, Iris'e derleme bağımlılığı olmadan, yüklü ise onun halka açık
 * API'sini ({@code net.irisshaders.iris.api.v0.IrisApi}) yansıma ile okur; yoksa ya da API değişmişse sessizce "paket yok"
 * sayar ve mod aynen çalışır.
 *
 * <p>Gök eğimi: gölgelendirici paketleri güneş yolunu {@code sunPathRotation} kadar yattırır ve Iris vanilla gökyüzünü
 * {@code Ry(-90) Rz(eğim) Rx(zaman)} sırasıyla çizer. Modun kendi gökyüzü ({@code renderSky} yerine geçince) bunu
 * bilmediğinden güneş, ay, yıldızlar ve 3 boyutlu cisimler paketin ışığından ve vanilla gökten farklı yerde çizilirdi; ay
 * çarpmasında ay bu yüzden "sıçrayıp" karşı tarafa geliyordu. {@link #gokEgimiDerece} aynı eğimi modun göğüne de uygulatır.
 * Yalnız istemci render thread'i.
 */
public final class IrisUyumu {
    private static final String API_SINIFI = "net.irisshaders.iris.api.v0.IrisApi";
    private static boolean hazir;
    private static Object api;
    private static Method paketVar;
    private static Method egimOku;

    private IrisUyumu() {
    }

    private static synchronized void hazirla() {
        if (hazir) {
            return;
        }
        hazir = true;
        if (!Uyum.yuklu("oculus") && !Uyum.yuklu("iris")) {
            return;
        }
        try {
            Class<?> sinif = Class.forName(API_SINIFI);
            api = sinif.getMethod("getInstance").invoke(null);
            paketVar = sinif.getMethod("isShaderPackInUse");
            egimOku = sinif.getMethod("getSunPathRotation");
            DogalAfetler.LOGGER.info("[uyum] Iris/Oculus API'si bulundu: gökyüzü paketin güneş yolu eğimine uyar");
        } catch (ReflectiveOperationException | LinkageError hata) {
            api = null;
            DogalAfetler.LOGGER.warn("[uyum] Iris/Oculus yüklü ama API'si okunamadı; gök eğimi 0 sayılır: {}", hata.toString());
        }
    }

    /** Bir gölgelendirici paketi şu an kullanılıyor mu. */
    public static boolean paketKullaniliyor() {
        hazirla();
        if (api == null) {
            return false;
        }
        try {
            return (Boolean) paketVar.invoke(api);
        } catch (ReflectiveOperationException | RuntimeException hata) {
            return false;
        }
    }

    /** Paketin güneş yolu eğimi (derece, {@code Rz}); paket yoksa 0 (vanilla). */
    public static float gokEgimiDerece() {
        if (!paketKullaniliyor()) {
            return 0f;
        }
        try {
            Object e = egimOku.invoke(api);
            return e instanceof Number n && Float.isFinite(n.floatValue()) ? n.floatValue() : 0f;
        } catch (ReflectiveOperationException | RuntimeException hata) {
            return 0f;
        }
    }
}
