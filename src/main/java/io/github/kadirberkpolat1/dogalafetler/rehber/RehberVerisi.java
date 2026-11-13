package io.github.kadirberkpolat1.dogalafetler.rehber;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Oyuncu başına kalıcı "bu cümle söylendi" bayrakları: {@code getPersistentData()} içinde {@value #ETIKET} (başarım ve sıcaklık
 * verisiyle aynı desen). Oyuncu dosyasıyla dünyanın kaydedilir, yani bayrak her dünyada ayrıdır; ölüm ve End dönüşünde
 * {@link #kopyala} ile yeni oyuncuya taşınır. Yalnız sunucuda okunur, istemciye gitmez.
 */
public final class RehberVerisi {
    static final String ETIKET = "dogalafetler_rehber";

    private RehberVerisi() {
    }

    /** Bu cümle bu oyuncuya daha önce söylendi mi. */
    public static boolean soylendi(ServerPlayer oyuncu, RehberMesaji mesaj) {
        CompoundTag kalici = oyuncu.getPersistentData();
        return kalici.contains(ETIKET, Tag.TAG_COMPOUND) && kalici.getCompound(ETIKET).getBoolean(mesaj.anahtar());
    }

    /** Cümleyi söylenmiş say. */
    static void isaretle(ServerPlayer oyuncu, RehberMesaji mesaj) {
        CompoundTag kalici = oyuncu.getPersistentData();
        if (!kalici.contains(ETIKET, Tag.TAG_COMPOUND)) {
            kalici.put(ETIKET, new CompoundTag());
        }
        kalici.getCompound(ETIKET).putBoolean(mesaj.anahtar(), true);
    }

    /** Ölüm ya da End dönüşü ({@code PlayerEvent.Clone}): bayraklar yeni oyuncu nesnesine taşınır. */
    static void kopyala(ServerPlayer eski, ServerPlayer yeni) {
        CompoundTag kalici = eski.getPersistentData();
        if (kalici.contains(ETIKET, Tag.TAG_COMPOUND)) {
            yeni.getPersistentData().put(ETIKET, kalici.getCompound(ETIKET).copy());
        }
    }
}
