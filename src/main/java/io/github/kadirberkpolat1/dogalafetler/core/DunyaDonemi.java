package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.BeklemeDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.CarpanListesi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.GeciciCarpan;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.OyunGunu;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonumTipi;
import io.github.kadirberkpolat1.dogalafetler.core.olay.DonemDegistiEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.MinecraftForge;

/**
 * Dünya dönemi: tek SavedData ({@value #VERI_ADI}, üst dünyanın veri deposunda, boyuttan bağımsız; GOREV §4, madde 3, 9,
 * 11, 16). Gökyüzü (ay, güneş evresi), donma, buharlaşma, deniz seviyesi, geçici çarpanlar, aktif afet, son afetler geçmişi,
 * bekleme sayaçları, oyun günü ofseti ve uyku birikimi, dönem süreçlerinin durdurma kayıtları ve ölçekli afet saati burada
 * durur. Her alanın okuyucusu vardır; dönem alanlarını sahibi olan alt sistem setter'larla değiştirir: setter
 * {@code setDirty} yapar, {@link DonemDegistiEvent} yayınlar ve istemci senkronunu işaretler. Gamerule kapatılınca
 * ulaşılmış dönemler geri alınmaz (madde 11); yalnız {@link #sifirla()} ilk hâle döndürür. Yalnız sunucu ana thread'inde
 * kullanılır.
 */
public final class DunyaDonemi extends SavedData {
    public static final String VERI_ADI = "dogalafetler_donem";
    public static final int BICIM_SURUMU = 1;
    /** Gelişmiş Hava Radarı'nın gösterdiği son afet sayısı (madde 13a). */
    public static final int GECMIS_SINIRI = 5;
    public static final float DONMA_EN_COK = 100f;
    public static final float BUHARLASMA_EN_COK = 64f;

    private boolean ayVar = true;
    private long ayOlaySaati = AyDurumu.OLAY_YOK;
    private GunesEvresi gunesEvresi = GunesEvresi.NORMAL;
    private float evreIlerlemesi;
    private long evreBaslangicSaati;
    private float donmaIlerlemesi;
    private boolean cozulmeDonemi;
    private float buharlasmaDerinligi;
    private DenizSeviyesi deniz = DenizSeviyesi.VARSAYILAN;
    private final CarpanListesi carpanlar = new CarpanListesi();
    private final BeklemeDurumu bekleme = new BeklemeDurumu();
    private double uygulananSiklik = 1.0;
    private final Deque<GecmisKaydi> sonAfetler = new ArrayDeque<>();
    private UUID sonHedef;
    private int sonSeviye;
    private long gunOfseti;
    private long uykuBirikimi;
    private long saat;
    private final CompoundTag ekVeri = new CompoundTag();
    private final Map<String, DonemSureci.DurdurmaNedeni> surecDurdurmalari = new TreeMap<>();
    private boolean kayittanOkundu;

    private Afet canliAfet;
    private CompoundTag bekleyenAfet;

    private boolean senkronGerekli = true;
    private boolean acilSenkron = true;

    public DunyaDonemi() {
    }

