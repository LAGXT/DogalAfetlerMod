package io.github.kadirberkpolat1.dogalafetler.meteor;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulIstegi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /dogalafet meteor dusur [buyuk] [ay] [oyuncu]} (komut kaynağının ya da oyuncunun konumuna; konsolda dünya doğuş
 * noktası), {@code yagmur <sn> [dakikaBasina]} (boyuttaki bütün oyuncular), {@code durdur}, {@code bilgi} (OP). Ayrı alt komut
 * {@code /dogalafet odul meteor [parca|cekirdek|hepsi] [adet] [oyuncu]} (KOM.13, K-E18): argümansız, komutu veren oyuncuya
 * birer Meteorit Parçası ve Kozmik Çekirdek.
 */
final class MeteorKomutlari {
    static final String DUSUR = "commands.dogalafetler.meteor.dusur";
    static final String DUSUR_HEMEN = "commands.dogalafetler.meteor.dusur_hemen";
    static final String DUSUR_YOK = "commands.dogalafetler.meteor.dusur_yok";
    static final String YAGMUR = "commands.dogalafetler.meteor.yagmur";
    static final String DURDUR = "commands.dogalafetler.meteor.durdur";
    static final String BILGI = "commands.dogalafetler.meteor.bilgi";
    static final String ODUL = "commands.dogalafetler.odul.meteor";
    static final String ODUL_TUR = "commands.dogalafetler.odul.meteor_tur";
    private static final int YETKI = 2;
    /** Tasarım: ay çarpması meteorları oyuncu başına 1 / 8 sn. */
    private static final double VARSAYILAN_DAKIKA_BASINA = 7.5;

    private MeteorKomutlari() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> komut() {
        return Commands.literal("meteor").requires(k -> k.hasPermission(YETKI))
                .then(dusurDali(Commands.literal("dusur"), MeteorBoyutu.KUCUK, MeteorGorunumu.GUNES)
                        .then(dusurDali(Commands.literal("buyuk"), MeteorBoyutu.BUYUK, MeteorGorunumu.GUNES)
                                .then(dusurDali(Commands.literal("ay"), MeteorBoyutu.BUYUK, MeteorGorunumu.AY)))
                        .then(dusurDali(Commands.literal("ay"), MeteorBoyutu.KUCUK, MeteorGorunumu.AY)))
                .then(Commands.literal("yagmur")
                        .then(Commands.argument("saniye", IntegerArgumentType.integer(1, 3600))
                                .executes(b -> yagmur(b, VARSAYILAN_DAKIKA_BASINA))
                                .then(Commands.argument("dakikaBasina", DoubleArgumentType.doubleArg(0.1, 600))
                                        .executes(b -> yagmur(b, DoubleArgumentType.getDouble(b, "dakikaBasina"))))))
                .then(Commands.literal("durdur").executes(b -> durdur(b.getSource())))
                .then(Commands.literal("bilgi").executes(b -> bilgi(b.getSource())));
    }

    /** {@code /dogalafet odul meteor ...} (KOM.13). */
    static LiteralArgumentBuilder<CommandSourceStack> odulKomutu() {
        return Commands.literal("odul").requires(k -> k.hasPermission(YETKI))
                .then(Commands.literal("meteor")
                        .executes(b -> odul(b.getSource(), b.getSource().getPlayerOrException(), OdulIstegi.VARSAYILAN))
                        .then(Commands.argument("tur", StringArgumentType.word())
                                .suggests((b, kurucu) -> SharedSuggestionProvider.suggest(OdulIstegi.TURLER, kurucu))
                                .executes(b -> odul(b, 1, b.getSource().getPlayerOrException()))
                                .then(Commands.argument("adet", IntegerArgumentType.integer(1, OdulIstegi.EN_COK_ADET))
                                        .executes(b -> odul(b, IntegerArgumentType.getInteger(b, "adet"),
                                                b.getSource().getPlayerOrException()))
                                        .then(Commands.argument("oyuncu", EntityArgument.player())
                                                .executes(b -> odul(b, IntegerArgumentType.getInteger(b, "adet"),
                                                        EntityArgument.getPlayer(b, "oyuncu")))))));
    }

