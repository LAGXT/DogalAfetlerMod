package io.github.kadirberkpolat1.dogalafetler.kural;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Madde 11'deki gamerule'ların gerçek dünyada kayıtlı ve varsayılanlarının şartnamedeki gibi olduğunu doğrular. */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KuralOyunTestleri {
    private KuralOyunTestleri() {
    }

    @GameTest(template = "bos")
    public static void varsayilanlarSartnamedekiGibi(GameTestHelper yardimci) {
        GameRules kurallar = yardimci.getLevel().getGameRules();
        yardimci.assertTrue(Kurallar.MANTIKSAL_KURALLAR.size() + Kurallar.TAMSAYI_KURALLAR.size() == 13,
                "madde 11'de 13 gamerule var");
        for (GameRules.Key<GameRules.BooleanValue> anahtar : Kurallar.MANTIKSAL_KURALLAR) {
            yardimci.assertTrue(kurallar.getBoolean(anahtar), anahtar.getId() + " varsayılanı true olmalı");
        }
        yardimci.assertTrue(kurallar.getInt(Kurallar.AFET_SIKLIGI) == 100, "afetSikligi varsayılanı 100 olmalı");
        yardimci.assertTrue(kurallar.getInt(Kurallar.AFET_MAKS_SIDDET) == 5, "afetMaksSiddet varsayılanı 5 olmalı");
        for (AfetTuru tur : AfetTuru.values()) {
            yardimci.assertTrue(Kurallar.afetSecilebilir(kurallar, tur), tur.kimlik() + " varsayılan olarak seçilebilir olmalı");
        }
        yardimci.succeed();
    }
}
