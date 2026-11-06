package io.github.kadirberkpolat1.dogalafetler.yangin;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

/** Boyut başına aktif yangın kümesi ({@code dogalafetler_yangin}); sunucu yeniden açılınca yangın sürer. */
final class YanginVerisi extends SavedData {
    static final String AD = "dogalafetler_yangin";
    private static final int BICIM_SURUMU = 2;
    private static final int SURE_PENCERESI = 100;

    /** Ekleme sırası = yaş: tavan aşılınca baştakiler söner. */
    final LinkedHashMap<Long, YanginHucresi> hucreler = new LinkedHashMap<>();
    /** Hücresi kalkmış ama chunk'ı yüklü olmadığı için silinemeyen ateş blokları (Y-19); chunk yüklenince kalkar. */
    final LongOpenHashSet bekleyenSilmeler = new LongOpenHashSet();

    // Çalışma durumu (kaydedilmez).
    final RandomSource rastgele = RandomSource.create();
    /** Blok hasarı kapalıyken yanıp biten konumlar: bitiş zamanına kadar yeniden tutuşmaz. */
    final Map<Long, Long> yanmislar = new HashMap<>();
    final List<Long> buharlar = new ArrayList<>();
    final Set<UUID> paketAlanlar = new HashSet<>();
    /** Birkaç tick'e yayılan {@code alanTutustur} işleri (Y-21), sırayla. */
    final ArrayDeque<YanginYoneticisi.AlanIsi> alanIsleri = new ArrayDeque<>();
    long alanButcesiTick = Long.MIN_VALUE;
    int alanButcesi;
    /** {@code ChunkEvent.Load} (başka thread'den de gelebilir) bekleyen silmeleri hemen denetletir. */
    volatile boolean chunkYuklendi;
    int sonButce = -1;
    YanginHucresi[] adaylar = new YanginHucresi[64];
    double[] puanlar = new double[64];
    private final double[] sureler = new double[SURE_PENCERESI];
    private int sureIndeksi;
    private int sureSayisi;

    static YanginVerisi yukle(CompoundTag etiket) {
        YanginVerisi veri = new YanginVerisi();
        ListTag liste = etiket.getList("hucreler", Tag.TAG_COMPOUND);
        for (int i = 0; i < liste.size(); i++) {
            CompoundTag h = liste.getCompound(i);
            YanginHucresi hucre = new YanginHucresi(h.getLong("k"), YanginKaynagi.oku(h.getString("t"), h.getLong("n")),
                    h.getFloat("y"), h.getLong("b"), h.getLong("e"));
            hucre.sonIslenme = h.getLong("s");
            hucre.yayilabilir = h.getBoolean("p");
            if (h.contains("a", Tag.TAG_LONG)) {
                hucre.atesKonumu = h.getLong("a");
                hucre.atesVar = true;
            }
            veri.hucreler.put(hucre.konum, hucre);
        }
        for (long konum : etiket.getLongArray("bekleyen")) {
            veri.bekleyenSilmeler.add(konum);
        }
        return veri;
    }

    @Override
    public CompoundTag save(CompoundTag etiket) {
        etiket.putInt("surum", BICIM_SURUMU);
        ListTag liste = new ListTag();
        for (YanginHucresi hucre : hucreler.values()) {
            CompoundTag h = new CompoundTag();
            h.putLong("k", hucre.konum);
            h.putString("t", hucre.kaynak.turKimligi());
            h.putLong("n", hucre.kaynak.afetNo());
            h.putFloat("y", hucre.yogunluk);
            h.putLong("b", hucre.baslangic);
            h.putLong("e", hucre.bitis);
            h.putLong("s", hucre.sonIslenme);
            h.putBoolean("p", hucre.yayilabilir);
            if (hucre.atesVar) {
                h.putLong("a", hucre.atesKonumu);
            }
            liste.add(h);
        }
        etiket.put("hucreler", liste);
        etiket.putLongArray("bekleyen", bekleyenSilmeler.toLongArray());
        return etiket;
    }

    void diziHazirla(int boy) {
        if (adaylar.length < boy) {
            int yeni = Math.max(boy, adaylar.length * 2);
            adaylar = new YanginHucresi[yeni];
            puanlar = new double[yeni];
        }
    }

    /** Yöneticinin bu boyuttaki bir tick'lik süresi (ms); son {@value #SURE_PENCERESI} çalışan tick tutulur. */
    void sureKaydet(double ms) {
        sureler[sureIndeksi] = ms;
        sureIndeksi = (sureIndeksi + 1) % sureler.length;
        sureSayisi = Math.min(sureler.length, sureSayisi + 1);
    }

    /** {ortalama, en çok} ms; ölçüm yoksa {0, 0}. */
    double[] sureOzeti() {
        double toplam = 0;
        double enCok = 0;
        for (int i = 0; i < sureSayisi; i++) {
            toplam += sureler[i];
            enCok = Math.max(enCok, sureler[i]);
        }
        return new double[] {sureSayisi == 0 ? 0 : toplam / sureSayisi, enCok};
    }
}
