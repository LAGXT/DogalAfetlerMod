package io.github.kadirberkpolat1.dogalafetler.uyari;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Spike komutları: {@code /dogalafet spike siren <standart|gunes> [saniye]} ve {@code /dogalafet spike siren dur}.
 * Sohbete geri bildirim yazmaz (ekran görüntüsünde yazı olmasın, N6); olanı sunucu log'una yazar.
 */
final class SirenKomutu {
    private static final String SANIYE = "saniye";

    private SirenKomutu() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> olustur() {
        LiteralArgumentBuilder<CommandSourceStack> siren = Commands.literal("siren");
        for (SirenTuru tur : SirenTuru.values()) {
            siren.then(Commands.literal(tur.kimlik())
                    .executes(baglam -> baslat(baglam, tur, varsayilanSure(tur)))
                    .then(Commands.argument(SANIYE, IntegerArgumentType.integer(1, SirenTuru.EN_UZUN_TICK / SirenTuru.SANIYE))
                            .executes(baglam -> baslat(baglam, tur,
                                    IntegerArgumentType.getInteger(baglam, SANIYE) * SirenTuru.SANIYE))));
        }
        siren.then(Commands.literal("dur").executes(SirenKomutu::durdur));
        return Commands.literal("spike").then(siren);
    }

    private static int varsayilanSure(SirenTuru tur) {
        return tur == SirenTuru.GUNES ? SirenTuru.UZUN_SURE_TICK : SirenTuru.STANDART_SURE_TICK;
    }

    private static int baslat(CommandContext<CommandSourceStack> baglam, SirenTuru tur, int sureTick) {
        SirenPaketleri.herkeseBaslat(tur, sureTick);
        int oyuncu = baglam.getSource().getServer().getPlayerCount();
        DogalAfetler.LOGGER.info("[siren] sunucu: SirenPaketi({}, {} tick) {} oyuncuya gönderildi", tur.kimlik(), sureTick,
                oyuncu);
        return oyuncu;
    }

    private static int durdur(CommandContext<CommandSourceStack> baglam) {
        SirenPaketleri.herkeseDurdur();
        int oyuncu = baglam.getSource().getServer().getPlayerCount();
        DogalAfetler.LOGGER.info("[siren] sunucu: SirenDurdurPaketi {} oyuncuya gönderildi", oyuncu);
        return oyuncu;
    }
}
