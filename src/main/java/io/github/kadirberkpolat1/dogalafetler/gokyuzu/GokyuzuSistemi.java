package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Spike: gökyüzü durumunun sunucu tarafı. Durum SavedData'da, değişince herkese, girişte oyuncuya paketle gider.
 * Test komutu: {@code /dogalafet spike gokyuzu <on|off> <normal|buyuyen|supernova|gunessiz|kizilcuce> [ilerleme]}.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class GokyuzuSistemi {
    private static final SimpleCommandExceptionType GECERSIZ =
            new SimpleCommandExceptionType(Component.literal("ay on|off, gunes normal|buyuyen|supernova|gunessiz|kizilcuce"));

    private GokyuzuSistemi() {
    }

    /** Mod inşasında çağrılır: komut kaydı (paket {@link ModOlaylari} ile ortak kurulumda). */
    public static void kaydet() {
        KomutKaydi.ekle(Commands.literal("spike").then(Commands.literal("gokyuzu")
                .then(Commands.literal("durum").executes(GokyuzuSistemi::durumYaz))
                .then(Commands.argument("ay", StringArgumentType.word())
                        .suggests((b, s) -> SharedSuggestionProvider.suggest(new String[] {"on", "off"}, s))
                        .then(Commands.argument("gunes", StringArgumentType.word())
                                .suggests((b, s) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(GunesEvresi.values()).map(e -> e.komutAdi), s))
                                .executes(b -> ayarla(b, -1f))
                                .then(Commands.argument("ilerleme", FloatArgumentType.floatArg(0f, 1f))
                                        .executes(b -> ayarla(b, FloatArgumentType.getFloat(b, "ilerleme"))))))));
    }

    private static int ayarla(CommandContext<CommandSourceStack> b, float ilerleme) throws CommandSyntaxException {
        String ay = StringArgumentType.getString(b, "ay");
        if (!ay.equals("on") && !ay.equals("off")) {
            throw GECERSIZ.create();
        }
        GunesEvresi evre;
        try {
            evre = GunesEvresi.komuttan(StringArgumentType.getString(b, "gunes"));
        } catch (IllegalArgumentException e) {
            throw GECERSIZ.create();
        }
        if (ilerleme < 0f) {
            ilerleme = switch (evre) {
                case NORMAL -> 0f;
                case BUYUYEN, GUNESSIZ -> 0.5f;
                case SUPERNOVA -> 0.1f;
                case KIZIL_CUCE -> 1f;
            };
        }
        GokyuzuDurumu yeni = new GokyuzuDurumu(ay.equals("on"), evre, ilerleme);
        durumAyarla(b.getSource().getServer(), yeni);
        b.getSource().sendSuccess(() -> Component.literal("Gökyüzü: " + yeni), true);
        return 1;
    }

    private static int durumYaz(CommandContext<CommandSourceStack> b) {
        GokyuzuDurumu d = GokyuzuVerisi.al(b.getSource().getServer()).durum();
        b.getSource().sendSuccess(() -> Component.literal("Gökyüzü: " + d), false);
        return 1;
    }

    /** Durumu kaydeder ve bütün oyunculara (her boyutta, N11) yollar. */
    public static void durumAyarla(MinecraftServer sunucu, GokyuzuDurumu yeni) {
        GokyuzuVerisi.al(sunucu).durumAyarla(yeni);
        AgKanali.herkeseGonder(new GokyuzuPaketi(yeni));
        DogalAfetler.LOGGER.info("[gokyuzu] durum ayarlandı ve herkese gönderildi: {}", yeni);
    }

    @SubscribeEvent
    public static void oyuncuGirdi(PlayerEvent.PlayerLoggedInEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            GokyuzuDurumu d = GokyuzuVerisi.al(oyuncu.server).durum();
            AgKanali.oyuncuyaGonder(oyuncu, new GokyuzuPaketi(d));
            DogalAfetler.LOGGER.info("[gokyuzu] girişte {} oyuncusuna gönderildi: {}", oyuncu.getGameProfile().getName(), d);
        }
    }

    /** K0-07: paket kaydı ortak kurulumda; modun kendi dinleyicileri tek thread'de sırayla çalıştığı için kimlik sırası sabit. */
    @Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModOlaylari {
        private ModOlaylari() {
        }

        @SubscribeEvent
        public static void ortakKurulum(FMLCommonSetupEvent olay) {
            AgKanali.sunucudanIstemciye(GokyuzuPaketi.class, GokyuzuPaketi::yaz, GokyuzuPaketi::oku, GokyuzuPaketi::isle);
            DogalAfetler.LOGGER.info("[gokyuzu] paket ortak kurulumda kaydedildi");
        }
    }
}
