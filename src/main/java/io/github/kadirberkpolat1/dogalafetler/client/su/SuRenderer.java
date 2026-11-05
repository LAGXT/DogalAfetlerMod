package io.github.kadirberkpolat1.dogalafetler.client.su;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.su.DenizSeviyesiAlani;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import io.github.kadirberkpolat1.dogalafetler.su.SuAraliklari;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sanal su yüzeyi (şader'siz). Her oyun tick'inde kameranın çevresi bir {@link VertexBuffer}'a kurulur, her karede
 * {@code AFTER_TRANSLUCENT_BLOCKS} aşamasında vanilla {@code rendertype_translucent} şaderi, su dokusu, biyom su rengi
 * ve ışık haritasıyla çizilir. Yakın: 1 blok hücre + dalgacık + yan yüzler + köpük; uzak (LOD): 4x4 blok düz hücre.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SuRenderer {
    static final int YAKIN = 48;
    static final int LOD = 4;
    private static final ResourceLocation SU = ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");
    private static final int N = 2 * YAKIN + 1;
    private static final float[] UST = new float[N * N];
    private static final float[] ALT = new float[N * N];
    /** 0 yok, 1 sanal (dalgalı), 2 sanal (sakin, mağara), 3 gerçek su (yalnız yan yüz komşusu). */
    private static final byte[] TUR = new byte[N * N];
    private static final SuAraliklari A = new SuAraliklari();
    private static final BufferBuilder BB = new BufferBuilder(1 << 21);
    private static final BlockPos.MutableBlockPos P = new BlockPos.MutableBlockPos();
    private static VertexBuffer vb;
    private static boolean bos = true;
    private static long kurulanTick = Long.MIN_VALUE;
    private static int ox;
    private static int oz;
    private static float u0;
    private static float u1;
    private static float v0;
    private static float v1;
    private static int dortgen;
    private static long kurulumNs;
    private static int kurulumSayisi;
    private static int lodDortgen;
    private static long sonKareNs;
    private static long pencereBitisNs;
    private static long pencereMaksNs;
    private static int pencereKare;
    private static String pencereEtiket;

    private SuRenderer() {
    }

    /** Sonraki {@code ms} boyunca en uzun kare süresini ölçer ve log'a yazar (takılma ölçüsü). */
    public static void pencereBasla(String etiket, long ms) {
        pencereEtiket = etiket;
        pencereBitisNs = System.nanoTime() + ms * 1_000_000L;
        pencereMaksNs = 0;
        pencereKare = 0;
    }

    @SubscribeEvent
    public static void ciz(RenderLevelStageEvent e) {
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            kareOlc();
            return;
        }
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        SanalSuDunyasi d = SanalSu.dunya(l);
        if (d == null) {
            return;
        }
        long tick = l.getGameTime();
        if (tick != kurulanTick) {
            kurulanTick = tick;
            long t0 = System.nanoTime();
            kur(mc, l, d, e.getCamera(), tick + 0.0);
            kurulumNs += System.nanoTime() - t0;
            if (++kurulumSayisi % 200 == 0) {
                DogalAfetler.LOGGER.info("SPIKE-SU render kurulum ort={} ms dortgen={} (lod={})",
                        String.format("%.2f", kurulumNs / 200 / 1e6), dortgen, lodDortgen);
                kurulumNs = 0;
            }
        }
        if (bos) {
            return;
        }
        PoseStack ps = e.getPoseStack();
        Vec3 cam = e.getCamera().getPosition();
        ps.pushPose();
        ps.translate(ox - cam.x, -cam.y, oz - cam.z);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        mc.gameRenderer.lightTexture().turnOnLightLayer();
        vb.bind();
        vb.drawWithShader(ps.last().pose(), e.getProjectionMatrix(), GameRenderer.getRendertypeTranslucentShader());
        VertexBuffer.unbind();
        mc.gameRenderer.lightTexture().turnOffLightLayer();
        RenderSystem.enableCull();
        ps.popPose();
    }

    private static void kareOlc() {
        long simdi = System.nanoTime();
        if (sonKareNs != 0 && pencereBitisNs != 0) {
            if (simdi < pencereBitisNs) {
                pencereMaksNs = Math.max(pencereMaksNs, simdi - sonKareNs);
                pencereKare++;
            } else {
                DogalAfetler.LOGGER.info("SPIKE-SU kare etiket={} kare={} en_uzun_kare={} ms", pencereEtiket, pencereKare,
                        String.format("%.1f", pencereMaksNs / 1e6));
                pencereBitisNs = 0;
            }
        }
        sonKareNs = simdi;
    }

    private static double dalgacik(double x, double z, double t) {
        return 0.06 * Math.sin(0.55 * x + 0.08 * t) + 0.04 * Math.sin(0.37 * z - 0.06 * t + 1.3)
                + 0.03 * Math.sin(0.9 * (x + z) + 0.11 * t);
    }

    private static void kur(Minecraft mc, ClientLevel l, SanalSuDunyasi d, Camera camera, double t) {
        Vec3 cam = camera.getPosition();
        int cx = (int) Math.floor(cam.x);
        int cz = (int) Math.floor(cam.z);
        ox = cx;
        oz = cz;
        TextureAtlasSprite sp = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SU);
        u0 = sp.getU0();
        u1 = sp.getU1();
        v0 = sp.getV0();
        v1 = sp.getV1();
        DenizSeviyesiAlani alan = d.alan();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int x = cx - YAKIN + i;
                int z = cz - YAKIN + j;
                int k = i * N + j;
                d.araliklar(x, z, t, A);
                if (A.adet() > 0) {
                    int s = A.adet() - 1;
                    UST[k] = (float) A.ust(s);
                    ALT[k] = (float) A.alt(s);
                    TUR[k] = (byte) (A.sakin(s) ? 2 : 1);
                } else if (alan.su(x, z)) {
                    UST[k] = alan.zemin(x, z);
                    ALT[k] = alan.taban(x, z);
                    TUR[k] = 3;
                } else {
                    TUR[k] = 0;
                }
            }
        }
        BB.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        dortgen = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int k = i * N + j;
                if (TUR[k] != 0) {
                    sutun(l, alan, cx - YAKIN + i, cz - YAKIN + j, i, j, t);
                }
            }
        }
        lodDortgen = 0;
        int r = Math.min(mc.options.getEffectiveRenderDistance(), 12) * 16;
        int gx0 = Math.floorDiv(cx - r, LOD) * LOD;
        int gz0 = Math.floorDiv(cz - r, LOD) * LOD;
        int ic = YAKIN - LOD;
        for (int gx = gx0; gx <= cx + r; gx += LOD) {
            for (int gz = gz0; gz <= cz + r; gz += LOD) {
                int dx = gx + LOD / 2 - cx;
                int dz = gz + LOD / 2 - cz;
                if ((Math.abs(dx) < ic && Math.abs(dz) < ic) || dx * dx + dz * dz > r * r) {
                    continue;
                }
                d.araliklar(gx + LOD / 2, gz + LOD / 2, t, A);
                if (A.adet() == 0) {
                    continue;
                }
                float y = (float) A.ust(A.adet() - 1) - 0.16f;
                int renk = BiomeColors.getAverageWaterColor(l, P.set(gx, (int) y, gz));
                dortgenUst(gx - ox, gz - oz, LOD, y, y, y, y, renk, 0.72f, LightTexture.pack(0, 15));
                lodDortgen++;
            }
        }
        if (dortgen + lodDortgen == 0) {
            BB.end().release();
            bos = true;
            return;
        }
        if (vb == null) {
            vb = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        vb.bind();
        vb.upload(BB.end());
        VertexBuffer.unbind();
        bos = false;
    }

    private static int komsu(int i, int j) {
        return i < 0 || j < 0 || i >= N || j >= N ? -1 : i * N + j;
    }

    private static void sutun(ClientLevel l, DenizSeviyesiAlani alan, int x, int z, int i, int j, double t) {
        int k = i * N + j;
        byte tur = TUR[k];
        float yu = UST[k] - 0.11f;
        float alt = ALT[k];
        int[] di = {1, -1, 0, 0};
        int[] dj = {0, 0, 1, -1};
        int isik = LevelRenderer.getLightColor(l, P.set(x, (int) Math.floor(UST[k]), z));
        int renk = BiomeColors.getAverageWaterColor(l, P);
        float egim = 0;
        for (int q = 0; q < 4; q++) {
            int n = komsu(i + di[q], j + dj[q]);
            if (n >= 0 && TUR[n] != 0) {
                egim = Math.max(egim, Math.abs(UST[k] - UST[n]));
            }
        }
        float kopuk = tur == 1 ? Math.max(0, Math.min(0.85f, (egim - 0.6f) / 2.5f)) : 0;
        if (tur != 3) {
            float derin = UST[k] - alt;
            float a = Math.max(kopuk, Math.min(0.8f, 0.35f + 0.08f * derin));
            int c = karistir(renk, 0xFFFFFF, kopuk);
            float rx = x - ox;
            float rz = z - oz;
            if (tur == 1) {
                dortgenUst(rx, rz, 1, yu + (float) dalgacik(x, z, t), yu + (float) dalgacik(x, z + 1, t),
                        yu + (float) dalgacik(x + 1, z + 1, t), yu + (float) dalgacik(x + 1, z, t), c, a, isik);
            } else {
                dortgenUst(rx, rz, 1, yu, yu, yu, yu, c, a, isik);
            }
        }
        for (int q = 0; q < 4; q++) {
            int n = komsu(i + di[q], j + dj[q]);
            float taban;
            if (n >= 0 && TUR[n] != 0) {
                taban = Math.max(UST[n] - 0.11f, alt);
            } else {
                if (tur == 3) {
                    continue;
                }
                int zn = alan.zemin(x + di[q], z + dj[q]);
                if (zn != Integer.MIN_VALUE && zn >= UST[k]) {
                    continue;
                }
                taban = alt;
            }
            if (yu - taban < 0.05f) {
                continue;
            }
            float golge = q < 2 ? 0.6f : 0.8f;
            int c = karistir(carp(renk, golge), 0xFFFFFF, kopuk * 0.8f);
            float a = Math.max(0.55f, kopuk);
            float ex0;
            float ez0;
            float ex1;
            float ez1;
            float rx = x - ox;
            float rz = z - oz;
            switch (q) {
                case 0 -> {
                    ex0 = rx + 1; ez0 = rz; ex1 = rx + 1; ez1 = rz + 1;
                }
                case 1 -> {
                    ex0 = rx; ez0 = rz; ex1 = rx; ez1 = rz + 1;
                }
                case 2 -> {
                    ex0 = rx; ez0 = rz + 1; ex1 = rx + 1; ez1 = rz + 1;
                }
                default -> {
                    ex0 = rx; ez0 = rz; ex1 = rx + 1; ez1 = rz;
                }
            }
            dikey(ex0, ez0, ex1, ez1, taban, yu, c, a, isik);
        }
    }

    private static int carp(int c, float f) {
        return ((int) ((c >> 16 & 255) * f) << 16) | ((int) ((c >> 8 & 255) * f) << 8) | (int) ((c & 255) * f);
    }

    private static int karistir(int a, int b, float f) {
        int r = (int) ((a >> 16 & 255) * (1 - f) + (b >> 16 & 255) * f);
        int g = (int) ((a >> 8 & 255) * (1 - f) + (b >> 8 & 255) * f);
        int bl = (int) ((a & 255) * (1 - f) + (b & 255) * f);
        return (r << 16) | (g << 8) | bl;
    }

    private static void kose(float x, float y, float z, int c, float a, float u, float v, int isik, float ny) {
        BB.vertex(x, y, z).color((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, a).uv(u, v).uv2(isik)
                .normal(0, ny, 0).endVertex();
    }

    private static void dortgenUst(float x, float z, float b, float y00, float y01, float y11, float y10, int c, float a,
            int isik) {
        kose(x, y00, z, c, a, u0, v0, isik, 1);
        kose(x, y01, z + b, c, a, u0, v1, isik, 1);
        kose(x + b, y11, z + b, c, a, u1, v1, isik, 1);
        kose(x + b, y10, z, c, a, u1, v0, isik, 1);
        dortgen++;
    }

    private static void dikey(float x0, float z0, float x1, float z1, float alt, float ust, int c, float a, int isik) {
        float vb0 = v0 + (v1 - v0) * Math.min(1, ust - alt);
        kose(x0, ust, z0, c, a, u0, v0, isik, 0);
        kose(x1, ust, z1, c, a, u1, v0, isik, 0);
        kose(x1, alt, z1, c, a, u1, vb0, isik, 0);
        kose(x0, alt, z0, c, a, u0, vb0, isik, 0);
        dortgen++;
    }
}
