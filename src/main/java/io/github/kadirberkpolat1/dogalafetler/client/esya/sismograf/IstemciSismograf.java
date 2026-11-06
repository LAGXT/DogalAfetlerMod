package io.github.kadirberkpolat1.dogalafetler.client.esya.sismograf;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.esya.EsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.HalkaTampon;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.OncuSarsinti;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.SismikDalga;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.YonMesafe;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarCevirileri;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.Sismograf;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografCevirileri;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografIstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografPaketi;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sismografın istemci tarafı (madde 13b). Sunucudan gelen algılama durumunu tutar ve sismograf taşınırken:
 * <ul>
 * <li>her istemci tick'inde bir sismik dalga örneğini 60 sn'lik halka tampona yazar (grafik ekranı bunu çizer);</li>
 * <li>öncü sarsıntıda (ON_UYARI ve SIREN) darbeler üretir: her darbede kısık bir zemin gürlemesi (yalnız bu istemcide) ve
 * çok hafif kamera titremesi;</li>
 * <li>saniyede bir yalnız bu oyuncunun action bar'ına "Öncü sarsıntı algılandı · %şiddet · yön mesafe" yazar (N6
 * istisnası; Hassas Sismograf seviyeyi ekler).</li>
 * </ul>
 * Sismograf taşınmazken hiçbir şey yapmaz; tampon boşalır. Çıkışta sıfırlanır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciSismograf {
    /** 60 sn × 20 tick. */
    public static final int ORNEK_SAYISI = 1200;
    private static final HalkaTampon TAMPON = new HalkaTampon(ORNEK_SAYISI);

    private static SismografPaketi durum = SismografPaketi.YOK;
    private static long tick;
    private static int darbede = -1;
    private static float zarf;
    private static float oncekiZarf;
    private static int darbeSayisi;
    private static int actionBarSayisi;
    /** Son yazılan action bar metni ve tick'i: radar paneli o satırın üstüne kalkar. */
    private static Component sonMetin;
    private static long sonMetinTick = Long.MIN_VALUE;

    private IstemciSismograf() {
    }

    public static Consumer<SismografPaketi> alici() {
        return IstemciSismograf::guncelle;
    }

    static void guncelle(SismografPaketi paket) {
        boolean yeni = paket.algilandi() && !durum.algilandi();
        durum = paket;
        if (yeni) {
            actionBar(Minecraft.getInstance());
        }
    }

    /** Sağ tık ({@code Sismograf.use}, N15 yolu): grafik ekranını açar. */
    public static void ekraniAc() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !SismografIstemciAyarlari.grafik()) {
            return;
        }
        Sismograf sismograf = Sismograf.tasinan(mc.player);
        mc.setScreen(new SismografEkrani(Component.translatable(sismograf != null && sismograf.hassas()
                ? "item.dogalafetler.hassas_sismograf" : "item.dogalafetler.sismograf")));
    }

    public static SismografPaketi durum() {
        return durum;
    }

    static HalkaTampon tampon() {
        return TAMPON;
    }

    static long tick() {
        return tick;
    }

    /** Oturumdaki öncü darbe sayısı (görsel kanıt otomasyonu log'a yazar). */
    public static int darbeSayisi() {
        return darbeSayisi;
    }

    /** Oturumda yazılan action bar sayısı (otomasyon). */
    public static int actionBarSayisi() {
        return actionBarSayisi;
    }

    /** Ekrandaki sismograf action bar satırının genişliği (vanilla 60 tick + 20 tick sönüm); yoksa 0. */
    public static int actionBarGenisligi(Font font) {
        return sonMetin != null && tick - sonMetinTick <= 80 ? font.width(sonMetin) : 0;
    }

    static float zarf(float pt) {
        return Mth.lerp(pt, oncekiZarf, zarf);
    }

    @SubscribeEvent
    public static void istemciTick(TickEvent.ClientTickEvent olay) {
        Minecraft mc = Minecraft.getInstance();
        if (olay.phase != TickEvent.Phase.END || mc.player == null || mc.isPaused()) {
            return;
        }
        tick++;
        oncekiZarf = zarf;
        if (!durum.tasiyor() || Sismograf.tasinan(mc.player) == null) {
            if (TAMPON.boyut() > 0) {
                TAMPON.temizle();
            }
            zarf = 0;
            darbede = -1;
            return;
        }
        boolean oncu = durum.algilandi() && (durum.asama() == AfetAsamasi.ON_UYARI || durum.asama() == AfetAsamasi.SIREN);
        if (oncu) {
            if (darbede < 0 || darbede >= OncuSarsinti.darbeAraligi(durum.oncu())) {
                darbede = 0;
                darbeSayisi++;
                sesCal(mc);
            }
            zarf = (float) OncuSarsinti.zarf(darbede);
            darbede++;
        } else {
            zarf = 0;
            darbede = -1;
        }
        double genlik = SismikDalga.genlik(durum.algilandi(), durum.asama(), durum.oncu(), zarf);
        TAMPON.ekle((float) (SismikDalga.ornek(tick, durum.afetNo()) * genlik));
        if (durum.algilandi() && tick % 20 == 0) {
            actionBar(mc);
        }
    }

    private static void sesCal(Minecraft mc) {
        if (!SismografIstemciAyarlari.ses()) {
            return;
        }
        float perde = 0.85f + 0.15f * (float) Math.sin(darbeSayisi * 1.7);
        mc.getSoundManager().play(new SimpleSoundInstance(EsyaKayitlari.ONCU_SARSINTI_ADI, SoundSource.AMBIENT,
                OncuSarsinti.sesSeviyesi(durum.oncu()), perde, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, 0, 0, 0, true));
    }

    private static void actionBar(Minecraft mc) {
        if (mc.player != null && durum.algilandi()) {
            sonMetin = metin(mc.player, durum);
            sonMetinTick = tick;
            mc.player.displayClientMessage(sonMetin, true);
            actionBarSayisi++;
        }
    }

    /** "Öncü sarsıntı algılandı · %45 · KD 240 m [· Seviye: Şiddetli]" (ana sarsıntıda başlık değişir). */
    static Component metin(Player oyuncu, SismografPaketi d) {
        double mx = d.tahminX() + 0.5;
        double mz = d.tahminZ() + 0.5;
        double mesafe = YonMesafe.mesafe(oyuncu.getX(), oyuncu.getZ(), mx, mz);
        int yon = YonMesafe.sekizYon(YonMesafe.pusulaAcisi(oyuncu.getX(), oyuncu.getZ(), mx, mz));
        long gosterilen = d.hassas() ? Math.round(mesafe) : Math.round(mesafe / 10.0) * 10;
        MutableComponent m = Component.translatable(SismografCevirileri.ON + (d.asama() == AfetAsamasi.AKTIF ? "ana" : "oncu"))
                .withStyle(ChatFormatting.GOLD);
        m.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
        m.append(Component.translatable(SismografCevirileri.ON + "yuzde", d.yuzde()).withStyle(ChatFormatting.YELLOW));
        m.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
        m.append(Component.translatable(SismografCevirileri.ON + "konum", Component.translatable(RadarCevirileri.yonAnahtari(yon)),
                gosterilen).withStyle(ChatFormatting.WHITE));
        if (d.hassas() && d.seviye() > 0) {
            m.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
            m.append(Component.translatable(SismografCevirileri.ON + "seviye",
                    Component.translatable("seviye.dogalafetler." + d.seviye())).withStyle(ChatFormatting.RED));
        }
        return m;
    }

    /** Çok hafif titreme: yalnız öncü darbe sürerken, CLIENT config ile kapatılabilir, şiddet SERVER config'ten. */
    @SubscribeEvent
    public static void kameraAcilari(ViewportEvent.ComputeCameraAngles olay) {
        if (zarf <= 0 && oncekiZarf <= 0 || !durum.algilandi() || !SismografIstemciAyarlari.titreme()) {
            return;
        }
        float pt = (float) olay.getPartialTick();
        double a = OncuSarsinti.titremeAcisi(durum.oncu(), zarf(pt), durum.titreme());
        double t = tick + pt;
        olay.setRoll((float) (olay.getRoll() + a * Math.sin(t * 1.9)));
        olay.setPitch((float) (olay.getPitch() + a * 0.6 * Math.sin(t * 2.7 + 1.0)));
        olay.setYaw((float) (olay.getYaw() + a * 0.4 * Math.sin(t * 1.3 + 2.0)));
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        durum = SismografPaketi.YOK;
        sonMetin = null;
        TAMPON.temizle();
        zarf = oncekiZarf = 0;
        darbede = -1;
    }
}
