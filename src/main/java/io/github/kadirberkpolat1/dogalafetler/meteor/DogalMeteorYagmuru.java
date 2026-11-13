package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Meteor modülünün kendi yağmuru: meteorlar eskiden yalnız güneş patlaması ve ay çarpması afetlerinde yağdığından (ikisi de
 * nadir) normal oyunda neredeyse hiç görülmüyordu. Dünya'da her dakikada bir zar atılır; başlama olasılığı
 * {@code meteor.dogalYagmurBeklenen} (oyun günü başına beklenen yağmur) sayısından türetilir. Yağmur tüm oyunculara
 * {@link MeteorAPI#yagmur} ile düşer; başka bir yağmur (afet ya da komut) sürerken yenisi başlamaz. {@code dogalAfetlerAcik}
 * kapalıyken yağmaz. Her yağmurun kendi kaynağı vardır ({@code afetNo} = başlangıç tick'i + 1; tür yok), böylece ödül tavanı
 * ({@code meteor.odulTavaniAfetBasina}) her yağmurda sıfırlanır. {@code afetSikligi} bu yağmuru ölçeklemez (yalnız afet
 * takvimini).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class DogalMeteorYagmuru {
    /** Zar aralığı (tick): 1 dakika; bir oyun gününde {@value #GUNLUK_ZAR} zar. */
    static final int ZAR_ARALIGI = 1200;
    static final int GUNLUK_ZAR = 24000 / ZAR_ARALIGI;

    private DogalMeteorYagmuru() {
    }

    /** Bir zarda yağmur başlama olasılığı: günde beklenen sayı {@code beklenen} olacak şekilde (1 − e^(−beklenen/zar)). */
    static double zarOlasiligi(double beklenen) {
        return beklenen <= 0 ? 0 : 1 - Math.exp(-beklenen / GUNLUK_ZAR);
    }

    @SubscribeEvent
    public static void dunyaTick(TickEvent.LevelTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END || !(olay.level instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD || level.getGameTime() % ZAR_ARALIGI != 0) {
            return;
        }
        double olasilik = zarOlasiligi(MeteorAyarlari.dogalYagmurBeklenen());
        if (olasilik <= 0 || !level.getGameRules().getBoolean(Kurallar.DOGAL_AFETLER_ACIK)
                || MeteorAPI.aktifYagmurSayisi(level) > 0 || level.random.nextDouble() >= olasilik) {
            return;
        }
        boolean oyuncuVar = false;
        for (ServerPlayer o : level.players()) {
            if (o.isAlive() && !o.isSpectator()) {
                oyuncuVar = true;
                break;
            }
        }
        if (!oyuncuVar) {
            return;
        }
        int sure = MeteorAyarlari.dogalYagmurSaniye() * 20;
        YanginKaynagi kaynak = new YanginKaynagi(null, level.getGameTime() + 1);
        int kimlik = MeteorAPI.yagmur(level, List.of(), sure, MeteorAyarlari.dogalYagmurDakikaBasina(),
                MeteorAyarlari.dogalYagmurBuyukOrani(), kaynak);
        DogalAfetler.LOGGER.info("meteor modülü kendi yağmurunu başlattı (#{}): {} sn, oyuncu başına dakikada {}", kimlik,
                MeteorAyarlari.dogalYagmurSaniye(), MeteorAyarlari.dogalYagmurDakikaBasina());
    }
}
