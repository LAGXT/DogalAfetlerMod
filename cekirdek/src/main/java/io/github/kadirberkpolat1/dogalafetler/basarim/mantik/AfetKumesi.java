package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * #25 "Hepsini Atlattım": oyuncunun en az bir kez sağ çıktığı afet türleri. Oyuncu başına kalıcıdır (oyuncu verisinde
 * tür kimlikleri olarak saklanır); sekiz türün hepsi olunca tamamdır.
 */
public final class AfetKumesi {
    private final Set<AfetTuru> turler = EnumSet.noneOf(AfetTuru.class);

    public static AfetKumesi kimliklerden(Collection<String> kimlikler) {
        AfetKumesi kume = new AfetKumesi();
        for (String kimlik : kimlikler) {
            AfetTuru.kimlikIle(kimlik).ifPresent(kume.turler::add);
        }
        return kume;
    }

    /** Yeni türse true. */
    public boolean ekle(AfetTuru tur) {
        return turler.add(tur);
    }

    public boolean iceriyor(AfetTuru tur) {
        return turler.contains(tur);
    }

    public int boyut() {
        return turler.size();
    }

    public boolean tamamMi() {
        return turler.size() == AfetTuru.values().length;
    }

    /** Kayıt için sıralı kimlikler. */
    public List<String> kimlikler() {
        List<String> sonuc = new ArrayList<>();
        for (AfetTuru tur : turler) {
            sonuc.add(tur.kimlik());
        }
        sonuc.sort(null);
        return sonuc;
    }
}
