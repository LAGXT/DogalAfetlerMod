package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

/**
 * Sunucunun tek gökyüzü durumu: ay var/yok, güneş evresi ve evre ilerlemesi (0–1). Ay ve güneş birbirinden
 * bağımsızdır (madde 16). Değişmez; her değişiklik yeni nesnedir.
 */
public record GokyuzuDurumu(boolean ayVar, GunesEvresi evre, float ilerleme) {
    public static final GokyuzuDurumu VARSAYILAN = new GokyuzuDurumu(true, GunesEvresi.NORMAL, 0f);

    public GokyuzuDurumu {
        ilerleme = Mth.clamp(ilerleme, 0f, 1f);
    }

    /** Vanilla gökyüzü aynen kalır mı? (renderer devre dışı, başka modun gökyüzü korunur) */
    public boolean vanillaMi() {
        return ayVar && evre == GunesEvresi.NORMAL;
    }

    public CompoundTag nbtYaz(CompoundTag etiket) {
        etiket.putBoolean("ayVar", ayVar);
        etiket.putString("gunesEvresi", evre.komutAdi);
        etiket.putFloat("ilerleme", ilerleme);
        return etiket;
    }

    public static GokyuzuDurumu nbtOku(CompoundTag etiket) {
        GunesEvresi evre;
        try {
            evre = GunesEvresi.komuttan(etiket.getString("gunesEvresi"));
        } catch (IllegalArgumentException e) {
            evre = GunesEvresi.NORMAL;
        }
        boolean ay = !etiket.contains("ayVar") || etiket.getBoolean("ayVar");
        return new GokyuzuDurumu(ay, evre, etiket.getFloat("ilerleme"));
    }

    public void yaz(FriendlyByteBuf tampon) {
        tampon.writeBoolean(ayVar);
        tampon.writeEnum(evre);
        tampon.writeFloat(ilerleme);
    }

    public static GokyuzuDurumu oku(FriendlyByteBuf tampon) {
        return new GokyuzuDurumu(tampon.readBoolean(), tampon.readEnum(GunesEvresi.class), tampon.readFloat());
    }

    @Override
    public String toString() {
        return "ay=" + (ayVar ? "var" : "yok") + ", gunes=" + evre.komutAdi + ", ilerleme=" + ilerleme;
    }
}
