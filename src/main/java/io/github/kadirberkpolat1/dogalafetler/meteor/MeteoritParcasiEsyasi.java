package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZamani;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Meteorit Parçası ve Kozmik Çekirdek: yerde {@code meteor.odulOmruSaniye} (10 dk) sonra yok olur (M-07). */
public class MeteoritParcasiEsyasi extends Item {
    public MeteoritParcasiEsyasi(Properties ozellikler) {
        super(ozellikler);
    }

    @Override
    public int getEntityLifespan(ItemStack yigin, Level level) {
        try {
            return OdulZamani.omurTick(MeteorAyarlari.odulOmruSaniye());
        } catch (IllegalStateException yuklenmedi) {
            return OdulZamani.omurTick(OdulZamani.OMUR_SANIYE);
        }
    }
}
