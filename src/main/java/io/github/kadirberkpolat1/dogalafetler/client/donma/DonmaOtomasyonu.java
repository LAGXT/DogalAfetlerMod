package io.github.kadirberkpolat1.dogalafetler.client.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaApi;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaZamanlayici;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Donma görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=donma} ile, tek oyunculu Quick Play
 * dünyasında çalışır; üretimde etkisizdir. Yakındaki bir gölü (yoksa nehir/kıyıyı) bulur, kıyısındaki tepeden ona bakar
 * ve sırayla: vitrin (güneşsiz evre, G 30'dan gerçek donma süreciyle {@code hiz 20} altında %55'e, kar yağışı; kıyıdan
 * içeri donan göl %35, %45 ve %55'te, %47'de üstten), %100 donmuş manzara, büyüyen güneşte buharlaşmış göl yatağı ve
 * süpernovada geri dolması, kızıl cücede bir kalıcı kış sınırında çözülme (G=40). Güneş evresi entegre sunucuda doğrudan
 * ayarlanır (evre komutu gökyüzü dalında). Aralarda {@code /forge tps} ve {@code /dogalafet donma bilgi} çıktıları log'a
 * düşer. Karelerde arayüz gizlidir. Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır; kendi açmadığı duraklatma
 * ekranını kapatır; adımlar durum koşuluyla ilerler.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class DonmaOtomasyonu {
    private static final String MOD = System.getProperty("dogalafetler.otomasyon", "");
    private static final String ON = "DONMA_OTOMASYON ";

    private record Adim(String ad, Runnable is, BooleanSupplier hazir, int enCokTick) {
    }

    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static int tus;
    private static int fare;
    private static int teker;
    private static int sayac;
    private static int adim = -1;
    private static int adimTick;
    private static int bosTick;
    private static int[] gol;
    private static BlockPos sinir;

    private DonmaOtomasyonu() {
    }

    private static boolean aktif() {
        return MOD.equals("donma");
    }

    @SubscribeEvent
    public static void tusa(InputEvent.Key olay) {
        if (aktif()) {
            tus++;
            DogalAfetler.LOGGER.warn(ON + "girdi: tuş {} eylem {}", olay.getKey(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void fareye(InputEvent.MouseButton.Pre olay) {
        if (aktif()) {
            fare++;
            DogalAfetler.LOGGER.warn(ON + "girdi: fare {} eylem {}", olay.getButton(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void tekerlege(InputEvent.MouseScrollingEvent olay) {
        if (aktif()) {
            teker++;
            DogalAfetler.LOGGER.warn(ON + "girdi: tekerlek {}", olay.getScrollDelta());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (!aktif() || olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PauseScreen) {
            DogalAfetler.LOGGER.info(ON + "kendi açmadığım duraklatma ekranı kapatıldı");
            mc.setScreen(null);
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            return;
        }
        sayac++;
        bosTick = DonmaZamanlayici.kuyrukBos() ? bosTick + 1 : 0;
        if (adim < 0) {
            if (sayac < 200 || !mc.player.hasPermissions(2)) {
                return;
            }
            plan(mc);
            adim = 0;
            adimTick = 0;
            DogalAfetler.LOGGER.info(ON + "başlıyor: {} adım", ADIMLAR.size());
            ADIMLAR.get(0).is().run();
            return;
        }
        if (adim >= ADIMLAR.size()) {
            return;
        }
        adimTick++;
        Adim a = ADIMLAR.get(adim);
        boolean hazir = a.hazir().getAsBoolean();
        if (hazir || adimTick >= a.enCokTick()) {
            if (!hazir) {
                DogalAfetler.LOGGER.warn(ON + "{}: {} tick'te koşul sağlanmadı, devam", a.ad(), adimTick);
            }
            adim++;
            adimTick = 0;
            if (adim < ADIMLAR.size()) {
                ADIMLAR.get(adim).is().run();
            }
        }
    }

    private static void ekle(String ad, Runnable is, BooleanSupplier hazir, int enCokTick) {
        ADIMLAR.add(new Adim(ad, is, hazir, enCokTick));
    }

    private static void komut(Minecraft mc, String k) {
        DogalAfetler.LOGGER.info(ON + "komut /{}", k);
        mc.player.connection.sendCommand(k);
    }

    private static BooleanSupplier bekle(int tick) {
        return () -> adimTick >= tick;
    }

    /** İstemcideki donma ilerlemesi en az {@code g} (sunucu paketinden). */
    private static BooleanSupplier gEnAz(float g) {
        return () -> IstemciDonem.durum().donmaIlerlemesi() >= g;
    }

    /** Kuyruk 60 tick boyunca boş (yetişme bitti) ve en az {@code enAz} tick geçti. */
    private static BooleanSupplier yetisti(int enAz) {
        return () -> adimTick >= enAz && bosTick >= 60;
    }

    private static void plan(Minecraft mc) {
        ekle("kurulum", () -> {
            for (String k : new String[] {"gamemode spectator", "gamerule doDaylightCycle false", "time set 6000",
                    "weather clear 100000", "gamerule doWeatherCycle false", "dogalafet donma 0 donma",
                    "dogalafet buharlasma 0"}) {
                komut(mc, k);
            }
        }, yetisti(100), 2400);
        ekle("göl ara", () -> {
            gol = golBul(mc.level, mc.player.blockPosition(), 112);
            DogalAfetler.LOGGER.info(ON + "göl: {}", gol == null ? "bulunamadı"
                    : String.format(Locale.ROOT, "merkez (%d, %d) yüzey Y=%d alan %d yarıçap %d", gol[0], gol[1], gol[2],
                            gol[3], gol[4]));
            yanBak(mc);
        }, bekle(120), 200);
        ekle("G 30, güneşsiz", () -> {
            evre(mc, GunesEvresi.GUNESSIZ);
            komut(mc, "dogalafet donma 30 donma");
            komut(mc, "weather rain 100000");
        }, yetisti(60), 3000);
        // Gerçek donma süreci hiz 20 altında (72 tick'te 1 puan); kıyıdan içeri halka 35 ve 45'te, 55'te gölün çoğu buz.
        ekle("hiz 20 ile 35'e", () -> komut(mc, "dogalafet hiz 20"), gEnAz(35f), 1200);
        ekle("vitrin %35", () -> kare(mc, "vitrin_35"), gEnAz(45f), 1200);
        ekle("vitrin %45", () -> {
            kare(mc, "vitrin_45");
            ustBak(mc);
        }, gEnAz(47f), 400);
        ekle("vitrin %47 üstten", () -> {
            kare(mc, "vitrin_47_ust");
            yanBak(mc);
        }, gEnAz(55f), 1200);
        ekle("vitrin %55 yetişme sırasında", () -> {
            kare(mc, "vitrin_55_yetisirken");
            komut(mc, "dogalafet stop");
            komut(mc, "dogalafet hiz 1");
        }, bekle(1), 2);
        ekle("vitrin %55", () -> { }, yetisti(20), 2400);
        ekle("vitrin kare", () -> {
            kare(mc, "vitrin_55");
            bilgi(mc);
        }, bekle(40), 60);
        ekle("üstten", () -> ustBak(mc), bekle(100), 120);
        ekle("üstten kare", () -> kare(mc, "vitrin_55_ust"), bekle(20), 30);
        ekle("G 100", () -> {
            yanBak(mc);
            komut(mc, "dogalafet donma 100");
        }, yetisti(100), 4800);
        ekle("%100 kare", () -> {
            kare(mc, "donmus_100");
            bilgi(mc);
        }, bekle(40), 60);
        ekle("manzara", () -> uzakBak(mc), bekle(120), 140);
        ekle("manzara kare", () -> kare(mc, "donmus_100_manzara"), bekle(20), 30);
        ekle("çöz 0", () -> {
            yanBak(mc);
            komut(mc, "weather clear 100000");
            komut(mc, "dogalafet donma 0 donma");
        }, yetisti(100), 4800);
        ekle("buharlaşma 6", () -> {
            evre(mc, GunesEvresi.BUYUYEN);
            komut(mc, "dogalafet buharlasma 6");
        }, yetisti(60), 3000);
        ekle("buharlaşma kare", () -> {
            kare(mc, "buharlasma_gol_yatagi");
            bilgi(mc);
        }, bekle(40), 60);
        ekle("süpernova: geri dol", () -> evre(mc, GunesEvresi.SUPERNOVA), yetisti(60), 3000);
        ekle("geri dolmuş kare", () -> kare(mc, "buharlasma_geri_dolmus"), bekle(20), 30);
        ekle("kış sınırı ara", () -> {
            MinecraftServer server = mc.getSingleplayerServer();
            BlockPos o = mc.player.blockPosition();
            sinir = server.submit(() -> DonmaApi.kisSiniriBul(server.overworld(), o.getX(), o.getZ(), 4000)).join();
            DogalAfetler.LOGGER.info(ON + "kış sınırı: {}", sinir);
            evre(mc, GunesEvresi.GUNESSIZ);
            komut(mc, "dogalafet donma 100 donma");
            if (sinir != null) {
                komut(mc, "tp @s " + sinir.getX() + " 200 " + sinir.getZ() + " 0 60");
            }
        }, bekle(200), 220);
        ekle("sınırda %100", () -> sinirBak(mc), yetisti(200), 7200);
        ekle("kızıl cüce: sınırda çözülme", () -> {
            evre(mc, GunesEvresi.KIZIL_CUCE);
            komut(mc, "dogalafet donma 40 cozulme");
        }, yetisti(100), 6000);
        ekle("sınır kare", () -> {
            sinirBak(mc);
            bilgi(mc);
        }, bekle(80), 100);
        ekle("sınır kare 2", () -> kare(mc, "cozulme_kis_siniri"), bekle(40), 60);
        ekle("kapanış", () -> {
            komut(mc, "dogalafet donma 0 donma");
            DogalAfetler.LOGGER.info(ON + "bitti; FPS {}", mc.getFps());
        }, bekle(100), 120);
        ekle("çıkış", () -> {
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            mc.stop();
        }, bekle(1), 2);
    }

    private static void bilgi(Minecraft mc) {
        komut(mc, "forge tps");
        komut(mc, "dogalafet donma bilgi");
    }

    /** Güneş evresini entegre sunucuda ayarlar (yalnız geliştirme otomasyonu; evre komutu gökyüzü dalında). */
    private static void evre(Minecraft mc, GunesEvresi evre) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.submit(() -> DunyaDonemi.al(server).gunesEvresiAyarla(evre)).join();
        DogalAfetler.LOGGER.info(ON + "güneş evresi {}", evre.kimlik());
    }

    private static void kare(Minecraft mc, String ad) {
        mc.options.hideGui = true;
        String dosya = "donma_" + ad + ".png";
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> { });
        DogalAfetler.LOGGER.info(ON + "kare {} (G {} istemci, FPS {}, oyuncu {})", dosya,
                String.format(Locale.ROOT, "%.1f", IstemciDonem.durum().donmaIlerlemesi()), mc.getFps(),
                mc.player.blockPosition().toShortString());
    }

    /** Gölü güneybatı kıyısından, Y+12, ~30° aşağı bakarak görür (tasarım notu §3). */
    private static void yanBak(Minecraft mc) {
        if (gol == null) {
            return;
        }
        double m = gol[4] + 10;
        double x = gol[0] - m * 0.7071;
        double z = gol[1] + m * 0.7071;
        tpBak(mc, x, gol[2] + 13, z, gol[0] + 0.5, gol[2], gol[1] + 0.5);
    }

    private static void ustBak(Minecraft mc) {
        if (gol != null) {
            komut(mc, String.format(Locale.ROOT, "tp @s %.1f %d %.1f 0 90", gol[0] + 0.5, gol[2] + Math.max(24, gol[4] * 2 + 8),
                    gol[1] + 0.5));
        }
    }

    private static void uzakBak(Minecraft mc) {
        if (gol != null) {
            double m = gol[4] + 40;
            tpBak(mc, gol[0] - m * 0.7071, gol[2] + 34, gol[1] + m * 0.7071, gol[0] + 0.5, gol[2], gol[1] + 0.5);
        }
    }

    private static void sinirBak(Minecraft mc) {
        if (sinir == null) {
            return;
        }
        int y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, sinir.getX(), sinir.getZ());
        tpBak(mc, sinir.getX() - 60, y + 50, sinir.getZ() - 60, sinir.getX(), y, sinir.getZ());
    }

    private static void tpBak(Minecraft mc, double x, double y, double z, double hx, double hy, double hz) {
        double dx = hx - x;
        double dz = hz - z;
        double yatay = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(y - hy, yatay));
        komut(mc, String.format(Locale.ROOT, "tp @s %.1f %.1f %.1f %.1f %.1f", x, y, z, yaw, pitch));
    }

    /**
     * Oyuncu çevresinde en yakın göl: üstü açık su kaynağı sütunlarının aynı Y'de 4 komşulu bağlı bileşenleri, alanı
     * 60–3000. Sonuç: merkez x, z, yüzey Y, alan, yarıçap; yoksa {@code null}.
     */
    static int[] golBul(ClientLevel level, BlockPos merkez, int r) {
        int n = 2 * r + 1;
        int[] yuzey = new int[n * n];
        java.util.Arrays.fill(yuzey, Integer.MIN_VALUE);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                int x = merkez.getX() + dx;
                int z = merkez.getZ() + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                BlockState s = level.getBlockState(p.set(x, y, z));
                if (s.is(Blocks.WATER) && s.getFluidState().isSource()) {
                    yuzey[(dz + r) * n + dx + r] = y;
                }
            }
        }
        boolean[] gorulen = new boolean[n * n];
        int[] kuyruk = new int[n * n];
        int[] en = null;
        long enUzaklik = Long.MAX_VALUE;
        int[] yedek = null;
        long yedekUzaklik = Long.MAX_VALUE;
        for (int i = 0; i < n * n; i++) {
            if (gorulen[i] || yuzey[i] == Integer.MIN_VALUE) {
                continue;
            }
            int bas = 0;
            int son = 0;
            kuyruk[son++] = i;
            gorulen[i] = true;
            long sx = 0;
            long sz = 0;
            int minX = n;
            int maxX = 0;
            int minZ = n;
            int maxZ = 0;
            int yakin = i;
            long yakinUzaklik = Long.MAX_VALUE;
            while (bas < son) {
                int j = kuyruk[bas++];
                int jx = j % n;
                int jz = j / n;
                sx += jx;
                sz += jz;
                long u = (long) (jx - r) * (jx - r) + (long) (jz - r) * (jz - r);
                if (u < yakinUzaklik) {
                    yakinUzaklik = u;
                    yakin = j;
                }
                minX = Math.min(minX, jx);
                maxX = Math.max(maxX, jx);
                minZ = Math.min(minZ, jz);
                maxZ = Math.max(maxZ, jz);
                int[] komsular = {jx > 0 ? j - 1 : -1, jx < n - 1 ? j + 1 : -1, jz > 0 ? j - n : -1, jz < n - 1 ? j + n : -1};
                for (int k : komsular) {
                    if (k >= 0 && !gorulen[k] && yuzey[k] == yuzey[i]) {
                        gorulen[k] = true;
                        kuyruk[son++] = k;
                    }
                }
            }
            int alan = son;
            if (alan > 3000 && yakinUzaklik < yedekUzaklik) {
                yedekUzaklik = yakinUzaklik;
                yedek = new int[] {merkez.getX() + yakin % n - r, merkez.getZ() + yakin / n - r, yuzey[i], alan, 24};
            }
            if (alan < 60 || alan > 3000) {
                continue;
            }
            int cx = (int) (sx / alan) - r;
            int cz = (int) (sz / alan) - r;
            long uzaklik = (long) cx * cx + (long) cz * cz;
            if (uzaklik < enUzaklik) {
                enUzaklik = uzaklik;
                en = new int[] {merkez.getX() + cx, merkez.getZ() + cz, yuzey[i], alan,
                        Math.max(maxX - minX, maxZ - minZ) / 2 + 1};
            }
        }
        return en != null ? en : yedek;
    }
}
