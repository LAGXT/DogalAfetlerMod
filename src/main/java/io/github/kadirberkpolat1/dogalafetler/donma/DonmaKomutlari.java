package io.github.kadirberkpolat1.dogalafetler.donma;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.KisAlani;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Donma alt komutları (KOMUTLAR, N18): {@code /dogalafet donma <yüzde> [donma|cozulme]} global ilerlemeyi (ve isteğe bağlı
 * dönemi) ayarlar/sıfırlar; {@code /dogalafet donma bilgi} ilerlemeyi, komutu yazanın chunk'ının kayıtlı ve hedef seviyesini,
 * sütununun kış ağırlığını, bütçeyi, kısmayı ve kuyruğu yazar; {@code /dogalafet buharlasma <derinlik>} buharlaşma
 * derinliğini ayarlar (test). Çıktılar yalnız komutu yazan OP'ye gider (N6). Komutlar chunk yüklemez.
 */
final class DonmaKomutlari {
    private DonmaKomutlari() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> donma() {
        return Commands.literal("donma")
                .then(Commands.literal("bilgi").executes(DonmaKomutlari::bilgi))
                .then(Commands.argument("yuzde", FloatArgumentType.floatArg(0f, 100f))
                        .executes(b -> ayarla(b, null))
                        .then(Commands.literal("donma").executes(b -> ayarla(b, false)))
                        .then(Commands.literal("cozulme").executes(b -> ayarla(b, true))));
    }

    static LiteralArgumentBuilder<CommandSourceStack> buharlasma() {
        return Commands.literal("buharlasma")
                .then(Commands.argument("derinlik", FloatArgumentType.floatArg(0f, DunyaDonemi.BUHARLASMA_EN_COK))
                        .executes(DonmaKomutlari::buharlasmaAyarla));
    }

    private static String sayi(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    private static int ayarla(CommandContext<CommandSourceStack> b, Boolean cozulme) {
        DunyaDonemi d = DunyaDonemi.al(b.getSource().getServer());
        float yuzde = FloatArgumentType.getFloat(b, "yuzde");
        if (cozulme != null) {
            d.cozulmeDonemiAyarla(cozulme);
        }
        d.donmaIlerlemesiAyarla(yuzde);
        b.getSource().sendSuccess(() -> Component.translatable(DonmaCevirileri.AYARLANDI, sayi(d.donmaIlerlemesi()),
                Component.translatable(d.cozulmeDonemi() ? DonmaCevirileri.DONEM_COZULME : DonmaCevirileri.DONEM_DONMA)), false);
        return 1;
    }

    private static int buharlasmaAyarla(CommandContext<CommandSourceStack> b) {
        MinecraftServer server = b.getSource().getServer();
        DunyaDonemi d = DunyaDonemi.al(server);
        d.buharlasmaDerinligiAyarla(FloatArgumentType.getFloat(b, "derinlik"));
        b.getSource().sendSuccess(() -> Component.translatable(DonmaCevirileri.BUHARLASMA_AYARLANDI,
                sayi(d.buharlasmaDerinligi()), DonmaZamanlayici.etkinBuhar(server, d)), false);
        return 1;
    }

    static List<Component> bilgiSatirlari(CommandSourceStack kaynak) {
        MinecraftServer server = kaynak.getServer();
        ServerLevel level = server.overworld();
        DunyaDonemi d = DunyaDonemi.al(server);
        AfetYoneticisi y = AfetYoneticisi.al(server);
        List<Component> satirlar = new ArrayList<>();
        Component surec = DonmaSistemi.surecDurumu(y, level);
        satirlar.add(Component.translatable(DonmaCevirileri.BILGI_ILERLEME, sayi(d.donmaIlerlemesi()),
                Component.translatable(d.cozulmeDonemi() ? DonmaCevirileri.DONEM_COZULME : DonmaCevirileri.DONEM_DONMA),
                Component.translatable(d.gunesEvresi().ceviriAnahtari()), sayi(d.buharlasmaDerinligi()),
                DonmaZamanlayici.etkinBuhar(server, d), surec));
        BlockPos konum = BlockPos.containing(kaynak.getPosition());
        LevelChunk chunk = level.getChunkSource().getChunkNow(konum.getX() >> 4, konum.getZ() >> 4);
        ChunkDonmaVerisi c = chunk == null ? null : ChunkDonmaVerisi.al(chunk);
        if (c != null) {
            satirlar.add(Component.translatable(DonmaCevirileri.BILGI_CHUNK, konum.getX() >> 4, konum.getZ() >> 4,
                    c.sonG(), Component.translatable(c.cozulme() ? DonmaCevirileri.DONEM_COZULME : DonmaCevirileri.DONEM_DONMA),
                    DonmaZamanlayici.hedefG(d), c.sonBuhar(),
                    c.yarimVar() ? String.valueOf(c.yarimG()) : "-"));
        } else {
            satirlar.add(Component.translatable(DonmaCevirileri.BILGI_CHUNK_YOK, konum.getX() >> 4, konum.getZ() >> 4));
        }
        KisAlani.Chunk k = KisBolgesi.alan(level).chunk(konum.getX() >> 4, konum.getZ() >> 4);
        int yuzey = DonmaApi.yuzeyY(level, konum);
        int x = konum.getX();
        int z = konum.getZ();
        satirlar.add(Component.translatable(DonmaCevirileri.BILGI_SUTUN, x, z, yuzey, DonmaApi.sutunHedefi(level, konum),
                String.format(Locale.ROOT, "%.3f", k.agirlik(x, z, yuzey)),
                String.format(Locale.ROOT, "%.3f", k.maskeAgirligi(x, z)),
                String.format(Locale.ROOT, "%.3f", k.biyomAgirligi(x, z)),
                String.format(Locale.ROOT, "%.3f", k.yukseklikAgirligi(yuzey)), sayi(k.ofset(x, z)),
                Component.translatable(KisBolgesi.sabitMi(d) ? DonmaCevirileri.KIS_SABIT : DonmaCevirileri.KIS_CONFIG)));
        satirlar.add(Component.translatable(DonmaCevirileri.BILGI_BUTCE, DonmaZamanlayici.sonBlokSiniri(),
                sayi(TpsOlcer.kismaCarpani()), String.format(Locale.ROOT, "%.1f", TpsOlcer.tps()),
                DonmaZamanlayici.enCokTickDegisen()));
        satirlar.add(Component.translatable(DonmaCevirileri.BILGI_KUYRUK, DonmaZamanlayici.yukluSayisi(),
                DonmaZamanlayici.bekleyenSayisi(), DonmaZamanlayici.surenIsSayisi(), DonmaZamanlayici.tamamlananAdim(),
                DonmaZamanlayici.hedefeUlasan(), DonmaZamanlayici.toplamDegisen()));
        return satirlar;
    }

    private static int bilgi(CommandContext<CommandSourceStack> b) {
        for (Component satir : bilgiSatirlari(b.getSource())) {
            b.getSource().sendSuccess(() -> satir, false);
        }
        return 1;
    }
}
