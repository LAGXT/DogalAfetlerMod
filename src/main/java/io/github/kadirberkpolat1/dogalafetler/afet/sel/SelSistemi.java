package io.github.kadirberkpolat1.dogalafetler.afet.sel;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik.SelKurallari;
import io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik.SelTetikleyicileri;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.BitisNedeni;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonumTipi;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Selin mod kurucusundan çağrılan tek kayıt noktası: config, afet fabrikası ve doğal seçim tetikleyicileri. Sel; yağmur ve
 * fırtınayla, süren bir tsunamiyle anında, tsunami doğal bitince ve ay çarpmasından sonra geçici çarpanla daha olasıdır
 * ({@link SelTetikleyicileri}). Su ve ısı buharlaşması ortak sanal sudadır ({@code su}).
 */
public final class SelSistemi {
    private SelSistemi() {
    }

    public static void kaydet() {
        SelAyarlari.kaydet();
        AfetKaydi.kaydet(AfetTuru.SEL, Sel::new);
        MinecraftForge.EVENT_BUS.addListener(SelSistemi::afetBitti);
        MinecraftForge.EVENT_BUS.addListener(SelSistemi::nefes);
    }

    /**
     * Doğal seçimde selin anında çarpanı: üst dünyada yağmur/fırtına ve süren tsunami. Geçici çarpanlar (tsunami sonrası, ay
     * çarpması) dönemin çarpan listesindedir ve ayrıca uygulanır.
     */
    public static double secimCarpani(AfetYoneticisi yonetici, MinecraftServer server) {
        ServerLevel ust = server.overworld();
        // Hava bayrağına bakılır: Level.isRaining() yumuşak geçişli yağmur düzeyini okur ve bayrak değişince saniyelerce gecikir.
        return SelTetikleyicileri.anlikCarpan(ust.getLevelData().isRaining(), ust.getLevelData().isThundering(),
                yonetici.aktifAfet(AfetTuru.TSUNAMI).isPresent(), SelAyarlari.anlikTetikleyiciler());
    }

    /**
     * Sel suyunda boğulma (K-E81): gözü sel suyunun içinde olan ve nefes alamayan canlı, vanilla'nın tükettiği havanın
     * {@link SelAyarlari#bogulmaCarpani} katını tüketir: hava çabuk biter, boğulma hasarı sık gelir. Su altı solunumu, ölümsüzlük
     * ve yaratıcı mod vanilla gibi boğulmayı zaten engeller ({@code canBreathe}); sunucuda çalışır (hava sunucu otoritesidir).
     */
    static void nefes(LivingBreatheEvent olay) {
        LivingEntity en = olay.getEntity();
        int taban = olay.getConsumeAirAmount();
        if (olay.canBreathe() || taban <= 0 || en.level().isClientSide() || !SanalSu.gozSelSuyundaMi(en)) {
            return;
        }
        olay.setConsumeAirAmount(SelKurallari.bogulmaTuketimi(taban, en.tickCount, SelAyarlari.bogulmaCarpani()));
    }

    /** Tsunami doğal bitti: deniz taşması bir süre daha olası (geçici çarpan). */
    static void afetBitti(AfetBittiEvent olay) {
        if (olay.tur() == AfetTuru.TSUNAMI && olay.neden() == BitisNedeni.DOGAL) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                geciciCarpanEkle(server, SelAyarlari.tsunamiSonrasi(), "tsunami bitti");
            }
        }
    }

    /** Ay çarptı: sel olasılığı uzun süre yüksek kalır (çekirdeğin çarpan listesine eklenir). */
    public static void ayCarpmasiOldu(MinecraftServer server) {
        geciciCarpanEkle(server, SelAyarlari.ayCarpmasiSonrasi(), "ay çarptı");
    }

    private static void geciciCarpanEkle(MinecraftServer server, SelTetikleyicileri.Gecici gecici, String neden) {
        if (!gecici.etkili()) {
            return;
        }
        AfetYoneticisi.al(server).donem().carpanEkle(AfetTuru.SEL, gecici.tepe(), gecici.sabitTick(), gecici.sonumTick(),
                SonumTipi.DOGRUSAL);
        DogalAfetler.LOGGER.info("sel: {}; sel çarpanı x{} ({} sn tepede, {} sn sönüm)", neden,
                String.format(Locale.ROOT, "%.2f", gecici.tepe()), gecici.sabitTick() / 20, gecici.sonumTick() / 20);
    }
}
