package io.github.kadirberkpolat1.dogalafetler.basarim;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.AfetKumesi;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Unvan;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.advancements.Advancement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /dogalafet unvan <oyuncu> [ad|yok]} (N16 kozmetik ünvanını gösterir, verir ya da kaldırır) ve
 * {@code /dogalafet basarim bilgi [oyuncu]} (sekmede yüklenen başarım sayısı; oyuncunun ilerlemesi). Yetki kök
 * komuttan gelir (OP, madde 3).
 */
final class BasarimKomutlari {
    static final String YOK = "yok";

    private BasarimKomutlari() {
    }

    static void kaydet() {
        KomutKaydi.ekle(Commands.literal("unvan")
                .then(Commands.argument("oyuncu", EntityArgument.player())
                        .executes(k -> goster(k.getSource(), EntityArgument.getPlayer(k, "oyuncu")))
                        .then(Commands.argument("ad", StringArgumentType.word())
                                .suggests((k, oneri) -> SharedSuggestionProvider.suggest(adlar(), oneri))
                                .executes(k -> ayarla(k.getSource(), EntityArgument.getPlayer(k, "oyuncu"),
                                        StringArgumentType.getString(k, "ad"))))));
        KomutKaydi.ekle(Commands.literal("basarim")
                .then(Commands.literal("bilgi")
                        .executes(k -> bilgi(k.getSource(), null))
                        .then(Commands.argument("oyuncu", EntityArgument.player())
                                .executes(k -> bilgi(k.getSource(), EntityArgument.getPlayer(k, "oyuncu"))))));
    }

    private static List<String> adlar() {
        List<String> adlar = new ArrayList<>();
        for (Unvan unvan : Unvan.values()) {
            adlar.add(unvan.kimlik());
        }
        adlar.add(YOK);
        return adlar;
    }

    private static int goster(CommandSourceStack kaynak, ServerPlayer oyuncu) {
        Optional<Unvan> unvan = BasarimVerisi.unvan(oyuncu);
        if (unvan.isPresent()) {
            kaynak.sendSuccess(() -> Component.translatable("commands.dogalafetler.unvan.mevcut", oyuncu.getName(),
                    Unvanlar.onek(unvan.get())), false);
        } else {
            kaynak.sendSuccess(() -> Component.translatable("commands.dogalafetler.unvan.yok", oyuncu.getName()),
                    false);
        }
        if (!BasarimAyarlari.unvanGoster()) {
            kaynak.sendSuccess(() -> Component.translatable("commands.dogalafetler.unvan.gizli"), false);
        }
        return unvan.isPresent() ? 1 : 0;
    }

    private static int ayarla(CommandSourceStack kaynak, ServerPlayer oyuncu, String ad) {
        if (YOK.equals(ad.toLowerCase(Locale.ROOT))) {
            Unvanlar.ayarla(oyuncu, Optional.empty());
            kaynak.sendSuccess(() -> Component.translatable("commands.dogalafetler.unvan.kaldirildi", oyuncu.getName()),
                    true);
            return 1;
        }
        Optional<Unvan> unvan = Unvan.kimlikIle(ad);
        if (unvan.isEmpty()) {
            kaynak.sendFailure(Component.translatable("commands.dogalafetler.unvan.bilinmeyen", ad,
                    String.join(", ", adlar())));
            return 0;
        }
        Unvanlar.ayarla(oyuncu, unvan);
        kaynak.sendSuccess(() -> Component.translatable("commands.dogalafetler.unvan.ayarlandi", oyuncu.getName(),
                Unvanlar.onek(unvan.get())), true);
        return 1;
    }

    private static int bilgi(CommandSourceStack kaynak, ServerPlayer oyuncu) {
        List<Advancement> sekme = new ArrayList<>();
        for (Advancement basarim : kaynak.getServer().getAdvancements().getAllAdvancements()) {
            if (DogalAfetler.MOD_ID.equals(basarim.getId().getNamespace())
                    && basarim.getId().getPath().startsWith("dogalafetler/")) {
                sekme.add(basarim);
            }
        }
        int yuklu = sekme.size();
        kaynak.sendSuccess(() -> Component.translatable("commands.dogalafetler.basarim.yuklu", yuklu,
                Basarimlar.values().length), false);
        if (oyuncu == null) {
            return yuklu;
        }
        int kazanilan = 0;
        for (Advancement basarim : sekme) {
            if (oyuncu.getAdvancements().getOrStartProgress(basarim).isDone()) {
                kazanilan++;
            }
        }
        AfetKumesi atlatilan = BasarimVerisi.atlatilanlar(oyuncu);
        String turler = atlatilan.kimlikler().isEmpty() ? "-" : String.join(", ", atlatilan.kimlikler());
        int toplamKazanilan = kazanilan;
        Component unvan = BasarimVerisi.unvan(oyuncu).<Component>map(Unvanlar::onek)
                .orElse(Component.translatable("commands.dogalafetler.basarim.unvan_yok"));
        String kutupGun = String.format(Locale.ROOT, "%.2f", BasarimVerisi.kutup(oyuncu).gun());
        int balik = BasarimVerisi.balik(oyuncu);
        kaynak.sendSuccess(() -> Component.translatable("commands.dogalafetler.basarim.oyuncu", oyuncu.getName(),
                toplamKazanilan, yuklu, atlatilan.boyut(), AfetTuru.values().length, turler, balik, kutupGun, unvan),
                false);
        return toplamKazanilan;
    }
}
