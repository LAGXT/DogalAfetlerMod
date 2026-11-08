package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarSesleri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KaraDelikYutmaEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.SesSonumu;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenNobeti;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Pulsar ve kara delik seslerinin istemcisi (pulsar-2 tur 4 D). Evre PULSAR'da (ve KARADELIK_YUTMA'da pulsar yutulana dek,
 * {@link KaraDelikYutmaEgrileri#pulsarAlfa}) Overworld'deki oyuncuya {@code pulsar_vurus} döngüsü kuru ve boğuk-yankılı iki
 * örnekle aynı tick'te başlar; ikisi de konumsuzdur (dinleyicide, sönümsüz) ve aynı uzunlukta döndüğü için fazı hep
 * aynıdır. Her tick kazançlar {@link SesSonumu}'ndan: gök oranı = başın bir üstündeki seviyede 5×5 sütunda gök ışığı 15
 * olan hücrelerin payı, derinlik ayak y'sinden; k birkaç tick'te yumuşak izlenir (çatı altına girince sesin cızırtısız
 * boğulması için). Döngünün kazancı ayrıca pulsar gücüyle ({@link PulsarEgrileri#yogunluk}, yutmada pulsarAlfa) ölçeklenir
 * ve ilk saniyede sıfırdan açılır. Kategori WEATHER; dosya −26 LUFS ve kazanç ≤ 1 olduğundan MASTER'da −14 LUFS tavanı
 * aşılmaz. Ses motorunun kestiği döngü ({@code /stopsound}, kaynak yenileme, aygıt değişimi) iki örnekle birlikte yeniden
 * başlar ({@link SirenNobeti}, K0-27). Ekrana yazı yok (N6).
 *
 * <p>OpenAL EFX (alçak geçiren/yankı filtresi) kullanılmaz: kaynağın OpenAL kimliği {@code Channel}'da özel alan, Forge'un
 * {@code PlaySoundSourceEvent}'i yalnız {@code Channel}'ı verir; kimliğe mixin, erişim dönüştürücü ya da yansıma olmadan
 * ulaşılamıyor. Yerine hazır boğuk-yankılı dosyayla çapraz geçiş.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class PulsarSesi {
    /**
     * Sönüm durumu: oyuncunun ayak y'si, gök oranı, k ve iki varyantın sönüm kazancı (pulsar gücü hariç). Oyuncu yoksa
     * y NaN, gerisi 0.
     */
    public record Sonum(double y, float gokOrani, float k, float kuru, float yanki) {
        @Override
        public String toString() {
            return String.format(Locale.ROOT, "y=%.1f gök=%.2f k=%.3f kuru=%.3f yankı=%.3f", y, gokOrani, k, kuru, yanki);
        }
    }

    /** {@code pulsar_vurus} döngüsünün boyu (sn; {@code ses_uret.py} {@code PULSAR_DONGU_SN}, 87 vuruş). */
    static final double DONGU_SN = 8.0;
    /** Gök oranı sütununun yarı genişliği (5×5). */
    static final int SUTUN_YARICAP = 2;
    /** k'nin hedefe tick başına yaklaşma payı (~1 sn'de %99). */
    static final float YUMUSAMA = 0.2f;
    /** Döngünün açılışı (tick). */
    static final int GIRIS_TICK = 20;
    private static final Sonum BOS = new Sonum(Double.NaN, 0f, 0f, 0f, 0f);

    private static final SirenNobeti NOBET = new SirenNobeti();
    private static Ornek kuruDongu;
    private static Ornek yankiDongu;
    private static Ornek kuruYutma;
    private static Ornek yankiYutma;
    /** Yumuşatılmış k; NaN = henüz ölçülmedi (ilk ölçüm doğrudan alınır). */
    private static float k = Float.NaN;
    private static float guc;
    private static int yas;
    /** Döngü başladığından beri duraklamasız geçen süre: ses motoru duraklamada örnekleri de durdurur. */
    private static long calmaNanos;
    private static long sonNanos;

    private PulsarSesi() {
    }

    /**
     * Kuru döngünün başlangıcından beri geçen gerçek saniye mod {@value #DONGU_SN} (duraklama sayılmaz); i. vuruş
     * i · 8/87 sn'de başlar, zarfı ~6 ms'de tepededir ({@code docs/kanit/ses/pulsar_vurus_ebur128.txt}). Döngü çalmıyorsa
     * NaN. İstemci ana thread'inde (çizim).
     */
    public static float vurusSaniyesi() {
        saatIlerle();
        if (kuruDongu == null || !Minecraft.getInstance().getSoundManager().isActive(kuruDongu)) {
            return Float.NaN;
        }
        return (float) ((calmaNanos / 1e9) % DONGU_SN);
    }

    /**
     * Kara delik yutmasının kütleçekim dalgası sesi: {@code karadelik_yutma} kuru + yankılı, aynı tick'te, döngüyle aynı k
     * ile (pulsar gücüyle ölçeklenmez). Çalan eski yutma sesi kesilir. İstemci ana thread'inde.
     */
    public static void yutmaCal() {
        Minecraft mc = Minecraft.getInstance();
        SoundManager sesler = mc.getSoundManager();
        yutmayiKes(sesler);
        if (Float.isNaN(k)) {
            k = olc(mc).k();
        }
        kuruYutma = new Ornek(PulsarSesleri.KARADELIK_YUTMA.get(), false, false);
        yankiYutma = new Ornek(PulsarSesleri.KARADELIK_YUTMA_YANKILI.get(), false, true);
        sesler.play(kuruYutma);
        sesler.play(yankiYutma);
        DogalAfetler.LOGGER.info("[pulsar] yutma sesi çalındı (kuru + yankılı) k={}", String.format(Locale.ROOT, "%.3f", k));
    }

    /** Oyuncunun şu anki konumunda sönüm (anında ölçüm, yumuşatmasız). İstemci ana thread'inde. */
    public static Sonum sonumBilgisi() {
        return olc(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        SoundManager sesler = mc.getSoundManager();
        if (mc.level == null || mc.player == null) {
            temizle(sesler);
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        float hedefGuc = pulsarGucu(mc.level);
        boolean yutmaCaliyor = kuruYutma != null && (sesler.isActive(kuruYutma) || sesler.isActive(yankiYutma));
        if (Float.isNaN(hedefGuc) && kuruDongu == null && !yutmaCaliyor) {
            k = Float.NaN;
            return;
        }
        float hedefK = olc(mc).k();
        k = Float.isNaN(k) ? hedefK : k + (hedefK - k) * YUMUSAMA;
        if (Float.isNaN(hedefGuc)) {
            donguyuDurdur(sesler, "evre/boyut");
            return;
        }
        guc = hedefGuc;
        yas++;
        if (kuruDongu == null) {
            donguyuBaslat(sesler);
            DogalAfetler.LOGGER.info("[pulsar] vuruş sesi başladı (kuru + yankılı) güç={} k={}",
                    String.format(Locale.ROOT, "%.3f", guc), String.format(Locale.ROOT, "%.3f", k));
            return;
        }
        boolean caliyor = sesler.isActive(kuruDongu) && sesler.isActive(yankiDongu);
        boolean sessiz = mc.options.getSoundSourceVolume(SoundSource.MASTER) <= 0f
                || mc.options.getSoundSourceVolume(SoundSource.WEATHER) <= 0f;
        if (NOBET.yenidenCalinmali(false, false, caliyor, sessiz)) {
            donguyuKes(sesler);
            donguyuBaslat(sesler);
            DogalAfetler.LOGGER.info("[pulsar] vuruş sesi yeniden başladı (ses motoru kesti), faz sıfırlandı");
        }
    }

    @SubscribeEvent
    public static void cizim(TickEvent.RenderTickEvent olay) {
        if (olay.phase == TickEvent.Phase.START && kuruDongu != null) {
            saatIlerle();
        }
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        temizle(Minecraft.getInstance().getSoundManager());
    }

    /** Döngünün gücü 0–1; NaN: bu oyuncu için pulsar sesi yok (başka evre, başka boyut ya da pulsar yutuldu). */
    private static float pulsarGucu(ClientLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return Float.NaN;
        }
        IstemciGokyuzu.Gorunum g = IstemciGokyuzu.gorunum(0f);
        return switch (g.evre()) {
            case PULSAR -> PulsarEgrileri.yogunluk(g.ilerleme());
            case KARADELIK_YUTMA -> {
                float alfa = KaraDelikYutmaEgrileri.pulsarAlfa(g.saniye());
                yield alfa > 0f ? alfa : Float.NaN;
            }
            default -> Float.NaN;
        };
    }

    private static Sonum olc(Minecraft mc) {
        LocalPlayer oyuncu = mc.player;
        ClientLevel level = mc.level;
        if (oyuncu == null || level == null) {
            return BOS;
        }
        int ust = Mth.floor(oyuncu.getEyeY()) + 1;
        int bx = oyuncu.getBlockX();
        int bz = oyuncu.getBlockZ();
        BlockPos.MutableBlockPos hucre = new BlockPos.MutableBlockPos();
        int acik = 0;
        for (int dx = -SUTUN_YARICAP; dx <= SUTUN_YARICAP; dx++) {
            for (int dz = -SUTUN_YARICAP; dz <= SUTUN_YARICAP; dz++) {
                if (level.getBrightness(LightLayer.SKY, hucre.set(bx + dx, ust, bz + dz)) >= 15) {
                    acik++;
                }
            }
        }
        int kenar = 2 * SUTUN_YARICAP + 1;
        float oran = acik / (float) (kenar * kenar);
        double y = oyuncu.getY();
        float sonumK = SesSonumu.k(oran, y);
        return new Sonum(y, oran, sonumK, SesSonumu.kuruKazanc(sonumK), SesSonumu.yankiKazanc(sonumK));
    }

    private static void donguyuBaslat(SoundManager sesler) {
        yas = 0;
        kuruDongu = new Ornek(PulsarSesleri.PULSAR_VURUS.get(), true, false);
        yankiDongu = new Ornek(PulsarSesleri.PULSAR_VURUS_YANKILI.get(), true, true);
        sesler.play(kuruDongu);
        sesler.play(yankiDongu);
        calmaNanos = 0L;
        sonNanos = System.nanoTime();
    }

    private static void donguyuDurdur(SoundManager sesler, String neden) {
        if (kuruDongu != null) {
            donguyuKes(sesler);
            DogalAfetler.LOGGER.info("[pulsar] vuruş sesi durdu ({})", neden);
        }
    }

    private static void donguyuKes(SoundManager sesler) {
        kes(sesler, kuruDongu);
        kes(sesler, yankiDongu);
        kuruDongu = null;
        yankiDongu = null;
    }

    private static void yutmayiKes(SoundManager sesler) {
        kes(sesler, kuruYutma);
        kes(sesler, yankiYutma);
        kuruYutma = null;
        yankiYutma = null;
    }

    private static void kes(SoundManager sesler, Ornek ornek) {
        if (ornek != null) {
            ornek.bitir();
            sesler.stop(ornek);
        }
    }

    private static void temizle(SoundManager sesler) {
        donguyuDurdur(sesler, "çıkış");
        yutmayiKes(sesler);
        k = Float.NaN;
    }

    private static void saatIlerle() {
        long simdi = System.nanoTime();
        if (!Minecraft.getInstance().isPaused()) {
            calmaNanos += simdi - sonNanos;
        }
        sonNanos = simdi;
    }

    /** Konumsuz (dinleyicide) örnek; düzeyini her tick sönümden, döngüde ayrıca pulsar gücünden ve açılıştan okur. */
    private static final class Ornek extends AbstractTickableSoundInstance {
        private final boolean dongu;
        private final boolean yankili;

        private Ornek(SoundEvent ses, boolean dongu, boolean yankili) {
            super(ses, SoundSource.WEATHER, SoundInstance.createUnseededRandom());
            this.dongu = dongu;
            this.yankili = yankili;
            this.looping = dongu;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = true;
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
            this.volume = duzey();
        }

        private float duzey() {
            float kazanc = yankili ? SesSonumu.yankiKazanc(k) : SesSonumu.kuruKazanc(k);
            return dongu ? kazanc * guc * Math.min(1f, yas / (float) GIRIS_TICK) : kazanc;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (!isStopped()) {
                volume = duzey();
            }
        }

        private void bitir() {
            stop();
        }
    }
}
