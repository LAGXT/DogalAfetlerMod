package io.github.kadirberkpolat1.dogalafetler.journal;

import com.mojang.brigadier.context.CommandContext;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /dogalafet defter}: Afet Defteri'ni açar (oyuncuya veriyi tek paketle yollar, ekranı istemci açar). Diğer
 * {@code /dogalafet} alt komutlarının aksine OP gerektirmez, yalnız oyuncular kullanabilir ({@link KomutKaydi#ekleAcik}).
 */
final class DefterKomutu {
    static final String AD = "defter";

    private DefterKomutu() {
    }

    static void kaydet() {
        KomutKaydi.ekleAcik(Commands.literal(AD)
                .requires(kaynak -> kaynak.getEntity() instanceof ServerPlayer)
                .executes(DefterKomutu::ac));
    }

    private static int ac(CommandContext<CommandSourceStack> baglam) {
        ServerPlayer oyuncu = baglam.getSource().getPlayer();
        if (oyuncu == null) {
            return 0;
        }
        DefterPaketi.gonder(oyuncu);
        return 1;
    }
}
