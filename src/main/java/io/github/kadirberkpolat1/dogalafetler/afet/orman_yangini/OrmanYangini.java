package io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini.mantik.OrmanKurallari;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKayitlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;

/**
 * Orman yangını (bölüm G, madde 6, 7, 10, tasarım notu {@code docs/tasarim/orman_yangini.md}). Yangın mantığı ortak
 * {@link YanginYoneticisi}'ndedir (madde 6, G.4): afet AKTIF'e geçerken başlangıç alanını kendi kaynağıyla
 * ({@code YanginKaynagi.afet(ORMAN_YANGINI, afetNo)}) seviyenin yoğunluğunda tutuşturur; yayılma, bütçe, tavan, yağmurda
 * sönme, yanıcılık ve ortam ısısı (madde 7) yöneticinin işidir. Afet bitince yönetici {@code AfetBittiEvent}'ten
 * kaynağı kendisi kapatır (Y-27): {@code /dogalafet stop} ateşi hemen söndürür, doğal bitişte kalan hücreler yayılmadan
 * yanıp biter. Afet ayrıca etki alanını yanan alanla büyütür, "Yanmadan Çıktım" ipucunu her saniye yayınlar, ateş
 * altındaki zemini kaba toprağa çevirir (yanık izi, {@code afetBlokHasari}) ve istemciye sıcak kare özeti verir.
 */
public final class OrmanYangini extends Afet {
    private static final int SICAK_KARE_TAVANI = 32;
    private static final int YANIK_IZI_YUKSEKLIGI = 2;

    private double alanYaricapi;
    private boolean tutusturuldu;
    private int sifirSaniye;
    private int toplamHucre;
    private int enCokHucre;
    private int yanikIziToplam;
    private int[] sicakKareler = new int[0];

    public OrmanYangini() {
        super(AfetTuru.ORMAN_YANGINI);
    }

    private OrmanKurallari.Seviye ayar() {
        return OrmanAyarlari.tablo().seviye(seviye());
    }

    YanginKaynagi kaynak() {
        return YanginKaynagi.afet(tur(), baglam().baslangicSaati());
    }

    @Override
    public EtkiAlani etkiAlani() {
        BlockPos m = baglam().merkez();
        double yaricap = Math.max(alanYaricapi, ayar().yaricap() + OrmanKurallari.ALAN_PAYI);
        return EtkiAlani.daire(m.getX() + 0.5, m.getY(), m.getZ() + 0.5, yaricap);
    }

    @Override
    public BlockPos merkezSec(ServerLevel level, ServerPlayer hedef, int seviye, RandomSource rastgele) {
        return OrmanYeri.sec(level, hedef.blockPosition(), rastgele);
    }

    @Override
    public boolean yerUygunMu(ServerLevel level, BlockPos merkez, int seviye) {
        return OrmanYeri.yakitVar(level, merkez, Math.max(3, OrmanAyarlari.tablo().seviye(seviye).yaricap()));
    }

    @Override
    protected void asamaDegisti(ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni) {
        if (yeni == AfetAsamasi.AKTIF && !tutusturuldu) {
            tutustur(level);
        }
    }

