package io.github.kadirberkpolat1.dogalafetler.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Sanal suyun kalıcılığı (madde 3, SU-24): dalga, çekilme ve mağara cebi <b>parametreleri</b> dönemin {@code ekVeri("su")}
 * etiketinde ({@code dalgalar}, {@code cekilmeler}, {@code magaralar}) durur ve sunucu açılınca yeniden kurulur. Mağara
 * hücreleri saklanmaz: cebin en alçak hücresi, yüzeyi, hacmi ve kutusu saklanır, hücreler
 * {@link MagaraDolumu#seviyeyeKadar} ile aynı dünyadan yeniden hesaplanır (kayıt başına ~0,3 KB). Zamanlar ölçekli afet
 * saatidir; saat kalıcı ve monoton olduğu için mağara kaldığı yerden sürer. Yalnız sunucu ana thread'i.
 */
public final class SuKalicilik {
    static final String DALGALAR = "dalgalar";
    static final String CEKILMELER = "cekilmeler";
    static final String MAGARALAR = "magaralar";
    /** {@code ekVeri} küçük kalsın (çekirdek önerisi ≤ 16 KB): tür başına en çok bu kadar kayıt (en yenileri). */
    static final int EN_COK_KAYIT = 40;

    private SuKalicilik() {
    }

    public static void dalgaYaz(CompoundTag t, int id, Dalga d) {
        t.putInt("id", id);
        double[] v = {d.x0(), d.z0(), d.yonX(), d.yonZ(), d.baslangic(), d.hiz(), d.yukseklik(), d.genislik(), d.uzunluk(),
                d.taban(), d.menzil(), d.ivme(), d.hasar()};
        ListTag l = new ListTag();
        for (double x : v) {
            l.add(net.minecraft.nbt.DoubleTag.valueOf(x));
        }
        t.put("p", l);
    }

    public static Dalga dalgaOku(CompoundTag t) {
        ListTag l = t.getList("p", Tag.TAG_DOUBLE);
        double[] v = new double[13];
        for (int i = 0; i < v.length; i++) {
            v[i] = l.getDouble(i);
        }
        return new Dalga(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11], v[12]);
    }

    public static void cekilmeYaz(CompoundTag t, Cekilme c) {
        t.putDouble("x", c.x());
        t.putDouble("z", c.z());
        t.putDouble("r", c.yaricap());
        t.putDouble("d", c.derinlik());
        t.putLong("bas", c.baslangic());
        t.putLong("sure", c.sure());
    }

    public static Cekilme cekilmeOku(CompoundTag t) {
        return new Cekilme(t.getDouble("x"), t.getDouble("z"), t.getDouble("r"), t.getDouble("d"), t.getLong("bas"),
                t.getLong("sure"));
    }

    /** Mağara zaman çizelgesi ve donma anı (NaN: donmamış); {@code minY}/{@code ustY} cepten gelir. */
    public static void zamanYaz(CompoundTag t, MagaraZamani z, double donma) {
        t.putDouble("bas", z.baslangic());
        t.putDouble("dolma", z.dolmaHizi());
        t.putDouble("bekleme", z.bekleme());
        t.putDouble("buhar", z.buharlasmaHizi());
        t.putDouble("donma", donma);
    }

    public static MagaraZamani zamanOku(CompoundTag t, double minY, double ustY) {
        return new MagaraZamani(t.getDouble("bas"), minY, ustY, t.getDouble("dolma"), t.getDouble("bekleme"),
                t.getDouble("buhar"));
    }

    public static double donmaOku(CompoundTag t) {
        return t.getDouble("donma");
    }

    /** Bütün boyutların dalga, çekilme ve mağara parametrelerini döneme yazar (her senkronda, {@link SanalSu#yayinla}). */
    static void yaz(MinecraftServer server) {
        if (server == null) {
            return;
        }
        ListTag dalgalar = new ListTag();
        ListTag cekilmeler = new ListTag();
        ListTag magaralar = new ListTag();
        for (Map.Entry<ResourceKey<Level>, SanalSu.Boyut> e : SanalSu.BOYUTLAR.entrySet()) {
            String boyut = e.getKey().location().toString();
            SanalSu.Boyut b = e.getValue();
            for (Map.Entry<Integer, Dalga> d : son(b.dunya.dalgalar())) {
                CompoundTag t = new CompoundTag();
                t.putString("boyut", boyut);
                dalgaYaz(t, d.getKey(), d.getValue());
                dalgalar.add(t);
            }
            for (Map.Entry<Integer, Cekilme> c : son(b.dunya.cekilmeler())) {
                CompoundTag t = new CompoundTag();
                t.putString("boyut", boyut);
                t.putInt("id", c.getKey());
                cekilmeYaz(t, c.getValue());
                cekilmeler.add(t);
            }
            for (Map.Entry<Integer, SanalSuDunyasi.Magara> m : son(b.dunya.magaralar())) {
                MagaraCebi cep = m.getValue().cep();
                int[] tohum = cep.enAlcak();
                long[] kutu = b.magaraKutusu.get(m.getKey());
                if (tohum == null || kutu == null) {
                    continue;
                }
                CompoundTag t = new CompoundTag();
                t.putString("boyut", boyut);
                t.putInt("id", m.getKey());
                t.putIntArray("tohum", tohum);
                t.putInt("ustY", cep.ustY());
                t.putInt("agizY", cep.agizY());
                t.putInt("hacim", cep.hacim());
                t.putLongArray("kutu", kutu);
                zamanYaz(t, m.getValue().zaman(), m.getValue().donma());
                magaralar.add(t);
            }
        }
        DunyaDonemi donem = DunyaDonemi.al(server);
        CompoundTag su = donem.ekVeri(DenizYukselmesiSureci.VERI);
        if (su.getList(DALGALAR, Tag.TAG_COMPOUND).equals(dalgalar)
                && su.getList(CEKILMELER, Tag.TAG_COMPOUND).equals(cekilmeler)
                && su.getList(MAGARALAR, Tag.TAG_COMPOUND).equals(magaralar)) {
            return;
        }
        su.put(DALGALAR, dalgalar);
        su.put(CEKILMELER, cekilmeler);
        su.put(MAGARALAR, magaralar);
        donem.ekVeriDegisti(DenizYukselmesiSureci.VERI);
    }

    private static <V> List<Map.Entry<Integer, V>> son(Map<Integer, V> m) {
        List<Map.Entry<Integer, V>> l = new ArrayList<>(m.entrySet());
        if (l.size() > EN_COK_KAYIT) {
            l.sort(Map.Entry.comparingByKey());
            DogalAfetler.LOGGER.warn("Sanal su: {} kayıttan yalnız en yeni {} tanesi kalıcı", l.size(), EN_COK_KAYIT);
            l = l.subList(l.size() - EN_COK_KAYIT, l.size());
        }
        return l;
    }

    private static ServerLevel seviye(MinecraftServer server, String boyut) {
        ResourceLocation r = ResourceLocation.tryParse(boyut);
        return r == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, r));
    }

    /**
     * {@code su} etiketindeki kayıtları yeniden kurar (sunucu açılışında ve test). Mağara kutusunun chunk'ları yüklenir, cep
     * yeniden hesaplanır; hacim tutmazsa (dünya değişti) kayıt atılır ve uyarılır. Kurulan sayıları döner
     * ({@code {dalga, cekilme, magara}}).
     */
    public static int[] kur(MinecraftServer server, CompoundTag su) {
        int[] sayi = new int[3];
        for (Tag x : su.getList(DALGALAR, Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) x;
            ServerLevel l = seviye(server, t.getString("boyut"));
            if (l != null) {
                int id = t.getInt("id");
                SanalSu.kimlikGordu(id);
                SanalSu.boyut(l).dunya.dalgaEkle(id, dalgaOku(t));
                sayi[0]++;
            }
        }
        for (Tag x : su.getList(CEKILMELER, Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) x;
            ServerLevel l = seviye(server, t.getString("boyut"));
            if (l != null) {
                int id = t.getInt("id");
                SanalSu.kimlikGordu(id);
                SanalSu.boyut(l).dunya.cekilmeAyarla(id, cekilmeOku(t));
                sayi[1]++;
            }
        }
        for (Tag x : su.getList(MAGARALAR, Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) x;
            ServerLevel l = seviye(server, t.getString("boyut"));
            long[] kutu = t.getLongArray("kutu");
            int[] tohum = t.getIntArray("tohum");
            if (l == null || kutu.length != 7 || tohum.length != 3) {
                continue;
            }
            for (int cx = (int) kutu[0] >> 4; cx <= (int) kutu[3] >> 4; cx++) {
                for (int cz = (int) kutu[2] >> 4; cz <= (int) kutu[5] >> 4; cz++) {
                    l.getChunk(cx, cz);
                }
            }
            int hacim = t.getInt("hacim");
            MagaraCebi cep = MagaraDolumu.seviyeyeKadar(SanalSu.magaraHucresi(l), tohum[0], tohum[1], tohum[2], t.getInt("ustY"),
                    t.getInt("agizY"), hacim);
            int id = t.getInt("id");
            if (cep.hacim() != hacim) {
                DogalAfetler.LOGGER.warn("Sanal su: mağara {} yeniden kurulamadı (kayıtlı hacim {}, bulunan {}); atıldı", id, hacim,
                        cep.hacim());
                continue;
            }
            SanalSu.kimlikGordu(id);
            SanalSu.Boyut b = SanalSu.boyut(l);
            b.magaraKutusu.put(id, kutu);
            b.dunya.magaraKoy(id, new SanalSuDunyasi.Magara(cep, zamanOku(t, cep.minY(), cep.ustY()), donmaOku(t)));
            sayi[2]++;
        }
        if (sayi[0] + sayi[1] + sayi[2] > 0) {
            DogalAfetler.LOGGER.info("Sanal su: kayıttan yeniden kuruldu: {} dalga, {} çekilme, {} mağara", sayi[0], sayi[1], sayi[2]);
            for (ServerLevel l : server.getAllLevels()) {
                if (SanalSu.BOYUTLAR.containsKey(l.dimension())) {
                    SanalSu.yayinla(l);
                }
            }
        }
        return sayi;
    }
}
