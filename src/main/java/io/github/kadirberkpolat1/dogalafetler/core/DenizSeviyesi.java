package io.github.kadirberkpolat1.dogalafetler.core;

import net.minecraft.nbt.CompoundTag;

/**
 * Deniz seviyesi artışının global durumu (bölüm H, spike-sanal-su SU-01: diskte yalnız global artış). Değerler blok
 * cinsindendir; {@code baslangicSaati} yükselişin başladığı ölçekli afet saatidir. Adım adım yükseltmeyi {@code su}
 * sistemi yapar; çekirdek yalnız saklar ve senkronlar.
 */
public record DenizSeviyesi(float hedefArtis, float mevcutArtis, long baslangicSaati, boolean kalici) {
    public static final float EN_COK = 64f;
    public static final DenizSeviyesi VARSAYILAN = new DenizSeviyesi(0f, 0f, 0L, true);

    public DenizSeviyesi {
        hedefArtis = sinirla(hedefArtis);
        mevcutArtis = sinirla(mevcutArtis);
    }

    private static float sinirla(float deger) {
        return Float.isNaN(deger) ? 0f : Math.max(0f, Math.min(EN_COK, deger));
    }

    public CompoundTag yaz() {
        CompoundTag etiket = new CompoundTag();
        etiket.putFloat("hedef", hedefArtis);
        etiket.putFloat("mevcut", mevcutArtis);
        etiket.putLong("baslangic", baslangicSaati);
        etiket.putBoolean("kalici", kalici);
        return etiket;
    }

    public static DenizSeviyesi oku(CompoundTag etiket) {
        if (etiket.isEmpty()) {
            return VARSAYILAN;
        }
        return new DenizSeviyesi(etiket.getFloat("hedef"), etiket.getFloat("mevcut"), etiket.getLong("baslangic"),
                !etiket.contains("kalici") || etiket.getBoolean("kalici"));
    }
}
