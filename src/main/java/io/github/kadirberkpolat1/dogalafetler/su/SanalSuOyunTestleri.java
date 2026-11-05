package io.github.kadirberkpolat1.dogalafetler.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Sanal su (spike, inceleme B1 ve B2): gerçek dünyada mağara hücre sınıfı ve deniz alanının tazelenmesi. İki test ayrı
 * batch'te sırayla koşar (ikisi de sunucunun ortak {@link SanalSu#SUNUCU} durumunu kullanır). Şablon {@code su_alani}:
 * 16x12x16 hava ({@code tools/yapilar/su_alani_uret.py}).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SanalSuOyunTestleri {
    private SanalSuOyunTestleri() {
    }

    private static void koy(GameTestHelper h, int x, int y, int z, BlockState s) {
        h.getLevel().setBlock(h.absolutePos(new BlockPos(x, y, z)), s, Block.UPDATE_CLIENTS);
    }

    private static void kutu(GameTestHelper h, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    koy(h, x, y, z, s);
                }
            }
        }
    }

    private static BlockPos m(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new BlockPos(x, y, z));
    }

    private static SuAraliklari araliklar(GameTestHelper h, int x, int z) {
        BlockPos p = m(h, x, 0, z);
        SuAraliklari a = new SuAraliklari();
        SanalSu.SUNUCU.araliklar(p.getX(), p.getZ(), h.getLevel().getGameTime(), a);
        return a;
    }

    private static long sablonHash(GameTestHelper h) {
        BlockPos a = m(h, 0, 0, 0);
        BlockPos b = m(h, 15, 11, 15);
        return SanalSu.bolgeHash(h.getLevel(), Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), b.getY(), Math.max(a.getZ(), b.getZ()));
    }

    /**
     * Taş tepe (x 0..13, y 0..9, z 0..13) içinde 8x4x8 oda (x 3..10, y 2..5, z 3..10); odanın y 4..5 hizasından doğuya
     * açılan tünel (x 11..13, z 6..7) tepenin dışına (x 14..15, gökyüzü) çıkar. Su ağzın eşiğine (y 4) kadar kalır: oda
     * y 2..3 dolar, tünel ve dışarısı kuru. Zemindeki meşale, ot ve sarkıt suyun içindedir; dışarıdaki yaprak taçının altı
     * açıktır. Mağara bitince bölge hash'i baştakiyle aynıdır (hiçbir blok değişmez).
     */
    @GameTest(template = "su_alani", batch = "su_magara", timeoutTicks = 300)
    public static void yanAgizliMagaraEsigeKadarDolarDunyaDegismez(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        kutu(h, 0, 0, 0, 13, 9, 13, Blocks.STONE.defaultBlockState());
        kutu(h, 3, 2, 3, 10, 5, 10, Blocks.AIR.defaultBlockState());
        kutu(h, 11, 4, 6, 13, 5, 7, Blocks.AIR.defaultBlockState());
        koy(h, 6, 1, 6, Blocks.GRASS_BLOCK.defaultBlockState());
        h.setBlock(5, 2, 5, Blocks.TORCH);
        h.setBlock(6, 2, 6, Blocks.GRASS);
        h.setBlock(8, 2, 8, Blocks.POINTED_DRIPSTONE);
        koy(h, 15, 8, 15, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        long hashOnce = sablonHash(h);

        MagaraDolumu.Hucre hucre = SanalSu.magaraHucresi(l);
        BlockPos mesale = m(h, 5, 2, 5);
        BlockPos ot = m(h, 6, 2, 6);
        BlockPos sarkit = m(h, 8, 2, 8);
        BlockPos agizDisi = m(h, 14, 4, 6);
        BlockPos yaprakAlti = m(h, 15, 6, 15);
        h.assertTrue(hucre.tur(mesale.getX(), mesale.getY(), mesale.getZ()) == MagaraDolumu.Tur.BOS, "meşale BOS değil");
        h.assertTrue(hucre.tur(ot.getX(), ot.getY(), ot.getZ()) == MagaraDolumu.Tur.BOS, "ot BOS değil");
        h.assertTrue(hucre.tur(sarkit.getX(), sarkit.getY(), sarkit.getZ()) == MagaraDolumu.Tur.BOS, "sarkıt BOS değil");
        h.assertTrue(hucre.tur(agizDisi.getX(), agizDisi.getY(), agizDisi.getZ()) == MagaraDolumu.Tur.ACIK,
                "ağızın dışı ACIK değil");
        h.assertTrue(hucre.tur(yaprakAlti.getX(), yaprakAlti.getY(), yaprakAlti.getZ()) == MagaraDolumu.Tur.ACIK,
                "yaprak altı ACIK değil");

        BlockPos tohum = m(h, 4, 2, 4);
        MagaraCebi cep = SanalSuKomutu.cepHesapla(l, tohum.getX(), tohum.getY(), tohum.getZ());
        int esik = m(h, 0, 4, 0).getY();
        DogalAfetler.LOGGER.info("SPIKE-SU GAMETEST magara cep={} agizY={} ustY={} beklenenEsik={} ziyaret={} yaprakAlti_canSeeSky={}",
                cep.hacim(), cep.agizY(), cep.ustY(), esik, cep.ziyaretEdilen(), l.canSeeSky(yaprakAlti));
        h.assertTrue(cep.agizY() == esik, "ağız eşiği " + cep.agizY() + " != " + esik);
        h.assertTrue(cep.ustY() == esik, "su yüzeyi " + cep.ustY() + " != eşik " + esik);
        h.assertTrue(cep.hacim() == 8 * 2 * 8, "hacim " + cep.hacim() + " != 128");
        h.assertTrue(cep.icerir(mesale.getX(), mesale.getY(), mesale.getZ()), "meşale suda değil");
        h.assertTrue(cep.icerir(ot.getX(), ot.getY(), ot.getZ()), "ot suda değil");
        h.assertTrue(cep.icerir(sarkit.getX(), sarkit.getY(), sarkit.getZ()), "sarkıt suda değil");

        long[] kutu = SanalSuKomutu.magaraBaslat(SanalSu.SUNUCU, l, cep, 20, 0.2);
        SanalSuSunucu.MAGARA_HASH.put(cep, kutu);
        h.startSequence()
                .thenExecuteAfter(25, () -> {
                    SuAraliklari oda = araliklar(h, 4, 4);
                    h.assertTrue(oda.adet() == 1 && oda.alt(0) == m(h, 0, 2, 0).getY() && oda.ust(0) == esik,
                            "oda sütunu [y2, eşik) değil: adet=" + oda.adet());
                    for (int x = 11; x <= 15; x++) {
                        SuAraliklari a = araliklar(h, x, 6);
                        for (int i = 0; i < a.adet(); i++) {
                            h.assertTrue(a.ust(i) <= esik, "ağız/dış sütununda eşiğin üstünde su x=" + x);
                        }
                        h.assertTrue(a.adet() == 0, "ağız/dış sütunu ıslak x=" + x);
                    }
                })
                .thenWaitUntil(() -> h.assertTrue(SanalSu.SUNUCU.magaralar().isEmpty(), "mağara bitmedi"))
                .thenExecute(() -> {
                    long sonra = SanalSu.bolgeHash(l, (int) kutu[0], (int) kutu[1], (int) kutu[2], (int) kutu[3],
                            (int) kutu[4], (int) kutu[5]);
                    long sablonSonra = sablonHash(h);
                    DogalAfetler.LOGGER.info("SPIKE-SU GAMETEST magara hash cep once={} sonra={} sablon once={} sonra={}",
                            Long.toHexString(kutu[6]), Long.toHexString(sonra), Long.toHexString(hashOnce),
                            Long.toHexString(sablonSonra));
                    h.assertTrue(sonra == kutu[6], "cep kutusu hash'i değişti");
                    h.assertTrue(sablonSonra == hashOnce, "şablon hash'i değişti");
                    h.assertTrue(araliklar(h, 4, 4).adet() == 0, "mağara bitti ama su kaldı");
                    SanalSu.SUNUCU.temizle();
                    SanalSuSunucu.MAGARA_HASH.clear();
                })
                .thenSucceed();
    }

    /**
     * Kıyı: taş zemin, batıda su havuzu (x 1..7, y 1..2), doğuda kara (x 8..15, y 1..3). Deniz +4 açıkken kara sütunu
     * [y4, y7) sanal sudadır. {@code UPDATE_CLIENTS} (bayrak 2, blok olayı atmaz) ile sütuna 6 blokluk taş ve su yüzeyine buz
     * konur: periyodik tazeleme 40 tick içinde yakalamalı. Sonra bayrak 3 ile ikinci sütun: blok olayı yolu.
     */
    @GameTest(template = "su_alani", batch = "su_deniz", timeoutTicks = 400)
    public static void denizAlaniBlokDegisiminiYakalar(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockState tas = Blocks.STONE.defaultBlockState();
        kutu(h, 0, 0, 0, 15, 0, 15, tas);
        kutu(h, 0, 1, 0, 0, 2, 15, tas);
        kutu(h, 1, 1, 0, 7, 2, 0, tas);
        kutu(h, 1, 1, 15, 7, 2, 15, tas);
        kutu(h, 1, 1, 1, 7, 2, 14, Blocks.WATER.defaultBlockState());
        kutu(h, 8, 1, 0, 15, 3, 15, tas);

        BlockPos a = m(h, 0, 0, 0);
        BlockPos b = m(h, 15, 0, 15);
        for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++) {
            for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) {
                SanalSuSunucu.SABIT.add(DenizSeviyesiAlani.anahtar(cx, cz));
            }
        }
        SanalSu.SUNUCU.denizAyarla(4, l.getGameTime(), 0);
        int y4 = m(h, 0, 4, 0).getY();
        int y7 = m(h, 0, 7, 0).getY();
        BlockPos suSutunu = m(h, 4, 0, 8);
        long[] an = new long[4];
        h.startSequence()
                .thenWaitUntil(() -> {
                    SuAraliklari k = araliklar(h, 12, 8);
                    h.assertTrue(k.adet() == 1 && k.alt(0) == y4 && k.ust(0) == y7, "kara sütunu henüz [y4, y7) değil");
                    h.assertTrue(araliklar(h, 12, 10).adet() == 1, "ikinci sütun henüz ıslak değil");
                    h.assertTrue(SanalSu.SUNUCU.alan().su(suSutunu.getX(), suSutunu.getZ()), "su sütunu okunmadı");
                })
                .thenExecute(() -> {
                    kutu(h, 12, 4, 8, 12, 9, 8, tas);
                    koy(h, 4, 2, 8, Blocks.ICE.defaultBlockState());
                    an[0] = l.getGameTime();
                })
                .thenWaitUntil(() -> {
                    h.assertTrue(araliklar(h, 12, 8).adet() == 0, "bayrak-2 taş sütunu alana yansımadı");
                    h.assertTrue(!SanalSu.SUNUCU.alan().su(suSutunu.getX(), suSutunu.getZ()), "bayrak-2 buz alana yansımadı");
                    an[1] = l.getGameTime();
                })
                .thenExecute(() -> {
                    for (int y = 4; y <= 9; y++) {
                        h.setBlock(12, y, 10, Blocks.STONE);
                    }
                    an[2] = l.getGameTime();
                })
                .thenWaitUntil(() -> {
                    h.assertTrue(araliklar(h, 12, 10).adet() == 0, "bayrak-3 taş sütunu alana yansımadı");
                    an[3] = l.getGameTime();
                })
                .thenExecute(() -> {
                    DogalAfetler.LOGGER.info("SPIKE-SU GAMETEST deniz bayrak2_gecikme={} tick bayrak3_gecikme={} tick "
                            + "degisen_okuma={} bfs={}", an[1] - an[0], an[3] - an[2],
                            SanalSu.SUNUCU.alan().degisenOkumaSayisi(), SanalSu.SUNUCU.alan().hesapSayisi());
                    h.assertTrue(an[1] - an[0] <= 40, "bayrak-2 değişimi " + (an[1] - an[0]) + " tick sürdü (> 40)");
                    h.assertTrue(an[3] - an[2] <= 40, "bayrak-3 değişimi " + (an[3] - an[2]) + " tick sürdü (> 40)");
                    SanalSu.SUNUCU.temizle();
                    SanalSuSunucu.SABIT.clear();
                })
                .thenSucceed();
    }
}
