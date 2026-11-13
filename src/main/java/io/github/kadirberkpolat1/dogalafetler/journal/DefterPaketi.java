package io.github.kadirberkpolat1.dogalafetler.journal;

import io.github.kadirberkpolat1.dogalafetler.basarim.OzelBasarim;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * {@code defter/veri} (S2C): Afet Defteri'nin bütün içeriği tek paketle. Yalnız defter açılırken, sunucu oyuncunun eşyayı
 * kullanmasına ya da {@code /dogalafet defter} komutuna yanıt olarak yollar ({@link #gonder}); istemci paketi alınca ekranı
 * açar. Periyodik yayın yoktur. Boyut: en çok 128 eşya satırı (~35 bayt) + sabit alanlar, ~5 KB altı.
 *
 * @param dunyaGunu      oyun günü (dünya yaşı)
 * @param yasamGunu      bu yaşamda hayatta kalınan oyun günü
 * @param enUzunGun      en uzun yaşamın günü (süren yaşam dahil)
 * @param olum           toplam ölüm (vanilla istatistiği)
 * @param mesafeCm       {@link MesafeGrubu} sırasıyla santimetre
 * @param turler         {@code AfetTuru.values()} sırasıyla afet kayıtları
 * @param enYuksekSeviye atlatılan en yüksek afet seviyesi (yoksa 0)
 * @param enYuksekTur    o afetin {@code AfetTuru} sırası (yoksa -1)
 * @param tsunamiDalga   karşılaşılan tsunami dalgası sayısı
 * @param enSogukC       en düşük vücut sıcaklığı (°C; kayıt yoksa NaN)
 * @param enSicakC       en yüksek vücut sıcaklığı (°C; kayıt yoksa NaN)
 * @param meteoritParca  toplanan Meteorit Parçası
 * @param kozmikCekirdek toplanan Kozmik Çekirdek
 * @param buzDeligi      açılan buz deliği
 * @param buzTutus       buz deliğinden yapılan başarılı çekme
 * @param buzBalik       buz deliğinden tutulan buz balığı
 * @param olta           bütün olta tutuşları (vanilla istatistiği)
 * @param balikToplam    tutulan balık adedi (bütün eşya kayıtlarından)
 * @param esyaToplam     tutulan balık dışı eşya adedi
 * @param son            son afetler, en yenisi başta (en çok {@value DefterVerisi#SON_AFET_SAYISI})
 * @param esyalar        tutulan eşyalar, adede göre azalan
 * @param esikC          vücut sıcaklığı eşikleri (°C): donma hasarı, donma, üşüme, bunalma, yanma, yanma hasarı
 */
public record DefterPaketi(long dunyaGunu, long yasamGunu, long enUzunGun, int olum, long[] mesafeCm,
        List<TurKaydi> turler, int enYuksekSeviye, int enYuksekTur, int tsunamiDalga, float enSogukC, float enSicakC,
        int meteoritParca, int kozmikCekirdek, int buzDeligi, int buzTutus, int buzBalik, int olta, int balikToplam,
        int esyaToplam, List<SonAfet> son, List<EsyaKaydi> esyalar, float[] esikC) {
    public static final String AD = "defter/veri";
    /** Aynı oyuncuya iki paket arasındaki en kısa süre (tick); eşyaya art arda tıklayınca sunucuyu yormasın. */
    public static final int EN_KISA_ARALIK = 10;
    static final int EN_COK_ESYA = 128;
    private static final int OKUMA_SINIRI_TUR = 32;
    private static final int OKUMA_SINIRI_SON = 16;
    private static final int OKUMA_SINIRI_ESYA = 512;
    private static final int SEVIYE_SAYISI = 5;
    private static final int ESIK_SAYISI = 6;
    private static final int MESAFE_SAYISI = MesafeGrubu.values().length;

    /** Bir afet türünün kaydı. {@code seviye} 5 uzunlukta (S1…S5); {@code ilkGun} -1 ise tür hiç yaşanmadı. */
    public record TurKaydi(int atlatilan, int karsilasilan, int olum, int[] seviye, long ilkGun, long ilkTarih) {
    }

    /** Son afet satırı. */
    public record SonAfet(int tur, int seviye, long gun) {
    }

    /** Tutulan bir eşya: kayıt kimliği, adet ve balık mı. */
    public record EsyaKaydi(String kimlik, int adet, boolean balik) {
    }

    /** Mod kurucusunda, koşulsuz ({@code AgKanali} deseni). */
    static void kaydet() {
        AgKanali.kaydet(k -> k.sunucudanIstemciye(AD, DefterPaketi.class, DefterPaketi::yaz, DefterPaketi::oku,
                DefterPaketi::isle));
    }

    /** Son paketin sunucu tick'i; yeniden doğan oyuncu yeni nesnedir, eskisi kendiliğinden düşer. Yalnız ana thread. */
    private static final Map<ServerPlayer, Integer> SON_GONDERIM = new WeakHashMap<>();

    /**
     * Oyuncunun defterini kurup yollar (sunucu ana thread'i). Son gönderimden beri {@value #EN_KISA_ARALIK} tick geçmediyse
     * hiçbir şey gitmez ve {@code false} döner.
     */
    public static boolean gonder(ServerPlayer oyuncu) {
        int simdi = oyuncu.server.getTickCount();
        Integer son = SON_GONDERIM.get(oyuncu);
        if (son != null && simdi - son < EN_KISA_ARALIK) {
            return false;
        }
        SON_GONDERIM.put(oyuncu, simdi);
        AgKanali.oyuncuyaGonder(oyuncu, DefterVerisi.ozet(oyuncu));
        OzelBasarim.ver(oyuncu, OzelBasarim.DEFTER_ILK_ACILIS);
        return true;
    }

    public void yaz(FriendlyByteBuf t) {
        t.writeVarLong(dunyaGunu);
        t.writeVarLong(yasamGunu);
        t.writeVarLong(enUzunGun);
        t.writeVarInt(olum);
        t.writeVarInt(mesafeCm.length);
        for (long cm : mesafeCm) {
            t.writeVarLong(cm);
        }
        t.writeVarInt(turler.size());
        for (TurKaydi tur : turler) {
            t.writeVarInt(tur.atlatilan());
            t.writeVarInt(tur.karsilasilan());
            t.writeVarInt(tur.olum());
            for (int i = 0; i < SEVIYE_SAYISI; i++) {
                t.writeVarInt(i < tur.seviye().length ? tur.seviye()[i] : 0);
            }
            t.writeLong(tur.ilkGun());
            t.writeLong(tur.ilkTarih());
        }
        t.writeVarInt(enYuksekSeviye);
        t.writeVarInt(enYuksekTur);
        t.writeVarInt(tsunamiDalga);
        t.writeFloat(enSogukC);
        t.writeFloat(enSicakC);
        t.writeVarInt(meteoritParca);
        t.writeVarInt(kozmikCekirdek);
        t.writeVarInt(buzDeligi);
        t.writeVarInt(buzTutus);
        t.writeVarInt(buzBalik);
        t.writeVarInt(olta);
        t.writeVarInt(balikToplam);
        t.writeVarInt(esyaToplam);
        t.writeVarInt(son.size());
        for (SonAfet s : son) {
            t.writeVarInt(s.tur());
            t.writeVarInt(s.seviye());
            t.writeVarLong(s.gun());
        }
        t.writeVarInt(esyalar.size());
        for (EsyaKaydi e : esyalar) {
            t.writeUtf(e.kimlik(), 128);
            t.writeVarInt(e.adet());
            t.writeBoolean(e.balik());
        }
        t.writeVarInt(esikC.length);
        for (float f : esikC) {
            t.writeFloat(f);
        }
    }

    public static DefterPaketi oku(FriendlyByteBuf t) {
        long dunyaGunu = t.readVarLong();
        long yasamGunu = t.readVarLong();
        long enUzunGun = t.readVarLong();
        int olum = t.readVarInt();
        long[] mesafe = new long[MESAFE_SAYISI];
        int mesafeN = t.readVarInt();
        for (int i = 0; i < mesafeN; i++) {
            long cm = t.readVarLong();
            if (i < MESAFE_SAYISI) {
                mesafe[i] = cm;
            }
        }
        int turN = Math.min(t.readVarInt(), OKUMA_SINIRI_TUR);
        List<TurKaydi> turler = new ArrayList<>(turN);
        for (int i = 0; i < turN; i++) {
            int atlatilan = t.readVarInt();
            int karsilasilan = t.readVarInt();
            int turOlum = t.readVarInt();
            int[] seviye = new int[SEVIYE_SAYISI];
            for (int s = 0; s < SEVIYE_SAYISI; s++) {
                seviye[s] = t.readVarInt();
            }
            turler.add(new TurKaydi(atlatilan, karsilasilan, turOlum, seviye, t.readLong(), t.readLong()));
        }
        int enYuksekSeviye = t.readVarInt();
        int enYuksekTur = t.readVarInt();
        int tsunamiDalga = t.readVarInt();
        float enSogukC = t.readFloat();
        float enSicakC = t.readFloat();
        int meteoritParca = t.readVarInt();
        int kozmikCekirdek = t.readVarInt();
        int buzDeligi = t.readVarInt();
        int buzTutus = t.readVarInt();
        int buzBalik = t.readVarInt();
        int olta = t.readVarInt();
        int balikToplam = t.readVarInt();
        int esyaToplam = t.readVarInt();
        int sonN = Math.min(t.readVarInt(), OKUMA_SINIRI_SON);
        List<SonAfet> son = new ArrayList<>(sonN);
        for (int i = 0; i < sonN; i++) {
            son.add(new SonAfet(t.readVarInt(), t.readVarInt(), t.readVarLong()));
        }
        int esyaN = Math.min(t.readVarInt(), OKUMA_SINIRI_ESYA);
        List<EsyaKaydi> esyalar = new ArrayList<>(esyaN);
        for (int i = 0; i < esyaN; i++) {
            esyalar.add(new EsyaKaydi(t.readUtf(128), t.readVarInt(), t.readBoolean()));
        }
        float[] esik = new float[ESIK_SAYISI];
        int esikN = t.readVarInt();
        for (int i = 0; i < esikN; i++) {
            float f = t.readFloat();
            if (i < ESIK_SAYISI) {
                esik[i] = f;
            }
        }
        return new DefterPaketi(dunyaGunu, yasamGunu, enUzunGun, olum, mesafe, List.copyOf(turler), enYuksekSeviye,
                enYuksekTur, tsunamiDalga, enSogukC, enSicakC, meteoritParca, kozmikCekirdek, buzDeligi, buzTutus,
                buzBalik, olta, balikToplam, esyaToplam, List.copyOf(son), List.copyOf(esyalar), esik);
    }

    /** İstemci ana thread'inde: ekranı {@code client.journal.IstemciDefter} açar (N15 yolu, ortak kod istemciyi import etmez). */
    public static void isle(DefterPaketi paket, Supplier<NetworkEvent.Context> baglam) {
        Consumer<DefterPaketi> alici = DistExecutor.safeCallWhenOn(Dist.CLIENT,
                () -> io.github.kadirberkpolat1.dogalafetler.client.journal.IstemciDefter::alici);
        if (alici != null) {
            alici.accept(paket);
        }
    }
}
