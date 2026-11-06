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
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
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
 * Asit yağmuru (bölüm E, madde 10, tasarım notu {@code docs/tasarim/asit_yagmuru.md}). Alan hedef oyuncunun çevresinde
 * doğar ve günün rüzgârıyla kayar; AKTIF'te yağmur 10 sn'de yoğunlaşır, son 15 sn'de seyrelir. Gökyüzüne açık oyuncu
 * aralıklı hasar alır (can {@code asitMinimumCan} altına inmez; çatı altı hiç hasar yok). Gökyüzünü gören yüzey blokları
 * önce 3–5 sn çatlak örtüsü taşır, sonra dönüşür ({@code afetBlokHasari=false} iken hiç aşınma yok). Yağmur, sis ve
 * dumanlar istemcide {@code client.asit_yagmuru} tarafından bu afetin durum paketinden çizilir; afet ses çalmaz, sireni
 * çekirdek çalar.
 */
public final class AsitYagmuru extends Afet {
    private static final int HASAR_ADIMI = 4;
    /** Kırılma örtüsü kimlikleri negatif: oyuncu varlık kimlikleriyle çakışmaz (vanilla kendi kırdığını o oyuncuya göndermez). */
    private static final int KIMLIK_TABANI = -0x41534900;
    private static final int KIMLIK_ARALIGI = 4096;
    private static final int SES_TAVANI = 4;
    /** Oyuncu çevresinden örnekleme yarıçapı (blok). */
    private static final double YAKIN_ORNEK = 24;
    private static final int ISTEMCI_ASINMA_TAVANI = 32;

    private double x;
    private double y;
    private double z;
    private double birikim;
    private int sonrakiKimlik;
    private int asinanToplam;
    private final List<Asinma> asinanlar = new ArrayList<>();
    private final Map<UUID, Integer> maruziyet = new HashMap<>();
    private Double yogunlukZorlamasi;

    public AsitYagmuru() {
        super(AfetTuru.ASIT_YAGMURU);
    }

    private AsitKurallari.Seviye ayar() {
        return AsitAyarlari.tablo().seviye(seviye());
    }

    @Override
    public EtkiAlani etkiAlani() {
        return EtkiAlani.daire(x, y, z, ayar().yaricap());
    }

    @Override
    protected void baslarken(ServerLevel level) {
        BlockPos m = baglam().merkez();
        x = m.getX() + 0.5;
        y = m.getY();
        z = m.getZ() + 0.5;
    }

    @Override
    protected void asamaDegisti(ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni) {
        if (yeni == AfetAsamasi.AKTIF) {
            AsitKurallari.Seviye s = ayar();
            DogalAfetler.LOGGER.info("asit yağmuru: seviye {} alan {} blok, {} sn, {} blok/sn aşınma, {} can / {} tick ({})",
                    seviye(), s.yaricap(), s.sureTick() / 20, s.asindirmaSaniye(), s.hasar(), s.hasarAraligi(),
                    Kurallar.blokHasariAcik(level.getGameRules()) ? "blok hasarı açık" : "blok hasarı kapalı");
        }
    }

    @Override
    protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
        if (asama != AfetAsamasi.AKTIF) {
            return;
        }
        kaydir(level, adim);
        double yogunluk = yogunluk();
        if (yogunluk >= AsitKurallari.ETKI_ESIGI) {
            if (level.getGameTime() % HASAR_ADIMI == 0) {
                oyunculariDenetle(level);
            }
            if (Kurallar.blokHasariAcik(level.getGameRules())) {
                asindir(level, yogunluk, adim);
            }
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
        DogalAfetler.LOGGER.info("asit yağmuru bitti: seviye {} {} blok aşındı, {} çatlak örtüsü kaldırıldı", seviye(),
                asinanToplam, kalan);
    }

    @Override
    protected void ozelDurumYaz(CompoundTag etiket) {
        etiket.putDouble("x", x);
        etiket.putDouble("y", y);
        etiket.putDouble("z", z);
        etiket.putDouble("birikim", birikim);
        etiket.putInt("toplam", asinanToplam);
    }

