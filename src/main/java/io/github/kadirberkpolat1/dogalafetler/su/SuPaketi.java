package io.github.kadirberkpolat1.dogalafetler.su;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Sanal suyun tam durumu (sunucu -> istemci). Hücre değil parametre gider: deniz için üç sayı, dalga için 11, çekilme
 * için 6; yalnız mağara cepleri sütun aralıklarıyla gider. İstemci alanı ve yüzeyi aynı formülle kendisi hesaplar.
 */
public final class SuPaketi {
    private final double denizHedef;
    private final long denizBaslangic;
    private final long denizSure;
    private final List<TsunamiDalgasi> dalgalar;
    private final List<SanalSuDunyasi.Magara> magaralar;
    private final Cekilme cekilme;
    private final int yol;

    private SuPaketi(double denizHedef, long denizBaslangic, long denizSure, List<TsunamiDalgasi> dalgalar,
            List<SanalSuDunyasi.Magara> magaralar, Cekilme cekilme, int yol) {
        this.denizHedef = denizHedef;
        this.denizBaslangic = denizBaslangic;
        this.denizSure = denizSure;
        this.dalgalar = dalgalar;
        this.magaralar = magaralar;
        this.cekilme = cekilme;
        this.yol = yol;
    }

    public static SuPaketi durumdan(SanalSuDunyasi d) {
        return new SuPaketi(d.denizHedef(), d.denizBaslangic(), d.denizSure(), new ArrayList<>(d.dalgalar()),
                new ArrayList<>(d.magaralar()), d.cekilme(), SanalSu.yol.ordinal());
    }

    public static void yaz(SuPaketi m, FriendlyByteBuf b) {
        b.writeDouble(m.denizHedef);
        b.writeLong(m.denizBaslangic);
        b.writeLong(m.denizSure);
        b.writeByte(m.yol);
        b.writeVarInt(m.dalgalar.size());
        for (TsunamiDalgasi w : m.dalgalar) {
            for (double v : new double[] {w.x0(), w.z0(), w.yonX(), w.yonZ(), w.baslangic(), w.hiz(), w.yukseklik(),
                    w.genislik(), w.uzunluk(), w.taban(), w.menzil()}) {
                b.writeDouble(v);
            }
        }
        b.writeVarInt(m.magaralar.size());
        for (SanalSuDunyasi.Magara g : m.magaralar) {
            MagaraZamani z = g.zaman();
            for (double v : new double[] {z.baslangic(), z.minY(), z.ustY(), z.dolmaHizi(), z.bekleme(),
                    z.buharlasmaHizi()}) {
                b.writeDouble(v);
            }
            MagaraCebi c = g.cep();
            b.writeVarInt(c.hacim());
            b.writeInt(c.minY());
            b.writeInt(c.ustY());
            b.writeVarInt(c.ziyaretEdilen());
            b.writeBoolean(c.acik());
            b.writeVarInt(c.sutunSayisi());
            for (Map.Entry<Long, int[]> e : c.sutunlar().entrySet()) {
                b.writeLong(e.getKey());
                int[] r = e.getValue();
                b.writeVarInt(r.length);
                for (int y : r) {
                    b.writeInt(y);
                }
            }
        }
        b.writeBoolean(m.cekilme != null);
        if (m.cekilme != null) {
            Cekilme c = m.cekilme;
            b.writeDouble(c.x());
            b.writeDouble(c.z());
            b.writeDouble(c.yaricap());
            b.writeDouble(c.derinlik());
            b.writeLong(c.baslangic());
            b.writeLong(c.sure());
        }
    }

    public static SuPaketi oku(FriendlyByteBuf b) {
        double hedef = b.readDouble();
        long bas = b.readLong();
        long sure = b.readLong();
        int yol = b.readByte();
        int n = b.readVarInt();
        List<TsunamiDalgasi> dalgalar = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            dalgalar.add(new TsunamiDalgasi(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                    b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                    b.readDouble()));
        }
        n = b.readVarInt();
        List<SanalSuDunyasi.Magara> magaralar = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            MagaraZamani z = new MagaraZamani(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                    b.readDouble(), b.readDouble());
            int hacim = b.readVarInt();
            int minY = b.readInt();
            int ustY = b.readInt();
            int ziyaret = b.readVarInt();
            boolean acik = b.readBoolean();
            int sutun = b.readVarInt();
            Map<Long, int[]> s = new HashMap<>();
            for (int j = 0; j < sutun; j++) {
                long k = b.readLong();
                int[] r = new int[b.readVarInt()];
                for (int q = 0; q < r.length; q++) {
                    r[q] = b.readInt();
                }
                s.put(k, r);
            }
            magaralar.add(new SanalSuDunyasi.Magara(new MagaraCebi(s, hacim, minY, ustY, ziyaret, acik), z));
        }
        Cekilme c = b.readBoolean()
                ? new Cekilme(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readLong(), b.readLong())
                : null;
        return new SuPaketi(hedef, bas, sure, dalgalar, magaralar, c, yol);
    }

    public static void isle(SuPaketi m, Supplier<NetworkEvent.Context> baglam) {
        SanalSuDunyasi d = SanalSu.ISTEMCI;
        d.temizle();
        d.denizAyarla(m.denizHedef, m.denizBaslangic, m.denizSure);
        m.dalgalar.forEach(d::dalgaEkle);
        m.magaralar.forEach(g -> d.magaraEkle(g.cep(), g.zaman()));
        d.cekilmeAyarla(m.cekilme);
        SanalSu.istemciCekilme = m.cekilme;
        SanalSu.yol = SanalSu.Yol.values()[m.yol];
        baglam.get().setPacketHandled(true);
    }
}
