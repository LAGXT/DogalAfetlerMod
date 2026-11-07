package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GunSaatiSabitleme;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * Güneş patlaması AKTIF (büyüme/kavrulma) boyunca üst dünyanın gün saatini sabit tutar (gökyüzü-2 kullanıcı isteği 25):
 * başta ilk öğleye geçilir, her tick sonunda vanilla'nın ilerlettiği saat geri alınır ({@link GunSaatiSabitleme}); istemci
 * saat paketinin "gün döngüsü" bayrağı kapalı gönderildiği için istemcide de güneş kaymaz. {@code doDaylightCycle}
 * gamerule'üne dokunulmaz (oyuncunun kuralı aynen kalır); sabit saat {@code ekVeri("gun_saati")}'nde kalıcıdır, AKTIF bitince
 * silinir ve vanilla döngü kaldığı yerden sürer. Siren (gece aurora) ve süpernova sonrası dönemler etkilenmez. Oyun günü ve
 * afetin 5 günlük süresi gün saatinden değil ölçekli saatten/oyun zamanından sayılır.
 */
final class GunSaatiSabitleyici {
    static final String KIMLIK = "gun_saati";
    static final String HEDEF = "sabit";
    /** Sabit saatin log'a yazılma aralığı (sunucu tick'i). */
    private static final int LOG_ARALIGI = 1_200;

    private GunSaatiSabitleyici() {
    }

    static void kaydet() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, false, TickEvent.ServerTickEvent.class,
                GunSaatiSabitleyici::tick);
    }

    /** Gün saati şu an sabit tutulmalı mı: güneş patlaması AKTIF aşamasında. */
    static boolean sabitlenmeli(AfetYoneticisi yonetici) {
        return yonetici.aktifAfet().filter(a -> a.tur() == AfetTuru.GUNES_PATLAMASI && a.asama() == AfetAsamasi.AKTIF)
                .isPresent();
    }

    private static void tick(TickEvent.ServerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = olay.getServer();
        ServerLevel ust = server.overworld();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        CompoundTag veri = donem.ekVeri(KIMLIK);
        boolean dongu = ust.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        long simdi = ust.getDayTime();
        if (!sabitlenmeli(yonetici)) {
            if (veri.contains(HEDEF)) {
                veri.remove(HEDEF);
                donem.ekVeriDegisti(KIMLIK);
                gonder(server, ust, dongu);
                DogalAfetler.LOGGER.info("Gün saati serbest: dayTime={} (güneş patlaması AKTIF bitti), doDaylightCycle={}",
                        simdi, dongu);
            }
            return;
        }
        boolean yeni = !veri.contains(HEDEF);
        long hedef;
        if (yeni) {
            hedef = GunSaatiSabitleme.ilkHedef(simdi);
            DogalAfetler.LOGGER.info("Gün saati sabit (güneş patlaması AKTIF): dayTime {} -> {} (öğle), doDaylightCycle={} "
                    + "(dokunulmadı), oyun günü {}", simdi, hedef, dongu, yonetici.oyunGunu());
        } else {
            long eski = veri.getLong(HEDEF);
            hedef = GunSaatiSabitleme.tut(eski, simdi, dongu);
            if (hedef != eski) {
                DogalAfetler.LOGGER.info("Gün saati dışarıdan değişti ({} -> {}): yeni sabit {}", eski, simdi, hedef);
            }
        }
        if (yeni || veri.getLong(HEDEF) != hedef) {
            veri.putLong(HEDEF, hedef);
            donem.ekVeriDegisti(KIMLIK);
        }
        if (simdi != hedef) {
            ust.setDayTime(hedef);
        }
        // Vanilla 20 tick'te bir gün döngüsü bayrağıyla saat paketi yollar (bu olaydan önce); bizimki ardından gelip
        // istemcinin saati kendi başına ilerletmesini kapatır.
        if (yeni || server.getTickCount() % 20 == 0) {
            gonder(server, ust, false);
        }
        if (server.getTickCount() % LOG_ARALIGI == 0) {
            DogalAfetler.LOGGER.info("Gün saati sabit: dayTime={} oyun günü {} (güneş patlaması AKTIF)", ust.getDayTime(),
                    yonetici.oyunGunu());
        }
    }

    private static void gonder(MinecraftServer server, ServerLevel ust, boolean gunDongusu) {
        server.getPlayerList().broadcastAll(new ClientboundSetTimePacket(ust.getGameTime(), ust.getDayTime(), gunDongusu),
                ust.dimension());
    }
}
