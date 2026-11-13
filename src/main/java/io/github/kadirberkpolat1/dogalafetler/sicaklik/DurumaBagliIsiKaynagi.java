package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Config'teki ısı/soğuk kaynağı satırının (blok=güç:yarıçap) gücünü ve yarıçapını blok durumuna göre ölçekleyen bloklar
 * için kanca ({@link OrtamOlcer} tarar). Çarpan 0 ya da altındaysa o durum kaynak sayılmaz (söndürülmüş kütük gibi).
 * Satırı olmayan blok bu arayüzü uygulasa da kaynak olmaz; kaynak tablosu config'ten gelir.
 */
public interface DurumaBagliIsiKaynagi {
    /** Config gücünün bu durumdaki çarpanı; 0 = ısı vermez. */
    double gucCarpani(BlockState durum);

    /** Config yarıçapının bu durumdaki çarpanı (pozitif). */
    double yaricapCarpani(BlockState durum);
}
