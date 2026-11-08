package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.GunSaatiPenceresi;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;

/**
 * Afet yaşam döngüsü (GOREV §4): {@code ON_UYARI → SIREN → AKTIF → BITIS}. Afet işçileri bu sınıfı genişletir ve
 * fabrikasını {@link AfetKaydi}'na kaydeder. Aşama, süre, oyuncu takibi ve kayıt biçimi burada, tek yerde yönetilir;
 * alt sınıf yalnız afetin kendi davranışını yazar:
 *
 * <ul>
 * <li>{@link #etkiAlani()}: merkez + yarıçap (hareket eden afette güncel konum); siren ve "sağ çıkan" bundan hesaplanır.</li>
 * <li>{@link #asamaTick}: her sunucu tick'inde, aşama fark etmeksizin; {@code adim} bu tick'te ilerleyen ölçekli
 * tick sayısıdır ({@code /dogalafet hiz}, 0 olabilir).</li>
 * <li>{@link #aktifBitti()}: AKTIF aşaması kendiliğinden bitti mi.</li>
 * <li>{@link #temizle}: geride varlık ya da durum bırakmadan durur.</li>
 * <li>{@link #ozelDurumYaz}/{@link #ozelDurumOku}: afetin kendi NBT'si (sunucu yeniden başlayınca devam eder).</li>
 * </ul>
 *
 * Ön-uyarı süresini yönetici (config penceresi), siren süresini {@link SirenTuru} belirler. Afet kodu yalnız sunucu ana
 * thread'inde çağrılır.
 */
public abstract class Afet {
    private final AfetTuru tur;
    private AfetBaglami baglam;
    private AfetAsamasi asama = AfetAsamasi.ON_UYARI;
    private long asamaGecen;
    private int altAsama;
    private boolean sonlandi;
    private boolean temizlendi;
    private final Set<UUID> etkilenenler = new LinkedHashSet<>();
    private final Set<UUID> olenler = new LinkedHashSet<>();
    /** Patlama penceresi bekleme durumu (yalnız log geçişi için; kalıcı değil). */
    private boolean pencereBekleniyor;
    /** Pencere için beklenen gerçek tick (üst sınır {@link GunSaatiPenceresi#EN_COK_BEKLEME}; kayıtta). */
    private long pencereBeklenen;
    /** Pencereye kalan gerçek tick; beklenmiyorsa -1 (yalnız gösterim, kalıcı değil). */
    private long pencereKalan = -1;
    /** Hareketli afetin yöneldiği ara hedef {x, z}; bildirmeyen afette {@code null} (kalıcı değil; afet her tick yazar). */
    private double[] yolHedefi;

    protected Afet(AfetTuru tur) {
        this.tur = Objects.requireNonNull(tur, "tur");
    }

    // ---------------------------------------------------------------- alt sınıfın yazdıkları

    /**
     * Güncel etki alanı. Sunucu geneli afet {@link EtkiAlani#herYer()} döner. Çekirdek dışındaki kod bunu doğrudan çağırmaz,
     * yöneticinin korumalı {@code AfetYoneticisi.etkiAlani(afet)} okumasını kullanır (alt sınıf hatası sunucu tick'ine
     * çıkmasın; K-C32, K-C46).
     */
    public abstract EtkiAlani etkiAlani();

    /** Afetin her tick'teki işi; aşama geçişleri çağrıdan sonra temel sınıfta yapılır. */
    protected abstract void asamaTick(ServerLevel level, AfetAsamasi asama, int adim);

    /** AKTIF aşaması kendiliğinden bitti mi ({@code true} → BITIS). */
    protected abstract boolean aktifBitti();

