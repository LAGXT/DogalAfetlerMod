package io.github.kadirberkpolat1.dogalafetler.client.asit_yagmuru;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Random;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Asit hasarı anında ekran efekti (tasarım notu §2 "Oyuncu"; yazı değil, N6): 0,3 sn hafif sarı-yeşil kenar vinyeti ve
 * kenarlardan içe akan kısa damla izleri. Damla yerleri tetiklemede tohumlanır.
 */
final class AsitEkranEfekti {
    private static final int SURE = 6;
    private static final int DAMLA = 10;
    private static final int SERIT = 8;
    private static long baslangic = Long.MIN_VALUE;
    private static long tohum;

    private AsitEkranEfekti() {
    }

    static void tetikle() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            baslangic = mc.level.getGameTime();
            tohum = mc.level.random.nextLong();
        }
    }

    static void ciz(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }
        float gecen = mc.level.getGameTime() - baslangic + pt;
        if (gecen < 0 || gecen > SURE * 2) {
            return;
        }
        float guc = gecen <= SURE ? 1f : 1f - (gecen - SURE) / SURE;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int serit = Math.max(4, Math.min(w, h) / 6);
        for (int i = 0; i < SERIT; i++) {
            int a = (int) (90 * guc * (1f - i / (float) SERIT));
            int renk = a << 24 | 0xB4D23C;
            int k = serit * i / SERIT;
            int k2 = serit * (i + 1) / SERIT;
            g.fill(0, k, w, k2, renk);
            g.fill(0, h - k2, w, h - k, renk);
            g.fill(k, k2, k2, h - k2, renk);
            g.fill(w - k2, k2, w - k, h - k2, renk);
        }
        Random r = new Random(tohum);
        float ilerleme = Mth.clamp(gecen / (SURE * 2f), 0f, 1f);
        int damlaRengi = (int) (200 * guc) << 24 | 0xC8E04A;
        for (int i = 0; i < DAMLA; i++) {
            int boy = 8 + r.nextInt(18);
            if (r.nextBoolean()) {
                int x = (int) (r.nextFloat() * w);
                int y = (int) (ilerleme * serit * 1.5f) - boy;
                g.fill(x, y, x + 2, y + boy, damlaRengi);
            } else {
                boolean sol = r.nextBoolean();
                int y = (int) (r.nextFloat() * h * 0.8f + ilerleme * h * 0.15f);
                int x = sol ? (int) (ilerleme * serit) : w - (int) (ilerleme * serit) - 2;
                g.fill(x, y, x + 2, y + boy, damlaRengi);
            }
        }
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
