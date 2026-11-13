package io.github.kadirberkpolat1.dogalafetler.komut;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
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
 *
 * <p>Kök komut oyunculara da görünür ({@link DogalAfetKomutu#kokKullanilabilir}); bu yüzden {@link #ekle} ile kaydedilen her
 * alt komuta kendi koşuluna ek olarak OP yetkisi (seviye {@value DogalAfetKomutu#YETKI_SEVIYESI}) eklenir. Herkesin (oyuncunun)
 * kullanabileceği komut yalnız {@link #ekleAcik} ile kaydedilir ve kendi {@code requires} koşulunu kendisi koyar.
 */
public final class KomutKaydi {
    private static final List<AltKomut> ALT_KOMUTLAR = new CopyOnWriteArrayList<>();
    private static final List<AltKomut> ACIK_ALT_KOMUTLAR = new CopyOnWriteArrayList<>();

    private KomutKaydi() {
    }

    /** OP gerektiren alt komut (kökün varsayılanı). */
    public static void ekle(LiteralArgumentBuilder<CommandSourceStack> altKomut) {
        Objects.requireNonNull(altKomut, "altKomut");
        opGerektir(altKomut);
        ALT_KOMUTLAR.add(baglam -> altKomut);
    }

    /** OP gerektiren alt komut (kökün varsayılanı). */
    public static void ekle(AltKomut altKomut) {
        Objects.requireNonNull(altKomut, "altKomut");
        ALT_KOMUTLAR.add(baglam -> opGerektir(altKomut.olustur(baglam)));
    }

    /** OP yetkisi eklenmeyen alt komut: koşulu ({@code requires}) komutun kendisi koyar (ör. {@code defter}: yalnız oyuncu). */
    public static void ekleAcik(LiteralArgumentBuilder<CommandSourceStack> altKomut) {
        Objects.requireNonNull(altKomut, "altKomut");
        ACIK_ALT_KOMUTLAR.add(baglam -> altKomut);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> opGerektir(LiteralArgumentBuilder<CommandSourceStack> altKomut) {
        Predicate<CommandSourceStack> onceki = altKomut.getRequirement();
        return altKomut.requires(kaynak -> kaynak.hasPermission(DogalAfetKomutu.YETKI_SEVIYESI) && onceki.test(kaynak));
    }

    /** Kayıtlı alt komutları köke bağlar; {@code ayrilmisAdlar} kökün kendi alt komutlarıdır. */
    static void baglaTumunu(LiteralArgumentBuilder<CommandSourceStack> kok, CommandBuildContext baglam,
            Set<String> ayrilmisAdlar) {
        Set<String> kullanilan = new HashSet<>(ayrilmisAdlar);
        for (List<AltKomut> liste : List.of(ALT_KOMUTLAR, ACIK_ALT_KOMUTLAR)) {
            for (AltKomut altKomut : liste) {
                LiteralCommandNode<CommandSourceStack> dugum = altKomut.olustur(baglam).build();
                if (!kullanilan.add(dugum.getLiteral())) {
                    throw new IllegalStateException("/" + kok.getLiteral() + " altında aynı adlı iki alt komut var: "
                            + dugum.getLiteral());
                }
                kok.then(dugum);
            }
        }
    }
}