    /**
     * Afeti durdurur; bir kez çağrılır. {@code temiz=true}: dünyada geçici hiçbir iz kalmaz (varlık, parçacık kaynağı, sanal
     * su, geçici blok) — {@code /dogalafet stop}, doğal bitiş ve {@code sifirla}. {@code temiz=false}: yumuşak durdurma;
     * afet kendi mantığını bırakır, geçici efektler sahibi alt sistemde kendiliğinden sönebilir — sunucu geneli afetin
     * gamerule'ü kapatılınca ve afet kodu hata verince.
     */
    protected abstract void temizle(ServerLevel level, boolean temiz);

    protected abstract void ozelDurumYaz(CompoundTag etiket);

    protected abstract void ozelDurumOku(CompoundTag etiket);

    /** Hedef oyuncunun çevresinde merkez seçer; varsayılan oyuncunun bulunduğu blok. */
    public BlockPos merkezSec(ServerLevel level, ServerPlayer hedef, int seviye, RandomSource rastgele) {
        return hedef.blockPosition();
    }

    /** N14: afet bu merkezi reddedebilir (ör. tsunami için yakında geniş su yok); yönetici başka oyuncu dener ya da erteler. */
    public boolean yerUygunMu(ServerLevel level, BlockPos merkez, int seviye) {
        return true;
    }

    /** {@link #baslat} sonunda, ON_UYARI'nın ilk anında. */
    protected void baslarken(ServerLevel level) {
    }

    protected void asamaDegisti(ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni) {
    }

    /**
     * {@code /dogalafet asama ileri} AKTIF aşamadayken: uzun afet bir sonraki alt aşamaya geçip {@code true} döner;
     * {@code false} dönerse afet BITIS'e geçer.
     */
    protected boolean altAsamaIleri(ServerLevel level) {
        return false;
    }

    /** BITIS (sönüm) aşaması tamam mı; varsayılan hemen. {@code gecen}: BITIS'te geçen ölçekli tick. */
    protected boolean bitisTamam(long gecen) {
        return true;
    }

    /** Sunucu yeniden açılıp afet kayıttan yüklendikten sonra (çalışma zamanı durumunu yeniden kurmak için). */
    protected void yuklendi(ServerLevel level) {
    }

    /** Sunucu kapanırken, durum kaydedilmeden önce (afet sürer; yeniden açılışta {@link #yuklendi}). */
    protected void kapanirken(ServerLevel level) {
    }

