package io.github.kadirberkpolat1.dogalafetler.su;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Vitrin sahnesi (SU-29, tasarım notu §3): bulunan kumsala denize uzanan ahşap iskele ve kıyı köyü (8 ev, fenerler) kurar,
 * kamerayı hazır açılara taşır ve vitrin dalgasını (çekilmeyle) başlatır. Yalnız test ve görsel kanıt komutudur; gerçek
 * blokları değiştirir (sanal su sistemi değil). Belirlenimci: aynı dünya ve kumsal aynı sahneyi verir. Sunucu thread'i.
 */
final class SuVitrin {
    enum Kamera {
        ISKELE, SUALTI, TEPE
    }

    /** Kumsal noktası, kara yönü {@code (dx, dz)} (birim eksen), deniz seviyesi. {@code a} karaya, {@code b} yanal. */
    record Sahne(int x, int z, int dx, int dz, int deniz) {
        BlockPos p(int a, int b, int y) {
            return new BlockPos(x + dx * a - dz * b, y, z + dz * a + dx * b);
        }

        double wx(double a, double b) {
            return x + 0.5 + dx * a - dz * b;
        }

        double wz(double a, double b) {
            return z + 0.5 + dz * a + dx * b;
        }

        int iskeleY() {
            return deniz + 3;
        }
    }

    /** Ev merkezleri {@code {a, b}}: ön sıra kıyıya yakın (+4 denizde yarı gömülü), arka sıralar yükselen arazide. */
    private static final int[][] EVLER = {{7, -13}, {7, -5}, {7, 6}, {7, 14}, {15, -9}, {15, 2}, {15, 11}, {22, -3}};
    private static final Block[][] MALZEME = {
            {Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.SPRUCE_PLANKS},
            {Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.DARK_OAK_PLANKS},
            {Blocks.BIRCH_PLANKS, Blocks.BIRCH_LOG, Blocks.SPRUCE_PLANKS}};
    private static Sahne sahne;

    private SuVitrin() {
    }

    static Sahne kur(ServerLevel l, int px, int pz) {
        int[] k = SuKomutlari.kumsalBul(l, px, pz);
        if (k == null) {
            return null;
        }
        Sahne s = new Sahne(k[0], k[1], k[2], k[3], l.getSeaLevel());
        for (int i = 0; i < EVLER.length; i++) {
            ev(l, s, EVLER[i][0], EVLER[i][1], MALZEME[i % MALZEME.length]);
        }
        iskele(l, s);
        sahne = s;
        return s;
    }

    private static void koy(ServerLevel l, BlockPos p, Block b) {
        koy(l, p, b.defaultBlockState());
    }

    private static void koy(ServerLevel l, BlockPos p, BlockState s) {
        l.setBlock(p, Block.updateFromNeighbourShapes(s, l, p), Block.UPDATE_ALL);
    }

    /** Denize 16 blok uzanan 3 genişlikte iskele: güverte deniz + 3 (+4 denizde yüzeyle bir), kazıklar, korkuluk, fenerler. */
    private static void iskele(ServerLevel l, Sahne s) {
        int y = s.iskeleY();
        for (int a = -16; a <= 2; a++) {
            for (int b = -1; b <= 1; b++) {
                for (int h = y + 1; h <= y + 3; h++) {
                    koy(l, s.p(a, b, h), Blocks.AIR);
                }
                koy(l, s.p(a, b, y), Blocks.SPRUCE_PLANKS);
            }
        }
        for (int a : new int[] {-16, -12, -8, -4, 0}) {
            for (int b : new int[] {-1, 1}) {
                for (int h = s.deniz() - 7; h < y; h++) {
                    koy(l, s.p(a, b, h), Blocks.STRIPPED_SPRUCE_LOG);
                }
            }
        }
        for (int a = -16; a <= -1; a++) {
            for (int b : new int[] {-1, 1}) {
                koy(l, s.p(a, b, y + 1), Blocks.SPRUCE_FENCE);
            }
        }
        for (int a : new int[] {-16, -9, -2}) {
            for (int b : new int[] {-1, 1}) {
                koy(l, s.p(a, b, y + 2), Blocks.LANTERN);
            }
        }
    }

