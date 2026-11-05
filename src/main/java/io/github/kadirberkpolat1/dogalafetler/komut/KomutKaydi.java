package io.github.kadirberkpolat1.dogalafetler.komut;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

/**
 * {@code /dogalafet} alt komutlarının kayıt kancası. Alt sistemler mod inşası sırasında kaydeder, örneğin:
 *
 * <pre>{@code
 * KomutKaydi.ekle(Commands.literal("sicaklik").then(...));
 * KomutKaydi.ekle(baglam -> Commands.literal("odul").then(...));
 * }</pre>
 *
 * Aynı adlı iki alt komut Brigadier'da sessizce birleşeceği için reddedilir.
 */
public final class KomutKaydi {
    private static final List<AltKomut> ALT_KOMUTLAR = new CopyOnWriteArrayList<>();

    private KomutKaydi() {
    }

    public static void ekle(LiteralArgumentBuilder<CommandSourceStack> altKomut) {
        Objects.requireNonNull(altKomut, "altKomut");
        ALT_KOMUTLAR.add(baglam -> altKomut);
    }

    public static void ekle(AltKomut altKomut) {
        ALT_KOMUTLAR.add(Objects.requireNonNull(altKomut, "altKomut"));
    }

    /** Kayıtlı alt komutları köke bağlar; {@code ayrilmisAdlar} kökün kendi alt komutlarıdır. */
    static void baglaTumunu(LiteralArgumentBuilder<CommandSourceStack> kok, CommandBuildContext baglam,
            Set<String> ayrilmisAdlar) {
        Set<String> kullanilan = new HashSet<>(ayrilmisAdlar);
        for (AltKomut altKomut : ALT_KOMUTLAR) {
            LiteralCommandNode<CommandSourceStack> dugum = altKomut.olustur(baglam).build();
            if (!kullanilan.add(dugum.getLiteral())) {
                throw new IllegalStateException("/" + kok.getLiteral() + " altında aynı adlı iki alt komut var: "
                        + dugum.getLiteral());
            }
            kok.then(dugum);
        }
    }
}
