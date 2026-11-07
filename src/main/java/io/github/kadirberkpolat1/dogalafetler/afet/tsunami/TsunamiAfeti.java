package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.su.Cekilme;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.uyari.HacimEgrisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

/**
 * Tsunami (bölüm B, madde 10.4). Yaşam döngüsü çekirdeğindir (ön-uyarı, siren); AKTIF başlayınca kıyıda su görsel olarak
 * çekilir ({@link Cekilme}), {@code dalgaGecikmeSn} sonra açıkta alçak bir dalga doğar ve kıyıya yaklaştıkça yükselir
 * (sığlaşma, {@link TsunamiSeviyesi#siglasma}); kıyıya vurunca çekilme biter, cephe karada {@code karaGiris} blok ilerler,
 * önündeki şeridi yıkar ({@link TsunamiYikimi}) ve dalgaya açılan mağaraları doldurur ({@link SanalSu#magaraDoldur}). S3+
 * ikinci, %60 yükseklikte dalga gelir. Sürükleme, dalga hasarı ve çarpma hasarı sanal suyundur ({@code Dalga.ivme/hasar});
 * afet oyuncu sürüklendiği an {@link Ipucu#SURUKLENDI} yayınlar. AKTIF son dalga menzile varınca biter; BITIS dalgalar
 * sönünceye kadar sürer (geri çekilme). Mağara suyu doğal bitişte kalır ({@code magara_suyu} süreci), {@code stop} hepsini
 * kaldırır. Kimlikler kalıcıdır: dalga kayıttan aynı kimlikle döner, yeniden başlatılmaz (sanal su §A2).
 */
public final class TsunamiAfeti extends Afet {
    static final double IKINCI_PAY = 0.6;
    static final String KATMAN = "tsunami/ugultu";
    private static final int GUNCELLEME_ARALIGI = 10;

    /** Test ölçeği: küçük sahnede açıklık, karaya giriş, genişlik, dalga uzunluğu ve dalga gecikmesi (tick). */
    record Olcek(double aciklik, double karaGiris, double genislik, double uzunluk, long gecikme) {
    }

    private TsunamiGeometrisi geometri;
    private Olcek olcek;
    private long aktifBaslangic = -1;
    private long simdi;
    private double b1 = Double.NaN;
    private double b2 = Double.NaN;
    private double h1;
    private double h2;
    private double taban;
    private double cekilmeCarpani = 1;
    private int dalga1 = -1;
    private int dalga2 = -1;
    private int cekilme = -1;
    private double yikimSatir = Double.NaN;
    private boolean vurdu;
    private boolean dalgalarBitti;
    private boolean dogalBitti;
    private final List<Integer> magaralar = new ArrayList<>();
    private final List<BlockPos> magaraDenemeleri = new ArrayList<>();
    private final Set<UUID> suruklenenler = new HashSet<>();
    private final TsunamiYikimi yikim = new TsunamiYikimi();
    private final double[] itme = new double[2];
    private boolean yuklemeDenetimi;

    public TsunamiAfeti() {
        super(AfetTuru.TSUNAMI);
    }

    // ---------------------------------------------------------------- yer ve alan

    @Override
    public boolean yerUygunMu(ServerLevel level, BlockPos merkez, int seviye) {
        TsunamiAyarlari a = TsunamiAyarlari.al();
        geometri = KiyiAnalizi.analiz(level, merkez, a.aramaYaricapi.get(), a.enAzDenizSutunu.get()).orElse(null);
        if (geometri == null) {
            DogalAfetler.LOGGER.info("Tsunami: {} çevresinde {} blok içinde en az {} açık deniz sütunu yok (N14)",
                    merkez.toShortString(), a.aramaYaricapi.get(), a.enAzDenizSutunu.get());
        }
        return geometri != null;
    }

    /** GameTest: küçük sahnenin geometrisi ve ölçeği (başlatmadan önce). */
    void testIcin(TsunamiGeometrisi g, Olcek o) {
        geometri = g;
        olcek = o;
    }

