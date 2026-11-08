package io.github.kadirberkpolat1.dogalafetler.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Dünya dönemindeki süren afetlerin NBT biçimi (AS-8): {@value #ANAHTAR} listesi, başlama sırasıyla. Çoklu afetten önceki
 * kayıtlar tek afeti {@value #ESKI_ANAHTAR} altında tutuyordu; o da okunur (liste yoksa tek elemanlı liste olur). Yazarken
 * yalnız liste yazılır. Yalnız NBT sınıflarına dayanır.
 */
public final class AfetKayitListesi {
    public static final String ANAHTAR = "aktifAfetler";
    public static final String ESKI_ANAHTAR = "aktifAfet";

    private AfetKayitListesi() {
    }

    /** Kayıttaki afet etiketlerinin kopyaları; hiç yoksa boş. */
    public static List<CompoundTag> oku(CompoundTag donem) {
        List<CompoundTag> sonuc = new ArrayList<>();
        if (donem.contains(ANAHTAR, Tag.TAG_LIST)) {
            for (Tag t : donem.getList(ANAHTAR, Tag.TAG_COMPOUND)) {
                sonuc.add(((CompoundTag) t).copy());
            }
        } else if (donem.contains(ESKI_ANAHTAR, Tag.TAG_COMPOUND)) {
            sonuc.add(donem.getCompound(ESKI_ANAHTAR).copy());
        }
        return sonuc;
    }

    /** Afet etiketlerini {@code donem}'e yazar; liste boşsa anahtar hiç yazılmaz. */
    public static void yaz(CompoundTag donem, List<CompoundTag> afetler) {
        if (afetler.isEmpty()) {
            return;
        }
        ListTag liste = new ListTag();
        for (CompoundTag afet : afetler) {
            liste.add(afet.copy());
        }
        donem.put(ANAHTAR, liste);
    }
}
