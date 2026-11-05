package io.github.kadirberkpolat1.dogalafetler.core.olay;

import io.github.kadirberkpolat1.dogalafetler.core.DonemAlani;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import net.minecraftforge.eventbus.api.Event;

/** Bir dünya dönemi alanı değişti (setter içinden, eşzamanlı). Gökyüzü, sıcaklık, su ve donma dinler. */
public class DonemDegistiEvent extends Event {
    private final DunyaDonemi donem;
    private final DonemAlani alan;

    public DonemDegistiEvent(DunyaDonemi donem, DonemAlani alan) {
        this.donem = donem;
        this.alan = alan;
    }

    public DunyaDonemi donem() {
        return donem;
    }

    public DonemAlani alan() {
        return alan;
    }
}
