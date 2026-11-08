package io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;

/**
 * Asit yağmuru (bölüm E, madde 10, tasarım notu {@code docs/tasarim/asit_yagmuru.md}). AKTIF iken bütün dünyada normal
 * yağmur gibi yağar (AF2-01): etki alanı {@link EtkiAlani#herYer()} (siren, durum paketi ve "etkilenen" herkese), sunucu
 * vanilla yağmurunu açıp tutar ve bitişte önceki havayı geri yükler ({@link AsitHavasi}). Yağmur 10 sn'de yoğunlaşır, son
 * 15 sn'de seyrelir; sürdükçe başı gökyüzüne açık her oyuncu saniyede {@code hasarSaniye} hasar alır (seviyeden bağımsız;
 * can {@code asitMinimumCan} altına inmez; çatı altı hiç hasar yok). Her oyuncunun çevresindeki yüklü chunk'larda gökyüzünü
 * gören yüzey blokları bütçeyle örneklenir, önce 3–5 sn çatlak örtüsü taşır, sonra dönüşür ({@code afetBlokHasari=false}
 * iken hiç aşınma yok). Hedef oyuncu yalnız komut uyumluluğu için kalır, etkisi yoktur. Yağmur, sis ve dumanlar istemcide
 * {@code client.asit_yagmuru} tarafından bu afetin durum paketinden her biyomda (çöl ve kar dahil) çizilir; afet ses çalmaz,
 * sireni çekirdek çalar.
 */
public final class AsitYagmuru extends Afet {
    private static final int HASAR_ADIMI = 4;
    /** Kırılma örtüsü kimlikleri negatif: oyuncu varlık kimlikleriyle çakışmaz (vanilla kendi kırdığını o oyuncuya göndermez). */
    private static final int KIMLIK_TABANI = -0x41534900;
    private static final int KIMLIK_ARALIGI = 4096;
    private static final int SES_TAVANI = 4;
    /** Örneklerin yarısının seçildiği oyuncu çevresi (blok): değişim görenin önünde olur. */
    private static final double YAKIN_ORNEK = 24;
    private static final int ISTEMCI_ASINMA_TAVANI = 32;

    private double birikim;
    private int sonrakiKimlik;
    private int asinanToplam;
    private final List<Asinma> asinanlar = new ArrayList<>();
    private final Map<UUID, Integer> maruziyet = new HashMap<>();
    /** Son hasar denetiminden beri ilerleyen ölçekli tick (K-C11): {@code /dogalafet hiz} maruziyeti de hızlandırır. */
    private int bekleyenAdim;
    private Double yogunlukZorlamasi;
    /** AKTIF'e geçerken saklanan vanilla hava durumu; asit havayı devralmadıysa {@code null} (kayıtta). */
    private CompoundTag oncekiHava;
    /** Son "vanilla havası geri alındı" log'unun oyun saati (log sel olmasın; kalıcı değil). */
    private long sonGeriAlmaLogu = -200;

    public AsitYagmuru() {
        super(AfetTuru.ASIT_YAGMURU);
    }

    private AsitKurallari.Seviye ayar() {
        return AsitAyarlari.tablo().seviye(seviye());
    }

    @Override
    public EtkiAlani etkiAlani() {
        return EtkiAlani.herYer();
    }

    @Override
    protected void asamaDegisti(ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni) {
        if (yeni == AfetAsamasi.AKTIF) {
            AsitKurallari.Seviye s = ayar();
            DogalAfetler.LOGGER.info("asit yağmuru: seviye {} her yerde, {} sn, oyuncu başına {} blok/sn aşınma ({} blok çevre, "
                    + "tick bütçesi {}), {} can/sn ({})", seviye(), s.sureTick() / 20, s.asindirmaSaniye(), s.yaricap(),
                    AsitAyarlari.blokButcesi(), AsitAyarlari.hasarSaniye(),
                    Kurallar.blokHasariAcik(level.getGameRules()) ? "blok hasarı açık" : "blok hasarı kapalı");
            havayiTut(level);
        }
    }

