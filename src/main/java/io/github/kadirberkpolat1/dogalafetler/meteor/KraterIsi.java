package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.KraterProfili;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginEtiketleri;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
 * ({@code dogalafetler:deprem_dayanikli}, {@code dogalafetler:yanmaz}, kırılmaz, blok varlıklı) ve sıvılar korunur. Sıradaki
 * hücrenin chunk'ı yüklü değilse iş orada bekler; iş boyutun {@code dogalafetler_meteor} kaydında durur ve chunk yüklenince
 * (yeniden açılıştan sonra da) kaldığı yerden sürer (M-25). Plan tohumdan yeniden üretilir.
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
    private final long tohum;
    private final double olcek;
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
    private boolean bekliyor;

    KraterIsi(BlockPos merkez, double yaricap, double derinlik, long tohum, double olcek, MeteorGorunumu gorunum,
            YanginKaynagi kaynak, OdulZari.Sonuc odul, boolean blokHasari) {
        this.merkez = merkez.immutable();
        this.plan = KraterProfili.planla(yaricap, derinlik, tohum, MeteorCarpmasi.UST_TEMIZLIK, blokHasari);
        this.yaricap = yaricap;
        this.derinlik = derinlik;
        this.tohum = tohum;
        this.olcek = olcek;
        this.gorunum = gorunum;
        this.kaynak = kaynak;
        this.odul = odul;
        this.blokHasari = blokHasari;
        this.rastgele = new Random(tohum);
    }

    void yaz(CompoundTag t) {
        t.putLong("merkez", merkez.asLong());
        t.putDouble("yaricap", yaricap);
        t.putDouble("derinlik", derinlik);
        t.putLong("tohum", tohum);
        t.putDouble("olcek", olcek);
        t.putByte("gorunum", (byte) gorunum.ordinal());
        t.putString("kaynakTur", kaynak.tur() == null ? "" : kaynak.tur().kimlik());
        t.putLong("afetNo", kaynak.afetNo());
        t.putBoolean("parca", odul.parca());
        t.putBoolean("cekirdek", odul.cekirdek());
        t.putBoolean("blokHasari", blokHasari);
        t.putByte("evre", (byte) evre.ordinal());
        t.putInt("sira", sira);
        t.putIntArray("sayilar", new int[] {kazilan, korunan, astarlanan, yigilan});
    }

    static KraterIsi oku(CompoundTag t) {
        AfetTuru tur = AfetTuru.kimlikIle(t.getString("kaynakTur")).orElse(null);
        YanginKaynagi kaynak = tur == null ? YanginKaynagi.KOMUT : YanginKaynagi.afet(tur, t.getLong("afetNo"));
        // Ölçekten önceki kayıtta yalnız boyut baytı var: sınıfın tipik ölçeği.
        double olcek = t.contains("olcek") ? t.getDouble("olcek") : MeteorBoyutu.kimlik(t.getByte("boyut")).tipikOlcek();
        KraterIsi is = new KraterIsi(BlockPos.of(t.getLong("merkez")), t.getDouble("yaricap"), t.getDouble("derinlik"),
                t.getLong("tohum"), olcek, MeteorGorunumu.kimlik(t.getByte("gorunum")),
                kaynak, new OdulZari.Sonuc(t.getBoolean("parca"), t.getBoolean("cekirdek")), t.getBoolean("blokHasari"));
        is.evre = Evre.values()[Math.max(0, Math.min(Evre.values().length - 1, t.getByte("evre")))];
        is.sira = Math.max(0, t.getInt("sira"));
        int[] sayilar = t.getIntArray("sayilar");
        if (sayilar.length == 4) {
            is.kazilan = sayilar[0];
            is.korunan = sayilar[1];
            is.astarlanan = sayilar[2];
            is.yigilan = sayilar[3];
        }
        return is;
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

    /** Son ilerletmede sıradaki hücrenin chunk'ı yüklü değildi (iş bekliyor). */
    public boolean bekliyor() {
        return bekliyor;
    }

    /** Kazı, astar ve kenar bitti; bitiş (yangın, ödül, olay) sırada. */
    boolean bitiste() {
        return evre == Evre.KENAR && sira >= plan.kenar().size();
    }

    /** Kayıt karşılaştırması (GameTest): evre, sıra ve sayaçlar. */
    String ozet() {
        return merkez.toShortString() + " ölçek " + (float) olcek + " " + gorunum + " " + kaynak + " " + odul + " " + evre
                + "/" + sira + " " + kazilan + "/" + korunan + "/" + astarlanan + "/" + yigilan + " t=" + tohum + " h=" + blokHasari;
    }

    /**
     * En çok {@code butce} blok işlemi yapar; kullanılan bütçeyi döndürür. Sıradaki hücrenin chunk'ı yüklü değilse durur
     * ({@link #bekliyor()}). Bitince yangın ve ödüller gelir.
     */
    int ilerlet(ServerLevel level, int butce) {
        int kullanilan = 0;
        bekliyor = false;
        while (kullanilan < butce && evre != Evre.BITTI) {
            BlockPos siradaki = siradakiKonum();
            if (siradaki != null && !MeteorYoneticisi.yuklu(level, siradaki)) {
                bekliyor = true;
                return kullanilan;
            }
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
                    } else if (!bitisYuklu(level)) {
                        bekliyor = true;
                        return kullanilan;
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

    /** Sıradaki işlemin hücresi (kenarda sütunun en üstü; evre geçişinde ve sonda {@code null}). */
    private BlockPos siradakiKonum() {
        return switch (evre) {
            case KAZI -> sira < plan.kazi().size() ? konum(plan.kazi().get(sira)) : null;
            case ASTAR -> sira < plan.astar().size() ? konum(plan.astar().get(sira)) : null;
            case KENAR -> sira < plan.kenar().size() ? merkez.offset(plan.kenar().get(sira).dx(), KENAR_ARAMA,
                    plan.kenar().get(sira).dz()) : null;
            default -> null;
        };
    }

    private BlockPos konum(KraterProfili.Hucre h) {
        return merkez.offset(h.dx(), h.dy(), h.dz());
    }

    private BlockPos konum(KraterProfili.AstarHucresi h) {
        return merkez.offset(h.dx(), h.dy(), h.dz());
    }

    /**
     * Bitiş ödülleri krater tabanına bırakır: merkez sütunu ve parça için merkezden en çok {@code 0,6 r} uzaklıktaki
     * kare. Bu karenin değdiği bütün chunk'lar yüklü olmalı; değilse bitiş bekler (senkron chunk yüklemesi yok).
     */
    private boolean bitisYuklu(ServerLevel level) {
        int d = odul.parca() ? (int) Math.ceil(0.6 * yaricap) : 0;
        for (int cx = (merkez.getX() - d) >> 4; cx <= (merkez.getX() + d) >> 4; cx++) {
            for (int cz = (merkez.getZ() - d) >> 4; cz <= (merkez.getZ() + d) >> 4; cz++) {
                if (!MeteorYoneticisi.yuklu(level, new BlockPos(cx << 4, merkez.getY(), cz << 4))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Krater kazmaz: çarpışma sert etiketli, kırılmaz ya da blok varlıklı (sandık vb.) blok. */
    static boolean sert(BlockState durum, ServerLevel level, BlockPos konum) {
        return durum.is(YanginEtiketleri.DEPREM_DAYANIKLI) || durum.is(YanginEtiketleri.YANMAZ)
                || durum.getDestroySpeed(level, konum) < 0 || durum.hasBlockEntity();
    }

    private void kaz(ServerLevel level, KraterProfili.Hucre h) {
        BlockPos p = konum(h);
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
        BlockPos p = konum(h);
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
            MeteorYoneticisi.sogumaEkle(level, p, level.getGameTime() + 20L * MeteorAyarlari.magmaSogumaSaniye(),
                    gorunum == MeteorGorunumu.AY ? Blocks.SMOOTH_BASALT.defaultBlockState()
                            : Blocks.BLACKSTONE.defaultBlockState());
        }
    }

    private void yig(ServerLevel level, KraterProfili.AstarHucresi h) {
        for (int dy = KENAR_ARAMA; dy >= -KENAR_ARAMA; dy--) {
            BlockPos zemin = merkez.offset(h.dx(), dy, h.dz());
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
                MeteorAyarlari.yanginYogunlugu(olcek));
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
        DogalAfetler.LOGGER.info("meteor krateri tamam: {} (ölçek {}, {}, {}): kazılan {}, korunan {}, astar {}, kenar {}; "
                + "ateş alanı r={} ilk tutuşan {} hücre; ödül parça={} çekirdek={}; blok hasarı {}", merkez.toShortString(),
                (float) olcek, gorunum, level.dimension().location(), kazilan, korunan, astarlanan, yigilan, yanginYaricapi,
                tutusan, odul.parca(), odul.cekirdek(), blokHasari ? "açık" : "kapalı");
        MinecraftForge.EVENT_BUS.post(new MeteorCarptiEvent(level, merkez, olcek, kaynak, odul, false));
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
