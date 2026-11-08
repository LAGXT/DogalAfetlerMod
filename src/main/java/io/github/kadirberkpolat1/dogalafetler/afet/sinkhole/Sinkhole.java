package io.github.kadirberkpolat1.dogalafetler.afet.sinkhole;

import static io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi.CATLAMA;
import static io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi.COKUS;
import static io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi.DURULMA;
import static io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi.GERI_CEKILME;
import static io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi.SARKMA;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.BlokIsleri;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremSistemi;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.CukurPlani;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.uyari.HacimEgrisi;
import io.github.kadirberkpolat1.dogalafetler.uyari.OrtamSesi;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

/**
 * Sinkhole (bölüm F; örtü-çökme): AKTIF aşama beş alt aşamadır ({@link SinkholeSeviyesi}). Çatlama: eş merkezli ve radyal
 * çatlak örtüleri büyür, çıtırtı ve toz; sarkma: merkezin %40'ı bir blok oturur; çöküş: çekirdeğin içi boşaltılır, sonra yüzey
 * ve üstündeki her şey (ağaç, çit) düşen blok olarak tek seferde çukura iner, güm ve toz patlaması; geri çekilme: dış
 * halkalar sırayla içe devrilir, her biri bir basamak bırakır; durulma: kenar çatlak halkası (çimen → kaba toprak), aralıklı
 * kenar dökülmesi ve dipten yükselen toz. Dibe moloz konur, düşen bloklar üstüne yığılır ("bloklar çukura dolsun"). Çöken
 * alana düşen oyuncu için {@link Ipucu#CUKURA_DUSTU} yayınlanır (başarım §A.3 #4). {@code afetBlokHasari=false} iken blok
 * değişmez.
 */
public final class Sinkhole extends Afet {
    static final String SES_KATMANI = "sinkhole/gurleme";
    private static final int ORTU_TABANI = -2_000_000;
    private static final int ORTU_TAVANI = 400;

    private final BlockPos sabitMerkez;
    private final RandomSource rastgele = RandomSource.create();
    private final BitSet tamam = new BitSet();
    private final BitSet dusenHalka = new BitSet();
    private final Set<UUID> dusenler = new HashSet<>();
    private long tohum;
    private int fazGecen;
    private int sayac;
    private int zemin = Integer.MIN_VALUE;
    private boolean sarkti;
    private boolean kenarIslendi;
    private SinkholeSeviyesi seviyeBilgisi;
    private CukurPlani.Plan plan;
    private List<int[]> halkalar;
    private BlokIsleri isler;

    public Sinkhole() {
        this(null);
    }

    private Sinkhole(BlockPos sabitMerkez) {
        super(AfetTuru.SINKHOLE);
        this.sabitMerkez = sabitMerkez;
    }

    /** Yalnız GameTest: merkezi hedef oyuncudan bağımsız sabit sinkhole. */
    static Sinkhole sabitMerkezli(BlockPos merkez) {
        return new Sinkhole(merkez.immutable());
    }

    SinkholeSeviyesi seviyeBilgisi() {
        if (seviyeBilgisi == null) {
            seviyeBilgisi = SinkholeAyarlari.seviye(seviye());
        }
        return seviyeBilgisi;
    }

    CukurPlani.Plan plan() {
        return plan;
    }

    BlokIsleri isler() {
        return isler;
    }

    int zemin() {
        return zemin;
    }

