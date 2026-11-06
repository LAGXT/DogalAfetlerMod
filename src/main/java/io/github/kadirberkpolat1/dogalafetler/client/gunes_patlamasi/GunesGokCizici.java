package io.github.kadirberkpolat1.dogalafetler.client.gunes_patlamasi;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik.GunesGorselEgrileri;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Çizim: aurora perdeleri (gök küresinde, kamera merkezli) ve görünüm uzayında tam ekran örtüler. Doku ve şader yok
 * ({@code position_color}); tasarım §5 kalite tablosu: aurora katmanı Düşük 1 / Orta 2 / Yüksek ve Ultra 3.
 */
final class GunesGokCizici {
    /** Ekran örtüleri: süpernova parlaması, ısı dalgalanması ve şafak tonu (0–1). */
    record Ortuler(float flas, float isi, float safak) {
        static final Ortuler YOK = new Ortuler(0f, 0f, 0f);

        boolean bos() {
            return flas < 0.003f && isi < 0.003f && safak < 0.003f;
        }
    }

    private static final float[] YESIL = {0x3C / 255f, 1f, 0x8A / 255f};
    private static final float[] KIZIL = {1f, 0x3C / 255f, 0x5A / 255f};
    /** Perdenin kuzeyden (−Z) iki yana yayıldığı açı, derece. */
    private static final float YAY = 100f;
    private static final int EN_COK_DILIM = 128;
    /** Sütun başına azimut, yarıçap, perde boyu ve parlaklık; yalnız render thread'i yazar (kare başına ayırma yok). */
    private static final float[] AZIMUT = new float[EN_COK_DILIM + 1];
    private static final float[] YARICAP = new float[EN_COK_DILIM + 1];
    private static final float[] BOY = new float[EN_COK_DILIM + 1];
    private static final float[] PARLAK = new float[EN_COK_DILIM + 1];

    private GunesGokCizici() {
    }

    static int katmanSayisi(EfektKalitesi k) {
        return switch (k) {
            case DUSUK -> 1;
            case ORTA -> 2;
            case YUKSEK, ULTRA -> 3;
        };
    }

    static int dilimSayisi(EfektKalitesi k) {
        return switch (k) {
            case DUSUK -> 48;
            case ORTA -> 64;
            case YUKSEK -> 96;
            case ULTRA -> 128;
        };
    }