    @Override
    protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
        if (asama != AfetAsamasi.AKTIF) {
            return;
        }
        if (!tutusturuldu) {
            tutustur(level);
        }
        if (level.getGameTime() % 20 == 0) {
            ozetle(level);
            ipuclari(level);
        }
        if (Kurallar.blokHasariAcik(level.getGameRules())) {
            yanikIzi(level);
        }
    }

    @Override
    protected boolean aktifBitti() {
        return asamaGecenTick() >= ayar().sureTick()
                || OrmanKurallari.sondu(sifirSaniye, OrmanAyarlari.erkenBitisSaniye());
    }

    @Override
    public long aktifKalanTick() {
        return Math.max(0, ayar().sureTick() - asamaGecenTick());
    }

    /** Ateş ortak yöneticide kalır: yönetici {@code AfetBittiEvent}'ten kaynağı kapatır (stop: söndür, doğal: yanarak biter). */
    @Override
    protected void temizle(ServerLevel level, boolean temiz) {
        DogalAfetler.LOGGER.info("orman yangını bitti: seviye {} en çok {} hücre, alan {} blok, yanık izi {} blok", seviye(),
                enCokHucre, Math.round(etkiAlani().yaricap()), yanikIziToplam);
        sicakKareler = new int[0];
        toplamHucre = 0;
    }

    @Override
    protected void ozelDurumYaz(CompoundTag etiket) {
        etiket.putDouble("alan", alanYaricapi);
        etiket.putBoolean("tutusturuldu", tutusturuldu);
        etiket.putInt("sifir", sifirSaniye);
        etiket.putInt("enCok", enCokHucre);
        etiket.putInt("yanik", yanikIziToplam);
    }

    @Override
    protected void ozelDurumOku(CompoundTag etiket) {
        alanYaricapi = etiket.getDouble("alan");
        tutusturuldu = etiket.getBoolean("tutusturuldu");
        sifirSaniye = etiket.getInt("sifir");
        enCokHucre = etiket.getInt("enCok");
        yanikIziToplam = etiket.getInt("yanik");
    }

    /** {@code n}: alandaki yanan hücre; {@code h}: en kalabalık sıcak kareler (x, z, sayı) — istemcide sis ve ısı. */
    @Override
    protected void istemciVerisiYaz(CompoundTag etiket) {
        etiket.putInt("n", toplamHucre);
        etiket.putIntArray("h", sicakKareler);
    }

    int toplamHucre() {
        return toplamHucre;
    }

    private void tutustur(ServerLevel level) {
        tutusturuldu = true;
        OrmanKurallari.Seviye s = ayar();
        BlockPos m = baglam().merkez();
        int tutusan = YanginYoneticisi.alanTutustur(level, m, s.yaricap(), kaynak(), s.yogunluk());
        DogalAfetler.LOGGER.info("orman yangını: seviye {} başlangıç alanı {} r={} yoğunluk {} süre {} sn, ilk tick'te {} hücre "
                + "tutuştu (alan işi {})", seviye(), m.toShortString(), s.yaricap(), s.yogunluk(), s.sureTick() / 20, tutusan,
                YanginYoneticisi.bekleyenAlanSayisi(level));
    }

    /**
     * Saniyede bir: etki alanı boyunca 32 blokluk karelerde yanan hücre sayılır ({@code aktifSayi}); alan en uzak yanan
     * kareyle büyür, en kalabalık kareler istemciye gider. Alan boş ve bekleyen tutuşturma yoksa sönmüş saniye sayacı artar.
     */
    private void ozetle(ServerLevel level) {
        BlockPos m = baglam().merkez();
        int kare = OrmanKurallari.KARE;
        int menzil = (int) Math.ceil(Math.min(OrmanKurallari.EN_BUYUK_ALAN, etkiAlani().yaricap() + kare));
        int alt = level.getMinBuildHeight();
        int ust = level.getMaxBuildHeight();
        List<OrmanKurallari.Kare> dolu = new ArrayList<>();
        int toplam = 0;
        double enUzak = 0;
        for (int kx = Math.floorDiv(m.getX() - menzil, kare); kx <= Math.floorDiv(m.getX() + menzil, kare); kx++) {
            for (int kz = Math.floorDiv(m.getZ() - menzil, kare); kz <= Math.floorDiv(m.getZ() + menzil, kare); kz++) {
                int sayi = YanginYoneticisi.aktifSayi(level,
                        new AABB(kx * kare, alt, kz * kare, (kx + 1) * kare, ust, (kz + 1) * kare));
                if (sayi == 0) {
                    continue;
                }
                int cx = kx * kare + kare / 2;
                int cz = kz * kare + kare / 2;
                dolu.add(new OrmanKurallari.Kare(cx, cz, sayi));
                toplam += sayi;
                enUzak = Math.max(enUzak, OrmanKurallari.kareUzakligi(cx - m.getX() - 0.5, cz - m.getZ() - 0.5));
            }
        }
        toplamHucre = toplam;
        enCokHucre = Math.max(enCokHucre, toplam);
        sicakKareler = OrmanKurallari.sicakKareler(dolu, SICAK_KARE_TAVANI);
        alanYaricapi = OrmanKurallari.alanYaricapi(alanYaricapi, enUzak, ayar().yaricap());
        sifirSaniye = toplam == 0 && YanginYoneticisi.bekleyenAlanSayisi(level) == 0 ? sifirSaniye + 1 : 0;
    }

    /** Yanan hücreye 8 blok yakın oyuncu bu saniye yangın alanındadır (başarım §A.3 #5, saniyede bir olay). */
    private void ipuclari(ServerLevel level) {
        EtkiAlani alan = etkiAlani();
        for (ServerPlayer oyuncu : level.players()) {
            if (oyuncu.isSpectator() || !oyuncu.isAlive()
                    || !alan.iceriyor(oyuncu.getX(), oyuncu.getY(), oyuncu.getZ(), 0)) {
                continue;
            }
            AABB yakin = new AABB(oyuncu.blockPosition()).inflate(OrmanKurallari.YANGIN_YAKINLIGI);
            if (YanginYoneticisi.aktifSayi(level, yakin) > 0) {
                MinecraftForge.EVENT_BUS.post(new BasarimIpucuEvent(oyuncu, Ipucu.YANGIN_ALANINDA, this));
            }
        }
    }

    /** Yanık izi: sıcak karelerden rastgele sütunlar yoklanır; zemin üstünde ateş varsa zemin kaba toprağa döner. */
    private void yanikIzi(ServerLevel level) {
        int[] kareler = sicakKareler;
        int butce = (int) Math.round(OrmanAyarlari.yanikIziButcesi() * TpsOlcer.kismaCarpani());
        if (kareler.length == 0 || butce <= 0) {
            return;
        }
        RandomSource r = level.getRandom();
        int kare = OrmanKurallari.KARE;
        LongSet degisen = new LongOpenHashSet();
        for (int i = 0; i < butce; i++) {
            int k = r.nextInt(kareler.length / 3) * 3;
            int x = kareler[k] - kare / 2 + r.nextInt(kare);
            int z = kareler[k + 1] - kare / 2 + r.nextInt(kare);
            if (yanikIziSutunu(level, x, z)) {
                degisen.add(ChunkPos.asLong(x >> 4, z >> 4));
                yanikIziToplam++;
            }
        }
        for (long chunk : degisen) {
            SanalSu.chunkDegisti(level, new ChunkPos(chunk));
        }
    }

    /** Sütunun zemini (yapraksız en üst katı blok) çimen/toprak ise ve hemen üstünde afet ateşi yanıyorsa kaba toprak olur. */
    static boolean yanikIziSutunu(ServerLevel level, int x, int z) {
        if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
            return false;
        }
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, h - 1, z);
        if (!zeminMi(level.getBlockState(p))) {
            return false;
        }
        Block ates = YanginKayitlari.AFET_ATESI.get();
        for (int dy = 0; dy <= YANIK_IZI_YUKSEKLIGI; dy++) {
            p.setY(h + dy);
            if (level.getBlockState(p).is(ates)) {
                p.setY(h - 1);
                return level.setBlock(p, Blocks.COARSE_DIRT.defaultBlockState(), 3);
            }
        }
        return false;
    }

    private static boolean zeminMi(BlockState d) {
        return d.is(Blocks.GRASS_BLOCK) || d.is(Blocks.DIRT) || d.is(Blocks.PODZOL) || d.is(Blocks.MYCELIUM)
                || d.is(Blocks.ROOTED_DIRT) || d.is(Blocks.MOSS_BLOCK);
    }
}
