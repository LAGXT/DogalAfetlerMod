package io.github.kadirberkpolat1.dogalafetler.client.meteor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarKayitlari;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAyarlari;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorGorunumu;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorKayitlari;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorOdulleri;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorSarsintiPaketi;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorVarligi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorOlcegi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.Sarsinti;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Meteorun istemci olayları: {@code meteor/sarsinti} paketiyle çarpma parçacıkları (şok halkası tozu, fışkıran blok enkazı,
 * erimiş kaya kıvılcımları, duman sütunu; suda sıçrama + buhar), uzaklıkla azalan beyaz parlama (4 tick) ve oyuncuya özgü
 * ekran sarsıntısı; meteor uçuş ve çarpma sesleri ({@link MeteorSesi}); yerdeki meteor ödüllerinin parıltısı (parça altın,
 * sıcakken alev; çekirdek mor-mavi ve büyü harfleri).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciMeteor {
    private static final int PARLAMA_TICK = 5;
    private static final double ODUL_MENZILI = 40;

    private static long saat;
    private static int parlamaKalan;
    private static float parlamaGucu;
    private static int sarsintiKalan;
    private static int sarsintiToplam;
    private static float sarsintiGenlik;
    private static int carpmaSayisi;

    private IstemciMeteor() {
    }

    /** Görsel otomasyon: alınan çarpma paketi sayısı. */
    public static int carpmaSayisi() {
        return carpmaSayisi;
    }

    public static Consumer<MeteorSarsintiPaketi> alici() {
        return IstemciMeteor::carpma;
    }

    private static void carpma(MeteorSarsintiPaketi p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        carpmaSayisi++;
        Vec3 nokta = new Vec3(p.x(), p.y(), p.z());
        double mesafe = mc.player.position().distanceTo(nokta);
        double boy = p.olcek();
        boolean ay = p.gorunumu() == MeteorGorunumu.AY;
        if (!p.suya()) {
            MeteorSesi.carpma(nokta, mc.player.getEyePosition().distanceTo(nokta), boy, ay);
            double menzil = MeteorOlcegi.ara(boy, 100, 200, 60, 260);
            double f = Math.max(0, 1 - mesafe / menzil);
            float guc = (float) (MeteorAyarlari.parlamaOlcegi() * MeteorOlcegi.ara(boy, 0.5, 0.85, 0.3, 1.0) * f * f);
            if (guc > 0.02F && guc >= parlamaGucu * (parlamaKalan / (float) PARLAMA_TICK)) {
                parlamaGucu = guc;
                parlamaKalan = PARLAMA_TICK;
            }
        }
        double olcek = MeteorAyarlari.sarsintiOlcegi();
        if (p.siddet() > 0 && olcek > 0) {
            float genlik = (float) (p.siddet() * 3.5 * olcek);
            if (genlik >= sarsintiGenlik * kalanOran()) {
                sarsintiToplam = Sarsinti.sureTick(p.siddet());
                sarsintiKalan = sarsintiToplam;
                sarsintiGenlik = genlik;
            }
        }
        double yogunluk = MeteorAyarlari.izYogunlugu() * MeteorVarligi.kaliteCarpani(IstemciAyarlari.efektKalitesi())
                * (mesafe > 160 ? 0.4 : 1.0);
        if (yogunluk > 0) {
            if (p.suya()) {
                suCarpmasi(level, nokta, boy, yogunluk);
            } else {
                karaCarpmasi(level, nokta, boy, p.gorunumu(), yogunluk);
            }
        }
    }

    private static float kalanOran() {
        return sarsintiToplam <= 0 ? 0 : sarsintiKalan / (float) sarsintiToplam;
    }

    /** Tipik küçük/büyük sayılar arası ölçekli parçacık sayısı (eksi değil). */
    private static double say(double boy, double kucuk, double buyuk) {
        return Math.max(0, MeteorOlcegi.ara(boy, kucuk, buyuk));
    }

    private static void karaCarpmasi(ClientLevel level, Vec3 n, double boy, MeteorGorunumu gorunum, double y) {
        boolean ay = gorunum == MeteorGorunumu.AY;
        RandomSource r = level.random;
        double cap = MeteorOlcegi.ara(boy, 1.1, 2.6, 0.6, 3.4);
        double halkaHizi = 4 * cap / 7.0;
        int halka = adet(r, say(boy, 26, 56) * y);
        for (int i = 0; i < halka; i++) {
            double aci = i * Math.PI * 2 / Math.max(1, halka) + r.nextDouble() * 0.2;
            double hiz = halkaHizi * (0.85 + r.nextDouble() * 0.3);
            level.addParticle(MeteorKayitlari.TOZ.get(), n.x + Math.cos(aci) * 0.6, n.y + 0.3, n.z + Math.sin(aci) * 0.6,
                    Math.cos(aci) * hiz, 0.01, Math.sin(aci) * hiz);
        }
        BlockState zemin = level.getBlockState(BlockPos.containing(n.x, n.y - 0.5, n.z));
        if (zemin.isAir()) {
            zemin = Blocks.DIRT.defaultBlockState();
        }
        BlockParticleOption enkaz = new BlockParticleOption(ParticleTypes.BLOCK, zemin);
        int enkazSayisi = adet(r, say(boy, 14, 30) * y);
        for (int i = 0; i < enkazSayisi; i++) {
            hizli(enkaz, n.add(0, 0.6, 0), yarikure(r, 0.25, 0.55, 1.05));
        }
        ParticleOptions kivilcim = gorunum == MeteorGorunumu.PULSAR ? PulsarKayitlari.KOR.get()
                : (ay ? MeteorKayitlari.KIVILCIM_AY : MeteorKayitlari.KIVILCIM).get();
        int kivilcimSayisi = adet(r, say(boy, 18, 44) * y);
        for (int i = 0; i < kivilcimSayisi; i++) {
            hizli(kivilcim, n.add(0, 0.5, 0), yarikure(r, 0.35, 0.3, 0.95));
        }
        ParticleOptions sicak = ay ? ParticleTypes.END_ROD : ParticleTypes.LAVA;
        int sicakSayisi = adet(r, say(boy, 5, 12) * y);
        for (int i = 0; i < sicakSayisi; i++) {
            level.addParticle(sicak, n.x + (r.nextDouble() - 0.5) * cap, n.y + 0.4, n.z + (r.nextDouble() - 0.5) * cap,
                    0, 0.1, 0);
        }
        ParticleOptions plazma = (ay ? MeteorKayitlari.PLAZMA_AY : MeteorKayitlari.PLAZMA).get();
        int plazmaSayisi = adet(r, say(boy, 12, 26) * y);
        for (int i = 0; i < plazmaSayisi; i++) {
            Vec3 v = yarikure(r, 0.25, 0.05, 0.3);
            level.addParticle(plazma, n.x, n.y + 0.5, n.z, v.x, v.y, v.z);
        }
        int duman = adet(r, say(boy, 10, 26) * y);
        for (int i = 0; i < duman; i++) {
            level.addParticle(MeteorKayitlari.DUMAN.get(), n.x + (r.nextDouble() - 0.5) * cap * 1.5,
                    n.y + 0.5 + r.nextDouble() * 1.5, n.z + (r.nextDouble() - 0.5) * cap * 1.5,
                    (r.nextDouble() - 0.5) * 0.05, 0.12 + r.nextDouble() * 0.22, (r.nextDouble() - 0.5) * 0.05);
        }
    }

    private static void suCarpmasi(ClientLevel level, Vec3 n, double boy, double y) {
        RandomSource r = level.random;
        int sicrama = adet(r, say(boy, 40, 90) * y);
        for (int i = 0; i < sicrama; i++) {
            hizli(ParticleTypes.SPLASH, n.add(0, 0.3, 0), yarikure(r, 0.3, 0.4, 0.9));
        }
        int buhar = adet(r, say(boy, 14, 36) * y);
        for (int i = 0; i < buhar; i++) {
            level.addParticle(ParticleTypes.CLOUD, n.x + (r.nextDouble() - 0.5) * 2, n.y + 0.4 + r.nextDouble(),
                    n.z + (r.nextDouble() - 0.5) * 2, (r.nextDouble() - 0.5) * 0.06, 0.08 + r.nextDouble() * 0.12,
                    (r.nextDouble() - 0.5) * 0.06);
        }
    }

    private static void hizli(ParticleOptions tur, Vec3 p, Vec3 v) {
        Particle parcacik = Minecraft.getInstance().particleEngine.createParticle(tur, p.x, p.y, p.z, v.x, v.y, v.z);
        if (parcacik != null) {
            parcacik.setParticleSpeed(v.x, v.y, v.z);
        }
    }

    /** Yukarı yarıkürede rastgele hız: yatay {@code yatay}, düşey {@code enAz}–{@code enCok}. */
    private static Vec3 yarikure(RandomSource r, double yatay, double enAz, double enCok) {
        double aci = r.nextDouble() * Math.PI * 2;
        double h = yatay * (0.3 + r.nextDouble() * 0.7);
        return new Vec3(Math.cos(aci) * h, enAz + r.nextDouble() * (enCok - enAz), Math.sin(aci) * h);
    }

    private static int adet(RandomSource r, double beklenen) {
        int tam = (int) beklenen;
        return tam + (r.nextDouble() < beklenen - tam ? 1 : 0);
    }

    /** Ekran parlamasının gücü (0–1). */
    static float parlama(float kismi) {
        if (parlamaKalan <= 0) {
            return 0;
        }
        float oran = Math.max(0, (parlamaKalan - kismi) / PARLAMA_TICK);
        return Math.min(1, parlamaGucu * oran);
    }

    /**
     * Çarpma parlaması: dünya çizimi bitince (el ve GUI'den önce) tam ekran sıcak beyaz örtü. GUI katmanı değildir; F1
     * ({@code hideGui}) iken de görünür (M-23). Derinlik ve sis yok; izdüşüm geçici olarak birim matris.
     */
    @SubscribeEvent
    public static void dunyaCizimi(RenderLevelStageEvent olay) {
        if (olay.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        float guc = parlama(olay.getPartialTick());
        if (guc <= 0.004F) {
            return;
        }
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack gorus = RenderSystem.getModelViewStack();
        gorus.pushPose();
        gorus.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder tampon = Tesselator.getInstance().getBuilder();
        tampon.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float r = 1.0F;
        float g = 0.965F;
        float b = 0.91F;
        tampon.vertex(-1, -1, 0).color(r, g, b, guc).endVertex();
        tampon.vertex(1, -1, 0).color(r, g, b, guc).endVertex();
        tampon.vertex(1, 1, 0).color(r, g, b, guc).endVertex();
        tampon.vertex(-1, 1, 0).color(r, g, b, guc).endVertex();
        BufferUploader.drawWithShader(tampon.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        gorus.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    @SubscribeEvent
    public static void kamera(ViewportEvent.ComputeCameraAngles olay) {
        if (sarsintiKalan <= 0 || sarsintiToplam <= 0) {
            return;
        }
        float t = (float) Math.max(0, Math.min(1, (sarsintiKalan - olay.getPartialTick()) / sarsintiToplam));
        float a = sarsintiGenlik * t * t;
        double z = saat + olay.getPartialTick();
        olay.setYaw(olay.getYaw() + a * (float) (Math.sin(z * 1.9) * 0.6 + Math.sin(z * 3.7 + 1) * 0.4));
        olay.setPitch(olay.getPitch() + a * (float) (Math.sin(z * 2.3 + 2) * 0.6 + Math.sin(z * 4.1) * 0.4));
        olay.setRoll(olay.getRoll() + a * 0.5F * (float) Math.sin(z * 2.9 + 0.5));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) {
            return;
        }
        saat++;
        if (parlamaKalan > 0) {
            parlamaKalan--;
        }
        if (sarsintiKalan > 0) {
            sarsintiKalan--;
        }
        if (mc.level != null && mc.player != null) {
            MeteorSesi.tick(mc);
            if (saat % 2 == 0) {
                odulParlamasi(mc.level, mc.player);
            }
        }
    }

    private static void odulParlamasi(ClientLevel level, Player oyuncu) {
        RandomSource r = level.random;
        Item parca = MeteorEsyaKayitlari.METEORIT_PARCASI.get();
        Item cekirdek = MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get();
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, oyuncu.getBoundingBox().inflate(ODUL_MENZILI))) {
            Item item = e.getItem().getItem();
            double x = e.getX();
            double y = e.getY() + 0.25;
            double z = e.getZ();
            if (item == parca) {
                if (r.nextFloat() < 0.7F) {
                    level.addParticle(MeteorKayitlari.PARILTI.get(), x + (r.nextDouble() - 0.5) * 0.5,
                            y + r.nextDouble() * 0.4, z + (r.nextDouble() - 0.5) * 0.5, 0, 0.012, 0);
                }
                if (MeteorOdulleri.sicakMi(e.getItem(), level.getGameTime())) {
                    if (r.nextFloat() < 0.5F) {
                        level.addParticle(ParticleTypes.SMALL_FLAME, x + (r.nextDouble() - 0.5) * 0.3, y,
                                z + (r.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
                    }
                    if (r.nextFloat() < 0.4F) {
                        level.addParticle(ParticleTypes.SMOKE, x, y + 0.2, z, 0, 0.04, 0);
                    }
                }
            } else if (item == cekirdek) {
                for (int i = 0; i < 2; i++) {
                    level.addParticle(MeteorKayitlari.KOZMIK.get(), x + (r.nextDouble() - 0.5) * 0.6,
                            y + r.nextDouble() * 0.5, z + (r.nextDouble() - 0.5) * 0.6, 0, 0.01, 0);
                }
                if (r.nextFloat() < 0.35F) {
                    level.addParticle(ParticleTypes.ENCHANT, x, y + 0.1, z, (r.nextDouble() - 0.5) * 1.6, 0.9,
                            (r.nextDouble() - 0.5) * 1.6);
                }
            }
        }
    }

    @SubscribeEvent
    public static void varlikGirdi(EntityJoinLevelEvent olay) {
        if (olay.getLevel().isClientSide() && olay.getEntity() instanceof MeteorVarligi meteor) {
            MeteorSesi.ekle(meteor);
        }
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        parlamaKalan = 0;
        sarsintiKalan = 0;
        sarsintiGenlik = 0;
        MeteorSesi.temizle();
    }
}
