package io.github.kadirberkpolat1.dogalafetler.client.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Klavyesiz görsel kanıt: yalnız {@code -Dspike... dogalafetler.spike=su} (Gradle {@code -Pspike=su}) ile açılır.
 * Girişten sonra senaryoyu sırayla koşar, ekran görüntüsü alır, FPS'i log'a yazar ve oyunu kapatır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SpikeOtomasyon {
    private static final boolean ACIK = "su".equals(System.getProperty("dogalafetler.spike"));

    private record Adim(int bekle, Consumer<Minecraft> is) {
    }

    private static List<Adim> adimlar;
    private static int sira;
    private static int sayac;
    private static String fpsEtiket;
    private static final List<Integer> FPS = new ArrayList<>();
    private static int fpsSayac;

    private SpikeOtomasyon() {
    }

    @SubscribeEvent
    public static void giris(ClientPlayerNetworkEvent.LoggingIn e) {
        if (ACIK && adimlar == null) {
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
        if (fpsEtiket != null && ++fpsSayac % 20 == 0) {
            FPS.add(mc.getFps());
            if (FPS.size() == 4) {
                double ort = FPS.stream().mapToInt(Integer::intValue).average().orElse(0);
                DogalAfetler.LOGGER.info("SPIKE-SU FPS etiket={} ort={} min={} max={} ornek={}", fpsEtiket,
                        String.format("%.1f", ort), FPS.stream().mapToInt(Integer::intValue).min().orElse(0),
                        FPS.stream().mapToInt(Integer::intValue).max().orElse(0), FPS);
                fpsEtiket = null;
            }
        }
        if (sira < adimlar.size() && ++sayac >= adimlar.get(sira).bekle()) {
            sayac = 0;
            adimlar.get(sira++).is().accept(mc);
        }
    }

    private static Adim komut(int bekle, String k) {
        return new Adim(bekle, mc -> {
            DogalAfetler.LOGGER.info("SPIKE-SU otomasyon komut: /{}", k);
            mc.player.connection.sendCommand(k);
        });
    }

    private static Adim foto(int bekle, String ad) {
        return new Adim(bekle, mc -> Screenshot.grab(mc.gameDirectory, "spike_su_" + ad + ".png", mc.getMainRenderTarget(),
                m -> DogalAfetler.LOGGER.info("SPIKE-SU foto {} fps={} -> {}", ad, mc.getFps(), m.getString())));
    }

    private static Adim fps(int bekle, String etiket) {
        return new Adim(bekle, mc -> {
            fpsEtiket = etiket;
            FPS.clear();
            fpsSayac = 0;
            SuRenderer.pencereBasla(etiket, 4000);
        });
    }

    private static List<Adim> senaryo() {
        String su = "dogalafet spike su ";
        List<Adim> s = new ArrayList<>();
        s.add(komut(100, "gamerule doDaylightCycle false"));
        s.add(komut(2, "gamerule doWeatherCycle false"));
        s.add(komut(2, "gamerule doMobSpawning false"));
        s.add(komut(2, "time set 6000"));
        s.add(komut(2, "weather clear"));
        s.add(komut(2, "gamemode creative"));
        s.add(komut(2, su + "izle ac"));
        s.add(komut(2, su + "konum kiyi -40"));
        s.add(fps(160, "afetsiz"));
        s.add(foto(90, "01_afetsiz"));
        s.add(komut(5, su + "hash kiyi_once"));
        s.add(komut(5, su + "deniz 4 100"));
        s.add(foto(200, "02_deniz4"));
        s.add(fps(5, "deniz4"));
        s.add(komut(90, "gamemode spectator"));
        s.add(komut(2, "tp @s ~ ~40 ~ ~ 35"));
        s.add(fps(80, "deniz4_yuksekten_lod"));
        s.add(foto(85, "05_lod_uzak"));
        s.add(komut(10, "gamemode survival"));
        s.add(komut(2, su + "konum kiyi -40"));
        s.add(komut(2, "tp @s ~ ~ ~ ~ -20"));
        s.add(komut(2, "summon pig ~1.5 ~ ~1.5 {NoAI:1b,Tags:[\"spike\"]}"));
        s.add(foto(60, "03_sualti"));
        s.add(fps(5, "sualti"));
        s.add(komut(380, "effect give @s minecraft:instant_health 1 3"));
        s.add(komut(2, su + "yol olay"));
        s.add(komut(110, su + "yol mixin"));
        s.add(komut(2, "effect give @s minecraft:instant_health 1 3"));
        s.add(komut(2, "kill @e[tag=spike]"));
        s.add(komut(2, "gamemode creative"));
        s.add(komut(2, su + "kapat"));
        s.add(komut(2, su + "konum kiyi 0"));
        s.add(komut(20, "summon zombie ^2 ^ ^10 {Tags:[\"spike\"],PersistenceRequired:1b}"));
        s.add(komut(2, "summon pig ^-2 ^ ^14 {Tags:[\"spike\"]}"));
        s.add(komut(2, su + "dalga 8"));
        s.add(fps(60, "dalga"));
        s.add(foto(60, "04_dalga_yaklasiyor"));
        s.add(foto(25, "04b_dalga_yakin"));
        s.add(komut(180, su + "kapat"));
        s.add(komut(2, "kill @e[tag=spike]"));
        s.add(komut(2, su + "konum kiyi -40"));
        s.add(komut(10, su + "hash kiyi_sonra"));
        s.add(komut(5, "gamemode spectator"));
        s.add(komut(2, su + "konum kiyi 0"));
        s.add(komut(2, "tp @s ~ ~6 ~ ~ 30"));
        s.add(komut(10, su + "cekilme 30 3"));
        s.add(fps(130, "cekilme"));
        s.add(foto(90, "06_cekilme"));
        s.add(komut(5, su + "kapat"));
        s.add(komut(40, "gamemode creative"));
        s.add(komut(2, "effect give @s minecraft:night_vision 600 0 true"));
        s.add(komut(2, su + "konum kiyi -40"));
        s.add(komut(10, su + "magara 100000 0.05"));
        s.add(foto(140, "07_magara"));
        s.add(fps(5, "magara"));
        s.add(komut(90, su + "magara kurut 0.05"));
        s.add(foto(50, "08_magara_buhar"));
        s.add(foto(640, "09_magara_kurudu"));
        s.add(komut(10, su + "kapat"));
        s.add(new Adim(60, mc -> {
            DogalAfetler.LOGGER.info("SPIKE-SU otomasyon bitti, oyun kapatiliyor");
            mc.stop();
        }));
        return s;
    }
}
