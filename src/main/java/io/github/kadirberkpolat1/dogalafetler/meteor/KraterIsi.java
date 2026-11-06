package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.KraterProfili;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginEtiketleri;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

/**
 * Bir çarpmanın krater işi: kazı (yukarıdan aşağı), astar, kenar enkazı, sonra yangın, ödüller ve {@link MeteorCarptiEvent}.
 * İşlemler tick başına ortak bütçeyle ({@link MeteorAyarlari#kraterBlokButcesi} × TPS kısması) yapılır. Sert bloklar
 * ({@code dogalafetler:deprem_dayanikli}, {@code dogalafetler:yanmaz}, kırılmaz, blok varlıklı) ve sıvılar korunur.
 */
public final class KraterIsi {
    /** Komşu güncellemesi + istemci, damla yok (iki parçalı blokların diğer yarısı eşya düşürmez). */
    private static final int BAYRAKLAR = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
    private static final int KENAR_ARAMA = 3;

    private enum Evre {
        KAZI,
        ASTAR,
        KENAR,
        BITTI
    }

    private final BlockPos merkez;
    private final KraterProfili.Plan plan;
    private final double yaricap;
    private final double derinlik;
    private final MeteorBoyutu boyut;
    private final MeteorGorunumu gorunum;
    private final YanginKaynagi kaynak;
    private final OdulZari.Sonuc odul;
    private final boolean blokHasari;
    private final Random rastgele;
    private Evre evre = Evre.KAZI;
    private int sira;
    private int kazilan;
    private int korunan;
    private int astarlanan;
    private int yigilan;

    KraterIsi(BlockPos merkez, KraterProfili.Plan plan, double yaricap, double derinlik, MeteorBoyutu boyut,
            MeteorGorunumu gorunum, YanginKaynagi kaynak, OdulZari.Sonuc odul, boolean blokHasari, Random rastgele) {
        this.merkez = merkez;
        this.plan = plan;
        this.yaricap = yaricap;
        this.derinlik = derinlik;
        this.boyut = boyut;
        this.gorunum = gorunum;
        this.kaynak = kaynak;
        this.odul = odul;
        this.blokHasari = blokHasari;
        this.rastgele = rastgele;
    }

    public boolean bitti() {
        return evre == Evre.BITTI;
    }

    public BlockPos merkez() {
        return merkez;
    }

    public OdulZari.Sonuc odul() {
        return odul;
    }

    public int kazilan() {
        return kazilan;
    }

    public int korunan() {
        return korunan;
    }

    /** En çok {@code butce} blok işlemi yapar; kullanılan bütçeyi döndürür. Bitince yangın ve ödüller gelir. */
    int ilerlet(ServerLevel level, int butce) {
        int kullanilan = 0;
        while (kullanilan < butce && evre != Evre.BITTI) {
            switch (evre) {
                case KAZI -> {
                    if (sira < plan.kazi().size()) {
                        kaz(level, plan.kazi().get(sira++));
                        kullanilan++;
                    } else {
                        evre = Evre.ASTAR;
                        sira = 0;
                    }
                }
                case ASTAR -> {
                    if (sira < plan.astar().size()) {
                        astarla(level, plan.astar().get(sira++));
                        kullanilan++;
                    } else {
                        evre = Evre.KENAR;
                        sira = 0;
                    }
                }
                case KENAR -> {
                    if (sira < plan.kenar().size()) {
                        yig(level, plan.kenar().get(sira++));
                        kullanilan++;
                    } else {
                        bitir(level);
                        evre = Evre.BITTI;
                    }
                }
                default -> {
                }
            }
        }
        return kullanilan;
    }

    /** Krater kazmaz: çarpışma sert etiketli, kırılmaz ya da blok varlıklı (sandık vb.) blok. */
    static boolean sert(BlockState durum, ServerLevel level, BlockPos konum) {
        return durum.is(YanginEtiketleri.DEPREM_DAYANIKLI) || durum.is(YanginEtiketleri.YANMAZ)
                || durum.getDestroySpeed(level, konum) < 0 || durum.hasBlockEntity();
    }

    private void kaz(ServerLevel level, KraterProfili.Hucre h) {
        BlockPos p = merkez.offset(h.dx(), h.dy(), h.dz());
        if (!level.isLoaded(p)) {
            return;
        }
        BlockState durum = level.getBlockState(p);
        if (durum.isAir() || !durum.getFluidState().isEmpty()) {
            return;
        }
        if (sert(durum, level, p)) {
            korunan++;
            return;
        }
        level.setBlock(p, Blocks.AIR.defaultBlockState(), BAYRAKLAR);
        kazilan++;
    }