    @Override
    protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
        if (asama != AfetAsamasi.AKTIF) {
            return;
        }
        havayiTut(level);
        double yogunluk = yogunluk();
        if (yogunluk > 0) {
            bekleyenAdim += adim;
            if (level.getGameTime() % HASAR_ADIMI == 0) {
                oyunculariDenetle(level, bekleyenAdim);
                bekleyenAdim = 0;
            }
        }
        if (yogunluk >= AsitKurallari.ETKI_ESIGI && Kurallar.blokHasariAcik(level.getGameRules())) {
            asindir(level, yogunluk, adim);
        }
        if (!Kurallar.blokHasariAcik(level.getGameRules())) {
            asinanlariBirak(level);
        }
        asinanlariIlerlet(level);
    }

    @Override
    protected boolean aktifBitti() {
        return asamaGecenTick() >= ayar().sureTick();
    }

    @Override
    public long aktifKalanTick() {
        return Math.max(0, ayar().sureTick() - asamaGecenTick());
    }

    @Override
    protected void temizle(ServerLevel level, boolean temiz) {
        int kalan = asinanlar.size();
        asinanlariBirak(level);
        maruziyet.clear();
        bekleyenAdim = 0;
        if (oncekiHava != null) {
            AsitHavasi.geriYukle(level, oncekiHava);
            DogalAfetler.LOGGER.info("asit yağmuru: önceki hava geri yüklendi ({})", AsitHavasi.ozet(oncekiHava));
            oncekiHava = null;
        }
        DogalAfetler.LOGGER.info("asit yağmuru bitti: seviye {} {} blok aşındı, {} çatlak örtüsü kaldırıldı", seviye(),
                asinanToplam, kalan);
    }

    @Override
    protected void ozelDurumYaz(CompoundTag etiket) {
        etiket.putDouble("birikim", birikim);
        etiket.putInt("toplam", asinanToplam);
        if (oncekiHava != null) {
            etiket.put("hava", oncekiHava.copy());
        }
    }

    @Override
    protected void ozelDurumOku(CompoundTag etiket) {
        birikim = etiket.getDouble("birikim");
        asinanToplam = etiket.getInt("toplam");
        oncekiHava = etiket.contains("hava") ? etiket.getCompound("hava").copy() : null;
    }

    /** {@code y}: yağmur yoğunluğu 0–1; {@code a}: şu an aşınan bloklar (istemcide yoğun cızlama dumanları). */
    @Override
    protected void istemciVerisiYaz(CompoundTag etiket) {
        etiket.putFloat("y", asama() == AfetAsamasi.AKTIF ? (float) yogunluk() : 0f);
        int n = Math.min(ISTEMCI_ASINMA_TAVANI, asinanlar.size());
        long[] konumlar = new long[n];
        for (int i = 0; i < n; i++) {
            konumlar[i] = asinanlar.get(i).konum.asLong();
        }
        etiket.putLongArray("a", konumlar);
    }

    double yogunluk() {
        if (yogunlukZorlamasi != null) {
            return yogunlukZorlamasi;
        }
        return asama() == AfetAsamasi.AKTIF ? AsitKurallari.yogunluk(asamaGecenTick(), ayar().sureTick()) : 0;
    }

    /** GameTest: 10 sn'lik yoğunlaşmayı beklemeden yoğunluğu sabitler ({@code null} eğriye döner). */
    void yogunlukZorla(Double yogunluk) {
        yogunlukZorlamasi = yogunluk;
    }

    /** GameTest: çatlak örtüsü taşıyan (dönüşmeyi bekleyen) blok sayısı. */
    int bekleyenAsinma() {
        return asinanlar.size();
    }

    /**
     * Hava ilk kez saklanır (kayıttan dönen eski afette de), sonra vanilla yağmuru açık tutulur; vanilla havası yağmuru
     * kesmişse ({@code /weather clear}, döngü) geri alınır ve 10 sn'de en çok bir satır yazılır.
     */
    private void havayiTut(ServerLevel level) {
        boolean ilk = oncekiHava == null;
        if (ilk) {
            oncekiHava = AsitHavasi.sakla(level);
            if (oncekiHava == null) {
                return;
            }
            DogalAfetler.LOGGER.info("asit yağmuru: vanilla yağmuru açıldı (önceki hava {})", AsitHavasi.ozet(oncekiHava));
        }
        if (AsitHavasi.tut(level) == AsitHavasi.Sonuc.GERI_ALINDI && !ilk
                && level.getGameTime() - sonGeriAlmaLogu >= 200) {
            sonGeriAlmaLogu = level.getGameTime();
            DogalAfetler.LOGGER.info("asit yağmuru: vanilla havası yağmuru kesmişti, yağmur yeniden açıldı");
        }
    }

    /**
     * Hasar koşulu (AF2-03): yaratıcı, izleyici ya da ölü değil ve başının üstü gökyüzüne açık; yağmur her yerde yağdığından
     * konum koşulu yok (çöl ve kar biyomu dahil). Oyuncunun chunk'ı henüz yüklenmediyse (ışınlanma) bu denetim atlanır.
     */
    static boolean maruzMu(ServerLevel level, ServerPlayer oyuncu) {
        if (oyuncu.isSpectator() || oyuncu.isCreative() || !oyuncu.isAlive() || !level.isLoaded(oyuncu.blockPosition())) {
            return false;
        }
        int harita = level.getHeight(Heightmap.Types.MOTION_BLOCKING, oyuncu.getBlockX(), oyuncu.getBlockZ());
        return AsitKurallari.acikta(harita, oyuncu.getEyeY());
    }

    /**
     * Başının üstü gökyüzüne açık her oyuncu açıkta geçirdiği her saniye {@code hasarSaniye} hasar alır; hasar anında başarım
     * ipucu (Çatı Altı'nı bozar). Maruziyet {@code adim} ölçekli tick kadar birikir ({@link AsitKurallari#maruziyet}).
     */
    private void oyunculariDenetle(ServerLevel level, int adim) {
        float hasar = AsitAyarlari.hasarSaniye();
        for (ServerPlayer oyuncu : level.players()) {
            if (!maruzMu(level, oyuncu)) {
                continue;
            }
            AsitKurallari.Maruziyet m = AsitKurallari.maruziyet(maruziyet.getOrDefault(oyuncu.getUUID(), 0), adim);
            maruziyet.put(oyuncu.getUUID(), m.sayac());
            if (!m.hasar()) {
                continue;
            }
            float miktar = AsitKurallari.hasarMiktari(oyuncu.getHealth(), hasar, AsitAyarlari.minimumCan());
            if (miktar > 0 && oyuncu.hurt(AsitKayitlari.hasar(level), miktar)) {
                MinecraftForge.EVENT_BUS.post(new BasarimIpucuEvent(oyuncu, Ipucu.HASAR_ALDI));
            }
        }
    }

    /**
     * Aşınma oyuncuların (izleyici ve yaratıcı hariç; AF2-05) çevresinden örneklenir: her örnek rastgele bir oyuncu seçer,
     * yarısı onun 24 blok, yarısı seviyenin yarıçapı içinden; yüklü olmayan sütun atlanır (chunk yüklenmez). Tick bütçesi
     * {@code blokButcesi} × TPS kısması; aşınabilir blok hız birikimi el verdikçe çatlak kuyruğuna girer.
     */
    private void asindir(ServerLevel level, double yogunluk, int adim) {
        List<ServerPlayer> oyuncular = level.players().stream().filter(o -> !o.isSpectator() && !o.isCreative()).toList();
        if (oyuncular.isEmpty()) {
            return;
        }
        AsitKurallari.Seviye s = ayar();
        double kisma = TpsOlcer.kismaCarpani();
        int n = oyuncular.size();
        birikim = AsitKurallari.birikimEkle(birikim, s.asindirmaSaniye(), n, yogunluk, kisma, adim);
        int ornek = AsitKurallari.ornekButcesi(s.asindirmaSaniye(), kisma, n, AsitAyarlari.blokButcesi());
        int tavan = AsitAyarlari.eszamanliAsindirma();
        RandomSource r = level.getRandom();
        for (int i = 0; i < ornek && birikim >= 1 && asinanlar.size() < tavan; i++) {
            ServerPlayer goren = oyuncular.get(r.nextInt(n));
            double menzil = r.nextBoolean() ? Math.min(YAKIN_ORNEK, s.yaricap()) : s.yaricap();
            double aci = r.nextDouble() * Math.PI * 2;
            double uzaklik = Math.sqrt(r.nextDouble()) * menzil;
            BlockPos aday = AsitAsindirici.aday(level, Mth.floor(goren.getX() + Math.cos(aci) * uzaklik),
                    Mth.floor(goren.getZ() + Math.sin(aci) * uzaklik), s.tasAsinir());
            if (aday == null
                    || r.nextDouble() >= AsitKurallari.kabulOlasiligi(AsitAsindirici.sinif(level.getBlockState(aday)))) {
                continue;
            }
            if (kuyrugaAl(level, aday)) {
                birikim -= 1;
            }
        }
    }

    /** Blok 3–5 sn'lik çatlak örtüsüyle aşınma kuyruğuna girer; zaten sıradaysa, aşınmazsa ya da kuyruk doluysa {@code false}. */
    boolean kuyrugaAl(ServerLevel level, BlockPos konum) {
        BlockState durum = level.getBlockState(konum);
        if (asinanlar.size() >= AsitAyarlari.eszamanliAsindirma() || sirada(konum)
                || AsitAsindirici.donusum(durum, ayar().tasAsinir()) == AsitKurallari.Donusum.YOK) {
            return false;
        }
        RandomSource r = level.getRandom();
        int sure = AsitKurallari.CATLAK_EN_AZ + r.nextInt(AsitKurallari.CATLAK_EN_COK - AsitKurallari.CATLAK_EN_AZ + 1);
        Asinma a = new Asinma(konum.immutable(), KIMLIK_TABANI - sonrakiKimlik, level.getGameTime(), sure, durum);
        sonrakiKimlik = (sonrakiKimlik + 1) % KIMLIK_ARALIGI;
        asinanlar.add(a);
        level.destroyBlockProgress(a.kimlik, a.konum, 0);
        return true;
    }

    private boolean sirada(BlockPos konum) {
        for (Asinma a : asinanlar) {
            if (a.konum.equals(konum)) {
                return true;
            }
        }
        return false;
    }

    /** Çatlak örtüsü ilerler; süresi dolan blok dönüşür, değişen chunk sanal suya bildirilir. */
    private void asinanlariIlerlet(ServerLevel level) {
        if (asinanlar.isEmpty()) {
            return;
        }
        long simdi = level.getGameTime();
        RandomSource r = level.getRandom();
        LongSet degisen = new LongOpenHashSet();
        int ses = 0;
        for (Iterator<Asinma> it = asinanlar.iterator(); it.hasNext();) {
            Asinma a = it.next();
            if (!level.isLoaded(a.konum)) {
                it.remove();
                continue;
            }
            if (!level.getBlockState(a.konum).is(a.durum.getBlock())) {
                level.destroyBlockProgress(a.kimlik, a.konum, -1);
                it.remove();
                continue;
            }
            long gecen = simdi - a.baslangic;
            if (gecen < a.sure) {
                if (gecen % 5 == 0) {
                    level.destroyBlockProgress(a.kimlik, a.konum, AsitKurallari.catlakAsamasi(gecen, a.sure));
                }
                continue;
            }
            level.destroyBlockProgress(a.kimlik, a.konum, -1);
            it.remove();
            if (AsitAsindirici.uygula(level, a.konum, ayar().tasAsinir())) {
                degisen.add(ChunkPos.asLong(a.konum.getX() >> 4, a.konum.getZ() >> 4));
                asinanToplam++;
                if (ses++ < SES_TAVANI) {
                    level.playSound(null, a.konum, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25F,
                            1.4F + r.nextFloat() * 0.4F);
                }
            }
        }
        for (long chunk : degisen) {
            SanalSu.chunkDegisti(level, new ChunkPos(chunk));
        }
    }

    /** Kuyruktaki blokların çatlak örtüsü kalkar, bloklar olduğu gibi kalır (stop, blok hasarı kapandı). */
    private void asinanlariBirak(ServerLevel level) {
        for (Asinma a : asinanlar) {
            level.destroyBlockProgress(a.kimlik, a.konum, -1);
        }
        asinanlar.clear();
        birikim = 0;
    }

    private record Asinma(BlockPos konum, int kimlik, long baslangic, int sure, BlockState durum) {
    }
}
