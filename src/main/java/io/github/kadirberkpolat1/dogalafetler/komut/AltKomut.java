package io.github.kadirberkpolat1.dogalafetler.komut;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

/**
 * {@code /dogalafet} altına eklenecek bir alt komutu üretir. Komutlar her sunucu açılışında ve {@code /reload}'da
 * yeniden kurulur; eşya/blok argümanları gereken komutlar {@link CommandBuildContext}'i kullanır.
 */
@FunctionalInterface
public interface AltKomut {
    LiteralArgumentBuilder<CommandSourceStack> olustur(CommandBuildContext baglam);
}