    /** 5x5 ev: taş temel, düz duvar + kütük köşeler, cam pencereler, denize bakan kapı boşluğu, basamaklı çatı, kapıda fener. */
    private static void ev(ServerLevel l, Sahne s, int a, int b, Block[] m) {
        BlockPos merkez = s.p(a, b, 0);
        l.getChunk(merkez.getX() >> 4, merkez.getZ() >> 4);
        int taban = Math.max(l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, merkez.getX(), merkez.getZ()),
                s.deniz() + 1);
        for (int u = -3; u <= 3; u++) {
            for (int w = -3; w <= 3; w++) {
                for (int y = taban; y <= taban + 8; y++) {
                    koy(l, s.p(a + u, b + w, y), Blocks.AIR);
                }
            }
        }
        for (int u = -2; u <= 2; u++) {
            for (int w = -2; w <= 2; w++) {
                for (int y = taban - 4; y < taban; y++) {
                    koy(l, s.p(a + u, b + w, y), Math.abs(u) < 2 && Math.abs(w) < 2 && y == taban - 1 ? m[0] : Blocks.COBBLESTONE);
                }
                if (Math.abs(u) == 2 || Math.abs(w) == 2) {
                    for (int y = taban; y <= taban + 2; y++) {
                        koy(l, s.p(a + u, b + w, y), Math.abs(u) == 2 && Math.abs(w) == 2 ? m[1] : m[0]);
                    }
                }
            }
        }
        koy(l, s.p(a - 2, b, taban), Blocks.AIR);
        koy(l, s.p(a - 2, b, taban + 1), Blocks.AIR);
        for (int[] pencere : new int[][] {{-2, -1}, {-2, 1}, {0, -2}, {0, 2}, {2, 0}}) {
            koy(l, s.p(a + pencere[0], b + pencere[1], taban + 1), Blocks.GLASS_PANE);
        }
        for (int kat = 0; kat < 3; kat++) {
            int r = 3 - kat;
            for (int u = -r; u <= r; u++) {
                for (int w = -r; w <= r; w++) {
                    koy(l, s.p(a + u, b + w, taban + 3 + kat), m[2]);
                }
            }
        }
        koy(l, s.p(a, b, taban + 2), Blocks.LANTERN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        koy(l, s.p(a - 3, b + 1, taban), Blocks.OAK_FENCE);
        koy(l, s.p(a - 3, b + 1, taban + 1), Blocks.LANTERN);
        koy(l, s.p(a - 3, b - 1, taban), Blocks.BARREL);
    }

    /** Kamerayı hazır açıya taşır (izleyici); sahne yoksa {@code false}. */
    static boolean kamera(ServerPlayer p, Kamera ne) {
        Sahne s = sahne;
        if (s == null) {
            return false;
        }
        ServerLevel l = p.serverLevel();
        double x;
        double y;
        double z;
        double hx;
        double hz;
        float egim;
        switch (ne) {
            case ISKELE -> {
                // Tasarım notu: iskelenin ucunda, güvertenin 3 blok üstünde, kıyıya dönük, pitch -8° (aşağı).
                x = s.wx(-15, 0);
                z = s.wz(-15, 0);
                y = s.iskeleY() + 1 + 3;
                hx = s.dx();
                hz = s.dz();
                egim = 8f;
            }
            case SUALTI -> {
                // Su altı (+4 denizde): kumsalın üstünde, yüzeyin altında, iskele kazıklarına ve yüzeye yukarı bakar.
                x = s.wx(1.5, 4.5);
                z = s.wz(1.5, 4.5);
                y = s.deniz() + 2.3;
                hx = s.wx(-9, 0) - x;
                hz = s.wz(-9, 0) - z;
                egim = -12f;
            }
            default -> {
                // Tsunami vitrini, geniş plan: iskele ekseninde, ilk ev sırasının arasından alçaktan denize bakar; iskele gözü
                // dalgaya taşır, yanlardaki evler ölçek verir. Dalga kıyıya ~8 blok kala (kameradan ~19 blok, yakın halkada:
                // tepe köpüğü ve kırılan dudak tam ayrıntıda) duvar kadrajın ortasında; arkası uzak LOD cephesiyle (SU-37).
                x = s.wx(11, 0.5);
                z = s.wz(11, 0.5);
                l.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
                y = Math.max(l.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z)) + 1.6,
                        s.deniz() + 5.5);
                hx = -s.dx();
                hz = -s.dz();
                egim = -3f;
            }
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-hx, hz));
        p.setGameMode(GameType.SPECTATOR);
        p.teleportTo(l, x, y, z, yaw, egim);
        return true;
    }

    /**
     * Vitrin dalgası: 95 blok açıktan kıyıya ilerleyen, 170 genişliğinde dalga (taban gerçek su yüzeyinin bir altı) ve kıyı
     * önünde çekilme (ıslak dip açığa çıkar). Dalga kimliği; sahne yoksa -1.
     */
    static int dalga(ServerLevel l, int yukseklik) {
        Sahne s = sahne;
        if (s == null) {
            return -1;
        }
        long saat = SanalSu.sunucuSaati;
        SanalSu.cekilmeBaslat(l, new Cekilme(s.wx(-12, 0), s.wz(-12, 0), 26, 3, saat, 120));
        return SanalSu.dalgaBaslat(l, new Dalga(s.wx(-95, 0), s.wz(-95, 0), s.dx(), s.dz(), saat, 0.35, yukseklik, 170, 45,
                s.deniz() - 1, 112));
    }
}
