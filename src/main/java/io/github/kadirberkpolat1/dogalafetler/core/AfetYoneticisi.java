package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.CekirdekAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.AfetYogunlugu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.AfetZamanlayicisi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.EszamanliAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.GelisimOlcutu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.HedefSecici;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.OnUyariPenceresi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.OyunGunu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SecimTuru;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SeviyeDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SiddetAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SiddetSecici;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonrakiAfetTahmini;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.TekSeferlikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.TekSeferlikKurallar;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.ZamanlayiciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.ZamanOlcegi;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetAsamaDegistiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBasladiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.AfetBittiEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.SirenDurdurEvent;
import io.github.kadirberkpolat1.dogalafetler.core.olay.SirenIstegiEvent;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Afet yöneticisi (madde 3, 4, 10, 11): sunucu tick'inde zamanlayıcıyı çalıştırır, afet seçer, yaşam döngüsünü ilerletir,
 * siren isteklerini ve afet durum paketlerini etkilenebilecek oyunculara gönderir, "sağ çıkan" takibini yapar ve afet
 * bitince {@link AfetBittiEvent} yayınlar. Doğal seçim boş afet yuvası varken çalışır: oyun gününe göre en çok 1–3 doğal
 * afet, farklı türden, etki alanları çakışmadan ({@link EszamanliAfetler}, K-E67); komut aynı anda birden çok afet
 * başlatabilir, aynı türden yalnız bir tane (AS-8). Afet yuvası dışında süren dönem süreçleri
 * {@link #donemSureciKaydet} ile katılır; {@code stop} ve gamerule onları durdurur, gamerule ile duran süreç kural açılınca
 * sürer (madde 11, K-C41). Karar mantığı {@code core.mantik}'tedir; bu sınıf Minecraft bağlantısıdır. Afet kodunun
 * (alt sınıf) her çağrısı korunur: istisna ERROR log'a düşer, afet {@link BitisNedeni#HATA} ile yumuşak durdurulur. Sunucu
 * başına bir örnek ({@link #al}); yalnız sunucu ana thread'inde kullanılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class AfetYoneticisi {
    /** Elle ya da otomatik başlatmanın sonucu. */
    public enum BaslatmaSonucu {
        BASLADI,
        ZATEN_AKTIF,
        KAYITLI_DEGIL,
        YER_UYGUN_DEGIL,
        /** Doğal afetin etki alanı süren bir afetinkiyle çakışıyor (K-E67; komutla başlatmada olmaz). */
        CAKISIYOR,
        HEDEF_YOK,
        HATA
    }

    private static final List<DonemSureci> DONEM_SURECLERI = new CopyOnWriteArrayList<>();
    private static AfetYoneticisi ornek;

    private final MinecraftServer server;
    private final ZamanOlcegi zamanOlcegi = new ZamanOlcegi();
    private final SplittableRandom rastgele = new SplittableRandom();
    private final boolean otomatikSecim;
    /** Afet başına siren ve durum paketi alıcıları (kimlik eşitliğiyle; afet bitince silinir). */
    private final Map<Afet, Set<UUID>> sirenAlicilari = new IdentityHashMap<>();
    private final Map<Afet, Set<UUID>> paketAlicilari = new IdentityHashMap<>();
    private boolean geriYuklendi;
    private int sonAdim;
    private long sonKontrolSaati = Long.MIN_VALUE;
    private long tickSayaci;
    /** Bu sunucu oturumunda son görülen ham {@code afetSikligi} (bildirim yalnız değişimde; ilk okuma sessiz). */
    private int sonSiklik = Integer.MIN_VALUE;
    private long sonDonemPaketi = Long.MIN_VALUE / 2;

    private AfetYoneticisi(MinecraftServer server) {
        this.server = server;
        // GameTest sunucusunda rastgele afet testlerin dünyasını bozar; testler seçimi doğrudan çağırır.
        this.otomatikSecim = !(server instanceof GameTestServer);
    }

    /** Sunucunun yöneticisi. */
    public static AfetYoneticisi al(MinecraftServer server) {
        AfetYoneticisi yonetici = ornek;
        if (yonetici == null || yonetici.server != server) {
            yonetici = new AfetYoneticisi(server);
            ornek = yonetici;
        }
        return yonetici;
    }

    // ---------------------------------------------------------------- dönem süreçleri kaydı

    /** Afet yuvası dışında süren bir dönem sürecini kaydeder; yalnız mod kurucusunda. Kimlik benzersiz olmalı. */
    public static void donemSureciKaydet(DonemSureci surec) {
        Objects.requireNonNull(surec, "surec");
        for (DonemSureci kayitli : DONEM_SURECLERI) {
            if (kayitli.kimlik().equals(surec.kimlik())) {
                throw new IllegalStateException("Aynı kimlikli iki dönem süreci: " + surec.kimlik());
            }
        }
        DONEM_SURECLERI.add(surec);
    }

    /** Yalnız GameTest'ler için: süreci geçici olarak kaydeder; {@code close()} kaydı kaldırır. */
    public static AfetKaydi.GeciciKayit geciciDonemSureciKaydet(DonemSureci surec) {
        donemSureciKaydet(surec);
        return () -> DONEM_SURECLERI.remove(surec);
    }

    public static List<DonemSureci> donemSurecleri() {
        return List.copyOf(DONEM_SURECLERI);
    }

    // ---------------------------------------------------------------- Forge olayları

    @SubscribeEvent
    public static void sunucuBasliyor(ServerStartingEvent olay) {
        ornek = new AfetYoneticisi(olay.getServer());
        TpsOlcer.sifirla();
    }

    @SubscribeEvent
    public static void sunucuBasladi(ServerStartedEvent olay) {
        al(olay.getServer()).geriYukle();
    }

    @SubscribeEvent
    public static void sunucuDuruyor(ServerStoppingEvent olay) {
        AfetYoneticisi yonetici = ornek;
        if (yonetici != null && yonetici.server == olay.getServer()) {
            yonetici.kapanis();
        }
    }

    @SubscribeEvent
    public static void sunucuDurdu(ServerStoppedEvent olay) {
        if (ornek != null && ornek.server == olay.getServer()) {
            ornek = null;
        }
    }

    @SubscribeEvent
    public static void sunucuTick(TickEvent.ServerTickEvent olay) {
        if (olay.phase == TickEvent.Phase.END) {
            al(olay.getServer()).tick();
        }
    }

    @SubscribeEvent
    public static void oyuncuGirdi(PlayerEvent.PlayerLoggedInEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            al(oyuncu.server).donemGonder(oyuncu);
        }
    }

    /** Çıkan oyuncu siren ve paket alıcılarından düşer: geri girince 1 Hz denetimde kalan süreyle siren alır (K0-27). */
    @SubscribeEvent
    public static void oyuncuCikti(PlayerEvent.PlayerLoggedOutEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            AfetYoneticisi yonetici = al(oyuncu.server);
            yonetici.sirenAlicilari.values().forEach(alicilar -> alicilar.remove(oyuncu.getUUID()));
            yonetici.paketAlicilari.values().forEach(alicilar -> alicilar.remove(oyuncu.getUUID()));
        }
    }

    @SubscribeEvent
    public static void boyutDegisti(PlayerEvent.PlayerChangedDimensionEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            al(oyuncu.server).donemGonder(oyuncu);
        }
    }

    /**
     * K-C42: uyku geceyi atlar; üst dünyada atlanan gün saati oyun gününe eklenir (en son öncelikte: diğer modlar uyanış
     * saatini değiştirdikten sonra).
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void uykuBitti(SleepFinishedTimeEvent olay) {
        if (olay.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            long atlanan = OyunGunu.uykuAtlamasi(olay.getNewTime(), level.getDayTime());
            if (atlanan > 0) {
                DunyaDonemi donem = DunyaDonemi.al(level);
                donem.uykuEkle(atlanan);
                DogalAfetler.LOGGER.info("Uyku {} tick atladı; uyku birikimi {}, oyun günü {}", atlanan, donem.uykuBirikimi(),
                        donem.oyunGunu(level.getGameTime()));
            }
        }
    }

    @SubscribeEvent
    public static void canliOldu(LivingDeathEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            al(oyuncu.server).olumuKaydet(oyuncu);
        }
    }

    // ---------------------------------------------------------------- okuma API'si

    public MinecraftServer server() {
        return server;
    }

    public DunyaDonemi donem() {
        return DunyaDonemi.al(server);
    }

    /** Süren afetler (ON_UYARI dahil), başlama sırasıyla; radar ve sismograf aşamayı {@link Afet#normalOnUyariBasladi()} ile sınar. */
    public List<Afet> aktifAfetler() {
        return donem().aktifAfetler();
    }

    /** Birincil afet: sürenlerin en erken başlayanı (tek afet varken o); türe bağlı okuyucu {@link #aktifAfet(AfetTuru)} kullanır. */
    public Optional<Afet> aktifAfet() {
        return donem().aktifAfet();
    }

    /** Bu türden süren afet. */
    public Optional<Afet> aktifAfet(AfetTuru tur) {
        return donem().aktifAfet(tur);
    }

    /** Afetin güncel etki alanı; afet kodu hata verirse boş (afet yumuşak durdurulur). */
    public Optional<EtkiAlani> etkiAlani(Afet afet) {
        return Optional.ofNullable(alanOku(afet));
    }

    /**
     * Afetin geçerli aşamasının kalan ölçekli tick'i, korumalı (AKTIF'te alt sınıfın {@code aktifKalanTick}'i).
     * Bilinmiyorsa ya da afet kodu hata verirse -1; hata durumunda afet {@link BitisNedeni#HATA} ile yumuşak durur.
     */
    public long kalanOku(Afet afet) {
        try {
            return afet.asamaKalanTick();
        } catch (RuntimeException hata) {
            hataVerdi(afet, "aktifKalanTick", hata);
            return -1;
        }
    }

    /** Afet ve dönem zamanlayıcılarının ortak hız çarpanı. */
    public ZamanOlcegi zamanOlcegi() {
        return zamanOlcegi;
    }

    /** Bu tick'te ilerleyen ölçekli tick sayısı (dönem süreçleri bununla ilerler). */
    public int zamanAdimi() {
        return sonAdim;
    }

    /** Oyun günü (dünya yaşı): üst dünyanın (oyun zamanı + uyku birikimi) / 24000 + {@code /dogalafet gun} ofseti. */
    public long oyunGunu() {
        return donem().oyunGunu(server.overworld().getGameTime());
    }

    /** Çevrim içi oyuncuların gelişim puanı (config ölçütüne göre ortalama ya da en gelişmiş). */
    public double gelisimPuani() {
        List<ServerPlayer> oyuncular = server.getPlayerList().getPlayers();
        double[] puanlar = oyuncular.stream().filter(oyuncu -> !oyuncu.isSpectator())
                .mapToDouble(oyuncu -> GelisimOlcutu.puan(oyuncu.experienceLevel, oyuncu.getArmorValue())).toArray();
        return CekirdekAyarlari.al().gelisimOlcutu().topla(puanlar);
    }

    /**
     * Verilen oyun günündeki seviye olasılıkları ve 0 olanların nedeni: doğal seçimin kullandığı değerler (gelişim
     * kriteri, gamerule üst sınırı, art arda 4-5 kuralı, afet yoğunluğu dahil; K-E67).
     */
    public SeviyeDurumu seviyeDurumu(double gun) {
        SiddetAyarlari ayar = CekirdekAyarlari.al().siddet();
        return SeviyeDurumu.hesapla(ayar, gun, SiddetSecici.etkinGun(ayar, gun, gelisimPuani()),
                Kurallar.maksSiddet(server.getGameRules()), SiddetSecici.yuksek(donem().sonSeviye()), afetYuku(gun));
    }

    /** Afet yoğunluğu yükü ({@link AfetYogunlugu#yuk}): son gündeki doğal afetler ve {@code afetSikligi}. */
    public double afetYuku(double gun) {
        return AfetYogunlugu.yuk(donem().sonGunDogalAfetSayisi(), gun, Kurallar.siklikCarpani(server.getGameRules()));
    }

    /**
     * Oyun gününün takvim çarpanı (K-E67, {@link ZamanlayiciAyarlari#gunCarpani}): bölgesel afetlerin oranını {@code afetSikligi}
     * gibi çarpar, yeni başlayan beklemeleri böler.
     */
    public double gunCarpani() {
        return CekirdekAyarlari.al().zamanlayici().gunCarpani(oyunGunu());
    }

    /** Doğal afet için boş yuva yok mu ({@link EszamanliAfetler#yuvaDolu}; TPS kısılıyken ikinci afet ertelenir, K-C37). */
    public boolean dogalYuvaDolu() {
        return EszamanliAfetler.yuvaDolu(aktifTurler(), CekirdekAyarlari.al().zamanlayici().eszamanliSinir(oyunGunu()),
                TpsOlcer.kismaCarpani() < 1.0);
    }

    private List<AfetTuru> aktifTurler() {
        return aktifAfetler().stream().map(Afet::tur).toList();
    }

    /**
     * Afetten etkilenebilecek oyuncular (N11): bölgesel afette afetin boyutunda, etki alanı +
     * {@code cekirdek.hedef.sirenEkYaricap} içinde olanlar (küresel alanlı bölgesel afette, ör. asit yağmuru, o boyuttaki
     * herkes; AF2-08); sunucu geneli afette her boyuttaki bütün çevrim içi oyuncular. Siren ve afet durum paketi bunlara gider.
     */
    public List<ServerPlayer> etkilenebilecekOyuncular(Afet afet) {
        EtkiAlani alan = alanOku(afet);
        if (alan == null) {
            return List.of();
        }
        if (afet.tur().sunucuGeneli()) {
            return List.copyOf(server.getPlayerList().getPlayers());
        }
        ServerLevel level = server.getLevel(afet.baglam().boyut());
        if (level == null) {
            return List.of();
        }
        double ek = CekirdekAyarlari.al().sirenEkYaricap();
        List<ServerPlayer> sonuc = new ArrayList<>();
        for (ServerPlayer oyuncu : level.players()) {
            if (alan.iceriyor(oyuncu.getX(), oyuncu.getY(), oyuncu.getZ(), ek)) {
                sonuc.add(oyuncu);
            }
        }
        return sonuc;
    }

    /** Süren dönem süreçleri: tamamlanmamış ve durdurulmamış. */
    public List<DonemSureci> aktifDonemSurecleri() {
        ServerLevel ust = server.overworld();
        List<DonemSureci> sonuc = new ArrayList<>();
        for (DonemSureci surec : DONEM_SURECLERI) {
            if (!surecDurdu(surec) && aktifMi(surec, ust)) {
                sonuc.add(surec);
            }
        }
        return sonuc;
    }

    /** Tamamlanmamış ama durdurulmuş dönem süreçleri ve durdurma nedenleri (kayıt sırasıyla). */
    public Map<DonemSureci, DonemSureci.DurdurmaNedeni> durdurulmusDonemSurecleri() {
        ServerLevel ust = server.overworld();
        Map<DonemSureci, DonemSureci.DurdurmaNedeni> sonuc = new LinkedHashMap<>();
        for (DonemSureci surec : DONEM_SURECLERI) {
            Optional<DonemSureci.DurdurmaNedeni> neden = durdurmaNedeni(surec);
            if (neden.isPresent() && aktifMi(surec, ust)) {
                sonuc.put(surec, neden.get());
            }
        }
        return sonuc;
    }

    /** Sürecin çekirdekteki durdurma kaydı (kalıcı, K-C41); yoksa süreç durdurulmamıştır. */
    public Optional<DonemSureci.DurdurmaNedeni> durdurmaNedeni(DonemSureci surec) {
        return donem().surecDurdurmasi(surec.kimlik());
    }

    /** Süreç durduruldu mu; sahibi her ilerlemeden önce sorar, durmuşsa ilerlemez. */
    public boolean surecDurdu(DonemSureci surec) {
        return durdurmaNedeni(surec).isPresent();
    }

    /**
     * Sürecin sahibi yeni bir döngü başlattı (ör. ikinci güneş patlamasından sonraki süpernova): önceki döngünün durdurma
     * kaydı silinir (Y4). Kural kapalıysa çekirdek bir sonraki denetimde yeni döngüyü yine durdurur.
     */
    public void donemSureciBasladi(DonemSureci surec) {
        if (surecDurdu(surec)) {
            donem().surecDurdurmasiAyarla(surec.kimlik(), null);
            DogalAfetler.LOGGER.info("Dönem süreci yeni döngüde: {} (önceki durdurma kaydı silindi)", surec.kimlik());
        }
    }

    // ---------------------------------------------------------------- komut ve alt sistem API'si

    /**
     * {@code /dogalafet start}: bekleme ve gamerule kurallarını atlar; başka türden süren afetler engel değildir, aynı türden
     * süren afet varsa reddeder (AS-8). {@code seviye} boşsa
     * şiddet seçiciyle seçilir (seviyesiz afette yok sayılır); verilen seviye {@code afetMaksSiddet}'i aşabilir (komut çıktısı
     * bunu belirtir, K-E67). {@code hedef} boşsa uygun bir çevrim içi oyuncu, o da
     * yoksa {@code konumLevel}/{@code konum} kullanılır.
     */
    public BaslatmaSonucu elleBaslat(AfetTuru tur, Integer seviye, ServerPlayer hedef, ServerLevel konumLevel,
            BlockPos konum) {
        if (aktifAfet(tur).isPresent()) {
            return BaslatmaSonucu.ZATEN_AKTIF;
        }
        if (!AfetKaydi.kayitliMi(tur)) {
            return BaslatmaSonucu.KAYITLI_DEGIL;
        }
        int etkinSeviye = !tur.seviyeli() ? 0
                : seviye != null ? Math.max(AfetTuru.EN_DUSUK_SEVIYE, Math.min(AfetTuru.EN_YUKSEK_SEVIYE, seviye))
                        : seviyeSec(tur);
        if (tur.sunucuGeneli()) {
            return sunucuGeneliBaslat(tur, true);
        }
        if (hedef != null) {
            return oyuncuyaBaslat(tur, etkinSeviye, hedef, true);
        }
        List<ServerPlayer> adaylar = hedefAdaylari(CekirdekAyarlari.al());
        for (UUID kimlik : HedefSecici.sirala(adayListesi(adaylar), donem().sonHedef().orElse(null), rastgele)) {
            ServerPlayer oyuncu = server.getPlayerList().getPlayer(kimlik);
            BaslatmaSonucu sonuc = oyuncu == null ? BaslatmaSonucu.YER_UYGUN_DEGIL
                    : oyuncuyaBaslat(tur, etkinSeviye, oyuncu, true);
            if (sonuc != BaslatmaSonucu.YER_UYGUN_DEGIL) {
                return sonuc;
            }
        }
        return konumaBaslat(tur, etkinSeviye, konumLevel, konum);
    }

    /**
     * Yalnız GameTest: sunucu geneli türü otomatik seçimin başlattığı gibi (elle değil) başlatır. GameTest sunucusunda otomatik
     * seçim kapalıdır; gamerule'ün süren afete etkisi (K-C7, elle başlayan muaf) ancak böyle sınanır.
     */
    BaslatmaSonucu secimleBaslat(AfetTuru tur) {
        if (!tur.sunucuGeneli()) {
            throw new IllegalArgumentException(tur.kimlik() + " sunucu geneli değil");
        }
        return sunucuGeneliBaslat(tur, false);
    }

    /**
     * Yalnız GameTest: bölgesel türü doğal seçimin başlattığı gibi (elle değil) oyuncuda başlatır; eşzamanlı doğal afet
     * kuralları ve alan çakışması (K-E67) ancak böyle sınanır.
     */
    BaslatmaSonucu dogalBaslat(AfetTuru tur, int seviye, ServerPlayer hedef) {
        if (tur.sunucuGeneli()) {
            throw new IllegalArgumentException(tur.kimlik() + " bölgesel değil");
        }
        return oyuncuyaBaslat(tur, seviye, hedef, false);
    }

    /** Süren bütün afetleri temizce durdurur ({@code /dogalafet stop}); afet yoksa {@code false}. */
    public boolean durdur() {
        return hepsiniSonlandir(BitisNedeni.KOMUT);
    }

    private boolean hepsiniSonlandir(BitisNedeni neden) {
        List<Afet> afetler = aktifAfetler();
        for (Afet afet : afetler) {
            sonlandir(afet, server.getLevel(afet.baglam().boyut()), neden, true);
        }
        return !afetler.isEmpty();
    }

    /** Bu türden süren afeti temizce durdurur ({@code /dogalafet stop <afet>}); yoksa {@code false}. */
    public boolean durdur(AfetTuru tur) {
        Optional<Afet> afet = aktifAfet(tur);
        afet.ifPresent(a -> sonlandir(a, server.getLevel(a.baglam().boyut()), BitisNedeni.KOMUT, true));
        return afet.isPresent();
    }

    /**
     * {@code /dogalafet stop} aktif afet yokken: süren dönem süreçlerini {@link DonemSureci.DurdurmaNedeni#KOMUT} ile
     * durdurur; kural ile durmuş olanların kaydını da {@code KOMUT}'a çevirip sahibine {@code durdur(KOMUT)} bildirir (artık kural
     * açılınca sürmez; kuralın dondurduğu plan ya da mağara bırakılır, {@code durum}'da süresiz kalmaz); bunları döner.
     * Güneş patlamasına bağlı süreç komutla durunca tamamlanmamış {@code BUYUYEN} evresi {@code NORMAL}'e döner
     * (K-C26; güneş patlaması afeti sürerken dönmez).
     */
    public List<DonemSureci> donemSurecleriniDurdur() {
        ServerLevel ust = server.overworld();
        List<DonemSureci> durdurulan = new ArrayList<>();
        for (DonemSureci surec : DONEM_SURECLERI) {
            Optional<DonemSureci.DurdurmaNedeni> neden = durdurmaNedeni(surec);
            if (neden.equals(Optional.of(DonemSureci.DurdurmaNedeni.KOMUT)) || !aktifMi(surec, ust)) {
                continue;
            }
            if (neden.isPresent()) {
                DogalAfetler.LOGGER.info("Dönem süreci kural yerine komutla durmuş sayılıyor: {}", surec.kimlik());
            }
            surecDurdur(surec, ust, DonemSureci.DurdurmaNedeni.KOMUT);
            durdurulan.add(surec);
        }
        return durdurulan;
    }

    /** {@code /dogalafet asama ileri} birincil afet için: bir sonraki aşama; afet yoksa {@code false}. */
    public boolean asamaIleri() {
        return aktifAfet().map(this::asamaIleri).orElse(false);
    }

    /** {@code /dogalafet asama ileri <afet>}: o türden süren afet için bir sonraki aşama; yoksa {@code false}. */
    public boolean asamaIleri(AfetTuru tur) {
        return aktifAfet(tur).map(this::asamaIleri).orElse(false);
    }

    private boolean asamaIleri(Afet afet) {
        ServerLevel level = server.getLevel(afet.baglam().boyut());
        if (level == null) {
            sonlandir(afet, null, BitisNedeni.HATA, false);
            return true;
        }
        AfetAsamasi once = afet.asama();
        int altOnce = afet.altAsama();
        try {
            afet.asamaIleri(level);
        } catch (RuntimeException hata) {
            hataVerdi(afet, "asamaIleri", hata);
            return true;
        }
        gecisleriIsle(afet, level, once, altOnce);
        if (hala(afet) && afet.sonlandi()) {
            sonlandir(afet, level, BitisNedeni.DOGAL, true);
        }
        return true;
    }

    /**
     * {@code /dogalafet asama ileri [süreç]} aktif afet yokken (ya da süreç adı verilince): önce süren, sonra durdurulmuş
     * dönem süreçlerine sorar; ilk dolu çıktı döner. {@code kimlik} verilirse yalnız onu kimliği ya da o anki takma adı
     * olarak tanıyan süreç sorulur (K-C48, {@link DonemSureci#kimlikEsler}). Durdurulmuş
     * süreç durmuş kalır, yalnız bir aşama geçer (K-C41); böylece tamamlanırsa kaydı silinir.
     */
    public Optional<Component> donemAsamaIleri(Optional<String> kimlik) {
        ServerLevel ust = server.overworld();
        List<DonemSureci> sira = new ArrayList<>(aktifDonemSurecleri());
        sira.addAll(durdurulmusDonemSurecleri().keySet());
        kimlik.ifPresent(k -> sira.removeIf(surec -> !surec.kimlikEsler(k)));
        for (DonemSureci surec : sira) {
            try {
                Optional<Component> sonuc = surec.asamaIleri(ust);
                if (sonuc.isPresent()) {
                    DogalAfetler.LOGGER.info("Dönem süreci aşama ilerledi: {}{}", surec.kimlik(),
                            surecDurdu(surec) ? " (durdurulmuş)" : "");
                    tamamlandiysaKaydiSil(surec, ust);
                    return sonuc;
                }
            } catch (RuntimeException hata) {
                DogalAfetler.LOGGER.error("Dönem süreci ({}) aşama geçişinde hata verdi", surec.kimlik(), hata);
            }
        }
        return Optional.empty();
    }

    /**
     * Bir seçim turu (kuru çalışma: afet başlatılmaz). {@code zorla}: olay olasılığı 1 sayılır, bekleme ve sıklık yok
     * sayılır; kayıt, gamerule ve tek seferlik kurallar korunur. Hedef adayı (ayarlı boyutlarda oyuncu) yoksa yalnız sunucu
     * geneli afetler seçilebilir (K-C30). Çevrim içi (izleyici olmayan) oyuncu yoksa ya da {@code dogalAfetlerAcik=false} ise
     * boş.
     */
    public Optional<AfetTuru> secimYap(boolean zorla) {
        CekirdekAyarlari ayar = CekirdekAyarlari.al();
        return turuIsle(zorla, ayar.kontrolAraligiTick(), (tur, hedef) -> SecimTuru.Deneme.BASLADI)
                .map(SecimTuru.Sonuc::tur);
    }

    /**
     * Boş doğal afet yuvası varken sonraki doğal afet seçiminin tahmini (ölçekli tick; süren türler dışta); doğal seçim
     * olamıyorsa boş.
     */
    public Optional<SonrakiAfetTahmini> sonrakiAfetTahmini() {
        GameRules kurallar = server.getGameRules();
        if (!kurallar.getBoolean(Kurallar.DOGAL_AFETLER_ACIK)) {
            return Optional.empty();
        }
        CekirdekAyarlari ayar = CekirdekAyarlari.al();
        DunyaDonemi donem = donem();
        double siklik = Kurallar.siklikCarpani(kurallar);
        long garanti = ilkAfetGarantiAni(ayar, donem, siklik);
        return SonrakiAfetTahmini.hesapla(ayar.zamanlayici(), donem.saat(), donem.bekleme(),
                EszamanliAfetler.adaylar(uygunTurler(kurallar, ayar), aktifTurler()), donem::carpan, siklik, gunCarpani(),
                garanti == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0, garanti - donem.saat()));
    }

    /**
     * AS-12: dünyada ({@code sifirla}'dan beri) hiç afet olmadıysa ilk afet garantisinin anı (ölçekli saat); yoksa
     * {@link Long#MAX_VALUE}.
     */
    private static long ilkAfetGarantiAni(CekirdekAyarlari ayar, DunyaDonemi donem, double siklik) {
        long sure = ayar.zamanlayici().ilkAfetGarantiTick();
        if (sure <= 0 || !(siklik > 0) || !donem.sonAfetler().isEmpty()) {
            return Long.MAX_VALUE;
        }
        return donem.ilkAfetTabani() + AfetZamanlayicisi.beklemeOlcekle(sure, siklik);
    }

    /**
     * {@code afetSikligi}'nı uygular (her tick; {@code /gamerule} geri çağırması yok): çarpan değiştiyse süren beklemeler
     * yeni sıklığa ölçeklenir ({@link DunyaDonemi#siklikUygula}, kalıcı). Ham değer bu oturumda değiştiyse ve {@code bildir}
     * ise yeni değer ve sonraki afet tahmini OP oyunculara ve konsola yazılır. Ham değer değiştiyse true.
     */
    public boolean siklikDenetle(boolean bildir) {
        int ham = server.getGameRules().getInt(Kurallar.AFET_SIKLIGI);
        DunyaDonemi donem = donem();
        double onceki = donem.uygulananSiklik();
        if (donem.siklikUygula(Kurallar.siklikCarpani(server.getGameRules()))) {
            DogalAfetler.LOGGER.info("afetSikligi {}: süren beklemeler x{} → x{} ölçeklendi (genel kalan {} tick)", ham,
                    onceki, donem.uygulananSiklik(), donem.genelBeklemeKalan());
        }
        boolean degisti = sonSiklik != Integer.MIN_VALUE && ham != sonSiklik;
        sonSiklik = ham;
        if (degisti && bildir) {
            for (Component satir : SiklikBilgisi.degisimSatirlari(this)) {
                server.sendSystemMessage(satir);
                for (ServerPlayer oyuncu : server.getPlayerList().getPlayers()) {
                    if (oyuncu.hasPermissions(Commands.LEVEL_GAMEMASTERS)) {
                        oyuncu.sendSystemMessage(satir);
                    }
                }
            }
        }
        return degisti;
    }

    /** {@code /dogalafet hiz}. */
    public void hizAyarla(double carpan) {
        zamanOlcegi.ayarla(carpan);
        donem().senkronIste(true);
        DogalAfetler.LOGGER.info("Afet ve dönem zamanlayıcıları x{}", zamanOlcegi.carpan());
    }

    /**
     * {@code /dogalafet sifirla}: aktif afetler temizce iptal edilir (başarımda sayılmaz, AS-15), dönem süreçleri durur, dönem
     * ilk hâline ve hız 1'e döner.
     */
    public void sifirla() {
        hepsiniSonlandir(BitisNedeni.IPTAL);
        donemSurecleriniDurdur();
        zamanOlcegi.sifirla();
        sonKontrolSaati = Long.MIN_VALUE;
        DunyaDonemi donem = donem();
        donem.sifirla();
        DogalAfetler.LOGGER.info("Dünya dönemi sıfırlandı: {}", donem.ozet());
    }

    /** Bölüm H: ay çarpmasından sonra tsunami olasılığını config varsayılanlarıyla geçici olarak artırır. */
    public void ayCarpmasiTsunamiCarpaniniEkle() {
        CekirdekAyarlari ayar = CekirdekAyarlari.al();
        donem().carpanEkle(AfetTuru.TSUNAMI, ayar.tsunamiCarpani(), ayar.tsunamiCarpanSuresiTick(), ayar.tsunamiSonumTick(),
                ayar.tsunamiSonumTipi());
    }

    /**
     * Madde 11, K-C41 (saniyede bir, tick'ten): gamerule'ü ya da {@code dogalAfetlerAcik} kapatılan türe bağlı süren dönem
     * süreçlerini {@link DonemSureci.DurdurmaNedeni#KURAL} ile durdurur; kuralı yeniden açılan türün kural ile durmuş
     * süreçlerini sürdürür ({@link DonemSureci#devamEt}); tamamlanmış süreçlerin durdurma kaydını siler. Kural ile
     * durdurulanları döner.
     */
    public List<DonemSureci> kurallariDenetle() {
        GameRules kurallar = server.getGameRules();
        ServerLevel ust = server.overworld();
        List<DonemSureci> durdurulan = new ArrayList<>();
        for (DonemSureci surec : DONEM_SURECLERI) {
            tamamlandiysaKaydiSil(surec, ust);
            Optional<AfetTuru> tur = surec.afetTuru();
            if (tur.isEmpty() || !aktifMi(surec, ust)) {
                continue;
            }
            Optional<DonemSureci.DurdurmaNedeni> neden = durdurmaNedeni(surec);
            boolean acik = Kurallar.afetSecilebilir(kurallar, tur.get());
            if (!acik && neden.isEmpty()) {
                surecDurdur(surec, ust, DonemSureci.DurdurmaNedeni.KURAL);
                durdurulan.add(surec);
            } else if (acik && neden.equals(Optional.of(DonemSureci.DurdurmaNedeni.KURAL))) {
                surecDevamEttir(surec, ust);
            }
        }
        return durdurulan;
    }

    // ---------------------------------------------------------------- tick

    private void tick() {
        if (!geriYuklendi) {
            geriYukle();
        }
        tickSayaci++;
        CekirdekAyarlari ayar = CekirdekAyarlari.al();
        TpsOlcer.guncelle(server, ayar.tpsEsikleri());
        DunyaDonemi donem = donem();
        int adim = zamanOlcegi.ilerle();
        sonAdim = adim;
        donem.saatIlerlet(adim);
        siklikDenetle(true);
        List<Afet> afetler = donem.aktifAfetler();
        for (Afet afet : afetler) {
            if (hala(afet)) {
                afetiIsle(afet, adim);
            }
        }
        if (otomatikSecim && !dogalYuvaDolu()) {
            secimKontrolu(donem, ayar);
        }
        if (tickSayaci % ayar.afetPaketAraligiTick() == 0) {
            saniyelikIsler();
            kurallariDenetle();
        }
        donemSenkronu(donem, ayar);
    }

    private void secimKontrolu(DunyaDonemi donem, CekirdekAyarlari ayar) {
        long saat = donem.saat();
        if (sonKontrolSaati == Long.MIN_VALUE || saat < sonKontrolSaati) {
            sonKontrolSaati = saat;
            return;
        }
        long gecen = saat - sonKontrolSaati;
        if (gecen < ayar.kontrolAraligiTick()) {
            return;
        }
        sonKontrolSaati = saat;
        donem.bitmisCarpanlariTemizle();
        Map<AfetTuru, Integer> seviyeler = new EnumMap<>(AfetTuru.class);
        Optional<SecimTuru.Sonuc<UUID>> sonuc = turuIsle(false, gecen, (tur, hedef) -> {
            int seviye = seviyeler.computeIfAbsent(tur, this::seviyeSec);
            if (tur.sunucuGeneli()) {
                return deneme(sunucuGeneliBaslat(tur, false));
            }
            ServerPlayer oyuncu = server.getPlayerList().getPlayer(hedef);
            return oyuncu == null ? SecimTuru.Deneme.YER_YOK : deneme(oyuncuyaBaslat(tur, seviye, oyuncu, false));
        });
        sonuc.ifPresent(tur -> {
            for (AfetTuru ertelenen : tur.ertelenenler()) {
                DogalAfetler.LOGGER.info("{} için uygun yer bulunamadı; ertelendi", ertelenen.kimlik());
            }
            if (!tur.ertelenenler().isEmpty()) {
                donem.setDirty();
            }
        });
    }

    private static SecimTuru.Deneme deneme(BaslatmaSonucu sonuc) {
        return switch (sonuc) {
            case BASLADI -> SecimTuru.Deneme.BASLADI;
            case CAKISIYOR -> SecimTuru.Deneme.CAKISTI;
            default -> SecimTuru.Deneme.YER_YOK;
        };
    }

    /**
     * Bir seçim turunu sunucu durumuyla kurar ve {@link SecimTuru} ile işler. Çevrim içi (izleyici olmayan) oyuncu yoksa ya da
     * {@code dogalAfetlerAcik=false} ise boş; hedef adayı (ayarlı boyutlarda oyuncu) yoksa yalnız sunucu geneli afetler (N11).
     * Süren türler aday değildir; yuva doluysa seçim yok (K-E67).
     */
    private Optional<SecimTuru.Sonuc<UUID>> turuIsle(boolean zorla, long adim, SecimTuru.Baslatici<UUID> baslatici) {
        GameRules kurallar = server.getGameRules();
        if (!kurallar.getBoolean(Kurallar.DOGAL_AFETLER_ACIK)) {
            return Optional.empty();
        }
        if (server.getPlayerList().getPlayers().stream().allMatch(ServerPlayer::isSpectator)) {
            return Optional.empty();
        }
        CekirdekAyarlari ayar = CekirdekAyarlari.al();
        DunyaDonemi donem = donem();
        double siklik = Kurallar.siklikCarpani(kurallar);
        boolean garanti = !zorla && donem.saat() >= ilkAfetGarantiAni(ayar, donem, siklik);
        AfetZamanlayicisi.Girdi girdi = new AfetZamanlayicisi.Girdi(dogalYuvaDolu(), donem.saat(), adim, donem.bekleme(),
                EszamanliAfetler.adaylar(uygunTurler(kurallar, ayar), aktifTurler()), donem::carpan, siklik, zorla,
                garanti, gunCarpani());
        SecimTuru.Sonuc<UUID> sonuc = SecimTuru.isle(ayar.zamanlayici(), girdi, adayListesi(hedefAdaylari(ayar)),
                donem.sonHedef().orElse(null), baslatici, rastgele);
        return sonuc.basladi() || !sonuc.ertelenenler().isEmpty() ? Optional.of(sonuc) : Optional.empty();
    }

    /** Kayıtlı, gamerule'ü açık ve tek seferlik kuralları geçen türler. */
    private Set<AfetTuru> uygunTurler(GameRules kurallar, CekirdekAyarlari ayar) {
        double gun = oyunGunu();
        double puan = gelisimPuani();
        Set<AfetTuru> uygun = EnumSet.noneOf(AfetTuru.class);
        for (AfetTuru tur : AfetKaydi.kayitliTurler()) {
            if (Kurallar.afetSecilebilir(kurallar, tur) && tekSeferlikUygun(tur, gun, puan, ayar.tekSeferlik())) {
                uygun.add(tur);
            }
        }
        return uygun;
    }

    /**
     * Seçim turunun tek seferlik süzgeci (madde 4) bu dönemde türü geçiriyor mu: ay, güneş evresi ve pulsar zincirinin
     * geçmişi ({@link PulsarYolu#donusYolu}, {@link PulsarYolu#zincirTukendi}) dönemden okunur.
     */
    public boolean tekSeferlikUygun(AfetTuru tur, double gun, double puan, TekSeferlikAyarlari ayarlar) {
        DunyaDonemi donem = donem();
        return TekSeferlikKurallar.uygunMu(tur, donem.ayVar(), donem.gunesEvresi(), PulsarYolu.donusYolu(donem),
                PulsarYolu.zincirTukendi(donem), gun, puan, ayarlar);
    }

    private int seviyeSec(AfetTuru tur) {
        if (!tur.seviyeli()) {
            return 0;
        }
        SiddetAyarlari ayar = CekirdekAyarlari.al().siddet();
        long oyunGunu = oyunGunu();
        double gun = SiddetSecici.etkinGun(ayar, oyunGunu, gelisimPuani());
        return SiddetSecici.sec(ayar, gun, Kurallar.maksSiddet(server.getGameRules()),
                SiddetSecici.yuksek(donem().sonSeviye()), afetYuku(oyunGunu), rastgele);
    }

    /** Ayarlı boyutlardaki (N11), izleyici olmayan, canlı çevrim içi oyuncular. */
    private List<ServerPlayer> hedefAdaylari(CekirdekAyarlari ayar) {
        Set<ResourceLocation> boyutlar = ayar.boyutlar();
        List<ServerPlayer> sonuc = new ArrayList<>();
        for (ServerPlayer oyuncu : server.getPlayerList().getPlayers()) {
            if (!oyuncu.isSpectator() && oyuncu.isAlive() && boyutlar.contains(oyuncu.level().dimension().location())) {
                sonuc.add(oyuncu);
            }
        }
        return sonuc;
    }

    /** Hedef adayları; son afet merkezine {@code ardisikAfetMesafesi}'nden yakın olanlar işaretlenir (sırada sona). */
    private List<HedefSecici.Aday<UUID>> adayListesi(List<ServerPlayer> oyuncular) {
        List<GecmisKaydi> gecmis = donem().sonAfetler();
        GecmisKaydi son = gecmis.isEmpty() ? null : gecmis.get(0);
        double mesafe = CekirdekAyarlari.al().ardisikAfetMesafesi();
        List<HedefSecici.Aday<UUID>> liste = new ArrayList<>();
        for (ServerPlayer oyuncu : oyuncular) {
            boolean yakin = false;
            if (son != null && son.boyut().equals(oyuncu.level().dimension())) {
                double dx = oyuncu.getX() - (son.merkez().getX() + 0.5);
                double dz = oyuncu.getZ() - (son.merkez().getZ() + 0.5);
                yakin = dx * dx + dz * dz <= mesafe * mesafe;
            }
            liste.add(new HedefSecici.Aday<>(oyuncu.getUUID(), yakin));
        }
        return liste;
    }

    /** Sunucu geneli afet: hedef oyuncusu yoktur, merkez üst dünyanın doğma noktasıdır. */
    private BaslatmaSonucu sunucuGeneliBaslat(AfetTuru tur, boolean elle) {
        ServerLevel level = server.overworld();
        return konumdaBaslat(tur, 0, level, level.getSharedSpawnPos(), null, elle);
    }

    private BaslatmaSonucu konumaBaslat(AfetTuru tur, int seviye, ServerLevel level, BlockPos konum) {
        if (level == null || konum == null) {
            return BaslatmaSonucu.HEDEF_YOK;
        }
        return konumdaBaslat(tur, seviye, level, konum, null, true);
    }

    private BaslatmaSonucu oyuncuyaBaslat(AfetTuru tur, int seviye, ServerPlayer hedef, boolean elle) {
        if (yuvaDolu(tur, elle)) {
            return BaslatmaSonucu.ZATEN_AKTIF;
        }
        ServerLevel level = hedef.serverLevel();
        Afet afet;
        BlockPos merkez;
        try {
            afet = AfetKaydi.olustur(tur).orElse(null);
            if (afet == null) {
                return BaslatmaSonucu.KAYITLI_DEGIL;
            }
            merkez = afet.merkezSec(level, hedef, seviye, level.getRandom());
            if (!afet.yerUygunMu(level, merkez, seviye)) {
                return BaslatmaSonucu.YER_UYGUN_DEGIL;
            }
        } catch (RuntimeException hata) {
            DogalAfetler.LOGGER.error("{} için yer seçilirken hata", tur.kimlik(), hata);
            return BaslatmaSonucu.HATA;
        }
        return afetiBaslat(afet, level, merkez, seviye, hedef.getUUID(), elle);
    }

    private BaslatmaSonucu konumdaBaslat(AfetTuru tur, int seviye, ServerLevel level, BlockPos merkez, UUID hedef,
            boolean elle) {
        if (yuvaDolu(tur, elle)) {
            return BaslatmaSonucu.ZATEN_AKTIF;
        }
        Afet afet;
        try {
            afet = AfetKaydi.olustur(tur).orElse(null);
            if (afet == null) {
                return BaslatmaSonucu.KAYITLI_DEGIL;
            }
            if (!afet.yerUygunMu(level, merkez, seviye)) {
                return BaslatmaSonucu.YER_UYGUN_DEGIL;
            }
        } catch (RuntimeException hata) {
            DogalAfetler.LOGGER.error("{} için yer denetlenirken hata", tur.kimlik(), hata);
            return BaslatmaSonucu.HATA;
        }
        return afetiBaslat(afet, level, merkez, seviye, hedef, elle);
    }

    /**
     * AS-8, K-E67: komut için aynı türden süren afet yuvayı doldurur; doğal başlatma için ayrıca eşzamanlı doğal afet
     * kuralları ({@link EszamanliAfetler}: güne göre sınır, sunucu geneli afet tek başına, TPS kısması).
     */
    private boolean yuvaDolu(AfetTuru tur, boolean elle) {
        if (aktifAfet(tur).isPresent()) {
            return true;
        }
        return !elle && (dogalYuvaDolu() || EszamanliAfetler.adaylar(Set.of(tur), aktifTurler()).isEmpty());
    }

    private BaslatmaSonucu afetiBaslat(Afet afet, ServerLevel level, BlockPos merkez, int seviye, UUID hedef,
            boolean elle) {
        CekirdekAyarlari ayar = CekirdekAyarlari.al();
        DunyaDonemi donem = donem();
        long erken = ayar.erkenBildirimTick();
        long pencere = OnUyariPenceresi.sure(ayar.onUyariEnAzTick(), ayar.onUyariEnCokTick(),
                SirenTuru.sureTick(afet.tur()) - SirenTuru.STANDART_SURE_TICK, rastgele);
        AfetBaglami baglam = new AfetBaglami(afet.tur(), seviye, merkez, level.dimension(), hedef, donem.yeniAfetNo(),
                erken + pencere, erken, elle);
        try {
            afet.baslat(level, baglam);
            if (!elle && alanCakisiyor(afet, level)) {
                afet.durdur(level, false);
                return BaslatmaSonucu.CAKISIYOR;
            }
        } catch (RuntimeException hata) {
            DogalAfetler.LOGGER.error("{} başlatılırken hata; afet yumuşak durduruluyor", afet.tur().kimlik(), hata);
            try {
                afet.durdur(level, false);
            } catch (RuntimeException ikinci) {
                DogalAfetler.LOGGER.error("{} durdurulurken hata", afet.tur().kimlik(), ikinci);
            }
            return BaslatmaSonucu.HATA;
        }
        donem.afetEkle(afet);
        if (!elle) {
            donem.dogalBaslangicEkle();
        }
        if (hedef != null) {
            donem.sonHedefAyarla(hedef);
        }
        sirenAlicilari.put(afet, new HashSet<>());
        paketAlicilari.put(afet, new HashSet<>());
        DogalAfetler.LOGGER.info("Afet başladı: {} seviye {} merkez {} {} hedef {} ön-uyarı {} tick{}", afet.tur().kimlik(),
                seviye, merkez.toShortString(), level.dimension().location(), hedef == null ? "yok" : hedef,
                baglam.onUyariSuresi(), elle ? " (elle)" : "");
        MinecraftForge.EVENT_BUS.post(new AfetBasladiEvent(afet, level));
        return BaslatmaSonucu.BASLADI;
    }

    /**
     * K-E67: başlatılmış (henüz eklenmemiş) doğal afetin etki alanı aynı boyutta süren bir afetinkiyle çakışıyor mu
     * (global alanlı bölgesel afet, ör. asit yağmuru, o boyutta her alanla çakışır). Alanı okunamayan süren afet çakışma
     * sayılmaz (o afet kendi yolunda yumuşak durur).
     */
    private boolean alanCakisiyor(Afet afet, ServerLevel level) {
        List<Afet> surenler = aktifAfetler();
        if (surenler.isEmpty()) {
            return false;
        }
        EtkiAlani alan = Objects.requireNonNull(afet.etkiAlani(), "etkiAlani() null döndü");
        for (Afet diger : surenler) {
            if (!diger.baglam().boyut().equals(level.dimension()) && !diger.tur().sunucuGeneli()) {
                continue;
            }
            EtkiAlani digerAlani = alanOku(diger);
            if (digerAlani != null && alan.kesisir(digerAlani)) {
                DogalAfetler.LOGGER.info("{} başlamadı: etki alanı süren {} ile çakışıyor", afet.tur().kimlik(),
                        diger.tur().kimlik());
                return true;
            }
        }
        return false;
    }

    private void afetiIsle(Afet afet, int adim) {
        ServerLevel level = server.getLevel(afet.baglam().boyut());
        if (level == null) {
            DogalAfetler.LOGGER.warn("Afet boyutu {} yüklü değil; afet kaldırılıyor", afet.baglam().boyut().location());
            sonlandir(afet, null, BitisNedeni.HATA, false);
            return;
        }
        if (!afet.baglam().elle() && !Kurallar.afetSecilebilir(server.getGameRules(), afet.tur())) {
            if (afet.asama() == AfetAsamasi.ON_UYARI) {
                sonlandir(afet, level, BitisNedeni.IPTAL, true);
                return;
            }
            if (afet.tur().sunucuGeneli()) {
                sonlandir(afet, level, BitisNedeni.KURAL, false);
                return;
            }
        }
        AfetAsamasi once = afet.asama();
        int altOnce = afet.altAsama();
        try {
            afet.tick(level, adim);
        } catch (RuntimeException hata) {
            hataVerdi(afet, "tick", hata);
            return;
        }
        gecisleriIsle(afet, level, once, altOnce);
        if (hala(afet) && afet.sonlandi()) {
            sonlandir(afet, level, BitisNedeni.DOGAL, true);
        }
    }

    private void gecisleriIsle(Afet afet, ServerLevel level, AfetAsamasi eski, int eskiAlt) {
        AfetAsamasi yeni = afet.asama();
        if (yeni == eski && afet.altAsama() == eskiAlt) {
            return;
        }
        if (yeni != eski) {
            DogalAfetler.LOGGER.info("Afet aşaması: {} {} -> {}", afet.tur().kimlik(), eski.kimlik(), yeni.kimlik());
            if (eski == AfetAsamasi.SIREN) {
                sirenDurdur(afet);
            }
            if (yeni == AfetAsamasi.SIREN) {
                sirenIstegiGonder(afet);
            }
            if (yeni == AfetAsamasi.AKTIF) {
                takipGuncelle(afet, level);
            }
        }
        if (!hala(afet)) {
            return;
        }
        MinecraftForge.EVENT_BUS.post(new AfetAsamaDegistiEvent(afet, level, eski, yeni, eskiAlt, afet.altAsama()));
        if (!hala(afet)) {
            return;
        }
        paketleriGonder(afet);
    }

    /**
     * Sirenin henüz gitmediği (ya da çıkıp geri giren) etkilenebilecek oyunculara kalan süreyle siren isteği. AS-9: oyuncu
     * daha öncelikli bir afetin sirenini duyuyorsa bu afetin sireni ona gitmez ve sonradan da gitmez (kuyruğa alınmaz, susar).
     */
    private void sirenIstegiGonder(Afet afet) {
        Set<UUID> alicilar = sirenAlicilari.computeIfAbsent(afet, a -> new HashSet<>());
        List<ServerPlayer> yeniler = new ArrayList<>();
        for (ServerPlayer oyuncu : etkilenebilecekOyuncular(afet)) {
            if (alicilar.add(oyuncu.getUUID()) && !ustSirenDuyuyor(afet, oyuncu.getUUID())) {
                yeniler.add(oyuncu);
            }
        }
        if (yeniler.isEmpty()) {
            return;
        }
        long olcekli = kalanOku(afet);
        if (!hala(afet)) {
            return;
        }
        long kalan = zamanOlcegi.gercekTick(olcekli);
        MinecraftForge.EVENT_BUS.post(new SirenIstegiEvent(afet.tur(), SirenTuru.afetIcin(afet.tur()),
                (int) Math.max(1, Math.min(Integer.MAX_VALUE, kalan)), yeniler));
    }

    /** {@code oyuncu}, {@code afet}'ten öncelikli ({@link SirenTuru#oncelik}, eşitlikte önce başlayan) bir sireni duyuyor mu. */
    private boolean ustSirenDuyuyor(Afet afet, UUID oyuncu) {
        List<Afet> afetler = aktifAfetler();
        int sira = afetler.indexOf(afet);
        for (int i = 0; i < afetler.size(); i++) {
            Afet diger = afetler.get(i);
            if (diger == afet || diger.asama() != AfetAsamasi.SIREN) {
                continue;
            }
            int fark = SirenTuru.oncelik(diger.tur()) - SirenTuru.oncelik(afet.tur());
            Set<UUID> duyanlar = sirenAlicilari.get(diger);
            if ((fark > 0 || (fark == 0 && i < sira)) && duyanlar != null && duyanlar.contains(oyuncu)) {
                return true;
            }
        }
        return false;
    }

    private void sirenDurdur(Afet afet) {
        Set<UUID> alicilar = sirenAlicilari.get(afet);
        if (alicilar == null || alicilar.isEmpty()) {
            return;
        }
        List<ServerPlayer> oyuncular = cevrimIcindekiler(alicilar);
        alicilar.clear();
        MinecraftForge.EVENT_BUS.post(new SirenDurdurEvent(afet.tur(), oyuncular));
    }

    /** AKTIF aşamada etki alanındaki oyuncuları "etkilenen" olarak işaretler (bölgesel afette yalnız afetin boyutunda). */
    private void takipGuncelle(Afet afet, ServerLevel level) {
        if (afet.asama() != AfetAsamasi.AKTIF) {
            return;
        }
        EtkiAlani alan = alanOku(afet);
        if (alan == null) {
            return;
        }
        boolean herkes = afet.tur().sunucuGeneli();
        for (ServerPlayer oyuncu : herkes ? server.getPlayerList().getPlayers() : level.players()) {
            if (!oyuncu.isSpectator() && oyuncu.isAlive()
                    && (herkes || alan.iceriyor(oyuncu.getX(), oyuncu.getY(), oyuncu.getZ(), 0))) {
                afet.etkilenenEkle(oyuncu.getUUID());
            }
        }
    }

    private void olumuKaydet(ServerPlayer oyuncu) {
        for (Afet afet : aktifAfetler()) {
            olumuKaydet(oyuncu, afet);
        }
    }

    private void olumuKaydet(ServerPlayer oyuncu, Afet afet) {
        if (afet.asama() == AfetAsamasi.AKTIF) {
            EtkiAlani alan = alanOku(afet);
            if (alan == null) {
                return;
            }
            boolean alanda = afet.tur().sunucuGeneli()
                    || (oyuncu.level().dimension().equals(afet.baglam().boyut())
                            && alan.iceriyor(oyuncu.getX(), oyuncu.getY(), oyuncu.getZ(), 0));
            if (alanda) {
                afet.etkilenenEkle(oyuncu.getUUID());
            }
        }
        if (afet.asama() == AfetAsamasi.AKTIF || afet.asama() == AfetAsamasi.BITIS) {
            afet.olumEkle(oyuncu.getUUID());
        }
    }

    private void saniyelikIsler() {
        for (Afet afet : aktifAfetler()) {
            if (hala(afet)) {
                saniyelikIsler(afet);
            }
        }
    }

    private void saniyelikIsler(Afet afet) {
        ServerLevel level = server.getLevel(afet.baglam().boyut());
        if (level == null) {
            return;
        }
        if (afet.asama() == AfetAsamasi.SIREN) {
            sirenIstegiGonder(afet);
        }
        if (hala(afet)) {
            takipGuncelle(afet, level);
        }
        if (hala(afet)) {
            paketleriGonder(afet);
        }
    }

    /** Afet durum paketi etkilenebilecek oyunculara; alandan çıkanlara bir kez "afet yok". ON_UYARI gizlidir. */
    private void paketleriGonder(Afet afet) {
        if (afet.asama() == AfetAsamasi.ON_UYARI) {
            return;
        }
        EtkiAlani alan = alanOku(afet);
        if (alan == null) {
            return;
        }
        CompoundTag ozel = new CompoundTag();
        try {
            afet.istemciVerisiYaz(ozel);
        } catch (RuntimeException hata) {
            hataVerdi(afet, "istemciVerisiYaz", hata);
            return;
        }
        long kalan = kalanOku(afet);
        if (!hala(afet)) {
            return;
        }
        AfetDurumPaketi paket = AfetDurumPaketi.den(afet, alan, zamanOlcegi.gercekTick(kalan), ozel);
        Set<UUID> yeni = new HashSet<>();
        for (ServerPlayer oyuncu : etkilenebilecekOyuncular(afet)) {
            AgKanali.oyuncuyaGonder(oyuncu, paket);
            yeni.add(oyuncu.getUUID());
        }
        Set<UUID> eskiler = paketAlicilari.getOrDefault(afet, Set.of());
        AfetDurumPaketi yok = AfetDurumPaketi.yok(afet.tur());
        for (UUID eski : eskiler) {
            if (!yeni.contains(eski)) {
                ServerPlayer oyuncu = server.getPlayerList().getPlayer(eski);
                if (oyuncu != null) {
                    AgKanali.oyuncuyaGonder(oyuncu, yok);
                }
            }
        }
        paketAlicilari.put(afet, yeni);
    }

    private void sonlandir(Afet afet, ServerLevel level, BitisNedeni neden, boolean temiz) {
        if (!hala(afet)) {
            return;
        }
        DunyaDonemi donem = donem();
        long saat = donem.saat();
        boolean aktifeGirdi = afet.asama() == AfetAsamasi.AKTIF || afet.asama() == AfetAsamasi.BITIS;
        sirenDurdur(afet);
        if (level != null) {
            try {
                afet.durdur(level, temiz);
            } catch (RuntimeException hata) {
                DogalAfetler.LOGGER.error("Afet durdurulurken hata: {}", afet.tur().kimlik(), hata);
            }
        }
        AfetDurumPaketi yok = AfetDurumPaketi.yok(afet.tur());
        for (ServerPlayer oyuncu : cevrimIcindekiler(paketAlicilari.getOrDefault(afet, Set.of()))) {
            AgKanali.oyuncuyaGonder(oyuncu, yok);
        }
        paketAlicilari.remove(afet);
        sirenAlicilari.remove(afet);
        donem.afetCikar(afet);

        AfetBaglami baglam = afet.baglam();
        Set<UUID> etkilenenler = new LinkedHashSet<>(afet.etkilenenler());
        Set<UUID> sagCikanlar = new LinkedHashSet<>();
        for (UUID kimlik : etkilenenler) {
            if (!afet.olenler().contains(kimlik) && server.getPlayerList().getPlayer(kimlik) != null) {
                sagCikanlar.add(kimlik);
            }
        }
        if (neden != BitisNedeni.IPTAL) {
            donem.gecmiseEkle(new GecmisKaydi(afet.tur(), baglam.seviye(), baglam.merkez(), baglam.boyut(), saat));
            donem.bekleme().afetBitti(afet.tur(), baglam.seviye(), saat, CekirdekAyarlari.al().zamanlayici(),
                    donem.uygulananSiklik(), gunCarpani());
            // K-E67: art arda 4-5 kuralı yalnız doğal afetler arasındadır; komutla başlatılan seviye sonrakini kısıtlamaz.
            if (afet.tur().seviyeli() && !baglam.elle()) {
                donem.sonSeviyeAyarla(baglam.seviye());
            }
            donem.setDirty();
        }
        boolean surecGeriAlindi = afet.tur() == AfetTuru.GUNES_PATLAMASI && neden != BitisNedeni.DOGAL
                && tamamlanmamisGunesSureciniGeriAl(neden.name().toLowerCase(Locale.ROOT));
        DogalAfetler.LOGGER.info("Afet bitti: {} seviye {} neden {} etkilenen {} sağ çıkan {}", afet.tur().kimlik(),
                baglam.seviye(), neden, etkilenenler.size(), sagCikanlar.size());
        MinecraftForge.EVENT_BUS.post(new AfetBittiEvent(afet.tur(), baglam.seviye(), baglam.merkez(), baglam.boyut(),
                etkilenenler, sagCikanlar, baglam.baslangicSaati(), saat, neden, aktifeGirdi, surecGeriAlindi));
    }

    /**
     * Madde 11, K-C26: güneş patlamasının 5 günlük büyümesi tamamlanmamış süreçtir; durdurulunca güneş normale döner.
     * Geri aldıysa true.
     */
    private boolean tamamlanmamisGunesSureciniGeriAl(String neden) {
        DunyaDonemi donem = donem();
        if (donem.gunesEvresi() != GunesEvresi.BUYUYEN) {
            return false;
        }
        donem.gunesEvresiAyarla(GunesEvresi.NORMAL);
        DogalAfetler.LOGGER.info("Güneş patlaması tamamlanmadan durdu ({}): güneş evresi NORMAL'e döndü", neden);
        return true;
    }

    private boolean aktifMi(DonemSureci surec, ServerLevel ust) {
        try {
            return surec.aktifMi(ust);
        } catch (RuntimeException hata) {
            DogalAfetler.LOGGER.error("Dönem süreci ({}) durumu okunamadı", surec.kimlik(), hata);
            return false;
        }
    }

    /** Süreç tamamlandıysa ({@code aktifMi} false) durdurma kaydını siler; {@code aktifMi} hata verirse kayda dokunmaz. */
    private void tamamlandiysaKaydiSil(DonemSureci surec, ServerLevel ust) {
        if (!surecDurdu(surec)) {
            return;
        }
        boolean aktif;
        try {
            aktif = surec.aktifMi(ust);
        } catch (RuntimeException hata) {
            DogalAfetler.LOGGER.error("Dönem süreci ({}) durumu okunamadı; durdurma kaydı korunuyor", surec.kimlik(), hata);
            return;
        }
        if (!aktif) {
            donem().surecDurdurmasiAyarla(surec.kimlik(), null);
            DogalAfetler.LOGGER.info("Dönem süreci tamamlandı; durdurma kaydı silindi: {}", surec.kimlik());
        }
    }

    private void surecDurdur(DonemSureci surec, ServerLevel ust, DonemSureci.DurdurmaNedeni neden) {
        donem().surecDurdurmasiAyarla(surec.kimlik(), neden);
        try {
            surec.durdur(ust, neden);
        } catch (RuntimeException hata) {
            DogalAfetler.LOGGER.error("Dönem süreci ({}) durdurulurken hata", surec.kimlik(), hata);
        }
        DogalAfetler.LOGGER.info("Dönem süreci durduruldu ({}): {}", neden.name().toLowerCase(Locale.ROOT), surec.kimlik());
        if (neden == DonemSureci.DurdurmaNedeni.KOMUT) {
            gunesBuyumesiniGeriAl(surec);
        }
    }

    private void surecDevamEttir(DonemSureci surec, ServerLevel ust) {
        donem().surecDurdurmasiAyarla(surec.kimlik(), null);
        try {
            surec.devamEt(ust);
        } catch (RuntimeException hata) {
            DogalAfetler.LOGGER.error("Dönem süreci ({}) sürdürülürken hata", surec.kimlik(), hata);
        }
        DogalAfetler.LOGGER.info("Dönem süreci kural açılınca sürüyor: {}", surec.kimlik());
    }

    /**
     * K-C26, Y5: güneş patlamasına bağlı süreç komutla durunca tamamlanmamış büyüme geri alınır; güneş patlaması afeti
     * sürerken alınmaz (büyüme o afetindir; afet kendi bitişinde karar verir).
     */
    private void gunesBuyumesiniGeriAl(DonemSureci surec) {
        boolean gunesSureci = surec.afetTuru().filter(tur -> tur == AfetTuru.GUNES_PATLAMASI).isPresent();
        boolean gunesAfetiSuruyor = aktifAfet(AfetTuru.GUNES_PATLAMASI).isPresent();
        if (gunesSureci && !gunesAfetiSuruyor) {
            tamamlanmamisGunesSureciniGeriAl("komut");
        }
    }

    /** Afet hâlâ süren afetlerden mi (hata ya da durdurma onu kaldırmış olabilir). */
    private boolean hala(Afet afet) {
        return donem().afetVarMi(afet);
    }

    /** Afetin etki alanını korumalı okur; hata verirse afet yumuşak durur ve {@code null} döner. */
    private EtkiAlani alanOku(Afet afet) {
        try {
            return Objects.requireNonNull(afet.etkiAlani(), "etkiAlani() null döndü");
        } catch (RuntimeException hata) {
            hataVerdi(afet, "etkiAlani", hata);
            return null;
        }
    }

    /** K-C23: afet kodu hata verdi; ERROR log, aktif afetse {@link BitisNedeni#HATA} ile yumuşak durdurma. */
    private void hataVerdi(Afet afet, String metot, RuntimeException hata) {
        DogalAfetler.LOGGER.error("Afet kodu hata verdi ({}.{}); afet yumuşak durduruluyor", afet.tur().kimlik(), metot,
                hata);
        if (hala(afet)) {
            sonlandir(afet, server.getLevel(afet.baglam().boyut()), BitisNedeni.HATA, false);
        }
    }

    private List<ServerPlayer> cevrimIcindekiler(Set<UUID> kimlikler) {
        List<ServerPlayer> sonuc = new ArrayList<>();
        for (UUID kimlik : kimlikler) {
            ServerPlayer oyuncu = server.getPlayerList().getPlayer(kimlik);
            if (oyuncu != null) {
                sonuc.add(oyuncu);
            }
        }
        return sonuc;
    }

    // ---------------------------------------------------------------- kalıcılık ve senkron

    private void geriYukle() {
        if (geriYuklendi) {
            return;
        }
        geriYuklendi = true;
        DunyaDonemi donem = donem();
        afetiGeriYukle(donem);
        DogalAfetler.LOGGER.info("Dünya dönemi {} ({}): {}", donem.kayittanOkundu() ? "okundu" : "oluşturuldu",
                DunyaDonemi.VERI_ADI, donem.ozet());
    }

    private void afetiGeriYukle(DunyaDonemi donem) {
        Set<AfetTuru> yuklenen = EnumSet.noneOf(AfetTuru.class);
        for (CompoundTag kayit : donem.bekleyenAfetleriAl()) {
            afetiGeriYukle(donem, kayit, yuklenen);
        }
    }

    private void afetiGeriYukle(DunyaDonemi donem, CompoundTag kayit, Set<AfetTuru> yuklenen) {
        Optional<Afet> afet;
        try {
            afet = Afet.turOku(kayit).flatMap(AfetKaydi::olustur);
            if (afet.isEmpty()) {
                DogalAfetler.LOGGER.warn("Kayıttaki aktif afet ({}) kayıtlı değil; atıldı", kayit.getString("tur"));
                donem.setDirty();
                return;
            }
            if (!yuklenen.add(afet.get().tur())) {
                DogalAfetler.LOGGER.warn("Kayıtta aynı türden ikinci aktif afet ({}); atıldı", afet.get().tur().kimlik());
                donem.setDirty();
                return;
            }
            afet.get().durumOku(kayit);
        } catch (RuntimeException hata) {
            DogalAfetler.LOGGER.warn("Kayıttaki aktif afet okunamadı; atıldı: {}", kayit, hata);
            donem.setDirty();
            return;
        }
        ServerLevel level = server.getLevel(afet.get().baglam().boyut());
        if (level == null) {
            DogalAfetler.LOGGER.warn("Kayıttaki aktif afetin boyutu yok ({}); atıldı", afet.get().baglam().boyut().location());
            donem.setDirty();
            return;
        }
        donem.afetEkle(afet.get());
        sirenAlicilari.put(afet.get(), new HashSet<>());
        paketAlicilari.put(afet.get(), new HashSet<>());
        try {
            afet.get().yuklendi(level);
        } catch (RuntimeException hata) {
            hataVerdi(afet.get(), "yuklendi", hata);
            return;
        }
        DogalAfetler.LOGGER.info("Aktif afet geri yüklendi: {} seviye {} aşama {} (aşamada {} tick)",
                afet.get().tur().kimlik(), afet.get().seviye(), afet.get().asama().kimlik(), afet.get().asamaGecenTick());
    }

    private void kapanis() {
        DunyaDonemi donem = donem();
        for (Afet afet : donem.aktifAfetler()) {
            ServerLevel level = server.getLevel(afet.baglam().boyut());
            if (level == null) {
                continue;
            }
            try {
                afet.kapanirken(level);
            } catch (RuntimeException hata) {
                hataVerdi(afet, "kapanirken", hata);
            }
        }
        DogalAfetler.LOGGER.info("Dünya dönemi (kapanış): {}", donem.ozet());
    }

    private void donemGonder(ServerPlayer oyuncu) {
        AgKanali.oyuncuyaGonder(oyuncu, DonemPaketi.den(donem(), zamanOlcegi.carpan(),
                Kurallar.blokHasariAcik(server.getGameRules())));
    }

    private void donemSenkronu(DunyaDonemi donem, CekirdekAyarlari ayar) {
        long gecen = tickSayaci - sonDonemPaketi;
        boolean tazele = gecen >= ayar.donemTazelemeTick();
        if (!tazele && !(donem.senkronGerekli() && (donem.acilSenkron() || gecen >= ayar.donemPaketAraligiTick()))) {
            return;
        }
        if (!server.getPlayerList().getPlayers().isEmpty()) {
            AgKanali.herkeseGonder(DonemPaketi.den(donem, zamanOlcegi.carpan(),
                    Kurallar.blokHasariAcik(server.getGameRules())));
        }
        donem.senkronTamam();
        sonDonemPaketi = tickSayaci;
    }
}
