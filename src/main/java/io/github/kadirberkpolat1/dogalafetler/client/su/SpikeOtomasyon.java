package io.github.kadirberkpolat1.dogalafetler.client.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * Klavyesiz görsel kanıt: yalnız {@code -Ddogalafetler.spike=su} (Gradle {@code -Pspike=su}) ile açılır. Girişten sonra
 * senaryoyu sırayla koşar, ekran görüntüsü alır, FPS'i ve su render maliyetini log'a yazar ve oyunu kapatır. Bekleme süreleri
 * seviye oyun zamanıyla (sunucu tick'i) ölçülür; pencere gizli workspace'te yavaş çizse de olaylarla eş kalır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SpikeOtomasyon {
    private static final boolean ACIK = "su".equals(System.getProperty("dogalafetler.spike"));
    /**
     * Koşulacak bölümler: hepsi ya da virgüllü liste (deniz, sualti, dalga, cekilme, adil, hash, magara); fpsdongu yalnız
     * açıkça istenince koşar.
     */
    private static final String BOLUM = System.getProperty("dogalafetler.spike.bolum", "hepsi");

    /** {@code bekle}: önceki adımdan sonra oyun tick'i; {@code kosul}: ayrıca doğru olana kadar bekle (null: yok). */
    private record Adim(int bekle, Predicate<Minecraft> kosul, int kosulSiniri, Consumer<Minecraft> is) {
    }

    private static List<Adim> adimlar;
    private static int sira;
    private static long oncekiZaman = Long.MIN_VALUE;
    private static long kosulBaslangic = Long.MIN_VALUE;
    private static String fpsEtiket;
    private static final List<Integer> FPS = new ArrayList<>();
    private static long fpsSonNs;
    private static int yuzmeKalan;
    private static int ekranBos;

    private SpikeOtomasyon() {
    }

    @SubscribeEvent
    public static void giris(ClientPlayerNetworkEvent.LoggingIn e) {
        if (ACIK && adimlar == null) {
            ayarlariSabitle(Minecraft.getInstance());
            adimlar = senaryo();
            DogalAfetler.LOGGER.info("SPIKE-SU otomasyon basladi, adim={}", adimlar.size());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (adimlar == null || e.phase != TickEvent.Phase.END || mc.player == null || mc.level == null) {
            return;
        }
        mc.options.hideGui = true;
        if (mc.screen instanceof PauseScreen) {
            DogalAfetler.LOGGER.info("SPIKE-SU otomasyon: disaridan acilan duraklatma ekrani kapatildi");
            mc.setScreen(null);
        }
        ekranBos = mc.screen == null ? ekranBos + 1 : 0;
        fpsOrnekle(mc);
        yuzmeTick(mc);
        long t = mc.level.getGameTime();
        if (oncekiZaman == Long.MIN_VALUE) {
            oncekiZaman = t;
        }
        if (sira >= adimlar.size()) {
            return;
        }
        Adim a = adimlar.get(sira);
        if (t - oncekiZaman < a.bekle()) {
            return;
        }
        if (a.kosul() != null && !a.kosul().test(mc)) {
            if (kosulBaslangic == Long.MIN_VALUE) {
                kosulBaslangic = t;
            }
            if (t - kosulBaslangic < a.kosulSiniri()) {
                return;
            }
            DogalAfetler.LOGGER.info("SPIKE-SU otomasyon: kosul {} tick icinde saglanmadi, devam", a.kosulSiniri());
        }
        kosulBaslangic = Long.MIN_VALUE;
        oncekiZaman = t;
        sira++;
        a.is().accept(mc);
    }

    /**
     * FPS ölçümü ve LOD için sabit görüntü ayarları: options.txt dışarıdan değişse de (bir koşuda görüş mesafesi 3 ve
     * dikey eşitleme açık bulundu) ölçümler aynı koşulda alınır.
     */
    private static void ayarlariSabitle(Minecraft mc) {
        mc.options.renderDistance().set(12);
        mc.options.simulationDistance().set(12);
        mc.options.enableVsync().set(false);
        mc.options.framerateLimit().set(260);
        mc.options.graphicsMode().set(GraphicsStatus.FANCY);
        mc.options.save();
        DogalAfetler.LOGGER.info("SPIKE-SU ayarlar: gorus={} simulasyon={} vsync={} fpsSiniri={} grafik={}",
                mc.options.renderDistance().get(), mc.options.simulationDistance().get(), mc.options.enableVsync().get(),
                mc.options.framerateLimit().get(), mc.options.graphicsMode().get());
    }

    private static void fpsOrnekle(Minecraft mc) {
        if (fpsEtiket == null) {
            return;
        }
        long simdi = System.nanoTime();
        if (simdi - fpsSonNs < 1_000_000_000L) {
            return;
        }
        fpsSonNs = simdi;
        FPS.add(mc.getFps());
        if (FPS.size() == 5) {
            List<Integer> ornek = FPS.subList(1, 5);
            double ort = ornek.stream().mapToInt(Integer::intValue).average().orElse(0);
            DogalAfetler.LOGGER.info("SPIKE-SU FPS etiket={} ort={} min={} max={} ornek={} yuk={} | {}", fpsEtiket,
                    String.format(Locale.ROOT, "%.1f", ort), ornek.stream().mapToInt(Integer::intValue).min().orElse(0),
                    ornek.stream().mapToInt(Integer::intValue).max().orElse(0), ornek, sistemYuku(), SuRenderer.olcumOzeti());
            fpsEtiket = null;
        }
    }

    /** Makinenin 1 dakikalık yük ortalaması (FPS ölçümünü başka işlerden ayırmak için); okunamazsa "?". */
    private static String sistemYuku() {
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of("/proc/loadavg")).split(" ")[0];
        } catch (java.io.IOException | RuntimeException e) {
            return "?";
        }
    }

    private static void yuzmeTick(Minecraft mc) {
        if (yuzmeKalan <= 0) {
            return;
        }
        yuzmeKalan--;
        boolean bas = yuzmeKalan > 0;
        mc.options.keyUp.setDown(bas);
        mc.options.keySprint.setDown(bas);
        LocalPlayer p = mc.player;
        if (yuzmeKalan % 10 == 0) {
            boolean gercekSu = !mc.level.getFluidState(p.blockPosition()).isEmpty()
                    || !mc.level.getFluidState(net.minecraft.core.BlockPos.containing(p.getEyePosition())).isEmpty();
            DogalAfetler.LOGGER.info(
                    "SPIKE-SU yuzme(istemci) y={} isSprinting={} isSwimming={} poz={} isInWater={} isUnderWater={} batiklikOrani={} gercekSuTemasi={}",
                    String.format(Locale.ROOT, "%.2f", p.getY()), p.isSprinting(), p.isSwimming(), p.getPose(),
                    p.isInWater(), p.isUnderWater(), String.format(Locale.ROOT, "%.2f", SanalSu.batiklikOrani(p)),
                    gercekSu);
        }
    }

    private static Adim is(int bekle, Consumer<Minecraft> is) {
        return new Adim(bekle, null, 0, is);
    }

    private static Adim komut(int bekle, String k) {
        return is(bekle, mc -> {
            DogalAfetler.LOGGER.info("SPIKE-SU otomasyon komut: /{}", k);
            mc.player.connection.sendCommand(k);
        });
    }

    /** Ekran (yükleme, menü) kapanana ve en az 40 tick boş kalana kadar bekler. */
    private static Adim ekranBekle() {
        return new Adim(0, mc -> ekranBos >= 40, 2400, mc -> DogalAfetler.LOGGER.info("SPIKE-SU otomasyon: ekran bos"));
    }

    /** Görünen chunk'lar derlenene kadar (en çok {@code sinir} tick) bekler. */
    private static Adim chunkBekle(int bekle, int sinir) {
        return new Adim(bekle, mc -> mc.levelRenderer.hasRenderedAllChunks(), sinir,
                mc -> DogalAfetler.LOGGER.info("SPIKE-SU otomasyon: chunk derleme bitti, cizilen={}",
                        mc.levelRenderer.countRenderedChunks()));
    }

    private static Adim foto(int bekle, String ad) {
        return is(bekle, mc -> {
            DogalAfetler.LOGGER.info("SPIKE-SU nisan {} {}", ad, nisan(mc));
            Screenshot.grab(mc.gameDirectory, "spike_su_" + ad + ".png", mc.getMainRenderTarget(),
                    m -> DogalAfetler.LOGGER.info("SPIKE-SU foto {} fps={} poz=({} {} {}) -> {}", ad, mc.getFps(),
                            String.format(Locale.ROOT, "%.1f", mc.player.getX()),
                            String.format(Locale.ROOT, "%.1f", mc.player.getEyeY()),
                            String.format(Locale.ROOT, "%.1f", mc.player.getZ()), m.getString()));
        });
    }

    /**
     * Ekran merkezinden giden ışında ilk sanal su ve ilk hava olmayan blok (24 bloğa kadar): görüntünün ortasında neyin
     * olduğunun kanıtı. Sanal su için render'ın kullandığı renk (deniz seviyesindeki biyom) ve ışık da yazılır.
     */
    private static String nisan(Minecraft mc) {
        Camera kamera = mc.gameRenderer.getMainCamera();
        Vec3 p = kamera.getPosition();
        Vector3f bak = kamera.getLookVector();
        ClientLevel l = mc.level;
        SanalSuDunyasi d = SanalSu.dunya(l);
        long t = l.getGameTime();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        String su = "yok";
        String blok = "yok";
        for (double s = 0; s <= 24 && ("yok".equals(su) || "yok".equals(blok)); s += 0.05) {
            double x = p.x + bak.x() * s;
            double y = p.y + bak.y() * s;
            double z = p.z + bak.z() * s;
            if ("yok".equals(su) && d != null) {
                double ust = d.ustu(x, y, z, t);
                if (!Double.isNaN(ust)) {
                    int renk = BiomeColors.getAverageWaterColor(l, m.set(Mth.floor(x), l.getSeaLevel(), Mth.floor(z)));
                    int isik = LevelRenderer.getLightColor(l, m.set(Mth.floor(x), Mth.floor(ust), Mth.floor(z)));
                    su = String.format(Locale.ROOT, "%.2f (%.1f %.2f %.1f) yuzey=%.2f renk=%06x isik=%x", s, x, y, z, ust,
                            renk & 0xFFFFFF, isik);
                }
            }
            if ("yok".equals(blok)) {
                BlockState b = l.getBlockState(m.set(Mth.floor(x), Mth.floor(y), Mth.floor(z)));
                if (!b.isAir()) {
                    blok = String.format(Locale.ROOT, "%.2f (%d %d %d) %s", s, m.getX(), m.getY(), m.getZ(), b);
                }
            }
        }
        return String.format(Locale.ROOT, "kamera=(%.2f %.2f %.2f) yaw=%.0f pitch=%.0f ilk_sanal_su=%s ilk_blok=%s", p.x, p.y,
                p.z, kamera.getYRot(), kamera.getXRot(), su, blok);
    }

    /** 5 sn boyunca FPS örnekler (ilk saniye atılır) ve su render maliyetini yazar; sonraki adımlar eşzamanlı koşar. */
    private static Adim fps(int bekle, String etiket) {
        return is(bekle, mc -> {
            fpsEtiket = etiket;
            FPS.clear();
            fpsSonNs = System.nanoTime();
            SuRenderer.olcumSifirla();
            SuRenderer.pencereBasla(etiket, 5000);
        });
    }

    private static Adim yuz(int bekle, int tick) {
        return is(bekle, mc -> {
            DogalAfetler.LOGGER.info("SPIKE-SU otomasyon: ileri+kos tuslari {} tick basili", tick);
            yuzmeKalan = tick;
        });
    }

    private static boolean bolum(String ad) {
        return "hepsi".equals(BOLUM) || ("," + BOLUM + ",").contains("," + ad + ",");
    }

    private static List<Adim> senaryo() {
        String su = "dogalafet spike su ";
        List<Adim> s = new ArrayList<>();
        s.add(ekranBekle());
        s.add(komut(0, "gamerule doDaylightCycle false"));
        s.add(komut(2, "gamerule doWeatherCycle false"));
        s.add(komut(2, "gamerule doMobSpawning false"));
        s.add(komut(2, "gamerule randomTickSpeed 0"));
        s.add(komut(2, "gamerule doFireTick false"));
        // Hash kanıtı deterministik olsun: koyunların ot yemesi (çimen -> toprak) 2. koşuda kutuda 3 blok değiştirdi.
        s.add(komut(2, "gamerule mobGriefing false"));
        s.add(komut(2, "gamerule doImmediateRespawn true"));
        s.add(komut(2, "time set 6000"));
        s.add(komut(2, "weather clear"));
        s.add(komut(2, "gamemode spectator"));
        s.add(komut(2, su + "izle ac"));
        s.add(komut(2, su + "konum kiyi -40"));
        s.add(komut(5, su + "hash kiyi_once"));
        if (bolum("deniz")) {
            // A: açık hava, deniz +4 (aynı kamera önce/sonra).
            s.add(komut(5, "tp @s ~ ~5 ~ ~ 22"));
            s.add(chunkBekle(20, 600));
            s.add(fps(40, "afetsiz_kiyi"));
            s.add(foto(60, "01_kiyi_afetsiz"));
            s.add(komut(50, "tp @s ~ ~35 ~ ~ 38"));
            s.add(chunkBekle(20, 600));
            s.add(fps(40, "afetsiz_yuksek"));
            s.add(foto(60, "05a_lod_afetsiz"));
            s.add(komut(50, su + "deniz 4 100"));
            s.add(foto(200, "05_lod_deniz4"));
            s.add(fps(5, "deniz4_yuksek_lod"));
            s.add(komut(110, "tp @s ~ ~-35 ~ ~ 22"));
            s.add(chunkBekle(20, 600));
            s.add(fps(40, "deniz4_kiyi"));
            s.add(foto(60, "02_kiyi_deniz4"));
            s.add(komut(60, su + "kapat"));
        }
        if (bolum("sualti")) {
            // B: sualtı, boğulma, mob, yüzme pozu, olay yolu karşılaştırması.
            s.add(komut(10, su + "deniz 4 0"));
            s.add(komut(2, "gamemode survival"));
            s.add(komut(2, su + "konum kiyi -40"));
            s.add(komut(2, "tp @s ~ ~ ~ ~ -15"));
            s.add(komut(2, "summon pig ~1.5 ~ ~1.5 {NoAI:1b,Tags:[\"spike\"]}"));
            s.add(foto(60, "03_sualti"));
            s.add(fps(5, "sualti"));
            s.add(komut(110, su + "konum kiyi 180"));
            s.add(yuz(5, 60));
            s.add(foto(30, "03b_sualti_yuzme"));
            s.add(komut(40, su + "konum kiyi -40"));
            s.add(komut(200, "effect give @s minecraft:instant_health 1 3"));
            s.add(komut(2, su + "yol olay"));
            s.add(yuz(20, 40));
            s.add(komut(100, su + "yol mixin"));
            s.add(komut(2, "effect give @s minecraft:instant_health 1 3"));
            s.add(komut(2, "kill @e[tag=spike]"));
            s.add(komut(2, su + "kapat"));
            s.add(komut(10, "gamemode spectator"));
        }
        if (bolum("dalga")) {
            // C: dalga, mob sürüklenmesi (kamera yanda ve yüksekte).
            s.add(komut(10, su + "konum kiyi 0"));
            s.add(komut(2, "execute positioned ~ ~ ~ run summon zombie ^2 ^ ^8 {Tags:[\"spike\"],PersistenceRequired:1b}"));
            s.add(komut(2, "execute positioned ~ ~ ~ run summon pig ^-2 ^ ^6 {Tags:[\"spike\"]}"));
            s.add(komut(2, "tp @s ~ ~2 ~ ~ -4"));
            s.add(chunkBekle(10, 400));
            s.add(fps(20, "dalga_once"));
            s.add(foto(60, "04a_dalga_once"));
            s.add(komut(40, su + "dalga 10"));
            s.add(fps(60, "dalga"));
            s.add(foto(40, "04_dalga_yaklasiyor"));
            s.add(foto(40, "04b_dalga_yakin"));
            s.add(komut(160, su + "kapat"));
            s.add(komut(2, "kill @e[tag=spike]"));
        }
        if (bolum("cekilme")) {
            // D: çekilme.
            s.add(komut(10, su + "konum kiyi 0"));
            s.add(komut(2, "tp @s ~ ~6 ~ ~ 30"));
            s.add(chunkBekle(10, 400));
            s.add(fps(20, "cekilme_once"));
            s.add(foto(60, "06a_cekilme_once"));
            s.add(komut(40, su + "cekilme 30 3"));
            s.add(fps(120, "cekilme"));
            s.add(foto(60, "06_cekilme"));
            s.add(komut(40, su + "kapat"));
        }
        if (bolum("adil")) {
            // F: aynı kamera, oturmuş dünya: afetsiz / deniz +4 / dalga FPS'i.
            s.add(komut(10, su + "konum kiyi 0"));
            s.add(komut(2, "tp @s ~ ~6 ~ ~ 22"));
            s.add(chunkBekle(200, 600));
            s.add(fps(0, "adil_afetsiz"));
            s.add(komut(110, su + "deniz 4 0"));
            s.add(fps(60, "adil_deniz4"));
            s.add(foto(60, "10_adil_deniz4"));
            s.add(komut(60, su + "kapat"));
            s.add(fps(60, "adil_afetsiz_2"));
            s.add(komut(110, su + "dalga 10"));
            s.add(fps(80, "adil_dalga"));
            s.add(komut(110, su + "kapat"));
        }
        if (("," + BOLUM + ",").contains(",fpsdongu,")) {
            // G: aynı kamerada dönüşümlü ölçüm (afetsiz / deniz +4 / afetsiz / dalga) x3; makine yükü koşudan koşuya oynadığı için
            // oranlar yan yana ölçülen çiftlerden hesaplanır.
            s.add(komut(10, su + "kapat"));
            s.add(komut(2, "gamemode spectator"));
            s.add(komut(2, su + "konum kiyi 0"));
            s.add(komut(2, "tp @s ~ ~6 ~ ~ 22"));
            s.add(chunkBekle(200, 600));
            for (int i = 1; i <= 3; i++) {
                s.add(fps(20, "dongu" + i + "_afetsiz"));
                s.add(komut(110, su + "deniz 4 0"));
                s.add(fps(60, "dongu" + i + "_deniz4"));
                s.add(komut(110, su + "kapat"));
                s.add(fps(60, "dongu" + i + "_afetsiz_b"));
                s.add(komut(110, su + "dalga 10"));
                s.add(fps(60, "dongu" + i + "_dalga"));
                s.add(komut(110, su + "kapat"));
            }
        }
        if (("," + BOLUM + ",").contains(",fpsdongu2,")) {
            // H: aynı kamerada dönüşümlü ölçüm: afetsiz / çekilme (chunk yeniden derleme bitince) x3, afetsiz / mağara x3.
            s.add(komut(10, su + "kapat"));
            s.add(komut(2, "gamemode spectator"));
            s.add(komut(2, su + "konum kiyi 0"));
            s.add(komut(2, "tp @s ~ ~6 ~ ~ 30"));
            s.add(chunkBekle(200, 600));
            for (int i = 1; i <= 3; i++) {
                s.add(fps(60, "cdongu" + i + "_afetsiz"));
                s.add(komut(110, su + "cekilme 30 3"));
                s.add(fps(160, "cdongu" + i + "_cekilme"));
                s.add(komut(110, su + "kapat"));
            }
            s.add(komut(100, "effect give @s minecraft:night_vision 900 0 true"));
            s.add(komut(2, su + "magara hazirla 14 17 -95"));
            s.add(chunkBekle(200, 600));
            for (int i = 1; i <= 3; i++) {
                s.add(fps(20, "mdongu" + i + "_afetsiz"));
                s.add(komut(110, su + "magara baslat 100000 0.05"));
                s.add(fps(110, "mdongu" + i + "_magara"));
                s.add(komut(110, su + "kapat"));
            }
        }
        if (bolum("hash")) {
            s.add(komut(40, su + "konum kiyi -40"));
            s.add(komut(10, su + "hash kiyi_sonra kiyi_once"));
        }
        if (bolum("magara")) {
            // E: mağara (çevrimdışı analizle bulunan kapalı cepler; tohum en alçak hücre; son başarılı olan kalır).
            s.add(komut(10, "effect give @s minecraft:night_vision 900 0 true"));
            s.add(komut(2, su + "magara hazirla 60 22 33"));
            s.add(komut(10, su + "magara hazirla 14 17 -95"));
            s.add(chunkBekle(20, 400));
            s.add(fps(100, "magara_once"));
            s.add(foto(100, "07a_magara_once"));
            s.add(komut(40, su + "magara baslat 100000 0.05"));
            s.add(foto(140, "07_magara"));
            s.add(fps(5, "magara"));
            s.add(komut(110, su + "magara kurut 0.04"));
            s.add(foto(60, "08_magara_buhar"));
            s.add(foto(300, "09_magara_kurudu"));
            s.add(komut(20, su + "kapat"));
        }
        s.add(is(40, mc -> {
            DogalAfetler.LOGGER.info("SPIKE-SU otomasyon bitti, oyun kapatiliyor");
            mc.stop();
        }));
        return s;
    }
}
