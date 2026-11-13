package io.github.kadirberkpolat1.dogalafetler.client.uyum;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.sicaklik.IstemciSicaklik;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.PunchySicaklikMantigi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikDurumu;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import punchy.api.PunchyApi;

/**
 * Punchy 3.2+ {@link PunchyApi} ile dört sıcaklık durumunun ({@code BUNALMA}, {@code YANMA}, {@code USUME}, {@code DONMA})
 * birinci şahıs entegrasyonu. Bu sınıf yalnız Punchy yüklüyken {@link PunchyIstemciOlaylari} tarafından çağrılır; Punchy yoksa
 * hiç yüklenmez.
 * <ul>
 * <li>Kaynak paketlerinin {@code when}/{@code unless} alanlarında kullanabileceği {@code dogalafetler:bunalma},
 *     {@code dogalafetler:yanma}, {@code dogalafetler:usume}, {@code dogalafetler:donma}, {@code dogalafetler:islak}
 *     koşullarını kaydeder.</li>
 * <li>Birinci şahıs kollarında yanık ve kırağı kaplamalarını ({@code PunchyApi.armStates}) ve sürekli hafif titreme/soluma
 *     salınımını ({@code PunchyApi.armPose}) sürer.</li>
 * <li>Oyuncu boştayken anormal sıcaklık durumuna girildiğinde ve periyodik aralıklarla tepki animasyonunu
 *     ({@code vz_sicaklik_*}) çatışmayı bölmeden tetikler.</li>
 * </ul>
 */
final class PunchySicaklikKopru {
    private static boolean kayitDenedi;
    private static boolean apiHazir;
    private static SicaklikDurumu sonOynatilan = SicaklikDurumu.NORMAL;
    private static long sonOynatmaTick = -1L;

    private PunchySicaklikKopru() {
    }

    static void sifirla() {
        sonOynatilan = SicaklikDurumu.NORMAL;
        sonOynatmaTick = -1L;
    }

    static void istemciTick(Minecraft mc) {
        if (!kayitDenedi) {
            kaydet();
        }
        if (!apiHazir) {
            return;
        }
        LocalPlayer oyuncu = mc.player;
        if (oyuncu == null || mc.level == null || mc.isPaused() || !IstemciSicaklik.acik()) {
            sonOynatilan = SicaklikDurumu.NORMAL;
            return;
        }
        SicaklikDurumu guncel = IstemciSicaklik.sicaklikDurumu();
        if (guncel == SicaklikDurumu.NORMAL) {
            sonOynatilan = SicaklikDurumu.NORMAL;
            return;
        }
        try {
            if (!PunchyApi.isActive()) {
                return;
            }
            boolean mesgul = oyuncuMesgulMu(mc, oyuncu);
            long simdi = IstemciSicaklik.tick();
            if (!PunchySicaklikMantigi.tepkiTetiklenmeli(sonOynatilan, guncel, simdi, sonOynatmaTick, mesgul)) {
                return;
            }
            String klip = PunchySicaklikMantigi.klipAdi(guncel);
            String moment = PunchySicaklikMantigi.momentAdi(guncel);
            if (moment != null) {
                PunchyApi.moment(moment);
            }
            if (klip != null && PunchyApi.clipExists(klip)) {
                PunchyApi.playClip(
                        DogalAfetler.MOD_ID,
                        PunchyApi.ClipRequest.named(klip)
                                .bothArms()
                                .blendIn(0.18f)
                                .blendOut(0.22f)
                                .kick(false));
            }
            sonOynatilan = guncel;
            sonOynatmaTick = simdi;
        } catch (LinkageError | RuntimeException hata) {
            apiHazir = false;
        }
    }

    private static boolean oyuncuMesgulMu(Minecraft mc, LocalPlayer oyuncu) {
        if (mc.screen != null || oyuncu.isSpectator() || oyuncu.isSleeping() || oyuncu.isPassenger()) {
            return true;
        }
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            return true;
        }
        if (oyuncu.isUsingItem() || oyuncu.swinging || mc.options.keyAttack.isDown() || mc.options.keyUse.isDown()) {
            return true;
        }
        if (oyuncu.isSwimming() || oyuncu.onClimbable() || oyuncu.isFallFlying()) {
            return true;
        }
        PunchyApi.State durum = PunchyApi.state();
        if (durum == null || !durum.active()) {
            return true;
        }
        return aktifAksiyonKlibiVar(durum.clipOn(true)) || aktifAksiyonKlibiVar(durum.clipOn(false));
    }

    private static boolean aktifAksiyonKlibiVar(PunchyApi.Clip klip) {
        if (klip == null) {
            return false;
        }
        String katman = klip.layer();
        return katman != null && !"idle".equalsIgnoreCase(katman) && !"locomotion".equalsIgnoreCase(katman);
    }

    private static void kaydet() {
        kayitDenedi = true;
        try {
            String id = DogalAfetler.MOD_ID;
            PunchyApi.condition(id, "bunalma",
                    () -> IstemciSicaklik.acik() && IstemciSicaklik.sicaklikDurumu() == SicaklikDurumu.BUNALMA);
            PunchyApi.condition(id, "yanma",
                    () -> IstemciSicaklik.acik() && IstemciSicaklik.sicaklikDurumu() == SicaklikDurumu.YANMA);
            PunchyApi.condition(id, "usume",
                    () -> IstemciSicaklik.acik() && IstemciSicaklik.sicaklikDurumu() == SicaklikDurumu.USUME);
            PunchyApi.condition(id, "donma",
                    () -> IstemciSicaklik.acik() && IstemciSicaklik.sicaklikDurumu() == SicaklikDurumu.DONMA);
            PunchyApi.condition(id, "islak",
                    () -> IstemciSicaklik.acik() && IstemciSicaklik.islaklik(0f) >= 0.25f);

            PunchyApi.armStates(id, (oyuncu, solKol, durumlar) -> {
                if (!IstemciSicaklik.acik()) {
                    return;
                }
                SicaklikDurumu sd = IstemciSicaklik.sicaklikDurumu();
                float yanik = PunchySicaklikMantigi.yanikYogunlugu(sd);
                float kiragi = PunchySicaklikMantigi.kiragiYogunlugu(sd);
                if (yanik > 0f) {
                    durumlar.burn(yanik);
                }
                if (kiragi > 0f) {
                    durumlar.frost(kiragi);
                }
            });

            PunchyApi.armPose(id, "sicaklik_mikro", poz -> {
                if (!IstemciSicaklik.acik()) {
                    return;
                }
                SicaklikDurumu sd = IstemciSicaklik.sicaklikDurumu();
                if (sd == SicaklikDurumu.NORMAL) {
                    return;
                }
                double saniye = (IstemciSicaklik.tick() + poz.partialTick()) / 20.0;
                boolean sol = poz.leftArm();
                float egim = PunchySicaklikMantigi.kolEgimDerece(sd, saniye, sol);
                float yalpa = PunchySicaklikMantigi.kolYalpaDerece(sd, saniye, sol);
                if (egim != 0f || yalpa != 0f) {
                    poz.turnView(egim, 0f, yalpa);
                }
            });
            apiHazir = true;
        } catch (LinkageError | RuntimeException hata) {
            apiHazir = false;
        }
    }
}
