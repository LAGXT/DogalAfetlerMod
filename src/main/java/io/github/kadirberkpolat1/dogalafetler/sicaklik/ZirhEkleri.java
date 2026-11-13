package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Yalitim;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * Zırh parçasının sıcaklık ekleri (N1: ItemStack NBT'si). Hepsi tek bir {@value #KOK} bileşik etiketinde durur:
 * {@code tuy} (Tüy Upgrade'i sayısı, N2), {@code yunlu} (N3) ve sonradan kaydedilen ekler (Faz 3: {@code termostatli}).
 * Ekler {@link #kaydet} ile kaydedilir; tooltip ve {@link ZirhEkiTarifi} kayıtlı eki tanır.
 */
public final class ZirhEkleri {
    public static final String KOK = DogalAfetler.MOD_ID;
    public static final String TUY = "tuy";

    /** Bir zirh parçasına bir kez uygulanabilen ek (yünlü, termostatlı). */
    public record Ek(String anahtar, String ceviriAnahtari) {
        private static final Pattern AD = Pattern.compile("[a-z0-9_]+");

        public Ek {
            if (!AD.matcher(anahtar).matches() || TUY.equals(anahtar)) {
                throw new IllegalArgumentException("Ek adı küçük ASCII olmalı ve 'tuy' olamaz: " + anahtar);
            }
        }
    }

    public static final Ek YUNLU = new Ek("yunlu", "tooltip.dogalafetler.yunlu");

    private static final List<Ek> EKLER = new CopyOnWriteArrayList<>(List.of(YUNLU));

    private ZirhEkleri() {
    }

    /** Yeni bir ek türü kaydeder (ör. Faz 3 termostat: {@code new Ek("termostatli", "tooltip.dogalafetler.termostatli")}). */
    public static void kaydet(Ek ek) {
        Objects.requireNonNull(ek, "ek");
        for (Ek var : EKLER) {
            if (var.anahtar().equals(ek.anahtar())) {
                throw new IllegalStateException("Aynı adlı zırh eki iki kez kaydedildi: " + ek.anahtar());
            }
        }
        EKLER.add(ek);
    }

    public static List<Ek> ekler() {
        return List.copyOf(EKLER);
    }

    public static Ek ek(String anahtar) {
        for (Ek ek : EKLER) {
            if (ek.anahtar().equals(anahtar)) {
                return ek;
            }
        }
        throw new IllegalArgumentException("Kayıtlı olmayan zırh eki: " + anahtar);
    }

    public static boolean zirhMi(ItemStack yigin) {
        return !yigin.isEmpty() && yigin.getItem() instanceof ArmorItem;
    }

    private static CompoundTag okunacak(ItemStack yigin) {
        CompoundTag etiket = yigin.getTag();
        return etiket != null && etiket.contains(KOK, Tag.TAG_COMPOUND) ? etiket.getCompound(KOK) : null;
    }

    public static int tuySayisi(ItemStack yigin) {
        CompoundTag kok = okunacak(yigin);
        return kok == null ? 0 : Math.max(0, kok.getInt(TUY));
    }

    public static void tuyAyarla(ItemStack yigin, int sayi) {
        yigin.getOrCreateTagElement(KOK).putInt(TUY, Math.max(0, sayi));
    }

    public static boolean varMi(ItemStack yigin, Ek ek) {
        CompoundTag kok = okunacak(yigin);
        return kok != null && kok.getBoolean(ek.anahtar());
    }

    public static void koy(ItemStack yigin, Ek ek) {
        yigin.getOrCreateTagElement(KOK).putBoolean(ek.anahtar(), true);
    }

    /** Giyilen dört zırh yuvasından yalıtım girdisi (her parça ayrı sayılır, toplam dört parçanınkı). */
    public static Yalitim.Girdi yalitimGirdisi(LivingEntity varlik) {
        int zirh = 0;
        int yun = 0;
        int tuy = 0;
        int termostat = 0;
        Ek termostatEki = termostatEki();
        for (EquipmentSlot yuva : EquipmentSlot.values()) {
            if (yuva.getType() != EquipmentSlot.Type.ARMOR) {
                continue;
            }
            ItemStack yigin = varlik.getItemBySlot(yuva);
            if (!zirhMi(yigin)) {
                continue;
            }
            zirh++;
            tuy += tuySayisi(yigin);
            if (varMi(yigin, YUNLU)) {
                yun++;
            }
            if (termostatEki != null && varMi(yigin, termostatEki)) {
                termostat++;
            }
        }
        return new Yalitim.Girdi(zirh, yun, tuy, termostat);
    }

    /** Faz 3 termostat eki kayıtlıysa ({@code termostatli}) onu döndürür. */
    static Ek termostatEki() {
        for (Ek ek : EKLER) {
            if (ek.anahtar().equals("termostatli")) {
                return ek;
            }
        }
        return null;
    }
}