    /**
     * ON_UYARI süresi dolunca SIREN'e geçiş beklemeli mi (gökyüzü-2, istek 26): doğal başlayan güneş patlaması ve ay çarpması
     * AKTIF'e (sirenin gerçek süresi sonra) türün gün saati penceresinde ({@link GunSaatiPenceresi#dogalPatlama}) geçecek şekilde
     * ON_UYARI'da bekler (atlanmaz; pencere kaçtıysa ertesi gün). Her tick yeniden hesaplanır: uyku, {@code /time set} ve
     * {@code /dogalafet hiz} hesaba katılır. {@link GunSaatiPenceresi#EN_COK_BEKLEME} gerçek tick beklendiği hâlde pencere
     * gelmediyse (saati kilitleyen başka mod) bekleme biter. Komutla başlayan afet ve {@code asama ileri} beklemez.
     */
    protected boolean patlamaPenceresiniBekle(ServerLevel level) {
        Optional<GunSaatiPenceresi> pencere = GunSaatiPenceresi.dogalPatlama(tur);
        if (pencere.isEmpty() || baglam.elle()) {
            return false;
        }
        ServerLevel ust = level.getServer().overworld();
        long siren = AfetYoneticisi.al(level.getServer()).zamanOlcegi().gercekTick(SirenTuru.sureTick(tur));
        long gunSaati = ust.getDayTime();
        boolean bekle = GunSaatiPenceresi.bekle(pencere, false, ust.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT),
                gunSaati, siren, pencereBeklenen);
        pencereKalan = bekle ? pencere.get().bekleme(gunSaati, siren) : -1;
        if (bekle) {
            pencereBeklenen++;
        }
        if (bekle != pencereBekleniyor) {
            pencereBekleniyor = bekle;
            String durum = bekle ? "bekleniyor, bekleme " + pencereKalan + " tick"
                    : pencereBeklenen >= GunSaatiPenceresi.EN_COK_BEKLEME
                            ? "gelmedi, " + pencereBeklenen + " tick beklendi; bekleme bitti" : "geldi";
            DogalAfetler.LOGGER.info("Patlama penceresi ({}): {}; dayTime={} siren {} tick, pencere [{}, {})", tur.kimlik(),
                    durum, gunSaati, siren, pencere.get().bas(), pencere.get().son());
        }
        return bekle;
    }

    /**
     * Doğal patlama penceresine kalan gerçek tick (ON_UYARI süresi doldu, pencere bekleniyor); beklenmiyorsa -1. Yalnız
     * gösterim için ({@code /dogalafet durum}); {@link #asamaKalanTick()} bu sürede 0 kalır.
     */
    public final long pencereBeklemesi() {
        return asama == AfetAsamasi.ON_UYARI ? pencereKalan : -1;
    }

    /** AKTIF aşamasının kalan ölçekli tick'i; bilinmiyorsa -1 (komut, radar). */
    public long aktifKalanTick() {
        return -1;
    }

    /** İstemciye {@code AfetDurumPaketi} ile gidecek özel görüntü parametreleri (küçük tutulmalı; 1 Hz). */
    protected void istemciVerisiYaz(CompoundTag etiket) {
    }

    // ---------------------------------------------------------------- okuma

    public final AfetTuru tur() {
        return tur;
    }

    public final boolean basladi() {
        return baglam != null;
    }

    public final AfetBaglami baglam() {
        if (baglam == null) {
            throw new IllegalStateException(tur.kimlik() + " henüz başlatılmadı");
        }
        return baglam;
    }

    /** Seviye (1–5); seviyesiz afette 0. */
    public final int seviye() {
        return baglam().seviye();
    }

    public final AfetAsamasi asama() {
        return asama;
    }

    public final int altAsama() {
        return altAsama;
    }

    /** Geçerli aşamanın başından beri geçen ölçekli tick. */
    public final long asamaGecenTick() {
        return asamaGecen;
    }

    public final boolean sonlandi() {
        return sonlandi;
    }

    /**
     * Geçerli aşamanın kalan ölçekli tick'i; bilinmiyorsa -1. AKTIF'te alt sınıfın {@link #aktifKalanTick()}'ine iner:
     * çekirdek dışındaki kod (radar, sismograf) bunu doğrudan çağırmaz, yöneticinin korumalı
     * {@code AfetYoneticisi.kalanOku(afet)} okumasını kullanır (K-C32, K-C46).
     */
    public final long asamaKalanTick() {
        return switch (asama) {
            case ON_UYARI -> Math.max(0, baglam().onUyariSuresi() - asamaGecen);
            case SIREN -> Math.max(0, SirenTuru.sureTick(tur) - asamaGecen);
            case AKTIF -> aktifKalanTick();
            case BITIS -> -1;
        };
    }

    /**
     * Normal ön-uyarı penceresi başladı mı: Hava Radarı ve Sismograf bundan sonra görür; Hassas Sismograf ise
     * ON_UYARI'nın başından beri (madde 13b).
     */
    public final boolean normalOnUyariBasladi() {
        return asama != AfetAsamasi.ON_UYARI || asamaGecen >= baglam().erkenBildirimSuresi();
    }

    /** AKTIF aşamada etki alanına en az bir kez girmiş oyuncular. */
    public final Set<UUID> etkilenenler() {
        return Collections.unmodifiableSet(etkilenenler);
    }

    /** Etkilendikten sonra afet bitmeden ölen oyuncular. */
    public final Set<UUID> olenler() {
        return Collections.unmodifiableSet(olenler);
    }

    /**
     * Hareketli afetin şu an yöneldiği ara hedef {x, z} (Gelişmiş Hava Radarı'nın yol tahmini); bildirmeyen afette boş.
     * Alanı afetin kendisi {@link #yolHedefiAyarla} ile yazar; alt sınıf kancası olmadığından korumalı okuma gerekmez
     * (K-C46).
     */
    public final Optional<double[]> yolHedefi() {
        return yolHedefi == null ? Optional.empty() : Optional.of(yolHedefi.clone());
    }

    // ---------------------------------------------------------------- çekirdeğin çağırdıkları

    /** Afeti ON_UYARI aşamasında başlatır; bir kez çağrılır. */
    public final void baslat(ServerLevel level, AfetBaglami yeniBaglam) {
        if (baglam != null) {
            throw new IllegalStateException(tur.kimlik() + " zaten başlatıldı");
        }
        if (yeniBaglam.tur() != tur) {
            throw new IllegalArgumentException("Bağlam türü " + yeniBaglam.tur() + ", afet " + tur);
        }
        baglam = yeniBaglam;
        asama = AfetAsamasi.ON_UYARI;
        asamaGecen = 0;
        baslarken(level);
    }

    /** Bir sunucu tick'i: afetin işi, sonra süresi dolan aşamanın geçişi (tick başına en çok bir geçiş). */
    public final void tick(ServerLevel level, int adim) {
        if (sonlandi || baglam == null) {
            return;
        }
        asamaGecen += Math.max(0, adim);
        asamaTick(level, asama, adim);
        switch (asama) {
            case ON_UYARI -> {
                if (asamaGecen >= baglam.onUyariSuresi() && !patlamaPenceresiniBekle(level)) {
                    gec(level, AfetAsamasi.SIREN);
                }
            }
            case SIREN -> {
                if (asamaGecen >= SirenTuru.sureTick(tur)) {
                    gec(level, AfetAsamasi.AKTIF);
                }
            }
            case AKTIF -> {
                if (aktifBitti()) {
                    gec(level, AfetAsamasi.BITIS);
                }
            }
            case BITIS -> {
                if (bitisTamam(asamaGecen)) {
                    sonlandi = true;
                }
            }
        }
    }

    /** {@code /dogalafet asama ileri}: bir sonraki aşama (AKTIF'te önce alt aşama); BITIS'ten sonra afet sonlanır. */
    public final void asamaIleri(ServerLevel level) {
        if (sonlandi || baglam == null) {
            return;
        }
        switch (asama) {
            case ON_UYARI -> gec(level, AfetAsamasi.SIREN);
            case SIREN -> gec(level, AfetAsamasi.AKTIF);
            case AKTIF -> {
                if (!altAsamaIleri(level)) {
                    gec(level, AfetAsamasi.BITIS);
                }
            }
            case BITIS -> sonlandi = true;
        }
    }

    /** Afeti durdurur (bkz. {@link #temizle}); birden çok çağrı güvenlidir. */
    public final void durdur(ServerLevel level, boolean temiz) {
        sonlandi = true;
        if (!temizlendi) {
            temizlendi = true;
            temizle(level, temiz);
        }
    }

    /** Uzun afetin alt aşamasını değiştirir; yönetici değişimi olay ve paketle duyurur. */
    protected final void altAsamaAyarla(int yeni) {
        altAsama = Math.max(0, yeni);
    }

    /** Hareketli afet yöneldiği ara hedefi bildirir ({@link #yolHedefi()}). */
    protected final void yolHedefiAyarla(double x, double z) {
        yolHedefi = new double[] {x, z};
    }

    private void gec(ServerLevel level, AfetAsamasi yeni) {
        AfetAsamasi eski = asama;
        asama = yeni;
        asamaGecen = 0;
        asamaDegisti(level, eski, yeni);
    }

    void etkilenenEkle(UUID oyuncu) {
        etkilenenler.add(oyuncu);
    }

    /** Ölüm yalnız etkilenmiş oyuncu için sayılır. */
    void olumEkle(UUID oyuncu) {
        if (etkilenenler.contains(oyuncu)) {
            olenler.add(oyuncu);
        }
    }

    // ---------------------------------------------------------------- kayıt

    /** Afetin tüm durumunu (temel alanlar + {@code ozel}) yazar; {@code DunyaDonemi} kaydederken çağrılır. */
    public final CompoundTag durumYaz(CompoundTag etiket) {
        AfetBaglami b = baglam();
        etiket.putString("tur", tur.kimlik());
        etiket.putInt("seviye", b.seviye());
        etiket.put("merkez", NbtUtils.writeBlockPos(b.merkez()));
        etiket.putString("boyut", b.boyut().location().toString());
        if (b.hedefOyuncu() != null) {
            etiket.putUUID("hedef", b.hedefOyuncu());
        }
        etiket.putLong("baslangic", b.baslangicSaati());
        etiket.putLong("onUyari", b.onUyariSuresi());
        etiket.putLong("erken", b.erkenBildirimSuresi());
        etiket.putBoolean("elle", b.elle());
        etiket.putString("asama", asama.kimlik());
        etiket.putLong("asamaGecen", asamaGecen);
        etiket.putInt("altAsama", altAsama);
        etiket.putLong("pencereBeklenen", pencereBeklenen);
        etiket.put("etkilenenler", kimlikListesi(etkilenenler));
        etiket.put("olenler", kimlikListesi(olenler));
        CompoundTag ozel = new CompoundTag();
        ozelDurumYaz(ozel);
        etiket.put("ozel", ozel);
        return etiket;
    }

    /** {@link #durumYaz} çıktısını henüz başlatılmamış bir örneğe okur. */
    public final void durumOku(CompoundTag etiket) {
        if (baglam != null) {
            throw new IllegalStateException(tur.kimlik() + " zaten başlatıldı");
        }
        if (turOku(etiket).orElse(null) != tur) {
            throw new IllegalArgumentException("Kayıttaki tür " + etiket.getString("tur") + ", afet " + tur.kimlik());
        }
        ResourceLocation boyut = ResourceLocation.tryParse(etiket.getString("boyut"));
        if (boyut == null) {
            throw new IllegalArgumentException("Kayıtta geçersiz boyut: " + etiket.getString("boyut"));
        }
        baglam = new AfetBaglami(tur, etiket.getInt("seviye"), NbtUtils.readBlockPos(etiket.getCompound("merkez")),
                ResourceKey.create(Registries.DIMENSION, boyut), etiket.hasUUID("hedef") ? etiket.getUUID("hedef") : null,
                etiket.getLong("baslangic"), etiket.getLong("onUyari"), etiket.getLong("erken"), etiket.getBoolean("elle"));
        asama = AfetAsamasi.kimlikIle(etiket.getString("asama")).orElse(AfetAsamasi.ON_UYARI);
        asamaGecen = Math.max(0, etiket.getLong("asamaGecen"));
        altAsama = Math.max(0, etiket.getInt("altAsama"));
        pencereBeklenen = Math.max(0, etiket.getLong("pencereBeklenen"));
        kimlikleriOku(etiket.getList("etkilenenler", Tag.TAG_INT_ARRAY), etkilenenler);
        kimlikleriOku(etiket.getList("olenler", Tag.TAG_INT_ARRAY), olenler);
        ozelDurumOku(etiket.getCompound("ozel"));
    }

    /** Kayıttaki afetin türü. */
    public static Optional<AfetTuru> turOku(CompoundTag etiket) {
        return AfetTuru.kimlikIle(etiket.getString("tur"));
    }

    private static ListTag kimlikListesi(Set<UUID> kimlikler) {
        ListTag liste = new ListTag();
        for (UUID kimlik : kimlikler) {
            liste.add(NbtUtils.createUUID(kimlik));
        }
        return liste;
    }

    private static void kimlikleriOku(ListTag liste, Set<UUID> hedef) {
        hedef.clear();
        for (Tag etiket : liste) {
            hedef.add(NbtUtils.loadUUID(etiket));
        }
    }
}
