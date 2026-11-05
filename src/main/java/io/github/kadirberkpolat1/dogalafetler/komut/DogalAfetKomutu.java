package io.github.kadirberkpolat1.dogalafetler.komut;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** {@code /dogalafet} kök komutu. Yalnız OP (yetki seviyesi 2) kullanabilir (madde 3, KOMUTLAR). */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class DogalAfetKomutu {
    public static final String KOK = "dogalafet";
    public static final int YETKI_SEVIYESI = 2;

    public static final String DURUM = "durum";
    public static final String DURUM_AFET_YOK = "commands.dogalafetler.durum.afet_yok";

    private DogalAfetKomutu() {
    }

    @SubscribeEvent
    public static void komutlariKaydet(RegisterCommandsEvent olay) {
        LiteralArgumentBuilder<CommandSourceStack> kok = Commands.literal(KOK)
                .requires(kaynak -> kaynak.hasPermission(YETKI_SEVIYESI));
        kok.then(Commands.literal(DURUM).executes(DogalAfetKomutu::durum));
        KomutKaydi.baglaTumunu(kok, olay.getBuildContext(), Set.of(DURUM));
        olay.getDispatcher().register(kok);
    }

    private static int durum(CommandContext<CommandSourceStack> baglam) {
        baglam.getSource().sendSuccess(() -> Component.translatable(DURUM_AFET_YOK), false);
        return 1;
    }

    public static void cevirileriYaz(CeviriYazici yazici) {
        yazici.ekle(DURUM_AFET_YOK, "No disaster yet.", "Henüz afet yok.");
    }
}