    @Override
    public EtkiAlani etkiAlani() {
        BlockPos m = baglam().merkez();
        if (geometri == null) {
            return EtkiAlani.daire(m.getX() + 0.5, m.getY(), m.getZ() + 0.5, 96);
        }
        double orta = (karaGiris() - aciklik()) / 2;
        double r = Math.max((karaGiris() + aciklik()) / 2, genislik() / 2) + 16;
        return EtkiAlani.daire(geometri.x(orta, 0), taban, geometri.z(orta, 0), r);
    }

    private TsunamiSeviyesi sv() {
        return TsunamiSeviyesi.al(seviye());
    }

    private double aciklik() {
        return olcek != null ? olcek.aciklik() : sv().aciklik();
    }

    private double karaGiris() {
        return olcek != null ? olcek.karaGiris() : sv().karaGiris();
    }

    private double genislik() {
        return olcek != null ? olcek.genislik() : sv().genislik();
    }

    private double menzil() {
        return aciklik() + karaGiris();
    }

    private double cekilmeMesafesi() {
        return sv().cekilme() * cekilmeCarpani;
    }

    // ---------------------------------------------------------------- yaşam döngüsü

    @Override
    protected void baslarken(ServerLevel level) {
        if (geometri == null) {
            TsunamiAyarlari a = TsunamiAyarlari.al();
            geometri = KiyiAnalizi.analiz(level, baglam().merkez(), a.aramaYaricapi.get(), 1).orElseGet(() -> {
                BlockPos m = baglam().merkez();
                return new TsunamiGeometrisi(m.getX() + 0.5, m.getZ() + 0.5, 1, 0, level.getSeaLevel() - 1, 0);
            });
        }
        taban = geometri.taban();
        DogalAfetler.LOGGER.info("Tsunami: seviye {} kıyı ({}, {}) yön ({}, {}) taban {} deniz sütunu {}", seviye(),
                sayi(geometri.kiyiX()), sayi(geometri.kiyiZ()), sayi(geometri.yonX()), sayi(geometri.yonZ()), sayi(taban),
                geometri.denizSutunu());
    }

    @Override
    protected void asamaDegisti(ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni) {
        if (yeni == AfetAsamasi.AKTIF && aktifBaslangic < 0) {
            aktifBasla(level);
        }
    }

    private void aktifBasla(ServerLevel level) {
        TsunamiSeviyesi sv = sv();
        TsunamiAyarlari a = TsunamiAyarlari.al();
        DunyaDonemi donem = DunyaDonemi.al(level);
        simdi = donem.saat();
        aktifBaslangic = simdi;
        cekilmeCarpani = donem.carpan(AfetTuru.TSUNAMI) > 1.0001 ? a.ayCekilmeCarpani.get() : 1.0;
        taban = geometri.taban() + Math.floor(donem.deniz().mevcutArtis());
        double mesafe = cekilmeMesafesi();
        // Çekilmenin başında ve sonunda istemci çember içindeki bütün su bölümlerini yeniden derler; sonu dalganın kıyıya
        // vurduğu ana, yani S5 yıkımının başına denk gelir (TS-12): yarıçapın cephe genişliği payı %30'da tutulur.
        cekilme = SanalSu.cekilmeBaslat(level, new Cekilme(geometri.x(-mesafe / 2, 0), geometri.z(-mesafe / 2, 0),
                Math.max(mesafe * 1.5, genislik() * 0.3), sv.cekilmeDerinligi(cekilmeCarpani), simdi,
                Math.round(a.cekilmeSn.get() * 20)));
        long gecikme = olcek != null ? olcek.gecikme() : Math.round(a.dalgaGecikmeSn.get() * 20);
        b1 = simdi + gecikme;
        h1 = sv.yukseklik() * TsunamiSeviyesi.siglasma(1);
        dalga1 = SanalSu.dalgaBaslat(level, dalga(b1, h1), baglam().baslangicSaati());
        yikimSatir = -Math.min(mesafe, aciklik());
        sabitle(level);
        DogalAfetler.LOGGER.info("Tsunami: çekilme #{} ({} blok, ay çarpanı {}), dalga #{} yükseklik {} hız {} blok/sn genişlik {} "
                + "açıklık {} karaya giriş {}", cekilme, sayi(mesafe), sayi(cekilmeCarpani), dalga1, sayi(sv.yukseklik()),
                sayi(sv.hizBlokSn()), sayi(genislik()), sayi(aciklik()), sayi(karaGiris()));
    }

