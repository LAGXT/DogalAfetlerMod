package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Shader paketi köprüsünün kodlaması (DogalAfetlerShader, {@code shaders/lib/kopru.glsl} ile birebir). Oculus/Iris
 * {@code fogColor} uniform'unu {@code RenderSystem.getShaderFogColor()}'dan okur; mod durumu her kanalın 1/255'ten küçük
 * kesrine yazılır, görünen sis rengi değişmez. Kanal başına 12 bit: üstteki iki bit imza {@code 10}, altındaki 10 bit veri.
 *
 * <pre>
 * kırmızı: bit 0-2 güneş evresi sırası ({@link #EVRE_DONUS} = normal evreye geçiş), 3 ay var, 4 ay çarpması gökte, 5-9 ışık ölçeği
 * yeşil:   bit 0-4 ışık tonu kırmızı, 5-9 ışık tonu yeşil
 * mavi:    bit 0-4 ışık tonu mavi, 5 asit yağmuru, 6 tsunami, 7 sel, 8 kasırga, 9 orman yangını
 * </pre>
 *
 * Ölçek ve tonlar 0–{@value #TON_UST} aralığında 5 bitle nicelenir. Saf Java'dır.
 */
public final class ShaderKodu {
    public static final int ASIT = 1;
    public static final int TSUNAMI = 2;
    public static final int SEL = 4;
    public static final int KASIRGA = 8;
    public static final int ORMAN_YANGINI = 16;
    /**
     * Evre alanının yedinci değeri: evre normal ama mod göğü hâlâ çiziyor (kızıl cüceden eski güneşin dönüş geçişi). Paket ışığı
     * normal evreninki sayar, göğü moda bırakır; yoksa kendi göğünü çizip dönüşü siler.
     */
    public static final int EVRE_DONUS = 7;

    /** Kesrin çözünürlüğü: kanal değeri × 255'in kesri × 4096. */
    public static final int KESIR = 4096;
    /** İmza: veri 10 biti üstünde {@code 10} ikilisi. */
    public static final int IMZA = 2048;
    public static final float TON_UST = 1.55f;

    private ShaderKodu() {
    }

    /**
     * Sis rengini durumla kodlar. {@code isik} modun ışık haritası tonu {ölçek, kırmızı, yeşil, mavi}; {@code afetler}
     * bu sınıfın bayraklarının birleşimi.
     */
    public static float[] kodla(float r, float g, float b, int evreSirasi, boolean ayVar, boolean ayEtkin, float[] isik,
            int afetler) {
        int kirmizi = (evreSirasi & 7) | (ayVar ? 8 : 0) | (ayEtkin ? 16 : 0) | (nicele(isik[0]) << 5);
        int yesil = nicele(isik[1]) | (nicele(isik[2]) << 5);
        int mavi = nicele(isik[3]) | ((afetler & 31) << 5);
        return new float[] {kanal(r, kirmizi), kanal(g, yesil), kanal(b, mavi)};
    }

    /** 0–{@value #TON_UST} değerini 0–31'e. */
    static int nicele(float v) {
        float t = Math.max(0f, Math.min(1f, v / TON_UST));
        return Math.round(t * 31f);
    }

    /** Kanalı 1/255 ızgarasının altına indirir (en çok 254, kesir 1'i aşmasın) ve kesre imzalı veriyi yazar: kayma < 1/255. */
    static float kanal(float c, int veri) {
        int taban = Math.max(0, Math.min(254, (int) Math.floor(c * 255f)));
        return (taban + (IMZA + (veri & 1023) + 0.5f) / KESIR) / 255f;
    }
}
