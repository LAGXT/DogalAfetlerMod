package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.BakmaKurallari;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.network.BakmaPaketi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Gökyüzüne bakma algısı (istemci): oyuncunun bakış yönü gökteki bir cisme yeterince yakınsa sayaç tick'te bir artar.
 * <ul>
 * <li>Devasa kara delik: {@value BakmaKurallari#DEVASA_TIK} tick (5 sn) bakılmalı; bir an kopma ({@value #DEVASA_KAYIP_TICK} tick'e kadar) sayacı sıfırlamaz.</li>
 * <li>Gezegenler (Merkür…Neptün ve Plüton, sekizi de): her birine <b>toplam</b> {@value BakmaKurallari#GEZEGEN_TIK} tick (3 sn); küçük
 * gezegeni sabit tutmak zor olduğundan kümülatiftir. Birbirine yakın iki gezegenden bakışa en yakını sayar. Gezegenler gece yarısı
 * göğün güneş karşıtı noktasının çevresinde kendi yuvasındadır ({@code GezegenYerlesimi}); gece yarısı hepsi ufkun üzerindedir.</li>
 * </ul>
 * Cisimler yalnız gerçekten çizildiğinde sayılır: Overworld, zengin gökyüzü (ay yok / olay), derin uzay alfası ≥ {@value #GORUNUR_ALFA}
 * (süpernova gününde de), nebula katmanı açık, körlük/karanlık etkisi yok, göz göğe açık, cisim ufkun üzerinde. Paket yalnız görev
 * (devasa bir kez, her gezegen bir kez) gider; sunucu ilerlemeyi oyuncu başına tutar ({@code BakmaIlerlemesi}). Yalnız
 * istemci ana thread'i.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class GokyuzuBakisi {
    /**
     * Derin uzay bu alfanın altında çizilse bile sayılır (K-E84: eskiden 0,5; süpernova gününün parlaklığında gök cisimleri 0,15–0,4 alfayla
     * görünür durur ama bakma hiç sayılmıyordu). {@code 0,12}: alfa tabana yakın ama çizim hala sıfır değil.
     */
    private static final float GORUNUR_ALFA = 0.12f;
    /** Gezegene bakarken en az bu açı (derece) hata kabul edilir; büyük/halkalı gezegende görünür yarıçapın 1,5 katı. */
    private static final float GEZEGEN_TOLERANS = 3.5f;
    /** Devasa kara delik için en az bu açı (derece); yarıçapın 0,8 katı daha büyükse o (gök saniyede ~1° döner). */
    private static final float DEVASA_TOLERANS = 5f;
    /** Bakış ya da görünürlük bu kadar tick kopunca devasa sayaç sıfırlanır. */
    private static final int DEVASA_KAYIP_TICK = 10;
    /** Cisim ufkun bu kadar üzerinde değilse (dünya yönü y bileşeni) sayılmaz. */
    private static final float UFUK = 0.02f;

    private static final Gezegenler.Durum DURUM = new Gezegenler.Durum();
    private static final int[] GEZEGEN_SAYAC = new int[BakmaKurallari.GEZEGEN_SAYISI];
    private static int gonderilenMaske;
    private static int devasaSayac;
    private static int devasaKayip;
    private static boolean devasaGonderildi;

    private GokyuzuBakisi() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer oyuncu = mc.player;
        ClientLevel level = mc.level;
        if (oyuncu == null || level == null || mc.isPaused()) {
            return;
        }
        boolean gezegenlerTamam = BakmaKurallari.tamMi(gonderilenMaske);
        if (devasaGonderildi && gezegenlerTamam) {
            return;
        }
        if (!bakilabilir(mc, level, oyuncu)) {
            devasaKaybi();
            return;
        }
        float zaman = level.getTimeOfDay(1f);
        float sn = ((level.getGameTime() % 72000L) + 1f) / 20f;
        Quaternionf donus = GokyuzuCizici.gokDonusu(zaman, sn);
        Vec3 bakis = oyuncu.getViewVector(1f);
        Vector3f yon = new Vector3f();
        if (!devasaGonderildi) {
            devasaBak(donus, bakis, yon);
        }
        if (!gezegenlerTamam) {
            gezegenBak(donus, bakis, yon);
        }
    }

    private static void devasaBak(Quaternionf donus, Vec3 bakis, Vector3f yon) {
        donus.transform(KaraDelikler.devasaYon(), yon);
        float tolerans = Math.max(DEVASA_TOLERANS, 0.8f * KaraDelikler.DEVASA_DERECE);
        if (yon.y > UFUK && yakin(yon, bakis, tolerans)) {
            devasaKayip = 0;
            if (++devasaSayac >= BakmaKurallari.DEVASA_TIK) {
                devasaGonderildi = true;
                AgKanali.sunucuyaGonder(new BakmaPaketi(BakmaKurallari.TUR_DEVASA, 0));
            }
        } else {
            devasaKaybi();
        }
    }

    /**
     * Bakış ya da görünürlük bir an kopunca (fare sıçraması, göğün dönmesi) sayaç hemen sıfırlanmaz: {@value #DEVASA_KAYIP_TICK}
     * tick'ten uzun süren kopma sıfırlar (K-E84; eskiden tek tick'lik kopma 5 saniyelik bakmayı siliyordu).
     */
    private static void devasaKaybi() {
        if (++devasaKayip > DEVASA_KAYIP_TICK) {
            devasaSayac = 0;
        }
    }

    private static void gezegenBak(Quaternionf donus, Vec3 bakis, Vector3f yon) {
        DURUM.hesapla(Gezegenler.zaman());
        int en = -1;
        float enKos = -2f;
        for (int i = 0; i < BakmaKurallari.GEZEGEN_SAYISI; i++) {
            if (BakmaKurallari.maskedeVar(gonderilenMaske, i)) {
                continue;
            }
            donus.transform(yon.set(DURUM.yon[3 * i], DURUM.yon[3 * i + 1], DURUM.yon[3 * i + 2]));
            if (yon.y <= UFUK) {
                continue;
            }
            float tolerans = Math.max(GEZEGEN_TOLERANS, 1.5f * Gezegenler.gorunurYaricap(i, DURUM.derece[i]));
            float kos = (float) (yon.x * bakis.x + yon.y * bakis.y + yon.z * bakis.z);
            if (kos >= kosinus(tolerans) && kos > enKos) {
                en = i;
                enKos = kos;
            }
        }
        if (en >= 0 && ++GEZEGEN_SAYAC[en] >= BakmaKurallari.GEZEGEN_TIK) {
            gonderilenMaske = BakmaKurallari.maskeyeEkle(gonderilenMaske, en);
            AgKanali.sunucuyaGonder(new BakmaPaketi(BakmaKurallari.TUR_GEZEGEN, en));
        }
    }

    private static boolean yakin(Vector3f yon, Vec3 bakis, float toleransDerece) {
        return yon.x * bakis.x + yon.y * bakis.y + yon.z * bakis.z >= kosinus(toleransDerece);
    }

    private static float kosinus(float derece) {
        return (float) Math.cos(Math.toRadians(derece));
    }

    /** Derin uzay katmanı bu karede gerçekten çiziliyor mu ve oyuncu göğe bakabiliyor mu. */
    private static boolean bakilabilir(Minecraft mc, ClientLevel level, LocalPlayer oyuncu) {
        if (level.dimension() != Level.OVERWORLD || oyuncu.hasEffect(MobEffects.BLINDNESS)
                || oyuncu.hasEffect(MobEffects.DARKNESS) || !GokyuzuAyarlari.nebulaKatmani()) {
            return false;
        }
        IstemciGokyuzu.Gorunum d = IstemciGokyuzu.gorunum(1f);
        if (!GokyuzuRenkleri.zengin(d)) {
            return false;
        }
        float uzay = GokyuzuRenkleri.uzayAlfa(d, level.getTimeOfDay(1f)) * (1f - level.getRainLevel(1f));
        return uzay >= GORUNUR_ALFA && level.canSeeSky(BlockPos.containing(oyuncu.getEyePosition(1f)));
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        java.util.Arrays.fill(GEZEGEN_SAYAC, 0);
        gonderilenMaske = 0;
        devasaSayac = 0;
        devasaKayip = 0;
        devasaGonderildi = false;
    }
}
