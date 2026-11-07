package io.github.kadirberkpolat1.dogalafetler.komut;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * {@code /dogalafet} kök komutu. Yalnız OP (yetki seviyesi 2) kullanabilir (madde 3, KOMUTLAR). Çekirdeğin alt komutları
 * burada kurulur; diğer alt komutlar sahipleri tarafından {@link KomutKaydi#ekle} ile gelir. Alt komutsuz {@code /dogalafet}
 * kullanım listesini verir.
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

    /** Süren ve durdurulmuş dönem süreçlerinin kimlikleri ({@code asama ileri [süreç]}, K-C48). */
    private static final SuggestionProvider<CommandSourceStack> SUREC_ADLARI = (baglam, kurucu) -> {
        AfetYoneticisi yonetici = AfetYoneticisi.al(baglam.getSource().getServer());
        return SharedSuggestionProvider.suggest(Stream.concat(yonetici.aktifDonemSurecleri().stream(),
                yonetici.durdurulmusDonemSurecleri().keySet().stream()).map(DonemSureci::kimlik), kurucu);
    };

    private DogalAfetKomutu() {
    }

    @SubscribeEvent
    public static void komutlariKaydet(RegisterCommandsEvent olay) {
        LiteralArgumentBuilder<CommandSourceStack> kok = Commands.literal(KOK)
                .requires(kaynak -> kaynak.hasPermission(YETKI_SEVIYESI))
                .executes(CekirdekKomutlari::yardim);
        kok.then(Commands.literal(DURUM).executes(CekirdekKomutlari::durum));
        kok.then(Commands.literal(START)
                .then(Commands.argument("afet", StringArgumentType.word()).suggests(AFET_ADLARI)
                        .executes(b -> CekirdekKomutlari.start(b, null, null))
                        .then(Commands.argument("seviye", IntegerArgumentType.integer(AfetTuru.EN_DUSUK_SEVIYE,
                                AfetTuru.EN_YUKSEK_SEVIYE))
                                .executes(b -> CekirdekKomutlari.start(b, IntegerArgumentType.getInteger(b, "seviye"), null))
                                .then(Commands.argument("oyuncu", EntityArgument.player())
                                        .executes(b -> CekirdekKomutlari.start(b,
                                                IntegerArgumentType.getInteger(b, "seviye"),
                                                EntityArgument.getPlayer(b, "oyuncu")))))));
        kok.then(Commands.literal(STOP).executes(CekirdekKomutlari::stop));
        kok.then(Commands.literal(GUN)
                .then(Commands.argument("sayi", IntegerArgumentType.integer(0, 10_000_000))
                        .executes(CekirdekKomutlari::gun)));
        kok.then(Commands.literal(HIZ)
                .then(Commands.argument("carpan", DoubleArgumentType.doubleArg(ZamanOlcegi.EN_AZ, ZamanOlcegi.EN_COK))
                        .executes(CekirdekKomutlari::hiz)));
        kok.then(Commands.literal(SIKLIK).executes(b -> CekirdekKomutlari.siklik(b, null))
                .then(Commands.argument("yuzde", IntegerArgumentType.integer(KuralSinirlari.SIKLIK_EN_AZ,
                        KuralSinirlari.SIKLIK_EN_COK))
                        .executes(b -> CekirdekKomutlari.siklik(b, IntegerArgumentType.getInteger(b, "yuzde")))));
        kok.then(Commands.literal(SIFIRLA).executes(CekirdekKomutlari::sifirla));
        kok.then(Commands.literal(ASAMA).then(Commands.literal(ILERI)
                .executes(b -> CekirdekKomutlari.asamaIleri(b, Optional.empty()))
                .then(Commands.argument("surec", StringArgumentType.word()).suggests(SUREC_ADLARI)
                        .executes(b -> CekirdekKomutlari.asamaIleri(b,
                                Optional.of(StringArgumentType.getString(b, "surec")))))));
        KomutKaydi.baglaTumunu(kok, olay.getBuildContext(), CEKIRDEK_ALT_KOMUTLARI);
        olay.getDispatcher().register(kok);
    }
}
