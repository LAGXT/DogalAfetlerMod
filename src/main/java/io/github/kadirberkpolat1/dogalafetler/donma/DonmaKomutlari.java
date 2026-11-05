package io.github.kadirberkpolat1.dogalafetler.donma;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Spike komutları: {@code /dogalafet spike donma <yuzde> | bilgi | kis | cozulme <bool> | izle | butce <blok> |
 * yapayyuk <ms>}. Konum gerektirenler komut kaynağının konumunu kullanır (konsolda {@code execute positioned ...}).
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
        String chunkBilgi = chunk == null ? "yüklü değil"
                : "seviye " + ChunkDonmaSeviyesi.oku(chunk) + ", hedef " + DonmaZamanlayici.hedef(level, chunk, veri);
        yaz(b, String.format("Donma: G=%d çözülme=%s | kuyruk %d (süren %d), yüklü %d | işlenen adım %d, hedefe ulaşan"
                        + " %d, değişen blok %d, taranan sütun %d | tick bütçesi %d blok (1/%d) | TPS %.1f (%.1f ms/tick)"
                        + " | chunk %s: %s",
                veri.ilerleme(), veri.cozulme(), DonmaZamanlayici.bekleyenSayisi(), DonmaZamanlayici.surenIsSayisi(),
                DonmaZamanlayici.yukluSayisi(), DonmaZamanlayici.tamamlananAdim, DonmaZamanlayici.hedefeUlasan,
                DonmaZamanlayici.toplamDegisen, DonmaZamanlayici.toplamSutun, DonmaZamanlayici.sonButce,
                DonmaZamanlayici.sonBolen, DonmaZamanlayici.sonTps, kaynak.getServer().getAverageTickTime(), p, chunkBilgi));
        return DonmaZamanlayici.bekleyenSayisi();
    }

    private static int kis(CommandContext<CommandSourceStack> b) {
        CommandSourceStack kaynak = b.getSource();
        ServerLevel level = kaynak.getServer().overworld();
        BlockPos konum = BlockPos.containing(kaynak.getPosition());
        ChunkPos p = new ChunkPos(konum);
        KisMaskesi maske = DonmaZamanlayici.maske(level);
        double maskeAgirligi = maske.agirlik(p.x, p.z);
        double gurultu = maske.gurultu(p.getMiddleBlockX(), p.getMiddleBlockZ());
        // getChunk yüklü olmayan chunk'ı ana thread'de senkron üretir; komut chunk yüklememeli.
        LevelChunk chunk = level.getChunkSource().getChunkNow(p.x, p.z);
        int yuzeyY = chunk != null
                ? chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, konum.getX() & 15, konum.getZ() & 15)
                : level.getSeaLevel();
        Holder<Biome> biyom = level.getBiome(new BlockPos(p.getMiddleBlockX(), yuzeyY, p.getMiddleBlockZ()));
        boolean soguk = DonmaZamanlayici.sogukBiyom(biyom);
        double w = DonmaHedefi.kisAgirligi(maskeAgirligi, soguk);
        DonmaVerisi veri = DonmaVerisi.al(kaynak.getServer());
        yaz(b, String.format("Kış: chunk %s maske ağırlığı %.3f (gürültü %.4f, eşik %.4f) | biyom %s sıcaklık %.2f soğuk=%s"
                        + " | yüzey Y=%d%s (Y>%d kalıcı kış=%s) | birleşik ağırlık %.3f → kış=%s | çözülmede hedef %d"
                        + " (G=%d)",
                p, maskeAgirligi, gurultu, maske.esik(), biyom.unwrapKey().map(k -> k.location().toString()).orElse("?"),
                biyom.value().getBaseTemperature(), soguk, yuzeyY, chunk == null ? " (chunk yüklü değil, deniz seviyesi)" : "",
                DonmaAyarlari.kisYEsigi, yuzeyY > DonmaAyarlari.kisYEsigi, w, w >= 0.5,
                DonmaHedefi.hedef(veri.ilerleme(), true, w), veri.ilerleme()));
        return w >= 0.5 ? 1 : 0;
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
