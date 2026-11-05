package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.CekirdekAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.AfetZamanlayicisi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.GelisimOlcutu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.HedefSecici;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.OnUyariPenceresi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SiddetAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SiddetSecici;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.TekSeferlikKurallar;
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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Afet yöneticisi (madde 3, 4, 10, 11): sunucu tick'inde zamanlayıcıyı çalıştırır, afet seçer, yaşam döngüsünü ilerletir,
 * siren isteklerini ve afet durum paketlerini etkilenebilecek oyunculara gönderir, "sağ çıkan" takibini yapar ve afet
 * bitince {@link AfetBittiEvent} yayınlar. Aynı anda tek afet vardır. Karar mantığı {@code core.mantik}'tedir; bu sınıf
 * Minecraft bağlantısıdır. Sunucu başına bir örnek ({@link #al}); yalnız sunucu ana thread'inde kullanılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class AfetYoneticisi {
    /** Elle ya da otomatik başlatmanın sonucu. */
    public enum BaslatmaSonucu {
        BASLADI,
        ZATEN_AKTIF,
        KAYITLI_DEGIL,
        YER_UYGUN_DEGIL,
        HEDEF_YOK,
        HATA
    }

    private static AfetYoneticisi ornek;

    private final MinecraftServer server;
    private final ZamanOlcegi zamanOlcegi = new ZamanOlcegi();
    private final SplittableRandom rastgele = new SplittableRandom();
    private final boolean otomatikSecim;
    private final Set<UUID> sirenAlicilari = new HashSet<>();
    private Set<UUID> paketAlicilari = new HashSet<>();
    private boolean geriYuklendi;
    private int sonAdim;
    private long sonKontrolSaati = Long.MIN_VALUE;
    private long tickSayaci;
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

    @SubscribeEvent
    public static void boyutDegisti(PlayerEvent.PlayerChangedDimensionEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            al(oyuncu.server).donemGonder(oyuncu);
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

    /** Süren afet (ON_UYARI dahil); radar ve sismograf aşamayı {@link Afet#normalOnUyariBasladi()} ile sınar. */
    public Optional<Afet> aktifAfet() {
        return donem().aktifAfet();
    }

    /** Afet ve dönem zamanlayıcılarının ortak hız çarpanı. */
    public ZamanOlcegi zamanOlcegi() {
        return zamanOlcegi;
    }

    /** Bu tick'te ilerleyen ölçekli tick sayısı (dönem süreçleri bununla ilerler). */
    public int zamanAdimi() {
        return sonAdim;
    }

    /** Oyun günü: üst dünyanın zamanı / 24000 + {@code /dogalafet gun} ofseti. */
    public long oyunGunu() {
        return donem().oyunGunu(server.overworld().getDayTime());
    }

    /** Çevrim içi oyuncuların gelişim puanı (config ölçütüne göre ortalama ya da en gelişmiş). */
    public double gelisimPuani() {
        List<ServerPlayer> oyuncular = server.getPlayerList().getPlayers();
        double[] puanlar = oyuncular.stream().filter(oyuncu -> !oyuncu.isSpectator())
                .mapToDouble(oyuncu -> GelisimOlcutu.puan(oyuncu.experienceLevel, oyuncu.getArmorValue())).toArray();
        return CekirdekAyarlari.al().gelisimOlcutu().topla(puanlar);
    }

    /** Verilen oyun günündeki seviye olasılıkları (S1–S5; gamerule üst sınırı ve art arda 4-5 kuralı dahil). */
    public double[] seviyeOlasiliklari(double gun) {
        SiddetAyarlari ayar = CekirdekAyarlari.al().siddet();
        return SiddetSecici.olasiliklar(ayar, SiddetSecici.etkinGun(ayar, gun, gelisimPuani()),
                Kurallar.maksSiddet(server.getGameRules()), SiddetSecici.yuksek(donem().sonSeviye()));
    }

    /**
     * Afetten etkilenebilecek oyuncular: etki alanı + {@code cekirdek.hedef.etkilenmeEkMesafesi} içinde, afetin boyutunda;
     * sunucu geneli afette bütün çevrim içi oyuncular (N11). Siren ve afet durum paketi bunlara gider.
     */
    public List<ServerPlayer> etkilenebilecekOyuncular(Afet afet) {
        EtkiAlani alan = afet.etkiAlani();
        if (afet.tur().sunucuGeneli() || alan.global()) {
            return List.copyOf(server.getPlayerList().getPlayers());
        }
        ServerLevel level = server.getLevel(afet.baglam().boyut());
        if (level == null) {
            return List.of();
        }
        double ek = CekirdekAyarlari.al().etkilenmeEkMesafesi();
        List<ServerPlayer> sonuc = new ArrayList<>();
        for (ServerPlayer oyuncu : level.players()) {
            if (alan.iceriyor(oyuncu.getX(), oyuncu.getY(), oyuncu.getZ(), ek)) {
                sonuc.add(oyuncu);
            }
        }
        return sonuc;
    }

    // ---------------------------------------------------------------- komut ve alt sistem API'si

    /**
     * {@code /dogalafet start}: bekleme ve gamerule kurallarını atlar, ama aktif afet varken reddeder. {@code seviye} boşsa
     * şiddet seçiciyle seçilir (seviyesiz afette yok sayılır). {@code hedef} boşsa uygun bir çevrim içi oyuncu, o da
     * yoksa {@code konumLevel}/{@code konum} kullanılır.
     */
    public BaslatmaSonucu elleBaslat(AfetTuru tur, Integer seviye, ServerPlayer hedef, ServerLevel konumLevel,
            BlockPos konum) {
        if (aktifAfet().isPresent()) {
            return BaslatmaSonucu.ZATEN_AKTIF;
        }
        if (!AfetKaydi.kayitliMi(tur)) {
            return BaslatmaSonucu.KAYITLI_DEGIL;
        }
        int etkinSeviye = !tur.seviyeli() ? 0
                : seviye != null ? Math.max(AfetTuru.EN_DUSUK_SEVIYE, Math.min(AfetTuru.EN_YUKSEK_SEVIYE, seviye))
                        : seviyeSec(tur);
        if (hedef != null || tur.sunucuGeneli()) {
            return baslat(tur, etkinSeviye, hedef, null, null, true);
        }
        if (!hedefAdaylari(CekirdekAyarlari.al()).isEmpty()) {
            BaslatmaSonucu sonuc = baslat(tur, etkinSeviye, null, null, null, true);
            if (sonuc != BaslatmaSonucu.YER_UYGUN_DEGIL) {
                return sonuc;
            }
        }
        return baslat(tur, etkinSeviye, null, konumLevel, konum, true);
    }

    /** {@code /dogalafet stop}: aktif afeti temizce durdurur; afet yoksa {@code false}. */
    public boolean durdur() {
        Optional<Afet> afet = aktifAfet();
        if (afet.isEmpty()) {
            return false;
        }
        sonlandir(afet.get(), server.getLevel(afet.get().baglam().boyut()), BitisNedeni.KOMUT, true);
        return true;
    }

    /** {@code /dogalafet asama ileri} aktif afet için: bir sonraki aşama; afet yoksa {@code false}. */
    public boolean asamaIleri() {
        Optional<Afet> secili = aktifAfet();
        if (secili.isEmpty()) {
            return false;
        }
        Afet afet = secili.get();
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
            DogalAfetler.LOGGER.error("Afet aşama geçişi hata verdi: {}", afet.tur().kimlik(), hata);
            sonlandir(afet, level, BitisNedeni.HATA, false);
            return true;
        }
        gecisleriIsle(afet, level, once, altOnce);
        if (afet.sonlandi()) {
            sonlandir(afet, level, BitisNedeni.DOGAL, true);
        }
        return true;
    }

    /**
     * Bir seçim turu (kuru çalışma: afet başlatılmaz). {@code zorla}: olay olasılığı 1 sayılır, bekleme ve sıklık yok
     * sayılır; kayıt, gamerule ve tek seferlik kurallar korunur. Oyuncu yoksa ya da {@code dogalAfetlerAcik=false} ise boş.
     */
    public Optional<AfetTuru> secimYap(boolean zorla) {
        return secimYap(zorla, CekirdekAyarlari.al().kontrolAraligiTick(), EnumSet.noneOf(AfetTuru.class));
    }

    /** {@code /dogalafet hiz}. */
    public void hizAyarla(double carpan) {
        zamanOlcegi.ayarla(carpan);
        donem().senkronIste(true);
        DogalAfetler.LOGGER.info("Afet ve dönem zamanlayıcıları x{}", zamanOlcegi.carpan());
    }

    /** {@code /dogalafet sifirla}: aktif afet temizce durur, dönem ilk hâline ve hız 1'e döner. */
    public void sifirla() {
        durdur();
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

    // ---------------------------------------------------------------- tick

    private void tick() {
        if (!geriYuklendi) {
            geriYukle();
        }
        tickSayaci++;
        CekirdekAyarlari ayar = CekirdekAyarlari.al();
        TpsOlcer.guncelle(server, ayar.tpsEsigi(), ayar.tpsKritikEsigi());
        DunyaDonemi donem = donem();
        int adim = zamanOlcegi.ilerle();
        sonAdim = adim;
        donem.saatIlerlet(adim);
        Optional<Afet> afet = donem.aktifAfet();
        if (afet.isPresent()) {
            afetiIsle(afet.get(), adim);
        } else if (otomatikSecim) {
            secimKontrolu(donem, ayar);
        }
        if (tickSayaci % ayar.afetPaketAraligiTick() == 0) {
            saniyelikIsler();
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
        Optional<AfetTuru> secim = secimYap(false, gecen, EnumSet.noneOf(AfetTuru.class));
        secim.ifPresent(this::otomatikBaslat);
    }

    private Optional<AfetTuru> secimYap(boolean zorla, long adim, Set<AfetTuru> haric) {
        GameRules kurallar = server.getGameRules();
        if (!kurallar.getBoolean(Kurallar.DOGAL_AFETLER_ACIK)) {
            return Optional.empty();
        }
        CekirdekAyarlari ayar = CekirdekAyarlari.al();
        if (hedefAdaylari(ayar).isEmpty()) {
            return Optional.empty();
        }
        DunyaDonemi donem = donem();
        Set<AfetTuru> uygun = uygunTurler(kurallar, ayar);
        uygun.removeAll(haric);
        AfetZamanlayicisi.Girdi girdi = new AfetZamanlayicisi.Girdi(donem.aktifAfet().isPresent(), donem.saat(), adim,
                donem.bekleme(), uygun, donem::carpan, Kurallar.siklikCarpani(kurallar), zorla);
        return AfetZamanlayicisi.sec(ayar.zamanlayici(), girdi, rastgele);
    }

    /** Kayıtlı, gamerule'ü açık ve tek seferlik kuralları geçen türler. */
    private Set<AfetTuru> uygunTurler(GameRules kurallar, CekirdekAyarlari ayar) {
        DunyaDonemi donem = donem();
        double gun = oyunGunu();
        double puan = gelisimPuani();
        Set<AfetTuru> uygun = EnumSet.noneOf(AfetTuru.class);
        for (AfetTuru tur : AfetKaydi.kayitliTurler()) {
            if (Kurallar.afetSecilebilir(kurallar, tur) && TekSeferlikKurallar.uygunMu(tur, donem.ayVar(),
                    donem.gunesEvresi(), gun, puan, ayar.tekSeferlik())) {
                uygun.add(tur);
            }
        }
        return uygun;
    }

    /** Seçilen türü başlatır; uygun yer yoksa tür ertelenir ve sıradaki afet seçilir (N14). */
    private void otomatikBaslat(AfetTuru ilk) {
        CekirdekAyarlari ayar = CekirdekAyarlari.al();
        DunyaDonemi donem = donem();
        Set<AfetTuru> denenen = EnumSet.noneOf(AfetTuru.class);
        AfetTuru tur = ilk;
        while (tur != null) {
            denenen.add(tur);
            BaslatmaSonucu sonuc = baslat(tur, seviyeSec(tur), null, null, null, false);
            if (sonuc == BaslatmaSonucu.BASLADI || sonuc == BaslatmaSonucu.ZATEN_AKTIF) {
                return;
            }
            donem.bekleme().ertele(tur, donem.saat(), ayar.zamanlayici().ertelemeTick());
            donem.setDirty();
            DogalAfetler.LOGGER.info("{} başlatılamadı ({}); ertelendi, sıradaki afet seçiliyor", tur.kimlik(), sonuc);
            Set<AfetTuru> haric = EnumSet.copyOf(denenen);
            for (AfetTuru aday : AfetTuru.values()) {
                if (donem.bekleme().turBeklemede(aday, donem.saat())) {
                    haric.add(aday);
                }
            }
            tur = secimYap(true, ayar.kontrolAraligiTick(), haric).orElse(null);
        }
    }

    private int seviyeSec(AfetTuru tur) {
        if (!tur.seviyeli()) {
            return 0;
        }
        SiddetAyarlari ayar = CekirdekAyarlari.al().siddet();
        double gun = SiddetSecici.etkinGun(ayar, oyunGunu(), gelisimPuani());
        return SiddetSecici.sec(ayar, gun, Kurallar.maksSiddet(server.getGameRules()),
                SiddetSecici.yuksek(donem().sonSeviye()), rastgele);
    }

    /** Afetin boyutlarındaki (N11), izleyici olmayan çevrim içi oyuncular. */
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

    private BaslatmaSonucu baslat(AfetTuru tur, int seviye, ServerPlayer zorunluHedef, ServerLevel konumLevel,
            BlockPos konum, boolean elle) {
        if (aktifAfet().isPresent()) {
            return BaslatmaSonucu.ZATEN_AKTIF;
        }
        if (!AfetKaydi.kayitliMi(tur)) {
            return BaslatmaSonucu.KAYITLI_DEGIL;
        }
        try {
            if (tur.sunucuGeneli()) {
                ServerLevel level = server.overworld();
                Afet afet = AfetKaydi.olustur(tur).orElseThrow();
                BlockPos merkez = level.getSharedSpawnPos();
                return afet.yerUygunMu(level, merkez, seviye) ? afetiBaslat(afet, level, merkez, seviye, null, elle)
                        : BaslatmaSonucu.YER_UYGUN_DEGIL;
            }
            if (zorunluHedef != null) {
                return oyuncuyaBaslat(tur, seviye, zorunluHedef, elle) ? BaslatmaSonucu.BASLADI
                        : BaslatmaSonucu.YER_UYGUN_DEGIL;
            }
            if (konum != null && konumLevel != null) {
                Afet afet = AfetKaydi.olustur(tur).orElseThrow();
                return afet.yerUygunMu(konumLevel, konum, seviye)
                        ? afetiBaslat(afet, konumLevel, konum, seviye, null, elle) : BaslatmaSonucu.YER_UYGUN_DEGIL;
            }
            CekirdekAyarlari ayar = CekirdekAyarlari.al();
            List<ServerPlayer> adaylar = hedefAdaylari(ayar);
            if (adaylar.isEmpty()) {
                return BaslatmaSonucu.HEDEF_YOK;
            }
            List<GecmisKaydi> gecmis = donem().sonAfetler();
            GecmisKaydi son = gecmis.isEmpty() ? null : gecmis.get(0);
            double mesafe = ayar.ardisikAfetMesafesi();
            List<HedefSecici.Aday<UUID>> liste = new ArrayList<>();
            for (ServerPlayer oyuncu : adaylar) {
                boolean yakin = false;
                if (son != null && son.boyut().equals(oyuncu.level().dimension())) {
                    double dx = oyuncu.getX() - (son.merkez().getX() + 0.5);
                    double dz = oyuncu.getZ() - (son.merkez().getZ() + 0.5);
                    yakin = dx * dx + dz * dz <= mesafe * mesafe;
                }
                liste.add(new HedefSecici.Aday<>(oyuncu.getUUID(), yakin));
            }
            for (UUID kimlik : HedefSecici.sirala(liste, donem().sonHedef().orElse(null), rastgele)) {
                ServerPlayer oyuncu = server.getPlayerList().getPlayer(kimlik);
                if (oyuncu != null && oyuncuyaBaslat(tur, seviye, oyuncu, elle)) {
                    return BaslatmaSonucu.BASLADI;
                }
            }
            return BaslatmaSonucu.YER_UYGUN_DEGIL;
        } catch (RuntimeException hata) {
            DogalAfetler.LOGGER.error("{} başlatılırken hata", tur.kimlik(), hata);
            return BaslatmaSonucu.HATA;
        }
    }

    private boolean oyuncuyaBaslat(AfetTuru tur, int seviye, ServerPlayer hedef, boolean elle) {
        ServerLevel level = hedef.serverLevel();
        Afet afet = AfetKaydi.olustur(tur).orElseThrow();
        BlockPos merkez = afet.merkezSec(level, hedef, seviye, level.getRandom());
        if (!afet.yerUygunMu(level, merkez, seviye)) {
            return false;
        }
        return afetiBaslat(afet, level, merkez, seviye, hedef.getUUID(), elle) == BaslatmaSonucu.BASLADI;
    }

    private BaslatmaSonucu afetiBaslat(Afet afet, ServerLevel level, BlockPos merkez, int seviye, UUID hedef,
            boolean elle) {
        CekirdekAyarlari ayar = CekirdekAyarlari.al();
        DunyaDonemi donem = donem();
        long erken = ayar.erkenBildirimTick();
        long pencere = OnUyariPenceresi.sure(ayar.onUyariEnAzTick(), ayar.onUyariEnCokTick(), rastgele);
        AfetBaglami baglam = new AfetBaglami(afet.tur(), seviye, merkez, level.dimension(), hedef, donem.saat(),
                erken + pencere, erken, elle);
        afet.baslat(level, baglam);
        donem.aktifAfetAyarla(afet);
        if (hedef != null) {
            donem.sonHedefAyarla(hedef);
        }
        sirenAlicilari.clear();
        paketAlicilari = new HashSet<>();
        DogalAfetler.LOGGER.info("Afet başladı: {} seviye {} merkez {} {} hedef {} ön-uyarı {} tick{}", afet.tur().kimlik(),
                seviye, merkez.toShortString(), level.dimension().location(), hedef == null ? "yok" : hedef,
                baglam.onUyariSuresi(), elle ? " (elle)" : "");
        MinecraftForge.EVENT_BUS.post(new AfetBasladiEvent(afet, level));
        return BaslatmaSonucu.BASLADI;
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
            DogalAfetler.LOGGER.error("Afet tick'i hata verdi: {}; afet yumuşak durduruluyor", afet.tur().kimlik(), hata);
            sonlandir(afet, level, BitisNedeni.HATA, false);
            return;
        }
        gecisleriIsle(afet, level, once, altOnce);
        if (afet.sonlandi()) {
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
        MinecraftForge.EVENT_BUS.post(new AfetAsamaDegistiEvent(afet, level, eski, yeni, eskiAlt, afet.altAsama()));
        paketleriGonder(afet);
    }

    /** Sirenin henüz gitmediği etkilenebilecek oyunculara kalan süreyle siren isteği. */
    private void sirenIstegiGonder(Afet afet) {
        List<ServerPlayer> yeniler = new ArrayList<>();
        for (ServerPlayer oyuncu : etkilenebilecekOyuncular(afet)) {
            if (sirenAlicilari.add(oyuncu.getUUID())) {
                yeniler.add(oyuncu);
            }
        }
        if (yeniler.isEmpty()) {
            return;
        }
        long kalan = zamanOlcegi.gercekTick(afet.asamaKalanTick());
        MinecraftForge.EVENT_BUS.post(new SirenIstegiEvent(afet.tur(), SirenTuru.afetIcin(afet.tur()),
                (int) Math.max(1, Math.min(Integer.MAX_VALUE, kalan)), yeniler));
    }

    private void sirenDurdur(Afet afet) {
        if (sirenAlicilari.isEmpty()) {
            return;
        }
        List<ServerPlayer> oyuncular = cevrimIcindekiler(sirenAlicilari);
        sirenAlicilari.clear();
        MinecraftForge.EVENT_BUS.post(new SirenDurdurEvent(afet.tur(), oyuncular));
    }

    /** AKTIF aşamada etki alanındaki oyuncuları "etkilenen" olarak işaretler. */
    private void takipGuncelle(Afet afet, ServerLevel level) {
        if (afet.asama() != AfetAsamasi.AKTIF) {
            return;
        }
        EtkiAlani alan = afet.etkiAlani();
        boolean herkes = afet.tur().sunucuGeneli() || alan.global();
        for (ServerPlayer oyuncu : herkes ? server.getPlayerList().getPlayers() : level.players()) {
            if (!oyuncu.isSpectator() && oyuncu.isAlive()
                    && (herkes || alan.iceriyor(oyuncu.getX(), oyuncu.getY(), oyuncu.getZ(), 0))) {
                afet.etkilenenEkle(oyuncu.getUUID());
            }
        }
    }

    private void olumuKaydet(ServerPlayer oyuncu) {
        Optional<Afet> secili = aktifAfet();
        if (secili.isEmpty()) {
            return;
        }
        Afet afet = secili.get();
        if (afet.asama() == AfetAsamasi.AKTIF) {
            EtkiAlani alan = afet.etkiAlani();
            boolean alanda = afet.tur().sunucuGeneli() || alan.global()
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
        Optional<Afet> secili = aktifAfet();
        if (secili.isEmpty()) {
            return;
        }
        Afet afet = secili.get();
        ServerLevel level = server.getLevel(afet.baglam().boyut());
        if (level == null) {
            return;
        }
        if (afet.asama() == AfetAsamasi.SIREN) {
            sirenIstegiGonder(afet);
        }
        takipGuncelle(afet, level);
        paketleriGonder(afet);
    }

    /** Afet durum paketi etkilenebilecek oyunculara; alandan çıkanlara bir kez "afet yok". ON_UYARI gizlidir. */
    private void paketleriGonder(Afet afet) {
        if (afet.asama() == AfetAsamasi.ON_UYARI) {
            return;
        }
        CompoundTag ozel = new CompoundTag();
        afet.istemciVerisiYaz(ozel);
        AfetDurumPaketi paket = AfetDurumPaketi.den(afet, zamanOlcegi.gercekTick(afet.asamaKalanTick()), ozel);
        Set<UUID> yeni = new HashSet<>();
        for (ServerPlayer oyuncu : etkilenebilecekOyuncular(afet)) {
            AgKanali.oyuncuyaGonder(oyuncu, paket);
            yeni.add(oyuncu.getUUID());
        }
        for (UUID eski : paketAlicilari) {
            if (!yeni.contains(eski)) {
                ServerPlayer oyuncu = server.getPlayerList().getPlayer(eski);
                if (oyuncu != null) {
                    AgKanali.oyuncuyaGonder(oyuncu, AfetDurumPaketi.YOK);
                }
            }
        }
        paketAlicilari = yeni;
    }

    private void sonlandir(Afet afet, ServerLevel level, BitisNedeni neden, boolean temiz) {
        DunyaDonemi donem = donem();
        long saat = donem.saat();
        sirenDurdur(afet);
        if (level != null) {
            try {
                afet.durdur(level, temiz);
            } catch (RuntimeException hata) {
                DogalAfetler.LOGGER.error("Afet durdurulurken hata: {}", afet.tur().kimlik(), hata);
            }
        }
        for (ServerPlayer oyuncu : cevrimIcindekiler(paketAlicilari)) {
            AgKanali.oyuncuyaGonder(oyuncu, AfetDurumPaketi.YOK);
        }
        paketAlicilari = new HashSet<>();
        donem.aktifAfetAyarla(null);

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
            donem.bekleme().afetBitti(afet.tur(), baglam.seviye(), saat, CekirdekAyarlari.al().zamanlayici());
            if (afet.tur().seviyeli()) {
                donem.sonSeviyeAyarla(baglam.seviye());
            }
            donem.setDirty();
        }
        DogalAfetler.LOGGER.info("Afet bitti: {} seviye {} neden {} etkilenen {} sağ çıkan {}", afet.tur().kimlik(),
                baglam.seviye(), neden, etkilenenler.size(), sagCikanlar.size());
        MinecraftForge.EVENT_BUS.post(new AfetBittiEvent(afet.tur(), baglam.seviye(), baglam.merkez(), baglam.boyut(),
                etkilenenler, sagCikanlar, baglam.baslangicSaati(), saat, neden));
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
        CompoundTag kayit = donem.bekleyenAfetiAl();
        if (kayit == null) {
            return;
        }
        Optional<AfetTuru> tur = Afet.turOku(kayit);
        Optional<Afet> afet = tur.flatMap(AfetKaydi::olustur);
        if (afet.isEmpty()) {
            DogalAfetler.LOGGER.warn("Kayıttaki aktif afet ({}) kayıtlı değil; atıldı", kayit.getString("tur"));
            donem.setDirty();
            return;
        }
        try {
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
        donem.aktifAfetAyarla(afet.get());
        afet.get().yuklendi(level);
        DogalAfetler.LOGGER.info("Aktif afet geri yüklendi: {} seviye {} aşama {} (aşamada {} tick)",
                afet.get().tur().kimlik(), afet.get().seviye(), afet.get().asama().kimlik(), afet.get().asamaGecenTick());
    }

    private void kapanis() {
        DunyaDonemi donem = donem();
        donem.aktifAfet().ifPresent(afet -> {
            ServerLevel level = server.getLevel(afet.baglam().boyut());
            if (level != null) {
                afet.kapanirken(level);
            }
        });
        DogalAfetler.LOGGER.info("Dünya dönemi (kapanış): {}", donem.ozet());
    }

    private void donemGonder(ServerPlayer oyuncu) {
        AgKanali.oyuncuyaGonder(oyuncu, DonemPaketi.den(donem(), zamanOlcegi.carpan()));
    }

    private void donemSenkronu(DunyaDonemi donem, CekirdekAyarlari ayar) {
        long gecen = tickSayaci - sonDonemPaketi;
        boolean tazele = gecen >= ayar.donemTazelemeTick();
        if (!tazele && !(donem.senkronGerekli() && (donem.acilSenkron() || gecen >= ayar.donemPaketAraligiTick()))) {
            return;
        }
        if (!server.getPlayerList().getPlayers().isEmpty()) {
            AgKanali.herkeseGonder(DonemPaketi.den(donem, zamanOlcegi.carpan()));
        }
        donem.senkronTamam();
        sonDonemPaketi = tickSayaci;
    }
}
