package io.github.kadirberkpolat1.dogalafetler.su;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** {@code /dogalafet spike su ...}: spike deney komutları (yalnız OP; kök komut yetkiyi denetler). */
final class SanalSuKomutu {
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
                .then(Commands.literal("magara").executes(c -> magara(c, 200, 0.05))
                        .then(Commands.literal("kurut").then(Commands.argument("hiz", DoubleArgumentType.doubleArg(0.001, 1))
                                .executes(c -> kurut(c, DoubleArgumentType.getDouble(c, "hiz")))))
                        .then(Commands.argument("bekleme", IntegerArgumentType.integer(0))
                                .then(Commands.argument("buhar", DoubleArgumentType.doubleArg(0.001, 1))
                                        .executes(c -> magara(c, IntegerArgumentType.getInteger(c, "bekleme"),
                                                DoubleArgumentType.getDouble(c, "buhar"))))))
                .then(Commands.literal("cekilme").executes(c -> cekilme(c, 30, 3))
                        .then(Commands.argument("yaricap", DoubleArgumentType.doubleArg(4, 96))
                                .then(Commands.argument("derinlik", DoubleArgumentType.doubleArg(0.5, 8))
                                        .executes(c -> cekilme(c, DoubleArgumentType.getDouble(c, "yaricap"),
                                                DoubleArgumentType.getDouble(c, "derinlik"))))))
                .then(Commands.literal("kapat").executes(SanalSuKomutu::kapat))
                .then(Commands.literal("hash").then(Commands.argument("etiket", StringArgumentType.word())
                        .executes(SanalSuKomutu::hash)))
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
        return bildir(c, String.format("dalga yukseklik=%.1f baslangic=(%.1f,%.1f) yon=(%.2f,%.2f)", yukseklik, w.x0(),
                w.z0(), w.yonX(), w.yonZ()));
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

    private static int magara(CommandContext<CommandSourceStack> c, int bekleme, double buhar) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel l = p.serverLevel();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        MagaraDolumu.Hucre h = (x, y, z) -> {
            m.set(x, y, z);
            return l.isLoaded(m) && l.getBlockState(m).isAir() && !l.canSeeSky(m);
        };
        BlockPos o = p.blockPosition();
        long t0 = System.nanoTime();
        int deneme = 0;
        for (int r = 0; r <= 96 && deneme < 80; r += 4) {
            for (int dx = -r; dx <= r && deneme < 80; dx += 4) {
                for (int dz = -r; dz <= r && deneme < 80; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int x = o.getX() + dx;
                    int z = o.getZ() + dz;
                    int ust = zemin(l, x, z) - 8;
                    for (int y = ust; y > l.getMinBuildHeight() + 5; y--) {
                        if (!h.bos(x, y, z) || h.bos(x, y - 1, z)) {
                            continue;
                        }
                        deneme++;
                        MagaraCebi tam = MagaraDolumu.doldur(h, x, y, z, 20_000, 20_000);
                        if (tam.hacim() < 200) {
                            break;
                        }
                        int hedef = tam.acik() ? 4000 : Math.min(4000, (int) (tam.hacim() * 0.6));
                        MagaraCebi cep = MagaraDolumu.doldur(h, x, y, z, hedef, 20_000);
                        int su = cep.ustY();
                        for (long k : cep.sutunAnahtarlari()) {
                            int sx = (int) (k >> 32);
                            int sz = (int) k;
                            if (h.bos(sx, su, sz) && h.bos(sx, su + 1, sz) && h.bos(sx, su + 2, sz)) {
                                return magaraBaslat(c, p, l, cep, sx, sz, bekleme, buhar, deneme, t0);
                            }
                        }
                        break;
                    }
                }
            }
        }
        return bildir(c, "magara bulunamadi deneme=" + deneme);
    }

    private static int magaraBaslat(CommandContext<CommandSourceStack> c, ServerPlayer p, ServerLevel l, MagaraCebi cep,
            int sx, int sz, int bekleme, double buhar, int deneme, long t0) {
        int x0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int z0 = Integer.MAX_VALUE;
        int z1 = Integer.MIN_VALUE;
        double ox = 0;
        double oz = 0;
        for (long k : cep.sutunAnahtarlari()) {
            int x = (int) (k >> 32);
            int z = (int) k;
            x0 = Math.min(x0, x);
            x1 = Math.max(x1, x);
            z0 = Math.min(z0, z);
            z1 = Math.max(z1, z);
            ox += x;
            oz += z;
        }
        ox /= cep.sutunSayisi();
        oz /= cep.sutunSayisi();
        long[] kutu = {x0 - 2, cep.minY() - 2, z0 - 2, x1 + 2, cep.ustY() + 3, z1 + 2, 0};
        kutu[6] = SanalSu.bolgeHash(l, (int) kutu[0], (int) kutu[1], (int) kutu[2], (int) kutu[3], (int) kutu[4],
                (int) kutu[5]);
        double bulma = (System.nanoTime() - t0) / 1e6;
        long t = l.getGameTime();
        MagaraZamani z = new MagaraZamani(t, cep.minY(), cep.ustY(), 0.1, bekleme, buhar);
        SanalSu.SUNUCU.magaraEkle(cep, z);
        SanalSuSunucu.MAGARA_HASH.put(cep, kutu);
        SanalSuSunucu.yayinla(l);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(l, sx + 0.5, cep.ustY() + 0.4, sz + 0.5, yaw(ox - sx, oz - sz), 35f);
        return bildir(c, String.format(
                "magara hacim=%d sutun=%d minY=%d ustY=%d acik=%b ziyaret=%d deneme=%d bulma=%.1fms hashOnce=%s kutu=[%d,%d,%d..%d,%d,%d]",
                cep.hacim(), cep.sutunSayisi(), cep.minY(), cep.ustY(), cep.acik(), cep.ziyaretEdilen(), deneme, bulma,
                Long.toHexString(kutu[6]), kutu[0], kutu[1], kutu[2], kutu[3], kutu[4], kutu[5]));
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
        return bildir(c, String.format("cekilme merkez=(%.1f,%.1f) yaricap=%.1f derinlik=%.1f", cx, cz, yaricap, derinlik));
    }

    private static int kapat(CommandContext<CommandSourceStack> c) {
        SanalSu.SUNUCU.temizle();
        SanalSuSunucu.MAGARA_HASH.clear();
        SanalSuSunucu.yayinla(c.getSource().getLevel());
        return bildir(c, "kapat");
    }

    private static int hash(CommandContext<CommandSourceStack> c) {
        ServerLevel l = c.getSource().getLevel();
        BlockPos m = BlockPos.containing(c.getSource().getPosition());
        long t0 = System.nanoTime();
        long h = SanalSu.bolgeHash(l, m.getX() - 40, l.getMinBuildHeight(), m.getZ() - 40, m.getX() + 40,
                l.getMaxBuildHeight() - 1, m.getZ() + 40);
        return bildir(c, String.format("HASH etiket=%s merkez=(%d,%d) kutu=81x%dx81 deger=%s sure=%.0fms",
                StringArgumentType.getString(c, "etiket"), m.getX(), m.getZ(), l.getHeight(), Long.toHexString(h),
                (System.nanoTime() - t0) / 1e6));
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