    private void astarla(ServerLevel level, KraterProfili.AstarHucresi h) {
        BlockPos p = merkez.offset(h.dx(), h.dy(), h.dz());
        if (!level.isLoaded(p)) {
            return;
        }
        BlockState durum = level.getBlockState(p);
        if (durum.isAir() || !durum.getFluidState().isEmpty() || !durum.isCollisionShapeFullBlock(level, p)) {
            return;
        }
        if (sert(durum, level, p)) {
            korunan++;
            return;
        }
        level.setBlock(p, malzeme(h.malzeme()), BAYRAKLAR);
        astarlanan++;
        if (h.malzeme() == KraterProfili.Malzeme.MAGMA) {
            MeteorYoneticisi.sogumaEkle(level, p, level.getGameTime() + 20L * MeteorAyarlari.sicakSaniye(),
                    gorunum == MeteorGorunumu.AY ? Blocks.SMOOTH_BASALT.defaultBlockState()
                            : Blocks.BLACKSTONE.defaultBlockState());
        }
    }

    private void yig(ServerLevel level, KraterProfili.AstarHucresi h) {
        for (int dy = KENAR_ARAMA; dy >= -KENAR_ARAMA; dy--) {
            BlockPos zemin = merkez.offset(h.dx(), dy, h.dz());
            if (!level.isLoaded(zemin)) {
                return;
            }
            BlockState durum = level.getBlockState(zemin);
            if (!durum.isCollisionShapeFullBlock(level, zemin)) {
                continue;
            }
            BlockPos ust = zemin.above();
            BlockState ustDurum = level.getBlockState(ust);
            if ((ustDurum.isAir() || ustDurum.canBeReplaced()) && ustDurum.getFluidState().isEmpty()) {
                level.setBlock(ust, malzeme(h.malzeme()), BAYRAKLAR);
                yigilan++;
            }
            return;
        }
    }

    private BlockState malzeme(KraterProfili.Malzeme m) {
        boolean ay = gorunum == MeteorGorunumu.AY;
        return switch (m) {
            case MAGMA -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case KOYU -> (ay ? Blocks.SMOOTH_BASALT : Blocks.BLACKSTONE).defaultBlockState();
            case ORTA -> (ay ? Blocks.TUFF : Blocks.BASALT).defaultBlockState();
            case ACIK -> (ay ? Blocks.CALCITE : Blocks.COARSE_DIRT).defaultBlockState();
        };
    }

    private void bitir(ServerLevel level) {
        int yanginYaricapi = (int) Math.ceil(yaricap) + MeteorAyarlari.yanginYaricapEki();
        int tutusan = YanginYoneticisi.alanTutustur(level, merkez, yanginYaricapi, kaynak,
                MeteorAyarlari.yanginYogunlugu(boyut));
        if (odul.cekirdek()) {
            MeteorOdulleri.birak(level, tabanNoktasi(level, 0, 0),
                    new ItemStack(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get()), 0);
        }
        if (odul.parca()) {
            double r = rastgele.nextDouble() * 0.6 * yaricap;
            double aci = rastgele.nextDouble() * 2 * Math.PI;
            MeteorOdulleri.birak(level, tabanNoktasi(level, (int) Math.round(Math.cos(aci) * r),
                    (int) Math.round(Math.sin(aci) * r)), new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get()),
                    MeteorAyarlari.sicakSaniye());
        }
        DogalAfetler.LOGGER.info("meteor krateri tamam: {} ({}, {}, {}): kazılan {}, korunan {}, astar {}, kenar {}; "
                + "ateş alanı r={} ilk tutuşan {} hücre; ödül parça={} çekirdek={}; blok hasarı {}", merkez.toShortString(),
                boyut, gorunum, level.dimension().location(), kazilan, korunan, astarlanan, yigilan, yanginYaricapi,
                tutusan, odul.parca(), odul.cekirdek(), blokHasari ? "açık" : "kapalı");
        MinecraftForge.EVENT_BUS.post(new MeteorCarptiEvent(level, merkez, boyut, kaynak, odul, false));
    }

    /** Krater tabanında {@code (dx, dz)} sütununun ilk boş noktası (sert blok kalmışsa üstünde). */
    private Vec3 tabanNoktasi(ServerLevel level, int dx, int dz) {
        int k = blokHasari ? KraterProfili.derinlik(Math.sqrt(dx * dx + dz * dz), yaricap, derinlik) : 0;
        BlockPos p = merkez.offset(dx, Math.max(1 - k, -64), dz);
        for (int i = 0; i < k + MeteorCarpmasi.UST_TEMIZLIK + 2 && !level.getBlockState(p).isAir(); i++) {
            p = p.above();
        }
        return Vec3.atBottomCenterOf(p).add(0, 0.1, 0);
    }
}
