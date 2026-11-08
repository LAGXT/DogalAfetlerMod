package io.github.kadirberkpolat1.dogalafetler.esya.radar;

import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.RadarGorunumu;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * {@code esya/radar}: radarı elinde tutan oyuncuya giden gösterge durumu (madde 3, 13a). Durum değişince (en çok
 * {@code esya.radar.paketAraligiTick}, varsayılan 1 Hz) yalnız o oyuncuya; radar bırakılınca tek {@link #YOK}. Merkez
 * blok koordinatıdır; yön ve mesafeyi istemci her karede oyuncunun konumu ve bakışıyla hesaplar. Her yerde süren bölgesel
 * afette (asit yağmuru, AF2-06) merkez {@link #HER_YER} işaretidir: gösterge mesafe yerine "her yer" yazar (tel biçimi aynı).
 * Normal radarda seviye, hareket ve geçmiş hiç gönderilmez (madde 13a: "normal radar afet seviyesini göstermesin").
 * ~30 bayt, geçmişle ~45.
 *
 * @param elde        radar elde mi; false ise diğer alanlar anlamsız ({@link #YOK})
 * @param menzil      radarın menzili (blok; gösterge ölçeği)
 * @param kalanSn     aşamanın kalanı gerçek saniye (-1 bilinmiyor): ON_UYARI/SIREN'de varış, AKTIF'te bitiş
 * @param hareketYonu hareketli afetin pusula açısı; durağansa ya da normal radarda {@code NaN}
 * @param hiz         blok/sn
 */
public record RadarPaketi(boolean elde, boolean gelismis, int menzil, RadarGorunumu gorunum, AfetTuru tur,
        AfetAsamasi asama, int merkezX, int merkezZ, int seviye, long kalanSn, float hareketYonu, float hiz,
        List<Gecmis> gecmis) {
    public static final String AD = "esya/radar";
    public static final int GECMIS_SINIRI = 5;
    /** {@code merkezX}/{@code merkezZ} bu değerse afet her yerdedir (küresel etki alanı, gökyüzü anomalisi değil). */
    public static final int HER_YER = Integer.MIN_VALUE;
    public static final RadarPaketi YOK = new RadarPaketi(false, false, 0, RadarGorunumu.GIZLI, AfetTuru.KASIRGA,
            AfetAsamasi.BITIS, 0, 0, 0, -1, Float.NaN, 0, List.of());

    /** Geçmiş satırı: tür, seviye (seviyesizde 0), bittiğinden beri geçen gerçek dakika. */
    public record Gecmis(AfetTuru tur, int seviye, int yasDakika) {
    }

    public RadarPaketi {
        gecmis = List.copyOf(gecmis.size() > GECMIS_SINIRI ? gecmis.subList(0, GECMIS_SINIRI) : gecmis);
    }

    /** Afet her yerde mi (AF2-06): gösterge yön oku ve mesafe yerine "her yer" gösterir. */
    public boolean herYer() {
        return merkezX == HER_YER && merkezZ == HER_YER;
    }

    /** Radar elde ama gösterilecek afet yok: "gökyüzü sakin". */
    public static RadarPaketi sakin(boolean gelismis, int menzil, List<Gecmis> gecmis) {
        return new RadarPaketi(true, gelismis, menzil, RadarGorunumu.GIZLI, AfetTuru.KASIRGA, AfetAsamasi.BITIS, 0, 0, 0, -1,
                Float.NaN, 0, gelismis ? gecmis : List.of());
    }

    public static void kaydet() {
        AgKanali.kaydet(k -> k.sunucudanIstemciye(AD, RadarPaketi.class, RadarPaketi::yaz, RadarPaketi::oku,
                RadarPaketi::isle));
    }

    public void yaz(FriendlyByteBuf t) {
        t.writeBoolean(elde);
        if (!elde) {
            return;
        }
        t.writeBoolean(gelismis);
        t.writeVarInt(menzil);
        t.writeEnum(gorunum);
        if (gorunum != RadarGorunumu.GIZLI) {
            t.writeEnum(tur);
            t.writeEnum(asama);
            t.writeVarInt(merkezX);
            t.writeVarInt(merkezZ);
            t.writeVarInt(seviye);
            t.writeVarLong(kalanSn + 1);
            t.writeFloat(hareketYonu);
            t.writeFloat(hiz);
        }
        t.writeVarInt(gecmis.size());
        for (Gecmis g : gecmis) {
            t.writeEnum(g.tur());
            t.writeVarInt(g.seviye());
            t.writeVarInt(g.yasDakika());
        }
    }

    public static RadarPaketi oku(FriendlyByteBuf t) {
        if (!t.readBoolean()) {
            return YOK;
        }
        boolean gelismis = t.readBoolean();
        int menzil = t.readVarInt();
        RadarGorunumu gorunum = t.readEnum(RadarGorunumu.class);
        AfetTuru tur = AfetTuru.KASIRGA;
        AfetAsamasi asama = AfetAsamasi.BITIS;
        int x = 0;
        int z = 0;
        int seviye = 0;
        long kalan = -1;
        float yon = Float.NaN;
        float hiz = 0;
        if (gorunum != RadarGorunumu.GIZLI) {
            tur = t.readEnum(AfetTuru.class);
            asama = t.readEnum(AfetAsamasi.class);
            x = t.readVarInt();
            z = t.readVarInt();
            seviye = t.readVarInt();
            kalan = t.readVarLong() - 1;
            yon = t.readFloat();
            hiz = t.readFloat();
        }
        int adet = Math.min(t.readVarInt(), GECMIS_SINIRI);
        List<Gecmis> gecmis = new ArrayList<>(adet);
        for (int i = 0; i < adet; i++) {
            gecmis.add(new Gecmis(t.readEnum(AfetTuru.class), t.readVarInt(), t.readVarInt()));
        }
        return new RadarPaketi(true, gelismis, menzil, gorunum, tur, asama, x, z, seviye, kalan, yon, hiz, gecmis);
    }

    /** İstemci ana thread'inde: {@code client.esya.radar.IstemciRadar}'a N15 yoluyla (K-C14). */
    public static void isle(RadarPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        Consumer<RadarPaketi> alici = DistExecutor.safeCallWhenOn(Dist.CLIENT,
                () -> io.github.kadirberkpolat1.dogalafetler.client.esya.radar.IstemciRadar::alici);
        if (alici != null) {
            alici.accept(paket);
        }
    }
}
