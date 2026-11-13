package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.basarim.OzelBasarim;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.level.BlockEvent;

/**
 * "Sifon" başarımı ({@link OzelBasarim#SIFON}). Tsunami suyu sanaldır (dalga ve mağara cebi gerçek blok değildir); kova ya da sünger
 * onu alamaz. Su bir deliğe (çukur, mağara, kazı) girdiğinde oyuncu o suyun içine blok koyup yolunu keserse ya da deliği
 * kapatırsa verilir: tsunami sürerken ve konulan blok o sütundaki sanal su yüzeyinin altındaysa (dalga, mağara ya da
 * yükselmiş deniz üstü; yanındaki zemin su yüzeyinin altında kaldığı için su oraya girer). Tsunami yoksa olay hemen dışarı çıkar;
 * olay başına tek sütun sorgusu. Sunucu ana thread'i.
 */
final class TsunamiSifon {
    private TsunamiSifon() {
    }

    static void blokYerlestirildi(BlockEvent.EntityPlaceEvent olay) {
        if (!(olay.getEntity() instanceof ServerPlayer oyuncu) || !(olay.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (AfetYoneticisi.al(level.getServer()).aktifAfet(AfetTuru.TSUNAMI).isEmpty()) {
            return;
        }
        BlockPos p = olay.getPos();
        double yuzey = SanalSu.suSeviyesi(level, p.getX(), p.getZ());
        if (!Double.isNaN(yuzey) && p.getY() + 1 <= yuzey + 0.05) {
            OzelBasarim.ver(oyuncu, OzelBasarim.SIFON);
        }
    }
}
