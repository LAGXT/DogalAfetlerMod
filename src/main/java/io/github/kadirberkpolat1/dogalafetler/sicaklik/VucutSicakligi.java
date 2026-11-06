package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.IsiAlisverisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SureliEtki;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Oyuncunun vücut sıcaklığı (madde 7a, 3): oyuncu başına, sunucuda, {@code PersistentData}'da ({@value #ETIKET}) saklanır;
 * çıkış/girişte oyuncu dosyasıyla kalır, ölümde sıfırlanır. Değer −100…+100 vücut birimidir (0 nötr).
 *
 * <p>Diğer alt sistemlerin API'si (yalnız sunucu ana thread'i):
 * <ul>
 * <li>{@link #al(ServerPlayer)} — vücut sıcaklığı (başarımlar, yiyecek).</li>
 * <li>{@link #etkiEkle(ServerPlayer, double, int, double)} — yiyecek/içecek etkisi (madde 14): anında bir miktar + süre
 * boyunca giderek azalan hızla toplam bir miktar.</li>
 * <li>{@link #ayarla(ServerPlayer, double)} — komut ve testler.</li>
 * </ul>
 */
public final class VucutSicakligi {
    public static final String ETIKET = "dogalafetler_sicaklik";
    /** Aynı anda tutulan en çok süreli etki (eskisi düşer). */
    public static final int EN_COK_ETKI = 16;

    private VucutSicakligi() {
    }

    private static CompoundTag veri(ServerPlayer oyuncu) {
        CompoundTag kalici = oyuncu.getPersistentData();
        if (!kalici.contains(ETIKET, Tag.TAG_COMPOUND)) {
            kalici.put(ETIKET, new CompoundTag());
        }
        return kalici.getCompound(ETIKET);
    }

    public static double al(ServerPlayer oyuncu) {
        return veri(oyuncu).getDouble("vucut");
    }

    /** Vücut sıcaklığını ayarlar (ölçeğe sıkıştırılır) ve HUD'u hemen tazeler. */
    public static void ayarla(ServerPlayer oyuncu, double deger) {
        yaz(oyuncu, deger);
        SicaklikYoneticisi.hemenGonder(oyuncu);
    }

    static void yaz(ServerPlayer oyuncu, double deger) {
        veri(oyuncu).putDouble("vucut", IsiAlisverisi.sinirla(deger));
    }

    /**
     * Yiyecek/içecek sıcaklık etkisi (madde 14). {@code anlikDelta} hemen eklenir; {@code toplamDelta} {@code sureTick}
     * boyunca giderek azalan hızla eklenir. Değerler vücut birimidir (ölçeğin bir yanı 100: "orta %15–25" = 15–25).
     * Etki sistem kapalıyken de kaydedilir, sıcaklığa sistem açıkken uygulanır.
     */
    public static void etkiEkle(ServerPlayer oyuncu, double anlikDelta, int sureTick, double toplamDelta) {
        CompoundTag v = veri(oyuncu);
        if (anlikDelta != 0) {
            v.putDouble("vucut", IsiAlisverisi.sinirla(v.getDouble("vucut") + anlikDelta));
        }
        if (sureTick > 0 && toplamDelta != 0) {
            ListTag liste = v.getList("etkiler", Tag.TAG_COMPOUND);
            while (liste.size() >= EN_COK_ETKI) {
                liste.remove(0);
            }
            CompoundTag e = new CompoundTag();
            e.putDouble("toplam", toplamDelta);
            e.putInt("sure", sureTick);
            e.putInt("gecen", 0);
            liste.add(e);
            v.put("etkiler", liste);
        }
        SicaklikYoneticisi.hemenGonder(oyuncu);
    }

    /** Süreli etkileri {@code tick} kadar ilerletir, bu adımda verdikleri toplamı döndürür. */
    static double etkileriIlerlet(ServerPlayer oyuncu, int tick) {
        CompoundTag v = veri(oyuncu);
        if (!v.contains("etkiler", Tag.TAG_LIST)) {
            return 0;
        }
        ListTag liste = v.getList("etkiler", Tag.TAG_COMPOUND);
        ListTag kalan = new ListTag();
        double toplam = 0;
        for (int i = 0; i < liste.size(); i++) {
            CompoundTag e = liste.getCompound(i);
            int sure = e.getInt("sure");
            if (sure <= 0) {
                continue;
            }
            SureliEtki etki = new SureliEtki(e.getDouble("toplam"), sure, e.getInt("gecen"));
            toplam += etki.adim(tick);
            if (!etki.bitti()) {
                e.putInt("gecen", etki.gecen());
                kalan.add(e);
            }
        }
        if (kalan.isEmpty()) {
            v.remove("etkiler");
        } else {
            v.put("etkiler", kalan);
        }
        return toplam;
    }

    /** Süren etkilerin kalan toplamı ({@code bilgi} komutu). */
    public static List<SureliEtki> etkiler(ServerPlayer oyuncu) {
        ListTag liste = veri(oyuncu).getList("etkiler", Tag.TAG_COMPOUND);
        List<SureliEtki> sonuc = new ArrayList<>();
        for (int i = 0; i < liste.size(); i++) {
            CompoundTag e = liste.getCompound(i);
            if (e.getInt("sure") > 0) {
                sonuc.add(new SureliEtki(e.getDouble("toplam"), e.getInt("sure"), e.getInt("gecen")));
            }
        }
        return sonuc;
    }

    /** Hasar sayacı (tick): hasar kademesindeyken birikir. */
    static int hasarSayaci(ServerPlayer oyuncu) {
        return veri(oyuncu).getInt("hasarSayaci");
    }

    static void hasarSayaciAyarla(ServerPlayer oyuncu, int deger) {
        veri(oyuncu).putInt("hasarSayaci", Math.max(0, deger));
    }

    /** Respawn: nötr vücut, etkiler ve sayacı silinir (madde 3). */
    public static void sifirla(ServerPlayer oyuncu) {
        oyuncu.getPersistentData().put(ETIKET, new CompoundTag());
    }

    /** Boyut dönüşü gibi ölümsüz kopyada durum yeni oyuncu nesnesine taşınır. */
    static void kopyala(ServerPlayer eski, ServerPlayer yeni) {
        CompoundTag kalici = eski.getPersistentData();
        if (kalici.contains(ETIKET, Tag.TAG_COMPOUND)) {
            yeni.getPersistentData().put(ETIKET, kalici.getCompound(ETIKET).copy());
        }
    }
}