    @Override
    protected void ozelDurumOku(CompoundTag etiket) {
        x = etiket.getDouble("x");
        y = etiket.getDouble("y");
        z = etiket.getDouble("z");
        birikim = etiket.getDouble("birikim");
        asinanToplam = etiket.getInt("toplam");
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

    /** Alan günün rüzgârı yönünde (ortak yangının dumanıyla aynı yön) {@code kaymaHizi} ile kayar. */
    private void kaydir(ServerLevel level, int adim) {
        double[] ruzgar = YanginMantigi.gununRuzgari(level.getGameTime() / 24000L);
        double guc = Math.hypot(ruzgar[0], ruzgar[1]);
        if (guc <= 0 || adim <= 0) {
            return;
        }
        double yol = AsitAyarlari.kaymaHizi() * adim / 20.0;
        x += ruzgar[0] / guc * yol;
        z += ruzgar[1] / guc * yol;
    }

    /** Başının üstü gökyüzüne açık oyuncu aralıklı hasar alır; hasar anında başarım ipucu (Çatı Altı'nı bozar). */
    private void oyunculariDenetle(ServerLevel level) {
        AsitKurallari.Seviye s = ayar();
        EtkiAlani alan = etkiAlani();
        for (ServerPlayer oyuncu : level.players()) {
            if (oyuncu.isSpectator() || oyuncu.isCreative() || !oyuncu.isAlive()
                    || !alan.iceriyor(oyuncu.getX(), oyuncu.getY(), oyuncu.getZ(), 0)) {
                continue;
            }
            int harita = level.getHeight(Heightmap.Types.MOTION_BLOCKING, oyuncu.getBlockX(), oyuncu.getBlockZ());
            if (!AsitKurallari.acikta(harita, oyuncu.getEyeY())) {
                continue;
            }
            int birikmis = maruziyet.merge(oyuncu.getUUID(), HASAR_ADIMI, Integer::sum);
            if (birikmis < s.hasarAraligi()) {
                continue;
            }
            maruziyet.put(oyuncu.getUUID(), birikmis - s.hasarAraligi());
            float miktar = AsitKurallari.hasarMiktari(oyuncu.getHealth(), s.hasar(), AsitAyarlari.minimumCan());
            if (miktar > 0 && oyuncu.hurt(AsitKayitlari.hasar(level), miktar)) {
                MinecraftForge.EVENT_BUS.post(new BasarimIpucuEvent(oyuncu, Ipucu.HASAR_ALDI));
            }
        }
    }

    /**
     * Alandan rastgele yüzey sütunları örneklenir; aşınabilir blok hız birikimi el verdikçe çatlak kuyruğuna girer. Alanda oyuncu
     * (gözlemci dahil) varsa örneklerin yarısı rastgele bir oyuncunun 24 blok çevresinden seçilir: değişim görenin önünde olur.
     */
    private void asindir(ServerLevel level, double yogunluk, int adim) {
        AsitKurallari.Seviye s = ayar();
        double kisma = TpsOlcer.kismaCarpani();
        birikim = AsitKurallari.birikimEkle(birikim, s.asindirmaSaniye(), yogunluk, kisma, adim);
        int tavan = AsitAyarlari.eszamanliAsindirma();
        RandomSource r = level.getRandom();
        EtkiAlani alan = etkiAlani();
        List<ServerPlayer> gorenler = new ArrayList<>();
        for (ServerPlayer oyuncu : level.players()) {
            if (alan.iceriyor(oyuncu.getX(), oyuncu.getY(), oyuncu.getZ(), 0)) {
                gorenler.add(oyuncu);
            }
        }
        int ornek = AsitKurallari.ornekSayisi(s.asindirmaSaniye(), kisma);
        for (int i = 0; i < ornek && birikim >= 1 && asinanlar.size() < tavan; i++) {
            double cx = x;
            double cz = z;
            double menzil = s.yaricap();
            if (!gorenler.isEmpty() && r.nextBoolean()) {
                ServerPlayer goren = gorenler.get(r.nextInt(gorenler.size()));
                cx = goren.getX();
                cz = goren.getZ();
                menzil = Math.min(YAKIN_ORNEK, menzil);
            }
            double aci = r.nextDouble() * Math.PI * 2;
            double uzaklik = Math.sqrt(r.nextDouble()) * menzil;
            double px = cx + Math.cos(aci) * uzaklik;
            double pz = cz + Math.sin(aci) * uzaklik;
            if (!alan.iceriyor(px, y, pz, 0)) {
                continue;
            }
            BlockPos aday = AsitAsindirici.aday(level, Mth.floor(px), Mth.floor(pz), s.tasAsinir());
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
