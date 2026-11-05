package io.github.kadirberkpolat1.dogalafetler.donma;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Spike komutları: {@code /dogalafet spike donma <yuzde> | bilgi | kis | ornek | dagbul <x> <z> <y> | cozulme <bool> | izle |
 * butce <blok> | yapayyuk <ms>}. Konum gerektirenler komut kaynağının konumunu kullanır (konsolda
 * {@code execute positioned ...}); hiçbiri chunk yüklemez.
 */
public final class DonmaKomutlari {
    private DonmaKomutlari() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> olustur() {
        return Commands.literal("spike").then(Commands.literal("donma")
                .then(Commands.argument("yuzde", IntegerArgumentType.integer(0, 100))
                        .executes(b -> ilerleme(b, IntegerArgumentType.getInteger(b, "yuzde"))))
                .then(Commands.literal("bilgi").executes(DonmaKomutlari::bilgi))
                .then(Commands.literal("kis").executes(DonmaKomutlari::kis))
                .then(Commands.literal("ornek").executes(DonmaKomutlari::ornek))
                .then(Commands.literal("dagbul").then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer(64, 320))
                                        .executes(b -> dagBul(b, IntegerArgumentType.getInteger(b, "x"),
                                                IntegerArgumentType.getInteger(b, "z"), IntegerArgumentType.getInteger(b, "y")))))))
                .then(Commands.literal("cozulme").then(Commands.argument("acik", BoolArgumentType.bool())
                        .executes(b -> cozulme(b, BoolArgumentType.getBool(b, "acik")))))
                .then(Commands.literal("izle").executes(DonmaKomutlari::izle))
                .then(Commands.literal("butce").then(Commands.argument("blok", IntegerArgumentType.integer(1, 65536))
                        .executes(b -> butce(b, IntegerArgumentType.getInteger(b, "blok")))))
                .then(Commands.literal("yapayyuk").then(Commands.argument("ms", IntegerArgumentType.integer(0, 500))
                        .executes(b -> yapayYuk(b, IntegerArgumentType.getInteger(b, "ms"))))));
    }

    private static void yaz(CommandContext<CommandSourceStack> b, String metin) {
        b.getSource().sendSuccess(() -> Component.literal(metin), false);
    }

    private static int ilerleme(CommandContext<CommandSourceStack> b, int yuzde) {
        DonmaVerisi veri = DonmaVerisi.al(b.getSource().getServer());
        int eski = veri.ilerleme();
        veri.ilerleme(yuzde);
        DonmaZamanlayici.hepsiniKuyrugaAl();
        DogalAfetler.LOGGER.info("[donma] global ilerleme {} → {}; {} yüklü chunk kuyruğa alındı", eski, yuzde,
                DonmaZamanlayici.yukluSayisi());
        yaz(b, "Donma ilerlemesi " + eski + " → " + yuzde + "; kuyruk " + DonmaZamanlayici.bekleyenSayisi());
        return yuzde;
    }

    private static int cozulme(CommandContext<CommandSourceStack> b, boolean acik) {
        DonmaVerisi.al(b.getSource().getServer()).cozulme(acik);
        DonmaZamanlayici.hepsiniKuyrugaAl();
        yaz(b, "Çözülme dönemi: " + acik);
        return 1;
    }

    private static int bilgi(CommandContext<CommandSourceStack> b) {
        CommandSourceStack kaynak = b.getSource();
        ServerLevel level = kaynak.getServer().overworld();
        DonmaVerisi veri = DonmaVerisi.al(kaynak.getServer());
        ChunkPos p = new ChunkPos(BlockPos.containing(kaynak.getPosition()));
        LevelChunk chunk = level.getChunkSource().getChunkNow(p.x, p.z);
        ChunkDonmaSeviyesi d = chunk == null ? null : ChunkDonmaSeviyesi.al(chunk);
        String chunkBilgi = d == null ? "yüklü değil"
                : "seviye " + d.sonG() + " (dönem " + DonmaZamanlayici.donemAdi(d.cozulme()) + ")";
        yaz(b, String.format("Donma: G=%d çözülme=%s | kuyruk %d (süren %d), yüklü %d | işlenen adım %d, hedefe ulaşan"
                        + " %d, değişen blok %d, taranan sütun %d | tick bütçesi %d blok (1/%d) | TPS %.1f (%.1f ms/tick)"
                        + " | chunk %s: %s",
                veri.ilerleme(), veri.cozulme(), DonmaZamanlayici.bekleyenSayisi(), DonmaZamanlayici.surenIsSayisi(),
                DonmaZamanlayici.yukluSayisi(), DonmaZamanlayici.tamamlananAdim, DonmaZamanlayici.hedefeUlasan,
                DonmaZamanlayici.toplamDegisen, DonmaZamanlayici.toplamSutun, DonmaZamanlayici.sonButce,
                DonmaZamanlayici.sonBolen, DonmaZamanlayici.sonTps, DonmaZamanlayici.sonMs, p, chunkBilgi));
        return DonmaZamanlayici.bekleyenSayisi();
    }

    /** Komut konumundaki sütunun kış ağırlığı bileşenleri ve çözülme hedefi. */
    private static int kis(CommandContext<CommandSourceStack> b) {
        CommandSourceStack kaynak = b.getSource();
        ServerLevel level = kaynak.getServer().overworld();
        BlockPos konum = BlockPos.containing(kaynak.getPosition());
        ChunkPos p = new ChunkPos(konum);
        LevelChunk chunk = level.getChunkSource().getChunkNow(p.x, p.z);
        int y = chunk != null ? chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, konum.getX() & 15, konum.getZ() & 15)
                : DunyaKisi.tabanYuksekligi(level, konum.getX(), konum.getZ());
        KisAlani.Chunk k = DunyaKisi.alan(level).chunk(p.x, p.z);
        double w = k.agirlik(konum.getX(), konum.getZ(), y);
        double ofset = k.ofset(konum.getX(), konum.getZ());
        DonmaVerisi veri = DonmaVerisi.al(kaynak.getServer());
        int hedef = DonmaHedefi.sutunHedefi(veri.ilerleme(), true, w, ofset);
        yaz(b, String.format("Kış sütunu (%d, %d) chunk %s: yüzey Y=%d%s | maske %.3f biyom %.3f yükseklik %.3f → w %.3f"
                        + " | sıcaklık %.2f ofset %.1f | çözülmede hedef %d (G=%d)",
                konum.getX(), konum.getZ(), p, y, chunk == null ? " (chunk yüklü değil, gürültü yüzeyi)" : "",
                k.maskeAgirligi(konum.getX(), konum.getZ()), k.biyomAgirligi(konum.getX(), konum.getZ()),
                k.yukseklikAgirligi(y), w, k.sicaklik(konum.getX(), konum.getZ()), ofset, hedef, veri.ilerleme()));
        return w >= 0.5 ? 1 : 0;
    }

    /**
     * Yüklü chunk'ın sütun örnekleri (log): en yüksek ve en alçak sütun, dört köşe ve özet. Güncel G ve dönemle sütun
     * hedefi; üstündeki kar katmanı.
     */
    private static int ornek(CommandContext<CommandSourceStack> b) {
        CommandSourceStack kaynak = b.getSource();
        ServerLevel level = kaynak.getServer().overworld();
        ChunkPos p = new ChunkPos(BlockPos.containing(kaynak.getPosition()));
        LevelChunk chunk = level.getChunkSource().getChunkNow(p.x, p.z);
        if (chunk == null) {
            yaz(b, "Chunk " + p + " yüklü değil");
            return 0;
        }
        DonmaVerisi veri = DonmaVerisi.al(kaynak.getServer());
        KisAlani.Chunk k = DunyaKisi.alan(level).chunk(p.x, p.z);
        KisAlani.Ayar ayar = DonmaAyarlari.kisAyar;
        int ustSayisi = 0;
        int ustTam = 0;
        int altSayisi = 0;
        int enDusukHedef = 100;
        int enYuksekHedef = 0;
        int enY = Integer.MIN_VALUE;
        int enYi = 0;
        int dipY = Integer.MAX_VALUE;
        int dipi = 0;
        int[] y = new int[256];
        int[] hedef = new int[256];
        for (int i = 0; i < 256; i++) {
            int x = p.getMinBlockX() + (i & 15);
            int z = p.getMinBlockZ() + (i >> 4);
            y[i] = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15);
            hedef[i] = DonmaHedefi.sutunHedefi(veri.ilerleme(), veri.cozulme(), k.agirlik(x, z, y[i]), k.ofset(x, z));
            enDusukHedef = Math.min(enDusukHedef, hedef[i]);
            enYuksekHedef = Math.max(enYuksekHedef, hedef[i]);
            if (y[i] >= ayar.yUst()) {
                ustSayisi++;
                ustTam += hedef[i] == 100 ? 1 : 0;
            } else if (y[i] <= ayar.yAlt()) {
                altSayisi++;
            }
            if (y[i] > enY) {
                enY = y[i];
                enYi = i;
            }
            if (y[i] < dipY) {
                dipY = y[i];
                dipi = i;
            }
        }
        for (int i : new int[] {enYi, dipi, 0, 15, 240, 255}) {
            int x = p.getMinBlockX() + (i & 15);
            int z = p.getMinBlockZ() + (i >> 4);
            BlockState ust = chunk.getBlockState(new BlockPos(x, y[i], z));
            BlockState ustundeki = chunk.getBlockState(new BlockPos(x, y[i] + 1, z));
            int kar = ustundeki.is(Blocks.SNOW) ? ustundeki.getValue(SnowLayerBlock.LAYERS) : 0;
            DogalAfetler.LOGGER.info("[donma] w_sütun ({}, {}) Y={} w={} (maske {} biyom {} yükseklik {}) ofset {} hedef {} | yüzey {}"
                            + " kar {}",
                    x, z, y[i], String.format("%.3f", k.agirlik(x, z, y[i])), String.format("%.3f", k.maskeAgirligi(x, z)),
                    String.format("%.3f", k.biyomAgirligi(x, z)), String.format("%.3f", k.yukseklikAgirligi(y[i])),
                    String.format("%.1f", k.ofset(x, z)), hedef[i], ust.getBlock().getDescriptionId(), kar);
        }
        ChunkDonmaSeviyesi d = ChunkDonmaSeviyesi.al(chunk);
        DogalAfetler.LOGGER.info("[donma] w_sütun özeti chunk {}: G={} {} chunk seviyesi {} | Y≥{}: {} sütun, hedefi 100: {}"
                        + " | Y≤{}: {} sütun | hedef {}..{}",
                p, veri.ilerleme(), DonmaZamanlayici.donemAdi(veri.cozulme()), d == null ? -1 : d.sonG(), ayar.yUst(),
                ustSayisi, ustTam, ayar.yAlt(), altSayisi, enDusukHedef, enYuksekHedef);
        yaz(b, "Örnek yazıldı: chunk " + p + ", Y≥" + ayar.yUst() + " sütun " + ustSayisi + ", hedefi 100 " + ustTam);
        return ustTam;
    }

    /** {@code (x, z)} çevresinde halka halka gürültü yüzeyi en az {@code enAzY} olan ilk noktayı arar (chunk yüklemez). */
    private static int dagBul(CommandContext<CommandSourceStack> b, int x0, int z0, int enAzY) {
        ServerLevel level = b.getSource().getServer().overworld();
        int taranan = 0;
        for (int d = 512; d <= 4096; d += 128) {
            int nokta = Math.max(8, d / 64);
            for (int k = 0; k < nokta; k++) {
                double aci = 2 * Math.PI * k / nokta;
                int x = x0 + (int) Math.round(d * Math.cos(aci));
                int z = z0 + (int) Math.round(d * Math.sin(aci));
                int h = DunyaKisi.tabanYuksekligi(level, x, z);
                taranan++;
                if (h >= enAzY) {
                    int cx = (x >> 4) * 16 + 8;
                    int cz = (z >> 4) * 16 + 8;
                    DogalAfetler.LOGGER.info("[donma] dağ bulundu: {} {} {} (gürültü yüzeyi {}, {} nokta tarandı)", cx, h, cz,
                            h, taranan);
                    yaz(b, "Dağ: " + cx + " " + h + " " + cz);
                    return h;
                }
            }
        }
        DogalAfetler.LOGGER.info("[donma] dağ bulunamadı ({} nokta tarandı)", taranan);
        yaz(b, "Dağ bulunamadı");
        return 0;
    }

    private static int izle(CommandContext<CommandSourceStack> b) {
        ChunkPos p = new ChunkPos(BlockPos.containing(b.getSource().getPosition()));
        DonmaZamanlayici.izle(p.toLong());
        yaz(b, "İzlenen chunk: " + p);
        return 1;
    }

    private static int butce(CommandContext<CommandSourceStack> b, int blok) {
        DonmaAyarlari.temelButce = blok;
        yaz(b, "Temel bütçe: " + blok + " blok/tick");
        return blok;
    }

    private static int yapayYuk(CommandContext<CommandSourceStack> b, int ms) {
        DonmaZamanlayici.yapayYukMs = ms;
        DogalAfetler.LOGGER.info("[donma] yapay yük: her tick {} ms uyku", ms);
        yaz(b, "Yapay yük: " + ms + " ms/tick");
        return ms;
    }
}
