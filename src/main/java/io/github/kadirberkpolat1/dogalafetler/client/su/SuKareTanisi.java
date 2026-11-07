package io.github.kadirberkpolat1.dogalafetler.client.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import io.github.kadirberkpolat1.dogalafetler.su.SuAraliklari;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenshotEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Kare tanısı (SU-41): her ekran görüntüsünde (Forge {@link ScreenshotEvent}, otomasyondan bağımsız) kameranın sanal su
 * yüzeyine göre konumu, blok ve gök ışığı, su altı sisinin sıcak payı ve uzak düzlemi, son yapımın mağara tavan
 * yansıması ve kare kapısı bilgisi (FPS, son 20 tick'te çizilen kare; A-34) {@code SU-TANI} satırı olarak log'a yazılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SuKareTanisi {
    private static final int[] TICK_KARESI = new int[20];
    private static final SuAraliklari A = new SuAraliklari();
    private static int halka;
    private static int buTick;

    private SuKareTanisi() {
    }

    @SubscribeEvent
    public static void kare(TickEvent.RenderTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            buTick++;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            TICK_KARESI[halka] = buTick;
            halka = (halka + 1) % TICK_KARESI.length;
            buTick = 0;
        }
    }

    @SubscribeEvent
    public static void kareAlindi(ScreenshotEvent e) {
        Minecraft mc = Minecraft.getInstance();
        SanalSuDunyasi d = SanalSu.dunya(mc.level);
        if (d == null) {
            return;
        }
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 p = cam.getPosition();
        BlockPos kp = cam.getBlockPosition();
        d.araliklar((int) Math.floor(p.x), (int) Math.floor(p.z), IstemciDonem.saat(0f), A);
        double yuzey = A.adet() == 0 ? Double.NaN : A.ust(A.adet() - 1);
        DogalAfetler.LOGGER.info("SU-TANI kare={} kamera=({} {} {}) yuzey_y={} kamera_yuzeye_gore={} kamera_suda={} blok_isik={} "
                        + "gok_isik={} sicak={} sis_uzak={} {} fps={} son_20_tick_kare={}", e.getScreenshotFile().getName(),
                String.format(Locale.ROOT, "%.2f", p.x), String.format(Locale.ROOT, "%.2f", p.y),
                String.format(Locale.ROOT, "%.2f", p.z), String.format(Locale.ROOT, "%.2f", yuzey),
                String.format(Locale.ROOT, "%.2f", p.y - yuzey), IstemciSu.kameraSuda(cam),
                mc.level.getBrightness(LightLayer.BLOCK, kp), mc.level.getBrightness(LightLayer.SKY, kp),
                String.format(Locale.ROOT, "%.2f", IstemciSu.sonSicak()), String.format(Locale.ROOT, "%.1f", IstemciSu.sonSisUzak()),
                SuRenderer.yansimaOzeti(), mc.getFps(), Arrays.stream(TICK_KARESI).sum());
    }
}
