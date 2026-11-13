package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.EkIsi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.IsiKaynagi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Katki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.KaynakYigilmasi;
import io.github.kadirberkpolat1.dogalafetler.uyum.CuriosKopru;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Oyuncunun ekipmanından gelen ısı/soğuk kalemleri (yalnız sunucu) ve diğer alt sistemlerin kullanacağı kayıt API'si.
 *
 * <p>Yerleşik kaynaklar ({@link EkIsi.Ayarlar} config'i): yünlü zırh parçası ve envanterdeki yün eşyalar
 * ({@link Katki#YUN}), termostatlı zırh ve envanterdeki Termostat Plakası ({@link Katki#TERMOSTAT}; yalnız soğukta).
 *
 * <p>Başka alt sistemlerin kaynakları (ısıtıcı ya da soğutucu), mod kurucusunda (eşya kayıt dilimi) bir kez kaydedilir:
 * <pre>
 * IsiKaynaklari.zirhEkiKaydet(EK, IsiKaynagi.sogutucu("ek", 3));            // giyilen her parça ortamı 3 °C soğutur
 * IsiKaynaklari.esyaKaydet(BUZ_PARCASI::get, IsiKaynagi.sogutucu("buz_parcasi", 2));  // envanterdeki (sol el dahil) her yığın
 * IsiKaynaklari.hedefliSogutucuEsyaKaydet(SERIN_KOLYE::get, ayar::kolyeC);   // ortamı serin hedefe çeker (aşağıda)
 * </pre>
 * Sabit kaynaklar ortamın ne olduğuna bakmadan {@code guc} °C ekler. <b>Hedefe çeken soğutucular</b> (Serin Kolye, Soğutmalı
 * zırh) ise ortam {@link #hedefliSogutucuHedefiAyarla serin hedefin} üstündeyken fark kadar, en çok kapasite kadar soğutur;
 * hedefin altındaki ortama dokunmaz. Bunlar ölçüm anında, oyuncunun ekipmanından hesaplanır: konuma, hareketli kaynağa
 * ya da zamanlamaya bağlı değildir (eski geçici kaynak yöntemi oyuncu yürürken kaynağın yarıçapından çıkıp etkisiz kalıyordu).
 *
 * <p>Birden fazla ısıtıcı (ya da soğutucu) taşındığında toplam, kaynak sayısıyla artar ({@link KaynakYigilmasi}); ısıtıcı ve
 * soğutucu birlikteyse yığılma paylarını kısmen götürürler. Toplamlar {@code EkIsi.Ayarlar#kisiselUstSiniri}/
 * {@code kisiselAltSiniri} ile sınırlıdır; ısıtıcılar {@link Katki#ISITICI_ESYA}, soğutucular {@link Katki#SOGUTUCU_ESYA}
 * satırı olarak (kaynak sayısıyla) dökümde görünür.
 */
public final class IsiKaynaklari {
    /** Hedefe çeken soğutucu kaydedilmediyse kullanılan serin hedef (°C). */
    private static final double VARSAYILAN_HEDEF_C = 20.0;

    private record EsyaKaydi(Supplier<? extends Item> esya, IsiKaynagi kaynak) {
    }

    private record EkKaydi(ZirhEkleri.Ek ek, IsiKaynagi kaynak) {
    }

    private record HedefliEsyaKaydi(Supplier<? extends Item> esya, DoubleSupplier kapasiteC) {
    }

    private record HedefliEkKaydi(ZirhEkleri.Ek ek, DoubleSupplier kapasiteC) {
    }

    private static final List<EsyaKaydi> ESYALAR = new CopyOnWriteArrayList<>();
    private static final List<EkKaydi> EKLER = new CopyOnWriteArrayList<>();
    private static final List<HedefliEsyaKaydi> HEDEFLI_ESYALAR = new CopyOnWriteArrayList<>();
    private static final List<HedefliEkKaydi> HEDEFLI_EKLER = new CopyOnWriteArrayList<>();
    private static volatile DoubleSupplier hedefliHedefC = () -> VARSAYILAN_HEDEF_C;

    private IsiKaynaklari() {
    }

    /**
     * Envanterde (ana envanter ve sol el) taşınan her {@code esya} yığını ortama {@code kaynak.guc()} °C ekler (yığın
     * adedi sayılmaz). Eşya tembel çözülür, kayıt zamanı eşya kaydı hazır olmak zorunda değildir.
     */
    public static void esyaKaydet(Supplier<? extends Item> esya, IsiKaynagi kaynak) {
        ESYALAR.add(new EsyaKaydi(Objects.requireNonNull(esya, "esya"), Objects.requireNonNull(kaynak, "kaynak")));
    }

    /** Giyilen her zırh parçasındaki {@code ek} ortama {@code kaynak.guc()} °C ekler ({@link ZirhEkleri#kaydet} ile kayıtlı ek). */
    public static void zirhEkiKaydet(ZirhEkleri.Ek ek, IsiKaynagi kaynak) {
        Objects.requireNonNull(ek, "ek");
        Objects.requireNonNull(kaynak, "kaynak");
        for (EkKaydi var : EKLER) {
            if (var.ek().anahtar().equals(ek.anahtar())) {
                throw new IllegalStateException("Aynı zırh eki için ısı kaynağı iki kez kaydedildi: " + ek.anahtar());
            }
        }
        EKLER.add(new EkKaydi(ek, kaynak));
    }

    /**
     * Envanterde (ana envanter ve sol el) taşınan her {@code esya} yığını ortamı serin hedefe çeken bir soğutucu olur:
     * ortam hedefin üstündeyse fark kadar, en çok {@code kapasiteC} (°C; yığılmayla artar) soğutur. Kapasite her ölçümde
     * çağrılır (config değeri); 0 ya da eksi kapasite kaynağı yok sayar.
     */
    public static void hedefliSogutucuEsyaKaydet(Supplier<? extends Item> esya, DoubleSupplier kapasiteC) {
        HEDEFLI_ESYALAR.add(new HedefliEsyaKaydi(Objects.requireNonNull(esya, "esya"),
                Objects.requireNonNull(kapasiteC, "kapasiteC")));
    }

    /** Giyilen her zırh parçasındaki {@code ek}, ortamı serin hedefe çeken bir soğutucudur ({@link #hedefliSogutucuEsyaKaydet}). */
    public static void hedefliSogutucuEkiKaydet(ZirhEkleri.Ek ek, DoubleSupplier kapasiteC) {
        Objects.requireNonNull(ek, "ek");
        Objects.requireNonNull(kapasiteC, "kapasiteC");
        for (HedefliEkKaydi var : HEDEFLI_EKLER) {
            if (var.ek().anahtar().equals(ek.anahtar())) {
                throw new IllegalStateException("Aynı zırh eki için hedefe çeken soğutucu iki kez kaydedildi: " + ek.anahtar());
            }
        }
        HEDEFLI_EKLER.add(new HedefliEkKaydi(ek, kapasiteC));
    }

    /** Hedefe çeken bütün soğutucuların çektiği serin hedef (°C); her ölçümde çağrılır (config değeri). */
    public static void hedefliSogutucuHedefiAyarla(DoubleSupplier hedefC) {
        hedefliHedefC = Objects.requireNonNull(hedefC, "hedefC");
    }

    /**
     * Oyuncunun zırh ve envanterinden ek ısı kalemlerini ({@link Katki#YUN}, {@link Katki#TERMOSTAT},
     * {@link Katki#ISITICI_ESYA}, {@link Katki#SOGUTUCU_ESYA}) sıfır olmayanları {@code katkilar}'a, ısıtıcı/soğutucu
     * kaynak sayılarını {@code adetler}'e yazar.
     *
     * @param mekanC   ek kalemler eklenmeden önceki ortam sıcaklığı (termostat yalnız soğukta ısıtır)
     * @param digerEkC bu çağrıdan önce hesaplanmış ek kalemlerin (yün blokları, lavın içi, yakın oyuncular) °C toplamı; hedefe
     *                 çeken soğutucu bunlar da dahil ortama bakar
     */
    static void olc(ServerPlayer oyuncu, EkIsi.Ayarlar a, double mekanC, double digerEkC, Map<Katki, Double> katkilar,
            Map<Katki, Integer> adetler) {
        int yunluParca = 0;
        int termostatliParca = 0;
        double isitici = 0;
        int isiSayisi = 0;
        double sogutucu = 0;
        int soguSayisi = 0;
        double hedefliKapasite = 0;
        int hedefliSayisi = 0;
        ZirhEkleri.Ek termostatEki = ZirhEkleri.termostatEki();
        for (EquipmentSlot yuva : EquipmentSlot.values()) {
            if (yuva.getType() != EquipmentSlot.Type.ARMOR) {
                continue;
            }
            ItemStack yigin = oyuncu.getItemBySlot(yuva);
            if (!ZirhEkleri.zirhMi(yigin)) {
                continue;
            }
            if (ZirhEkleri.varMi(yigin, ZirhEkleri.YUNLU)) {
                yunluParca++;
            }
            if (termostatEki != null && ZirhEkleri.varMi(yigin, termostatEki)) {
                termostatliParca++;
            }
            for (EkKaydi kayit : EKLER) {
                if (ZirhEkleri.varMi(yigin, kayit.ek())) {
                    double guc = kayit.kaynak().guc();
                    if (guc > 0) {
                        isitici += guc;
                        isiSayisi++;
                    } else {
                        sogutucu += guc;
                        soguSayisi++;
                    }
                }
            }
            for (HedefliEkKaydi kayit : HEDEFLI_EKLER) {
                double kapasite = kayit.kapasiteC().getAsDouble();
                if (kapasite > 0 && ZirhEkleri.varMi(yigin, kayit.ek())) {
                    hedefliKapasite += kapasite;
                    hedefliSayisi++;
                }
            }
        }
        int yunEsyasi = 0;
        int plaka = 0;
        Item plakaEsyasi = ForgeRegistries.ITEMS.getValue(DogalAfetler.konum("termostat_plakasi"));
        Inventory envanter = oyuncu.getInventory();
        // Curios yuvalarındaki yığınlar (Curios yoksa boş liste) envanter yığınlarıyla aynı sayılır (K-E74).
        for (List<ItemStack> liste : List.of(envanter.items, envanter.offhand, CuriosKopru.takiliYiginlar(oyuncu))) {
            for (ItemStack yigin : liste) {
                if (yigin.isEmpty()) {
                    continue;
                }
                if (yigin.is(ItemTags.WOOL)) {
                    yunEsyasi++;
                }
                if (plakaEsyasi != null && yigin.is(plakaEsyasi)) {
                    plaka++;
                }
                for (EsyaKaydi kayit : ESYALAR) {
                    if (yigin.is(kayit.esya().get())) {
                        double guc = kayit.kaynak().guc();
                        if (guc > 0) {
                            isitici += guc;
                            isiSayisi++;
                        } else {
                            sogutucu += guc;
                            soguSayisi++;
                        }
                    }
                }
                for (HedefliEsyaKaydi kayit : HEDEFLI_ESYALAR) {
                    double kapasite = kayit.kapasiteC().getAsDouble();
                    if (kapasite > 0 && yigin.is(kayit.esya().get())) {
                        hedefliKapasite += kapasite;
                        hedefliSayisi++;
                    }
                }
            }
        }
        double yun = EkIsi.yun(yunluParca, yunEsyasi, a);
        double termostat = EkIsi.termostat(termostatliParca, plaka, mekanC, a);
        yaz(katkilar, Katki.YUN, yun);
        yaz(katkilar, Katki.TERMOSTAT, termostat);
        EkIsi.Kayitli kayitli = EkIsi.kayitliKaynaklar(isitici, isiSayisi, sogutucu, soguSayisi, hedefliKapasite,
                hedefliSayisi, mekanC + digerEkC + yun + termostat, hedefliHedefC.getAsDouble(), a);
        yaz(katkilar, Katki.ISITICI_ESYA, kayitli.isitici());
        yaz(katkilar, Katki.SOGUTUCU_ESYA, kayitli.sogutucu());
        if (kayitli.isitici() != 0) {
            adetler.put(Katki.ISITICI_ESYA, kayitli.isiAdedi());
        }
        if (kayitli.sogutucu() != 0) {
            adetler.put(Katki.SOGUTUCU_ESYA, kayitli.soguAdedi());
        }
    }

    private static void yaz(Map<Katki, Double> sonuc, Katki kalem, double deger) {
        if (deger != 0) {
            sonuc.put(kalem, deger);
        }
    }
}