    /**
     * Kuzey göğünde dikey aurora perdeleri: alt kenar yeşil, üst kenar kızıl, toplamalı; U yönünde kayan ışınlar ve
     * yavaşça dalgalanan yükseklik. {@code poz} kameranın dönüşüdür (gökyüzü gibi ötelemesiz).
     */
    static void aurora(Matrix4f poz, float guc, float zaman, EfektKalitesi kalite) {
        int katman = katmanSayisi(kalite);
        int dilim = dilimSayisi(kalite);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int k = 0; k < katman; k++) {
            float yaricap = 100f + 18f * k;
            float taban = 7f + 3f * k;
            float boy = 22f + 7f * k;
            float katmanGucu = guc * (1f - 0.22f * k);
            for (int n = 0; n <= dilim; n++) {
                float u = (float) n / dilim;
                AZIMUT[n] = (-YAY + 2f * YAY * u) * Mth.DEG_TO_RAD;
                YARICAP[n] = yaricap + 10f * Mth.sin(AZIMUT[n] * 3f + zaman * 0.002f + k);
                BOY[n] = boy * (0.72f + 0.28f * Mth.sin(n * 0.21f + zaman * 0.004f + k * 1.3f));
                PARLAK[n] = katmanGucu * Mth.sin((float) Math.PI * u) * GunesGorselEgrileri.isin(n, zaman, k);
            }
            for (int i = 0; i < dilim; i++) {
                int j = i + 1;
                float alt = taban - 1.5f;
                bant(b, poz, i, j, alt, alt, taban + 0.25f * BOY[i], taban + 0.25f * BOY[j], YESIL, 0f, YESIL, 0.7f);
                bant(b, poz, i, j, taban + 0.25f * BOY[i], taban + 0.25f * BOY[j], taban + 0.6f * BOY[i],
                        taban + 0.6f * BOY[j], YESIL, 0.7f, KIZIL, 0.45f);
                bant(b, poz, i, j, taban + 0.6f * BOY[i], taban + 0.6f * BOY[j], taban + BOY[i], taban + BOY[j], KIZIL,
                        0.45f, KIZIL, 0f);
            }
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** İki sütun arasında bir yükseklik bandı (alt ve üst kenar ayrı renk/alfa, sütunun parlaklığıyla). */
    private static void bant(BufferBuilder b, Matrix4f poz, int i, int j, float altI, float altJ, float ustI, float ustJ,
            float[] renkAlt, float alfaAlt, float[] renkUst, float alfaUst) {
        nokta(b, poz, AZIMUT[i], YARICAP[i], altI, renkAlt, alfaAlt * PARLAK[i]);
        nokta(b, poz, AZIMUT[j], YARICAP[j], altJ, renkAlt, alfaAlt * PARLAK[j]);
        nokta(b, poz, AZIMUT[j], YARICAP[j], ustJ, renkUst, alfaUst * PARLAK[j]);
        nokta(b, poz, AZIMUT[i], YARICAP[i], ustI, renkUst, alfaUst * PARLAK[i]);
    }

    /** Azimut kuzeyden (−Z) doğuya (+X), yükseklik açısı derece. */
    private static void nokta(BufferBuilder b, Matrix4f poz, float azimut, float yaricap, float yukseklik, float[] c,
            float alfa) {
        float el = yukseklik * Mth.DEG_TO_RAD;
        float yatay = yaricap * Mth.cos(el);
        b.vertex(poz, Mth.sin(azimut) * yatay, yaricap * Mth.sin(el), -Mth.cos(azimut) * yatay)
                .color(c[0], c[1], c[2], Mth.clamp(alfa, 0f, 1f)).endVertex();
    }

    /**
     * Görünüm uzayında (z = −1) tam ekran örtüler; F1 (gizli arayüz) ve üçüncü kişi görünümde de çizilir. Isı
     * dalgalanması ekranın alt yarısında yana kayan, titreşen sıcak şeritlerdir (şadersiz serap).
     */
    static void ekran(Matrix4f projeksiyon, Ortuler o, float zaman, EfektKalitesi kalite) {
        if (o.bos()) {
            return;
        }
        float gx = 1.2f / projeksiyon.m00();
        float gy = 1.2f / projeksiyon.m11();
        PoseStack mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        if (o.safak() > 0f) {
            dortgen(b, -gx, -gy, gx, gy, 1f, 0x6A / 255f, 0x3A / 255f, o.safak());
        }
        if (o.isi() > 0f) {
            int serit = switch (kalite) {
                case DUSUK -> 10;
                case ORTA -> 16;
                case YUKSEK -> 24;
                case ULTRA -> 32;
            };
            float ust = gy * 0.1f;
            float yukseklik = (ust + gy) / serit;
            for (int j = 0; j < serit; j++) {
                float y0 = -gy + j * yukseklik;
                float kayma = gx * 0.015f * Mth.sin(zaman * 0.25f + j * 1.3f);
                float alfa = o.isi() * 0.075f * (0.55f + 0.45f * Mth.sin(zaman * 0.17f + j * 2.1f))
                        * (1f - 0.6f * j / serit);
                dortgen(b, -gx + kayma, y0, gx + kayma, y0 + yukseklik * 0.6f, 1f, 0.78f, 0.5f, alfa);
            }
        }
        if (o.flas() > 0f) {
            dortgen(b, -gx, -gy, gx, gy, 0.92f, 0.96f, 1f, o.flas());
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        mv.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private static void dortgen(BufferBuilder b, float x0, float y0, float x1, float y1, float r, float g, float m, float a) {
        float alfa = Mth.clamp(a, 0f, 1f);
        b.vertex(x0, y0, -1f).color(r, g, m, alfa).endVertex();
        b.vertex(x1, y0, -1f).color(r, g, m, alfa).endVertex();
        b.vertex(x1, y1, -1f).color(r, g, m, alfa).endVertex();
        b.vertex(x0, y1, -1f).color(r, g, m, alfa).endVertex();
    }
}
