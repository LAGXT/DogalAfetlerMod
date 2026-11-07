package io.github.kadirberkpolat1.dogalafetler.client.meteor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorGorunumu;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorVarligi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorOlcegi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Meteorun çizimi (tasarım notu "Meteor"): dönen kaya kümesi gövde (güneş: magma + karataş/bazalt, ay: kalsit + bazalt/tüf),
 * önünde additive plazma kabuğu ve ışık halesi, arkasında iki katmanlı additive iz şeridi (içte beyaz-sarı / mavi-beyaz
 * çekirdek, dışta turuncu / mavi; uzakta dışı en az {@value #DIS_EN_AZ_PIKSEL}, içi {@value #IC_EN_AZ_PIKSEL} piksel),
 * inişe 1,5 sn kala yerde büyüyen parlama diski. Işık motoruna dokunulmaz.
 */
final class MeteorCizici extends EntityRenderer<MeteorVarligi> {
    private static final ResourceLocation PLAZMA = DogalAfetler.konum("textures/entity/meteor_plazmasi.png");
    private static final ResourceLocation IZ = DogalAfetler.konum("textures/entity/meteor_izi.png");
    private static final int IZ_PARCASI = 14;
    /** İz şeridinin ekranda en ince kalınlığı (piksel; MT2-32: 200 blok yukarıdaki küçük meteor da seçilsin). */
    private static final double DIS_EN_AZ_PIKSEL = 2.0;
    private static final double IC_EN_AZ_PIKSEL = 1.0;
    private static final double DISK_ONCESI = 30;
    /** Bunun ötesinde blok kümesi gövde çizilmez: uzaktan kare leke gibi okunuyordu, plazma panosu yeterli. */
    private static final double GOVDE_MESAFESI = 96;
    private static final float[][] PARCALAR = {{0.42F, 0.3F, 0.1F}, {-0.38F, 0.25F, -0.2F}, {0.05F, -0.4F, 0.35F},
        {-0.1F, 0.1F, -0.45F}};

    private final BlockRenderDispatcher bloklar;

    MeteorCizici(EntityRendererProvider.Context baglam) {
        super(baglam);
        this.bloklar = baglam.getBlockRenderDispatcher();
        this.shadowRadius = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorVarligi m) {
        return PLAZMA;
    }

    @Override
    public void render(MeteorVarligi m, float yaw, float kismi, PoseStack ps, MultiBufferSource tampon, int isik) {
        boolean ay = m.gorunum() == MeteorGorunumu.AY;
        double boyOlcegi = m.olcek();
        float olcek = (float) MeteorOlcegi.ara(boyOlcegi, 1.1, 2.6, 0.6, 3.4);
        Vec3 hiz = m.getDeltaMovement();
        if (hiz.lengthSqr() < 1e-8) {
            return;
        }
        Vec3 yon = hiz.normalize();
        Vec3 merkez = new Vec3(Mth.lerp(kismi, m.xo, m.getX()), Mth.lerp(kismi, m.yo, m.getY()), Mth.lerp(kismi, m.zo, m.getZ()));
        Vec3 goz = entityRenderDispatcher.camera.getPosition().subtract(merkez);
        float yas = m.yas() + kismi;

        VertexConsumer izTampon = tampon.getBuffer(MeteorIsikTipi.ISIK.apply(IZ));
        double uzunluk = m.izUzunlugu();
        double piksel = pikselAcisi();
        if (ay) {
            iz(ps, izTampon, yon, uzunluk, goz, 1.95F * olcek, DIS_EN_AZ_PIKSEL * piksel, 0.30F, 0.50F, 1.0F, 1.125F);
            iz(ps, izTampon, yon, uzunluk * 0.7, goz, 0.75F * olcek, IC_EN_AZ_PIKSEL * piksel, 0.85F, 0.94F, 1.0F, 1.5F);
        } else {
            iz(ps, izTampon, yon, uzunluk, goz, 1.95F * olcek, DIS_EN_AZ_PIKSEL * piksel, 1.0F, 0.42F, 0.10F, 1.125F);
            iz(ps, izTampon, yon, uzunluk * 0.7, goz, 0.75F * olcek, IC_EN_AZ_PIKSEL * piksel, 1.0F, 0.88F, 0.55F, 1.5F);
        }

        if (goz.lengthSqr() < GOVDE_MESAFESI * GOVDE_MESAFESI) {
            govde(m, ps, tampon, ay, olcek, yas);
        }

        VertexConsumer plazma = tampon.getBuffer(MeteorIsikTipi.ISIK.apply(PLAZMA));
        float titresim = 0.92F + 0.08F * Mth.sin(yas * 1.7F);
        if (ay) {
            pano(ps, plazma, yon.scale(0.35 * olcek), 2.0F * olcek * titresim, 0.75F, 0.88F, 1.0F, 1.0F);
            pano(ps, plazma, Vec3.ZERO, 5.0F * olcek, 0.20F, 0.32F, 0.60F, 0.6F);
        } else {
            pano(ps, plazma, yon.scale(0.35 * olcek), 2.0F * olcek * titresim, 1.0F, 0.92F, 0.62F, 1.0F);
            pano(ps, plazma, Vec3.ZERO, 5.0F * olcek, 0.65F, 0.30F, 0.08F, 0.6F);
        }

        Vec3 inis = m.inis();
        double kalan = m.inisKalanTick();
        if (inis != null && kalan >= 0 && kalan <= DISK_ONCESI) {
            float k = (float) (1 - kalan / DISK_ONCESI);
            float boy = (float) MeteorOlcegi.ara(boyOlcegi, 2.6, 5.5, 1.6, 7.0) * (0.3F + 0.7F * k);
            float guc = 0.25F + 0.75F * k;
            Vec3 p = inis.subtract(merkez).add(0, 0.04, 0);
            if (ay) {
                disk(ps, plazma, p, boy, 0.35F * guc, 0.55F * guc, 1.0F * guc);
            } else {
                disk(ps, plazma, p, boy, 1.0F * guc, 0.48F * guc, 0.12F * guc);
            }
        }
        super.render(m, yaw, kismi, ps, tampon, isik);
    }

    private void govde(MeteorVarligi m, PoseStack ps, MultiBufferSource tampon, boolean ay, float olcek, float yas) {
        BlockState ana = ay ? Blocks.CALCITE.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
        BlockState[] parcalar = ay
                ? new BlockState[] {Blocks.SMOOTH_BASALT.defaultBlockState(), Blocks.TUFF.defaultBlockState()}
                : new BlockState[] {Blocks.BLACKSTONE.defaultBlockState(), Blocks.MAGMA_BLOCK.defaultBlockState()};
        int tohum = m.getId();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(yas * 9F + tohum * 37));
        ps.mulPose(Axis.XP.rotationDegrees(yas * 6.5F + tohum * 11));
        ps.scale(olcek, olcek, olcek);
        blok(ps, tampon, ana, 0, 0, 0, 0.78F);
        for (int i = 0; i < PARCALAR.length; i++) {
            float[] p = PARCALAR[i];
            blok(ps, tampon, parcalar[(i + tohum) & 1], p[0], p[1], p[2], 0.5F);
        }
        ps.popPose();
    }

    private void blok(PoseStack ps, MultiBufferSource tampon, BlockState durum, float x, float y, float z, float boy) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Axis.ZP.rotationDegrees((x + y + z) * 120));
        ps.scale(boy, boy, boy);
        ps.translate(-0.5, -0.5, -0.5);
        bloklar.renderSingleBlock(durum, ps, tampon, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    /** Bir pikselin kameradan birim uzaklıktaki dünya boyu (dikey FOV ve pencere yüksekliğinden). */
    private static double pikselAcisi() {
        Minecraft mc = Minecraft.getInstance();
        return 2 * Math.tan(Math.toRadians(mc.options.fov().get()) / 2) / Math.max(1, mc.getWindow().getHeight());
    }

    /**
     * Kameraya dönük, baştan kuyruğa daralan ve sönen şerit (additive; renk parlaklıktır, 1'de kırpılır). Kalınlık her
     * noktada en az {@code enAzKalinlik} × kameraya uzaklıktır.
     */
    private static void iz(PoseStack ps, VertexConsumer vc, Vec3 yon, double uzunluk, Vec3 goz, float genislik,
            double enAzKalinlik, float r, float g, float b, float guc) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        Vec3 oncekiP = null;
        Vec3 oncekiYan = null;
        float oncekiT = 0;
        for (int i = 0; i <= IZ_PARCASI; i++) {
            float t = i / (float) IZ_PARCASI;
            Vec3 p = yon.scale(-uzunluk * t);
            Vec3 bakis = goz.subtract(p);
            Vec3 yan = yon.cross(bakis);
            if (yan.lengthSqr() < 1e-8) {
                yan = new Vec3(0, 1, 0).cross(yon);
            }
            yan = yan.normalize().scale(Math.max(genislik * (1 - 0.85F * t), enAzKalinlik * bakis.length()) * 0.5);
            if (oncekiP != null) {
                float a0 = guc * (1 - oncekiT);
                float a1 = guc * (1 - t);
                dortgen(m, n, vc, oncekiP.add(oncekiYan), oncekiP.subtract(oncekiYan), p.subtract(yan), p.add(yan),
                        oncekiT, t, r, g, b, a0, a1);
            }
            oncekiP = p;
            oncekiYan = yan;
            oncekiT = t;
        }
    }

    private static void dortgen(Matrix4f m, Matrix3f n, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float u1,
            float r, float g, float bl, float k0, float k1) {
        Vec3[] k = {a, b, c, d};
        float[] us = {u0, u0, u1, u1};
        float[] vs = {0, 1, 1, 0};
        float[] gs = {k0, k0, k1, k1};
        for (int yon = 0; yon < 2; yon++) {
            for (int j = 0; j < 4; j++) {
                int i = yon == 0 ? j : 3 - j;
                tepe(m, n, vc, k[i], Math.min(1, r * gs[i]), Math.min(1, g * gs[i]), Math.min(1, bl * gs[i]), us[i],
                        vs[i]);
            }
        }
    }

    private void pano(PoseStack ps, VertexConsumer vc, Vec3 ofset, float boy, float r, float g, float b, float guc) {
        ps.pushPose();
        ps.translate(ofset.x, ofset.y, ofset.z);
        ps.mulPose(entityRenderDispatcher.cameraOrientation());
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float h = boy / 2;
        Vec3 a = new Vec3(-h, -h, 0);
        Vec3 bb = new Vec3(h, -h, 0);
        Vec3 c = new Vec3(h, h, 0);
        Vec3 d = new Vec3(-h, h, 0);
        kare(m, n, vc, a, bb, c, d, r * guc, g * guc, b * guc);
        ps.popPose();
    }

    private static void disk(PoseStack ps, VertexConsumer vc, Vec3 p, float boy, float r, float g, float b) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float h = boy / 2;
        kare(m, n, vc, p.add(-h, 0, -h), p.add(h, 0, -h), p.add(h, 0, h), p.add(-h, 0, h), r, g, b);
    }

    private static void kare(Matrix4f m, Matrix3f n, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g,
            float bl) {
        Vec3[] k = {a, b, c, d};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int yon = 0; yon < 2; yon++) {
            for (int j = 0; j < 4; j++) {
                int i = yon == 0 ? j : 3 - j;
                tepe(m, n, vc, k[i], r, g, bl, uv[i][0], uv[i][1]);
            }
        }
    }

    private static void tepe(Matrix4f m, Matrix3f n, VertexConsumer vc, Vec3 p, float r, float g, float b, float u, float v) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, 1.0F).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 1, 0).endVertex();
    }
}