    private Dalga dalga(double baslangic, double yukseklik) {
        TsunamiSeviyesi sv = sv();
        double uzunluk = olcek != null ? olcek.uzunluk() : sv.uzunluk();
        return new Dalga(geometri.x(-aciklik(), 0), geometri.z(-aciklik(), 0), geometri.yonX(), geometri.yonZ(), baslangic,
                sv.hizTick(), yukseklik, genislik(), uzunluk, taban, menzil(), sv.ivme(), sv.hasar());
    }

    /** Cephenin kıyı çizgisine göre konumu (artı: karada); dalga henüz doğmadıysa -∞. */
    private double cephe(double baslangic) {
        if (Double.isNaN(baslangic) || simdi < baslangic) {
            return Double.NEGATIVE_INFINITY;
        }
        return Math.min((simdi - baslangic) * sv().hizTick(), menzil()) - aciklik();
    }

    /** Son dalganın menzile vardığı ölçekli saat. */
    private double sonVaris() {
        double sure = menzil() / sv().hizTick();
        if (!Double.isNaN(b2)) {
            return b2 + sure;
        }
        return b1 + (sv().ikinciDalga() ? 2 * sure : sure);
    }

    @Override
    protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
        if (aktifBaslangic < 0 || (asama != AfetAsamasi.AKTIF && asama != AfetAsamasi.BITIS)) {
            return;
        }
        simdi = DunyaDonemi.al(level).saat();
        if (yuklemeDenetimi) {
            yuklemeDenetimi = false;
            DogalAfetler.LOGGER.info("Tsunami: kayıttan sürüyor, dalga #{} var={}, ikinci #{} var={}", dalga1,
                    dalga1 > 0 && SanalSu.dalgaVarMi(level, dalga1), dalga2, dalga2 > 0 && SanalSu.dalgaVarMi(level, dalga2));
        }
        TsunamiSeviyesi sv = sv();
        if (asama == AfetAsamasi.AKTIF && sv.ikinciDalga() && dalga2 < 0 && simdi >= b1 + menzil() / sv.hizTick()) {
            b2 = b1 + menzil() / sv.hizTick();
            h2 = sv.yukseklik() * IKINCI_PAY * TsunamiSeviyesi.siglasma(1);
            dalga2 = SanalSu.dalgaBaslat(level, dalga(b2, h2), baglam().baslangicSaati() + 1);
            DogalAfetler.LOGGER.info("Tsunami: ikinci dalga #{} (yükseklik {})", dalga2, sayi(sv.yukseklik() * IKINCI_PAY));
        }
        if (level.getGameTime() % GUNCELLEME_ARALIGI == 0) {
            h1 = siglas(level, dalga1, b1, h1, sv.yukseklik());
            h2 = siglas(level, dalga2, b2, h2, sv.yukseklik() * IKINCI_PAY);
        }
        double on = Math.max(cephe(b1), cephe(b2));
        if (cekilme > 0 && cephe(b1) >= 0) {
            SanalSu.cekilmeBitir(level, cekilme);
            cekilme = -1;
        }
        if (!vurdu && on >= 0) {
            vurdu = true;
            cepheSesi(level, TsunamiSesleri.CARPMA.get(), 6f, 0.85f, 0.95f, 1.05f);
            cepheSesi(level, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 4f, 0.6f, 0.6f, 0.6f);
            DogalAfetler.LOGGER.info("Tsunami: dalga kıyıya vurdu (yükseklik {})", sayi(h1));
        }
        yik(level, sv, on);
        surukle(level);
        sesler(level, on);
        altAsamaAyarla(dalga2 > 0 ? 3 : on >= 0 ? 2 : simdi >= b1 ? 1 : 0);
        dalgalarBitti = !(dalga1 > 0 && SanalSu.dalgaVarMi(level, dalga1)) && !(dalga2 > 0 && SanalSu.dalgaVarMi(level, dalga2));
    }

    /** Sığlaşma: yeni yükseklik en az 0,4 blok değişmişse dalgayı günceller (paket); güncel yükseklik. */
    private double siglas(ServerLevel level, int id, double baslangic, double h, double tepe) {
        if (id <= 0 || Double.isNaN(baslangic)) {
            return h;
        }
        double kalan = Math.max(0, -cephe(baslangic)) / aciklik();
        double hedef = tepe * TsunamiSeviyesi.siglasma(kalan);
        if (Math.abs(hedef - h) < 0.4 || !SanalSu.dalgaGuncelle(level, id, dalga(baslangic, hedef))) {
            return h;
        }
        return hedef;
    }

    private void yik(ServerLevel level, TsunamiSeviyesi sv, double on) {
        if (Double.isNaN(yikimSatir)) {
            yikimSatir = -Math.min(cekilmeMesafesi(), aciklik());
        }
        double hedef = Math.min(on + 3, karaGiris());
        int ust = (int) Math.ceil(taban + Math.max(h1, h2)) + 1;
        while (yikimSatir + 0.7 <= hedef) {
            yikimSatir += 0.7;
            yikim.satirEkle(geometri, yikimSatir, genislik(), ust);
        }
        TsunamiAyarlari a = TsunamiAyarlari.al();
        double kisma = TpsOlcer.kismaCarpani();
        int butce = Math.max(1, (int) Math.round(sv.yikimButcesi() * a.yikimCarpani.get() * kisma));
        int hacim = Math.min(a.magaraHacmi.get() * seviye(), SuAyarlari.sunucu().magaraMaksHacim.get());
        long kalis = SuAyarlari.sunucu().magaraKalisSn.get() * 20L;
        int bolum = Math.max(1, (int) Math.round(a.yikimBolumTick.get() * kisma));
        yikim.tick(level, simdi, sv, taban, kisma, butce, bolum, a.yikimChunkTick.get(), a.yikimEfektTick.get(),
                Kurallar.blokHasariAcik(level.getGameRules()),
                p -> magaraDene(level, sv, p, hacim, kalis));
    }

    /** Mağara ağzı adayı: öncekilere 12 bloktan yakın değilse doldurur. Daha aday isteniyorsa true. */
    private boolean magaraDene(ServerLevel level, TsunamiSeviyesi sv, BlockPos p, int hacim, long kalis) {
        if (magaralar.size() >= sv.magaraSayisi()) {
            return false;
        }
        for (BlockPos q : magaraDenemeleri) {
            if (q.distSqr(p) < 144) {
                return true;
            }
        }
        magaraDenemeleri.add(p);
        int id = SanalSu.magaraDoldur(level, p, hacim, kalis);
        if (id > 0) {
            magaralar.add(id);
            DogalAfetler.LOGGER.info("Tsunami: mağara #{} dolduruldu, ağız {} (en çok {} hücre, kalış {} tick)", id,
                    p.toShortString(), hacim, kalis);
        }
        return magaralar.size() < sv.magaraSayisi();
    }

    /** Dalga oyuncuyu sürüklediği an (etki alanında, ilk kez) başarım ipucu (#2 Dalga Avcısı, başarım §A.3). */
    private void surukle(ServerLevel level) {
        EtkiAlani alan = etkiAlani();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || suruklenenler.contains(p.getUUID()) || !alan.iceriyor(p.getX(), p.getY(), p.getZ(), 0)) {
                continue;
            }
            if (SanalSu.itmeHesapla(p, itme) != null) {
                suruklenenler.add(p.getUUID());
                MinecraftForge.EVENT_BUS.post(new BasarimIpucuEvent(p, Ipucu.SURUKLENDI));
                DogalAfetler.LOGGER.info("Tsunami: {} dalgada sürükleniyor (SURUKLENDI)", p.getGameProfile().getName());
            }
        }
    }

    /** Çekilmede çırpınan balık ve damlama; yaklaşan dalganın uğultusu cephenin ortasında döngülü ortam katmanı. */
    private void sesler(ServerLevel level, double on) {
        long t = level.getGameTime();
        if (on < 0 && t % 30 == 0) {
            double b = -cekilmeMesafesi() * (0.2 + 0.6 * level.getRandom().nextDouble());
            double y = (genislik() / 2) * (level.getRandom().nextDouble() * 2 - 1);
            double x = geometri.x(b, y);
            double z = geometri.z(b, y);
            level.playSound(null, x, taban, z, SoundEvents.COD_FLOP, SoundSource.AMBIENT, 1f, 0.9f);
            level.playSound(null, x, taban + 1, z, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.AMBIENT, 1f, 1f);
        }
        ugultu(level, on);
    }

    /**
     * Uğultu katmanı (uyarı-ses §A3): dalga doğduğu an başlar, açıklığı geçerken %35'ten tama çıkar (sığlaşmayla), cepheyle
     * 4 Hz taşınır, dalgalar sönünce 3 sn'de söner. Sunucu yeniden açılınca katman bellekte olmadığı için kaldığı yerden
     * yeniden kurulur.
     */
    private void ugultu(ServerLevel level, double on) {
        boolean suruyor = on > Double.NEGATIVE_INFINITY && !dalgalarBitti;
        if (!suruyor) {
            if (OrtamSesi.aktifMi(KATMAN)) {
                OrtamSesi.durdur(level.getServer(), KATMAN, 60);
            }
            return;
        }
        Vec3 nokta = new Vec3(geometri.x(on, 0), taban + 2, geometri.z(on, 0));
        if (!OrtamSesi.aktifMi(KATMAN)) {
            int kiyiya = (int) Math.ceil(aciklik() / sv().hizTick());
            int gecen = (int) Math.max(0, simdi - b1);
            HacimEgrisi egri = gecen >= kiyiya ? HacimEgrisi.sabit(1f)
                    : HacimEgrisi.of(new HacimEgrisi.Nokta(0, 0.35f + 0.65f * gecen / kiyiya),
                            new HacimEgrisi.Nokta(kiyiya - gecen, 1f));
            OrtamSesi.baslat(level.getServer(), KATMAN, baglam().baslangicSaati(), TsunamiSesleri.UGULTU, SoundSource.WEATHER,
                    OrtamSesi.Yer.nokta(level.dimension(), nokta, (float) (96 + genislik() / 2)), egri);
        } else if (level.getGameTime() % 5 == 0) {
            OrtamSesi.konumGuncelle(level.getServer(), KATMAN, nokta);
        }
    }

    private void cepheSesi(ServerLevel level, SoundEvent ses, float hacim, float... perdeler) {
        double on = Math.max(cephe(b1), cephe(b2));
        for (int i = 0; i < perdeler.length; i++) {
            double yan = (i - (perdeler.length - 1) / 2.0) * genislik() / 3;
            level.playSound(null, geometri.x(on, yan), taban + 2, geometri.z(on, yan), ses, SoundSource.WEATHER, hacim,
                    perdeler[i]);
        }
    }

    /**
     * Oyuncusuz da hesaplanacak deniz alanı: kıyı ±32 blok. Dalga tabanı canlı zeminden gelir (SU-36), çekilme istemcide
     * hesaplanır; çekilmenin tamamını (S5'te ~600 chunk) sabitlemek sunucu tick'ine pahalıydı.
     */
    private void sabitle(ServerLevel level) {
        double m = 32;
        double x = geometri.kiyiX();
        double z = geometri.kiyiZ();
        SanalSu.bolgeSabitle(level, new ChunkPos(BlockPos.containing(x - m, 0, z - m)),
                new ChunkPos(BlockPos.containing(x + m, 0, z + m)));
    }

    @Override
    protected boolean aktifBitti() {
        return aktifBaslangic >= 0 && simdi >= sonVaris();
    }

    @Override
    public long aktifKalanTick() {
        return aktifBaslangic < 0 ? -1 : Math.max(0, (long) Math.ceil(sonVaris() - simdi));
    }

    @Override
    protected boolean bitisTamam(long gecen) {
        if (dalgalarBitti || gecen > 2400) {
            dogalBitti = true;
            return true;
        }
        return false;
    }

    /**
     * {@code /dogalafet asama ileri} AKTIF'te (N18): çekilme ya da yaklaşma sırasında dalgayı şimdi kıyıya getirir; karadaysa ve
     * ikinci dalga gelecekse onu kıyıda başlatır; sonra BITIS.
     */
    @Override
    protected boolean altAsamaIleri(ServerLevel level) {
        if (aktifBaslangic < 0 || geometri == null) {
            return false;
        }
        simdi = DunyaDonemi.al(level).saat();
        TsunamiSeviyesi sv = sv();
        double kiyiya = aciklik() / sv.hizTick();
        if (cephe(b1) < 0) {
            b1 = simdi - kiyiya;
            h1 = sv.yukseklik();
            SanalSu.dalgaGuncelle(level, dalga1, dalga(b1, h1));
            altAsamaAyarla(2);
            return true;
        }
        if (sv.ikinciDalga() && dalga2 < 0) {
            b2 = simdi - kiyiya;
            h2 = sv.yukseklik() * IKINCI_PAY;
            dalga2 = SanalSu.dalgaBaslat(level, dalga(b2, h2), baglam().baslangicSaati() + 1);
            altAsamaAyarla(3);
            return true;
        }
        return false;
    }

    @Override
    protected void temizle(ServerLevel level, boolean temiz) {
        int bekleyen = yikim.bekleyen();
        yikim.temizle();
        if (cekilme > 0) {
            SanalSu.cekilmeBitir(level, cekilme);
        }
        if (temiz) {
            for (int id : new int[] {dalga1, dalga2}) {
                if (id > 0) {
                    SanalSu.dalgaBitir(level, id);
                }
            }
            if (!dogalBitti) {
                for (int id : magaralar) {
                    SanalSu.magaraBitir(level, id);
                }
            }
        }
        SanalSu.bolgeBirak(level);
        OrtamSesi.durdur(level.getServer(), KATMAN, temiz ? 20 : 60);
        var d = SanalSu.dunya(level);
        DogalAfetler.LOGGER.info("Tsunami: temizlendi (temiz={}, doğal={}): kırılan {} blok, değişen bölüm {}, taranan {} sütun, "
                + "bekleyen yıkım {}, mağara {}; boyutta kalan dalga {}, çekilme {}, mağara {}", temiz, dogalBitti,
                yikim.toplamKirilan(), yikim.toplamBolum(), yikim.toplamTaranan(), bekleyen, magaralar.size(),
                d == null ? 0 : d.dalgalar().size(), d == null ? 0 : d.cekilmeler().size(), d == null ? 0 : d.magaralar().size());
    }

    /** Sanal su dalgaları bundan sonra ({@code ServerStartedEvent}) kayıttan döner: süreklilik ilk tick'te log'lanır. */
    @Override
    protected void yuklendi(ServerLevel level) {
        if (aktifBaslangic >= 0 && geometri != null) {
            sabitle(level);
        }
        yuklemeDenetimi = true;
        DogalAfetler.LOGGER.info("Tsunami: kayıttan yüklendi, dalga #{}, ikinci #{}, çekilme #{}", dalga1, dalga2, cekilme);
    }

    @Override
    protected void istemciVerisiYaz(CompoundTag t) {
        if (geometri != null) {
            t.putFloat("yx", (float) geometri.yonX());
            t.putFloat("yz", (float) geometri.yonZ());
            t.putFloat("kx", (float) geometri.kiyiX());
            t.putFloat("kz", (float) geometri.kiyiZ());
            t.putFloat("tb", (float) taban);
            t.putBoolean("yagmur", aktifBaslangic >= 0);
        }
    }

    // ---------------------------------------------------------------- kayıt

    @Override
    protected void ozelDurumYaz(CompoundTag t) {
        if (geometri != null) {
            CompoundTag g = new CompoundTag();
            g.putDouble("kx", geometri.kiyiX());
            g.putDouble("kz", geometri.kiyiZ());
            g.putDouble("yx", geometri.yonX());
            g.putDouble("yz", geometri.yonZ());
            g.putDouble("taban", geometri.taban());
            g.putInt("deniz", geometri.denizSutunu());
            t.put("geometri", g);
        }
        if (olcek != null) {
            t.putDouble("oAcik", olcek.aciklik());
            t.putDouble("oKara", olcek.karaGiris());
            t.putDouble("oGenis", olcek.genislik());
            t.putDouble("oUzun", olcek.uzunluk());
            t.putLong("oGecikme", olcek.gecikme());
        }
        t.putLong("aktif", aktifBaslangic);
        t.putDouble("b1", b1);
        t.putDouble("b2", b2);
        t.putDouble("h1", h1);
        t.putDouble("h2", h2);
        t.putDouble("taban", taban);
        t.putDouble("carpan", cekilmeCarpani);
        t.putInt("dalga1", dalga1);
        t.putInt("dalga2", dalga2);
        t.putInt("cekilme", cekilme);
        t.putDouble("yikim", yikimSatir);
        t.putBoolean("vurdu", vurdu);
        t.putIntArray("magaralar", magaralar);
    }

    @Override
    protected void ozelDurumOku(CompoundTag t) {
        if (t.contains("geometri")) {
            CompoundTag g = t.getCompound("geometri");
            geometri = new TsunamiGeometrisi(g.getDouble("kx"), g.getDouble("kz"), g.getDouble("yx"), g.getDouble("yz"),
                    g.getDouble("taban"), g.getInt("deniz"));
        }
        if (t.contains("oAcik")) {
            olcek = new Olcek(t.getDouble("oAcik"), t.getDouble("oKara"), t.getDouble("oGenis"), t.getDouble("oUzun"),
                    t.getLong("oGecikme"));
        }
        aktifBaslangic = t.contains("aktif") ? t.getLong("aktif") : -1;
        simdi = Math.max(aktifBaslangic, 0);
        b1 = t.contains("b1") ? t.getDouble("b1") : Double.NaN;
        b2 = t.contains("b2") ? t.getDouble("b2") : Double.NaN;
        h1 = t.getDouble("h1");
        h2 = t.getDouble("h2");
        taban = t.getDouble("taban");
        cekilmeCarpani = t.contains("carpan") ? t.getDouble("carpan") : 1;
        dalga1 = t.contains("dalga1") ? t.getInt("dalga1") : -1;
        dalga2 = t.contains("dalga2") ? t.getInt("dalga2") : -1;
        cekilme = t.contains("cekilme") ? t.getInt("cekilme") : -1;
        yikimSatir = t.contains("yikim") ? t.getDouble("yikim") : Double.NaN;
        vurdu = t.getBoolean("vurdu");
        magaralar.clear();
        for (int id : t.getIntArray("magaralar")) {
            magaralar.add(id);
        }
    }

    // ---------------------------------------------------------------- test okumaları

    int dalga1() {
        return dalga1;
    }

    int dalga2() {
        return dalga2;
    }

    int cekilmeKimligi() {
        return cekilme;
    }

    List<Integer> magaralar() {
        return magaralar;
    }

    long kirilan() {
        return yikim.toplamKirilan();
    }

    TsunamiGeometrisi geometri() {
        return geometri;
    }

    private static String sayi(double d) {
        return String.format(java.util.Locale.ROOT, "%.2f", d);
    }
}
