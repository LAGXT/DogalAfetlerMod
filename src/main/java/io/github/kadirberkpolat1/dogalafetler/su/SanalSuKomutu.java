package io.github.kadirberkpolat1.dogalafetler.su;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** {@code /dogalafet spike su ...}: spike deney komutları (yalnız OP; kök komut yetkiyi denetler). */
final class SanalSuKomutu {
    private record Anlik(int x0, int y0, int z0, int x1, int y1, int z1, int[] kimlik) {
    }

    private static final Map<String, Anlik> ANLIKLAR = new HashMap<>();
    private static MagaraCebi hazirCep;
    private static final int HASH_YARICAP = 64;
    /** Kıyı hash kutusunun deniz seviyesinden yüksekliği: deniz +6 ve 24 bloğluk dalga kutunun içinde kalmalı. */
    private static final int HASH_DENIZ_USTU = 32;

    private SanalSuKomutu() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> olustur() {
        return Commands.literal("spike").then(Commands.literal("su")
                .then(Commands.literal("deniz").then(Commands.argument("artis", DoubleArgumentType.doubleArg(0, 6))
                        .executes(c -> deniz(c, 100))
                        .then(Commands.argument("sure", IntegerArgumentType.integer(0))
                                .executes(c -> deniz(c, IntegerArgumentType.getInteger(c, "sure"))))))
                .then(Commands.literal("dalga").executes(c -> dalga(c, 8))
                        .then(Commands.argument("yukseklik", DoubleArgumentType.doubleArg(1, 24))
                                .executes(c -> dalga(c, DoubleArgumentType.getDouble(c, "yukseklik")))))
                .then(Commands.literal("magara")
                        .then(Commands.literal("ara").executes(SanalSuKomutu::magaraAra))
                        .then(Commands.literal("hazirla")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(c -> hazirla(c, IntegerArgumentType.getInteger(c, "x"),
                                                                IntegerArgumentType.getInteger(c, "y"),
                                                                IntegerArgumentType.getInteger(c, "z")))))))
                        .then(Commands.literal("baslat")
                                .then(Commands.argument("bekleme", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("buhar", DoubleArgumentType.doubleArg(0.001, 1))
                                                .executes(c -> baslat(c, IntegerArgumentType.getInteger(c, "bekleme"),
                                                        DoubleArgumentType.getDouble(c, "buhar"))))))
                        .then(Commands.literal("kurut").then(Commands.argument("hiz", DoubleArgumentType.doubleArg(0.001, 1))
                                .executes(c -> kurut(c, DoubleArgumentType.getDouble(c, "hiz"))))))
                .then(Commands.literal("cekilme").executes(c -> cekilme(c, 30, 3))
                        .then(Commands.argument("yaricap", DoubleArgumentType.doubleArg(4, 96))
                                .then(Commands.argument("derinlik", DoubleArgumentType.doubleArg(0.5, 8))
                                        .executes(c -> cekilme(c, DoubleArgumentType.getDouble(c, "yaricap"),
                                                DoubleArgumentType.getDouble(c, "derinlik"))))))
                .then(Commands.literal("kapat").executes(SanalSuKomutu::kapat))
                .then(Commands.literal("hash").then(Commands.argument("etiket", StringArgumentType.word())
                        .executes(c -> hash(c, null))
                        .then(Commands.argument("onceki", StringArgumentType.word())
                                .executes(c -> hash(c, StringArgumentType.getString(c, "onceki"))))))
                .then(Commands.literal("yol")
                        .then(Commands.literal("mixin").executes(c -> yol(c, SanalSu.Yol.MIXIN)))
                        .then(Commands.literal("olay").executes(c -> yol(c, SanalSu.Yol.OLAY))))
                .then(Commands.literal("izle")
                        .then(Commands.literal("ac").executes(c -> izle(c, true)))
                        .then(Commands.literal("kapat").executes(c -> izle(c, false))))
                .then(Commands.literal("konum").then(Commands.literal("kiyi").executes(c -> kiyi(c, -40))
                        .then(Commands.argument("aci", DoubleArgumentType.doubleArg(-180, 180))
                                .executes(c -> kiyi(c, DoubleArgumentType.getDouble(c, "aci")))))));
    }

    private static int bildir(CommandContext<CommandSourceStack> c, String m) {
        DogalAfetler.LOGGER.info("SPIKE-SU komut: {}", m);
        c.getSource().sendSuccess(() -> Component.literal(m), false);
        return 1;
    }

    private static String fmt(String bicim, Object... a) {
        return String.format(Locale.ROOT, bicim, a);
    }

    private static int deniz(CommandContext<CommandSourceStack> c, int sure) {
        ServerLevel l = c.getSource().getLevel();
        double artis = DoubleArgumentType.getDouble(c, "artis");
        SanalSu.SUNUCU.denizAyarla(artis, l.getGameTime(), sure);
        SanalSuSunucu.yayinla(l);
        return bildir(c, "deniz artis=" + artis + " sure=" + sure);
    }

    private static int dalga(CommandContext<CommandSourceStack> c, double yukseklik) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel l = p.serverLevel();
        Vec3 bak = p.getLookAngle();
        double bx = bak.x;
        double bz = bak.z;
        double boy = Math.sqrt(bx * bx + bz * bz);
        if (boy < 1e-3) {
            bx = 1;
            bz = 0;
            boy = 1;
        }
        bx /= boy;
        bz /= boy;
        TsunamiDalgasi w = new TsunamiDalgasi(p.getX() + bx * 48, p.getZ() + bz * 48, -bx, -bz, l.getGameTime(), 0.25,
                yukseklik, 80, 30, l.getSeaLevel(), 88);
        SanalSu.SUNUCU.dalgaEkle(w);
        SanalSuSunucu.yayinla(l);
        return bildir(c, fmt("dalga yukseklik=%.1f baslangic=(%.1f,%.1f) yon=(%.2f,%.2f)", yukseklik, w.x0(), w.z0(),
                w.yonX(), w.yonZ()));
    }

    private static int zemin(Level l, int x, int z) {
        return l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static boolean deniz(Level l, int x, int z) {
        int y = zemin(l, x, z);
        return y == l.getSeaLevel() && l.getFluidState(new BlockPos(x, y - 1, z)).is(FluidTags.WATER);
    }

    /** Yakındaki deniz sütunlarının ağırlık merkezine birim yön; deniz yoksa {@code null}. */
    private static double[] denizYonu(Level l, double px, double pz, int r) {
        double sx = 0;
        double sz = 0;
        int n = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (deniz(l, (int) Math.floor(px) + dx, (int) Math.floor(pz) + dz)) {
                    sx += dx;
                    sz += dz;
                    n++;
                }
            }
        }
        double boy = Math.sqrt(sx * sx + sz * sz);
        return n == 0 || boy < 1e-6 ? null : new double[] {sx / boy, sz / boy};
    }

    private static float yaw(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
    }

    /** Spawn çevresinde deterministik bir plaj sütunu bulur, oraya ışınlar ve denize (+açı) döndürür. */
    private static int kiyi(CommandContext<CommandSourceStack> c, double aci) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel l = p.serverLevel();
        BlockPos sp = l.getSharedSpawnPos();
        int deniz = l.getSeaLevel();
        long enIyi = Long.MAX_VALUE;
        int bx = 0;
        int bz = 0;
        for (int dx = -48; dx <= 48; dx++) {
            for (int dz = -48; dz <= 48; dz++) {
                int x = sp.getX() + dx;
                int z = sp.getZ() + dz;
                long uz = (long) dx * dx + (long) dz * dz;
                if (uz >= enIyi || zemin(l, x, z) != deniz + 1) {
                    continue;
                }
                int n = 0;
                int top = 0;
                for (int ex = -10; ex <= 10; ex += 2) {
                    for (int ez = -10; ez <= 10; ez += 2) {
                        top++;
                        if (deniz(l, x + ex, z + ez)) {
                            n++;
                        }
                    }
                }
                double o = n / (double) top;
                if (o >= 0.3 && o <= 0.55) {
                    enIyi = uz;
                    bx = x;
                    bz = z;
                }
            }
        }
        if (enIyi == Long.MAX_VALUE) {
            return bildir(c, "kiyi bulunamadi");
        }
        double[] yon = denizYonu(l, bx + 0.5, bz + 0.5, 12);
        float yw = (yon == null ? 0f : yaw(yon[0], yon[1])) + (float) aci;
        p.teleportTo(l, bx + 0.5, deniz + 1, bz + 0.5, yw, 12f);
        return bildir(c, "kiyi " + bx + " " + (deniz + 1) + " " + bz + " yaw=" + yw);
    }

    private static MagaraDolumu.Hucre hucre(ServerLevel l) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        return (x, y, z) -> {
            m.set(x, y, z);
            return l.isLoaded(m) && l.getBlockState(m).isAir() && !l.canSeeSky(m);
        };
    }

    /** Kapalı bir boşluğu süre bütçesiyle arar (en çok 1,5 sn); bulursa {@link #hazirla} ile hazırlar. */
    private static int magaraAra(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel l = p.serverLevel();
        MagaraDolumu.Hucre h = hucre(l);
        BlockPos o = p.blockPosition();
        long t0 = System.nanoTime();
        long butce = 1_500_000_000L;
        int deneme = 0;
        int acik = 0;
        int kucuk = 0;
        for (int r = 0; r <= 64; r += 3) {
            for (int dx = -r; dx <= r; dx += 3) {
                for (int dz = -r; dz <= r; dz += 3) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int x = o.getX() + dx;
                    int z = o.getZ() + dz;
                    for (int y = zemin(l, x, z) - 6; y > l.getMinBuildHeight() + 6; y--) {
                        if (System.nanoTime() - t0 > butce) {
                            return bildir(c, fmt("magara ara: butce doldu deneme=%d acik=%d kucuk=%d", deneme, acik, kucuk));
                        }
                        if (!h.bos(x, y, z) || h.bos(x, y - 1, z) || !h.bos(x, y + 1, z)) {
                            continue;
                        }
                        deneme++;
                        MagaraCebi cep = MagaraDolumu.doldur(h, x, y, z, 3000, 6000);
                        if (cep.acik()) {
                            acik++;
                        } else if (cep.hacim() < 300) {
                            kucuk++;
                        } else {
                            DogalAfetler.LOGGER.info("SPIKE-SU magara ara: bulundu ({},{},{}) deneme={} sure={} ms", x, y, z,
                                    deneme, fmt("%.0f", (System.nanoTime() - t0) / 1e6));
                            return hazirla(c, x, y, z);
                        }
                        y -= 4;
                    }
                }
            }
        }
        return bildir(c, fmt("magara ara: bulunamadi deneme=%d acik=%d kucuk=%d", deneme, acik, kucuk));
    }

    /** Cebi hesaplar (su eklemez), kamerayı en geniş açık su yüzeyinin 2,6 blok üstüne, aşağı bakacak şekilde koyar. */
    private static int hazirla(CommandContext<CommandSourceStack> c, int x, int y, int z) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel l = p.serverLevel();
        MagaraDolumu.Hucre h = hucre(l);
        long t0 = System.nanoTime();
        MagaraCebi tam = MagaraDolumu.doldur(h, x, y, z, 20_000, 20_000);
        if (tam.bos()) {
            return bildir(c, "magara hazirla: tohum bos degil");
        }
        int hedef = tam.acik() ? 4000 : Math.min(4000, (int) (tam.hacim() * 0.6));
        MagaraCebi cep = MagaraDolumu.doldur(h, x, y, z, hedef, 20_000);
        double sure = (System.nanoTime() - t0) / 1e6;
        if (cep.bos()) {
            return bildir(c, fmt("magara hazirla: cep bos (ilk kat hedefi asiyor) tohum=(%d,%d,%d) tam=%d acik=%b hedef=%d sure=%.1fms",
                    x, y, z, tam.hacim(), tam.acik(), hedef, sure));
        }
        int su = cep.ustY();
        int enIyi = -1;
        int kx = 0;
        int kz = 0;
        for (long k : cep.sutunAnahtarlari()) {
            int sx = (int) (k >> 32);
            int sz = (int) k;
            if (!h.bos(sx, su, sz) || !h.bos(sx, su + 1, sz) || !h.bos(sx, su + 2, sz) || !h.bos(sx, su + 3, sz)) {
                continue;
            }
            int acikKomsu = acikYuzey(cep, h, sx, sz, su, 3);
            if (acikKomsu > enIyi) {
                enIyi = acikKomsu;
                kx = sx;
                kz = sz;
            }
        }
        if (enIyi < 0) {
            return bildir(c, fmt("magara hazirla: 3 blok bosluklu kamera sutunu yok tohum=(%d,%d,%d) cep=%d", x, y, z,
                    cep.hacim()));
        }
        double yx = 0;
        double yz = 0;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (cep.icerir(kx + dx, su - 1, kz + dz) && h.bos(kx + dx, su, kz + dz)) {
                    yx += dx;
                    yz += dz;
                }
            }
        }
        hazirCep = cep;
        double goz = su + 2.6;
        float yw = Math.abs(yx) + Math.abs(yz) < 1e-6 ? 0f : yaw(yx, yz);
        p.teleportTo(l, kx + 0.5, goz - p.getEyeHeight(), kz + 0.5, yw, 70f);
        return bildir(c, fmt("magara hazirla tohum=(%d,%d,%d) tam=%d acik=%b cep=%d sutun=%d minY=%d ustY=%d kamera=(%d,%.1f,%d) yaw=%.0f pitch=70 acik_yuzey_7x7=%d/49 sure=%.1fms",
                x, y, z, tam.hacim(), tam.acik(), cep.hacim(), cep.sutunSayisi(), cep.minY(), cep.ustY(), kx, goz, kz, yw,
                enIyi, sure));
    }

    /** Sütun çevresindeki ({@code 2r+1} kare) üstü açık su yüzeyi sayısı. */
    private static int acikYuzey(MagaraCebi cep, MagaraDolumu.Hucre h, int x, int z, int su, int r) {
        int n = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (cep.icerir(x + dx, su - 1, z + dz) && h.bos(x + dx, su, z + dz)) {
                    n++;
                }
            }
        }
        return n;
    }

    private static int baslat(CommandContext<CommandSourceStack> c, int bekleme, double buhar) {
        MagaraCebi cep = hazirCep;
        if (cep == null || cep.bos()) {
            return bildir(c, "magara baslat: hazir cep yok");
        }
        ServerLevel l = c.getSource().getLevel();
        int x0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int z0 = Integer.MAX_VALUE;
        int z1 = Integer.MIN_VALUE;
        for (long k : cep.sutunAnahtarlari()) {
            int x = (int) (k >> 32);
            int z = (int) k;
            x0 = Math.min(x0, x);
            x1 = Math.max(x1, x);
            z0 = Math.min(z0, z);
            z1 = Math.max(z1, z);
        }
        long[] kutu = {x0 - 2, cep.minY() - 2, z0 - 2, x1 + 2, cep.ustY() + 3, z1 + 2, 0};
        kutu[6] = SanalSu.bolgeHash(l, (int) kutu[0], (int) kutu[1], (int) kutu[2], (int) kutu[3], (int) kutu[4],
                (int) kutu[5]);
        MagaraZamani z = new MagaraZamani(l.getGameTime(), cep.minY(), cep.ustY(), 0.1, bekleme, buhar);
        SanalSu.SUNUCU.magaraEkle(cep, z);
        SanalSuSunucu.MAGARA_HASH.put(cep, kutu);
        SanalSuSunucu.yayinla(l);
        return bildir(c, fmt("magara baslat hacim=%d hashOnce=%s kutu=[%d,%d,%d..%d,%d,%d] bekleme=%d buhar=%.3f",
                cep.hacim(), Long.toHexString(kutu[6]), kutu[0], kutu[1], kutu[2], kutu[3], kutu[4], kutu[5], bekleme,
                buhar));
    }

    /** Bekleyen mağaraların buharlaşmasını şimdi başlatır. */
    private static int kurut(CommandContext<CommandSourceStack> c, double hiz) {
        ServerLevel l = c.getSource().getLevel();
        long t = l.getGameTime();
        SanalSuDunyasi d = SanalSu.SUNUCU;
        for (int i = 0; i < d.magaralar().size(); i++) {
            MagaraZamani z = d.magaralar().get(i).zaman();
            double dolma = (z.ustY() - z.minY()) / z.dolmaHizi();
            d.magaraZamaniDegistir(i, new MagaraZamani(t - dolma, z.minY(), z.ustY(), z.dolmaHizi(), 0, hiz));
        }
        SanalSuSunucu.yayinla(l);
        return bildir(c, "magara kurut hiz=" + hiz + " adet=" + d.magaralar().size());
    }

    private static int cekilme(CommandContext<CommandSourceStack> c, double yaricap, double derinlik)
            throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel l = p.serverLevel();
        double[] yon = denizYonu(l, p.getX(), p.getZ(), 24);
        if (yon == null) {
            return bildir(c, "cekilme: yakinda deniz yok");
        }
        double cx = p.getX() + yon[0] * yaricap * 0.6;
        double cz = p.getZ() + yon[1] * yaricap * 0.6;
        SanalSu.SUNUCU.cekilmeAyarla(new Cekilme(cx, cz, yaricap, derinlik, l.getGameTime(), 100));
        SanalSuSunucu.yayinla(l);
        return bildir(c, fmt("cekilme merkez=(%.1f,%.1f) yaricap=%.1f derinlik=%.1f", cx, cz, yaricap, derinlik));
    }

    private static int kapat(CommandContext<CommandSourceStack> c) {
        SanalSu.SUNUCU.temizle();
        SanalSuSunucu.MAGARA_HASH.clear();
        SanalSuSunucu.yayinla(c.getSource().getLevel());
        return bildir(c, "kapat");
    }

    /**
     * {@code hash <etiket>}: kaynak konum çevresindeki kutunun (±64 x/z, dünya tabanından deniz seviyesi +32'ye) kimlik anlık
     * görüntüsünü saklar. {@code hash <etiket> <onceki>}: öncekinin kutusunu yeniden okur, iki hash'i ve farklı blokları yazar.
     */
    private static int hash(CommandContext<CommandSourceStack> c, String onceki) {
        ServerLevel l = c.getSource().getLevel();
        String etiket = StringArgumentType.getString(c, "etiket");
        long t0 = System.nanoTime();
        Anlik eski = onceki == null ? null : ANLIKLAR.get(onceki);
        if (onceki != null && eski == null) {
            return bildir(c, "HASH: onceki anlik yok: " + onceki);
        }
        Anlik a;
        if (eski != null) {
            a = new Anlik(eski.x0, eski.y0, eski.z0, eski.x1, eski.y1, eski.z1,
                    SanalSu.bolgeKimlikleri(l, eski.x0, eski.y0, eski.z0, eski.x1, eski.y1, eski.z1));
        } else {
            BlockPos m = BlockPos.containing(c.getSource().getPosition());
            int x0 = m.getX() - HASH_YARICAP;
            int z0 = m.getZ() - HASH_YARICAP;
            int x1 = m.getX() + HASH_YARICAP;
            int z1 = m.getZ() + HASH_YARICAP;
            int y0 = l.getMinBuildHeight();
            int y1 = l.getSeaLevel() + HASH_DENIZ_USTU;
            a = new Anlik(x0, y0, z0, x1, y1, z1, SanalSu.bolgeKimlikleri(l, x0, y0, z0, x1, y1, z1));
        }
        ANLIKLAR.put(etiket, a);
        long h = SanalSu.hash(a.kimlik);
        double sure = (System.nanoTime() - t0) / 1e6;
        if (eski == null) {
            return bildir(c, fmt("HASH etiket=%s kutu=[%d,%d,%d..%d,%d,%d] blok=%d deger=%s sure=%.0fms", etiket, a.x0, a.y0,
                    a.z0, a.x1, a.y1, a.z1, a.kimlik.length, Long.toHexString(h), sure));
        }
        long ho = SanalSu.hash(eski.kimlik);
        int fark = 0;
        int ny = a.y1 - a.y0 + 1;
        int nz = a.z1 - a.z0 + 1;
        StringBuilder ornek = new StringBuilder();
        for (int i = 0; i < a.kimlik.length; i++) {
            if (a.kimlik[i] != eski.kimlik[i]) {
                if (fark < 8) {
                    int y = a.y0 + i % ny;
                    int z = a.z0 + (i / ny) % nz;
                    int x = a.x0 + i / (ny * nz);
                    ornek.append(fmt(" (%d,%d,%d) %s -> %s;", x, y, z, Block.stateById(eski.kimlik[i]),
                            Block.stateById(a.kimlik[i])));
                }
                fark++;
            }
        }
        return bildir(c, fmt("HASH karsilastir etiket=%s onceki=%s once=%s sonra=%s esit=%b farkli_blok=%d sure=%.0fms%s",
                etiket, onceki, Long.toHexString(ho), Long.toHexString(h), h == ho, fark, sure, ornek));
    }

    private static int yol(CommandContext<CommandSourceStack> c, SanalSu.Yol y) {
        SanalSu.yol = y;
        SanalSuSunucu.yayinla(c.getSource().getLevel());
        return bildir(c, "yol=" + y);
    }

    private static int izle(CommandContext<CommandSourceStack> c, boolean ac) {
        SanalSuSunucu.izle = ac;
        return bildir(c, "izle=" + ac);
    }
}
