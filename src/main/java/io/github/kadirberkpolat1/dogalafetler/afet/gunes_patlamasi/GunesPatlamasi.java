package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik.GunesPatlamasiEgrileri;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik.MeteorYagmuruEgrisi;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAPI;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.uyari.HacimEgrisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Güneş patlaması (bölüm D): seviyesiz, sunucu geneli. Siren başlayınca güneş BÜYÜYEN olur ve sirenin 25 sn'sinde 1,6 kata
 * büyür; AKTIF {@code aktifSureGun} (5 gün) sürer: evre ilerlemesi doğrusal 1'e (gökyüzü 3,2 kat, sıcaklık artışı ve
 * buharlaşma bu evreden okur), her oyuncunun çevresine {@link MeteorAPI#yagmur} ile dengeli meteorlar, 3. günden sonra
 * çimenler kavrulur. Meteor yoğunluğu {@link MeteorYagmuruEgrisi}'dir: ön-uyarının sonunda başlayıp sirende artan öncü
 * yağmur, AKTIF'in ve her günün başında {@code meteorYagmuruSaniye} süren meteor yağmuru, arasında 1/40 sn → 1/12 sn
 * arka plan; afetin tek yağmuru eğriyi her tick izler. Doğal bitişte (BITIS) evre SÜPERNOVA olur ve
 * gökyüzünün {@code GunesZinciriSureci}'i devralır (güneşsiz dönem, donma, kızıl cüce). Doğal olmayan bitişte
 * tamamlanmamış BÜYÜYEN'i çekirdek NORMAL'e döndürür (K-C26). Alt aşama AKTIF'in günüdür; {@code asama ileri} bir gün atlar.
 */
public final class GunesPatlamasi extends Afet {
    /** Öncü ve günlük yağmur pencerelerinin INFO satırındaki yoğunluk biçimi. */
    private static final String YOGUNLUK = "%.1f";
    /** Kavurucu rüzgâr katmanı ({@code OrtamSesi}, global). */
    static final String RUZGAR_KATMANI = "gunes/kavrulma";
    /** Oyuncu başına bir tick'teki en çok kavrulma denemesi ({@code /dogalafet hiz} yüksekken TPS korunur). */
    static final int KAVRULMA_DENEME_TAVANI = 32;

    private long aktifSure = -1;
    private long aktifGecen;
    /** Afetin yağmuru (-1: yok); kalıcı değil (yağmur kaydedilmez, yüklenince ilk tick'te yeniden kurulur). */
    private int yagmurKimlik = -1;
    /** Öncü ya da günlük yağmur penceresinde mi (yalnız INFO satırı için). */
    private boolean yogunYagmur;

    public GunesPatlamasi() {
        super(AfetTuru.GUNES_PATLAMASI);
    }

    @Override
    public EtkiAlani etkiAlani() {
        return EtkiAlani.herYer();
    }

    /** Yalnız güneş NORMAL ya da kızıl cüce doğuşu tamamken (zincir sürerken {@code start} "uygun yer yok" der). */
    @Override
    public boolean yerUygunMu(ServerLevel level, BlockPos merkez, int seviye) {
        DunyaDonemi donem = DunyaDonemi.al(level);
        return GunesPatlamasiEgrileri.baslayabilir(donem.gunesEvresi(), donem.evreIlerlemesi());
    }

    @Override
    protected void baslarken(ServerLevel level) {
        aktifSure = GunesPatlamasiEgrileri.aktifSure(GunesPatlamasiAyarlari.aktifSureGun());
    }

    @Override
    protected void asamaDegisti(ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni) {
        DunyaDonemi donem = DunyaDonemi.al(level);
        switch (yeni) {
            case SIREN -> {
                if (GunesPatlamasiEgrileri.baslayabilir(donem.gunesEvresi(), donem.evreIlerlemesi())) {
                    donem.gunesEvresiAyarla(GunesEvresi.BUYUYEN);
                    DogalAfetler.LOGGER.info("Güneş patlaması: siren, güneş büyümeye başladı (afetNo {})",
                            baglam().baslangicSaati());
                }
            }
            case AKTIF -> {
                DogalAfetler.LOGGER.info("Güneş patlaması: meteorlar başlıyor, süreç {} gün ({} ölçekli tick)",
                        GunesPatlamasiEgrileri.gunSayisi(aktifSure), aktifSure);
                if (donem.gunesEvresi() == GunesEvresi.BUYUYEN) {
                    donem.evreIlerlemesiAyarla(GunesPatlamasiEgrileri.aktifIlerlemesi(aktifGecen, aktifSure));
                }
                ruzgarBaslat(level);
            }
            case BITIS -> {
                if (eski == AfetAsamasi.AKTIF && donem.gunesEvresi() == GunesEvresi.BUYUYEN) {
                    donem.gunesEvresiAyarla(GunesEvresi.SUPERNOVA);
                    DogalAfetler.LOGGER.info("Güneş patlaması doğal bitti: süpernova (saat {})", donem.saat());
                }
            }
            default -> {
            }
        }
    }

    @Override
    protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
        DunyaDonemi donem = DunyaDonemi.al(level);
        boolean buyuyen = donem.gunesEvresi() == GunesEvresi.BUYUYEN;
        long sirenSure = SirenTuru.sureTick(tur());
        long oncuTick = GunesPatlamasiAyarlari.meteorYagmuruOncuSaniye() * 20L;
        double oncuBas = GunesPatlamasiAyarlari.meteorYagmuruOncuDakikaBasina();
        double oncuSon = GunesPatlamasiAyarlari.meteorYagmuruOncuSonDakikaBasina();
        if (asama == AfetAsamasi.ON_UYARI) {
            double y = MeteorYagmuruEgrisi.onUyari(asamaKalanTick(), baglam().onUyariSuresi(), sirenSure, oncuTick, oncuBas,
                    oncuSon);
            yagmurSur(level, y, y > 0);
            return;
        }
        if (asama == AfetAsamasi.SIREN) {
            if (buyuyen) {
                donem.evreIlerlemesiAyarla(GunesPatlamasiEgrileri.sirenIlerlemesi(asamaGecenTick(), (int) sirenSure));
            }
            yagmurSur(level, MeteorYagmuruEgrisi.siren(asamaGecenTick(), baglam().onUyariSuresi(), sirenSure, oncuTick,
                    oncuBas, oncuSon), true);
            return;
        }
        if (asama != AfetAsamasi.AKTIF) {
            return;
        }
        aktifGecen = Math.min(aktifSure, aktifGecen + Math.max(0, adim));
        if (buyuyen) {
            donem.evreIlerlemesiAyarla(GunesPatlamasiEgrileri.aktifIlerlemesi(aktifGecen, aktifSure));
        }
        int gun = Math.min(GunesPatlamasiEgrileri.gun(aktifGecen), GunesPatlamasiEgrileri.gunSayisi(aktifSure) - 1);
        if (gun != altAsama()) {
            altAsamaAyarla(gun);
        }
        long ilkGunTick = GunesPatlamasiAyarlari.meteorYagmuruSaniye() * 20L;
        long gunlukTick = GunesPatlamasiAyarlari.meteorYagmuruGunlukSaniye() * 20L;
        yagmurSur(level, MeteorYagmuruEgrisi.aktif(aktifGecen, aktifSure, GunesPatlamasiAyarlari.meteorBaslangic(),
                GunesPatlamasiAyarlari.meteorSon(), ilkGunTick, gunlukTick, GunesPatlamasiAyarlari.meteorYagmuruDakikaBasina()),
                MeteorYagmuruEgrisi.yagmurda(aktifGecen, ilkGunTick, gunlukTick));
        kavur(level, GunesPatlamasiEgrileri.kavrulmaOlasiligi(aktifGecen, aktifSure,
                GunesPatlamasiAyarlari.kavrulmaBaslangicGun(), GunesPatlamasiAyarlari.kavrulmaOlasiligi()), adim);
    }

    /**
     * Afetin tek yağmuru eğriyi izler: varsa yoğunluğu güncellenir (0'da durmaz, birikim kalır), yoksa yoğunluk pozitifken
     * kurulur. {@code yogun}: öncü ya da günlük yağmur penceresi; değişince bir INFO satırı.
     */
    private void yagmurSur(ServerLevel level, double yogunluk, boolean yogun) {
        if (yogun != yogunYagmur) {
            yogunYagmur = yogun;
            DogalAfetler.LOGGER.info("Güneş patlaması: meteor yağmuru {} ({}, gün {}; oyuncu başına dakikada {})",
                    yogun ? "başladı" : "seyreldi", asama(), altAsama(), String.format(Locale.ROOT, YOGUNLUK, yogunluk));
        }
        if (yagmurKimlik >= 0 && MeteorAPI.yagmurYogunluguAyarla(level, yagmurKimlik, yogunluk)) {
            return;
        }
        if (yogunluk > 0) {
            yagmurKimlik = MeteorAPI.yagmur(level, List.of(), Integer.MAX_VALUE, yogunluk,
                    GunesPatlamasiAyarlari.buyukMeteorOrani(), kaynak());
        }
    }

    /**
     * Kavurucu rüzgâr (global, WEATHER): kalan süre boyunca çalışmaya geçen paya göre %30'dan %100'e yükselir (eğri gerçek
     * tick; hız 1 varsayılır, sonra son düzeyde kalır). Afet bitince temizlikte söner.
     */
    private void ruzgarBaslat(ServerLevel level) {
        float bas = 0.3f + 0.7f * (float) aktifGecen / Math.max(1L, aktifSure);
        int kalan = (int) Math.min(Integer.MAX_VALUE / 2, Math.max(200L, aktifSure - aktifGecen));
        OrtamSesi.baslat(level.getServer(), RUZGAR_KATMANI, baglam().baslangicSaati(), GunesPatlamasiSesleri.KAVRULMA_RUZGAR,
                SoundSource.WEATHER, OrtamSesi.Yer.global(), HacimEgrisi.of(new HacimEgrisi.Nokta(0, 0f),
                        new HacimEgrisi.Nokta(100, bas), new HacimEgrisi.Nokta(100 + kalan, 1f)));
    }

    /**
     * Kavrulma: oyuncu başına her ölçekli tick'te olasılıkla bir deneme ({@code adim} kadar, en çok
     * {@value #KAVRULMA_DENEME_TAVANI}); süreç {@code /dogalafet hiz} ile sıkışınca kavrulma da sıkışır. Bütçe TPS
     * kısmasıyla çarpılır.
     */
    private static void kavur(ServerLevel level, double olasilik, int adim) {
        if (olasilik <= 0 || adim <= 0 || !Kurallar.blokHasariAcik(level.getGameRules())) {
            return;
        }
        double p = olasilik * TpsOlcer.kismaCarpani();
        int deneme = Math.min(adim, KAVRULMA_DENEME_TAVANI);
        int yaricap = GunesPatlamasiAyarlari.kavrulmaYaricap();
        RandomSource r = level.getRandom();
        for (ServerPlayer oyuncu : level.players()) {
            if (oyuncu.isSpectator()) {
                continue;
            }
            for (int i = 0; i < deneme; i++) {
                if (r.nextDouble() >= p) {
                    continue;
                }
                int x = oyuncu.getBlockX() + r.nextInt(2 * yaricap + 1) - yaricap;
                int z = oyuncu.getBlockZ() + r.nextInt(2 * yaricap + 1) - yaricap;
                if (level.hasChunk(x >> 4, z >> 4)) {
                    kavur(level, new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z));
                }
            }
        }
    }

    /**
     * Güneşe açık çimen bloğunu (sütunun en üstteki hareket engelleyen bloğu; yaprak ve çatı gölge yapar) kaba toprağa
     * çevirir (yangın sistemi dışı, {@code afetBlokHasari} ile); değişen chunk sanal suya bildirilir. Yükseklik haritası
     * blok değişiminde eşzamanlı güncellenir; gökyüzü ışığı gecikebilir. Değişti mi.
     */
    public static boolean kavur(ServerLevel level, BlockPos konum) {
        if (!Kurallar.blokHasariAcik(level.getGameRules()) || !level.getBlockState(konum).is(Blocks.GRASS_BLOCK)
                || level.getHeight(Heightmap.Types.MOTION_BLOCKING, konum.getX(), konum.getZ()) > konum.getY() + 1) {
            return false;
        }
        level.setBlock(konum, Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL);
        SanalSu.chunkDegisti(level, new ChunkPos(konum));
        return true;
    }

    @Override
    protected boolean aktifBitti() {
        return aktifGecen >= aktifSure;
    }

    @Override
    public long aktifKalanTick() {
        return Math.max(0L, aktifSure - aktifGecen);
    }

    /** {@code /dogalafet asama ileri} AKTIF'te: sonraki günün başına atlar; son günde BITIS (doğal bitiş, süpernova). */
    @Override
    protected boolean altAsamaIleri(ServerLevel level) {
        int sonraki = GunesPatlamasiEgrileri.gun(aktifGecen) + 1;
        if (sonraki >= GunesPatlamasiEgrileri.gunSayisi(aktifSure)) {
            aktifGecen = aktifSure;
            return false;
        }
        aktifGecen = sonraki * GunesPatlamasiEgrileri.GUN_TICK;
        altAsamaAyarla(sonraki);
        DunyaDonemi donem = DunyaDonemi.al(level);
        if (donem.gunesEvresi() == GunesEvresi.BUYUYEN) {
            donem.evreIlerlemesiAyarla(GunesPatlamasiEgrileri.aktifIlerlemesi(aktifGecen, aktifSure));
        }
        return true;
    }

    @Override
    protected void temizle(ServerLevel level, boolean temiz) {
        int durdu = MeteorAPI.yagmurlariDurdur(level.getServer(), kaynak(), temiz);
        if (OrtamSesi.aktifMi(RUZGAR_KATMANI)) {
            OrtamSesi.durdur(level.getServer(), RUZGAR_KATMANI, 80);
        }
        DogalAfetler.LOGGER.info("Güneş patlaması durdu (temiz {}): {} meteor yağmuru durduruldu", temiz, durdu);
    }

    @Override
    protected void yuklendi(ServerLevel level) {
        yagmurKimlik = -1;
        if (asama() == AfetAsamasi.AKTIF) {
            ruzgarBaslat(level);
        }
    }

    @Override
    protected void istemciVerisiYaz(CompoundTag etiket) {
        etiket.putInt("gun", altAsama());
    }

    @Override
    protected void ozelDurumYaz(CompoundTag etiket) {
        etiket.putLong("aktifSure", aktifSure);
        etiket.putLong("aktifGecen", aktifGecen);
    }

    @Override
    protected void ozelDurumOku(CompoundTag etiket) {
        aktifSure = etiket.contains("aktifSure") ? etiket.getLong("aktifSure")
                : GunesPatlamasiEgrileri.aktifSure(GunesPatlamasiAyarlari.aktifSureGun());
        aktifGecen = Math.max(0L, etiket.getLong("aktifGecen"));
    }

    YanginKaynagi kaynak() {
        return YanginKaynagi.afet(tur(), baglam().baslangicSaati());
    }

    /** Test ve komut okuması. */
    long aktifGecen() {
        return aktifGecen;
    }

    /** Test okuması: afetin yağmuru (-1: yok). */
    int yagmurKimlik() {
        return yagmurKimlik;
    }
}
