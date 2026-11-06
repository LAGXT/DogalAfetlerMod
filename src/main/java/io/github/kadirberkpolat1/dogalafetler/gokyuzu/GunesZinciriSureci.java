package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GunesZinciri;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * Güneş zinciri dönem süreci (bölüm D, madde 9, çekirdek sözleşmesi §3a): güneş patlaması afeti doğal bitip evreyi
 * SÜPERNOVA yaptıktan sonra süpernovayı config süresinde söndürür, GÜNEŞSİZ dönemde kalıntı nebulasını genişletir,
 * donma %100 olduktan {@code kizilCuceGecikmeGun} sonra ilk şafakta KIZIL CÜCE'yi doğurur ve doğuşu tamamlar. Evre
 * ilerlemesi her evrede bu sürecindir (BÜYÜYEN'inki afetin). Durdurma kaydı çekirdektedir; döngü kimliği (SÜPERNOVA'nın
 * {@code evreBaslangicSaati}) ve kızıl cüce kapı sayacı {@code ekVeri("gunes_zinciri")}'ndedir. İlerleme
 * {@code zamanAdimi()} ile biriktiği için {@link #devamEt} boştur.
 */
public final class GunesZinciriSureci implements DonemSureci {
    public static final String KIMLIK = "gunes_zinciri";
    public static final String AD_ANAHTARI = "donem_sureci.dogalafetler." + KIMLIK;
    static final String DONGU = "dongu";
    static final String KAPI = "kapi";
    private static final int KAYIT_ARALIGI_TICK = 20;

    private static GunesZinciriSureci ornek;

    private GunesZinciriSureci() {
    }

    static void kaydet() {
        ornek = new GunesZinciriSureci();
        AfetYoneticisi.donemSureciKaydet(ornek);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.ServerTickEvent.class, ornek::tick);
    }

    public static GunesZinciriSureci al() {
        return ornek;
    }

    @Override
    public String kimlik() {
        return KIMLIK;
    }

    @Override
    public Component ad() {
        return Component.translatable(AD_ANAHTARI);
    }

    @Override
    public Optional<AfetTuru> afetTuru() {
        return Optional.of(AfetTuru.GUNES_PATLAMASI);
    }

    @Override
    public boolean aktifMi(ServerLevel ustDunya) {
        DunyaDonemi donem = DunyaDonemi.al(ustDunya);
        return GunesZinciri.aktif(donem.gunesEvresi(), donem.evreIlerlemesi());
    }

    /** SÜPERNOVA → GÜNEŞSİZ → KIZIL CÜCE (donma ve şafak beklenmez) → doğuş tamam. */
    @Override
    public Optional<Component> asamaIleri(ServerLevel ustDunya) {
        DunyaDonemi donem = DunyaDonemi.al(ustDunya);
        GunesEvresi evre = donem.gunesEvresi();
        if (evre == GunesEvresi.KIZIL_CUCE && donem.evreIlerlemesi() < 1f) {
            donem.evreIlerlemesiAyarla(1f);
            return Optional.of(Component.translatable(GokyuzuKomutlari.ASAMA_DOGUS_TAMAM, ad()));
        }
        return GunesZinciri.sonraki(evre).map(yeni -> {
            evreDegistir(donem, yeni, "asama ileri");
            return Component.translatable(GokyuzuKomutlari.ASAMA_EVRE, ad(), Component.translatable(yeni.ceviriAnahtari()));
        });
    }

    /** {@code /dogalafet kizilcuce baslat} ve zincirin kendi doğuşu: kızıl cüce şimdi doğar (herkese aynı anda). */
    static void dogusBaslat(DunyaDonemi donem, String neden) {
        evreDegistir(donem, GunesEvresi.KIZIL_CUCE, neden);
    }

    private static void evreDegistir(DunyaDonemi donem, GunesEvresi yeni, String neden) {
        GunesEvresi eski = donem.gunesEvresi();
        donem.gunesEvresiAyarla(yeni);
        DogalAfetler.LOGGER.info("Güneş zinciri: {} -> {} ({}), saat={}, donma={}", eski.kimlik(), yeni.kimlik(), neden,
                donem.saat(), donem.donmaIlerlemesi());
    }

    private void tick(TickEvent.ServerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = olay.getServer();
        ServerLevel ust = server.overworld();
        if (!aktifMi(ust)) {
            return;
        }
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        CompoundTag veri = donem.ekVeri(KIMLIK);
        if (donem.gunesEvresi() == GunesEvresi.SUPERNOVA
                && (!veri.contains(DONGU) || veri.getLong(DONGU) != donem.evreBaslangicSaati())) {
            veri.putLong(DONGU, donem.evreBaslangicSaati());
            veri.putLong(KAPI, 0L);
            donem.ekVeriDegisti(KIMLIK);
            yonetici.donemSureciBasladi(this);
            DogalAfetler.LOGGER.info("Güneş zinciri yeni döngüde: süpernova saat {}", donem.evreBaslangicSaati());
        }
        int adim = yonetici.zamanAdimi();
        if (yonetici.surecDurdu(this) || adim <= 0) {
            return;
        }
        GunesZinciri.Ayarlar ayar = GokyuzuAyarlari.zincir();
        GunesEvresi evre = donem.gunesEvresi();
        float ilerleme = GunesZinciri.ilerlet(donem.evreIlerlemesi(), adim, GunesZinciri.evreSuresi(evre, ayar));
        if (evre == GunesEvresi.SUPERNOVA && ilerleme >= 1f) {
            evreDegistir(donem, GunesEvresi.GUNESSIZ, "süpernova söndü");
            return;
        }
        donem.evreIlerlemesiAyarla(ilerleme);
        if (evre == GunesEvresi.GUNESSIZ) {
            kapiyiIsle(ust, donem, veri, adim, ayar);
        }
    }

    private static void kapiyiIsle(ServerLevel ust, DunyaDonemi donem, CompoundTag veri, int adim,
            GunesZinciri.Ayarlar ayar) {
        long once = veri.getLong(KAPI);
        long gecen = GunesZinciri.kapiIlerlet(once, donem.donmaIlerlemesi(), adim);
        if (gecen != once) {
            veri.putLong(KAPI, gecen);
            boolean yeniAcildi = GunesZinciri.kapiAcik(gecen, ayar) && !GunesZinciri.kapiAcik(once, ayar);
            if (yeniAcildi || gecen == 0 || ust.getGameTime() % KAYIT_ARALIGI_TICK == 0) {
                donem.ekVeriDegisti(KIMLIK);
            }
            if (yeniAcildi) {
                DogalAfetler.LOGGER.info("Kızıl cüce kapısı açıldı (donma %100 + {} gün); ilk şafak bekleniyor",
                        ayar.kizilCuceGecikmeGun());
            }
        }
        if (GunesZinciri.kapiAcik(gecen, ayar)
                && GunesZinciri.safakMi(ust.getDayTime(), ust.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT))) {
            dogusBaslat(donem, "ilk şafak");
        }
    }

    /** Doğuş kapısının donma %100'de biriken süresi (ölçekli tick); komut ve test için. */
    public static long kapiSayaci(DunyaDonemi donem) {
        return donem.ekVeri(KIMLIK).getLong(KAPI);
    }
}
