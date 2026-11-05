package io.github.kadirberkpolat1.dogalafetler.komut;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** {@code /dogalafet} kökünün kaydını ve yetki seviyesini doğrular. */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KomutOyunTestleri {
    private KomutOyunTestleri() {
    }

    @GameTest(template = "bos")
    public static void kokKomutuOpIleCalisir(GameTestHelper yardimci) {
        CommandDispatcher<CommandSourceStack> dagitici = yardimci.getLevel().getServer().getCommands().getDispatcher();
        CommandNode<CommandSourceStack> kok = dagitici.getRoot().getChild(DogalAfetKomutu.KOK);
        yardimci.assertTrue(kok != null, "/dogalafet kayıtlı değil");
        yardimci.assertTrue(kok.getChild(DogalAfetKomutu.DURUM) != null, "/dogalafet durum kayıtlı değil");

        CommandSourceStack kaynak = yardimci.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
        yardimci.assertTrue(!kok.canUse(kaynak.withPermission(DogalAfetKomutu.YETKI_SEVIYESI - 1)),
                "OP olmayan /dogalafet kullanabiliyor");
        yardimci.assertTrue(kok.canUse(kaynak.withPermission(DogalAfetKomutu.YETKI_SEVIYESI)),
                "OP /dogalafet kullanamıyor");

        try {
            int sonuc = dagitici.execute(DogalAfetKomutu.KOK + " " + DogalAfetKomutu.DURUM,
                    kaynak.withPermission(DogalAfetKomutu.YETKI_SEVIYESI));
            yardimci.assertTrue(sonuc == 1, "/dogalafet durum sonucu 1 değil: " + sonuc);
        } catch (CommandSyntaxException hata) {
            yardimci.fail("/dogalafet durum çalışmadı: " + hata.getMessage());
        }
        yardimci.succeed();
    }
}
