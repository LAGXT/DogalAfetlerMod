package io.github.kadirberkpolat1.dogalafetler.yangin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** {@code /dogalafet yangin tutustur [yarıçap] | sondur | bilgi} (OP). */
final class YanginKomutlari {
    static final String TUTUSTUR = "commands.dogalafetler.yangin.tutustur";
    static final String TUTUSTUR_SURUYOR = "commands.dogalafetler.yangin.tutustur_suruyor";
    static final String SONDUR = "commands.dogalafetler.yangin.sondur";
    static final String BILGI = "commands.dogalafetler.yangin.bilgi";
    private static final int YETKI = 2;
    private static final int VARSAYILAN_YARICAP = 4;

    private YanginKomutlari() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> komut() {
        return Commands.literal("yangin").requires(k -> k.hasPermission(YETKI))
                .then(Commands.literal("tutustur")
                        .executes(b -> tutustur(b.getSource(), VARSAYILAN_YARICAP))
                        .then(Commands.argument("yaricap", IntegerArgumentType.integer(1, 16))
                                .executes(b -> tutustur(b.getSource(), IntegerArgumentType.getInteger(b, "yaricap")))))
                .then(Commands.literal("sondur").executes(b -> sondur(b.getSource())))
                .then(Commands.literal("bilgi").executes(b -> bilgi(b.getSource())));
    }

    private static int tutustur(CommandSourceStack kaynak, int yaricap) {
        ServerLevel level = kaynak.getLevel();
        int sayi = YanginYoneticisi.alanTutustur(level, BlockPos.containing(kaynak.getPosition()), yaricap,
                YanginKaynagi.KOMUT, 1.0);
        boolean suruyor = YanginYoneticisi.bekleyenAlanSayisi(level) > 0;
        kaynak.sendSuccess(() -> Component.translatable(suruyor ? TUTUSTUR_SURUYOR : TUTUSTUR, sayi), true);
        return sayi;
    }

    private static int sondur(CommandSourceStack kaynak) {
        int sayi = YanginYoneticisi.sondur(kaynak.getLevel(), null);
        kaynak.sendSuccess(() -> Component.translatable(SONDUR, sayi), true);
        return sayi;
    }

    private static int bilgi(CommandSourceStack kaynak) {
        int aktif = YanginYoneticisi.aktifSayi(kaynak.getLevel());
        String carpan = String.format(Locale.ROOT, "%.2f", YanginYoneticisi.kismaCarpani());
        String tps = String.format(Locale.ROOT, "%.1f", TpsOlcer.tps());
        double[] sure = YanginYoneticisi.sureOzeti(kaynak.getLevel());
        String ortalama = String.format(Locale.ROOT, "%.2f", sure[0]);
        String enCok = String.format(Locale.ROOT, "%.2f", sure[1]);
        kaynak.sendSuccess(() -> Component.translatable(BILGI, aktif, YanginAyarlari.aktifTavan(),
                YanginYoneticisi.etkinButce(), YanginAyarlari.tickButcesi(), carpan, tps, ortalama, enCok), false);
        return aktif;
    }
}
