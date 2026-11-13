package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Gökyüzü animasyon katmanlarının (kayan yıldız, kara delik diski, galaksi kolu, gezegen) ortak çizim araçları: dokulu
 * {@code QUADS} yazıcısı (aynı dokuyu kullanan tüm nesneler tek çizimde), eğik/dönen dokulu gök kuadı, belirleyici
 * karma (hash) ve olay şiddeti. Animasyon açıları {@code sn} (3600 sn'de sarılan oyun saati) ile üretilir; döngü
 * süreleri 3600'ü böler, sarılmada sıçrama olmaz. Yalnız render thread'i.
 */
final class GokyuzuCizim {
    private GokyuzuCizim() {
    }

    static ResourceLocation doku(String ad) {
        return DogalAfetler.konum("textures/environment/gokyuzu/" + ad + ".png");
    }

    /** Toplamalı karma (SRC_ALPHA, ONE): ışık yayan katmanlar. */
    static void toplamali() {
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    /** Normal alfa karışımı: arkayı örten katmanlar (gölge, gezegen). */
    static void normal() {
        RenderSystem.defaultBlendFunc();
    }

    /** Dokulu, köşe renkli {@code QUADS} topluşu başlatır; en az bir kuad eklenip {@link #bitir} çağrılmalı. */
    static BufferBuilder baslat(ResourceLocation doku) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, doku);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        return bb;
    }

    static void bitir(BufferBuilder bb) {
        BufferUploader.drawWithShader(bb.end());
    }

    /** Gök küresinde (yükseklik {@code el}, yön {@code az}; derece) birim yön. */
    static Vector3f yon(float az, float el) {
        float a = az * Mth.DEG_TO_RAD;
        float e = el * Mth.DEG_TO_RAD;
        return new Vector3f(Mth.cos(e) * Mth.cos(a), Mth.sin(e), Mth.cos(e) * Mth.sin(a));
    }

    /** {@code periyotSn} saniyelik döngünün derece açısı (0..360). */
    static float dongu(float sn, float periyotSn) {
        return sn % periyotSn / periyotSn * 360f;
    }

    /** (a, b, tuz) üçlüsünden belirleyici [0,1) sayı; her istemci aynı kayan yıldız/parıltı düzenini görür. */
    static float rastgele(int a, int b, int tuz) {
        int h = a * 0x9E3779B1 + b * 0x85EBCA6B + tuz * 0xC2B2AE35;
        h ^= h >>> 16;
        h *= 0x7FEB352D;
        h ^= h >>> 15;
        h *= 0x846CA68B;
        h ^= h >>> 16;
        return (h >>> 8) / (float) (1 << 24);
    }

    /**
     * Gök küresine teğet kuad ekler: merkez {@code yon*100}; {@code yariU}/{@code yariV} yarı boylar; kuadın kendisi
     * {@code egim} derece döndürülür (elips büyük ekseni), doku {@code uvAci} derece kendi düzleminde döndürülür (elipsin
     * içinde dönen halka). Sol/sağ köşe alfası {@code aSol}/{@code aSag} (Doppler parlaklığı). Doku dairesi kuadın içine
     * çizilmiş olmalı (kenar alfası 0).
     */
    static void egikKuadEkle(BufferBuilder bb, Matrix4f m, Vector3f yon, float yariU, float yariV, float egim,
            float uvAci, float r, float g, float b, float aSol, float aSag) {
        Vector3f ref = Math.abs(yon.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(ref).cross(yon).normalize();
        Vector3f v = new Vector3f(yon).cross(u).normalize();
        float dr = egim * Mth.DEG_TO_RAD;
        float cd = Mth.cos(dr);
        float sd = Mth.sin(dr);
        float ux = (u.x * cd + v.x * sd) * yariU;
        float uy = (u.y * cd + v.y * sd) * yariU;
        float uz = (u.z * cd + v.z * sd) * yariU;
        float vx = (v.x * cd - u.x * sd) * yariV;
        float vy = (v.y * cd - u.y * sd) * yariV;
        float vz = (v.z * cd - u.z * sd) * yariV;
        float cx = yon.x * 100f;
        float cy = yon.y * 100f;
        float cz = yon.z * 100f;
        float ua = uvAci * Mth.DEG_TO_RAD;
        float ca = Mth.cos(ua);
        float sa = Mth.sin(ua);
        for (int k = 0; k < 4; k++) {
            float sx = k == 1 || k == 2 ? 1f : -1f;
            float sy = k >= 2 ? 1f : -1f;
            float x = 0.5f * sx;
            float y = 0.5f * sy;
            bb.vertex(m, cx + sx * ux + sy * vx, cy + sx * uy + sy * vy, cz + sx * uz + sy * vz)
                    .uv(0.5f + x * ca - y * sa, 0.5f + x * sa + y * ca)
                    .color(r, g, b, sx < 0f ? aSol : aSag).endVertex();
        }
    }

    /**
     * Gökyüzü olay şiddeti [0,1]: normal ay'lı gece 0,25; ay yok 0,6; süpernova, güneşsiz, pulsar, kara delik yutması 1;
     * kızıl cüce 0,85. Evre geçişinde {@code gecis} ile yumuşar. Kara delik sayısı, kayan yıldız sıklığı bununla artar.
     */
    static float kritik(IstemciGokyuzu.Gorunum d) {
        float yeni = evreKritik(d.evre(), d.ayVar());
        if (d.onceki() == null || d.gecis() >= 1f) {
            return yeni;
        }
        return Mth.lerp(d.gecis(), evreKritik(d.onceki(), d.ayVar()), yeni);
    }

    private static float evreKritik(GunesEvresi evre, boolean ayVar) {
        return switch (evre) {
            case SUPERNOVA, GUNESSIZ, PULSAR, KARADELIK_YUTMA -> 1f;
            case KIZIL_CUCE -> 0.85f;
            case NORMAL, BUYUYEN -> ayVar ? 0.25f : 0.6f;
        };
    }
}
