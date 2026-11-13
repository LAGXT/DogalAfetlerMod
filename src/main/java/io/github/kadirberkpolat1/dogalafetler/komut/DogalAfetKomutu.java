package io.github.kadirberkpolat1.dogalafetler.komut;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DonemSureci;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.ZamanOlcegi;
import io.github.kadirberkpolat1.dogalafetler.kural.KuralSinirlari;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * {@code /dogalafet} kök komutu. Kökün kendisi oyunculara açıktır ({@link #kokKullanilabilir}); çekirdeğin her alt komutu ve
 * {@link KomutKaydi#ekle} ile gelenler yalnız OP (yetki seviyesi 2) kullanabilir (madde 3, KOMUTLAR); OP gerektirmeyen tek yol
 * {@link KomutKaydi#ekleAcik}'tir (ör. {@code defter}). Alt komutsuz {@code /dogalafet} OP'ye kullanım listesini verir.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class DogalAfetKomutu {
    public static final String KOK = "dogalafet";
    public static final int YETKI_SEVIYESI = 2;

    public static final String DURUM = "durum";
    public static final String START = "start";
    public static final String STOP = "stop";
    public static final String GUN = "gun";
    public static final String HIZ = "hiz";
    public static final String SIKLIK = "siklik";
    public static final String SIFIRLA = "sifirla";
    public static final String ASAMA = "asama";
    public static final String ILERI = "ileri";

    /** Çekirdeğin alt komut adları; başka paket bunları kaydedemez. */
    public static final Set<String> CEKIRDEK_ALT_KOMUTLARI = Set.of(DURUM, START, STOP, GUN, HIZ, SIKLIK, SIFIRLA,
            ASAMA);

    /** N18 afet adları (kayıtlı olsun olmasın; kayıtlı olmayan için komut "henüz kayıtlı değil" der). */
    private static final SuggestionProvider<CommandSourceStack> AFET_ADLARI = (baglam, kurucu) ->
            SharedSuggestionProvider.suggest(Arrays.stream(AfetTuru.values()).map(AfetTuru::kimlik), kurucu);

    /**
     * Süren ve durdurulmuş dönem süreçlerinin kimlikleri ve o an geçerli takma adları ({@code asama ileri [süreç]}, K-C48;
     * {@link DonemSureci#takmaAdlar}).
     */
    private static final SuggestionProvider<CommandSourceStack> SUREC_ADLARI = (baglam, kurucu) -> {
        AfetYoneticisi yonetici = AfetYoneticisi.al(baglam.getSource().getServer());
        ServerLevel ust = baglam.getSource().getServer().overworld();
        return SharedSuggestionProvider.suggest(Stream.concat(yonetici.aktifDonemSurecleri().stream(),
                yonetici.durdurulmusDonemSurecleri().keySet().stream())
                .flatMap(surec -> Stream.concat(Stream.of(surec.kimlik()), surec.takmaAdlar(ust).stream()))
                .distinct(), kurucu);
    };

    /** Süren afetlerin adları ({@code stop <afet>}, AS-8). */
    private static final SuggestionProvider<CommandSourceStack> SUREN_AFETLER = (baglam, kurucu) ->
            SharedSuggestionProvider.suggest(AfetYoneticisi.al(baglam.getSource().getServer()).aktifAfetler().stream()
                    .map(afet -> afet.tur().kimlik()), kurucu);

    /** {@code asama ileri [afet|süreç]}: süren afetler ve dönem süreçleri (AS-8, K-C48). */
    private static final SuggestionProvider<CommandSourceStack> ASAMA_HEDEFLERI = (baglam, kurucu) -> {
        SUREN_AFETLER.getSuggestions(baglam, kurucu);
        return SUREC_ADLARI.getSuggestions(baglam, kurucu);
    };

    private DogalAfetKomutu() {
    }

    @SubscribeEvent
    public static void komutlariKaydet(RegisterCommandsEvent olay) {
        LiteralArgumentBuilder<CommandSourceStack> kok = Commands.literal(KOK)
                .requires(DogalAfetKomutu::kokKullanilabilir)
                .executes(DogalAfetKomutu::kokYardim);
        kok.then(op(Commands.literal(DURUM)).executes(CekirdekKomutlari::durum));
        kok.then(op(Commands.literal(START))
                .then(Commands.argument("afet", StringArgumentType.word()).suggests(AFET_ADLARI)
                        .executes(b -> CekirdekKomutlari.start(b, null, null))
                        .then(Commands.argument("seviye", IntegerArgumentType.integer(AfetTuru.EN_DUSUK_SEVIYE,
                                AfetTuru.EN_YUKSEK_SEVIYE))
                                .executes(b -> CekirdekKomutlari.start(b, IntegerArgumentType.getInteger(b, "seviye"), null))
                                .then(Commands.argument("oyuncu", EntityArgument.player())
                                        .executes(b -> CekirdekKomutlari.start(b,
                                                IntegerArgumentType.getInteger(b, "seviye"),
                                                EntityArgument.getPlayer(b, "oyuncu")))))));
        kok.then(op(Commands.literal(STOP)).executes(b -> CekirdekKomutlari.stop(b, Optional.empty()))
                .then(Commands.argument("afet", StringArgumentType.word()).suggests(SUREN_AFETLER)
                        .executes(b -> CekirdekKomutlari.stop(b, Optional.of(StringArgumentType.getString(b, "afet"))))));
        kok.then(op(Commands.literal(GUN))
                .then(Commands.argument("sayi", IntegerArgumentType.integer(0, 10_000_000))
                        .executes(CekirdekKomutlari::gun)));
        kok.then(op(Commands.literal(HIZ))
                .then(Commands.argument("carpan", DoubleArgumentType.doubleArg(ZamanOlcegi.EN_AZ, ZamanOlcegi.EN_COK))
                        .executes(CekirdekKomutlari::hiz)));
        kok.then(op(Commands.literal(SIKLIK)).executes(b -> CekirdekKomutlari.siklik(b, null))
                .then(Commands.argument("yuzde", IntegerArgumentType.integer(KuralSinirlari.SIKLIK_EN_AZ,
                        KuralSinirlari.SIKLIK_EN_COK))
                        .executes(b -> CekirdekKomutlari.siklik(b, IntegerArgumentType.getInteger(b, "yuzde")))));
        kok.then(op(Commands.literal(SIFIRLA)).executes(CekirdekKomutlari::sifirla));
        kok.then(op(Commands.literal(ASAMA)).then(Commands.literal(ILERI)
                .executes(b -> CekirdekKomutlari.asamaIleri(b, Optional.empty()))
                .then(Commands.argument("surec", StringArgumentType.word()).suggests(ASAMA_HEDEFLERI)
                        .executes(b -> CekirdekKomutlari.asamaIleri(b,
                                Optional.of(StringArgumentType.getString(b, "surec")))))));
        KomutKaydi.baglaTumunu(kok, olay.getBuildContext(), CEKIRDEK_ALT_KOMUTLARI);
        olay.getDispatcher().register(kok);
    }

    /** Kökün görünürlüğü: OP ya da oyuncu (oyuncuya yalnız {@link KomutKaydi#ekleAcik} komutları açık kalır). */
    public static boolean kokKullanilabilir(CommandSourceStack kaynak) {
        return kaynak.hasPermission(YETKI_SEVIYESI) || kaynak.getEntity() instanceof ServerPlayer;
    }

    /** Alt komuta OP yetkisi koyar. */
    private static LiteralArgumentBuilder<CommandSourceStack> op(LiteralArgumentBuilder<CommandSourceStack> altKomut) {
        return altKomut.requires(kaynak -> kaynak.hasPermission(YETKI_SEVIYESI));
    }

    /** Alt komutsuz {@code /dogalafet}: OP'ye kullanım listesi; oyuncu için Brigadier'ın "bilinmeyen komut" yanıtı. */
    private static int kokYardim(CommandContext<CommandSourceStack> baglam) {
        if (!baglam.getSource().hasPermission(YETKI_SEVIYESI)) {
            baglam.getSource().sendFailure(Component.translatable("command.unknown.command"));
            return 0;
        }
        return CekirdekKomutlari.yardim(baglam);
    }
}