    private static int odul(CommandContext<CommandSourceStack> b, int adet, ServerPlayer oyuncu) {
        String tur = StringArgumentType.getString(b, "tur");
        if (!OdulIstegi.gecerliTur(tur)) {
            b.getSource().sendFailure(Component.translatable(ODUL_TUR, tur, String.join(", ", OdulIstegi.TURLER)));
            return 0;
        }
        return odul(b.getSource(), oyuncu, OdulIstegi.coz(tur, adet));
    }

    private static int odul(CommandSourceStack kaynak, ServerPlayer oyuncu, OdulIstegi istek) {
        int verilen = MeteorAPI.odulVer(oyuncu, istek.parca(), istek.cekirdek());
        kaynak.sendSuccess(() -> Component.translatable(ODUL, istek.parca(), istek.cekirdek(), oyuncu.getDisplayName()), true);
        return verilen;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dusurDali(LiteralArgumentBuilder<CommandSourceStack> dal,
            MeteorBoyutu boyut, MeteorGorunumu gorunum) {
        return dal.executes(b -> dusur(b.getSource(), BlockPos.containing(b.getSource().getPosition()), boyut, gorunum))
                .then(Commands.argument("oyuncu", EntityArgument.player())
                        .executes(b -> oyuncuya(b, boyut, gorunum)));
    }

    private static int oyuncuya(CommandContext<CommandSourceStack> b, MeteorBoyutu boyut, MeteorGorunumu gorunum)
            throws CommandSyntaxException {
        ServerPlayer oyuncu = EntityArgument.getPlayer(b, "oyuncu");
        return dusur(b.getSource().withLevel(oyuncu.serverLevel()), oyuncu.blockPosition(), boyut, gorunum);
    }

    private static int dusur(CommandSourceStack kaynak, BlockPos hedef, MeteorBoyutu boyut, MeteorGorunumu gorunum) {
        ServerLevel level = kaynak.getLevel();
        // API'nin boş dönüşü üç durumu kapsar; ilk ikisi değilse çarpma hemen oldu (krater ya da suya düşüş).
        boolean atilabilir = level.hasChunkAt(hedef)
                && MeteorYoneticisi.aktifMeteorSayisi(level) < MeteorAyarlari.varlikTavani();
        Optional<MeteorVarligi> m = MeteorAPI.dusur(level, hedef, boyut, gorunum, YanginKaynagi.KOMUT, true);
        String ad = boyut.name().toLowerCase(Locale.ROOT) + "/" + gorunum.name().toLowerCase(Locale.ROOT);
        if (m.isPresent()) {
            kaynak.sendSuccess(() -> Component.translatable(DUSUR, ad, hedef.getX(), hedef.getZ()), true);
            return 1;
        }
        if (atilabilir) {
            kaynak.sendSuccess(() -> Component.translatable(DUSUR_HEMEN, ad, hedef.getX(), hedef.getZ()), true);
            return 1;
        }
        kaynak.sendFailure(Component.translatable(DUSUR_YOK, MeteorAyarlari.varlikTavani()));
        return 0;
    }

    private static int yagmur(CommandContext<CommandSourceStack> b, double dakikaBasina) {
        CommandSourceStack kaynak = b.getSource();
        int saniye = IntegerArgumentType.getInteger(b, "saniye");
        int kimlik = MeteorAPI.yagmur(kaynak.getLevel(), List.of(), saniye * 20, dakikaBasina,
                MeteorAyarlari.yagmurBuyukOrani(), YanginKaynagi.KOMUT);
        kaynak.sendSuccess(() -> Component.translatable(YAGMUR, kimlik, saniye, dakikaBasina), true);
        return kimlik;
    }

    private static int durdur(CommandSourceStack kaynak) {
        int sayi = MeteorAPI.yagmurlariDurdur(kaynak.getServer(), YanginKaynagi.KOMUT, true);
        kaynak.sendSuccess(() -> Component.translatable(DURDUR, sayi), true);
        return sayi;
    }

    private static int bilgi(CommandSourceStack kaynak) {
        ServerLevel level = kaynak.getLevel();
        int ucan = MeteorAPI.aktifMeteorSayisi(level);
        int yagmur = MeteorAPI.aktifYagmurSayisi(level);
        int krater = MeteorAPI.bekleyenKraterSayisi(level);
        int bekleyen = MeteorYoneticisi.bekleyenChunkKraterSayisi(level);
        kaynak.sendSuccess(() -> Component.translatable(BILGI, ucan, MeteorAyarlari.varlikTavani(), yagmur, krater, bekleyen),
                false);
        return ucan;
    }
}
