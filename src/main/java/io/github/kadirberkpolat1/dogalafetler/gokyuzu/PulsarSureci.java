package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DonemAlani;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.core.PulsarYolu;
import io.github.kadirberkpolat1.dogalafetler.core.olay.DonemDegistiEvent;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GunesZinciri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarZinciri;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Pulsar zinciri dönem süreci (gökyüzü-2, pulsar-2; çekirdek §3a): PULSAR ({@code pulsarGun} oyun günü, ilk
 * {@code pulsarIsinmaGun} boyunca evre ilerlemesi 0 → 1 ısınma; dünya etkileri {@link PulsarEtkileri}'nde) → KARADELİK_YUTMA
 * ({@value PulsarZinciri#YUTMA_SN} sn ölçekli afet saatiyle, 55. sn'de bir kez sarsıntı + ses) → pulsar yolunda GÜNEŞSİZ
 * (buzul kıyamet, {@code pulsarSonrasiGunessizGun} oyun günü + ilk şafak; donma her zamanki gibi) → KIZIL CÜCE doğuşu
 * → NORMAL: eski güneş döner, donma doğal çözülür; güneş patlaması bundan sonra yalnız {@code tekrarlanabilir} açıkken
 * yeniden seçilir ({@link PulsarYolu#zincirTukendi}). Gün süreleri K-C29 oyun tick'iyle, başlangıçları {@link PulsarYolu}'nda
 * kalıcıdır (yeniden başlatmada kaldığı yerden sürer). K-C46: {@link #durdur} durma anını (oyun tick'i ve ölçekli saat)
 * yazar, {@link #devamEt} farkıyla hesaplanan süreleri (15 gün, 10 gün, yutmanın saniyesi) durduğu süre kadar ileri
 * çeker; ısınma ve doğuş {@code zamanAdimi()} ile birikir. Durmuşken evre geçerse ({@code asama ileri}) yeni evrenin süresi
 * o andan sayılır. Güneşsiz dönem ve doğuş pulsar yolundayken {@link GunesZinciriSureci} devre dışıdır. Döngü kimliği
 * PULSAR'ın {@code evreBaslangicSaati} (K-C46), yangın/meteor kaynağı {@code PulsarEtkileri.kaynak}.
 */
public final class PulsarSureci implements DonemSureci {
    public static final String KIMLIK = PulsarYolu.ALT_SISTEM;
    public static final String AD_ANAHTARI = "donem_sureci.dogalafetler." + KIMLIK;
    static final String DONGU = PulsarYolu.DONGU;
    private static final float DONMA_PAYI = 1e-3f;

    private static PulsarSureci ornek;
    /** GameTest'in "55. sn'de bir kez" denetimi için: bu sunucu oturumunda çalan yutma patlaması sayısı. */
    private static int yutmaPatlamasiSayisi;

    /** Önceki tick'in üst dünya gün saati (uyku atlaması şafağı geçerse doğuş kaçmasın); kalıcı değil. */
    private long sonGunSaati = Long.MIN_VALUE;
    /** Pulsar günleri doldu, yutma gündüz penceresini bekliyor (log bir kez); kalıcı değil. */
    private static boolean yutmaBekleniyor;

    private PulsarSureci() {
    }

    static void kaydet() {
        ornek = new PulsarSureci();
        AfetYoneticisi.donemSureciKaydet(ornek);
        BasarimIpucuEvent.yayinciKaydet(AfetTuru.GUNES_PATLAMASI, Ipucu.YENI_SAFAK);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.ServerTickEvent.class, ornek::tick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, DonemDegistiEvent.class, PulsarSureci::donemDegisti);
    }

    public static PulsarSureci al() {
        return ornek;
    }

    static int yutmaPatlamasiSayisi() {
        return yutmaPatlamasiSayisi;
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
        return PulsarZinciri.aktif(donem.gunesEvresi(), donem.evreIlerlemesi(), PulsarYolu.donusYolu(donem));
    }

    /** Kimlik her zaman; {@code karadelik|gunessiz|kizilcuce} yalnız o aşamadayken. */
    @Override
    public boolean kimlikEsler(String ad) {
        if (KIMLIK.equals(ad)) {
            return true;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && takmaAdlar(server.overworld()).contains(ad);
    }

    @Override
    public List<String> takmaAdlar(ServerLevel ustDunya) {
        DunyaDonemi donem = DunyaDonemi.al(ustDunya);
        return PulsarZinciri.takmaAd(donem.gunesEvresi(), donem.evreIlerlemesi(), PulsarYolu.donusYolu(donem))
                .map(List::of).orElse(List.of());
    }

    /** PULSAR'da ısınma 1'in altındaysa ısınmayı tamamlar, yoksa zinciri bir evre ilerletir. */
    @Override
    public Optional<Component> asamaIleri(ServerLevel ustDunya) {
        DunyaDonemi donem = DunyaDonemi.al(ustDunya);
        PulsarZinciri.AsamaAdimi adim = PulsarZinciri.asamaAdimi(donem.gunesEvresi(), donem.evreIlerlemesi(),
                PulsarYolu.donusYolu(donem));
        switch (adim) {
            case YOK -> {
                return Optional.empty();
            }
            case ISINMA -> {
                donem.evreIlerlemesiAyarla(1f);
                return Optional.of(Component.translatable(GokyuzuKomutlari.ASAMA_PULSAR_TAMAM, ad()));
            }
            case ESKI_GUNES -> {
                eskiGunesDoner(ustDunya.getServer(), donem, "asama ileri");
                return Optional.of(Component.translatable(GokyuzuKomutlari.ASAMA_ESKI_GUNES, ad()));
            }
            default -> {
                evreGec(ustDunya, donem, adim.hedef(), "asama ileri");
                return Optional.of(Component.translatable(GokyuzuKomutlari.ASAMA_EVRE, ad(),
                        Component.translatable(adim.hedef().ceviriAnahtari())));
            }
        }
    }

    @Override
    public Optional<Component> durumSatiri(ServerLevel ustDunya) {
        DunyaDonemi donem = DunyaDonemi.al(ustDunya);
        return kalanMetni(ustDunya, donem).map(kalan -> Component.translatable(GokyuzuKomutlari.DURUM_PULSAR,
                Component.translatable(donem.gunesEvresi().ceviriAnahtari()), kalan));
    }

    /**
     * Etkiler durur (yangınlar yayılmadan yanıp biter); ulaşılmış evre ve süre kayıtları korunur, durma anı yazılır (kural
     * kaydı komuta çevrilince ilk an kalır).
     */
    @Override
    public void durdur(ServerLevel ustDunya, DurdurmaNedeni neden) {
        DunyaDonemi donem = DunyaDonemi.al(ustDunya);
        if (!PulsarYolu.durmaVar(donem)) {
            PulsarYolu.durmaAyarla(donem, oyunTick(ustDunya, donem), donem.saat());
        }
        PulsarEtkileri.bitir(ustDunya.getServer(), donem, "durduruldu (" + neden + ")");
    }

    /** K-C46: kural açıldı; süreler durduğu süre kadar ileri çekilir, zincir kaldığı yerden sürer. */
    @Override
    public void devamEt(ServerLevel ustDunya) {
        DunyaDonemi donem = DunyaDonemi.al(ustDunya);
        if (!PulsarYolu.durmaVar(donem)) {
            return;
        }
        long oyun = PulsarZinciri.duraklama(PulsarYolu.durmaOyunTick(donem), oyunTick(ustDunya, donem));
        long olcekli = PulsarZinciri.duraklama(PulsarYolu.durmaSaati(donem), donem.saat());
        if (donem.gunesEvresi() == GunesEvresi.KARADELIK_YUTMA) {
            donem.evreBaslangiciniKaydir(olcekli);
        }
        PulsarYolu.durmaBitti(donem, oyun);
        DogalAfetler.LOGGER.info("Pulsar süreci kaldığı yerden sürüyor: evre {}, {} oyun tick'i ({} ölçekli) durmuştu",
                donem.gunesEvresi().kimlik(), oyun, olcekli);
    }

    /** K-C29 oyun tick'i (üst dünyanın oyun zamanı + uyku birikimi + gün ofseti). */
    static long oyunTick(ServerLevel ust, DunyaDonemi donem) {
        return PulsarZinciri.oyunTick(ust.getGameTime(), donem.uykuBirikimi(), donem.gunOfseti());
    }

    /** Durmuşsa durma anı, yoksa şimdiki K-C29 oyun tick'i: duran sürenin kalanı azalmaz. */
    private static long etkinOyunTick(ServerLevel ust, DunyaDonemi donem) {
        return PulsarYolu.durmaVar(donem) ? PulsarYolu.durmaOyunTick(donem) : oyunTick(ust, donem);
    }

    /**
     * Zincirin bu aşamadaki kalan süresi ({@code durum} ve {@code kizilcuce bilgi}); pulsar zinciri dışında boş. Durmuş
     * sürecin kalanı durma anındaki değerdir.
     */
    static Optional<Component> kalanMetni(ServerLevel ust, DunyaDonemi donem) {
        long simdi = etkinOyunTick(ust, donem);
        long saat = PulsarYolu.durmaVar(donem) ? PulsarYolu.durmaSaati(donem) : donem.saat();
        float p = donem.evreIlerlemesi();
        boolean yol = PulsarYolu.donusYolu(donem);
        return switch (donem.gunesEvresi()) {
            case PULSAR -> {
                long bas = PulsarYolu.baslangicVar(donem) ? PulsarYolu.baslangic(donem) : simdi;
                yield Optional.of(Component.translatable(GokyuzuKomutlari.KALAN_PULSAR,
                        GokyuzuKomutlari.yuzde(p * 100f),
                        GokyuzuKomutlari.gun(PulsarZinciri.kalanTick(bas, simdi, GokyuzuAyarlari.pulsarGun()))));
            }
            case KARADELIK_YUTMA -> Optional.of(Component.translatable(GokyuzuKomutlari.KALAN_KARADELIK,
                    GokyuzuKomutlari.yuzde(PulsarZinciri.yutmaKalanSaniye(
                            PulsarZinciri.yutmaSaniyesi(donem.evreBaslangicSaati(), saat)))));
            case GUNESSIZ -> {
                if (!yol) {
                    yield Optional.empty();
                }
                long bas = PulsarYolu.gunessizBaslangicVar(donem) ? PulsarYolu.gunessizBaslangic(donem) : simdi;
                int gun = GokyuzuAyarlari.pulsarSonrasiGunessizGun();
                if (PulsarZinciri.doldu(bas, simdi, gun)) {
                    yield Optional.of(Component.translatable(GokyuzuKomutlari.KALAN_BUZUL_SAFAK,
                            GunesZinciri.safagaKalan(ust.getDayTime())));
                }
                boolean dongu = ust.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
                yield Optional.of(Component.translatable(dongu ? GokyuzuKomutlari.KALAN_BUZUL
                        : GokyuzuKomutlari.KALAN_BUZUL_DONGUSUZ, GokyuzuKomutlari.gun(PulsarZinciri.kalanTick(bas, simdi, gun))));
            }
            case KIZIL_CUCE -> yol && p < 1f ? Optional.of(Component.translatable(GokyuzuKomutlari.KALAN_DONUS_DOGUS,
                    GokyuzuKomutlari.yuzde(p * 100f),
                    GokyuzuKomutlari.gun((1f - p) * GokyuzuAyarlari.zincir().dogusSuresiTick())))
                    : Optional.empty();
            case NORMAL -> PulsarYolu.gunesDondu(donem)
                    ? Optional.of(Component.translatable(GokyuzuKomutlari.KALAN_GUNES_DONDU,
                            GokyuzuKomutlari.yuzde(donem.donmaIlerlemesi())))
                    : Optional.empty();
            default -> Optional.empty();
        };
    }

    /**
     * Zincirin bir sonraki evresi. Pulsar yolu bayrağı evreden önce yazılır: dönem olayını dinleyenler (donma) ve istemci
     * paketi ikisini birlikte görür. Durmuş süreçte ({@code asama ileri}) durma anı şimdiye çekilir: yeni evrenin süresi
     * kural açılınca başlar.
     */
    private static void evreGec(ServerLevel ust, DunyaDonemi donem, GunesEvresi yeni, String neden) {
        switch (yeni) {
            case KARADELIK_YUTMA -> PulsarYolu.yutmaPatlamasiAyarla(donem, false);
            case GUNESSIZ -> {
                PulsarYolu.gunessizBaslangicAyarla(donem, oyunTick(ust, donem));
                PulsarYolu.donusYoluAyarla(donem, true);
            }
            default -> {
            }
        }
        GunesEvresi eski = donem.gunesEvresi();
        donem.gunesEvresiAyarla(yeni);
        if (PulsarYolu.durmaVar(donem)) {
            PulsarYolu.durmaAyarla(donem, oyunTick(ust, donem), donem.saat());
        }
        DogalAfetler.LOGGER.info("Pulsar zinciri: {} -> {} ({}), saat={}, oyun tick={}, donma={}", eski.kimlik(),
                yeni.kimlik(), neden, donem.saat(), oyunTick(ust, donem), donem.donmaIlerlemesi());
    }

    /**
     * Kızıl cüce doğuşu bitti: eski güneş döner, donma doğal çözülür, üst dünyadaki oyunculara "Yeni Şafak" ipucu; zincir
     * tükendi (madde 4: güneş patlaması yalnız {@code tekrarlanabilir} açıkken yeniden seçilir).
     */
    private static void eskiGunesDoner(MinecraftServer server, DunyaDonemi donem, String neden) {
        if (PulsarYolu.durmaVar(donem)) {
            PulsarYolu.durmaBitti(donem, 0L);
        }
        PulsarYolu.zincirTukendiAyarla(donem, true);
        PulsarYolu.donusYoluAyarla(donem, false);
        PulsarYolu.gunesDonduAyarla(donem, true);
        donem.gunesEvresiAyarla(GunesEvresi.NORMAL);
        DogalAfetler.LOGGER.info("Güneş zinciri: kizil_cuce_dogus -> normal (eski güneş döndü)");
        DogalAfetler.LOGGER.info("Pulsar zinciri tamam ({}), saat={}, donma={}", neden, donem.saat(), donem.donmaIlerlemesi());
        for (ServerPlayer oyuncu : server.overworld().players()) {
            MinecraftForge.EVENT_BUS.post(new BasarimIpucuEvent(oyuncu, Ipucu.YENI_SAFAK));
        }
    }

    /**
     * Evre PULSAR'dan çıkınca ({@code asama ileri}, süre doldu, başka yıldız, {@code sifirla}) dünya etkileri biter ve
     * kavurmanın eşdeğer gün sayacı sıfırlanır.
     */
    private static void donemDegisti(DonemDegistiEvent olay) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || DunyaDonemi.al(server) != olay.donem()
                || olay.alan() != DonemAlani.GUNES_EVRESI && olay.alan() != DonemAlani.SIFIRLAMA
                || olay.donem().gunesEvresi() == GunesEvresi.PULSAR) {
            return;
        }
        PulsarEtkileri.bitir(server, olay.donem(), "evre " + olay.donem().gunesEvresi().kimlik());
        KavurmaKuyrugu.esdegerGunuSifirla();
    }

    private void tick(TickEvent.ServerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = olay.getServer();
        ServerLevel ust = server.overworld();
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        DunyaDonemi donem = yonetici.donem();
        bayraklariDenetle(donem);
        if (!aktifMi(ust)) {
            sonGunSaati = Long.MIN_VALUE;
            return;
        }
        long gunSaati = ust.getDayTime();
        long oncekiGunSaati = sonGunSaati;
        sonGunSaati = gunSaati;
        long simdi = oyunTick(ust, donem);
        GunesEvresi evre = donem.gunesEvresi();
        if (evre == GunesEvresi.PULSAR) {
            donguyuDenetle(yonetici, donem, simdi, etkinOyunTick(ust, donem));
        } else if (evre == GunesEvresi.GUNESSIZ && !PulsarYolu.gunessizBaslangicVar(donem)) {
            PulsarYolu.gunessizBaslangicAyarla(donem, etkinOyunTick(ust, donem));
        }
        int adim = yonetici.zamanAdimi();
        if (yonetici.surecDurdu(this) || adim <= 0) {
            return;
        }
        switch (evre) {
            case PULSAR -> pulsarTick(server, ust, donem, adim, simdi);
            case KARADELIK_YUTMA -> yutmaTick(server, ust, donem);
            case GUNESSIZ -> buzulTick(ust, donem, simdi, oncekiGunSaati, gunSaati);
            case KIZIL_CUCE -> dogusTick(server, donem, adim);
            default -> {
            }
        }
    }

    /**
     * Süreç aktif olmasa da her tick: pulsar yolu evresi dışında (ör. geliştirme komutuyla evre değişti) dönüş bayrağı
     * silinir; eski güneş döndükten sonra donma 0'a çözülünce (ya da evre NORMAL'den çıkınca) dönüş bayrağı ve çözülme
     * dönemi kapanır.
     */
    private static void bayraklariDenetle(DunyaDonemi donem) {
        GunesEvresi evre = donem.gunesEvresi();
        if (PulsarYolu.donusYolu(donem) && evre != GunesEvresi.GUNESSIZ && evre != GunesEvresi.KIZIL_CUCE) {
            PulsarYolu.donusYoluAyarla(donem, false);
        }
        if (PulsarYolu.gunesDondu(donem)
                && (evre != GunesEvresi.NORMAL || donem.donmaIlerlemesi() <= DONMA_PAYI)) {
            PulsarYolu.gunesDonduAyarla(donem, false);
            if (evre == GunesEvresi.NORMAL) {
                donem.cozulmeDonemiAyarla(false);
            }
            DogalAfetler.LOGGER.info("Pulsar zinciri: eski güneşin çözülmesi bitti (evre {}, donma {})", evre.kimlik(),
                    donem.donmaIlerlemesi());
        }
    }

    /**
     * Yeni pulsar döngüsü (K-C46: yalnız PULSAR başlangıcında; durdurma kaydı ve durma anı silinir) ya da eski kayıtta
     * eksik başlangıç ({@code etkin}: durmuşsa durma anı).
     */
    private void donguyuDenetle(AfetYoneticisi yonetici, DunyaDonemi donem, long simdi, long etkin) {
        if (!PulsarYolu.donguVar(donem) || PulsarYolu.dongu(donem) != donem.evreBaslangicSaati()) {
            PulsarYolu.yeniDongu(donem, donem.evreBaslangicSaati(), simdi);
            yonetici.donemSureciBasladi(this);
            DogalAfetler.LOGGER.info("Pulsar süreci yeni döngüde: pulsar saat {}, oyun tick {}, süre {} gün",
                    donem.evreBaslangicSaati(), simdi, GokyuzuAyarlari.pulsarGun());
        } else if (!PulsarYolu.baslangicVar(donem)) {
            PulsarYolu.baslangicAyarla(donem, etkin);
            DogalAfetler.LOGGER.info("Pulsar süreci: eski kayıtta başlangıç yoktu, oyun tick {} yazıldı", etkin);
        }
    }

    private static void pulsarTick(MinecraftServer server, ServerLevel ust, DunyaDonemi donem, int adim, long simdi) {
        int gun = GokyuzuAyarlari.pulsarGun();
        long bas = PulsarYolu.baslangic(donem);
        if (PulsarZinciri.yutmaBaslar(bas, simdi, gun, ust.getDayTime(),
                ust.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT))) {
            yutmaBekleniyor = false;
            evreGec(ust, donem, GunesEvresi.KARADELIK_YUTMA, "pulsar " + gun + " günü doldu");
            return;
        }
        if (PulsarZinciri.doldu(bas, simdi, gun) && !yutmaBekleniyor) {
            yutmaBekleniyor = true;
            DogalAfetler.LOGGER.info("Pulsar {} günü doldu; kara delik yutması gündüz penceresini bekliyor (dayTime={})", gun,
                    ust.getDayTime());
        }
        float ilerleme = GunesZinciri.ilerlet(donem.evreIlerlemesi(), adim, GokyuzuAyarlari.pulsarIsinmaTick());
        donem.evreIlerlemesiAyarla(ilerleme);
        PulsarEtkileri.tick(server, ust, donem, PulsarEgrileri.yogunluk(ilerleme), adim);
    }

    private static void yutmaTick(MinecraftServer server, ServerLevel ust, DunyaDonemi donem) {
        float saniye = PulsarZinciri.yutmaSaniyesi(donem.evreBaslangicSaati(), donem.saat());
        donem.evreIlerlemesiAyarla(PulsarZinciri.yutmaIlerlemesi(saniye));
        if (PulsarZinciri.yutmaParlamasiGeldi(saniye) && !PulsarYolu.yutmaPatlamasiOldu(donem)) {
            PulsarYolu.yutmaPatlamasiAyarla(donem, true);
            yutmaPatlamasiSayisi++;
            PulsarSesleri.yutmaPatlamasi(server);
            DogalAfetler.LOGGER.info("Kara delik yutması: parlama ve sarsıntı ({} sn)", saniye);
        }
        if (PulsarZinciri.yutmaBitti(saniye)) {
            evreGec(ust, donem, GunesEvresi.GUNESSIZ, "kara delik yuttu");
        }
    }

    /** Buzul kıyamet: süre dolunca kızıl cüce ilk şafakta (gün döngüsü kapalıysa hemen) doğar. */
    private static void buzulTick(ServerLevel ust, DunyaDonemi donem, long simdi, long oncekiGunSaati, long gunSaati) {
        PulsarEtkileri.buzul(ust.getServer());
        long bas = PulsarYolu.gunessizBaslangic(donem);
        int gun = GokyuzuAyarlari.pulsarSonrasiGunessizGun();
        donem.evreIlerlemesiAyarla(PulsarZinciri.ilerleme(bas, simdi, gun));
        GunesZinciri.DogusNedeni neden = GunesZinciri.dogusNedeni(PulsarZinciri.doldu(bas, simdi, gun), oncekiGunSaati,
                gunSaati, ust.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT));
        if (neden.dogar()) {
            evreGec(ust, donem, GunesEvresi.KIZIL_CUCE, "buzul kıyamet " + gun + " gün, "
                    + neden.name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private static void dogusTick(MinecraftServer server, DunyaDonemi donem, int adim) {
        float ilerleme = GunesZinciri.ilerlet(donem.evreIlerlemesi(), adim, GokyuzuAyarlari.zincir().dogusSuresiTick());
        if (ilerleme >= 1f) {
            eskiGunesDoner(server, donem, "doğuş tamam");
            return;
        }
        donem.evreIlerlemesiAyarla(ilerleme);
    }
}
