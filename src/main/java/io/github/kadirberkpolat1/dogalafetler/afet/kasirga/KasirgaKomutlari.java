package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** {@code /dogalafet kasirga bilgi} (OP): aktif kasırganın durumu ve dünyadaki kasırga/uçan blok varlık sayıları (temiz bitiş kanıtı). */
final class KasirgaKomutlari {
    static final String BILGI = "commands.dogalafetler.kasirga.bilgi";
    static final String YOK = "commands.dogalafetler.kasirga.yok";
    private static final int YETKI = 2;

    private KasirgaKomutlari() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> komut() {
        return Commands.literal("kasirga").requires(k -> k.hasPermission(YETKI))
                .then(Commands.literal("bilgi").executes(b -> bilgi(b.getSource())));
    }

    private static int bilgi(CommandSourceStack kaynak) {
        MinecraftServer server = kaynak.getServer();
        int kasirgaVarligi = 0;
        int ucanVarlik = 0;
        for (ServerLevel l : server.getAllLevels()) {
            kasirgaVarligi += l.getEntities(KasirgaKayitlari.KASIRGA.get(), e -> true).size();
            ucanVarlik += l.getEntities(KasirgaKayitlari.UCAN_BLOK.get(), e -> true).size();
        }
        int kv = kasirgaVarligi;
        int uv = ucanVarlik;
        Kasirga k = AfetYoneticisi.al(server).aktifAfet(AfetTuru.KASIRGA).filter(Kasirga.class::isInstance).map(Kasirga.class::cast)
                .orElse(null);
        if (k == null) {
            kaynak.sendSuccess(() -> Component.translatable(YOK, kv, uv), false);
            return 0;
        }
        KasirgaSeviyesi s = k.seviyeBilgisi();
        String merkez = BlockPos.containing(k.konum()).toShortString();
        long yon = Math.round(Math.toDegrees(k.yon()));
        kaynak.sendSuccess(() -> Component.translatable(BILGI, k.asama().name(), k.durum().evre().name(),
                String.format(Locale.ROOT, "%.2f", k.durum().ilerleme()), s.seviye(), merkez, yon,
                k.ucanBlokSayisi(), KasirgaAyarlari.ucanBlokTavani(), k.toplamKaldirilan(), kv, uv), false);
        return 1;
    }
}
