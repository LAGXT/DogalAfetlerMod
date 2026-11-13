package io.github.kadirberkpolat1.dogalafetler.su;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Bir boyuttaki sanal suyun tam durumu (sunucu -> istemci, SU-01): hücre değil parametre gider (dalga 13, çekilme 6 sayı);
 * yalnız mağara cepleri sütun aralıklarıyla, varint ve sütun-delta kodlamasıyla gider (SU-10 B9). Deniz artışı çekirdeğin
 * dönem paketinde gelir. Değişimde boyuttaki oyunculara, girişte/boyut değişiminde/yeniden doğuşta o oyuncuya.
 */
public record SuPaketi(ResourceKey<Level> boyut, Map<Integer, Dalga> dalgalar, Map<Integer, SanalSuDunyasi.Magara> magaralar,
        Map<Integer, Cekilme> cekilmeler, Map<Integer, SelSuyu> seller, List<IsiBolgesi> isiBolgeleri) {
    public static SuPaketi durumdan(ResourceKey<Level> boyut, SanalSuDunyasi d) {
        return new SuPaketi(boyut, new HashMap<>(d.dalgalar()), new HashMap<>(d.magaralar()), new HashMap<>(d.cekilmeler()),
                new HashMap<>(d.seller()), List.of(d.isiDizisi()));
    }

    public static void yaz(SuPaketi m, FriendlyByteBuf b) {
        b.writeResourceKey(m.boyut);
        b.writeVarInt(m.dalgalar.size());
        m.dalgalar.forEach((id, w) -> {
            b.writeVarInt(id);
            for (double v : new double[] {w.x0(), w.z0(), w.yonX(), w.yonZ(), w.baslangic(), w.hiz(), w.yukseklik(),
                    w.genislik(), w.uzunluk(), w.taban(), w.menzil(), w.ivme(), w.hasar()}) {
                b.writeDouble(v);
            }
            Seritler s = w.seritler();
            b.writeDouble(s.karaBasi());
            b.writeByteArray(s.erisimBayt());
            b.writeByteArray(s.olcekBayt());
        });
        b.writeVarInt(m.magaralar.size());
        m.magaralar.forEach((id, g) -> {
            b.writeVarInt(id);
            MagaraZamani zm = g.zaman();
            for (double v : new double[] {zm.baslangic(), zm.minY(), zm.ustY(), zm.dolmaHizi(), zm.bekleme(),
                    zm.buharlasmaHizi(), g.donma()}) {
                b.writeDouble(v);
            }
            MagaraCebi c = g.cep();
            b.writeVarInt(c.hacim());
            b.writeInt(c.minY());
            b.writeInt(c.ustY());
            b.writeInt(c.agizY());
            long[] anahtarlar = c.sutunAnahtarlari();
            java.util.Arrays.sort(anahtarlar);
            b.writeVarInt(anahtarlar.length);
            int oncekiX = 0;
            int oncekiZ = 0;
            for (long k : anahtarlar) {
                int x = (int) (k >> 32);
                int z = (int) k;
                b.writeVarInt(zigzag(x - oncekiX));
                b.writeVarInt(zigzag(z - oncekiZ));
                oncekiX = x;
                oncekiZ = z;
                int[] r = c.sutunlar().get(k);
                b.writeVarInt(r.length / 2);
                int onceki = c.minY();
                for (int y : r) {
                    b.writeVarInt(zigzag(y - onceki));
                    onceki = y;
                }
            }
        });
        b.writeVarInt(m.cekilmeler.size());
        m.cekilmeler.forEach((id, c) -> {
            b.writeVarInt(id);
            b.writeDouble(c.x());
            b.writeDouble(c.z());
            b.writeDouble(c.yaricap());
            b.writeDouble(c.derinlik());
            b.writeLong(c.baslangic());
            b.writeLong(c.sure());
        });
        selYaz(b, m.seller);
        isiYaz(b, m.isiBolgeleri);
    }

    /** Sel suyu parametreleri: kimlik ve beş sayı (başlangıç, yükselme, zirve, inme, hedef). */
    static void selYaz(FriendlyByteBuf b, Map<Integer, SelSuyu> seller) {
        b.writeVarInt(seller.size());
        seller.forEach((id, s) -> {
            b.writeVarInt(id);
            b.writeDouble(s.baslangic());
            b.writeDouble(s.yukselme());
            b.writeDouble(s.zirve());
            b.writeDouble(s.inme());
            b.writeDouble(s.hedef());
        });
    }

    static Map<Integer, SelSuyu> selOku(FriendlyByteBuf b) {
        Map<Integer, SelSuyu> seller = new HashMap<>();
        for (int n = b.readVarInt(); n > 0; n--) {
            seller.put(b.readVarInt(), new SelSuyu(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                    b.readDouble()));
        }
        return seller;
    }

    /** Sel suyunu buharlaştıran ısı kaynakları: blok konumu (x, y, z) ve yarıçap. */
    static void isiYaz(FriendlyByteBuf b, List<IsiBolgesi> bolgeler) {
        b.writeVarInt(bolgeler.size());
        for (IsiBolgesi i : bolgeler) {
            b.writeInt(i.x());
            b.writeShort(i.y());
            b.writeInt(i.z());
            b.writeFloat(i.yaricap());
        }
    }

    static List<IsiBolgesi> isiOku(FriendlyByteBuf b) {
        int sayi = b.readVarInt();
        List<IsiBolgesi> bolgeler = new ArrayList<>(sayi);
        for (int n = sayi; n > 0; n--) {
            bolgeler.add(new IsiBolgesi(b.readInt(), b.readShort(), b.readInt(), b.readFloat()));
        }
        return bolgeler;
    }

    private static int zigzag(int v) {
        return (v << 1) ^ (v >> 31);
    }

    private static int geriZigzag(int v) {
        return (v >>> 1) ^ -(v & 1);
    }

    public static SuPaketi oku(FriendlyByteBuf b) {
        ResourceKey<Level> boyut = b.readResourceKey(Registries.DIMENSION);
        Map<Integer, Dalga> dalgalar = new HashMap<>();
        for (int n = b.readVarInt(); n > 0; n--) {
            int id = b.readVarInt();
            double[] v = new double[13];
            for (int i = 0; i < v.length; i++) {
                v[i] = b.readDouble();
            }
            Seritler seritler = new Seritler(b.readDouble(), b.readByteArray(), b.readByteArray());
            dalgalar.put(id, new Dalga(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11], v[12], seritler));
        }
        Map<Integer, SanalSuDunyasi.Magara> magaralar = new HashMap<>();
        for (int n = b.readVarInt(); n > 0; n--) {
            int id = b.readVarInt();
            double[] v = new double[7];
            for (int i = 0; i < v.length; i++) {
                v[i] = b.readDouble();
            }
            int hacim = b.readVarInt();
            int minY = b.readInt();
            int ustY = b.readInt();
            int agizY = b.readInt();
            Map<Long, int[]> sutunlar = new HashMap<>();
            int x = 0;
            int z = 0;
            for (int s = b.readVarInt(); s > 0; s--) {
                x += geriZigzag(b.readVarInt());
                z += geriZigzag(b.readVarInt());
                int[] r = new int[b.readVarInt() * 2];
                int onceki = minY;
                for (int i = 0; i < r.length; i++) {
                    onceki += geriZigzag(b.readVarInt());
                    r[i] = onceki;
                }
                sutunlar.put(MagaraCebi.anahtar(x, z), r);
            }
            magaralar.put(id, new SanalSuDunyasi.Magara(new MagaraCebi(sutunlar, hacim, minY, ustY, 0, false, agizY),
                    new MagaraZamani(v[0], v[1], v[2], v[3], v[4], v[5]), v[6]));
        }
        Map<Integer, Cekilme> cekilmeler = new HashMap<>();
        for (int n = b.readVarInt(); n > 0; n--) {
            cekilmeler.put(b.readVarInt(), new Cekilme(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                    b.readLong(), b.readLong()));
        }
        Map<Integer, SelSuyu> seller = selOku(b);
        List<IsiBolgesi> isiBolgeleri = isiOku(b);
        return new SuPaketi(boyut, dalgalar, magaralar, cekilmeler, seller, isiBolgeleri);
    }

    /** İstemci ana thread'inde: {@code client.su.IstemciSu}'ya N15 yoluyla iletilir. */
    public static void isle(SuPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        Consumer<SuPaketi> alici = DistExecutor.safeCallWhenOn(Dist.CLIENT,
                () -> io.github.kadirberkpolat1.dogalafetler.client.su.IstemciSu::paketAlicisi);
        if (alici != null) {
            alici.accept(paket);
        }
    }

    /** İstemcinin kopyasını bu durumla değiştirir (deniz artışı hariç). */
    public void uygula(SanalSuDunyasi d) {
        d.temizle();
        dalgalar.forEach(d::dalgaEkle);
        magaralar.forEach(d::magaraKoy);
        cekilmeler.forEach(d::cekilmeAyarla);
        seller.forEach(d::selEkle);
        d.isiBolgeleriAyarla(isiBolgeleri);
    }

    /** Log için kodlanmış boyut (bayt). */
    public int boyutBayt() {
        FriendlyByteBuf b = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            yaz(this, b);
            return b.writerIndex();
        } finally {
            b.release();
        }
    }

    static List<Integer> kimlikler(Map<Integer, ?> m) {
        return new ArrayList<>(m.keySet());
    }
}
