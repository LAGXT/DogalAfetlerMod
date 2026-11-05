package io.github.kadirberkpolat1.dogalafetler.client.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Klavyesiz görsel kanıt: yalnız {@code -Ddogalafetler.spike=su} (Gradle {@code -Pspike=su}) ile açılır. Girişten sonra
 * senaryoyu sırayla koşar, ekran görüntüsü alır, FPS'i ve su render maliyetini log'a yazar ve oyunu kapatır. Bekleme süreleri
 * seviye oyun zamanıyla (sunucu tick'i) ölçülür; pencere gizli workspace'te yavaş çizse de olaylarla eş kalır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SpikeOtomasyon {
    private static final boolean ACIK = "su".equals(System.getProperty("dogalafetler.spike"));

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
            DogalAfetler.LOGGER.info("SPIKE-SU FPS etiket={} ort={} min={} max={} ornek={} | {}", fpsEtiket,
                    String.format(Locale.ROOT, "%.1f", ort), ornek.stream().mapToInt(Integer::intValue).min().orElse(0),
                    ornek.stream().mapToInt(Integer::intValue).max().orElse(0), ornek, SuRenderer.olcumOzeti());
            fpsEtiket = null;
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
        return is(bekle, mc -> Screenshot.grab(mc.gameDirectory, "spike_su_" + ad + ".png", mc.getMainRenderTarget(),
                m -> DogalAfetler.LOGGER.info("SPIKE-SU foto {} fps={} poz=({} {} {}) -> {}", ad, mc.getFps(),
                        String.format(Locale.ROOT, "%.1f", mc.player.getX()),
                        String.format(Locale.ROOT, "%.1f", mc.player.getEyeY()),
                        String.format(Locale.ROOT, "%.1f", mc.player.getZ()), m.getString())));
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

    private static List<Adim> senaryo() {
        String su = "dogalafet spike su ";
        List<Adim> s = new ArrayList<>();
        s.add(ekranBekle());
        s.add(komut(0, "gamerule doDaylightCycle false"));
        s.add(komut(2, "gamerule doWeatherCycle false"));
        s.add(komut(2, "gamerule doMobSpawning false"));
        s.add(komut(2, "gamerule randomTickSpeed 0"));
        s.add(komut(2, "gamerule doFireTick false"));
        s.add(komut(2, "time set 6000"));
        s.add(komut(2, "weather clear"));
        s.add(komut(2, "gamemode spectator"));
        s.add(komut(2, su + "izle ac"));
        s.add(komut(2, su + "konum kiyi -40"));
        s.add(komut(5, su + "hash kiyi_once"));
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
        // B: sualtı, boğulma, mob, yüzme pozu, olay yolu karşılaştırması.
        s.add(komut(60, "gamemode survival"));
        s.add(komut(2, su + "konum kiyi -40"));
        s.add(komut(2, "tp @s ~ ~ ~ ~ -15"));
        s.add(komut(2, "summon pig ~1.5 ~ ~1.5 {NoAI:1b,Tags:[\"spike\"]}"));
        s.add(foto(60, "03_sualti"));
        s.add(fps(5, "sualti"));
        s.add(komut(110, su + "konum kiyi 180"));
        s.add(yuz(5, 60));
        s.add(foto(30, "03b_sualti_yuzme"));
        s.add(komut(200, "effect give @s minecraft:instant_health 1 3"));
        s.add(komut(2, su + "konum kiyi -40"));
        s.add(komut(2, su + "yol olay"));
        s.add(yuz(20, 40));
        s.add(komut(100, su + "yol mixin"));
        s.add(komut(2, "effect give @s minecraft:instant_health 1 3"));
        s.add(komut(2, "kill @e[tag=spike]"));
        s.add(komut(2, su + "kapat"));
        // C: dalga, mob sürüklenmesi (kamera yanda ve yüksekte).
        s.add(komut(10, "gamemode spectator"));
        s.add(komut(2, su + "konum kiyi 0"));
        s.add(komut(2, "execute positioned ~ ~ ~ run summon zombie ^2 ^ ^8 {Tags:[\"spike\"],PersistenceRequired:1b}"));
        s.add(komut(2, "execute positioned ~ ~ ~ run summon pig ^-2 ^ ^6 {Tags:[\"spike\"]}"));
        s.add(komut(2, "tp @s ~ ~9 ~ ~ 18"));
        s.add(chunkBekle(10, 400));
        s.add(fps(20, "dalga_once"));
        s.add(foto(60, "04a_dalga_once"));
        s.add(komut(40, su + "dalga 10"));
        s.add(fps(60, "dalga"));
        s.add(foto(40, "04_dalga_yaklasiyor"));
        s.add(foto(40, "04b_dalga_yakin"));
        s.add(komut(160, su + "kapat"));
        s.add(komut(2, "kill @e[tag=spike]"));
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
        s.add(komut(40, su + "konum kiyi -40"));
        s.add(komut(10, su + "hash kiyi_sonra kiyi_once"));
        // E: mağara (çevrimdışı analizle bulunan kapalı cep; tohum en alçak hücre).
        s.add(komut(10, "effect give @s minecraft:night_vision 600 0 true"));
        s.add(komut(2, su + "magara hazirla 30 32 58"));
        s.add(chunkBekle(20, 400));
        s.add(fps(20, "magara_once"));
        s.add(foto(60, "07a_magara_once"));
        s.add(komut(40, su + "magara baslat 100000 0.05"));
        s.add(foto(140, "07_magara"));
        s.add(fps(5, "magara"));
        s.add(komut(110, su + "magara kurut 0.04"));
        s.add(foto(60, "08_magara_buhar"));
        s.add(foto(300, "09_magara_kurudu"));
        s.add(komut(20, su + "kapat"));
        s.add(is(40, mc -> {
            DogalAfetler.LOGGER.info("SPIKE-SU otomasyon bitti, oyun kapatiliyor");
            mc.stop();
        }));
        return s;
    }
}
