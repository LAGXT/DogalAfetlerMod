package io.github.kadirberkpolat1.dogalafetler.client.asit_yagmuru;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Random;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Asit maruziyetinin ekran efekti (tasarım notu §2 "Oyuncu"; yazı değil, N6). Oyuncu asit yağmuru altında gökyüzüne açık
 * kaldıkça sürekli: sarı-yeşil kenar vinyeti ve kenarlardan döngüyle akan damla izleri, 0,5 sn'de belirir ve söner. Her
 * asit hasarında 0,3 sn güçlenen bir vuruş eklenir. Sürekli damlaların yeri sabit tohumdan, vuruşunki tetiklemede gelir.
 */
final class AsitEkranEfekti {
    private static final int SURE = 6;
    private static final int DAMLA = 10;
    private static final int SUREKLI_DAMLA = 7;
    private static final int SERIT = 8;
    private static final int DONGU = 40;
    /** Sürekli vinyetin vuruşa göre gücü. */
    private static final float TABAN = 0.45f;
    private static final float SOLMA = 0.1f;
    private static long baslangic = Long.MIN_VALUE;
    private static long tohum;
    private static float maruziyet;
    private static float oncekiMaruziyet;

    private AsitEkranEfekti() {
    }

    static void tetikle() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            baslangic = mc.level.getGameTime();
            tohum = mc.level.random.nextLong();
        }
    }

    /** Her istemci tick'inde: oyuncu şu an asit yağmuruna maruz kalıyor mu. */
    static void maruzGuncelle(boolean maruz) {
        oncekiMaruziyet = maruziyet;
        maruziyet = Mth.clamp(maruziyet + (maruz ? SOLMA : -SOLMA), 0f, 1f);
    }

    /** Otomasyon ve sahne sıfırlaması için: sürekli efektin şu anki gücü 0–1. */
    static float maruziyet() {
        return maruziyet;
    }

    static void sifirla() {
        maruziyet = 0f;
        oncekiMaruziyet = 0f;
        baslangic = Long.MIN_VALUE;
    }

    static void ciz(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }
        float gecen = mc.level.getGameTime() - baslangic + pt;
        float vurus = gecen < 0 || gecen > SURE * 2 ? 0f : gecen <= SURE ? 1f : 1f - (gecen - SURE) / SURE;
        float surekli = TABAN * Mth.lerp(pt, oncekiMaruziyet, maruziyet);
        float guc = Math.min(1f, surekli + vurus * (1f - surekli));
        if (guc <= 0f) {
            return;
        }
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
        if (surekli > 0f) {
            float zaman = (mc.level.getGameTime() + pt) / DONGU;
            damlalar(g, w, h, serit, new Random(0x41534954L), SUREKLI_DAMLA, (int) (170 * surekli / TABAN), zaman);
        }
        if (vurus > 0f) {
            damlalar(g, w, h, serit, new Random(tohum), DAMLA, (int) (200 * vurus), Mth.clamp(gecen / (SURE * 2f), 0f, 1f));
        }
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /**
     * Kenarlardan içe akan damla izleri. {@code zaman}'ın tam kısmı her damlaya ayrı bir faz verir (sürekli efektte damlalar
     * döngü boyunca kayar ve yeniden başlar); vuruşta {@code zaman} 0–1 ilerlemedir.
     */
    private static void damlalar(GuiGraphics g, int w, int h, int serit, Random r, int adet, int alfa, float zaman) {
        int renk = Mth.clamp(alfa, 0, 255) << 24 | 0xC8E04A;
        for (int i = 0; i < adet; i++) {
            int boy = 8 + r.nextInt(18);
            float faz = r.nextFloat();
            float ilerleme = zaman + faz - (float) Math.floor(zaman + faz);
            if (r.nextBoolean()) {
                int x = (int) (r.nextFloat() * w);
                int y = (int) (ilerleme * serit * 1.5f) - boy;
                g.fill(x, y, x + 2, y + boy, renk);
            } else {
                boolean sol = r.nextBoolean();
                int y = (int) (r.nextFloat() * h * 0.8f + ilerleme * h * 0.15f);
                int x = sol ? (int) (ilerleme * serit) : w - (int) (ilerleme * serit) - 2;
                g.fill(x, y, x + 2, y + boy, renk);
            }
        }
    }
}