    /** Sunucunun dünya dönemi (yoksa oluşturulur). */
    public static DunyaDonemi al(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DunyaDonemi::yukle, DunyaDonemi::new, VERI_ADI);
    }

    public static DunyaDonemi al(ServerLevel level) {
        return al(level.getServer());
    }

    // ---------------------------------------------------------------- gökyüzü

    public boolean ayVar() {
        return ayVar;
    }

    /** Elle ay var/yok (test, geliştirme komutu): olay saati silinir, yaklaşma ya da enkaz halkası çizilmez. */
    public void ayVarAyarla(boolean var) {
        if (ayVar != var || ayOlaySaati != AyDurumu.OLAY_YOK) {
            ayVar = var;
            ayOlaySaati = AyDurumu.OLAY_YOK;
            degisti(DonemAlani.AY, true);
        }
    }

    public AyDurumu ayDurumu() {
        return AyDurumu.den(ayVar, ayOlaySaati);
    }

    /** YAKLASIYOR'da yaklaşmanın, YOK'ta çarpmanın ölçekli afet saati; yoksa {@link AyDurumu#OLAY_YOK}. */
    public long ayOlaySaati() {
        return ayOlaySaati;
    }

    /**
     * Ay çarpması (sahibi): {@code YAKLASIYOR} yaklaşmanın başladığı, {@code YOK} çarpmanın saatiyle; {@code VAR} olay
     * saatini siler (siren sırasında durdurulan yaklaşma). Değişim {@link DonemAlani#AY} olayı ve acil senkrondur.
     */
    public void ayDurumuAyarla(AyDurumu durum, long olaySaati) {
        boolean var = durum != AyDurumu.YOK;
        long yeniSaat = durum == AyDurumu.VAR ? AyDurumu.OLAY_YOK : Math.max(0L, olaySaati);
        if (ayVar != var || ayOlaySaati != yeniSaat) {
            ayVar = var;
            ayOlaySaati = yeniSaat;
            degisti(DonemAlani.AY, true);
        }
    }

    public GunesEvresi gunesEvresi() {
        return gunesEvresi;
    }

    /** Evreyi değiştirir; evre ilerlemesi 0'a, evre başlangıcı şimdiki afet saatine çekilir. */
    public void gunesEvresiAyarla(GunesEvresi evre) {
        if (gunesEvresi != evre) {
            gunesEvresi = evre;
            evreIlerlemesi = 0f;
            evreBaslangicSaati = saat;
            degisti(DonemAlani.GUNES_EVRESI, true);
        }
    }

    /** Geçerli evrenin ilerlemesi (0–1). */
    public float evreIlerlemesi() {
        return evreIlerlemesi;
    }

    public void evreIlerlemesiAyarla(float ilerleme) {
        float yeni = sinirla(ilerleme, 0f, 1f);
        if (yeni != evreIlerlemesi) {
            evreIlerlemesi = yeni;
            degisti(DonemAlani.EVRE_ILERLEMESI, false);
        }
    }

    /** Geçerli evrenin başladığı ölçekli afet saati (istemci geçişleri aynı andan hesaplar). */
    public long evreBaslangicSaati() {
        return evreBaslangicSaati;
    }

    // ---------------------------------------------------------------- donma

    /** Global donma ilerlemesi G (0–100, madde 9). */
    public float donmaIlerlemesi() {
        return donmaIlerlemesi;
    }

    public void donmaIlerlemesiAyarla(float ilerleme) {
        float yeni = sinirla(ilerleme, 0f, DONMA_EN_COK);
        if (yeni != donmaIlerlemesi) {
            donmaIlerlemesi = yeni;
            degisti(DonemAlani.DONMA, false);
        }
    }

    /** Kızıl cüce çözülme dönemi mi (madde 9 ÇÖZÜLME). */
    public boolean cozulmeDonemi() {
        return cozulmeDonemi;
    }

    public void cozulmeDonemiAyarla(boolean cozulme) {
        if (cozulmeDonemi != cozulme) {
            cozulmeDonemi = cozulme;
            degisti(DonemAlani.COZULME, true);
        }
    }

    // ---------------------------------------------------------------- su

    /** Güneş patlamasındaki buharlaşma derinliği (blok, N9). */
    public float buharlasmaDerinligi() {
        return buharlasmaDerinligi;
    }

    public void buharlasmaDerinligiAyarla(float derinlik) {
        float yeni = sinirla(derinlik, 0f, BUHARLASMA_EN_COK);
        if (yeni != buharlasmaDerinligi) {
            buharlasmaDerinligi = yeni;
            degisti(DonemAlani.BUHARLASMA, false);
        }
    }

    public DenizSeviyesi deniz() {
        return deniz;
    }

    public void denizAyarla(DenizSeviyesi yeni) {
        if (!deniz.equals(yeni)) {
            boolean acil = deniz.kalici() != yeni.kalici() || deniz.hedefArtis() != yeni.hedefArtis();
            deniz = yeni;
            degisti(DonemAlani.DENIZ, acil);
        }
    }

    // ---------------------------------------------------------------- geçici çarpanlar (madde 4)

    /**
     * Bir afetin olasılığını geçici olarak çarpar: şimdiden {@code sabitSure} boyunca {@code tepe}, sonra {@code sonumSuresi}
     * boyunca 1'e döner (ölçekli tick). Bekleme ve tek afet kuralı bozulmaz.
     */
    public void carpanEkle(AfetTuru tur, double tepe, long sabitSure, long sonumSuresi, SonumTipi tip) {
        carpanlar.ekle(new GeciciCarpan(tur, tepe, saat, sabitSure, sonumSuresi, tip));
        degisti(DonemAlani.CARPANLAR, false);
    }

    /** Türün şimdiki geçici çarpanı (yoksa 1). */
    public double carpan(AfetTuru tur) {
        return carpanlar.carpan(tur, saat);
    }

    public List<GeciciCarpan> carpanlar() {
        return carpanlar.liste();
    }

    void bitmisCarpanlariTemizle() {
        if (carpanlar.temizle(saat)) {
            degisti(DonemAlani.CARPANLAR, false);
        }
    }

    // ---------------------------------------------------------------- bekleme ve geçmiş

    public long genelBeklemeKalan() {
        return bekleme.genelKalan(saat);
    }

    public long turBeklemeKalan(AfetTuru tur) {
        return bekleme.turKalan(tur, saat);
    }

    BeklemeDurumu bekleme() {
        return bekleme;
    }

    /** Süren beklemelerin hesaplandığı {@code afetSikligi} çarpanı (kayıtta yoksa 1: eski kayıtlar ölçeksizdir). */
    public double uygulananSiklik() {
        return uygulananSiklik;
    }

    /**
     * {@code afetSikligi} çarpanı değiştiyse süren beklemeleri yeni sıklığa ölçekler ve çarpanı kaydeder. 0 (seçim yok)
     * kaydedilmez: sıklık yeniden açılınca son pozitif değerden ölçeklenir. Çarpan değiştiyse true.
     */
    public boolean siklikUygula(double siklik) {
        if (!(siklik > 0) || Double.isInfinite(siklik) || siklik == uygulananSiklik) {
            return false;
        }
        bekleme.siklikDegisti(saat, uygulananSiklik, siklik);
        uygulananSiklik = siklik;
        setDirty();
        return true;
    }

    /** Genel ve afet başına beklemeleri sıfırlar. */
    public void beklemeleriSifirla() {
        bekleme.sifirla();
        setDirty();
    }

    /** Son afetler, en yenisi başta (en çok {@value #GECMIS_SINIRI}). */
    public List<GecmisKaydi> sonAfetler() {
        return List.copyOf(sonAfetler);
    }

    void gecmiseEkle(GecmisKaydi kayit) {
        sonAfetler.addFirst(kayit);
        while (sonAfetler.size() > GECMIS_SINIRI) {
            sonAfetler.removeLast();
        }
        setDirty();
    }

    /** Son afetin hedef oyuncusu (aynı oyuncu art arda hedef alınmaz). */
    public Optional<UUID> sonHedef() {
        return Optional.ofNullable(sonHedef);
    }

    void sonHedefAyarla(UUID hedef) {
        sonHedef = hedef;
        setDirty();
    }

    /** Son seviyeli afetin seviyesi; yoksa 0 (art arda 4-5 kuralı). */
    public int sonSeviye() {
        return sonSeviye;
    }

    void sonSeviyeAyarla(int seviye) {
        sonSeviye = seviye;
        setDirty();
    }

    // ---------------------------------------------------------------- gün ve saat

    /**
     * {@code /dogalafet gun} ofseti: oyun günü = (oyun zamanı + uyku birikimi) / 24000 + ofset. Ofset "istenen gün − o anki
     * gün"dür; dünya saatine ({@code getDayTime()}) değil, oyun zamanına ve uyku birikimine göredir (K-C29, K-C42).
     */
    public long gunOfseti() {
        return gunOfseti;
    }

    public void gunOfsetiAyarla(long ofset) {
        if (gunOfseti != ofset) {
            gunOfseti = ofset;
            degisti(DonemAlani.GUN, false);
        }
    }

    /** Uykunun atladığı gün saatlerinin toplamı (tick, K-C42); oyun gününe eklenir. */
    public long uykuBirikimi() {
        return uykuBirikimi;
    }

    /** Uyku bitti: atlanan gün saatini biriktirir (yalnız pozitif). */
    void uykuEkle(long atlanan) {
        if (atlanan > 0) {
            uykuBirikimi += atlanan;
            degisti(DonemAlani.GUN, false);
        }
    }

    /**
     * Oyun günü (dünya yaşı, madde 10). {@code oyunZamani} üst dünyanın {@code getGameTime()}'ıdır: {@code /time set} onu
     * değiştirmez; uykunun atladığı süre {@link #uykuBirikimi()} ile eklenir ({@link OyunGunu}).
     */
    public long oyunGunu(long oyunZamani) {
        return OyunGunu.gun(oyunZamani, uykuBirikimi, gunOfseti);
    }

    /** Ölçekli afet saati: afet ve dönem zamanlayıcılarının ortak saati ({@code /dogalafet hiz} ile hızlanır). */
    public long saat() {
        return saat;
    }

    void saatIlerlet(int adim) {
        if (adim > 0) {
            saat += adim;
            setDirty();
        }
    }

    // ---------------------------------------------------------------- aktif afet

    /** Süren afet (ON_UYARI dahil). */
    public Optional<Afet> aktifAfet() {
        return Optional.ofNullable(canliAfet);
    }

    void aktifAfetAyarla(Afet afet) {
        canliAfet = afet;
        bekleyenAfet = null;
        setDirty();
    }

    /** Kayıttan okunmuş ama henüz yöneticiye verilmemiş aktif afet NBT'sini alır ve bırakır. */
    CompoundTag bekleyenAfetiAl() {
        CompoundTag sonuc = bekleyenAfet;
        bekleyenAfet = null;
        return sonuc;
    }

    // ---------------------------------------------------------------- alt sistem ek verisi

    /**
     * Alt sistemin dönemle birlikte saklanan kendi verisi (ör. {@code donma} kış maskesi parametreleri). Dönen etiket
     * değiştirilebilir; değişiklikten sonra {@link #ekVeriDegisti} çağrılır. İstemciye gitmez. Aynı alt sistem için
     * dönen nesne {@link #sifirla()} sonrasında da aynıdır (içi boşaltılır); yine de küçük tutulmalıdır (büyük veri chunk
     * capability'de).
     */
    public CompoundTag ekVeri(String altSistem) {
        if (!ekVeri.contains(altSistem, Tag.TAG_COMPOUND)) {
            ekVeri.put(altSistem, new CompoundTag());
        }
        return ekVeri.getCompound(altSistem);
    }

    public void ekVeriDegisti(String altSistem) {
        setDirty();
        MinecraftForge.EVENT_BUS.post(new DonemDegistiEvent(this, DonemAlani.EK_VERI));
    }

    // ---------------------------------------------------------------- dönem süreçlerinin durdurma kayıtları (K-C41)

    /** Sürecin durdurma kaydı (kimliğe göre); yoksa süreç durdurulmamıştır. */
    public Optional<DonemSureci.DurdurmaNedeni> surecDurdurmasi(String kimlik) {
        return Optional.ofNullable(surecDurdurmalari.get(kimlik));
    }

    /** Durdurma kaydını yazar; {@code neden == null} kaydı siler. */
    void surecDurdurmasiAyarla(String kimlik, DonemSureci.DurdurmaNedeni neden) {
        DonemSureci.DurdurmaNedeni eski = neden == null ? surecDurdurmalari.remove(kimlik)
                : surecDurdurmalari.put(kimlik, neden);
        if (eski != neden) {
            setDirty();
        }
    }

    /** Bütün durdurma kayıtlarının salt okunur kopyası (kimlik → neden). */
    public Map<String, DonemSureci.DurdurmaNedeni> surecDurdurmalari() {
        return Map.copyOf(surecDurdurmalari);
    }

    // ---------------------------------------------------------------- sıfırlama ve senkron

    /**
     * {@code /dogalafet sifirla}: ay, güneş, donma, buharlaşma, deniz, çarpanlar, beklemeler, geçmiş, son hedef, gün ofseti,
     * süreç durdurma kayıtları ve ek veri ilk hâline döner. Uyku birikimi korunur: yaşanmış gecelerin payıdır, dünya
     * yaşının parçasıdır (K-C42 rev., K-C46). Afet saati monoton kalır; aktif afeti ve dönem süreçlerini önce yönetici
     * durdurur.
     */
    public void sifirla() {
        ayVar = true;
        ayOlaySaati = AyDurumu.OLAY_YOK;
        gunesEvresi = GunesEvresi.NORMAL;
        evreIlerlemesi = 0f;
        evreBaslangicSaati = saat;
        donmaIlerlemesi = 0f;
        cozulmeDonemi = false;
        buharlasmaDerinligi = 0f;
        deniz = DenizSeviyesi.VARSAYILAN;
        carpanlar.sifirla();
        bekleme.sifirla();
        sonAfetler.clear();
        sonHedef = null;
        sonSeviye = 0;
        gunOfseti = 0;
        surecDurdurmalari.clear();
        for (String anahtar : List.copyOf(ekVeri.getAllKeys())) {
            if (ekVeri.get(anahtar) instanceof CompoundTag altSistem) {
                for (String alan : List.copyOf(altSistem.getAllKeys())) {
                    altSistem.remove(alan);
                }
            } else {
                ekVeri.remove(anahtar);
            }
        }
        degisti(DonemAlani.SIFIRLAMA, true);
    }

    /** Bu nesne kayıttan mı okundu (yoksa yeni dünya). */
    boolean kayittanOkundu() {
        return kayittanOkundu;
    }

    boolean senkronGerekli() {
        return senkronGerekli;
    }

    boolean acilSenkron() {
        return acilSenkron;
    }

    void senkronIste(boolean acil) {
        senkronGerekli = true;
        acilSenkron |= acil;
    }

    void senkronTamam() {
        senkronGerekli = false;
        acilSenkron = false;
    }

    private void degisti(DonemAlani alan, boolean acil) {
        setDirty();
        senkronIste(acil);
        MinecraftForge.EVENT_BUS.post(new DonemDegistiEvent(this, alan));
    }

    private static float sinirla(float deger, float enAz, float enCok) {
        return Float.isNaN(deger) ? enAz : Math.max(enAz, Math.min(enCok, deger));
    }

    /** Log ve komut için tek satırlık özet. */
    public String ozet() {
        return String.format(Locale.ROOT,
                "saat=%d gunOfseti=%d uyku=%d ay=%s gunes=%s ilerleme=%.3f donma=%.2f cozulme=%s buharlasma=%.2f "
                        + "deniz=%.2f/%.2f kalici=%s carpan=%d gecmis=%d genelBekleme=%d sonSeviye=%d aktifAfet=%s "
                        + "durdurulanSurec=%d ayOlay=%d siklik=%.2f",
                saat, gunOfseti, uykuBirikimi, ayVar, gunesEvresi.kimlik(), evreIlerlemesi, donmaIlerlemesi, cozulmeDonemi,
                buharlasmaDerinligi, deniz.mevcutArtis(), deniz.hedefArtis(), deniz.kalici(), carpanlar.liste().size(),
                sonAfetler.size(), bekleme.genelKalan(saat), sonSeviye,
                canliAfet != null ? canliAfet.tur().kimlik() + "/" + canliAfet.asama().kimlik()
                        : bekleyenAfet != null ? bekleyenAfet.getString("tur") + "/bekliyor" : "yok",
                surecDurdurmalari.size(), ayOlaySaati, uygulananSiklik);
    }

    // ---------------------------------------------------------------- NBT

    @Override
    public CompoundTag save(CompoundTag etiket) {
        etiket.putInt("surum", BICIM_SURUMU);
        etiket.putBoolean("ayVar", ayVar);
        etiket.putLong("ayOlay", ayOlaySaati);
        etiket.putString("gunesEvresi", gunesEvresi.kimlik());
        etiket.putFloat("evreIlerlemesi", evreIlerlemesi);
        etiket.putLong("evreBaslangic", evreBaslangicSaati);
        etiket.putFloat("donma", donmaIlerlemesi);
        etiket.putBoolean("cozulme", cozulmeDonemi);
        etiket.putFloat("buharlasma", buharlasmaDerinligi);
        etiket.put("deniz", deniz.yaz());
        ListTag carpanListesi = new ListTag();
        for (GeciciCarpan carpan : carpanlar.liste()) {
            CompoundTag c = new CompoundTag();
            c.putString("tur", carpan.tur().kimlik());
            c.putDouble("tepe", carpan.tepe());
            c.putLong("baslangic", carpan.baslangic());
            c.putLong("sabit", carpan.sabitSure());
            c.putLong("sonum", carpan.sonumSuresi());
            c.putString("tip", carpan.tip().name());
            carpanListesi.add(c);
        }
        etiket.put("carpanlar", carpanListesi);
        CompoundTag beklemeEtiketi = new CompoundTag();
        beklemeEtiketi.putLong("genel", bekleme.genelBitis());
        CompoundTag turler = new CompoundTag();
        for (Map.Entry<AfetTuru, Long> girdi : bekleme.turBitisleri().entrySet()) {
            turler.putLong(girdi.getKey().kimlik(), girdi.getValue());
        }
        beklemeEtiketi.put("turler", turler);
        etiket.put("bekleme", beklemeEtiketi);
        etiket.putDouble("uygulananSiklik", uygulananSiklik);
        ListTag gecmis = new ListTag();
        for (GecmisKaydi kayit : sonAfetler) {
            gecmis.add(kayit.yaz());
        }
        etiket.put("sonAfetler", gecmis);
        if (sonHedef != null) {
            etiket.putUUID("sonHedef", sonHedef);
        }
        etiket.putInt("sonSeviye", sonSeviye);
        etiket.putLong("gunOfseti", gunOfseti);
        etiket.putLong("uykuBirikimi", uykuBirikimi);
        CompoundTag durdurmalar = new CompoundTag();
        surecDurdurmalari.forEach((kimlik, neden) -> durdurmalar.putString(kimlik, neden.name()));
        etiket.put("surecDurdurmalari", durdurmalar);
        etiket.putLong("saat", saat);
        etiket.put("ekVeri", ekVeri.copy());
        if (canliAfet != null) {
            try {
                etiket.put("aktifAfet", canliAfet.durumYaz(new CompoundTag()));
            } catch (RuntimeException hata) {
                DogalAfetler.LOGGER.error("Aktif afet ({}) kaydedilemedi; dönem afetsiz kaydediliyor",
                        canliAfet.tur().kimlik(), hata);
            }
        } else if (bekleyenAfet != null) {
            etiket.put("aktifAfet", bekleyenAfet.copy());
        }
        return etiket;
    }

    /**
     * Kayıttan okur; bilinmeyen ya da bozuk alanlar varsayılana düşer (dünya açılışı durmaz). Özet log satırını aktif afet geri
     * yüklendikten sonra {@code AfetYoneticisi} yazar.
     */
    public static DunyaDonemi yukle(CompoundTag etiket) {
        DunyaDonemi donem = new DunyaDonemi();
        donem.ayVar = !etiket.contains("ayVar") || etiket.getBoolean("ayVar");
        donem.ayOlaySaati = etiket.contains("ayOlay") ? Math.max(AyDurumu.OLAY_YOK, etiket.getLong("ayOlay"))
                : AyDurumu.OLAY_YOK;
        donem.gunesEvresi = GunesEvresi.kimlikIle(etiket.getString("gunesEvresi"));
        donem.evreIlerlemesi = sinirla(etiket.getFloat("evreIlerlemesi"), 0f, 1f);
        donem.evreBaslangicSaati = etiket.getLong("evreBaslangic");
        donem.donmaIlerlemesi = sinirla(etiket.getFloat("donma"), 0f, DONMA_EN_COK);
        donem.cozulmeDonemi = etiket.getBoolean("cozulme");
        donem.buharlasmaDerinligi = sinirla(etiket.getFloat("buharlasma"), 0f, BUHARLASMA_EN_COK);
        donem.deniz = DenizSeviyesi.oku(etiket.getCompound("deniz"));
        List<GeciciCarpan> carpanlar = new ArrayList<>();
        for (Tag t : etiket.getList("carpanlar", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Optional<AfetTuru> tur = AfetTuru.kimlikIle(c.getString("tur"));
            try {
                tur.ifPresent(afet -> carpanlar.add(new GeciciCarpan(afet, c.getDouble("tepe"), c.getLong("baslangic"),
                        c.getLong("sabit"), c.getLong("sonum"), SonumTipi.valueOf(c.getString("tip")))));
            } catch (IllegalArgumentException hata) {
                DogalAfetler.LOGGER.warn("Bozuk geçici çarpan kaydı atlandı: {}", c, hata);
            }
        }
        donem.carpanlar.yukle(carpanlar);
        CompoundTag beklemeEtiketi = etiket.getCompound("bekleme");
        CompoundTag turler = beklemeEtiketi.getCompound("turler");
        Map<AfetTuru, Long> turBitisleri = new EnumMap<>(AfetTuru.class);
        for (String anahtar : turler.getAllKeys()) {
            AfetTuru.kimlikIle(anahtar).ifPresent(tur -> turBitisleri.put(tur, turler.getLong(anahtar)));
        }
        donem.bekleme.yukle(beklemeEtiketi.getLong("genel"), turBitisleri);
        double siklik = etiket.contains("uygulananSiklik") ? etiket.getDouble("uygulananSiklik") : 1.0;
        donem.uygulananSiklik = siklik > 0 && !Double.isInfinite(siklik) ? siklik : 1.0;
        for (Tag t : etiket.getList("sonAfetler", Tag.TAG_COMPOUND)) {
            GecmisKaydi.oku((CompoundTag) t).ifPresent(donem.sonAfetler::addLast);
        }
        while (donem.sonAfetler.size() > GECMIS_SINIRI) {
            donem.sonAfetler.removeLast();
        }
        donem.sonHedef = etiket.hasUUID("sonHedef") ? etiket.getUUID("sonHedef") : null;
        donem.sonSeviye = etiket.getInt("sonSeviye");
        donem.gunOfseti = etiket.getLong("gunOfseti");
        donem.uykuBirikimi = Math.max(0, etiket.getLong("uykuBirikimi"));
        CompoundTag durdurmalar = etiket.getCompound("surecDurdurmalari");
        for (String kimlik : durdurmalar.getAllKeys()) {
            try {
                donem.surecDurdurmalari.put(kimlik, DonemSureci.DurdurmaNedeni.valueOf(durdurmalar.getString(kimlik)));
            } catch (IllegalArgumentException hata) {
                DogalAfetler.LOGGER.warn("Bozuk dönem süreci durdurma kaydı atlandı: {}={}", kimlik,
                        durdurmalar.getString(kimlik));
            }
        }
        donem.saat = Math.max(0, etiket.getLong("saat"));
        donem.ekVeri.merge(etiket.getCompound("ekVeri"));
        if (etiket.contains("aktifAfet", Tag.TAG_COMPOUND)) {
            donem.bekleyenAfet = etiket.getCompound("aktifAfet").copy();
        }
        donem.kayittanOkundu = true;
        return donem;
    }
}