    /**
     * Hedef oyuncudan 12–40 blok uzakta, çap boyunca yüksekliği ±4 blok düz, merkezi sıvı olmayan yer (8 deneme); hiçbiri
     * uymazsa en düz aday. Düzlük yalnız tercihtir: elle verilen konumda da çukur açılır ({@link #yerUygunMu}).
     */
    @Override
    public BlockPos merkezSec(ServerLevel level, ServerPlayer hedef, int seviye, RandomSource r) {
        if (sabitMerkez != null) {
            return sabitMerkez;
        }
        double yaricap = SinkholeAyarlari.seviye(seviye).yaricap();
        BlockPos enIyi = hedef.blockPosition();
        int enIyiFark = Integer.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = 12 + r.nextDouble() * 28;
            int x = (int) Math.floor(hedef.getX() + Math.cos(a) * d);
            int z = (int) Math.floor(hedef.getZ() + Math.sin(a) * d);
            int fark = duzlukFarki(level, x, z, yaricap);
            if (fark >= YUKLU_DEGIL) {
                continue;
            }
            if (fark < enIyiFark) {
                enIyiFark = fark;
                enIyi = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
                if (fark <= 4) {
                    break;
                }
            }
        }
        return enIyi;
    }

    @Override
    public boolean yerUygunMu(ServerLevel level, BlockPos merkez, int seviye) {
        if (sabitMerkez != null) {
            return true;
        }
        if (!level.hasChunk(merkez.getX() >> 4, merkez.getZ() >> 4)) {
            return false;
        }
        BlockPos ust = new BlockPos(merkez.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, merkez.getX(),
                merkez.getZ()) - 1, merkez.getZ());
        return level.getFluidState(ust).isEmpty();
    }

    /** {@link #duzlukFarki} yüklü olmayan chunk'ta; aday atlanır (yükseklik okuması chunk'ı eşzamanlı yüklemesin). */
    private static final int YUKLU_DEGIL = 1000;

    /** Merkez ve çap çemberindeki 8 noktanın yüzey yüksekliği farki; yüklü olmayan chunk varsa {@link #YUKLU_DEGIL}. */
    private static int duzlukFarki(ServerLevel level, int x, int z, double yaricap) {
        int enAz = Integer.MAX_VALUE;
        int enCok = Integer.MIN_VALUE;
        for (int i = -1; i < 8; i++) {
            int px = i < 0 ? x : (int) Math.floor(x + Math.cos(i * Math.PI / 4) * yaricap);
            int pz = i < 0 ? z : (int) Math.floor(z + Math.sin(i * Math.PI / 4) * yaricap);
            if (!level.hasChunk(px >> 4, pz >> 4)) {
                return YUKLU_DEGIL;
            }
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
            enAz = Math.min(enAz, h);
            enCok = Math.max(enCok, h);
        }
        return enCok - enAz;
    }

    @Override
    public EtkiAlani etkiAlani() {
        BlockPos m = baglam().merkez();
        return EtkiAlani.daire(m.getX() + 0.5, m.getY(), m.getZ() + 0.5, seviyeBilgisi().yaricap() + 16);
    }

    @Override
    protected void baslarken(ServerLevel level) {
        tohum = level.random.nextLong();
    }

    @Override
    protected void asamaDegisti(ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni) {
        if (yeni == AfetAsamasi.AKTIF) {
            sesiBaslat(level);
        } else if (yeni == AfetAsamasi.BITIS) {
            OrtamSesi.durdur(level.getServer(), SES_KATMANI, 60);
            if (isler != null) {
                isler.ortuleriKaldir();
            }
        }
    }

    @Override
    protected void yuklendi(ServerLevel level) {
        if (asama() == AfetAsamasi.AKTIF) {
            sesiBaslat(level);
        }
    }

    private void sesiBaslat(ServerLevel level) {
        OrtamSesi.baslat(level.getServer(), SES_KATMANI, baglam().baslangicSaati(), DepremSistemi.GURLEME_ADI,
                SoundSource.BLOCKS, OrtamSesi.Yer.nokta(level.dimension(), Vec3.atCenterOf(baglam().merkez()),
                        (float) (seviyeBilgisi().yaricap() + 48)), HacimEgrisi.giris(40, 0.7f));
    }

    @Override
    protected void asamaTick(ServerLevel level, AfetAsamasi asama, int adim) {
        if (asama != AfetAsamasi.AKTIF) {
            return;
        }
        if (isler == null) {
            hazirla(level);
        }
        SinkholeSeviyesi s = seviyeBilgisi();
        boolean hasar = Kurallar.blokHasariAcik(level.getGameRules());
        fazGecen += adim;
        sayac += adim;
        int alt = altAsama();
        int butce = (int) Math.max(16, SinkholeAyarlari.blokButcesi() * TpsOlcer.kismaCarpani() * Math.min(4, adim));
        switch (alt) {
            case CATLAMA -> {
                catlaklar(Math.min(6, fazGecen * 7 / Math.max(1, s.catlamaTick())));
                citirti(level, 4);
            }
            case SARKMA -> {
                if (!sarkti) {
                    sarkti = true;
                    if (hasar) {
                        sark(s);
                    }
                }
                catlaklar(8);
                citirti(level, 2);
            }
            case COKUS -> {
                if (adim > 0) {
                    halkalariIsle(level, s, 0, hasar, butce, false);
                }
            }
            case GERI_CEKILME -> {
                int h = Math.min(plan.halkaSayisi() - 1, 1 + fazGecen / Math.max(1, s.halkaTick()));
                if (adim > 0) {
                    halkalariIsle(level, s, h, hasar, butce, false);
                }
            }
            default -> {
                if (!kenarIslendi) {
                    kenarIslendi = true;
                    if (hasar) {
                        kenar();
                    }
                }
                if (sayac >= 6) {
                    sayac = 0;
                    dokul(level, s, hasar);
                }
            }
        }
        if (alt >= COKUS && fazGecen % 5 < Math.max(1, adim)) {
            cukurdakileriIsaretle(level, s.yaricap() + 0.5, zemin - 1);
        }
        if (alt < DURULMA && fazGecen >= s.sure(alt)) {
            gec(level, alt + 1, hasar);
        }
        isler.bildir();
    }

    private void gec(ServerLevel level, int yeni, boolean hasar) {
        int alt = altAsama();
        if (alt == COKUS || alt == GERI_CEKILME) {
            halkalariIsle(level, seviyeBilgisi(), alt == COKUS ? 0 : plan.halkaSayisi() - 1, hasar, Integer.MAX_VALUE, true);
        }
        if (yeni == COKUS) {
            isler.ortuleriKaldir();
        } else if (yeni == DURULMA) {
            SinkholeSeviyesi s = seviyeBilgisi();
            DogalAfetler.LOGGER.info("[sinkhole] çukur tamam: seviye {}, çap {}, derinlik {}, {} sütun, değişen blok {}, merkez {}",
                    s.seviye(), s.cap(), s.derinlik(), plan.sutunlar().size(), isler.degisenBlok(),
                    baglam().merkez().toShortString());
        }
        altAsamaAyarla(yeni);
        fazGecen = 0;
        sayac = 0;
    }

    @Override
    protected boolean altAsamaIleri(ServerLevel level) {
        if (isler == null) {
            hazirla(level);
        }
        if (altAsama() >= DURULMA) {
            return false;
        }
        gec(level, altAsama() + 1, Kurallar.blokHasariAcik(level.getGameRules()));
        isler.bildir();
        return true;
    }

    private void hazirla(ServerLevel level) {
        plan = CukurPlani.olustur(seviyeBilgisi(), tohum);
        isler = new BlokIsleri(level, ORTU_TABANI);
        halkalar = new ArrayList<>();
        for (int h = 0; h < plan.halkaSayisi(); h++) {
            halkalar.add(new int[0]);
        }
        List<List<Integer>> gecici = new ArrayList<>();
        for (int h = 0; h < plan.halkaSayisi(); h++) {
            gecici.add(new ArrayList<>());
        }
        for (int i = 0; i < plan.sutunlar().size(); i++) {
            gecici.get(plan.sutunlar().get(i).halka()).add(i);
        }
        for (int h = 0; h < plan.halkaSayisi(); h++) {
            halkalar.set(h, gecici.get(h).stream().mapToInt(Integer::intValue).toArray());
        }
        if (zemin == Integer.MIN_VALUE) {
            BlockPos m = baglam().merkez();
            int z = isler.zemin(m.getX(), m.getZ());
            zemin = z == Integer.MIN_VALUE ? m.getY() - 1 : z;
        }
    }

    private void catlaklar(int asama) {
        BlockPos m = baglam().merkez();
        List<int[]> c = plan.catlaklar();
        for (int i = 0; i < c.size() && i < ORTU_TAVANI; i++) {
            int x = m.getX() + c.get(i)[0];
            int z = m.getZ() + c.get(i)[1];
            int y = isler.ust(x, z);
            if (y != Integer.MIN_VALUE) {
                isler.ortu(i, new BlockPos(x, y, z), asama);
            }
        }
    }

    private void citirti(ServerLevel level, int toz) {
        BlockPos m = baglam().merkez();
        List<int[]> c = plan.catlaklar();
        if (c.isEmpty()) {
            return;
        }
        for (int k = 0; k < toz; k++) {
            int[] d = c.get(rastgele.nextInt(c.size()));
            int x = m.getX() + d[0];
            int z = m.getZ() + d[1];
            int y = isler.ust(x, z);
            if (y == Integer.MIN_VALUE) {
                continue;
            }
            BlockState st = level.getBlockState(new BlockPos(x, y, z));
            isler.parcacik(new BlockParticleOption(ParticleTypes.BLOCK, st.isAir() ? Blocks.DIRT.defaultBlockState() : st),
                    x + 0.5, y + 1.05, z + 0.5, 3, 0.25, 0.12);
            if (sayac >= 8) {
                sayac = 0;
                level.playSound(null, x + 0.5, y, z + 0.5, SoundEvents.DRIPSTONE_BLOCK_BREAK, SoundSource.BLOCKS, 1.2f,
                        0.5f + rastgele.nextFloat() * 0.4f);
            }
        }
    }

    /** Merkezin %40'ı bir blok oturur: yüzey bloğu bir altının yerine geçer. */
    private void sark(SinkholeSeviyesi s) {
        BlockPos m = baglam().merkez();
        double sinir = 0.4 * s.yaricap();
        for (CukurPlani.Sutun c : plan.sutunlar()) {
            if (Math.hypot(c.dx(), c.dz()) > sinir) {
                continue;
            }
            int x = m.getX() + c.dx();
            int z = m.getZ() + c.dz();
            int g = isler.zemin(x, z);
            if (g == Integer.MIN_VALUE) {
                continue;
            }
            BlockPos ust = new BlockPos(x, g, z);
            BlockState yuzey = isler.level().getBlockState(ust);
            if (isler.kirilabilir(ust, yuzey) && isler.degistir(ust.below(), yuzey)) {
                isler.kaldir(ust, true);
            }
        }
    }

    /**
     * {@code sonHalka}'ya kadar halkalar: içi (zeminin altı, tabanına kadar) bütçeyle boşaltılır; içi biten halkanın yüzeyi ve
     * üstündekiler tek seferde düşer. {@code zorla}: aşama biterken kalan hepsi.
     */
    private void halkalariIsle(ServerLevel level, SinkholeSeviyesi s, int sonHalka, boolean hasar, int butce,
            boolean zorla) {
        BlockPos m = baglam().merkez();
        int kalan = butce;
        for (int h = 0; h <= sonHalka && h < halkalar.size(); h++) {
            if (dusenHalka.get(h)) {
                continue;
            }
            boolean icBitti = true;
            for (int i : halkalar.get(h)) {
                if (tamam.get(i)) {
                    continue;
                }
                if (kalan <= 0 && !zorla) {
                    icBitti = false;
                    break;
                }
                if (hasar) {
                    kalan -= iciBosalt(m, plan.sutunlar().get(i));
                }
                tamam.set(i);
            }
            if (!icBitti) {
                return;
            }
            dusenHalka.set(h);
            halkaDussun(level, s, h, hasar);
        }
    }

    private int taban(CukurPlani.Sutun c) {
        return Math.max(isler.enAltY(), zemin - c.derinlik() + 1);
    }

    private int iciBosalt(BlockPos m, CukurPlani.Sutun c) {
        int x = m.getX() + c.dx();
        int z = m.getZ() + c.dz();
        int g = isler.zemin(x, z);
        if (g == Integer.MIN_VALUE) {
            return 0;
        }
        int alt = taban(c);
        int n = 0;
        for (int y = g - 1; y >= alt; y--) {
            if (isler.kaldir(new BlockPos(x, y, z), y == alt)) {
                n++;
            }
        }
        return Math.max(1, n);
    }

    /** Halkanın yüzeyi ve üstündeki her şey düşen blok olur (tavan doluysa kaldırılır); dibe moloz; güm, toz, hasar. */
    private void halkaDussun(ServerLevel level, SinkholeSeviyesi s, int h, boolean hasar) {
        BlockPos m = baglam().merkez();
        int[] sutunlar = halkalar.get(h).clone();
        for (int k = sutunlar.length - 1; k > 0; k--) {
            int j = rastgele.nextInt(k + 1);
            int t = sutunlar[k];
            sutunlar[k] = sutunlar[j];
            sutunlar[j] = t;
        }
        int tavan = SinkholeAyarlari.dusenBlokTavani();
        if (hasar) {
            for (int i : sutunlar) {
                CukurPlani.Sutun c = plan.sutunlar().get(i);
                int x = m.getX() + c.dx();
                int z = m.getZ() + c.dz();
                int g = isler.zemin(x, z);
                int ust = isler.ust(x, z);
                if (g == Integer.MIN_VALUE || g < taban(c)) {
                    continue;
                }
                double uz = Math.max(1, Math.hypot(c.dx(), c.dz()));
                Vec3 hiz = h == 0 ? new Vec3(0, -0.2, 0) : new Vec3(-c.dx() / uz * 0.12, -0.05, -c.dz() / uz * 0.12);
                int bosluk = g - taban(c);
                if (bosluk >= 2 && rastgele.nextInt(2) == 0) {
                    isler.koy(new BlockPos(x, taban(c), z), moloz(taban(c)));
                }
                // Altında boşluk yoksa yüzey düşse yerine konar: kaldırılır, üstündekiler onun yerine iner.
                int ilk = g;
                if (bosluk == 0) {
                    isler.kaldir(new BlockPos(x, g, z), true);
                    ilk = g + 1;
                }
                for (int y = ilk; y <= ust; y++) {
                    isler.dusur(new BlockPos(x, y, z), hiz, tavan);
                }
            }
        }
        double r = h == 0 && plan.halkaSayisi() > 1 ? 0.5 * s.yaricap() + 1 : s.yaricap() + 1;
        if (h == 0) {
            level.playSound(null, m.getX() + 0.5, zemin, m.getZ() + 0.5, DepremSistemi.COKUS.get(), SoundSource.BLOCKS, 4f,
                    0.9f + rastgele.nextFloat() * 0.2f);
            cukurdakileriIsaretle(level, r, taban(plan.sutunlar().get(0)) - 1);
        } else {
            level.playSound(null, m.getX() + 0.5, zemin, m.getZ() + 0.5, SoundEvents.GRAVEL_FALL, SoundSource.BLOCKS, 3f, 0.6f);
        }
        level.playSound(null, m.getX() + 0.5, zemin, m.getZ() + 0.5, SoundEvents.SAND_FALL, SoundSource.BLOCKS, 2.5f, 0.5f);
        BlockState yuzey = level.getBlockState(new BlockPos(m.getX(), zemin, m.getZ()));
        isler.parcacik(ParticleTypes.CAMPFIRE_COSY_SMOKE, m.getX() + 0.5, zemin + 1, m.getZ() + 0.5, (int) (8 + 4 * r), r * 0.5,
                0.03);
        isler.parcacik(new BlockParticleOption(ParticleTypes.BLOCK, yuzey.isAir() ? Blocks.DIRT.defaultBlockState() : yuzey),
                m.getX() + 0.5, zemin + 1, m.getZ() + 0.5, (int) (20 + 8 * r), r * 0.5, 0.3);
        if (s.dusenBlokHasari() > 0) {
            AABB kutu = new AABB(m).inflate(r, s.derinlik() + 8, r);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, kutu, LivingEntity::isAlive)) {
                if (Math.hypot(e.getX() - m.getX() - 0.5, e.getZ() - m.getZ() - 0.5) <= r) {
                    e.hurt(level.damageSources().generic(), s.dusenBlokHasari());
                }
            }
        }
    }

    private BlockState moloz(int y) {
        return switch (rastgele.nextInt(4)) {
            case 0 -> Blocks.COARSE_DIRT.defaultBlockState();
            case 1 -> Blocks.GRAVEL.defaultBlockState();
            case 2 -> Blocks.DIRT.defaultBlockState();
            default -> (y < 0 ? Blocks.COBBLED_DEEPSLATE : Blocks.COBBLESTONE).defaultBlockState();
        };
    }

    /** Çöken alana düşen (ya da çökerken üstünde duran) oyuncular: başarım ipucu, oyuncu başına bir kez. */
    private void cukurdakileriIsaretle(ServerLevel level, double yaricap, int enAltY) {
        BlockPos m = baglam().merkez();
        for (ServerPlayer o : level.players()) {
            if (o.isSpectator() || !o.isAlive() || dusenler.contains(o.getUUID())) {
                continue;
            }
            boolean icinde = Math.hypot(o.getX() - m.getX() - 0.5, o.getZ() - m.getZ() - 0.5) <= yaricap;
            boolean dustu = o.getY() < zemin + (altAsama() == COKUS && fazGecen <= seviyeBilgisi().cekirdekTick() ? 2.5 : 0);
            if (icinde && dustu && o.getY() >= enAltY - 2) {
                dusenler.add(o.getUUID());
                MinecraftForge.EVENT_BUS.post(new BasarimIpucuEvent(o, Ipucu.CUKURA_DUSTU, this));
            }
        }
    }

    private void kenar() {
        BlockPos m = baglam().merkez();
        for (int[] d : plan.kenar()) {
            int x = m.getX() + d[0];
            int z = m.getZ() + d[1];
            int g = isler.zemin(x, z);
            if (g == Integer.MIN_VALUE) {
                continue;
            }
            BlockPos p = new BlockPos(x, g, z);
            if (isler.level().getBlockState(p).is(Blocks.GRASS_BLOCK)) {
                isler.degistir(p, Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
    }

    /** Durulma: kenardan tek tek dökülen bloklar, çakıl sızması, dipten yükselen toz. */
    private void dokul(ServerLevel level, SinkholeSeviyesi s, boolean hasar) {
        BlockPos m = baglam().merkez();
        double a = rastgele.nextDouble() * Math.PI * 2;
        double r = s.yaricap() + 0.5 + rastgele.nextDouble();
        int x = (int) Math.floor(m.getX() + 0.5 + Math.cos(a) * r);
        int z = (int) Math.floor(m.getZ() + 0.5 + Math.sin(a) * r);
        int g = isler.zemin(x, z);
        if (hasar && g != Integer.MIN_VALUE && rastgele.nextInt(3) == 0) {
            isler.dusur(new BlockPos(x, g, z), new Vec3(-Math.cos(a) * 0.12, 0, -Math.sin(a) * 0.12),
                    SinkholeAyarlari.dusenBlokTavani());
            level.playSound(null, x + 0.5, g, z + 0.5, SoundEvents.GRAVEL_FALL, SoundSource.BLOCKS, 1f, 0.8f);
        }
        double dip = zemin - s.derinlik() + 2;
        isler.parcacik(ParticleTypes.CAMPFIRE_COSY_SMOKE, m.getX() + 0.5, dip, m.getZ() + 0.5, 2, s.yaricap() * 0.3, 0.02);
    }

    @Override
    protected boolean aktifBitti() {
        return altAsama() >= DURULMA && fazGecen >= SinkholeSeviyesi.DURULMA_TICK;
    }

    @Override
    public long aktifKalanTick() {
        SinkholeSeviyesi s = seviyeBilgisi();
        int alt = Math.min(DURULMA, altAsama());
        long kalan = Math.max(0, s.sure(alt) - fazGecen);
        for (int a = alt + 1; a <= DURULMA; a++) {
            kalan += s.sure(a);
        }
        return kalan;
    }

    @Override
    protected void temizle(ServerLevel level, boolean temiz) {
        OrtamSesi.durdur(level.getServer(), SES_KATMANI, temiz ? 20 : 40);
        if (isler != null) {
            isler.ortuleriKaldir();
            if (temiz) {
                isler.dusenleriKaldir();
            }
            isler.bildir();
        }
    }

    @Override
    protected void istemciVerisiYaz(CompoundTag t) {
        SinkholeSeviyesi s = seviyeBilgisi();
        t.putInt("g", fazGecen);
        t.putFloat("R", (float) s.yaricap());
        t.putFloat("a", (float) (1.5 * seviye() / 3.0));
        t.putInt("z", zemin);
    }

    @Override
    protected void ozelDurumYaz(CompoundTag t) {
        t.putLong("tohum", tohum);
        t.putInt("fazGecen", fazGecen);
        t.putInt("zemin", zemin);
        t.putBoolean("sarkti", sarkti);
        t.putBoolean("kenar", kenarIslendi);
        t.putLongArray("tamam", tamam.toLongArray());
        t.putLongArray("dusenHalka", dusenHalka.toLongArray());
    }

    @Override
    protected void ozelDurumOku(CompoundTag t) {
        tohum = t.getLong("tohum");
        fazGecen = Math.max(0, t.getInt("fazGecen"));
        zemin = t.contains("zemin") ? t.getInt("zemin") : Integer.MIN_VALUE;
        sarkti = t.getBoolean("sarkti");
        kenarIslendi = t.getBoolean("kenar");
        tamam.clear();
        tamam.or(BitSet.valueOf(t.getLongArray("tamam")));
        dusenHalka.clear();
        dusenHalka.or(BitSet.valueOf(t.getLongArray("dusenHalka")));
    }
}
